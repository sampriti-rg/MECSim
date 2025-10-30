package applications.fptosm;

import com.mechalikh.pureedgesim.scenariomanager.Scenario;
import com.mechalikh.pureedgesim.simulationengine.PureEdgeSim;
import com.mechalikh.pureedgesim.simulationmanager.SimLog;
import com.mechalikh.pureedgesim.taskgenerator.Task;
import common.DataProcessor;
import common.MECSimManager;
import common.Job;
import dag.TaskNode;
import org.jgrapht.alg.util.Pair;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class FpTosmSimManager extends MECSimManager {
    Integer totalTaskSent;
    Integer totalDuplicateTask;
    FP_TOSM_Offloader fpTosmOffloader = null;
    public Map<String, Pair<Task, Task>> taskTrackingMap = new HashMap<>();
    public FpTosmSimManager(SimLog simLog, PureEdgeSim pureEdgeSim, int simulationId, int iteration, Scenario scenario) {
        super(simLog, pureEdgeSim, simulationId, iteration, scenario);
        totalTaskSent = 0;
        totalDuplicateTask = 0;
        fpTosmOffloader = new FP_TOSM_Offloader(this);
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

            List<TaskNode> tasks = new ArrayList<>();
            tasks.add(taskNode);
            fpTosmOffloader.processTasks(tasks);
            schedule(this, taskNode.getTime(), SEND_TO_ORCH, taskNode);
            addToTrackTask(taskNode, null);
        }
    }

    public void addToTrackTask(Task orig, Task copy){
        Pair<Task, Task> taskPair = new Pair<>(orig, copy);
        String key = orig.getApplicationID() + "_" + orig.getId();
        taskTrackingMap.put(key, taskPair);
    }

    @Override
    public void scheduleSuccessors(TaskNode taskNode1){

        List<TaskNode> tasksList = taskNode1.successors;
        List<TaskNode> tmpList = new ArrayList<>();
        for (TaskNode task : tasksList) {
            if (areAllPredecesorTasksDone(task)) {
                tmpList.add(task);
            }
        }

        fpTosmOffloader.processTasks(tmpList);
        for(TaskNode taskNode : tmpList){
            taskNode.setStartTime(this.getSimulation().clock());
            schedule(this, taskNode.getTime(), SEND_TO_ORCH, taskNode);
            totalTaskSent++;
        }
    }
}
