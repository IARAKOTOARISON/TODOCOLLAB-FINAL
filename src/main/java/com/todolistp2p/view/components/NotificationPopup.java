package com.todolistp2p.view.components;

import javafx.animation.FadeTransition;
import javafx.animation.PauseTransition;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Rectangle2D;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.stage.Screen;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.util.Duration;

public class NotificationPopup {
    /**
     * Show a small transient popup notification on the screen.
     */
    public static void show(String message) {
        Platform.runLater(() -> {
            try {
                Stage stage = new Stage();
                stage.initStyle(StageStyle.TRANSPARENT);
                stage.setAlwaysOnTop(true);

                Label lbl = new Label(message);
                lbl.setStyle("-fx-background-color: rgba(0,0,0,0.8); -fx-text-fill: white; -fx-padding: 8px; -fx-background-radius: 6;");

                StackPane root = new StackPane(lbl);
                root.setPadding(new Insets(6));

                Scene scene = new Scene(root);
                scene.setFill(Color.TRANSPARENT);
                stage.setScene(scene);

                // place near top-right of primary screen
                Rectangle2D bounds = Screen.getPrimary().getVisualBounds();
                double width = 300;
                stage.setWidth(width);
                stage.setX(bounds.getMaxX() - width - 20);
                stage.setY(bounds.getMinY() + 40);

                // show and animate
                stage.show();

                FadeTransition ftIn = new FadeTransition(Duration.millis(200), root);
                ftIn.setFromValue(0.0);
                ftIn.setToValue(1.0);
                ftIn.play();

                PauseTransition wait = new PauseTransition(Duration.seconds(4));
                wait.setOnFinished(e -> {
                    FadeTransition ftOut = new FadeTransition(Duration.millis(350), root);
                    ftOut.setFromValue(1.0);
                    ftOut.setToValue(0.0);
                    ftOut.setOnFinished(ev -> stage.close());
                    ftOut.play();
                });
                wait.play();
            } catch (Throwable t) {
                // If anything goes wrong showing a popup, ignore to avoid crashing the UI thread
            }
        });
    }
}
