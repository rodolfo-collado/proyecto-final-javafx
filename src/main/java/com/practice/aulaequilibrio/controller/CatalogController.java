package com.practice.aulaequilibrio.controller;

import com.practice.aulaequilibrio.config.HibernateUtil;
import com.practice.aulaequilibrio.dao.CatalogDAO;
import com.practice.aulaequilibrio.model.Availability;
import com.practice.aulaequilibrio.model.Teacher;
import com.practice.aulaequilibrio.util.Dialogs;
import com.practice.aulaequilibrio.view.mdi.MdiDesktop;
import lombok.RequiredArgsConstructor;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import org.hibernate.exception.ConstraintViolationException;

import java.text.Normalizer;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.concurrent.Callable;
import java.util.function.Consumer;
import java.util.regex.Pattern;

@RequiredArgsConstructor
public class CatalogController {
    private static final List<String> DAYS = List.of("Lunes", "Martes", "Miércoles", "Jueves", "Viernes", "Sábado", "Domingo");
    private static final String TEACHER_FORM = "Formulario de docente";
    private static final String ALL_DAYS = "Todos los días";
    private static final String WITH_EMAIL = "Con correo";
    private static final String WITHOUT_EMAIL = "Sin correo";
    private static final Pattern DIACRITICS = Pattern.compile("\\p{M}+");

    @FXML private SplitPane catalogs;
    @FXML private VBox availabilityPanel;
    @FXML private TableView<Teacher> teachers;
    @FXML private TableColumn<Teacher, String> teacherName, teacherEmail;
    @FXML private TableView<Availability> blocks;
    @FXML private TableColumn<Availability, String> blockDay;
    @FXML private TableColumn<Availability, Integer> blockValue;
    @FXML private ComboBox<String> day, emailFilter, dayFilter;
    @FXML private TextField blockNumber, teacherSearch, blockFilter;
    @FXML private Label availabilityTitle, blockEditorTitle, emptyTeachers, emptyBlocks, status;
    @FXML private Button editTeacherButton, deleteTeacherButton, deleteBlockButton;

    private final CatalogDAO dao;
    private final SimpleBooleanProperty busy = new SimpleBooleanProperty();
    private final ObservableList<Teacher> teacherRows = FXCollections.observableArrayList();
    private final ObservableList<Availability> blockRows = FXCollections.observableArrayList();
    private final FilteredList<Teacher> filteredTeachers = new FilteredList<>(teacherRows);
    private final FilteredList<Availability> filteredBlocks = new FilteredList<>(blockRows);
    private MdiDesktop desktop;
    private String moduleTitle;
    private boolean refreshingTeachers;

    public CatalogController() {
        this(new CatalogDAO(HibernateUtil.getSessionFactory()));
    }

    public void attachDesktop(MdiDesktop desktop, String moduleTitle) {
        this.desktop = desktop;
        this.moduleTitle = moduleTitle;
    }

    @FXML
    private void initialize() {
        teacherName.setCellValueFactory(cell -> new ReadOnlyStringWrapper(cell.getValue().getName()));
        teacherEmail.setCellValueFactory(cell -> new ReadOnlyStringWrapper(cell.getValue().getEmail()));
        blockDay.setCellValueFactory(cell -> new ReadOnlyStringWrapper(DAYS.get(cell.getValue().getDay() - 1)));
        blockValue.setCellValueFactory(cell -> new ReadOnlyObjectWrapper<>(cell.getValue().getBlockNumber()));
        teachers.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        blocks.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        day.getItems().setAll(DAYS);
        configureFilters();

        var noTeacher = teachers.getSelectionModel().selectedItemProperty().isNull();
        catalogs.disableProperty().bind(busy);
        editTeacherButton.disableProperty().bind(noTeacher);
        deleteTeacherButton.disableProperty().bind(noTeacher);
        availabilityPanel.disableProperty().bind(noTeacher);
        deleteBlockButton.disableProperty().bind(blocks.getSelectionModel().selectedItemProperty().isNull());
        teachers.getSelectionModel().selectedItemProperty().addListener((observable, previous, teacher) -> {
            if (!refreshingTeachers) {
                showBlocks(teacher, List.of());
                if (teacher != null) {
                    run("Disponibilidad actualizada.", () -> dao.listAvailability(teacher.getId()),
                            result -> showBlocks(teacher, result), this::message);
                }
            }
        });
        blocks.getSelectionModel().selectedItemProperty().addListener((observable, previous, block) -> {
            if (block == null) {
                blockEditorTitle.setText("Nuevo bloque");
                day.getSelectionModel().clearSelection();
                blockNumber.clear();
            } else {
                blockEditorTitle.setText("Editar bloque");
                day.getSelectionModel().select(block.getDay() - 1);
                blockNumber.setText(block.getBlockNumber().toString());
            }
        });
        reload();
    }

    private void configureFilters() {
        var sortedTeachers = new SortedList<>(filteredTeachers);
        sortedTeachers.comparatorProperty().bind(teachers.comparatorProperty());
        teachers.setItems(sortedTeachers);
        var sortedBlocks = new SortedList<>(filteredBlocks);
        sortedBlocks.comparatorProperty().bind(blocks.comparatorProperty());
        blocks.setItems(sortedBlocks);

        emailFilter.getItems().setAll("Todos", WITH_EMAIL, WITHOUT_EMAIL);
        emailFilter.getSelectionModel().selectFirst();
        dayFilter.getItems().add(ALL_DAYS);
        dayFilter.getItems().addAll(DAYS);
        dayFilter.getSelectionModel().selectFirst();

        teacherSearch.textProperty().addListener((observable, before, after) -> filterTeachers());
        emailFilter.valueProperty().addListener((observable, before, after) -> filterTeachers());
        dayFilter.valueProperty().addListener((observable, before, after) -> filterBlocks());
        blockFilter.textProperty().addListener((observable, before, after) -> filterBlocks());
    }

    private void filterTeachers() {
        String query = searchable(teacherSearch.getText().strip());
        String emailChoice = emailFilter.getValue();
        Teacher selected = teachers.getSelectionModel().getSelectedItem();
        refreshingTeachers = true;
        filteredTeachers.setPredicate(teacher -> {
            String email = Objects.toString(teacher.getEmail(), "");
            boolean hasEmail = !email.isBlank();
            boolean matchesEmail = (!WITH_EMAIL.equals(emailChoice) || hasEmail)
                    && (!WITHOUT_EMAIL.equals(emailChoice) || !hasEmail);
            return matchesEmail && (searchable(teacher.getName()).contains(query) || searchable(email).contains(query));
        });
        if (selected != null && filteredTeachers.contains(selected)) {
            teachers.getSelectionModel().select(selected);
        } else {
            teachers.getSelectionModel().clearSelection();
            showBlocks(null, List.of());
        }
        refreshingTeachers = false;
        emptyTeachers.setText(query.isEmpty() && "Todos".equals(emailChoice)
                ? "No hay docentes registrados." : "Ningún docente coincide con los filtros.");
    }

    private void filterBlocks() {
        int selectedDay = Math.max(0, dayFilter.getSelectionModel().getSelectedIndex());
        String number = blockFilter.getText().strip();
        Availability selected = blocks.getSelectionModel().getSelectedItem();
        filteredBlocks.setPredicate(block -> (selectedDay == 0 || block.getDay() == selectedDay)
                && (number.isEmpty() || block.getBlockNumber().toString().equals(number)));
        if (selected != null && filteredBlocks.contains(selected)) {
            blocks.getSelectionModel().select(selected);
        } else {
            blocks.getSelectionModel().clearSelection();
        }
        updateBlockPlaceholder();
    }

    private void updateBlockPlaceholder() {
        if (teachers.getSelectionModel().isEmpty()) {
            emptyBlocks.setText("Selecciona un docente para ver sus bloques.");
        } else if (blockRows.isEmpty()) {
            emptyBlocks.setText("Sin bloques registrados.");
        } else {
            emptyBlocks.setText("Ningún bloque coincide con los filtros.");
        }
    }

    private static String searchable(String text) {
        return DIACRITICS.matcher(Normalizer.normalize(text, Normalizer.Form.NFD))
                .replaceAll("").toLowerCase(Locale.ROOT);
    }

    @FXML private void newTeacher() {
        openTeacherForm(null);
    }

    @FXML private void editTeacher() {
        Teacher teacher = teachers.getSelectionModel().getSelectedItem();
        if (teacher != null) {
            openTeacherForm(teacher);
        }
    }

    private void openTeacherForm(Teacher teacher) {
        if (desktop.activate(TEACHER_FORM)) {
            return;
        }
        boolean editing = teacher != null;
        TextField name = new TextField(editing ? teacher.getName() : "");
        TextField email = new TextField(editing ? teacher.getEmail() : "");
        name.setPromptText("Nombre del docente");
        email.setPromptText("correo@ejemplo.com");
        Label title = new Label(editing ? "Editar docente" : "Nuevo docente");
        title.getStyleClass().add("page-title");
        Label feedback = new Label();
        feedback.setWrapText(true);
        feedback.getStyleClass().add("field-error");
        Button cancel = new Button("Cancelar");
        cancel.setOnAction(event -> desktop.close(TEACHER_FORM));
        Button save = new Button("Guardar docente");
        save.getStyleClass().add("primary");
        save.disableProperty().bind(busy);
        HBox actions = new HBox(8, cancel, save);
        actions.setAlignment(Pos.CENTER_RIGHT);
        VBox form = new VBox(10, title, new Label("Nombre *"), name,
                new Label("Correo opcional"), email, feedback, actions);
        form.getStyleClass().add("module-form");

        save.setOnAction(event -> {
            String enteredName = name.getText();
            String enteredEmail = email.getText();
            Integer id = editing ? teacher.getId() : null;
            feedback.setText("");
            run("Docente guardado.", () -> dao.saveTeacher(id, enteredName, enteredEmail), saved -> {
                updateTeacherRow(saved);
                if (catalogs.getScene() != null) {
                    desktop.close(TEACHER_FORM);
                }
            }, feedback::setText);
        });
        desktop.openPopup(moduleTitle, TEACHER_FORM, form, 460, 345);
        name.requestFocus();
    }

    private void updateTeacherRow(Teacher saved) {
        refreshingTeachers = true;
        teacherRows.removeIf(teacher -> Objects.equals(teacher.getId(), saved.getId()));
        teacherRows.add(saved);
        teacherRows.sort(Comparator.comparing(Teacher::getName).thenComparing(Teacher::getId));
        teachers.getSelectionModel().clearSelection();
        refreshingTeachers = false;
        showBlocks(null, List.of());
        if (filteredTeachers.contains(saved)) {
            teachers.getSelectionModel().select(saved);
        }
    }

    @FXML private void deleteTeacher() {
        Teacher teacher = teachers.getSelectionModel().getSelectedItem();
        if (teacher != null) {
            Dialogs.confirm(catalogs, "Eliminar docente", "¿Eliminar al docente «" + teacher.getName() + "»?",
                    () -> run("Docente eliminado.", () -> {
                dao.deleteTeacher(teacher.getId());
                return teacher;
            }, removed -> {
                refreshingTeachers = true;
                teachers.getSelectionModel().clearSelection();
                teacherRows.remove(removed);
                refreshingTeachers = false;
                showBlocks(null, List.of());
            }, this::message));
        }
    }

    @FXML private void newBlock() {
        blocks.getSelectionModel().clearSelection();
        day.getSelectionModel().clearSelection();
        blockNumber.clear();
        day.requestFocus();
    }

    @FXML private void saveBlock() {
        Teacher owner = teachers.getSelectionModel().getSelectedItem();
        if (owner == null) {
            return;
        }
        Availability selected = blocks.getSelectionModel().getSelectedItem();
        Integer id = selected == null ? null : selected.getId();
        Integer dayValue = day.getValue() == null ? null : DAYS.indexOf(day.getValue()) + 1;
        int number;
        try {
            number = Integer.parseInt(blockNumber.getText().strip());
        } catch (NumberFormatException exception) {
            message("El número de bloque debe ser un entero mayor que cero.");
            return;
        }
        run("Bloque guardado.", () -> dao.saveAvailability(id, owner.getId(), dayValue, number), saved -> {
            blocks.getSelectionModel().clearSelection();
            blockRows.removeIf(block -> Objects.equals(block.getId(), saved.getId()));
            blockRows.add(saved);
            blockRows.sort(Comparator.comparing(Availability::getDay)
                    .thenComparing(Availability::getBlockNumber).thenComparing(Availability::getId));
            updateBlockPlaceholder();
            day.getSelectionModel().clearSelection();
            blockNumber.clear();
        }, this::message);
    }

    @FXML private void deleteBlock() {
        Availability block = blocks.getSelectionModel().getSelectedItem();
        if (block != null) {
            Dialogs.confirm(catalogs, "Eliminar bloque", "¿Eliminar el bloque seleccionado?",
                    () -> run("Bloque eliminado.", () -> {
                dao.deleteAvailability(block.getId());
                return block;
            }, removed -> {
                blocks.getSelectionModel().clearSelection();
                blockRows.remove(removed);
                updateBlockPlaceholder();
            }, this::message));
        }
    }

    @FXML private void reload() {
        Teacher selected = teachers.getSelectionModel().getSelectedItem();
        Integer selectedId = selected == null ? null : selected.getId();
        run("Docentes actualizados.", dao::listTeachers, items -> {
            refreshingTeachers = true;
            teacherRows.setAll(items);
            teachers.getSelectionModel().clearSelection();
            refreshingTeachers = false;
            showBlocks(null, List.of());
            filteredTeachers.stream().filter(teacher -> Objects.equals(teacher.getId(), selectedId))
                    .findFirst().ifPresent(teachers.getSelectionModel()::select);
        }, this::message);
    }

    private void showBlocks(Teacher teacher, List<Availability> items) {
        availabilityTitle.setText(teacher == null ? "Selecciona un docente" : teacher.getName());
        blocks.getSelectionModel().clearSelection();
        blockRows.setAll(items);
        updateBlockPlaceholder();
        day.getSelectionModel().clearSelection();
        blockNumber.clear();
    }

    private <T> void run(String success, Callable<T> work, Consumer<T> apply, Consumer<String> showError) {
        busy.set(true);
        status.setText("Consultando PostgreSQL…");
        Task<T> task = new Task<>() {
            @Override protected T call() throws Exception {
                return work.call();
            }
        };
        task.setOnSucceeded(event -> {
            busy.set(false);
            status.setText(success);
            apply.accept(task.getValue());
        });
        task.setOnFailed(event -> {
            busy.set(false);
            Throwable failure = task.getException();
            String text = failure instanceof IllegalArgumentException ? failure.getMessage()
                    : failure instanceof ConstraintViolationException ? "Ya existe un registro con esos datos."
                    : "No se pudo completar la operación. Revisa la conexión con PostgreSQL.";
            status.setText(text);
            showError.accept(text);
        });
        Thread worker = new Thread(task, "catalogos-hibernate");
        worker.setDaemon(true);
        worker.start();
    }

    private void message(String text) {
        Dialogs.warning(catalogs, text);
    }
}
