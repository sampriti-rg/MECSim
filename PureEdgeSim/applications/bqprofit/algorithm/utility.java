package applications.bqprofit.algorithm;

import com.mechalikh.pureedgesim.datacentersmanager.ComputingNode;
import com.mechalikh.pureedgesim.datacentersmanager.DataCenter;
import com.mechalikh.pureedgesim.datacentersmanager.DataCentersManager;
import com.mechalikh.pureedgesim.taskgenerator.Task;
import common.Helper;
import dag.TaskNode;

public class utility {
    public static double getTaskExecutionCost(TaskNode taskNode, ComputingNode computingNode){
        double cpu_cost = taskNode.getLength() * computingNode.getCostPlan().getCostPerMi();
        double memory_cost = (taskNode.getMemoryNeed()/1024)*computingNode.getCostPlan().getCostPerMB();
        double io_cost = (taskNode.getReadOps() + taskNode.getWriteOps())*computingNode.getCostPlan().getCostPerIo();

        return cpu_cost + memory_cost + io_cost;
    }

    public static double calculateTransmissionLatency(Task task, ComputingNode computingNode){
        double distance = task.getEdgeDevice().getMobilityModel().distanceTo(computingNode);
        double upload_latency = Helper.getWirelessTransmissionLatency(task.getFileSize()) + Helper.calculatePropagationDelay(distance);
        double download_latency = Helper.getWirelessTransmissionLatency(task.getOutputSize()) + Helper.calculatePropagationDelay(distance);
        return upload_latency + download_latency;
    }

    public static double calculateExecutionTime_new(ComputingNode computingNode, Task task){
        double io_time = 0;
        double cpu_time = 0;
        double mem_tr_time = 0;

        cpu_time  = task.getLength() / computingNode.getMipsPerCore();
        TaskNode taskNode = (TaskNode) task;
        if(computingNode.getReadBandwidth() > 0){
            io_time += taskNode.getReadOps()/computingNode.getReadBandwidth();
        }

        if(computingNode.getWriteBandwidth() > 0){
            io_time += taskNode.getWriteOps()/computingNode.getWriteBandwidth();
        }

        if(computingNode.getDataBusBandwidth() > 0) {
            mem_tr_time = task.getMemoryNeed() / computingNode.getDataBusBandwidth();
        }

        double total_latency = cpu_time + io_time + mem_tr_time;
        //System.out.println(cpu_time + " : " +  io_time + " : " + total_latency);
        return total_latency;
    }

    public static double calculateLatency(TaskNode taskNode, ComputingNode computingNode,
                                          DataCenter nearestDc, boolean isCloud){
        double transmission_latency = utility.calculateTransmissionLatency(taskNode, computingNode);
        double mecComputingTime = calculateExecutionTime_new(computingNode, taskNode) + transmission_latency;

        double distance = computingNode.getMobilityModel().distanceTo(nearestDc.nodeList.get(0));
        double upload_latency = 0.;
        double download_latency = 0;
        if(isCloud) {
            upload_latency = Helper.getManCloudTransmissionLatency(taskNode.getFileSize()) + Helper.calculatePropagationDelay(distance);
            download_latency =Helper.getManCloudTransmissionLatency(taskNode.getOutputSize()) + Helper.calculatePropagationDelay(distance);
            upload_latency *=500;
            download_latency *= 500;
        } else{
            upload_latency = Helper.getManEdgeTransmissionLatency(taskNode.getFileSize()) + Helper.calculatePropagationDelay(distance);
            download_latency = Helper.getManEdgeTransmissionLatency(taskNode.getOutputSize()) + Helper.calculatePropagationDelay(distance);
        }
        return mecComputingTime + upload_latency + download_latency;
    }

    public static DataCenter getNearestDC(TaskNode taskNode, DataCentersManager dataCentersManager){
        DataCenter nearestDc = null;
        double distance = Double.MAX_VALUE;
        for(DataCenter dc : dataCentersManager.getEdgeDatacenterList()) {
            double dist = taskNode.getEdgeDevice().getMobilityModel().distanceTo(dc.nodeList.get(0));
            if(dist < distance){
                nearestDc = dc;
                distance = dist;
            }
        }

        return nearestDc;
    }
}
