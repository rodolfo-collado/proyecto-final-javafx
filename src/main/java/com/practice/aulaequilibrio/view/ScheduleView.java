package com.practice.aulaequilibrio.view;

import com.practice.aulaequilibrio.model.ScheduleAssignment;
import com.practice.aulaequilibrio.model.ScheduleVersion;
import com.practice.aulaequilibrio.repository.ScheduleRepository;
import com.practice.aulaequilibrio.repository.ScheduleRepository.Option;
import com.practice.aulaequilibrio.repository.ScheduleRepository.Placement;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.collections.FXCollections;
import javafx.event.ActionEvent;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.util.StringConverter;

import java.time.DayOfWeek;
import java.time.format.TextStyle;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.function.Function;

public class ScheduleView extends BorderPane {
    private static final System.Logger LOGGER = System.getLogger(ScheduleView.class.getName());
    private static final String STYLESHEET = ScheduleView.class.getResource(
            "/com/practice/aulaequilibrio/styles/aula.css").toExternalForm();
    private static final Option ALL_GROUPS = new Option(null, "Todos los grupos");
    private static final ButtonType SAVE = new ButtonType("Guardar", ButtonBar.ButtonData.OK_DONE);

    private final ScheduleRepository repository = new ScheduleRepository();
    private final ComboBox<ScheduleVersion> versions = new ComboBox<>();
    private final ComboBox<Option> groups = new ComboBox<>();
    private final TableView<ScheduleAssignment> table = new TableView<>();
    private final Label message = new Label();

    public ScheduleView() {
        getStylesheets().add(STYLESHEET);
        getStyleClass().add("schedule-page");
        setPadding(new Insets(20));
        versions.setPromptText("Elige una versión");
        versions.setConverter(display(version -> version.getName() + " · " + version.getCreatedOn()));
        versions.setPrefWidth(280);
        groups.setPrefWidth(200);

        Button editVersion = button("Renombrar", () -> versionDialog(versions.getValue()));
        Button deleteVersion = button("Eliminar versión", this::deleteVersion);
        deleteVersion.getStyleClass().add("danger");
        Button newVersion = button("Nueva versión", () -> versionDialog(null));
        newVersion.getStyleClass().add("primary");
        editVersion.disableProperty().bind(versions.valueProperty().isNull());
        deleteVersion.disableProperty().bind(versions.valueProperty().isNull());
        HBox versionBar = new HBox(8, new Label("Versión"), versions,
                newVersion, editVersion, deleteVersion);
        versionBar.setAlignment(Pos.CENTER_LEFT);
        HBox filterBar = new HBox(8, new Label("Grupo"), groups,
                button("Actualizar", () -> reload(selectedVersionId())));
        filterBar.setAlignment(Pos.CENTER_LEFT);
        Label title = new Label("Horarios");
        title.getStyleClass().add("page-title");
        VBox filters = new VBox(9, versionBar, filterBar);
        filters.getStyleClass().add("panel");
        setTop(new VBox(12, title, filters));

        table.getColumns().add(column("Día", a -> dayLabel(DayOfWeek.of(a.getDay()))));
        table.getColumns().add(column("Bloque", a -> String.valueOf(a.getBlock())));
        table.getColumns().add(column("Grupo", a -> a.getGroup().getNombre()));
        table.getColumns().add(column("Asignatura", a -> a.getSubject().getNombre()));
        table.getColumns().add(column("Docente", a -> a.getTeacher().getName()));
        table.getColumns().add(column("Aula", a -> a.getClassroom().getCodigo()));
        table.setPlaceholder(new Label("No hay asignaciones para esta selección."));
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        setMargin(table, new Insets(12, 0, 12, 0));
        setCenter(table);

        Button add = button("Nueva asignación", () -> assignmentDialog(null));
        Button edit = button("Editar asignación", () -> assignmentDialog(table.getSelectionModel().getSelectedItem()));
        Button delete = button("Eliminar asignación", this::deleteAssignment);
        add.getStyleClass().add("primary");
        delete.getStyleClass().add("danger");
        add.disableProperty().bind(versions.valueProperty().isNull());
        edit.disableProperty().bind(table.getSelectionModel().selectedItemProperty().isNull());
        delete.disableProperty().bind(table.getSelectionModel().selectedItemProperty().isNull());
        message.setWrapText(true);
        setBottom(new VBox(10, new HBox(8, add, edit, delete), message));

        versions.setOnAction(event -> refreshAssignments());
        groups.setOnAction(event -> refreshAssignments());
    }

    public void load() {
        runAction(() -> reload(null));
    }

    private void reload(Integer versionId) {
        Integer groupId = selectedId(groups);
        List<Option> options = repository.listCatalogs().groups();
        groups.getItems().setAll(ALL_GROUPS);
        groups.getItems().addAll(options);
        select(groups, groupId);
        if (groups.getValue() == null) {
            groups.setValue(ALL_GROUPS);
        }
        reloadVersions(versionId);
    }

    private void reloadVersions(Integer versionId) {
        List<ScheduleVersion> available = repository.listVersions();
        versions.getItems().setAll(available);
        versions.setValue(available.stream()
                .filter(version -> version.getId().equals(versionId))
                .findFirst()
                .orElse(available.isEmpty() ? null : available.getFirst()));
        refreshAssignments();
    }

    private Integer selectedVersionId() {
        ScheduleVersion version = versions.getValue();
        return version == null ? null : version.getId();
    }

    private void refreshAssignments() {
        table.getItems().clear();
        Integer versionId = selectedVersionId();
        if (versionId != null) {
            runAction(() -> table.getItems().setAll(repository.listAssignments(versionId, selectedId(groups))));
        }
    }

    private void versionDialog(ScheduleVersion version) {
        TextField name = new TextField(version == null ? "" : version.getName());
        showEditor(version == null ? "Nueva versión" : "Renombrar versión", form("Nombre", name), () -> {
            int id = repository.saveVersion(version == null ? null : version.getId(), name.getText());
            runAction(() -> reloadVersions(id));
        });
    }

    private void deleteVersion() {
        ScheduleVersion version = versions.getValue();
        if (confirm("¿Eliminar la versión «" + version.getName() + "»?")) {
            repository.deleteVersion(version.getId());
            reloadVersions(null);
        }
    }

    private void assignmentDialog(ScheduleAssignment assignment) {
        int versionId = selectedVersionId();
        var catalogs = repository.listCatalogs();
        if (catalogs.groups().isEmpty() || catalogs.subjects().isEmpty()
                || catalogs.teachers().isEmpty() || catalogs.classrooms().isEmpty()) {
            throw new IllegalArgumentException("Registra al menos un grupo, una asignatura, un docente y un aula "
                    + "en los catálogos antes de crear una asignación.");
        }

        ComboBox<Option> group = chooser(catalogs.groups());
        ComboBox<Option> subject = chooser(catalogs.subjects());
        ComboBox<Option> teacher = chooser(catalogs.teachers());
        ComboBox<Option> classroom = chooser(catalogs.classrooms());
        ComboBox<DayOfWeek> day = new ComboBox<>(FXCollections.observableArrayList(DayOfWeek.values()));
        day.setConverter(display(ScheduleView::dayLabel));
        day.setValue(DayOfWeek.MONDAY);
        TextField block = new TextField("1");

        if (assignment != null) {
            select(group, assignment.getGroup().getId());
            select(subject, assignment.getSubject().getId());
            select(teacher, assignment.getTeacher().getId());
            select(classroom, assignment.getClassroom().getId());
            day.setValue(DayOfWeek.of(assignment.getDay()));
            block.setText(String.valueOf(assignment.getBlock()));
        } else if (selectedId(groups) != null) {
            select(group, selectedId(groups));
        }

        GridPane fields = form("Grupo", group);
        fields.addRow(1, new Label("Asignatura"), subject);
        fields.addRow(2, new Label("Docente"), teacher);
        fields.addRow(3, new Label("Aula"), classroom);
        fields.addRow(4, new Label("Día"), day);
        fields.addRow(5, new Label("Bloque"), block);

        showEditor(assignment == null ? "Nueva asignación" : "Editar asignación", fields, () -> {
            Placement placement = new Placement(day.getValue().getValue(), readBlock(block.getText()),
                    selectedId(group), selectedId(subject), selectedId(teacher), selectedId(classroom));
            repository.saveAssignment(versionId, assignment == null ? null : assignment.getId(), placement);
            refreshAssignments();
        });
    }

    private void deleteAssignment() {
        ScheduleAssignment assignment = table.getSelectionModel().getSelectedItem();
        if (confirm("¿Eliminar la asignación seleccionada?")) {
            repository.deleteAssignment(selectedVersionId(), assignment.getId());
            refreshAssignments();
        }
    }

    private void showEditor(String title, GridPane fields, Runnable save) {
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.initOwner(getScene().getWindow());
        dialog.setTitle(title);
        Label error = new Label();
        error.setWrapText(true);
        error.getStyleClass().add("field-error");
        VBox content = new VBox(10, fields, error);
        content.setPrefWidth(360);
        dialog.getDialogPane().setContent(content);
        dialog.getDialogPane().getStylesheets().add(STYLESHEET);
        dialog.getDialogPane().getButtonTypes().setAll(SAVE, ButtonType.CANCEL);
        dialog.getDialogPane().lookupButton(SAVE).addEventFilter(ActionEvent.ACTION, event -> {
            try {
                save.run();
            } catch (RuntimeException exception) {
                error.setText(errorMessage(exception));
                event.consume();
            }
        });
        dialog.showAndWait();
    }

    private boolean confirm(String question) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION, question, ButtonType.OK, ButtonType.CANCEL);
        alert.initOwner(getScene().getWindow());
        alert.getDialogPane().getStylesheets().add(STYLESHEET);
        return alert.showAndWait().filter(ButtonType.OK::equals).isPresent();
    }

    private Button button(String label, Runnable action) {
        Button button = new Button(label);
        button.setOnAction(event -> runAction(action));
        return button;
    }

    private void runAction(Runnable action) {
        message.setText("");
        try {
            action.run();
        } catch (RuntimeException exception) {
            message.setText(errorMessage(exception));
        }
    }

    private static String errorMessage(RuntimeException exception) {
        if (exception instanceof IllegalArgumentException) {
            return exception.getMessage();
        }
        LOGGER.log(System.Logger.Level.ERROR, "Error en el módulo de horarios", exception);
        return "No se pudo completar la operación. Actualiza la vista e inténtalo de nuevo.";
    }

    private static int readBlock(String text) {
        try {
            return Integer.parseInt(text.strip());
        } catch (NumberFormatException exception) {
            throw new IllegalArgumentException("Escribe un número entero para el bloque.", exception);
        }
    }

    private static String dayLabel(DayOfWeek day) {
        return day.getDisplayName(TextStyle.FULL, Locale.forLanguageTag("es-NI"));
    }

    private static GridPane form(String label, Node input) {
        GridPane pane = new GridPane();
        pane.setHgap(12);
        pane.setVgap(8);
        pane.addRow(0, new Label(label), input);
        return pane;
    }

    private static ComboBox<Option> chooser(List<Option> options) {
        ComboBox<Option> chooser = new ComboBox<>(FXCollections.observableArrayList(options));
        chooser.setPrefWidth(220);
        return chooser;
    }

    private static Integer selectedId(ComboBox<Option> chooser) {
        return chooser.getValue() == null ? null : chooser.getValue().id();
    }

    private static void select(ComboBox<Option> chooser, Integer id) {
        chooser.setValue(chooser.getItems().stream()
                .filter(option -> Objects.equals(option.id(), id)).findFirst().orElse(null));
    }

    private static TableColumn<ScheduleAssignment, String> column(String title,
                                                                   Function<ScheduleAssignment, String> value) {
        TableColumn<ScheduleAssignment, String> column = new TableColumn<>(title);
        column.setCellValueFactory(cell -> new ReadOnlyStringWrapper(value.apply(cell.getValue())));
        return column;
    }

    private static <T> StringConverter<T> display(Function<T, String> label) {
        return new StringConverter<>() {
            @Override
            public String toString(T value) {
                return value == null ? "" : label.apply(value);
            }

            @Override
            public T fromString(String value) {
                throw new UnsupportedOperationException();
            }
        };
    }
}
