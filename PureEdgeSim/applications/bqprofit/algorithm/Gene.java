package applications.bqprofit.algorithm;

public class Gene {
    int dcIndex;
    int serverIndex;
    Gene(int dc_index, int svrIndex){
        dcIndex = dc_index;
        serverIndex = svrIndex;
    }
    public int getDcIndex() {
        return dcIndex;
    }

    public void setDcIndex(int dcIndex) {
        this.dcIndex = dcIndex;
    }

    public int getServerIndex() {
        return serverIndex;
    }

    public void setServerIndex(int serverIndex) {
        this.serverIndex = serverIndex;
    }
}