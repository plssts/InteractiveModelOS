/*
 */
package interactivemodelos;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import javafx.beans.property.SimpleStringProperty;

/**
 * @author Paulius Staisiunas, Informatika 3 k., 3 gr.
 */
public class Assembly {
    private boolean parseCodeGracefully = false;
    
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
            
            if (line.equals("CODE")){
                if (!parseData){
                    throw new IOException("DATA must come before CODE.");
                }
            }
            
            parseCode(br, commands);
        }
        
        return commands;
    }
    
    private void parseData(BufferedReader br, ArrayList commands) throws IOException{
        String line;
        
        while ((line = br.readLine()) != null){
            if (line.equals("CODE")){
                commands.add("[STC]"); // switch-to-code prasymas kraunant i atminti
                parseCodeGracefully = true; // pazymime, kad mes radome CODE segmenta, o ne siaip pateikiame faila i parseCode()
                return; // Eisime i  CODE segmento parsinima
            }
            
            String[] all = line.split(", 0");
            int wordCount = Integer.parseInt(line.split("w")[0].trim());
            
            // Jeigu cia yra argumentas-stringas
            if (all[0].contains("\"")){
                String word = all[0].split("\"")[1];
                for (int i = 0; i < wordCount; ++i){
                    if (i == wordCount - 1){
                        commands.add(word.substring(0, word.length()));
                        break;
                    }
                    commands.add(word.substring(0, 4));
                    word = word.substring(4);
                }
            }
            else { // Cia argumentas - skaicius signed int is 8 simboliu
                if (wordCount > 1){
                    throw new IOException("Use 1 w for constants.");
                }
                
                commands.add(line.split("w")[1].trim());
            }
            
        }
    }
    
    private void parseCode(BufferedReader br, ArrayList commands) throws IOException{
        if (!parseCodeGracefully){
            throw new IOException("Something is wrong with source code formatting");
        }
        
        String line;
        
        while ((line = br.readLine()) != null){
            for (int i = 0; i < line.length(); i += 4){
                String word = line.substring(i, i+4);
                commands.add(word);
            }
        }
    }
}
