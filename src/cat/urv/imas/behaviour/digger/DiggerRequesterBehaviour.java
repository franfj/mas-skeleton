package cat.urv.imas.behaviour.digger;

import cat.urv.imas.agent.DiggerAgent;
import jade.core.AID;
import jade.lang.acl.ACLMessage;
import jade.proto.AchieveREInitiator;
import cat.urv.imas.onthology.GameSettings;

/**
 * Behaviour for the Coordinator agent to deal with AGREE messages.
 * The Coordinator Agent sends a REQUEST for the
 * information of the game settings. The System Agent sends an AGREE and 
 * then it informs of this information which is stored by the Coordinator Agent. 
 * 
 * NOTE: The game is processed by another behaviour that we add after the 
 * INFORM has been processed.
 */
public class DiggerRequesterBehaviour extends AchieveREInitiator {

    public DiggerRequesterBehaviour(DiggerAgent agent, ACLMessage requestMsg) {
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
        DiggerAgent agent = (DiggerAgent) this.getAgent();
        agent.log("AGREE received from " + ((AID) msg.getSender()).getLocalName());
    }

    /**
     * Handle INFORM messages
     *
     * @param msg Message
     */
    @Override
    protected void handleInform(ACLMessage msg) {
//        ACLMessage msg = this.getAgent().receive();
        
        if (msg != null && msg.getContent() != null){
            DiggerAgent agent = (DiggerAgent) this.getAgent();
            
            if (msg.getContent().equals("new step")) {
                agent.log("INFORM received: " + msg.getContent());
                agent.requestMap();
            }
            else {
                agent.log("INFORM received from " + ((AID) msg.getSender()).getLocalName());
                try {
                    GameSettings game = (GameSettings) msg.getContentObject();
                    agent.setGame(game);
                    agent.log(game.getShortString());

                    agent.log("Game received!");
                    
                } catch (Exception e) {
                    agent.errorLog("Incorrect content: " + e.toString());
                }
            }
        }
    }

    /**
     * Handle NOT-UNDERSTOOD messages
     *
     * @param msg Message
     */
    @Override
    protected void handleNotUnderstood(ACLMessage msg) {
        DiggerAgent agent = (DiggerAgent) this.getAgent();
        agent.log("This message NOT UNDERSTOOD.");
    }

    /**
     * Handle FAILURE messages
     *
     * @param msg Message
     */
    @Override
    protected void handleFailure(ACLMessage msg) {
        DiggerAgent agent = (DiggerAgent) this.getAgent();
        agent.log("The action has failed.");

    } //End of handleFailure

    /**
     * Handle REFUSE messages
     *
     * @param msg Message
     */
    @Override
    protected void handleRefuse(ACLMessage msg) {
        DiggerAgent agent = (DiggerAgent) this.getAgent();
        agent.log("Action refused.");
    }

}
