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
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.stage.FileChooser;
import javafx.stage.FileChooser.ExtensionFilter;
import javafx.stage.Stage;

public class InteractiveModelOS extends Application {
    Button button2 = new Button();

    @Override
    public void start(Stage primaryStage) {
        // Pagrindinis pane, laikantis visus elementus
        BorderPane root = new BorderPane();
        root.setMaxSize(1400, 800);
        root.setMinSize(1400, 800);
        
        // Reali masina
        RealMachine rm = new RealMachine();
        // Virtuali masina
        VirtualMachine vm = new VirtualMachine();
        // Realus procesorius
        RealCPU rcpu = new RealCPU();
        // Virtualus procesorius
        VirtualCPU vcpu = new VirtualCPU();
        
        // Virtualios masinos uzkurimas
        System.out.println("Kuriama VM");
        rm.loadVirtualMachine(rcpu, vm);
        
        // RM atminties pane
        GridPane rmMem = new GridPane();
        rmMem.gridLinesVisibleProperty().setValue(Boolean.TRUE);
        
        // VM atminties pane
        GridPane vmMem = new GridPane();
        vmMem.gridLinesVisibleProperty().setValue(Boolean.TRUE);
        
        // Nustatymai rmMem ir vmMem stulpeliams
        for (int i = 0; i < 16; ++i){
            ColumnConstraints column = new ColumnConstraints();
            column.setMinWidth(58);
            column.setMaxWidth(58);
            column.setHalignment(HPos.CENTER);
            rmMem.getColumnConstraints().add(column);
            vmMem.getColumnConstraints().add(column);
        }

        // Realios atminties zodziai
        for (int block = 0; block < 64; ++block){
            for (int word = 0; word < 16; ++word){
                Label temp = new Label("label");
                        
                temp.textProperty().bind(rm.memoryProperty(block, word));
                rmMem.add(temp, word, block);
            }
            
            // Papildomas stulpelis bloko numeriams
            Label blockNum = new Label(Integer.toHexString(block));
            blockNum.setTextFill(Color.TEAL);
            rmMem.add(blockNum, 17, block);
        }
        
        // Virtualios atminties zodziai
        for (int block = 0; block < 16; ++block){
            for (int word = 0; word < 16; ++word){
                Label temp = new Label("label");
                        
                temp.textProperty().bind(vm.memoryProperty(block, word));
                vmMem.add(temp, word, block);
            }
            
            // Papildomas stulpelis bloko numeriams
            Label blockNum = new Label(Integer.toHexString(block));
            blockNum.setTextFill(Color.TEAL);
            vmMem.add(blockNum, 17, block);
        }
        
        Button startProg = new Button("Paleisti programa");
        startProg.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent event) {
                rm.setWord(0,0,String.valueOf(Integer.toHexString(new Random().nextInt(16))) + "e15" + String.valueOf(Integer.toHexString(new Random().nextInt(16))) + "57d");
            }
        });
        
        Button loadProg = new Button("Uzkrauti programa");
        loadProg.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent event) {
                FileChooser browser = new FileChooser();
                browser.getExtensionFilters().add(new ExtensionFilter("Tekstiniai programos failai", "*.txt"));
                File sourceCode = browser.showOpenDialog(primaryStage);
                if (sourceCode == null){
                    return;
                }
                vm.loadProgram(sourceCode);
            }
        });
        
        // Realaus procesoriaus pane
        HBox rcpuData = new HBox();
        rcpuData.setStyle("-fx-border-style: solid inside;");
        Label ptr = new Label("PTR");    
        ptr.textProperty().bind(rcpu.ptrProperty());
        rcpuData.getChildren().add(new Label("PTR "));
        rcpuData.getChildren().add(ptr);
        
        // Kairinio pane objektu laikykle
        VBox leftOrganized = new VBox();
        Label rmemLabel = new Label("Reali atmintis");
        rmemLabel.setTextFill(Color.RED);
        leftOrganized.getChildren().add(rmemLabel);
        
        Label wordLabel = new Label(String.format("%5s%8s%9s%8s%8s%8s%8s%8s%8s%8s%9s%8s%8s%8s%8s%8s",
                "0", "1", "2", "3", "4", "5", "6", "7", "8", "9", "a", "b", "c", "d", "e", "f"));
        wordLabel.setTextFill(Color.DARKCYAN);
        leftOrganized.getChildren().add(wordLabel);
        
        // Scrollable reali atmintis
        ScrollPane rmemScroll = new ScrollPane(rmMem);
        rmemScroll.setMaxHeight(450);
        rmemScroll.setMinHeight(450);
        
        // Scrollable virtuali atmintis
        ScrollPane vmemScroll = new ScrollPane(vmMem);
        vmemScroll.setMaxHeight(150);
        vmemScroll.setMinHeight(150);
        
        rmemScroll.setContent(rmMem);
        vmemScroll.setContent(vmMem);
        rmemScroll.setVbarPolicy(ScrollPane.ScrollBarPolicy.ALWAYS);
        vmemScroll.setVbarPolicy(ScrollPane.ScrollBarPolicy.ALWAYS);
        
        leftOrganized.getChildren().add(rmemScroll);
        Label vmemLabel = new Label("Virtuali atmintis");
        vmemLabel.setTextFill(Color.RED);
        leftOrganized.getChildren().add(vmemLabel);
        
        // Reikia naujo objekto, kitaip duplicate child exception
        wordLabel = new Label(String.format("%5s%8s%9s%8s%8s%8s%8s%8s%8s%8s%9s%8s%8s%8s%8s%8s",
                "0", "1", "2", "3", "4", "5", "6", "7", "8", "9", "a", "b", "c", "d", "e", "f"));
        wordLabel.setTextFill(Color.DARKCYAN);
        leftOrganized.getChildren().add(wordLabel);
        leftOrganized.getChildren().add(vmemScroll);
        
        // Desininio pane objektu laikykle
        VBox rightOrganized = new VBox();
        
        Label rcpuLabel = new Label("Realus procesorius");
        rcpuLabel.setTextFill(Color.RED);

        rightOrganized.getChildren().add(startProg);
        rightOrganized.getChildren().add(loadProg);
        rightOrganized.getChildren().add(rcpuLabel);
        rightOrganized.getChildren().add(rcpuData);

        root.setLeft(leftOrganized);
        root.setRight(rightOrganized);
        
        Scene scene = new Scene(root, 1400, 800);
        primaryStage.setScene(scene);
        primaryStage.setResizable(false);
        primaryStage.show();
    }
}