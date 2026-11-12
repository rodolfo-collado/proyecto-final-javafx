package com.practice.aulaequilibrio.util;

import com.practice.aulaequilibrio.view.mdi.MdiDesktop;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.DialogPane;
import javafx.scene.layout.Region;

import java.util.function.BiConsumer;

public final class Dialogs {
    private Dialogs() {
    }

    public static void error(Node source, String title, Throwable exception) {
        error(source, title, ErrorMessages.forUser(exception));
    }

    public static void error(Node source, String title, String message) {
        showMessage(source, title, message, "mdi-error");
    }

    public static void warning(Node source, String message) {
        showMessage(source, "Revisa la operación", message, "mdi-warning");
    }

    public static void info(Node source, String title, String message) {
        showMessage(source, title, message, "mdi-info");
    }

    public static void confirm(Node source, String title, String message, Runnable onConfirm) {
        MdiDesktop desktop = desktopOf(source);
        if (desktop == null) {
            return;
        }
        DialogPane pane = pane(title, "mdi-warning", ButtonType.OK, ButtonType.CANCEL);
        pane.setContentText(message);
        button(pane, ButtonType.CANCEL).setText("Cancelar");
        button(pane, ButtonType.CANCEL).setOnAction(event -> desktop.closeDialog(pane));
        Button confirm = button(pane, ButtonType.OK);
        confirm.setText("Confirmar");
        confirm.getStyleClass().add("danger");
        confirm.setOnAction(event -> {
            desktop.closeDialog(pane);
            onConfirm.run();
        });
        desktop.showDialog(pane);
    }

    public static void form(Node source, String title, Node content, BiConsumer<DialogPane, Runnable> onSave) {
        MdiDesktop desktop = desktopOf(source);
        if (desktop == null) {
            return;
        }
        DialogPane pane = pane(title, "mdi-info", ButtonType.OK, ButtonType.CANCEL);
        pane.setContent(content);
        Runnable close = () -> desktop.closeDialog(pane);
        button(pane, ButtonType.CANCEL).setText("Cancelar");
        button(pane, ButtonType.CANCEL).setOnAction(event -> close.run());
        Button save = button(pane, ButtonType.OK);
        save.setText("Guardar");
        save.getStyleClass().add("primary");
        save.setOnAction(event -> onSave.accept(pane, close));
        desktop.showDialog(pane);
    }

    private static void showMessage(Node source, String title, String message, String style) {
        MdiDesktop desktop = desktopOf(source);
        if (desktop == null) {
            return;
        }
        DialogPane pane = pane(title, style, ButtonType.OK);
        pane.setContentText(message);
        Button accept = button(pane, ButtonType.OK);
        accept.setText("Aceptar");
        accept.setCancelButton(true);
        accept.getStyleClass().add("primary");
        accept.setOnAction(event -> desktop.closeDialog(pane));
        desktop.showDialog(pane);
    }

    private static DialogPane pane(String title, String style, ButtonType... buttons) {
        DialogPane pane = new DialogPane();
        pane.setHeaderText(title);
        pane.setPrefWidth(470);
        pane.setMaxSize(Region.USE_PREF_SIZE, Region.USE_PREF_SIZE);
        pane.setFocusTraversable(true);
        pane.getStyleClass().addAll("mdi-dialog", style);
        pane.getButtonTypes().setAll(buttons);
        return pane;
    }

    private static Button button(DialogPane pane, ButtonType type) {
        return (Button) pane.lookupButton(type);
    }

    private static MdiDesktop desktopOf(Node source) {
        for (Node node = source; node != null; node = node.getParent()) {
            if (node instanceof MdiDesktop desktop) {
                return desktop;
            }
        }
        return null;
    }
}
