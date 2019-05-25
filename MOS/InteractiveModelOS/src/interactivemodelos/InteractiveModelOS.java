/*
Holds the GUI. Also starts the JavaFX main stage.
*/
package interactivemodelos;

import java.io.File;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.HPos;
import javafx.geometry.Orientation;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.TextArea;
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
import javafx.stage.StageStyle;
import javafx.stage.WindowEvent;
import javafx.util.Pair;
import process.StartStop;
import process.VM;

/**
 * @author Paulius Staisiunas, Computer Science 3 yr., 3 gr.
 */

public class InteractiveModelOS extends Application {
    @Override
    public void start(Stage primaryStage) {
        // Main pane, fixed size
        BorderPane root = new BorderPane();
        root.setMaxSize(1200, 800);
        root.setMinSize(1200, 800);
        
        // Real machine
        RealMachine rm = new RealMachine();
        // Virtual machine master view
        VirtualMachine vmMaster = new VirtualMachine();
        // Real CPU
        RealCPU rcpu = new RealCPU(rm.getSharedMemoryTracker());
        // Virtual CPU master view
        VirtualCPU vcpuMaster = new VirtualCPU();
        
        Scheduler sch = new Scheduler();
        
        // Input/ouput 'devices'
        TextField stdout = new TextField();
        stdout.setMinSize(500, 30);
        stdout.setMaxSize(500, 30);
        stdout.setEditable(false);
        Label stdinLabel = new Label("Input device");
        stdinLabel.setTextFill(Color.RED);
        Label stdinStatus = new Label("OFFLINE");
        stdinStatus.setTextFill(Color.DARKRED);
        Label stdoutLabel = new Label("Output device");
        stdoutLabel.setTextFill(Color.RED);
        
        //rm.loadVirtualMachine(rcpu, vm);
        
        // RM memory pane
        GridPane rmMem = new GridPane();
        rmMem.gridLinesVisibleProperty().setValue(Boolean.TRUE);
        
        // VM memory pane
        GridPane vmMem = new GridPane();
        vmMem.gridLinesVisibleProperty().setValue(Boolean.TRUE);
        
        // Settings for rmMem and vmMem columns
        for (int i = 0; i < 16; ++i){
            ColumnConstraints column = new ColumnConstraints();
            column.setMinWidth(58);
            column.setMaxWidth(58);
            column.setHalignment(HPos.CENTER);
            rmMem.getColumnConstraints().add(column);
            vmMem.getColumnConstraints().add(column);
        }

        // Real memory words
        for (int block = 0; block < 64; ++block){
            for (int word = 0; word < 16; ++word){
                Label temp = new Label("label");
                        
                temp.textProperty().bind(rm.memoryProperty(block, word));
                rmMem.add(temp, word, block);
            }
            
            // Extra column for numbering
            Label blockNum = new Label(Integer.toHexString(block));
            blockNum.setTextFill(Color.TEAL);
            rmMem.add(blockNum, 17, block);
        }
        
        // Virtual memory words
        for (int block = 0; block < 16; ++block){
            for (int word = 0; word < 16; ++word){
                Label temp = new Label("label");
                        
                temp.textProperty().bind(vmMaster.memoryProperty(block, word));
                vmMem.add(temp, word, block);
            }
            
            // Extra column for numbering
            Label blockNum = new Label(Integer.toHexString(block));
            blockNum.setTextFill(Color.TEAL);
            vmMem.add(blockNum, 17, block);
        }
        
        //TESTING CASE CURRENTLY --------------------------------------------------------------------------------------------------------------------
        /*Button test = new Button("Gimme other VM");
        test.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent event) {
                try {
                    // Scheduler finds other VM - TODO
                    VirtualMachine testvm = new VirtualMachine();
                    System.out.println(rm.loadVirtualMachine(rcpu, testvm));
                } catch (NumberFormatException ex) {
                    stdout.setText(ex.getMessage());
                    System.out.println(ex);
                }
            }
        });*/
        //TESTING CASE CURRENTLY --------------------------------------------------------------------------------------------------------------------
        
        Button startProg = new Button("Run VM programme");
        startProg.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent event) {
                int outcome;
                try {
                    if (sch.allVMs.isEmpty()){
                        System.out.println("There are no VMs to run");
                        return;
                    }
                    while(true){
                        outcome = sch.executeCommand(rcpu, stdinStatus, stdout);
                        if (outcome == 0){
                            break;
                        }
                    }
                } catch (NumberFormatException | IOException ex) {
                    stdout.setText(ex.getMessage());
                    System.out.println(ex);
                }
            }
        });
        
        Button step = new Button("Step");
        step.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent event) {
                try {
                    sch.step(rcpu, stdinStatus, stdout, vcpuMaster, vmMaster);
                } catch (NumberFormatException | IOException ex) {
                    stdout.setText(ex.getMessage());
                    System.out.println(ex);
                }
            }
        });
        
        /*Button reset = new Button("Reset");
        reset.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent event) {
                vcpu.pcProperty().setValue(Integer.toHexString(vm.getInitialPC()/16) + Integer.toHexString(vm.getInitialPC()%16));
                vcpu.sfProperty().setValue("0");
                vcpu.axProperty().setValue("0");
                vcpu.bxProperty().setValue("0");
                rcpu.tmrProperty().setValue("a");
                rcpu.mdProperty().setValue("0");
                rcpu.siProperty().setValue("0");
                rcpu.piProperty().setValue("0");
                rcpu.shmProperty().setValue("0000000000000000");
                stdout.setText("");
                vm.reset();
            }
        });*/
        
        Button loadProg = new Button("Load");
        loadProg.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent event) {
                /*vm.reset();
                for (int i = 0; i < 16; ++i){
                    for (int j = 0; j < 16 ; ++j){
                        vm.setWord(i, j, "0");
                    }
                }*/
                VirtualMachine vm = new VirtualMachine();
                VirtualCPU vcpu = new VirtualCPU();
                
                char[] temp = rcpu.chProperty().get().toCharArray();
                temp[2] = '1';
                rcpu.chProperty().setValue(String.valueOf(temp));
                
                FileChooser browser = new FileChooser();
                browser.getExtensionFilters().add(new ExtensionFilter("Text source files", "*.txt"));
                File sourceCode = browser.showOpenDialog(primaryStage);
                
                temp = rcpu.chProperty().get().toCharArray();
                temp[2] = '0';
                rcpu.chProperty().setValue(String.valueOf(temp));
                
                if (sourceCode == null){
                    return;
                }
                
                if (rm.loadVirtualMachine(vcpu, vm, sch)){
                    vm.loadProgramme(sourceCode, vcpu);
                    vcpu.sfProperty().setValue("0");
                    vcpu.axProperty().setValue("0");
                    vcpu.bxProperty().setValue("0");
                    VM virtualmachine = new VM("VirtualMachine" + rm.getLastPTR());
                    sch.procList.add(virtualmachine);
                    sch.includeVM(rm.getLastPTR(), new Pair(vcpu, vm));
                }
                else {
                    stdout.setText("There is not enough space for another VM");
                }

                //rcpu.tmrProperty().setValue("a");
                //rcpu.mdProperty().setValue("0");
                //rcpu.siProperty().setValue("0");
                //rcpu.piProperty().setValue("0");
                //rcpu.shmProperty().setValue("0000000000000000");
            }
        });
        
        // Real CPU pane
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
        pc.textProperty().bind(vcpuMaster.pcProperty());
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
        sf.textProperty().bind(vcpuMaster.sfProperty());
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
        ax.textProperty().bind(vcpuMaster.axProperty());
        container.getChildren().add(new Label("AX"));
        container.getChildren().add(ax);
        rcpuData.getChildren().add(container);
        
        container = new HBox();
        container.setAlignment(Pos.CENTER);
        container.setSpacing(10);
        Label bx = new Label("BX");    
        bx.textProperty().bind(vcpuMaster.bxProperty());
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
        
        
        // Virtual CPU pane
        VBox vcpuData = new VBox();
        vcpuData.setStyle("-fx-border-style: solid inside;");  
        vcpuData.setSpacing(10);
        
        container = new HBox();
        container.setAlignment(Pos.CENTER);
        container.setSpacing(10);
        Label vpc = new Label();    
        vpc.textProperty().bind(vcpuMaster.pcProperty());
        container.getChildren().add(new Label("PC"));
        container.getChildren().add(vpc);
        vcpuData.getChildren().add(container);
        
        container = new HBox();
        container.setAlignment(Pos.CENTER);
        container.setSpacing(10);
        Label vax = new Label();    
        vax.textProperty().bind(vcpuMaster.axProperty());
        container.getChildren().add(new Label("AX"));
        container.getChildren().add(vax);
        vcpuData.getChildren().add(container);
        
        container = new HBox();
        container.setAlignment(Pos.CENTER);
        container.setSpacing(10);
        Label vbx = new Label();    
        vbx.textProperty().bind(vcpuMaster.bxProperty());
        container.getChildren().add(new Label("BX"));
        container.getChildren().add(vbx);
        vcpuData.getChildren().add(container);
        
        container = new HBox();
        container.setAlignment(Pos.CENTER);
        container.setSpacing(10);
        Label vsf = new Label();    
        vsf.textProperty().bind(vcpuMaster.sfProperty());
        container.getChildren().add(new Label("SF"));
        container.getChildren().add(vsf);
        vcpuData.getChildren().add(container);
        
        // 'Left side' objects
        VBox leftOrganized = new VBox();
        Label rmemLabel = new Label("Real memory");
        rmemLabel.setTextFill(Color.RED);
        leftOrganized.getChildren().add(rmemLabel);
        
        Label wordLabel = new Label(String.format("%9s%17s%17s%17s%16s%17s%17s%16s%16s%17s%17s%17s%17s%16s%17s%17s",
                "0", "1", "2", "3", "4", "5", "6", "7", "8", "9", "a", "b", "c", "d", "e", "f"));
        wordLabel.setTextFill(Color.DARKCYAN);
        leftOrganized.getChildren().add(wordLabel);
        
        // Scrollable real memory
        ScrollPane rmemScroll = new ScrollPane(rmMem);
        rmemScroll.setMaxHeight(450);
        rmemScroll.setMinHeight(450);
        
        // Scrollable virtual memory
        ScrollPane vmemScroll = new ScrollPane(vmMem);
        vmemScroll.setMaxHeight(150);
        vmemScroll.setMinHeight(150);
        
        rmemScroll.setContent(rmMem);
        vmemScroll.setContent(vmMem);
        rmemScroll.setVbarPolicy(ScrollPane.ScrollBarPolicy.ALWAYS);
        vmemScroll.setVbarPolicy(ScrollPane.ScrollBarPolicy.ALWAYS);
        
        leftOrganized.getChildren().add(rmemScroll);
        Label vmemLabel = new Label("Virtual memory");
        vmemLabel.setTextFill(Color.RED);
        leftOrganized.getChildren().add(vmemLabel);
        
        // Needs a separate label, otherwise throws duplicate child exception
        wordLabel = new Label(String.format("%9s%17s%17s%17s%16s%17s%17s%16s%16s%17s%17s%17s%17s%16s%17s%17s",
                "0", "1", "2", "3", "4", "5", "6", "7", "8", "9", "a", "b", "c", "d", "e", "f"));
        wordLabel.setTextFill(Color.DARKCYAN);
        leftOrganized.getChildren().add(wordLabel);
        leftOrganized.getChildren().add(vmemScroll);
        leftOrganized.getChildren().add(stdinLabel);
        leftOrganized.getChildren().add(stdinStatus);
        leftOrganized.getChildren().add(stdoutLabel);
        leftOrganized.getChildren().add(stdout);
        
        // 'Right side' objects
        VBox rightOrganized = new VBox();
        
        Label rcpuLabel = new Label("Real processor");
        rcpuLabel.setTextFill(Color.RED);
        Label vcpuLabel = new Label("Virtual processor");
        vcpuLabel.setTextFill(Color.RED);

        rightOrganized.getChildren().add(startProg);
        rightOrganized.getChildren().add(step);
        rightOrganized.getChildren().add(loadProg);
        //rightOrganized.getChildren().add(reset);
        
        // REMOVE AFTER TESTING
        //rightOrganized.getChildren().add(test);
        
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
        
        // Tells a little more about selected resource/process
        TextArea procInfo = new TextArea();
        procInfo.setMinSize(400, 100);
        procInfo.setMaxSize(400, 100);
        procInfo.editableProperty().setValue(Boolean.FALSE);
        
        TextArea resInfo = new TextArea();
        resInfo.setMinSize(400, 100);
        resInfo.setMaxSize(400, 100);
        resInfo.editableProperty().setValue(Boolean.FALSE);
        
        // Enveloping tab pane
        TabPane all = new TabPane();
        Tab machineTab = new Tab();
        machineTab.setContent(root);
        machineTab.setClosable(false);
        machineTab.textProperty().setValue("Active VM");
        all.getTabs().add(machineTab);
        
        // Resource and process tab
        Tab procresTab = new Tab();
        
        ListView<Process> processes = new ListView<>(sch.procList);
        processes.setOrientation(Orientation.VERTICAL);
        processes.setMaxSize(400, 400);
        processes.setMinSize(400, 400);
        
        ListView<Resource> resources = new ListView<>(sch.resList);
        resources.setOrientation(Orientation.VERTICAL);
        resources.setMaxSize(400, 400);
        resources.setMinSize(400, 400);
        
        // Processes starting here
        StartStop startstop = new StartStop("StartStop");
        sch.procList.add(startstop);
        //resList.add(new Resource("other test"));
        
        processes.getSelectionModel().selectedItemProperty().addListener(new ChangeListener<Process>(){
            @Override
            public void changed(ObservableValue<? extends Process> observable, Process oldValue, Process newValue) {
                /*try {
                    if (resources.getSelectionModel().getSelectedIndex() != -1){
                        resources.getSelectionModel().clearSelection();
                    }
                } catch(IndexOutOfBoundsException ex){}*/
                procInfo.setText(newValue.getAllValues());
            }
        });
        
        resources.getSelectionModel().selectedItemProperty().addListener(new ChangeListener<Resource>(){
            @Override
            public void changed(ObservableValue<? extends Resource> observable, Resource oldValue, Resource newValue) {
                /*try {
                    if (processes.getSelectionModel().getSelectedIndex() != -1){
                        processes.getSelectionModel().clearSelection();
                    }
                } catch(IndexOutOfBoundsException ex){}*/
                resInfo.setText(newValue.getAllValues());
            }
        });
        
        VBox base = new VBox();
        HBox infos = new HBox();
        HBox lists = new HBox();
        lists.getChildren().add(new Label("Processes"));
        lists.getChildren().add(processes);
        lists.getChildren().add(new Label("Resources"));
        lists.getChildren().add(resources);
        base.getChildren().add(lists);
        
        infos.getChildren().add(new Label("Processes"));
        infos.getChildren().add(procInfo);
        infos.getChildren().add(new Label("Resources"));
        infos.getChildren().add(resInfo);
        base.getChildren().add(infos);
        
        procresTab.setContent(base);
        procresTab.setClosable(false);
        procresTab.textProperty().setValue("Processes / Resources");
        all.getTabs().add(procresTab);
        
        Scene scene = new Scene(all, 1200, 800);
        primaryStage.setScene(scene);
        primaryStage.setResizable(false);
        primaryStage.show();
    }
}