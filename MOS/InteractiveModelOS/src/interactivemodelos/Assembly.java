/*
Parses through source files and assembles instruction sets into commands.
 */
package interactivemodelos;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;

/**
 * @author Paulius Staisiunas, Computer Science 3 yr., 3 gr.
 */

public class Assembly {
    private boolean parseCodeGracefully = false;
    private int codeStart = 0;
    private int dataWords = 0;
    
    public ArrayList parseFile(File file) throws FileNotFoundException, IOException{
        ArrayList<String> commands = new ArrayList<>();
        BufferedReader br = new BufferedReader(new FileReader(file));
        String line;
        boolean parseData = false;
        
        while ((line = br.readLine()) != null){
            if (line.equals("DATA")){
                parseData = true;
                parseData(br, commands);
            }
            
            if (line.startsWith("CODE")){
                if (!parseData){
                    throw new IOException("DATA goes before CODE.");
                }
            }
            parseCode(br, commands);
        }
        return commands;
    }
    
    private void parseData(BufferedReader br, ArrayList commands) throws IOException{
        String line;
        
        while ((line = br.readLine()) != null){
            if (line.startsWith("CODE")){
                try {
                    codeStart = Integer.parseInt(line.split(" ")[1], 16);
                    if (codeStart > 255){
                        throw new NumberFormatException();
                    }
                } catch(ArrayIndexOutOfBoundsException | NumberFormatException ex){
                    throw new IOException("Bad CODE PC formatting.");
                }
                
                commands.add("[STC] " + Integer.toHexString(codeStart)); 
                // switch-to-code request before loading into virtual memory
                
                parseCodeGracefully = true; // correct parsing was detected
                return;
            }
            
            String[] all = line.split(", 0");
            int wordCount = Integer.parseInt(line.split("w")[0].trim());
            dataWords += wordCount;
            if (dataWords > 256){
                throw new IOException("DATA memory size above 16 blocks.");
            }
            
            // String variable
            if (all[0].contains("\"")){
                String word = all[0].split("\"")[1];
                for (int i = 0; i < wordCount; ++i){
                    if (i == wordCount - 1){
                        commands.add(word.substring(0, word.length()));
                        if (word.substring(0, word.length()).length() > 4){
                            // less words than needed for this variable
                            throw new IOException("Incorrect word count " + wordCount + " for " + word);
                        }
                        break;
                    }
                    try {
                        commands.add(word.substring(0, 4));
                    } catch(StringIndexOutOfBoundsException ex){
                        // more words than needed for this variable
                        throw new IOException("Incorrect word count " + wordCount + " for " + word);
                    }
                    word = word.substring(4);
                }
            }
            else { // Constant variable
                if (wordCount > 1){
                    throw new IOException("Use 1 w for constants.");
                }
                
                commands.add(line.split("w")[1].trim());
            }
        }
    }
    
    private void parseCode(BufferedReader br, ArrayList commands) throws IOException{
        int codeWords = 0;
        if (!parseCodeGracefully){
            throw new IOException("Bad programme formatting.");
        }
        
        String line;
        
        while ((line = br.readLine()) != null){
            for (int i = 0; i < line.length(); i += 4){
                ++codeWords;
                if (codeWords > (255 - dataWords)){
                    throw new IOException("Too many commands.");
                }
                String word = line.substring(i, i+4);
                commands.add(word);
            }
        }
    }
}