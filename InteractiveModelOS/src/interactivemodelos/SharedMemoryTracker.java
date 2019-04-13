/*
Makes the shared memory visible to a virtual machine without direct access to the real CPU
 */
package interactivemodelos;

import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

/**
 * @author Paulius Staisiunas, Computer Science 3 yr., 3 gr.
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