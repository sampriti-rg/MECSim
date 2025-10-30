package applications.fptosm;

import com.mechalikh.pureedgesim.datacentersmanager.ComputingNode;
import dag.TaskNode;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class FP_TOSM_Scheduler {
    private double[] vmCriteriaWeights;

    public FP_TOSM_Scheduler() {
        // Calculate VM criteria weights once
        this.vmCriteriaWeights = AHPUtils.calculateWeights(FP_TOSM_Constants.VM_AHP_MATRIX);
    }

    // Algorithm 2: VM Selection and Allocation on a given Fog or Cloud node
    public ComputingNode scheduleTaskOnNode(TaskNode taskToSchedule, List<ComputingNode> vmsOnNode) {
        if (vmsOnNode == null || vmsOnNode.isEmpty()) {
            return null; // No VMs on this node
        }

        // Line 3: Receive Non-Utilized Data (filter VMs that can potentially run the task)
        List<ComputingNode> candidateVMs = new ArrayList<>();
        for (ComputingNode vm : vmsOnNode) {
            if(vm.getAvailableCores() > 0){
                candidateVMs.add(vm);
            }
        }

        if (candidateVMs.isEmpty()) {
            return null; // No VM can execute this task
        }

        // Line 4-11: Start AHP Process to return VMs priorities and calculate C.W
        for (ComputingNode vm : candidateVMs) {
            double vmScore = AHPUtils.calculateVmPriorityScore(vm, this.vmCriteriaWeights);
            vm.setPriorityScore(vmScore); // Store score in VM object
        }

        // Line 11: Return C.W (VM) -> Ž in Descending Order
        candidateVMs.sort(Comparator.comparingDouble(ComputingNode::getPriorityScore).reversed()); // Higher score is better

        // Line 14: Allocate V ∀ -> Ž (Allocate task to the highest priority suitable VM)
        ComputingNode bestVm = candidateVMs.get(0); // Highest priority VM that can execute

        // Simulate allocation (actual resource reduction

        // Line 15: Return execution results to devices (caller handles this after knowing assigned VM)
        return bestVm; // Return the VM to which the task was assigned
    }
}
