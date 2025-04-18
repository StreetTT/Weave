import mmap
import time
import ctypes

WEAVE_IPCMEM = None
PID = None
LIB = None
PROGRAM_MUTEX = None
PROGRAM_SIGNAL_IDX = None
_MAX_PROCESSES = 256


def __WEAVE_PID_TO_MUTEX(pid):
    mutex_size = 8
    pid_mutex_idx_start = mutex_size * (PID - 1)
    pid_mutex_idx_end = mutex_size * (PID)

    return int.from_bytes(WEAVE_IPCMEM[pid_mutex_idx_start:pid_mutex_idx_end], byteorder="little")

def __WEAVE_PROCESS_START(pid):
    global WEAVE_IPCMEM
    global LIB
    global PID
    global PROGRAM_SIGNAL_IDX
    MUTEX_SIZE = 8

    PID = pid
    #get access to the shared buffer
    print("attempting to map mem", flush=True)
    WEAVE_IPCMEM = mmap.mmap(-1, 8*_MAX_PROCESSES + _MAX_PROCESSES, "WEAVE_SHARED_IPC", access=mmap.ACCESS_WRITE)
    LIB = ctypes.CDLL("./lib/shared_map.dll")

    pid_signal_idx = pid
    signal_offset = MUTEX_SIZE * _MAX_PROCESSES
    print("Have signal offset", flush=True)

    PROGRAM_SIGNAL_IDX = signal_offset + PID

    LIB.python_mutex_lock.argtypes = [ctypes.c_void_p]
    LIB.python_mutex_lock.argtypes = [ctypes.c_void_p]

    LIB.python_mutex_lock.restype = None
    LIB.python_mutex_release.restype = None

    print(f"Attempting to lock mutex for {PID}", flush=True)
    LIB.python_mutex_lock(__WEAVE_PID_TO_MUTEX(PID))
    WEAVE_IPCMEM[PROGRAM_SIGNAL_IDX] = 1
    print(f"LOCKED MUTEX for {PID}", flush=True)

# Must occur after every single function call/block
def __WEAVE_WAIT_TILL_SCHEDULED():
    LIB.python_mutex_release(__WEAVE_PID_TO_MUTEX(PID))
    print(f"{PID} We wait till scheduled", flush=True)
    while (WEAVE_IPCMEM[PROGRAM_SIGNAL_IDX] == 1):
        continue

    LIB.python_mutex_lock(__WEAVE_PID_TO_MUTEX(PID))
    print(f"{PID} WE are reeady", flush=True)
    WEAVE_IPCMEM[PROGRAM_SIGNAL_IDX] = 1


def __WEAVE_PROCESS_END():
    WEAVE_IPCMEM[PROGRAM_SIGNAL_IDX] = 2
    LIB.python_mutex_release(__WEAVE_PID_TO_MUTEX(PID))
    print("FINISHED CLSOSING")
    WEAVE_IPCMEM.close()