package cat.urv.imas.behaviour.diggercoordinator;

import cat.urv.imas.agent.DiggerCoordinatorAgent;
import cat.urv.imas.onthology.GameSettings;
import cat.urv.imas.tools.Stats;
import cat.urv.imas.tools.Work;
import jade.core.AID;
import jade.core.behaviours.CyclicBehaviour;
import jade.lang.acl.ACLMessage;

/**
 *
 * @author Fran
 */
public class DiggerCoordinatorMessageReceiver extends CyclicBehaviour {

    public DiggerCoordinatorMessageReceiver(DiggerCoordinatorAgent a) {
        super(a);
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
            agent.prepareMapForDiggers();
            reply.setContentObject(agent.getGame());
            agent.send(reply);
            
            ((DiggerCoordinatorAgent)this.getAgent()).startContractNetProtocol();
            
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
            DiggerCoordinatorAgent agent = (DiggerCoordinatorAgent) this.getAgent();
            
            if(msg.getProtocol() != null && msg.getProtocol().equals("fipa-contract-net")){
                agent.getProposals().add(msg);
                String out = "Offer received from: " + msg.getSender().getLocalName() + 
                        " (" + msg.getContent() + ")";
                agent.log(out);
            }
            else if (msg.getContent().equals("new step")) {
                newStepAction(agent, msg);
            }
            else if (msg.getContent().equals("Get map")) {
                getMapAction(agent, msg);
            }
            else if (msg.getSender().getLocalName().length() > 5  && msg.getSender().getLocalName().substring(0, 6).equals("digger")) {
                agent.log("Work received from " + ((AID) msg.getSender()).getLocalName());
                
                try {
                    Work work = (Work) msg.getContentObject();
                    agent.addJob(work);
                } catch (Exception e) {
                    agent.errorLog("Incorrect content: " + e.toString());
                }
                
                agent.executeJobs();
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
                    
                } catch (Exception e) {
                    agent.errorLog("Incorrect content: " + e.toString());
                }
                
                agent.log("Game received!");
                agent.simulateSteps();
            }
        }
    }

}
