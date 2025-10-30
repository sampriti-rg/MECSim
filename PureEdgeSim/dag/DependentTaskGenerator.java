package dag;

import com.mechalikh.pureedgesim.simulationmanager.SimulationManager;
import com.mechalikh.pureedgesim.taskgenerator.Task;
import com.mechalikh.pureedgesim.taskgenerator.TaskGenerator;
import common.DataProcessor;

import java.util.List;

public class DependentTaskGenerator extends TaskGenerator {
    private double simulationTime;
    SimulationManager simulationManager = null;
    public DependentTaskGenerator(SimulationManager simulationManager) {
        super(simulationManager);
        this.simulationManager = simulationManager;
    }
    @Override
    public List<Task> generate() {
        DataProcessor dataProcessor = new DataProcessor(getSimulationManager(), devicesList);
        return dataProcessor.getTaskList();
    }
}
