#include <jni.h>

#include <stdint.h>
#include <Windows.h>

#define MAX_PROCESSES (255)
#define EXPORT __declspec(dllexport)


static void *mapped_file;  
static HANDLE file_handle;

JNIEXPORT void JNICALL Java_SharedMemory_AlocWeaveSharedBuffer(JNIEnv *env, jclass cls) {
        int mapping_size = sizeof(HANDLE) * MAX_PROCESSES + MAX_PROCESSES;
        file_handle = CreateFileMapping(INVALID_HANDLE_VALUE, 0, PAGE_READWRITE,  0, mapping_size, "WEAVE_SHARED_IPC");
        mapped_file = MapViewOfFile(file_handle, FILE_MAP_ALL_ACCESS, 0, 0, 0);
}

JNIEXPORT jobject JNICALL Java_SharedMemory_GetMutexByteArray(JNIEnv *env, jclass cls, jint ammount) {
        SECURITY_ATTRIBUTES s = { .nLength = sizeof(s), .bInheritHandle = TRUE };
        HANDLE *handles = (HANDLE *)mapped_file;
        for (int i = 0; i < ammount; ++i) {
                handles[i] = CreateSemaphore(&s, 1, 1, 0);
        }

        return (*env)->NewDirectByteBuffer(env, mapped_file, sizeof(HANDLE) * MAX_PROCESSES);
}


JNIEXPORT jobject JNICALL Java_SharedMemory_GetSignalArray(JNIEnv *env, jclass cls, jint ammount) {
        return (*env)->NewDirectByteBuffer(env, (char *)(mapped_file) + (sizeof(HANDLE) * MAX_PROCESSES), MAX_PROCESSES);
}

JNIEXPORT void JNICALL Java_SharedMemory_FreeWeaveSharedBuffer(JNIEnv *env, jclass cls) {
        UnmapViewOfFile(mapped_file);
        CloseHandle(file_handle);

}

JNIEXPORT void JNICALL Java_SharedMemory_WaitForMultipleMutex(JNIEnv *env, jclass cls, jlongArray handle_arr, jint count) {
        jlong *c_handle_arr = (*env)->GetLongArrayElements(env, handle_arr, 0);
        WaitForMultipleObjects(count, (HANDLE *)c_handle_arr, TRUE, INFINITE);
        (*env)->ReleaseLongArrayElements(env, handle_arr, c_handle_arr, 0);
}

JNIEXPORT void JNICALL Java_SharedMemory_WaitForMutex(JNIEnv *env, jclass cls, jlong mutex) {
        WaitForSingleObject((HANDLE)mutex, INFINITE);
}
JNIEXPORT void JNICALL Java_SharedMemory_ReleaseMutex(JNIEnv *env, jclass cls, jlong mutex) {
        ReleaseSemaphore((HANDLE)mutex, 1, 0);
}

JNIEXPORT jlong JNICALL Java_SharedMemory_CreateProcess(JNIEnv *env, jclass cls, jstring process) {
        char *c_process = (*env)->GetStringUTFChars(env, process, 0);
        STARTUPINFO si = {sizeof(si)};
        PROCESS_INFORMATION pi;
        CreateProcessA(0, c_process, 0, 0, TRUE, NORMAL_PRIORITY_CLASS, 0, 0, &si,
                       &pi);

        return (void *)pi.hProcess;
}

EXPORT void python_mutex_lock(void *mutex) {
        WaitForSingleObject((HANDLE)mutex, INFINITE);
}

EXPORT void python_mutex_release(void *mutex) {
        ReleaseSemaphore((HANDLE)mutex, 1, 0);
}
