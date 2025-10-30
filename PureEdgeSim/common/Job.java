package common;

import dag.TaskNode;

import java.util.Map;

public class Job {
    private boolean status;
    private Double budget;
    private String JobID_InDB;
    private String JobID;
    private String TimeDeadLinePrefered;
    private String TimeDeadlineFinal;
    private String ListOfTasks;
    private String MaxTaskSize;
    private String MinTaskSize;
    private String MaxParallelExecutableTasks;
    private String TasksWhichCanRunInParallel;
    private String MinStorageNeeded;
    private double maxBudget;
    private double endTime;
    private double usedBudget;
    private double startTime;

    private Integer taskAvgLength;
    Map<Integer, TaskNode> taskMap;

    public Map<Integer, TaskNode> getTaskMap() {
        return taskMap;
    }
    public void setTaskMap(Map<Integer, TaskNode> taskMap) {
        this.taskMap = taskMap;
    }
    public Job(){
        status = false;
    }
    public Integer getAvgTaskLength() {
        return taskAvgLength/100;
    }

    public void setAvgTaskLength() {
        this.taskAvgLength = (Integer.valueOf(getMinTaskSize()) + Integer.valueOf(getMaxTaskSize()))/2;
    }

    public boolean isStatus() {
        return status;
    }

    public void setStatus(boolean status) {
        this.status = status;
    }

    public void setJobID_InDB(String jobID_InDB) {
        JobID_InDB = jobID_InDB;
    }

    public String getJobID() {
        return JobID;
    }

    public void setJobID(String jobID) {
        JobID = jobID;
    }


    public void setTimeDeadLinePrefered(String timeDeadLinePrefered) {
        TimeDeadLinePrefered = timeDeadLinePrefered;
    }


    public void setListOfTasks(String listOfTasks) {
        ListOfTasks = listOfTasks;
    }


    public String getMaxTaskSize() {
        return MaxTaskSize;
    }

    public void setMaxTaskSize(String maxTaskSize) {
        MaxTaskSize = maxTaskSize;
    }

    public String getMinTaskSize() {
        return MinTaskSize;
    }

    public void setMinTaskSize(String minTaskSize) {
        MinTaskSize = minTaskSize;
    }


    public void setTasksWhichCanRunInParallel(String tasksWhichCanRunInParallel) {
        TasksWhichCanRunInParallel = tasksWhichCanRunInParallel;
    }

    public void setMinStorageNeeded(String minStorageNeeded) {
        MinStorageNeeded = minStorageNeeded;
    }

    public Double getBudget() {
        return budget;
    }
    public void setBudget(Double budget) {
        this.budget = budget;
    }
    public double getMaxBudget() {
        return maxBudget;
    }

    public void setMaxBudget(double budget) {
        this.maxBudget = budget;
    }

    public void setEndTime(double endTime) {
        this.endTime = endTime;
    }

    public double getUsedBudget() {
        return usedBudget;
    }

    public void setUsedBudget(double usedBudget) {
        this.usedBudget = usedBudget;
    }
    public void addCost(double cost){this.usedBudget += cost;}
    public double getStartTime() {
        return startTime;
    }

    public void setStartTime(double startTime) {
        this.startTime = startTime;
    }
}
