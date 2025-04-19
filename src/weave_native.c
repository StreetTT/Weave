#include <jni.h>
#include <stdint.h>
#include <Windows.h>
#include <string.h>
#include <assert.h>

#include "WeaveNativeImpl.h"

#pragma comment (lib, "onecore")

#define MAX_PROCESSES (256)
#define EXPORT __declspec(dllexport)
#define ARRSIZE(arr) sizeof(arr)/sizeof(arr[0])

struct Reader {
        HANDLE pipe_write_handle;
        HANDLE pipe_read_handle;

        char *scrollback_buffer1;
        char *scrollback_buffer2;
        int64_t scrollback_buffer_size;
        int64_t scrollback_write_offset;

        volatile BOOL started;
        volatile BOOL working;
};

static void *MAPPED_FILE;
static HANDLE FILE_HANDLE;
static HANDLE *MUTEX_ARRAY;
static struct Reader READER;

static int round_up_pow2(int value) {
        unsigned long index;
        _BitScanReverse(&index, value - 1);
        assert(index < 31);
        return 1U << (index + 1);
}

static DWORD CALLBACK reader_thread(LPVOID args) {
        struct Reader *reader = (struct Reader *)args;
        for (;;) {
                if (reader->started) {
                        InterlockedExchange(&reader->working, TRUE);
                        WakeByAddressSingle(&reader->working);
                        int relative_write_offset = reader->scrollback_write_offset & (reader->scrollback_buffer_size - 1);
                        int bytes_available;
                        // readfile will block waiting for data to be submitted, so we need to check there is actually something to read
                        // before we attempt to read it
                        PeekNamedPipe(reader->pipe_read_handle, NULL, 0, NULL, &bytes_available, NULL);
                        if (bytes_available) {
                            int out_bytes;
                            BOOL ok = ReadFile(reader->pipe_read_handle,
                                               reader->scrollback_buffer1 + relative_write_offset,
                                               reader->scrollback_buffer_size, &out_bytes, NULL); // read until we fail or run out of data to read

                            if (ok && out_bytes > 0)  {
                                reader->scrollback_write_offset += out_bytes;
                                uint64_t abs_read_offset = max(0, reader->scrollback_write_offset - reader->scrollback_buffer_size);
                                int read_size = reader->scrollback_write_offset - abs_read_offset;
                                int relative_read_offset = abs_read_offset & (reader->scrollback_buffer_size - 1); // mask off the high bits to get an offset
                                fprintf(stderr, "%.*s\n", read_size, reader->scrollback_buffer1 + relative_read_offset);
                                fflush(stderr);
                            }
                        }
                } else {
                        BOOL active = FALSE;
                        InterlockedExchange(&reader->working, FALSE);
                        WakeByAddressSingle(&reader->working);
                        WaitOnAddress(&reader->started, &active, sizeof(reader->started), INFINITE);
                }
        }
}

JNIEXPORT void JNICALL Java_WeaveNativeImpl_ReaderThreadStart(JNIEnv *env, jobject obj) {
        assert(READER.working == FALSE);
        InterlockedExchange(&READER.started, TRUE); // interlocked here because cba using memory barriers
        WakeByAddressSingle(&READER.started);

        // wait until the reader thread has actually started work
        while (READER.working == FALSE) {
                BOOL working = FALSE;
                WaitOnAddress(&READER.working, &working, sizeof(READER.working), INFINITE);
        }
}

JNIEXPORT void JNICALL Java_WeaveNativeImpl_ReaderThreadStop(JNIEnv *env, jobject obj) {
        assert(READER.started == TRUE);
        InterlockedExchange(&READER.started, FALSE);
        while (READER.working == TRUE) {
                BOOL working = TRUE;
                WaitOnAddress(&READER.working, &working, sizeof(READER.working), INFINITE);
        }
}

HANDLE pid_to_mutex(void *mutex_arr, int pid) {
        assert(pid != 0); // if this fires something has gone very very wrong 
        return MUTEX_ARRAY[pid - 1];
}

JNIEXPORT void JNICALL Java_WeaveNativeImpl_Init(JNIEnv *env, jobject obj) {
        // Setup shared memory buffers
        {
                int mapping_size = sizeof(HANDLE) * MAX_PROCESSES + MAX_PROCESSES;
                FILE_HANDLE = CreateFileMapping(INVALID_HANDLE_VALUE, 0, PAGE_READWRITE,  0, mapping_size, "WEAVE_SHARED_IPC");
                MAPPED_FILE = MapViewOfFile(FILE_HANDLE, FILE_MAP_ALL_ACCESS, 0, 0, 0);

                if (!FILE_HANDLE || !MAPPED_FILE) {
                        fprintf(stderr, "Failed to create shared mem mapping");
                        exit(1);
                }

                SECURITY_ATTRIBUTES s = { .nLength = sizeof(s), .bInheritHandle = TRUE };
                MUTEX_ARRAY = (HANDLE *)MAPPED_FILE;
                for (int i = 0; i < MAX_PROCESSES; ++i) {
                        MUTEX_ARRAY[i] = CreateSemaphore(&s, 1, 1, 0);
                }
        }

        //setup stdout pipe & scrollback buffer for output terminal
        {
                SECURITY_ATTRIBUTES s = { .nLength = sizeof(s), .bInheritHandle = TRUE };
                if (!CreatePipe(&READER.pipe_read_handle, &READER.pipe_write_handle, &s, 0)) {
                        fprintf(stderr, "Faiiled to create pipes");
                        exit(1);
                }

                READER.scrollback_buffer_size = round_up_pow2(1024 * 1000);
                char *placeholder1 = (char *)VirtualAlloc2(NULL, 0, READER.scrollback_buffer_size * 2,
                                                           MEM_RESERVE | MEM_RESERVE_PLACEHOLDER,
                                                           PAGE_NOACCESS, NULL, 0);

                char *placeholder2 = placeholder1 + READER.scrollback_buffer_size;

                BOOL ok = VirtualFree(placeholder1, READER.scrollback_buffer_size, MEM_RELEASE | MEM_PRESERVE_PLACEHOLDER);
                assert(ok);

                // allocate a physical memory file map
                HANDLE section = CreateFileMapping(INVALID_HANDLE_VALUE, NULL, PAGE_READWRITE,
                                                   0, READER.scrollback_buffer_size, NULL);
                assert(section);

                // allocate 2 virtual pages to the same filemap for ring buffer
                READER.scrollback_buffer1 = (char *)MapViewOfFile3(section, NULL, placeholder1, 0,
                                                            READER.scrollback_buffer_size,
                                                            MEM_REPLACE_PLACEHOLDER, PAGE_READWRITE, NULL, 0);

                READER.scrollback_buffer2 = (char *)MapViewOfFile3(section, NULL, placeholder2, 0,
                                                            READER.scrollback_buffer_size,
                                                            MEM_REPLACE_PLACEHOLDER, PAGE_READWRITE, NULL, 0);

                assert(READER.scrollback_buffer1 && READER.scrollback_buffer2);

                // kick off the reader thread
                CreateThread(NULL, 0, reader_thread, &READER, 0, NULL);

                // the memory will only be freed when its unmaped
                VirtualFree(placeholder1, 0, MEM_RELEASE);
                VirtualFree(placeholder2, 0, MEM_RELEASE);
                CloseHandle(section);
        }
}

JNIEXPORT jobject JNICALL Java_WeaveNativeImpl_GetSignalArray(JNIEnv *env, jobject obj) {
        return (*env)->NewDirectByteBuffer(env, (char *)(MAPPED_FILE) + (sizeof(HANDLE) * MAX_PROCESSES), MAX_PROCESSES);
}

JNIEXPORT void JNICALL Java_WeaveNativeImpl_DeInit(JNIEnv *env, jobject obj) {
        UnmapViewOfFile(MAPPED_FILE);
        CloseHandle(FILE_HANDLE);
        UnmapViewOfFile(READER.scrollback_buffer1);
        UnmapViewOfFile(READER.scrollback_buffer2);
}

JNIEXPORT void JNICALL Java_WeaveNativeImpl_WaitForProcess(JNIEnv *env, jobject obj, jint pid) {
        WaitForSingleObject(pid_to_mutex(MUTEX_ARRAY, pid), INFINITE);
}

JNIEXPORT void JNICALL Java_WeaveNativeImpl_ReleaseProcess(JNIEnv *env, jobject obj, jint pid) {
        ReleaseSemaphore(pid_to_mutex(MUTEX_ARRAY, pid), 1, 0);
}

JNIEXPORT jlong JNICALL Java_WeaveNativeImpl_CreatePythonProcess(JNIEnv *env, jobject obj, jstring filename) {
        const char *c_filename = (*env)->GetStringUTFChars(env, filename, 0);

        char full_process_str[4096] = "python ";

        const int buffsize = ARRSIZE(full_process_str);
        const int remaining_size = buffsize - (sizeof("python ") - 1);
        const int offset = buffsize - remaining_size;

        if (strcpy_s(full_process_str + offset, remaining_size, c_filename) != 0) {
            return 0;
        }

        STARTUPINFO startup_info = {};
        startup_info.cb = sizeof(STARTUPINFO);
        startup_info.hStdError = READER.pipe_write_handle;
        startup_info.hStdOutput = READER.pipe_write_handle;
        startup_info.dwFlags |= STARTF_USESTDHANDLES;

        PROCESS_INFORMATION pi;
        if (!CreateProcessA(0, full_process_str, 0, 0, TRUE, NORMAL_PRIORITY_CLASS, 0, 0, &startup_info, &pi)) {
            fprintf(stderr, "C ERROR: Error creating process");
            exit(1);
        }

        return (long long)(pi.hProcess);
}

JNIEXPORT jobject JNICALL Java_WeaveNativeImpl_GetProcessesOutput(JNIEnv *env, jobject obj) {
        uint64_t abs_read_offset = max(0, READER.scrollback_write_offset - READER.scrollback_buffer_size);
        int read_size = READER.scrollback_write_offset - abs_read_offset;
        int relative_read_offset = abs_read_offset & (READER.scrollback_buffer_size - 1); // mask off the high bits to get an offset
        return (*env)->NewDirectByteBuffer(env, (READER.scrollback_buffer1 + relative_read_offset), read_size);
}

JNIEXPORT jboolean JNICALL Java_WeaveNativeImpl_isProcessAlive(JNIEnv *env, jobject obj, jlong pHandle) {
        DWORD exit_code;
        if (GetExitCodeProcess((HANDLE)pHandle, &exit_code)) {
                if (exit_code == STILL_ACTIVE) {
                    return TRUE;
                } else {
                    return FALSE;
                }
        }

        return FALSE;
}

EXPORT void python_mutex_lock(void *mutex) {
        WaitForSingleObject((HANDLE)mutex, INFINITE);
}

EXPORT void python_mutex_release(void *mutex) {
        ReleaseSemaphore((HANDLE)mutex, 1, 0);
}