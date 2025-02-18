# Shared Map & Platform Interface

## Memory Allocation
`AlocWeaveSharedBuffer()` Call only once at the start of the application do not forget to call 
`FreeWeaveSharedBuffer()` before the application exits 

## Process Creation
`createProcess()` will return a long which is an handle to a process

