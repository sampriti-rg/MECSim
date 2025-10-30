package applications.rmeac;

import com.mechalikh.pureedgesim.datacentersmanager.ComputingNode;
import com.mechalikh.pureedgesim.datacentersmanager.DataCenter;
import com.mechalikh.pureedgesim.simulationmanager.SimulationManager;
import common.Helper;
import dag.TaskNode;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;

public class OffloadingOptimizer {
    // Constants
    private static final int POPULATION_SIZE = 100;
    private static final int MAX_GENERATIONS = 100;
    private static final double MUTATION_RATE = 0.05; // 5% chance of mutation per variable

    // Data Structures (you'll need to define these based on your specific model)
    private SimulationManager simulationManager;

    public OffloadingOptimizer(SimulationManager simManager){
        this.simulationManager = simManager;
    }

    public void getDecision(Map<Integer, TaskNode> job){
        List<TaskNode> tasklist = new ArrayList<>();
        for (Map.Entry<Integer, TaskNode> task : job.entrySet()) {
            tasklist.add(task.getValue());
        }
        optimizeOffloading(tasklist);
    }
    // The main optimization method
    List<OffloadingDecision> optimizeOffloading(List<TaskNode> tasklist) {
        List<Individual> population = initializePopulation(tasklist, POPULATION_SIZE);

        for (int generation = 0; generation < MAX_GENERATIONS; generation++) {
            evaluateFitness(population);

            List<Individual> selectedIndividuals = selectIndividuals(population);

            mutatePopulation(selectedIndividuals);

            population = selectedIndividuals;
        }

        return decodeBestSolution(getBestIndividual(population));
    }

    // Helper functions

    // Initialize the population (randomly assign edge servers to tasks)
    private List<Individual> initializePopulation(List<TaskNode> tasklist, int populationSize) {
        List<Individual> population = new ArrayList<>();
        Random random = new Random();

        for (int i = 0; i < populationSize; i++) {
            Individual individual = new Individual();
            for(TaskNode taskNode : tasklist){
                int rand_off = Helper.getRandomInteger(0, 10);
                if(rand_off == 0) {
                    individual.addOffloadingDecision(new OffloadingDecision(taskNode,0, 0, 1));
                } else{
                    rand_off = Helper.getRandomInteger(0, simulationManager.getDataCentersManager().getEdgeDatacenterList().size());
                    if(rand_off == simulationManager.getDataCentersManager().getEdgeDatacenterList().size()){
                        individual.addOffloadingDecision(new OffloadingDecision(taskNode,0, 0, 2));
                    } else{
                        int server_index = Helper.getRandomInteger(0, simulationManager.getDataCentersManager().getEdgeDatacenterList().get(rand_off).nodeList.size()-1);
                        individual.addOffloadingDecision(new OffloadingDecision(taskNode, rand_off, server_index, 0));
                    }
                }
            }
            population.add(individual);
        }
        return population;
    }

    // Evaluate the fitness of each individual in the population
    private void evaluateFitness(List<Individual> population) {
        for (Individual individual : population) {
            // Calculate the fitness score (e.g., using your fitness function)
            individual.setFitness(calculateFitness(individual.getOffloadingDecisions()));
        }
    }

    private double calculateFitness(List<OffloadingDecision> decisions) {
        double totalExecutionTime = 0;
        double totalEnergyConsumption = 0;
        double loadBalance = 0;

        for(OffloadingDecision decision : decisions){
            totalExecutionTime += calculateExecutionTime(decision);
            totalEnergyConsumption += calculateEnergyConsumption(decision);
            loadBalance += calculateLoadBalance(decision);
        }

        // 5. Combine these values (e.g., using weighted sum or other method) to get a fitness score
        double fitness = 0.5 * totalExecutionTime + 0.3 * totalEnergyConsumption + 0.1 * loadBalance;// - 0.1 * reliability;
        // You can adjust the weights based on your priorities

        return fitness;
    }

    private double calculateExecutionTime(OffloadingDecision offloadingDecision){
        ComputingNode computingNode = null;
        double transmission_time = 0;
        if(offloadingDecision.getDecision() == 1){
            computingNode = offloadingDecision.getTaskNode().getEdgeDevice();
        } else if(offloadingDecision.getDecision() == 0){
            computingNode = simulationManager.getDataCentersManager().getEdgeDatacenterList().get(offloadingDecision.getDcIndex()).nodeList.get(offloadingDecision.getServerIndex());
            transmission_time = Helper.calculateTransmissionLatency(offloadingDecision.getTaskNode(), computingNode);
        } else{
            computingNode = simulationManager.getDataCentersManager().getCloudDatacentersList().get(0).nodeList.get(0);
            transmission_time = Helper.calculateTransmissionLatency(offloadingDecision.getTaskNode(), computingNode);
        }

        double cpu_time = Helper.calculateExecutionTime(computingNode, offloadingDecision.getTaskNode());
        return cpu_time + transmission_time;
    }

    private double calculateLoadBalance(OffloadingDecision decision) {
        double totalUtilization = 0;
        double loadBalance = 0;
        int totalServer = 0;
        if (decision.getDecision() == 0) {
            for (DataCenter dc : simulationManager.getDataCentersManager().getEdgeDatacenterList()) {
                for (ComputingNode computingNode : dc.nodeList) {
                    totalUtilization += computingNode.getAvgCpuUtilization();
                    totalServer++;
                }
            }

            double averageUtilization = totalUtilization / totalServer;

            double sumSquaredDifferences = 0;
            if (decision.getDecision() == 0) {
                for (DataCenter dc : simulationManager.getDataCentersManager().getEdgeDatacenterList()) {
                    for (ComputingNode computingNode : dc.nodeList) {
                        double difference = computingNode.getAvgCpuUtilization() - averageUtilization;
                        sumSquaredDifferences += difference * difference;
                    }
                }
            }


            loadBalance = Math.sqrt(sumSquaredDifferences / totalServer);
        }
        return loadBalance;
    }

    private double calculateEnergyConsumption(OffloadingDecision offloadingDecision){
        double energy = 0;
        if(offloadingDecision.getDecision() == 1){
            ComputingNode computingNode = offloadingDecision.getTaskNode().getEdgeDevice();
            energy = Helper.dynamicEnergyConsumption(offloadingDecision.getTaskNode().getLength(), computingNode.getMipsPerCore(), computingNode.getMipsPerCore());
        } else if(offloadingDecision.getDecision() == 0){
            energy = Helper.calculateRemoteEnergyConsumption(offloadingDecision.getTaskNode());
        } else{
            energy = Helper.calculateRemoteEnergyConsumption(offloadingDecision.getTaskNode());
        }

        return energy;
    }

    // Select individuals for the next generation using tournament selection
    private List<Individual> selectIndividuals(List<Individual> population) {
        List<Individual> selectedIndividuals = new ArrayList<>();
        Random random = new Random();
        int tournamentSize = 5; // You can adjust this parameter

        for (int i = 0; i < POPULATION_SIZE; i++) {
            // 1. Randomly select tournamentSize individuals
            List<Individual> tournamentParticipants = new ArrayList<>();
            for (int j = 0; j < tournamentSize; j++) {
                int randomIndex = random.nextInt(population.size());
                tournamentParticipants.add(population.get(randomIndex));
            }

            // 2. Find the individual with the highest fitness
            Individual bestIndividual = tournamentParticipants.get(0);
            for (Individual individual : tournamentParticipants) {
                if (individual.getFitness() < bestIndividual.getFitness()) {
                    bestIndividual = individual;
                }
            }

            // 3. Add the best individual to the selectedIndividuals list
            selectedIndividuals.add(bestIndividual);
        }

        return selectedIndividuals;
    }

    // Apply mutation to the selected individuals
    private void mutatePopulation(List<Individual> population) {
        Random random = new Random();

        for (Individual individual : population) {
            // Apply a random mutation to each variable in the individual's representation
            for (int i = 0; i < individual.getOffloadingDecisions().size(); i++) {
                if (random.nextDouble() < MUTATION_RATE) {
                    OffloadingDecision offloadingDecision = individual.getOffloadingDecisions().get(i);
                    // Change the edge server assignment for the task
                    int rand_off = Helper.getRandomInteger(0, 2);
                    if(rand_off == 0) {
                        individual.setOffloadingDecision(i, new OffloadingDecision(offloadingDecision.getTaskNode(), 0, 0, 1));
                    } else{
                        rand_off = Helper.getRandomInteger(0, simulationManager.getDataCentersManager().getEdgeDatacenterList().size());
                        if(rand_off == simulationManager.getDataCentersManager().getEdgeDatacenterList().size()){
                            individual.setOffloadingDecision(i, new OffloadingDecision(offloadingDecision.getTaskNode(),0, 0, 2));
                        } else{
                            int server_index = Helper.getRandomInteger(0, simulationManager.getDataCentersManager().getEdgeDatacenterList().get(rand_off).nodeList.size()-1);
                            individual.setOffloadingDecision(i, new OffloadingDecision(offloadingDecision.getTaskNode(), rand_off, server_index, 0));
                        }
                    }
                }
            }
        }
    }

    // Get the best individual in the population
    private Individual getBestIndividual(List<Individual> population) {
        Individual bestIndividual = population.get(0);
        for (Individual individual : population) {
            if (individual.getFitness() < bestIndividual.getFitness()) {
                bestIndividual = individual;
            }
        }
        return bestIndividual;
    }

    // Decode the best solution (convert it back to a list of OffloadingDecisions)
    private List<OffloadingDecision> decodeBestSolution(Individual bestIndividual) {
        // ... Implement the decoding logic here (convert the individual's representation back into OffloadingDecision objects)
        return bestIndividual.getOffloadingDecisions();
    }
}

