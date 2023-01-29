import java.io.*;
import java.util.*;
import java.util.Map.Entry;


public class Core {
    static void init() throws Exception {
        SimApp.appendToTextArea("Initializing SwarmEngine");

        SimApp.appendToTextArea("Preparing the Optimization Workers (number: " + SimApp.threads + ")");
        MathEngine.generateTheOptimizerThreads();

        parseDB();
        // computeInitialCrossDistances();
        // Use this for debugging and to set the nodes to true Pos
        // reset_CurrentNodePos_to_TruePos();
        sortNodesByBeliefsStrength();

        // This is the first Map_Extent update to set the Global Min/Max X/Y to be ready for even the first optimization
        MapField.updateMapExtent();
    }

    static void getEffectiveNeighbors(Node currentNode) {

        // Find the closest remote Nodes to it
        SimApp.effective_remoteNodes = new ArrayList<>();

        // System.out.println("Getting Nodes with effective beliefs for node: " + currentNode.id);

        //Get the Measurements from all other Nodes
        for (HashMap.Entry<Integer, Double> entry : currentNode.measurement_from_node.entrySet()) {

            int remoteNode = entry.getKey();
            double measurement_value = entry.getValue();

            // Ignore all measurement values that are not described by our model
            // System.out.println("Checking if measurement " + measurement_value + " between remote Node " + remoteNode + " and current Node " + currentNode.id + " is at least " + SimApp.min_effective_measurement_inputTextField.getText());

            if ((measurement_value <= Integer.parseInt(SimApp.min_effective_measurement_inputTextField.getText()))
                    && (remoteNode != currentNode.id)) {
                SimApp.effective_remoteNodes.add(remoteNode);

                //System.out.println("Effective Node " + remoteNode + " added for " + currentNode + " due to measurement " + measurement_value);

                SimApp.nodeID_to_nodeObject.get(remoteNode).cdl.updateMeasurementAndExtentReach(measurement_value);
            }
        }
    }

    static private void sortNodesByBeliefsStrength(){
        // Create a Node-Popularity mapping (based on the average Beliefs-Strength from the k strongest measurements)
        HashMap<Integer, Double> node_popularity = new HashMap<>();

        for (Entry<Integer, Node> current_NodeEntry: SimApp.nodeID_to_nodeObject.entrySet()) {
            int current_NodeID = current_NodeEntry.getKey();
            Node current_Node = current_NodeEntry.getValue();

            List<Double> measurement_values_list = new ArrayList<>(){};

            measurement_values_list.addAll(current_Node.measurement_from_node.values());

            // Keep only the 5 strongest signals
            Collections.sort(measurement_values_list);

            int considered_kNearestNeigbors_for_ClosenessCheck = Integer.parseInt(
                    SimApp.kNearestNeighbours_for_BeliefsStrength_inputTextField.getText());

            if (measurement_values_list.size() > considered_kNearestNeigbors_for_ClosenessCheck){
                node_popularity.put(current_NodeID, getAverage(measurement_values_list.subList(0, considered_kNearestNeigbors_for_ClosenessCheck)));
            }
            else {
                node_popularity.put(current_NodeID, getAverage(measurement_values_list));
            }
        }

        SimApp.OrderedByBeliefsStrength_NodeIDs = new ArrayList<>();

//        System.out.println(node_popularity);

        node_popularity.entrySet().stream()
                .sorted(HashMap.Entry.comparingByValue())
                .forEachOrdered(x -> SimApp.OrderedByBeliefsStrength_NodeIDs.add(x.getKey()));

//        System.out.println(Sim_App.OrderedByBeliefsStrength_NodeIDs);
    }

    static private double getAverage(List <Double> items) {
        double sum = 0;
        if(!items.isEmpty()) {
            for (double mark: items) {
                sum += mark;
            }
            return sum/items.size();
        }
        return sum;
    }

    static private void constrainLongDistances(){
        // At this point we have considered all recorded pair-measurements.
        // However, we also need to consider all pairs for which no measurement exists. These relations shall be used
        // to constraint close distances
        for (Entry<Integer, Node> current_NodeEntry: SimApp.nodeID_to_nodeObject.entrySet()) {
            int current_NodeID = current_NodeEntry.getKey();
            Node current_Node = current_NodeEntry.getValue();

            for (Entry<Integer, Node> remote_NodeEntry: SimApp.nodeID_to_nodeObject.entrySet()) {
                int remote_NodeID = remote_NodeEntry.getKey();
                Node remote_Node = remote_NodeEntry.getValue();

                // Avoid considering the same Node
                if (current_NodeID != remote_NodeID){
                    //System.out.println(current_NodeID + " to " + remote_NodeID + ": " + );
                }
            }
        }
    }

    static private void computeInitialCrossDistances(){
        // At this point, we have already generated all involved Nodes

        // Update all the distances
        for (Entry<Integer, Node> current_NodeEntry: SimApp.nodeID_to_nodeObject.entrySet()) {
            int current_NodeID = current_NodeEntry.getKey();
            Node current_Node = current_NodeEntry.getValue();

            for (Entry<Integer, Node> remote_NodeEntry: SimApp.nodeID_to_nodeObject.entrySet()) {
                int remote_NodeID = remote_NodeEntry.getKey();
                Node remote_Node = remote_NodeEntry.getValue();

                // Avoid considering the same Node
                if(current_NodeID != remote_NodeID){
                    // Update for current Node,
                    // both the true and current (due to initial randomness) Distance towards the remote Node
                    double current_distance_to_remoteNode = current_Node.getDistanceToNode(remote_Node, false);
                    current_Node.current_distance_to_node.put(remote_NodeID, current_distance_to_remoteNode);

                    double true_distance_to_remoteNode = current_Node.getDistanceToNode(remote_Node, true);
                    current_Node.true_distance_to_node.put(remote_NodeID, true_distance_to_remoteNode);

                    //System.out.println(current_NodeID + ":[" + current_Node.true_relative_x + "," + current_Node.true_relative_y + "]- " + remote_NodeID + ":[" + remote_Node.true_relative_x + "," + remote_Node.true_relative_y + "]: DistanceToRemoteNode= " + current_distance_to_remoteNode);
                }
            }
        }
    }

    static private void parseDB() throws Exception {
        // Loading the database
        SimApp.appendToTextArea("Parsing Node DB");

        // Open the file
        FileInputStream fstream = new FileInputStream(SimApp.input_file_path);
        BufferedReader br = new BufferedReader(new InputStreamReader(fstream));

        String strLine;

        // Data format in the .rss file is important!
        String positions_header = "#POSITIONS GROUND TRUTH#";

        String current_parsing_type = null;

        //Read File Line By Line
        while ((strLine = br.readLine()) != null)   {

            // Try to detect a flag
            if (strLine.equals(positions_header)){
                current_parsing_type = positions_header;
                continue;
            }
            else if (strLine.startsWith("#")){
                current_parsing_type = "#";
                continue;
            }
            else if (strLine.equals("")){
                current_parsing_type = null;
                continue;
            }

            if (current_parsing_type != null){
                // Being here means that we haven't escaped above with a continue due to successful header_check matching
                // Handle accordingly the parsed data
                if (current_parsing_type.equals(positions_header)){
                    // Start with the position
                    String[] position_parts = strLine.split(":");
                    String[] coords = position_parts[1].split(";");

                    int parsed_nodeID = Integer.parseInt(position_parts[0].trim());
                    double posX = Double.parseDouble(coords[0].trim());
                    double posY = Double.parseDouble(coords[1].trim());

                    // Create the Node object and add it to the Node Map
                    Node new_node = new Node(parsed_nodeID, posX, posY);
                    SimApp.nodeID_to_nodeObject.put(parsed_nodeID, new_node);
                    SimApp.appendToTextArea(new_node.get_report());
                }

                else {
                    String[] measurement_value_parts = strLine.split(":");

                    String[] str_nodes = measurement_value_parts[0].split(";");
                    int nodeA = Integer.parseInt(str_nodes[0]);
                    int nodeB = Integer.parseInt(str_nodes[1]);

                    double measurement_value = Double.parseDouble(measurement_value_parts[1].split("&")[0]);

                    // Update for 1st Node, the measurement towards the 2nd Node
                    SimApp.nodeID_to_nodeObject.get(nodeA).measurement_from_node.put(nodeB, measurement_value);

                    // System.out.println("Node " + nodeA + " to Node " + nodeB + " measurement: " + measurement_value);
                }
            }
        }

        //Close the input stream
        fstream.close();
    }

    static void resume_SwarmPositioning() throws Exception {
        SimApp.optimization_running = true;

        SimApp.appendToTextArea("Position Estimations:");

        // Check if there is no remaining step from previous unfinished cycles
        if (SimApp.temp_OrderedRemoteNodeIDs.size()==0){

            if (!SimApp.headless_mode){
                // We align the swarm based on the principal spatial variation for better visualization in GUI
                // MathEngine.align_Swarm();
                // TODO we need to do that only for visualization purposes
            }

            // We start a new Cycle. At this point, sortNodesByBeliefsStrength() has already ordered the nodes.
            SimApp.temp_OrderedRemoteNodeIDs.addAll(SimApp.OrderedByBeliefsStrength_NodeIDs);

            // Update also the progress counters
            SimApp.cycleCounter = SimApp.cycleCounter + 1;
            SimApp.stepCounter = 0;
        }

        // Check if the user wants to get results per step
        if (!SimApp.headless_mode && SimApp.results_per_step_btn.getState()){
            SimApp.stepCounter = SimApp.stepCounter + 1;

            Node currentNode = SimApp.nodeID_to_nodeObject.get(SimApp.temp_OrderedRemoteNodeIDs.remove(0));
            //System.out.println("Removing: " + currentNode + " TempList: " + RemoteNodeIDbyPopularity_tracker.size() + " OriginalList: " + NodeIDbyPopularity_originalList.size());

            //currentNode = 6; // Set this manually for debugging purposes

            double[] new_current_position = MathEngine.findBestPositionForCurrentNode(currentNode, SimApp.cycleCounter, SimApp.stepCounter); // Java

            if (new_current_position != null){
                currentNode.update_CurrentNodePos(new_current_position[0], new_current_position[1]);
            }

            MapField.updateMapExtent();
            MathEngine.publishResultsInGUI(SimApp.cycleCounter, SimApp.stepCounter, currentNode);

            // todo use this for debugging whenever needed
            /*
            if (resetAll_CurrentNodePos_to_TruePos){
                reset_CurrentNodePos_to_TruePos();
            }
            */
        }
        // Being here means that the user wants to get results per cycle
        else {
            boolean last_step = false;
            int remaining_steps = SimApp.temp_OrderedRemoteNodeIDs.size();

            for (int step = 0; step<remaining_steps; step++){
                SimApp.stepCounter = SimApp.stepCounter + 1;

                int currentNodeID = SimApp.temp_OrderedRemoteNodeIDs.remove(0);
                Node currentNode = SimApp.nodeID_to_nodeObject.get(currentNodeID);

                // Check if after removing this node, we ended up at the last optimization step
                if (SimApp.temp_OrderedRemoteNodeIDs.size()==0){
                    last_step = true;
                }

                //System.out.println("Removing: " + currentNodeID + " TempList: " + RemoteNodeIDbyPopularity_tracker.size() + " OriginalList: " + NodeIDbyPopularity_originalList.size());

                double[] new_current_position = MathEngine.findBestPositionForCurrentNode(
                        SimApp.nodeID_to_nodeObject.get(currentNodeID), SimApp.cycleCounter, SimApp.stepCounter);

                if (new_current_position != null){
                    currentNode.update_CurrentNodePos(new_current_position[0], new_current_position[1]);
                }

                MapField.updateMapExtent();
                // Check whether we are currently at the last step
                if (last_step){
                    MathEngine.publishResultsInGUI(SimApp.cycleCounter, SimApp.stepCounter, currentNode);
                }

                // Check if the stop Button is not enabled.
                // In such case, the user has manually stopped the optimization and so, break the iteration..
                if (SimApp.optimization_cycles < SimApp.cycleCounter){
                    System.out.println("Current finished cycle: " + SimApp.optimization_cycles);
                    break;
                }
            }
        }

        SimApp.optimization_running = false;
        SimApp.appendToTextArea("Map Extent: (" + MapField.global_minPlotX + ", " + MapField.global_maxPlotX + "), (" + MapField.global_minPlotY + ", " + MapField.global_maxPlotY + ")");
        SimApp.appendToTextArea("=========== Optimization Finished ===========");

        // If we are running in headless mode and the cycles have finished,
        // we should stop nicely the current optimization process as we would do in GUI mode by pressing the Stop
        if (SimApp.headless_mode && SimApp.optimization_cycles <= SimApp.cycleCounter){
            // Stop the optimization right after the chosen amount of cycles
            SimApp.stopOptimization();

            // Stop the auto-resumer for the current optimization process
            SimApp.scheduled_auto_resumer.cancel(true);
            SimApp.scheduler.shutdown();
        }
    }

    static void resumeSwarmPositioningInGUIMode() throws Exception {
        SimApp.optimization_running = true;

        SimApp.appendToTextArea("Position Estimations:");

        // Check if there is no remaining step from previous unfinished cycles
        if (SimApp.temp_OrderedRemoteNodeIDs.size()==0){

            // We align the swarm based on the principal spatial variation for better visualization in GUI
            // MathEngine.align_Swarm();
            // TODO we need to do that only for visualization purposes

            // We start a new Cycle. At this point, sortNodesByBeliefsStrength() has already ordered the nodes.
            SimApp.temp_OrderedRemoteNodeIDs.addAll(SimApp.OrderedByBeliefsStrength_NodeIDs);

            // Update also the progress counters
            SimApp.cycleCounter = SimApp.cycleCounter + 1;
            SimApp.stepCounter = 0;
        }

        // Check if the user wants to get results per step
        if (SimApp.results_per_step_btn.getState()){

            SimApp.stepCounter = SimApp.stepCounter + 1;

            Node currentNode = SimApp.nodeID_to_nodeObject.get(SimApp.temp_OrderedRemoteNodeIDs.remove(0));
            //System.out.println("Removing: " + currentNode + " TempList: " + RemoteNodeIDbyPopularity_tracker.size() + " OriginalList: " + NodeIDbyPopularity_originalList.size());

            //currentNode = 6; // Set this manually for debugging purposes

            double[] new_current_position = MathEngine.findBestPositionForCurrentNode(currentNode, SimApp.cycleCounter, SimApp.stepCounter);

            // We use the same optimization function to publish the likelihood, before updating the nodes' positions
            MathEngine.publishResultsInGUI(SimApp.cycleCounter, SimApp.stepCounter, currentNode);

            if (new_current_position != null){
                currentNode.update_CurrentNodePos(new_current_position[0], new_current_position[1]);
            }

            MapField.updateMapExtent();

            // todo use this for debugging whenever needed
            /*
            if (resetAll_CurrentNodePos_to_TruePos){
                reset_CurrentNodePos_to_TruePos();
            }
            */
        }
        // Being here means that the user wants to get results per cycle
        else {
            boolean last_step = false;
            int remaining_steps = SimApp.temp_OrderedRemoteNodeIDs.size();

            for (int step = 0; step<remaining_steps; step++){
                SimApp.stepCounter = SimApp.stepCounter + 1;

                int currentNodeID = SimApp.temp_OrderedRemoteNodeIDs.remove(0);
                Node currentNode = SimApp.nodeID_to_nodeObject.get(currentNodeID);

                // Check if after removing this node, we ended up at the last optimization step
                if (SimApp.temp_OrderedRemoteNodeIDs.size()==0){
                    last_step = true;
                }

                //System.out.println("Removing: " + currentNodeID + " TempList: " + RemoteNodeIDbyPopularity_tracker.size() + " OriginalList: " + NodeIDbyPopularity_originalList.size());

                double[] new_current_position = MathEngine.findBestPositionForCurrentNode(
                        SimApp.nodeID_to_nodeObject.get(currentNodeID), SimApp.cycleCounter, SimApp.stepCounter);

                // Check whether we are currently at the last step
                if (last_step){
                    // We use the same optimization function to publish the likelihood, before updating the nodes' positions
                    MathEngine.publishResultsInGUI(SimApp.cycleCounter, SimApp.stepCounter, currentNode);
                }

                if (new_current_position != null){
                    currentNode.update_CurrentNodePos(new_current_position[0], new_current_position[1]);
                }

                MapField.updateMapExtent();

                // Check if the stop Button is not enabled.
                // In such case, the user has manually stopped the optimization and so, break the iteration..
//                if (SimApp.optimization_cycles < SimApp.cycleCounter){
//                    System.out.println("Current finished cycle: " + SimApp.optimization_cycles);
//                    break;
//                }
            }
        }

        SimApp.optimization_running = false;
        SimApp.appendToTextArea("Map Extent: (" + MapField.global_minPlotX + ", " + MapField.global_maxPlotX + "), (" + MapField.global_minPlotY + ", " + MapField.global_maxPlotY + ")");
        SimApp.appendToTextArea("=========== Optimization Finished ===========");
    }

    // Use the following to reset everytime the Nodes to their True Position (for debugging)
    static private void reset_CurrentNodePos_to_TruePos(){
        // Loop throughout all Nodes
        for (Node node: SimApp.nodeID_to_nodeObject.values()){
            node.update_CurrentNodePos(node.true_relative_x, node.true_relative_y);
        }
    }
}
