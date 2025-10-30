package applications.rmeac;

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

public class RmeacSimManager extends MECSimManager {
    Integer totalTaskSent;
    Integer totalDuplicateTask;
    RmeacScheduling rmeacScheduling = null;
    public Map<String, Pair<Task, Task>> taskTrackingMap = new HashMap<>();
    public RmeacSimManager(SimLog simLog, PureEdgeSim pureEdgeSim, int simulationId, int iteration, Scenario scenario) {
        super(simLog, pureEdgeSim, simulationId, iteration, scenario);
        totalTaskSent = 0;
        totalDuplicateTask = 0;
        rmeacScheduling = new RmeacScheduling(this);
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

            rmeacScheduling.Rmeac(taskNode);
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
        for (TaskNode task : tasksList) {
            if (areAllPredecesorTasksDone(task)) {
                rmeacScheduling.Rmeac(task);
                task.setStartTime(this.getSimulation().clock());
                schedule(this, task.getTime(), SEND_TO_ORCH, task);
                totalTaskSent++;
                TaskNode duplicateTask = null;
                if(task.getTaskDecision()!= TaskNode.TaskDecision.UE_ONLY && task.isCritical()){
                    duplicateTask = createReplica(task);
                    rmeacScheduling.Rmeac(duplicateTask);
                    duplicateTask.setStartTime(this.getSimulation().clock());
                    schedule(this, task.getTime(), SEND_TO_ORCH, duplicateTask);
                    totalDuplicateTask++;
                }
            }
        }
    }
}
