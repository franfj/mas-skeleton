/**
 *  IMAS base code for the practical work.
 *  Copyright (C) 2014 DEIM - URV
 *
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package cat.urv.imas.agent;

import cat.urv.imas.behaviour.common.InformNewStep;
import cat.urv.imas.behaviour.system.SystemMessageReceiver;
import cat.urv.imas.onthology.InitialGameSettings;
import cat.urv.imas.onthology.GameSettings;
import cat.urv.imas.gui.GraphicInterface;
import cat.urv.imas.behaviour.system.SystemRequestResponseBehaviour;
import cat.urv.imas.map.Cell;
import cat.urv.imas.map.PathCell;
import cat.urv.imas.map.SettableFieldCell;
import cat.urv.imas.onthology.DiggerInfoAgent;
import cat.urv.imas.onthology.InfoAgent;
import cat.urv.imas.onthology.MetalType;
import cat.urv.imas.tools.Coordinates;
import cat.urv.imas.tools.Stats;
import jade.core.*;
import jade.domain.*;
import jade.domain.FIPAAgentManagement.*;
import jade.domain.FIPANames.InteractionProtocol;
import jade.lang.acl.*;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;


/**
 * System agent that controls the GUI and loads initial configuration settings.
 * TODO: You have to decide the onthology and protocol when interacting among
 * the Coordinator agent.
 */
public class SystemAgent extends ImasAgent {

    private int initialSteps;
    private Stats stats;
    private int totalMetal;
    
    /**
     * GUI with the map, system agent log and statistics.
     */
    private GraphicInterface gui;
    /**
     * Game settings. At the very beginning, it will contain the loaded
     * initial configuration settings.
     */
    private InitialGameSettings game;
    
    /**
     * The Coordinator agent with which interacts sharing game settings every
     * round.
     */
    private AID coordinatorAgent;

    /**
     * Builds the System agent.
     */
    public SystemAgent() {
        super(AgentType.SYSTEM);
    }

    public Stats getStats() {
        return stats;
    }
    
    private void calculateTotalMetal() {
        totalMetal = 0;
        for(int i = 0; i < game.getMap().length; ++i) { 
            for(int j = 0; j < game.getMap()[i].length; ++j) { 
                if(game.getMap()[i][j] instanceof SettableFieldCell) {
                    Map<MetalType, Integer> metal = ((SettableFieldCell) game.getMap()[i][j]).getMetal();
                    if(metal.get(MetalType.SILVER) != null && metal.get(MetalType.SILVER) > 0) {
                        totalMetal += metal.get(MetalType.SILVER);
                    } else if(metal.get(MetalType.GOLD) != null && metal.get(MetalType.GOLD) > 0) {
                        totalMetal += metal.get(MetalType.GOLD);
                    }
                }
            }
        }
    }
    
    /**
     * A message is shown in the log area of the GUI, as well as in the
     * stantard output.
     *
     * @param log String to show
     */
    @Override
    public void log(String log) {
        if (gui != null) {
            gui.log(getLocalName()+ ": " + log + "\n");
        }
        super.log(log);
    }

    /**
     * An error message is shown in the log area of the GUI, as well as in the
     * error output.
     *
     * @param error Error to show
     */
    @Override
    public void errorLog(String error) {
        if (gui != null) {
            gui.log("ERROR: " + getLocalName()+ ": " + error + "\n");
        }
        super.errorLog(error);
    }

    /**
     * Gets the game settings.
     *
     * @return game settings.
     */
    public GameSettings getGame() {
        return this.game;
    }

    /**
     * Adds (if probability matches) new elements onto the map
     * for every simulation step.
     * This method is expected to be run from the corresponding Behaviour
     * to add new elements onto the map at each simulation step.
     */
    public void addElementsForThisSimulationStep() {
        this.game.addElementsForThisSimulationStep();
    }

    /**
     * Agent setup method - called when it first come on-line. Configuration of
     * language to use, ontology and initialization of behaviours.
     */
    @Override
    protected void setup() {        
        /* ** Very Important Line (VIL) ************************************* */
        this.setEnabledO2ACommunication(true, 1);

        // 1. Register the agent to the DF
        ServiceDescription sd1 = new ServiceDescription();
        sd1.setType(AgentType.SYSTEM.toString());
        sd1.setName(getLocalName());
        sd1.setOwnership(OWNER);

        DFAgentDescription dfd = new DFAgentDescription();
        dfd.addServices(sd1);
        dfd.setName(getAID());
        try {
            DFService.register(this, dfd);
            log("Registered to the DF");
        } catch (FIPAException e) {
            System.err.println(getLocalName() + " failed registration to DF [ko]. Reason: " + e.getMessage());
            doDelete();
        }

        // 2. Load game settings.
        this.game = InitialGameSettings.load("game.settings");
        log("Initial configuration settings loaded");

        // 3. Load GUI
        try {
            this.gui = new GraphicInterface(game);
            gui.setVisible(true);
            log("GUI loaded");
        } catch (Exception e) {
            e.printStackTrace();
        }

        // search CoordinatorAgent
        ServiceDescription searchCriterion = new ServiceDescription();
        searchCriterion.setType(AgentType.COORDINATOR.toString());
        this.coordinatorAgent = UtilsAgents.searchAgent(this, searchCriterion);
        // searchAgent is a blocking method, so we will obtain always a correct AID

        // Create Diggers, Prospectors, DiggerCoordinator and ProspectorCoordinator agents
        createNeededAgents();
        
        this.stats = new Stats();
        this.stats.setGame(game);
        calculateTotalMetal();
        
        // add behaviours
        // we wait for the initialization of the game
        MessageTemplate mt = MessageTemplate.and(MessageTemplate.MatchProtocol(InteractionProtocol.FIPA_REQUEST), MessageTemplate.MatchPerformative(ACLMessage.REQUEST));

        this.addBehaviour(new SystemRequestResponseBehaviour(this, mt));
        this.addBehaviour(new SystemMessageReceiver(this));       
        
        // Setup finished. When the last inform is received, the agent itself will add
        // a behaviour to send/receive actions
                
        List<Cell> cellsWithDiggers = game.getCellsWithDiggers();
        for(Cell cell: cellsWithDiggers){
            if(((PathCell)cell).getAgents().get(AgentType.DIGGER) != null){
                List<InfoAgent> agentsInCell = ((PathCell)cell).getAgents().get(AgentType.DIGGER);
                for(InfoAgent a : agentsInCell){
                    ((DiggerInfoAgent) a).setCapacity(game.getDiggersCapacity());
                }
            }
        }
        
        this.initialSteps = game.getSimulationSteps();
            
        simulateSteps(false);
    }
    
    public void simulateSteps(boolean updateGUI) { 
        if(game.getSimulationSteps() > 0) {
            if(updateGUI){
                updateGUI();
                this.addBehaviour(new InformNewStep(this, AgentType.COORDINATOR));
            } else {
                this.addBehaviour(new InformNewStep(this, AgentType.COORDINATOR));
            }
            
            game.setSimulationSteps(game.getSimulationSteps() - 1);
        } else {
            log("SIMULATION FINISHED");
        }
    }

    private void createNeededAgents(){
        UtilsAgents.createAgent(getContainerController(), "diggerCoordinator", "cat.urv.imas.agent.DiggerCoordinatorAgent", null);
        UtilsAgents.createAgent(getContainerController(), "prospectorCoordinator", "cat.urv.imas.agent.ProspectorCoordinatorAgent", null);

        UtilsAgents.createListOfAgents(getContainerController(), "digger", "cat.urv.imas.agent.DiggerAgent", getGame().getAgentList().get(AgentType.DIGGER).size(), null);
        UtilsAgents.createListOfAgents(getContainerController(), "prospector", "cat.urv.imas.agent.ProspectorAgent", getGame().getAgentList().get(AgentType.PROSPECTOR).size(), null);
    }
    
    public void updateGUI() {
        this.gui.updateGame(this.game.getMap());
    }
    
    public void updateStats(Stats newStats) {
        this.stats.setGame(newStats.getGame());
        
        this.stats.addPoints(newStats.getTotalPoints());
        this.stats.addGoldPoints(newStats.getGoldManufactured(), newStats.getGoldPoints());
        this.stats.addSilverPoints(newStats.getSilverManufactured(), newStats.getSilverPoints());
        
        for(Entry<Coordinates, Integer> cellsMined : newStats.getCellsMined().entrySet()) {
            this.stats.addCellMined(cellsMined.getKey(), cellsMined.getValue());
        }
        
        double stepsToMine = 0.0;
        for(Integer steps : stats.getCellsMined().values()) {
            stepsToMine += initialSteps - steps;
        }
        
        if(!stats.getCellsMined().isEmpty()){
            stepsToMine /= stats.getCellsMined().size();
        }
        
        this.gui.showStatistics("\n-- Step " 
                + String.valueOf(initialSteps - game.getSimulationSteps()) 
                + " --------------------------------");
        
        this.gui.showStatistics("Benefits: " + this.stats.getTotalPoints());
        
        this.gui.showStatistics("Gold manufactured: " + this.stats.getGoldManufactured());
        if(this.stats.getGoldManufactured() != 0.0) {
            this.gui.showStatistics("Avg. points per gold unit: " + this.stats.getGoldPoints() / this.stats.getGoldManufactured());
        }
        
        this.gui.showStatistics("Silver manufactured: " + this.stats.getSilverManufactured());
        if(this.stats.getSilverManufactured() != 0.0) {
            this.gui.showStatistics("Avg. points per silver unit: " + this.stats.getSilverPoints() / this.stats.getSilverManufactured());
        }
        
        if(stepsToMine != 0.0){
            this.gui.showStatistics("Avg. time for digging metal: " + stepsToMine + " steps");
        }
        
        double ratio = (this.stats.getGoldManufactured() + this.stats.getSilverManufactured()) / totalMetal * 100;
        this.gui.showStatistics("Ratio of collected metal: " + ratio + " %");
    }

    public void setGame(GameSettings game) {
        this.game = (InitialGameSettings) game;
    }

}
