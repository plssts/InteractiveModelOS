package interactivemodelos;

import java.awt.event.ActionListener;
import java.io.File;
import java.io.IOException;
import java.util.Random;
import java.util.logging.Level;
import java.util.logging.Logger;
import javafx.application.Application;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.event.EventType;
import javafx.geometry.HPos;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.DialogPane;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.control.TextInputDialog;
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
        
        // Ivesties ir isvesties irenginiai
        //HBox Idevice = new HBox();
        //TextField stdin = new TextField();
        //TextInputDialog dialog = new TextInputDialog();
        //System.out.println(dialog.getEditor().getProperties().keySet());
        TextField stdout = new TextField();
        //stdin.setMinSize(500, 30);
        //stdin.setMaxSize(500, 30);
        stdout.setMinSize(500, 30);
        stdout.setMaxSize(500, 30);
        stdout.setEditable(false);
        Label stdinLabel = new Label("Ivesties irenginys");
        stdinLabel.setTextFill(Color.RED);
        Label stdinStatus = new Label("NEAKTYVUS");
        stdinStatus.setTextFill(Color.DARKRED);
        Label stdoutLabel = new Label("Isvesties irenginys");
        stdoutLabel.setTextFill(Color.RED);
        //Button input = new Button("Ivesti");
        //input.setDisable(true);
        //Idevice.getChildren().add(stdin);
        //Idevice.getChildren().add(dialog.getEditor());
        //Idevice.getChildren().add(input);
        
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
                try {
                    while(vm.executeCommand(vcpu, stdinStatus, stdout)){
                        //
                    }
                } catch (IOException ex) {
                    //blogas formatavimas
                }
            }
        });
        
        Button step = new Button("Vykdyti zingsni");
        step.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent event) {
                try {
                    vm.executeCommand(vcpu, stdinStatus, stdout);
                } catch (IOException ex) {
                    //blogas formatavimas
                }
            }
        });
        
        Button reset = new Button("Atstatyti programa");
        reset.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent event) {
                vcpu.setPC(0);
                stdout.setText("");
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
        rcpuData.setSpacing(10);
        Label ptr = new Label();    
        ptr.textProperty().bind(rcpu.ptrProperty());
        rcpuData.getChildren().add(new Label("PTR "));
        rcpuData.getChildren().add(ptr);
        Label pc = new Label("PC");    
        pc.textProperty().bind(rcpu.pcProperty());
        rcpuData.getChildren().add(new Label("| PC "));
        rcpuData.getChildren().add(pc);
        
        // Virtualaus procesoriaus pane
        HBox vcpuData = new HBox();
        vcpuData.setStyle("-fx-border-style: solid inside;");  
        vcpuData.setSpacing(10);
        Label vpc = new Label();    
        vpc.textProperty().bind(vcpu.pcProperty());
        vcpuData.getChildren().add(new Label("PC "));
        vcpuData.getChildren().add(vpc);
        Label vax = new Label();    
        vax.textProperty().bind(vcpu.axProperty());
        vcpuData.getChildren().add(new Label("| AX "));
        vcpuData.getChildren().add(vax);
        Label vbx = new Label();    
        vbx.textProperty().bind(vcpu.bxProperty());
        vcpuData.getChildren().add(new Label("| BX "));
        vcpuData.getChildren().add(vbx);
        Label vsf = new Label();    
        vsf.textProperty().bind(vcpu.sfProperty());
        vcpuData.getChildren().add(new Label("| SF "));
        vcpuData.getChildren().add(vsf);
        
        // Kairinio pane objektu laikykle
        VBox leftOrganized = new VBox();
        Label rmemLabel = new Label("Reali atmintis");
        rmemLabel.setTextFill(Color.RED);
        leftOrganized.getChildren().add(rmemLabel);
        
        Label wordLabel = new Label(String.format("%9s%17s%17s%17s%16s%17s%17s%16s%16s%17s%17s%17s%17s%16s%17s%17s",
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
        wordLabel = new Label(String.format("%9s%17s%17s%17s%16s%17s%17s%16s%16s%17s%17s%17s%17s%16s%17s%17s",
                "0", "1", "2", "3", "4", "5", "6", "7", "8", "9", "a", "b", "c", "d", "e", "f"));
        wordLabel.setTextFill(Color.DARKCYAN);
        leftOrganized.getChildren().add(wordLabel);
        leftOrganized.getChildren().add(vmemScroll);
        leftOrganized.getChildren().add(stdinLabel);
        leftOrganized.getChildren().add(stdinStatus);
        leftOrganized.getChildren().add(stdoutLabel);
        leftOrganized.getChildren().add(stdout);
        
        // Desininio pane objektu laikykle
        VBox rightOrganized = new VBox();
        
        Label rcpuLabel = new Label("Realus procesorius");
        rcpuLabel.setTextFill(Color.RED);
        Label vcpuLabel = new Label("Virtualus procesorius");
        vcpuLabel.setTextFill(Color.RED);

        rightOrganized.getChildren().add(startProg);
        rightOrganized.getChildren().add(step);
        rightOrganized.getChildren().add(loadProg);
        rightOrganized.getChildren().add(reset);
        rightOrganized.getChildren().add(rcpuLabel);
        rightOrganized.getChildren().add(rcpuData);
        rightOrganized.getChildren().add(vcpuLabel);
        rightOrganized.getChildren().add(vcpuData);

        root.setLeft(leftOrganized);
        root.setRight(rightOrganized);
        
        Scene scene = new Scene(root, 1400, 800);
        primaryStage.setScene(scene);
        primaryStage.setResizable(false);
        primaryStage.show();
    }
}