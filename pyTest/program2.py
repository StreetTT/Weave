import weave_shared as __WEAVE
__WEAVE.__WEAVE_PROCESS_START(2)

def func_1():
    print(2, "func1")

def func_2():
    print(2, "func2")

func_1()
__WEAVE.__WEAVE_WAIT_TILL_SCHEDULED()
func_2()
__WEAVE.__WEAVE_PROCESS_END()