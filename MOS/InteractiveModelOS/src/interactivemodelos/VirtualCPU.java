/*
Manages virtual registers.
 */
package interactivemodelos;

import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

/**
 * @author Paulius Staisiunas, Computer Science 3 yr., 3 gr.
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
    
    public void setAll(StringProperty pc, StringProperty sf, StringProperty ax, StringProperty bx){
        PC = (SimpleStringProperty) pc;
        SF = (SimpleStringProperty) sf;
        AX = (SimpleStringProperty) ax;
        BX = (SimpleStringProperty) bx;
    }
    
    public void setPC(int value){
        String sigByte = value < 16 ? "0" : ""; // If PC is 0-F
        PC.setValue(sigByte + Integer.toHexString(value));
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