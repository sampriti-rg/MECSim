package applications.remec;

import com.mechalikh.pureedgesim.datacentersmanager.ComputingNode;
import com.mechalikh.pureedgesim.simulationmanager.SimLog;
import com.mechalikh.pureedgesim.simulationmanager.SimulationManager;
import com.mechalikh.pureedgesim.taskgenerator.Task;
import com.mechalikh.pureedgesim.taskorchestrator.Orchestrator;
import common.DataProcessor;
import common.Helper;
import common.Job;
import dag.TaskNode;
import net.sourceforge.jFuzzyLogic.FIS;

import java.util.*;

import static com.mechalikh.pureedgesim.taskgenerator.Task.FailureReason.FAILED_DUE_TO_LATENCY;
import static com.mechalikh.pureedgesim.taskgenerator.Task.FailureReason.FAILED_DUE_TO_UE_FAILURE;

public class ReMECOrchestrator extends Orchestrator {
    static double RELIABLE_THRESHOLD = 0.1;
    protected Map<Integer, Integer> historyMap = new HashMap<>();
    private SimulationManager simManager;
    private Integer currDCIndex;
    FIS fis_ue = null;
    FIS fis_mec = null;

    double min_rel = Double.MAX_VALUE;
    double max_rel = Double.MIN_VALUE;
    double startTime = 0;
    double failStartTime = 0;

    public ReMECOrchestrator(SimulationManager simulationManager) {
        super(simulationManager);
        simManager = simulationManager;
        // Initialize the history map
        for (int i = 0; i < nodeList.size(); i++)
            historyMap.put(i, 0);
        currDCIndex = 0;
        startTime = simManager.getSimulation().clock()/60.0;
        failStartTime = 0;
    }

    protected ComputingNode findComputingNode(String[] architecture, Task task) {
        TaskNode taskNode = (TaskNode) task;
        if ("ReMEC".equals(algorithm)) {
            return taskNode.getSelectedComputingNode();
        }
        else {
            throw new IllegalArgumentException(getClass().getSimpleName() + " - Unknown orchestration algorithm '"
                    + algorithm + "', please check the simulation parameters file...");
        }
    }

    public  void simulateFailures(double failureRate, double currentTime) {
        List<ComputingNode> deviceList = simManager.getDataCentersManager().getEdgeDevicesList();
        if(currentTime - failStartTime >= 10){//10 mins
            int failures = Helper.poisson(failureRate);
            Set<Integer> generatedNumbers = new HashSet<>();
            for (int i = 0; i < failures; i++) {
                Random random = new Random();
                int number;
                do {
                    number =  random.nextInt(deviceList.size());
                } while (generatedNumbers.contains(number));

                ComputingNode device = deviceList.get(number);
                device.setTransientFailure(true);
            }

            failStartTime = currentTime;
        }
    }

    void recoverDevices(){
        List<ComputingNode> deviceList = simManager.getDataCentersManager().getEdgeDevicesList();
        for(ComputingNode device : deviceList){
            if(device.isTransientFailure()){
                device.setTransientFailure(false);
            }
        }
    }

    @Override
    public void resultsReturned(Task task) {
        TaskNode taskNode = (TaskNode) task;
        taskNode.setTaskDone(true);
        ReMECSimManager customSimMananger = (ReMECSimManager) simManager;
        boolean skip = false;
        taskNode.setEndTime(simManager.getSimulation().clock());
        if(!taskNode.isPrimary()){
            skip = true;
        }
        double latency = task.getActualNetworkTime() + task.getActualCpuTime()+ task.getWatingTime();
        if(task.getStatus() == Task.Status.FAILED) {
            System.out.println("Job "+ task.getApplicationID()+ " Task " + task.getId() +
                    " CPU: " + task.getLength() + " node type: " + task.getComputingNode().getType() +
                    " ID:" + task.getComputingNode().getId() + " Reason : " + task.getFailureReason() +
                    " energy level: "+task.getEdgeDevice().getEnergyModel().getBatteryLevel() +
                    " latency: "+latency + " Deadline: "+ task.getMaxLatency());
            if(task.getFailureReason() == FAILED_DUE_TO_LATENCY){
                System.out.println("Network Time: "+task.getActualNetworkTime() + " CPU time: "+ task.getActualCpuTime()+
                        " Waiting time: "+ task.getWatingTime());
            }
        }

        if(skip)
            return;

        double currentTime = simManager.getSimulation().clock();
        this.simulateFailures(2, currentTime);

        double additionalTime = 0;
        if (taskNode.getEdgeDevice().isTransientFailure()){
            if(taskNode.getUmi() != null) { //checking UMI make sure the node type is not UE device.
                //This means we have backup for the critical tasks
                additionalTime = Helper.generateRandomFloat(0.5F, 2.0F);
                if((latency+additionalTime) > SimLog.CACHING_TIME){
                    //Caase of cache timeout
                    taskNode.setStatus(Task.Status.FAILED);
                    taskNode.setFailureReason(FAILED_DUE_TO_UE_FAILURE);
                    simLog.incrementFailedDueToUEFailire(taskNode);
                    additionalTime = 0;
                }
            } else if(task.getStatus() != Task.Status.FAILED){
                taskNode.setStatus(Task.Status.FAILED);
                taskNode.setFailureReason(FAILED_DUE_TO_UE_FAILURE);
                simLog.incrementFailedDueToUEFailire(taskNode);
            }
        } else{
            if(taskNode.isLowReliable() && task.getStatus() != Task.Status.FAILED){
                int rand_var = Helper.getRandomInteger(0, 2);
                if(taskNode.getUmi() != null){
                    if(rand_var == 0){
                        additionalTime = Helper.generateRandomFloat(0.5F, 2.0F);
                    }
                } else {
                    if (rand_var == 0) {
                        taskNode.setStatus(Task.Status.FAILED);
                        taskNode.setFailureReason(FAILED_DUE_TO_UE_FAILURE);
                        simLog.incrementFailedDueToUEFailire(taskNode);
                    }
                }
            }
        }

        if((currentTime - failStartTime) >= 8){
            recoverDevices();
            failStartTime = currentTime;
        }

        if(taskNode.isEndTask()) {
            Job app = DataProcessor.scheduledJob.get(taskNode.getApplicationID());
            app.setStatus(true);
            app.setEndTime(simManager.getSimulation().clock());
            if (customSimMananger.isAllDagCompleted()){
                customSimMananger.genReport();
            }
        } else{
            for(TaskNode taskNode1 : taskNode.successors){
                taskNode1.setAddionalTime(additionalTime);
            }
            customSimMananger.scheduleSuccessors(taskNode);
        }
    }
}

