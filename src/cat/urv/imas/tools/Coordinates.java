package cat.urv.imas.tools;

import java.io.Serializable;

/**
 * This class store a row and a column.
 */
public class Coordinates implements Serializable {
    
    /**
     * The row.
     */
    private final int row;
    
    /**
     * The column.
     */
    private final int col;

    /**
     * Constructor
     * @param row The row
     * @param col The column
     */
    public Coordinates(int row, int col) {
        this.row = row;
        this.col = col;
    }

    /**
     * Gets the row
     * @return row
     */
    public int getRow() {
        return row;
    }

    /**
     * Gets the column
     * @return column
     */
    public int getCol() {
        return col;
    }

    /**
     * Implementation of hashCode method.
     */
    @Override
    public int hashCode() {
        int hash = 7;
        hash = 47 * hash + this.row;
        hash = 47 * hash + this.col;
        return hash;
    }

    /**
     * Implementation of equals method.
     */
    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        
        if (getClass() != obj.getClass()) {
            return false;
        }
        
        final Coordinates other = (Coordinates) obj;
        
        if (this.row != other.row) {
            return false;
        }
        
        return this.col == other.col;
    }

}