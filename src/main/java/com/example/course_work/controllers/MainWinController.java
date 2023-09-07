package com.example.course_work.controllers;

import com.example.course_work.MainApplication;
import com.example.course_work.client.Client;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

import java.io.IOException;
import java.util.Objects;

public class MainWinController {
    @FXML
    private Label statusLabel;
    @FXML
    private Label addressLabel;
    @FXML
    private Button requestButton;
    @FXML
    private TextField hostTextField;
    @FXML
    private TextField portTextField;

    Client client;
    boolean isPressed = false;

    @FXML
    public void initialize() {
        client = new Client("localhost", 8433);
        addressLabel.setText(client.getLocalAddress());
    }

    @FXML
    protected void onRequestButtonClick() throws Exception {
        statusLabel.setText("wait...");
        requestButton.setDisable(true);
//        Task<Void> result = new Task<>() {
//            @Override
//            protected Void call() throws Exception {
//                boolean res = client.requestSession(hostTextField.getText() + ":" + portTextField.getText());
//                if(res) {
//                    return null;
//                }
//                requestButton.setDisable(false);
//                statusLabel.setText("Something wrong. Try again");
//                return null;
//            }
//        };
//        result.setOnSucceeded(event -> loadTransferWindow());
//        new Thread(result).start();
        boolean res = client.requestSession(hostTextField.getText() + ":" + portTextField.getText());
        if(res)
            loadTransferWindow();

    }

    void loadTransferWindow()
    {
        try {
            FXMLLoader loader = new FXMLLoader(Objects.requireNonNull(MainApplication.class.getResource("view/transfer.fxml")));
            Parent root = loader.load();
            TransferController transferController = loader.getController();
            transferController.initData(client);
            Scene scene = new Scene(root);
            Stage stage = (Stage)portTextField.getScene().getWindow();
            stage.setScene(scene);
            stage.show();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}