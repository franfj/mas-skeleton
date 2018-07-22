package cat.urv.imas.tools;

import cat.urv.imas.map.Cell;
import jade.core.AID;
import java.io.Serializable;

/**
 * Class used for storing information about the work done by diggers.
 */
public class Work implements Serializable {
    
    /**
     * The work type.
     */
    private WorkType type;
    
    /**
     * The digger AID.
     */
    private final AID diggerAID;
    
    /**
     * The old position coordinates.
     */
    private Coordinates oldPos;
    
    /**
     * The new position coordinates.
     */
    private Coordinates newPos;
    
    /**
     * The cell being mined.
     */
    private Cell fieldMined;
    
    /**
     * The cell being manufactured.
     */
    private Cell fieldManufacture;

    /**
     * Constructor
     * @param aid The digger AID
     * @param fieldMined The field being mined
     * @param fieldManufacture The field being manufactured
     */
    public Work(AID aid, Cell fieldMined, Cell fieldManufacture) {
        this.type = WorkType.NOTHING;
        this.diggerAID = aid;
        this.fieldMined = fieldMined;
        this.fieldManufacture = fieldManufacture;
    }

    /**
     * Gets the work type.
     * @return The work type
     */
    public WorkType getType() {
        return type;
    }

    /**
     * Sets the work type.
     * @param type The work type
     */
    public void setType(WorkType type) {
        this.type = type;
    }

    /**
     * Gets the old position.
     * @return Old position
     */
    public Coordinates getOldPos() {
        return oldPos;
    }

    /**
     * Sets the old position.
     * @param oldPos The old position.
     */
    public void setOldPos(Coordinates oldPos) {
        this.oldPos = oldPos;
    }

    /**
     * Gets the new position.
     * @return The new position.
     */
    public Coordinates getNewPos() {
        return newPos;
    }

    /**
     * Sets the new position.
     * @param newPos The new position
     */
    public void setNewPos(Coordinates newPos) {
        this.newPos = newPos;
    }

    /**
     * Gets the digger AID.
     * @return The digger AID
     */
    public AID getDiggerAID() {
        return diggerAID;
    }
           
    /**
     * Enum that stores the work type.
     */
    public enum WorkType {
        MOVE,
        MINE,
        MANUFACTURE,
        NOTHING
    }
    
}