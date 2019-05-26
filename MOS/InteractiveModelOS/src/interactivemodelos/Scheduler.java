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
        EXECUTE_VM, SWITCH_VM, STOP_MOS
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
                    } while(continuous);
                }
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
