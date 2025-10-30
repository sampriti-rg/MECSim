package applications.bqprofit;

import com.mechalikh.pureedgesim.datacentersmanager.CostPlan;
import dag.TaskNode;

public class CostProfitCalculator {
    double io_profit;
    double cpu_profit;
    double memory_profit;

    double io_cost;
    double cpu_cost;
    double memory_cost;

    double io_revenue;
    double cpu_revenue;

    double memory_revenue;

    double penalty;
    public CostProfitCalculator(){
        io_profit = 0;
        cpu_profit = 0;
        memory_profit = 0;
        penalty = 0;
        io_cost = 0;
        memory_cost = 0;
        cpu_cost = 0;
        io_revenue = 0;
        cpu_revenue = 0;
        memory_revenue = 0;
    }

    public double getIo_profit() {
        return io_profit;
    }

    public double getCpu_profit() {
        return cpu_profit;
    }

    public double getMemory_profit() {
        return memory_profit;
    }

    public double getPenalty() {
        return penalty;
    }
    public double getIo_cost() {
        return io_cost;
    }

    public double getCpu_cost() {
        return cpu_cost;
    }

    public double getMemory_cost() {
        return memory_cost;
    }

    public double getIo_revenue() {
        return io_revenue;
    }

    public double getCpu_revenue() {
        return cpu_revenue;
    }

    public double getMemory_revenue() {
        return memory_revenue;
    }

    public void updateMemoryProfit(Double memor_usage, CostPlan cost_plan, TaskNode taskNode){
        //Assuming memory is in KB
        Double charge = (memor_usage/1024)*(taskNode.getChargingPlan().getChargePerMemoryMB());
        Double peak_surcharge = 0.0;
        Double deadline_surcharge = 0.0;
        memory_cost += (memor_usage/1024)*cost_plan.getCostPerMB();
        memory_revenue = (memor_usage/1024)*taskNode.getChargingPlan().getChargePerMemoryMB();

        if(taskNode.isPeakTimeOffload()){
            peak_surcharge = charge*taskNode.getChargingPlan().getHigh_demand_surcharge();
        }

        if(taskNode.getDeadlineType().equals("hard")){
            deadline_surcharge = charge*taskNode.getChargingPlan().getHard_deadline_surcharge();
        }

        charge += peak_surcharge + deadline_surcharge;
        memory_revenue += charge;
        Double profit = charge - (memor_usage/1024)*cost_plan.getCostPerMB();
        memory_profit += profit;
    }

    public void updateCpuProfit(Double cpu_usage, CostPlan cost_plan, TaskNode taskNode){
        Double peak_surcharge = 0.0;
        Double deadline_surcharge = 0.0;
        Double charge = cpu_usage * (taskNode.getChargingPlan().getChargePerMi());
        cpu_cost += cpu_usage * cost_plan.getCostPerMi();

        //System.out.println("Id: "+taskNode.getId()+" chargeMI: " + taskNode.getChargingPlan().getChargePerMi()+" cpu usage: "+ cpu_usage);
        if(taskNode.isPeakTimeOffload()){
            peak_surcharge = charge*taskNode.getChargingPlan().getHigh_demand_surcharge();
        }

        if(taskNode.getDeadlineType().equals("hard")){
            deadline_surcharge = charge*taskNode.getChargingPlan().getHard_deadline_surcharge();
        }

        charge += peak_surcharge + deadline_surcharge;
        cpu_revenue += charge;
        Double profit = charge - cpu_usage * cost_plan.getCostPerMi();
        cpu_profit += profit;
    }

    public void updateIoProfit(Double io_usage, CostPlan cost_plan, TaskNode taskNode){
        Double peak_surcharge = 0.0;
        Double deadline_surcharge = 0.0;
        Double charge = io_usage * (taskNode.getChargingPlan().getChargePerIO());
        io_cost += io_usage * cost_plan.getCostPerIo();
        //io_revenue += io_usage * taskNode.getChargingPlan().getChargePerIO();
        if(taskNode.isPeakTimeOffload()){
            peak_surcharge = charge*taskNode.getChargingPlan().getHigh_demand_surcharge();
        }

        if(taskNode.getDeadlineType().equals("hard")){
            deadline_surcharge =  charge*taskNode.getChargingPlan().getHard_deadline_surcharge();
        }

        charge += peak_surcharge + deadline_surcharge;
        io_revenue += charge;
        Double profit = charge - cost_plan.getCostPerIo();
        io_profit += profit;
    }

    public void updatePenalty(Double penalty){
        this.penalty += penalty;
    }
}
