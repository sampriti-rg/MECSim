package com.mechalikh.pureedgesim.datacentersmanager;

public class CostPlan {
    private Double costPerMi;
    private Double costPerMB;
    private Double costPerIo;
    public void setCostPerMi(Double costPerMi) {
        this.costPerMi = costPerMi;
    }

    public void setCostPerMB(Double costPerMB) {
        this.costPerMB = costPerMB;
    }

    public void setCostPerIo(Double costPerIo) {
        this.costPerIo = costPerIo;
    }

    public Double getCostPerMi() {
        return costPerMi;
    }

    public Double getCostPerMB() {
        return costPerMB;
    }

    public Double getCostPerIo() {
        return costPerIo;
    }
}
