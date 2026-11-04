module com.practice.proyectofinaljavafx {
    requires javafx.controls;
    requires javafx.fxml;


    opens com.practice.proyectofinaljavafx to javafx.fxml;
    exports com.practice.proyectofinaljavafx;
}