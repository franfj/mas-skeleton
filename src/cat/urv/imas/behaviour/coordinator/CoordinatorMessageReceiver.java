package cat.urv.imas.behaviour.coordinator;

import cat.urv.imas.agent.CoordinatorAgent;
import cat.urv.imas.tools.Stats;
import jade.core.AID;
import jade.core.behaviours.CyclicBehaviour;
import jade.lang.acl.ACLMessage;

/**
 * This class controls the message sended to the Coodinator agent.
 */
public class CoordinatorMessageReceiver extends CyclicBehaviour {

    /**
     * Contructor
     * @param a The coordinator agent
     */
    public CoordinatorMessageReceiver(CoordinatorAgent a) {
        super(a);
    }
    
    /**
     * Controls the new step action.
     * @param agent The coordinator agent
     * @param msg The message
     */
    private void newStepAction(CoordinatorAgent agent, ACLMessage msg) {
        agent.log("INFORM received: " + msg.getContent());
        agent.requestMap();
    }
    
    /**
     * Controls the get map action.
     * @param agent The coordinator agent
     * @param msg The message
     */
    private void getMapAction(CoordinatorAgent agent, ACLMessage msg){
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
            agent.getStats().setGame(agent.getGame());
            reply.setContentObject(agent.getStats());
            agent.send(reply);
            
            agent.log("Game settings sent");
        } catch (Exception e) {
            reply.setPerformative(ACLMessage.FAILURE);
            agent.errorLog(e.toString());
        }
    }
    
    /**
     * Controls several actions.
     */
    @Override
    public void action() {
        ACLMessage msg = this.getAgent().receive();
        
        if (msg != null && msg.getContent() != null){
            CoordinatorAgent agent = (CoordinatorAgent) this.getAgent();
            
            if (msg.getContent().equals("new step")) {
                newStepAction(agent, msg);
                
            }
            else if (msg.getContent().equals("Get map")) {
                getMapAction(agent, msg);
                
            }
            else {
                agent.log("INFORM received from " + ((AID) msg.getSender()).getLocalName());
                try {
                    Stats stats = (Stats) msg.getContentObject();
                    agent.setGame(stats.getGame());
                    agent.setStats(stats);
                    agent.log(stats.getGame().getShortString());
                    
                    agent.log("Game received!");

                    if (((AID) msg.getSender()).getLocalName().equals("diggerCoordinator")){
                        agent.propagateNewMap();
                    } else { 
                        agent.simulateSteps();
                    }
                    
                } catch (Exception e) {
                    agent.errorLog("Incorrect content: " + e.toString());
                }
            }
        }
    }

}
