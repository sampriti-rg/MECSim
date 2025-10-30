package applications.fptosm;

import com.mechalikh.pureedgesim.datacentersmanager.ComputingNode;
import com.mechalikh.pureedgesim.datacentersmanager.DataCenter;
import com.mechalikh.pureedgesim.simulationmanager.SimLog;
import com.mechalikh.pureedgesim.simulationmanager.SimulationManager;
import common.Job;
import common.TaskReliability;
import dag.TaskNode;


import java.util.*;


public class FP_TOSM_Offloader {
    static double RELIABLE_THRESHOLD = 0.01;
    private List<ComputingNode> fogNodes; // All available fog nodes
    private List<ComputingNode> cloudNodes; // All available cloud nodes
    private double[] taskCriteriaWeights;
    SimulationManager simulationManager;

    public FP_TOSM_Offloader(SimulationManager simulationManager){
        this.simulationManager = simulationManager;
        this.taskCriteriaWeights = AHPUtils.calculateWeights(FP_TOSM_Constants.TASK_AHP_CRITERIA_MATRIX_EXAMPLE);
//        fogNodes = new ArrayList<>();
//        for(DataCenter dc : simulationManager.getDataCentersManager().getEdgeDatacenterList()){
//            fogNodes.addAll(dc.nodeList);
//        }
//        cloudNodes = new ArrayList<>();
//        cloudNodes.addAll(simulationManager.getDataCentersManager().getCloudDatacentersList().get(0).nodeList);
    }

    // Algorithm 1: Task Offloading Decision
    public void processTasks(List<TaskNode> incomingTasks) {
        if(fogNodes == null){
            fogNodes = new ArrayList<>();
            for(DataCenter dc : simulationManager.getDataCentersManager().getEdgeDatacenterList()){
                fogNodes.addAll(dc.nodeList);
            }
        }

        if(cloudNodes == null){
            cloudNodes = new ArrayList<>();
            cloudNodes.addAll(simulationManager.getDataCentersManager().getCloudDatacentersList().get(0).nodeList);
        }
        Map<ComputingNode, List<TaskNode>> tasksToOffloadToFog = new HashMap<>(); // FogNode -> List of tasks

        for(TaskNode t : incomingTasks){
            Job job = t.getJob();
            double rel = TaskReliability.getReliability(t);
            if(rel < RELIABLE_THRESHOLD){
                t.setLowReliable(true);
            }

            if(t.isLowReliable()){
                SimLog.totalLowRealiabilityTasks++;
            }
        }
        // Line 3: Start AHP Process to return tasks priorities
        for (TaskNode task : incomingTasks) {
            double betaScore = AHPUtils.calculateTaskPriorityScore(task, this.taskCriteriaWeights);
            task.setPriorityScore(betaScore);
        }

        // Line 5: Sort tasks by priority (descending for processing, paper implies ascending for total priority value)
        // The beta score (sum of weighted needs) - higher means more demanding / higher priority to offload
        incomingTasks.sort(Comparator.comparingDouble(TaskNode::getPriorityScore).reversed()); // Process most demanding first

        DataCenter cloudDC = simulationManager.getDataCentersManager().getCloudDatacentersList().get(0);
        ComputingNode cloudComputingNode =  cloudDC.nodeList.get(0);
        // Line 6: Offloading decision
        for (TaskNode task : incomingTasks) {
            double beta = task.getPriorityScore();
            //System.out.println("Score: "+beta);
            //System.out.println("Length: "+task.getLength());
            if (beta <= FP_TOSM_Constants.BETA_THRESHOLD_LOCAL_FOG) {
                task.setTaskDecision(TaskNode.TaskDecision.UE_ONLY);
                task.setExecutionNode(task.getEdgeDevice());// Queue Θ
                //tasksToExecuteLocally.add(task); // Queue Θ
            } else if (beta <= FP_TOSM_Constants.BETA_THRESHOLD_FOG_CLOUD) {
                // Queue ∀ (Fog)
                ComputingNode node = handleFogOffloading(task, tasksToOffloadToFog);
                if(node != null){
                    task.setTaskDecision(TaskNode.TaskDecision.MEC_ONLY);
                    task.setExecutionNode(node);
                } else {
                    task.setTaskDecision(TaskNode.TaskDecision.CLOUD);
                    task.setExecutionNode(cloudComputingNode);
                }
            } else {
                task.setTaskDecision(TaskNode.TaskDecision.CLOUD);
                task.setExecutionNode(cloudComputingNode);
            }
        }
    }

    private ComputingNode handleFogOffloading(TaskNode task, Map<ComputingNode, List<TaskNode>> tasksToOffloadToFog) {
        boolean scheduledOnFog = false;
        // Line 10-20: Fog Offloading Logic
        if (fogNodes.isEmpty()) {
            return null;
        }

        List<ComputingNode> sortedFogNodes = new ArrayList<>(fogNodes);
        Collections.sort(sortedFogNodes, new Comparator<ComputingNode>() {
            @Override
            public int compare(ComputingNode a, ComputingNode b) {
                double distanceA = task.getEdgeDevice().getMobilityModel().distanceTo(a);
                double distanceB = task.getEdgeDevice().getMobilityModel().distanceTo(b);
                return Double.compare(distanceA, distanceB);
            }
        });

        FP_TOSM_Scheduler scheduler = new FP_TOSM_Scheduler(); // Create scheduler instance
        ComputingNode assignedVm = scheduler.scheduleTaskOnNode(task, sortedFogNodes);

        return assignedVm;
    }

}
