/*
 */
package interactivemodelos;

import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

/**
 * @author Paulius Staisiunas, Informatika 3 k., 3 gr.
 */
public class SharedMemoryTracker {
    private final SimpleStringProperty[] memory;
    
    public SharedMemoryTracker(SimpleStringProperty[] shared){
        memory = shared;
    }
    
    public StringProperty memoryProperty(int word) {
        return memory[word];
    }
}
