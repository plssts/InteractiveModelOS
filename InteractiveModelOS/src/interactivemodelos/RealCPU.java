/*
 */
package interactivemodelos;

import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

/**
 * @author Paulius Staisiunas, Informatika 3 k., 3 gr.
 */
public class RealCPU {
    private SimpleStringProperty PTR;
    private SimpleStringProperty PC;
    private SimpleStringProperty MD;
    private SimpleStringProperty TMR;
    private SimpleStringProperty SF;
    private SimpleStringProperty SMR;
    private SimpleStringProperty AX;
    private SimpleStringProperty BX;
    private SimpleStringProperty PI;
    private SimpleStringProperty SI;
    private SimpleStringProperty CH;
    private SimpleStringProperty SHM;
    
    private SharedMemoryTracker smt;
    
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
        String sigByte = value < 16 ? "0" : ""; // Jeigu PTR yra 0-F
        PTR.setValue("00" + sigByte + Integer.toHexString(value));
    }
    
    public void setPC(int value){
        String sigByte = value < 16 ? "0" : ""; // Jeigu PC yra 0-F
        PC.setValue(sigByte + Integer.toHexString(value));
    }
    
    public void decrTMRandCheck(){
        if (Integer.parseInt(TMR.get(), 16) > 0){
            TMR.setValue(Integer.toHexString(Integer.parseInt(TMR.getValue(), 16) - 1));
        } else {
            // perduodamas valdymas kitai VM; siame projekte nerealizuojama.
            TMR.setValue("a");
        }
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
