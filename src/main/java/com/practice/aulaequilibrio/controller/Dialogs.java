package com.practice.aulaequilibrio.controller;

import com.practice.aulaequilibrio.service.ErrorMessages;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;

import java.util.Optional;

final class Dialogs {

    private Dialogs() {
    }

    static void error(String title, Throwable exception) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(title);
        alert.setContentText(ErrorMessages.forUser(exception));
        alert.showAndWait();
    }

    static void info(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    static boolean confirm(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION, message, ButtonType.OK, ButtonType.CANCEL);
        alert.setTitle(title);
        alert.setHeaderText(title);
        Optional<ButtonType> answer = alert.showAndWait();
        return answer.isPresent() && answer.get() == ButtonType.OK;
    }
}
