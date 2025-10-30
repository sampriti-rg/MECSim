package applications.bqprofit.algorithm;

import com.mechalikh.pureedgesim.datacentersmanager.ComputingNode;
import com.mechalikh.pureedgesim.datacentersmanager.DataCenter;
import com.mechalikh.pureedgesim.simulationmanager.SimulationManager;
import common.Helper;
import dag.TaskNode;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class RaGeneticAlgorithm {
    List<TaskNode> taskList;
    //static int MAX_GENERATION = 65;
    //static int MAX_GENERATION = 24;
    //static int POPULATION_SIZE = 100;
    static int MAX_GENERATION = 24;
    static int POPULATION_SIZE = 50;
    static int TOURNAMENT_SIZE = 4;
    List<Gene>[] population;
    SimulationManager simulationManager;
    int dc_size;
    public RaGeneticAlgorithm(SimulationManager simManager, List<TaskNode> tasklist){
        taskList = tasklist;
        simulationManager = simManager;
        dc_size = simulationManager.getDataCentersManager().getEdgeDatacenterList().size()+1; //1 is for cloud DC
        population = new ArrayList[POPULATION_SIZE];
    }

    public List<Gene> mapTaskToServer(){
        this.initPopulation();
        for(int generation =1; generation < MAX_GENERATION; generation++){
            List<Gene> fittestIndividual = getFittestIndividual();
            List<Gene>[] newPopulation = createNewPopulation(taskList.size());

            newPopulation[0] = fittestIndividual;
            for (int i = 1; i < POPULATION_SIZE; i++) {
                List<Gene> parent1 = rouletteWheelSelection();
                List<Gene> parent2 = rouletteWheelSelection();
                List<Gene> offspring = crossover(parent1, parent2);
                mutate(offspring);
                newPopulation[i] = offspring;
            }

            population = newPopulation;
        }

        return getFittestIndividual();
    }

    private void initPopulation(){
        int col_size = taskList.size();
        for(int i=0; i<POPULATION_SIZE; i++){
            List<Gene> row = new ArrayList<>();
            for(int j=0; j<col_size; j++){
                int dc_index = (int) Helper.getRandomInteger(0, dc_size-1);
                DataCenter dc = null;
                if(dc_index >= (dc_size -1)){
                    dc = simulationManager.getDataCentersManager().getCloudDatacentersList().get(0);
                }else {
                    dc = simulationManager.getDataCentersManager().getEdgeDatacenterList().get(dc_index);
                }

                int node_size = dc.nodeList.size();
                int node_index = (int) Helper.getRandomInteger(0, node_size-1);
                Gene node = new Gene(dc_index, node_index);

                row.add(node);
            }
            population[i] = row;
        }
    }

    private List<Gene> getFittestIndividual() {
        List<Gene> fittestIndividual = population[0];
        double fittestFitness = calculateFitness(fittestIndividual);
        for (int i = 1; i < POPULATION_SIZE; i++) {
            List<Gene> individual = population[i];
            double fitness = calculateFitness(individual);
            if (fitness < fittestFitness) {
                fittestIndividual = individual;
                fittestFitness = fitness;
            }
        }
        return fittestIndividual;
    }

    private double calculateFitness(List<Gene> individual) {
        double fitness = 0;
        int numTasks = individual.size();
        for (int i = 0; i < numTasks; i++) {
            DataCenter dataCenter = null;
            boolean isCloud = false;
            Gene gene = individual.get(i);
            if(gene.getDcIndex() >= (dc_size-1)){
                dataCenter = simulationManager.getDataCentersManager().getCloudDatacentersList().get(0);
                isCloud = true;
            } else{
                dataCenter = simulationManager.getDataCentersManager().getEdgeDatacenterList().get(gene.getDcIndex());
            }

            ComputingNode computingNode = dataCenter.nodeList.get(gene.getServerIndex());
            TaskNode taskNode = taskList.get(i);
            double cost = utility.getTaskExecutionCost(taskNode, computingNode);
            fitness += cost;

            //checking the constraint
            DataCenter nearestDc = utility.getNearestDC(taskNode, simulationManager.getDataCentersManager());
            Double latency = utility.calculateLatency(taskNode, computingNode, nearestDc, isCloud);
            if(latency > taskNode.getMaxLatency()){
                fitness += cost; //penalty
            }

            if(isCloud){
                fitness += cost/3;
            }

            /*
             // Check if constraints are violated
            if (cpuUsage > cpuLimits[server - 1] || memoryUsage > memoryLimits[server - 1]) {
                // Penalize individuals that exceed CPU or memory limits
                int cpuPenalty = Math.max(0, cpuUsage - cpuLimits[server - 1]);
                int memoryPenalty = Math.max(0, memoryUsage - memoryLimits[server - 1]);
                fitness += cpuPenalty + memoryPenalty;
            }
            */
        }
        return fitness;
    }

    private List<Gene>[] createNewPopulation(int col_size){
        List<Gene>[] new_population = new ArrayList[POPULATION_SIZE];
        for(int i=0; i<POPULATION_SIZE; i++) {
            List<Gene> row = new ArrayList<>();
            for(int j=0; j<col_size; j++) {
                Gene gene = new Gene(0, 0);
                row.add(gene);
            }

            new_population[i] = row;
        }

        return new_population;
    }

    private List<Gene> crossover(List<Gene> parent1, List<Gene> parent2){
        List<Gene> offspring = new ArrayList<>();
        List<Double> costVectorP1 = createCostVector(parent1);
        List<Double> costVectorP2 = createCostVector(parent2);
        List<Double> ETVectorP1 = createETVector(parent1);
        List<Double> ETVectorP2 = createETVector(parent2);

        for(int i=0; i < parent1.size(); i++){
            Double executionTimeP1 =  ETVectorP1.get(i);
            Double executionTimeP2 = ETVectorP2.get(i);
            Double latencyDeadline = taskList.get(i).getMaxLatency();

            if(executionTimeP1 > latencyDeadline){
                offspring.add(parent2.get(i));
                continue;
            }

            if(executionTimeP2 > latencyDeadline){
                offspring.add(parent1.get(i));
                continue;
            }

            Double geneCostP1 = costVectorP1.get(i);
            Double geneCostP2 = costVectorP2.get(i);
            if(geneCostP1 < geneCostP2){
                offspring.add(parent1.get(i));
            } else {
                offspring.add(parent2.get(i));
            }
        }
        return offspring;
    }

    private List<Double> createCostVector(List<Gene> parent){
        List<Double> costVector = new ArrayList<>();
        int taskIndex = 0;
        for(Gene gene : parent){
            DataCenter dataCenter = null;
            if(gene.getDcIndex() >= (dc_size - 1)){
                dataCenter = simulationManager.getDataCentersManager().getCloudDatacentersList().get(0);
            } else{
                dataCenter = simulationManager.getDataCentersManager().getEdgeDatacenterList().get(gene.getDcIndex());
            }
            ComputingNode computingNode = dataCenter.nodeList.get(gene.getServerIndex());
            double cost = utility.getTaskExecutionCost(taskList.get(taskIndex), computingNode);
            costVector.add(cost);
            taskIndex++;
        }
        return costVector;
    }

    private List<Double> createETVector(List<Gene> parent){
        List<Double> ETVector = new ArrayList<>();
        int taskIndex = 0;
        for(Gene gene : parent){
            boolean isCloud = false;
            DataCenter dataCenter = null;
            if(gene.getDcIndex() >= (dc_size - 1)){
                dataCenter = simulationManager.getDataCentersManager().getCloudDatacentersList().get(0);
                isCloud = true;
            } else{
                dataCenter = simulationManager.getDataCentersManager().getEdgeDatacenterList().get(gene.getDcIndex());
            }
            ComputingNode computingNode = dataCenter.nodeList.get(gene.getServerIndex());

            DataCenter nearestDc = utility.getNearestDC(taskList.get(taskIndex), simulationManager.getDataCentersManager());
            double latency = utility.calculateLatency(taskList.get(taskIndex), computingNode, nearestDc, isCloud);
            ETVector.add(latency);
            taskIndex++;
        }

        return ETVector;
    }

    private List<Gene> rouletteWheelSelection() {
        int totalFitness = 0;
        for (List<Gene> individual : population) {
            totalFitness += calculateFitness(individual);
        }

        Random random = new Random();
        int selectedValue = totalFitness>0?random.nextInt(totalFitness):random.nextInt(1) + 1;
        int currentSum = 0;

        for (List<Gene> individual : population) {
            currentSum += calculateFitness(individual);
            if (currentSum >= selectedValue) {
                return individual; // Clone to avoid modifying the original individual
            }
        }

        // This should not be reached, but to handle rare cases where rounding errors might occur
        return population[0]; // Return the first individual as a fallback
    }

    // Tournament Selection
    private List<Gene> tournamentSelection() {
        Random random = new Random();
        List<Gene> selectedParent = null;
        double selectedParentFitness = Integer.MAX_VALUE;

        for (int i = 0; i < TOURNAMENT_SIZE; i++) {
            int randomIndex = random.nextInt(POPULATION_SIZE);
            List<Gene> individual = population[randomIndex];
            double fitness = calculateFitness(individual);
            if (fitness < selectedParentFitness) {
                selectedParent = individual; // Clone to avoid modifying the original individual
                selectedParentFitness = fitness;
            }
        }

        return selectedParent;
    }

    private void mutate(List<Gene> offspring){
        int first_gene_index = Helper.getRandomInteger(0, offspring.size()-1);
        int second_gene_index = Helper.getRandomInteger(0, offspring.size()-1);

        Gene first_gene = offspring.get(first_gene_index);
        Gene second_gene = offspring.get(second_gene_index);

        offspring.set(first_gene_index, second_gene);
        offspring.set(second_gene_index, first_gene);
    }
}
