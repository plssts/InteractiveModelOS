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
    
    public Process(String externalName){
        this.externalName = externalName;
    }
    
    public String getAllValues(){
        return "some test value of a process";
    }
    
    @Override
    public String toString(){
        return externalName;
    }
}
