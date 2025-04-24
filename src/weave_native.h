#define MAX_PROCESSES (256)
#define ARRSIZE(arr) sizeof(arr)/sizeof(arr[0])
#define READER_BUFFER_SIZE (4096 * 16) // mulitple of 4KB page

#define WIN32_LEAN_AND_MEAN
#include <windows.h>

#ifdef _WIN32
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

static HANDLE FILE_HANDLE;

static void *MAPPED_FILE;
static HANDLE *MUTEX_ARRAY;
static int MAPPING_SIZE = sizeof(HANDLE) * MAX_PROCESSES + MAX_PROCESSES;

#endif

#ifdef __linux__
#define EXPORT __attribute__((visibility("default")))
#define TRUE (1)
#define FALSE (0)

#define max(a, b) MAX(a,b)

struct Reader {
        int pipe_read_handle;
        int pipe_write_handle;

        char *scrollback_buffer1;
        char *scrollback_buffer2;
        int64_t scrollback_buffer_size;
        int64_t scrollback_write_offset;

        volatile uint32_t started;
        volatile uint32_t working;
};

static int MAPPING_SIZE = sizeof(sem_t) * MAX_PROCESSES + MAX_PROCESSES;
static void *MAPPED_FILE;
static int FILE_HANDLE;
static sem_t *MUTEX_ARRAY;
#endif

static struct Reader READER;


