/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package interactivemodelos;

import java.util.ArrayList;

/**
 *
 * @author LAPTOPELIS
 */
public class Resource {
    private String externalName;
    private ArrayList<String> waitingProc = new ArrayList<>();
    private String creator;
    private String freedBy;
    private String ownedBy;
    
    public Resource(String externalName){
        this.externalName = externalName;
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
    
    public String getAllValues(){
        String output = "";
        output = "Name: " + externalName + "\nWaiting processes: \n";
        for (String s : waitingProc){
            output = output + s + "\n";
        }
        output += "Created by: " + creator + "\nFreed by: " + freedBy + "\nOwned by: " + ownedBy;
        return output;
    }
    
    @Override
    public String toString(){
        return externalName;
    }
}
