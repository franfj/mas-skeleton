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
import cat.urv.imas.behaviour.coordinator.CoordinatorMessageReceiver;
import cat.urv.imas.onthology.GameSettings;
import cat.urv.imas.behaviour.coordinator.CoordinatorRequesterBehaviour;
import cat.urv.imas.onthology.MessageContent;
import cat.urv.imas.tools.Stats;
import jade.core.*;
import jade.domain.*;
import jade.domain.FIPAAgentManagement.*;
import jade.domain.FIPANames.InteractionProtocol;
import jade.lang.acl.*;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * The main Coordinator agent. 
 * TODO: This coordinator agent should get the game settings from the System
 * agent every round and share the necessary information to other coordinators.
 */
public class CoordinatorAgent extends ImasAgent {

    private Stats stats;
    
    /**
     * Game settings in use.
     */
    private GameSettings game;
    /**
     * System agent id.
     */
    private AID systemAgent;

    /**
     * Builds the coordinator agent.
     */
    public CoordinatorAgent() {
        super(AgentType.COORDINATOR);
    }

    public Stats getStats() {
        return stats;
    }

    public void setStats(Stats stats) {
        this.stats = stats;
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
        sd1.setType(AgentType.COORDINATOR.toString());
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
        // searchAgent is a blocking method, so we will obtain always a correct AID

        this.addBehaviour(new CoordinatorMessageReceiver(this));

        // setup finished. When we receive the last inform, the agent itself will add
        // a behaviour to send/receive actions
    }
    
    public void requestMap() {
        ACLMessage initialRequest = new ACLMessage(ACLMessage.REQUEST);
        initialRequest.clearAllReceiver();
        initialRequest.addReceiver(this.systemAgent);
        initialRequest.setProtocol(InteractionProtocol.FIPA_REQUEST);
        log("Request message to agent");
        try {
            initialRequest.setContent(MessageContent.GET_MAP);
            log("Request message content:" + initialRequest.getContent());
            this.addBehaviour(new CoordinatorRequesterBehaviour(this, initialRequest));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void simulateSteps() { 
        this.addBehaviour(new InformNewStep(this, AgentType.DIGGER_COORDINATOR));
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

    public void propagateNewMap() {
        ACLMessage msg = new ACLMessage(ACLMessage.INFORM);
        msg.setSender(this.getAID());
        msg.addReceiver(systemAgent);

        try {
            msg.setContentObject(this.stats);
        } catch (IOException ex) {
            Logger.getLogger(DiggerAgent.class.getName()).log(Level.SEVERE, null, ex);
        }

        log("Sending new map to " + systemAgent.getLocalName());
        send(msg);
    }

}
