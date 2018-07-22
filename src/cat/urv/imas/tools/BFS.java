package cat.urv.imas.tools;

import cat.urv.imas.map.Cell;
import cat.urv.imas.map.FieldCell;
import cat.urv.imas.map.ManufacturingCenterCell;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.Queue;
import java.util.Stack;

/**
 * Breadth First Search implementation.
 */
public class BFS {
    
    /**
     * Nodes used in the BFS process.
     */
    private final Map<Coordinates, BFSNode> nodes;
    
    /**
     * The game map.
     */
    private final Cell[][] map;
    
    /**
     * Path from start node to goal node generated in BFS process.
     */
    private final Stack<Cell> pathToGoal;
    
    /**
     * Constructor
     * @param map The game map
     */
    public BFS(Cell[][] map) {
        this.map = map;
        this.nodes = new HashMap<>();
        this.pathToGoal = new Stack<>();
    }    
    
    /**
     * Gets the path to goal
     * @return The path to goal
     */
    public Stack<Cell> getPathToGoal() {
        return pathToGoal;
    }
        
    /**
     * BFS process.
     * @param startCell The starting cell
     * @param goalCell The goal cell
     * @param includeFirstStep Add or not first cell in pathToGoal
     * @return The distance
     */
    public int calculateDistanceBetweenCells(Cell startCell, Cell goalCell, boolean includeFirstStep) {        
        Map<Integer, BFSNode> auxMap = new HashMap<>();
        
        // Add startCell
        BFSNode newNode = new BFSNode(startCell, null);
        newNode.setDistance(0);
        nodes.put(new Coordinates(startCell.getRow(), startCell.getCol()), newNode);
        
        boolean goalCellHasBeenReached = false;
        
        Queue<BFSNode> neighboursToVisit = new LinkedList<>();
        addNeighbours(startCell, neighboursToVisit);
        
        while(!goalCellHasBeenReached) {
            BFSNode currentNode = neighboursToVisit.poll();
            
            if(currentNode != null){
                int currentRow = currentNode.getCell().getRow();
                int currentCol = currentNode.getCell().getCol();
                Coordinates currentCoordinates = new Coordinates(currentRow, currentCol);

                // Check if current node is the goal one
                if(currentRow == goalCell.getRow() && currentCol == goalCell.getCol()){
                    goalCellHasBeenReached = true;

                    currentNode.setDistance(currentNode.getParentCell().getDistance());
                    currentNode.setVisited(true);

                    nodes.put(currentCoordinates, currentNode);
                }
                
                // Discard field and manufacturing cells
                else if(!(map[currentRow][currentCol] instanceof FieldCell) && 
                        !(map[currentRow][currentCol] instanceof ManufacturingCenterCell)){
                    currentNode.setDistance(currentNode.getParentCell().getDistance() + 1);
                    currentNode.setVisited(true);

                    nodes.put(currentCoordinates, currentNode);

                    addNeighbours(currentNode.getCell(), neighboursToVisit);
                }
            } 
        }
        
        // Once the process is done, generate pathToGoal
        generatePathToGoal(goalCell, includeFirstStep);
        return nodes.get(new Coordinates(goalCell.getRow(), goalCell.getCol())).getDistance();
    }
       
    /**
     * Add node to neighbours not visited yet queue.
     * @param currentNode Node currently being analysed
     * @param neightboursToVisit Queue with nodes not yet visited
     */
    private void addNeighbours(Cell currentNode, Queue<BFSNode> neightboursToVisit){  
        BFSNode currentBFSNode = nodes.get(new Coordinates(currentNode.getRow(), currentNode.getCol()));
        
        // Add left neighbour
        if((currentNode.getRow() - 1) >= 0){
            if(nodes.get(new Coordinates(currentNode.getRow() - 1, currentNode.getCol())) == null){
                BFSNode newNode = new BFSNode(map[currentNode.getRow() - 1][currentNode.getCol()], currentBFSNode);

                neightboursToVisit.add(newNode);
                nodes.put(new Coordinates(currentNode.getRow() - 1, currentNode.getCol()), newNode);
            }
        }
        
        // Add right neighbour
        if((currentNode.getRow() + 1) < map.length){
            if(nodes.get(new Coordinates(currentNode.getRow() + 1, currentNode.getCol())) == null){
                BFSNode newNode = new BFSNode(map[currentNode.getRow() + 1][currentNode.getCol()], currentBFSNode);

                neightboursToVisit.add(newNode);
                nodes.put(new Coordinates(currentNode.getRow() + 1, currentNode.getCol()), newNode);
            }
        }
        
        // Add bottom neighbour
        if((currentNode.getCol() - 1) >= 0){
            if(nodes.get(new Coordinates(currentNode.getRow(), currentNode.getCol() - 1)) == null){
                BFSNode newNode = new BFSNode(map[currentNode.getRow()][currentNode.getCol() - 1], currentBFSNode);

                neightboursToVisit.add(newNode);
                nodes.put(new Coordinates(currentNode.getRow(), currentNode.getCol() - 1), newNode);
            }
        }
        
        // Add upper neighbour
        if((currentNode.getCol() + 1) < map.length){
                        if(nodes.get(new Coordinates(currentNode.getRow(), currentNode.getCol() + 1)) == null){
                BFSNode newNode = new BFSNode(map[currentNode.getRow()][currentNode.getCol() + 1], currentBFSNode);

                neightboursToVisit.add(newNode);
                nodes.put(new Coordinates(currentNode.getRow(), currentNode.getCol() + 1), newNode);
            }
        }
        
    }
    
    /**
     * Generate the path to the goal 
     * @param goalCell The goal cell
     * @param includeFirstStep Add or not first cell in pathToGoal
     */
    private void generatePathToGoal(Cell goalCell, boolean includeFirstStep) {
        BFSNode node = nodes.get(new Coordinates(goalCell.getRow(), goalCell.getCol()));
        
        if(!includeFirstStep) {
            node = node.parentCell;
        }
        
        while(node.parentCell != null){
            pathToGoal.add(node.parentCell.getCell());
            node = node.parentCell;
        }
    }

    /**
     * Class used auxiliar reasons to the BFS process
     */
    private class BFSNode {
        
        /**
         * If the node has been visited
         */
        private boolean visited;
        
        /**
         * Distance from the starting node to the node
         */
        private int distance;
        
        /**
         * Cell of the given node
         */
        private final Cell cell;
        
        /**
         * The parent node
         */
        private BFSNode parentCell;

        /**
         * Constructor
         * @param cell The cell of the given node
         * @param parentCell The parent node
         */
        public BFSNode(Cell cell, BFSNode parentCell) {
            this.cell = cell;
            this.parentCell = parentCell;
            this.distance = Integer.MAX_VALUE;
            this.visited = false;
        }

        /**
         * Gets if the node has been visited.
         * @return visited
         */
        public boolean isVisited() {
            return visited;
        }

        /**
         * Sets if the node has been visited.
         * @param visited 
         */
        public void setVisited(boolean visited) {
            this.visited = visited;
        }

        /**
         * Gets the distance.
         * @return distance
         */
        public int getDistance() {
            return distance;
        }

        /**
         * Sets the distance
         * @param distance 
         */
        public void setDistance(int distance) {
            this.distance = distance;
        }

        /**
         * Gets the parent node
         * @return parentCell
         */
        public BFSNode getParentCell() {
            return parentCell;
        }

        /**
         * Sets the parent node
         * @param parentCell the parent node
         */
        public void setParentCell(BFSNode parentCell) {
            this.parentCell = parentCell;
        }

        /**
         * Gets the cell of the node.
         * @return cell
         */
        public Cell getCell() {
            return cell;
        }

    }
       
}
