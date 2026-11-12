package com.practice.aulaequilibrio.controller;

import com.practice.aulaequilibrio.util.Dialogs;
import com.practice.aulaequilibrio.view.ScheduleView;
import com.practice.aulaequilibrio.view.mdi.MdiDesktop;
import javafx.collections.ListChangeListener;
import javafx.css.PseudoClass;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ToolBar;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

import java.io.IOException;

public class HomeController {
    private static final System.Logger LOGGER = System.getLogger(HomeController.class.getName());
    private static final String DASHBOARD = "Dashboard";
    private static final PseudoClass MODULE_OPEN = PseudoClass.getPseudoClass("module-open");
    private static final PseudoClass MODULE_ACTIVE = PseudoClass.getPseudoClass("module-active");

    @FXML
    private VBox home;
    @FXML
    private MdiDesktop desktop;
    @FXML
    private ToolBar taskbar;
    @FXML
    private HBox popupTasks;

    @FXML
    private void initialize() {
        taskbar.disableProperty().bind(desktop.dialogShowingProperty());
        desktop.activeTitleProperty().addListener((observable, previous, current) -> updateTaskbar());
        desktop.getChildrenUnmodifiable().addListener((ListChangeListener<Node>) change -> updateTaskbar());
        desktop.open(DASHBOARD, home, 1000, 660, false);
    }

    private void updateTaskbar() {
        String active = desktop.activeTitleProperty().get();
        for (Node item : taskbar.getItems()) {
            if (item instanceof Button button) {
                updateTask(button, active);
            }
        }
        popupTasks.getChildren().removeIf(node -> !desktop.isOpen(((Button) node).getText()));
        for (String title : desktop.openTitles()) {
            boolean inTaskbar = taskbar.getItems().stream()
                    .anyMatch(node -> node instanceof Button button && title.equals(button.getText()));
            if (!inTaskbar && popupTasks.getChildren().stream().noneMatch(node -> title.equals(((Button) node).getText()))) {
                Button task = new Button(title);
                task.setOnAction(event -> desktop.activate(title));
                popupTasks.getChildren().add(task);
            }
        }
        for (Node item : popupTasks.getChildren()) {
            updateTask((Button) item, active);
        }
    }

    private void updateTask(Button button, String active) {
        String title = button.getText();
        button.pseudoClassStateChanged(MODULE_OPEN, desktop.isOpen(title));
        button.pseudoClassStateChanged(MODULE_ACTIVE, title.equals(active));
    }

    @FXML
    private void showHome() {
        desktop.activate(DASHBOARD);
    }

    @FXML
    private void arrangeWindows() {
        desktop.cascade();
    }

    @FXML
    private void openCatalog(ActionEvent event) {
        Button shortcut = (Button) event.getSource();
        String title = shortcut.getAccessibleText();
        if (desktop.activate(title)) {
            return;
        }
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(
                    "/com/practice/aulaequilibrio/views/" + shortcut.getUserData()));
            Parent catalog = loader.load();
            if (loader.getController() instanceof CatalogController controller) {
                controller.attachDesktop(desktop, title);
            }
            desktop.open(title, catalog, 1000, 660, true);
        } catch (IOException exception) {
            LOGGER.log(System.Logger.Level.ERROR, "No se pudo abrir " + title, exception);
            Dialogs.error(desktop, "No se pudo abrir " + title, exception);
        }
    }

    @FXML
    private void openSchedules() {
        if (desktop.activate("Horarios")) {
            return;
        }
        ScheduleView schedules = new ScheduleView();
        schedules.load();
        desktop.open("Horarios", schedules, 900, 540, true);
    }
}
