package applications.bqprofit;

import com.mechalikh.pureedgesim.datacentersmanager.ComputingNode;
import com.mechalikh.pureedgesim.simulationmanager.SimulationManager;
import com.mechalikh.pureedgesim.taskgenerator.Task;
import com.mechalikh.pureedgesim.taskorchestrator.Orchestrator;
import common.DataProcessor;
import common.Job;
import dag.TaskNode;


import java.util.*;

import static com.mechalikh.pureedgesim.scenariomanager.SimulationParameters.TYPES.*;

public class BQProfitOrchestrator extends Orchestrator {
    protected Map<Integer, Integer> historyMap = new HashMap<>();
    private SimulationManager simManager;
    private Integer currDcIndex;

    public BQProfitOrchestrator(SimulationManager simulationManager) {
        super(simulationManager);
        simManager = simulationManager;
        // Initialize the history map
        for (int i = 0; i < nodeList.size(); i++)
            historyMap.put(i, 0);
        currDcIndex = 0;
    }

    protected ComputingNode findComputingNode(String[] architecture, Task task) {
            if ("BQProfit".equals(algorithm)) {
                return BQProfitPolicy(task);
            }  else {
                throw new IllegalArgumentException(getClass().getSimpleName() + " - Unknown orchestration algorithm '"
                        + algorithm + "', please check the simulation parameters file...");
            }

    }

    private ComputingNode BQProfitPolicy(Task task) {
        TaskNode taskNode = (TaskNode)task;
        //System.out.println("Allocating computing resources: "+taskNode.getApplicationID()+" : "+taskNode.getId());
        if(taskNode.getTaskDecision() == TaskNode.TaskDecision.UE_ONLY){
            return taskNode.getEdgeDevice();
        } else{
            return taskNode.getSelectedComputingNode();
        }
    }


    @Override
    public void resultsReturned(Task task) {
        TaskNode taskNode = (TaskNode) task;
        taskNode.setTaskDone(true);
        BQProfitSimMananger customSimMananger = (BQProfitSimMananger) simManager;
        if(task.getStatus() == Task.Status.FAILED) {
            System.out.println("Task " + task.getId() + " failed for job " + task.getApplicationID() +
                    " CPU: " + task.getLength() + " node type: " + task.getComputingNode().getType() +
                    " ID:" + task.getComputingNode().getId() + " Reason : " + task.getFailureReason());
            if(task.getApplicationID() == 4 && task.getId() ==69){
                System.out.println("Successor: "+ taskNode.successors.size()+ " End: "+ taskNode.isEndTask());
            }
            if(task.getComputingNode().getType() == EDGE_DATACENTER || task.getComputingNode().getType() == CLOUD) {
                simManager.getProfitCalculator().updatePenalty(taskNode.getChargingPlan().getPenalty());
            }
        } else{
            if (task.getComputingNode().getType() == EDGE_DATACENTER || task.getComputingNode().getType() == CLOUD) {
                simManager.getProfitCalculator().updateCpuProfit(taskNode.getLength(),
                                task.getComputingNode().getCostPlan(), taskNode);
                simManager.getProfitCalculator().updateMemoryProfit(taskNode.getMemoryNeed(),
                                task.getComputingNode().getCostPlan(), taskNode);
                simManager.getProfitCalculator().updateIoProfit((double) (taskNode.getReadOps() + taskNode.getWriteOps()),
                                task.getComputingNode().getCostPlan(), taskNode);
            }
        }

        if(taskNode.isEndTask()) {
            Job app = DataProcessor.scheduledJob.get(taskNode.getApplicationID());
            app.setStatus(true);

            if (customSimMananger.isAllDagCompleted()){
                customSimMananger.genReport();
            }
        } else if(!taskNode.isEndTask()) {
            customSimMananger.scheduleSuccessors(taskNode);
        }
    }

}
