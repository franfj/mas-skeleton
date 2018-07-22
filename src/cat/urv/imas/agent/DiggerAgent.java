package cat.urv.imas.agent;

import static cat.urv.imas.agent.ImasAgent.OWNER;
import cat.urv.imas.onthology.GameSettings;
import cat.urv.imas.behaviour.digger.DiggerMessageReceiver;
import cat.urv.imas.behaviour.digger.DiggerRequesterBehaviour;
import cat.urv.imas.map.Cell;
import cat.urv.imas.map.ManufacturingCenterCell;
import cat.urv.imas.map.PathCell;
import cat.urv.imas.map.SettableFieldCell;
import cat.urv.imas.onthology.DiggerInfoAgent;
import cat.urv.imas.onthology.InfoAgent;
import cat.urv.imas.onthology.MessageContent;
import cat.urv.imas.onthology.MetalType;
import cat.urv.imas.tools.BFS;
import cat.urv.imas.tools.Coordinates;
import cat.urv.imas.tools.Work;
import cat.urv.imas.tools.Work.WorkType;
import jade.core.*;
import jade.domain.*;
import jade.domain.FIPAAgentManagement.*;
import jade.domain.FIPANames.InteractionProtocol;
import jade.lang.acl.*;
import java.io.IOException;
import java.util.LinkedList;
import java.util.List;
import java.util.Stack;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * The Digger agent. 
 */
public class DiggerAgent extends ImasAgent {

    private boolean alreadyDiggered;
    private boolean working;
    private Work work;
    private Stack<Cell> pathToFollow;
    private Stack<Cell> pathToManufactureCenter;
    private boolean initializated = false;
    private int row;
    private int column;
    private Cell proposedCell;
    private Cell goalCell;
    private Cell centerCell;
    private MetalType goalMetal;

    public MetalType getGoalMetal() {
        return goalMetal;
    }

    public void setGoalMetal(MetalType goalMetal) {
        this.goalMetal = goalMetal;
    }

    public Cell getGoalCell() {
        return goalCell;
    }

    public void setGoalCell(Cell goalCell) {
        this.goalCell = goalCell;
    }
    
    /**
     * Game settings in use.
     */
    private GameSettings game;
    
    /**
     * System agent id.
     */
    private AID systemAgent;
    
        /**
     * System DiggerCoordinator id.
     */
    private AID diggerCoordinatorAgent;

    /**
     * Builds the digger agent.
     */
    public DiggerAgent() {
        super(AgentType.DIGGER);
        this.working = false;
    }

    public Cell getProposedCell() {
        return proposedCell;
    }

    /**
     * Agent setup method - called when it first come on-line. Configuration of
     * language to use, ontology and initialization of behaviours.
     */
    @Override
    protected void setup() {
        this.alreadyDiggered = false;
        
        /* ** Very Important Line (VIL) ***************************************/
        this.setEnabledO2ACommunication(true, 1);
        /* ********************************************************************/

        // Register the agent to the DF
        ServiceDescription sd1 = new ServiceDescription();
        sd1.setType(AgentType.DIGGER.toString());
        sd1.setName(getLocalName());
        sd1.setOwnership(OWNER);
        
        DFAgentDescription dfd = new DFAgentDescription();
        dfd.addServices(sd1);
        dfd.setName(getAID());
        try {
            DFService.register(this, dfd);
            log("Registered to the DF");
        } catch (FIPAException e) {
            System.err.println(getLocalName() + " registration with DF unsucceeded. Reason: " + e.getMessage());
            doDelete();
        }

        // search SystemAgent
        ServiceDescription searchCriterion = new ServiceDescription();
        searchCriterion.setType(AgentType.SYSTEM.toString());
        this.systemAgent = UtilsAgents.searchAgent(this, searchCriterion);
        
        // search CoordinatorAgent
        searchCriterion = new ServiceDescription();
        searchCriterion.setType(AgentType.DIGGER_COORDINATOR.toString());
        this.diggerCoordinatorAgent = UtilsAgents.searchAgent(this, searchCriterion);

        this.addBehaviour(new DiggerMessageReceiver(this));

        // setup finished. When we receive the last inform, the agent itself will add
        // a behaviour to send/receive actions
    }

    public void requestMap() {
        ACLMessage initialRequest = new ACLMessage(ACLMessage.REQUEST);
        initialRequest.clearAllReceiver();
        initialRequest.addReceiver(this.diggerCoordinatorAgent);
        initialRequest.setProtocol(InteractionProtocol.FIPA_REQUEST);
        log("Request message to agent");
        try {
            initialRequest.setContent(MessageContent.GET_MAP);
            log("Request message content:" + initialRequest.getContent());
            this.addBehaviour(new DiggerRequesterBehaviour(this, initialRequest));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    /**
     * Update the game settings.
     *
     * @param game current game settings.
     */
    public void setGame(GameSettings game) {
        this.game = game;
        
        if(!this.initializated) {
            List<Cell> cellsWithDiggers = game.getCellsWithDiggers();
                        
            for(Cell cell: cellsWithDiggers){
                if(((PathCell)cell).getAgents().get(AgentType.DIGGER) != null){
                    List<InfoAgent> agentsInCell = ((PathCell)cell).getAgents().get(AgentType.DIGGER);
                    if(!agentsInCell.isEmpty() && agentsInCell.get(0).getAID().equals(this.getAID())){
                        this.row = cell.getRow();
                        this.column = cell.getCol();
                    }
                }
            }
            
            List<InfoAgent> agentsInCell = ((PathCell) game.getMap()[row][column]).getAgents().get(AgentType.DIGGER);
            for(InfoAgent agent : agentsInCell) {
                if(agent.getAID().equals(this.getAID())){
                    DiggerInfoAgent diggerInfo = (DiggerInfoAgent) agent;
                    
                    diggerInfo.setCapacity(game.getDiggersCapacity());
                    log("I am the digger situated in (" + this.row + ", " + this.column + ")!!");
                }
            }
            
            initializated = true;
        }
    }
    
    /**
     * Gets the current game settings.
     *
     * @return the current game settings.
     */
    public GameSettings getGame() {
        return this.game;
    }

    public void setProposedCell(Cell proposedCell) {
        this.proposedCell = proposedCell;
    }
    
    public int calculateDistanceToProposedCell() {
        if(working) {
            if(proposedCell.getRow() == goalCell.getRow() && proposedCell.getCol() == goalCell.getCol()){
                return -1;
            } else {
                return Integer.MAX_VALUE;
            }
        } else {
            if(((SettableFieldCell) getProposedCell()).getMetal().get(MetalType.GOLD) == null) {
                setGoalMetal(MetalType.SILVER);
            } else {
                setGoalMetal(MetalType.GOLD);
            }
            
            BFS bfs = new BFS(this.game.getMap());
            int stepsToGoal = bfs.calculateDistanceBetweenCells(this.game.getMap()[row][column], proposedCell, true);
            this.pathToFollow = bfs.getPathToGoal();
            
            calculateBestManufactureCenter();
            
            return stepsToGoal;
        }
    }

    public void setPathToFollow(Stack<Cell> pathToFollow) {
        this.pathToFollow = pathToFollow;
    }

    public Stack<Cell> getPathToFollow() {
        return pathToFollow;
    }

    public boolean isWorking() {
        return working;
    }

    public void setWorking(boolean working) {
        this.working = working;
    }
    
    public void work() {
        if(working) {
            if(!pathToFollow.isEmpty()) {
                Cell nextCell = pathToFollow.pop();
                if(nextCell.getCol() == column && nextCell.getRow() == row){
                    nextCell = pathToFollow.pop();
                }

                moveToCell(nextCell);
            } else if (this.alreadyDiggered) {
                manufacture();
            } else {
                mine();
            }
        } else {
            sendWork();
        }
    }
    
    public void manufacture() {
        DiggerInfoAgent diggerInfo = null;
        
        List<InfoAgent> agentsInCell = ((PathCell) game.getMap()[row][column]).getAgents().get(AgentType.DIGGER);
        for(InfoAgent agent : agentsInCell) {
            if(agent.getAID().equals(this.getAID())){
                diggerInfo = (DiggerInfoAgent) agent;
            }
        }
        
        if(diggerInfo.getCapacity() < game.getDiggersCapacity()) {
            diggerInfo.increaseCapacity();
            this.work.setType(WorkType.MANUFACTURE);
            this.work.setOldPos(new Coordinates(row, column));
            this.work.setNewPos(new Coordinates(this.centerCell.getRow(), this.centerCell.getCol()));
            
            sendWork();
        } else {
            this.work.setType(WorkType.NOTHING);
            this.working = false;
            this.pathToFollow = null;
            this.pathToManufactureCenter = null;
            this.goalCell = null;
            this.centerCell = null;
            this.proposedCell = null;
            this.alreadyDiggered = false;
            
            sendWork();
        }
    }
    
    public void sendWork() {
        ACLMessage msg = new ACLMessage(ACLMessage.INFORM);
        msg.setSender(this.getAID());
        msg.addReceiver(diggerCoordinatorAgent);

        try {
            msg.setContentObject(work);
        } catch (IOException ex) {
            Logger.getLogger(DiggerAgent.class.getName()).log(Level.SEVERE, null, ex);
        }

        log("Sending my step decision to " + diggerCoordinatorAgent.getLocalName());
        send(msg);
    }
    
    public void mine() {
        DiggerInfoAgent diggerInfo = null;
        
        List<InfoAgent> agentsInCell = ((PathCell) game.getMap()[row][column]).getAgents().get(AgentType.DIGGER);
        for(InfoAgent agent : agentsInCell) {
            if(agent.getAID().equals(this.getAID())){
                diggerInfo = (DiggerInfoAgent) agent;
            }
        }
        
        if(diggerInfo.getCapacity() > 0) {
            SettableFieldCell c = ((SettableFieldCell) goalCell);
            if(c.isThereAnyMetal()) {
                work.setType(WorkType.MINE);
                work.setOldPos(new Coordinates(row, column));
                work.setNewPos(new Coordinates(goalCell.getRow(), goalCell.getCol()));
                diggerInfo.decreaseCapacity();
                c.removeMetal();
                
                sendWork();
            } else {
                prepareManufacture();
            }
        } else {
            prepareManufacture();
        }
    }    
    
    private void calculateBestManufactureCenter() {
        List<Cell> centers = game.getCellsWithManufactureCenters();
        List<Cell> interestingCenters = new LinkedList<>();
        
        for(int i = 0; i < centers.size(); ++i) {
            ManufacturingCenterCell center = ((ManufacturingCenterCell) centers.get(i));
            if(center.getMetal() == goalMetal) {
                interestingCenters.add(center);
            }
        }
        
        int bestDistance = Integer.MAX_VALUE;
        for(Cell center : interestingCenters) {
            BFS bfs = new BFS(game.getMap());
            int newDistance = bfs.calculateDistanceBetweenCells(this.pathToFollow.firstElement(), center, true);
            
            if(newDistance < bestDistance) {
                this.centerCell = center;
                this.pathToManufactureCenter = bfs.getPathToGoal();
                bestDistance = newDistance;
            }
        }
    }

    private void prepareManufacture() {
        this.alreadyDiggered = true;
        this.goalCell = this.centerCell;
        this.pathToFollow = this.pathToManufactureCenter;
        
        work();
    }
    
    public void setWork(Work work) {
        this.work = work;
    }
    
    public void moveToCell(Cell cellToGo) {
        work.setType(WorkType.MOVE);
        work.setOldPos(new Coordinates(row, column));
        
        this.row = cellToGo.getRow();
        this.column = cellToGo.getCol();
        
        work.setNewPos(new Coordinates(row, column));
        
        sendWork();
    }
    
}
