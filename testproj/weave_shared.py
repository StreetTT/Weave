import mmap
import time
import ctypes

WEAVE_IPCMEM = None
PID = None
LIB = None
PROGRAM_MUTEX = None
PROGRAM_SIGNAL_IDX = None
_MAX_PROCESSES = 256

def __WEAVE_PROCESS_START(pid):
    global WEAVE_IPCMEM
    global LIB
    global PROGRAM_MUTEX
    global PID
    global PROGRAM_SIGNAL_IDX

    PID = pid;
    #get access to the shared buffer
    WEAVE_IPCMEM = mmap.mmap(-1, 8*_MAX_PROCESSES + _MAX_PROCESSES, "WEAVE_SHARED_IPC", access=mmap.ACCESS_WRITE)
    LIB = ctypes.CDLL("./lib/shared_map.dll")

    pid_mutex_idx_start = 8 * pid
    pid_mutex_idx_end = 8 * (pid + 1)
    pid_signal_idx = pid
    signal_offset = 8 * _MAX_PROCESSES

    PROGRAM_SIGNAL_IDX = signal_offset + pid
    PROGRAM_MUTEX = ctypes.c_void_p(int.from_bytes(WEAVE_IPCMEM[pid_mutex_idx_start:pid_mutex_idx_end], "little"))

    LIB.python_mutex_lock.argtypes = [ctypes.c_void_p]
    LIB.python_mutex_release.argtypes = [ctypes.c_void_p]

    LIB.python_mutex_lock.restype = None
    LIB.python_mutex_release.restype = None

    LIB.python_mutex_lock(PROGRAM_MUTEX)
    WEAVE_IPCMEM[PROGRAM_SIGNAL_IDX] = 1

# Must occur after every single function call/block
def __WEAVE_WAIT_TILL_SCHEDULED():
    LIB.python_mutex_release(PROGRAM_MUTEX)
    while (WEAVE_IPCMEM[PROGRAM_SIGNAL_IDX] == 1):
        continue

    LIB.python_mutex_lock(PROGRAM_MUTEX)
    WEAVE_IPCMEM[PROGRAM_SIGNAL_IDX] = 1


def __WEAVE_PROCESS_END():
    LIB.python_mutex_release(PROGRAM_MUTEX)
    WEAVE_IPCMEM.close()