package applications;

import applications.bqprofit.BQProfitOrchestrator;
import applications.delta.DeltaOrchestrator;
import applications.fptosm.FpTosmOrchestrator;
import applications.remec.ReMECOrchestrator;
import applications.rmeac.RmeacOrchestrator;
import com.mechalikh.pureedgesim.simulationmanager.Simulation;
import common.GlobalConfig;
import dag.DependentTaskGenerator;

public class Main {
//    private static String settingsPath = "PureEdgeSim/applications/remec/settings/";
//    private static String outputPath = "PureEdgeSim/applications/remec/output/";
//    private static String settingsPath = "PureEdgeSim/applications/bqprofit/settings/";
//    private static String outputPath = "PureEdgeSim/applications/bqprofit/output/";
//    private static String settingsPath = "PureEdgeSim/applications/delta/settings/";
//    private static String outputPath = "PureEdgeSim/applications/delta/output/";
    private static String settingsPath = "PureEdgeSim/applications/rmeac/settings/";
    private static String outputPath = "PureEdgeSim/applications/rmeac/output/";
//    private static String settingsPath = "PureEdgeSim/applications/fptosm/settings/";
//    private static String outputPath = "PureEdgeSim/applications/fptosm/output/";
    public Main(){
        Simulation sim = new Simulation();
        sim.setCustomOutputFolder(outputPath);
        sim.setCustomSettingsFolder(settingsPath, "PureEdgeSim/sim_settings/");
        sim.setCustomTaskGenerator(DependentTaskGenerator.class);
        if(GlobalConfig.getInstance().getAlgorithm().equals("DELTA")) {
            sim.setCustomEdgeOrchestrator(DeltaOrchestrator.class);
        } else if (GlobalConfig.getInstance().getAlgorithm().equals("BQProfit")) {
            sim.setCustomEdgeOrchestrator(BQProfitOrchestrator.class);
        } else if (GlobalConfig.getInstance().getAlgorithm().equals("ReMEC")) {
            sim.setCustomEdgeOrchestrator(ReMECOrchestrator.class);
        } else if (GlobalConfig.getInstance().getAlgorithm().equals("RMEAC")) {
            sim.setCustomEdgeOrchestrator(RmeacOrchestrator.class);
        } else if (GlobalConfig.getInstance().getAlgorithm().equals("FP-TOSM")) {
            sim.setCustomEdgeOrchestrator(FpTosmOrchestrator.class);
        } else{
            System.out.println("Invalid algorithm provided");
            System.exit(0);
        }
        sim.launchSimulation();;
    }

    public static void main(String args[]){
        new Main();
    }
}