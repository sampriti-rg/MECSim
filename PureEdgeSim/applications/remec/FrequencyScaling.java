package applications.remec;

import com.mechalikh.pureedgesim.datacentersmanager.ComputingNode;
import com.mechalikh.pureedgesim.taskgenerator.Task;

public class FrequencyScaling {
    private static final double EPSILON = 1e-4;  // Convergence threshold
    private static final int MAX_ITERATIONS = 100;  // Safety limit
    private static double d = 0.1;        // Exponent factor
    //static double LAMBDA_0 = Math.pow(10, -9); //same as TaskReliability.java
    static double LAMBDA_0 = Math.pow(10, -3); //same as TaskReliability.java
    //static double RELIABLE_THRESHOLD = 0.015; //same as OffloadingAndScheduling.java
    static double RELIABLE_THRESHOLD = 0.02; //same as OffloadingAndScheduling.java

    /**
     * Computes the failure rate based on the given frequency.
     * @param lambda0 Base failure rate
     * @param d Failure rate exponent factor
     * @param f Current frequency
     * @param fMin Minimum frequency
     * @param tau Task length (in cycles)
     * @return Failure rate (R_i)
     */
    private static double computeFailureRate(double lambda0, double d, double f, double fMin, double tau) {
        double lambdaF = lambda0 * Math.pow(10, d * (1 - f) / (1 - fMin));
        return Math.exp(-lambdaF * tau / f);
    }

    static double calculateLambda(double fi, double fmin){
        double pow_coeff = d*(1-fi)/(1-fmin);
        return LAMBDA_0*Math.pow(10, pow_coeff);
    }

    /**
     * Safe Approximation Algorithm to find the optimal CPU frequency.
     * @param fMin Minimum CPU frequency (e.g., in GHz or MIPS)
     * @param fMax Maximum CPU frequency (e.g., in GHz or MIPS)
     * @param tau Task length (in cycles)
     * @return Approximate optimal frequency
     */
    private static double findOptimalFrequency(double fMin, double fMax, double tau) {
        double fi = fMin;

        for (int i = 0; i < MAX_ITERATIONS; i++) {
            double lambda = calculateLambda(fi, fMin);

            double fiNext = fi+fi*(-(lambda * tau)/Math.log(RELIABLE_THRESHOLD));

            fiNext = Math.max(fMin, Math.min(fiNext, fMax));

            // Convergence check
            if (Math.abs(fiNext - fi) < EPSILON) {
                return fiNext;
            }

            // Update frequency for the next iteration
            fi = fiNext;
        }

        // Return final frequency after max iterations (best guess)
        //System.out.println("fi: "+fi+" fmin: "+fMin+" fmax: "+fMax);
        return fi;
    }

    public static double getOptimalFrequency(Task task){
        ComputingNode edgeDevice = task.getEdgeDevice();
        double max_freq = edgeDevice.getMipsPerCore();
        double min_freq = task.getLength()/task.getMaxLatency();
        double min_freq_avg = (min_freq+max_freq)/2;
        double f =  findOptimalFrequency(min_freq_avg, max_freq, task.getMaxLatency());
        return f;
    }
}
