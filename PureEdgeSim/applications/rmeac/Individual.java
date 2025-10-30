package applications.rmeac;

import java.util.ArrayList;
import java.util.List;

public class Individual {
    private List<OffloadingDecision> offloadingDecisions;
    private double fitness;

    public Individual() {
        this.offloadingDecisions = new ArrayList<>();
    }

    public List<OffloadingDecision> getOffloadingDecisions() {
        return offloadingDecisions;
    }

    public void addOffloadingDecision(OffloadingDecision offloadingDecision) {
        this.offloadingDecisions.add(offloadingDecision);
    }

    public void setOffloadingDecision(int index, OffloadingDecision offloadingDecision) {
        this.offloadingDecisions.set(index, offloadingDecision);
    }

    public double getFitness() {
        return fitness;
    }

    public void setFitness(double fitness) {
        this.fitness = fitness;
    }
}
