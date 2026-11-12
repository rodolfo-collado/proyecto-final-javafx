package com.practice.aulaequilibrio.controller;

import com.practice.aulaequilibrio.model.Groupe;
import com.practice.aulaequilibrio.service.GroupeService;
import com.practice.aulaequilibrio.util.Dialogs;
import com.practice.aulaequilibrio.util.ErrorMessages;
import javafx.application.Platform;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;

public class GroupeController {

    @FXML
    private TableView<Groupe> table;
    @FXML
    private TableColumn<Groupe, String> nombreColumn;
    @FXML
    private TableColumn<Groupe, Integer> cantidadColumn;
    @FXML
    private Button editButton, deleteButton;

    private final GroupeService service = new GroupeService();

    @FXML
    private void initialize() {
        nombreColumn.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().getNombre()));
        cantidadColumn.setCellValueFactory(cell ->
                new SimpleObjectProperty<>(cell.getValue().getCantidadEstudiantes()));
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        var noSelection = table.getSelectionModel().selectedItemProperty().isNull();
        editButton.disableProperty().bind(noSelection);
        deleteButton.disableProperty().bind(noSelection);
        Platform.runLater(this::refresh);
    }

    @FXML
    private void refresh() {
        try {
            table.setItems(FXCollections.observableArrayList(service.listAll()));
        } catch (RuntimeException exception) {
            Dialogs.error(table, "No se pudieron cargar los grupos", exception);
        }
        table.getSelectionModel().clearSelection();
    }

    @FXML
    private void create() {
        openForm(null);
    }

    @FXML
    private void edit() {
        Groupe selected = table.getSelectionModel().getSelectedItem();
        if (selected != null) {
            openForm(selected);
        }
    }

    private void openForm(Groupe selected) {
        TextField nombre = new TextField(selected == null ? "" : selected.getNombre());
        nombre.setPromptText("Ej. 1A");
        TextField cantidad = new TextField(selected == null ? "" : selected.getCantidadEstudiantes().toString());
        cantidad.setPromptText("Mayor que cero");
        Label feedback = new Label();
        feedback.setWrapText(true);
        feedback.getStyleClass().add("field-error");
        VBox form = new VBox(8, new Label("Nombre *"), nombre, new Label("Cantidad de estudiantes *"), cantidad, feedback);
        Dialogs.form(table, selected == null ? "Nuevo grupo" : "Editar grupo", form, (pane, close) -> {
            Groupe saved;
            try {
                saved = service.save(selected == null ? null : selected.getId(), nombre.getText(), cantidad.getText());
            } catch (RuntimeException exception) {
                feedback.setText(ErrorMessages.forUser(exception));
                return;
            }
            close.run();
            refresh();
            selectById(saved.getId());
        });
    }

    @FXML
    private void delete() {
        Groupe selected = table.getSelectionModel().getSelectedItem();
        if (selected == null) {
            return;
        }
        Dialogs.confirm(table, "Eliminar grupo", "¿Desea eliminar el grupo \"" + selected.getNombre() + "\"?", () -> {
            try {
                service.delete(selected.getId());
                refresh();
            } catch (RuntimeException exception) {
                Dialogs.error(table, "No se pudo eliminar el grupo", exception);
                refresh();
            }
        });
    }

    private void selectById(Integer id) {
        table.getItems().stream()
                .filter(item -> item.getId().equals(id))
                .findFirst()
                .ifPresent(item -> table.getSelectionModel().select(item));
    }
}
