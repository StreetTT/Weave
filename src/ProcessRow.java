import javafx.scene.control.Tooltip;
import javafx.scene.layout.*;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;

import com.jfoenix.controls.*;
import javafx.geometry.*;

import javax.lang.model.AnnotatedConstruct;
import javax.swing.*;

public class ProcessRow extends HBox {

    public JFXButton deleteButton;
    public JFXButton runButton;
    public JFXButton selectButton;
    public boolean selected = true;
    VBox roundedContainer;
    WeaveProcess process;
    GridPaneRow gridPaneRow;
    //TODO: make the row dynamically change size when resized

    public ProcessRow(WeaveProcess p) {
        process = p;
        //styling for row
        this.setPadding(new Insets(10));
        this.setAlignment(Pos.CENTER_LEFT);

        //coinatiner for a rounded rectangle look
        roundedContainer = new VBox();
        roundedContainer.setSpacing(10);
        roundedContainer.setPadding(new Insets(15));
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
        this.runButton.setTooltip(new Tooltip("Run this process"));
        runButton.setRotate(90);
        runButton.setStyle(
                "-fx-background-color: #81c784;" +
                        "-fx-text-fill: white;" +
                        "-fx-background-radius: 0 15 15 0;" +
                        "-fx-padding: 4 10 4 10;"
        );

        runButton.setTranslateY(-15);
        AnchorPane.setLeftAnchor(runButton, 50.0);
        
        this.selectButton = new JFXButton("");
        selectButton.setRotate(90);
        handleSelect();

        selectButton.setTranslateY(-15);
        AnchorPane.setLeftAnchor(selectButton, 90.0);

        topBar.getChildren().addAll(deleteButton, runButton, selectButton);

        // CONTENT BOX AREA

        //showing name of proces(TODO: link name to procces)
        TextField processName = new TextField("");
        processName.setTooltip(new Tooltip("Enter process name"));
        processName.setPromptText("Process Name");
        processName.setStyle(
                "-fx-background-color: #eeeeee;" +
                        "-fx-padding: 10;" +
                        "-fx-font-size: 14px;" +
                        "-fx-background-radius: 5;"
        );
        processName.textProperty().addListener((obs, old, newVal) -> {
                process.name = newVal;
            });
        processName.setMinWidth(200);
        processName.setOnKeyPressed(event -> {
            if (event.getCode() == KeyCode.ENTER) {
                processName.getParent().requestFocus();
            }
        });

        //row of processes inside the rectangle
        this.gridPaneRow = new GridPaneRow(process);

        HBox blockWrapper = new HBox(this.gridPaneRow);
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

    public void handleSelect() {
        if (selected) {
            selectButton.setStyle(
                "-fx-background-color:rgb(45, 47, 45);" +
                "-fx-text-fill: white;" +
                "-fx-background-radius: 0 15 15 0;" +
                "-fx-padding: 4 10 4 10;" + 
                "-fx-font-size: 1em;"
            );
            selectButton.setText("◌");
            roundedContainer.setStyle(
                "-fx-background-color: #ffffff;" +
                "-fx-background-radius: 15px;" +
                "-fx-border-radius: 15px;" +
                "-fx-border-color: #cccccc;" +
                "-fx-border-width: 1px;"
            );
            selected = false;
        } else {
            selectButton.setStyle(
                "-fx-background-color:rgb(132, 215, 253);" +
                "-fx-text-fill: black;" +
                "-fx-background-radius: 0 15 15 0;" +
                "-fx-padding: 4 10 4 10;" 
            );
            selectButton.setText("●");
            roundedContainer.setStyle(
                "-fx-background-color:#394346;" +
                "-fx-background-radius: 15px;" +
                "-fx-border-radius: 15px;" +
                "-fx-border-color:rgb(0, 0, 0);" +
                "-fx-border-width: 1px;"
            );
            selected = true; 
        }
    }
}
