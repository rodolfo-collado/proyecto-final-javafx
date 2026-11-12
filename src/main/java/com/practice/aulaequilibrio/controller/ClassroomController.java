package com.practice.aulaequilibrio.controller;
import com.practice.aulaequilibrio.dao.*;
import com.practice.aulaequilibrio.model.*;
import com.practice.aulaequilibrio.util.Dialogs;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import java.util.List;
public class ClassroomController {
    @FXML private BorderPane root;
    @FXML private TableView<Classroom> classroomTable;
    @FXML private TableView<ClassroomType> typeTable;
    @FXML private TableColumn<Classroom,String> codeColumn,capacityColumn,classroomTypeColumn;
    @FXML private TableColumn<ClassroomType,String> typeNameColumn;
    @FXML private Label status, classroomScope;
    @FXML private Button editClassroomButton, deleteClassroomButton, editTypeButton, deleteTypeButton, showAllButton;
    private final ClassroomDAO classrooms=new ClassroomDAO();
    private final ClassroomTypeDAO types=new ClassroomTypeDAO();
    private boolean refreshing;
    @FXML private void initialize() {
        classroomTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);typeTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        codeColumn.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getCodigo()));
        capacityColumn.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getCapacidad().toString()));
        classroomTypeColumn.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getType().getName()));
        typeNameColumn.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getName()));
        var noRoom = classroomTable.getSelectionModel().selectedItemProperty().isNull();
        editClassroomButton.disableProperty().bind(noRoom);
        deleteClassroomButton.disableProperty().bind(noRoom);
        var noType = typeTable.getSelectionModel().selectedItemProperty().isNull();
        editTypeButton.disableProperty().bind(noType);
        deleteTypeButton.disableProperty().bind(noType);
        showAllButton.disableProperty().bind(noType);
        typeTable.getSelectionModel().selectedItemProperty().addListener((observable, previous, selected) -> {
            if (!refreshing) {
                loadClassrooms(selected);
            }
        });
        refresh();
    }

    @FXML private void refresh() {
        ClassroomType selected = typeTable.getSelectionModel().getSelectedItem();
        Integer typeId = selected == null ? null : selected.getId();
        root.setDisable(true);
        status.setText("Cargando aulas…");
        Thread thread = new Thread(() -> {
            try {
                List<ClassroomType> categories = types.listar();
                ClassroomType current = categories.stream().filter(type -> type.getId().equals(typeId))
                        .findFirst().orElse(null);
                List<Classroom> rooms = findClassrooms(current);
                Platform.runLater(() -> {
                    refreshing = true;
                    typeTable.getItems().setAll(categories);
                    typeTable.getSelectionModel().clearSelection();
                    if (current != null) {
                        typeTable.getSelectionModel().select(current);
                    }
                    refreshing = false;
                    showClassrooms(current, rooms);
                });
            } catch (Exception exception) {
                Platform.runLater(() -> loadFailed(exception));
            }
        }, "consultar-aulas");
        thread.setDaemon(true);
        thread.start();
    }

    @FXML private void showAllClassrooms() {
        typeTable.getSelectionModel().clearSelection();
    }

    private void loadClassrooms(ClassroomType selected) {
        classroomTable.getItems().clear();
        root.setDisable(true);
        status.setText("Cargando aulas…");
        Thread thread = new Thread(() -> {
            try {
                List<Classroom> rooms = findClassrooms(selected);
                Platform.runLater(() -> showClassrooms(selected, rooms));
            } catch (Exception exception) {
                Platform.runLater(() -> loadFailed(exception));
            }
        }, "filtrar-aulas");
        thread.setDaemon(true);
        thread.start();
    }

    private List<Classroom> findClassrooms(ClassroomType type) {
        return type == null ? classrooms.listar() : classrooms.listarPorTipo(type.getId());
    }

    private void showClassrooms(ClassroomType type, List<Classroom> rooms) {
        classroomTable.getSelectionModel().clearSelection();
        classroomTable.getItems().setAll(rooms);
        classroomScope.setText(type == null ? "Todos los tipos" : type.getName());
        status.setText(rooms.size() + " aulas · " + typeTable.getItems().size() + " tipos");
        root.setDisable(false);
    }

    private void loadFailed(Exception exception) {
        root.setDisable(false);
        status.setText("No se pudieron cargar las aulas.");
        error(exception);
    }

    @FXML private void addClassroom(){editClassroom(null);}
    @FXML private void editSelectedClassroom(){Classroom selected=classroomTable.getSelectionModel().getSelectedItem();if(selected==null){info("Seleccione un aula.");return;}editClassroom(selected);}
    private void editClassroom(Classroom selected) {
        if(typeTable.getItems().isEmpty()){info("Agregue primero un tipo de aula en el panel lateral.");return;}
        String title = selected == null ? "Agregar aula" : "Editar aula";
        TextField code=new TextField(selected==null?"":selected.getCodigo());TextField capacity=new TextField(selected==null?"":selected.getCapacidad().toString());
        ComboBox<ClassroomType> type=new ComboBox<>(FXCollections.observableArrayList(typeTable.getItems()));type.setPromptText("Seleccione un tipo");type.setMaxWidth(Double.MAX_VALUE);
        ClassroomType initialType = selected == null ? typeTable.getSelectionModel().getSelectedItem() : selected.getType();
        if (initialType != null) {
            type.getItems().stream().filter(option -> option.getId().equals(initialType.getId()))
                    .findFirst().ifPresent(type::setValue);
        }
        GridPane grid=grid();grid.addRow(0,new Label("Código *"),code);grid.addRow(1,new Label("Capacidad *"),capacity);grid.addRow(2,new Label("Tipo de aula *"),type);
        Label feedback=feedback();VBox content=new VBox(16,grid,feedback);content.setPadding(new Insets(18));
        Dialogs.form(root, title, content, (pane, close) -> {
            try {
                String normalized=code.getText().trim();if(normalized.isBlank()||normalized.length()>20)throw new IllegalArgumentException("Ingrese un código de hasta 20 caracteres.");
                int count;try{count=Integer.parseInt(capacity.getText().trim());}catch(NumberFormatException ex){throw new IllegalArgumentException("La capacidad debe ser un número entero mayor que cero.");}
                if(count<=0)throw new IllegalArgumentException("La capacidad debe ser mayor que cero.");if(type.getValue()==null)throw new IllegalArgumentException("Seleccione un tipo de aula.");
                // Mantener el id y la revisión para detectar ediciones simultáneas.
                Classroom value=selected==null?new Classroom():selected;String oldCode=value.getCodigo();Integer oldCapacity=value.getCapacidad();ClassroomType oldType=value.getType();
                value.setCodigo(normalized);value.setCapacidad(count);value.setType(type.getValue());
                save(pane, close, feedback,() -> {try{classrooms.guardar(value);}catch(RuntimeException ex){value.setCodigo(oldCode);value.setCapacidad(oldCapacity);value.setType(oldType);throw ex;}});
            }catch(Exception ex){feedback.setText(message(ex));}
        });
    }
    @FXML private void addType(){editType(null);}
    @FXML private void editSelectedType(){ClassroomType selected=typeTable.getSelectionModel().getSelectedItem();if(selected==null){info("Seleccione un tipo.");return;}editType(selected);}
    private void editType(ClassroomType selected) {
        String title = selected == null ? "Agregar tipo de aula" : "Editar tipo de aula";
        TextField name=new TextField(selected==null?"":selected.getName());Label feedback=feedback();VBox content=new VBox(12,new Label("Nombre *"),name,feedback);content.setPadding(new Insets(18));
        Dialogs.form(root, title, content, (pane, close) -> {
            String text=name.getText().trim();if(text.isBlank()||text.length()>60){feedback.setText("Ingrese un nombre de hasta 60 caracteres.");return;}
            ClassroomType value=selected==null?new ClassroomType():selected;String old=value.getName();value.setName(text);
            save(pane, close, feedback,() -> {try{types.guardar(value);}catch(RuntimeException ex){value.setName(old);throw ex;}});
        });
    }
    private void save(DialogPane pane, Runnable close, Label feedback, Runnable operation) {
        pane.setDisable(true);feedback.setText("Guardando…");Thread thread=new Thread(() -> {
            try{operation.run();Platform.runLater(() -> {pane.setDisable(false);close.run();refresh();});}
            catch(Exception ex){Platform.runLater(() -> {pane.setDisable(false);feedback.setText(message(ex));});}
        },"guardar-aulas");thread.setDaemon(true);thread.start();
    }
    @FXML private void deleteClassroom(){Classroom value=classroomTable.getSelectionModel().getSelectedItem();if(value==null){info("Seleccione un aula.");return;}remove("¿Eliminar el aula "+value.getCodigo()+"?",() -> classrooms.eliminar(value));}
    @FXML private void deleteType(){ClassroomType value=typeTable.getSelectionModel().getSelectedItem();if(value==null){info("Seleccione un tipo.");return;}remove("¿Eliminar el tipo "+value.getName()+"?",() -> types.eliminar(value));}
    private void remove(String question,Runnable operation) {
        Dialogs.confirm(root, "Confirmar eliminación", question, () -> {
            root.setDisable(true);
            Thread thread = new Thread(() -> {
                try {
                    operation.run();
                    Platform.runLater(this::refresh);
                } catch (Exception exception) {
                    Platform.runLater(() -> {
                        root.setDisable(false);
                        error(exception);
                    });
                }
            }, "eliminar-aulas");
            thread.setDaemon(true);
            thread.start();
        });
    }
    private GridPane grid(){GridPane grid=new GridPane();grid.setHgap(16);grid.setVgap(14);return grid;}
    private Label feedback(){Label label=new Label("Todos los campos son obligatorios.");label.setWrapText(true);label.setMaxWidth(400);return label;}
    private void info(String text) { Dialogs.info(root, "Aulas y tipos de aula", text); }
    private void error(Exception ex) { Dialogs.error(root, "No se completó la operación", message(ex)); }
    private String message(Exception ex){
        for(Throwable cause=ex;cause!=null;cause=cause.getCause()) {
            if(cause instanceof IllegalArgumentException)return cause.getMessage();
            if(cause instanceof org.hibernate.exception.ConstraintViolationException)return "No se puede completar: el valor está duplicado o el registro tiene referencias. Revise las aulas asociadas.";
            if(cause instanceof org.hibernate.exception.JDBCConnectionException)return "No se pudo conectar a PostgreSQL. Revise el servicio y la configuración local.";
        }
        return "No se completó la operación. Actualice la lista y revise la conexión a PostgreSQL.";
    }
}
