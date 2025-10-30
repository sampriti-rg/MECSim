package applications.bqprofit;

import common.GlobalConfig;
import common.Helper;
import common.Job;
import dag.TaskNode;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class ScientificWorkflowParser {
    Map<Integer, Map<Integer, TaskNode>> tasksMap;
    Map<Integer, Job> jobsMap;
    ChargingPlan chargingPlan;

    public ScientificWorkflowParser(Map<Integer, Map<Integer, TaskNode>> tMap,
                                    ChargingPlan cPlan, Map<Integer, Job> jMap){
        tasksMap = tMap;
        chargingPlan = cPlan;
        jobsMap = jMap;
    }

    public static List<Path> listFiles(Path path) throws IOException {

        List<Path> result;
        try (Stream<Path> walk = Files.walk(path)) {
            result = walk.filter(Files::isRegularFile)
                    .collect(Collectors.toList());
        }
        return result;

    }

    public void loadTasks(){
        try {
            Path path = Paths.get(GlobalConfig.getInstance().getScientificWorkFlowDir());
            List<Path> paths = listFiles(path);
            Integer jobId = 0;
            for (Path file_path : paths) {
                //System.out.println(file_path);
                String outFileName = String.valueOf(file_path.getFileName());
                if(outFileName.endsWith(".swp"))
                    continue;

                String[] parts = outFileName.split("\\.")[0].split("_");
                if(parts.length == 3){
                    jobId = Integer.valueOf(parts[2]);
                }

                File xmlFile = new File(String.valueOf(file_path));
                DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
                DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
                Document doc = dBuilder.parse(xmlFile);
                NodeList job_nodes = doc.getElementsByTagName("job");
                Map<Integer, TaskNode> tMap = null;
                Job job = new Job();
                job.setJobID(String.valueOf(jobId));
                for (int i = 0; i < job_nodes.getLength(); i++) {
                    Element jobElement = (Element) job_nodes.item(i);
                    Integer id = Integer.valueOf(jobElement.getAttribute("id").substring(2))+1;

                    Element taskLengthElement = (Element) jobElement.getElementsByTagName("task_length").item(0);
                    Integer taskLength = (int) Math.ceil(Double.valueOf(taskLengthElement.getTextContent()));

                    TaskNode taskNode =new TaskNode(id, taskLength);

                    Element memoryElement = (Element) jobElement.getElementsByTagName("memory").item(0);
                    Double memory = Double.valueOf(memoryElement.getTextContent());

                    Element rdIoElement = (Element) jobElement.getElementsByTagName("rd_io").item(0);
                    Long rdIo = (long) Math.abs(Double.valueOf(rdIoElement.getTextContent()));

                    Element wrIoElement = (Element) jobElement.getElementsByTagName("wr_io").item(0);
                    Long wrIo = (long) Math.abs(Double.valueOf(wrIoElement.getTextContent()));

                    Element deadlineElement = (Element) jobElement.getElementsByTagName("deadline").item(0);
                    Double deadline = Double.valueOf(deadlineElement.getTextContent());

                    Element deadlineTypeElement = (Element) jobElement.getElementsByTagName("deadline_type").item(0);
                    String deadlineType = deadlineTypeElement.getTextContent();

                    taskNode.setMemoryNeed(memory);
                    taskNode.setReadOps(rdIo);
                    taskNode.setWriteOps(wrIo);
                    taskNode.setMaxLatency(deadline);
                    taskNode.setDeadlineType(deadlineType);

                    taskNode.setStorageNeed((double) Helper.getRandomInteger(500, 1000));
                    taskNode.setFileSize(Helper.getRandomInteger(80000, 4000000)).setOutputSize(Helper.getRandomInteger(80000, 4000000));
                    taskNode.setTaskDecision(TaskNode.TaskDecision.OPEN);
                    taskNode.setJob(job);

                    taskNode.setApplicationID(jobId);
                    taskNode.setId(id);
                    taskNode.setContainerSize(25000);
                    taskNode.setChargingPlan(this.chargingPlan);

                    if (tasksMap.containsKey(jobId)) {
                        tasksMap.get(jobId).put(id, taskNode);
                    } else {
                        tMap = new HashMap<>();
                        tMap.put(id, taskNode);
                        tasksMap.put(jobId, tMap);
                    }
                }

                NodeList child_nodes = doc.getElementsByTagName("child");
                for(int j=0; j<child_nodes.getLength();j++){
                    Element childElement = (Element) child_nodes.item(j);
                    Integer child_id = Integer.valueOf(childElement.getAttribute("ref").substring(2))+1;
                    TaskNode childTaskNode = tMap.get(child_id);
                    if(childTaskNode == null){
                        System.out.println("Got task node as NULL that is not valid");
                        throw new RuntimeException();
                    }
                    NodeList parents_node = childElement.getElementsByTagName("parent");
                    for(int k=0; k<parents_node.getLength(); k++){
                        Element parentElement = (Element) parents_node.item(k);
                        Integer parent_id = Integer.valueOf(parentElement.getAttribute("ref").substring(2))+1;
                        TaskNode parentTaskNode = tMap.get(parent_id);
                        if(parentTaskNode == null){
                            System.out.println(childTaskNode.getId());
                        }
                        childTaskNode.predecessors.add(parentTaskNode);
                        parentTaskNode.successors.add(childTaskNode);
                    }
                }

                setStartEndTaskFlag(tMap, job, jobId);

                job.setJobID(String.valueOf(jobId));
                job.setTaskMap(tMap);
                if(GlobalConfig.getInstance().getAlgorithm().equals("BQProfit")) {
                    job.setBudget(Helper.getBudget(tMap, chargingPlan));
                }
                jobsMap.put(jobId, job);
                jobId++;
            }
        }catch (Exception e){
            e.printStackTrace();
        }
    }

    void setStartEndTaskFlag(Map<Integer, TaskNode> tMap, Job job, Integer jobId){
        int startCount = 0;
        int endCount = 0;
        int max_id = Integer.MAX_VALUE;
        for(Map.Entry<Integer, TaskNode> task : tMap.entrySet()){
            TaskNode taskNode = task.getValue();
            if(taskNode.predecessors.size() ==0){
                taskNode.setStartTask(true);
                startCount++;
            } else if(taskNode.successors.size() == 0){
                taskNode.setEndTask(true);
                endCount++;
            }

            if(taskNode.getId() > max_id){
                max_id = taskNode.getId();
            }
        }

        if(startCount > 1){
            TaskNode dummyTaskNode = new TaskNode(0, 1);
            dummyTaskNode.setApplicationID(jobId);
            dummyTaskNode.setMemoryNeed(1.0);
            dummyTaskNode.setFileSize(1);
            dummyTaskNode.setReadOps(1);
            dummyTaskNode.setWriteOps(1);
            dummyTaskNode.setTaskDecision(TaskNode.TaskDecision.UE_ONLY);
            dummyTaskNode.setStartTask(true);
            dummyTaskNode.setDeadlineType("soft");
            dummyTaskNode.setMaxLatency(1);
            dummyTaskNode.setJob(job);
            dummyTaskNode.setChargingPlan(chargingPlan);
            dummyTaskNode.setStorageNeed(1.0);
            dummyTaskNode.setMemoryNeed(1.0);
            for(Map.Entry<Integer, TaskNode> task : tMap.entrySet()){
                TaskNode taskNode = task.getValue();
                if(taskNode.isStartTask()){
                    dummyTaskNode.successors.add(taskNode);
                    taskNode.predecessors.add(dummyTaskNode);
                    taskNode.setStartTask(false);
                }
            }

            dummyTaskNode.setStartTask(true);
            tMap.put(0, dummyTaskNode);
        }

        if(endCount >1){
            TaskNode dummyTaskNode = new TaskNode(max_id+1, 1);
            dummyTaskNode.setApplicationID(jobId);
            dummyTaskNode.setMemoryNeed(1.0);
            dummyTaskNode.setFileSize(1);
            dummyTaskNode.setReadOps(1);
            dummyTaskNode.setWriteOps(1);
            dummyTaskNode.setTaskDecision(TaskNode.TaskDecision.UE_ONLY);
            dummyTaskNode.setStartTask(true);
            dummyTaskNode.setDeadlineType("soft");
            dummyTaskNode.setMaxLatency(1);
            dummyTaskNode.setJob(job);
            dummyTaskNode.setChargingPlan(chargingPlan);
            for(Map.Entry<Integer, TaskNode> task : tMap.entrySet()){
                TaskNode taskNode = task.getValue();
                if(taskNode.isStartTask()){
                    dummyTaskNode.predecessors.add(taskNode);
                    taskNode.successors.add(dummyTaskNode);
                    taskNode.setEndTask(false);
                }
            }

            dummyTaskNode.setEndTask(true);
            tMap.put(max_id+1, dummyTaskNode);
        }
    }

}