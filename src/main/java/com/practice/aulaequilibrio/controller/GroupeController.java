package com.practice.aulaequilibrio.controller;

import com.practice.aulaequilibrio.model.Groupe;
import com.practice.aulaequilibrio.service.GroupeService;
import com.practice.aulaequilibrio.util.Dialogs;
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

public class GroupeController {

    @FXML
    private TableView<Groupe> table;
    @FXML
    private TableColumn<Groupe, String> nombreColumn;
    @FXML
    private TableColumn<Groupe, Integer> cantidadColumn;
    @FXML
    private TextField nombreField;
    @FXML
    private TextField cantidadField;
    @FXML
    private Label formTitle;
    @FXML
    private Button deleteButton;

    private final GroupeService service = new GroupeService();
    private Integer editingId;

    @FXML
    private void initialize() {
        nombreColumn.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().getNombre()));
        cantidadColumn.setCellValueFactory(cell ->
                new SimpleObjectProperty<>(cell.getValue().getCantidadEstudiantes()));
        table.getSelectionModel().selectedItemProperty().addListener((obs, old, selected) -> fillForm(selected));
        Platform.runLater(this::refresh);
    }

    @FXML
    private void refresh() {
        try {
            table.setItems(FXCollections.observableArrayList(service.listAll()));
        } catch (RuntimeException exception) {
            Dialogs.error(table, "No se pudieron cargar los grupos", exception);
        }
        clearForm();
    }

    @FXML
    private void clearForm() {
        table.getSelectionModel().clearSelection();
        fillForm(null);
    }

    @FXML
    private void save() {
        boolean creating = editingId == null;
        try {
            Groupe saved = service.save(editingId, nombreField.getText(), cantidadField.getText());
            refresh();
            selectById(saved.getId());
            Dialogs.info(table, "Grupos", creating ? "Grupo creado correctamente." : "Grupo actualizado correctamente.");
        } catch (RuntimeException exception) {
            Dialogs.error(table, "No se pudo guardar el grupo", exception);
        }
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

    private void fillForm(Groupe groupe) {
        editingId = groupe == null ? null : groupe.getId();
        nombreField.setText(groupe == null ? "" : groupe.getNombre());
        cantidadField.setText(groupe == null ? "" : String.valueOf(groupe.getCantidadEstudiantes()));
        formTitle.setText(groupe == null ? "Nuevo grupo" : "Editar grupo");
        deleteButton.setDisable(groupe == null);
    }

    private void selectById(Integer id) {
        table.getItems().stream()
                .filter(item -> item.getId().equals(id))
                .findFirst()
                .ifPresent(item -> table.getSelectionModel().select(item));
    }
}
