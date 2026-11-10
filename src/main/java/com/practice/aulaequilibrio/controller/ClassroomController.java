package com.practice.aulaequilibrio.controller;
import com.practice.aulaequilibrio.dao.*;
import com.practice.aulaequilibrio.model.*;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.event.ActionEvent;
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
    @FXML private ComboBox<ClassroomType> typeFilter;
    @FXML private Label status;
    private final ClassroomDAO classrooms=new ClassroomDAO();
    private final ClassroomTypeDAO types=new ClassroomTypeDAO();
    private boolean refreshing;
    @FXML private void initialize() {
        classroomTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);typeTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        codeColumn.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getCodigo()));
        capacityColumn.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getCapacidad().toString()));
        classroomTypeColumn.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getType().getName()));
        typeNameColumn.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getName()));
        classroomTable.setPlaceholder(new Label("Agregue un aula para comenzar."));typeTable.setPlaceholder(new Label("Agregue primero un tipo, por ejemplo Laboratorio."));refresh();
    }
    @FXML private void refresh() {
        root.setDisable(true);status.setText("Cargando…");
        Thread thread=new Thread(() -> {
            try {
                List<Classroom> rooms=classrooms.listar();List<ClassroomType> categories=types.listar();
                Platform.runLater(() -> {
                    refreshing=true;classroomTable.setItems(FXCollections.observableArrayList(rooms));typeTable.setItems(FXCollections.observableArrayList(categories));
                    typeFilter.getItems().clear();typeFilter.getItems().add(null);typeFilter.getItems().addAll(categories);typeFilter.setValue(null);refreshing=false;
                    root.setDisable(false);status.setText(rooms.size()+" aulas · "+categories.size()+" tipos");
                });
            }catch(Exception ex){Platform.runLater(() -> {root.setDisable(false);status.setText("No se pudieron cargar los datos.");error(ex);});}
        },"consultar-aulas");thread.setDaemon(true);thread.start();
    }
    @FXML private void filter() {
        if(refreshing)return;ClassroomType selected=typeFilter.getValue();root.setDisable(true);
        Thread thread=new Thread(() -> {
            try {List<Classroom> rooms=selected==null?classrooms.listar():classrooms.listarPorTipo(selected.getId());Platform.runLater(() -> {classroomTable.getItems().setAll(rooms);root.setDisable(false);status.setText(rooms.size()+" aulas encontradas");});}
            catch(Exception ex){Platform.runLater(() -> {root.setDisable(false);error(ex);});}
        },"filtrar-aulas");thread.setDaemon(true);thread.start();
    }
    @FXML private void addClassroom(){editClassroom(null);}
    @FXML private void editSelectedClassroom(){Classroom selected=classroomTable.getSelectionModel().getSelectedItem();if(selected==null){info("Seleccione un aula.");return;}editClassroom(selected);}
    private void editClassroom(Classroom selected) {
        if(typeTable.getItems().isEmpty()){info("Agregue primero un tipo de aula en la pestaña Tipos de aula.");return;}
        Dialog<Void> dialog=dialog(selected==null?"Agregar aula":"Editar aula");
        TextField code=new TextField(selected==null?"":selected.getCodigo());TextField capacity=new TextField(selected==null?"":selected.getCapacidad().toString());
        ComboBox<ClassroomType> type=new ComboBox<>(FXCollections.observableArrayList(typeTable.getItems()));type.setPromptText("Seleccione un tipo");type.setMaxWidth(Double.MAX_VALUE);
        if(selected!=null)for(ClassroomType option:type.getItems())if(option.getId().equals(selected.getType().getId()))type.setValue(option);
        GridPane grid=grid();grid.addRow(0,new Label("Código *"),code);grid.addRow(1,new Label("Capacidad *"),capacity);grid.addRow(2,new Label("Tipo de aula *"),type);
        Label feedback=feedback();VBox content=new VBox(16,grid,feedback);content.setPadding(new Insets(18));dialog.getDialogPane().setContent(content);
        dialog.getDialogPane().lookupButton(ButtonType.OK).addEventFilter(ActionEvent.ACTION,event -> {
            event.consume();try {
                String normalized=code.getText().trim();if(normalized.isBlank()||normalized.length()>20)throw new IllegalArgumentException("Ingrese un código de hasta 20 caracteres.");
                int count;try{count=Integer.parseInt(capacity.getText().trim());}catch(NumberFormatException ex){throw new IllegalArgumentException("La capacidad debe ser un número entero mayor que cero.");}
                if(count<=0)throw new IllegalArgumentException("La capacidad debe ser mayor que cero.");if(type.getValue()==null)throw new IllegalArgumentException("Seleccione un tipo de aula.");
                // La entidad seleccionada conserva su id y revisión; no se clona una identidad falsa.
                Classroom value=selected==null?new Classroom():selected;String oldCode=value.getCodigo();Integer oldCapacity=value.getCapacidad();ClassroomType oldType=value.getType();
                value.setCodigo(normalized);value.setCapacidad(count);value.setType(type.getValue());
                save(dialog,feedback,() -> {try{classrooms.guardar(value);}catch(RuntimeException ex){value.setCodigo(oldCode);value.setCapacidad(oldCapacity);value.setType(oldType);throw ex;}});
            }catch(Exception ex){feedback.setText(message(ex));}
        });dialog.showAndWait();
    }
    @FXML private void addType(){editType(null);}
    @FXML private void editSelectedType(){ClassroomType selected=typeTable.getSelectionModel().getSelectedItem();if(selected==null){info("Seleccione un tipo.");return;}editType(selected);}
    private void editType(ClassroomType selected) {
        Dialog<Void> dialog=dialog(selected==null?"Agregar tipo de aula":"Editar tipo de aula");TextField name=new TextField(selected==null?"":selected.getName());Label feedback=feedback();VBox content=new VBox(12,new Label("Nombre *"),name,feedback);content.setPadding(new Insets(18));dialog.getDialogPane().setContent(content);
        dialog.getDialogPane().lookupButton(ButtonType.OK).addEventFilter(ActionEvent.ACTION,event -> {
            event.consume();String text=name.getText().trim();if(text.isBlank()||text.length()>60){feedback.setText("Ingrese un nombre de hasta 60 caracteres.");return;}
            ClassroomType value=selected==null?new ClassroomType():selected;String old=value.getName();value.setName(text);
            save(dialog,feedback,() -> {try{types.guardar(value);}catch(RuntimeException ex){value.setName(old);throw ex;}});
        });dialog.showAndWait();
    }
    private void save(Dialog<Void> dialog,Label feedback,Runnable operation) {
        dialog.getDialogPane().setDisable(true);feedback.setText("Guardando…");Thread thread=new Thread(() -> {
            try{operation.run();Platform.runLater(() -> {dialog.getDialogPane().setDisable(false);dialog.close();refresh();});}
            catch(Exception ex){Platform.runLater(() -> {dialog.getDialogPane().setDisable(false);feedback.setText(message(ex));});}
        },"guardar-aulas");thread.setDaemon(true);thread.start();
    }
    @FXML private void deleteClassroom(){Classroom value=classroomTable.getSelectionModel().getSelectedItem();if(value==null){info("Seleccione un aula.");return;}remove("¿Eliminar el aula "+value.getCodigo()+"?",() -> classrooms.eliminar(value));}
    @FXML private void deleteType(){ClassroomType value=typeTable.getSelectionModel().getSelectedItem();if(value==null){info("Seleccione un tipo.");return;}remove("¿Eliminar el tipo "+value.getName()+"?",() -> types.eliminar(value));}
    private void remove(String question,Runnable operation) {
        Alert confirm=new Alert(Alert.AlertType.CONFIRMATION,question,ButtonType.OK,ButtonType.CANCEL);confirm.initOwner(root.getScene().getWindow());confirm.setHeaderText("Confirmar eliminación");if(confirm.showAndWait().orElse(ButtonType.CANCEL)!=ButtonType.OK)return;
        root.setDisable(true);Thread thread=new Thread(() -> {try{operation.run();Platform.runLater(this::refresh);}catch(Exception ex){Platform.runLater(() -> {root.setDisable(false);error(ex);});}},"eliminar-aulas");thread.setDaemon(true);thread.start();
    }
    private Dialog<Void> dialog(String title){Dialog<Void> dialog=new Dialog<>();dialog.initOwner(root.getScene().getWindow());dialog.setTitle(title);dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK,ButtonType.CANCEL);((Button)dialog.getDialogPane().lookupButton(ButtonType.OK)).setText("Guardar");dialog.getDialogPane().setPrefWidth(460);return dialog;}
    private GridPane grid(){GridPane grid=new GridPane();grid.setHgap(16);grid.setVgap(14);return grid;}
    private Label feedback(){Label label=new Label("Todos los campos son obligatorios.");label.setWrapText(true);label.setMaxWidth(400);return label;}
    private void info(String text){Alert alert=new Alert(Alert.AlertType.INFORMATION,text,ButtonType.OK);alert.setHeaderText(null);alert.initOwner(root.getScene().getWindow());alert.showAndWait();}
    private void error(Exception ex){Alert alert=new Alert(Alert.AlertType.ERROR,message(ex),ButtonType.OK);alert.setHeaderText("No se completó la operación");alert.initOwner(root.getScene().getWindow());alert.showAndWait();}
    private String message(Exception ex){
        for(Throwable cause=ex;cause!=null;cause=cause.getCause()) {
            if(cause instanceof IllegalArgumentException)return cause.getMessage();
            if(cause instanceof org.hibernate.exception.ConstraintViolationException)return "No se puede completar: el valor está duplicado o el registro tiene referencias. Revise las aulas asociadas.";
            if(cause instanceof org.hibernate.exception.JDBCConnectionException)return "No se pudo conectar a PostgreSQL. Revise el servicio y la configuración local.";
        }
        return "No se completó la operación. Actualice la lista y revise la conexión a PostgreSQL.";
    }
}
