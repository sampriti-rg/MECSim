package applications.rmeac;

import com.mechalikh.pureedgesim.datacentersmanager.ComputingNode;
import com.mechalikh.pureedgesim.simulationmanager.SimLog;
import com.mechalikh.pureedgesim.simulationmanager.SimulationManager;
import com.mechalikh.pureedgesim.taskgenerator.Task;
import common.Helper;
import common.Job;
import common.TaskReliability;
import dag.TaskNode;

public class RmeacScheduling {
    static double RELIABLE_THRESHOLD = 0.01;
    SimulationManager simManager;
    public RmeacScheduling(SimulationManager simulationManager){
        this.simManager = simulationManager;
    }

    public void Rmeac(Task task){
        TaskNode taskNode = (TaskNode) task;
        ComputingNode computingNode = null;
        Job job = taskNode.getJob();
        double rel = TaskReliability.getReliability(taskNode);
        if(rel < RELIABLE_THRESHOLD){
            taskNode.setLowReliable(true);
        }

        if(taskNode.isLowReliable()){
            SimLog.totalLowRealiabilityTasks++;
        }
        if(taskNode.isStartTask() || taskNode.isEndTask()){
            taskNode.setTaskDecision(TaskNode.TaskDecision.UE_ONLY);
            taskNode.setExecutionNode(taskNode.getEdgeDevice());
            return;
        }

        double budgetNeed = getRequiredBudget(taskNode);
        double remaining_budget = job.getMaxBudget() - job.getUsedBudget();
        if(budgetNeed > remaining_budget){
            taskNode.setTaskDecision(TaskNode.TaskDecision.UE_ONLY);
            taskNode.setExecutionNode(taskNode.getEdgeDevice());
            taskNode.getOffloadingDecision().setDecision(1);
            return;
        }

        OffloadingDecision offloadingDecision = taskNode.getOffloadingDecision();
        if (offloadingDecision.getDecision() == 1) {
            taskNode.setTaskDecision(TaskNode.TaskDecision.UE_ONLY);
            taskNode.setExecutionNode(taskNode.getEdgeDevice());
        }else if (offloadingDecision.getDecision() == 2) {
            taskNode.setTaskDecision(TaskNode.TaskDecision.CLOUD);
            computingNode =  simManager.getDataCentersManager().getCloudDatacentersList().get(0).nodeList.get(0);
            job.addCost(budgetNeed);
            taskNode.setExecutionNode(computingNode);
        } else{
            computingNode = simManager.getDataCentersManager().getEdgeDatacenterList().get(offloadingDecision.getDcIndex()).nodeList.get(offloadingDecision.getServerIndex());
            taskNode.setTaskDecision(TaskNode.TaskDecision.MEC_ONLY);
            job.addCost(budgetNeed);
            taskNode.setExecutionNode(computingNode);
        }
    }

    private double getRequiredBudget(TaskNode taskNode) {
        return Helper.getCharge(taskNode.getLength(), taskNode.getReadOps()+taskNode.getWriteOps());
    }
}
