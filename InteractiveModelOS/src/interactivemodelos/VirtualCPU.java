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
    
    public VirtualCPU(){
        PC = new SimpleStringProperty("0");
    }
    
    public void setPC(int value){
        String sigByte = value < 16 ? "0" : ""; // Jeigu PC yra 0-F
        PC.setValue(sigByte + Integer.toHexString(value));
    }
    
    public StringProperty pcProperty(){
        return PC;
    }
}
