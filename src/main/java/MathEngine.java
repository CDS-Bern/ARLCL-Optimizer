import flanagan.math.MaximisationFunction;
import org.apache.commons.math3.stat.regression.SimpleRegression;
import org.apache.commons.math3.util.FastMath;

import java.io.File;
import java.math.BigDecimal;
import com.wolfram.jlink.*;

import java.nio.file.FileSystems;
import java.nio.file.Paths;
import java.util.*;


// Class to demonstrate Maximisation method, Maximisation nelderMead
class MathEngine {

    static String ProductFinalFunction_mtcaObject;

    // simplification 1
    static double bestLikelihood = Double.NEGATIVE_INFINITY; // POSITIVE_INFINITY // NEGATIVE_INFINITY;

    static String NodePos_Results_filename;

    static Optimizer[] swarmPositioningOptimizers = new Optimizer[SimApp.threads];

    static void create_the_optimizers(){
        // Generate as many Optimizers as the available Cores on the machine
        for (int thread = 0; thread< SimApp.threads; thread++){
            // Generate a new optimizer and add it to the thread mapper
            swarmPositioningOptimizers[thread] = new Optimizer();
        }
    }

    static void align_Swarm(){
        SimpleRegression regr = new SimpleRegression();

        // First compute the regression
        for (Node current_Node: SimApp.nodeID_to_nodeObject.values()) {
            regr.addData(current_Node.current_relative_x, current_Node.current_relative_y);
        }

        double y_intercept = regr.getIntercept();
        double rad_rotation = -FastMath.atan(regr.getSlope());

        // Prepare these variables to hold the new extent
        double minX = Double.MAX_VALUE;
        double minY = Double.MAX_VALUE;
        double maxX = Double.MIN_VALUE;
        double maxY = Double.MIN_VALUE;

        // Rotate all Nodes according to the calculated slope
        for (Node current_Node: SimApp.nodeID_to_nodeObject.values()) {

            final double newX = (current_Node.current_relative_x)*Math.cos(rad_rotation) - (current_Node.current_relative_y-y_intercept)*Math.sin(rad_rotation);
            final double newY = (current_Node.current_relative_x)*Math.sin(rad_rotation) + (current_Node.current_relative_y-y_intercept)*Math.cos(rad_rotation);

            // Update the extent variables
            if (newX<minX){
                minX = newX;
            }
            if (newX>maxX){
                maxX = newX;
            }

            if (newY<minY){
                minY = newY;
            }
            if (newY>maxY){
                maxY = newY;
            }

            current_Node.current_relative_x = newX;
            current_Node.current_relative_y = newY;
        }

        // Calculate the vector shift required to place the Swarm's center of mass at the Origin
        final double swarmCenterX = -(minX + ((maxX - minX)/2));
        final double swarmCenterY = -(minY + ((maxY - minY)/2));

        // After having rotated all Nodes, we will displace them so that their mass center is at 0,0
        for (Node current_Node: SimApp.nodeID_to_nodeObject.values()) {
            current_Node.current_relative_x = current_Node.current_relative_x + swarmCenterX;
            current_Node.current_relative_y = current_Node.current_relative_y + swarmCenterY;
        }

    }

    static List<Integer> order_NodeIDs_byPositionX(){

        List<Integer> orderedByLastCycleOrientation_NodeIDs = new ArrayList<>();

        Integer[] nodeIDs = new Integer[SimApp.nodeID_to_nodeObject.size()];
        double[] posX = new double[SimApp.nodeID_to_nodeObject.size()];

        for (int i = 0; i< SimApp.nodeID_to_nodeObject.size(); i++){
            nodeIDs[i] = i;
            posX[i] = SimApp.nodeID_to_nodeObject.get(i+1).current_relative_x;
        }

        Arrays.sort(nodeIDs, Comparator.comparingDouble(o -> posX[o]));

        for (int i:nodeIDs){
            orderedByLastCycleOrientation_NodeIDs.add(i+1);
        }

        return orderedByLastCycleOrientation_NodeIDs;
    }

    private static ArrayList[] collectIndividualMathematicaPlotComponents(Node current_node){

        System.out.println("Collecting individual mathematica plot components for node " + current_node.id);

        // Hold all likelihood components and labels in the following variables
        ArrayList productLikelihoodComponents = null;

        // Check if the user has selected to export also the ProductLikelihood
        if (SimApp.headless_mode || SimApp.export_ProductLikelihood_WolframPlot_function_btn.getState()){
            // Create the Wolfram CMD
            productLikelihoodComponents = getNeighborBeliefsForNode(current_node);
        }

        ArrayList[] mathematica_components = new ArrayList[2];

        mathematica_components[0] = build_PositionsCanvasPlot_WolframCMD(current_node);
        mathematica_components[1] = productLikelihoodComponents;

        return mathematica_components;
    }

    private static ArrayList build_PositionsCanvasPlot_WolframCMD(Node current_node){

        StringBuilder temp_EffectiveNode_Pos_component = new StringBuilder();
        temp_EffectiveNode_Pos_component.append("plotA = ListPlot[\n  {");

        StringBuilder temp_EffectiveNode_Labels_component = new StringBuilder();
        temp_EffectiveNode_Labels_component.append("} -> {");

        StringBuilder temp_nonEffectiveNode_Pos_component = new StringBuilder();
        temp_nonEffectiveNode_Pos_component.append("\n\nplotB = ListPlot[\n  {");

        StringBuilder temp_nonEffectiveNode_Labels_component = new StringBuilder();
        temp_nonEffectiveNode_Labels_component.append("} -> {");

        String temp_currentNode_component = null;

        for (Node remote_node: SimApp.nodeID_to_nodeObject.values()){
            // Make sure we are not considering the same
            if (remote_node.id != current_node.id){
                // Check if this remote Node is among the effective ones
                if (SimApp.effective_remoteNodes.contains(remote_node.id)){
                    temp_EffectiveNode_Pos_component.append("{" + remote_node.current_relative_x + ", " + remote_node.current_relative_y + "}, ");
                    temp_EffectiveNode_Labels_component.append(remote_node.id + ", ");
                }
                else{
                    // Being here means that this remote Node is not among the effective ones.
                    // Yet, we want to plot it
                    temp_nonEffectiveNode_Pos_component.append("{" + remote_node.current_relative_x + ", " + remote_node.current_relative_y + "}, ");
                    temp_nonEffectiveNode_Labels_component.append(remote_node.id + ", ");
                }
            }
            // Also handle specifically the styling of current Node
            else {
                temp_currentNode_component = "\n\nplotC = ListPlot[\n" +
                        "   {{" + current_node.current_relative_x + ", " + current_node.current_relative_y + "}} -> {\"" + current_node.id + "\"},\n" +
                        "   AspectRatio -> Automatic,\n" +
                        "   PlotStyle -> Opacity[1, Green],\n" +
                        "   Background -> Transparent,\n" +
                        "   PlotMarkers -> {Automatic, 30},\n" +
                        "   PlotRange -> {{" +
                                MapField.global_minPlotX + ", " +
                                MapField.global_maxPlotX + "}, {" +
                                MapField.global_minPlotY + ", " +
                                MapField.global_maxPlotY + "}},\n" +
                        "   LabelingFunction -> Center,\n" +
                        "   LabelStyle -> {FontFamily -> \"Helvetica\", FontSize -> 13, Bold}\n" +
                        "   ];\n";
            }
        }

        // Remove the last character from the commands
        temp_EffectiveNode_Pos_component.setLength(temp_EffectiveNode_Pos_component.length() - 2);
        temp_EffectiveNode_Labels_component.setLength(temp_EffectiveNode_Labels_component.length() - 2);
        temp_EffectiveNode_Pos_component.append(temp_EffectiveNode_Labels_component);

        temp_EffectiveNode_Pos_component.append("},\n" +
                "   AspectRatio -> Automatic,\n" +
                "   PlotStyle -> Opacity[1, Yellow],\n" +
                "   Background -> Transparent,\n" +
                "   PlotMarkers -> {Automatic, 25},\n" +
                "   PlotRange -> {{" +
                    MapField.global_minPlotX + ", " +
                    MapField.global_maxPlotX + "}, {" +
                    MapField.global_minPlotY + ", " +
                    MapField.global_maxPlotY + "}},\n" +
                "   LabelingFunction -> Center,\n" +
                "   LabelStyle -> {FontFamily -> \"Helvetica\", FontSize -> 13, Bold}\n" +
                "  ];");

        temp_nonEffectiveNode_Pos_component.setLength(temp_nonEffectiveNode_Pos_component.length() - 2);
        temp_nonEffectiveNode_Labels_component.setLength(temp_nonEffectiveNode_Labels_component.length() - 2);
        temp_nonEffectiveNode_Pos_component.append(temp_nonEffectiveNode_Labels_component);

        temp_nonEffectiveNode_Pos_component.append("},\n" +
                "   AspectRatio -> Automatic,\n" +
                "   PlotStyle -> Opacity[1, Orange],\n" +
                "   Background -> Transparent,\n" +
                "   PlotMarkers -> {Automatic, 25},\n" +
                "   PlotRange -> {{" +
                    MapField.global_minPlotX + ", " +
                    MapField.global_maxPlotX + "}, {" +
                    MapField.global_minPlotY + ", " +
                    MapField.global_maxPlotY + "}},\n" +
                "   LabelingFunction -> Center,\n" +
                "   LabelStyle -> {FontFamily -> \"Helvetica\", FontSize -> 13, Bold}\n" +
                "  ];");

        // Create an ArrayList that will always have 1 member only.
        // Namely, the Wolfram Command to generate the Canvas Plot with the Positions
        ArrayList positionsCanvasPlot_WolframCMD_ArrayListContainer = new ArrayList<>();

        // Make a check here to see if the list containing the effective_nodes has the same size as the entire Node db-1
        // This means that there is no non-Effective Node. Hence, we need to exclude Plot B
        if (SimApp.effective_remoteNodes.size() == (SimApp.nodeID_to_nodeObject.size()-1)){
            positionsCanvasPlot_WolframCMD_ArrayListContainer.add(temp_EffectiveNode_Pos_component.append(temp_currentNode_component).toString());
        }
        else{
            positionsCanvasPlot_WolframCMD_ArrayListContainer.add(temp_EffectiveNode_Pos_component.append(temp_nonEffectiveNode_Pos_component).append(temp_currentNode_component).toString());
        }

        return positionsCanvasPlot_WolframCMD_ArrayListContainer;
    }

    private static ArrayList getNeighborBeliefsForNode(Node current_node){

        ArrayList productLikelihoodComponents = new ArrayList<>();

        // System.out.println("Neighbors with effective beliefs: " + SimApp.effective_remoteNodes);

        for (Node remote_node: SimApp.nodeID_to_nodeObject.values()){
            // Make sure we are not considering the same
            if (remote_node.id != current_node.id){
                // Check if this remote Node is among the effective ones
                if (SimApp.effective_remoteNodes.contains(remote_node.id)){
                    remote_node.cdl.updateProductLikelihoodComponentWolframOBJ();
                    // System.out.println(remote_node.cdl.ProductLikelihoodComponent_WolframOBJ);
                    productLikelihoodComponents.add(remote_node.cdl.ProductLikelihoodComponent_WolframOBJ);
                }
            }
        }
        return productLikelihoodComponents;
    }

    static void publishResultsInHeadlessMode(int cycle, int step, Node currentNode){

        System.out.println("Publishing: " + SimApp.clean_evaluated_scenario_name + " Cycle:" + cycle);

        if (cycle == SimApp.optimization_cycles || cycle == 50 || cycle == 100 || cycle == 150 || cycle == 200 || cycle == 250){

            String NodePos_CMD_filename = Paths.get(
                    SimApp.output_iterated_results_folder_path,
                    "Positions_c" + cycle + "_s" + step + "_n" + currentNode.id + ".txt"
            ).toString();

            String NodePos_Plot_filename = Paths.get(
                    SimApp.output_iterated_results_folder_path,
                    "Positions_c" + cycle + "_s" + step + "_n" + currentNode.id + ".jpeg"
            ).toString();


            NodePos_Results_filename = Paths.get(
                    SimApp.output_iterated_results_folder_path,
                    "results.log"
            ).toString();

            ArrayList<String>[] mathematica_plot_components = collectIndividualMathematicaPlotComponents(currentNode);

            String ProductLikelihood_WolframCMD = "";

            // Check if the user has selected to export also the ProductLikelihood
            if (SimApp.headless_mode || SimApp.export_ProductLikelihood_WolframPlot_function_btn.getState()){
                // Create the Wolfram command
                ProductLikelihood_WolframCMD = buildProductLikelihoodWolframCMD(mathematica_plot_components[1]);
            }

            // Execute the Wolfram CMD that will generate the Canvas contents (i.e. the node's positions)
            String NodePos_WolframExportCMD;

            //Add to below (If we want to show also likelihood)
            boolean export_likelihood = false;
            if (SimApp.headless_mode || SimApp.export_ProductLikelihood_WolframPlot_function_btn.getState()){
                // Generate the final Wolfram command which we need to execute
                NodePos_WolframExportCMD = buildExportWolframCMD(
                        ProductLikelihood_WolframCMD + mathematica_plot_components[0].get(0),
                        NodePos_Plot_filename,
                        "model, ");
                export_likelihood = true;
            }
            else {
                // Generate the final Wolfram command which we need to execute
                NodePos_WolframExportCMD = buildExportWolframCMD(
                        mathematica_plot_components[0].get(0), NodePos_Plot_filename, "");
            }

            // Export the String to a file
            SimApp.writeString2File(NodePos_CMD_filename, NodePos_WolframExportCMD, export_likelihood);

            //SimApp.rendering_wolfram_data = false; // TODO
        }
    }

    static void publishResultsInGUI(int cycle, int step, Node currentNode) throws Exception {

        System.out.println("Publishing: " + SimApp.clean_evaluated_scenario_name + " Cycle:" + cycle);

        String NodePos_CMD_filename = Paths.get(
                SimApp.output_iterated_results_folder_path,
                "Positions_c" + cycle + "_s" + step + "_n" + currentNode.id + ".txt"
        ).toString();

        String NodePos_Plot_filename = Paths.get(
                SimApp.output_iterated_results_folder_path,
                "Positions_c" + cycle + "_s" + step + "_n" + currentNode.id + ".jpeg"
        ).toString();


        NodePos_Results_filename = Paths.get(
                SimApp.output_iterated_results_folder_path,
                "results.log"
        ).toString();

        ArrayList<String>[] mathematica_plot_components = collectIndividualMathematicaPlotComponents(currentNode);

        String ProductLikelihood_WolframCMD = "";

        // Check if the user has selected to export also the ProductLikelihood
        if (SimApp.export_ProductLikelihood_WolframPlot_function_btn.getState()){
            // Create the Wolfram command
            ProductLikelihood_WolframCMD = buildProductLikelihoodWolframCMD(mathematica_plot_components[1]);
        }

        // Execute the Wolfram CMD that will generate the Canvas contents (i.e. the node's positions)
        String NodePos_WolframExportCMD;

        //Add to below (If we want to show also likelihood)
        boolean export_likelihood = false;
        if (SimApp.export_ProductLikelihood_WolframPlot_function_btn.getState()){
            // Generate the final Wolfram command which we need to execute
            NodePos_WolframExportCMD = buildExportWolframCMD(
                    ProductLikelihood_WolframCMD + mathematica_plot_components[0].get(0),
                    NodePos_Plot_filename,
                    "model, ");
            export_likelihood = true;
        }
        else {
            // Generate the final Wolfram command which we need to execute
            NodePos_WolframExportCMD = buildExportWolframCMD(
                    mathematica_plot_components[0].get(0), NodePos_Plot_filename, "");
        }

        renderLikelihoodPlots();

        // Export the String to a file
        SimApp.writeString2File(NodePos_CMD_filename, NodePos_WolframExportCMD, export_likelihood);

        //SimApp.rendering_wolfram_data = false; // TODO

        // This assumes that the plot has been rendered and stored by Mathematica
        SimApp.updateCanvasPlot(NodePos_Plot_filename);
    }

    private static void renderLikelihoodPlots() {
        KernelLink ml = null;
        // Get the path to math kernel
        try {
            ml = MathLinkFactory.createKernelLink(SimApp.wolfram_engine_args);
            ml.evaluate("1+1");
            ml.waitForAnswer();
            int result = ml.getInteger();
            System.out.println("1 + 1 = " + result);
        } catch (MathLinkException e) {
            System.out.println("MathLinkException occurred: " + e.getMessage());
        } finally {
            ml.close();
        }
    }

    private static String buildProductLikelihoodWolframCMD(ArrayList<String> productLikelihoodComponents){

        StringBuilder temp_distance_likelihood_function = new StringBuilder();

        for (String f: productLikelihoodComponents){
            temp_distance_likelihood_function.append(f);
        }

        // Remove the last unwanted chars from the Strings
        // System.out.println(temp_distance_likelihood_function);
        temp_distance_likelihood_function.setLength(temp_distance_likelihood_function.length() - 2);
        ProductFinalFunction_mtcaObject = temp_distance_likelihood_function.toString();

        String mathematica_likelihdood_function = "r[distanceX_, distanceY_] := (" + ProductFinalFunction_mtcaObject + ")";
        String plot_cmd = mathematica_likelihdood_function
                + ")/ " + String.valueOf(bestLikelihood).replaceFirst("E-", "*^-") // Use extra ) for simplified UWB model
                + ";\n\n";

        plot_cmd = plot_cmd + "model = ContourPlot[r[distanceX, distanceY], " +
                "{distanceX, " + MapField.global_minPlotX + ", " + MapField.global_maxPlotX + "}, " +
                "{distanceY, " + MapField.global_minPlotY + ", " + MapField.global_maxPlotY + "}, " +
                "PlotPoints -> {" + Integer.valueOf(SimApp.plotResolution_inputTextField.getText()) + ", " + Integer.valueOf(SimApp.plotResolution_inputTextField.getText()) + "}, " +
                "MaxRecursion -> 1, " +
                "PlotRange -> Full, " +
                "ClippingStyle -> None, " +
                "FrameLabel -> {Style[\"X (cm)\", 20, Bold], Style[\"Y (cm)\", 20, Bold]}," +
                "FrameTicksStyle -> Directive[Black, 15]," +
                "GridLines -> Automatic," +
                "GridLinesStyle -> Directive[AbsoluteThickness[0.5], Black]," +
                "Contours -> {Automatic, 10}];\n\n";

        return plot_cmd;
    }

    private static String buildExportWolframCMD(String cmd_to_export, String filename, String extra_items_to_show){
        // Make a check here to see if the list containing the effective_nodes has the same size as the entire Node db-1
        // This means that there is no non-Effective Node. Hence, we need to exclude Plot B
        if (SimApp.effective_remoteNodes.size() == (SimApp.nodeID_to_nodeObject.size()-1)){
            return cmd_to_export + "\nExport[\"export/" + filename + "\", Show[" + extra_items_to_show + "plotA, plotC], ImageSize -> {1600, 1000}, \"CompressionLevel\" -> 0]";
        }
        else{
            return cmd_to_export + "\nExport[\"export/" + filename + "\", Show[" + extra_items_to_show + "plotA, plotB, plotC], ImageSize -> {1600, 1000}, \"CompressionLevel\" -> 0]";
        }
    }

    static double[] findBestPositionForCurrentNode(Node currentNode, int cycleCounter, int stepCounter){

        Core.getEffectiveNeighbors(currentNode);

        MathEngine.create_the_optimizers();

        // Start each thread worker or notify it to continue with the optimization (i.e. escape the waiting state)
        for (Optimizer optimizer: swarmPositioningOptimizers){
            // Reset the maxLikelihood
            bestLikelihood = Double.NEGATIVE_INFINITY; // TODO: Probably move that outside
            optimizer.start();
        }
        try {
            for (Optimizer optimizer: swarmPositioningOptimizers){
                optimizer.join();
            }
        }
        catch (InterruptedException ignored) {
            // In this case, the user has requested to stop the optimization
        }

        double[] best_params = new double[0];
        // At this point, all thread workers have finished
        for (Optimizer optimizer: swarmPositioningOptimizers){
            if (bestLikelihood < optimizer.optimal_probability){ // < optimizer.optimal_probability){
                bestLikelihood = optimizer.optimal_probability;
                //System.out.println(bestLikelihood);
                best_params = optimizer.best_params;
            }
        }

        //System.out.println(Arrays.toString(best_params));

        if (best_params.length == 0){
            SimApp.appendToTextArea("Optimization was not successful for current node.");
            return(null);
        }
        else{
            SimApp.appendToTextArea("Cycle:" + cycleCounter + " " +
                    "   Step:" + stepCounter + " " +
                    "   Node:" + currentNode.id + " " +
                    "   Pos: [x= " + SimApp.two_decimals_formatter.format(best_params[0]).replace(",", ".") +
                    ", y= " + SimApp.two_decimals_formatter.format(best_params[1]).replace(",", ".") + "]");
                    // "   Score: " + likelihood_formatter(bestLikelihood));

            // get values at optimum
            return(best_params);
        }
    }

    private static String likelihood_formatter(double likelihood){
        return Double.toString(likelihood).substring(0, 4) + "E-" + (BigDecimal.valueOf(likelihood).scale()-2);
    }
}

// Class to evaluate the Circular Distance Likelihood function
// Here, the rss is considered as the parameter
class CircularDistanceLikelihood implements MaximisationFunction{

    Node attachedNode;
    double measurement;
    double minPlotX = 0.0D;
    double maxPlotX = 0.0D;
    double minPlotY = 0.0D;
    double maxPlotY = 0.0D;

    String ProductLikelihoodComponent_WolframOBJ = null;

    public CircularDistanceLikelihood(Node attachedNode) {
        this.attachedNode = attachedNode;
    }

    // Set the Mathematica Likelihood Objects
    void updateProductLikelihoodComponentWolframOBJ() {

        // For BLE RSS
        //this.ProductLikelihoodComponent_WolframOBJ = "(25.029205084016887*E^(0.07848246*" + measurement + " -0.005271437722209233*(53.03569084 + " + measurement + " + 12.7416977*Log[(" +
        //        attachedNode.current_relative_x + " -distanceX)^2 + (" +
        //        attachedNode.current_relative_y + " -distanceY)^2])^2))*\n";
        //

        // For UWB time
        //this.ProductLikelihoodComponent_WolframOBJ =
        //        "((1 / (\\[Pi] * (1 + ((Sqrt[(" + attachedNode.current_relative_x + "-distanceX)^2 + (" + attachedNode.current_relative_y + "-distanceY)^2] - 30 *"
        //                + measurement + ") / (0.3 * " + measurement + " + 20))^2))) / (0.3 * " + measurement + " + 20))*\n";

        // For UWB time optimized
        if (SimApp.uwb_model) {
            this.ProductLikelihoodComponent_WolframOBJ = "-20 - 0.3 * " + measurement +
                    " - ((Sqrt[(" + attachedNode.current_relative_x + "-distanceX)^2 + (" + attachedNode.current_relative_y + "-distanceY)^2] - 30 * " + measurement + ")^2)/(20 + 0.3 * " + measurement + ")\n";
        }
    }

    // Evaluation function
    public double function(double[] coords){
        // Simplification BLE RSS 2
        // return 0.07848246 * measurement -0.005271437722209233*Math.pow(53.03569084+ measurement +12.7416977*Math.log(Math.sqrt(Math.pow(attachedNode.current_relative_x-coords[0], 2) + Math.pow(attachedNode.current_relative_y-coords[1], 2))), 2);

        // Original UWB
        //double loc = 30 * measurement;
        double scale = 20 + 0.3 * measurement;
        double x = Math.sqrt(Math.pow(attachedNode.current_relative_x-coords[0], 2) + Math.pow(attachedNode.current_relative_y-coords[1], 2));
        //return (1 / (Math.PI * (1 + Math.pow((x - 30 * measurement) / scale, 2)))) / scale;

        // Simplification
        return -(scale + Math.pow(x - 30 * measurement, 2) / scale);

    }

    // Method to set the parameters of the CircularDistanceLikelihood that belongs to the corresponding Node
    // This method is utilised only when the Node is among the "remote ones"
    void update_Measurement_and_ExtentReach(double measurement){
        this.measurement = measurement;

        this.minPlotX = attachedNode.current_relative_x - SimApp.max_distance;
        this.maxPlotX = attachedNode.current_relative_x + SimApp.max_distance;
        this.minPlotY = attachedNode.current_relative_y - SimApp.max_distance;
        this.maxPlotY = attachedNode.current_relative_y + SimApp.max_distance;
    }
}

class Optimizer extends Thread {

    // Class to evaluate the Position Likelihood function
    // Here, the rss is considered as the parameter
    static class PositionLikelihood implements MaximisationFunction { // MinimisationFunction {
        // Evaluation function
        public double function(double[] coordinates) {

            // simplification 2 (likelihoods are < 0, therefore, their addition is also <0)
            double total_likelihood = 0.D;

            // simplification 1
            //double total_likelihood = 1.D;

            for (int nodeID: SimApp.effective_remoteNodes){
                Node remoteNode = SimApp.nodeID_to_nodeObject.get(nodeID);
                double likelihood = remoteNode.cdl.function(coordinates);

                // simplification 2
                total_likelihood = total_likelihood + likelihood;

//                if (total_likelihood > -1){
//                    System.out.println(total_likelihood);
//                }

                // simplification 1
                // total_likelihood = total_likelihood * likelihood;

                // simplification 2
                if (total_likelihood==Double.NEGATIVE_INFINITY){ // POSITIVE_INFINITY // NEGATIVE_INFINITY){
                    // TODO: We expect maybe here for the function to crush at some point due to very small values?
                    //  Check when this happens
                    break;
                }

                // simplification 1
//                if (total_likelihood==0){
//                    break;
//                }
            }

            return total_likelihood;
        }
    }
    flanagan.math.Maximisation NodePosMax = new flanagan.math.Maximisation(); // flanagan.math.Minimisation NodePosMax = new flanagan.math.Minimisation();

    final PositionLikelihood position_likelihood;
    final long optimization_time = Integer.parseInt(SimApp.max_optimization_time_per_thread_inputTextField.getText());
    final Double initial_step_size = Double.parseDouble(SimApp.initial_step_size_inputTextField.getText());

    // simplification 1
    double optimal_probability = Double.NEGATIVE_INFINITY; // POSITIVE_INFINITY // Double.NEGATIVE_INFINITY;

    // simplification 2
    //double highest_probability = 0D;
    double[] best_params = new double[0];

    Optimizer() {
        NodePosMax.suppressNoConvergenceMessage();

        // Create instance of class holding function to be optimised
        position_likelihood = new PositionLikelihood();
    }

    public void run() {
        final long start_time = System.currentTimeMillis();
        final long stop_time = start_time + optimization_time;

        // This check can be used to set a time constraint for the optimization
        while (System.currentTimeMillis() < stop_time) {

            for (int optimization_iteration = 0; optimization_iteration< SimApp.optimization_iterations_per_thread; optimization_iteration++){

                double randomX = MapField.global_minPlotX + SimApp.random.nextDouble() * (MapField.global_maxPlotX - MapField.global_minPlotX);
                double randomY = MapField.global_minPlotY + SimApp.random.nextDouble() * (MapField.global_maxPlotY - MapField.global_minPlotY);

                //System.out.println(randomX + ", " + randomY);

                // As initial estimates use Node's current position
                //double[] start = {currentNode.current_relative_x, currentNode.current_relative_y};
                double[] start = {randomX, randomY};

                // initial step sizes
                double[] step = {initial_step_size, initial_step_size};

                // convergence tolerance
                double ftol = SimApp.ftol;

                // Nelder and Mead optimization procedure
                NodePosMax.nelderMead(position_likelihood, start, step, ftol);

                if (optimal_probability < NodePosMax.getMaximum()) { // > NodePosMax.getMinimum()) {
                    optimal_probability = NodePosMax.getMaximum(); // NodePosMax.getMinimum();
                    best_params = NodePosMax.getParamValues();

                    //System.out.println("Iteration: " + optimization_iteration + ": " + optimal_probability);

                    //if (optimization_iteration>200){
                    //    System.out.println(highest_probability + " " + Arrays.toString(NodePosMax.getParamValues()) + " {" + optimization_iteration + "}");
                    //}
                }
            }

            //System.out.println("Final: " + highest_probability + " " + Arrays.toString(best_params) + "\n");

            // Todo we need to make this optional for the user to be able to use the other mode
            break;
        }
    }
}
