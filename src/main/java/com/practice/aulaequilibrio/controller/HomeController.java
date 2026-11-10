package com.practice.aulaequilibrio.controller;

import com.practice.aulaequilibrio.view.ScheduleView;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.TabPane;
import javafx.stage.Modality;
import javafx.stage.Stage;

public class HomeController {
    @FXML
    private TabPane navigation;

    @FXML
    private void openCatalog(ActionEvent event) {
        Button shortcut = (Button) event.getSource();
        navigation.getTabs().stream()
                .filter(tab -> shortcut.getUserData().equals(tab.getId()))
                .findFirst()
                .ifPresent(navigation.getSelectionModel()::select);
    }

    @FXML
    private void openSchedules() {
        ScheduleView schedules = new ScheduleView();
        Stage window = new Stage();
        window.initOwner(navigation.getScene().getWindow());
        window.initModality(Modality.WINDOW_MODAL);
        window.setTitle("Aula Equilibrio · Horarios");
        window.setScene(new Scene(schedules, 900, 540));
        window.setMinWidth(840);
        schedules.load();
        window.show();
    }
}
