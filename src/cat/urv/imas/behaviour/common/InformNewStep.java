package cat.urv.imas.behaviour.common;

import cat.urv.imas.agent.AgentType;
import cat.urv.imas.agent.ImasAgent;
import cat.urv.imas.agent.UtilsAgents;
import jade.core.AID;
import jade.core.Agent;
import jade.core.behaviours.SimpleBehaviour;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.lang.acl.ACLMessage;
import java.util.List;

/**
 * Class used for sending INFORM messages with content: "new step".
 */
public class InformNewStep extends SimpleBehaviour {
        
    /**
     * Receiber agent type.
     */
    private final AgentType receiverType;
    
    /**
     * Constructor.
     * @param agent Sender agent.
     * @param receiver Receiver type.
     */
    public InformNewStep(Agent agent, AgentType receiver) {
        super(agent);
        
        this.receiverType = receiver;
    }
        
    /**
     * Message sending.
     */
    @Override
    public void action() {
        ACLMessage msg = new ACLMessage(ACLMessage.INFORM);

        ServiceDescription searchCriterion = new ServiceDescription();
        searchCriterion.setType(receiverType.toString());
        
        if(this.receiverType != AgentType.DIGGER){
            AID receiverAgent = UtilsAgents.searchAgent(this.getAgent(), searchCriterion);

            msg.setSender(getAgent().getAID());
            msg.addReceiver(receiverAgent);

            msg.setContent("new step");

            ((ImasAgent) this.getAgent()).log("INFORM sent to " + receiverType.toString() + ": new step");
            this.getAgent().send(msg);
            
        } else {
            List<AID> receiverAgents = UtilsAgents.searchMultipleAgents(this.getAgent(), searchCriterion);
            msg.setSender(getAgent().getAID());
            msg.setContent("new step");
            
            for(AID receiverAgent : receiverAgents){
                msg.addReceiver(receiverAgent);

            }
                ((ImasAgent) this.getAgent()).log("INFORM sent to all " + receiverType.toString() + ": new step");
                this.getAgent().send(msg);
        }
    }

    @Override
    public boolean done() {
        return true;
    }
     
}
