package applications.rmeac;

import com.mechalikh.pureedgesim.simulationmanager.SimLog;
import dag.TaskNode;

import java.util.Map;

/*
* @article{peng2022reliability,
  title={Reliability-aware computation offloading for delay-sensitive applications in mec-enabled aerial computing},
  author={Peng, Kai and Zhao, Bohai and Bilal, Muhammad and Xu, Xiaolong},
  journal={IEEE Transactions on Green Communications and Networking},
  volume={6},
  number={3},
  pages={1511--1519},
  year={2022},
  publisher={IEEE}
}
* */
public class TaskCategory {
    static double VOLUME_FACTOR = 0.6;
    static double DEPENDENCY_FACTOR =  0.4;
    public void assignTaskCriticality(Map<Integer, TaskNode> job){
        double maxVolume = 0;
        int maxSuccPre = 0;
        for (Map.Entry<Integer, TaskNode> task : job.entrySet()) {
            TaskNode taskNode = task.getValue();
            int totalSuccPre = taskNode.successors.size() + taskNode.predecessors.size();
            if(totalSuccPre > maxSuccPre){
                maxSuccPre = totalSuccPre;
            }

            if(taskNode.getFileSize() > maxVolume){
                maxVolume = taskNode.getFileSize();
            }
        }

        for (Map.Entry<Integer, TaskNode> task : job.entrySet()) {
            TaskNode taskNode = task.getValue();
            int totalSuccPre = taskNode.successors.size() + taskNode.predecessors.size();
            if(totalSuccPre > DEPENDENCY_FACTOR*maxVolume){
                taskNode.setCritical(true);
                SimLog.totalCriticalTasks++;
            }

            if(taskNode.getFileSize() > VOLUME_FACTOR*maxVolume){
                taskNode.setCritical(true);
                SimLog.totalCriticalTasks++;
            }
        }
    }
}
