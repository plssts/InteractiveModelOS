/*
Holds the GUI. Also starts the JavaFX main stage.
*/
package interactivemodelos;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import javafx.application.Application;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
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
import javafx.util.Pair;
import process.StartStop;

/**
 * @author Paulius Staisiunas, Computer Science 3 yr., 3 gr.
 */

public class InteractiveModelOS extends Application {
    @Override
    public void start(Stage primaryStage) {
        // Main pane, fixed size
        BorderPane root = new BorderPane();
        root.setMaxSize(1210, 800);
        root.setMinSize(1210, 800);
        
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
        
        Button step = new Button("Step");
        step.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent event) {
                int outcome;
                try {
                    outcome = sch.step(rcpu, stdinStatus, stdout, vcpuMaster, vmMaster, false);
                    if (outcome < 100){ // Signal to free memory with this PTR
                        ArrayList<Integer> blocks = new ArrayList<>();
                        for (int i = 0; i < 16; ++i){
                            String word = rm.getWord(outcome, i);
                            blocks.add(Integer.parseInt(word, 16));
                        }
                        
                        for (Integer i : blocks){
                            for (int j = 0; j < 16; ++j){
                                rm.setWord(i, j, "0");
                                rm.setWord(outcome, j, "0");
                            }
                        }
                        
                        for (Resource rr : sch.resList){
                            if (rr.getName().equals("VirtualMemory")){
                                rr.decrBlocks(17);
                            }
                        }
                        blocks.add(outcome);
                        rm.removeBlocks(blocks);
                    }
                } catch (java.util.ConcurrentModificationException ex){
                    
                }
                catch (NumberFormatException | IOException ex) {
                    stdout.setText(ex.getMessage());
                    System.out.println(ex);
                }
            }
        });
        
        Button loadProg = new Button("Load");
        loadProg.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent event) {
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
                
                // TaskInMemory is freed - we read the source code
                for (Resource r : sch.resList){
                    if (r.getName().equals("TaskInMemory")){
                        r.setFreed("ReadUI");
                        r.setOwned("JobCtrlLangInterpreter");
                        ArrayList<String> tmp = r.getWP();
                        tmp.remove("JobCtrlLangInterpreter");
                        r.setWP(tmp);
                        break;
                    }
                }
                
                // JCLI is unblocked - it received TaskInMemory
                for (Process p : sch.procList){
                    if (p.getName().equals("JobCtrlLangInterpreter")){
                        p.setStatus("RUNNING");
                        p.setWR("-");
                        ArrayList<String> tmp = p.getOwnedRs();
                        tmp.add("TaskInMemory");
                        p.setOwnedRs(tmp);
                        break;
                    }
                }
                
                // JCLI freed TaskProgrammeInMemory - Loader can receive it
                for (Resource r : sch.resList){
                    if (r.getName().equals("TaskProgrammeInMemory")){
                        r.setFreed("JobCtrlLangInterpreter");
                        r.setOwned("Loader");
                        ArrayList<String> tmp = r.getWP();
                        tmp.remove("Loader");
                        r.setWP(tmp);
                        break;
                    }
                }
                
                // JCLI is blocking again - waiting for TaskInMemory (other source code)
                for (Process p : sch.procList){
                    if (p.getName().equals("JobCtrlLangInterpreter")){
                        p.setStatus("BLOCKED");
                        p.setWR("TaskInMemory");
                        ArrayList<String> tmp = p.getOwnedRs();
                        tmp.remove("TaskInMemory");
                        p.setOwnedRs(tmp);
                        break;
                    }
                }
                
                // TaskInMemory has JCLI as waiting process again
                for (Resource r : sch.resList){
                    if (r.getName().equals("TaskInMemory")){
                        ArrayList<String> tmp = r.getWP();
                        tmp.add("JobCtrlLangInterpreter");
                        r.setWP(tmp);
                        break;
                    }
                }
                
                // Loader can now run with TaskProgrammeInMemory
                for (Process p : sch.procList){
                    if (p.getName().equals("Loader")){
                        p.setStatus("RUNNING");
                        p.setWR("-");
                        ArrayList<String> tmp = p.getOwnedRs();
                        tmp.add("TaskProgrammeInMemory");
                        p.setOwnedRs(tmp);
                        break;
                    }
                }
                
                // TaskReady was freed by Loader - Main can continue now
                for (Resource r : sch.resList){
                    if (r.getName().equals("TaskReady")){
                        r.setFreed("Loader");
                        r.setOwned("Main");
                        ArrayList<String> tmp = r.getWP();
                        tmp.remove("Main");
                        r.setWP(tmp);
                        break;
                    }
                }
                
                // Loader is blocking again - waiting for TaskProgrammeInMemory
                for (Process p : sch.procList){
                    if (p.getName().equals("Loader")){
                        p.setStatus("BLOCKED");
                        p.setWR("TaskProgrammeInMemory");
                        ArrayList<String> tmp = p.getOwnedRs();
                        tmp.remove("TaskProgrammeInMemory");
                        p.setOwnedRs(tmp);
                        break;
                    }
                }
                
                // TaskProgrammeInMemory has Loader in its waiting list
                for (Resource r : sch.resList){
                    if (r.getName().equals("TaskProgrammeInMemory")){
                        ArrayList<String> tmp = r.getWP();
                        tmp.add("Loader");
                        r.setWP(tmp);
                        break;
                    }
                }
                
                // Main can now run with TaskReady
                for (Process p : sch.procList){
                    if (p.getName().equals("Main")){
                        p.setStatus("RUNNING");
                        p.setWR("-");
                        ArrayList<String> tmp = p.getOwnedRs();
                        tmp.add("TaskReady");
                        p.setOwnedRs(tmp);
                        break;
                    }
                }

                if (rm.loadVirtualMachine(vcpu, vm, sch)){
                    vm.loadProgramme(sourceCode, vcpu);
                    
                    Process jg = new Process("JobGovernor" + rm.getLastPTR());
                    jg.setStatus("BLOCKED");
                    jg.setWR("FromInterrupt");
                    jg.setParent("Main");
                    ArrayList<String> tmp = jg.getOwnedRs();
                    tmp.add("VMAllocated");
                    tmp.add("InputRequest");
                    tmp.add("OutputRequest");
                    tmp.add("SHMControl");
                    tmp.add("VirtualMemory");
                    jg.setOwnedRs(tmp);
                    tmp = jg.getChildren();
                    tmp.add("VirtualMachine" + rm.getLastPTR());
                    jg.setChildren(tmp);
                    sch.procList.add(jg);
                    
                    // AllocateVM is waiting for another request
                    for (Process p : sch.procList){
                        if (p.getName().equals("AllocateVM")){
                            p.setStatus("BLOCKED");
                            p.setWR("VMRequest");
                            break;
                        }
                    }
                    
                    // Some virtual memory is gone
                    for (Resource rsrc : sch.resList){
                        if (rsrc.getName().equals("VirtualMemory")){
                            rsrc.incrBlocks(17);
                            break;
                        }
                    }
                    
                    // VMAllocated is now owned by latest JG
                    for (Resource r : sch.resList){
                        if (r.getName().equals("VMAllocated")){
                            r.setFreed("AllocateVM");
                            r.setOwned("JobGovernor" + rm.getLastPTR());
                            break;
                        }
                    }
                    
                    // New JG waits for FromInterrupt
                    for (Resource r : sch.resList){
                        if (r.getName().equals("FromInterrupt")){
                            tmp = r.getWP();
                            tmp.add("JobGovernor" + rm.getLastPTR());
                            r.setWP(tmp);
                            break;
                        }
                    }
                    
                    for (Resource r : sch.resList){
                        if (r.getName().equals("UserInput")){
                            tmp = r.getWP();
                            tmp.add("JobGovernor" + rm.getLastPTR());
                            r.setWP(tmp);
                            break;
                        }
                    }
                    
                    for (Resource r : sch.resList){
                        if (r.getName().equals("UserOutput")){
                            tmp = r.getWP();
                            tmp.add("JobGovernor" + rm.getLastPTR());
                            r.setWP(tmp);
                            break;
                        }
                    }
                    
                    Process shm = new Process("ManageSHM" + rm.getLastPTR());
                    shm.setParent("JobGovernor" + rm.getLastPTR());
                    shm.setStatus("BLOCKED");
                    shm.setWR("SHMControl");
                    ArrayList<String> res = shm.getOwnedRs();
                    res.add("SHMCEnd");
                    shm.setOwnedRs(res);
                    sch.procList.add(shm);
                    
                    vcpu.sfProperty().setValue("0");
                    vcpu.axProperty().setValue("0");
                    vcpu.bxProperty().setValue("0");

                    Process virtualmachine = new Process("VirtualMachine" + rm.getLastPTR());
                    virtualmachine.setParent("JobGovernor" + rm.getLastPTR());
                    virtualmachine.setStatus("READY_STOPPED");
                    sch.procList.add(virtualmachine);
                    sch.includeVM(rm.getLastPTR(), new Pair(vcpu, vm));
                }
                else {
                    // AllocateVM is blocking because there is no memory
                    for (Process p : sch.procList){
                        if (p.getName().equals("AllocateVM")){
                            p.setStatus("BLOCKED");
                            p.setWR("VirtualMemory");
                            break;
                        }
                    }
                    stdout.setText("There is not enough space for another VM");
                }
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

        rightOrganized.getChildren().add(step);
        rightOrganized.getChildren().add(loadProg);
        
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
        procInfo.setMinSize(400, 200);
        procInfo.setMaxSize(400, 200);
        procInfo.editableProperty().setValue(Boolean.FALSE);
        
        TextArea resInfo = new TextArea();
        resInfo.setMinSize(400, 200);
        resInfo.setMaxSize(400, 200);
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
        startstop.setStatus("BLOCKED");
        startstop.setWR("MOSEnd");
        startstop.setParent("-");
        ArrayList<String> temp = startstop.getOwnedRs();
        startstop.setOwnedRs(temp);
        temp = startstop.getCreatedRs();
        startstop.setCreatedRs(temp);
        temp = startstop.getChildren();
        temp.add("ReadUI");
        temp.add("JobCtrlLangInterpreter");
        temp.add("Loader");
        temp.add("Main");
        temp.add("AllocateVM");
        temp.add("Interrupt");
        temp.add("IData");
        temp.add("OData");
        startstop.setChildren(temp);
        sch.procList.add(startstop);
        
        Process readui = new Process("ReadUI");
        readui.setStatus("BLOCKED");
        readui.setWR("UILoad");
        readui.setParent("StartStop");
        temp = readui.getCreatedRs();
        readui.setCreatedRs(temp);
        temp = readui.getOwnedRs();
        temp.add("TaskInMemory");
        readui.setOwnedRs(temp);
        temp = readui.getChildren();
        readui.setChildren(temp);
        sch.procList.add(readui);
        
        Process jcl = new Process("JobCtrlLangInterpreter");
        jcl.setStatus("BLOCKED");
        jcl.setWR("TaskInMemory");
        jcl.setParent("StartStop");
        temp = jcl.getCreatedRs();
        jcl.setCreatedRs(temp);
        temp = jcl.getOwnedRs();
        temp.add("TaskProgrammeInMemory");
        jcl.setOwnedRs(temp);
        temp = jcl.getChildren();
        jcl.setChildren(temp);
        sch.procList.add(jcl);
        
        Process idata = new Process("IData");
        idata.setStatus("BLOCKED");
        idata.setWR("InputRequest");
        idata.setParent("StartStop");
        temp = idata.getCreatedRs();
        idata.setCreatedRs(temp);
        temp = idata.getOwnedRs();
        temp.add("UserInput");
        idata.setOwnedRs(temp);
        temp = idata.getChildren();
        idata.setChildren(temp);
        sch.procList.add(idata);
        
        Process odata = new Process("OData");
        odata.setStatus("BLOCKED");
        odata.setWR("OutputRequest");
        odata.setParent("StartStop");
        temp = odata.getCreatedRs();
        odata.setCreatedRs(temp);
        temp = odata.getOwnedRs();
        temp.add("UserOutput");
        odata.setOwnedRs(temp);
        temp = odata.getChildren();
        odata.setChildren(temp);
        sch.procList.add(odata);
        
        Process loader = new Process("Loader");
        loader.setStatus("BLOCKED");
        loader.setWR("TaskProgrammeInMemory");
        loader.setParent("StartStop");
        temp = loader.getCreatedRs();
        loader.setCreatedRs(temp);
        temp = loader.getOwnedRs();
        temp.add("TaskReady");
        loader.setOwnedRs(temp);
        temp = loader.getChildren();
        loader.setChildren(temp);
        sch.procList.add(loader);
        
        Process main = new Process("Main");
        main.setStatus("BLOCKED");
        main.setWR("TaskReady");
        main.setParent("StartStop");
        temp = main.getCreatedRs();
        main.setCreatedRs(temp);
        temp = main.getOwnedRs();
        main.setOwnedRs(temp);
        temp = main.getChildren();
        main.setChildren(temp);
        sch.procList.add(main);
        
        Process alloc = new Process("AllocateVM");
        alloc.setStatus("BLOCKED");
        alloc.setWR("VMRequest");
        alloc.setParent("StartStop");
        temp = alloc.getCreatedRs();
        alloc.setCreatedRs(temp);
        temp = alloc.getOwnedRs();
        temp.add("VMAllocated");
        alloc.setOwnedRs(temp);
        temp = alloc.getChildren();
        alloc.setChildren(temp);
        sch.procList.add(alloc);
        
        Process interr = new Process("Interrupt");
        interr.setStatus("BLOCKED");
        interr.setWR("Interrupt");
        interr.setParent("StartStop");
        temp = interr.getCreatedRs();
        interr.setCreatedRs(temp);
        temp = interr.getOwnedRs();
        temp.add("FromInterrupt");
        interr.setOwnedRs(temp);
        temp = interr.getChildren();
        interr.setChildren(temp);
        sch.procList.add(interr);
        
        // Resources below
        Resource vmalloc = new Resource("VMAllocated");
        vmalloc.setCreator("StartStop");
        vmalloc.setFreed("-");
        vmalloc.setOwned("AllocateVM");
        sch.resList.add(vmalloc);
        
        Resource fi = new Resource("FromInterrupt");
        fi.setCreator("StartStop");
        fi.setFreed("-");
        fi.setOwned("-");
        sch.resList.add(fi);
        
        Resource vm = new Resource("VirtualMemory");
        vm.setCreator("StartStop");
        vm.setFreed("-");
        vm.setOwned("Potentially owned by multiple JGs");
        sch.resList.add(vm);
        
        Resource mosend = new Resource("MOSEnd");
        mosend.setCreator("-");
        mosend.setFreed("-");
        mosend.setOwned("-");
        temp = new ArrayList<>();
        temp.add("StartStop");
        mosend.setWP(temp);
        sch.resList.add(mosend);
        
        Resource shared = new Resource("SharedMemory");
        shared.setCreator("StartStop");
        shared.setFreed("-");
        shared.setOwned("Potentially owned by multiple JGs");
        sch.resList.add(shared);
        
        Resource uin = new Resource("UserInput");
        uin.setCreator("StartStop");
        uin.setFreed("-");
        uin.setOwned("IData");
        sch.resList.add(uin);
        
        Resource uout = new Resource("UserOutput");
        uout.setCreator("StartStop");
        uout.setFreed("-");
        uout.setOwned("OData");
        sch.resList.add(uout);
        
        Resource inreq = new Resource("InputRequest");
        inreq.setCreator("StartStop");
        inreq.setFreed("-");
        inreq.setOwned("-");
        temp = new ArrayList<>();
        temp.add("IData");
        inreq.setWP(temp);
        sch.resList.add(inreq);
        
        Resource oureq = new Resource("OutputRequest");
        oureq.setCreator("StartStop");
        oureq.setFreed("-");
        oureq.setOwned("-");
        temp = new ArrayList<>();
        temp.add("OData");
        oureq.setWP(temp);
        sch.resList.add(oureq);
        
        Resource interrupt = new Resource("Interrupt");
        interrupt.setCreator("StartStop");
        interrupt.setFreed("-");
        interrupt.setOwned("-");
        temp = new ArrayList<>();
        temp.add("Interrupt");
        interrupt.setWP(temp);
        sch.resList.add(interrupt);
        
        Resource shmc = new Resource("SHMControl");
        shmc.setCreator("StartStop");
        shmc.setFreed("-");
        shmc.setOwned("-");
        sch.resList.add(shmc);
        
        Resource uiload = new Resource("UILoad");
        uiload.setCreator("StartStop");
        uiload.setFreed("-");
        uiload.setOwned("-");
        temp = new ArrayList<>();
        temp.add("ReadUI");
        uiload.setWP(temp);
        sch.resList.add(uiload);
        
        Resource taskmem = new Resource("TaskInMemory");
        taskmem.setCreator("StartStop");
        taskmem.setFreed("-");
        taskmem.setOwned("ReadUI");
        temp = new ArrayList<>();
        temp.add("JobCtrlLangInterpreter");
        taskmem.setWP(temp);
        sch.resList.add(taskmem);
        
        Resource taskprogmem = new Resource("TaskProgrammeInMemory");
        taskprogmem.setCreator("StartStop");
        taskprogmem.setFreed("-");
        taskprogmem.setOwned("JobCtrlLangInterpreter");
        temp = new ArrayList<>();
        temp.add("Loader");
        taskprogmem.setWP(temp);
        sch.resList.add(taskprogmem);
        
        Resource taskready = new Resource("TaskReady");
        taskready.setCreator("StartStop");
        taskready.setFreed("-");
        taskready.setOwned("Loader");
        temp = new ArrayList<>();
        temp.add("Main");
        taskready.setWP(temp);
        sch.resList.add(taskready);
        
        processes.getSelectionModel().selectedItemProperty().addListener(new ChangeListener<Process>(){
            @Override
            public void changed(ObservableValue<? extends Process> observable, Process oldValue, Process newValue) {
                procInfo.setText(newValue.getAllValues());
            }
        });
        
        resources.getSelectionModel().selectedItemProperty().addListener(new ChangeListener<Resource>(){
            @Override
            public void changed(ObservableValue<? extends Resource> observable, Resource oldValue, Resource newValue) {
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
        
        Scene scene = new Scene(all, 1210, 800);
        primaryStage.setScene(scene);
        primaryStage.setResizable(false);
        primaryStage.show();
    }
}