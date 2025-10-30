package common;

import com.mechalikh.pureedgesim.scenariomanager.Scenario;
import com.mechalikh.pureedgesim.scenariomanager.SimulationParameters;
import com.mechalikh.pureedgesim.simulationengine.PureEdgeSim;
import com.mechalikh.pureedgesim.simulationmanager.SimLog;
import com.mechalikh.pureedgesim.simulationmanager.SimulationManager;
import com.mechalikh.pureedgesim.simulationmanager.SimulationThread;
import dag.TaskNode;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class MECSimManager extends SimulationManager {
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
    public MECSimManager(SimLog simLog, PureEdgeSim pureEdgeSim, int simulationId, int iteration, Scenario scenario) {
        super(simLog, pureEdgeSim, simulationId, iteration, scenario);
        this.totalTaskSent = 0;
    }

    protected void scheduleSuccessors(TaskNode taskNode){}

    public void genReport(){
        // Scheduling the end of the simulation.
        schedule(this, SimulationParameters.SIMULATION_TIME, PRINT_LOG);

        // Schedule the update of real-time charts.
        if (SimulationParameters.DISPLAY_REAL_TIME_CHARTS && !SimulationParameters.PARALLEL)
            scheduleNow(this, UPDATE_REAL_TIME_CHARTS);

        // Show simulation progress.
        scheduleNow(this, SHOW_PROGRESS);

        pureEdgeSim.setSimDone(true);
        simLog.printSameLine("Simulation progress : [", "red");
        System.out.println();

    }

    protected boolean areAllPredecesorTasksDone(TaskNode task){
        boolean ret = true;
        List<TaskNode> preds = task.predecessors;
        for(TaskNode taskNode : preds){
            if(!taskNode.isTaskDone()){
                ret = false;
                break;
            }
        }
        return ret;
    }

    protected List<Integer> getRootTaskIndexes(){
        List<Integer> list_indexes = new ArrayList<>();
        for(int i = 0; i < tasksList.size(); i++){
            TaskNode taskNode = (TaskNode) tasksList.get(i);
            if(taskNode.isStartTask()){ //Assuming the task id of root task is 0
                list_indexes.add(i);
            }
        }
        return list_indexes;
    }

    public void setPureEdgeSim(PureEdgeSim pureEdgeSim) {
        this.pureEdgeSim = pureEdgeSim;
    }

    public boolean isAllDagCompleted() {
        boolean ret = true;
        for(Map.Entry<Integer, Job> entry : DataProcessor.scheduledJob.entrySet()){
            if(!entry.getValue().isStatus()){
                ret = false;
                break;
            }
        }
        return ret;
    }

    public TaskNode createReplica(TaskNode origTask){
        TaskNode duplicateTask = origTask.clone();
        duplicateTask.setSelectedComputingNode(null);
        duplicateTask.setPrimary(false);
        duplicateTask.setCopiedTask(true);
        duplicateTask.setReplica(origTask);
        origTask.setReplica(duplicateTask);
        return duplicateTask;
    }
}
