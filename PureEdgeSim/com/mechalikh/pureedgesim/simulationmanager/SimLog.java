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
 *     @author Mechalikh
 **/
package com.mechalikh.pureedgesim.simulationmanager;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;

import com.mechalikh.pureedgesim.datacentersmanager.ComputingNode;
import com.mechalikh.pureedgesim.datacentersmanager.DataCenter;
import com.mechalikh.pureedgesim.energy.EnergyModelNetworkLink;
import com.mechalikh.pureedgesim.network.NetworkLink;
import com.mechalikh.pureedgesim.network.TransferProgress;
import com.mechalikh.pureedgesim.scenariomanager.SimulationParameters;
import com.mechalikh.pureedgesim.taskgenerator.Task;
import common.DataProcessor;
import common.GlobalConfig;
import dag.TaskNode;

public class SimLog {
	public static final int NO_TIME = 0;
	public static final int SAME_LINE = 1;
	public static final int DEFAULT = 2;
	private List<String> resultsList = new ArrayList<String>();
	private DecimalFormat decimalFormat;
	private List<String> log = new ArrayList<String>();
	private String currentOrchArchitecture;
	private String currentOrchAlgorithm;
	private int currentEdgeDevicesCount;
	private String simStartTime;
	private SimulationManager simulationManager;
	private boolean isFirstIteration;

	// Tasks execution results
	private int generatedTasksCount = 0;
	private int tasksSent = 0;
	private int tasksFailed = 0;
	private int tasksFailedLatency = 0;
	private int tasksFailedMobility = 0;
	private int tasksFailedRessourcesUnavailable = 0;
	private int tasksFailedBeacauseDeviceDead = 0;
	private int notGeneratedBecDeviceDead = 0;
	private double totalExecutionTime = 0;
	private double totalWaitingTime = 0;
	private int executedTasksCount = 0;
	private int tasksExecutedOnCloud = 0;
	private int tasksExecutedOnEdge = 0;
	private int tasksExecutedOnMist = 0;
	private int tasksFailedCloud = 0;
	private int tasksFailedEdge = 0;
	private int tasksFailedMist = 0;
	private double totalNetworkTime = 0;
	private double totalNetworkTimeEx = 0;
	private double totalAdditionalTime;
	private static double cachingCost;

	// Network utilization
	private double totalLanUsage = 0;
	private double totalManUsage = 0;
	private double totalWanUsage = 0;
	private double totalBandwidth = 0;
	private int transfersCount = 0;
	private double containersLanUsage = 0;
	private double containersWanUsage = 0;
	private double containersManUsage = 0;
	private double totalTraffic = 0;
	Integer numCriticalTaskFailed = 0;
	int numTaskFailedOnUEFailure = 0;
	public static double CACHING_TIME = 5;

	//Test stats
	public static double min_nearLatencyReatio = Double.MAX_VALUE;
	public static double min_neighbourLatencyRatio = Double.MAX_VALUE;
	public static double min_nearestCPUUsage = Double.MAX_VALUE;

	public static double max_nearLatencyReatio = Double.MIN_VALUE;
	public static double max_neighbourLatencyRatio = Double.MIN_VALUE;
	public static double max_nearestCPUUsage = Double.MIN_VALUE;
	public static double min_defuzzifyVal = Double.MAX_VALUE;
	public static double max_defuzzifyVal = Double.MIN_VALUE;

	public static int nearestDc= 0;
	public static int neighbourDc = 0;
	public static int cloudDc = 0;
	public static int totalCriticalTasks = 0;
	public static int totalLowRealiabilityTasks = 0;

	private HashMap<String, Double> dcWiseTaskExecuted;
	public static void updateCachingCost(TaskNode task){
		cachingCost += CACHING_TIME*task.getFileSize()*0.0001;
	}

	public SimLog(String time, boolean isFirstIteration) {
		dcWiseTaskExecuted = new HashMap<>();
		this.setSimStartTime(time);
		this.isFirstIteration = isFirstIteration;

		// Use this format for all numbers
		DecimalFormatSymbols otherSymbols = new DecimalFormatSymbols(Locale.GERMAN);
		otherSymbols.setDecimalSeparator('.'); // use the dot "." as separation symbole, since the comma ","
												// is used in csv files as a separator
		decimalFormat = new DecimalFormat("######.####", otherSymbols);

		if (isFirstIteration) {
			// Add the CSV file header
			resultsList.add("Orchestration architecture,Orchestration algorithm,Edge devices count,"
					+ "Total tasks execution delay (s),Average execution delay (s),Total tasks waiting time (s),"
					+ "Average waiting time (s),Number of generated tasks,Tasks successfully executed,"
					+ "Task not executed (No resources available or long waiting time),Tasks failed (delay),Tasks failed (device dead),"
					+ "Tasks failed (mobility),Tasks not generated due to the death of devices,Total tasks executed (Cloud),"
					+ "Tasks successfully executed (Cloud),Total tasks executed (Edge),Tasks successfully executed (Edge),"
					+ "Total tasks executed (Mist),Tasks successfully executed (Mist),"
					+ "Network usage (s),Wan usage (s),Lan usage (s), Total network traffic (MBytes), Containers wan usage (s), Containers lan usage (s),Average bandwidth per task (Mbps),Average CPU usage (%),"
					+ "Average CPU usage (Cloud) (%),Average CPU usage (Edge) (%),Average CPU usage (Mist) (%),"
					+ "Energy consumption of computing nodes (Wh),Average energy consumption (Wh/Computing node),Cloud energy consumption (Wh),"
					+ "Average Cloud energy consumption (Wh/Data center),Edge energy consumption (Wh),Average Edge energy consumption (Wh/Data center),"
					+ "Mist energy consumption (Wh),Average Mist energy consumption (Wh/Device),"
					+ "WAN energy consumption (Wh), MAN energy consumption (Wh), LAN energy consumption (Wh),"
					+ "WiFi energy consumption (Wh), LTE energy consumption (Wh), Ethernet energy consumption (Wh),"
					+ "Dead devices count,Average remaining power (Wh),Average remaining power (%), First edge device death time (s),"
					+ "List of remaining power (%) (only battery powered devices / 0 = dead),List of the time when each device died (s)");
		}
	}

	public static void updateValues(double latencyAndDeadlineRatioNr, double latencyAndDeadlineRatioNb,
									double nearestDcCpuUsage, double defuzzifyValue){
		if(min_nearLatencyReatio > latencyAndDeadlineRatioNr){
			min_nearLatencyReatio = latencyAndDeadlineRatioNr;
		}
		if(min_neighbourLatencyRatio > latencyAndDeadlineRatioNb){
			min_neighbourLatencyRatio = latencyAndDeadlineRatioNb;
		}
		if(min_nearestCPUUsage > nearestDcCpuUsage){
			min_nearestCPUUsage = nearestDcCpuUsage;
		}
		if(defuzzifyValue > 0 && min_defuzzifyVal > defuzzifyValue){
			min_defuzzifyVal = defuzzifyValue;
		}

		if(max_nearLatencyReatio < latencyAndDeadlineRatioNr){
			max_nearLatencyReatio = latencyAndDeadlineRatioNr;
		}
		if(max_neighbourLatencyRatio < latencyAndDeadlineRatioNb){
			max_neighbourLatencyRatio = latencyAndDeadlineRatioNb;
		}
		if(max_nearestCPUUsage < nearestDcCpuUsage){
			max_nearestCPUUsage = nearestDcCpuUsage;
		}
		if(max_defuzzifyVal < defuzzifyValue){
			max_defuzzifyVal = defuzzifyValue;
		}
	}

	public static void updateDcStats(int nearest, int neighbour, int cloud){
		nearestDc+=nearest;
		neighbourDc+=neighbour;
		cloudDc+=cloud;
	}

	public void showIterationResults(List<Task> finishedTasks) {
		printTasksRelatedResults();
		printNetworkRelatedResults();
		printCPUutilizationResults();
		printPowerConsumptionResults(finishedTasks);
		StringBuilder s = new StringBuilder("\n");
		for (String value : log) {
			s.append(value).append("\n");
		}
		System.out.print(s);
		printDCwiseCounter();
		printImpStats();
		// update the log
		saveLog();
	}

	private void printDCwiseCounter(){
		for(Map.Entry<String, Double> set : dcWiseTaskExecuted.entrySet()){
			System.out.println("=======DC wise statistics=======");
			System.out.println("DC= " + set.getKey() + " Value: " + set.getValue());
		}
	}

	private void printCPUutilizationResults() {
		int edgeDevicesCount = 0;
		double averageCpuUtilization = 0;
		double averageCloudCpuUtilization = 0;
		double averageMistCpuUtilization = 0;
		double averageEdgeCpuUtilization = 0;
		for (DataCenter cloudDC : simulationManager.getDataCentersManager().getCloudDatacentersList()) {
			for(ComputingNode node : cloudDC.nodeList) {
				averageCloudCpuUtilization += node.getAvgCpuUtilization();
			}
		}
		for(DataCenter dc : simulationManager.getDataCentersManager().getEdgeDatacenterList()) {
			for (ComputingNode edgeDC : dc.nodeList) {
				averageEdgeCpuUtilization += edgeDC.getAvgCpuUtilization();
			}
		}
		for (ComputingNode edgeDevice : simulationManager.getDataCentersManager().getEdgeDevicesList()) {
			if (edgeDevice.getTotalMipsCapacity() > 0) {
				// only devices with computing capability
				// the devices that have no VM are considered simple sensors, and will not be
				// counted here
				averageMistCpuUtilization += edgeDevice.getAvgCpuUtilization();
				edgeDevicesCount++;
			}

		}

		averageCpuUtilization = (averageCloudCpuUtilization + averageMistCpuUtilization + averageEdgeCpuUtilization)
				/ (edgeDevicesCount + SimulationParameters.NUM_OF_EDGE_DATACENTERS
						+ SimulationParameters.NUM_OF_CLOUD_DATACENTERS);

		averageCloudCpuUtilization = averageCloudCpuUtilization / SimulationParameters.NUM_OF_CLOUD_DATACENTERS;
		averageEdgeCpuUtilization = averageEdgeCpuUtilization / SimulationParameters.NUM_OF_EDGE_DATACENTERS;
		averageMistCpuUtilization = averageMistCpuUtilization / edgeDevicesCount;

		print("Average CPU utilization                                                 :"
				+ padLeftSpaces(decimalFormat.format(averageCpuUtilization), 20) + " %");
		print("Average CPU utilization per level                                       :Cloud= "
				+ padLeftSpaces(decimalFormat.format(averageCloudCpuUtilization), 13) + " %");
		print("                                                                          Edge= "
				+ padLeftSpaces(decimalFormat.format(averageEdgeCpuUtilization), 13) + " %");
		print("                                                                          Mist= "
				+ padLeftSpaces(decimalFormat.format(averageMistCpuUtilization), 13) + " %");

		resultsList.set(resultsList.size() - 1,
				resultsList.get(resultsList.size() - 1) + decimalFormat.format(averageCpuUtilization) + ","
						+ decimalFormat.format(averageCloudCpuUtilization) + ","
						+ decimalFormat.format(averageEdgeCpuUtilization) + ","
						+ decimalFormat.format(averageMistCpuUtilization) + ",");
	}

	public void printTasksRelatedResults() {
		print(getClass().getSimpleName()+ " - Printing iteration output...");
		print("------------------------------------------------------- OUTPUT -------------------------------------------------------");
		print("");
		print("Tasks not sent because device died (low energy)                         :"
				+ padLeftSpaces(decimalFormat.format(notGeneratedBecDeviceDead / generatedTasksCount), 20) + " % ("
				+ notGeneratedBecDeviceDead + " tasks)");
		print("Tasks sent from edge devices                                            :"
				+ padLeftSpaces("" + decimalFormat.format(((double) tasksSent * 100) / ((double) generatedTasksCount)),
						20)
				+ " % (" + tasksSent + " among " + generatedTasksCount + " generated tasks)");

		print("-------------------------------------All values below are based on the sent tasks-------------------------------------");
		print("Total tasks execution time                                              :"
				+ padLeftSpaces(decimalFormat.format(totalExecutionTime), 20) + " seconds");
		print("Average task execution time                                             :"
				+ padLeftSpaces(decimalFormat.format(totalExecutionTime / executedTasksCount), 20) + " seconds");
		print("Total waiting time (from submitting the tasks to when execution started):"
				+ padLeftSpaces(decimalFormat.format(totalWaitingTime), 20) + " seconds");
		print("Average task waiting time                                               :"
				+ padLeftSpaces(decimalFormat.format(totalWaitingTime / executedTasksCount), 20) + " seconds");
		print("Tasks successfully executed                                             :"
				+ padLeftSpaces("" + decimalFormat.format((double) (tasksSent - tasksFailed) * 100 / tasksSent), 20)
				+ " % (" + (tasksSent - tasksFailed) + " among " + tasksSent + " sent tasks)");

		print("Tasks failures");
		print("                              Not executed due to resource unavailablity:"
				+ padLeftSpaces(decimalFormat.format((double) tasksFailedRessourcesUnavailable * 100 / tasksSent), 20)
				+ " % (" + tasksFailedRessourcesUnavailable + " tasks)");
		print("                                   Executed but failed due to high delay:"
				+ padLeftSpaces(decimalFormat.format((double) tasksFailedLatency * 100 / tasksSent), 20) + " % ("
				+ tasksFailedLatency + " tasks from " + tasksSent + " successfully sent tasks)");
		print("               Tasks execution results not returned due to devices death:"
				+ padLeftSpaces(decimalFormat.format((double) tasksFailedBeacauseDeviceDead * 100 / tasksSent), 20)
				+ " % (" + tasksFailedBeacauseDeviceDead + " tasks)");
		print("            Tasks execution results not returned due to devices mobility:"
				+ padLeftSpaces(decimalFormat.format((double) tasksFailedMobility * 100 / tasksSent), 20) + " % ("
				+ tasksFailedMobility + " tasks)");

		print("Tasks executed on each level                                            :" + "Cloud= "
				+ padLeftSpaces("" + tasksExecutedOnCloud, 13) + " tasks (where "
				+ (tasksExecutedOnCloud - tasksFailedCloud) + " were successfully executed )");
		print("                                                                         " + " Edge="
				+ padLeftSpaces("" + tasksExecutedOnEdge, 14) + " tasks (where "
				+ (tasksExecutedOnEdge - tasksFailedEdge) + " were successfully executed )");
		print("                                                                         " + " Mist="
				+ padLeftSpaces("" + tasksExecutedOnMist, 14) + " tasks (where "
				+ (tasksExecutedOnMist - tasksFailedMist) + " were successfully executed )");

		resultsList.add(currentOrchArchitecture + "," + currentOrchAlgorithm + "," + currentEdgeDevicesCount + ","
				+ decimalFormat.format(totalExecutionTime) + ","
				+ decimalFormat.format(totalExecutionTime / executedTasksCount) + ","
				+ decimalFormat.format(totalWaitingTime) + ","
				+ decimalFormat.format(totalWaitingTime / executedTasksCount) + "," + generatedTasksCount + ","
				+ (tasksSent - tasksFailed) + "," + tasksFailedRessourcesUnavailable + "," + tasksFailedLatency + ","
				+ tasksFailedBeacauseDeviceDead + "," + tasksFailedMobility + "," + notGeneratedBecDeviceDead + ","
				+ tasksExecutedOnCloud + "," + (tasksExecutedOnCloud - tasksFailedCloud) + "," + tasksExecutedOnEdge
				+ "," + (tasksExecutedOnEdge - tasksFailedEdge) + "," + tasksExecutedOnMist + ","
				+ (tasksExecutedOnMist - tasksFailedMist) + ",");
	}

	public void printImpStats() {
		double succ = (double) (tasksSent - tasksFailed) * 100 / tasksSent;
		double mistEnConsumption = 0.0;
		for (ComputingNode edgeDevice : simulationManager.getDataCentersManager().getEdgeDevicesList()) {
			mistEnConsumption += edgeDevice.getEnergyModel().getTotalEnergyConsumption();
		}
		double avgExTime = totalExecutionTime / executedTasksCount;
		double avgWaitTime = totalWaitingTime / executedTasksCount;
		Integer totalSuccTasks = tasksSent - tasksFailed;
		double totalSuccTime = totalExecutionTime + totalWaitingTime + this.totalNetworkTimeEx + this.totalAdditionalTime;

		System.out.println("Total tasks execution time              : "+decimalFormat.format(totalExecutionTime));
		System.out.println("Average task execution time             : "+decimalFormat.format(avgExTime));
		System.out.println("Total waiting time                      : "+decimalFormat.format(totalWaitingTime));
		System.out.println("Average task waiting time               : "+decimalFormat.format(avgWaitTime));
		System.out.println("Total Average Execution Time            : "+decimalFormat.format(avgExTime + avgWaitTime));
		System.out.println("Total successful task completion time   : " + decimalFormat.format(totalSuccTime) + " seconds");
		System.out.println("Average successful task completion time : " + decimalFormat.format(totalSuccTime/totalSuccTasks) + " seconds");
		System.out.println("Success (%)                             : "+decimalFormat.format(succ));
		System.out.println("Tasks failures (%)                      : "+decimalFormat.format(100-succ));
		System.out.println("Cloud (Total/success)                   : "+tasksExecutedOnCloud +"/"+ (tasksExecutedOnCloud-tasksFailedCloud));
		System.out.println("Edge (Total/success)                    : "+tasksExecutedOnEdge + "/"+(tasksExecutedOnEdge - tasksFailedEdge));
		System.out.println("UE (Total/success)                      : "+tasksExecutedOnMist+ "/"+ (tasksExecutedOnMist - tasksFailedMist));
		System.out.println("UE Energy consumption (total/Avg) (mWh) : "+1000*mistEnConsumption+"/"+(1000*mistEnConsumption / currentEdgeDevicesCount));


		if(GlobalConfig.getInstance().getAlgorithm().equals("BQProfit")){
			System.out.println();
			printRevenue();
			System.out.println();
			printCost();
			System.out.println();
			printProfit();
		}

		if(GlobalConfig.getInstance().getAlgorithm().equals("ReMEC") ||
				GlobalConfig.getInstance().getAlgorithm().equals("RMEAC") ||
				GlobalConfig.getInstance().getAlgorithm().equals("FP-TOSM")){
			printReMEC();
		}
	}

	private void printProfit(){
		Double total_profit = simulationManager.getProfitCalculator().getCpu_profit() +
				simulationManager.getProfitCalculator().getMemory_profit() +
				simulationManager.getProfitCalculator().getIo_profit();
		double mistEnConsumption = 0;
		for (ComputingNode edgeDevice : simulationManager.getDataCentersManager().getEdgeDevicesList()) {
			mistEnConsumption += edgeDevice.getEnergyModel().getTotalEnergyConsumption();
		}
		System.out.println("=======Profit and Penalty Statistics=======");
		System.out.println("CPU-profit                              : " + simulationManager.getProfitCalculator().getCpu_profit());
		System.out.println("Memory-profit                           : " + simulationManager.getProfitCalculator().getMemory_profit());
		System.out.println("IO-profit                               : " + simulationManager.getProfitCalculator().getIo_profit());
		System.out.println("Total-Penalty                           : " + simulationManager.getProfitCalculator().getPenalty());
		Integer total_succ_task_edge_cloud = (tasksExecutedOnCloud - tasksFailedCloud) + (tasksExecutedOnEdge - tasksFailedEdge) + (tasksExecutedOnMist - tasksFailedMist);
		System.out.println("Profit per Task                         : "+total_profit/total_succ_task_edge_cloud);
		System.out.println("Average task execution time (ms)        : " + decimalFormat.format(1000*(totalExecutionTime / executedTasksCount + totalWaitingTime / executedTasksCount)) + " ms");
		System.out.println("Total-Profit                            : " + total_profit);
	}

	private void printCost(){
		System.out.println("=======Cost Statistics=======");
		System.out.println("CPU-cost                                : " + simulationManager.getProfitCalculator().getCpu_cost());
		System.out.println("Memory-cost                             : " + simulationManager.getProfitCalculator().getMemory_cost());
		System.out.println("IO-cost                                 : " + simulationManager.getProfitCalculator().getIo_cost());
		Double total_cost = simulationManager.getProfitCalculator().getCpu_cost() +
				simulationManager.getProfitCalculator().getMemory_cost() +
				simulationManager.getProfitCalculator().getIo_cost();
		System.out.println("Total-Cost                              : " + total_cost);
		Integer total_succ_task_edge_cloud = (tasksExecutedOnCloud - tasksFailedCloud) + (tasksExecutedOnEdge - tasksFailedEdge) + (tasksExecutedOnMist - tasksFailedMist);
		System.out.println("Cost per Task                           : "+total_cost/total_succ_task_edge_cloud);
	}

	private void printReMEC(){
		int numSuccApp = 0;
		double totalTimeSuccApp = 0;
		int numFailedApp = 0;
		double totalTimeFailedApp = 0;
		int taskExRemote = 0;
		int taskSuccExRemote = 0;
		for (Map.Entry<Integer, Map<Integer, TaskNode>> entry : DataProcessor.scheduledTasksMap.entrySet()) {
			Map<Integer, TaskNode> job = entry.getValue();
			boolean succ = true;
			double startTime = 0;
			double endTime = 0;
			for (Map.Entry<Integer, TaskNode> task : job.entrySet()) {
				TaskNode taskNode = task.getValue();
				if(taskNode.isPrimary()) {
					if(taskNode.getStatus() == Task.Status.FAILED){
						TaskNode replica = taskNode.getReplica();
						if(replica == null) {
							succ = false;
							//System.out.println("Task failied: "+ taskNode.getId());
						} else if(replica != null && replica.getStatus() == Task.Status.FAILED){
							succ = false;
						}
					}

					if(taskNode.isStartTask()){
						startTime = taskNode.getStartTime();
					}

					if(taskNode.isEndTask()){
						endTime = taskNode.getEndTime();
					}
				}

				if(!taskNode.getEdgeDevice().equals(taskNode.getComputingNode())){
					taskExRemote++;
					if(taskNode.getStatus() == Task.Status.SUCCESS){
						taskSuccExRemote++;
					}
				}
			}

			if(succ){
				totalTimeSuccApp += (endTime - startTime);
				numSuccApp++;
			} else{
				numFailedApp++;
				totalTimeFailedApp += (endTime - startTime);
			}
		}


		System.out.println("Total success app completion time       : "+totalTimeSuccApp);
		System.out.println("Total failed app completion time        : " + totalTimeFailedApp );
		System.out.println("Average success app completion time     : "+decimalFormat.format(totalTimeSuccApp/numSuccApp));
		System.out.println("Average failed app completion time      : "+decimalFormat.format(totalTimeFailedApp/numFailedApp));
		System.out.println("Total average app completion time       :" + decimalFormat.format((totalTimeSuccApp+totalTimeFailedApp)/(numSuccApp + numFailedApp)));
		System.out.println("Total successful applications           : " + numSuccApp);
		System.out.println("Total failed applications               : "+numFailedApp);
		System.out.println("Percentage of app failed                : " + (numFailedApp)*100.0/(numSuccApp+numFailedApp));
		System.out.println("Total critical tasks                    : "+ totalCriticalTasks);
		System.out.println("Total critical tasks failed             : " +numCriticalTaskFailed);
		System.out.println("Total low reliability tasks             : "+totalLowRealiabilityTasks);
		System.out.println("Total task failed due to UE failure     : " + numTaskFailedOnUEFailure);
		System.out.println("Caching cost                            : "+cachingCost);
	}

	private void printRevenue(){
		System.out.println("=======Revenue Statistics=======");
		System.out.println("CPU-revenue                             : " + simulationManager.getProfitCalculator().getCpu_revenue());
		System.out.println("Memory-revenue                          : " + simulationManager.getProfitCalculator().getMemory_revenue());
		System.out.println("IO-revenue                              : " + simulationManager.getProfitCalculator().getIo_revenue());
		Double total_revenue = simulationManager.getProfitCalculator().getCpu_revenue() +
				simulationManager.getProfitCalculator().getMemory_revenue() +
				simulationManager.getProfitCalculator().getIo_revenue();
		System.out.println("Total-revenue                           : " + total_revenue);
		Integer total_succ_task_edge_cloud = (tasksExecutedOnCloud - tasksFailedCloud) + (tasksExecutedOnEdge - tasksFailedEdge) + (tasksExecutedOnMist - tasksFailedMist);
		System.out.println("Revenue per Task                        : "+total_revenue/total_succ_task_edge_cloud);
	}

	public void printNetworkRelatedResults() {
		print("Network usage                                                           :"
				+ padLeftSpaces(decimalFormat.format(totalLanUsage + totalManUsage + totalWanUsage), 20)
				+ " seconds (The total traffic: " + decimalFormat.format(totalTraffic) + " (MBytes) )");
		print("                                                                         " + "  Wan="
				+ padLeftSpaces(decimalFormat.format(totalWanUsage), 14) + " seconds ("
				+ decimalFormat.format(totalWanUsage * 100 / (totalLanUsage + totalManUsage + totalWanUsage))
				+ " % of total usage, WAN used when downloading containers="
				+ decimalFormat.format(totalWanUsage == 0 ? 0 : containersWanUsage * 100 / totalWanUsage)
				+ " % of WAN usage )");
		print("                                                                         " + "  Man="
				+ padLeftSpaces(decimalFormat.format(totalManUsage), 14) + " seconds ("
				+ decimalFormat.format(totalManUsage * 100 / (totalLanUsage + totalManUsage + totalWanUsage))
				+ " % of total usage, MAN used when downloading containers="
				+ decimalFormat.format(totalManUsage == 0 ? 0 : containersManUsage * 100 / totalManUsage)
				+ " % of MAN usage )");
		print("                                                                         " + "  Lan="
				+ padLeftSpaces(decimalFormat.format(totalLanUsage), 14) + " seconds ("
				+ decimalFormat.format(totalLanUsage * 100 / (totalLanUsage + totalManUsage + totalWanUsage))
				+ " % of total usage, LAN used when downloading containers="
				+ decimalFormat.format(containersLanUsage * 100 / totalLanUsage) + " % of LAN usage )");
		print("Average transfer speed                                                  :"
				+ padLeftSpaces(decimalFormat.format(totalBandwidth / transfersCount), 20) + " Mbps  ");
		// Add these values to the las item of the results list
		resultsList.set(resultsList.size() - 1,
				resultsList.get(resultsList.size() - 1) + totalLanUsage + "," + totalWanUsage + "," + totalLanUsage
						+ "," + totalTraffic + "," + containersWanUsage + "," + containersLanUsage + ","
						+ (totalBandwidth / transfersCount) + ",");
		print("Total Network Time                                                     : "+decimalFormat.format(totalNetworkTimeEx));
	}

	public void printPowerConsumptionResults(List<Task> finishedTasks) {
		int deadEdgeDevicesCount = 0;
		double energyConsumption = 0;
		double cloudEnConsumption = 0;
		double mistEnConsumption = 0;
		double edgeEnConsumption = 0;
		double averageRemainingPowerWh = 0;
		double averageRemainingPower = 0;
		List<Double> remainingPower = new ArrayList<>();
		double firstDeviceDeathTime = -1; // -1 means invalid
		List<Double> devicesDeathTime = new ArrayList<>();
		int batteryPoweredDevicesCount = 0;
		int aliveBatteryPoweredDevicesCount = 0;

		double wan = simulationManager.getDataCentersManager().getTopology().getWanLinks().stream().map(NetworkLink::getEnergyModel)
				.collect(Collectors.summingDouble(EnergyModelNetworkLink::getTotalEnergyConsumption));

		double man = simulationManager.getDataCentersManager().getTopology().getManLinks().stream().map(NetworkLink::getEnergyModel)
				.collect(Collectors.summingDouble(EnergyModelNetworkLink::getTotalEnergyConsumption));

		double lan = simulationManager.getDataCentersManager().getTopology().getLanLinks().stream().map(NetworkLink::getEnergyModel)
				.collect(Collectors.summingDouble(EnergyModelNetworkLink::getTotalEnergyConsumption));

		double fourG = simulationManager.getDataCentersManager().getTopology().get4gLinks().stream().map(NetworkLink::getEnergyModel)
				.collect(Collectors.summingDouble(EnergyModelNetworkLink::getTotalEnergyConsumption));
		
		double eth = simulationManager.getDataCentersManager().getTopology().getEthernetLinks().stream().map(NetworkLink::getEnergyModel)
				.collect(Collectors.summingDouble(EnergyModelNetworkLink::getTotalEnergyConsumption));
		
		double wifi = simulationManager.getDataCentersManager().getTopology().getWifiLinks().stream().map(NetworkLink::getEnergyModel)
				.collect(Collectors.summingDouble(EnergyModelNetworkLink::getTotalEnergyConsumption));

		for(DataCenter cloudDc : simulationManager.getDataCentersManager().getCloudDatacentersList()) {
			for (ComputingNode node : cloudDc.nodeList)
				cloudEnConsumption += node.getEnergyModel().getTotalEnergyConsumption();
		}

		for(DataCenter dc : simulationManager.getDataCentersManager().getEdgeDatacenterList()) {
			for (ComputingNode edgeDc : dc.nodeList)
				edgeEnConsumption += edgeDc.getEnergyModel().getTotalEnergyConsumption();
		}

		for (ComputingNode edgeDevice : simulationManager.getDataCentersManager().getEdgeDevicesList()) {
			mistEnConsumption += edgeDevice.getEnergyModel().getTotalEnergyConsumption();

			if (edgeDevice.isDead()) {
				devicesDeathTime.add(edgeDevice.getDeathTime());
				remainingPower.add(0.0);
				deadEdgeDevicesCount++;
				if (firstDeviceDeathTime == -1)
					firstDeviceDeathTime = edgeDevice.getDeathTime();
				else if (firstDeviceDeathTime > edgeDevice.getDeathTime())
					firstDeviceDeathTime = edgeDevice.getDeathTime();
			} else {
				if (edgeDevice.getEnergyModel().isBatteryPowered()) {
					averageRemainingPowerWh += edgeDevice.getEnergyModel().getBatteryLevel();
					averageRemainingPower += edgeDevice.getEnergyModel().getBatteryLevelPercentage();
					remainingPower.add(edgeDevice.getEnergyModel().getBatteryLevelPercentage());
					aliveBatteryPoweredDevicesCount++;
				}
			}

		}
		batteryPoweredDevicesCount = aliveBatteryPoweredDevicesCount + deadEdgeDevicesCount;
		// escape from devision by 0
		if (aliveBatteryPoweredDevicesCount == 0)
			aliveBatteryPoweredDevicesCount = 1;
		energyConsumption = cloudEnConsumption + edgeEnConsumption + mistEnConsumption;
		averageRemainingPower = averageRemainingPower / (double) aliveBatteryPoweredDevicesCount;
		averageRemainingPowerWh = averageRemainingPowerWh / (double) aliveBatteryPoweredDevicesCount;
		double averageCloudEnConsumption = cloudEnConsumption / SimulationParameters.NUM_OF_CLOUD_DATACENTERS;
		double averageEdgeEnConsumption = edgeEnConsumption / SimulationParameters.NUM_OF_EDGE_DATACENTERS;
		double averageMistEnConsumption = mistEnConsumption / simulationManager.getScenario().getDevicesCount();

		print("Energy consumption                                                      :"
				+ padLeftSpaces(decimalFormat.format(energyConsumption), 20) + " Wh (Average: "
				+ decimalFormat.format(energyConsumption
						/ (SimulationParameters.NUM_OF_EDGE_DATACENTERS + SimulationParameters.NUM_OF_CLOUD_DATACENTERS
								+ simulationManager.getScenario().getDevicesCount()))
				+ " Wh/data center(or device))");
		print("                                                                        :"
				+ padLeftSpaces("", 19) + "     (Average: "
				+ decimalFormat.format(energyConsumption / (double) finishedTasks.size()) + " Wh/task)");
		print("Energy Consumption per level                                            :Cloud= "
				+ padLeftSpaces(decimalFormat.format(cloudEnConsumption), 13) + " Wh (Average: "
				+ decimalFormat.format(cloudEnConsumption / SimulationParameters.NUM_OF_CLOUD_DATACENTERS)
				+ " Wh/data center)");
		print("                                                                          Edge="
				+ padLeftSpaces(decimalFormat.format(edgeEnConsumption), 14) + " Wh (Average: "
				+ decimalFormat.format(edgeEnConsumption / SimulationParameters.NUM_OF_EDGE_DATACENTERS)
				+ " Wh/data center)");
		print("                                                                          Mist="
				+ padLeftSpaces(decimalFormat.format(mistEnConsumption), 14) + " Wh (Average: "
				+ decimalFormat.format(mistEnConsumption / currentEdgeDevicesCount) + " Wh/edge device)");

		print("Energy Consumption per network                                          :  WAN="
				+ padLeftSpaces(decimalFormat.format(wan), 14) + " Wh");
		print("                                                                           MAN="
				+ padLeftSpaces(decimalFormat.format(man), 14) + " Wh ");
		print("                                                                           LAN="
				+ padLeftSpaces(decimalFormat.format(lan), 14) + " Wh ");

		print("Energy Consumption per technology                                       : WiFi="
				+ padLeftSpaces(decimalFormat.format(wifi), 14) + " Wh");
		print("                                                                      Cellular="
				+ padLeftSpaces(decimalFormat.format(fourG), 14) + " Wh ");
		print("                                                                      Ethernet="
				+ padLeftSpaces(decimalFormat.format(eth), 14) + " Wh ");

		print("Dead edge devices due to battery drain                                  :"
				+ padLeftSpaces(decimalFormat.format(deadEdgeDevicesCount), 20) + " devices (Among "
				+ batteryPoweredDevicesCount + " devices with batteries ("
				+ decimalFormat.format(((double) deadEdgeDevicesCount) * 100 / (double) batteryPoweredDevicesCount)
				+ " %))");
		print("Average remaining power (devices with batteries that are still alive)   :"
				+ padLeftSpaces(decimalFormat.format(averageRemainingPowerWh), 20) + " Wh (Average: "
				+ decimalFormat.format(averageRemainingPower) + " %)");
		if (firstDeviceDeathTime != -1)
			print("First device died at                                                    :"
					+ padLeftSpaces("" + firstDeviceDeathTime, 20) + " seconds");

		// Add these values to the las item of the results list
		resultsList.set(resultsList.size() - 1, resultsList.get(resultsList.size() - 1)
				+ decimalFormat.format(energyConsumption) + ","
				+ decimalFormat.format(energyConsumption
						/ (SimulationParameters.NUM_OF_EDGE_DATACENTERS + SimulationParameters.NUM_OF_CLOUD_DATACENTERS
								+ simulationManager.getScenario().getDevicesCount()))
				+ "," + decimalFormat.format(cloudEnConsumption) + "," + decimalFormat.format(averageCloudEnConsumption)
				+ "," + decimalFormat.format(edgeEnConsumption) + "," + decimalFormat.format(averageEdgeEnConsumption)
				+ "," + decimalFormat.format(mistEnConsumption) + "," + decimalFormat.format(averageMistEnConsumption)
				+ "," + decimalFormat.format(wan) + "," + decimalFormat.format(man) + "," + decimalFormat.format(lan)
				+ "," + decimalFormat.format(wifi) + "," + decimalFormat.format(fourG) + "," + decimalFormat.format(eth)
				+ "," + decimalFormat.format(deadEdgeDevicesCount) + "," + decimalFormat.format(averageRemainingPowerWh)
				+ "," + decimalFormat.format(averageRemainingPower) + "," + firstDeviceDeathTime + ","
				+ remainingPower.toString().replace(",", "-") + "," + devicesDeathTime.toString().replace(",", "-"));
	}

	public String padLeftSpaces(String str, int n) {
		return String.format("%1$" + n + "s", str);
	}

	public void cleanOutputFolder() throws IOException {
		// Clean the folder where the results files will be saved
		if (isFirstIteration) {
			print(getClass().getSimpleName()+" - Cleaning the outputfolder...");
			isFirstIteration = false;
			Path dir = new File(SimulationParameters.OUTPUT_FOLDER).toPath();
			deleteDirectory(dir);
		}
	}

	private void deleteDirectory(Path path) throws IOException {
		if (Files.isDirectory(path, LinkOption.NOFOLLOW_LINKS)) {
			try (DirectoryStream<Path> entries = Files.newDirectoryStream(path)) {
				for (Path entry : entries) {
					deleteDirectory(entry);
				}
			}
		}
		try {
			Files.delete(path);
		} catch (Exception e) {
			print(getClass().getSimpleName()+" - Could not delete file/folder: " + path.toString());
		}
	}

	public void saveLog() {
		// writing results in csv file
		writeFile(getFileName(".csv"), getResultsList());

		if (!SimulationParameters.SAVE_LOG) {
			println(getClass().getSimpleName()+" - No log saving");
			return;
		}

		writeFile(getFileName(".txt"), log);

	}

	private List<String> getResultsList() {
		return this.resultsList;
	}

	public void writeFile(String fileName, List<String> Lines) {
		try {
			BufferedWriter bufferedWriter = new BufferedWriter(new FileWriter(fileName, true));
			for (String str : Lines) {
				bufferedWriter.append(str);
				bufferedWriter.newLine();
			}
			Lines.clear();
			bufferedWriter.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public String getFileName(String extension) {
		String outputFilesName = SimulationParameters.OUTPUT_FOLDER + "/" + simStartTime;
		new File(outputFilesName).mkdirs();
		if (SimulationParameters.PARALLEL)
			outputFilesName += "/Parallel_simulation_" + simulationManager.getSimulationId();
		else
			outputFilesName += "/Sequential_simulation";

		return outputFilesName + extension;
	}

	public void print(String line, int flag) {
		String newLine = line;
		if (simulationManager == null) {
			System.out.println("    0.0" + " : " + newLine);
		} else {
			switch (flag) {
			case DEFAULT:
					newLine = padLeftSpaces(decimalFormat.format(
							simulationManager.getSimulation().clock()), 7)
							+ " (s) : " + newLine;
				log.add(new SimpleDateFormat("yyyy/MM/dd HH:mm:ss").format(new Date()) + " - simulation time "
						+ newLine);
				break;
			case NO_TIME:
				log.add(newLine);
				break;
			case SAME_LINE:
				log.set(log.size() - 1, log.get(log.size() - 1) + newLine);
				break;
			default:
				break;
			}
		}
	}

	public void print(String line) {
		print(line, DEFAULT);
	}

	public static void println(String line) {
		System.out.println(line);
	}

	public void deepLog(String line) {
		if (SimulationParameters.DEEP_LOGGING)
			print(line, DEFAULT);
	}

	public void deepLog(String line, int flag) {
		if (SimulationParameters.DEEP_LOGGING) {
			print(line, flag);
		}
	}

	public void printWithoutTime(String line) {
		print(line, NO_TIME);
	}

	public void printSameLine(String line, String color) {
		if ("red".equalsIgnoreCase(color))
			System.err.print(line);
		else
			System.out.print(line);
	}

	public void printSameLine(String line) {
		System.out.print(line);
	}

	public int getGeneratedTasks() {
		return generatedTasksCount;
	}

	public void setGeneratedTasks(int generatedTasks) {
		this.generatedTasksCount = generatedTasks;
	}

	public void setCurrentOrchPolicy(String currentOrchPolicy) {
		this.currentOrchArchitecture = currentOrchPolicy;
	}

	public void initialize(SimulationManager simulationManager, int dev, int alg, int arch) {
		this.currentEdgeDevicesCount = dev;
		this.currentOrchAlgorithm = SimulationParameters.ORCHESTRATION_AlGORITHMS[alg];
		this.currentOrchArchitecture = SimulationParameters.ORCHESTRATION_ARCHITECTURES[arch];
		this.simulationManager = simulationManager;
	}

	public String getSimStartTime() {
		return simStartTime;
	}

	public void setSimStartTime(String simStartTime) {
		this.simStartTime = simStartTime;
	}

	public void incrementTasksSent() {
		this.tasksSent++;
	}

	public void incrementTasksFailed(Task task) {
		this.tasksFailed++;
		if (task.getOffloadingDestination() == null)
			return;
		SimulationParameters.TYPES type = task.getOffloadingDestination().getType();

		if (type == SimulationParameters.TYPES.CLOUD) {
			this.tasksFailedCloud++;
		} else if (type == SimulationParameters.TYPES.EDGE_DATACENTER) {
			this.tasksFailedEdge++;
		} else if (type == SimulationParameters.TYPES.EDGE_DEVICE) {
			this.tasksFailedMist++;
		}

		TaskNode taskNode = (TaskNode) task;
		if(taskNode.isCritical()){
			numCriticalTaskFailed++;
		}
	}

	public void incrementFailedBeacauseDeviceDead(Task task) {
		this.tasksFailedBeacauseDeviceDead++;
		incrementTasksFailed(task);
	}

	public void incrementFailedDueToUEFailire(Task task){
		numTaskFailedOnUEFailure++;
		incrementTasksFailed(task);
	}

	public void incrementNotGeneratedBeacuseDeviceDead() {
		this.notGeneratedBecDeviceDead++;
	}

	public void incrementTasksFailedLatency(Task task) {
		this.tasksFailedLatency++;
		incrementTasksFailed(task);
	}

	public void incrementTasksFailedMobility(Task task) {
		this.tasksFailedMobility++;
		incrementTasksFailed(task);
	}

	public void incrementTasksFailedLackOfRessources(Task task) {
		this.tasksFailedRessourcesUnavailable++;
		incrementTasksFailed(task);
	}

	public void getTasksExecutionInfos(Task task) {
		this.totalExecutionTime += task.getActualCpuTime() + task.getUploadTransmissionLatency() + task.getDownloadTransmissionLatency();
		this.totalWaitingTime += task.getWatingTime();
		this.executedTasksCount++;
		this.totalNetworkTime += task.getUploadTransmissionLatency() + task.getDownloadTransmissionLatency();
		this.totalNetworkTimeEx += task.getActualNetworkTime();
	}

	public void taskSentFromOrchToDest(Task task) {
		SimulationParameters.TYPES type = task.getOffloadingDestination().getType();
		String dc_name = task.getOffloadingDestination().getName();
		if (type == SimulationParameters.TYPES.CLOUD) {
			this.tasksExecutedOnCloud++;
		} else if (type == SimulationParameters.TYPES.EDGE_DATACENTER) {
			this.tasksExecutedOnEdge++;
			Double count = dcWiseTaskExecuted.containsKey(dc_name) ? dcWiseTaskExecuted.get(dc_name) : 0;
			dcWiseTaskExecuted.put(dc_name, count + 1);
		} else if (type == SimulationParameters.TYPES.EDGE_DEVICE) {
			this.tasksExecutedOnMist++;
		}
	}

	public void updateNetworkUsage(TransferProgress transfer) {
		this.totalLanUsage += transfer.getLanNetworkUsage();
		this.totalManUsage += transfer.getManNetworkUsage();
		this.totalWanUsage += transfer.getWanNetworkUsage();
		this.totalBandwidth += transfer.getAverageBandwidth() / 1000000; // bits/s to Mbits/s
		this.totalTraffic += transfer.getFileSize() / 8000000; // bits to Mbytes

		if (transfer.getTransferType() == TransferProgress.Type.CONTAINER) {
			this.containersLanUsage += transfer.getLanNetworkUsage();
			this.containersWanUsage += transfer.getWanNetworkUsage();
			this.containersManUsage += transfer.getManNetworkUsage();
		}
		this.transfersCount++;

	}

}
