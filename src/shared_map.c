#include <jni.h>

#include <stdint.h>
#include <Windows.h>
#include <string.h>
#include <assert.h>

#define MAX_PROCESSES (256)
#define EXPORT __declspec(dllexport)
#define ARRSIZE(arr) sizeof(arr)/sizeof(arr[0])


static void *mapped_file;  
static HANDLE file_handle;
static HANDLE *mutex_array;

HANDLE pid_to_mutex(void *mutex_arr, int pid) {
        assert(pid != 0); // if this fires something has gone very very wrong 
        return (HANDLE *)(mutex_array)[pid - 1];
}

JNIEXPORT void JNICALL Java_SharedMemory_AlocWeaveSharedBuffer(JNIEnv *env, jclass cls) {
        int mapping_size = sizeof(HANDLE) * MAX_PROCESSES + MAX_PROCESSES;
        file_handle = CreateFileMapping(INVALID_HANDLE_VALUE, 0, PAGE_READWRITE,  0, mapping_size, "WEAVE_SHARED_IPC");
        mapped_file = MapViewOfFile(file_handle, FILE_MAP_ALL_ACCESS, 0, 0, 0);

        SECURITY_ATTRIBUTES s = { .nLength = sizeof(s), .bInheritHandle = TRUE };
        mutex_array = (HANDLE *)mapped_file;
        for (int i = 0; i < MAX_PROCESSES; ++i) {
                mutex_array[i] = CreateSemaphore(&s, 1, 1, 0);
        }
}

JNIEXPORT jobject JNICALL Java_SharedMemory_GetSignalArray(JNIEnv *env, jclass cls) {
        return (*env)->NewDirectByteBuffer(env, (char *)(mapped_file) + (sizeof(HANDLE) * MAX_PROCESSES), MAX_PROCESSES);
}

JNIEXPORT void JNICALL Java_SharedMemory_FreeWeaveSharedBuffer(JNIEnv *env, jclass cls) {
        UnmapViewOfFile(mapped_file);
        CloseHandle(file_handle);
}

JNIEXPORT void JNICALL Java_SharedMemory_WaitForProcess(JNIEnv *env, jclass cls, jint pid) {
        WaitForSingleObject(pid_to_mutex(mutex_array, pid), INFINITE);
}

JNIEXPORT void JNICALL Java_SharedMemory_ReleaseProcess(JNIEnv *env, jclass cls, jint pid) {
        ReleaseSemaphore(pid_to_mutex(mutex_array, pid), 1, 0);
}

JNIEXPORT void JNICALL Java_SharedMemory_CreatePythonProcess(JNIEnv *env, jclass cls, jstring filename) {
        const char *c_filename = (*env)->GetStringUTFChars(env, filename, 0);

        char full_process_str[4096] = "python ";

        const int buffsize = ARRSIZE(full_process_str);
        const int remaining_size = buffsize - (sizeof("python ") - 1);
        const int offset = buffsize - remaining_size;

        if (strcpy_s(full_process_str + offset, remaining_size, c_filename) != 0) {

        }

        STARTUPINFO si = {sizeof(si)};
        PROCESS_INFORMATION pi;
        if (!CreateProcessA(0, full_process_str, 0, 0, TRUE, NORMAL_PRIORITY_CLASS, 0, 0, &si, &pi)) {
            fprintf(stderr, "C ERROR: Error creating process");
        }
}

EXPORT void python_mutex_lock(void *mutex) {
        WaitForSingleObject((HANDLE)mutex, INFINITE);
}

EXPORT void python_mutex_release(void *mutex) {
        ReleaseSemaphore((HANDLE)mutex, 1, 0);
}
