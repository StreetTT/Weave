IF "%1" == "C" (
        javac -h ./ SharedMemory.java
        cl /Febuild\ /Fobuild\ /Fdbuild\ /I"C:\Program Files\jdk-23.0.2\include" /I"C:\Program Files\jdk-23.0.2\include\win32" /LD shared_map.c Kernel32.lib
        copy /Y build\shared_map.dll lib\shared_map.dll
)

set java_module_paths=".\lib\javafx-sdk-21.0.6\lib"
set java_modules=javafx.base,javafx.controls,javafx.fxml,javafx.graphics,javafx.media,javafx.swing,javafx.web

javac --module-path %java_module_paths% --add-modules %java_modules% Frontend.java -d build 


if "%1" == "run" (
    call java -cp .\build --module-path %java_module_paths% --add-modules %java_modules% Frontend
)
