package com.example.course_work;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;

public class MainApplication extends Application {
    @Override
    public void start(Stage stage) throws IOException {
//        BigInteger num_1 = new BigInteger(31, new Random());
//
//        Pair<NTRUEncrypt.Key, NTRUEncrypt.Key> keys = NTRUEncrypt.generateKey(num_1);
//        BigInteger C = NTRUEncrypt.encrypt(num_1, keys.getKey());
//        System.out.println("ecrypted: " + C);
//        BigInteger res = NTRUEncrypt.decrypt(C, keys.getValue());
//
//        System.out.println("исходное: " + num_1);
//        System.out.println("расшифрованное: " + res);
//        MainServer server = new MainServer("localhost", 8433);
//        Client client = new Client("localhost", 8433);
//        ExecutorService exc = Executors.newFixedThreadPool(2);
//        exc.execute();
//        exc.execute(client::start);

        {
            FXMLLoader fxmlLoader = new FXMLLoader(MainApplication.class.getResource("view/main_window.fxml"));
            Scene scene = new Scene(fxmlLoader.load());
            stage.setTitle("Course work");
            //stage.
            stage.setScene(scene);
            stage.show();
        }
    }

    public static void main(String[] args) {
        launch();
    }
}