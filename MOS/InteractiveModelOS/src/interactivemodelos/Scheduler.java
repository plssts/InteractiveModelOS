/*
--This boss lets others work--
A. Einstein
 */
package interactivemodelos;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.util.Pair;

public class Scheduler {
    ObservableList<Process> procList;
    ObservableList<Resource> resList;
    //File sourceCode = null;
    Button load;
    //RealMachine rm;
    
    Map allVMs;
    //Map vmsRegisters;
    Pair<VirtualCPU, VirtualMachine> currentVM;
    enum State {
        EXECUTE_VM, SWITCH_VM, STOP_MOS, INTERRUPT_ACTIVATE, ACTIVATE_IDATA, ACTIVATE_ODATA, SHM_LOCK, SHM_UNLOCK
    };
    
    private State state;
    
    private boolean freeTIM = true;
    private boolean goJCL = false;
    private boolean tofreeJCL = true;
    
    public Scheduler(){
        allVMs = new HashMap();
        //vmsRegisters = new HashMap();
        procList = FXCollections.<Process>observableArrayList();
        resList = FXCollections.<Resource>observableArrayList();
        state = State.SWITCH_VM;
    }
    
    /*public void linkRM(RealMachine rm){
        this.rm = rm;
    }*/
    
    public void addbtn(Button btn){
        load = btn;
    }
    
    /*public void setFile(File file){
        sourceCode = file;
    }*/
    
    // We return 100 if no special action is needed. The reson for a higher number than 64 is 
    // the fact that 0 can be both successful exit and PTR to remove. So 100 was chosen as OK status
    public int step(RealCPU rcpu, Label stdinStatus, TextField stdout, VirtualCPU cpumaster, VirtualMachine vmmaster, boolean continuous) throws IOException{
        int outcome;
        switch(state){
            case EXECUTE_VM:
                if (currentVM != null){
                    do {
                        outcome = currentVM.getValue().executeCommand(currentVM.getKey(), rcpu, stdinStatus, stdout);
                        remapParameters(cpumaster, vmmaster); // update values after call
                        if (outcome == 0 || outcome == 2){ // HALT - VM has finished its programme
                            System.out.println("HALT was reached by this VM");
                            state = State.SWITCH_VM;
                            excludeVM(rcpu.ptrProperty().get());
                            String temp = rcpu.ptrProperty().get();
                            for (Process p : procList){
                                if (p.getName().equals("VirtualMachine" + rcpu.ptrProperty().get())){
                                    procList.remove(p);
                                    rcpu.ptrProperty().setValue("0");
                                    rcpu.siProperty().setValue("0");
                                    rcpu.piProperty().setValue("0");
                                    rcpu.mdProperty().setValue("0");
                                    rcpu.tmrProperty().setValue("a");
                                    currentVM = new Pair(new VirtualCPU(), new VirtualMachine()); // null values
                                    remapParameters(cpumaster, vmmaster);
                                    currentVM = null;
                                    break;
                                }
                            }
                            for (Process p : procList){
                                if (p.getName().equals("JobGovernor" + temp)){
                                    Process proc = p;
                                    procList.remove(p);
                                    proc.setStatus("BLOCKED");
                                    proc.setWR("Nonexistent");
                                    ArrayList<String> arr = proc.getChildren();
                                    arr.clear();
                                    proc.setChildren(arr);
                                    arr = proc.getOwnedRs();
                                    arr.clear();
                                    proc.setOwnedRs(arr);
                                    proc.modifyName("JobGovernor-dormant:" + temp);
                                    procList.add(proc);
                                    break;
                                }
                            }
                            
                            for (Resource rsrc : resList){
                                if (rsrc.getName().equals("Interrupt")){
                                    rsrc.setOwned("-");
                                    break;
                                }
                            }
                            
                            for (Resource rsrc : resList){
                                if (rsrc.getName().equals("FromInterrupt")){
                                    ArrayList<String> arr = rsrc.getWP();
                                    arr.remove("JobGovernor" + temp);
                                    rsrc.setWP(arr);
                                    break;
                                }
                            }
                            
                            System.out.println("Excluded VM " + temp);
                            currentVM = null;
                            return Integer.parseInt(temp, 16);
                        }
                        
                        if (outcome == 3){ // TMR has depleted for currentvm
                            state = State.SWITCH_VM;
                            String temp = rcpu.ptrProperty().get();
                            for (Process p : procList){
                                if (p.getName().equals("VirtualMachine" + temp)){
                                    Process copy = p;
                                    procList.remove(p);
                                    copy.setStatus("READY");
                                    procList.add(copy);
                                    //rcpu.ptrProperty().setValue("0");
                                    //rcpu.siProperty().setValue("0");
                                    //rcpu.piProperty().setValue("0");
                                    //rcpu.mdProperty().setValue("0");
                                    //rcpu.tmrProperty().setValue("a");
                                    //currentVM = new Pair(new VirtualCPU(), new VirtualMachine()); // null values
                                    //remapParameters(cpumaster, vmmaster);
                                    //currentVM = null;
                                    break;
                                }
                            }
                            return 100;
                        }
                        
                        if (outcome == 4){
                            switch(rcpu.siProperty().get()){
                                case "1": // RW - input from user
                                    state = State.INTERRUPT_ACTIVATE;
                                    for (Process p : procList){
                                        if (p.getName().equals("VirtualMachine" + rcpu.ptrProperty().get())){
                                            p.setStatus("READY_STOPPED");
                                            break;
                                        }
                                    }
                                    
                                    for (Resource r : resList){
                                        if (r.getName().equals("Interrupt")){
                                            r.setFreed("VirtualMachine" + rcpu.ptrProperty().get());
                                            ArrayList<String> temp = r.getWP();
                                            temp.remove("Interrupt");
                                            r.setOwned("Interrupt");
                                            break;
                                        }
                                    }
                                    
                                    for (Process p : procList){
                                        if (p.getName().equals("Interrupt")){
                                            p.setStatus("READY");
                                            break;
                                        }
                                    }
                                    return 100;
                                    
                                case "2": // WW - output from user
                                    
                                    break;
                                    
                                case "5": // SHL - memory lock
                                    
                                    break;
                                    
                                case "6": // SHU - memory unlock
                                    
                                    break;
                            }
                        }
                    } while(continuous);
                }
                break;
                
            case INTERRUPT_ACTIVATE:
                System.out.println("Interrupt is running");
                for (Process p : procList){
                    if (p.getName().equals("Interrupt")){
                        p.setStatus("RUNNING");
                        break;
                    }
                }
                for (Resource r : resList){
                    if (r.getName().equals("FromInterrupt")){
                        r.setFreed("Interrupt");
                        r.setOwned("-");
                        break;
                    }
                }
                
                if (rcpu.siProperty().get().equals("1")){
                    state = State.ACTIVATE_IDATA;
                    for (Process p : procList){
                        if (p.getName().equals("JobGovernor" + rcpu.ptrProperty().get())){
                            p.setStatus("BLOCKED");
                            p.setWR("UserInput");
                            ArrayList<String> temp = p.getOwnedRs();
                            temp.remove("InputRequest");
                            p.setOwnedRs(temp);
                            break;
                        }
                    }
                    
                    for (Resource r : resList){
                        if (r.getName().equals("InputRequest")){
                            r.setFreed("JobGovernor" + rcpu.ptrProperty().get());
                            r.setOwned("IData");
                            ArrayList<String> temp = r.getWP();
                            temp.remove("IData");
                            r.setWP(temp);
                            break;
                        }
                    }
                    
                    for (Resource r : resList){
                    if (r.getName().equals("UserInput")){
                        //r.setFreed("IData");
                        //r.setOwned("JobGovernor" + rcpu.ptrProperty().get());
                        ArrayList<String> temp = r.getWP();
                        temp.remove("JobGovernor" + rcpu.ptrProperty().get());
                        r.setWP(temp);
                        break;
                    }
                }
                }
                else if (rcpu.siProperty().get().equals("2")){
                    state = State.ACTIVATE_ODATA;
                }
                else if (rcpu.siProperty().get().equals("5")){
                    state = State.SHM_LOCK;
                }
                else if (rcpu.siProperty().get().equals("6")){
                    state = State.SHM_UNLOCK;
                }
                return 100;
                
            case ACTIVATE_IDATA:
                System.out.println("IData is running and is blocked after 'assumed' input");
                for (Process p : procList){
                    if (p.getName().equals("IData")){ // Assuming AFTER input
                        p.setStatus("BLOCKED");
                        p.setWR("InputRequest");
                        //ArrayList<String> temp = p.getOwnedRs();
                        //temp.add("InputRequest");
                        //p.setOwnedRs(temp);
                        break;
                    }
                }
                
                for (Process p : procList){
                    if (p.getName().equals("JobGovernor" + rcpu.ptrProperty().get())){ // Assuming AFTER input
                        p.setStatus("BLOCKED");
                        p.setWR("FromInterrupt");
                        //ArrayList<String> temp = p.getOwnedRs();
                        //temp.add("InputRequest");
                        //p.setOwnedRs(temp);
                        break;
                    }
                }
                
                for (Resource r : resList){
                    if (r.getName().equals("InputRequest")){
                        r.setFreed("JobGovernor" + rcpu.ptrProperty().get());
                        r.setOwned("-");
                        ArrayList<String> temp = r.getWP();
                        temp.add("IData");
                        r.setWP(temp);
                        break;
                    }
                }
                
                for (Resource r : resList){
                    if (r.getName().equals("UserInput")){
                        r.setFreed("IData");
                        r.setOwned("JobGovernor" + rcpu.ptrProperty().get());
                        ArrayList<String> temp = r.getWP();
                        temp.add("JobGovernor" + rcpu.ptrProperty().get());
                        r.setWP(temp);
                        break;
                    }
                }
                
                for (Process p : procList){
                    if (p.getName().equals("VirtualMachine" + rcpu.ptrProperty().get())){
                        Process copy = p;
                        procList.remove(p);
                        copy.setStatus("RUNNING");
                        procList.add(copy);
                        break;
                    }
                }
                
                for (Process p : procList){
                    if (p.getName().equals("Interrupt")){
                        Process copy = p;
                        procList.remove(p);
                        copy.setStatus("BLOCKED");
                        procList.add(copy);
                        break;
                    }
                }
                
                state = State.EXECUTE_VM;
                System.out.println("Will resume execution");
                break;
                
            case SWITCH_VM:
                for (Process p : procList){
                    if (p.getName().startsWith("VirtualMachine")){
                        Process temp = p;
                        procList.remove(p);
                        temp.setStatus("RUNNING");
                        procList.add(temp);
                        System.out.println("Switching to machine PTR: " + p.getName().substring(14));
                        currentVM = (Pair<VirtualCPU, VirtualMachine>)allVMs.get(p.getName().substring(14));
                        state = State.EXECUTE_VM;
                        rcpu.ptrProperty().setValue(p.getName().substring(14));
                        remapParameters(cpumaster, vmmaster);
                        
                        for (Process pr : procList){
                            if (pr.getName().equals("Main")){
                                pr.setStatus("BLOCKED");
                                pr.setWR("TaskReady");
                                ArrayList<String> tmp = pr.getOwnedRs();
                                tmp.remove("TaskReady");
                                pr.setOwnedRs(tmp);
                                break;
                            }
                        }
                        
                        for (Resource r : resList){
                            if (r.getName().equals("TaskReady")){
                                ArrayList<String> tp = r.getWP();
                                tp.add("Main");
                                r.setWP(tp);
                                break;
                            }
                        }
                        
                        for (Resource r : resList){
                            if (r.getName().equals("Interrupt")){
                                //ArrayList<String> tp = r.getWP();
                                r.setOwned(p.getName());
                                //tp.add("Main");
                                //r.setWP(tp);
                                break;
                            }
                        }
                        
                        return 100;
                    }
                }
                System.out.println("Cannot find other VMs to execute");
                break;
        }
        return 100;
    }
    
    public void remapParameters(VirtualCPU cpumaster, VirtualMachine vmmaster){
        //System.out.println("Remapping VMEM memory to VM now");
        for (int i = 0; i < 16; ++i){
            for (int j = 0; j < 16; ++j){
                //vmmaster.setVirtualWordProperty(i, j, currentVM.getValue().memoryProperty(i, j));
                vmmaster.setWord(i, j, currentVM.getValue().getWord(i, j));
            }
        }
        
        //System.out.println("Remapping VCPU to VM now");
        //cpumaster.setAll(currentVM.getKey().pcProperty(), currentVM.getKey().sfProperty(),
        //                 currentVM.getKey().axProperty(), currentVM.getKey().bxProperty());
        cpumaster.pcProperty().setValue(currentVM.getKey().pcProperty().getValue());
        cpumaster.sfProperty().setValue(currentVM.getKey().sfProperty().getValue());
        cpumaster.axProperty().setValue(currentVM.getKey().axProperty().getValue());
        cpumaster.bxProperty().setValue(currentVM.getKey().bxProperty().getValue());
    }
    
    public void includeVM(String ptr, Pair<VirtualCPU, VirtualMachine> vm){
        if (allVMs.isEmpty()){
            currentVM = vm;
        }
        allVMs.put(ptr, vm);
        
    }
    
    public void excludeVM(String ptr){
        allVMs.remove(ptr);
    }
    
    public ObservableList<Process> procList(){
        return procList;
    }
    
    public ObservableList<Resource> resList(){
        return resList;
    }
}
