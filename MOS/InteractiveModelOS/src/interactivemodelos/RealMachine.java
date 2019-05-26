/*
Manages real memory and loads virtual machines (in this application - only one).
 */
package interactivemodelos;

import java.util.ArrayList;
import java.util.Random;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.util.Pair;

/**
 * @author Paulius Staisiunas, Computer Science 3 yr., 3 gr.
 */

public class RealMachine {
    private final SimpleStringProperty[][] memory;
    private ArrayList<Integer> allBlc;
    private String lastAllocPTR;
    
    public RealMachine(){
        allBlc = new ArrayList<>();
        // Initial memory loading
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
    
    public boolean checkIfMemoryAvailable(){
        return allBlc.size() <= 48;
    }
    
    public void removeBlocks(ArrayList<Integer> arr){
        allBlc.removeAll(arr);
    }
    
    public boolean checkFreeBlock(int block){
        for (Integer blocks : allBlc){
            if (block == blocks){
                return false;
            }
        }
        return true;
    }
    
    public boolean loadVirtualMachine(VirtualCPU cpu, VirtualMachine vm, Scheduler sch){
        if (!checkIfMemoryAvailable()){
            return false;
        }
        
        int pagingTableBlock;
        do {
            pagingTableBlock = new Random().nextInt(63); // Last block is shared memory
        } while(!checkFreeBlock(pagingTableBlock));
        
        String ptrValue = Integer.toHexString(pagingTableBlock);
        System.out.println("\tPaging tabele PTR: " + ptrValue);
        
        int allocatedBlocks = 0;
        String[] pagingTable = new String[16];              // Actual table
        for (int i = 0; i < 16; ++i){
            pagingTable[i] = "0";
        }
        int index = 0;
        
        while (allocatedBlocks < 16){
            boolean validBlock = true;
            int block = new Random().nextInt(63);           // Last block is shared memory
            for (String b : pagingTable){
                if (Integer.parseInt(b, 16) == block || block == pagingTableBlock || !checkFreeBlock(block)){
                    validBlock = false;
                }
            }
            // This block is valid - we take it
            if (validBlock){
                pagingTable[index] = Integer.toHexString(block);
                allBlc.add(block);
                ++index;
                ++allocatedBlocks;
            }
        }
        
        //cpu.setPTR(Integer.parseInt(ptrValue, 16));
        for (int i = 0; i < 16; ++i){
            memory[pagingTableBlock][i].setValue(pagingTable[i]);
        }
        bindVirtualWordsToReal(Integer.parseInt(ptrValue, 16), vm, pagingTable);
        allBlc.add(pagingTableBlock);
        sch.includeVM(ptrValue, new Pair(cpu, vm));
        lastAllocPTR = ptrValue;
        //System.out.println("\tVirtual blocks mapped to real blocks");
        return true;
    }
    
    public String getLastPTR(){
        return lastAllocPTR;
    }
    
    public void bindVirtualWordsToReal(int ptr, VirtualMachine vm, String[] pagingTable){
        int currentVirtualBlock = 0;
        for (String pagingStr : pagingTable){
            int currentRealBlock = Integer.parseInt(pagingStr, 16);
            for (int i = 0; i < 16 ; ++i){
                // Paging the memory
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