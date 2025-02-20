import mmap
import ctypes


__CLIB = None
__SHARED_MEM = None
__PROCESS_MUTEX = None
def weave_proess_init(): 
    __SHARED_MEM = mmap.mmap(-1, 8*255 + 255, "WEAVE_SHARED_IPC", access=mmap.ACCESS_WRITE)
    __CLIB = ctypes.CDLL("./lib/shared_map.dll")
    __PID = mmap.mmap
    __PROCESS_MUTEX = ctypes.c_void_p(int.from_bytes(WEAVE_IPCMEM[0:8], "little"))

def get_program_signal():
    program_signal_idx = 8*255 + __PID

lib.python_mutex_lock.argtypes = [ctypes.c_void_p]
lib.python_mutex_release.argtypes = [ctypes.c_void_p]
lib.python_mutex_lock.restype = None
lib.python_mutex_release.restype = None

lib.python_mutex_lock(program_mutex)
WEAVE_IPCMEM[program_signal_idx] = 1



def wait_till_scheduled():
    lib.python_mutex_release(program_mutex)
    while (WEAVE_IPCMEM[program_signal_idx] == 1): 
        continue

    lib.python_mutex_lock(program_mutex)
    WEAVE_IPCMEM[program_signal_idx] = 1