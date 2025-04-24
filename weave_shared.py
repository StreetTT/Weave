import mmap
import time
import ctypes
import sys
import os

#TODO(Ray): Wrap in class
WEAVE_IPCMEM = None
PID = None
LIB = None
PROGRAM_MUTEX = None
PROGRAM_SIGNAL_IDX = None
_MAX_PROCESSES = 256

__W_PROCESS_ACTIVE = 1
__W_PROCESS_FINISHED = 2
__W_PROCESS_ERROR = 3

if sys.platform == "win32":
    def __WEAVE_PID_TO_MUTEX(pid):
        mutex_size = 8
        pid_mutex_idx_start = mutex_size * (PID - 1)
        pid_mutex_idx_end = mutex_size * (PID)

        return int.from_bytes(WEAVE_IPCMEM[pid_mutex_idx_start:pid_mutex_idx_end], byteorder="little")

elif sys.platform == "linux":
    def __WEAVE_PID_TO_MUTEX(pid):
        mutex_size = 32 # mutexes are actually massive on linux
        pid_mutex_idx = mutex_size * (PID - 1)
        buffer = (ctypes.c_char * MAPPING_SIZE).from_buffer(WEAVE_IPCMEM)
        addr = ctypes.addressof(buffer)
        return addr + pid_mutex_idx

def __WEAVE_PROCESS_START(pid):
    global WEAVE_IPCMEM
    global LIB
    global PID
    global PROGRAM_SIGNAL_IDX
    global MAPPING_SIZE
    PID = pid
    #get access to the shared buffer
    platform = sys.platform
    if platform == "win32":
        MUTEX_SIZE = 8
        WEAVE_IPCMEM = mmap.mmap(-1, MUTEX_SIZE*_MAX_PROCESSES + _MAX_PROCESSES, "WEAVE_SHARED_IPC", access=mmap.ACCESS_WRITE)
        LIB = ctypes.CDLL("./lib/weave_native.dll")
    elif platform == "linux":
        MUTEX_SIZE = 32
        fd = os.getenv("WEAVE_SHARED_MAP")
        print(int(fd), flush=True)
        WEAVE_IPCMEM = mmap.mmap(int(fd), MUTEX_SIZE*_MAX_PROCESSES + _MAX_PROCESSES,
                                 flags=mmap.MAP_SHARED, prot=mmap.PROT_WRITE | mmap.PROT_READ, access=mmap.ACCESS_WRITE)
        LIB = ctypes.CDLL("./lib/weave_native.so")


    MAPPING_SIZE = MUTEX_SIZE*_MAX_PROCESSES + _MAX_PROCESSES
    PID = pid

    pid_signal_idx = pid
    signal_offset = MUTEX_SIZE * _MAX_PROCESSES

    PROGRAM_SIGNAL_IDX = signal_offset + PID

    LIB.python_mutex_lock.argtypes = [ctypes.c_void_p]
    LIB.python_mutex_release.argtypes = [ctypes.c_void_p]

    LIB.python_mutex_lock.restype = None
    LIB.python_mutex_release.restype = None

    LIB.python_mutex_lock(__WEAVE_PID_TO_MUTEX(PID))
    WEAVE_IPCMEM[PROGRAM_SIGNAL_IDX] = __W_PROCESS_ACTIVE

# Must occur after every single function call/block
def __WEAVE_WAIT_TILL_SCHEDULED():
    LIB.python_mutex_release(__WEAVE_PID_TO_MUTEX(PID))
    while (WEAVE_IPCMEM[PROGRAM_SIGNAL_IDX] == __W_PROCESS_ACTIVE):
        continue

    LIB.python_mutex_lock(__WEAVE_PID_TO_MUTEX(PID))
    WEAVE_IPCMEM[PROGRAM_SIGNAL_IDX] = __W_PROCESS_ACTIVE


def __WEAVE_PROCESS_END_ERROR():
    WEAVE_IPCMEM[PROGRAM_SIGNAL_IDX] = __W_PROCESS_ERROR
    LIB.python_mutex_release(__WEAVE_PID_TO_MUTEX(PID))
    WEAVE_IPCMEM.close()

def __WEAVE_PROCESS_END():
    WEAVE_IPCMEM[PROGRAM_SIGNAL_IDX] = __W_PROCESS_FINISHED
    LIB.python_mutex_release(__WEAVE_PID_TO_MUTEX(PID))
    WEAVE_IPCMEM.close()