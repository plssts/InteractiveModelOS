package interactivemodelos;

import java.util.ArrayList;

/**
 * @author Paulius Staisiunas, Computer Science 3 yr., 3 gr.
 */
public class Resource {
    protected String externalName;
    protected ArrayList<String> waitingProc = new ArrayList<>();
    protected String creator;
    protected String freedBy;
    protected String ownedBy;
    
    protected int ownedBlocks = 0;
    public String[] shmem = new String[16];
    
    public Resource(){
        for (int i = 0; i < 16; ++i){
            shmem[i] = " ";
        }
    }
    
    public void setShmem(String[] arr){
        shmem = arr;
    }
    
    public Resource(String externalName){
        this.externalName = externalName;
    }
    
    public String getName(){
        return externalName;
    }
    
    public ArrayList<String> getWP(){
        return waitingProc;
    }
    
    public void setWP(ArrayList<String> wp){
        waitingProc = wp;
    }
    
    public void setCreator(String cr){
        creator = cr;
    }
    
    public String getCreator(){
        return creator;
    }
    
    public void setOwned(String o){
        ownedBy = o;
    }
    
    public String getOwned(){
        return ownedBy;
    }
    
    public void setFreed(String f){
        freedBy = f;
    }
    
    public String getFreed(){
        return freedBy;
    }
    
     public void incrBlocks(int val){
        ownedBlocks += val;
    }
    
    public void decrBlocks(int val){
        ownedBlocks -= val;
    }
    
    public String getAllValues(){
        String output = "";
        output = "Name: " + externalName + "\nWaiting processes: \n";
        for (String s : waitingProc){
            output = output + s + "\n";
        }
        output += "Created by: " + creator + "\nFreed by: " + freedBy + "\nOwned by: " + ownedBy;
        
        if (externalName.equals("VirtualMemory")){
            output = output + "\nCurrently there are " + ownedBlocks + " memory blocks used.";
        }
        
        if (externalName.equals("SharedMemory")){
            output += "\n";
            for (String s: shmem){
                output = output + s + "\n";
            }
        }
        
        return output;
    }
    
    @Override
    public String toString(){
        return externalName;
    }
}
