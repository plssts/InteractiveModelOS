package interactivemodelos;

import java.util.Random;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.beans.property.*;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;
import javafx.util.converter.NumberStringConverter;

public class InteractiveModelOS extends Application {
    GridPane gridPane;
    GridPane pane2 = new GridPane();
    Button button2 = new Button();

    @Override
    public void start(Stage primaryStage) {
        BorderPane root = new BorderPane();
        RealMachine rm = new RealMachine();
        gridPane = new GridPane();
        gridPane.gridLinesVisibleProperty().setValue(Boolean.TRUE);
        
        pane2.gridLinesVisibleProperty().setValue(Boolean.TRUE);

        for (int i2=0; i2<13; i2++){
            Label temp = new Label("label");
            temp.textProperty().bind(rm.wordProperty());
            // reikes gettint property is 2d properciu masyvo pagal indeksus
            gridPane.add(temp, i2, 3);
            pane2.add(new Label("label"), i2, 3);
        }

        Button button = new Button();
        button.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent event) {
                System.out.println("HANDLE");
                rm.setWord(String.valueOf(Integer.toHexString(new Random().nextInt(16))) + "e" + String.valueOf(Integer.toHexString(new Random().nextInt(16))) + "d");
            }
        });
        
        button2.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent event) {
                System.out.println("button 2");
            }
        });
        pane2.getChildren().add(button2);
        //gridPane.getChildren().add(pane2);
        gridPane.getChildren().add(button);
        
        //root.setPadding(new Insets(100, 100, 100, 100));
        root.setLeft(gridPane);
        root.setRight(pane2);
        //gridPane.setAlignment(Pos.TOP_LEFT);
        //pane2.setAlignment(Pos.BOTTOM_LEFT);
        
        Scene scene = new Scene(root, 1000, 800);
        primaryStage.setScene(scene);
        primaryStage.setResizable(false);
        primaryStage.show();
    }
}