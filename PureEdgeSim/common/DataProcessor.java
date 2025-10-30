package common;

import applications.bqprofit.ChargingPlan;
import applications.bqprofit.Cost;
import applications.bqprofit.ScientificWorkflowParser;
import applications.bqprofit.algorithm.Gene;
import applications.bqprofit.algorithm.RaGeneticAlgorithm;
import applications.bqprofit.algorithm.TaskCost;
import applications.bqprofit.algorithm.mkl;
import applications.delta.TaskProcessing;
import applications.rmeac.OffloadingOptimizer;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.mechalikh.pureedgesim.datacentersmanager.ComputingNode;
import com.mechalikh.pureedgesim.datacentersmanager.DataCenter;
import com.mechalikh.pureedgesim.datacentersmanager.DataCentersManager;
import com.mechalikh.pureedgesim.simulationmanager.SimulationManager;
import com.mechalikh.pureedgesim.taskgenerator.Task;
import dag.TaskNode;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.lang.Integer.MAX_VALUE;
import static java.lang.Integer.MIN_VALUE;

public class DataProcessor {
    public static Map<Integer, Job> jobsMap;
    public static Map<Integer, Job> scheduledJob;
    public static Map<Integer, Map<Integer, TaskNode>> tasksMap;
    public static Map<Integer, Map<Integer, TaskNode>> scheduledTasksMap;
    private List<? extends ComputingNode> devices;
    SimulationManager simManager;
    private Integer ueDeviceIndex;
    static Integer totalCpuIntensiveTask = 0;
    private ChargingPlan chargingPlan;
    private ScientificWorkflowParser scientificWorkflowParser;
    private Cost costPlan;
    public Integer num_apps;

    public DataProcessor(SimulationManager simulationManager,
                              List<? extends ComputingNode> devicesList) {
        jobsMap = new HashMap();
        tasksMap = new HashMap<>();
        scheduledTasksMap = new HashMap<>();
        scheduledJob = new HashMap<>();
        simManager = simulationManager;
        devices = devicesList;
        ueDeviceIndex = 0;
        this.num_apps = GlobalConfig.getInstance().getMaxApps();
        if(GlobalConfig.getInstance().getAlgorithm().equals("BQProfit")){
            this.loadChargingPlan();
            this.loadCostPlan();
        }

        if(GlobalConfig.getInstance().getDatasetType() == GlobalConfig.DatasetType.SCIENTIFIC_WORKFLOW){
            scientificWorkflowParser = new ScientificWorkflowParser(tasksMap, chargingPlan, jobsMap);
            scientificWorkflowParser.loadTasks();
        } else if (GlobalConfig.getInstance().getDatasetType() == GlobalConfig.DatasetType.SYNTHETIC_TASKS) {
            loadSyntheticTasks();
        }else if(GlobalConfig.getInstance().getDatasetType() == GlobalConfig.DatasetType.IOT_DATASET){
            this.loadJobs();
            this.loadTasks();
            System.out.println("Loaded IoT dataset");
        } else{
            System.out.println("It only supports scientific workflow and IoT dataset");
            System.exit(0);
        }
    }



    public List<Task> getTaskList() {
        TaskCriticality taskCriticality = new TaskCriticality(simManager);
        List<Task> taskList = new ArrayList<>();
        TaskProcessing deltaTaskProcessing = null;
        OffloadingOptimizer optimizer = new OffloadingOptimizer(simManager);

        //this.assignUEDevice();
        this.assignDependencies();
        this.assignUEDevice();
        this.assignStartAndEndTask();

        if(GlobalConfig.getInstance().getAlgorithm().equals("BQProfit")){
            allocateBudgetToJob();
            this.assignPeakDemandTask(0);
            this.assignDeadlineType(0);
            this.assignOffloadingDecision();
        }

        Integer job_count = 0;
        for (Map.Entry<Integer, Map<Integer, TaskNode>> entry : tasksMap.entrySet()) {
            Map<Integer, TaskNode> job = entry.getValue();
            if (!isValidDag(job)) {
                System.out.println("DAG with ap_id: " + entry.getKey() + " is not valid");
                continue;
            }

            //printDag(job);
            if(!GlobalConfig.getInstance().getAlgorithm().equals("BQProfit")) {
                allocateBudgetToJob(job);
            }

            taskCriticality.assignTaskCriticality(job);

            if(GlobalConfig.getInstance().getAlgorithm().equals("RMEAC")){
                optimizer.getDecision(job);
            }

            job_count++;

            List<TaskNode> tempTaskList = new ArrayList<>();
            for (Map.Entry<Integer, TaskNode> task : job.entrySet()) {
                if(GlobalConfig.getInstance().getDatasetType() == GlobalConfig.DatasetType.IOT_DATASET) {
                    task.getValue().setContainerSize(jobsMap.get(entry.getKey()).getAvgTaskLength());
                } else{
                    task.getValue().setContainerSize(2500);
                }

                taskList.add(task.getValue());
                tempTaskList.add(task.getValue());
            }

            if(GlobalConfig.getInstance().getAlgorithm().equals("DELTA")) {
                if(deltaTaskProcessing == null) {
                    deltaTaskProcessing = new TaskProcessing(simManager);
                }
                deltaTaskProcessing.assignTaskType(job);
                deltaTaskProcessing.assignTaskRanking(tempTaskList);
                deltaTaskProcessing.assignDecision(tempTaskList);
            }

            scheduledJob.put(entry.getKey(), jobsMap.get(entry.getKey()));
            scheduledTasksMap.put(entry.getKey(), job);
            if (job_count >= num_apps)
                break;
        }

        return taskList;
    }

    void allocateBudgetToJob(){
        Double min_budget = Double.MAX_VALUE;
        Double max_budget = Double.MIN_VALUE;
        for(Map.Entry<Integer, Map<Integer, TaskNode>> entry : tasksMap.entrySet()){
            Map<Integer, TaskNode> job = entry.getValue();
            Double total_charge = 0.0;
            for(Map.Entry<Integer, TaskNode> job_entry : job.entrySet()){
                TaskNode taskNode = job_entry.getValue();
                total_charge += chargingPlan.getChargePerMi()*taskNode.getLength() +
                        chargingPlan.getChargePerMemoryMB()*(taskNode.getMemoryNeed()/1024) +
                        chargingPlan.getChargePerIO()*(taskNode.getReadOps() + taskNode.getWriteOps());
            }

            Integer budget_pct = Math.toIntExact(Helper.getRandomInteger(50, 60));
            for(Map.Entry<Integer, TaskNode> job_entry : job.entrySet()){
                TaskNode taskNode = job_entry.getValue();
                taskNode.getJob().setBudget(budget_pct*total_charge/100);
                if(min_budget > budget_pct*total_charge/100){
                    min_budget = budget_pct*total_charge/100;
                }

                if(max_budget < budget_pct*total_charge/100){
                    max_budget = budget_pct*total_charge/100;
                }
            }
        }
    }

    private void assignPeakDemandTask(int peakPct) {
        //We are assuming 10% of the tasks are offloaded in the high demand time
        Integer total_open_tasks = 0; //not including start and end task of the DAG
        List<TaskNode> tempTaskList = new ArrayList<>();
        for(Map.Entry<Integer, Map<Integer, TaskNode>> entry : tasksMap.entrySet()){
            Map<Integer, TaskNode> job = entry.getValue();
            for (Map.Entry<Integer, TaskNode> task : job.entrySet()) {
                if(!task.getValue().isStartTask() && !task.getValue().isEndTask()) {
                    total_open_tasks++;
                }

                tempTaskList.add(task.getValue());
            }
        }

        Integer high_demand_tasks = (int) (total_open_tasks*(peakPct/100.0));
        if(high_demand_tasks == 0)
            return;

        for(Map.Entry<Integer, Map<Integer, TaskNode>> entry : tasksMap.entrySet()){
            Map<Integer, TaskNode> job = entry.getValue();
            for (Map.Entry<Integer, TaskNode> task : job.entrySet()) {
                if(!task.getValue().isStartTask() && !task.getValue().isEndTask() && task.getValue().getTaskDecision() == TaskNode.TaskDecision.MEC_ONLY) {
                    if(high_demand_tasks > 0){
                        task.getValue().setPeakTimeOffload(true);
                        high_demand_tasks--;
                    }
                }
            }
        }

    }

    void assignDeadlineType(int hardPct){
        int total = 0;
        int h_count = 0;
        int s_count = 0;
        if(hardPct == 0)
            return;
        for(Map.Entry<Integer, Map<Integer, TaskNode>> entry : tasksMap.entrySet()){
            Map<Integer, TaskNode> job = entry.getValue();
            for (Map.Entry<Integer, TaskNode> task : job.entrySet()) {
                task.getValue().setDeadlineType(null);
            }
        }
        for(Map.Entry<Integer, Map<Integer, TaskNode>> entry : tasksMap.entrySet()){
            Map<Integer, TaskNode> job = entry.getValue();
            Integer hard_tasks = (int) (job.size()*(hardPct/100.0));
            List<Integer> index_list = Helper.getListofRandomNumberInRange(1, job.size()-1, hard_tasks+2);
            for(Integer index: index_list){
                job.get(index).setDeadlineType("hard");
                h_count++;
            }

            for (Map.Entry<Integer, TaskNode> task : job.entrySet()) {
                ++total;
                if(task.getValue().isStartTask() || task.getValue().isEndTask()){
                    if (task.getValue().getDeadlineType() != null && task.getValue().getDeadlineType().equals("hard")){
                        h_count--;
                    }
                    task.getValue().setDeadlineType("soft");
                } else if (task.getValue().getDeadlineType() == null) {
                    task.getValue().setDeadlineType("soft");
                    s_count++;
                }
            }
        }

        System.out.println("Total: "+total+" Hard: "+h_count+" Soft: "+s_count);
    }


    void printDag(Map<Integer, TaskNode> job) {
        for (Map.Entry<Integer, TaskNode> task : job.entrySet()) {
            TaskNode taskNode = task.getValue();
            String pred_str = "";
            String succ_str = "";
            for (TaskNode t : taskNode.successors) {
                if (succ_str.isEmpty()) {
                    succ_str += t.getId();
                } else {
                    succ_str += "->" + t.getId();
                }
            }

            for (TaskNode t : taskNode.predecessors) {
                if (pred_str.isEmpty()) {
                    pred_str += t.getId();
                } else {
                    pred_str += "->" + t.getId();
                }
            }

            System.out.println("Task: " + taskNode.getId() + " predecessors: " + pred_str + " Successors: " + succ_str);
        }
    }

    boolean isValidDag(Map<Integer, TaskNode> job) {
        boolean result = true;
        for (Map.Entry<Integer, TaskNode> task : job.entrySet()) {
            TaskNode taskNode = task.getValue();
            if (taskNode.isStartTask()) {
                if (taskNode.predecessors.size() != 0 || taskNode.successors.size() == 0) {
                    result = false;
                    break;
                }
            } else if (taskNode.isEndTask()) {
                if (taskNode.predecessors.size() == 0 || taskNode.successors.size() != 0) {
                    result = false;
                    break;
                }
            } else {
                if (taskNode.predecessors.size() == 0 || taskNode.successors.size() == 0) {
                    result = false;
                    break;
                }
            }
        }
        return result;
    }

    void assignUEDevice() {
        if (devices.size() == 0) {
            System.out.println("No UE devices available");
            return;
        }


        for (Map.Entry<Integer, Map<Integer, TaskNode>> entry : tasksMap.entrySet()) {
            Map<Integer, TaskNode> job = entry.getValue();
            for (Map.Entry<Integer, TaskNode> task : job.entrySet()) {
                TaskNode taskNode = task.getValue();
                double currentTime = simManager.getSimulation().clock();
                if (devices.get(ueDeviceIndex).getMipsPerCore() == 0) {
                    System.out.println("-MIPS zero");
                }
                taskNode.setEdgeDevice(devices.get(ueDeviceIndex));
                taskNode.getEdgeDevice().setStartTime(currentTime);
            }

            ueDeviceIndex++;
            if (ueDeviceIndex >= devices.size()) {
                ueDeviceIndex = 0;
            }
        }
    }

    void assignDependencies() {
        for (Map.Entry<Integer, Map<Integer, TaskNode>> entry : tasksMap.entrySet()) {
            Map<Integer, TaskNode> job = entry.getValue();
            for (Map.Entry<Integer, TaskNode> task : job.entrySet()) {
                TaskNode taskNode = task.getValue();
                for (Integer taskId : taskNode.getSuccessorsId()) {
                    TaskNode dTask = job.get(taskId);
                    if (dTask != null)
                        taskNode.successors.add(dTask);
                }

                for (Integer taskId : taskNode.getPredecessorsId()) {
                    TaskNode dTask = job.get(taskId);
                    if (dTask != null)
                        taskNode.predecessors.add(dTask);
                }

                if (taskNode.successors.size() == 0) {
                    taskNode.setEndTask(true);
                }

                if (taskNode.predecessors.size() == 0) {
                    taskNode.setStartTask(true);
                }

                //rectifying the dependency
                List<TaskNode> successors = taskNode.successors;
                for (TaskNode tNode : successors) {
                    boolean pred_check = false;
                    for (TaskNode succNode : tNode.predecessors) {
                        if (succNode.getId() == taskNode.getId()) {
                            pred_check = true;
                            break;
                        }
                    }
                    if (!pred_check) {
                        tNode.predecessors.add(taskNode);
                    }
                }

                List<TaskNode> predecessors = taskNode.predecessors;
                for (TaskNode tNode : predecessors) {
                    boolean succ_check = false;
                    for (TaskNode succNode : tNode.successors) {
                        if (succNode.getId() == taskNode.getId()) {
                            succ_check = true;
                            break;
                        }
                    }
                    if (!succ_check) {
                        tNode.successors.add(taskNode);
                    }
                }
            }

        }
    }

    void assignStartAndEndTask() {
        for (Map.Entry<Integer, Map<Integer, TaskNode>> entry : tasksMap.entrySet()) {
            Map<Integer, TaskNode> job = entry.getValue();
            List<TaskNode> startTasks = new ArrayList<>();
            List<TaskNode> endTasks = new ArrayList<>();
            ComputingNode ueDevice = null;
            for (Map.Entry<Integer, TaskNode> task : job.entrySet()) {
                TaskNode taskNode = task.getValue();
                ueDevice = taskNode.getEdgeDevice();
                if (taskNode.predecessors.size() == 0) {
                    startTasks.add(taskNode);
                }

                if (taskNode.successors.size() == 0) {
                    endTasks.add(taskNode);
                }
            }

            if (startTasks.size() > 1) {
                Integer dummy_task_id = this.getDummyTaskId(job);
                TaskNode taskNode = this.createDummyTask(dummy_task_id, Integer.valueOf(entry.getKey()), ueDevice);
                taskNode.setStartTask(true);
                taskNode.successors.addAll(startTasks);
                for (TaskNode task : startTasks) {
                    task.setStartTask(false);
                    task.predecessors.add(taskNode);
                }
                job.put(dummy_task_id, taskNode);
            }

            if (endTasks.size() > 1) {
                Integer dummy_task_id = this.getDummyTaskId(job);
                TaskNode taskNode = this.createDummyTask(dummy_task_id, Integer.valueOf(entry.getKey()), ueDevice);
                taskNode.predecessors.addAll(endTasks);
                taskNode.setEndTask(true);
                for (TaskNode task : endTasks) {
                    task.setEndTask(false);
                    task.successors.add(taskNode);
                }
                job.put(dummy_task_id, taskNode);
            }
        }

    }

    Integer getDummyTaskId(Map<Integer, TaskNode> job) {
        Integer task_id = Integer.MIN_VALUE;
        for (Map.Entry<Integer, TaskNode> task : job.entrySet()) {
            if (task.getKey() > task_id) {
                task_id = task.getKey();
            }
        }

        return task_id + 1;
    }

    TaskNode createDummyTask(Integer id, Integer app_id, ComputingNode ueDevice) {
        TaskNode taskNode = new TaskNode(0, 0);
        taskNode.setApplicationID(app_id);
        taskNode.setFileSize(0).setOutputSize(0);
        taskNode.setContainerSize(0);
        taskNode.setRegistry(simManager.getDataCentersManager().getCloudDatacentersList().get(0).nodeList.get(0));
        taskNode.setId(id);
        taskNode.setMaxLatency(0);
        taskNode.setMemoryNeed(0.0);
        taskNode.setStorageNeed(0.0);
        taskNode.setContainerSize(0);
        taskNode.setEdgeDevice(ueDevice);
        taskNode.setDummyTask(true);
        return taskNode;
    }

    public void loadJobs() {
        try {
            byte[] jsonData = Files.readAllBytes(Paths.get(GlobalConfig.getInstance().getJobsPath()));
            ObjectMapper objectMapper = new ObjectMapper();
            JsonNode jsonNodeRoot = objectMapper.readTree(jsonData);
            if (jsonNodeRoot.isObject()) {
                ArrayNode jobs = (ArrayNode) jsonNodeRoot.get("Jobs");
                if (jobs.isArray()) {
                    for (JsonNode objNode : jobs) {
                        Job job = new Job();
                        job.setJobID_InDB(objNode.get("JobID_InDB").asText());
                        job.setJobID(objNode.get("JobID").asText());
                        job.setTimeDeadLinePrefered(objNode.get("TimeDeadLinePrefered").asText());
                        job.setListOfTasks(objNode.get("ListOfTasks").asText());
                        job.setMinStorageNeeded(objNode.get("MinStorageNeeded").asText());
                        job.setTasksWhichCanRunInParallel(objNode.get("TasksWhichCanRunInParallel").asText());
                        job.setMinTaskSize(objNode.get("MinTaskSize").asText());
                        job.setMaxTaskSize(objNode.get("MaxTaskSize").asText());
                        job.setAvgTaskLength();
                        jobsMap.put(Integer.valueOf(job.getJobID()), job);
                    }
                }
            }
        } catch (IOException ioException) {
            ioException.printStackTrace();
        }
    }

    public void loadTasks() {
        BufferedReader reader;
        long lineCount = 0;
        FileInputStream inputStream = null;
        Scanner sc = null;
        Integer taskCount = 0;
        Integer dupTaskCount = 0;
        Long max_cpu = Long.MIN_VALUE;
        Long min_cpu = Long.MAX_VALUE;

        Double max_storage = Double.MIN_VALUE;
        Double min_storage = Double.MAX_VALUE;

        Double max_mem = Double.MIN_VALUE;
        Double min_mem = Double.MAX_VALUE;

        Double max_deadline = Double.MIN_VALUE;
        Double min_deadline = Double.MAX_VALUE;
        try {
            inputStream = new FileInputStream(GlobalConfig.getInstance().getTaskSetPath());
            sc = new Scanner(inputStream, "unicode");
            while (sc.hasNextLine()) {
                String line = sc.nextLine();
                lineCount++;
                if (lineCount > 1) {
                    String fields[] = line.split("\\[");

                    String other_fields[] = fields[0].split(",");
                    String taskId = other_fields[1];
                    if (!taskId.isEmpty()) {
                        Integer jobId = Integer.valueOf(other_fields[2]);
                        Long cpu = Long.parseLong(other_fields[3]) / 1000 + 1;
                        Double memory = Double.parseDouble(other_fields[4]);
                        Double storage = Double.parseDouble(other_fields[5]);
                        Double deadline = (Double.parseDouble(other_fields[8]) / 50)%100 + 1;
                        Integer task_id = Integer.parseInt(taskId);

                        if (cpu < min_cpu) {
                            min_cpu = cpu;
                        }

                        if (cpu > max_cpu) {
                            max_cpu = cpu;
                        }

                        if (memory < min_mem) {
                            min_mem = memory;
                        }

                        if (memory > max_mem) {
                            max_mem = memory;
                        }

                        if (deadline < min_deadline) {
                            min_deadline = deadline;
                        }

                        if (deadline > max_deadline) {
                            max_deadline = deadline;
                        }

                        if (storage < min_storage) {
                            min_storage = storage;
                        }

                        if (storage > max_storage) {
                            max_storage = storage;
                        }

                        double localTime = 1.2*cpu/2000; //taken from local device config
                        if(localTime > deadline){
                            deadline = localTime + 1;
                        }

                        TaskNode taskNode = new TaskNode(task_id, cpu);
                        taskNode.setApplicationID(Integer.valueOf(jobId));
                        taskNode.setFileSize(Helper.getRandomInteger(80, 200)).setOutputSize(Helper.getRandomInteger(80, 200)); //In MB as BW is measured in Mb
                        taskNode.setContainerSize(25000);
                        taskNode.setRegistry(simManager.getDataCentersManager().getCloudDatacentersList().get(0).nodeList.get(0));
                        taskNode.setId(task_id);
                        taskNode.setMaxLatency(deadline);
                        Integer deadline_type = Helper.getRandomInteger(0, 5);
                        if(deadline_type <= 2){
                            taskNode.setDeadlineType("hard");
                        } else {
                            taskNode.setDeadlineType("soft");
                        }

                        taskNode.setReadOps(Helper.getRandomInteger(50, 200));
                        taskNode.setWriteOps(Helper.getRandomInteger(30, 150));

                        if(GlobalConfig.getInstance().getAlgorithm().equals("DELTA")){
                            //To simulate memory intensive tasks
                            taskNode.setMemoryNeed(10*memory);
                            taskNode.setStorageNeed(storage*10);
                        }else {
                            taskNode.setMemoryNeed(memory);
                            taskNode.setStorageNeed(storage);
                        }


                        taskNode.setParallelTaskPct(Helper.getRandomInteger(70, 90));

                        Job job = jobsMap.get(taskNode.getApplicationID());
                        taskNode.setJob(job);
                        if(taskNode.getChargingPlan() == null) {
                            taskNode.setChargingPlan(chargingPlan);
                        }

                        job.setJobID(String.valueOf(jobId));
                        if(job.getTaskMap() == null){
                            if(tasksMap.containsKey(taskNode.getApplicationID())) {
                                Map<Integer, TaskNode> tMap = tasksMap.get(taskNode.getApplicationID());
                                job.setTaskMap(tMap);
                            } else{
                                Map<Integer, TaskNode> tMap = new HashMap<>();
                                tMap.put(task_id, taskNode);
                                tasksMap.put(jobId, tMap);
                            }
                        }


                        String successors_str = fields[1];
                        if (successors_str.length() > 1) {
                            String successors = fields[1].split("\\]")[0];
                            String[] succList = successors.split(",");
                            for (String succ : succList) {
                                if (!succ.isEmpty()) {
                                    taskNode.getSuccessorsId().add(Integer.parseInt(succ));
                                }
                            }
                        }
                        String predecessors_str = fields[3];

                        if (predecessors_str.length() > 1) {
                            String predecessors = predecessors_str.split("\\]")[0];
                            String[] predList = predecessors.split(",");
                            for (String pred : predList) {
                                if (!pred.isEmpty()) {
                                    taskNode.getPredecessorsId().add(Integer.parseInt(pred));
                                }
                            }
                        }

                        if (tasksMap.containsKey(jobId)) {
                            if (tasksMap.get(jobId).containsKey(task_id)) {
                                dupTaskCount++;
                            } else {
                                tasksMap.get(jobId).put(task_id, taskNode);
                            }
                        } else {
                            Map<Integer, TaskNode> tMap = new HashMap<>();
                            tMap.put(task_id, taskNode);
                            tasksMap.put(jobId, tMap);
                        }
                        taskCount++;
                    }
                }
            }
            sc.close();
        } catch (IOException ioException) {
            ioException.printStackTrace();
        }

//        System.out.println("Total tasks loaded: " + taskCount + " duplicate: " + dupTaskCount);
//        System.out.println("MAX CPU: " + max_cpu + " MIN CPU: " + min_cpu);
//        System.out.println("MAX storage: " + max_storage + " MIN storage: " + min_storage);
//        System.out.println("MAX memory: " + max_mem + " MIN memory: " + min_mem);
//        System.out.println("MAX deadline: " + max_deadline + " MIN deadline: " + min_deadline);
    }

    public static List<Path> listFiles(Path path) throws IOException {

        List<Path> result;
        try (Stream<Path> walk = Files.walk(path)) {
            result = walk.filter(Files::isRegularFile)
                    .collect(Collectors.toList());
        }
        return result;

    }

    public void loadSyntheticTasks() {
        try {
            Path path = Paths.get(GlobalConfig.getInstance().getSyntheticTasksDir());
            List<Path> paths = listFiles(path);
            Double deadline_max = Double.MIN_VALUE;
            Double deadline_min = Double.MAX_VALUE;
            Long cpu_min = Long.MAX_VALUE;
            Long cpu_max = Long.MIN_VALUE;
            Long io_read_min = Long.MAX_VALUE;
            Long io_read_max = Long.MIN_VALUE;
            Long io_write_min = Long.MAX_VALUE;
            Long io_write_max = Long.MIN_VALUE;
            int task_max = MIN_VALUE;
            int task_min = MAX_VALUE;
            for (Path file_path : paths) {
                File file = file_path.toFile();
                BufferedReader reader;
                long lineCount = 0;
                FileInputStream inputStream = null;
                Scanner sc = null;
                Integer taskCount = 0;
                Map<Integer, List<Integer>> dependency_list = new HashMap<>();
                Integer jobId = Integer.valueOf(String.valueOf(file_path.getFileName()).split("_")[1]);
                Map<Integer, TaskNode> tMap = null;
                Job job = new Job();
                try {
                    inputStream = new FileInputStream(String.valueOf(file_path));
                    sc = new Scanner(inputStream);
                    while (sc.hasNextLine()) {
                        String line = sc.nextLine();
                        String fields[] = line.split(" ");
                        String dependencies_str = null;

                        Integer task_id = Integer.valueOf(fields[1]);
                        TaskNode taskNode = null;
                        ++taskCount;
                        if (fields[3].equals("ROOT")) {
                            taskNode = new TaskNode(task_id, 1);
                            dependencies_str = fields[2];
                            taskNode.setMemoryNeed(1.0);
                            taskNode.setFileSize(1);
                            taskNode.setReadOps(1);
                            taskNode.setWriteOps(1);
                            taskNode.setStorageNeed(1.0);
                            taskNode.setTaskDecision(TaskNode.TaskDecision.UE_ONLY);
                            taskNode.setStartTask(true);
                            taskNode.setDeadlineType("soft");
                            taskNode.setMaxLatency(1);
                            taskNode.setJob(job);
                        } else if (fields[3].equals("END")) {
                            taskNode = new TaskNode(task_id, 1);
                            taskNode.setMemoryNeed(1.0);
                            taskNode.setFileSize(1);
                            taskNode.setReadOps(1);
                            taskNode.setWriteOps(1);
                            taskNode.setStorageNeed(1.0);
                            taskNode.setTaskDecision(TaskNode.TaskDecision.UE_ONLY);
                            taskNode.setEndTask(true);
                            taskNode.setDeadlineType("soft");

                            taskNode.setMaxLatency(1);
                            taskNode.setJob(job);
                        } else { //Computation tasks
                            dependencies_str = fields[2];
                            Long cpu = Long.valueOf(Integer.valueOf(fields[5]));

                            Double memory = Double.valueOf(fields[6]);
                            Long read_io = Long.valueOf(Integer.valueOf(fields[7]));
                            Long write_io = Long.valueOf(Integer.valueOf(fields[8]));

                            Double deadline = Double.valueOf(fields[9]); //large
                            Integer deadline_type = Integer.valueOf(fields[10]);

                            taskNode = new TaskNode(task_id, cpu);

                            if(GlobalConfig.getInstance().getAlgorithm().equals("DELTA")){
                                taskNode.setMemoryNeed(Helper.getRandomDouble(20, 2000)*Helper.getRandomInteger(1300, 2000));
                                taskNode.setStorageNeed(memory*Helper.getRandomInteger(120, 500));
                            } else{
                                taskNode.setStorageNeed(Helper.getRandomDouble(500, 2000));
                                taskNode.setMemoryNeed(memory);
                            }
                            taskNode.setReadOps(read_io);
                            taskNode.setWriteOps(write_io);
                            taskNode.setMaxLatency(deadline);
                            if(deadline_type > 0){
                                taskNode.setDeadlineType("hard");
                            } else {
                                taskNode.setDeadlineType("soft");
                            }
                            //taskNode.setStorageNeed((double) Helper.getRandomInteger(500, 1000));
                            taskNode.setFileSize(Helper.getRandomInteger(80000, 4000000)).setOutputSize(Helper.getRandomInteger(80000, 4000000));
                            taskNode.setTaskDecision(TaskNode.TaskDecision.OPEN);
                            taskNode.setJob(job);

                            if(cpu < cpu_min){
                                cpu_min = cpu;
                            }

                            if(cpu > cpu_max){
                                cpu_max = cpu;
                            }

                            if(read_io < io_read_min){
                                io_read_min = read_io;
                            }

                            if(read_io > io_read_max){
                                io_read_max = read_io;
                            }

                            if(write_io < io_write_min){
                                io_write_min = write_io;
                            }

                            if(write_io > io_read_max){
                                io_write_max = write_io;
                            }

                            if(deadline < deadline_min){
                                deadline_min = deadline;
                            }

                            if(deadline_max < deadline){
                                deadline_max = deadline;
                            }
                        }
                        taskNode.setApplicationID(jobId);
                        taskNode.setId(task_id);
                        taskNode.setContainerSize(25000);

                        taskNode.setChargingPlan(this.chargingPlan);

                        if (dependencies_str != null) {
                            String dependencies[] = dependencies_str.split(",");
                            for (int i = 0; i < dependencies.length; i++) {
                                if (!dependency_list.containsKey(task_id)) {
                                    List<Integer> d_tasks = new ArrayList<>();
                                    d_tasks.add(Integer.valueOf(dependencies[i]));
                                    dependency_list.put(task_id, d_tasks);
                                } else {
                                    dependency_list.get(task_id).add(Integer.valueOf(dependencies[i]));
                                }
                            }
                        }
                        if (tasksMap.containsKey(jobId)) {
                            tasksMap.get(jobId).put(task_id, taskNode);
                        } else {
                            tMap = new HashMap<>();
                            tMap.put(task_id, taskNode);
                            tasksMap.put(jobId, tMap);
                        }
                    }
                    sc.close();
                } catch (IOException ioException) {
                    ioException.printStackTrace();
                }

                for (Map.Entry<Integer, List<Integer>> task_dep_pair : dependency_list.entrySet()) {
                    Integer parent_id = task_dep_pair.getKey();
                    List<Integer> children = task_dep_pair.getValue();
                    for (Integer child : children) {
                        TaskNode childNode = tasksMap.get(jobId).get(child);
                        TaskNode parentNode = tasksMap.get(jobId).get(parent_id);
                        tasksMap.get(jobId).get(parent_id).successors.add(childNode);
                        tasksMap.get(jobId).get(child).predecessors.add(parentNode);
                    }
                }

                job.setJobID(String.valueOf(jobId));
                job.setTaskMap(tMap);

//                if(GlobalConfig.getInstance().getAlgorithm().equals("BQProfit")) {
//                    job.setBudget(Helper.getBudget(tMap, chargingPlan));
//                }
                jobsMap.put(jobId, job);
                //scheduledJob.put(jobId, job);
                if(taskCount > task_max){
                    task_max = taskCount;
                }

                if(taskCount < task_min){
                    task_min = taskCount;
                }

                taskCount = 0;
            }
//            System.out.println("min Task: "+task_min+" max task: "+task_max+" cpu min: "+ cpu_min+" cpu max: "+cpu_max);
//            System.out.println("IO read min: "+io_read_min+" IO read max: "+io_read_max+"IO write min: "+io_write_min+" IO write max: "+io_write_max);
//            System.out.println("Deadline min: "+deadline_min+" Deadline max: "+deadline_max);
        }catch (IOException io){
            io.printStackTrace();
        }
    }


    private void updateData(int type){ //1-cpu, 2-io, 3-memory
        int job_no = 0;
        for(Map.Entry<Integer, Map<Integer, TaskNode>> entry : tasksMap.entrySet()) {
            Map<Integer, TaskNode> job = entry.getValue();
            job_no++;
            int intensive_task = (int) Math.ceil(job.size()/2) + Helper.getRandomInteger(2, 5);
            int task_assigned = 0;
            List<DataCenter> dclist = simManager.getDataCentersManager().getEdgeDatacenterList();
            ComputingNode node = dclist.get(0).nodeList.get(0);
            final Double MIPS = node.getMipsPerCore();
            for (Map.Entry<Integer, TaskNode> task : job.entrySet()) {
                TaskNode taskNode = task.getValue();
                if(taskNode.isDummyTask())
                    continue;
                TaskNode.TaskType taskType = TaskNode.TaskType.NORMAL;

                Double cpuTime = taskNode.getLength() / MIPS;
                Double ioTime = taskNode.getStorageNeed() * 60 / (100 * node.getReadBandwidth()) //READ operation, 60% read
                        + taskNode.getStorageNeed() * 40 / (100 * node.getWriteBandwidth()); //WRITE operation, 40% write;;
                Double memoryTransferTime = taskNode.getMemoryNeed()/node.getDataBusBandwidth();

                Double totalTime = cpuTime + ioTime + memoryTransferTime;

                if(type == 1) {
                    if(cpuTime ==0)
                        continue;
                    if (cpuTime / totalTime > 0.5) {
                        task_assigned++;
                        continue;
                    }

                    Double required_cpu_time = memoryTransferTime + ioTime + Helper.getRandomInteger(10, 50);
                    int task_length = (int) Math.ceil((taskNode.getLength() * required_cpu_time) / (cpuTime));
                    taskNode.setLength(task_length);
                } else if(type == 2){
                    if(ioTime <= 0)
                        continue;
                    if(ioTime/totalTime > 0.5){
                        task_assigned++;
                        continue;
                    }

                    Double required_io_time = memoryTransferTime + cpuTime + Helper.getRandomInteger(10, 50);
                    int task_length = (int) Math.ceil((taskNode.getLength() * required_io_time) / (ioTime));
                    taskNode.setLength(task_length);
                } else { //type=3
                    if(memoryTransferTime <= 0)
                        continue;
                    if(memoryTransferTime/totalTime > 0.5){
                        task_assigned++;
                        continue;
                    }

                    Double required_mem_time = ioTime + cpuTime + Helper.getRandomInteger(10, 50);
                    int task_length = (int) Math.ceil((taskNode.getLength() * required_mem_time) / (memoryTransferTime));
                    taskNode.setLength(task_length);
                }

                if (task_assigned >= intensive_task)
                    break;
            }

            if(job_no >= 300)
                break;

        }
    }


    private void allocateBudgetToJob(Map<Integer, TaskNode> job) {
        Double min_budget = Double.MAX_VALUE;
        Double max_budget = Double.MIN_VALUE;
        Double total_charge = 0.0;
        Integer jobId = 0;
        for (Map.Entry<Integer, TaskNode> job_entry : job.entrySet()) {
            TaskNode taskNode = job_entry.getValue();
            total_charge += Helper.getCharge(taskNode.getLength(), taskNode.getReadOps()+taskNode.getWriteOps());
            jobId = taskNode.getApplicationID();
        }
        Integer budget_pct = Math.toIntExact(Helper.getRandomInteger(65, 70));
        double budget =  (total_charge*budget_pct)/100;
        if(jobId == 1){
            System.out.println("Budget: "+budget);
        }
        jobsMap.get(jobId).setMaxBudget(budget);
    }

    private void loadChargingPlan() {
        chargingPlan = new ChargingPlan();
        JSONParser parser = new JSONParser();
        try{
            Object obj = parser.parse(new FileReader("PureEdgeSim/applications/bqprofit/settings/charging_plan.json"));
            JSONObject jsonObject = (JSONObject)obj;
            Double cpu = (Double) jsonObject.get("cpu");
            Double memory = (Double) jsonObject.get("memory");
            Double io = (Double) jsonObject.get("io");
            Double penalty = (Double) jsonObject.get("penalty");
            Double hard_deadline_surcharge = (Double) jsonObject.get("hard_deadline_surcharge");
            Double high_demand_surcharge = (Double)jsonObject.get("high_demand_surcharge");
            chargingPlan.setChargePerIO(io);
            chargingPlan.setChargePerMi(cpu);
            chargingPlan.setChargePerMemoryMB(memory);
            chargingPlan.setPenalty(penalty);
            chargingPlan.setHigh_demand_surcharge(high_demand_surcharge);
            chargingPlan.setHard_deadline_surcharge(hard_deadline_surcharge);
        }catch(Exception e) {
            e.printStackTrace();
        }
    }

    public void loadCostPlan(){
        costPlan = new Cost();
        JSONParser parser = new JSONParser();
        try {
            Object obj = parser.parse(new FileReader("PureEdgeSim/applications/bqprofit/settings/cost_plan.json"));
            JSONObject jsonObject = (JSONObject) obj;
            Double very_high_cpu = (Double) jsonObject.get("very_high_cpu");
            Double high_cpu = (Double) jsonObject.get("high_cpu");
            Double medium_cpu = (Double) jsonObject.get("medium_cpu");
            Double low_cpu = (Double) jsonObject.get("low_cpu");
            Double very_high_memory = (Double) jsonObject.get("very_high_memory");
            Double high_memory = (Double) jsonObject.get("high_memory");
            Double medium_memory = (Double) jsonObject.get("medium_memory");
            Double low_memory = (Double) jsonObject.get("low_memory");

            Double very_high_io = (Double) jsonObject.get("very_high_io");
            Double high_io = (Double) jsonObject.get("high_io");
            Double medium_io = (Double) jsonObject.get("medium_io");
            Double low_io = (Double) jsonObject.get("low_io");

            costPlan.setVery_high_costPerMi(very_high_cpu);
            costPlan.setHigh_costPerMi(high_cpu);
            costPlan.setMedium_costPerMi(medium_cpu);
            costPlan.setLow_costPerMi(low_cpu);

            costPlan.setVery_high_costPerMemoryMB(very_high_memory);
            costPlan.setHigh_costPerMemoryMB(high_memory);
            costPlan.setMedium_costPerMemoryMB(medium_memory);
            costPlan.setLow_costPerMemoryMB(low_memory);

            costPlan.setVery_high_costPerIO(very_high_io);
            costPlan.setHigh_costPerIO(high_io);
            costPlan.setMedium_costPerIO(medium_io);
            costPlan.setLow_costPerIO(low_io);
        }catch(Exception e) {
            e.printStackTrace();
        }
    }

    void assignOffloadingDecision(){
        this.assignDecForBqprofit();
    }

    void assignDecForBqprofit(){
        int count = 0;
        for (Map.Entry<Integer, Job> jobentry : jobsMap.entrySet()) {
            Job job = jobentry.getValue();
            mkl partiton_algo = new mkl(job.getTaskMap(), simManager, job.getBudget());
            partiton_algo.partition();

            List<TaskCost> locallist = partiton_algo.getLocalList();
            List<TaskCost> remotelist = partiton_algo.getRemotelist();
            List<TaskNode> newremotelist = new ArrayList<>();
            for (TaskCost taskCost : locallist) {
                taskCost.getTaskNode().setTaskDecision(TaskNode.TaskDecision.UE_ONLY);
            }
            Double total_len = 0.0;
            for (TaskCost taskCost : remotelist) {
                if (taskCost.getTaskNode().isEndTask() || taskCost.getTaskNode().isStartTask()) {
                    taskCost.getTaskNode().setTaskDecision(TaskNode.TaskDecision.UE_ONLY);
                } else {
                    taskCost.getTaskNode().setTaskDecision(TaskNode.TaskDecision.MEC_ONLY);
                }
                total_len += taskCost.getTaskNode().getLength();
                newremotelist.add(taskCost.getTaskNode());
            }
            System.out.println(locallist.size() + " : " + remotelist.size());

            long startTime = System.nanoTime();  // Start time
            RaGeneticAlgorithm geneticAlgorithm = new RaGeneticAlgorithm(simManager, newremotelist);
            List<Gene> fittestIndividual = geneticAlgorithm.mapTaskToServer();
            long endTime = System.nanoTime();  // End time
            long duration = endTime - startTime; // Execution time in nanoseconds
            System.out.println("Execution Time: " + duration / 1_000_000.0 + " milliseconds");
            Integer taskIndex = 0;
            for (Gene gene : fittestIndividual) {
                DataCentersManager manager = simManager.getDataCentersManager();
                if (gene.getDcIndex() >= manager.getEdgeDatacenterList().size()) {
                    remotelist.get(taskIndex).getTaskNode().setSelectedComputingNode(manager.getCloudDatacentersList().get(0).nodeList.get(0));
                } else {
                    remotelist.get(taskIndex).getTaskNode().setTaskDecision(TaskNode.TaskDecision.MEC_ONLY);
                    remotelist.get(taskIndex).getTaskNode().setSelectedComputingNode(manager.getEdgeDatacenterList().get(gene.getDcIndex()).nodeList.get(gene.getServerIndex()));
                }

                ++taskIndex;
                if (taskIndex >= remotelist.size()) {
                    break;
                }
            }
            count++;
            if(count >= num_apps)
                break;
        }
    }
}
