package applications.remec;

import com.mechalikh.pureedgesim.datacentersmanager.ComputingNode;
import com.mechalikh.pureedgesim.simulationmanager.SimulationManager;
import common.Helper;
import dag.TaskNode;
import org.jgrapht.alg.util.Pair;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class GraphPartition {
    private static double INITIAL_TEMP = 1000.0;
    private static final double COOLING_RATE = 0.95;
    private static final int MAX_ITERATIONS = 100;
    private SimulationManager simulationManager;
    public GraphPartition(SimulationManager simManager){
        simulationManager = simManager;
    }
    public Pair<List<TaskNode>, List<TaskNode>> partition(Map<Integer, TaskNode> job){
        double temperature = INITIAL_TEMP;
        this.initializeGraphAndWeights(job);
        Pair<List<TaskNode>, List<TaskNode>> partition_pair = this.initialPartition(job);
        List<TaskNode> curr_x_l = partition_pair.getFirst();
        List<TaskNode> curr_x_r = partition_pair.getSecond();
        List<TaskNode> best_x_l = curr_x_l;
        List<TaskNode> best_x_r = curr_x_r;

        double bestCost = calculateCost(best_x_l, best_x_r);
        for (int iteration = 0; iteration < MAX_ITERATIONS; iteration++) {
            Pair<List<TaskNode>, List<TaskNode>> neighbour_partition_pair = generateNeighborPartition(curr_x_l, curr_x_r);
            double currCost = this.calculateCost(curr_x_l, curr_x_r);
            double newCost = this.calculateCost(neighbour_partition_pair.getFirst(), neighbour_partition_pair.getSecond());
            double costDifference = newCost - currCost;

            // Accept the new partition if it improves cost or based on probability
            if (costDifference < 0 || Math.exp(-costDifference / temperature) > Math.random()) {
                curr_x_l = neighbour_partition_pair.getFirst();
                curr_x_r = neighbour_partition_pair.getSecond();
            }

            // Update best partition and cost if necessary
            if (newCost < bestCost) {
                best_x_l = curr_x_l;
                best_x_r = curr_x_r;
                bestCost = newCost;
            }

            temperature *= COOLING_RATE;
        }
        System.out.println(best_x_l.size()+":"+best_x_r.size());
        return new Pair<>(best_x_l, best_x_r);
    }

    public void assignOffloadingDecision(Pair<List<TaskNode>, List<TaskNode>> partition){
        List<TaskNode> local = partition.getFirst();
        List<TaskNode> remote = partition.getSecond();
        for(TaskNode taskNode : local){
            taskNode.setTaskDecision(TaskNode.TaskDecision.UE_ONLY);
        }

        for(TaskNode taskNode : remote){
            taskNode.setTaskDecision(TaskNode.TaskDecision.OPEN);
        }
    }

    private void initializeGraphAndWeights(Map<Integer, TaskNode> job) {
        ComputingNode mecNode = simulationManager.getDataCentersManager().getEdgeDatacenterList().get(0).nodeList.get(0);
        ComputingNode cloudNode = simulationManager.getDataCentersManager().getCloudDatacentersList().get(0).nodeList.get(0);
        for (Map.Entry<Integer, TaskNode> taskInfo : job.entrySet()) {
            TaskNode task = taskInfo.getValue();
            double localLatency = Helper.calculateLocalLatency(task);
            double remoteLatency = Helper.calculateAverageRemoteLatency(task, mecNode, cloudNode);
            double localEnergy = Helper.dynamicEnergyConsumption(task.getLength(), task.getEdgeDevice().getMipsPerCore(),
                    task.getEdgeDevice().getMipsPerCore());
            double remoteEnergy = Helper.calculateRemoteEnergyConsumption(task);

            task.setLocalCost(0.4*localLatency + 0.6*localEnergy);
            task.setRemoteCost(0.4*remoteLatency + 0.6*remoteEnergy);
        }
    }

    private double calculateCost(List<TaskNode> x_l, List<TaskNode> x_r){
        double totalCost = 0.0;
        double waiting_time = 0.0;
        for(TaskNode taskNode : x_l){
            waiting_time += taskNode.getLocalCost();
            totalCost += taskNode.getLocalCost()+waiting_time/6;
        }

        for(TaskNode taskNode : x_r){
            totalCost += taskNode.getRemoteCost();
        }
        return totalCost;
    }

    Pair<List<TaskNode>, List<TaskNode>> generateNeighborPartition(List<TaskNode> x_l, List<TaskNode> x_r){
        List<TaskNode> n_x_l = new ArrayList<>(x_l);
        List<TaskNode> n_x_r = new ArrayList<>(x_r);
        int random_index = 0;
        if(n_x_l.size() > 1){
            random_index = Helper.getRandomInteger(0, n_x_l.size()-1);
            TaskNode tmp = n_x_l.get(random_index);
            n_x_l.remove(random_index);
            n_x_r.add(tmp);
        } else{
            random_index = Helper.getRandomInteger(0, n_x_r.size()-1);
            TaskNode tmp = n_x_r.get(random_index);
            n_x_r.remove(random_index);
            n_x_l.add(tmp);
        }
        return new Pair<>(n_x_l, n_x_r);
    }

    Pair<List<TaskNode>, List<TaskNode>> initialPartition(Map<Integer, TaskNode> job){
        List<TaskNode> x_l = new ArrayList<>();
        List<TaskNode> x_r = new ArrayList<>();
        for (Map.Entry<Integer, TaskNode> taskInfo : job.entrySet()) {
            TaskNode task = taskInfo.getValue();
            if(task.isCritical()){
                x_r.add(task);
            } else{
                x_l.add(task);
            }
        }

        return new Pair<>(x_l, x_r);
    }
}
