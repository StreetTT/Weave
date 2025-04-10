import javafx.geometry.Insets;
import javafx.scene.layout.*;
import javafx.scene.control.Label;
import com.jfoenix.controls.*;
import javafx.geometry.*;

import javax.lang.model.AnnotatedConstruct;
import javax.swing.*;

public class ProcessRow extends HBox {

    public JFXButton deleteButton;
    public JFXButton runButton;

    //TODO: make the row dynamically change size when resized

    public ProcessRow(WeaveProcess process) {
        //styling for row
        this.setPadding(new Insets(10));
        this.setAlignment(Pos.CENTER_LEFT);

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
        HBox.setHgrow(roundedContainer,Priority.ALWAYS);




        //Top bar with delete and run button
        AnchorPane topBar = new AnchorPane();
        topBar.setPrefHeight(30);

        //TODO: Find a nice icon for button using a lib
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

        //TODO: Functionaility for this buttton lol
        this.runButton = new JFXButton("▶");
        runButton.setRotate(90);
        runButton.setStyle(
                "-fx-background-color: #81c784;" +
                        "-fx-text-fill: white;" +
                        "-fx-background-radius: 0 15 15 0;" +
                        "-fx-padding: 4 10 4 10;"
        );

        runButton.setTranslateY(-15);
        AnchorPane.setLeftAnchor(runButton, 50.0);


        topBar.getChildren().addAll(deleteButton, runButton);

        // CONTENT BOX AREA

        //showing name of proces(TODO: link name to procces)
        Label processName = new Label("EXAMPLE PROCESS NAME");
        processName.setStyle(
                "-fx-background-color: #eeeeee;" +
                        "-fx-padding: 10;" +
                        "-fx-font-size: 14px;" +
                        "-fx-background-radius: 5;"
        );

        processName.setMinWidth(200);

        //row of processes inside the rectangle
        GridPaneRow newRow = new GridPaneRow(process);

        HBox blockWrapper = new HBox(newRow);
        blockWrapper.setAlignment(Pos.CENTER_LEFT);

        HBox contentBox = new HBox();
        contentBox.setSpacing(20);
        contentBox.setAlignment(Pos.CENTER_LEFT);
        contentBox.getChildren().addAll(processName, blockWrapper);

        VBox.setVgrow(contentBox, Priority.ALWAYS);
        //put all of the objects together
        roundedContainer.getChildren().addAll(topBar, contentBox);
        this.getChildren().add(roundedContainer);
    }
}
