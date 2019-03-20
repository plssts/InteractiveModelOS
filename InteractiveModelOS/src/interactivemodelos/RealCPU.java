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
    
    public RealCPU(){
        PTR = new SimpleStringProperty("0");
        PC = new SimpleStringProperty("0");
    }
    
    public void setPTR(int value){
        String sigByte = value < 16 ? "0" : ""; // Jeigu PTR yra 0-F
        PTR.setValue("00" + sigByte + Integer.toHexString(value));
    }
    
    public void setPC(int value){
        String sigByte = value < 16 ? "0" : ""; // Jeigu PC yra 0-F
        PC.setValue(sigByte + Integer.toHexString(value));
    }
    
    public StringProperty ptrProperty(){
        return PTR;
    }
    
    public StringProperty pcProperty(){
        return PC;
    }
}
