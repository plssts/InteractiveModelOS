/*
Manages a set of registers of the real machine
 */
package interactivemodelos;

import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

/**
 * @author Paulius Staisiunas, Computer Science 3 yr., 3 gr.
 */

public class RealCPU {
    private final SimpleStringProperty PTR;
    private final SimpleStringProperty PC;
    private final SimpleStringProperty MD;
    private final SimpleStringProperty TMR;
    private final SimpleStringProperty SF;
    private final SimpleStringProperty SMR;
    private final SimpleStringProperty AX;
    private final SimpleStringProperty BX;
    private final SimpleStringProperty PI;
    private final SimpleStringProperty SI;
    private final SimpleStringProperty CH;
    private final SimpleStringProperty SHM;
    
    private final SharedMemoryTracker smt;
    
    public RealCPU(SharedMemoryTracker smt){
        PTR = new SimpleStringProperty("0");
        PC = new SimpleStringProperty("0");
        TMR = new SimpleStringProperty("a");
        MD = new SimpleStringProperty("0");
        SF = new SimpleStringProperty("0");
        BX = new SimpleStringProperty("0");
        AX = new SimpleStringProperty("0");
        SMR = new SimpleStringProperty("3f");
        SHM = new SimpleStringProperty("0000000000000000");
        CH = new SimpleStringProperty("000");
        PI = new SimpleStringProperty("0");
        SI = new SimpleStringProperty("0");
        
        this.smt = smt;
    }
    
    public void setPTR(int value){
        String sigByte = value < 16 ? "0" : ""; // If PTR is 0-F
        PTR.setValue("00" + sigByte + Integer.toHexString(value));
    }
    
    public void setPC(int value){
        String sigByte = value < 16 ? "0" : ""; // If PC is 0-F
        PC.setValue(sigByte + Integer.toHexString(value));
    }
    
    // Returns 0 if current VM still executes
    // Returns 1 if a switch needs to be performed
    public int decrTMRandCheck(int decr){
        if (decr > Integer.parseInt(TMR.get(), 16)){
            TMR.setValue("0");
        }
        
        if (Integer.parseInt(TMR.get(), 16) > 0){
            TMR.setValue(Integer.toHexString(Integer.parseInt(TMR.getValue(), 16) - decr));
        } else {
            // Should switch to other virtual machines
            TMR.setValue("a");
            return 1;
        }
        return 0;
    }
    
    public StringProperty ptrProperty(){
        return PTR;
    }
    
    public StringProperty pcProperty(){
        return PC;
    }
    
    public StringProperty smrProperty(){
        return SMR;
    }
    
    public StringProperty mdProperty(){
        return MD;
    }
    
    public StringProperty tmrProperty(){
        return TMR;
    }
    
    public StringProperty sfProperty(){
        return SF;
    }
    
    public StringProperty axProperty(){
        return AX;
    }
    
    public StringProperty bxProperty(){
        return BX;
    }
    
    public StringProperty piProperty(){
        return PI;
    }
    
    public StringProperty siProperty(){
        return SI;
    }
    
    public StringProperty chProperty(){
        return CH;
    }
    
    public StringProperty shmProperty(){
        return SHM;
    }
    
    public SharedMemoryTracker smt(){
        return smt;
    }
}