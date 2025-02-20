import ctypes
import mmap
import os

WEAVE_IPCMEM = mmap.mmap(-1, 8*255 + 255, "WEAVE_SHARED_IPC", access=mmap.ACCESS_WRITE)
lib = ctypes.CDLL("./lib/shared_map.dll")
program_mutex = ctypes.c_void_p(int.from_bytes(WEAVE_IPCMEM[8:8*2], "little"))
program_signal_idx = 8*255 + 1

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



def func_1():
    print(f'p2 func1')

def func_2():
    print(f'p2 func2')

func_1()
wait_till_scheduled()
func_2()
lib.python_mutex_release(program_mutex)
WEAVE_IPCMEM.close()