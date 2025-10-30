package applications.bqprofit;

public class Cost {
    private Double very_high_costPerMi; //CPU cycle
    private Double high_costPerMi; //CPU cycle
    private Double medium_costPerMi; //CPU cycle
    private Double low_costPerMi; //CPU cycle

    private Double very_high_costPerMemoryMB; //memory
    private Double high_costPerMemoryMB; //memory
    private Double medium_costPerMemoryMB; //memory
    private Double low_costPerMemoryMB; //memory
    private Double very_high_costPerIO; //based on number of IO operations
    private Double high_costPerIO; //based on number of IO operations
    private Double medium_costPerIO; //based on number of IO operations
    private Double low_costPerIO; //based on number of IO operations

    public Double getVery_high_costPerMi() {
        return very_high_costPerMi;
    }

    public void setVery_high_costPerMi(Double very_high_costPerMi) {
        this.very_high_costPerMi = very_high_costPerMi;
    }

    public Double getHigh_costPerMi() {
        return high_costPerMi;
    }

    public void setHigh_costPerMi(Double high_costPerMi) {
        this.high_costPerMi = high_costPerMi;
    }

    public Double getMedium_costPerMi() {
        return medium_costPerMi;
    }

    public void setMedium_costPerMi(Double medium_costPerMi) {
        this.medium_costPerMi = medium_costPerMi;
    }

    public Double getLow_costPerMi() {
        return low_costPerMi;
    }

    public void setLow_costPerMi(Double low_costPerMi) {
        this.low_costPerMi = low_costPerMi;
    }

    public Double getVery_high_costPerMemoryMB() {
        return very_high_costPerMemoryMB;
    }

    public void setVery_high_costPerMemoryMB(Double very_high_costPerMemoryMB) {
        this.very_high_costPerMemoryMB = very_high_costPerMemoryMB;
    }

    public Double getHigh_costPerMemoryMB() {
        return high_costPerMemoryMB;
    }

    public void setHigh_costPerMemoryMB(Double high_costPerMemoryMB) {
        this.high_costPerMemoryMB = high_costPerMemoryMB;
    }

    public Double getMedium_costPerMemoryMB() {
        return medium_costPerMemoryMB;
    }

    public void setMedium_costPerMemoryMB(Double medium_costPerMemoryMB) {
        this.medium_costPerMemoryMB = medium_costPerMemoryMB;
    }

    public Double getLow_costPerMemoryMB() {
        return low_costPerMemoryMB;
    }

    public void setLow_costPerMemoryMB(Double low_costPerMemoryMB) {
        this.low_costPerMemoryMB = low_costPerMemoryMB;
    }

    public Double getVery_high_costPerIO() {
        return very_high_costPerIO;
    }

    public void setVery_high_costPerIO(Double very_high_costPerIO) {
        this.very_high_costPerIO = very_high_costPerIO;
    }

    public Double getHigh_costPerIO() {
        return high_costPerIO;
    }

    public void setHigh_costPerIO(Double high_costPerIO) {
        this.high_costPerIO = high_costPerIO;
    }

    public Double getMedium_costPerIO() {
        return medium_costPerIO;
    }

    public void setMedium_costPerIO(Double medium_costPerIO) {
        this.medium_costPerIO = medium_costPerIO;
    }

    public Double getLow_costPerIO() {
        return low_costPerIO;
    }

    public void setLow_costPerIO(Double low_costPerIO) {
        this.low_costPerIO = low_costPerIO;
    }

}
