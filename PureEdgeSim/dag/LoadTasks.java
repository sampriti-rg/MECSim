package dag;

import com.mechalikh.pureedgesim.datacentersmanager.ComputingNode;
import com.mechalikh.pureedgesim.simulationmanager.SimulationManager;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;

public class LoadTasks {
    private String taskDir;
    private List<? extends ComputingNode> devices;
    SimulationManager simManager;
    public LoadTasks(String dir, SimulationManager simulationManager,
                     List<? extends ComputingNode> devicesList){
        taskDir = dir;
        simManager = simulationManager;
        devices = devicesList;
    }

    public Map<Integer, List<TaskNode>> load(){
        System.out.println(taskDir);
        Map<Integer, List<TaskNode>> dags = new HashMap<>();
        try {
            List<String> file_list = walkDirTree(taskDir);
            for(String file : file_list){
                File f = new File(file);
                if(f.isDirectory())
                    continue;
                String file_name = f.getName();
                //System.out.println("file-path " +file+ " name " +file_name);
                Integer app_id = Integer.valueOf(file_name.split("_")[1]);
                if(devices.size() > 0) {
                    List<TaskNode> tasks = createDag(app_id, file, devices.get(0));
                    dags.put(app_id, tasks);
                    devices.remove(0);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return dags;
    }

    private void assignLabel(TaskNode taskNode, Integer label) {
        List<TaskNode> successors = taskNode.successors;
        for(TaskNode task : successors){
            task.setLevel(label + 1);
            assignLabel(task, label + 1);
        }
    }

    private List<TaskNode> createDag(Integer app_id, String file_name, ComputingNode ue_device) {
        BufferedReader reader;
        Map<Integer, TaskNode> taskMap = new HashMap<>();
        Map<Integer, List<Integer>> dependencyMap = new HashMap<>();
        try {
            reader = new BufferedReader(new FileReader(file_name));
            String line = reader.readLine();
            while (line != null){
                createTask(line, ue_device, dependencyMap, taskMap,
                        Integer.valueOf(app_id));
                line = reader.readLine();
            }
        } catch (IOException e){
            e.printStackTrace();
        }

        //filling sucessors
        for(Map.Entry<Integer, List<Integer>> entry : dependencyMap.entrySet()){
            if(taskMap.containsKey(entry.getKey())){
                TaskNode taskNode = taskMap.get(entry.getKey());
                List<Integer> children = entry.getValue();
                String child ="";
                for (Integer id : children){
                    if(taskMap.containsKey(id)){
                        child = child + " ," + taskMap.get(id).getId();
                        taskNode.successors.add(taskMap.get(id));
                    }
                }
            }
        }


        //filling predecessors
        for(Map.Entry<Integer, List<Integer>> entry : dependencyMap.entrySet()){
            List<Integer> children = entry.getValue();
            for (Integer id : children){
                if(taskMap.containsKey(id)){
                    TaskNode taskNode = taskMap.get(id);
                    if(taskMap.containsKey(entry.getKey())) {
                        taskNode.predecessors.add(taskMap.get(entry.getKey()));
                    }
                }
            }
        }

        if(taskMap.containsKey(0)){
            assignLabel(taskMap.get(0), 0);
        } else{
            System.out.println("Root task not found");
        }

        ArrayList<TaskNode> task_list = new ArrayList<TaskNode>(taskMap.values());

        return task_list;
    }

    private void createTask(String line, ComputingNode ue_device,
                            Map<Integer, List<Integer>> dependencyMap,
                            Map<Integer, TaskNode> taskMap, Integer app_id) {
        String fields[] = line.split(" ");
        if(fields.length >= 6) {
            int task_id = Integer.valueOf(fields[1]);
            String children_str = fields[2];
            String node_type = fields[3];
            String cost_str = fields[4];
            long length = 0;
            long requestSize = 0;
            long ouputSize = 0;
            if(cost_str.length() > 6){
                length = Long.valueOf(cost_str.substring(0, 5));
                requestSize = Long.valueOf(cost_str.substring(0, 4));
                double random = 0.6 + Math.random() * (1.5 - 0.6);
                ouputSize = (long) (random*requestSize);
            }

            String parallelization_overhead = fields[5];

            //TaskNode task = new TaskNode(task_id, length);
            TaskNode task = new TaskNode(task_id, 60000);
            task.setApplicationID(Integer.valueOf(app_id));
            //task.setFileSize(requestSize).setOutputSize(ouputSize);
            task.setFileSize(1500).setOutputSize(100);
            double random = 1.5 + Math.random() * (2.0 - 1.5);
            //task.setContainerSize((long) (random*requestSize));
            task.setContainerSize(25000);
            task.setRegistry(simManager.getDataCentersManager().getCloudDatacentersList().get(0).nodeList.get(0));
            task.setId(task_id);
            task.setMaxLatency(100);
            task.setEdgeDevice(ue_device);
            taskMap.put(task_id, task);
            if(!node_type.equals("END")){
                String children[] = children_str.strip().split(",");
                if(children.length >0) {
                    List<Integer> child_ist = new ArrayList<>();
                    for (String child : children) {
                        child_ist.add(Integer.valueOf(child));
                    }
                    dependencyMap.put(task_id, child_ist);
                }
            } else{
                List<Integer> child_ist = new ArrayList<>();
                task.setEndTask(true);
                dependencyMap.put(task_id, child_ist);
            }

            if(node_type.equals("ROOT")){
                task.setStartTask(true);
            }
        }
    }

    private static List<String> walkDirTree(String rootFolder) throws Exception {
        List<String> file_list = new ArrayList<>();
        Files.walk(Paths.get(rootFolder)).forEach(path -> {
            file_list.add(String.valueOf(path));
        });

        return file_list;
    }
}
