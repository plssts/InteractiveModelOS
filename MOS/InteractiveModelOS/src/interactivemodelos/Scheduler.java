/*
--This boss lets others work--
A. Einstein
 */
package interactivemodelos;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.util.Pair;

public class Scheduler {
    ObservableList<Process> procList;
    ObservableList<Resource> resList;
    
    Map allVMs;
    Pair<VirtualCPU, VirtualMachine> currentVM;
    enum State {
        EXECUTE_VM, SWITCH_VM, STOP_MOS
    };
    
    private State state;
    
    public Scheduler(){
        allVMs = new HashMap();
        procList = FXCollections.<Process>observableArrayList();
        resList = FXCollections.<Resource>observableArrayList();
        state = State.EXECUTE_VM; // change later
    }
    
    public void step(RealCPU rcpu, Label stdinStatus, TextField stdout) throws IOException{
        int outcome;
        switch(state){
            case EXECUTE_VM:
                if (currentVM != null){
                    outcome = currentVM.getValue().executeCommand(currentVM.getKey(), rcpu, stdinStatus, stdout);
                    if (outcome == 0){ // HALT - VM has finished its programme
                        state = State.SWITCH_VM;
                        excludeVM(rcpu.ptrProperty().get());
                        System.out.println("Excluded VM " + rcpu.ptrProperty().get());
                        currentVM = null;
                    }
                }
                break;
                
            case SWITCH_VM:
                for (Process p : procList){
                    if (p.getName().startsWith("VirtualMachine")){
                        Process temp = p;
                        procList.remove(p);
                        temp.setStatus("RUNNING");
                        procList.add(temp);
                        System.out.println("Switching to machine PTR: " + p.getName().substring(13));
                        currentVM = (Pair<VirtualCPU, VirtualMachine>)allVMs.get(p.getName().substring(13));
                        state = State.EXECUTE_VM;
                    }
                    // no virtual machines - do something else instead
                }
                break;
        }
    }
    
    public void remapParameters(VirtualCPU cpumaster, VirtualMachine vmmaster){
        
    }
    
    public int executeCommand(RealCPU rcpu, Label stdinStatus, TextField stdout) throws IOException{
        int outcome;
        while(true){
            outcome = currentVM.getValue().executeCommand(currentVM.getKey(), rcpu, stdinStatus, stdout);
            if (outcome == 0){ // HALT
                state = State.SWITCH_VM;
                return 0;
            }
        }
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
