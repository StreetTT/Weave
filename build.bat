IF "%1" == "C" (
        javac -h ./ SharedMemory.java
        cl /Febuild\ /Fobuild\ /Fdbuild\ /I"C:\Program Files\jdk-23.0.2\include" /I"C:\Program Files\jdk-23.0.2\include\win32" /LD shared_map.c Kernel32.lib
)

copy /Y build\shared_map.dll lib\shared_map.dll
javac SharedMemory.java -d build
