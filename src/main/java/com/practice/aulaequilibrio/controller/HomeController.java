package com.practice.aulaequilibrio.controller;

import javafx.fxml.FXML;
import javafx.scene.control.Label;

public class HomeController {
    @FXML
    private Label welcomeMessage;

    @FXML
    protected void showWelcomeMessage() {
        welcomeMessage.setText("Bienvenido a Aula Equilibrio");
    }
}
