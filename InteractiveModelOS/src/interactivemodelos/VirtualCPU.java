/*
 */
package interactivemodelos;

import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

/**
 * @author Paulius Staisiunas, Informatika 3 k., 3 gr.
 */
public class VirtualCPU {
    private SimpleStringProperty PC;
    private SimpleStringProperty SF;
    private SimpleStringProperty AX;
    private SimpleStringProperty BX;
    
    public VirtualCPU(){
        PC = new SimpleStringProperty("0");
        SF = new SimpleStringProperty("0");
        AX = new SimpleStringProperty("0");
        BX = new SimpleStringProperty("0");
    }
    
    public void setPC(int value){ // galbut nereikalingas metodas
        String sigByte = value < 16 ? "0" : ""; // Jeigu PC yra 0-F
        PC.setValue(sigByte + Integer.toHexString(value));
    }
    
    public void setSF(int value){ // galbut nereikalingas metodas
        // galimos reiksmes:    D: 00001101 ZSC / C: 00001100 ZS / 9: 00001001 ZC / 5: 00000101 SC
        //                      0: 00000000 - / 1: 00000001 C / 8: 00001000 Z / 4: 00000100 S
        String val = Integer.toHexString(value);
        if (!val.equals("d") && !val.equals("c") && !val.equals("9") && !val.equals("5") && 
            !val.equals("0") && !val.equals("1") && !val.equals("8") && !val.equals("4")){
            //bloga reiksme
        } else {
            SF.setValue(val);
        }
    }
    
    public StringProperty pcProperty(){
        return PC;
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
}
