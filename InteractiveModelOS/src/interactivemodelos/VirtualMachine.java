/*
 */
package interactivemodelos;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

/**
 * @author Paulius Staisiunas, Informatika 3 k., 3 gr.
 */
public class VirtualMachine {
    private int ptr = 0;
    private final SimpleStringProperty[][] memory = new SimpleStringProperty[16][16];
    
    public VirtualMachine(){ 
        // Pirminis atminties uzkrovimas po 4 baitus
        for (int i = 0; i < 16; ++i){
            memory[i] = new SimpleStringProperty[16];
            for (int j = 0; j < 16; ++j){
                memory[i][j] = new SimpleStringProperty("0");
            }
        }
    }
    
    public void loadProgram(File file){
        Assembly ass = new Assembly();
        ArrayList<String> commands = null;
        
        try {
            commands = ass.parseFile(file);
            if (commands == null){
                throw new IOException("Empty program.");
            }
        } catch (StringIndexOutOfBoundsException | IOException ex) {
            Logger.getLogger(VirtualMachine.class.getName()).log(Level.SEVERE, null, ex);
        }
        
        // Uzpildoma atmintis instrukcijomis
        int block = 0, wrd = 0;
        for (String word : commands){
            if (word.equals("[STC]")){
                block = 2; // pereinama i codo blokus lastelese nuo 20...
                wrd = 0;
                continue;
            }
            
            memory[block][wrd].setValue(word);
            ++wrd;
            if (wrd == 16){
                wrd = 0;
                ++block;
            }
        }
    }
    
    public void setWord(int block, int word, String value) {
        memory[block][word].setValue(value);
    }

    public String getWord(int block, int word) {
        return memory[block][word].getValue();
    }

    public StringProperty memoryProperty(int block, int word) {
        return memory[block][word];
    }
    
    public void setVirtualWordProperty(int block, int word, StringProperty ssp){
        memory[block][word] = (SimpleStringProperty) ssp;
    }
}
