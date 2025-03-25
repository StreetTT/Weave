@echo off

echo "Building Sources"
IF "%1" == "C" (
        javac -h %SOURCE_PATH% %SOURCE_PATH%\SharedMemory.java
        cl /Fe%OUT_PATH%\ /Fo%OUT_PATH%\ /Fd%OUT_PATH%\ /I"C:\Program Files\jdk-21.0.6\include" /I"C:\Program Files\jdk-21.0.6\include\win32" /LD %SOURCE_PATH%\shared_map.c Kernel32.lib
        copy /Y %OUT_PATH%\shared_map.dll .\lib\shared_map.dll
)

set java_module_paths=".\lib\win64\javafx-sdk-21.0.6\lib"
set java_modules=javafx.base,javafx.controls,javafx.fxml
set SOURCE_PATH=.\src
set OUT_PATH=.\build

call javac --module-path %java_module_paths% --add-modules %java_modules% %SOURCE_PATH%\*.java -d %OUT_PATH%

IF NOT %ERRORLEVEL% == 0 (
    echo "Error in Build Aborting..."
    goto :end
)
echo "Build Done"

IF "%1" == "run" (
    call java -cp %OUT_PATH% --module-path %java_module_paths% --add-modules %java_modules% Frontend
)

:end
