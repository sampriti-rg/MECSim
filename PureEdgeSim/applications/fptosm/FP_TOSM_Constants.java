package applications.fptosm;

public class FP_TOSM_Constants {
    // AHP Matrix for Task Prioritization (based on Fig. 4, criteria: RAM, MIPS, BW, NodeEnergy)
    // Rows/Cols: RAM, MIPS, BW, NodeEnergy
    public static final double[][] TASK_AHP_MATRIX = {
            {1.0, 1.0/3.0, 1.0/5.0, 1.0/3.0}, // RAM vs others (Example: RAM less important than MIPS)
            {3.0, 1.0,     1.0/3.0, 1.0},     // MIPS vs others
            {5.0, 3.0,     1.0,     3.0},     // BW vs others
            {3.0, 1.0,     1.0/3.0, 1.0}      // NodeEnergy vs others
            // These values are illustrative and need careful calibration based on paper/domain.
            // The paper's Eq.2 uses 1, 1/3, 1/5 relative to diagonal '1'.
            // A_ij = 1/A_ji. Diagonal is 1.
    };
    // This matrix needs to be consistent. For example, from Eq 2 in the paper:
    // Mp = [ 1   1/3 1/5 ... ]
    //      [ 3   1   1/3 ... ]
    //      [ 5   3   1   ... ]
    // This implies the *column* criterion is being compared to the *row* criterion.
    // If M_ij = 3, it means criterion i is 3 times more important than criterion j.
    // Let's re-define based on importance: Higher value means row criterion is more important than col criterion
    // Criteria order: RAM, MIPS, BW, NodeEnergyDemand
    public static final double[][] TASK_AHP_CRITERIA_MATRIX_EXAMPLE = {
            // RAM    MIPS    BW     NodeEnergyDemand
            {1.0,   1.0/3,  1.0/5,  1.0/2},  // RAM (less important than MIPS, much less than BW)
            {3.0,   1.0,    1.0/2,  2.0},    // MIPS
            {5.0,   2.0,    1.0,    3.0},    // BW (most important)
            {2.0,   1.0/2,  1.0/3,  1.0}     // NodeEnergyDemand
    };


    // AHP Matrix for VM Selection (Criteria: MIPS, RAM, BW, NodeEnergyFactor of VM)
    // Rows/Cols: MIPS_VM, RAM_VM, BW_VM, EnergyFactor_VM (lower is better)
    public static final double[][] VM_AHP_MATRIX = {
            {1.0, 2.0, 2.0, 3.0}, // MIPS_VM (more MIPS is better)
            {1.0/2.0, 1.0, 1.0, 2.0}, // RAM_VM (more RAM is better)
            {1.0/2.0, 1.0, 1.0, 2.0}, // BW_VM (more BW is better)
            {1.0/3.0, 1.0/2.0, 1.0/2.0, 1.0}  // EnergyFactor_VM (LOWER energy factor is better)
            // For criteria where lower is better (like EnergyFactor), the AHP scaling needs inversion or careful handling.
            // Or, use 1/EnergyFactor in comparisons.
    };

    public static final double BETA_THRESHOLD_LOCAL_FOG = 1650; // Tasks with score <= 30 are local
    public static final double BETA_THRESHOLD_FOG_CLOUD = 2500; // Tas

//    public static final double BETA_THRESHOLD_LOCAL_FOG = 3000; // Tasks with score <= 3000 are local
//    public static final double BETA_THRESHOLD_FOG_CLOUD = 10000; // Tas
}
