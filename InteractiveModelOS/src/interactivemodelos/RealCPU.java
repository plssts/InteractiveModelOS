/*
 */
package interactivemodelos;

import javafx.beans.property.SimpleStringProperty;

/**
 * @author Paulius Staisiunas, Informatika 3 k., 3 gr.
 */
public class RealCPU {
    private SimpleStringProperty PTR;
    
    public RealCPU(){
        PTR = new SimpleStringProperty("0");
    }
    
    public void setPTR(int value){
        PTR.setValue("00" + Integer.toString(value));
        System.out.println("PTR to " + value);
    }
}
