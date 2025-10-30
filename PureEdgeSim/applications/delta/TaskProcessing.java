package applications.delta;

import com.mechalikh.pureedgesim.datacentersmanager.ComputingNode;
import com.mechalikh.pureedgesim.datacentersmanager.DataCenter;
import com.mechalikh.pureedgesim.simulationmanager.SimulationManager;
import common.Helper;
import dag.TaskNode;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

public class TaskProcessing {
    SimulationManager simManager = null;

    public TaskProcessing(SimulationManager simulationManager) {
        this.simManager = simulationManager;
    }

    public void assignTaskRanking(List<TaskNode> tempTaskList) {
        DataCenter nearestDc = simManager.getDataCentersManager().getEdgeDatacenterList().get(0);
        DataCenter neighBorDc = simManager.getDataCentersManager().getEdgeDatacenterList().get(1);
        DataCenter cloudDc = simManager.getDataCentersManager().getCloudDatacentersList().get(0);

        ComputingNode nearestEdgeNode = nearestDc.nodeList.get(0);
        Double nearestEdgeMips = nearestEdgeNode.getMipsPerCore();
        ComputingNode cloudNode = cloudDc.nodeList.get(0);
        Double cloudMips = cloudNode.getMipsPerCore();
        ComputingNode neighborEdgeNode = neighBorDc.nodeList.get(0);
        Double neighborEdgeMips = neighborEdgeNode.getMipsPerCore();
        Double distBetweenNearestToCoud = nearestEdgeNode.getMobilityModel().distanceTo(cloudNode);
        Double distBetweenNearestToNeighbor = nearestEdgeNode.getMobilityModel().distanceTo(neighborEdgeNode);

        TaskNode rootNode = getRootTask(tempTaskList);
        Double ueTonearestDCDist = rootNode.getEdgeDevice().getMobilityModel().distanceTo(nearestEdgeNode);
        this.setRank(rootNode, nearestEdgeMips, neighborEdgeMips, cloudMips, ueTonearestDCDist,
                distBetweenNearestToNeighbor, distBetweenNearestToCoud);
    }

    public void setRank(TaskNode taskNode, Double nearestEdgeMips, Double neighborEdgeMips, Double cloudMips,
                        Double ueTonearestDCDist, Double distBetweenNearestToNeighbor, Double distBetweenNearestToCoud) {
        Double nearestET = taskNode.getLength() / nearestEdgeMips;
        Double neighborET = taskNode.getLength() / neighborEdgeMips;
        Double cloudET = taskNode.getLength() / cloudMips;
        Double ueMips = taskNode.getEdgeDevice().getMipsPerCore();
        Double ueET = taskNode.getLength() / ueMips;

        Double upload_latency = Helper.getWirelessTransmissionLatency(taskNode.getFileSize()) +
                Helper.calculatePropagationDelay(ueTonearestDCDist);
        Double nearToNeighborLatency = Helper.getManEdgeTransmissionLatency(taskNode.getFileSize()) +
                Helper.calculatePropagationDelay(distBetweenNearestToNeighbor);
        Double nearToCloudLatency = Helper.getManCloudTransmissionLatency(taskNode.getFileSize()) +
                Helper.calculatePropagationDelay(distBetweenNearestToCoud);

        Double ET_near = nearestET + upload_latency;
        Double ET_neighbor = neighborET + nearToNeighborLatency + upload_latency;
        Double ET_cloud = cloudET + nearToCloudLatency + upload_latency;

        Double S_avg = (ET_near + ET_neighbor + ET_cloud) / 3;
        Double T_avg = (S_avg + ueET) / 2;

        if (taskNode.isStartTask()) {
            taskNode.setRank((int) Math.ceil(T_avg));
        } else {
            Integer pred_max_rank = findMaxPredecRank(taskNode.predecessors);
            taskNode.setRank((int) Math.round(T_avg) + pred_max_rank);
        }

        taskNode.setExecutionTime(T_avg);
        if (taskNode.isEndTask()) {
            taskNode.setExecutionTime(T_avg);
            return;
        }

        for (TaskNode successor : taskNode.successors) {
            setRank(successor, nearestEdgeMips, neighborEdgeMips, cloudMips, ueTonearestDCDist,
                    distBetweenNearestToNeighbor, distBetweenNearestToCoud);
        }
    }

    private Integer findMaxPredecRank(List<TaskNode> predecessors) {
        Integer max_rank = Integer.MIN_VALUE;
        for (TaskNode pred : predecessors) {
            if (pred.getRank() == null) {
                continue;
            }
            if (max_rank < pred.getRank()) {
                max_rank = pred.getRank();
            }
        }

        return max_rank;
    }

    private TaskNode getRootTask(List<TaskNode> tempTaskList) {
        TaskNode rootNode = null;
        for (TaskNode taskNode : tempTaskList) {
            if (taskNode.isStartTask()) {
                rootNode = taskNode;
                break;
            }
        }

        return rootNode;
    }


    public void assignDecision(List<TaskNode> tempTaskList) {
        Collections.sort(tempTaskList, getCompByCPUNeed());
        int numNormalTask = 0;
        for (TaskNode taskNode : tempTaskList) {
            if (taskNode.getTaskType() == TaskNode.TaskType.NORMAL) {
                ++numNormalTask;
            }
        }

        int no_uetask = (int) Math.ceil(50 * numNormalTask / 100);
        int index = 0;
        while (no_uetask > 0 && index < tempTaskList.size()) {
            if (tempTaskList.get(index).getTaskType() == TaskNode.TaskType.NORMAL) {
                tempTaskList.get(index).setTaskDecision(TaskNode.TaskDecision.UE_ONLY);
                tempTaskList.get(index).setLength(Helper.getRandomInteger(5, 50));
                tempTaskList.get(index).setMaxLatency(Helper.getRandomInteger(60, 100));
                --no_uetask;
            }

            ++index;
        }
    }

    public static Comparator<TaskNode> getCompByCPUNeed() {
        Comparator comp = new Comparator<TaskNode>() {
            @Override
            public int compare(TaskNode t1, TaskNode t2) {
                if (t1.getLength() == t2.getLength())
                    return 0;
                else if (t1.getLength() < t2.getLength())
                    return -1;
                else
                    return 1;
            }
        };
        return comp;
    }

    public void assignTaskType(Map<Integer, TaskNode> job) {
        int numCPUIntensiveTask = 0;
        int numIoIntensiveTask = 0;
        int numMemIntensiveTask = 0;

        for (Map.Entry<Integer, TaskNode> task : job.entrySet()) {
            TaskNode.TaskType type = this.getTaskType(task.getValue());
            task.getValue().setTaskType(type);
            if (type == TaskNode.TaskType.CPU_INTENSIVE) {
                ++numCPUIntensiveTask;
            } else if (type == TaskNode.TaskType.IO_INTENSIVE) {
                ++numIoIntensiveTask;
            } else if (type == TaskNode.TaskType.MEM_INTENSIVE) {
                ++numMemIntensiveTask;
            }
        }

        Integer parallelizableTaskPct = Helper.getRandomInteger(55, 80); //55% to 80% of CPU intensive tasks are marking as GPU suitable tasks
        Integer totalGpuTask = parallelizableTaskPct * numCPUIntensiveTask / 100;
        for (Map.Entry<Integer, TaskNode> task : job.entrySet()) {
            if (task.getValue().getTaskType() == TaskNode.TaskType.CPU_INTENSIVE) {
                if (totalGpuTask > 0) {
                    task.getValue().setCpuType("GPU");
                    --totalGpuTask;
                }
            }
        }
        //System.out.println(numCPUIntensiveTask +" : "+numIoIntensiveTask +" : "+numMemIntensiveTask);
    }

    private TaskNode.TaskType getTaskType(TaskNode taskNode){
        /*
         * SSD: w: 540MB/s, r: 560MB/s
         * HD: 140-190 MB/s
         * MIPS- 400000
         * data bus: 300 MB/s (SATA 2.0 https://en.wikipedia.org/wiki/Serial_ATA)
         * */
        List<DataCenter> dclist = simManager.getDataCentersManager().getEdgeDatacenterList();
        ComputingNode node = dclist.get(0).nodeList.get(0);
        final Double MIPS = node.getMipsPerCore();
        TaskNode.TaskType taskType = TaskNode.TaskType.NORMAL;
        Double cpuTime = taskNode.getLength() / MIPS;
        Double ioTime = taskNode.getStorageNeed() * 60 / (100 * node.getReadBandwidth()) //READ operation, 60% read
                + taskNode.getStorageNeed() * 40 / (100 * node.getWriteBandwidth()); //WRITE operation, 40% write;;
        Double memoryTransferTime = taskNode.getMemoryNeed()/node.getDataBusBandwidth();
        Double totalTime = cpuTime + ioTime + memoryTransferTime;
        if(cpuTime/totalTime > 0.5) {
            taskType = TaskNode.TaskType.CPU_INTENSIVE;
        } else if(ioTime/totalTime > 0.5){
            taskType = TaskNode.TaskType.IO_INTENSIVE;
        } else if(memoryTransferTime/totalTime > 0.5){
            taskType = TaskNode.TaskType.MEM_INTENSIVE;
            taskNode.setCpuType("PIM");
        }

        return taskType;
    }
}
