package applications.delta;

import com.mechalikh.pureedgesim.datacentersmanager.ComputingNode;
import com.mechalikh.pureedgesim.datacentersmanager.DataCenter;
import com.mechalikh.pureedgesim.scenariomanager.SimulationParameters;
import com.mechalikh.pureedgesim.simulationmanager.SimulationManager;
import com.mechalikh.pureedgesim.taskgenerator.Task;
import com.mechalikh.pureedgesim.taskorchestrator.Orchestrator;
import common.*;
import dag.TaskNode;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;

import java.util.*;

public class DeltaOrchestrator extends Orchestrator {
    protected Map<Integer, Integer> historyMap = new HashMap<>();
    private SimulationManager simManager;
    private DecimalFormat decimalFormat;
    private boolean twoTier = false;

    public DeltaOrchestrator(SimulationManager simulationManager) {
        super(simulationManager);
        simManager = simulationManager;
        // Initialize the history map
        for (int i = 0; i < nodeList.size(); i++)
            historyMap.put(i, 0);

        DecimalFormatSymbols otherSymbols = new DecimalFormatSymbols(Locale.GERMAN);
        otherSymbols.setDecimalSeparator('.'); // use the dot "." as separation symbole, since the comma ","
        // is used in csv files as a separator
        decimalFormat = new DecimalFormat("######.####", otherSymbols);
    }

    protected ComputingNode findComputingNode(String[] architecture, Task task) {
        if ("DELTA".equals(algorithm)) {
            return schedulingAlgo(task);
        } else if ("DEVICE_ONLY".equals(algorithm)) {
            return deviceOnlyScheduling(task);
        } else if ("EDGE_ONLY".equals(algorithm)) {
            return edgeOnlyScheduling(task);
        } else {
            throw new IllegalArgumentException(getClass().getSimpleName() + " - Unknown orchestration algorithm '"
                    + algorithm + "', please check the simulation parameters file...");
        }
    }

    private ComputingNode deviceOnlyScheduling(Task task) {
        return task.getEdgeDevice();
    }

    private ComputingNode edgeOnlyScheduling(Task task) {
        DataCenter nearestDc = findNearestEdgeDC(task);
        ComputingNode node = this.getBestServer(nearestDc.nodeList, (TaskNode) task);
        double distance = task.getEdgeDevice().getMobilityModel().distanceTo(nearestDc.nodeList.get(0));
        this.updateTransmissionLatency(task, 0, distance);
        return node;
    }

    public ComputingNode schedulingAlgo(Task task){
        ComputingNode node = null;
        if(!task.getEdgeDevice().isSensor()) {
            if (((TaskNode) task).isDummyTask() ||
                    ((TaskNode) task).getTaskDecision() == TaskNode.TaskDecision.UE_ONLY){
                return task.getEdgeDevice();
            }
        }

        DataCenter nearestDc = findNearestEdgeDC(task);

        if(nearestDc == null){
            return task.getEdgeDevice();
        }

        List<DataCenter> dcList = new ArrayList<>();
        dcList.add(nearestDc);
        List<ComputingNode> serverList = getServerList(dcList, ((TaskNode)task).getTaskType(), task);
        node = this.getBestServer(serverList, (TaskNode) task);

        if(node == null){ //no suitable server in nearest DC
            List<DcDistanceIndexPair> dcDistanceIndexPairs = sortDcWithDistance(nearestDc);
            dcList.clear();
            for(DcDistanceIndexPair dcDistanceIndexPair : dcDistanceIndexPairs){
                DataCenter nextNearestDc = simManager.getDataCentersManager().getEdgeDatacenterList().get(dcDistanceIndexPair.getIndex());
                if(nextNearestDc.equals(nearestDc)) //skipping the nearest one it is already evaluated
                    continue;
                dcList.add(nextNearestDc);
            }

            serverList = getServerList(dcList, ((TaskNode)task).getTaskType(), task);
            node = this.getBestServer(serverList, (TaskNode) task);
            dcDistanceIndexPairs.clear();
        }

        if(node == null){ //at last, we go for cloud
            if(twoTier){
                node = getServerWithGreedy(task);
                if(node == null){
                    System.out.println("NO resource allocated");
                }
            } else {
                node = simManager.getDataCentersManager().getCloudDatacentersList().get(0).nodeList.get(0);
            }
        }

        return node;
    }

    ComputingNode getServerWithGreedy(Task task){
        ComputingNode schedule_node = null;
        Double min_latency = Double.MAX_VALUE;
        List<DataCenter> dcList = simManager.getDataCentersManager().getEdgeDatacenterList();
        for(DataCenter dc : dcList){
            for(ComputingNode computingNode : dc.nodeList){
                Double computationTime = Helper.calculateExecutionTime(computingNode, (TaskNode) task);
                Double transmissionTime = this.calculateTransmissionLatency(task, computingNode);
                Double latency = computationTime + transmissionTime + 0.012*transmissionTime;
                if(latency < min_latency) {
                    min_latency = latency;
                    schedule_node = computingNode;
                }
            }
        }
        return schedule_node;
    }

    List<ComputingNode> getServerList(List<DataCenter> dataCenterList, TaskNode.TaskType taskType, Task task){
        List<ComputingNode> nodeList = new ArrayList<>();
        for(DataCenter dc : dataCenterList){
            for(ComputingNode computingNode : dc.nodeList) {
                //if(task.getCpuType().equals("GPU") && computingNode.isGpuEnabled() && computingNode.getGpuNumbers() > 0){
                if(task.getCpuType().equals("GPU") && computingNode.isGpuEnabled()){
                    nodeList.add(computingNode);
                }

                if(task.getCpuType().equals("PIM") && computingNode.isPimEnabled()){
                    nodeList.add(computingNode);
                }

                if (taskType == TaskNode.TaskType.IO_INTENSIVE && computingNode.isSsdEnabled()) {
                    nodeList.add(computingNode);
                }

                if(taskType == TaskNode.TaskType.NORMAL
                        && !computingNode.isGpuEnabled()
                        && !computingNode.isPimEnabled()
                        && !computingNode.isGpuEnabled()){
                    nodeList.add(computingNode);
                }
            }
        }

        if(nodeList.size() == 0){
            for(DataCenter dc : dataCenterList){
                for(ComputingNode computingNode : dc.nodeList) {
                    nodeList.add(computingNode);
                }
            }
        }
        return nodeList;
    };

    private DataCenter findNearestEdgeDC(Task task){
        DataCenter dataCenter = null;
        List<DataCenter> edgeDcs = simManager.getDataCentersManager().getEdgeDatacenterList();
        for(DataCenter dc : edgeDcs){
            if(sameLocation(dc.nodeList.get(0), task.getEdgeDevice(), SimulationParameters.EDGE_DEVICES_RANGE)
                    || (SimulationParameters.ENABLE_ORCHESTRATORS && sameLocation(dc.nodeList.get(0),
                    task.getOrchestrator(), SimulationParameters.EDGE_DEVICES_RANGE))){
                dataCenter = dc;
            }
        }

        return dataCenter;
    }

    private List<DcDistanceIndexPair> sortDcWithDistance(DataCenter nearestDc){
        List<DataCenter> edgeDcs = simManager.getDataCentersManager().getEdgeDatacenterList();
        List<DcDistanceIndexPair> indexDistList = new ArrayList<>();
        for(Integer i = 0; i < edgeDcs.size(); i++){
            Double distance = Helper.calculateDistance(nearestDc, edgeDcs.get(i));
            DcDistanceIndexPair indexDist = new DcDistanceIndexPair();
            indexDist.setDistance(distance);
            indexDist.setIndex(i);
            indexDistList.add(indexDist);
        }

        Collections.sort(indexDistList, new DcDistanceIndexComparator());

        return indexDistList;
    }

    private ComputingNode getBestServer(List<ComputingNode> computingNodes, TaskNode task){ //null return means not possible
        ComputingNode schedule_node = null;
        Double min_latency = Double.MAX_VALUE;
        for(ComputingNode computingNode : computingNodes){
            Double computationTime = Helper.calculateExecutionTime(computingNode, task);
            Double transmissionTime = this.calculateTransmissionLatency(task, computingNode);
            Double latency = computationTime + transmissionTime + 0.012*transmissionTime;
            if(latency < task.getMaxLatency() && latency < min_latency && computingNode.getAvailableCores()>0) {
                min_latency = latency;
                schedule_node = computingNode;
            }
        }

        return schedule_node;
    }

    private double calculateTransmissionLatency(Task task, ComputingNode computingNode){
        double distance = task.getEdgeDevice().getMobilityModel().distanceTo(computingNode);
        double upload_latency = Helper.getWirelessTransmissionLatency(task.getFileSize()) + Helper.calculatePropagationDelay(distance);
        double download_latency = Helper.getWirelessTransmissionLatency(task.getOutputSize()) + Helper.calculatePropagationDelay(distance);
        return upload_latency + download_latency;
    }

    private void updateTransmissionLatency(Task task, Integer type, double distance){ //0-nearest edge, 1-neighbour edge, 2- cloud
        double upload_latency = 0;
        double download_latency = 0;
        double ul_wir = Helper.getWirelessTransmissionLatency(task.getFileSize());
        double dl_wir = Helper.getWirelessTransmissionLatency(task.getOutputSize());
        if(type == 0){
            upload_latency = ul_wir + Helper.calculatePropagationDelay(distance);
            download_latency = dl_wir + Helper.calculatePropagationDelay(distance);
        } else if(type == 1){
            upload_latency = ul_wir + Helper.getManEdgeTransmissionLatency(task.getFileSize()) + Helper.calculatePropagationDelay(distance);
            download_latency = dl_wir + Helper.getManEdgeTransmissionLatency(task.getOutputSize()) + Helper.calculatePropagationDelay(distance);
        } else if(type == 2){
            upload_latency = ul_wir + Helper.getManCloudTransmissionLatency(task.getFileSize()) + Helper.calculatePropagationDelay(distance);
            download_latency = dl_wir + Helper.getManCloudTransmissionLatency(task.getOutputSize()) + Helper.calculatePropagationDelay(distance);
        } else{
            System.out.println("Invalid Type");
        }

        task.setDownloadTransmissionLatency(download_latency);
        task.setUploadTransmissionLatency(upload_latency);
    }


    @Override
    public void resultsReturned(Task task) {
        if(simManager.getScenario().getStringOrchAlgorithm().equals("DELTA")){
            deltaResultReturn(task);
        } else{
            System.out.println("Invalid strategy, please configure the strategy correctly..!");
            System.exit(0);
        }
    }

    private void deltaResultReturn(Task task){
        TaskNode taskNode = (TaskNode) task;
        taskNode.setTaskDone(true);
        DeltaSimMananger customSimMananger = (DeltaSimMananger) simManager;
        if(task.getStatus() == Task.Status.FAILED) {
            System.out.println("Task " + task.getId() + " failed for job " + task.getApplicationID() + " CPU: " + decimalFormat.format(task.getLength()) + " Deadline: " + task.getMaxLatency() +" node type: " + task.getComputingNode().getType() + " ID:" + task.getComputingNode().getId() + " Actual Ex: " + taskNode.getActualExTime() + " Exec Time: " + ((TaskNode) task).getActualExTime() + " CPU: " + task.getActualCpuTime()+" Reason : " + task.getFailureReason());
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
