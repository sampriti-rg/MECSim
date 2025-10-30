package applications.fptosm;

import applications.remec.ReMECSimManager;
import com.mechalikh.pureedgesim.datacentersmanager.ComputingNode;
import com.mechalikh.pureedgesim.simulationmanager.SimulationManager;
import com.mechalikh.pureedgesim.taskgenerator.Task;
import com.mechalikh.pureedgesim.taskorchestrator.Orchestrator;
import common.DataProcessor;
import common.Helper;
import common.Job;
import dag.TaskNode;

import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

import static com.mechalikh.pureedgesim.taskgenerator.Task.FailureReason.FAILED_DUE_TO_LATENCY;
import static com.mechalikh.pureedgesim.taskgenerator.Task.FailureReason.FAILED_DUE_TO_UE_FAILURE;

public class FpTosmOrchestrator extends Orchestrator {
    double startTime = 0;
    double failStartTime = 0;
    private SimulationManager simManager;
    public FpTosmOrchestrator(SimulationManager simulationManager) {
        super(simulationManager);
        simManager = simulationManager;
        startTime = simManager.getSimulation().clock()/60.0;
        failStartTime = 0;
    }
    @Override
    protected ComputingNode findComputingNode(String[] architecture, Task task) {
        TaskNode taskNode = (TaskNode) task;
        return taskNode.getExecutionNode();
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

    @Override
    public void resultsReturned(Task task) {
        FpTosmSimManager customSimMananger = (FpTosmSimManager) simManager;
        TaskNode taskNode = (TaskNode) task;
        taskNode.setTaskDone(true);
        double latency = task.getActualNetworkTime() + task.getActualCpuTime()+ task.getWatingTime();
        if(task.getStatus() == Task.Status.FAILED) {
            //double latency = task.getActualNetworkTime() + task.getActualCpuTime()+ task.getWatingTime();
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

        if(taskNode.isEndTask()) {
            Job app = DataProcessor.scheduledJob.get(taskNode.getApplicationID());
            app.setStatus(true);
            app.setEndTime(simManager.getSimulation().clock());
            if (customSimMananger.isAllDagCompleted()){
                customSimMananger.genReport();
            }
        } else{
            for(TaskNode taskNode1 : taskNode.successors){
                taskNode1.setAddionalTime(0);
            }
            customSimMananger.scheduleSuccessors(taskNode);
        }
    }
}
