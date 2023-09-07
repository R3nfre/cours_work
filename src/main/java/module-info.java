module com.example.course_work {
    requires javafx.controls;
    requires javafx.fxml;
    requires lombok;
    requires com.google.gson;
    requires apache.log4j.extras;


    opens com.example.course_work to javafx.fxml;
//    opens sun.nio.fs to com.google.gson;
    opens com.example.course_work.entity to com.google.gson;
    exports com.example.course_work;
    exports com.example.course_work.controllers;
    exports com.example.course_work.entity;
    exports com.example.course_work.client;
    opens com.example.course_work.controllers to javafx.fxml;
    exports com.example.course_work.encryption;
    opens com.example.course_work.encryption to javafx.fxml;
}