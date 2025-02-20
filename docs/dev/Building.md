# Build Instructions

- run `build.bat` inside project directory to build the project java files
- `build.bat C` will rebuild the C modules (not needed unless you modify the C code)
- `build.bat run` will build the application and run it 

## Using InteliJ
if you want to debug in InteliJ. Open the Project in IntelliJ Goto:

Run/Debug Configurations -> Modify Options -> Click Add VM Options and paste in the following

`--module-path ".\lib\javafx-sdk-21.0.6\lib" --add-modules javafx.base,javafx.controls,javafx.fxml`

## Using Vscode
if you want to use VSCode. Open The Project navtigate to or create `.vscode/launch.json`
paste the following into the file
```
{
    "version": "0.2.0",
    "configurations": [
        {
            "type": "java",
            "name": "Frontend",
            "request": "launch",
            "mainClass": "Frontend",
            "projectName": "Weave_24f94098",
            "vmArgs": "--module-path \".\\lib\\javafx-sdk-21.0.6\\lib\" --add-modules javafx.base,javafx.controls,javafx.fxml",
        },

        {
            "type": "java",
            "name": "Current File",
            "request": "launch",
            "mainClass": "${file}",
        },
    ]
}
```