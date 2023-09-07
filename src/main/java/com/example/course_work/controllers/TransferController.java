package com.example.course_work.controllers;

import com.example.course_work.client.Client;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.TextField;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.File;
import java.nio.file.Paths;

public class TransferController {
    @FXML
    Button loadButton;
    @FXML
    Button downloadButton;
    @FXML
    TextField fileTextField;
    @FXML
    TextField dirTextField;
    @FXML
    TextField fileNameTextField;

    Client client = null;

//    @FXML
//    public void initialize() {
//    }
    public void initData(Client client)
    {
        this.client = client;
    }

    @FXML
    protected void onSearchDirButtonClick()
    {
        DirectoryChooser directoryChooser = new DirectoryChooser();
        File selectedDirectory = directoryChooser.showDialog((Stage) downloadButton.getScene().getWindow());

        if(selectedDirectory == null){
            //No Directory selected
        }else{
            dirTextField.setText(selectedDirectory.getAbsolutePath());
        }
    }

    @FXML
    protected void onSearchFileButtonClick()
    {
        FileChooser fileChooser = new FileChooser();
        File selectedFile = fileChooser.showOpenDialog((Stage) downloadButton.getScene().getWindow());

        if(selectedFile == null){
            //No Directory selected
        }
        else
        {
            fileTextField.setText(selectedFile.getAbsolutePath());
        }
    }

    @FXML
    protected void onLoadButtonClick()
    {
        client.loadFile(Paths.get(fileTextField.getText()));
    }

    @FXML
    protected void onDownloadButoonClick()
    {
        client.downloadFile(Paths.get(dirTextField.getText()), fileNameTextField.getText());
    }
}
