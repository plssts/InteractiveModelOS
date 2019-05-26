/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package resource;
import interactivemodelos.Resource;
/**
 *
 * @author LAPTOPELIS
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
