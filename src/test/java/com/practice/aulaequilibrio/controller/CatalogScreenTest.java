package com.practice.aulaequilibrio.controller;

import com.practice.aulaequilibrio.dao.CatalogDAO;
import com.practice.aulaequilibrio.dao.CatalogDAOTest;
import com.practice.aulaequilibrio.model.Availability;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.stage.Stage;
import javafx.stage.Window;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.concurrent.Callable;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

class CatalogScreenTest {
    private Parent root;

    @Test void userCanCreateFilterEditDeleteAndSeeReferenceWarning() throws Exception {
        assumeTrue(System.getenv("DISPLAY") != null, "La prueba visual requiere un servidor gráfico.");
        try { Platform.startup(() -> Platform.setImplicitExit(false)); }
        catch (IllegalStateException alreadyStarted) { /* JavaFX ya está inicializado. */ }
        try (var factory = CatalogDAOTest.createFactory("screen_" + System.nanoTime())) {
            var dao = new CatalogDAO(factory);
            Stage stage = fx(() -> {
                FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/practice/aulaequilibrio/views/home-view.fxml"));
                loader.setControllerFactory(type -> new HomeController(dao));
                root = loader.load();
                Stage window = new Stage();
                window.setScene(new Scene(root, 1040, 740));
                window.show();
                return window;
            });
            try {
                idle();
                teacher("Ana Pérez");
                teacher("Luis López");
                block(0, 0, "2");
                block(1, 1, "6");
                fx(() -> { combo("filterTeacher").getSelectionModel().select(0); click("Consultar"); return null; });
                idle();
                assertEquals(1, fx(() -> table("blocks").getItems().size()));
                Availability shown = fx(() -> (Availability) table("blocks").getItems().getFirst());
                assertEquals("Ana Pérez", shown.getTeacher().getName());
                fx(() -> {
                    table("blocks").getSelectionModel().select(0);
                    field("blockNumber").setText("9");
                    combo("day").getSelectionModel().select(4);
                    click("Guardar bloque"); return null;
                });
                idle();
                assertEquals(9, fx(() -> ((Availability) table("blocks").getItems().getFirst()).getBlockNumber()));
                fx(() -> {
                    table("teachers").getSelectionModel().select(0);
                    approveNextDialog();
                    click("Eliminar docente"); return null;
                });
                String warning = waitForWarning();
                assertTrue(warning.contains("bloques asociados"));
                assertEquals(2, dao.listTeachers().size());
                fx(() -> {
                    table("blocks").getSelectionModel().select(0);
                    approveNextDialog();
                    click("Eliminar bloque"); return null;
                });
                idle();
                assertEquals(0, fx(() -> table("blocks").getItems().size()));
                fx(() -> { click("Ver todos"); return null; });
                idle();
                assertEquals(1, fx(() -> table("blocks").getItems().size()));
                assertEquals("Luis López", fx(() -> ((Availability) table("blocks").getItems().getFirst()).getTeacher().getName()));
            } finally { fx(() -> { stage.close(); return null; }); }
        }
    }

    private void teacher(String name) throws Exception {
        fx(() -> { field("name").setText(name); click("Guardar docente"); return null; });
        idle();
    }
    private void block(int teacher, int day, String number) throws Exception {
        fx(() -> {
            combo("blockTeacher").getSelectionModel().select(teacher);
            combo("day").getSelectionModel().select(day);
            field("blockNumber").setText(number);
            click("Guardar bloque"); return null;
        });
        idle();
    }
    private String waitForWarning() throws Exception {
        for (int i = 0; i < 200; i++) {
            String text = fx(() -> {
                for (Window window : new ArrayList<>(Window.getWindows())) {
                    if (window.getScene().getRoot() instanceof DialogPane pane) {
                        String content = pane.getContentText();
                        ((Button) pane.lookupButton(ButtonType.OK)).fire();
                        return content;
                    }
                }
                return null;
            });
            if (text != null) return text;
            Thread.sleep(25);
        }
        throw new AssertionError("No apareció la advertencia de docente referenciado.");
    }
    private void approveNextDialog() {
        Platform.runLater(() -> {
            for (Window window : new ArrayList<>(Window.getWindows()))
                if (window.getScene().getRoot() instanceof DialogPane pane)
                    ((Button) pane.lookupButton(ButtonType.OK)).fire();
        });
    }
    private void idle() throws Exception {
        for (int i = 0; i < 400; i++) {
            if (fx(() -> !root.lookup("#catalogs").isDisabled())) return;
            Thread.sleep(25);
        }
        throw new AssertionError("La pantalla no terminó la operación.");
    }
    private void click(String text) {
        root.lookupAll(".button").stream().filter(n -> n instanceof Button b && text.equals(b.getText()))
                .map(n -> (Button) n).findFirst().orElseThrow().fire();
    }
    private TextField field(String id) { return (TextField) root.lookup("#" + id); }
    private ComboBox<?> combo(String id) { return (ComboBox<?>) root.lookup("#" + id); }
    private TableView<?> table(String id) { return (TableView<?>) root.lookup("#" + id); }
    private <T> T fx(Callable<T> action) throws Exception {
        FutureTask<T> task = new FutureTask<>(action);
        Platform.runLater(task);
        return task.get(15, TimeUnit.SECONDS);
    }
}
