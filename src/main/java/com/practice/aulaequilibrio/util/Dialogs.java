package com.practice.aulaequilibrio.util;

import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.util.Optional;

public final class Dialogs {
    private Dialogs() {
    }

    public static <T> Optional<T> show(Node source, Dialog<T> dialog) {
        if (source.getScene() == null
                || !(source.getScene().getWindow() instanceof Stage owner)
                || !owner.isShowing()) {
            return Optional.empty();
        }

        double width = owner.getWidth();
        double height = owner.getHeight();
        boolean maximized = owner.isMaximized();
        boolean fullScreen = owner.isFullScreen();

        dialog.initOwner(owner);
        dialog.initModality(Modality.WINDOW_MODAL);

        try {
            return dialog.showAndWait();
        } finally {
            if (owner.isShowing()) {
                if (owner.isFullScreen() != fullScreen) {
                    owner.setFullScreen(fullScreen);
                }
                if (owner.isMaximized() != maximized) {
                    owner.setMaximized(maximized);
                }
                if (!maximized && !fullScreen) {
                    if (owner.getWidth() != width) {
                        owner.setWidth(width);
                    }
                    if (owner.getHeight() != height) {
                        owner.setHeight(height);
                    }
                }
            }
        }
    }

    public static void error(Node source, String title, Throwable exception) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(title);
        alert.setContentText(ErrorMessages.forUser(exception));
        show(source, alert);
    }

    public static void info(Node source, String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        show(source, alert);
    }

    public static boolean confirm(Node source, String title, String message) {
        Alert alert = new Alert(
                Alert.AlertType.CONFIRMATION, message, ButtonType.OK, ButtonType.CANCEL);
        alert.setTitle(title);
        alert.setHeaderText(title);
        return show(source, alert).orElse(ButtonType.CANCEL) == ButtonType.OK;
    }
}
