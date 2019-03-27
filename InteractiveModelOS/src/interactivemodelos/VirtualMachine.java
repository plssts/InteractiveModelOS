/*
 */
package interactivemodelos;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.control.TextInputDialog;
import javafx.scene.paint.Color;

/**
 * @author Paulius Staisiunas, Informatika 3 k., 3 gr.
 */
public class VirtualMachine {
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
            System.out.println(ex);
            return;
        }
        
        // Uzpildoma atmintis instrukcijomis
        int block = 0, wrd = 0;
        for (String word : commands){
            System.out.println(word);
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
        }
    }
    
    public boolean executeCommand(VirtualCPU vcpu, RealCPU rcpu, Label stdinStatus, TextField stdout) throws IOException, NumberFormatException{
        int pc = Integer.parseInt(vcpu.pcProperty().getValue(), 16);
        int pcBlock = (pc / 16) + 2; // pradedama nuo 20 CODE segmento
        int pcWord = pc % 16;
        String position = memory[pcBlock][pcWord].getValue();
        System.out.println(position);
        
        if (position.startsWith("J+")){
            int offset = Integer.parseInt(position.substring(2, 4), 16);
            if ((pc + offset + 1) > 224){
                // pc 'islekia' is virtualios atminties reziu
                return false;
            }
            vcpu.setPC(pc + offset + 1);
            return true;
        }
        if (position.startsWith("J-")){
            int offset = Integer.parseInt(position.substring(2, 4), 16);
            if ((pc - offset + 1) < 0){
                // pc atsiduria DATA segmente
                return false;
            }
            vcpu.setPC(pc - offset + 1);
            return true;
        }
        
        if (position.startsWith("WW")){
            int srcBlock = -1, srcWord = -1;
            //try {
                srcBlock = Integer.parseInt(position.substring(2, 3), 16);
                srcWord = Integer.parseInt(position.substring(3, 4), 16);
            //} catch (NumberFormatException ex) {
                // neteisingas skaicius kode
            //}
            
            stdout.appendText(memory[srcBlock][srcWord].get());
            vcpu.setPC(pc+1);
            return true;
        }
        if (position.startsWith("RW")){
            int destBlock = -1, destWord = -1;
            //try {
                destBlock = Integer.parseInt(position.substring(2, 3), 16);
                destWord = Integer.parseInt(position.substring(3, 4), 16);
            //} catch (NumberFormatException ex) {
                // neteisingas skaicius kode
            //}
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
                //try {
                    memory[destBlock][destWord].setValue(Integer.toHexString((int)Long.parseLong(input, 16)));
                //} catch (NumberFormatException ex){
                    // neteisingas skaicius inpute
                //}
            } else {
                throw new IOException("Neteisingas eilutes formatavimas arba ilgis didesnis uz 4 baitus");
            }
            
            vcpu.setPC(pc+1);
            return true;
        }
        
        if (position.startsWith("SHL")){
            int id = Integer.parseInt(position.substring(3, 4), 16);
            String current = rcpu.shmProperty().get();
            if (current.charAt(id) == '1'){
                // jau uzrakinta
                return false;
            } else {
                char[] arr = current.toCharArray();
                arr[id] = '1';
                String result = String.valueOf(arr);
                rcpu.shmProperty().setValue(result);
                vcpu.setPC(pc+1);
                return true;
            }
        }
        if (position.startsWith("SHU")){
            int id = Integer.parseInt(position.substring(3, 4), 16);
            String current = rcpu.shmProperty().get();
            if (current.charAt(id) == '0'){
                // jau atrakinta
                return false;
            } else {
                char[] arr = current.toCharArray();
                arr[id] = '0';
                String result = String.valueOf(arr);
                rcpu.shmProperty().setValue(result);
                vcpu.setPC(pc+1);
                return true;
            }
        }
        
        switch(position){
            case "ADD ":
                ++pc;
                vcpu.setPC(pc);
                pcBlock = (pc / 16) + 2;
                pcWord = pc % 16;
                String registers = memory[pcBlock][pcWord].get();
                StringProperty first = null, second = null;
                switch (registers.substring(0, 2)){ // rezultato registras
                    case "AX":
                        first = vcpu.axProperty();
                        break;
                    case "BX":
                        first = vcpu.bxProperty();
                        break;
                    default:
                        throw new IOException("Neegzistuojantys ADD registrai");
                }
                switch (registers.substring(2, 4)){ // antrasis operandas
                    case "AX":
                        second = vcpu.axProperty();
                        break;
                    case "BX":
                        second = vcpu.bxProperty();
                        break;
                    default:
                        throw new IOException("Neegzistuojantys ADD registrai");
                }
                int result = (int)Long.parseLong(first.get(), 16) + (int)Long.parseLong(second.get(), 16);
                boolean sign = false, zero = false, carry = false;
                if (result < (int)Long.parseLong(first.get(), 16) || result < (int)Long.parseLong(second.get(), 16)){
                    carry = true;
                }
                if (result == 0){
                    zero = true;
                }
                if (result < 0){
                    sign = true;
                }
                vcpu.sfProperty().setValue(arrangeFlags(sign, zero, carry));
                first.setValue(Integer.toHexString(result));
                vcpu.setPC(pc+1);
                return true;
                
            case "SUB ":
                ++pc;
                vcpu.setPC(pc);
                pcBlock = (pc / 16) + 2;
                pcWord = pc % 16;
                registers = memory[pcBlock][pcWord].get();
                first = null; second = null;
                switch (registers.substring(0, 2)){ // rezultato registras
                    case "AX":
                        first = vcpu.axProperty();
                        break;
                    case "BX":
                        first = vcpu.bxProperty();
                        break;
                    default:
                        throw new IOException("Neegzistuojantys SUB registrai");
                }
                switch (registers.substring(2, 4)){ // antrasis operandas
                    case "AX":
                        second = vcpu.axProperty();
                        break;
                    case "BX":
                        second = vcpu.bxProperty();
                        break;
                    default:
                        throw new IOException("Neegzistuojantys SUB registrai");
                }
                result = (int)Long.parseLong(first.get(), 16) - (int)Long.parseLong(second.get(), 16);
                sign = false; zero = false; carry = false;
                if ((int)Long.parseLong(first.get(), 16) < (int)Long.parseLong(second.get(), 16)){
                    carry = true;
                }
                if (result == 0){
                    zero = true;
                }
                if (result < 0){
                    sign = true;
                }
                vcpu.sfProperty().setValue(arrangeFlags(sign, zero, carry));
                first.setValue(Integer.toHexString(result));
                vcpu.setPC(pc+1);
                return true;
                
            case "CMP ":
                ++pc;
                vcpu.setPC(pc);
                pcBlock = (pc / 16) + 2;
                pcWord = pc % 16;
                registers = memory[pcBlock][pcWord].get();
                first = null; second = null;
                switch (registers.substring(0, 2)){ // pirmasis operandas
                    case "AX":
                        first = vcpu.axProperty();
                        break;
                    case "BX":
                        first = vcpu.bxProperty();
                        break;
                    default:
                        throw new IOException("Neegzistuojantys CMP registrai");
                }
                switch (registers.substring(2, 4)){ // antrasis operandas
                    case "AX":
                        second = vcpu.axProperty();
                        break;
                    case "BX":
                        second = vcpu.bxProperty();
                        break;
                    default:
                        throw new IOException("Neegzistuojantys CMP registrai");
                }
                sign = false; zero = false; carry = false;
                if ((int)Long.parseLong(first.get(), 16) == (int)Long.parseLong(second.get(), 16)){
                    zero = true;
                } else if ((int)Long.parseLong(first.get(), 16) < (int)Long.parseLong(second.get(), 16)){
                    zero = false; carry = true;
                } else if ((int)Long.parseLong(first.get(), 16) > (int)Long.parseLong(second.get(), 16)){
                    zero = false; carry = false;
                } else if ((int)Long.parseLong(first.get(), 16) <= (int)Long.parseLong(second.get(), 16)){
                    zero = true; carry = true;
                } else if ((int)Long.parseLong(first.get(), 16) >= (int)Long.parseLong(second.get(), 16)){
                    carry = false;
                }
                vcpu.sfProperty().setValue(arrangeFlags(sign, zero, carry));
                vcpu.setPC(pc+1);
                return true;
                
            case "MUL ":
                ++pc;
                vcpu.setPC(pc);
                pcBlock = (pc / 16) + 2;
                pcWord = pc % 16;
                registers = memory[pcBlock][pcWord].get();
                first = null; second = null;
                switch (registers.substring(0, 2)){ // rezultato registras
                    case "AX":
                        first = vcpu.axProperty();
                        break;
                    case "BX":
                        first = vcpu.bxProperty();
                        break;
                    default:
                        throw new IOException("Neegzistuojantys MUL registrai");
                }
                switch (registers.substring(2, 4)){ // antrasis operandas
                    case "AX":
                        second = vcpu.axProperty();
                        break;
                    case "BX":
                        second = vcpu.bxProperty();
                        break;
                    default:
                        throw new IOException("Neegzistuojantys MUL registrai");
                }
                result = (int)Long.parseLong(first.get(), 16) * (int)Long.parseLong(second.get(), 16);
                sign = false; zero = false; carry = false;
                if (result < (int)Long.parseLong(first.get(), 16) || result < (int)Long.parseLong(second.get(), 16)){
                    carry = true;
                }
                if (result == 0){
                    zero = true;
                }
                if (result < 0){
                    sign = true;
                }
                vcpu.sfProperty().setValue(arrangeFlags(sign, zero, carry));
                first.setValue(Integer.toHexString(result));
                vcpu.setPC(pc+1);
                return true;
                
            case "DIV ":
                ++pc;
                vcpu.setPC(pc);
                pcBlock = (pc / 16) + 2;
                pcWord = pc % 16;
                registers = memory[pcBlock][pcWord].get();
                first = null; second = null;
                switch (registers.substring(0, 2)){ // rezultato registras
                    case "AX":
                        first = vcpu.axProperty();
                        break;
                    case "BX":
                        first = vcpu.bxProperty();
                        break;
                    default:
                        throw new IOException("Neegzistuojantys DIV registrai");
                }
                switch (registers.substring(2, 4)){ // antrasis operandas
                    case "AX":
                        second = vcpu.axProperty();
                        break;
                    case "BX":
                        second = vcpu.bxProperty();
                        break;
                    default:
                        throw new IOException("Neegzistuojantys DIV registrai");
                }
                if ((int)Long.parseLong(second.get(), 16) == 0){
                    throw new IOException("Daliklis yra 0");
                }
                
                result = (int)Long.parseLong(first.get(), 16) / (int)Long.parseLong(second.get(), 16);
                sign = false; zero = false; carry = false;
                if ((int)Long.parseLong(first.get(), 16) < (int)Long.parseLong(second.get(), 16)){
                    carry = true;
                }
                if (result == 0){
                    zero = true;
                }
                if (result < 0){
                    sign = true;
                }
                vcpu.sfProperty().setValue(arrangeFlags(sign, zero, carry));
                first.setValue(Integer.toHexString(result));
                vcpu.setPC(pc+1);
                return true;
                
            case "MOD ":
                ++pc;
                vcpu.setPC(pc);
                pcBlock = (pc / 16) + 2;
                pcWord = pc % 16;
                registers = memory[pcBlock][pcWord].get();
                first = null; second = null;
                switch (registers.substring(0, 2)){ // rezultato registras
                    case "AX":
                        first = vcpu.axProperty();
                        break;
                    case "BX":
                        first = vcpu.bxProperty();
                        break;
                    default:
                        throw new IOException("Neegzistuojantys MOD registrai");
                }
                switch (registers.substring(2, 4)){ // antrasis operandas
                    case "AX":
                        second = vcpu.axProperty();
                        break;
                    case "BX":
                        second = vcpu.bxProperty();
                        break;
                    default:
                        throw new IOException("Neegzistuojantys MOD registrai");
                }
                if ((int)Long.parseLong(second.get(), 16) == 0){
                    throw new IOException("Daliklis yra 0");
                }
                
                result = (int)Long.parseLong(first.get(), 16) % (int)Long.parseLong(second.get(), 16);
                sign = false; zero = false; carry = false;
                if ((int)Long.parseLong(first.get(), 16) < (int)Long.parseLong(second.get(), 16)){
                    carry = true;
                }
                if (result == 0){
                    zero = true;
                }
                if (result < 0){
                    sign = true;
                }
                vcpu.sfProperty().setValue(arrangeFlags(sign, zero, carry));
                first.setValue(Integer.toHexString(result));
                vcpu.setPC(pc+1);
                return true;
                
            case "MOV ":
                ++pc;
                vcpu.setPC(pc);
                pcBlock = (pc / 16) + 2;
                pcWord = pc % 16;
                registers = memory[pcBlock][pcWord].get();
                first = null; second = null;
                switch (registers.substring(0, 2)){ // rezultato registras/atmintis
                    case "AX":
                        first = vcpu.axProperty();
                        break;
                    case "BX":
                        first = vcpu.bxProperty();
                        break;
                    default:
                        first = memory[Integer.parseInt(registers.substring(0, 1), 16)][Integer.parseInt(registers.substring(1, 2), 16)];
                }
                switch (registers.substring(2, 4)){ // antrasis operandas
                    case "AX":
                        second = vcpu.axProperty();
                        break;
                    case "BX":
                        second = vcpu.bxProperty();
                        break;
                    default:
                        second = memory[Integer.parseInt(registers.substring(2, 3), 16)][Integer.parseInt(registers.substring(3, 4), 16)];
                }
                first.setValue(second.get());
                vcpu.setPC(pc+1);
                return true;
                
            case "MOVC":
                ++pc;
                vcpu.setPC(pc);
                pcBlock = (pc / 16) + 2;
                pcWord = pc % 16;
                String fword = memory[pcBlock][pcWord].get();
                ++pc;
                vcpu.setPC(pc);
                pcBlock = (pc / 16) + 2;
                pcWord = pc % 16;
                String sword = memory[pcBlock][pcWord].get();
                String constant = fword + sword;
                vcpu.axProperty().setValue(Integer.toHexString((int)Long.parseLong(constant, 16)));
                vcpu.setPC(pc+1);
                return true;
                
            case "JEQL":
                ++pc;
                vcpu.setPC(pc);
                pcBlock = (pc / 16) + 2;
                pcWord = pc % 16;
                registers = memory[pcBlock][pcWord].get();
                switch (registers.substring(1, 2)){ // + arba -
                    case "+":
                        int offset = Integer.parseInt(registers.substring(2, 4), 16);
                        if ((Integer.parseInt(vcpu.sfProperty().get(), 16) & 0b00001000) == 0b00001000){
                            if ((pc + offset + 1) > 224){
                                // pc 'islekia' is atminties reziu
                                return false;
                            }
                            vcpu.setPC(pc + offset + 1);
                            return true;
                        }
                        else {
                            vcpu.setPC(pc+1);
                            return true;
                        }
                        
                    case "-":
                        offset = Integer.parseInt(registers.substring(2, 4), 16);
                        if ((Integer.parseInt(vcpu.sfProperty().get(), 16) & 0b00001000) == 0b00001000){
                            if ((pc - offset + 1) < 0){
                                // pc atsiduria DATA segmente
                                return false;
                            }
                            vcpu.setPC(pc - offset + 1);
                            return true;
                        }
                        else {
                            vcpu.setPC(pc+1);
                            return true;
                        }
                    default:
                        throw new IOException("Neteisingas poslinkio formatas");
                }
                
            case "JAEQ":
                ++pc;
                vcpu.setPC(pc);
                pcBlock = (pc / 16) + 2;
                pcWord = pc % 16;
                registers = memory[pcBlock][pcWord].get();
                switch (registers.substring(1, 2)){ // + arba -
                    case "+":
                        int offset = Integer.parseInt(registers.substring(2, 4), 16);
                        if ((Integer.parseInt(vcpu.sfProperty().get(), 16) & 0b00000001) == 0b00000000){
                            if ((pc + offset + 1) > 224){
                                // pc 'islekia' is atminties reziu
                                return false;
                            }
                            vcpu.setPC(pc + offset + 1);
                            return true;
                        }
                        else {
                            vcpu.setPC(pc+1);
                            return true;
                        }
                        
                    case "-":
                        offset = Integer.parseInt(registers.substring(2, 4), 16);
                        if ((Integer.parseInt(vcpu.sfProperty().get(), 16) & 0b00000001) == 0b00000000){
                            if ((pc - offset + 1) <= 0){
                                // pc atsiduria DATA segmente
                                return false;
                            }
                            vcpu.setPC(pc - offset + 1);
                            return true;
                        }
                        else {
                            vcpu.setPC(pc+1);
                            return true;
                        }
                    default:
                        throw new IOException("Neteisingas poslinkio formatas");
                }
                
            case "JBEQ":
                ++pc;
                vcpu.setPC(pc);
                pcBlock = (pc / 16) + 2;
                pcWord = pc % 16;
                registers = memory[pcBlock][pcWord].get();
                switch (registers.substring(1, 2)){ // + arba -
                    case "+":
                        int offset = Integer.parseInt(registers.substring(2, 4), 16);
                        if ((Integer.parseInt(vcpu.sfProperty().get(), 16) & 0b00001001) == 0b00001001 || 
                            (Integer.parseInt(vcpu.sfProperty().get(), 16) & 0b00000001) == 0b00000001 ||
                            (Integer.parseInt(vcpu.sfProperty().get(), 16) & 0b00001000) == 0b00001000){
                            if ((pc + offset + 1) > 224){
                                // pc 'islekia' is atminties reziu
                                return false;
                            }
                            vcpu.setPC(pc + offset + 1);
                            return true;
                        }
                        else {
                            vcpu.setPC(pc+1);
                            return true;
                        }
                        
                    case "-":
                        offset = Integer.parseInt(registers.substring(2, 4), 16);
                        if ((Integer.parseInt(vcpu.sfProperty().get(), 16) & 0b00001001) == 0b00001001 || 
                            (Integer.parseInt(vcpu.sfProperty().get(), 16) & 0b00000001) == 0b00000001 ||
                            (Integer.parseInt(vcpu.sfProperty().get(), 16) & 0b00001000) == 0b00001000){
                            if ((pc - offset + 1) < 0){
                                // pc atsiduria DATA segmente
                                return false;
                            }
                            vcpu.setPC(pc - offset + 1);
                            return true;
                        }
                        else {
                            vcpu.setPC(pc+1);
                            return true;
                        }
                    default:
                        throw new IOException("Neteisingas poslinkio formatas");
                }
                
            case "JABV":
                ++pc;
                vcpu.setPC(pc);
                pcBlock = (pc / 16) + 2;
                pcWord = pc % 16;
                registers = memory[pcBlock][pcWord].get();
                switch (registers.substring(1, 2)){ // + arba -
                    case "+":
                        int offset = Integer.parseInt(registers.substring(2, 4), 16);
                        if ((Integer.parseInt(vcpu.sfProperty().get(), 16) & 0b00001001) == 0b00000000){
                            if ((pc + offset + 1) > 224){
                                // pc 'islekia' is atminties reziu
                                return false;
                            }
                            vcpu.setPC(pc + offset + 1);
                            return true;
                        }
                        else {
                            vcpu.setPC(pc+1);
                            return true;
                        }
                        
                    case "-":
                        offset = Integer.parseInt(registers.substring(2, 4), 16);
                        if ((Integer.parseInt(vcpu.sfProperty().get(), 16) & 0b00001001) == 0b00000000){
                            if ((pc - offset + 1) < 0){
                                // pc atsiduria DATA segmente
                                return false;
                            }
                            vcpu.setPC(pc - offset + 1);
                            return true;
                        }
                        else {
                            vcpu.setPC(pc+1);
                            return true;
                        }
                    default:
                        throw new IOException("Neteisingas poslinkio formatas");
                }
                
            case "JBLW":
                ++pc;
                vcpu.setPC(pc);
                pcBlock = (pc / 16) + 2;
                pcWord = pc % 16;
                registers = memory[pcBlock][pcWord].get();
                switch (registers.substring(1, 2)){ // + arba -
                    case "+":
                        int offset = Integer.parseInt(registers.substring(2, 4), 16);
                        if ((Integer.parseInt(vcpu.sfProperty().get(), 16) & 0b00000001) == 0b00000001){
                            if ((pc + offset + 1) > 224){
                                // pc 'islekia' is atminties reziu
                                return false;
                            }
                            vcpu.setPC(pc + offset + 1);
                            return true;
                        }
                        else {
                            vcpu.setPC(pc+1);
                            return true;
                        }
                        
                    case "-":
                        offset = Integer.parseInt(registers.substring(2, 4), 16);
                        if ((Integer.parseInt(vcpu.sfProperty().get(), 16) & 0b00000001) == 0b00000001){
                            if ((pc - offset + 1) < 0){
                                // pc atsiduria DATA segmente
                                return false;
                            }
                            vcpu.setPC(pc - offset + 1);
                            return true;
                        }
                        else {
                            vcpu.setPC(pc+1);
                            return true;
                        }
                    default:
                        throw new IOException("Neteisingas poslinkio formatas");
                }
                
            case "SHW ":
                ++pc;
                vcpu.setPC(pc);
                pcBlock = (pc / 16) + 2;
                pcWord = pc % 16;
                registers = memory[pcBlock][pcWord].get();
                int sharedWord = Integer.parseInt(registers.substring(1, 2), 16);
                if (rcpu.shmProperty().get().charAt(sharedWord) == '1'){
                    first = rcpu.smt().memoryProperty(sharedWord);
                    second = memory[Integer.parseInt(registers.substring(2, 3), 16)][Integer.parseInt(registers.substring(3, 4), 16)];
                    first.setValue(second.get());
                    vcpu.setPC(pc+1);
                    return true;
                } else {
                    // dirbama su neuzrakinta atmintimi
                    return false;
                }
                
            case "SHR ":
                ++pc;
                vcpu.setPC(pc);
                pcBlock = (pc / 16) + 2;
                pcWord = pc % 16;
                registers = memory[pcBlock][pcWord].get();
                sharedWord = Integer.parseInt(registers.substring(3, 4), 16);
                if (rcpu.shmProperty().get().charAt(sharedWord) == '1'){
                    second = rcpu.smt().memoryProperty(sharedWord);
                    first = memory[Integer.parseInt(registers.substring(0, 1), 16)][Integer.parseInt(registers.substring(1, 2), 16)];
                    first.setValue(second.get());
                    vcpu.setPC(pc+1);
                    return true;
                } else {
                    // dirbama su neuzrakinta atmintimi
                    return false;
                }
                
            case "HALT":
                System.out.println("\u001B[31mPasiekta programos pabaiga.\u001B[0m");
                return false;
        }
        
        // neegzistuojanti komanda
        return false;
    }
    
    private String arrangeFlags(boolean sign, boolean zero, boolean carry){
        return sign && zero && carry ? "d" : zero && sign && !carry ? "c" : zero && carry && !sign ? "9" :
                sign && carry && !zero ? "5" : !sign && !zero && !carry ? "0" : carry && !zero && !sign ? "1" :
                zero && !carry && !sign ? "8" : "4";
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
