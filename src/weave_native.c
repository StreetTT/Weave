
#ifdef __linux__
#define _GNU_SOURCE
#endif

#include <jni.h>
#include <stdint.h>
#include <string.h>
#include <assert.h>

#include "WeaveNativeImpl.h"
#include "weave_native.h"


JNIEXPORT jobject JNICALL Java_WeaveNativeImpl_GetProcessesOutput(JNIEnv *env, jobject obj) {
        uint64_t abs_read_offset = max(0, READER.scrollback_write_offset - READER.scrollback_buffer_size);
        int relative_read_offset = abs_read_offset & (READER.scrollback_buffer_size - 1); // mask off the high bits to get an offset

        // go to the first full line
        if (abs_read_offset != 0) {
            while (READER.scrollback_buffer1[relative_read_offset++] != '\n') {
                ++abs_read_offset;
            }
        }

        ++abs_read_offset; // skip the newline
        int read_size = READER.scrollback_write_offset - abs_read_offset;
        return (*env)->NewDirectByteBuffer(env, (READER.scrollback_buffer1 + relative_read_offset), read_size);
}

JNIEXPORT void JNICALL Java_WeaveNativeImpl_ClearProcessOutput(JNIEnv *env, jobject obj) {
    READER.scrollback_write_offset = 0;
}

static char *reader_get_write_ptr(struct Reader *reader) {
        return reader->scrollback_buffer1 + (reader->scrollback_write_offset & (reader->scrollback_buffer_size - 1));
}


#ifdef _WIN32
#define EXPORT __declspec(dllexport)
#define WIN32_LEAN_AND_MEAN
#include <Windows.h>
#pragma comment (lib, "onecore")


static int round_up_pow2(int value) {
        unsigned long index;
        _BitScanReverse(&index, value - 1);
        assert(index < 31);
        return 1U << (index + 1);
}

HANDLE pid_to_mutex(void *mutex_arr, int pid) {
        assert(pid != 0); // if this fires something has gone very very wrong
        return MUTEX_ARRAY[pid - 1];
}

static BOOL reader_read_data(struct Reader *reader) {
        int relative_write_offset = reader->scrollback_write_offset & (reader->scrollback_buffer_size - 1);
        int out_bytes;
        BOOL ok = ReadFile(reader->pipe_read_handle,
                           reader_get_write_ptr(reader),
                           reader->scrollback_buffer_size, &out_bytes, NULL); // read until we fail or run out of data to read

        reader->scrollback_write_offset += out_bytes;

        return ok;
}


static DWORD CALLBACK reader_thread(LPVOID args) {
        struct Reader *reader = (struct Reader *)args;
        for (;;) {
                if (reader->started) {
                        InterlockedExchange(&reader->working, TRUE);
                        WakeByAddressSingle(&reader->working);
                        int bytes_available;
                        // readfile will block waiting for data to be submitted, so we need to check there is actually something to read
                        // before we attempt to read it
                        PeekNamedPipe(reader->pipe_read_handle, NULL, 0, NULL, &bytes_available, NULL);
                        if (bytes_available) {
                            if (reader_read_data(reader))  {
#if 0
                                uint64_t abs_read_offset = max(0, reader->scrollback_write_offset - reader->scrollback_buffer_size);
                                int read_size = reader->scrollback_write_offset - abs_read_offset;
                                int relative_read_offset = abs_read_offset & (reader->scrollback_buffer_size - 1); // mask off the high bits to get an offset
                                fprintf(stderr, "%.*s\n", read_size, reader->scrollback_buffer1 + relative_read_offset);
                                fflush(stderr);
#endif
                            }
                        }
                } else {
                        BOOL active = FALSE;
                        InterlockedExchange(&reader->working, FALSE);
                        WakeByAddressSingle(&reader->working);
                        WaitOnAddress(&reader->started, &active, sizeof(reader->started), INFINITE);
                }
        }

        return 0;
}

JNIEXPORT void JNICALL Java_WeaveNativeImpl_ReaderThreadStart(JNIEnv *env, jobject obj) {
        SECURITY_ATTRIBUTES s = { .nLength = sizeof(s), .bInheritHandle = TRUE };
        if (!CreatePipe(&READER.pipe_read_handle, &READER.pipe_write_handle, &s, 0)) {
                fprintf(stderr, "Faiiled to create pipes");
                exit(1);
        }

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

        CloseHandle(READER.pipe_write_handle);

        BOOL ok;
        do {
                ok = reader_read_data(&READER);
                if (GetLastError() == ERROR_BROKEN_PIPE) {
                        fprintf(stderr, "FINISHED READING\n");
                }
        } while (ok);

        const char end_marker[] = ">>>>>>>>>>>>\n";
        for (const char *c = end_marker; *c != '\0'; ++c) {
                *reader_get_write_ptr(&READER) = *c;
                READER.scrollback_write_offset++;
        }

        CloseHandle(READER.pipe_read_handle);
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
                READER.scrollback_buffer_size = round_up_pow2(READER_BUFFER_SIZE);
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
        startup_info.hStdInput = GetStdHandle(STD_INPUT_HANDLE);
        startup_info.dwFlags |= STARTF_USESTDHANDLES;

        PROCESS_INFORMATION pi;
        if (!CreateProcessA(0, full_process_str, 0, 0, TRUE, NORMAL_PRIORITY_CLASS, 0, 0, &startup_info, &pi)) {
            fprintf(stderr, "C ERROR: Error creating process");
            exit(1);
        }

        return (long long)(pi.hProcess);
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

#endif

#ifdef __linux__

#include <sys/syscall.h>
#include <unistd.h>
#include <semaphore.h>
#include <sys/mman.h>
#include <fcntl.h>
#include <pthread.h>
#include <stdlib.h>
#include <sys/types.h>
#include <sys/wait.h>
#include <sys/param.h>
#include <signal.h>
#include <linux/futex.h>
#include <stdatomic.h>



static int round_up_pow2(int value) {
        int index = __builtin_clz(value - 1);
        assert(index < 31);
        return 1U << (index + 1);
}

static int reader_read_data(struct Reader *reader) {
        int relative_write_offset = reader->scrollback_write_offset & (reader->scrollback_buffer_size - 1);

        int bytes_read = read(reader->pipe_read_handle, reader_get_write_ptr(reader), reader->scrollback_buffer_size);

        if (bytes_read != -1) {
            reader->scrollback_write_offset += bytes_read;
        } else {
            bytes_read = 0;
        }

        return bytes_read;
}

static void *reader_thread(void *args) {
        struct Reader *reader = (struct Reader *)args;
        for (;;) {
                if (reader->started) {
                        atomic_exchange(&reader->working, TRUE);
                        syscall(SYS_futex, &reader->working, FUTEX_WAKE_PRIVATE, 1, NULL, NULL, 0);
                        int bytes_available;
                        if (reader_read_data(reader))  {
#if 0
                            uint64_t abs_read_offset = MAX(0, reader->scrollback_write_offset - reader->scrollback_buffer_size);
                            int read_size = reader->scrollback_write_offset - abs_read_offset;
                            int relative_read_offset = abs_read_offset & (reader->scrollback_buffer_size - 1); // mask off the high bits to get an offset
                            fprintf(stderr, "%.*s\n", read_size, reader->scrollback_buffer1 + relative_read_offset);
                            fflush(stderr);
#endif
                        }
                } else {
                        atomic_exchange(&reader->working, FALSE);
                        syscall(SYS_futex, &reader->working, FUTEX_WAKE_PRIVATE, 1, NULL, NULL, 0);
                        syscall(SYS_futex, &reader->started, FUTEX_WAIT_PRIVATE, 0, NULL, NULL, 0);
                }
        }

        return 0;
}

JNIEXPORT void JNICALL Java_WeaveNativeImpl_ReaderThreadStart(JNIEnv *env, jobject obj) {
        if (pipe(&READER.pipe_read_handle) == -1) {
                fprintf(stderr, "Reader failed to create pipe\n");
                exit(1);
        }

        fcntl(READER.pipe_read_handle, F_SETFL, fcntl(READER.pipe_read_handle, F_GETFL) | O_NONBLOCK); // set non blocking

        assert(READER.working == FALSE);
        atomic_exchange(&READER.started, TRUE); // interlocked here because cba using memory barriers
        syscall(SYS_futex, &READER.started, FUTEX_WAKE_PRIVATE, 1, NULL, NULL, 0);

        // wait until the reader thread has actually started work
        while (READER.working == FALSE) {
                syscall(SYS_futex, &READER.working, FUTEX_WAIT_PRIVATE, FALSE, NULL, NULL, 0);
        }
}

JNIEXPORT void JNICALL Java_WeaveNativeImpl_ReaderThreadStop(JNIEnv *env, jobject obj) {
        assert(READER.started == TRUE);
        atomic_exchange(&READER.started, FALSE);
        fprintf(stderr, "ATTEMPTING FINISHED READING\n");
        while (READER.working == TRUE) {
                syscall(SYS_futex, &READER.working, FUTEX_WAIT_PRIVATE, TRUE, NULL, NULL, 0);
        }

        close(READER.pipe_write_handle); // close our write handle

        fcntl(READER.pipe_read_handle, F_SETFL, fcntl(READER.pipe_read_handle, F_GETFL) & ~O_NONBLOCK); // set blocking again just incase
        int ok;
        fprintf(stderr, "ATTEMPTING FINISHED READING\n");
        do {
                ok = reader_read_data(&READER);
        } while (ok);
        fprintf(stderr, "FINISHED READING\n");

        const char end_marker[] = ">>>>>>>>>>>>\n";
        for (const char *c = end_marker; *c != '\0'; ++c) {
                *reader_get_write_ptr(&READER) = *c;
                READER.scrollback_write_offset++;
        }

        close(READER.pipe_read_handle); // close the pipe up
}

sem_t *pid_to_mutex(void *mutex_arr, int pid) {
        fprintf(stderr, "before assert");
        assert(pid != 0); // if this fires something has gone very very wrong
        fprintf(stderr, "after assert\n");
        fprintf(stderr, "%d", pid);
        sem_t *result = MUTEX_ARRAY + (pid - 1);
        fprintf(stderr, "got result\n");

        return result;
}


JNIEXPORT void JNICALL Java_WeaveNativeImpl_Init(JNIEnv *env, jobject obj) {
        // Setup shared memory buffers
        {
                FILE_HANDLE = memfd_create("shared_map", 0);

                if(ftruncate(FILE_HANDLE, MAPPING_SIZE) == -1) {
                        fprintf(stderr, "Failed to truncate memory region\n");
                        exit(1);
                }

                MAPPED_FILE = mmap(0, MAPPING_SIZE, PROT_READ | PROT_WRITE, MAP_SHARED, FILE_HANDLE, 0);

                if (FILE_HANDLE == -1 || MAPPED_FILE == MAP_FAILED) {
                        fprintf(stderr, "Failed to create shared mem mapping\n");
                        exit(1);
                }

                MUTEX_ARRAY = (sem_t *)MAPPED_FILE;
                fprintf(stderr, "MUTEX_ARRAY: %p\n", MUTEX_ARRAY);
                fprintf(stderr, "sem_t size: %lu\n", sizeof(sem_t));
                for (int i = 0; i < MAX_PROCESSES; ++i) {
                        sem_init(MUTEX_ARRAY + i, 1, 1);
                }
        }

        //setup stdout pipe & scrollback buffer for output terminal
        {
                // allocate physical mapping
                READER.scrollback_buffer_size = round_up_pow2(READER_BUFFER_SIZE);
                int scrollback_fd = memfd_create("ring_buffer", MFD_CLOEXEC);
                if (scrollback_fd == -1)  {
                        fprintf(stderr, "Failed to get scrollback mapping");
                }

                if (ftruncate(scrollback_fd, READER.scrollback_buffer_size) == -1) {
                        fprintf(stderr, "Failed to truncate ring buffer\n");
                }

                //allocate two virtual pages to the same mapping for ring buffer
                READER.scrollback_buffer1 = (char *)mmap(0, READER.scrollback_buffer_size,
                                                         PROT_READ | PROT_WRITE, MAP_SHARED, scrollback_fd, 0);
                READER.scrollback_buffer2 = (char *)mmap(READER.scrollback_buffer1 + READER.scrollback_buffer_size,
                                                         READER.scrollback_buffer_size, PROT_READ | PROT_WRITE,
                                                         MAP_SHARED | MAP_FIXED, scrollback_fd, 0);

                assert(READER.scrollback_buffer1 != MAP_FAILED && READER.scrollback_buffer2 != MAP_FAILED);

                pthread_attr_t attr = {};
                pthread_attr_setdetachstate(&attr, PTHREAD_CREATE_DETACHED);
                pthread_t thread;
                pthread_create(&thread, &attr, reader_thread, &READER);
                close(scrollback_fd);
        }
}

JNIEXPORT jobject JNICALL Java_WeaveNativeImpl_GetSignalArray(JNIEnv *env, jobject obj) {
        return (*env)->NewDirectByteBuffer(env, (char *)(MAPPED_FILE) + (sizeof(sem_t) * MAX_PROCESSES), MAX_PROCESSES);
}


JNIEXPORT void JNICALL Java_WeaveNativeImpl_DeInit(JNIEnv *env, jobject obj) {
        munmap(MAPPED_FILE, MAPPING_SIZE);
        close(FILE_HANDLE);
        munmap(READER.scrollback_buffer1, READER.scrollback_buffer_size);
        munmap(READER.scrollback_buffer2, READER.scrollback_buffer_size);
}

JNIEXPORT void JNICALL Java_WeaveNativeImpl_WaitForProcess(JNIEnv *env, jobject obj, jint pid) {
        fprintf(stderr, "Waitig for sem\n");
        if (sem_wait(pid_to_mutex(MUTEX_ARRAY, pid)) == -1) {
            fprintf(stderr, "failed waiting for sem\n");
        }
}

JNIEXPORT void JNICALL Java_WeaveNativeImpl_ReleaseProcess(JNIEnv *env, jobject obj, jint pid) {
        if (sem_post(pid_to_mutex(MUTEX_ARRAY, pid)) == -1) {
                fprintf(stderr, "failed to post\n");
        }
}

JNIEXPORT jlong JNICALL Java_WeaveNativeImpl_CreatePythonProcess(JNIEnv *env, jobject obj, jstring filename) {
        const char *c_filename = (*env)->GetStringUTFChars(env, filename, 0);
        fprintf(stderr, "%s\n", c_filename);

        pid_t pid = fork();
        if (pid == -1)  {
                fprintf(stderr, "Failed to create python process\n");
                return 0;
        }

        if (pid == 0) {
            char fd_string[10];
            snprintf(fd_string, sizeof(fd_string), "%d", FILE_HANDLE);
            fprintf(stderr, "fdstring: %s\n", fd_string);

            //Redirect stdout
            close(READER.pipe_read_handle);
            dup2(READER.pipe_write_handle, STDOUT_FILENO);
            close(READER.pipe_write_handle);

            setenv("WEAVE_SHARED_MAP", fd_string, 1);
            execlp("python3", "python3", c_filename, NULL);
        } else {
                return (long long)(pid);
        }
}

JNIEXPORT void JNICALL Java_WeaveNativeImpl_ClearProcessOutput(JNIEnv *env, jobject obj) {
        READER.scrollback_write_offset = 0;
}

JNIEXPORT jboolean JNICALL Java_WeaveNativeImpl_isProcessAlive(JNIEnv *env, jobject obj, jlong pHandle) {
        int status;
        if (waitpid((pid_t)pHandle, &status, WNOHANG) == 0) {
                return JNI_TRUE;
        } else if(WIFEXITED(status) || WIFSTOPPED(status)) {
                return JNI_FALSE;
        }

        return JNI_TRUE;
}

EXPORT void python_mutex_lock(void *mutex) {
        fprintf(stderr, "converting mut\n");
        fprintf(stderr, "mutex: %p\n", mutex);
        sem_wait((sem_t *)mutex);
}

EXPORT void python_mutex_release(void *mutex) {
        sem_post((sem_t *)mutex);
}

#endif