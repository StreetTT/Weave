# Proposed Shceduler Interface 
Shceduler keeps an 2 arrays of for the number of blocks 256*1024 of 2 values the Process Id and its value on the time axis
The frontend is responsible for updating values as blocks and processes are reorderedd

# Scheduler Run 
runs all the blocks in correct order by going in order through both the arrays and running the ascociated PIDs in time axis order waiting in between time units such that
