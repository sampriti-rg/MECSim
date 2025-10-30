package applications.bqprofit;

public class ChargingPlan {
    private Double chargePerMi; //CPU cycle
    private Double chargePerMemoryMB; //memory
    private Double chargePerIO; //based on number of IO operations
    private Double penalty;

    private Double hard_deadline_surcharge;
    private Double high_demand_surcharge;
    public Double getHigh_demand_surcharge() {
        return high_demand_surcharge;
    }

    public Double getHard_deadline_surcharge() {
        return hard_deadline_surcharge;
    }

    public void setHard_deadline_surcharge(Double hard_deadline_surcharge) {
        this.hard_deadline_surcharge = hard_deadline_surcharge;
    }
    public void setHigh_demand_surcharge(Double high_demand_surcharge) {
        this.high_demand_surcharge = high_demand_surcharge;
    }

    public Double getPenalty() {
        return penalty;
    }

    public void setPenalty(Double penalty) {
        this.penalty = penalty;
    }

    public Double getChargePerMi() {
        return chargePerMi;
    }

    public void setChargePerMi(Double chargePerMi) {
        this.chargePerMi = chargePerMi;
    }

    public Double getChargePerMemoryMB() {
        return chargePerMemoryMB;
    }

    public void setChargePerMemoryMB(Double chargePerMemoryMB) {
        this.chargePerMemoryMB = chargePerMemoryMB;
    }

    public Double getChargePerIO() {
        return chargePerIO;
    }

    public void setChargePerIO(Double chargePerIO) {
        this.chargePerIO = chargePerIO;
    }

}