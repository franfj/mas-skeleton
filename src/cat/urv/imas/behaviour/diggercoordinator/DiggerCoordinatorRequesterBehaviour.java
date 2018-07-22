package cat.urv.imas.behaviour.diggercoordinator;

import jade.core.AID;
import jade.lang.acl.ACLMessage;
import jade.proto.AchieveREInitiator;
import cat.urv.imas.agent.DiggerCoordinatorAgent;
import cat.urv.imas.onthology.GameSettings;
import cat.urv.imas.tools.Stats;

/**
 * Behaviour for the Coordinator agent to deal with AGREE messages.
 * The Coordinator Agent sends a REQUEST for the
 * information of the game settings. The System Agent sends an AGREE and 
 * then it informs of this information which is stored by the Coordinator Agent. 
 * 
 * NOTE: The game is processed by another behaviour that we add after the 
 * INFORM has been processed.
 */
public class DiggerCoordinatorRequesterBehaviour extends AchieveREInitiator {

    public DiggerCoordinatorRequesterBehaviour(DiggerCoordinatorAgent agent, ACLMessage requestMsg) {
        super(agent, requestMsg);
        agent.log("Started behaviour to deal with AGREEs");
    }

    /**
     * Handle AGREE messages
     *
     * @param msg Message to handle
     */
    @Override
    protected void handleAgree(ACLMessage msg) {
        DiggerCoordinatorAgent agent = (DiggerCoordinatorAgent) this.getAgent();
        agent.log("AGREE received from " + ((AID) msg.getSender()).getLocalName());
    }

    /**
     * Handle INFORM messages
     *
     * @param msg Message
     */
    @Override
    protected void handleInform(ACLMessage msg) {        
        if (msg != null && msg.getContent() != null){
            DiggerCoordinatorAgent agent = (DiggerCoordinatorAgent) this.getAgent();
            
            if (msg.getContent().equals("new step")) {
                newStepAction(agent, msg);
            }
            
            else if (msg.getContent().equals("Get map")) {
                getMapAction(agent, msg);
            }
            
            else {
                agent.log("INFORM received from " + ((AID) msg.getSender()).getLocalName());
                try {
                    if(!msg.getSender().equals(agent.getCoordinatorAgent())){
                        GameSettings game = (GameSettings) msg.getContentObject();
                        agent.setGame(game);
                        agent.log(game.getShortString());
                    } else {
                        Stats stats = (Stats) msg.getContentObject();
                        agent.setStats(stats);
                        agent.setGame(stats.getGame());
                        agent.log(stats.getGame().getShortString());
                    }

                    agent.log("Game received!");
                    
                    agent.simulateSteps();
                    
                } catch (Exception e) {
                    agent.errorLog("Incorrect content: " + e.toString());
                }
            }
        }
    }
    
    private void newStepAction(DiggerCoordinatorAgent agent, ACLMessage msg) {
        agent.log("INFORM received: " + msg.getContent());
        agent.requestMap();
    }
    
    private void getMapAction(DiggerCoordinatorAgent agent, ACLMessage msg){
        ACLMessage reply = msg.createReply();
        try {
            Object content = (Object) msg.getContent();
            agent.log("Request received");
            reply.setPerformative(ACLMessage.AGREE);
            
        } catch (Exception e) {
            reply.setPerformative(ACLMessage.FAILURE);
            agent.errorLog(e.getMessage());
        }
        agent.log("Response being prepared");
        
        reply = msg.createReply();
        reply.setPerformative(ACLMessage.INFORM);

        try {
            reply.setContentObject(agent.getGame());
            agent.send(reply);
            
            agent.log("Game settings sent");
            
            ((DiggerCoordinatorAgent)this.getAgent()).startContractNetProtocol();
        } catch (Exception e) {
            reply.setPerformative(ACLMessage.FAILURE);
            agent.errorLog(e.toString());
        }
    }

    /**
     * Handle NOT-UNDERSTOOD messages
     *
     * @param msg Message
     */
    @Override
    protected void handleNotUnderstood(ACLMessage msg) {
        DiggerCoordinatorAgent agent = (DiggerCoordinatorAgent) this.getAgent();
        agent.log("This message NOT UNDERSTOOD.");
    }

    /**
     * Handle FAILURE messages
     *
     * @param msg Message
     */
    @Override
    protected void handleFailure(ACLMessage msg) {
        DiggerCoordinatorAgent agent = (DiggerCoordinatorAgent) this.getAgent();
        agent.log("The action has failed.");
    } 

    /**
     * Handle REFUSE messages
     *
     * @param msg Message
     */
    @Override
    protected void handleRefuse(ACLMessage msg) {
        DiggerCoordinatorAgent agent = (DiggerCoordinatorAgent) this.getAgent();
        agent.log("Action refused.");
    }

}
