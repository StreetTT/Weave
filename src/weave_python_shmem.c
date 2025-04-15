#define WIN32_LEAN_AND_MEAN
#include <windows.h>
#include <stdlib.h>

struct ShmemArena {
        void *memory;
        uint64_t length;
        size_t capacity;
};

ShmemArena shmem_arena_init(size_t capacity) {
        malloc()
};


EXPORT void python_create_shared_mapping(size_t element_size, size_t num_elements, const char *name) {
        HANDLE file_handle = CreateFileMapping(INVALID_HANDLE_VALUE, 0, PAGE_READWRITE,  0, mapping_size, name);
}

EXPORT void python_create_shared_mapping_and_mutex(size_t element_size, size_t num_elements, const char *name) {
        HANDLE file_handle = CreateFileMapping(INVALID_HANDLE_VALUE, 0, PAGE_READWRITE,  0, mapping_size, name);
}
