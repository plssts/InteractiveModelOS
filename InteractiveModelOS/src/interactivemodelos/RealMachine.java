/*
 */
package interactivemodelos;

import java.util.Random;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

/**
 * @author Paulius Staisiunas, Informatika 3 k., 3 gr.
 */
public class RealMachine {
    private final SimpleStringProperty[][] memory;
    
    public RealMachine(){
        // Pirminis atminties uzkrovimas po 4 baitus
        memory = new SimpleStringProperty[64][16];
        for (int i = 0; i < 64; ++i){
            memory[i] = new SimpleStringProperty[16];
            for (int j = 0; j < 16; ++j){
                memory[i][j] = new SimpleStringProperty("0");
            }
        }
    }
    
    public void loadVirtualMachine(RealCPU cpu){
        int pagingTableBlock = new Random().nextInt(63);    // Paskutinis 64 blokas yra bendra atmintis
        String ptrValue = Integer.toHexString(pagingTableBlock);
        int allocatedBlocks = 0;
        String[] pagingTable = new String[16];              // Pati lentele ir jos reiksmes
        for (int i = 0; i < 16; ++i){
            pagingTable[i] = "0";
        }
        int index = 0;
        
        while (allocatedBlocks < 16){
            boolean validBlock = true;
            int block = new Random().nextInt(63);           // Paskutinis 64 blokas yra bendra atmintis
            for (String b : pagingTable){
                if (Integer.parseInt(b, 16) == block){
                    validBlock = false;
                }
            }
            // Tinkamas blokas - ji paimame atminciai
            if (validBlock){
                pagingTable[index] = Integer.toHexString(block);
                ++index;
                ++allocatedBlocks;
            }
        }
        
        cpu.setPTR(Integer.parseInt(ptrValue, 16));
        for (int i = 0; i < 16; ++i){
            memory[pagingTableBlock][i].setValue(pagingTable[i]);
        }
    }
    
    public void setWord(int block, int word, String value) {
        this.memory[block][word].setValue(value);
    }

    public String getWord(int block, int word) {
        return memory[block][word].getValue();
    }

    public StringProperty memoryProperty(int block, int word) {
        return memory[block][word];
    }
}
