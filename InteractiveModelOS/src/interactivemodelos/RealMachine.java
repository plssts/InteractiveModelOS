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
    
    public SharedMemoryTracker getSharedMemoryTracker(){
        SimpleStringProperty[] shared = new SimpleStringProperty[16];
        for (int i = 0; i < 16; ++i){
            shared[i] = memory[63][i];
        }
        return new SharedMemoryTracker(shared);
    }
    
    public void loadVirtualMachine(RealCPU cpu, VirtualMachine vm){
        int pagingTableBlock = new Random().nextInt(63);    // Paskutinis 64 blokas yra bendra atmintis
        String ptrValue = Integer.toHexString(pagingTableBlock);
        System.out.println("\tIsskirta puslapiu lentele su PTR: " + ptrValue);
        
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
                if (Integer.parseInt(b, 16) == block || block == pagingTableBlock){
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
        
        System.out.println("\tIsskirti 16 virtualiu bloku");
        
        cpu.setPTR(Integer.parseInt(ptrValue, 16));
        for (int i = 0; i < 16; ++i){
            memory[pagingTableBlock][i].setValue(pagingTable[i]);
        }
        bindVirtualWordsToReal(Integer.parseInt(ptrValue, 16), vm, pagingTable);
        System.out.println("\tSujungti virtualieji blokai su realiaisiais");
    }
    
    public void bindVirtualWordsToReal(int ptr, VirtualMachine vm, String[] pagingTable){
        int currentVirtualBlock = 0;
        for (String pagingStr : pagingTable){
            int currentRealBlock = Integer.parseInt(pagingStr, 16);
            for (int i = 0; i < 16 ; ++i){
                // Puslapiavimo mechanizmas
                vm.setVirtualWordProperty(currentVirtualBlock, i, memoryProperty(currentRealBlock, i));
            }
            ++currentVirtualBlock;
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
}
