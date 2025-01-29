import java.awt.Frame;
import java.awt.GridLayout;
import java.awt.Panel;
import java.awt.BorderLayout;
import java.awt.Button;
import java.awt.Checkbox;
import java.awt.Color;
import java.awt.Point;
import java.awt.ScrollPane;
import java.awt.event.*;
import java.awt.Scrollbar;

class Main {
        public static void main(String[] args) {
                Frame frame = new Frame("WEAVE");
                frame.setSize(1280, 720);
                frame.setBackground(Color.MAGENTA);

                frame.addWindowListener(new WindowAdapter() {
                        public void windowClosing(WindowEvent e) {
                                System.exit(0);
                        }
                });

                frame.setVisible(true);
        }
}
