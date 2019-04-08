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
    private int startPC = 0;
    
    private boolean nonCommandStep = false;
    private boolean setModeTo0 = false;
    private boolean setModeTo1 = false;
    private boolean throwableInt = false;
    private String nextPIval = "";
    private String nextSIval = "";
    
    private boolean wwChannelOpened = false;
    private boolean wwNeedsOpening = false;
    private boolean wwNeedsClosing = false;
    
    private boolean contentProcessed = false;
    
    private boolean rwChannelOpened = false;
    private boolean rwNeedsOpening = false;
    private boolean rwNeedsClosing = false;
    
    private boolean shmNeedsLocking = false;
    private boolean shmNeedsUnlocking = false;
    
    // nors galetu logiskai buti vienas, tik priesingu reiksmiu,
    // siuo atveju prasme programine, o ne logines busenos
    private boolean shmLocked = false;
    private boolean shmUnlocked = false;
    
    private boolean finaliseSHL = false;
    private boolean finaliseSHU = false;
    private int nextSHword = -1;
    
    private boolean shrNeedsStart = false;
    private boolean shwNeedsStart = false;
    private boolean finaliseSHR = false;
    private boolean finaliseSHW = false;
    
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
    
    public int getInitialPC(){
        return startPC;
    }
    
    public void loadProgram(File file, VirtualCPU vcpu){
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
            if (word.startsWith("[STC]")){
                int codeStart = Integer.parseInt(word.split(" ")[1], 16);
                startPC = codeStart;
                block = codeStart / 16;
                wrd = codeStart % 16;
                System.out.println(Integer.toHexString(block) + Integer.toHexString(wrd));
                vcpu.pcProperty().setValue(Integer.toHexString(block) + Integer.toHexString(wrd));
                System.out.println("vcpu PC yra: " + vcpu.pcProperty().get());
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
    
    public void clearBooleans(){
        nonCommandStep = false;
        setModeTo0 = false;
        setModeTo1 = false;
        throwableInt = false;
        nextPIval = "";
        nextSIval = "";
        wwChannelOpened = false;
        wwNeedsOpening = false;
        wwNeedsClosing = false;
        rwChannelOpened = false;
        rwNeedsOpening = false;
        rwNeedsClosing = false;
        shmNeedsLocking = false;
        shmNeedsUnlocking = false;
        shmLocked = false;
        shmUnlocked = false;
        finaliseSHL = false;
        finaliseSHU = false;
        nextSHword = -1;
        shrNeedsStart = false;
        shwNeedsStart = false;
        finaliseSHR = false;
        finaliseSHW = false;
        //contentProcessed = false;
    }
    
    public void reset(){
        clearBooleans();
        contentProcessed = false;
    }
    
    public boolean executeCommand(VirtualCPU vcpu, RealCPU rcpu, Label stdinStatus, TextField stdout) throws IOException, NumberFormatException{
        if (nonCommandStep){
            //System.out.println("NON-CMD");
            if (setModeTo0){
                rcpu.mdProperty().setValue("0");
                //setModeTo0 = false;
                //nonCommandStep = false;
                //clearBooleans();
                if (throwableInt){
                    String message = "";
                    switch(nextPIval){
                        case "1":
                            message = "Neteisingas adresas";
                            break;
                        case "2":
                            message = "Neteisingas operacijos kodas";
                            break;
                        case "3":
                            message = "Neteisingas priskyrimas";
                            break;
                        case "5":
                            message = "Bandymas naudotis bendraja atmintimi jos neuzrakinus";
                            break;
                    }
                    throw new IOException("[INT] " + message + " [INT]");
                }
                clearBooleans();
                return true;
                
            } 
            if (setModeTo1){
                rcpu.mdProperty().setValue("1");
                setModeTo1 = false;
                return true;
                
            } 
            if (!nextPIval.isEmpty()){
                rcpu.piProperty().setValue(nextPIval);
                setModeTo0 = true;
                if (!nextPIval.equals("4")){
                    throwableInt = true;
                }
                else {
                    stdout.setText("[INT] Perpildymas [INT]");
                }
                return true;
                
            } 
            if (!nextSIval.isEmpty()){
                rcpu.siProperty().setValue(nextSIval);
                //setModeTo0 = true;
                //if (nextSIval.equals("0")){
                    //setModeTo0 = true;
                //} else {
                    //nonCommandStep = false;
                //}
                nextSIval = "";
                return true;
                
            }
            if (wwNeedsOpening){
                wwChannelOpened = true;
                wwNeedsOpening = false;
                nonCommandStep = false;
                char[] temp = rcpu.chProperty().get().toCharArray();
                temp[1] = '1';
                rcpu.chProperty().setValue(String.valueOf(temp));
                return true;
                
            }
            if (wwNeedsClosing){
                wwChannelOpened = false;
                wwNeedsClosing = false;
                setModeTo0 = true;
                char[] temp = rcpu.chProperty().get().toCharArray();
                temp[1] = '0';
                rcpu.chProperty().setValue(String.valueOf(temp));
                return true;
            }
            
            if (rwNeedsOpening){
                rwChannelOpened = true;
                rwNeedsOpening = false;
                nonCommandStep = false;
                char[] temp = rcpu.chProperty().get().toCharArray();
                temp[0] = '1';
                rcpu.chProperty().setValue(String.valueOf(temp));
                return true;
                
            }
            if (rwNeedsClosing){
                rwChannelOpened = false;
                rwNeedsClosing = false;
                setModeTo0 = true;
                char[] temp = rcpu.chProperty().get().toCharArray();
                temp[0] = '0';
                rcpu.chProperty().setValue(String.valueOf(temp));
                return true;
            }
            
            if (shmNeedsLocking){
                shmLocked = true;
                shmNeedsLocking = false;
                nonCommandStep = false;
                //finaliseSHL = true;
                char[] temp = rcpu.shmProperty().get().toCharArray();
                temp[nextSHword] = '1';
                rcpu.shmProperty().setValue(String.valueOf(temp));
                return true;
                
            }
            if (shmNeedsUnlocking){
                shmUnlocked = true;
                shmNeedsUnlocking = false;
                nonCommandStep = false;
                //finaliseSHL = true;
                char[] temp = rcpu.shmProperty().get().toCharArray();
                temp[nextSHword] = '0';
                rcpu.shmProperty().setValue(String.valueOf(temp));
                return true;
                
            }
            
            if (finaliseSHL){
                /*if (nextSIval.equals("0")){
                    return true;
                }*/
                finaliseSHL = false;
                setModeTo0 = true;
                return true;
            }
            
            if (finaliseSHU){
                /*if (nextSIval.equals("0")){
                    return true;
                }*/
                finaliseSHU = false;
                setModeTo0 = true;
                return true;
            }
            
            if (shrNeedsStart){
                nonCommandStep = false;
                shrNeedsStart = false;
                return true;
            }
            if (shwNeedsStart){
                nonCommandStep = false;
                shwNeedsStart = false;
                return true;
            }
            if (finaliseSHR){
                finaliseSHR = false;
                //System.out.println("YUP");
                setModeTo0 = true;
                return true;
            }
            if (finaliseSHW){
                finaliseSHW = false;
                //System.out.println("YUP");
                setModeTo0 = true;
                return true;
            }
        }
        
        int pc = Integer.parseInt(vcpu.pcProperty().getValue(), 16);
        int pcBlock = (pc / 16) /*+ 2*/; // pradedama nuo 20 CODE segmento
        int pcWord = pc % 16;
        String position = memory[pcBlock][pcWord].getValue();
        
        if (rcpu.siProperty().get().equals("7")){ // HALT pasiektas
            return false;
        }
        
        System.out.println(position);
        
        try {
            if (position.startsWith("J+")){
                int offset = Integer.parseInt(position.substring(2, 4), 16);
                if ((pc + offset + 1) > 255){
                    // pc 'islekia' is virtualios atminties reziu
                    nextPIval = "1";
                    nonCommandStep = true;
                    setModeTo1 = true;
                    rcpu.decrTMRandCheck(1);
                    return true;
                }
                vcpu.setPC(pc + offset + 1);
                rcpu.decrTMRandCheck(1);
                return true;
            }
            if (position.startsWith("J-")){
                int offset = Integer.parseInt(position.substring(2, 4), 16);
                if ((pc - offset + 1) < 0){
                    // pc
                    nextPIval = "1";
                    nonCommandStep = true;
                    setModeTo1 = true;
                    rcpu.decrTMRandCheck(1);
                    return true;
                }
                vcpu.setPC(pc - offset + 1);
                rcpu.decrTMRandCheck(1);
                return true;
            }

            if (position.startsWith("WW")){
                if (rcpu.siProperty().get().equals("0") && !contentProcessed){
                    nextSIval = "2";
                    nonCommandStep = true;
                    setModeTo1 = true;
                    return true;
                }
                
                //System.out.println("GONNA OPEN");
                if (!wwChannelOpened && !contentProcessed){
                    wwNeedsOpening = true;
                    nonCommandStep = true;
                    return true;
                }
                //System.out.println("OPENED");
                //rcpu.mdProperty().setValue("1");
                //rcpu.siProperty().setValue("2");
                //char[] temp = rcpu.chProperty().get().toCharArray();
                //temp[1] = '1';
                //rcpu.chProperty().setValue(String.valueOf(temp));
                if (!contentProcessed){
                    int srcBlock = -1, srcWord = -1;
                    srcBlock = Integer.parseInt(position.substring(2, 3), 16);
                    srcWord = Integer.parseInt(position.substring(3, 4), 16);

                    stdout.appendText(memory[srcBlock][srcWord].get());
                    contentProcessed = true;
                    return true;
                }
                
                if (wwChannelOpened){
                    wwNeedsClosing = true;
                    nonCommandStep = true;
                    nextSIval = "0";
                    return true;
                }
                
                vcpu.setPC(pc+1);
                contentProcessed = false;
                //temp = rcpu.chProperty().get().toCharArray();
                //temp[1] = '0';
                //rcpu.chProperty().setValue(String.valueOf(temp));
                rcpu.decrTMRandCheck(3);
                //rcpu.siProperty().setValue("0");
                //rcpu.mdProperty().setValue("0");
                
                return true;
            }
            if (position.startsWith("RW")){
                if (rcpu.siProperty().get().equals("0") && !contentProcessed){
                    nextSIval = "1";
                    nonCommandStep = true;
                    setModeTo1 = true;
                    return true;
                }
                //rcpu.mdProperty().setValue("1");
                //rcpu.siProperty().setValue("1");
                if (!rwChannelOpened && !contentProcessed){
                    rwNeedsOpening = true;
                    nonCommandStep = true;
                    return true;
                }
                //char[] temp = rcpu.chProperty().get().toCharArray();
                //temp[0] = '1';
                //rcpu.chProperty().setValue(String.valueOf(temp));
                if (!contentProcessed){
                    int destBlock = -1, destWord = -1;
                    destBlock = Integer.parseInt(position.substring(2, 3), 16);
                    destWord = Integer.parseInt(position.substring(3, 4), 16);

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
                    //temp = rcpu.chProperty().get().toCharArray();
                    //temp[0] = '0';
                    //rcpu.chProperty().setValue(String.valueOf(temp));
                    if (input.startsWith("\"")){ // Ivestas string, ne skaicius
                        if (input.endsWith("\"") && input.length() < 7){
                            memory[destBlock][destWord].setValue(input.substring(1, input.length() - 1));
                        }
                        else {
                            throw new IOException("Neteisingas eilutes formatavimas arba ilgis didesnis uz 4 baitus");
                        }
                    }
                    else if (input.length() < 9){ // Ivestas skaicius (16-aineje sistemoje)
                        memory[destBlock][destWord].setValue(Integer.toHexString((int)Long.parseLong(input, 16)));
                    } else {
                        throw new IOException("Neteisingas eilutes formatavimas arba ilgis didesnis uz 4 baitus");
                    }
                    contentProcessed = true;
                    return true;
                }

                if (rwChannelOpened){
                    rwNeedsClosing = true;
                    nonCommandStep = true;
                    nextSIval = "0";
                    return true;
                }
                
                vcpu.setPC(pc+1);
                contentProcessed = false;
                rcpu.decrTMRandCheck(3);
                //rcpu.siProperty().setValue("0");
                //rcpu.mdProperty().setValue("0");
                return true;
            }

            if (position.startsWith("SHL")){
                int id = Integer.parseInt(position.substring(3, 4), 16);
                nextSHword = id;
                String current = rcpu.shmProperty().get();
                
                if (rcpu.siProperty().get().equals("0")){
                    nextSIval = "5";
                    nonCommandStep = true;
                    setModeTo1 = true;
                    return true;
                }
                
                if (!shmLocked){
                    shmNeedsLocking = true;
                    nonCommandStep = true;
                    return true;
                }
                
                //if (!finaliseSHL){
                finaliseSHL = true;
                
                //}

                //rcpu.mdProperty().setValue("1");
                //rcpu.siProperty().setValue("5");
                //char[] arr = current.toCharArray();
                //arr[id] = '1';
                //String result = String.valueOf(arr);
                //rcpu.shmProperty().setValue(result);
                
                vcpu.setPC(pc+1);
                rcpu.decrTMRandCheck(3);
                nonCommandStep = true;
                nextSIval = "0";
                return true;
                //rcpu.siProperty().setValue("0");
                //rcpu.mdProperty().setValue("0");
            }
            if (position.startsWith("SHU")){
                int id = Integer.parseInt(position.substring(3, 4), 16);
                nextSHword = id;
                String current = rcpu.shmProperty().get();
                
                if (rcpu.siProperty().get().equals("0")){
                    nextSIval = "6";
                    nonCommandStep = true;
                    setModeTo1 = true;
                    return true;
                }
                
                if (!shmUnlocked){
                    shmNeedsUnlocking = true;
                    nonCommandStep = true;
                    return true;
                }
                
                finaliseSHU = true;
                
                /*if (current.charAt(id) == '0'){
                    // jau atrakinta
                    vcpu.setPC(pc+1);
                    rcpu.decrTMRandCheck();
                    return true;
                } else {*/
                
                //rcpu.mdProperty().setValue("1");
                //rcpu.siProperty().setValue("6");
                //char[] arr = current.toCharArray();
                //arr[id] = '0';
                //String result = String.valueOf(arr);
                //rcpu.shmProperty().setValue(result);
                vcpu.setPC(pc+1);
                rcpu.decrTMRandCheck(3);
                nonCommandStep = true;
                nextSIval = "0";
                return true;

            }
        } catch(NumberFormatException ex){
            nextPIval = "1";
            nonCommandStep = true;
            setModeTo1 = true;
            return true;
        }
        
        try {
            switch(position){
                case "ADD ":
                    ++pc;
                    vcpu.setPC(pc);
                    pcBlock = (pc / 16) /*+ 2*/;
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
                        //rcpu.mdProperty().setValue("1");
                        //rcpu.piProperty().setValue("4");
                        //rcpu.mdProperty().setValue("0");
                    }
                    if (result == 0){
                        zero = true;
                    }
                    if (result < 0){
                        sign = true;
                    }
                    vcpu.sfProperty().setValue(arrangeFlags(sign, zero, carry));

                    if (carry){
                        nonCommandStep = true;
                        setModeTo1 = true;
                        nextPIval = "4";
                    }

                    first.setValue(Integer.toHexString(result));
                    vcpu.setPC(pc+1);
                    rcpu.decrTMRandCheck(1);
                    //rcpu.mdProperty().setValue("1");
                    //rcpu.piProperty().setValue("0");
                    //rcpu.mdProperty().setValue("0");
                    return true;

                case "SUB ":
                    ++pc;
                    vcpu.setPC(pc);
                    pcBlock = (pc / 16) /*+ 2*/;
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
                    // perpildymo pertraukimas
                    if (result > (int)Long.parseLong(first.get(), 16)){
                        //rcpu.mdProperty().setValue("1");
                        //rcpu.piProperty().setValue("4");
                        //rcpu.mdProperty().setValue("0");
                        nonCommandStep = true;
                        setModeTo1 = true;
                        nextPIval = "4";
                    }
                    vcpu.sfProperty().setValue(arrangeFlags(sign, zero, carry));
                    first.setValue(Integer.toHexString(result));
                    vcpu.setPC(pc+1);
                    rcpu.decrTMRandCheck(1);
                    // perpildymo pertraukimo atstatymas
                    /*if (result > (int)Long.parseLong(first.get(), 16)){
                        rcpu.mdProperty().setValue("1");
                        rcpu.piProperty().setValue("0");
                        rcpu.mdProperty().setValue("0");
                    }*/
                    return true;

                case "CMP ":
                    ++pc;
                    vcpu.setPC(pc);
                    pcBlock = (pc / 16) /*+ 2*/;
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
                    rcpu.decrTMRandCheck(1);
                    return true;

                case "MUL ":
                    ++pc;
                    vcpu.setPC(pc);
                    pcBlock = (pc / 16) /*+ 2*/;
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
                        //rcpu.mdProperty().setValue("1");
                        //rcpu.piProperty().setValue("4");
                        //rcpu.mdProperty().setValue("0");
                    }
                    if (result == 0){
                        zero = true;
                    }
                    if (result < 0){
                        sign = true;
                    }
                    vcpu.sfProperty().setValue(arrangeFlags(sign, zero, carry));

                    if (carry){
                        nonCommandStep = true;
                        setModeTo1 = true;
                        nextPIval = "4";
                    }

                    first.setValue(Integer.toHexString(result));
                    vcpu.setPC(pc+1);
                    rcpu.decrTMRandCheck(1);
                    //rcpu.mdProperty().setValue("1");
                    //rcpu.piProperty().setValue("4");
                    //rcpu.mdProperty().setValue("0");
                    return true;

                case "DIV ":
                    ++pc;
                    vcpu.setPC(pc);
                    pcBlock = (pc / 16) /*+ 2*/;
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
                        nextPIval = "3";
                        nonCommandStep = true;
                        setModeTo1 = true;
                        return true;
                        //throw new IOException("Neteisingas priskyrimas");
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
                    rcpu.decrTMRandCheck(1);
                    return true;

                case "MOD ":
                    ++pc;
                    vcpu.setPC(pc);
                    pcBlock = (pc / 16) /*+ 2*/;
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
                        nextPIval = "3";
                        nonCommandStep = true;
                        setModeTo1 = true;
                        return true;
                        //throw new IOException("Daliklis yra 0");
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
                    rcpu.decrTMRandCheck(1);
                    return true;

                case "MOV ":
                    ++pc;
                    vcpu.setPC(pc);
                    pcBlock = (pc / 16) /*+ 2*/;
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
                            try {
                                first = memory[Integer.parseInt(registers.substring(0, 1), 16)][Integer.parseInt(registers.substring(1, 2), 16)];
                            } catch(NumberFormatException ex){
                                nextPIval = "1";
                                nonCommandStep = true;
                                setModeTo1 = true;
                                return true;
                            }
                    }
                    switch (registers.substring(2, 4)){ // antrasis operandas
                        case "AX":
                            second = vcpu.axProperty();
                            break;
                        case "BX":
                            second = vcpu.bxProperty();
                            break;
                        default:
                            try {
                                second = memory[Integer.parseInt(registers.substring(2, 3), 16)][Integer.parseInt(registers.substring(3, 4), 16)];
                            } catch(NumberFormatException ex){
                                nextPIval = "1";
                                nonCommandStep = true;
                                setModeTo1 = true;
                                return true;
                            }
                    }
                    first.setValue(second.get());
                    vcpu.setPC(pc+1);
                    rcpu.decrTMRandCheck(1);
                    return true;

                case "MOVC":
                    ++pc;
                    vcpu.setPC(pc);
                    pcBlock = (pc / 16) /*+ 2*/;
                    pcWord = pc % 16;
                    String fword = memory[pcBlock][pcWord].get();
                    ++pc;
                    vcpu.setPC(pc);
                    pcBlock = (pc / 16) /*+ 2*/;
                    pcWord = pc % 16;
                    String sword = memory[pcBlock][pcWord].get();
                    String constant = fword + sword;
                    try {
                        vcpu.axProperty().setValue(Integer.toHexString((int)Long.parseLong(constant, 16)));
                    } catch(NumberFormatException ex){
                        nextPIval = "3";
                        nonCommandStep = true;
                        setModeTo1 = true;
                        return true;
                    }
                    vcpu.setPC(pc+1);
                    rcpu.decrTMRandCheck(1);
                    return true;

                case "JEQL":
                    ++pc;
                    vcpu.setPC(pc);
                    pcBlock = (pc / 16) /*+ 2*/;
                    pcWord = pc % 16;
                    registers = memory[pcBlock][pcWord].get();
                    switch (registers.substring(1, 2)){ // + arba -
                        case "+":
                            int offset = Integer.parseInt(registers.substring(2, 4), 16);
                            if ((Integer.parseInt(vcpu.sfProperty().get(), 16) & 0b00001000) == 0b00001000){
                                if ((pc + offset + 1) > 255){
                                    // pc 'islekia' is atminties reziu
                                    nextPIval = "1";
                                    nonCommandStep = true;
                                    setModeTo1 = true;
                                    rcpu.decrTMRandCheck(1);
                                    return true;
                                }
                                vcpu.setPC(pc + offset + 1);
                                rcpu.decrTMRandCheck(1);
                                return true;
                            }
                            else {
                                vcpu.setPC(pc+1);
                                rcpu.decrTMRandCheck(1);
                                return true;
                            }

                        case "-":
                            offset = Integer.parseInt(registers.substring(2, 4), 16);
                            if ((Integer.parseInt(vcpu.sfProperty().get(), 16) & 0b00001000) == 0b00001000){
                                if ((pc - offset + 1) < 0){
                                    // pc 
                                    nextPIval = "1";
                                    nonCommandStep = true;
                                    setModeTo1 = true;
                                    rcpu.decrTMRandCheck(1);
                                    return true;
                                }
                                vcpu.setPC(pc - offset + 1);
                                rcpu.decrTMRandCheck(1);
                                return true;
                            }
                            else {
                                vcpu.setPC(pc+1);
                                rcpu.decrTMRandCheck(1);
                                return true;
                            }
                        default:
                            throw new IOException("Neteisingas poslinkio formatas");
                    }

                case "JAEQ":
                    ++pc;
                    vcpu.setPC(pc);
                    pcBlock = (pc / 16) /*+ 2*/;
                    pcWord = pc % 16;
                    registers = memory[pcBlock][pcWord].get();
                    switch (registers.substring(1, 2)){ // + arba -
                        case "+":
                            int offset = Integer.parseInt(registers.substring(2, 4), 16);
                            if ((Integer.parseInt(vcpu.sfProperty().get(), 16) & 0b00000001) == 0b00000000){
                                if ((pc + offset + 1) > 255){
                                    // pc 'islekia' is atminties reziu
                                    nextPIval = "1";
                                    nonCommandStep = true;
                                    setModeTo1 = true;
                                    rcpu.decrTMRandCheck(1);
                                    return true;
                                }
                                vcpu.setPC(pc + offset + 1);
                                rcpu.decrTMRandCheck(1);
                                return true;
                            }
                            else {
                                vcpu.setPC(pc+1);
                                rcpu.decrTMRandCheck(1);
                                return true;
                            }

                        case "-":
                            offset = Integer.parseInt(registers.substring(2, 4), 16);
                            if ((Integer.parseInt(vcpu.sfProperty().get(), 16) & 0b00000001) == 0b00000000){
                                if ((pc - offset + 1) < 0){
                                    // pc
                                    nextPIval = "1";
                                    nonCommandStep = true;
                                    setModeTo1 = true;
                                    rcpu.decrTMRandCheck(1);
                                    return true;
                                }
                                vcpu.setPC(pc - offset + 1);
                                rcpu.decrTMRandCheck(1);
                                return true;
                            }
                            else {
                                vcpu.setPC(pc+1);
                                rcpu.decrTMRandCheck(1);
                                return true;
                            }
                        default:
                            throw new IOException("Neteisingas poslinkio formatas");
                    }

                case "JBEQ":
                    ++pc;
                    vcpu.setPC(pc);
                    pcBlock = (pc / 16) /*+ 2*/;
                    pcWord = pc % 16;
                    registers = memory[pcBlock][pcWord].get();
                    switch (registers.substring(1, 2)){ // + arba -
                        case "+":
                            int offset = Integer.parseInt(registers.substring(2, 4), 16);
                            if ((Integer.parseInt(vcpu.sfProperty().get(), 16) & 0b00001001) == 0b00001001 || 
                                (Integer.parseInt(vcpu.sfProperty().get(), 16) & 0b00000001) == 0b00000001 ||
                                (Integer.parseInt(vcpu.sfProperty().get(), 16) & 0b00001000) == 0b00001000){
                                if ((pc + offset + 1) > 255){
                                    // pc 'islekia' is atminties reziu
                                    nextPIval = "1";
                                    nonCommandStep = true;
                                    setModeTo1 = true;
                                    rcpu.decrTMRandCheck(1);
                                    return true;
                                }
                                vcpu.setPC(pc + offset + 1);
                                rcpu.decrTMRandCheck(1);
                                return true;
                            }
                            else {
                                vcpu.setPC(pc+1);
                                rcpu.decrTMRandCheck(1);
                                return true;
                            }

                        case "-":
                            offset = Integer.parseInt(registers.substring(2, 4), 16);
                            if ((Integer.parseInt(vcpu.sfProperty().get(), 16) & 0b00001001) == 0b00001001 || 
                                (Integer.parseInt(vcpu.sfProperty().get(), 16) & 0b00000001) == 0b00000001 ||
                                (Integer.parseInt(vcpu.sfProperty().get(), 16) & 0b00001000) == 0b00001000){
                                if ((pc - offset + 1) < 0){
                                    // pc
                                    nextPIval = "1";
                                    nonCommandStep = true;
                                    setModeTo1 = true;
                                    rcpu.decrTMRandCheck(1);
                                    return true;
                                }
                                vcpu.setPC(pc - offset + 1);
                                rcpu.decrTMRandCheck(1);
                                return true;
                            }
                            else {
                                vcpu.setPC(pc+1);
                                rcpu.decrTMRandCheck(1);
                                return true;
                            }
                        default:
                            throw new IOException("Neteisingas poslinkio formatas");
                    }

                case "JABV":
                    ++pc;
                    vcpu.setPC(pc);
                    pcBlock = (pc / 16) /*+ 2*/;
                    pcWord = pc % 16;
                    registers = memory[pcBlock][pcWord].get();
                    switch (registers.substring(1, 2)){ // + arba -
                        case "+":
                            int offset = Integer.parseInt(registers.substring(2, 4), 16);
                            if ((Integer.parseInt(vcpu.sfProperty().get(), 16) & 0b00001001) == 0b00000000){
                                if ((pc + offset + 1) > 255){
                                    // pc 'islekia' is atminties reziu
                                    nextPIval = "1";
                                    nonCommandStep = true;
                                    setModeTo1 = true;
                                    rcpu.decrTMRandCheck(1);
                                    return true;
                                }
                                vcpu.setPC(pc + offset + 1);
                                rcpu.decrTMRandCheck(1);
                                return true;
                            }
                            else {
                                vcpu.setPC(pc+1);
                                rcpu.decrTMRandCheck(1);
                                return true;
                            }

                        case "-":
                            offset = Integer.parseInt(registers.substring(2, 4), 16);
                            if ((Integer.parseInt(vcpu.sfProperty().get(), 16) & 0b00001001) == 0b00000000){
                                if ((pc - offset + 1) < 0){
                                    // pc
                                    nextPIval = "1";
                                    nonCommandStep = true;
                                    setModeTo1 = true;
                                    rcpu.decrTMRandCheck(1);
                                    return true;
                                }
                                vcpu.setPC(pc - offset + 1);
                                rcpu.decrTMRandCheck(1);
                                return true;
                            }
                            else {
                                vcpu.setPC(pc+1);
                                rcpu.decrTMRandCheck(1);
                                return true;
                            }
                        default:
                            throw new IOException("Neteisingas poslinkio formatas");
                    }

                case "JBLW":
                    ++pc;
                    vcpu.setPC(pc);
                    pcBlock = (pc / 16) /*+ 2*/;
                    pcWord = pc % 16;
                    registers = memory[pcBlock][pcWord].get();
                    switch (registers.substring(1, 2)){ // + arba -
                        case "+":
                            int offset = Integer.parseInt(registers.substring(2, 4), 16);
                            if ((Integer.parseInt(vcpu.sfProperty().get(), 16) & 0b00000001) == 0b00000001){
                                if ((pc + offset + 1) > 255){
                                    // pc 'islekia' is atminties reziu
                                    nextPIval = "1";
                                    nonCommandStep = true;
                                    setModeTo1 = true;
                                    rcpu.decrTMRandCheck(1);
                                    return true;
                                }
                                vcpu.setPC(pc + offset + 1);
                                rcpu.decrTMRandCheck(1);
                                return true;
                            }
                            else {
                                vcpu.setPC(pc+1);
                                rcpu.decrTMRandCheck(1);
                                return true;
                            }

                        case "-":
                            offset = Integer.parseInt(registers.substring(2, 4), 16);
                            if ((Integer.parseInt(vcpu.sfProperty().get(), 16) & 0b00000001) == 0b00000001){
                                if ((pc - offset + 1) < 0){
                                    // pc
                                    nextPIval = "1";
                                    nonCommandStep = true;
                                    setModeTo1 = true;
                                    rcpu.decrTMRandCheck(1);
                                    return true;
                                }
                                vcpu.setPC(pc - offset + 1);
                                rcpu.decrTMRandCheck(1);
                                return true;
                            }
                            else {
                                vcpu.setPC(pc+1);
                                rcpu.decrTMRandCheck(1);
                                return true;
                            }
                        default:
                            throw new IOException("Neteisingas poslinkio formatas");
                    }

                case "SHW ":
                    if (rcpu.siProperty().get().equals("0") && !contentProcessed){
                        nextSIval = "4";
                        nonCommandStep = true;
                        setModeTo1 = true;
                        shwNeedsStart = true;
                        return true;
                    }
                    
                    if (!contentProcessed){
                        contentProcessed = true;
                        ++pc;
                        //vcpu.setPC(pc);
                        pcBlock = (pc / 16) /*+ 2*/;
                        pcWord = pc % 16;
                        registers = memory[pcBlock][pcWord].get();
                        int sharedWord = Integer.parseInt(registers.substring(1, 2), 16);
                        if (rcpu.shmProperty().get().charAt(sharedWord) == '1'){
                            //rcpu.mdProperty().setValue("1");
                            //rcpu.siProperty().setValue("4");
                            first = rcpu.smt().memoryProperty(sharedWord);
                            second = memory[Integer.parseInt(registers.substring(2, 3), 16)][Integer.parseInt(registers.substring(3, 4), 16)];
                            first.setValue(second.get());
                            
                            //rcpu.siProperty().setValue("0");
                            //rcpu.mdProperty().setValue("0");
                            return true;
                        } else {
                            // dirbama su neuzrakinta atmintimi
                            rcpu.decrTMRandCheck(3);
                            nextPIval = "5";
                            nonCommandStep = true;
                            //setModeTo1 = true;
                            return true;
                            //rcpu.mdProperty().setValue("1");
                            //rcpu.piProperty().setValue("5");
                            //stdout.setText("[INT] Bandymas naudotis bendrąją atmintimi jos neužrakinus [INT]");
                            //rcpu.mdProperty().setValue("0");
                            //throw new IOException("Bandymas naudotis bendrąją atmintimi jos neužrakinus");
                        }
                    }
                    rcpu.decrTMRandCheck(3);
                    vcpu.setPC(pc+2);
                    contentProcessed = false;
                    nonCommandStep = true;
                    nextSIval = "0";
                    finaliseSHW = true;
                    //System.out.println("SHW should set MD to 0");
                    return true;

                case "SHR ":
                    if (rcpu.siProperty().get().equals("0") && !contentProcessed){
                        nextSIval = "3";
                        nonCommandStep = true;
                        setModeTo1 = true;
                        shrNeedsStart = true;
                        return true;
                    }
                    
                    if (!contentProcessed){
                        contentProcessed = true;
                        ++pc;
                        //vcpu.setPC(pc);
                        pcBlock = (pc / 16) /*+ 2*/;
                        pcWord = pc % 16;
                        registers = memory[pcBlock][pcWord].get();
                        int sharedWord = Integer.parseInt(registers.substring(3, 4), 16);
                        if (rcpu.shmProperty().get().charAt(sharedWord) == '1'){
                            //rcpu.mdProperty().setValue("1");
                            //rcpu.siProperty().setValue("3");
                            second = rcpu.smt().memoryProperty(sharedWord);
                            first = memory[Integer.parseInt(registers.substring(0, 1), 16)][Integer.parseInt(registers.substring(1, 2), 16)];
                            first.setValue(second.get());
                            //rcpu.siProperty().setValue("0");
                            //rcpu.mdProperty().setValue("0");
                            return true;
                        } else {
                            // dirbama su neuzrakinta atmintimi
                            rcpu.decrTMRandCheck(3);
                            nextPIval = "5";
                            nonCommandStep = true;
                            //setModeTo1 = true;
                            return true;
                            //rcpu.mdProperty().setValue("1");
                            //rcpu.piProperty().setValue("5");
                            //stdout.setText("[INT] Bandymas naudotis bendrąją atmintimi jos neužrakinus [INT]");
                            //md.Property().setValue("0");
                            //throw new IOException("Bandymas naudotis bendrąją atmintimi jos neužrakinus");
                        }
                    }
                    rcpu.decrTMRandCheck(3);
                    vcpu.setPC(pc+2);
                    contentProcessed = false;
                    nonCommandStep = true;
                    nextSIval = "0";
                    finaliseSHR = true;
                    //System.out.println("SHR should set MD to 0");
                    return true;

                case "HALT":
                    if (rcpu.mdProperty().get().equals("0")){
                        rcpu.mdProperty().setValue("1");
                        return true;
                    }

                    System.out.println("\u001B[31mPasiekta programos pabaiga.\u001B[0m");
                    rcpu.decrTMRandCheck(3);

                    rcpu.siProperty().setValue("7");
                    //rcpu.mdProperty().setValue("0");
                    //vcpu.setPC(pc+1);
                    return false;
            }
        } catch(NumberFormatException ex){
            nextPIval = "1";
            nonCommandStep = true;
            setModeTo1 = true;
            return true;
        }
        // neegzistuojanti komanda
        /*if (rcpu.mdProperty().get().equals("0")){
            rcpu.mdProperty().setValue("1");
            return true;
        }*/
        //rcpu.mdProperty().setValue("1");
        //rcpu.piProperty().setValue("2");
        //stdout.setText("[INT] Neteisingas op. kodas [INT]");
        nextPIval = "2";
        nonCommandStep = true;
        setModeTo1 = true;
        return true;
        //rcpu.mdProperty().setValue("0");
        //throw new IOException("[INT] Neteisingas op. kodas [INT]");
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
