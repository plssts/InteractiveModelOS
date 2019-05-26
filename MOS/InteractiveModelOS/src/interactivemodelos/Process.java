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
public class Process {
    private String externalName;
    private String status;
    private String waitResource;
    private String parent;
    private ArrayList<String> children = new ArrayList<>();
    private ArrayList<String> createdRs = new ArrayList<>();
    private ArrayList<String> ownedRs = new ArrayList<>();
    
    public Process(String externalName){
        this.externalName = externalName;
        status = "";
        waitResource = "";
    }
    
    public String getName(){
        return externalName;
    }
    
    public void setParent(String p){
        parent = p;
    }
    
    public void setChildren(ArrayList<String> c){
        children = c;
    }
    
    public ArrayList<String> getChildren(){
        return children;
    }
    
    public void setCreatedRs(ArrayList<String> rs){
        createdRs = rs;
    }
    
    public ArrayList<String> getCreatedRs(){
        return createdRs;
    }
    
    public void setOwnedRs(ArrayList<String> rs){
        ownedRs = rs;
    }
    
    public ArrayList<String> getOwnedRs(){
        return ownedRs;
    }
    
    public void setWR(String resource){
        waitResource = resource;
    }
    
    public void setStatus(String s){
        status = s;
    }
    public String getAllValues(){
        String output = "";
        output += "Name: " + externalName + "\nStatus: " + status + "\nWaiting for resource: " + waitResource + "\nParent: " + parent + "\nChildren: \n";
        for (String s : children){
            output = output + s + "\n";
        }
        output += "Created resources: \n";
        for (String s : createdRs){
            output = output + s + "\n";
        }
        output += "Owned resources: \n";
        for (String s : ownedRs){
            output = output + s + "\n";
        }
        return output;
    }
    
    @Override
    public String toString(){
        return externalName;
    }
}
