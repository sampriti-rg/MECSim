package com.mechalikh.pureedgesim.datacentersmanager;

import java.util.LinkedList;

import com.mechalikh.pureedgesim.energy.EnergyModelComputingNode;
import com.mechalikh.pureedgesim.locationmanager.MobilityModel;
import com.mechalikh.pureedgesim.network.NetworkLink;
import com.mechalikh.pureedgesim.scenariomanager.SimulationParameters.TYPES;
import com.mechalikh.pureedgesim.taskgenerator.Task;

public class ComputingNodeNull implements ComputingNode {

	@Override
	public void submitTask(Task task) {
	}

	@Override
	public TYPES getType() {
		return null;
	}

	@Override
	public void setType(TYPES type) {
	}

	@Override
	public void setName(String name) {
	}

	@Override
	public String getName() {
		return "";
	}

	@Override
	public boolean isOrchestrator() {
		return false;
	}

	@Override
	public void setAsOrchestrator(boolean isOrchestrator) {
	}

	@Override
	public ComputingNode getOrchestrator() {
		return ComputingNode.NULL;
	}

	@Override
	public void enableTaskGeneration(boolean generateTasks) {
	}

	@Override
	public boolean isGeneratingTasks() {
		return false;
	}

	@Override
	public NetworkLink getCurrentUpLink() {
		return NetworkLink.NULL;
	}

	@Override
	public void setCurrentUpLink(NetworkLink currentUpLink) {
	}

	@Override
	public NetworkLink getCurrentDownLink() {
		return NetworkLink.NULL;
	}

	@Override
	public void setCurrentDownLink(NetworkLink currentDownLink) {
	}

	@Override
	public NetworkLink getCurrentWiFiLink() {
		return NetworkLink.NULL;
	}

	@Override
	public void setCurrentWiFiLink(NetworkLink currentWiFiDeviceToDeviceLink) {
	}

	@Override
	public boolean isDead() {
		return false;
	}

	@Override
	public double getDeathTime() {
		return -1;
	}

	@Override
	public EnergyModelComputingNode getEnergyModel() {
		return EnergyModelComputingNode.NULL;
	}

	@Override
	public void setEnergyModel(EnergyModelComputingNode energyModel) {
	}

	@Override
	public MobilityModel getMobilityModel() {
		return MobilityModel.NULL;
	}

	@Override
	public void setMobilityModel(MobilityModel mobilityModel) {
	}

	@Override
	public boolean isPeripheral() {
		return false;
	}

	@Override
	public void setPeriphery(boolean periphery) {
	}

	@Override
	public void setApplicationPlacementLocation(ComputingNode node) {
	}

	@Override
	public ComputingNode getApplicationPlacementLocation() {
		return ComputingNode.NULL;
	}

	@Override
	public boolean isApplicationPlaced() {
		return false;
	}

	@Override
	public void setApplicationPlaced(boolean isApplicationPlaced) {
	}

	@Override
	public double getNumberOfCPUCores() {
		return 0;
	}

	@Override
	public void setNumberOfCPUCores(double numberOfCPUCores) {
	}

	@Override
	public int getApplicationType() {
		return -1;
	}

	@Override
	public void setApplicationType(int applicationType) {
	}

	@Override
	public double getAvailableStorage() {
		return 0;
	}

	@Override
	public void setAvailableStorage(double availableStorage) {
	}

	@Override
	public double getAvgCpuUtilization() {
		return 0;
	}

	@Override
	public double getCurrentCpuUtilization() {
		return 0;
	}

	@Override
	public boolean isIdle() {
		return false;
	}

	@Override
	public void setIdle(boolean isIdle) {
	}

	@Override
	public void addCpuUtilization(Task task) {
	}

	@Override
	public void removeCpuUtilization(Task task) {
	}

	@Override
	public boolean isSensor() {
		return false;
	}

	@Override
	public void setAsSensor(boolean isSensor) {
	}

	@Override
	public LinkedList<Task> getTasksQueue() {
		return new LinkedList<Task>();
	}

	@Override
	public double getTotalStorage() {
		return 0;
	}

	@Override
	public void setStorage(double storage) {
	}

	@Override
	public double getTotalMipsCapacity() {
		return 0;
	}

	@Override
	public void setTotalMipsCapacity(double totalMipsCapacity) {
	}

	@Override
	public double getMipsPerCore() {
		return 0;
	}

	@Override
	public int getId() {
		return -1;
	}
	@Override
	public String getStorageType() {
		return "";
	}
	@Override
	public void setStorageType(String storageType) {
	}

	@Override
	public long getReadBandwidth() {
		return -1;
	}
	@Override
	public void setReadBandwidth(long readBandwidth) {
	}
	@Override
	public long getWriteBandwidth() {
		return -1;
	}
	@Override
	public void setWriteBandwidth(long writeBandwidth) {
	}
	@Override
	public boolean isPimEnabled(){
		return false;
	}
	@Override
	public void setPimEnabled(boolean pimEnabled){

	}
	@Override
	public double getPimMips(){ return 0;}
	@Override
	public void setPimMips(double pimMips){
	}
	@Override
	public double getDataBusBandwidth(){ return 0;}
	@Override
	public void setDataBusBandwidth(double dataBusBandwidth) {}

	@Override
	public void setGpuNumbers(int gpu_num){}

	@Override
	public int getGpuNumbers(){return 0;}

	@Override
	public boolean isGpuEnabled(){return false;}
	@Override
	public void setGpuEnabled(boolean gpuEnabled){}
	@Override
	public double getNumberOfGPUCores() {return 0;}
	@Override
	public void setNumberOfGPUCores(double numberOfGPUCores) {}
	@Override
	public double getGpuMipsPerCore() {return 0;}
	@Override
	public void setGpuMipsPerCore(double gpuMipsPerCore) {}
	@Override
	public boolean isSsdEnabled() {return false;}
	@Override
	public void setSsdEnabled(boolean ssdEnabled) {}

	@Override
	public double getSsdStorage() { return 0;}
	@Override
	public void setSsdStorage(double ssdStorage) {}
	@Override
	public double getSsdReadBw() {return 0;}
	@Override
	public void setSsdReadBw(double ssdReadBw) {}
	@Override
	public double getSsdWriteBw() {return 0;}
	@Override
	public void setSsdWriteBw(double ssdWriteBw) {}
	@Override
	public double getAvailableCores(){return 0;}
	@Override
	public CostPlan getCostPlan(){return null;}
	@Override
	public void setCostPlan(CostPlan costPlan){}
	@Override
	public void setServerType(String serverType) {}
	@Override
	public String getServerType(){return "";}
	@Override
	public long getRam(){return 0;}
	@Override
	public void setRam(long ram){};
	@Override
	public void setTransientFailure(boolean transientFailure){};
	@Override
	public boolean isTransientFailure(){return false;};
	@Override
	public void setNearestDC(DataCenter nearestDC){};
	@Override
	public DataCenter getNearestDC(){return null;}
	@Override
	public DataCenter getDataCenter(){return null;}
	@Override
	public void setDataCenter(DataCenter dc){};
	@Override
	public double getStartTime(){return 0;}
	@Override
	public void setStartTime(double startTime){}
	@Override
	public String getDcName() {return "";}
	@Override
	public void setDcName(String dcName) {}
	@Override
	public boolean isDvfsEnabled(){return false;}
	@Override
	public void setDvfsEnabled(boolean dvfs_enabled){}
	@Override
	public double getPriorityScore(){return  0.0;}
	@Override
	public void setPriorityScore(double priorityScore){}
}
