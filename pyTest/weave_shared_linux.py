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
    global PID
    global PROGRAM_SIGNAL_IDX


    pid_signal_idx = pid
    signal_offset = MUTEX_SIZE * _MAX_PROCESSES

    PROGRAM_SIGNAL_IDX = signal_offset + PID

    LIB.python_mutex_lock.argtypes = [ctypes.c_int]
    LIB.python_mutex_lock.argtypes = [ctypes.c_int]

    LIB.python_mutex_lock.restype = None
    LIB.python_mutex_release.restype = None

    LIB.python_mutex_lock(PID)
    WEAVE_IPCMEM[PROGRAM_SIGNAL_IDX] = 1

# Must occur after every single function call/block
def __WEAVE_WAIT_TILL_SCHEDULED():
    LIB.python_mutex_release(PID)
    while (WEAVE_IPCMEM[PROGRAM_SIGNAL_IDX] == 1):
        continue

    LIB.python_mutex_lock(PID)
    WEAVE_IPCMEM[PROGRAM_SIGNAL_IDX] = 1


def __WEAVE_PROCESS_END():
    LIB.python_mutex_release(PID)
    WEAVE_IPCMEM[PROGRAM_SIGNAL_IDX] = 2
    WEAVE_IPCMEM.close()