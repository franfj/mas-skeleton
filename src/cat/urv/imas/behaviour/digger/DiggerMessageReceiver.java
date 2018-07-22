/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package cat.urv.imas.behaviour.digger;

import cat.urv.imas.agent.DiggerAgent;
import cat.urv.imas.map.Cell;
import cat.urv.imas.onthology.GameSettings;
import cat.urv.imas.tools.Work;
import jade.core.AID;
import jade.core.behaviours.CyclicBehaviour;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.UnreadableException;
import java.util.HashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * This class controls the messages sended to digger agents.
 */
public class DiggerMessageReceiver extends CyclicBehaviour {

    /**
     * Constructor.
     * @param a The digger agent
     */
    public DiggerMessageReceiver(DiggerAgent a) {
        super(a);
    }
    
    private void newStepAction(DiggerAgent agent, ACLMessage msg) {
        agent.log("INFORM received: " + msg.getContent());
        agent.requestMap();
    }
    
    private void getMapAction(DiggerAgent agent, ACLMessage msg){
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
        } catch (Exception e) {
            reply.setPerformative(ACLMessage.FAILURE);
            agent.errorLog(e.toString());
        }
    }
    
    @Override
    public void action() {
        ACLMessage msg = this.getAgent().receive();
        
        if (msg != null && msg.getContent() != null){
            DiggerAgent agent = (DiggerAgent) this.getAgent();
            
            if (msg.getPerformative() == 0 && msg.getContent().equals("yes")){
                agent.log("I got accepted!! :)");
                agent.setWorking(true);
                agent.setGoalCell(agent.getProposedCell());
                                
                agent.setWork(new Work(agent.getAID(), agent.getProposedCell(), agent.getProposedCell()));
                agent.log("I will follow the following path: " + agent.getPathToFollow());
                agent.work();
            }
            
            else if (msg.getPerformative() == 15 && msg.getContent().equals("no")){
                agent.log("I got rejected!! :(");
                if(!agent.isWorking()) {
                    agent.setPathToFollow(null);
                }
                agent.setWork(new Work(agent.getAID(), null, null));
                agent.work();
            }
            
            else if (msg.getProtocol() != null && msg.getProtocol().equals("fipa-contract-net")) {
                try {                    
                    HashMap<String, Cell> cellReceived = (HashMap<String, Cell>) msg.getContentObject();
                    
                    if(cellReceived.get("get offer") != null) {
                        agent.setProposedCell(cellReceived.get("get offer"));
                        int distanceCalculated = agent.calculateDistanceToProposedCell();
                        agent.log("Sending contract net offer (" + distanceCalculated + ")");

                        ACLMessage offer = msg.createReply();
                        offer.setPerformative(ACLMessage.PROPOSE);
                        offer.setContent(String.valueOf(distanceCalculated));
                        
                        this.getAgent().send(offer);
                    } else {
                        ACLMessage offer = msg.createReply();
                        offer.setPerformative(ACLMessage.PROPOSE);
                        offer.setContent(String.valueOf(Integer.MAX_VALUE));
                        
                        this.getAgent().send(offer);
                    }
                    
                } catch (UnreadableException ex) {
                    Logger.getLogger(DiggerMessageReceiver.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
            else if (msg.getContent().equals("new step")) {
                newStepAction(agent, msg);
            }
            else if (msg.getContent().equals("Get map")) {
                getMapAction(agent, msg);
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

}
