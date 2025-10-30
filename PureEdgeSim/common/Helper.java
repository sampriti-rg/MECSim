package common;

import applications.bqprofit.ChargingPlan;
import com.mechalikh.pureedgesim.datacentersmanager.ComputingNode;
import com.mechalikh.pureedgesim.datacentersmanager.DataCenter;
import com.mechalikh.pureedgesim.scenariomanager.SimulationParameters;
import com.mechalikh.pureedgesim.taskgenerator.Task;
import dag.TaskNode;

import java.util.*;

public class Helper {
    public static double calculateExecutionTime(ComputingNode computingNode, Task task){
        double execTime = 0;
        if(GlobalConfig.getInstance().getAlgorithm().equals("DELTA")){
            execTime = calculateExecutionTimeDelta(computingNode, task);
        } else if (GlobalConfig.getInstance().getAlgorithm().equals("BQProfit")) {
            execTime = calculateExecutionTimeBQProfit(computingNode, task);
        } else if (GlobalConfig.getInstance().getAlgorithm().equals("ReMEC")) {
            execTime = calculateExecutionTimeReMEC(computingNode, task);
        } else{
            execTime = calculateExecutionTimeGeneric(computingNode, task);
        }

        return execTime;
    }

    public static double calculateExecutionTimeGeneric(ComputingNode computingNode, Task task){
        double mem_tr_time = 0.0;
        double io_time = 0;
        double cpu_time = 0;
        if(GlobalConfig.getInstance().isModern_arch()) {
            if (task.getCpuType().equals("GPU") && computingNode.isGpuEnabled()) {
                double time = task.getLength() / (computingNode.getGpuMipsPerCore() * computingNode.getNumberOfGPUCores());
                cpu_time = time / getAmdahlsSpeedup(task, computingNode);
            } else if (task.getCpuType().equals("PIM") && computingNode.isPimEnabled()) {
                cpu_time = task.getLength() / computingNode.getPimMips();
            } else {
                cpu_time = task.getLength() / computingNode.getMipsPerCore();
            }
        } else{
            cpu_time = task.getLength() / computingNode.getMipsPerCore();
        }
        if(computingNode.getDataBusBandwidth() > 0) {
            mem_tr_time = task.getMemoryNeed() / computingNode.getDataBusBandwidth();
        }
        if(GlobalConfig.getInstance().isModern_arch()) {
            if( ((TaskNode)task).getTaskType() == TaskNode.TaskType.IO_INTENSIVE && computingNode.isSsdEnabled()) {
                io_time = task.getStorageNeed() * 60 / (100 * computingNode.getSsdReadBw()) //READ operation, 60% read
                        + task.getStorageNeed() * 40 / (100 * computingNode.getSsdWriteBw()); //WRITE operation, 40% write;;
            } else {
                if (computingNode.getReadBandwidth() > 0 && computingNode.getWriteBandwidth() > 0) {
                    io_time = task.getStorageNeed() * 60 / (100 * computingNode.getReadBandwidth()) //READ operation, 60% read
                            + task.getStorageNeed() * 40 / (100 * computingNode.getWriteBandwidth()); //WRITE operation, 40% write;
                }
            }
        } else{
            if (computingNode.getReadBandwidth() > 0 && computingNode.getWriteBandwidth() > 0) {
                io_time = task.getStorageNeed() * 60 / (100 * computingNode.getReadBandwidth()) //READ operation, 60% read
                        + task.getStorageNeed() * 40 / (100 * computingNode.getWriteBandwidth()); //WRITE operation, 40% write;
            }
        }

        double total_latency = 0;
        if(GlobalConfig.getInstance().isModern_arch()) {
            if (task.getCpuType().equals("PIM") && computingNode.isPimEnabled()) {
                total_latency = cpu_time + io_time;
            } else {
                total_latency = cpu_time + io_time + mem_tr_time;
            }
        } else{
            total_latency = cpu_time + io_time + mem_tr_time;
        }
        return total_latency;
    }

    public static double calculateExecutionTimeDelta(ComputingNode computingNode, Task task){
        double mem_tr_time = 0.0;
        double io_time = 0;
        double cpu_time = 0;
        if(GlobalConfig.getInstance().isModern_arch()) {
            if (task.getCpuType().equals("GPU") && computingNode.isGpuEnabled()) {
                double time = task.getLength() / (computingNode.getGpuMipsPerCore() * computingNode.getNumberOfGPUCores());
                cpu_time = time / getAmdahlsSpeedup(task, computingNode);
            } else if (task.getCpuType().equals("PIM") && computingNode.isPimEnabled()) {
                cpu_time = task.getLength() / computingNode.getPimMips();
            } else {
                cpu_time = task.getLength() / computingNode.getMipsPerCore();
            }
        } else{
            cpu_time = task.getLength() / computingNode.getMipsPerCore();
        }
        if(computingNode.getDataBusBandwidth() > 0) {
            mem_tr_time = task.getMemoryNeed() / computingNode.getDataBusBandwidth();
        }
        if(GlobalConfig.getInstance().isModern_arch()) {
            if( ((TaskNode)task).getTaskType() == TaskNode.TaskType.IO_INTENSIVE && computingNode.isSsdEnabled()) {
                io_time = task.getStorageNeed() * 60 / (100 * computingNode.getSsdReadBw()) //READ operation, 60% read
                        + task.getStorageNeed() * 40 / (100 * computingNode.getSsdWriteBw()); //WRITE operation, 40% write;;
            } else {
                if (computingNode.getReadBandwidth() > 0 && computingNode.getWriteBandwidth() > 0) {
                    io_time = task.getStorageNeed() * 60 / (100 * computingNode.getReadBandwidth()) //READ operation, 60% read
                            + task.getStorageNeed() * 40 / (100 * computingNode.getWriteBandwidth()); //WRITE operation, 40% write;
                }
            }
        } else{
            if (computingNode.getReadBandwidth() > 0 && computingNode.getWriteBandwidth() > 0) {
                io_time = task.getStorageNeed() * 60 / (100 * computingNode.getReadBandwidth()) //READ operation, 60% read
                        + task.getStorageNeed() * 40 / (100 * computingNode.getWriteBandwidth()); //WRITE operation, 40% write;
            }
        }

        double total_latency = 0;
        if(GlobalConfig.getInstance().isModern_arch()) {
            if (task.getCpuType().equals("PIM") && computingNode.isPimEnabled()) {
                total_latency = cpu_time + io_time;
            } else {
                total_latency = cpu_time + io_time + mem_tr_time;
            }
        } else{
            total_latency = cpu_time + io_time + mem_tr_time;
        }
        return total_latency;
    }

    public static double calculateExecutionTimeBQProfit(ComputingNode computingNode, Task task){
        double mem_tr_time = 0.0;
        double io_time = 0;
        double cpu_time = 0;

        cpu_time  = task.getLength() / computingNode.getMipsPerCore();

        if(computingNode.getDataBusBandwidth() > 0) {
            mem_tr_time = task.getMemoryNeed() / computingNode.getDataBusBandwidth();
        }
        if(task.getStorageType().equals("SSD") && computingNode.isSsdEnabled()){
            io_time = task.getReadOps() / computingNode.getSsdReadBw() //READ operation, 60% read
                    + task.getWriteOps() / computingNode.getSsdWriteBw(); //WRITE operation, 40% write;;
        } else {
            if(computingNode.getReadBandwidth() > 0 && computingNode.getWriteBandwidth() > 0) {
                io_time = task.getReadOps() / computingNode.getReadBandwidth() //READ operation, 60% read
                        + task.getWriteOps()/ computingNode.getWriteBandwidth(); //WRITE operation, 40% write;
            }
        }

        double total_latency = 0;
        total_latency = cpu_time + io_time + mem_tr_time;

        return total_latency;
    }

    public static double calculateExecutionTimeReMEC(ComputingNode computingNode, Task task) {
        double io_time = 0;

        double cpu_time = task.getLength() / computingNode.getMipsPerCore();

        if(computingNode.getReadBandwidth() > 0 && computingNode.getWriteBandwidth() > 0) {
            io_time = task.getStorageNeed() * 60 / (100 * computingNode.getReadBandwidth()) //READ operation, 60% read
                    + task.getStorageNeed() * 40 / (100 * computingNode.getWriteBandwidth()); //WRITE operation, 40% write;
        }

        double total_latency = cpu_time + io_time;
        return total_latency;
    }

    public static float getAmdahlsSpeedup(Task task, ComputingNode computingNode){
        float p = (float)task.getParallelTaskPct()/(float)100;
        float speedup = (float) (1.0/((1.0-p) + p/(float)computingNode.getNumberOfGPUCores()));
        return speedup;
    }

    public static Integer getRandomInteger(Integer min, Integer max){
        Random r = new Random();
        return r.nextInt((max - min) + 1) + min;
    }

    public static Double calculateDistance(DataCenter dc1, DataCenter dc2){
        double x1 = dc1.getLocation().getXPos();
        double y1 = dc1.getLocation().getYPos();
        double x2 = dc2.getLocation().getXPos();
        double y2 = dc2.getLocation().getYPos();
        double distance = Math.sqrt(Math.pow((x2-x1), 2) + Math.pow((y2-y1), 2));
        return distance;
    }

    public static double getDataRate(double bw){
        //SimulationParameters.MAN_BANDWIDTH_BITS_PER_SECOND
        float leftLimit = 0.2F;
        float rightLimit = 0.7F;
        float generatedFloat = leftLimit + new Random().nextFloat() * (rightLimit - leftLimit);
        return 2*bw*generatedFloat;
    }

    public static double calculatePropagationDelay(double distance){
        return distance*10/300000000;
    }

    public static double getManEdgeTransmissionLatency(double bits){
        double dataRate = Helper.getDataRate(SimulationParameters.MAN_BANDWIDTH_BITS_PER_SECOND);
        return bits/dataRate;
    }

    public static double getManCloudTransmissionLatency(double bits){
        double dataRate = Helper.getDataRate(SimulationParameters.WAN_BANDWIDTH_BITS_PER_SECOND);
        return bits/dataRate;
    }

    public static double getWirelessTransmissionLatency(double bits){
        double dataRate = Helper.getDataRate(SimulationParameters.WIFI_BANDWIDTH_BITS_PER_SECOND);
        return bits/dataRate;
    }

    public static double calculateExecutionTime_new(ComputingNode computingNode, Task task){
        double io_time = 0;
        double cpu_time = 0;

        cpu_time  = task.getLength() / computingNode.getMipsPerCore();

        io_time = task.getStorageNeed() * 60 / (100 * computingNode.getReadBandwidth()) //READ operation, 60% read
                + task.getStorageNeed() * 40 / (100 * computingNode.getWriteBandwidth()); //WRITE operation, 40% write;

        double total_latency = cpu_time + io_time;
        return total_latency;
    }

    public static List<Integer> getListofRandomNumberInRange(Integer min, Integer max, Integer n){
        Set<Integer> hash_set= new HashSet<Integer>();
        while(hash_set.size() < n){
            long number = getRandomInteger(min, max);
            if(!hash_set.contains(number)){
                hash_set.add((int) number);
            }
        }
        List<Integer> number_list = new ArrayList<>(hash_set);
        return number_list;
    }

    public static double getRandomDouble(double min, double max){
        Random rand = new Random();
        return rand.nextDouble() * (max - min) + min;
    }

    public static double calculateRemoteEnergyConsumption(TaskNode task){
        /**
         if ("cellular".equals(connectivity)) {
         transmissionEnergyPerBits = SimulationParameters.CELLULAR_DEVICE_TRANSMISSION_WATTHOUR_PER_BIT;
         receptionEnergyPerBits = SimulationParameters.CELLULAR_DEVICE_RECEPTION_WATTHOUR_PER_BIT;
         } else if ("wifi".equals(connectivity)) {
         transmissionEnergyPerBits = SimulationParameters.WIFI_DEVICE_TRANSMISSION_WATTHOUR_PER_BIT;
         receptionEnergyPerBits = SimulationParameters.WIFI_DEVICE_RECEPTION_WATTHOUR_PER_BIT;
         } else {
         transmissionEnergyPerBits = SimulationParameters.ETHERNET_WATTHOUR_PER_BIT / 2;
         receptionEnergyPerBits = SimulationParameters.ETHERNET_WATTHOUR_PER_BIT / 2;
         }
         */
        return task.getFileSize()*SimulationParameters.WIFI_DEVICE_TRANSMISSION_WATTHOUR_PER_BIT;
    }

    public static double dynamicEnergyConsumption(double taskLength, double mipsCapacity, double mipsRequirement) {
        double latency = taskLength / mipsCapacity;
        double cpuEnergyConsumption = (0.01 * (mipsRequirement * mipsRequirement) / (mipsCapacity * mipsCapacity) * latency) / 3600; //alpha = 0.01
        return cpuEnergyConsumption;
    }

    public static double calculateLocalLatency(Task task) {
        return Helper.calculateExecutionTime(task.getEdgeDevice(), task);
    }

    public static double calculateTransmissionLatency(Task task, ComputingNode computingNode) {
        double distance = task.getEdgeDevice().getMobilityModel().distanceTo(computingNode);
        double upload_latency = Helper.getWirelessTransmissionLatency(task.getFileSize()) + Helper.calculatePropagationDelay(distance)+Helper.getManCloudTransmissionLatency(task.getFileSize());;
        double download_latency = Helper.getWirelessTransmissionLatency(task.getOutputSize()) + Helper.calculatePropagationDelay(distance)+Helper.getManCloudTransmissionLatency(task.getOutputSize());
        return upload_latency + download_latency;
    }

    public static double calculateLocalLatency(Task task, double frequency){
        double io_time = 0;

        double cpu_time = task.getLength() / frequency;
        if(task.getEdgeDevice().getReadBandwidth() > 0 && task.getEdgeDevice().getWriteBandwidth() > 0) {
            io_time = task.getStorageNeed() * 60 / (100 * task.getEdgeDevice().getReadBandwidth()) //READ operation, 60% read
                    + task.getStorageNeed() * 40 / (100 * task.getEdgeDevice().getWriteBandwidth()); //WRITE operation, 40% write;
        }

        double total_latency = cpu_time + io_time;
        return total_latency;
    }

    public static double calculateAverageRemoteLatency(Task task, ComputingNode mec, ComputingNode cloud) {
        double mec_ex_time = Helper.calculateExecutionTime(mec, task);
        double cloud_ex_time = Helper.calculateExecutionTime(cloud, task);

        double mec_trans_time = calculateTransmissionLatency(task, mec);
        double cloud_trans_time = calculateTransmissionLatency(task, mec);

        return ((mec_ex_time + 10*mec_trans_time) + (cloud_ex_time + 100*cloud_trans_time)) / 2.0;
    }

    public static int poisson(double mean) {
        double L = Math.exp(-mean);
        int k = 0;
        double p = 1.0;
        Random random = new Random();
        do {
            k++;
            p *= random.nextDouble();
        } while (p > L);
        return k - 1;
    }

    public static double calculateAverageLatency(Task task, ComputingNode local, ComputingNode mec, ComputingNode cloud) {
        double local_ex_time = Helper.calculateExecutionTime(local, task);
        double mec_ex_time = Helper.calculateExecutionTime(mec, task);
        double cloud_ex_time = Helper.calculateExecutionTime(cloud, task);

        double mec_trans_time = Helper.calculateTransmissionLatency(task, mec);
        double cloud_trans_time = Helper.calculateTransmissionLatency(task, mec);

        return (local_ex_time + (mec_ex_time + mec_trans_time) + (cloud_ex_time + cloud_trans_time)) / 3.0;
    }

    public static double getCharge(double taskLength, double io_ops){
        double cpu = 0.005;
        double io = 0.0005;
        return cpu*taskLength + io*io_ops;
    }

    public static float generateRandomFloat(float min, float max) {
        if (min >= max) {
            throw new IllegalArgumentException("Max must be greater than min");
        }

        Random random = new Random();
        return min + random.nextFloat() * (max - min);
    }

    public static Double getBudget(Map<Integer, TaskNode> tMap, ChargingPlan chargingPlan) {
        Double budget = 0.0;
        for(Map.Entry<Integer, TaskNode> task : tMap.entrySet()){
            TaskNode taskNode = task.getValue();
            budget += taskNode.getLength()*chargingPlan.getChargePerMi() +
                    (taskNode.getMemoryNeed()/1024)*chargingPlan.getChargePerMemoryMB() +
                    (taskNode.getWriteOps() + taskNode.getReadOps())*chargingPlan.getChargePerIO();
        }
        return budget* Helper.getRandomDouble(0.6, 0.8); //assuming 70% budget
    }
}

