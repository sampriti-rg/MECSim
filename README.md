# MECSim

MECSim: A Comprehensive Simulation Platform for Multi-Access Edge Computing

## 📖 Overview
MECSim (multi-access edge computing simulator), an enhanced simulation framework that extends PureEdgeSim
to enable realistic modeling of heterogeneous, cooperative, and fault-tolerant edge computing ecosystems.
MECSim supports multi–data-center clusters along with dynamic voltage and frequency scaling (DVFS) capable 
user devices for energy-efficient operation. The framework further integrates dependent task modeling, cost 
and profit evaluation for service providers, and reliability mechanisms through transient failure simulation, 
caching, and task replication.


MECSim offers many features as PureEdgeSim: 

1. Realistic modeling of edge computing scenarios (and its related computing paradigms).

*   It supports devices heterogeneity (sensors, mobile devices, battery-powered..).
*   It supports the heterogeneity of their means of communication (WiFi, 5G, 4G, Ethernet..).
*   Realistic modeling of computing infrastructure (modeling computational tasks, infrastructure topology...).
*   Realistic network model (Latency, bandwidth allocation, support for peer to peer communications, routing..).
*   Realistic mobility model.
*   Realistic energy model.

2.    The support for online decision making (tasks orchestration, scheduling, etc.).

3.    The study of QoS-aware architectures and topologies.

4.    Efficiency and Scalability.

*   MECSim supports the simulation of thousands of devices.
*   MECSim supports scenarios with longer simualtion time (even +24 hours).

5.    A wide collection of metrics.

*   Delays: execution time, waiting time, network time.
*   CPU utilization.
*   Network utilization: Average bandwidth, total traffic...
*   Energy consumption: The energy consumption of computing nodes (Cloud, edge/fog servers,edge devices), the remaining energy of battery-powered devices, the energy consumption of WAN, MAN, and LAN networks, and the enrgy consumption of WiFi, Cellular (5G, 4G,..) and Ethernet.
*   Task success rate and reason of failure (the rate of tasks failed due to the lack of resources, due to mobility, due to latency, or due to the death of edge devices when they run out of battery).
*   and many others.

7.    Extensibility: Users can implement their scenarios and integrate their models without modeifying PureEdgeSim codebase.
8.    Wide Applicability

*   Mist computing scenarios, mobile ad hoc edge computing, fog computing,...
*   Worklaod orchestration, scheduling, caching...

9.    Ease of use.

10.    Reliability.

11.    Correctness.

## 🧰 Exclusive Features

MECSim supports following exclusive features.
1. It supports multi-DC cooperative edge cluster with heterogeneous edge servers in terms of
   computing and storage capacities. 
* It also supports modern hardware supports in terms of computing and storage (GPU, PiM, SSD).
* It enhanced the task latency calculation to make it more reliable by addimg factors like IO time,
  data transfer between memory and processor time in addition to CPU time. Existing work only considers the CPU time.
* It supports task execution result caching using Broker

2. It supports dynamic voltage and frequency scaling (DVFS) devices and it helps in reducing energy consumption.

3. It supports dependent task modeling based on directed acyclic graphs (DAGs).
* It has  the capability to identify task criticality
* It can calculate reliability of a task and based on it can find low reliable tasks
* It supports reliability-aware offloading and selective task replication.
* It has option to simulate user device failure

![Real time charts](https://github.com/CharafeddineMechalikh/PureEdgeSim/blob/master/PureEdgeSim/files/real%20time.gif)
The live visualization of the simulated environement

### Efficiency and Scalability similar to PureEdgeSim
A system is said to be efficient when it can maintain a specified level of proficiency without utilizing an excessive amount of resources, i.e., execution time and memory. Determining the theoretical time complexity of the given simulation is not trivial because the execution time depends entirely on the user scenario (i.e., the devices and tasks count, the mobility model, the types of resources, etc.) and the number of events that occur during the simulation. Furthermore, some parameters strongly influence the number of simulations and, obviously, the number of runs also directly influences the execution time.
To demonstrate the scalability of PureEdgeSim, a few experiments were conducted on a single Intel Core i7-8550U.

1.  A 10-minute simulation scenario with 10000 devices (ten thousand, which generated 1015000 tasks) took 72 seconds. 
2.  A 24-hour simulation scenario with 200 devices (total of 2110706 tasks generated) took 91 seconds. 
3.  A 60-minute scenario with 100 devices took only 3 seconds, meaning that the simulator was able to run through as much as 1200 seconds of simulated time in just one second of real time. 

Of course, this time is highly dependent on the complexity of the decision making algorithm itself and the number of generated events  (as more devices = more tasks = more events), which is why it took longer with 10,000 devices. That said, it is still faster than real-time and outperforms all CloudSim/ CloudSim plus based simulators that either struggle to start due to the large number of devices or run out of memory due to the large number of events generated.

It is therefore easy to conclude that PureEdgeSim meets the “efficiency and scalability” criteria as it allows for simulated experiments involving hundreds if not thousands of devices and tasks and runs considerably faster than in wall-clock time. An important step to reduce the simulation duration and solve its dependency on the number of runs is to implement multi-threading to execute simulation runs in parallel, which is already supported by PureEdgeSim.

### Simulation Accuracy similar to PureEdgeSim

As mentioned previously, PureEdgeSim offers the most realistic network, energy, and mobility models as compared to existing solutions. However, since it is a DES, the time complexity of the simulation is dependent on the number of events generated at runtime. To decrease simulation time, PureEdgeSim provides fast and complete control over the simulation environment by its collection of parameters, allowing users to compromise between simulation precision and length. Hence, how accurate the simulation is will be influenced by the user’s settings, particularly the update intervals. The shorter these intervals, the more precise and realistic the simulation, but also the longer it takes. 

### Correctness similar to PureEdgeSim

The tool is said to be correct if it fully complies with the stated requirements, which can be verified by checking test cases. To verify PureEdgeSim’s correctness through unit and integration testing, several test cases have been implemented. Nevertheless, this alone does not provide an assessment of which portions of the code were tested and which were not, neither gives a percentage of the overall coverage of tests alongside the project source code. To evaluate the amount of code that the available unit tests cover, code coverage reports were added to the project with the help of the Java Code Coverage Library (JaCoCo). 
It is important to clarify that the concern of such test cases is the technical feasibility of the scenarios and not the evaluation of the simulation outcome. It is simply a matter of testing whether the given results are logically reasonable and not of actually validating them. To validate the software, a case study was conducted using the simulator. [At least 95% of PureEdgeSim code is covered by those tests](https://app.codacy.com/gh/CharafeddineMechalikh/PureEdgeSim/dashboard?utm_source=github.com&utm_medium=referral&utm_content=CharafeddineMechalikh/PureEdgeSim&utm_campaign=Badge_Coverage). 

This achievement was made possible by testing all the new features and also by the considerable decrease in code duplication since duplicating code brings neglect of testing. 
In conclusion, we can say that the proposed simulator performs as intended. Every function has finished without abortions and gave the expected output; All the input files were parsed successfully, and all the personalized models were integrated and verified with the predicted results. The plotting of the simulation results gave a reasonable graphical output. In terms of intended errors, such as incorrect inputs, proper error messages were displayed, and correct information was written into the log file. Moreover, the parameters can be modified, stored, and loaded without any error.
 

## 👩🏽‍💻 How to Use

There are several ways to use PureEdgeSim; however, it is strongly advisable to run MECSim via a Java development environment, like Eclipse or IntelliJ IDE. A set of predefined examples is provided under the “/examples“ directory, which should allow anyone to become familiar with MECSim.

### Using an IDE

The simplest and recommended method to run this project is to use an IDE like Eclipse, IntelliJ IDEA, or NetBeans. The required steps to get started with the project are listed below:

1.    Downloading and extracting the project source code archive from the project repository : This can be done using the web browser or the command line `git clone https://github.com/CharafeddineMechalikh/PureEdgeSim.git`.
2.    Importing the project to the IDE:

*   In NetBeans, simply select the "Open project" option and chose the project directory.
*   In Eclipse or IntelliJ IDEA, the project must be imported by selecting the location where it was extracted or cloned .

3.    The imported project consists of ten packages: the main application package, the above-mentioned modules, and the examples package. The main application and the modules are the simulator source code that normally should not be modified. The examples, however, are where the users can start.
4.    It is necessary to convert the project into a Maven project in order to download all the required libraries.
5.    Once all the necessary libraries are downloaded, users can start with the most basic examples by running any of the classes located in the “examples” package.
6.    To build a new simulation scenario, the simplest way is to create another class in this package.

![Environment](https://github.com/CharafeddineMechalikh/PureEdgeSim/blob/master/PureEdgeSim/files/importingproject.gif)
Importing PureEdgeSim project

### Adding PureEdgeSim as a Dependency

It is possible to use PureEdgeSim as a dependency in a Maven project, by inserting the dependency  below into the pom.xml file:
```xml
<dependency>
	<groupId>com.mechalikh</groupId>
	<artifactId>pureedgesim</artifactId>
	<version>5.0.0</version>
</dependency>
```
Or on Gradle :

```groovy
dependencies {
 implementation 'com.mechalikh:pureedgesim:5.0.0'
}
````

### Via Command Line

Assuming that git  and maven  are already installed, MECSim can be run from the command line as follows:
1.    First, the project source code must be downloaded by cloning the repository via the command `git clone https://github.com/CharafeddineMechalikh/PureEdgeSim.git`. 
2.    Now that the project is cloned, it can be built using Maven by executing the  `mvn clean install ` command in the directory where it was cloned.
3.    Now, the examples can be executed on Windows, Linux, or Mac operating systems, using the command  `mvn exec:java -Dexec.mainClass="package.Class_Name" `. For instance, to execute “Example1”, the command is  `mvn exec:java -Dexec.mainClass="examples.Example1" `
