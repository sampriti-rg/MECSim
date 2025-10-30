package applications.fptosm;

import com.mechalikh.pureedgesim.datacentersmanager.ComputingNode;
import common.Helper;
import dag.TaskNode;


public class AHPUtils {
    /**
     * Calculates priority weights from a pairwise comparison matrix.
     * @param matrix The square pairwise comparison matrix.
     * @return An array of weights for each criterion.
     */
    public static double[] calculateWeights(double[][] matrix) {
        int n = matrix.length;
        double[] columnSums = new double[n];
        double[][] normalizedMatrix = new double[n][n];

        // 1. Sum columns
        for (int j = 0; j < n; j++) {
            for (int i = 0; i < n; i++) {
                columnSums[j] += matrix[i][j];
            }
        }

        // 2. Normalize matrix
        for (int i = 0; i < n; i++) {
            for (int j = 0; j < n; j++) {
                normalizedMatrix[i][j] = matrix[i][j] / columnSums[j];
            }
        }

        // 3. Calculate weights (average of rows in normalized matrix)
        double[] weights = new double[n];
        for (int i = 0; i < n; i++) {
            for (int j = 0; j < n; j++) {
                weights[i] += normalizedMatrix[i][j];
            }
            weights[i] /= n;
        }
        return weights;
    }

    /**
     * Calculates the AHP priority score for a task.
     * Assumes task attributes are normalized or scaled appropriately before applying weights.
     * For criteria where "lower is better" (e.g., energy demand), the input score should be inverted
     * or the weight applied negatively if not handled in the AHP matrix itself.
     * This implementation sums weighted values, as implied by Eq 1 combined with Eq 8 context (β = Φ+£+д+Е).
     */
    public static double calculateTaskPriorityScore(TaskNode task, double[] criteriaWeights) {
        // Criteria order: RAM, MIPS, BW, NodeEnergyDemand
        // These task getters should return values possibly normalized or scaled appropriately.
        // For simplicity, we're directly using them.
        // The paper implies summing weighted parameters (Eq 1, and beta = sum of weighted Ti).
        // A higher score means higher priority / more resource intensive.

        // Let's assume criteriaWeights are for: RAM_req, MIPS_req, BW_req, EnergyDemand_req
        // And the task values are the actual requirements.
        // The AHP matrix should reflect "importance". If a criterion is more important, its weight is higher.
        // A task needing more of an important resource gets a higher score contribution from that.

        double energy = Helper.dynamicEnergyConsumption(task.getLength(), task.getEdgeDevice().getMipsPerCore(), task.getEdgeDevice().getMipsPerCore());
        double score = 0;
        score += criteriaWeights[0] * task.getMemoryNeed();       // Weight for RAM * RAM needed
        score += criteriaWeights[1] * task.getLength();       // Weight for MIPS * MIPS needed
        score += criteriaWeights[2] * 150;     // Weight for BW * BW needed
        score += criteriaWeights[3] * energy; // Weight for Energy * Energy needed (higher impact = higher score)
        return score;
    }

    /**
     * Calculates the AHP priority score for a VM.
     * For criteria where "lower is better" (e.g., nodeEnergyFactor), use 1/value or ensure matrix reflects this.
     */
    public static double calculateVmPriorityScore(ComputingNode vm, double[] criteriaWeights) {
        // Criteria order: MIPS_VM, RAM_VM, BW_VM, EnergyFactor_VM
        // Higher score means more suitable VM.
        double score = 0;
        score += criteriaWeights[0] * vm.getMipsPerCore();
        score += criteriaWeights[1] * 1500;
        score += criteriaWeights[2] * (vm.getReadBandwidth()+vm.getWriteBandwidth());
        // For EnergyFactor, lower is better. So, if AHP matrix is for "goodness", use 1/factor
        // Or ensure criteriaWeights[3] is for "1/EnergyFactor" or AHP matrix handles inversion.
        // Assuming criteriaWeights[3] is for a "lower is better" type of preference:
        // score -= criteriaWeights[3] * vm.nodeEnergyFactor; // If weight is positive importance
        // Or, if AHP matrix handles it (e.g. comparing 1/E1 vs 1/E2):
        double nodeEnergyFactor = Helper.generateRandomFloat(10, 100);
        score += criteriaWeights[3] * (nodeEnergyFactor > 0 ? (1.0 / nodeEnergyFactor) : Double.MAX_VALUE); // Higher score for lower factor
        return score;
    }
}
