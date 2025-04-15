import mmap
import time
import ctypes
import pickle

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
    WEAVE_IPCMEM = mmap.mmap(-1, 8*_MAX_PROCESSES + _MAX_PROCESSES, "WEAVE_SHARED_IPC", access=mmap.ACCESS_WRITE)
    LIB = ctypes.CDLL("./lib/weave_native.dll")

    pid_signal_idx = pid
    signal_offset = MUTEX_SIZE * _MAX_PROCESSES

    PROGRAM_SIGNAL_IDX = signal_offset + PID

    LIB.python_mutex_lock.argtypes = [ctypes.c_void_p]
    LIB.python_mutex_release.argtypes = [ctypes.c_void_p]

    LIB.python_mutex_lock.restype = None
    LIB.python_mutex_release.restype = None

    LIB.python_create_shared_mapping.argtypes = [ctypes.c_size_t, ctypes.c_size_t, ctypes.c_char_p]
    LIB.python_create_shared_mapping.restype = None

    LIB.python_mutex_lock(__WEAVE_PID_TO_MUTEX(PID))
    WEAVE_IPCMEM[PROGRAM_SIGNAL_IDX] = 1

# Must occur after every single function call/block
def __WEAVE_WAIT_TILL_SCHEDULED():
    LIB.python_mutex_release(__WEAVE_PID_TO_MUTEX(PID))
    while (WEAVE_IPCMEM[PROGRAM_SIGNAL_IDX] == 1):
        continue

    LIB.python_mutex_lock(__WEAVE_PID_TO_MUTEX(PID))
    WEAVE_IPCMEM[PROGRAM_SIGNAL_IDX] = 1


def __WEAVE_PROCESS_END():
    WEAVE_IPCMEM[PROGRAM_SIGNAL_IDX] = 2
    LIB.python_mutex_release(__WEAVE_PID_TO_MUTEX(PID))
    WEAVE_IPCMEM.close()

def get_shared_array(cls, n_elements, name):
    instance = cls()
    serialised = pickle.dumps(instance)
    size_in_bytes = len(serialised)

    LIB.python_create_shared_mapping(size_in_bytes, n_elements, ctypes.c_char_p(name));
    return mmap.mmap(-1, size_in_bytes * n_elements, name, access=mmap.ACCESS_WRITE)

def get_shared_safe_array(cls, n_elements, name):
    instance = cls()
    serialised = pickle.dumps(instance)
    size_in_bytes = len(serialised);

    LIB.python_create_safe_shared_mapping(size_in_bytes, n_elements, ctypes.c_char_p(name));
