@echo off
IF "%1" == "C" (
        javac -h ./ SharedMemory.java
        cl /Febuild\ /Fobuild\ /Fdbuild\ /I"C:\Program Files\jdk-21.0.6\include" /I"C:\Program Files\jdk-21.0.6\include\win32" /LD shared_map.c Kernel32.lib
        copy /Y .\build\shared_map.dll .\lib\shared_map.dll
)

set java_module_paths=".\lib\win64\javafx-sdk-21.0.6\lib"
set java_modules=javafx.base,javafx.controls,javafx.fxml


call javac --module-path %java_module_paths% --add-modules %java_modules% *.java -d build

IF NOT %ERRORLEVEL% == 0 (
    echo "Error in Build Aborting..."
    goto :end
)

IF "%1" == "run" (
    call java -cp .\build --module-path %java_module_paths% --add-modules %java_modules% Frontend
)

:end
echo "Finished"
