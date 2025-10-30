/**
 *     PureEdgeSim:  A Simulation Framework for Performance Evaluation of Cloud, Edge and Mist Computing Environments 
 *
 *     This file is part of PureEdgeSim Project.
 *
 *     PureEdgeSim is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 *
 *     PureEdgeSim is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU General Public License for more details.
 *
 *     You should have received a copy of the GNU General Public License
 *     along with PureEdgeSim. If not, see <http://www.gnu.org/licenses/>.
 *     
 *     @author Charafeddine Mechalikh
 **/
package com.mechalikh.pureedgesim.datacentersmanager;

import java.io.FileInputStream;
import java.io.InputStream;
import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import common.GlobalConfig;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import com.mechalikh.pureedgesim.energy.EnergyModelComputingNode;
import com.mechalikh.pureedgesim.locationmanager.Location;
import com.mechalikh.pureedgesim.locationmanager.MobilityModel;
import com.mechalikh.pureedgesim.scenariomanager.SimulationParameters;
import com.mechalikh.pureedgesim.scenariomanager.SimulationParameters.TYPES;
import com.mechalikh.pureedgesim.simulationmanager.SimulationManager;
import com.mechalikh.pureedgesim.taskgenerator.Task;

/**
 * This class is responsible for generating the computing resources from the
 * input files ( @see
 * com.mechalikh.pureedgesim.simulationcore.SimulationAbstract#setCustomSettingsFolder(String))
 * 
 * @author Charafeddine Mechalikh
 * @since PureEdgeSim 1.0
 */
public class ComputingNodesGenerator {
	/**
	 * List of edge/fog data centers.
	 * 
	 * @see #generateDataCenters(String, TYPES)
	 */
	private List<DataCenter> edgeDataCentersList;

	/**
	 * List of cloud data centers.
	 * 
	 * @see #generateDataCenters(String, TYPES)
	 */
	private List<DataCenter> cloudDataCentersList;

	/**
	 * List of edge devices.
	 * 
	 * @see #generateEdgeDevices()
	 */
	private List<ComputingNode> edgeDevicesList;

	/**
	 * The list of all resources, that will be used by the orchestrator.
	 * 
	 * @see com.mechalikh.pureedgesim.taskorchestrator.Orchestrator
	 */
	private List<ComputingNode> computingNodesList;

	/**
	 * The list of orchestrators. It is used by the simulation manager when the
	 * orchestrators are deloyed to computing nodes. In this case, the tasks are
	 * sent over the network to one of the orchestrators to make decisions.
	 * 
	 * @see #generateDataCenters(String, TYPES)
	 * @see com.mechalikh.pureedgesim.simulationmanager.SimulationManager#sendTaskToOrchestrator(Task)
	 */
	private List<ComputingNode> orchestratorsList;

	/**
	 * The simulation manager.
	 */
	private SimulationManager simulationManager;

	/**
	 * The Mobility Model to be used in this scenario
	 * 
	 * @see com.mechalikh.pureedgesim.simulationmanager.SimulationThread#loadModels(SimulationManager)
	 */
	private Class<? extends MobilityModel> mobilityModelClass;

	/**
	 * The Computing Node Class to be used in this scenario
	 * 
	 * @see com.mechalikh.pureedgesim.simulationmanager.SimulationThread#loadModels(SimulationManager)
	 */
	private Class<? extends ComputingNode> computingNodeClass;

	/**
	 * Initializes the Computing nodes generator.
	 *
	 * @param simulationManager  The simulation Manager
	 * @param mobilityModelClass The mobility model that will be used in the
	 *                           simulation
	 * @param computingNodeClass The computing node class that will be used to
	 *                           generate computing resources
	 */
	public ComputingNodesGenerator(SimulationManager simulationManager,
			Class<? extends MobilityModel> mobilityModelClass, Class<? extends ComputingNode> computingNodeClass) {
		this.mobilityModelClass = mobilityModelClass;
		this.computingNodeClass = computingNodeClass;
		this.simulationManager = simulationManager;
		edgeDataCentersList = new ArrayList<>(10);
		cloudDataCentersList = new ArrayList<>(5);
		edgeDevicesList = new ArrayList<>(simulationManager.getScenario().getDevicesCount());
		computingNodesList = new ArrayList<>(simulationManager.getScenario().getDevicesCount() + 15);
		orchestratorsList = new ArrayList<>(simulationManager.getScenario().getDevicesCount());

	}
	public DataCenter getNearestDC(ComputingNode edgeDevice){
		DataCenter nearestDC = null;
		double min_dist = Double.MAX_VALUE;
		for(DataCenter dataCenter : edgeDataCentersList){
			double currDistance = dataCenter.nodeList.get(0).getMobilityModel().distanceTo(edgeDevice);
			if(currDistance < min_dist){
				min_dist = currDistance;
				nearestDC = dataCenter;
			}
		}

		return nearestDC;
	}

	public void pointNearestDCWithEdgeDevice(){
		for(ComputingNode edgeDevice : edgeDevicesList){
			DataCenter dc = getNearestDC(edgeDevice);
			edgeDevice.setNearestDC(dc);
		}
	}

	/**
	 * Generates all computing nodes, including the Cloud data centers, the edge
	 * ones, and the edge devices.
	 */
	public void generateDatacentersAndDevices() {
		try {
			// Generate Edge and Cloud data centers.
			generateDataCenters(SimulationParameters.CLOUD_DATACENTERS_FILE, SimulationParameters.TYPES.CLOUD);
			SimulationParameters.NUM_OF_CLOUD_DATACENTERS = cloudDataCentersList.size();
			generateDataCenters(SimulationParameters.EDGE_DATACENTERS_FILE, SimulationParameters.TYPES.EDGE_DATACENTER);
			SimulationParameters.NUM_OF_EDGE_DATACENTERS = edgeDataCentersList.size();

			// Generate edge devices.
			generateEdgeDevices();
			this.pointNearestDCWithEdgeDevice();
		} catch (Exception e) {
			e.printStackTrace();
			System.out.print(false);
		}

		getSimulationManager().getSimulationLogger()
				.print(getClass().getSimpleName() + " - Datacenters and devices were generated");

	}

	/**
	 * Generates edge devices
	 */
	public void generateEdgeDevices() throws Exception {

		// Generate edge devices instances from edge devices types in xml file.
		InputStream devicesFile = new FileInputStream(SimulationParameters.EDGE_DEVICES_FILE);
		DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
		DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
		Document doc = dBuilder.parse(devicesFile);
		NodeList nodeList = doc.getElementsByTagName("device");
		Element edgeElement = null;

		// Load all devices types in edge_devices.xml file.
		for (int i = 0; i < nodeList.getLength(); i++) {
			Node edgeNode = nodeList.item(i);
			edgeElement = (Element) edgeNode;
			generateDevicesInstances(edgeElement);
		}

		// if percentage of generated devices is < 100%.
		if (edgeDevicesList.size() < getSimulationManager().getScenario().getDevicesCount())
			getSimulationManager().getSimulationLogger().print(getClass().getSimpleName()
					+ " - Wrong percentages values (the sum is inferior than 100%), check edge_devices.xml file !");
		// Add more devices.
		int missingInstances = getSimulationManager().getScenario().getDevicesCount() - edgeDevicesList.size();
		for (int k = 0; k < missingInstances; k++) {
			edgeDevicesList.add(createComputingNode(edgeElement, SimulationParameters.TYPES.EDGE_DEVICE, ""));
		}

		devicesFile.close();

	}

	/**
	 * Generates the required number of instances for each type of edge devices.
	 * 
	 * @param type The type of edge devices.
	 */
	private void generateDevicesInstances(Element type) throws Exception {

		int instancesPercentage = Integer.parseInt(type.getElementsByTagName("percentage").item(0).getTextContent());

		// Find the number of instances of this type of devices
		float devicesInstances = getSimulationManager().getScenario().getDevicesCount() * instancesPercentage / 100;

		for (int j = 0; j < devicesInstances; j++) {
			if (edgeDevicesList.size() > getSimulationManager().getScenario().getDevicesCount()) {
				getSimulationManager().getSimulationLogger().print(getClass().getSimpleName()
						+ " - Wrong percentages values (the sum is superior than 100%), check edge_devices.xml file !");
				break;
			}

			edgeDevicesList.add(createComputingNode(type, SimulationParameters.TYPES.EDGE_DEVICE, ""));

		}
	}

	/**
	 * Generates the Cloud and Edge data centers.
	 * 
	 * @param file The configuration file.
	 * @param type The type, whether a CLOUD data center or an EDGE one.
	 */
	private void generateDataCenters(String file, TYPES type) throws Exception {

		// Fill list with edge data centers
		InputStream serversFile = new FileInputStream(file);
		DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
		DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
		Document doc = dBuilder.parse(serversFile);
		NodeList datacenterList = doc.getElementsByTagName("datacenter");
		for (int i = 0; i < datacenterList.getLength(); i++) {
			Element datacenterElement = (Element) datacenterList.item(i);
			String dcname = datacenterElement.getAttribute("name");
			DataCenter dataCenter = new DataCenter(datacenterElement.getAttribute("name"), this.simulationManager);
			Element nodes_tag = (Element) datacenterElement.getElementsByTagName("nodes").item(0);
			NodeList nodeList = nodes_tag.getElementsByTagName("node");

			if(datacenterElement.getElementsByTagName("ID").item(0) != null) {
				Integer dcId = Integer.parseInt(datacenterElement.getElementsByTagName("ID").item(0).getTextContent());
				dataCenter.setDcId(dcId);
			}

			for (int idx = 0; idx < nodeList.getLength(); idx++) {
				Element node_element = (Element) nodeList.item(idx);
				ComputingNode computingNode = createComputingNode(node_element, type, datacenterElement.getAttribute("name"));
				if(dcname != null) {
					computingNode.setDcName(dcname);
				} else {
					computingNode.setDcName("");
				}
				computingNode.setDataCenter(dataCenter);
				dataCenter.nodeList.add(computingNode);
			}
			dataCenter.createTopology();

			if (type == TYPES.CLOUD) {
				cloudDataCentersList.add(dataCenter);
			}else {
				Element location = (Element) datacenterElement.getElementsByTagName("location").item(0);
				int x_position = Integer.parseInt(location.getElementsByTagName("x_pos").item(0).getTextContent());
				int y_position = Integer.parseInt(location.getElementsByTagName("y_pos").item(0).getTextContent());
				Location datacenterLocation = new Location(x_position, y_position);
				dataCenter.setLocation(datacenterLocation);
				edgeDataCentersList.add(dataCenter);
			}
		}
		serversFile.close();
	}

	/**
	 * Creates the computing nodes.
	 * 
	 * @see #generateDataCenters(String, TYPES)
	 * @see #generateDevicesInstances(Element)
	 * 
	 * @param datacenterElement The configuration file.
	 * @param type              The type, whether an MIST (edge) device, an EDGE
	 *                          data center, or a CLOUD one.
	 */
	private ComputingNode createComputingNode(Element datacenterElement, SimulationParameters.TYPES type, String dcName)
			throws Exception {
		Boolean mobile = false;
		double speed = 0;
		double minPauseDuration = 0;
		double maxPauseDuration = 0;
		double minMobilityDuration = 0;
		double maxMobilityDuration = 0;
		int x_position = -1;
		int y_position = -1;
		long storage = 0;
		long readBandwidth = 0;
		long writeBandwidth = 0;
		boolean gpuEnabled = false;
		boolean pimEnabled = false;
		double pimMips = 0;
		double dataBusBw = 0;
		double gpuCores = 0;
		double gpuMips = 0;
		boolean ssdEnabled = false;
		double ssdStorage = 0;
		double ssdReadBw = 0;
		double ssdWriteBW = 0;
		int numberGpu = 0;
		String serverType = "";
		Double costMi = 0.0;
		Double costMemory = 0.0;
		Double costIo = 0.0;
		CostPlan costPlan = null;

		SimulationManager simulationManager = GlobalConfig.getInstance().getSimulationManager();

		double idleConsumption = Double
				.parseDouble(datacenterElement.getElementsByTagName("idleConsumption").item(0).getTextContent());
		double maxConsumption = Double
				.parseDouble(datacenterElement.getElementsByTagName("maxConsumption").item(0).getTextContent());
		Location datacenterLocation = new Location(x_position, y_position);
		long numOfCores = Integer.parseInt(datacenterElement.getElementsByTagName("cores").item(0).getTextContent());
		double mips = Double.parseDouble(datacenterElement.getElementsByTagName("mips").item(0).getTextContent());

		if (type == TYPES.EDGE_DEVICE) {
			storage = Long.parseLong(datacenterElement.getElementsByTagName("storage").item(0).getTextContent());
		} else {
			Element storage_element = (Element) datacenterElement.getElementsByTagName("storageMeta").item(0);
			Element storage_hdd = (Element)storage_element.getElementsByTagName("HDD").item(0);
			Element storage_ssd = (Element)storage_element.getElementsByTagName("SSD").item(0);
			storage = Long.parseLong(storage_hdd.getElementsByTagName("storage").item(0).getTextContent());
			readBandwidth = Long.parseLong(storage_hdd.getElementsByTagName("readBandwidth").item(0).getTextContent());
			writeBandwidth = Long.parseLong(storage_hdd.getElementsByTagName("writeBandwidth").item(0).getTextContent());

			if(storage_ssd != null){
				ssdEnabled = true;
				ssdStorage = Long.parseLong(storage_ssd.getElementsByTagName("storage").item(0).getTextContent());
				ssdReadBw = Long.parseLong(storage_ssd.getElementsByTagName("readBandwidth").item(0).getTextContent());
				ssdWriteBW = Long.parseLong(storage_ssd.getElementsByTagName("writeBandwidth").item(0).getTextContent());
			}
			dataBusBw = Double.parseDouble(datacenterElement.getElementsByTagName("dataBusBandwidth").item(0).getTextContent());

			if(datacenterElement.getElementsByTagName("pimEnabled").item(0) != null) {
				pimEnabled = Boolean.parseBoolean(datacenterElement.getElementsByTagName("pimEnabled").item(0).getTextContent());
				if (pimEnabled) {
					pimMips = Double.parseDouble(datacenterElement.getElementsByTagName("pimMips").item(0).getTextContent());
				}
			}

			if(datacenterElement.getElementsByTagName("gpuEnabled").item(0) != null) {
				gpuEnabled = Boolean.parseBoolean(datacenterElement.getElementsByTagName("gpuEnabled").item(0).getTextContent());
				if (gpuEnabled) {
					Element gpuMeta = (Element) datacenterElement.getElementsByTagName("gpuMeta").item(0);
					gpuCores = Double.parseDouble(gpuMeta.getElementsByTagName("cores").item(0).getTextContent());
					gpuMips = Double.parseDouble(gpuMeta.getElementsByTagName("mips").item(0).getTextContent());
					numberGpu = Integer.parseInt(gpuMeta.getElementsByTagName("numberGpu").item(0).getTextContent());
				}
			}


				if(type == SimulationParameters.TYPES.EDGE_DATACENTER) {
					if(datacenterElement.getElementsByTagName("serverType").item(0) != null) {
						serverType = datacenterElement.getElementsByTagName("serverType").item(0).getTextContent();
					}
				}
				Element cost_element = (Element) datacenterElement.getElementsByTagName("costPlan").item(0);
				if(cost_element != null) {
					costMi = Double.valueOf(cost_element.getElementsByTagName("costPerMi").item(0).getTextContent());
					costMemory = Double.valueOf(cost_element.getElementsByTagName("costPerMB").item(0).getTextContent());
					costIo = Double.valueOf(cost_element.getElementsByTagName("costPerIo").item(0).getTextContent());
					costPlan = new CostPlan();
					costPlan.setCostPerIo(costIo);
					costPlan.setCostPerMB(costMemory);
					costPlan.setCostPerMi(costMi);
				}

		}

		// We will add ram into account in future updates
		long ram = Integer.parseInt(datacenterElement.getElementsByTagName("ram").item(0).getTextContent());
		Constructor<?> datacenterConstructor = computingNodeClass.getConstructor(SimulationManager.class, double.class,
				long.class, long.class);
		ComputingNode computingNode = (ComputingNode) datacenterConstructor.newInstance(getSimulationManager(), mips,
				numOfCores, storage);
		computingNode.setAsOrchestrator(Boolean
				.parseBoolean(datacenterElement.getElementsByTagName("isOrchestrator").item(0).getTextContent()));
		if (computingNode.isOrchestrator()) {
			orchestratorsList.add(computingNode);
		}
		computingNode.setEnergyModel(new EnergyModelComputingNode(maxConsumption, idleConsumption));
		computingNode.setReadBandwidth(readBandwidth);
		computingNode.setWriteBandwidth(writeBandwidth);
		computingNode.setDataBusBandwidth(dataBusBw);
		computingNode.setServerType(serverType);
		computingNode.setRam(ram);
		computingNode.getEnergyModel().setDeviceType(type);
		if(costPlan != null){
			computingNode.setCostPlan(costPlan);
		}

		if(type == SimulationParameters.TYPES.EDGE_DATACENTER || type == SimulationParameters.TYPES.CLOUD){
			Element location = (Element) datacenterElement.getElementsByTagName("location").item(0);
			x_position = Integer.parseInt(location.getElementsByTagName("x_pos").item(0).getTextContent());
			y_position = Integer.parseInt(location.getElementsByTagName("y_pos").item(0).getTextContent());
			datacenterLocation = new Location(x_position, y_position);
		}

		if (type == SimulationParameters.TYPES.EDGE_DATACENTER) {
			//String name = datacenterElement.getAttribute("name");
			computingNode.setName(dcName);
			computingNode.setPimEnabled(pimEnabled);
			computingNode.setPimMips(pimMips);
			computingNode.setGpuEnabled(gpuEnabled);
			computingNode.setGpuNumbers(numberGpu);
			computingNode.setGpuMipsPerCore(gpuMips);
			computingNode.setNumberOfGPUCores(gpuCores);
			computingNode.setSsdEnabled(ssdEnabled);
			computingNode.setSsdStorage(ssdStorage);
			computingNode.setSsdReadBw(ssdReadBw);
			computingNode.setSsdWriteBw(ssdWriteBW);

			for(DataCenter dc : edgeDataCentersList) {
				for (ComputingNode edgeDC : dc.nodeList)
					if (datacenterLocation.equals(edgeDC.getMobilityModel().getCurrentLocation()))
						throw new IllegalArgumentException(
								" Each Edge Data Center must have a different location, check the \"edge_datacenters.xml\" file!");
			}

			computingNode.setPeriphery(
					Boolean.parseBoolean(datacenterElement.getElementsByTagName("periphery").item(0).getTextContent()));

		} else if (type == SimulationParameters.TYPES.EDGE_DEVICE) {
			mobile = Boolean.parseBoolean(datacenterElement.getElementsByTagName("mobility").item(0).getTextContent());
			speed = Double.parseDouble(datacenterElement.getElementsByTagName("speed").item(0).getTextContent());
			minPauseDuration = Double
					.parseDouble(datacenterElement.getElementsByTagName("minPauseDuration").item(0).getTextContent());
			maxPauseDuration = Double
					.parseDouble(datacenterElement.getElementsByTagName("maxPauseDuration").item(0).getTextContent());
			minMobilityDuration = Double.parseDouble(
					datacenterElement.getElementsByTagName("minMobilityDuration").item(0).getTextContent());
			maxMobilityDuration = Double.parseDouble(
					datacenterElement.getElementsByTagName("maxMobilityDuration").item(0).getTextContent());
			computingNode.getEnergyModel().setBattery(
					Boolean.parseBoolean(datacenterElement.getElementsByTagName("battery").item(0).getTextContent()));
			computingNode.getEnergyModel().setBatteryCapacity(Double
					.parseDouble(datacenterElement.getElementsByTagName("batteryCapacity").item(0).getTextContent()));
			computingNode.getEnergyModel().setConnectivityType(
					datacenterElement.getElementsByTagName("connectivity").item(0).getTextContent());
			computingNode.enableTaskGeneration(Boolean
					.parseBoolean(datacenterElement.getElementsByTagName("generateTasks").item(0).getTextContent()));
			computingNode.setDvfsEnabled(Boolean
					.parseBoolean(datacenterElement.getElementsByTagName("dvfs").item(0).getTextContent()));
			// Generate random location for edge devices
			datacenterLocation = new Location(new Random().nextInt(SimulationParameters.AREA_LENGTH),
					new Random().nextInt(SimulationParameters.AREA_LENGTH));
			getSimulationManager().getSimulationLogger()
					.deepLog("ComputingNodesGenerator- Edge device:" + edgeDevicesList.size() + "    location: ( "
							+ datacenterLocation.getXPos() + "," + datacenterLocation.getYPos() + " )");
		}
		computingNode.setType(type);
		Constructor<?> mobilityConstructor = mobilityModelClass.getConstructor(SimulationManager.class, Location.class);
		MobilityModel mobilityModel = ((MobilityModel) mobilityConstructor.newInstance(simulationManager,
				datacenterLocation)).setMobile(mobile).setSpeed(speed).setMinPauseDuration(minPauseDuration)
				.setMaxPauseDuration(maxPauseDuration).setMinMobilityDuration(minMobilityDuration)
				.setMaxMobilityDuration(maxMobilityDuration);

		computingNode.setMobilityModel(mobilityModel);

		computingNodesList.add(computingNode);
		return computingNode;
	}

	/**
	 * Returns the computing nodes that have been generated.
	 * 
	 * @see #generateDatacentersAndDevices()
	 * 
	 * @return The list of computing nodes
	 */
	public List<ComputingNode> getComputingNodes() {
		return computingNodesList;
	}

	/**
	 * Returns the cloud data centers list.
	 * 
	 * @see #generateDataCenters(String, TYPES)
	 * 
	 * @return The list of cloud data centers
	 */
	public List<DataCenter> getCloudDatacenterList() {
		return cloudDataCentersList;
	}

	/**
	 * Returns the list of edge data centers that have been generated.
	 * 
	 * @see #generateDataCenters(String, TYPES)
	 * 
	 * @return The list of edge data centers
	 */
	public List<DataCenter> getEdgeDatacenterList() {
		return edgeDataCentersList;
	}

	/**
	 * Returns the list of edge devices that have been generated.
	 * 
	 * @see #generateDevicesInstances(Element)
	 * 
	 * @return The list of all edge devices
	 */
	public List<ComputingNode> getEdgeDevicesList() {
		return edgeDevicesList;
	}

	/**
	 * Returns the list of computing nodes that have been selected as orchestrators
	 * (i.e. to make offloading decisions).
	 * 
	 * @return The list of orchestrators
	 */
	public List<ComputingNode> getOrchestratorsList() {
		return orchestratorsList;
	}

	/**
	 * Returns the simulation Manager.
	 * 
	 * @return The simulation manager
	 */
	public SimulationManager getSimulationManager() {
		return simulationManager;
	}

}
