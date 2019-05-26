/*
Manages the virtual memory and instruction sets.
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
 * @author Paulius Staisiunas, Computer Science 3 yr., 3 gr.
 */

public class VirtualMachine {
    private final Assembly asm;
    private final SimpleStringProperty[][] memory = new SimpleStringProperty[16][16];
    private int startPC = 0;                    // Used after refreshing a programme
    
    // Variables intended to provide 'step mode' support
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
    
    // logically a single variable would suffice. However,
    // in this case separate variables are needed programmatically
    private boolean shmLocked = false;
    private boolean shmUnlocked = false;
    
    private boolean finaliseSHL = false;
    private boolean finaliseSHU = false;
    private int nextSHword = -1;
    
    private boolean shrNeedsStart = false;
    private boolean shwNeedsStart = false;
    private boolean finaliseSHR = false;
    private boolean finaliseSHW = false;
    
    private boolean TMRstep = false;
    private int nextTMRdecr = 0;
    
    public VirtualMachine(){ 
        // Initial memory loading
        for (int i = 0; i < 16; ++i){
            memory[i] = new SimpleStringProperty[16];
            for (int j = 0; j < 16; ++j){
                memory[i][j] = new SimpleStringProperty("0");
            }
        }
        asm = new Assembly();
    }
    
    public int getInitialPC(){
        return startPC;
    }
    
    public void loadProgramme(File file, VirtualCPU vcpu){
        ArrayList<String> commands = null;
        
        try {
            commands = asm.parseFile(file);
            if (commands == null){
                throw new IOException("Empty programme.");
            }
        } catch (StringIndexOutOfBoundsException | IOException ex) {
            System.out.println(ex);
            return;
        }
        
        // Putting instructions into the memory
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
        TMRstep = false;
        nextTMRdecr = 0;
    }
    
    public void reset(){
        clearBooleans();
        contentProcessed = false;
    }
    
    // Unnecessarily long method for command execution
    // Returns 1 if current VM can continue execution
    // Returns 0 if current VM has halted and can be removed
    // Returns 2 if current VM encountered an interrupt (not possible to recover)
    // Returns 3 if the timer has depleted and we needa new VM
    public int executeCommand(VirtualCPU vcpu, RealCPU rcpu, Label stdinStatus, TextField stdout) throws IOException, NumberFormatException{
        if (TMRstep){
            int outcome = rcpu.decrTMRandCheck(nextTMRdecr);
            nextTMRdecr = 0;
            TMRstep = false;
            if (outcome == 1 && !throwableInt){ // Throwable interrupt is going to switch machines anyway
                return 3;
            }
            return 1;
        }
        if (nonCommandStep){
            if (setModeTo0){
                rcpu.mdProperty().setValue("0");
                if (throwableInt){
                    String message = "";
                    switch(nextPIval){
                        case "1":
                            message = "Bad address";
                            break;
                        case "2":
                            message = "Bad instruction code";
                            break;
                        case "3":
                            message = "Bad assignment";
                            break;
                        case "5":
                            message = "Using shared memory without locking it";
                            break;
                    }
                    throw new IOException("[INT] " + message + " [INT]");
                }
                clearBooleans();
                return 2;
                
            } 
            if (setModeTo1){
                rcpu.mdProperty().setValue("1");
                setModeTo1 = false;
                return 1;
                
            } 
            if (!nextPIval.isEmpty()){
                rcpu.piProperty().setValue(nextPIval);
                setModeTo0 = true;
                if (!nextPIval.equals("4")){
                    throwableInt = true;
                }
                else {
                    stdout.setText("[INT] Overflow [INT]");
                }
                return 1;
                
            } 
            if (!nextSIval.isEmpty()){
                rcpu.siProperty().setValue(nextSIval);
                nextSIval = "";
                return 1;
                
            }
            if (wwNeedsOpening){
                wwChannelOpened = true;
                wwNeedsOpening = false;
                nonCommandStep = false;
                char[] temp = rcpu.chProperty().get().toCharArray();
                temp[1] = '1';
                rcpu.chProperty().setValue(String.valueOf(temp));
                return 1;
                
            }
            if (wwNeedsClosing){
                wwChannelOpened = false;
                wwNeedsClosing = false;
                setModeTo0 = true;
                char[] temp = rcpu.chProperty().get().toCharArray();
                temp[1] = '0';
                rcpu.chProperty().setValue(String.valueOf(temp));
                return 1;
            }
            
            if (rwNeedsOpening){
                rwChannelOpened = true;
                rwNeedsOpening = false;
                nonCommandStep = false;
                char[] temp = rcpu.chProperty().get().toCharArray();
                temp[0] = '1';
                rcpu.chProperty().setValue(String.valueOf(temp));
                return 1;
                
            }
            if (rwNeedsClosing){
                rwChannelOpened = false;
                rwNeedsClosing = false;
                setModeTo0 = true;
                char[] temp = rcpu.chProperty().get().toCharArray();
                temp[0] = '0';
                rcpu.chProperty().setValue(String.valueOf(temp));
                return 1;
            }
            
            if (shmNeedsLocking){
                shmLocked = true;
                shmNeedsLocking = false;
                nonCommandStep = false;
                char[] temp = rcpu.shmProperty().get().toCharArray();
                temp[nextSHword] = '1';
                rcpu.shmProperty().setValue(String.valueOf(temp));
                return 1;
                
            }
            if (shmNeedsUnlocking){
                shmUnlocked = true;
                shmNeedsUnlocking = false;
                nonCommandStep = false;
                char[] temp = rcpu.shmProperty().get().toCharArray();
                temp[nextSHword] = '0';
                rcpu.shmProperty().setValue(String.valueOf(temp));
                return 1;
                
            }
            
            if (finaliseSHL){
                finaliseSHL = false;
                setModeTo0 = true;
                return 1;
            }
            
            if (finaliseSHU){
                finaliseSHU = false;
                setModeTo0 = true;
                return 1;
            }
            
            if (shrNeedsStart){
                nonCommandStep = false;
                shrNeedsStart = false;
                return 1;
            }
            if (shwNeedsStart){
                nonCommandStep = false;
                shwNeedsStart = false;
                return 1;
            }
            if (finaliseSHR){
                finaliseSHR = false;
                clearBooleans();
                return 1;
            }
            if (finaliseSHW){
                finaliseSHW = false;
                clearBooleans();
                return 1;
            }
        }
        
        int pc = Integer.parseInt(vcpu.pcProperty().getValue(), 16);
        int pcBlock = (pc / 16);
        int pcWord = pc % 16;
        String position = memory[pcBlock][pcWord].getValue();
        
        if (rcpu.siProperty().get().equals("7")){ // HALT
            return 0;
        }
        
        System.out.println(position);
        
        try {
            if (position.startsWith("J+")){
                int offset = Integer.parseInt(position.substring(2, 4), 16);
                if ((pc + offset + 1) > 255){
                    nextPIval = "1";
                    nonCommandStep = true;
                    setModeTo1 = true;
                    nextTMRdecr = 1;
                    TMRstep = true;
                    return 1;
                }
                vcpu.setPC(pc + offset + 1);
                nextTMRdecr = 1;
                TMRstep = true;
                return 1;
            }
            if (position.startsWith("J-")){
                int offset = Integer.parseInt(position.substring(2, 4), 16);
                if ((pc - offset + 1) < 0){
                    nextPIval = "1";
                    nonCommandStep = true;
                    setModeTo1 = true;
                    nextTMRdecr = 1;
                    TMRstep = true;
                    return 1;
                }
                vcpu.setPC(pc - offset + 1);
                nextTMRdecr = 1;
                TMRstep = true;
                return 1;
            }

            if (position.startsWith("WW")){
                if (rcpu.siProperty().get().equals("0") && !contentProcessed){
                    nextSIval = "2";
                    nonCommandStep = true;
                    setModeTo1 = true;
                    return 1;
                }
                
                if (!wwChannelOpened && !contentProcessed){
                    wwNeedsOpening = true;
                    nonCommandStep = true;
                    return 1;
                }

                if (!contentProcessed){
                    int srcBlock = -1, srcWord = -1;
                    srcBlock = Integer.parseInt(position.substring(2, 3), 16);
                    srcWord = Integer.parseInt(position.substring(3, 4), 16);

                    stdout.appendText(memory[srcBlock][srcWord].get());
                    contentProcessed = true;
                    return 1;
                }
                
                if (wwChannelOpened){
                    wwNeedsClosing = true;
                    nonCommandStep = true;
                    nextSIval = "0";
                    return 1;
                }
                
                vcpu.setPC(pc+1);
                contentProcessed = false;
                nextTMRdecr = 3;
                TMRstep = true;
                return 1;
            }
            if (position.startsWith("RW")){
                if (rcpu.siProperty().get().equals("0") && !contentProcessed){
                    nextSIval = "1";
                    nonCommandStep = true;
                    setModeTo1 = true;
                    return 1;
                }

                if (!rwChannelOpened && !contentProcessed){
                    rwNeedsOpening = true;
                    nonCommandStep = true;
                    return 1;
                }

                if (!contentProcessed){
                    int destBlock = -1, destWord = -1;
                    destBlock = Integer.parseInt(position.substring(2, 3), 16);
                    destWord = Integer.parseInt(position.substring(3, 4), 16);

                    TextInputDialog dialog = new TextInputDialog();
                    dialog.setResizable(false);
                    dialog.setHeaderText("");
                    dialog.setWidth(10);
                    dialog.setGraphic(null);
                    dialog.setTitle("Input device");
                    stdinStatus.setTextFill(Color.DARKGREEN);
                    stdinStatus.setText("ONLINE");
                    dialog.showAndWait();
                    stdinStatus.setText("OFFLINE");
                    stdinStatus.setTextFill(Color.DARKRED);
                    String input = dialog.getEditor().getText();

                    if (input.startsWith("\"")){ // A string
                        if (input.endsWith("\"") && input.length() < 7){
                            memory[destBlock][destWord].setValue(input.substring(1, input.length() - 1));
                        }
                        else {
                            throw new IOException("Bad string formatting or size greater than 4 bytes.");
                        }
                    }
                    else if (input.length() < 9){ // A hex number
                        memory[destBlock][destWord].setValue(Integer.toHexString((int)Long.parseLong(input, 16)));
                    } else {
                        throw new IOException("Bad string formatting or size greater than 4 bytes.");
                    }
                    contentProcessed = true;
                    return 1;
                }

                if (rwChannelOpened){
                    rwNeedsClosing = true;
                    nonCommandStep = true;
                    nextSIval = "0";
                    return 1;
                }
                
                vcpu.setPC(pc+1);
                contentProcessed = false;
                nextTMRdecr = 3;
                TMRstep = true;
                return 1;
            }

            if (position.startsWith("SHL")){
                int id = Integer.parseInt(position.substring(3, 4), 16);
                nextSHword = id;
                
                if (rcpu.siProperty().get().equals("0")){
                    nextSIval = "5";
                    nonCommandStep = true;
                    setModeTo1 = true;
                    return 1;
                }
                
                if (!shmLocked){
                    shmNeedsLocking = true;
                    nonCommandStep = true;
                    return 1;
                }
                
                finaliseSHL = true;
                
                vcpu.setPC(pc+1);
                nextTMRdecr = 3;
                TMRstep = true;
                nonCommandStep = true;
                nextSIval = "0";
                return 1;
            }
            if (position.startsWith("SHU")){
                int id = Integer.parseInt(position.substring(3, 4), 16);
                nextSHword = id;
                
                if (rcpu.siProperty().get().equals("0")){
                    nextSIval = "6";
                    nonCommandStep = true;
                    setModeTo1 = true;
                    return 1;
                }
                
                if (!shmUnlocked){
                    shmNeedsUnlocking = true;
                    nonCommandStep = true;
                    return 1;
                }
                
                finaliseSHU = true;

                vcpu.setPC(pc+1);
                nextTMRdecr = 3;
                TMRstep = true;
                nonCommandStep = true;
                nextSIval = "0";
                return 1;

            }
        } catch(NumberFormatException ex){
            nextPIval = "1";
            nonCommandStep = true;
            setModeTo1 = true;
            return 1;
        }
        
        try {
            switch(position){
                case "ADD ":
                    ++pc;
                    vcpu.setPC(pc);
                    pcBlock = (pc / 16);
                    pcWord = pc % 16;
                    String registers = memory[pcBlock][pcWord].get();
                    StringProperty first = null, second = null;
                    switch (registers.substring(0, 2)){
                        case "AX":
                            first = vcpu.axProperty();
                            break;
                        case "BX":
                            first = vcpu.bxProperty();
                            break;
                        default:
                            throw new IOException("Bad ADD registers.");
                    }
                    switch (registers.substring(2, 4)){
                        case "AX":
                            second = vcpu.axProperty();
                            break;
                        case "BX":
                            second = vcpu.bxProperty();
                            break;
                        default:
                            throw new IOException("Bad ADD registers.");
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

                    if (carry){
                        nonCommandStep = true;
                        setModeTo1 = true;
                        nextPIval = "4";
                    }

                    first.setValue(Integer.toHexString(result));
                    vcpu.setPC(pc+1);
                    nextTMRdecr = 1;
                    TMRstep = true;
                    return 1;

                case "SUB ":
                    ++pc;
                    vcpu.setPC(pc);
                    pcBlock = (pc / 16);
                    pcWord = pc % 16;
                    registers = memory[pcBlock][pcWord].get();
                    first = null; second = null;
                    switch (registers.substring(0, 2)){
                        case "AX":
                            first = vcpu.axProperty();
                            break;
                        case "BX":
                            first = vcpu.bxProperty();
                            break;
                        default:
                            throw new IOException("Bad SUB registers.");
                    }
                    switch (registers.substring(2, 4)){
                        case "AX":
                            second = vcpu.axProperty();
                            break;
                        case "BX":
                            second = vcpu.bxProperty();
                            break;
                        default:
                            throw new IOException("Bad SUB registers.");
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
                    
                    // Overflow
                    if (result > (int)Long.parseLong(first.get(), 16)){
                        nonCommandStep = true;
                        setModeTo1 = true;
                        nextPIval = "4";
                    }
                    vcpu.sfProperty().setValue(arrangeFlags(sign, zero, carry));
                    first.setValue(Integer.toHexString(result));
                    vcpu.setPC(pc+1);
                    nextTMRdecr = 1;
                    TMRstep = true;
                    return 1;

                case "CMP ":
                    ++pc;
                    vcpu.setPC(pc);
                    pcBlock = (pc / 16);
                    pcWord = pc % 16;
                    registers = memory[pcBlock][pcWord].get();
                    first = null; second = null;
                    switch (registers.substring(0, 2)){
                        case "AX":
                            first = vcpu.axProperty();
                            break;
                        case "BX":
                            first = vcpu.bxProperty();
                            break;
                        default:
                            throw new IOException("Bad CMP registers.");
                    }
                    switch (registers.substring(2, 4)){
                        case "AX":
                            second = vcpu.axProperty();
                            break;
                        case "BX":
                            second = vcpu.bxProperty();
                            break;
                        default:
                            throw new IOException("Bad CMP registers.");
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
                    nextTMRdecr = 1;
                    TMRstep = true;
                    return 1;

                case "MUL ":
                    ++pc;
                    vcpu.setPC(pc);
                    pcBlock = (pc / 16);
                    pcWord = pc % 16;
                    registers = memory[pcBlock][pcWord].get();
                    first = null; second = null;
                    switch (registers.substring(0, 2)){
                        case "AX":
                            first = vcpu.axProperty();
                            break;
                        case "BX":
                            first = vcpu.bxProperty();
                            break;
                        default:
                            throw new IOException("Bad MUL registers.");
                    }
                    switch (registers.substring(2, 4)){
                        case "AX":
                            second = vcpu.axProperty();
                            break;
                        case "BX":
                            second = vcpu.bxProperty();
                            break;
                        default:
                            throw new IOException("Bad MUL registers.");
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

                    if (carry){
                        nonCommandStep = true;
                        setModeTo1 = true;
                        nextPIval = "4";
                    }

                    first.setValue(Integer.toHexString(result));
                    vcpu.setPC(pc+1);
                    nextTMRdecr = 1;
                    TMRstep = true;
                    return 1;

                case "DIV ":
                    ++pc;
                    vcpu.setPC(pc);
                    pcBlock = (pc / 16);
                    pcWord = pc % 16;
                    registers = memory[pcBlock][pcWord].get();
                    first = null; second = null;
                    switch (registers.substring(0, 2)){
                        case "AX":
                            first = vcpu.axProperty();
                            break;
                        case "BX":
                            first = vcpu.bxProperty();
                            break;
                        default:
                            throw new IOException("Bad DIV registers.");
                    }
                    switch (registers.substring(2, 4)){
                        case "AX":
                            second = vcpu.axProperty();
                            break;
                        case "BX":
                            second = vcpu.bxProperty();
                            break;
                        default:
                            throw new IOException("Bad DIV registers.");
                    }
                    if ((int)Long.parseLong(second.get(), 16) == 0){
                        nextPIval = "3";
                        nonCommandStep = true;
                        setModeTo1 = true;
                        return 1;
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
                    nextTMRdecr = 1;
                    TMRstep = true;
                    return 1;

                case "MOD ":
                    ++pc;
                    vcpu.setPC(pc);
                    pcBlock = (pc / 16);
                    pcWord = pc % 16;
                    registers = memory[pcBlock][pcWord].get();
                    first = null; second = null;
                    switch (registers.substring(0, 2)){
                        case "AX":
                            first = vcpu.axProperty();
                            break;
                        case "BX":
                            first = vcpu.bxProperty();
                            break;
                        default:
                            throw new IOException("Bad MOD registers.");
                    }
                    switch (registers.substring(2, 4)){
                        case "AX":
                            second = vcpu.axProperty();
                            break;
                        case "BX":
                            second = vcpu.bxProperty();
                            break;
                        default:
                            throw new IOException("Bad MOD registers.");
                    }
                    if ((int)Long.parseLong(second.get(), 16) == 0){
                        nextPIval = "3";
                        nonCommandStep = true;
                        setModeTo1 = true;
                        return 1;
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
                    nextTMRdecr = 1;
                    TMRstep = true;
                    return 1;

                case "MOV ":
                    ++pc;
                    vcpu.setPC(pc);
                    pcBlock = (pc / 16);
                    pcWord = pc % 16;
                    registers = memory[pcBlock][pcWord].get();
                    first = null; second = null;
                    switch (registers.substring(0, 2)){
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
                                return 1;
                            }
                    }
                    switch (registers.substring(2, 4)){
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
                                return 1;
                            }
                    }
                    first.setValue(second.get());
                    vcpu.setPC(pc+1);
                    nextTMRdecr = 1;
                    TMRstep = true;
                    return 1;

                case "MOVC":
                    ++pc;
                    vcpu.setPC(pc);
                    pcBlock = (pc / 16);
                    pcWord = pc % 16;
                    String fword = memory[pcBlock][pcWord].get();
                    ++pc;
                    vcpu.setPC(pc);
                    pcBlock = (pc / 16);
                    pcWord = pc % 16;
                    String sword = memory[pcBlock][pcWord].get();
                    String constant = fword + sword;
                    try {
                        vcpu.axProperty().setValue(Integer.toHexString((int)Long.parseLong(constant, 16)));
                    } catch(NumberFormatException ex){
                        nextPIval = "3";
                        nonCommandStep = true;
                        setModeTo1 = true;
                        return 1;
                    }
                    vcpu.setPC(pc+1);
                    nextTMRdecr = 1;
                    TMRstep = true;
                    return 1;

                // JMP section. Could be implemented better.
                case "JEQL":
                    ++pc;
                    vcpu.setPC(pc);
                    pcBlock = (pc / 16);
                    pcWord = pc % 16;
                    registers = memory[pcBlock][pcWord].get();
                    switch (registers.substring(1, 2)){
                        case "+":
                            int offset = Integer.parseInt(registers.substring(2, 4), 16);
                            if ((Integer.parseInt(vcpu.sfProperty().get(), 16) & 0b00001000) == 0b00001000){
                                if ((pc + offset + 1) > 255){
                                    nextPIval = "1";
                                    nonCommandStep = true;
                                    setModeTo1 = true;
                                    nextTMRdecr = 1;
                                    TMRstep = true;
                                    return 1;
                                }
                                vcpu.setPC(pc + offset + 1);
                                nextTMRdecr = 1;
                                TMRstep = true;
                                return 1;
                            }
                            else {
                                vcpu.setPC(pc+1);
                                nextTMRdecr = 1;
                                TMRstep = true;
                                return 1;
                            }

                        case "-":
                            offset = Integer.parseInt(registers.substring(2, 4), 16);
                            if ((Integer.parseInt(vcpu.sfProperty().get(), 16) & 0b00001000) == 0b00001000){
                                if ((pc - offset + 1) < 0){
                                    nextPIval = "1";
                                    nonCommandStep = true;
                                    setModeTo1 = true;
                                    nextTMRdecr = 1;
                                    TMRstep = true;
                                    return 1;
                                }
                                vcpu.setPC(pc - offset + 1);
                                nextTMRdecr = 1;
                                TMRstep = true;
                                return 1;
                            }
                            else {
                                vcpu.setPC(pc+1);
                                nextTMRdecr = 1;
                                TMRstep = true;
                                return 1;
                            }
                        default:
                            throw new IOException("Bad offset formatting.");
                    }

                case "JAEQ":
                    ++pc;
                    vcpu.setPC(pc);
                    pcBlock = (pc / 16);
                    pcWord = pc % 16;
                    registers = memory[pcBlock][pcWord].get();
                    switch (registers.substring(1, 2)){
                        case "+":
                            int offset = Integer.parseInt(registers.substring(2, 4), 16);
                            if ((Integer.parseInt(vcpu.sfProperty().get(), 16) & 0b00000001) == 0b00000000){
                                if ((pc + offset + 1) > 255){
                                    nextPIval = "1";
                                    nonCommandStep = true;
                                    setModeTo1 = true;
                                    nextTMRdecr = 1;
                                    TMRstep = true;
                                    return 1;
                                }
                                vcpu.setPC(pc + offset + 1);
                                nextTMRdecr = 1;
                                TMRstep = true;
                                return 1;
                            }
                            else {
                                vcpu.setPC(pc+1);
                                nextTMRdecr = 1;
                                TMRstep = true;
                                return 1;
                            }

                        case "-":
                            offset = Integer.parseInt(registers.substring(2, 4), 16);
                            if ((Integer.parseInt(vcpu.sfProperty().get(), 16) & 0b00000001) == 0b00000000){
                                if ((pc - offset + 1) < 0){
                                    nextPIval = "1";
                                    nonCommandStep = true;
                                    setModeTo1 = true;
                                    nextTMRdecr = 1;
                                    TMRstep = true;
                                    return 1;
                                }
                                vcpu.setPC(pc - offset + 1);
                                nextTMRdecr = 1;
                                TMRstep = true;
                                return 1;
                            }
                            else {
                                vcpu.setPC(pc+1);
                                nextTMRdecr = 1;
                                TMRstep = true;
                                return 1;
                            }
                        default:
                            throw new IOException("Bad offset formatting.");
                    }

                case "JBEQ":
                    ++pc;
                    vcpu.setPC(pc);
                    pcBlock = (pc / 16);
                    pcWord = pc % 16;
                    registers = memory[pcBlock][pcWord].get();
                    switch (registers.substring(1, 2)){
                        case "+":
                            int offset = Integer.parseInt(registers.substring(2, 4), 16);
                            if ((Integer.parseInt(vcpu.sfProperty().get(), 16) & 0b00001001) == 0b00001001 || 
                                (Integer.parseInt(vcpu.sfProperty().get(), 16) & 0b00000001) == 0b00000001 ||
                                (Integer.parseInt(vcpu.sfProperty().get(), 16) & 0b00001000) == 0b00001000){
                                if ((pc + offset + 1) > 255){
                                    nextPIval = "1";
                                    nonCommandStep = true;
                                    setModeTo1 = true;
                                    nextTMRdecr = 1;
                                    TMRstep = true;
                                    return 1;
                                }
                                vcpu.setPC(pc + offset + 1);
                                nextTMRdecr = 1;
                                TMRstep = true;
                                return 1;
                            }
                            else {
                                vcpu.setPC(pc+1);
                                nextTMRdecr = 1;
                                TMRstep = true;
                                return 1;
                            }

                        case "-":
                            offset = Integer.parseInt(registers.substring(2, 4), 16);
                            if ((Integer.parseInt(vcpu.sfProperty().get(), 16) & 0b00001001) == 0b00001001 || 
                                (Integer.parseInt(vcpu.sfProperty().get(), 16) & 0b00000001) == 0b00000001 ||
                                (Integer.parseInt(vcpu.sfProperty().get(), 16) & 0b00001000) == 0b00001000){
                                if ((pc - offset + 1) < 0){
                                    nextPIval = "1";
                                    nonCommandStep = true;
                                    setModeTo1 = true;
                                    nextTMRdecr = 1;
                                    TMRstep = true;
                                    return 1;
                                }
                                vcpu.setPC(pc - offset + 1);
                                nextTMRdecr = 1;
                                TMRstep = true;
                                return 1;
                            }
                            else {
                                vcpu.setPC(pc+1);
                                nextTMRdecr = 1;
                                TMRstep = true;
                                return 1;
                            }
                        default:
                            throw new IOException("Bad offset formatting.");
                    }

                case "JABV":
                    ++pc;
                    vcpu.setPC(pc);
                    pcBlock = (pc / 16);
                    pcWord = pc % 16;
                    registers = memory[pcBlock][pcWord].get();
                    switch (registers.substring(1, 2)){
                        case "+":
                            int offset = Integer.parseInt(registers.substring(2, 4), 16);
                            if ((Integer.parseInt(vcpu.sfProperty().get(), 16) & 0b00001001) == 0b00000000){
                                if ((pc + offset + 1) > 255){
                                    nextPIval = "1";
                                    nonCommandStep = true;
                                    setModeTo1 = true;
                                    nextTMRdecr = 1;
                                    TMRstep = true;
                                    return 1;
                                }
                                vcpu.setPC(pc + offset + 1);
                                nextTMRdecr = 1;
                                TMRstep = true;
                                return 1;
                            }
                            else {
                                vcpu.setPC(pc+1);
                                nextTMRdecr = 1;
                                TMRstep = true;
                                return 1;
                            }

                        case "-":
                            offset = Integer.parseInt(registers.substring(2, 4), 16);
                            if ((Integer.parseInt(vcpu.sfProperty().get(), 16) & 0b00001001) == 0b00000000){
                                if ((pc - offset + 1) < 0){
                                    nextPIval = "1";
                                    nonCommandStep = true;
                                    setModeTo1 = true;
                                    nextTMRdecr = 1;
                                    TMRstep = true;
                                    return 1;
                                }
                                vcpu.setPC(pc - offset + 1);
                                nextTMRdecr = 1;
                                TMRstep = true;
                                return 1;
                            }
                            else {
                                vcpu.setPC(pc+1);
                                nextTMRdecr = 1;
                                TMRstep = true;
                                return 1;
                            }
                        default:
                            throw new IOException("Bad offset formatting.");
                    }

                case "JBLW":
                    ++pc;
                    vcpu.setPC(pc);
                    pcBlock = (pc / 16);
                    pcWord = pc % 16;
                    registers = memory[pcBlock][pcWord].get();
                    switch (registers.substring(1, 2)){
                        case "+":
                            int offset = Integer.parseInt(registers.substring(2, 4), 16);
                            if ((Integer.parseInt(vcpu.sfProperty().get(), 16) & 0b00000001) == 0b00000001){
                                if ((pc + offset + 1) > 255){
                                    nextPIval = "1";
                                    nonCommandStep = true;
                                    setModeTo1 = true;
                                    nextTMRdecr = 1;
                                    TMRstep = true;
                                    return 1;
                                }
                                vcpu.setPC(pc + offset + 1);
                                nextTMRdecr = 1;
                                TMRstep = true;
                                return 1;
                            }
                            else {
                                vcpu.setPC(pc+1);
                                nextTMRdecr = 1;
                                TMRstep = true;
                                return 1;
                            }

                        case "-":
                            offset = Integer.parseInt(registers.substring(2, 4), 16);
                            if ((Integer.parseInt(vcpu.sfProperty().get(), 16) & 0b00000001) == 0b00000001){
                                if ((pc - offset + 1) < 0){
                                    nextPIval = "1";
                                    nonCommandStep = true;
                                    setModeTo1 = true;
                                    nextTMRdecr = 1;
                                    TMRstep = true;
                                    return 1;
                                }
                                vcpu.setPC(pc - offset + 1);
                                nextTMRdecr = 1;
                                TMRstep = true;
                                return 1;
                            }
                            else {
                                vcpu.setPC(pc+1);
                                nextTMRdecr = 1;
                                TMRstep = true; 
                                return 1;
                            }
                        default:
                            throw new IOException("Bad offset formatting.");
                    }

                case "SHW ":
                    if (!contentProcessed){
                        contentProcessed = true;
                        ++pc;
                        pcBlock = (pc / 16);
                        pcWord = pc % 16;
                        registers = memory[pcBlock][pcWord].get();
                        int sharedWord = Integer.parseInt(registers.substring(1, 2), 16);
                        if (rcpu.shmProperty().get().charAt(sharedWord) == '1'){
                            first = rcpu.smt().memoryProperty(sharedWord);
                            second = memory[Integer.parseInt(registers.substring(2, 3), 16)][Integer.parseInt(registers.substring(3, 4), 16)];
                            first.setValue(second.get());
                            return 1;
                        } else {
                            // Shared memory was not locked
                            nextTMRdecr = 3;
                            TMRstep = true;
                            nextPIval = "5";
                            nonCommandStep = true;
                            return 1;
                        }
                    }
                    nextTMRdecr = 3;
                    TMRstep = true;
                    vcpu.setPC(pc+2);
                    contentProcessed = false;
                    clearBooleans();
                    return 1;

                case "SHR ":
                    if (!contentProcessed){
                        contentProcessed = true;
                        ++pc;
                        pcBlock = (pc / 16);
                        pcWord = pc % 16;
                        registers = memory[pcBlock][pcWord].get();
                        int sharedWord = Integer.parseInt(registers.substring(3, 4), 16);
                        if (rcpu.shmProperty().get().charAt(sharedWord) == '1'){
                            second = rcpu.smt().memoryProperty(sharedWord);
                            first = memory[Integer.parseInt(registers.substring(0, 1), 16)][Integer.parseInt(registers.substring(1, 2), 16)];
                            first.setValue(second.get());
                            return 1;
                        } else {
                            // Shared memory was not locked
                            nextTMRdecr = 3;
                            TMRstep = true;
                            nextPIval = "5";
                            nonCommandStep = true;
                            return 1;
                        }
                    }
                    nextTMRdecr = 3;
                    TMRstep = true;
                    vcpu.setPC(pc+2);
                    contentProcessed = false;
                    clearBooleans();
                    return 1;

                case "HALT":
                    if (rcpu.mdProperty().get().equals("0")){
                        rcpu.mdProperty().setValue("1");
                        return 1;
                    }

                    System.out.println("\u001B[31mEnd of programme.\u001B[0m");
                    nextTMRdecr = 3;
                    TMRstep = true;

                    rcpu.siProperty().setValue("7");
                    return 0;
            }
        } catch(NumberFormatException ex){
            nextPIval = "1";
            nonCommandStep = true;
            setModeTo1 = true;
            return 1;
        }
        
        // Unrecognised command
        nextPIval = "2";
        nonCommandStep = true;
        setModeTo1 = true;
        return 1;
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
        memory[block][word].setValue(memory[block][word].get());
    }
}