package interactivemodelos;

import java.io.File;
import java.util.Random;
import javafx.application.Application;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.HPos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.stage.FileChooser;
import javafx.stage.FileChooser.ExtensionFilter;
import javafx.stage.Stage;

public class InteractiveModelOS extends Application {
    GridPane vmMem = new GridPane();
    Button button2 = new Button();

    @Override
    public void start(Stage primaryStage) {
        // Pagrindinis pane, laikantis visus elementus
        BorderPane root = new BorderPane();
        root.setMaxSize(1200, 800);
        root.setMinSize(1200, 800);
        
        // Reali masina
        RealMachine rm = new RealMachine();
        
        // Kairinio pane objektu laikykle
        VBox leftOrganized = new VBox();
        Label rmemLabel = new Label("Reali atmintis");
        rmemLabel.setTextFill(Color.RED);
        leftOrganized.getChildren().add(rmemLabel);
        
        Label wordLabel = new Label(String.format("%5s%8s%9s%8s%8s%8s%8s%8s%8s%8s%9s%8s%8s%8s%8s%8s",
                "0", "1", "2", "3", "4", "5", "6", "7", "8", "9", "a", "b", "c", "d", "e", "f"));
        wordLabel.setTextFill(Color.DARKCYAN);
        leftOrganized.getChildren().add(wordLabel);
        
        // RM atminties pane
        GridPane rmMem = new GridPane();
        rmMem.gridLinesVisibleProperty().setValue(Boolean.TRUE);
        //rmMem.setMaxHeight(1000);
        for (int i = 0; i < 16; ++i){
            ColumnConstraints column = new ColumnConstraints();
            column.setMinWidth(30);
            column.setMaxWidth(30);
            column.setHalignment(HPos.CENTER);
            rmMem.getColumnConstraints().add(column);
        }
        
        vmMem.gridLinesVisibleProperty().setValue(Boolean.TRUE);

        for (int block = 0; block < 64; ++block){
            for (int word = 0; word < 16; ++word){
                Label temp = new Label("label");
                temp.textProperty().bind(rm.wordProperty());
                // reikes gettint property is 2d properciu masyvo pagal indeksus
                rmMem.add(temp, word, block);
                vmMem.add(new Label("label"), word, block);
            }
            Label blockNum = new Label(Integer.toHexString(block));
            blockNum.setTextFill(Color.TEAL);
            rmMem.add(blockNum, 17, block);
        }
        
        Button startProg = new Button("Paleisti programa");
        startProg.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent event) {
                System.out.println("HANDLE");
                rm.setWord(String.valueOf(Integer.toHexString(new Random().nextInt(16))) + "e" + String.valueOf(Integer.toHexString(new Random().nextInt(16))) + "d");
            }
        });
        
        Button loadProg = new Button("Uzkrauti programa");
        loadProg.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent event) {
                FileChooser browser = new FileChooser();
                browser.getExtensionFilters().add(new ExtensionFilter("Tekstiniai programos failai", "*.txt"));
                File sourceCode = browser.showOpenDialog(primaryStage);
                // pass to VM
            }
        });
        
        button2.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent event) {
                System.out.println("button 2");
            }
        });
        vmMem.getChildren().add(button2);
        
        ScrollPane memScroll = new ScrollPane(rmMem);
        memScroll.setContent(rmMem);
        memScroll.setVbarPolicy(ScrollPane.ScrollBarPolicy.ALWAYS);
        //root.setLeft(rmMem);
        leftOrganized.getChildren().add(memScroll);
        leftOrganized.getChildren().add(startProg);
        leftOrganized.getChildren().add(loadProg);
        //StackPane.setAlignment(rmemLabel, Pos.TOP_LEFT);
        //StackPane.setAlignment(memScroll, Pos.CENTER_LEFT);
        //root.setLeft(memScroll);
        root.setLeft(leftOrganized);
        root.setRight(vmMem);
        //root.setCenter(startProg);
        
        Scene scene = new Scene(root, 1200, 800);
        primaryStage.setScene(scene);
        primaryStage.setResizable(false);
        primaryStage.show();
    }
}