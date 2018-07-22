package cat.urv.imas.agent;

import cat.urv.imas.tools.WorkTypeComparator;
import static cat.urv.imas.agent.ImasAgent.OWNER;
import cat.urv.imas.behaviour.common.InformNewStep;
import cat.urv.imas.behaviour.diggercoordinator.DiggerCoordinatorContractNetInitiator;
import cat.urv.imas.behaviour.diggercoordinator.DiggerCoordinatorMessageReceiver;
import cat.urv.imas.behaviour.diggercoordinator.DiggerCoordinatorRequesterBehaviour;
import cat.urv.imas.map.Cell;
import cat.urv.imas.map.FieldCell;
import cat.urv.imas.map.ManufacturingCenterCell;
import cat.urv.imas.map.PathCell;
import cat.urv.imas.map.SettableFieldCell;
import cat.urv.imas.onthology.DiggerInfoAgent;
import cat.urv.imas.onthology.GameSettings;
import cat.urv.imas.onthology.InfoAgent;
import cat.urv.imas.onthology.MessageContent;
import cat.urv.imas.onthology.MetalType;
import cat.urv.imas.tools.Coordinates;
import cat.urv.imas.tools.Stats;
import cat.urv.imas.tools.Work;
import jade.core.*;
import jade.domain.*;
import jade.domain.FIPAAgentManagement.*;
import jade.domain.FIPANames.InteractionProtocol;
import jade.lang.acl.ACLMessage;
import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * The DiggerCoordinator agent. 
 * TODO: All
 */
public class DiggerCoordinatorAgent extends ImasAgent {

    private Stats stats;
    private List<Work> jobsToExecute;
    private int nDiggersToContractNet;
    private Cell cellToBeAssigned;
    private List<ACLMessage> proposals;
    
    private boolean initialized;

    public AID getCoordinatorAgent() {
        return coordinatorAgent;
    }

    public Stats getStats() {
        return stats;
    }

    public void setStats(Stats stats) {
        this.stats = stats;
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
     * Coordinator agent id.
     */
    private AID coordinatorAgent;

    /**
     * Digger agents ids.
     */
    private List<AID> diggerAgents;
    
    /**
     * Builds the diggercoordinator agent.
     */
    public DiggerCoordinatorAgent() {
        super(AgentType.DIGGER_COORDINATOR);
        
        this.proposals = new LinkedList<>();
    }

    /**
     * Agent setup method - called when it first come on-line. Configuration of
     * language to use, ontology and initialization of behaviours.
     */
    @Override
    protected void setup() {
        /* ** Very Important Line (VIL) ***************************************/
        this.setEnabledO2ACommunication(true, 1);
        /* ********************************************************************/

        // Register the agent to the DF
        ServiceDescription sd1 = new ServiceDescription();
        sd1.setType(AgentType.DIGGER_COORDINATOR.toString());
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
        
        this.initialized = false;
        
        this.jobsToExecute = new LinkedList<>();
        
        // search SystemAgent
        ServiceDescription searchCriterion = new ServiceDescription();
        searchCriterion.setType(AgentType.SYSTEM.toString());
        this.systemAgent = UtilsAgents.searchAgent(this, searchCriterion);
        
        // search CoordinatorAgent
        searchCriterion = new ServiceDescription();
        searchCriterion.setType(AgentType.COORDINATOR.toString());
        this.coordinatorAgent = UtilsAgents.searchAgent(this, searchCriterion);
        
        // search DiggerAgents
        searchCriterion = new ServiceDescription();
        searchCriterion.setType(AgentType.DIGGER.toString());
        this.diggerAgents = new LinkedList<>();
        this.diggerAgents = UtilsAgents.searchMultipleAgents(this, searchCriterion);
        
        this.nDiggersToContractNet = diggerAgents.size();

        // setup finished. When we receive the last inform, the agent itself will add
        // a behaviour to send/receive actions
        
        this.addBehaviour(new DiggerCoordinatorMessageReceiver(this));
    }
    
    public void requestMap() {
        ACLMessage initialRequest = new ACLMessage(ACLMessage.REQUEST);
        initialRequest.clearAllReceiver();
        initialRequest.addReceiver(this.coordinatorAgent);
        initialRequest.setProtocol(InteractionProtocol.FIPA_REQUEST);
        log("Request message to agent");
        try {
            initialRequest.setContent(MessageContent.GET_MAP);
            log("Request message content:" + initialRequest.getContent());
        } catch (Exception e) {
            e.printStackTrace();
        }

        this.addBehaviour(new DiggerCoordinatorRequesterBehaviour(this, initialRequest));
    }
    
    public void simulateSteps() { 
        this.addBehaviour(new InformNewStep(this, AgentType.DIGGER));
        getRandomMiningCell();
    }
    
    public void startContractNetProtocol() {
        this.nDiggersToContractNet--;
        
        if(this.nDiggersToContractNet == 0){
            this.nDiggersToContractNet = this.diggerAgents.size();
            
            ACLMessage cfp = new ACLMessage(ACLMessage.CFP);
            for (AID digger : this.diggerAgents) {
                cfp.addReceiver(digger);
            }

            //Define CFP
            cfp.setProtocol(FIPANames.InteractionProtocol.FIPA_CONTRACT_NET);
            
            Map<String, Cell> content = new HashMap<>();
            content.put("get offer", cellToBeAssigned);
            
            try {
                cfp.setContentObject((Serializable) content);
            } catch (IOException ex) {
                Logger.getLogger(DiggerCoordinatorAgent.class.getName()).log(Level.SEVERE, null, ex);
            }
            
            cfp.setReplyByDate(new Date(System.currentTimeMillis() + 1000));

            log("Start Contract Net protocol");

            this.addBehaviour(new DiggerCoordinatorContractNetInitiator(this, cfp));
        }
    }
    
    public void getRandomMiningCell() {
        Random rand = new Random();
       
        int nCellsWithMetal = game.getCellsWithMetal().size();
        
        if(nCellsWithMetal != 0) {
            Cell cell = game.getCellsWithMetal().get(Math.abs(rand.nextInt()) % nCellsWithMetal);

            this.log("In this step (" + game.getSimulationSteps() + 
                    " to finish) ,  cell " + cell + " will be assigned");

            this.cellToBeAssigned = cell;
        } else {
            this.cellToBeAssigned = null;
        }
    }

    public List<ACLMessage> getProposals() {
        return proposals;
    }
    
    public void executeJobs() { 
        stats = new Stats();
        
        if(jobsToExecute.size() == diggerAgents.size()){
            List<Coordinates> newPositions = new ArrayList<>();
            
            Collections.sort(jobsToExecute, new WorkTypeComparator());
            for(Work job: jobsToExecute){
                if(job.getType() == Work.WorkType.MOVE) {
                    try {
                        
                        DiggerInfoAgent diggerInfo = null;
                        List<InfoAgent> agentsInCell = ((PathCell) game.getMap()[job.getOldPos().getRow()][job.getOldPos().getCol()]).getAgents().get(AgentType.DIGGER);
                        for(InfoAgent agent : agentsInCell) {
                            if(agent.getAID().equals(job.getDiggerAID())){
                                diggerInfo = (DiggerInfoAgent) agent;
                            }
                        }
                        
                        ((PathCell) game.getMap()[job.getOldPos().getRow()][job.getOldPos().getCol()])
                                .removeAgent(diggerInfo);
                        
                        ((PathCell) game.getMap()[job.getNewPos().getRow()][job.getNewPos().getCol()])
                                .addAgent(diggerInfo);
                        
                        newPositions.add(job.getNewPos());
                        
                    } catch (Exception ex) {
                        Logger.getLogger(DiggerCoordinatorAgent.class.getName()).log(Level.SEVERE, null, ex);
                    }
                    
                } else if(job.getType() == Work.WorkType.MINE) {
                    boolean canMine = true;
                    for(Coordinates newPosition : newPositions){
                        if(newPosition.equals(job.getNewPos())) {
                            canMine = false;
                        }
                    }
                        
                    DiggerInfoAgent diggerInfo = null;
                    List<InfoAgent> agentsInCell = ((PathCell) game.getMap()[job.getOldPos().getRow()][job.getOldPos().getCol()]).getAgents().get(AgentType.DIGGER);
                    for(InfoAgent agent : agentsInCell) {
                        if(agent.getAID().equals(job.getDiggerAID())){
                            diggerInfo = (DiggerInfoAgent) agent;
                        }
                    }
                    
                    if(canMine) {
                        SettableFieldCell metalField = ((SettableFieldCell) game.getMap()[job.getNewPos().getRow()][job.getNewPos().getCol()]);
                        metalField.removeMetal();

                        if(metalField.getMetal().get(MetalType.GOLD) == null || metalField.getMetal().get(MetalType.GOLD) == 0) {
                            if(metalField.getMetal().get(MetalType.SILVER) == null || metalField.getMetal().get(MetalType.SILVER) == 0) {
                                game.getMap()[job.getNewPos().getRow()][job.getNewPos().getCol()] = new FieldCell(job.getNewPos().getRow(), job.getNewPos().getCol());
                            }
                        }

                        diggerInfo.decreaseCapacity();
                        stats.addCellMined(new Coordinates(job.getNewPos().getRow(), job.getNewPos().getCol()), game.getSimulationSteps());
                    } else {
                        diggerInfo.increaseCapacity();
                        
                        ACLMessage msg = new ACLMessage(ACLMessage.INFORM);
                        msg.setSender(this.getAID());
                        msg.addReceiver(job.getDiggerAID());

                        msg.setContent("Mining rejected");

                        log("Mining rejected, " + job.getDiggerAID().getLocalName());
                        send(msg);
                    }
                    
                } else if(job.getType() == Work.WorkType.MANUFACTURE) {
                    DiggerInfoAgent diggerInfo = null;
                    List<InfoAgent> agentsInCell = ((PathCell) game.getMap()[job.getOldPos().getRow()][job.getOldPos().getCol()]).getAgents().get(AgentType.DIGGER);
                    for(InfoAgent agent : agentsInCell) {
                        if(agent.getAID().equals(job.getDiggerAID())){
                            diggerInfo = (DiggerInfoAgent) agent;
                        }
                    }
                    
                    diggerInfo.increaseCapacity();
                    ManufacturingCenterCell centerCell = ((ManufacturingCenterCell) game.getMap()[job.getNewPos().getRow()][job.getNewPos().getCol()]);

                    stats.addPoints(centerCell.getPrice());
                    if(centerCell.getMetal() == MetalType.GOLD){
                        stats.addGoldPoints(1.0, centerCell.getPrice());
                    } else {
                        stats.addSilverPoints(1.0, centerCell.getPrice());
                    }

                }
            }
            
            stats.setGame(this.game);
            
            ACLMessage msg = new ACLMessage(ACLMessage.INFORM);
            msg.setSender(this.getAID());
            msg.addReceiver(coordinatorAgent);

            try {
                msg.setContentObject(stats);
            } catch (IOException ex) {
                Logger.getLogger(DiggerAgent.class.getName()).log(Level.SEVERE, null, ex);
            }

            log("Sending new map to " + coordinatorAgent.getLocalName());
            send(msg);
            
            jobsToExecute.clear();
        }
    }
    
    public void addJob(Work w) { 
        this.jobsToExecute.add(w);
    }

    public List<Work> getJobsToExecute() {
        return jobsToExecute;
    }
    
    /**
     * Update the game settings.
     *
     * @param game current game settings.
     */
    public void setGame(GameSettings game) {
        this.game = game;
    }

    /**
     * Gets the current game settings.
     *
     * @return the current game settings.
     */
    public GameSettings getGame() {
        return this.game;
    }

    public void prepareMapForDiggers() {
        if(!initialized){
            List<Cell> cellsWithDiggers = game.getCellsWithDiggers();
            
            int j = 0;
            for(Cell cell: cellsWithDiggers){
                if(((PathCell)cell).getAgents().get(AgentType.DIGGER) != null){
                    List<InfoAgent> agentsInCell = ((PathCell)cell).getAgents().get(AgentType.DIGGER);
                    for(InfoAgent a : agentsInCell){
                        a.setAID(diggerAgents.get(j++));
                        ((DiggerInfoAgent) a).setCapacity(game.getDiggersCapacity());
                    }
                }
            }
            
            initialized = true;
        }
    }

}
