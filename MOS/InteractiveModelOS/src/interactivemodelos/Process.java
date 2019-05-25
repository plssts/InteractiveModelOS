/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package interactivemodelos;

/**
 *
 * @author LAPTOPELIS
 */
public class Process {
    private String externalName;
    private String status;
    private String waitResource;
    
    public Process(String externalName){
        this.externalName = externalName;
        status = "";
        waitResource = "";
    }
    
    public String getName(){
        return externalName;
    }
    
    public void setWR(String resource){
        waitResource = resource;
    }
    
    public void setStatus(String s){
        status = s;
    }
    public String getAllValues(){
        return externalName + "\n" + status + "\n" + waitResource;
    }
    
    @Override
    public String toString(){
        return externalName;
    }
}
