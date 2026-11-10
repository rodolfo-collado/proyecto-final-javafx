package com.practice.aulaequilibrio.controller;

import com.practice.aulaequilibrio.config.HibernateUtil;
import com.practice.aulaequilibrio.dao.CatalogDAO;
import com.practice.aulaequilibrio.model.Availability;
import com.practice.aulaequilibrio.model.Teacher;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.util.StringConverter;

import java.util.List;
import java.util.concurrent.Callable;

public class CatalogController {
    private static final List<String> DAYS = List.of("Lunes", "Martes", "Miércoles", "Jueves", "Viernes", "Sábado", "Domingo");
    @FXML private VBox catalogs;
    @FXML private TableView<Teacher> teachers;
    @FXML private TableColumn<Teacher, Integer> teacherId;
    @FXML private TableColumn<Teacher, String> teacherName, teacherEmail;
    @FXML private TextField name, email, blockNumber;
    @FXML private ComboBox<Teacher> blockTeacher, filterTeacher;
    @FXML private ComboBox<String> day;
    @FXML private TableView<Availability> blocks;
    @FXML private TableColumn<Availability, Integer> blockId, blockValue;
    @FXML private TableColumn<Availability, String> blockOwner, blockDay;
    @FXML private Label status;
    private final CatalogDAO dao;

    public CatalogController() { this(new CatalogDAO(HibernateUtil.getSessionFactory())); }
    public CatalogController(CatalogDAO dao) { this.dao = dao; }

    @FXML
    public void initialize() {
        teacherId.setCellValueFactory(c -> new ReadOnlyObjectWrapper<>(c.getValue().getId()));
        teacherName.setCellValueFactory(c -> new ReadOnlyStringWrapper(c.getValue().getName()));
        teacherEmail.setCellValueFactory(c -> new ReadOnlyStringWrapper(c.getValue().getEmail()));
        blockId.setCellValueFactory(c -> new ReadOnlyObjectWrapper<>(c.getValue().getId()));
        blockOwner.setCellValueFactory(c -> new ReadOnlyStringWrapper(c.getValue().getTeacher().getName()));
        blockDay.setCellValueFactory(c -> new ReadOnlyStringWrapper(DAYS.get(c.getValue().getDay() - 1)));
        blockValue.setCellValueFactory(c -> new ReadOnlyObjectWrapper<>(c.getValue().getBlockNumber()));
        var converter = new StringConverter<Teacher>() {
            @Override public String toString(Teacher t) { return t == null ? "" : t.getName() + " (#" + t.getId() + ")"; }
            @Override public Teacher fromString(String text) { return null; }
        };
        blockTeacher.setConverter(converter);
        filterTeacher.setConverter(converter);
        day.getItems().setAll(DAYS);
        teachers.getSelectionModel().selectedItemProperty().addListener((obs, old, t) -> {
            if (t != null) { name.setText(t.getName()); email.setText(t.getEmail()); }
        });
        blocks.getSelectionModel().selectedItemProperty().addListener((obs, old, b) -> {
            if (b != null) {
                selectTeacher(blockTeacher, b.getTeacher().getId());
                day.getSelectionModel().select(b.getDay() - 1);
                blockNumber.setText(b.getBlockNumber().toString());
            }
        });
        run("Catálogos cargados.", this::refresh);
    }

    @FXML private void newTeacher() {
        teachers.getSelectionModel().clearSelection();
        name.clear(); email.clear(); name.requestFocus();
    }
    @FXML private void newBlock() {
        blocks.getSelectionModel().clearSelection();
        blockTeacher.getSelectionModel().clearSelection();
        day.getSelectionModel().clearSelection(); blockNumber.clear();
    }
    @FXML private void saveTeacher() {
        Teacher selected = teachers.getSelectionModel().getSelectedItem();
        Integer id = selected == null ? null : selected.getId();
        String enteredName = name.getText(), enteredEmail = email.getText();
        run("Docente guardado.", () -> { dao.saveTeacher(id, enteredName, enteredEmail); return refresh(); });
    }
    @FXML private void deleteTeacher() {
        Teacher selected = teachers.getSelectionModel().getSelectedItem();
        if (selected == null) { message("Selecciona el docente que deseas eliminar."); return; }
        if (!confirm("¿Eliminar al docente «" + selected.getName() + "»?")) return;
        run("Docente eliminado.", () -> { dao.deleteTeacher(selected.getId()); return refresh(); });
    }
    @FXML private void saveBlock() {
        Availability selected = blocks.getSelectionModel().getSelectedItem();
        Integer id = selected == null ? null : selected.getId();
        Teacher owner = blockTeacher.getValue();
        Integer ownerId = owner == null ? null : owner.getId();
        Integer dayValue = day.getValue() == null ? null : DAYS.indexOf(day.getValue()) + 1;
        Integer number;
        try { number = Integer.valueOf(blockNumber.getText().strip()); }
        catch (NumberFormatException e) { message("El número de bloque debe ser un entero mayor que cero."); return; }
        run("Bloque guardado.", () -> { dao.saveAvailability(id, ownerId, dayValue, number); return refresh(); });
    }
    @FXML private void deleteBlock() {
        Availability selected = blocks.getSelectionModel().getSelectedItem();
        if (selected == null) { message("Selecciona el bloque que deseas eliminar."); return; }
        if (!confirm("¿Eliminar el bloque seleccionado?")) return;
        run("Bloque eliminado.", () -> { dao.deleteAvailability(selected.getId()); return refresh(); });
    }
    @FXML private void filter() {
        Teacher selected = filterTeacher.getValue();
        if (selected == null) { message("Selecciona un docente para consultar su disponibilidad."); return; }
        Integer id = selected.getId();
        run("Disponibilidad de " + selected.getName() + ".", () -> new Catalogs(dao.listTeachers(), dao.listAvailability(id), id));
    }
    @FXML private void showAll() {
        filterTeacher.getSelectionModel().clearSelection();
        run("Mostrando todos los bloques.", this::refresh);
    }
    @FXML private void reload() { run("Catálogos actualizados.", this::refresh); }

    // Captura el filtro en el hilo de JavaFX antes de iniciar cada trabajo.
    private Integer activeFilter;
    private Catalogs refresh() {
        return new Catalogs(dao.listTeachers(), dao.listAvailability(activeFilter), activeFilter);
    }
    private record Catalogs(List<Teacher> teachers, List<Availability> blocks, Integer filterId) { }

    private void run(String success, Callable<Catalogs> work) {
        Teacher filter = filterTeacher.getValue();
        activeFilter = filter == null ? null : filter.getId();
        catalogs.setDisable(true);
        status.setText("Consultando la base de datos…");
        Task<Catalogs> task = new Task<>() {
            @Override protected Catalogs call() throws Exception { return work.call(); }
        };
        task.setOnSucceeded(event -> {
            catalogs.setDisable(false);
            Catalogs result = task.getValue();
            teachers.getItems().setAll(result.teachers());
            blockTeacher.getItems().setAll(result.teachers());
            filterTeacher.getItems().setAll(result.teachers());
            selectTeacher(filterTeacher, result.filterId());
            blocks.getItems().setAll(result.blocks());
            newTeacher(); newBlock();
            status.setText(success + " Bloques mostrados: " + result.blocks().size());
        });
        task.setOnFailed(event -> {
            catalogs.setDisable(false);
            Throwable error = task.getException();
            String text = error instanceof IllegalArgumentException ? error.getMessage()
                    : "No se pudo completar la operación. Revisa la conexión y que el correo no esté repetido.";
            status.setText(text);
            message(text);
        });
        Thread worker = new Thread(task, "catalogos-hibernate");
        worker.setDaemon(true);
        worker.start();
    }

    private void selectTeacher(ComboBox<Teacher> combo, Integer id) {
        combo.getSelectionModel().clearSelection();
        if (id != null) combo.getItems().stream().filter(t -> id.equals(t.getId())).findFirst().ifPresent(combo::setValue);
    }
    private void message(String text) {
        Alert alert = new Alert(Alert.AlertType.WARNING, text, ButtonType.OK);
        alert.setHeaderText("Revisa la operación"); alert.showAndWait();
    }
    private boolean confirm(String text) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION, text, ButtonType.OK, ButtonType.CANCEL);
        alert.setHeaderText("Confirmar eliminación");
        return alert.showAndWait().orElse(ButtonType.CANCEL) == ButtonType.OK;
    }
}
