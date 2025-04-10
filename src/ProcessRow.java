import javafx.geometry.Insets;
import javafx.scene.layout.*;
import javafx.scene.control.Label;
import com.jfoenix.controls.*;
import javafx.geometry.*;

import javax.lang.model.AnnotatedConstruct;
import javax.swing.*;

public class ProcessRow extends HBox {

    public JFXButton deleteButton;

    //TODO: make the row dynamically change size when resized
    public ProcessRow(WeaveProcess process) {
        //styling for row
        this.setPadding(new Insets(10));
        this.setAlignment(Pos.CENTER_LEFT);
        this.setSpacing(50);

        //coinatiner for a rounded rectangle look
        VBox roundedContainer = new VBox();
        roundedContainer.setSpacing(10);
        roundedContainer.setPadding(new Insets(15));
        roundedContainer.setStyle(
                "-fx-background-color: #ffffff;" +
                        "-fx-background-radius: 15px;" +
                        "-fx-border-radius: 15px;" +
                        "-fx-border-color: #cccccc;" +
                        "-fx-border-width: 1px;"
        );
        roundedContainer.setMaxWidth(Double.MAX_VALUE);


        //Top bar with delete and (TODO: MAkE Run button)
        AnchorPane topBar = new AnchorPane();
        topBar.setPrefHeight(30);

        this.deleteButton = new JFXButton("X");
        deleteButton.setRotate(90);
        deleteButton.setStyle(
                "-fx-background-color: #e57373;" +
                        "-fx-text-fill: white;" +
                        "-fx-background-radius: 0 15 15 0;" +
                        "-fx-padding: 4 10 4 10;"
        );

        deleteButton.setTranslateY(-15);
        AnchorPane.setLeftAnchor(deleteButton, 10.0);

        topBar.getChildren().add(deleteButton);

        //showing name of process (TODO: link name to procces)
        Label processName = new Label("EXAMPLE PROCESS NAME");
        processName.setStyle(
                "-fx-background-color: #eeeeee;" +
                        "-fx-padding: 10;" +
                        "-fx-font-size: 14px;" +
                        "-fx-background-radius: 5;"
        );
        processName.setPrefWidth(200);

        //row of processes inside the rectangle
        GridPaneRow newRow = new GridPaneRow(process);

        HBox blockWrapper = new HBox(newRow);
        blockWrapper.setAlignment(Pos.CENTER_LEFT);
        blockWrapper.setPadding(new Insets(0, 0, 0, 0 ));

        //put all of the objects together
        roundedContainer.getChildren().addAll(topBar, processName, blockWrapper);
        this.getChildren().add(roundedContainer);
    }
}
