package common;

import com.mechalikh.pureedgesim.simulationmanager.SimulationManager;

public class GlobalConfig {
    public enum DatasetType {
        NONE,
        IOT_DATASET,
        SCIENTIFIC_WORKFLOW,
        SYNTHETIC_TASKS
    };
    private SimulationManager simulationManager;
    DatasetType datasetType;
    private String algorithm;
    String taskSetPath;
    String jobsPath;
    String scientificWorkFlowDir;
    String syntheticTasksDir;
    Integer maxApps;

    boolean modern_arch;
    // 2. Make the constructor private to prevent other classes from instantiating it
    private GlobalConfig() {
    }

    private static GlobalConfig instance = new GlobalConfig();

    public DatasetType getDatasetType() {
        return datasetType;
    }

    public void setDatasetType(DatasetType datasetType) {
        this.datasetType = datasetType;
    }

    public static void setInstance(GlobalConfig instance) {
        GlobalConfig.instance = instance;
    }

    // 3. Provide a public static method to get the single instance
    public static GlobalConfig getInstance() {
        return instance;
    }

    public SimulationManager getSimulationManager() {
        return simulationManager;
    }

    public void setSimulationManager(SimulationManager simulationManager) {
        this.simulationManager = simulationManager;
    }
    public String getAlgorithm() {
        return algorithm;
    }

    public void setAlgorithm(String algorithm) {
        this.algorithm = algorithm;
    }

    public String getJobsPath() {
        return jobsPath;
    }

    public void setJobsPath(String jobsPath) {
        this.jobsPath = jobsPath;
    }

    public String getScientificWorkFlowDir() {
        return scientificWorkFlowDir;
    }

    public void setScientificWorkFlowDir(String scientificWorkFlowDir) {
        this.scientificWorkFlowDir = scientificWorkFlowDir;
    }

    public String getTaskSetPath() {
        return taskSetPath;
    }

    public void setTaskSetPath(String taskSetPath) {
        this.taskSetPath = taskSetPath;
    }
    public String getSyntheticTasksDir() {
        return syntheticTasksDir;
    }

    public void setSyntheticTasksDir(String syntheticTasksDir) {
        this.syntheticTasksDir = syntheticTasksDir;
    }
    public Integer getMaxApps() {
        return maxApps;
    }

    public void setMaxApps(Integer maxApps) {
        this.maxApps = maxApps;
    }


    public boolean isModern_arch() {
        return modern_arch;
    }

    public void setModern_arch(boolean modern_arch) {
        this.modern_arch = modern_arch;
    }
}
