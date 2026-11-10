package com.practice.aulaequilibrio.controller;

import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;
import javafx.scene.control.Alert;

public class HomeController {
    @FXML
    private Label welcomeMessage;

    @FXML
    private void openClassrooms() {
        try {
            FXMLLoader loader=new FXMLLoader(getClass().getResource("/com/practice/aulaequilibrio/views/classroom-view.fxml"));
            Stage window=new Stage();window.setTitle("AulaEquilibrio · Aulas y tipos");window.setScene(new Scene(loader.load(),1000,650));window.setMinWidth(960);window.setMinHeight(560);window.initOwner(welcomeMessage.getScene().getWindow());window.show();
        } catch(java.io.IOException ex){Alert alert=new Alert(Alert.AlertType.ERROR,"No se pudo abrir el módulo de aulas.");alert.showAndWait();}
    }
    @FXML
    protected void showWelcomeMessage() {
        welcomeMessage.setText("Bienvenido a Aula Equilibrio");
    }
}
