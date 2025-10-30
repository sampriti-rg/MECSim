package applications.bqprofit.algorithm;

import dag.TaskNode;

public class TaskCost {
    TaskNode taskNode;
    Double latency;
    Double energy;
    public TaskNode getTaskNode() {
        return taskNode;
    }

    public void setTaskNode(TaskNode taskNode) {
        this.taskNode = taskNode;
    }

    public Double getLatency() {
        return latency;
    }

    public void setLatency(Double latency) {
        this.latency = latency;
    }

    public Double getEnergy() {
        return energy;
    }

    public void setEnergy(Double energy) {
        this.energy = energy;
    }

}