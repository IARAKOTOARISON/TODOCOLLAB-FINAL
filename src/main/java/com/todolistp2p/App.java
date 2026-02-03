package com.todolistp2p;

import com.todolistp2p.controller.MainController;
import com.todolistp2p.view.MainView;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class App extends Application {
    @Override
    public void start(Stage primaryStage) throws Exception {
        MainController controller = new MainController();
        MainView view = new MainView(controller);

        primaryStage.setTitle("todolist-p2p");
        primaryStage.setScene(new Scene(view.root(), 900, 600));
        primaryStage.show();
        controller.start();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
