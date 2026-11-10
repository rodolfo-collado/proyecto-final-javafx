package com.practice.aulaequilibrio.controller;

import com.practice.aulaequilibrio.view.ScheduleView;
import javafx.fxml.FXML;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.stage.Modality;
import javafx.stage.Stage;

public class HomeController {
    @FXML
    private Label welcomeMessage;

    @FXML
    protected void showWelcomeMessage() {
        welcomeMessage.setText("Bienvenido a Aula Equilibrio");
    }

    @FXML
    private void openSchedules() {
        ScheduleView schedules = new ScheduleView();
        Stage window = new Stage();
        window.initOwner(welcomeMessage.getScene().getWindow());
        window.initModality(Modality.WINDOW_MODAL);
        window.setTitle("Aula Equilibrio · Horarios");
        window.setScene(new Scene(schedules, 900, 540));
        window.setMinWidth(840);
        schedules.load();
        window.show();
    }
}
