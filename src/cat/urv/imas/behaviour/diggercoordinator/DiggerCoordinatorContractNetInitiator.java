/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package cat.urv.imas.behaviour.diggercoordinator;

import cat.urv.imas.agent.DiggerCoordinatorAgent;
import jade.core.AID;
import jade.core.Agent;
import jade.lang.acl.ACLMessage;
import jade.proto.ContractNetInitiator;
import java.util.Vector;

/**
 * Class for Contract Net protocol in DiggerCoordinator side.
 */
public class DiggerCoordinatorContractNetInitiator extends ContractNetInitiator {
 
    public DiggerCoordinatorContractNetInitiator(Agent agent, ACLMessage msg) {
        super(agent, msg);
    }

    @Override
    protected void handlePropose(ACLMessage msg, Vector acceptances) {
        ((DiggerCoordinatorAgent) this.getAgent()).getProposals().add(msg);
        String out = "Offer received from: " + msg.getSender().getLocalName() + 
                " (" + msg.getContent() + ")";

        ((DiggerCoordinatorAgent)this.getAgent()).log(out);
    }

    @Override
    protected void handleRefuse(ACLMessage msg) {
        // Nothing to do
    }

    @Override
    protected void handleFailure(ACLMessage msg) {
        // Nothing to do
    }

    @Override
    protected void handleAllResponses(Vector responses, Vector acceptances) {
        DiggerCoordinatorAgent agent = ((DiggerCoordinatorAgent) this.getAgent());
        
        int bestOffer = 9999;
        AID bestOfferAID = null;
        ACLMessage accepted = null;

        for (Object resp : ((DiggerCoordinatorAgent) this.getAgent()).getProposals()) {
            ACLMessage msg = (ACLMessage) resp;

            if (msg.getPerformative() == ACLMessage.PROPOSE) {
                ACLMessage reply = msg.createReply();
                
                reply.setPerformative(ACLMessage.REJECT_PROPOSAL);
                reply.setContent("no");
                reply.setSender(this.getAgent().getAID());
                
                acceptances.add(reply);

                int offer = Integer.parseInt(msg.getContent());
                if (offer < bestOffer) {
                    bestOffer = offer;
                    bestOfferAID = msg.getSender();
                    accepted = reply;
                }
            }
        }
                
        agent.getProposals().clear();
        if (accepted != null) {
            agent.log("Job assigned to " + bestOfferAID.getLocalName() + " (" + bestOffer + ")");
            accepted.setPerformative(ACLMessage.ACCEPT_PROPOSAL);
            accepted.setContent("yes");
        }
    }

    @Override
    protected void handleInform(ACLMessage msg) {
        // Nothing to do
    }
}