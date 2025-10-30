package applications.remec;

import com.mechalikh.pureedgesim.scenariomanager.Scenario;
import com.mechalikh.pureedgesim.simulationengine.PureEdgeSim;
import com.mechalikh.pureedgesim.simulationmanager.SimLog;
import com.mechalikh.pureedgesim.simulationmanager.SimulationThread;
import com.mechalikh.pureedgesim.taskgenerator.Task;
import common.DataProcessor;
import common.MECSimManager;
import common.Job;
import dag.TaskNode;
import org.jgrapht.alg.util.Pair;

import java.util.*;

public class ReMECSimManager extends MECSimManager {
    PureEdgeSim pureEdgeSim;
    Integer totalTaskSent;
    Integer totalDuplicateTask;
    OffloadingAndScheduling offloadingAndScheduling;

    boolean randomOffloading = false;
    public Map<String, Pair<Task, Task>> taskTrackingMap = new HashMap<>();
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
    public ReMECSimManager(SimLog simLog, PureEdgeSim pureEdgeSim, int simulationId, int iteration, Scenario scenario) {
        super(simLog, pureEdgeSim, simulationId, iteration, scenario);
        totalTaskSent = 0;
        totalDuplicateTask = 0;

        offloadingAndScheduling = new OffloadingAndScheduling(this);
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
            Job app = DataProcessor.scheduledJob.get(tasksList.get(index).getApplicationID());
            app.setStartTime(this.getSimulation().clock());
            TaskNode taskNode = (TaskNode) tasksList.get(index);
            taskNode.setStartTime(this.simulation.clock());

            offloadingAndScheduling.remec(taskNode, false);
            schedule(this, taskNode.getTime(), SEND_TO_ORCH, taskNode);

            addToTrackTask(taskNode, null);
        }

        totalTaskSent += rootTaskIndexes.size();
    }

    public void addToTrackTask(Task orig, Task copy){
        Pair<Task, Task> taskPair = new Pair<>(orig, copy);
        String key = orig.getApplicationID() + "_" + orig.getId();
        taskTrackingMap.put(key, taskPair);
    }

    @Override
    public void scheduleSuccessors(TaskNode taskNode1){
        List<TaskNode> tasksList = taskNode1.successors;
        Task.Status parentStatus = taskNode1.getStatus();
        List<TaskNode> tmpList = new ArrayList<>();
        for (TaskNode task : tasksList) {
            if (areAllPredecesorTasksDone(task)) {
                tmpList.add(task);
            }
        }

        Collections.sort(tmpList, Comparator.comparingDouble(task->task.getLength()));
        for(TaskNode taskNode : tmpList){
            offloadingAndScheduling.remec(taskNode, false);
            taskNode.setStartTime(this.getSimulation().clock());
            schedule(this, taskNode.getTime(), SEND_TO_ORCH, taskNode);
            totalTaskSent++;
            TaskNode duplicateTask = null;
            if(parentStatus == Task.Status.SUCCESS && taskNode.getTaskDecision() == TaskNode.TaskDecision.UE_ONLY
                    && (taskNode.isCritical() || taskNode.isLowReliable())){
                duplicateTask = createReplica(taskNode);
                offloadingAndScheduling.remec(duplicateTask, true);
                if(duplicateTask.getTaskDecision() != TaskNode.TaskDecision.UE_ONLY) {
                    //There is no point in executing replica node on UE, therefore replica is not scheduling in case of UE
                    duplicateTask.setStartTime(this.getSimulation().clock());
                    schedule(this, taskNode.getTime(), SEND_TO_ORCH, duplicateTask);
                    totalDuplicateTask++;
                    taskNode.setUmi(duplicateTask.getUmi());
                    //totalTaskSent++;
                }
            }
            addToTrackTask(taskNode, duplicateTask);
        }
    }
}

