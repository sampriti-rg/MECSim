package applications.rmeac;

import dag.TaskNode;

public class OffloadingDecision {
    private int dcIndex;
    private int serverIndex;
    private int decision;//0-edge, 1-UE, 2-cloud
    private TaskNode taskNode;

    public OffloadingDecision(TaskNode taskNode, int dc_index, int server_index, int decision) {
        this.taskNode = taskNode;
        this.dcIndex = dc_index;
        this.serverIndex = server_index;
        this.decision = decision;
        taskNode.setOffloadingDecision(this);
    }
    public int getServerIndex() {
        return serverIndex;
    }

    public int getDcIndex() {
        return dcIndex;
    }

    public int getDecision() {
        return decision;
    }

    public void setDecision(int decision) {
        this.decision = decision;
    }
    public TaskNode getTaskNode() {
        return taskNode;
    }
}
