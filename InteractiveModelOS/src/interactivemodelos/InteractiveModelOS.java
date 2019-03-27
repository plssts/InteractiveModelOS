package interactivemodelos;

import java.io.File;
import java.io.IOException;
import javafx.application.Application;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.HPos;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
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
        System.out.println("Kuriama");
        RealCPU rcpu = new RealCPU(rm.getSharedMemoryTracker());
        System.out.println("Sukurta");
        // Virtualus procesorius
        VirtualCPU vcpu = new VirtualCPU();
        
        // Ivesties ir isvesties irenginiai
        TextField stdout = new TextField();
        stdout.setMinSize(500, 30);
        stdout.setMaxSize(500, 30);
        stdout.setEditable(false);
        Label stdinLabel = new Label("Ivesties irenginys");
        stdinLabel.setTextFill(Color.RED);
        Label stdinStatus = new Label("NEAKTYVUS");
        stdinStatus.setTextFill(Color.DARKRED);
        Label stdoutLabel = new Label("Isvesties irenginys");
        stdoutLabel.setTextFill(Color.RED);
        
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
                    while(vm.executeCommand(vcpu, rcpu, stdinStatus, stdout)){
                        //
                    }
                } catch (NumberFormatException | IOException ex) {
                    System.out.println(ex);
                    //blogas formatavimas
                }
            }
        });
        
        Button step = new Button("Vykdyti zingsni");
        step.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent event) {
                try {
                    vm.executeCommand(vcpu, rcpu, stdinStatus, stdout);
                } catch (NumberFormatException | IOException ex) {
                    System.out.println(ex);
                    //blogas formatavimas
                }
            }
        });
        
        Button reset = new Button("Atstatyti programa");
        reset.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent event) {
                vcpu.setPC(0);
                vcpu.setSF(0);
                vcpu.axProperty().setValue("0");
                vcpu.bxProperty().setValue("0");
                rcpu.tmrProperty().setValue("a");
                stdout.setText("");
            }
        });
        
        Button loadProg = new Button("Uzkrauti programa");
        loadProg.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent event) {
                for (int i = 0; i < 16; ++i){
                    for (int j = 0; j < 16 ; ++j){
                        vm.setWord(i, j, "0");
                    }
                }
                
                char[] temp = rcpu.chProperty().get().toCharArray();
                temp[2] = '1';
                rcpu.chProperty().setValue(String.valueOf(temp));
                
                FileChooser browser = new FileChooser();
                browser.getExtensionFilters().add(new ExtensionFilter("Tekstiniai programos failai", "*.txt"));
                File sourceCode = browser.showOpenDialog(primaryStage);
                
                temp = rcpu.chProperty().get().toCharArray();
                temp[2] = '0';
                rcpu.chProperty().setValue(String.valueOf(temp));
                
                if (sourceCode == null){
                    return;
                }
                vm.loadProgram(sourceCode);
                vcpu.setPC(0);
                vcpu.setSF(0);
                vcpu.axProperty().setValue("0");
                vcpu.bxProperty().setValue("0");
            }
        });
        
        // Realaus procesoriaus pane
        VBox rcpuData = new VBox();
        rcpuData.setStyle("-fx-border-style: solid inside;");
        rcpuData.setSpacing(10);
        rcpuData.setAlignment(Pos.CENTER);
        
        HBox container = new HBox();
        container.setAlignment(Pos.CENTER);
        container.setSpacing(10);
        Label ptr = new Label();    
        ptr.textProperty().bind(rcpu.ptrProperty());
        container.getChildren().add(new Label("PTR"));
        container.getChildren().add(ptr);
        rcpuData.getChildren().add(container);
        
        container = new HBox();
        container.setAlignment(Pos.CENTER);
        container.setSpacing(10);
        Label pc = new Label("PC");    
        pc.textProperty().bind(rcpu.pcProperty());
        container.getChildren().add(new Label("PC"));
        container.getChildren().add(pc);
        rcpuData.getChildren().add(container);
        
        container = new HBox();
        container.setAlignment(Pos.CENTER);
        container.setSpacing(10);
        Label md = new Label("MD");    
        md.textProperty().bind(rcpu.mdProperty());
        container.getChildren().add(new Label("MD"));
        container.getChildren().add(md);
        rcpuData.getChildren().add(container);
        
        container = new HBox();
        container.setAlignment(Pos.CENTER);
        container.setSpacing(10);
        Label tmr = new Label("TMR");    
        tmr.textProperty().bind(rcpu.tmrProperty());
        container.getChildren().add(new Label("TMR"));
        container.getChildren().add(tmr);
        rcpuData.getChildren().add(container);
        
        container = new HBox();
        container.setAlignment(Pos.CENTER);
        container.setSpacing(10);
        Label sf = new Label("SF");    
        sf.textProperty().bind(rcpu.sfProperty());
        container.getChildren().add(new Label("SF"));
        container.getChildren().add(sf);
        rcpuData.getChildren().add(container);
        
        container = new HBox();
        container.setAlignment(Pos.CENTER);
        container.setSpacing(10);
        Label smr = new Label("SMR");    
        smr.textProperty().bind(rcpu.smrProperty());
        container.getChildren().add(new Label("SMR"));
        container.getChildren().add(smr);
        rcpuData.getChildren().add(container);
        
        container = new HBox();
        container.setAlignment(Pos.CENTER);
        container.setSpacing(10);
        Label ax = new Label("AX");    
        ax.textProperty().bind(rcpu.axProperty());
        container.getChildren().add(new Label("AX"));
        container.getChildren().add(ax);
        rcpuData.getChildren().add(container);
        
        container = new HBox();
        container.setAlignment(Pos.CENTER);
        container.setSpacing(10);
        Label bx = new Label("BX");    
        bx.textProperty().bind(rcpu.bxProperty());
        container.getChildren().add(new Label("BX"));
        container.getChildren().add(bx);
        rcpuData.getChildren().add(container);
        
        container = new HBox();
        container.setAlignment(Pos.CENTER);
        container.setSpacing(10);
        Label pi = new Label("PI");    
        pi.textProperty().bind(rcpu.piProperty());
        container.getChildren().add(new Label("PI"));
        container.getChildren().add(pi);
        rcpuData.getChildren().add(container);
        
        container = new HBox();
        container.setAlignment(Pos.CENTER);
        container.setSpacing(10);
        Label si = new Label("SI");    
        si.textProperty().bind(rcpu.siProperty());
        container.getChildren().add(new Label("SI"));
        container.getChildren().add(si);
        rcpuData.getChildren().add(container);
        
        container = new HBox();
        container.setAlignment(Pos.CENTER);
        container.setSpacing(10);
        Label ch = new Label("CH");    
        ch.textProperty().bind(rcpu.chProperty());
        container.getChildren().add(new Label("CH"));
        container.getChildren().add(ch);
        rcpuData.getChildren().add(container);
        
        HBox shmem = new HBox();
        shmem.setStyle("-fx-border-style: solid inside;");
        shmem.setSpacing(10);
        shmem.setAlignment(Pos.CENTER);
        Label shm = new Label();    
        shm.textProperty().bind(rcpu.shmProperty());
        shmem.getChildren().add(new Label("SHM "));
        shmem.getChildren().add(shm);
        
        
        // Virtualaus procesoriaus pane
        VBox vcpuData = new VBox();
        vcpuData.setStyle("-fx-border-style: solid inside;");  
        vcpuData.setSpacing(10);
        
        container = new HBox();
        container.setAlignment(Pos.CENTER);
        container.setSpacing(10);
        Label vpc = new Label();    
        vpc.textProperty().bind(vcpu.pcProperty());
        container.getChildren().add(new Label("PC"));
        container.getChildren().add(vpc);
        vcpuData.getChildren().add(container);
        
        container = new HBox();
        container.setAlignment(Pos.CENTER);
        container.setSpacing(10);
        Label vax = new Label();    
        vax.textProperty().bind(vcpu.axProperty());
        container.getChildren().add(new Label("AX"));
        container.getChildren().add(vax);
        vcpuData.getChildren().add(container);
        
        container = new HBox();
        container.setAlignment(Pos.CENTER);
        container.setSpacing(10);
        Label vbx = new Label();    
        vbx.textProperty().bind(vcpu.bxProperty());
        container.getChildren().add(new Label("BX"));
        container.getChildren().add(vbx);
        vcpuData.getChildren().add(container);
        
        container = new HBox();
        container.setAlignment(Pos.CENTER);
        container.setSpacing(10);
        Label vsf = new Label();    
        vsf.textProperty().bind(vcpu.sfProperty());
        container.getChildren().add(new Label("SF"));
        container.getChildren().add(vsf);
        vcpuData.getChildren().add(container);
        
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
        HBox processors = new HBox();
        processors.setSpacing(10);
        VBox real = new VBox();
        real.getChildren().add(rcpuLabel);
        real.getChildren().add(rcpuData);
        real.getChildren().add(shmem);
        processors.getChildren().add(real);
        VBox virtual = new VBox();
        virtual.getChildren().add(vcpuLabel);
        virtual.getChildren().add(vcpuData);
        processors.getChildren().add(virtual);
        rightOrganized.getChildren().add(processors);

        root.setLeft(leftOrganized);
        root.setRight(rightOrganized);
        
        Scene scene = new Scene(root, 1400, 800);
        primaryStage.setScene(scene);
        primaryStage.setResizable(false);
        primaryStage.show();
    }
}