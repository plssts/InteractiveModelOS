package resource;
import interactivemodelos.Resource;
/**
 * @author Paulius Staisiunas, Computer Science 3 yr., 3 gr.
 */
public class VirtualMemory extends Resource {
    protected int ownedBlocks = 0;
    
    public VirtualMemory(String externalName){
        super(externalName);
    }
    
    public void incrBlocks(int val){
        ownedBlocks += val;
    }
    
    public void decrBlocks(int val){
        ownedBlocks -= val;
    }
    
    @Override
    public String getAllValues(){
        String output = "";
        output = "Name: " + externalName + "\nWaiting processes: \n";
        for (String s : waitingProc){
            output = output + s + "\n";
        }
        output += "Created by: " + creator + "\nFreed by: " + freedBy + "\nOwned by: " + ownedBy;
        
        output = output + "\nCurrently there are " + ownedBlocks + " memory blocks used.";
        
        return output;
    }
}
