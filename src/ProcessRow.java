import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.*;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;

import com.jfoenix.controls.*;
import javafx.geometry.*;


public class ProcessRow extends HBox {
    public JFXButton addBlockButton;
    public JFXButton deleteButton;
    public JFXButton runButton;
    public JFXButton selectButton;
    public boolean selected = true;
    public TextField processName;
    private Runnable onBlockAddedCallback;
    VBox roundedContainer;
    WeaveProcess process;
    GridPaneRow gridPaneRow;
    //TODO: make the row dynamically change size when resized


    public GridPaneRow getGridPaneRow() {
        return gridPaneRow;
    }

    public void setOnBlockAdded(Runnable callback) {
        this.onBlockAddedCallback = callback;
    }

    public ProcessRow(WeaveProcess p) {
        process = p;
        //styling for row
        this.setPadding(new Insets(10));
        this.setAlignment(Pos.CENTER_LEFT);

        //coinatiner for a rounded rectangle look
        roundedContainer = new VBox();
        roundedContainer.getStyleClass().add("process-row-container");
        roundedContainer.setSpacing(10);
        roundedContainer.setPadding(new Insets(15));
        roundedContainer.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(roundedContainer, Priority.ALWAYS);




        //Top bar with delete and run button
        AnchorPane topBar = new AnchorPane();
        topBar.setPrefHeight(30);

        //del
        this.deleteButton = new JFXButton("X");
        this.deleteButton.getStyleClass().add("process-button");
        deleteButton.setRotate(90);
        deleteButton.setTranslateY(-15);
        AnchorPane.setLeftAnchor(deleteButton, 10.0);


        this.runButton = new JFXButton("▶");
        this.runButton.getStyleClass().add("process-button");
        this.runButton.setTooltip(new Tooltip("Run this process"));
        runButton.setRotate(90);
        runButton.setTranslateY(-15);
        AnchorPane.setLeftAnchor(runButton, 50.0);


        //run button
        this.selectButton = new JFXButton("");
        selectButton.setRotate(90);
        selectButton.setTranslateY(-15);
        AnchorPane.setLeftAnchor(selectButton, 90.0);

        //add block button
        this.addBlockButton = new JFXButton("+");
        this.addBlockButton.getStyleClass().add("process-button");
        addBlockButton.setRotate(90);
        addBlockButton.setTooltip(new Tooltip("Add a new block to this process"));
        addBlockButton.setTranslateY(-15);
        AnchorPane.setLeftAnchor(addBlockButton, 130.0);

        //click handler that adds a new block
        addBlockButton.setOnAction(e -> {
            gridPaneRow.addNewBlock();
            if (onBlockAddedCallback != null) {
                Platform.runLater(() -> {
                    onBlockAddedCallback.run();
                });
            }
        });
        topBar.getChildren().addAll(deleteButton, runButton, selectButton, addBlockButton);

        // CONTENT BOX AREA

        //showing name of proces
        processName = new TextField(this.process.name);
        processName.getStyleClass().add("process-name-field");
        processName.setTooltip(new Tooltip("Enter process name"));
        processName.setPromptText("Process Name");

        processName.textProperty().addListener((obs, old, newVal) -> {
            process.name = newVal;
        });
        processName.setMinWidth(200);

        //set init style
        handleSelect();

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

    public void setStatus(byte status){
        final int PROCESS_FINISHED = 2;
        final int PROCESS_ERROR = 3;

        // Remove previous status styles first
        processName.getStyleClass().removeAll("process-name-field-success", "process-name-field-error");
        // Ensure base style is always present
        if (!processName.getStyleClass().contains("process-name-field")) {
            processName.getStyleClass().add("process-name-field");
        }


        switch (status) {
            case PROCESS_FINISHED:
                // Apply success style class
                processName.getStyleClass().add("process-name-field-success");
                break;
            case PROCESS_ERROR:
                // Apply error style class
                processName.getStyleClass().add("process-name-field-error");
                break;
            default:
                // No additional style class needed, base style is sufficient
                break;
        }

    }

    public void handleSelect() {
        selectButton.getStyleClass().removeAll("select-button", "select-button-selected");
        roundedContainer.getStyleClass().removeAll("process-row-container", "process-row-container-selected");

        if (selected) {
            selectButton.getStyleClass().add("select-button");
            selectButton.setText("◌");

            roundedContainer.getStyleClass().add("process-row-container");

            selected = false;
        } else {
            selectButton.getStyleClass().add("select-button-selected");
            selectButton.setText("●"); // Filled circle

            roundedContainer.getStyleClass().add("process-row-container-selected");

            selected = true;
        }
    }


}
