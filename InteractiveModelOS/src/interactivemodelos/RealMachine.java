/*
 */
package interactivemodelos;

import java.util.Random;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

/**
 * @author Paulius Staisiunas, Informatika 3 k., 3 gr.
 */
public class RealMachine {
    private int[][] memory;
    private final SimpleStringProperty word = new SimpleStringProperty("0000");
    //reikes 16*16 tokiu string properciu
    
    public RealMachine(){
        memory = new int[16][16];
        //memory[0][0].set("FF");
    }
    
    public void setWord(String scores) {
        this.word.setValue(scores);
        System.out.println("Is now " + this.getWord());
    }

    public String getWord() {
        return word.getValue();
    }

    public StringProperty wordProperty() {
        return word;
    }
    
    public int[][] getMemory(){
        return memory;
    }
    
    public int testUI(){
        // this sets a random value of the memory - the change should be visible
        // on the main stack pane
        return memory[0/*new Random().nextInt(16)*/][0/*new Random().nextInt(16)*/] = new Random().nextInt(256);
    }
}
