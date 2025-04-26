import org.fxmisc.richtext.CodeArea;
import org.fxmisc.richtext.LineNumberFactory;
import org.fxmisc.richtext.model.StyleSpans;
import org.fxmisc.richtext.model.StyleSpansBuilder;
import javafx.scene.Scene;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.WindowEvent;

import java.awt.*;
import java.time.Duration;
import java.util.Collection;
import java.util.Collections;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


//create a popup window for editing python code with syntax highlighting
public class PopupEditor {

    //tracks if the window is open
    public boolean showing;
    //the javafx stage
    public Stage currStage;
    //the data the block is being editied
    private Block block;
    //the text area where code is typed
    private CodeArea codeArea;
    //store of last position and size of the editor window
    private double lastX = -1;
    private double lastY = -1;
    private double lastWidth = 800;
    private double lastHeight = 600;

    //list of python keywords for highlighting
    private static final String[] KEYWORDS = new String[] {
        "False", "None", "True", "and", "as", "assert", "async", 
        "await", "break", "class", "continue", "def", "del", "elif", 
        "else", "except", "finally", "for", "from", "global", "if", 
        "import", "in", "is", "lambda", "nonlocal", "not", "or", "pass", 
        "raise", "return", "try", "while", "with", "yield"
    };

    //list of python built in functions for highlighting
    private static final String[] BUILTIN_FUNCTIONS = new String[] {
        "abs", "all", "any", "ascii", "bin", "bool", "bytearray", "bytes",
        "chr", "classmethod", "compile", "complex", "dict", "dir", "divmod",
        "enumerate", "eval", "exec", "filter", "float", "format", "frozenset",
        "getattr", "globals", "hasattr", "hash", "help", "hex", "id", "input",
        "int", "isinstance", "issubclass", "iter", "len", "list", "locals", "map",
        "max", "min", "next", "object", "oct", "open", "ord", "pow", "print",
        "property", "range", "repr", "reversed", "round", "set", "setattr",
        "slice", "sorted", "staticmethod", "str", "sum", "super", "tuple",
        "type", "vars", "zip"
    };


    //regex patterns for the different python syntax
    private static final String KEYWORD_PATTERN = "\\b(" + String.join("|", KEYWORDS) + ")\\b";
    private static final String BUILTIN_PATTERN = "\\b(" + String.join("|", BUILTIN_FUNCTIONS) + ")\\b";
    private static final String STRING_PATTERN =
        "\"\"\"[^\"\"\"\\\\]*(?:\\\\.[^\"\"\"\\\\]*)*\"\"\"|" +
        "'''[^'''\\\\]*(?:\\\\.[^'''\\\\]*)*'''|" +
        "\"[^\"\\\\]*(?:\\\\.[^\"\\\\]*)*\"|" +
        "'[^'\\\\]*(?:\\\\.[^'\\\\]*)*'";
    private static final String COMMENT_PATTERN = "#[^\n]*";
    private static final String NUMBER_PATTERN = 
        "\\b\\d*\\.\\d+([eE][+-]?\\d+)?[jf]?\\b|" +
        "\\b\\d+[jf]?\\b|" +
        "\\b0[xX][0-9a-fA-F]+\\b|" +
        "\\b0[bB][01]+\\b|" +
        "\\b0[oO][0-7]+\\b";
    private static final String DECORATOR_PATTERN = "@[A-Za-z_][A-Za-z0-9_]*";
    private static final String METHOD_PATTERN = 
        "(?<![A-Za-z0-9_])" +
        "[A-Za-z_][A-Za-z0-9_]*" +
        "\\s*" +
        "(?=\\s*\\([^\\)]*\\))";



    //combines all the patternms into a massive one for matching
    private static final Pattern PATTERN = Pattern.compile(
        "(?<KEYWORD>" + KEYWORD_PATTERN + ")" +
        "|(?<BUILTIN>" + BUILTIN_PATTERN + ")" +
        "|(?<STRING>" + STRING_PATTERN + ")" +
        "|(?<COMMENT>" + COMMENT_PATTERN + ")" +
        "|(?<NUMBER>" + NUMBER_PATTERN + ")" +
        "|(?<DECORATOR>" + DECORATOR_PATTERN + ")" +
        "|(?<METHOD>" + METHOD_PATTERN + ")",
        //allowing the pattern to match acros multiple lines
        Pattern.MULTILINE
    );

    //constructor for popupeditior
    public PopupEditor(Block block) {
        this.showing = false;
        this.block = block;
    }



    //shows the popup editor window
    public void showPopup() {
        //only creates a new window if one inst already showing
        if (!showing) {
            this.showing = true;
            this.currStage = new Stage();
            currStage.setTitle("Python Editor");
            //prevents interaction with main window why text editor is open
            currStage.initModality(Modality.NONE);

            //creates the special area for a text box
            this.codeArea = new CodeArea();
            this.codeArea.getStyleClass().add("code-area");
            //adds line numbers to the left side of textbox
            this.codeArea.setParagraphGraphicFactory(LineNumberFactory.get(codeArea));
            //loads the code from the block into the editor
            this.codeArea.replaceText(0, 0, this.block.fileContents.toString());



            //setus up the automatic syntax highlighting whenever the text changes
            codeArea.setStyleSpans(0, computeHighlighting(codeArea.getText()));
            codeArea.multiPlainChanges()
                    //waits 30 ms before highlighting (weird stuff if no delay)
                   .successionEnds(Duration.ofMillis(30))
                   .subscribe(ignore -> codeArea.setStyleSpans(0, computeHighlighting(codeArea.getText())));


            //loads the CSS file for stlying
            String cssPath = "/python-keywords.css";
            Scene scene = new Scene(codeArea, lastWidth, lastHeight);
            scene.getStylesheets().add(getClass().getResource(cssPath).toExternalForm());

            currStage.setScene(scene);
            
            //restores the window position if it was opened before
            if (lastX >= 0 && lastY >= 0) {
                currStage.setX(lastX);
                currStage.setY(lastY);
            }

            //saves window position and size when changed
            currStage.xProperty().addListener((obs, oldX, newX) -> lastX = newX.doubleValue());
            currStage.yProperty().addListener((obs, oldY, newY) -> lastY = newY.doubleValue());
            currStage.widthProperty().addListener((obs, oldW, newW) -> lastWidth = newW.doubleValue());
            currStage.heightProperty().addListener((obs, oldH, newH) -> lastHeight = newH.doubleValue());


            //makes sure code is saved when the window is closed
            currStage.setOnCloseRequest(this::saveOnClose);
            //shows the window and waits until it closes
            currStage.showAndWait();
            //tries to give the window some focus
            currStage.requestFocus();
        } else {
            currStage.requestFocus();
        }
    }




    //works out the syntax highlighting for the given text
    private StyleSpans<Collection<String>> computeHighlighting(String text) {
        Matcher matcher = PATTERN.matcher(text);
        int lastKwEnd = 0;

        //builds the list of styles to apply
        StyleSpansBuilder<Collection<String>> spansBuilder = new StyleSpansBuilder<>();


        //loops through all matches found in the pattern
        while (matcher.find()) {
            String styleClass = 
            matcher.group("KEYWORD") != null ? "keyword" :
            matcher.group("BUILTIN") != null ? "builtin" :
            matcher.group("STRING") != null ? "string" :
            matcher.group("COMMENT") != null ? "comment" :
            matcher.group("NUMBER") != null ? "number" :
            matcher.group("DECORATOR") != null ? "decorator" :
            matcher.group("METHOD") != null ? "method" :
            null;

            spansBuilder.add(Collections.singleton("plainText"), matcher.start() - lastKwEnd);
            spansBuilder.add(Collections.singleton(styleClass), matcher.end() - matcher.start());
            lastKwEnd = matcher.end();
        }

        //adds the plain text style for any remaining character after the last match
        spansBuilder.add(Collections.singleton("plainText"), text.length() - lastKwEnd);
        
        return spansBuilder.create();
    }


    //saves the code from the editor back to the blcok when the window closes
    public void saveOnClose(WindowEvent event) {

        //clears the orginal block content, appends the edited text and marks the editor not to be shown.
        this.block.fileContents.setLength(0);
        this.block.fileContents.append(this.codeArea.getText());
        this.showing = false;
    }
}
