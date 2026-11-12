package com.practice.aulaequilibrio.controller;

import com.practice.aulaequilibrio.model.Subject;
import com.practice.aulaequilibrio.service.SubjectService;
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
    private Button editButton, deleteButton;

    private final SubjectService service = new SubjectService();

    @FXML
    private void initialize() {
        codigoColumn.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().getCodigo()));
        nombreColumn.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().getNombre()));
        sesionesColumn.setCellValueFactory(cell ->
                new SimpleObjectProperty<>(cell.getValue().getSesionesRequeridas()));
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
            Dialogs.error(table, "No se pudieron cargar las asignaturas", exception);
        }
        table.getSelectionModel().clearSelection();
    }

    @FXML
    private void create() {
        openForm(null);
    }

    @FXML
    private void edit() {
        Subject selected = table.getSelectionModel().getSelectedItem();
        if (selected != null) {
            openForm(selected);
        }
    }

    private void openForm(Subject selected) {
        TextField codigo = new TextField(selected == null ? "" : selected.getCodigo());
        codigo.setPromptText("Ej. MAT101");
        TextField nombre = new TextField(selected == null ? "" : selected.getNombre());
        nombre.setPromptText("Ej. Matemática I");
        TextField sesiones = new TextField(selected == null ? "" : selected.getSesionesRequeridas().toString());
        sesiones.setPromptText("Mayor que cero");
        Label feedback = new Label();
        feedback.setWrapText(true);
        feedback.getStyleClass().add("field-error");
        VBox form = new VBox(8, new Label("Código *"), codigo, new Label("Nombre *"), nombre, new Label("Sesiones requeridas *"), sesiones, feedback);
        Dialogs.form(table, selected == null ? "Nueva asignatura" : "Editar asignatura", form, (pane, close) -> {
            Subject saved;
            try {
                saved = service.save(selected == null ? null : selected.getId(), codigo.getText(), nombre.getText(), sesiones.getText());
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
        Subject selected = table.getSelectionModel().getSelectedItem();
        if (selected == null) {
            return;
        }
        Dialogs.confirm(table, "Eliminar asignatura",
                "¿Desea eliminar la asignatura \"" + selected + "\"?", () -> {
            try {
                service.delete(selected.getId());
                refresh();
            } catch (RuntimeException exception) {
                Dialogs.error(table, "No se pudo eliminar la asignatura", exception);
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
