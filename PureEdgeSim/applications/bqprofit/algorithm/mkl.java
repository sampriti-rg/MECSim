package applications.bqprofit.algorithm;

import com.mechalikh.pureedgesim.datacentersmanager.ComputingNode;
import com.mechalikh.pureedgesim.datacentersmanager.DataCenter;
import com.mechalikh.pureedgesim.scenariomanager.SimulationParameters;
import com.mechalikh.pureedgesim.simulationmanager.SimulationManager;
import dag.TaskNode;

import java.util.*;

public class mkl {
    Map<Integer, TaskNode> dag;
    List<TaskCost> remotelist;
    List<TaskCost> localList;
    TreeMap<Double, TaskCost> gainMap;
    SimulationManager simulationManager;
    double budget;
    public mkl(Map<Integer, TaskNode> job, SimulationManager simManager, double budget){
        dag = job;
        simulationManager = simManager;
        this.budget = budget;
        gainMap = new TreeMap<>(Collections.reverseOrder());
        localList = new ArrayList<>();
        remotelist = new ArrayList<>();
    }

    public List<TaskCost> getLocalList() {
        return localList;
    }

    public List<TaskCost> getRemotelist() {
        return remotelist;
    }
    public void partition(){
        DataCenter dc = simulationManager.getDataCentersManager().getEdgeDatacenterList().get(0);
        ComputingNode computingNode = dc.nodeList.get(1);
        this.initialPartition();
        while (true) {
            boolean continue_loop = false;
            double curr_cost = totalCost(computingNode);

            for (TaskCost taskCost : localList) {
                double gain = calculateGain(curr_cost, taskCost, computingNode);
                gainMap.put(gain, taskCost);
            }

            double serviceProviderCost = 0;
            for (TaskCost taskCost : remotelist) {
                serviceProviderCost += calculateServiceProviderCharge(taskCost.getTaskNode());
            }

            double oldCost = curr_cost;
            for (Map.Entry<Double, TaskCost> entry : gainMap.entrySet()) {
                TaskCost taskCost = entry.getValue();
                if(isAvailable(localList, taskCost)) {
                    double new_task_cost = totalCost(computingNode) + this.calculateRemoteCost(taskCost.getTaskNode(), computingNode) -
                            this.getWeightedCost(taskCost.getLatency(), taskCost.getEnergy());
                    double new_svc_provider_charge = serviceProviderCost + calculateServiceProviderCharge(taskCost.getTaskNode());
                    if (new_svc_provider_charge < budget && new_task_cost < oldCost) {
                        localList.remove(taskCost);
                        remotelist.add(taskCost);
                        oldCost = new_task_cost;
                        continue_loop = true;
                    }
                }
            }

            if(!continue_loop){
                //No further improvement
                break;
            }
        }

        this.allocateBudget();
    }

    boolean isAvailable(List<TaskCost> tasklist, TaskCost localCost){
        for(TaskCost task : tasklist){
            if(task == localCost){
                return true;
            }
        }

        return  false;
    }

    private void allocateBudget(){
        for(TaskCost localCost : remotelist){
            double charge = calculateServiceProviderCharge(localCost.getTaskNode());
            localCost.getTaskNode().setBudget(charge);
        }
    }

    private double calculateServiceProviderCharge(TaskNode taskNode){
        double extra_cost = 0;
        double cpu_cost = taskNode.getLength() * taskNode.getChargingPlan().getChargePerMi();
        double memory_cost = (taskNode.getMemoryNeed()/1024)*taskNode.getChargingPlan().getChargePerMemoryMB();
        double io_cost = (taskNode.getReadOps() + taskNode.getWriteOps())*taskNode.getChargingPlan().getChargePerIO();
        Double total_cost = cpu_cost + memory_cost + io_cost;

        if(taskNode.getDeadlineType().equals("hard")){
            extra_cost += total_cost*taskNode.getChargingPlan().getHard_deadline_surcharge();
        }

        if(taskNode.isPeakTimeOffload()){
            extra_cost += total_cost*taskNode.getChargingPlan().getHigh_demand_surcharge();
        }

        return total_cost + extra_cost;
    }

    private double calculateGain(double curr_cost, TaskCost  taskCost, ComputingNode computingNode){
        double remote_cost = this.calculateRemoteCost(taskCost.getTaskNode(), computingNode);
        //Since we are trying to move node from local list to remote list, so curr task cost is nothing but the local cost
        double new_cost = curr_cost - this.getWeightedCost(taskCost.getLatency(), taskCost.getEnergy()) + remote_cost;
        //this.calculateLocalCost(taskNode) + remote_cost;

        return new_cost;
    }

    private double totalCost(ComputingNode computingNode){
        double total_cost = 0;
        for (TaskCost localCost : localList){
            total_cost += this.getWeightedCost(localCost.getLatency(), localCost.getEnergy());
        }
        for (TaskCost cost : remotelist){
            total_cost += this.getWeightedCost(cost.getLatency(), cost.getEnergy());
        }

        return total_cost;
    }

    private void initialPartition(){
        double queue_time = 0;
        Double cores = 0.0;
        DataCenter dc = simulationManager.getDataCentersManager().getEdgeDatacenterList().get(0);
        ComputingNode computingNode = dc.nodeList.get(1);
        if(!dag.isEmpty()) {
            double energy_remain = 0.0;
            Map.Entry<Integer, TaskNode> entry = dag.entrySet().iterator().next();
            energy_remain = entry.getValue().getEdgeDevice().getEnergyModel().getBatteryLevel();
            cores = entry.getValue().getEdgeDevice().getNumberOfCPUCores();

            for (Map.Entry<Integer, TaskNode> task : dag.entrySet()) {
                TaskNode taskNode = task.getValue();
                TaskCost taskLocalCost = new TaskCost();
                taskLocalCost.setTaskNode(taskNode);
                double latency = 0;
                double energy_need = 0;
                if (taskNode.getEdgeDevice().isSensor()) {
                    latency = this.calculateRemoteLatency(taskNode, computingNode);
                    energy_need = this.calculateRemoteEnergy(taskNode, computingNode);
                    taskLocalCost.setLatency(latency);
                    taskLocalCost.setEnergy(energy_need);
                    remotelist.add(taskLocalCost);
                } else{
                    latency = this.calculateLatency(taskNode) + getQueueTime(localList)/cores;
                    energy_need = this.calulateEnergyNeed(taskNode, latency);
                    if (latency < taskNode.getMaxLatency() && energy_need < energy_remain) {
                        taskLocalCost.setLatency(latency);
                        taskLocalCost.setEnergy(energy_need);
                        localList.add(taskLocalCost);
                        energy_remain -= energy_need;
                    } else {
                        latency = this.calculateRemoteLatency(taskNode, computingNode);
                        energy_need = this.calculateRemoteEnergy(taskNode, computingNode);
                        taskLocalCost.setLatency(latency);
                        taskLocalCost.setEnergy(energy_need);
                        remotelist.add(taskLocalCost);
                    }
                }
                //task.getValue().getMemoryNeed() < taskNode.getEdgeDevice().
            }
        }
    }

    private double getQueueTime(List<TaskCost> taskLocalCosts){
        double qtime = 0;
        for(TaskCost taskLocalCost : taskLocalCosts){
            qtime += taskLocalCost.getLatency();
        }

        return qtime;
    }

    private double calculateRemoteCost(TaskNode taskNode, ComputingNode computingNode){
        double mecComputingTime = calculateRemoteLatency(taskNode, computingNode);
        double remote_energy = calculateRemoteEnergy(taskNode, computingNode);
        return mecComputingTime + remote_energy;
    }

    private double calculateRemoteLatency(TaskNode taskNode, ComputingNode computingNode){
        double transmission_latency = utility.calculateTransmissionLatency(taskNode, computingNode);
        double mecComputingTime = utility.calculateExecutionTime_new(computingNode, taskNode) + transmission_latency;
        return  mecComputingTime;
    }

    private double calculateRemoteEnergy(TaskNode taskNode, ComputingNode computingNode){
        double remote_energy = SimulationParameters.CELLULAR_DEVICE_TRANSMISSION_WATTHOUR_PER_BIT*taskNode.getFileSize() +
                SimulationParameters.CELLULAR_DEVICE_RECEPTION_WATTHOUR_PER_BIT*taskNode.getOutputSize();
        return remote_energy;
    }

    private double calculateLocalCost(TaskNode taskNode){
        double latency = calculateLatency(taskNode);
        double energy = calulateEnergyNeed(taskNode, latency);

        return latency + energy;
    }

    private double calculateLatency(TaskNode task){
        //We are considering only CPU for mobile device
        double cpu_time  = task.getLength() / task.getEdgeDevice().getMipsPerCore();
        return cpu_time;
    }

    private double calulateEnergyNeed(TaskNode task, double latency){
        		/*
		@article{kim2011power,
  			title={Power-aware provisioning of virtual machines for real-time Cloud services},
  			author={Kim, Kyong Hoon and Beloglazov, Anton and Buyya, Rajkumar},
  			journal={Concurrency and Computation: Practice and Experience},
  			volume={23},
  			number={13},
  			pages={1491--1505},
  			year={2011},
  			publisher={Wiley Online Library}
		}
		E = alpha * t * s^2 , t = execution time, s = f/f_max, alpha = constant
		* */
        double energy_need = (0.01 * (task.getLength()*task.getLength())/(task.getEdgeDevice().getMipsPerCore()*task.getEdgeDevice().getMipsPerCore())*latency)/3600; //alpha = 0.01;
        return energy_need;
    }

    double getWeightedCost(double latency, double energy){
        double x = 0.7;
        double y = 0.3;
        return x*latency + y*energy;
    }
}
