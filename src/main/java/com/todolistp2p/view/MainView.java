package com.todolistp2p.view;

import com.todolistp2p.controller.MainController;
import com.todolistp2p.view.components.NotificationPopup;
import com.todolistp2p.model.Project;
import com.todolistp2p.model.Task;
import com.todolistp2p.model.TodoStateContainer;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

import java.io.IOException;

public class MainView {
    private final MainController controller;
    private final BorderPane root = new BorderPane();

    private final ListView<Project> projectList = new ListView<>();
    private final ListView<String> notifications = new ListView<>();
    private final ListView<String> peersList = new ListView<>();
    private final TableView<Task> taskTable = new TableView<>();

    public MainView(MainController controller) {
        this.controller = controller;
        // ensure UI updates happen on JavaFX thread
        controller.setOnStateChanged(v -> javafx.application.Platform.runLater(this::refresh));
        buildUi();
        refresh();
    }

    public BorderPane root() { return root; }

    private void buildUi() {
        // top toolbar
        Button newProj = new Button("Nouveau Projet");
        Button newTask = new Button("Nouvelle Tâche");
        Button sync = new Button("Synchroniser");
        HBox toolbar = new HBox(8, newProj, newTask, sync);
        toolbar.setPadding(new Insets(8));
        root.setTop(toolbar);

        // left: projects
        projectList.setCellFactory(lv -> new ListCell<>(){
            private final HBox box = new HBox(8);
            private final Label nameLabel = new Label();
            private final Button delBtn = new Button("Supprimer");

            {
                box.getChildren().addAll(nameLabel, delBtn);
                delBtn.setOnAction(e -> {
                    Project p = getItem();
                    if (p==null) return;
                    Alert a = new Alert(Alert.AlertType.CONFIRMATION, "Supprimer le projet '" + p.getName() + "' ?", ButtonType.OK, ButtonType.CANCEL);
                    a.setHeaderText("Confirmation");
                    a.showAndWait().ifPresent(bt -> {
                        if (bt == ButtonType.OK) {
                            try { controller.deleteProject(p.getId()); } catch (IOException ex) { ex.printStackTrace(); new Alert(Alert.AlertType.ERROR, "Erreur lors de la suppression").show(); }
                        }
                    });
                });
            }

            @Override protected void updateItem(Project item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item==null) {
                    setGraphic(null);
                    setText(null);
                } else {
                    nameLabel.setText(item.getName());
                    setGraphic(box);
                    setText(null);
                }
            }
        });
        projectList.getSelectionModel().selectedItemProperty().addListener((obs,oldV,newV)-> showTasksFor(newV));
        root.setLeft(projectList);

        // center: tasks table simple
        TableColumn<Task,String> titleCol = new TableColumn<>("Titre");
        titleCol.setCellValueFactory(c -> new javafx.beans.property.SimpleStringProperty(c.getValue().getTitle()));
        TableColumn<Task,String> stateCol = new TableColumn<>("État");
        stateCol.setCellValueFactory(c -> new javafx.beans.property.SimpleStringProperty(c.getValue().getState().name()));
        // custom cell with three checkboxes: À faire / En cours / Fait
        stateCol.setCellFactory(col -> new TableCell<Task,String>() {
            private final CheckBox cbTodo = new CheckBox("À faire");
            private final CheckBox cbInprog = new CheckBox("En cours");
            private final CheckBox cbDone = new CheckBox("Fait");
            private final HBox box = new HBox(8, cbTodo, cbInprog, cbDone);

            private void updateSelectionFromState(Task t) {
                if (t==null) {
                    cbTodo.setSelected(false);
                    cbInprog.setSelected(false);
                    cbDone.setSelected(false);
                } else {
                    String s = t.getState()==null ? "" : t.getState().name();
                    cbTodo.setSelected("TODO".equals(s));
                    cbInprog.setSelected("IN_PROGRESS".equals(s));
                    cbDone.setSelected("DONE".equals(s));
                }
            }

            private void changeState(Task t, String newState) {
                Project p = projectList.getSelectionModel().getSelectedItem();
                if (p==null || t==null) return;
                try { controller.updateTaskState(p.getId(), t.getId(), newState); } catch (IOException ex) { ex.printStackTrace(); }
            }

            {
                cbTodo.setOnAction(e -> {
                    Task t = getTableRow()==null ? null : getTableRow().getItem();
                    if (cbTodo.isSelected()) {
                        cbInprog.setSelected(false);
                        cbDone.setSelected(false);
                        changeState(t, "TODO");
                    } else {
                        if (!cbInprog.isSelected() && !cbDone.isSelected()) cbTodo.setSelected(true);
                    }
                });
                cbInprog.setOnAction(e -> {
                    Task t = getTableRow()==null ? null : getTableRow().getItem();
                    if (cbInprog.isSelected()) {
                        cbTodo.setSelected(false);
                        cbDone.setSelected(false);
                        changeState(t, "IN_PROGRESS");
                    } else {
                        if (!cbTodo.isSelected() && !cbDone.isSelected()) cbInprog.setSelected(true);
                    }
                });
                cbDone.setOnAction(e -> {
                    Task t = getTableRow()==null ? null : getTableRow().getItem();
                    if (cbDone.isSelected()) {
                        cbTodo.setSelected(false);
                        cbInprog.setSelected(false);
                        changeState(t, "DONE");
                    } else {
                        if (!cbTodo.isSelected() && !cbInprog.isSelected()) cbDone.setSelected(true);
                    }
                });
            }

            @Override protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || getIndex()<0 || getIndex()>=getTableView().getItems().size()) {
                    setGraphic(null);
                } else {
                    Task t = getTableView().getItems().get(getIndex());
                    updateSelectionFromState(t);
                    setGraphic(box);
                }
            }
        });
        // add delete column for tasks
        TableColumn<Task, Void> deleteCol = new TableColumn<>("Suppr.");
        deleteCol.setCellFactory(col -> new TableCell<Task, Void>() {
            private final Button del = new Button("Supprimer");
            {
                del.setOnAction(e -> {
                    Task t = getTableRow()==null ? null : getTableRow().getItem();
                    Project p = projectList.getSelectionModel().getSelectedItem();
                    if (t==null || p==null) return;
                    Alert a = new Alert(Alert.AlertType.CONFIRMATION, "Supprimer la tâche '" + t.getTitle() + "' ?", ButtonType.OK, ButtonType.CANCEL);
                    a.setHeaderText("Confirmation");
                    a.showAndWait().ifPresent(bt -> {
                        if (bt == ButtonType.OK) {
                            try { controller.deleteTask(p.getId(), t.getId()); } catch (IOException ex) { ex.printStackTrace(); new Alert(Alert.AlertType.ERROR, "Erreur lors de la suppression").show(); }
                        }
                    });
                });
            }

            @Override protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || getTableRow()==null || getTableRow().getItem()==null) setGraphic(null);
                else setGraphic(del);
            }
        });

        root.setCenter(taskTable);

        // right: notifications
    // right: notifications + bottom peers
    VBox right = new VBox(new Label("Notifications"), notifications);
    right.setPadding(new Insets(8));
    right.setPrefWidth(300);
    // bottom: peers list
    VBox bottomBox = new VBox(new Label("Pairs connectés"), peersList);
    bottomBox.setPadding(new Insets(8));
    bottomBox.setPrefHeight(120);
    BorderPane rightPane = new BorderPane();
    rightPane.setCenter(right);
    rightPane.setBottom(bottomBox);
    root.setRight(rightPane);

        HBox.setHgrow(projectList, Priority.ALWAYS);

        // actions
        newProj.setOnAction(e -> {
            TextInputDialog d = new TextInputDialog();
            d.setHeaderText("Nom du projet");
            d.showAndWait().ifPresent(name -> {
                try { controller.createProject(name); } catch (IOException ex) { ex.printStackTrace(); }
            });
        });

        newTask.setOnAction(e -> {
            Project p = projectList.getSelectionModel().getSelectedItem();
            if (p==null) { new Alert(Alert.AlertType.WARNING, "Veuillez sélectionner un projet").show(); return; }
            Dialog<Task> dlg = new Dialog<>();
            dlg.setTitle("Nouvelle tâche");
            TextField title = new TextField();
            TextArea desc = new TextArea();
            dlg.getDialogPane().setContent(new VBox(new Label("Titre"), title, new Label("Description"), desc));
            dlg.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
            dlg.setResultConverter(bt -> bt==ButtonType.OK ? new Task(null, title.getText(), desc.getText(), System.currentTimeMillis()) : null);
            dlg.showAndWait().ifPresent(t -> {
                try { controller.createTask(p.getId(), t.getTitle(), t.getDescription()); } catch (IOException ex) { ex.printStackTrace(); }
            });
        });

        sync.setOnAction(e -> refresh());
        sync.setOnAction(e -> {
            refresh();
            try { controller.syncNow(); } catch (Exception ex) { ex.printStackTrace(); }
        });

        // register listeners from controller
        controller.addNotificationListener(msg -> javafx.application.Platform.runLater(() -> {
            notifications.getItems().add(0, msg);
            try { NotificationPopup.show(msg); } catch (Exception ignored) {}
        }));

        controller.addPeersListener(set -> javafx.application.Platform.runLater(() -> {
            peersList.getItems().setAll(set);
        }));
    taskTable.getColumns().add(titleCol);
    taskTable.getColumns().add(stateCol);
    taskTable.getColumns().add(deleteCol);
        // context menu for changing task state
        taskTable.setRowFactory(tv -> {
            TableRow<Task> row = new TableRow<>();
            final ContextMenu rowMenu = new ContextMenu();
            MenuItem todo = new MenuItem("À faire");
            MenuItem inprog = new MenuItem("En cours");
            MenuItem done = new MenuItem("Fait");
            rowMenu.getItems().addAll(todo, inprog, done);
            todo.setOnAction(ev -> changeState(row.getItem(), "TODO"));
            inprog.setOnAction(ev -> changeState(row.getItem(), "IN_PROGRESS"));
            done.setOnAction(ev -> changeState(row.getItem(), "DONE"));
            row.contextMenuProperty().bind(javafx.beans.binding.Bindings.when(row.emptyProperty()).then((ContextMenu)null).otherwise(rowMenu));
            return row;
        });
    }

    private void showTasksFor(Project p) {
        if (p==null) { taskTable.setItems(FXCollections.observableArrayList()); return; }
        ObservableList<Task> items = FXCollections.observableArrayList(p.getTasks());
        taskTable.setItems(items);
    }

    public void refresh() {
        TodoStateContainer state = controller.getState();
        if (state==null) return;
        ObservableList<Project> projs = FXCollections.observableArrayList(state.getProjects());
        projectList.setItems(projs);
    }

    private void changeState(Task t, String stateName) {
        Project p = projectList.getSelectionModel().getSelectedItem();
        if (p==null || t==null) return;
        try { controller.updateTaskState(p.getId(), t.getId(), stateName); } catch (IOException ex) { ex.printStackTrace(); }
    }
}
