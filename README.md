# ARLCL-Optimizer

[![ARLCL-Optimizer](https://indoorpaths.com/3rd_sources/ARLCL/Cycles_in_ARLCL.jpg "ARLCL-Optimizer")](https://indoorpaths.com/3rd_sources/ARLCL/Cycles_in_ARLCL.jpg "ARLCL-Optimizer")

ARLCL-Optimizer is an application implementing the cooperative localization method ARLCL: Anchor-free Ranging-Likelihood-based Cooperative Localization. This method has been developed by the Communication and Distributed Systems research group at the University of Bern.

The application supports both Graphical (for single scenario executions) and Headless (for batch executions) modes.

## Ranging Database

Depending on the execution mode, two different types of files can be used. GUI mode makes use of single database files (.rss or .smpl) containing the ranging measurements for a specific scenario. Headless mode makes use of an additional index file containing the scenario names of multiple database files. 

This repo provides sample databases and index files.
The complete databases used for the evaluation of ARLCL are also openly available:

Complete Evaluation Dataset for BLE:
[![DOI](https://zenodo.org/badge/DOI/10.5281/zenodo.7552462.svg)](https://doi.org/10.5281/zenodo.7552462)


For the creation of new database files and to support the resulted localization performance, the structure of a database file needs to be specific and contain a) for each node, the positions' ground truth and b) for each pair, the ranging measurement (dBm/time) used by arlcl and the distance measurement (cm) used by the baselines.

DB ("T_4,5,6_1.rss") example for BLE measurements:

	#POSITIONS GROUND TRUTH#
	4:-5.682;-4.377
	5:-5.237;-3.921
	6:-3.808;-2.652

	#RSS_0#
	4;5:-47.292382240617094&0.6372
	5;4:-47.292382240617094&0.6372
	4;6:-64.94839708270037&2.5471
	6;4:-64.94839708270037&2.5471
	5;6:-61.288393078461255&1.9111
	6;5:-61.288393078461255&1.9111
	
	...

	#RSS_100#
	4;5:-49.4872253506454&0.7569
	5;4:-49.4872253506454&0.7569
	4;6:-64.72211723091444&2.5022
	6;4:-64.72211723091444&2.5022
	5;6:-58.29407210961294&1.5109
	6;5:-58.29407210961294&1.5109

DB ("A_2,4,18,30,34_5.smpl") example for UWB measurements:

	#POSITIONS GROUND TRUTH#
	2:-213.725998;-196.776993
	4:-212.324997;-196.675003
	18:-213.651993;-193.326004
	30:-212.774994;-186.389999
	34:-211.026993;-188.248001

	#TIME_0#
	2;4:12.058198919907305&361.3872928528868
	2;18:45.38120426134302&1360.0862503051758
	4;2:11.966366375597334&358.635048110168
	4;30:39.7734261513773&1192.0197120484183
	18;2:45.52596614645396&1364.4248008728027
	18;34:27.9284414707915&837.0225042536639
	30;4:39.99122473425343&1198.5471910505153
	30;34:29.63656320286695&888.2153476225702
	34;18:27.28812194275694&817.8319649090843
	34;30:28.18789569155599&844.7984133329657

	...

	#TIME_100#
	2;4:12.423001798192777&372.3205280303955
	2;18:45.38120426134302&1360.0862503051758
	4;2:12.413611670373811&372.0391035079956
	4;30:40.67758376713238&1219.117546081543
	18;2:45.52596614645396&1364.4248008728027
	18;34:26.041666825515573&780.4753875732422
	30;4:40.98119519518652&1228.2168579101562
	30;34:25.118313882410856&752.8022646903992
	34;18:21.750425825083774&651.8658018112183
	34;30:31.040289340698912&930.2853775024414
		


An index file contains (per line) a scenario name available in the following in a database file, in the following format:

    T (1,2,3,4,5,6,7,8,9,10,11,12,13,14,15,16,17,18,19,20,21) 20
    T (4,5,6) 1 
    ...

## GUI mode
- Executes a cooperative localization optimization for the selected database of ranging measurements.

The application may start by executing the provided run.bat or from CLI using

`$ java --add-exports java.base/java.lang=ALL-UNNAMED --add-exports java.desktop/sun.awt=ALL-UNNAMED --add-exports java.desktop/sun.java2d=ALL-UNNAMED -jar path\to\arlcl-optimizer.jar`

In both cases one needs to set the correct "path\to\arlcl-optimizer.jar"

### Parameters

Parameter  | Description
------------- | -------------
Results per|Optimization state after which results shall be presented [Step/Cycle].
Rang. Model|Type of ranging measurement (should correspond to the loaded DB file) [BLE/UWB].
Export function|For storing the likelihood function as a Wolfram plot.
Contours|Number of contours to be drawn. Setting this to 0 will draw a 3D plot (Used for 3D localization).
Min Effective Measurement|Measurements above this threshold will not be considered during the localization. The effective neighbors are represented in yellow.
k Nearest Nodes for Effectiveness Check|The k closest neighbors to be considered for identifying the optimization order.
Extent of Random Positions Initialization|How far nodes can be randomly spawn during their initialization.
Seed for Random Positions Initialization|Seed for any random values generated during the optimization.
Iteration in scenario to evaluate|The is the ID of the last iteration. Each DB file can contain multiple evaluation iterations (e.g. 100 for the case of #RSS_100#).
Optimization Threads|Number of optimization thread workers to be used.
Optimization Iterations per Thread|How deep each optimization worker will be searching for the optimal solution at each step. 
Max Step-Opt. Runtime per Thread|A time threshold to be used per step optimization. Has a higher priority compared to the “Optimization Iterations per Thread”.
Optimization's ftol|Tolerance for the optimization based on the Nelder–Mead method.
Optimization's Initial Step Size|The step size considered by the Nelder–Mead method.
Optimization Cycles|For how many cycles to perform localization.

----

### Demo
www.youtube.com/watch?v=2DhkNLAwHkw
[![ARLCL-Optimizer GUI Usage/Export demo with UWB measurements](https://indoorpaths.com/3rd_sources/ARLCL/GUI_screenshot.png)](https://youtu.be/2DhkNLAwHkw)

## Headless mode

For batch execution, the application may start by executing the jar and passing the selected parameters.

Example command:

`$ java -jar "path/to/arlcl-optimizer.jar" out_path="path/to/the/results/folder/" db_path="path/to/the/db/folder/" db_idx_path="path/to/the/db-index/folder/" scenarios_path="D:\\OneDrive - Universitaet Bern\\Workspace\\Swarm Positioning\\UWB\\various\\Listed Scenarios\\35,40_nodes.txt" seed=3 end_iter=100 opt_iter=1000 threads=1 cycles=50 f_tol=1e-2 max_t=10000 kn=6 min_m=60 contours=30 step=10`

----

# CL based on Mass Spring
The cooperative localization engine based on **Mass Spring** which was used as a baseline.

The python script supports only headless mode (for batch execution) and requires batch results being available from ARLCL-Optimizer.

----

# CL based on Maximum Likelihood - Particle Swarm Optimization
The cooperative localization engine based on **Maximum Likelihood - Particle Swarm Optimization** which was used as a baseline.

The python script supports only headless mode (for batch execution) and requires batch results being available from ARLCL-Optimizer.

----


