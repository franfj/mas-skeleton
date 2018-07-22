package cat.urv.imas.tools;

import cat.urv.imas.onthology.GameSettings;
import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

/**
 * This class stores the game statistics.
 */
public class Stats implements Serializable {
    
    /**
     * The game itself.
     */
    private GameSettings game;
    
    /**
     * Total number of points obtained.
     */
    private double totalPoints;
    
    /**
     * Total gold units manufactured.
     */
    private double goldManufactured;
    
    /**
     * Total silver units manufactured.
     */
    private double silverManufactured;
    
    /**
     * Total number of points obtained with gold.
     */
    private double goldPoints;
    
    /**
     * Total number of points obtained with silver.
     */
    private double silverPoints;
    
    /**
     * Average time until a cell is mined for the first time.
     */
    private double avgTimeUntilMining;
    
    /**
     * Percentage of metal cells mined.
     */
    private double percentageMetalMined;
    
    /**
     * Map that stores the cells mined.
     */
    private final Map<Coordinates, Integer> cellsMined;
    
    /**
     * Default constructor.
     */
    public Stats() {
        this.totalPoints = 0.0;
        this.goldManufactured = 0.0;
        this.silverManufactured = 0.0;
        this.goldPoints = 0.0;
        this.silverPoints = 0.0;
        this.cellsMined = new HashMap();
    }
    
    /**
     * Add the cell mined and the first step in which it has been mineed.
     * @param coordinates The cell coordinates
     * @param step The current step
     */
    public void addCellMined(Coordinates coordinates, int step) {
        if(cellsMined.get(coordinates) == null){
            cellsMined.put(coordinates, step);
        }
    }

    /**
     * Gets the cell mined map.
     * @return The cell mined map
     */
    public Map<Coordinates, Integer> getCellsMined() {
        return cellsMined;
    }
    
    /**
     * Add points to total.
     * @param d The points
     */
    public void addPoints(double d) {
        this.totalPoints += d;
    }
        
    /**
     * Add units and points to gold total.
     * @param n The number of units
     * @param d The points
     */
    public void addGoldPoints(double n, double d) {
        this.goldManufactured += n;
        this.goldPoints += d;
    }
        
    /**
     * Add units and points to silver total.
     * @param n The number of units
     * @param d The points
     */
    public void addSilverPoints(double n, double d) {
        this.silverManufactured += n;
        this.silverPoints += d;
    }
    
    /**
     * Sets the game.
     * @param game The game
     */
    public void setGame(GameSettings game) {
        this.game = game;
    }

    /**
     * Gets the total of points.
     * @return the total points
     */
    public double getTotalPoints() {
        return totalPoints;
    }

    /**
     * Gets the gold units manufactured.
     * @return Gold units manufactured
     */
    public double getGoldManufactured() {
        return goldManufactured;
    }

    /**
     * Gets the silver units manufactured.
     * @return Silver units manufactured
     */
    public double getSilverManufactured() {
        return silverManufactured;
    }

    /**
     * Gets the gold points manufactured.
     * @return Gold points manufactured
     */
    public double getGoldPoints() {
        return goldPoints;
    }

    /**
     * Gets the silver points manufactured.
     * @return Silver points manufactured
     */
    public double getSilverPoints() {
        return silverPoints;
    }

    /**
     * Gets the average of time until the metal cells are mined.
     * @return The average time
     */
    public double getAvgTimeUntilMining() {
        return avgTimeUntilMining;
    }

    /**
     * Gets the percentage of metal mined.
     * @return Percentage of metal mined
     */
    public double getPercentageMetalMined() {
        return percentageMetalMined;
    }

    /**
     * Gets the game.
     * @return The game
     */
    public GameSettings getGame() {
        return game;
    }
    
}
