package com.practice.aulaequilibrio.controller;

import com.practice.aulaequilibrio.model.Subject;
import com.practice.aulaequilibrio.service.SubjectService;
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

public class SubjectController {

    @FXML
    private TableView<Subject> table;
    @FXML
    private TableColumn<Subject, String> codigoColumn;
    @FXML
    private TableColumn<Subject, String> nombreColumn;
    @FXML
    private TableColumn<Subject, Integer> sesionesColumn;
    @FXML
    private TextField codigoField;
    @FXML
    private TextField nombreField;
    @FXML
    private TextField sesionesField;
    @FXML
    private Label formTitle;
    @FXML
    private Button deleteButton;

    private final SubjectService service = new SubjectService();
    private Integer editingId;

    @FXML
    private void initialize() {
        codigoColumn.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().getCodigo()));
        nombreColumn.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().getNombre()));
        sesionesColumn.setCellValueFactory(cell ->
                new SimpleObjectProperty<>(cell.getValue().getSesionesRequeridas()));
        table.getSelectionModel().selectedItemProperty().addListener((obs, old, selected) -> fillForm(selected));
        Platform.runLater(this::refresh);
    }

    @FXML
    private void refresh() {
        try {
            table.setItems(FXCollections.observableArrayList(service.listAll()));
        } catch (RuntimeException exception) {
            Dialogs.error(table, "No se pudieron cargar las asignaturas", exception);
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
            Subject saved = service.save(
                    editingId, codigoField.getText(), nombreField.getText(), sesionesField.getText());
            refresh();
            selectById(saved.getId());
            Dialogs.info(table, "Asignaturas",
                    creating ? "Asignatura creada correctamente." : "Asignatura actualizada correctamente.");
        } catch (RuntimeException exception) {
            Dialogs.error(table, "No se pudo guardar la asignatura", exception);
        }
    }

    @FXML
    private void delete() {
        Subject selected = table.getSelectionModel().getSelectedItem();
        if (selected == null) {
            return;
        }
        if (!Dialogs.confirm(table, "Eliminar asignatura",
                "¿Desea eliminar la asignatura \"" + selected + "\"?")) {
            return;
        }
        try {
            service.delete(selected.getId());
            refresh();
        } catch (RuntimeException exception) {
            Dialogs.error(table, "No se pudo eliminar la asignatura", exception);
            refresh();
        }
    }

    private void fillForm(Subject subject) {
        editingId = subject == null ? null : subject.getId();
        codigoField.setText(subject == null ? "" : subject.getCodigo());
        nombreField.setText(subject == null ? "" : subject.getNombre());
        sesionesField.setText(subject == null ? "" : String.valueOf(subject.getSesionesRequeridas()));
        formTitle.setText(subject == null ? "Nueva asignatura" : "Editar asignatura");
        deleteButton.setDisable(subject == null);
    }

    private void selectById(Integer id) {
        table.getItems().stream()
                .filter(item -> item.getId().equals(id))
                .findFirst()
                .ifPresent(item -> table.getSelectionModel().select(item));
    }
}
