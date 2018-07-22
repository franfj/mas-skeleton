package cat.urv.imas.behaviour.system;

import cat.urv.imas.agent.SystemAgent;
import cat.urv.imas.tools.Stats;
import jade.core.AID;
import jade.core.behaviours.CyclicBehaviour;
import jade.lang.acl.ACLMessage;

/**
 *
 */
public class SystemMessageReceiver extends CyclicBehaviour {

    public SystemMessageReceiver(SystemAgent a) {
        super(a);
    }
    
    private void getMapAction(SystemAgent agent, ACLMessage msg){
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
    
    @Override
    public void action() {
        ACLMessage msg = this.getAgent().receive();
        
        if (msg != null && msg.getContent() != null){
            SystemAgent agent = (SystemAgent) this.getAgent();
            
            if (msg.getContent().equals("Get map")) {
                getMapAction(agent, msg);
            } else { 
                agent.log("INFORM received from " + ((AID) msg.getSender()).getLocalName());
                try {
                    Stats stats = (Stats) msg.getContentObject();
                    agent.setGame(stats.getGame());
                    agent.updateStats(stats);
                    agent.log(stats.getGame().getShortString());

                    agent.log("Game received!");
                } catch (Exception e) {
                    agent.errorLog("Incorrect content: " + e.toString());
                }
                agent.simulateSteps(true);
            }
        }
    }
}

