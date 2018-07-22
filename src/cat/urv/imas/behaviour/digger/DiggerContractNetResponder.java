package cat.urv.imas.behaviour.digger;

import cat.urv.imas.agent.DiggerAgent;
import jade.core.Agent;
import jade.domain.FIPAAgentManagement.FailureException;
import jade.domain.FIPAAgentManagement.NotUnderstoodException;
import jade.domain.FIPAAgentManagement.RefuseException;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import jade.proto.ContractNetResponder;
import java.util.Random;

/**
 * Class for Contract Net protocol in the Digger side.
 */
public class DiggerContractNetResponder extends ContractNetResponder {
    
    /**
     * Constructor.
     * @param agent The digger agent
     * @param msg The message
     */
    public DiggerContractNetResponder(Agent agent, MessageTemplate msg) {
        super(agent, msg);
    }

    @Override
    protected ACLMessage prepareResponse(ACLMessage msg) throws NotUnderstoodException, RefuseException {
        ACLMessage offer = msg.createReply();
        offer.setPerformative(ACLMessage.PROPOSE);
        offer.setContent(String.valueOf(new Random().nextInt() % 10));
        return offer;
    }

    @Override
    protected ACLMessage prepareResultNotification(ACLMessage msg, ACLMessage propose, ACLMessage accept) throws FailureException {
        ACLMessage inform = accept.createReply();
        inform.setPerformative(ACLMessage.INFORM);
        return inform;
    }

    @Override
    protected void handleRejectProposal(ACLMessage msg, ACLMessage propose, ACLMessage reject) {
        ((DiggerAgent) this.getAgent()).log("We got rejected. :(");
    }
}
