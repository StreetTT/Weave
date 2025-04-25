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

public class PopupEditor {
    public boolean showing;
    public Stage currStage;
    private Block block;
    private CodeArea codeArea;
    private double lastX = -1;
    private double lastY = -1;
    private double lastWidth = 800;
    private double lastHeight = 600;

    private static final String[] KEYWORDS = new String[] {
        "False", "None", "True", "and", "as", "assert", "async", 
        "await", "break", "class", "continue", "def", "del", "elif", 
        "else", "except", "finally", "for", "from", "global", "if", 
        "import", "in", "is", "lambda", "nonlocal", "not", "or", "pass", 
        "raise", "return", "try", "while", "with", "yield"
    };

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

    private static final String KEYWORD_PATTERN = "\\b(" + String.join("|", KEYWORDS) + ")\\b";
    private static final String BUILTIN_PATTERN = "\\b(" + String.join("|", BUILTIN_FUNCTIONS) + ")\\b";
    private static final String STRING_PATTERN = 
        "\"\"\"[^\"\"\"\\\\]*(?:\\\\.[^\"\"\"\\\\]*)*\"\"\"|" + // Triple quoted strings
        "'''[^'''\\\\]*(?:\\\\.[^'''\\\\]*)*'''|" +           // Triple quoted strings
        "\"[^\"\\\\]*(?:\\\\.[^\"\\\\]*)*\"|" +              // Double quoted strings
        "'[^'\\\\]*(?:\\\\.[^'\\\\]*)*'";                    // Single quoted strings
    private static final String COMMENT_PATTERN = "#[^\n]*";
    private static final String NUMBER_PATTERN = 
        "\\b\\d*\\.\\d+([eE][+-]?\\d+)?[jf]?\\b|" +         // Decimals like 5.0, 5.5e-10
        "\\b\\d+[jf]?\\b|" +                                 // Integers like 5, 5j
        "\\b0[xX][0-9a-fA-F]+\\b|" +                        // Hex like 0xFF
        "\\b0[bB][01]+\\b|" +                               // Binary like 0b1010
        "\\b0[oO][0-7]+\\b";                                // Octal like 0o777
    private static final String DECORATOR_PATTERN = "@[A-Za-z_][A-Za-z0-9_]*";
    private static final String METHOD_PATTERN = 
        "(?<![A-Za-z0-9_])" +                    // Negative lookbehind for word chars
        "[A-Za-z_][A-Za-z0-9_]*" +              // Method name
        "\\s*" +                                 // Optional whitespace
        "(?=\\s*\\([^\\)]*\\))";                // Positive lookahead for parentheses and args
    
    private static final Pattern PATTERN = Pattern.compile(
        "(?<KEYWORD>" + KEYWORD_PATTERN + ")" +
        "|(?<BUILTIN>" + BUILTIN_PATTERN + ")" +
        "|(?<STRING>" + STRING_PATTERN + ")" +
        "|(?<COMMENT>" + COMMENT_PATTERN + ")" +
        "|(?<NUMBER>" + NUMBER_PATTERN + ")" +
        "|(?<DECORATOR>" + DECORATOR_PATTERN + ")" +
        "|(?<METHOD>" + METHOD_PATTERN + ")",
        Pattern.MULTILINE
    );

    public PopupEditor(Block block) {
        this.showing = false;
        this.block = block;
    }

    public void showPopup() {
        if (!showing) {
            this.showing = true;
            this.currStage = new Stage();
            currStage.setTitle("Python Editor");
            currStage.initModality(Modality.NONE);

            // Create the CodeArea instead of TextArea
            this.codeArea = new CodeArea();
            this.codeArea.getStyleClass().add("code-area");
            this.codeArea.setParagraphGraphicFactory(LineNumberFactory.get(codeArea));
            this.codeArea.replaceText(0, 0, this.block.fileContents.toString());


            codeArea.setStyleSpans(0, computeHighlighting(codeArea.getText()));
            codeArea.multiPlainChanges()
                   .successionEnds(Duration.ofMillis(30)) // 30ms delay
                   .subscribe(ignore -> codeArea.setStyleSpans(0, computeHighlighting(codeArea.getText())));

            String cssPath = "/python-keywords.css";
            Scene scene = new Scene(codeArea, lastWidth, lastHeight);
            scene.getStylesheets().add(getClass().getResource(cssPath).toExternalForm());

            currStage.setScene(scene);
            
            // Set the position if we have previous values
            if (lastX >= 0 && lastY >= 0) {
                currStage.setX(lastX);
                currStage.setY(lastY);
            }

            // Add listeners to save window position and size
            currStage.xProperty().addListener((obs, oldX, newX) -> lastX = newX.doubleValue());
            currStage.yProperty().addListener((obs, oldY, newY) -> lastY = newY.doubleValue());
            currStage.widthProperty().addListener((obs, oldW, newW) -> lastWidth = newW.doubleValue());
            currStage.heightProperty().addListener((obs, oldH, newH) -> lastHeight = newH.doubleValue());

            currStage.setOnCloseRequest(this::saveOnClose);
            currStage.showAndWait();
            currStage.requestFocus();
        } else {
            currStage.requestFocus();
        }
    }

    private StyleSpans<Collection<String>> computeHighlighting(String text) {
        Matcher matcher = PATTERN.matcher(text);
        int lastKwEnd = 0;
        StyleSpansBuilder<Collection<String>> spansBuilder = new StyleSpansBuilder<>();

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
        spansBuilder.add(Collections.singleton("plainText"), text.length() - lastKwEnd);
        
        return spansBuilder.create();
    }

    public void saveOnClose(WindowEvent event) {
        this.block.fileContents.setLength(0);
        this.block.fileContents.append(this.codeArea.getText());
        this.showing = false;
    }
}
