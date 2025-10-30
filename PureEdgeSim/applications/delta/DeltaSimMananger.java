package applications.delta;

import com.mechalikh.pureedgesim.scenariomanager.Scenario;
import com.mechalikh.pureedgesim.simulationengine.PureEdgeSim;
import com.mechalikh.pureedgesim.simulationmanager.SimLog;
import com.mechalikh.pureedgesim.simulationmanager.SimulationThread;
import common.MECSimManager;
import dag.TaskNode;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class DeltaSimMananger extends MECSimManager {
    PureEdgeSim pureEdgeSim;
    Integer totalTaskSent;
    /**
     * Initializes the simulation manager.
     *
     * @param simLog       The simulation logger
     * @param pureEdgeSim  The CloudSim simulation engine.
     * @param simulationId The simulation ID
     * @param iteration    Which simulation run
     * @param scenario     The scenario is composed of the algorithm and
     *                     architecture that are being used, and the number of edge
     *                     devices.
     * @see SimulationThread#startSimulation()
     */
    public DeltaSimMananger(SimLog simLog, PureEdgeSim pureEdgeSim, int simulationId, int iteration, Scenario scenario) {
        super(simLog, pureEdgeSim, simulationId, iteration, scenario);
        totalTaskSent = 0;
    }

    @Override
    public void startInternal() {
        // Initialize logger variables.
        simLog.setGeneratedTasks(tasksList.size());
        simLog.setCurrentOrchPolicy(scenario.getStringOrchArchitecture());

        simLog.print(getClass().getSimpleName() + " - Simulation: " + getSimulationId() + "  , iteration: "
                + getIteration());
        List<Integer> rootTaskIndexes = getRootTaskIndexes();
        for(Integer index : rootTaskIndexes) {
            schedule(this, tasksList.get(index).getTime(), SEND_TO_ORCH, tasksList.get(index));
        }

        totalTaskSent += rootTaskIndexes.size();
    }

    @Override
    public void scheduleSuccessors(TaskNode taskNode){
        List<TaskNode> tasksList = taskNode.successors;
        List<TaskNode> taskAvlForSched = new ArrayList<>();
        for (TaskNode task : tasksList) {
            if (areAllPredecesorTasksDone(task)) {
                taskAvlForSched.add(task);
            }
        }

        Collections.sort(taskAvlForSched, new RankComparator());
        for(TaskNode task : taskAvlForSched){
            //System.out.println("Scheduling- Task: "+task.getId() + " app_id: " + task.getApplicationID() + " : "  + task.getEdgeDevice().isApplicationPlaced());
            schedule(this, taskNode.getTime(), SEND_TO_ORCH, task);
            totalTaskSent++;
        }
    }
}
