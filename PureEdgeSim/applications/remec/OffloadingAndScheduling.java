package applications.remec;

import com.mechalikh.pureedgesim.datacentersmanager.ComputingNode;
import com.mechalikh.pureedgesim.datacentersmanager.DataCenter;
import com.mechalikh.pureedgesim.simulationmanager.SimLog;
import com.mechalikh.pureedgesim.simulationmanager.SimulationManager;
import com.mechalikh.pureedgesim.taskgenerator.Task;
import common.Helper;
import common.Job;
import common.TaskReliability;
import dag.TaskNode;
import net.sourceforge.jFuzzyLogic.FIS;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class OffloadingAndScheduling {
    static String fileName_mec = "PureEdgeSim/applications/remec/scheduling_mec.fcl";
    static double RELIABLE_THRESHOLD = 0.01;
    FIS fis_ue = null;
    FIS fis_mec = null;
    SimulationManager simManager = null;
    private UMIGenerator umiGenerator = null;
    protected Integer messageId = 0;
    private static double w1 = 0.5; //latency
    private static double w2 = 0.5; //energy
    private Map<Integer, Integer> lbMap = new HashMap<>();
    boolean isRandom = false;
    boolean randomOffloading = false;
    //Security security = null;
    public OffloadingAndScheduling(SimulationManager simulationManager){
        simManager = simulationManager;
        fis_mec = FIS.load(fileName_mec, true);
        umiGenerator = new UMIGenerator();
    }

    public void remec(Task task, boolean is_secondary){
        TaskNode taskNode = (TaskNode) task;
        double rel = TaskReliability.getReliability(taskNode);
        if(rel < RELIABLE_THRESHOLD){
            taskNode.setLowReliable(true);
        }

        if(taskNode.isLowReliable()){
            SimLog.totalLowRealiabilityTasks++;
        }

        if(taskNode.isCritical()){
            SimLog.totalCriticalTasks++;
        }

        if(taskNode.isStartTask() || taskNode.isEndTask()){
            taskNode.setTaskDecision(TaskNode.TaskDecision.UE_ONLY);
            taskNode.setSelectedComputingNode(taskNode.getEdgeDevice());
            return;
        }

        Job job = taskNode.getJob();
        double budgetNeed = getRequiredBudget(taskNode);
        double remaining_budget = job.getMaxBudget() - job.getUsedBudget();
        TaskNode.TaskDecision decision = getOffloadingDecisionLatest(taskNode, remaining_budget, budgetNeed);

        if(!is_secondary && decision == TaskNode.TaskDecision.UE_ONLY){
            taskNode.setTaskDecision(TaskNode.TaskDecision.UE_ONLY);
            taskNode.setSelectedComputingNode(taskNode.getEdgeDevice());
            return;
        }

        ComputingNode computingNode = null;

        messageId++;
        TaskNode.MecSchedulingDecision schedulingDecision = TaskNode.MecSchedulingDecision.NEAREST;
        if(isRandom){
            Integer r = Helper.getRandomInteger(0, simManager.getDataCentersManager().getEdgeDatacenterList().size());
            if(r < simManager.getDataCentersManager().getEdgeDatacenterList().size()){
                DataCenter dc = simManager.getDataCentersManager().getEdgeDatacenterList().get(r);
                String umi = umiGenerator.generateUMI(dc.getDcId(), taskNode.getApplicationID(), messageId);
                dc.getBroker().addTask(umi, taskNode);
                computingNode = getBestServer(dc.nodeList, taskNode);
                taskNode.setTaskDecision(TaskNode.TaskDecision.MEC_ONLY);
                job.addCost(budgetNeed);
                taskNode.setSelectedComputingNode(computingNode);
                taskNode.setUmi(umi);
            } else{
                DataCenter cloudDC = simManager.getDataCentersManager().getCloudDatacentersList().get(0);
                computingNode =  cloudDC.nodeList.get(0);
                taskNode.setTaskDecision(TaskNode.TaskDecision.CLOUD);
                String umi = umiGenerator.generateUMI(cloudDC.getDcId(), taskNode.getApplicationID(), messageId);
                cloudDC.getBroker().addTask(umi, taskNode);
                job.addCost(budgetNeed);
                taskNode.setSelectedComputingNode(computingNode);
                taskNode.setUmi(umi);
            }
            if(computingNode == null){
                DataCenter cloudDC = simManager.getDataCentersManager().getCloudDatacentersList().get(0);
                computingNode =  cloudDC.nodeList.get(0);
                taskNode.setTaskDecision(TaskNode.TaskDecision.CLOUD);
                if(taskNode.isCritical()) {
                    String umi = umiGenerator.generateUMI(cloudDC.getDcId(), taskNode.getApplicationID(), messageId);
                    cloudDC.getBroker().addTask(umi, taskNode);
                    taskNode.setUmi(umi);
                }
                taskNode.setSelectedComputingNode(computingNode);
                job.addCost(budgetNeed);
            }
        } else {
            schedulingDecision = getMecSchedulingDecision(taskNode);

            DataCenter cloudDC = simManager.getDataCentersManager().getCloudDatacentersList().get(0);
            if (schedulingDecision == TaskNode.MecSchedulingDecision.NEAREST) {
                //find best nearest server
                DataCenter nearestDc = taskNode.getEdgeDevice().getNearestDC();
                String umi = umiGenerator.generateUMI(nearestDc.getDcId(), taskNode.getApplicationID(), messageId);
                nearestDc.getBroker().addTask(umi, taskNode);
                computingNode = getBestServer(nearestDc.nodeList, taskNode);
                taskNode.setTaskDecision(TaskNode.TaskDecision.MEC_ONLY);
                job.addCost(budgetNeed);
                taskNode.setSelectedComputingNode(computingNode);
                taskNode.setUmi(umi);
                SimLog.updateCachingCost(taskNode);
                if (lbMap.containsKey(nearestDc.getDcId())) {
                    lbMap.put(nearestDc.getDcId(), lbMap.get(nearestDc.getDcId()) + 1);
                } else {
                    lbMap.put(nearestDc.getDcId(), 1);
                }
            } else if (schedulingDecision == TaskNode.MecSchedulingDecision.NEIGHBOUR) {
                //find best neighbour
                List<ComputingNode> serverList = new ArrayList<>();
                Integer min = Integer.MAX_VALUE;
                DataCenter dc = null;
                for (DataCenter dataCenter : simManager.getDataCentersManager().getEdgeDatacenterList()) {
                    if (!lbMap.containsKey(dataCenter.getDcId())) {
                        lbMap.put(dataCenter.getDcId(), 0);
                    }
                    if (lbMap.get(dataCenter.getDcId()) < min) {
                        dc = dataCenter;
                        min = lbMap.get(dataCenter.getDcId());
                    }
                }
                serverList.addAll(dc.nodeList);
                computingNode = getBestServer(serverList, taskNode);
                if (computingNode != null) {
                    taskNode.setTaskDecision(TaskNode.TaskDecision.MEC_ONLY);

                    String umi = umiGenerator.generateUMI(computingNode.getDataCenter().getDcId(), taskNode.getApplicationID(), messageId);
                    computingNode.getDataCenter().getBroker().addTask(umi, taskNode);
                    job.addCost(budgetNeed);
                    taskNode.setSelectedComputingNode(computingNode);
                    taskNode.setUmi(umi);
                    SimLog.updateCachingCost(taskNode);
                    if (lbMap.containsKey(computingNode.getDataCenter().getDcId())) {
                        lbMap.put(computingNode.getDataCenter().getDcId(), lbMap.get(computingNode.getDataCenter().getDcId()) + 1);
                    } else {
                        lbMap.put(computingNode.getDataCenter().getDcId(), 1);
                    }
                } else {
                    computingNode = cloudDC.nodeList.get(0);
                    taskNode.setTaskDecision(TaskNode.TaskDecision.CLOUD);
                    String umi = umiGenerator.generateUMI(cloudDC.getDcId(), taskNode.getApplicationID(), messageId);
                    cloudDC.getBroker().addTask(umi, taskNode);
                    job.addCost(budgetNeed);
                    taskNode.setSelectedComputingNode(computingNode);
                    taskNode.setUmi(umi);
                    SimLog.updateCachingCost(taskNode);
                }
            } else {
                computingNode = cloudDC.nodeList.get(0);
                taskNode.setTaskDecision(TaskNode.TaskDecision.CLOUD);
                String umi = umiGenerator.generateUMI(cloudDC.getDcId(), taskNode.getApplicationID(), messageId);
                cloudDC.getBroker().addTask(umi, taskNode);
                job.addCost(budgetNeed);
                taskNode.setSelectedComputingNode(computingNode);
                taskNode.setUmi(umi);
                SimLog.updateCachingCost(taskNode);
            }

            if (computingNode == null) {
                computingNode = cloudDC.nodeList.get(0);
                taskNode.setTaskDecision(TaskNode.TaskDecision.CLOUD);
                if (taskNode.isCritical()) {
                    String umi = umiGenerator.generateUMI(cloudDC.getDcId(), taskNode.getApplicationID(), messageId);
                    cloudDC.getBroker().addTask(umi, taskNode);
                    taskNode.setUmi(umi);
                    SimLog.updateCachingCost(taskNode);
                }
                taskNode.setSelectedComputingNode(computingNode);
                job.addCost(budgetNeed);
            }
        }
    }

    private TaskNode.TaskDecision getOffloadingDecisionLatest(TaskNode taskNode, double remaining_budget,
                                                              double budgetNeed){
        TaskNode.TaskDecision decision = TaskNode.TaskDecision.UE_ONLY;
        if(randomOffloading){
            Integer rand = Helper.getRandomInteger(0, 1);
            if(rand == 0){
                decision = TaskNode.TaskDecision.UE_ONLY;
            } else if (rand == 1 && remaining_budget >= budgetNeed) {
                decision = TaskNode.TaskDecision.MEC_ONLY;
            } else{
                decision = TaskNode.TaskDecision.UE_ONLY;
            }
        }else {
            if((taskNode.isCritical() || taskNode.isLowReliable()) && budgetNeed <= remaining_budget){
                decision = TaskNode.TaskDecision.MEC_ONLY;
            } else{
                double fi = FrequencyScaling.getOptimalFrequency(taskNode);
                double localLatency = Helper.calculateLocalLatency(taskNode, fi);
                double remoteLatency = Helper.calculateAverageRemoteLatency(taskNode, simManager.getDataCentersManager().getEdgeDatacenterList().get(0).nodeList.get(0),
                        simManager.getDataCentersManager().getCloudDatacentersList().get(0).nodeList.get(0));
                double localEnergy = 1000 * Helper.dynamicEnergyConsumption(taskNode.getLength(), taskNode.getEdgeDevice().getMipsPerCore(), fi);
                double remoteEnergy = 1000 * Helper.calculateRemoteEnergyConsumption(taskNode);
                double localWeightedSum = w1 * localLatency + w2 * localEnergy;
                double remoteWeightedSum = w1 * remoteLatency + w2 * remoteEnergy;
                if((localWeightedSum < remoteWeightedSum && localLatency < taskNode.getMaxLatency())
                        || budgetNeed > remaining_budget){
                    decision = TaskNode.TaskDecision.UE_ONLY;
                } else{
                    decision = TaskNode.TaskDecision.MEC_ONLY;
                }
            }
        }
        //System.out.println("Budget remaining: "+remaining_budget);
        return decision;
    }


    private TaskNode.TaskDecision getOffloadingDecision1(TaskNode taskNode, double remaining_budget,
                                                         double budgetNeed){
        TaskNode.TaskDecision decision = TaskNode.TaskDecision.UE_ONLY;
        if(randomOffloading){
            Integer rand = Helper.getRandomInteger(0, 1);
            if(rand == 0){
                decision = TaskNode.TaskDecision.UE_ONLY;
            } else if (rand == 1 && remaining_budget >= budgetNeed) {
                decision = TaskNode.TaskDecision.MEC_ONLY;
            } else{
                decision = TaskNode.TaskDecision.UE_ONLY;
            }
        }else {
            double localLatency = Helper.calculateLocalLatency(taskNode);
            double remoteLatency = Helper.calculateAverageRemoteLatency(taskNode, simManager.getDataCentersManager().getEdgeDatacenterList().get(1).nodeList.get(0),
                    simManager.getDataCentersManager().getCloudDatacentersList().get(0).nodeList.get(0));
            double localEnergy = 1000 * Helper.dynamicEnergyConsumption(taskNode.getLength(), taskNode.getEdgeDevice().getMipsPerCore(), taskNode.getEdgeDevice().getMipsPerCore());
            double remoteEnergy = 1000 * Helper.calculateRemoteEnergyConsumption(taskNode);

            double localWeightedSum = w1 * localLatency + w2 * localEnergy;
            double remoteWeightedSum = w1 * remoteLatency + w2 * remoteEnergy;
            if ((localWeightedSum < remoteWeightedSum && localLatency <= taskNode.getMaxLatency() && !(taskNode.isLowReliable() || taskNode.isCritical())) /*|| (taskNode.isLowReliable() && localLatency <= taskNode.getMaxLatency())*/) {
                //if((localWeightedSum < remoteWeightedSum && localLatency <= taskNode.getMaxLatency())){
                //System.out.println("AKHIRUL: "+localWeightedSum+" : "+remoteWeightedSum + " lat: "+localLatency+" : "+remoteLatency + " Energy: "+localEnergy+" : "+remoteEnergy);
                decision = TaskNode.TaskDecision.UE_ONLY;
            } else if (remaining_budget >= budgetNeed) {
                decision = TaskNode.TaskDecision.MEC_ONLY;
            } else {
                decision = TaskNode.TaskDecision.UE_ONLY;
            }
        }

        return decision;
    }

    private TaskNode.TaskDecision getOffloadingDecision(TaskNode taskNode, double remaining_budget,
                                                        double budgetNeed){
        TaskNode.TaskDecision decision = TaskNode.TaskDecision.UE_ONLY;
        if((remaining_budget >= budgetNeed) && (taskNode.isLowReliable() || taskNode.isCritical())){
            decision = TaskNode.TaskDecision.MEC_ONLY;
        } else{
            double localLatency = Helper.calculateLocalLatency(taskNode);
            double remoteLatency = Helper.calculateAverageRemoteLatency(taskNode, simManager.getDataCentersManager().getEdgeDatacenterList().get(0).nodeList.get(0),
                    simManager.getDataCentersManager().getCloudDatacentersList().get(0).nodeList.get(0));
            double localEnergy = Helper.dynamicEnergyConsumption(taskNode.getLength(), taskNode.getEdgeDevice().getMipsPerCore(), taskNode.getEdgeDevice().getMipsPerCore());
            double remoteEnergy = Helper.calculateRemoteEnergyConsumption(taskNode);

            double localWeightedSum = w1*localLatency + w2*localEnergy;
            double remoteWeightedSum = w1*remoteLatency + w2*remoteEnergy;
            //System.out.println("Job: "+taskNode.getApplicationID()+" Task "+taskNode.getId()+" Local: "+localLatency+" Max: "+taskNode.getMaxLatency());
            if((localWeightedSum < remoteWeightedSum) && localLatency < taskNode.getMaxLatency()){
                decision = TaskNode.TaskDecision.UE_ONLY;
            } else {
                decision = TaskNode.TaskDecision.MEC_ONLY;
            }
        }
        return decision;
    }

    private TaskNode.TaskDecision getOffloadingDecisionFuzzy(TaskNode taskNode){
        TaskNode.TaskDecision decision = TaskNode.TaskDecision.UE_ONLY;

        double latency = Helper.calculateLocalLatency(taskNode);
        double remainingBudgetPct = 100 - (taskNode.getJob().getUsedBudget())*100/(taskNode.getJob().getMaxBudget());
        double energyRem = taskNode.getEdgeDevice().getEnergyModel().getBatteryLevel()*100/taskNode.getEdgeDevice().getEnergyModel().getBatteryCapacity();

        fis_ue.setVariable("latencyAndDeadlineRatio", latency/taskNode.getMaxLatency());
        fis_ue.setVariable("remainingEnergyPct", energyRem);
        fis_ue.setVariable("remainingBudgetPct", remainingBudgetPct-2);
        //fis_ue.setVariable("cpuUsage", cpuUtil);
        fis_ue.setVariable("reliability", 0.5);

        //System.out.println("latencyratio: "+latency/taskNode.getMaxLatency() + " remainingEnergyPct: "+energyRem+ " budget: "+remainingBudgetPct+" cpu: "+cpuUtil+" rel: "+rel);
        // Evaluate
        fis_ue.evaluate();
        double defuzzifyValue = fis_ue.getVariable("decision").defuzzify();
        //System.out.println("defuzzifyValue: "+defuzzifyValue+" CPU: "+taskNode.getLength());
        if(defuzzifyValue > 10){
            decision = TaskNode.TaskDecision.MEC_ONLY;
        }
        return decision;
    }

    private TaskNode.MecSchedulingDecision getMecSchedulingDecision(TaskNode taskNode){
        TaskNode.MecSchedulingDecision decision = TaskNode.MecSchedulingDecision.CLOUD;
        double latency = Helper.calculateExecutionTime(taskNode.getEdgeDevice().getNearestDC().nodeList.get(0), taskNode) +
                2*Helper.calculatePropagationDelay(taskNode.getEdgeDevice().getMobilityModel().distanceTo(taskNode.getEdgeDevice().getNearestDC().nodeList.get(0)))+
                Helper.getManEdgeTransmissionLatency(taskNode.getFileSize());
        latency = 3*latency; //This is because of the waiting time
        double neighborLatency = 3*getLatencyForNeighbour(taskNode, taskNode.getEdgeDevice().getNearestDC());
        //double wanDelay = Helper.getManCloudTransmissionLatency(taskNode.getFileSize());
        double latencyAndDeadlineRatioNr = latency/taskNode.getMaxLatency();
        double latencyAndDeadlineRatioNb = neighborLatency/taskNode.getMaxLatency();
        double nearestDcCpuUsage = getAvgCPUUsageOfDc(taskNode.getEdgeDevice().getNearestDC());
        fis_mec.setVariable("latencyAndDeadlineRatioNr", latencyAndDeadlineRatioNr);
        fis_mec.setVariable("latencyAndDeadlineRatioNb", latencyAndDeadlineRatioNb);
        fis_mec.setVariable("nearestDcCpuUsage", nearestDcCpuUsage);
        //fis_mec.setVariable("wanLatency", wanDelay);

        //double decision = fis_mec.getVariable("decision").defuzzify();
        fis_mec.evaluate();
        double defuzzifyValue = fis_mec.getVariable("decision").defuzzify();
        //System.out.println("MEC scheduling defuzzifyValue: "+defuzzifyValue+" : "+latencyAndDeadlineRatioNr+" : "+latencyAndDeadlineRatioNb+" : "+nearestDcCpuUsage);
        SimLog.updateValues(latencyAndDeadlineRatioNr, latencyAndDeadlineRatioNb, nearestDcCpuUsage, defuzzifyValue);
        //23.5<= nearest, 23.5< neighbour<=24.5, cloud > 24.5 -best
        //26, 28 --good
        if(defuzzifyValue <= 23.5){
            decision = TaskNode.MecSchedulingDecision.NEAREST;
            SimLog.updateDcStats(1, 0, 0);
        } else if(defuzzifyValue > 23.5 && defuzzifyValue <= 24.5){
            decision = TaskNode.MecSchedulingDecision.NEIGHBOUR;
            SimLog.updateDcStats(0, 1, 0);
        } else{
            SimLog.updateDcStats(0, 0, 1);
        }
        return decision;
    }

    private double getLatencyForNeighbour(TaskNode taskNode, DataCenter nearestDC){
        double totalExLatency = 0;
        double totalTransLatency = 0;
        int serverCount = 0;
        for(DataCenter dc : simManager.getDataCentersManager().getEdgeDatacenterList()) {
            if (!dc.equals(nearestDC)) {
                for (ComputingNode computingNode : dc.nodeList) {
                    totalExLatency += Helper.calculateExecutionTime(computingNode, taskNode);
                    serverCount++;
                }
                totalTransLatency += Helper.calculatePropagationDelay(nearestDC.nodeList.get(0).getMobilityModel().distanceTo(dc.nodeList.get(0))) +
                        Helper.getManEdgeTransmissionLatency(taskNode.getFileSize());
            }
        }
        return totalExLatency/serverCount + totalTransLatency/(simManager.getDataCentersManager().getEdgeDatacenterList().size()-1);
    }

    double getAvgCPUUsageOfDc(DataCenter dc){
        double totalCpuUsage = 0;
        for(ComputingNode computingNode : dc.nodeList){
            totalCpuUsage += computingNode.getAvgCpuUtilization();
        }
        return totalCpuUsage/dc.nodeList.size();
    }

    private ComputingNode getBestServer(List<ComputingNode> computingNodes, TaskNode task){ //null return means not possible
        ComputingNode schedule_node = null;
        double min_latency = Double.MAX_VALUE;
        double cpuUtil = 0.0;
        double minCpuUtil = Double.MIN_VALUE;
        if(isRandom){
            Integer i = Helper.getRandomInteger(0, computingNodes.size()-1);
            schedule_node = computingNodes.get(i);
        }else {
            for (ComputingNode computingNode : computingNodes) {
                cpuUtil += (computingNode.getCurrentCpuUtilization() != 0) ? computingNode.getCurrentCpuUtilization() : 1;
            }
            double avgCpuUtil = cpuUtil / computingNodes.size();

            for (ComputingNode computingNode : computingNodes) {
                Double computationTime = Helper.calculateExecutionTime(computingNode, task);
                Double transmissionTime = this.calculateTransmissionLatency(task, computingNode);
                Double latency = 1.5 + computationTime + transmissionTime + 0.012 * transmissionTime;
                if (computingNode.getCurrentCpuUtilization() <= avgCpuUtil && latency < task.getMaxLatency() && latency < min_latency && computingNode.getAvailableCores() > 0) {
                    //computingNode.getCurrentCpuUtilization() <= avgCpuUtil &&
                    //&& computingNode.getAvailableCores() > 0
                    min_latency = latency;
                    schedule_node = computingNode;
                }
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

    private double getRequiredBudget(TaskNode taskNode) {
        return Helper.getCharge(taskNode.getLength(), taskNode.getReadOps()+taskNode.getWriteOps());
    }
}
