package dag;

import applications.bqprofit.ChargingPlan;
import applications.rmeac.OffloadingDecision;
import com.mechalikh.pureedgesim.datacentersmanager.ComputingNode;
import com.mechalikh.pureedgesim.taskgenerator.Task;
import common.Job;

import java.util.ArrayList;
import java.util.List;


public class TaskNode extends Task implements Cloneable{
    @Override
    public TaskNode clone() {
        try {
            TaskNode clone = (TaskNode) super.clone();
            // TODO: copy mutable state here, so the clone can't change the internals of the original
            return clone;
        } catch (CloneNotSupportedException e) {
            throw new AssertionError();
        }
    }

    public enum TaskType {
        NONE,
        NORMAL,
        IO_INTENSIVE,
        CPU_INTENSIVE,
        MEM_INTENSIVE
    };

    public enum TaskDecision{
        UE_ONLY,
        MEC_ONLY,
        CLOUD,
        OPEN,
        UNKNOWN
    };

    public enum MecSchedulingDecision{
        NEAREST,
        NEIGHBOUR,
        CLOUD
    };
    public List<TaskNode>   predecessors;
    public List<TaskNode>   successors;
    List<Integer> predecessorsId;
    List<Integer> successorsId;
    private boolean taskDone;
    private Integer level;
    private boolean startTask;
    private boolean endTask;
    private TaskType taskType;
    private boolean isDummyTask;
    private String deadlineType;
    private boolean isPeakTimeOffload;
    private Job job;
    private ChargingPlan chargingPlan;
    private ComputingNode selectedComputingNode;
    private ComputingNode computingNode = ComputingNode.NULL;
    private Double budget;
    private double addionalTime;
    boolean lowReliable;
    boolean copiedTask;
    String umi;
    boolean primary = true;
    private double localCost;
    private double remoteCost;
    private boolean isCritical;
    double startTime;
    double endTime;
    TaskNode replica;
    private double actualExTime;
    private Double subDeadline;
    private double executionTime;
    private Integer rank;
    private TaskDecision taskDecision;
    double priorityScore;
    ComputingNode executionNode;
    OffloadingDecision offloadingDecision;

    public TaskNode(int id, long length){
        super(id, length);
        predecessors = new ArrayList<>();
        successors = new ArrayList<>();
        successorsId = new ArrayList<>();
        predecessorsId = new ArrayList<>();
        taskDone = false;
        level = 0;
        startTask = false;
        endTask = false;
        taskType = TaskType.NORMAL;
        isDummyTask = false;
        taskDecision = TaskDecision.OPEN;
    }

    public double getActualExTime() {
        return actualExTime;
    }
    public void setActualExTime(double actualExTime) {
        this.actualExTime = actualExTime;
    }
    public Double getSubDeadline() {
        return subDeadline;
    }
    public void setSubDeadline(Double subDeadline) {
        this.subDeadline = subDeadline;
    }
    public double getExecutionTime() {
        return executionTime;
    }
    public void setExecutionTime(double executionTime) {
        this.executionTime = executionTime;
    }
    public Integer getRank() {
        return rank;
    }
    public void setRank(Integer rank) {
        this.rank = rank;
    }
    public TaskDecision getTaskDecision() {
        return taskDecision;
    }
    public void setTaskDecision(TaskDecision taskDecision) {
        this.taskDecision = taskDecision;
    }
    public boolean isDummyTask() {
        return isDummyTask;
    }
    public void setDummyTask(boolean dummyTask) {
        isDummyTask = dummyTask;
    }
    public TaskType getTaskType() {
        return taskType;
    }
    public void setTaskType(TaskType taskType) {
        this.taskType = taskType;
    }
    public Integer getLevel() {
        return level;
    }
    public void setLevel(Integer level) {
        this.level = level;
    }
    public boolean isStartTask() {
        return startTask;
    }
    public void setStartTask(boolean startTask) {
        this.startTask = startTask;
    }
    public boolean isEndTask() {
        return endTask;
    }
    public void setEndTask(boolean endTask) {
        this.endTask = endTask;
    }
    public boolean isTaskDone() {
        return taskDone;
    }
    public void setTaskDone(boolean taskDone) {
        this.taskDone = taskDone;
    }
    public List<Integer> getPredecessorsId() {
        return predecessorsId;
    }
    public void setPredecessorsId(List<Integer> predecessorsId) {
        this.predecessorsId = predecessorsId;
    }
    public List<Integer> getSuccessorsId() {
        return successorsId;
    }
    public void setSuccessorsId(List<Integer> successorsId) {
        this.successorsId = successorsId;
    }
    public String getDeadlineType() {
        return deadlineType;
    }
    public void setDeadlineType(String deadlineType) {
        this.deadlineType = deadlineType;
    }
    public boolean isPeakTimeOffload() {
        return isPeakTimeOffload;
    }

    public void setPeakTimeOffload(boolean peakTimeOffload) {
        isPeakTimeOffload = peakTimeOffload;
    }
    public Job getJob() {
        return job;
    }
    public void setJob(Job job) {
        this.job = job;
    }
    public ChargingPlan getChargingPlan() {
        return chargingPlan;
    }
    public void setChargingPlan(ChargingPlan chargingPlan) {
        this.chargingPlan = chargingPlan;
    }
    public ComputingNode getSelectedComputingNode() {
        return selectedComputingNode;
    }
    public void setSelectedComputingNode(ComputingNode selectedComputingNode) {
        this.selectedComputingNode = selectedComputingNode;
    }
    public ComputingNode getOffloadingDestination() {
        return computingNode;
    }
    public void setComputingNode(ComputingNode applicationPlacementLocation) {
        this.computingNode = applicationPlacementLocation;
    }
    public ComputingNode getComputingNode(){
        return this.computingNode;
    }
    public Double getBudget() {
        return budget;
    }
    public void setBudget(Double budget) {
        this.budget = budget;
    }
    public boolean isPrimary() {
        return primary;
    }
    public void setPrimary(boolean primary) {
        this.primary = primary;
    }
    public double getAddionalTime() {
        return addionalTime;
    }
    public void setAddionalTime(double addionalTime) {
        this.addionalTime = addionalTime;
    }
    public boolean isLowReliable() {
        return lowReliable;
    }
    public void setLowReliable(boolean lowReliable) {
        this.lowReliable = lowReliable;
    }
    public boolean isCopiedTask() {
        return copiedTask;
    }
    public void setCopiedTask(boolean copiedTask) {
        this.copiedTask = copiedTask;
    }
    public String getUmi() {
        return umi;
    }
    public void setUmi(String umi) {
        this.umi = umi;
    }
    public double getLocalCost() {
        return localCost;
    }
    public void setLocalCost(double localCost) {
        this.localCost = localCost;
    }
    public double getRemoteCost() {
        return remoteCost;
    }
    public void setRemoteCost(double remoteCost) {
        this.remoteCost = remoteCost;
    }
    public boolean isCritical() {
        return isCritical;
    }
    public void setCritical(boolean critical) {
        isCritical = critical;
    }
    public double getEndTime() {
        return endTime;
    }
    public void setEndTime(double endTime) {
        this.endTime = endTime;
    }
    public double getStartTime() {
        return startTime;
    }
    public void setStartTime(double startTime) {
        this.startTime = startTime;
    }
    public TaskNode getReplica() {
        return replica;
    }
    public void setReplica(TaskNode replica) {
        this.replica = replica;
    }
    public double getPriorityScore() {
        return priorityScore;
    }

    public void setPriorityScore(double priorityScore) {
        this.priorityScore = priorityScore;
    }
    public ComputingNode getExecutionNode() {
        return executionNode;
    }

    public void setExecutionNode(ComputingNode executionNode) {
        this.executionNode = executionNode;
    }
    public OffloadingDecision getOffloadingDecision() {
        return offloadingDecision;
    }

    public void setOffloadingDecision(OffloadingDecision offloadingDecision) {
        this.offloadingDecision = offloadingDecision;
    }
}
