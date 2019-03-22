/*
 */
package interactivemodelos;

import java.awt.event.InputMethodEvent;
import java.awt.event.InputMethodListener;
import java.awt.event.KeyEvent;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;
import javafx.animation.PauseTransition;
import javafx.animation.Timeline;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.event.Event;
import javafx.event.EventHandler;
import javafx.event.EventType;
import javafx.scene.control.Button;
import javafx.scene.control.DialogPane;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.control.TextInputDialog;
import javafx.scene.paint.Color;
import javafx.stage.Modality;

/**
 * @author Paulius Staisiunas, Informatika 3 k., 3 gr.
 */
public class VirtualMachine {
    private int ptr = 0;
    private Assembly ass;
    private final SimpleStringProperty[][] memory = new SimpleStringProperty[16][16];
    
    public VirtualMachine(){ 
        // Pirminis atminties uzkrovimas po 4 baitus
        for (int i = 0; i < 16; ++i){
            memory[i] = new SimpleStringProperty[16];
            for (int j = 0; j < 16; ++j){
                memory[i][j] = new SimpleStringProperty("0");
            }
        }
        ass = new Assembly();
    }
    
    public void loadProgram(File file){
        ArrayList<String> commands = null;
        
        try {
            commands = ass.parseFile(file);
            if (commands == null){
                throw new IOException("Tuscia programa");
            }
        } catch (StringIndexOutOfBoundsException | IOException ex) {
            Logger.getLogger(VirtualMachine.class.getName()).log(Level.SEVERE, null, ex);
        }
        
        // Uzpildoma atmintis instrukcijomis
        int block = 0, wrd = 0;
        for (String word : commands){
            if (word.equals("[STC]")){
                block = 2; // pereinama i code blokus lastelese nuo 20...
                wrd = 0;
                continue;
            }
            
            memory[block][wrd].setValue(word);
            ++wrd;
            if (wrd == 16){
                wrd = 0;
                ++block;
            }
            
            // padaryti exception, kai block >15 pasidaro (netelpa programa)
        }
    }
    
    public boolean executeCommand(VirtualCPU vcpu, Label stdinStatus, TextField stdout) throws IOException{
        int pc = Integer.parseInt(vcpu.pcProperty().getValue(), 16);
        int pcBlock = (pc / 16) + 2; // pradedama nuo 20 CODE segmento
        int pcWord = pc % 16;
        String position = memory[pcBlock][pcWord].getValue();
        System.out.println(position);
        
        if (position.startsWith("J+")){
            
        }
        if (position.startsWith("J-")){
            
        }
        
        if (position.startsWith("WW")){
            int srcBlock = -1, srcWord = -1;
            try {
                srcBlock = Integer.parseInt(position.substring(2, 3), 16);
                srcWord = Integer.parseInt(position.substring(3, 4), 16);
            } catch (NumberFormatException ex) {
                // neteisingas skaicius kode
            }
            
            stdout.appendText(memory[srcBlock][srcWord].get());
            vcpu.setPC(pc+1);
            return true;
        }
        if (position.startsWith("RW")){
            int destBlock = -1, destWord = -1;
            try {
                destBlock = Integer.parseInt(position.substring(2, 3), 16);
                destWord = Integer.parseInt(position.substring(3, 4), 16);
            } catch (NumberFormatException ex) {
                // neteisingas skaicius kode
            }
            TextInputDialog dialog = new TextInputDialog();
            dialog.setResizable(false);
            dialog.setHeaderText("");
            dialog.setWidth(10);
            dialog.setGraphic(null);
            dialog.setTitle("Ivesties irenginys");
            stdinStatus.setTextFill(Color.DARKGREEN);
            stdinStatus.setText("AKTYVUS");
            dialog.showAndWait();
            stdinStatus.setText("NEAKTYVUS");
            stdinStatus.setTextFill(Color.DARKRED);
            String input = dialog.getEditor().getText();
            System.out.println(input);
            if (input.startsWith("\"")){ // Ivestas string, ne skaicius
                if (input.endsWith("\"") && input.length() < 7){
                    memory[destBlock][destWord].setValue(input.substring(1, input.length() - 1));
                }
                else {
                    throw new IOException("Neteisingas eilutes formatavimas arba ilgis didesnis uz 4 baitus");
                }
            }
            else if (input.length() < 9){ // Ivestas skaicius (16-aineje sistemoje)
                try {
                    memory[destBlock][destWord].setValue(Integer.toHexString(Integer.parseInt(input, 16)));
                } catch (NumberFormatException ex){
                    // neteisingas skaicius inpute
                }
            } else {
                throw new IOException("Neteisingas eilutes formatavimas arba ilgis didesnis uz 4 baitus");
            }
            
            vcpu.setPC(pc+1);
            return true;
        }
        
        if (position.startsWith("SHL")){
            
        }
        if (position.startsWith("SHU")){
            
        }
        
        switch(position){
            case "ADD ":
                
            case "SUB ":
                
            case "CMP ":
                
            case "MUL ":
                
            case "DIV ":
                
            case "MOD ":
                
            case "MOV ":
                
            case "MOVC":
                
            case "JEQL":
                
            case "JAEQ":
                
            case "JBEQ":
                
            case "JABV":
                
            case "JBLW":
                
            case "SHW ":
                
            case "SHR ":
                
            case "HALT":
                return false;
        }
        
        vcpu.setPC(pc+1);
        return true;
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
    
    public void setVirtualWordProperty(int block, int word, StringProperty ssp){
        memory[block][word] = (SimpleStringProperty) ssp;
    }
}
