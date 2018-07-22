package cat.urv.imas.agent;

import static cat.urv.imas.agent.ImasAgent.OWNER;
import cat.urv.imas.onthology.GameSettings;
import jade.core.*;
import jade.domain.*;
import jade.domain.FIPAAgentManagement.*;

/**
 * The ProspectorCoordinator agent. 
 * TODO: All
 */
public class ProspectorCoordinatorAgent extends ImasAgent {

    /**
     * Game settings in use.
     */
    private GameSettings game;
    
    /**
     * System agent id.
     */
    private AID systemAgent;

    /**
     * Builds the prospectorcoordinator agent.
     */
    public ProspectorCoordinatorAgent() {
        super(AgentType.PROSPECTOR_COORDINATOR);
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
        sd1.setType(AgentType.PROSPECTOR_COORDINATOR.toString());
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
        searchCriterion.setType(AgentType.PROSPECTOR_COORDINATOR.toString());
        this.systemAgent = UtilsAgents.searchAgent(this, searchCriterion);
        // searchAgent is a blocking method, so we will obtain always a correct AID

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

}
