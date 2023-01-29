import com.google.common.collect.Sets;
import flanagan.math.MaximisationFunction;

import java.io.*;
import java.math.RoundingMode;
import java.nio.file.FileSystems;
import java.text.DecimalFormat;
import java.util.*;

public class Ideal_RSS_Generator {

    static DecimalFormat df = new DecimalFormat("#.####");

    // Class for finding the ideal (i.e. mode) Distance between two nodes
    // given some RSS and a model
    // This function needs to be the same as the function used within the simulator!!
    static class DistanceLikelihood implements MaximisationFunction {

        private double rss = 0.0D;

        // Evaluation function
        public double function(double[] distance){
            return Math.exp(0.07848246*rss-0.005271437722209233*Math.pow(53.03569084+rss+12.7416977*Math.log(distance[0]), 2));
        }

        // Method to set rss
        void setRSS(double rss){
            this.rss = rss;
        }
    }

    // Class for finding the ideal (i.e. mode) RSS between two nodes
    // given some Distance and a model
    // This function needs to be the same as the function used within the simulator!!
    static class RSSLikelihood implements MaximisationFunction {

        private double distance = 0.0D;

        // Evaluation function
        public double function(double[] rss){
            return 0.040962797040502034*Math.exp(-0.005271437722209233*Math.pow(53.03569084+rss[0]+12.7416977*Math.log(distance), 2));
        }

        // Method to set distance
        void setDistance(double distance){
            this.distance = distance;
        }
    }

    // These parameters need to be the same as the parameters used within the simulator!!
    private static final double min_rss = -95;
    private static final double max_rss = -30;
    private static final double min_distance = 0.001;
    private static final double max_distance = 10.;

    // Data structure needs to be respected. Every Node needs to be on a separate line
    final static String sparse_raw_true_nodes_pos =
            """
            1:-6.437;-3.742
            2:-4.638;-1.842
            3:-2.892;-0.014
            4:-5.682;-4.377
            5:-3.808;-2.652
            6:-2.111;-0.75
            7:-4.852;-5.248
            8:-3.044;-3.409
            9:-1.242;-1.547
            10:-4.083;-6
            11:-2.28;-4.169
            12:-0.461;-2.288
            13:-3.273;-6.769
            14:-1.481;-4.893
            15:0.248;-3
            16:-2.443;-7.497
            17:-0.75;-5.637
            18:1.065;-3.762
            19:-1.665;-8.263
            20:0.079;-6.451
            21:1.84;-4.559
            """;

    final static String thin_raw_true_nodes_pos =
            """
            1:-6.437;-3.742
            2:-5.98;-3.257
            3:-4.638;-1.842
            4:-5.682;-4.377
            5:-5.237;-3.921
            6:-3.808;-2.652
            7:-4.852;-5.248
            8:-4.421;-4.769
            9:-3.044;-3.409
            10:-4.083;-6
            11:-3.643;-5.543
            12:-2.28;-4.169
            13:-3.273;-6.769
            14:-2.807;-6.305
            15:-1.481;-4.893
            16:-2.443;-7.497
            17:-2.014;-7.049
            18:-0.75;-5.637
            19:-1.665;-8.263
            20:-1.229;-7.788
            21:0.079;-6.451
            """;

    final static LinkedHashSet<String> swarm_combinations_for_evaluation = new LinkedHashSet<>();
    final static HashMap<String, String> deployment_TO_raw_true_nodes_pos = new HashMap<>();
    final static HashMap<String, HashMap<String, double[]>> deployment_TO_nodeID_2_double_true_node_pos = new HashMap<>();
    final static HashMap<String, HashMap<String, String>> deployment_TO_nodeID_2_str_true_node_pos = new HashMap<>();
    final static HashMap<String, LinkedHashMap<String, Double>> deployment_TO_node_pair_2_between_distance = new HashMap<>();

    final static Random random = new Random();

    public static void main(String[] argv){
        // setting seed
        long seed = 20;
        Ideal_RSS_Generator.random.setSeed(seed);

        // Update the deployment-based datastructures with the corresponding data
        Ideal_RSS_Generator.deployment_TO_raw_true_nodes_pos.put("S", Ideal_RSS_Generator.sparse_raw_true_nodes_pos);
        Ideal_RSS_Generator.deployment_TO_raw_true_nodes_pos.put("T", Ideal_RSS_Generator.thin_raw_true_nodes_pos);

        Ideal_RSS_Generator.df.setRoundingMode(RoundingMode.HALF_UP);

        // Parse the combinations
        parse_swarm_combinations();

        for (String deployment: Ideal_RSS_Generator.deployment_TO_raw_true_nodes_pos.keySet()){
            parse_node_pos(deployment);
            compute_all_cross_distances(deployment);
        }
        compute_sampled_rss_between_node_pairs();
    }

    private static void parse_swarm_combinations() {
        // Loading the file with the Swarm Combinations to be evaluated
        System.out.println("Loading Combinations");
        final String combinations_filename = "Node_Samples//combos.txt";
        final String combinations_filename_path = FileSystems.getDefault().getPath("").toAbsolutePath() + "/src/" + combinations_filename;

        try {
            // Open the file
            FileInputStream fstream = new FileInputStream(combinations_filename_path);
            BufferedReader br = new BufferedReader(new InputStreamReader(fstream));

            String strLine;

            //Read File Line By Line
            while ((strLine = br.readLine()) != null)   {
                // Parse the parameters for this case

                String[] parameters = strLine.replace("\n", "").replace("(", "").replace(")", "").split(" ");
                String deployment = parameters[0];

                // Get the nodeIDs
                String[] node_ids_str = parameters[1].split(",");
                Set<String> node_ids_set = new HashSet<>(Arrays.asList(node_ids_str));
                int[] node_ids_int = new int[node_ids_str.length];
                int index = 0;
                for (String node: node_ids_str){
                    node_ids_int[index] = Integer.parseInt(node);
                    index++;
                }
                int samples = Integer.parseInt(parameters[2]);

                // Create the evaluation id
                String evaluation_id = deployment + "_" + parameters[1] + "_" + samples;

                System.out.println("Folder name: " + evaluation_id);

                // Put this combo into the general data structure
                Ideal_RSS_Generator.swarm_combinations_for_evaluation.add(evaluation_id);

                System.out.println(node_ids_set);
                Set<Set<String>> combinations = Sets.combinations(node_ids_set, 2);

                /*
                for (Set<String> pair: combinations){
                    System.out.println(pair);
                }
                */

                System.out.println(Arrays.toString(parameters) + "\n");

            }

            //Close the input stream
            fstream.close();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void compute_all_cross_distances(String deployment) {

        LinkedHashMap<String, Double> node_pair_2_between_distance = new LinkedHashMap<>();

        for (Map.Entry<String, double[]> current_NodeEntry: Ideal_RSS_Generator.deployment_TO_nodeID_2_double_true_node_pos.get(deployment).entrySet()) {
            String current_NodeID = current_NodeEntry.getKey();
            //System.out.println(current_NodeID);
            double[] current_NodePOS = current_NodeEntry.getValue();

            for (Map.Entry<String, double[]> remote_NodeEntry: Ideal_RSS_Generator.deployment_TO_nodeID_2_double_true_node_pos.get(deployment).entrySet()) {
                String remote_NodeID = remote_NodeEntry.getKey();
                //System.out.println(remote_NodeID);
                double[] remote_NodePOS = remote_NodeEntry.getValue();

                // Avoid considering the same Node
                if(!current_NodeID.equals(remote_NodeID)){

                    String current_node_combo = current_NodeID + ";" + remote_NodeID;
                    String remote_node_combo = remote_NodeID + ";" + current_NodeID;

                    // Calculate the distance between the two Nodes in cm
                    double distance_between_Nodes = Math.sqrt(Math.pow(remote_NodePOS[0] - current_NodePOS[0], 2) + Math.pow(remote_NodePOS[1] - current_NodePOS[1], 2))*100;

                    node_pair_2_between_distance.put(current_node_combo, distance_between_Nodes);
                    node_pair_2_between_distance.put(remote_node_combo, distance_between_Nodes);
                }
            }
        }

        Ideal_RSS_Generator.deployment_TO_node_pair_2_between_distance.put(deployment, node_pair_2_between_distance);
    }

    private static void compute_sampled_rss_between_node_pairs() {
        System.out.println("Computing the RSS and Distance samples");

        try {
            // For every different swarm (having different sample size)
            for (String swarm_combination_id: Ideal_RSS_Generator.swarm_combinations_for_evaluation) {

                String[] parameters = swarm_combination_id.split("_");
                String deployment = parameters[0];

                // Get the nodeIDs
                String[] node_ids_str = parameters[1].split(",");
                Set<String> node_ids_set = new HashSet<>(Arrays.asList(node_ids_str));
                System.out.println(swarm_combination_id);

                int[] node_ids_int = new int[node_ids_str.length];
                int index = 0;
                for (String node: node_ids_str){
                    node_ids_int[index] = Integer.parseInt(node);
                    index++;
                }
                int samples = Integer.parseInt(parameters[2]);

                FileWriter rss_file_Writer = new FileWriter("D:\\GIT_Projects\\ARLCL-Sim\\src\\Node_Samples\\" + swarm_combination_id + ".rss");
                rss_file_Writer.write("#POSITIONS GROUND TRUTH#\n");

                // Log the Ground Truth
                for (String nodeID: node_ids_set){
                    rss_file_Writer.write(nodeID + ":" + Ideal_RSS_Generator.deployment_TO_nodeID_2_str_true_node_pos.get(deployment).get(nodeID) + "\n");
                }

                for (int monte_carlo_iteration=0; monte_carlo_iteration < 100+1; monte_carlo_iteration++) {

                    rss_file_Writer.write("\n#RSS_" + monte_carlo_iteration + "#\n");

                    HashMap<String, String> already_sampled_cases = new HashMap<>();

                    // Iterate over all possible precalculated pairs
                    for (Map.Entry<String, Double> node_pair : Ideal_RSS_Generator.deployment_TO_node_pair_2_between_distance.get(deployment).entrySet()) {

                        String current_node_combo = node_pair.getKey();
                        double distance_between_node_pair = node_pair.getValue();

                        String[] nodes = current_node_combo.split(";");
                        String nodeA = nodes[0];
                        String nodeB = nodes[1];

                        // Filter the pairs that are not contained in current combination set
                        if (node_ids_set.contains(nodeA) && node_ids_set.contains(nodeB)){

                            // Construct the ID of the reversed pair
                            String remote_node_combo = nodeB + ";" + nodeA;

                            double mean_rss_from_sample;

                            if (!already_sampled_cases.containsKey(current_node_combo)) {
                                if (monte_carlo_iteration == 0) {
                                    mean_rss_from_sample = find_most_probable_rss_for_distance(distance_between_node_pair / 100)[1];
                                } else {
                                    // Draw n samples from distribution given distance
                                    double[] rss_samples = sample_from_distro_given_distance(samples, distance_between_node_pair / 100);
                                    mean_rss_from_sample = get_mean(rss_samples);
                                }

                                // Calculate for this sampled rss, the most probable distance in cm based on our model
                                // This shall be used for the evaluation
                                double most_probable_distance_for_rss = find_most_probable_distance_for_rss(mean_rss_from_sample)[1];

                                String formatted_double = df.format(most_probable_distance_for_rss);
                                String current_printed_entry = current_node_combo + ":" + mean_rss_from_sample + "&" + formatted_double + "\n";
                                String remote_printed_entry = remote_node_combo + ":" + mean_rss_from_sample + "&" + formatted_double + "\n";

                                already_sampled_cases.put(remote_node_combo, remote_printed_entry);
                                rss_file_Writer.write(current_printed_entry);
                            } else {
                                rss_file_Writer.write(already_sampled_cases.get(current_node_combo));
                            }
                        }
                    }
                }
                rss_file_Writer.close();
            }
        } catch(Exception e){
            e.printStackTrace();
        }
    }

    private static double[] sample_from_distro_given_distance(int samples, double distance_between_node_pair) {
        double[] temp_samples = new double[samples*2];

        double norm_dist_mu = -53.03569084-12.7416977*Math.log(distance_between_node_pair);
        double var = 9.73913671;

        // We multiply the samples to assign a given sample size to each one of the two members of the pair
        // This information will be averaged later
        for (int sample_id = 0; sample_id<samples*2; sample_id++){
            temp_samples[sample_id] = norm_dist_mu + Ideal_RSS_Generator.random.nextGaussian()*var;
        }

        return temp_samples;
    }

    private static double get_mean(double[] m) {
        double sum = 0;
        for (double v: m) {
            sum += v;
        }
        return sum / m.length;
    }

    static private void parse_node_pos(String deployment){
        // Loading the database
        System.out.println("Parsing Nodes");

        // Create a temporal NodeID to NodePOS mapping for current deployment
        HashMap<String, double[]> nodeID_to_double_true_node_pos = new HashMap<>();
        HashMap<String, String> nodeID_to_str_true_node_pos = new HashMap<>();

        // Prepare the parsing for the Sparse Deployment
        String[] raw_true_nodes_pos = Ideal_RSS_Generator.deployment_TO_raw_true_nodes_pos.get(deployment).split("\n");

        for (String raw_true_node_pos: raw_true_nodes_pos){
            String[] str_node_parts = raw_true_node_pos.split(":");

            String nodeID = str_node_parts[0];
            String[] str_nodePOS = str_node_parts[1].split(";");
            double nodePOSx = Double.parseDouble(str_nodePOS[0]);
            double nodePOSy = Double.parseDouble(str_nodePOS[1]);

            nodeID_to_double_true_node_pos.put(nodeID, new double[]{nodePOSx, nodePOSy});
            nodeID_to_str_true_node_pos.put(nodeID, str_node_parts[1]);

            System.out.println("Deployment: " + deployment + " NodeID: " + nodeID + " " + Arrays.toString(nodeID_to_double_true_node_pos.get(nodeID)));
        }

        Ideal_RSS_Generator.deployment_TO_nodeID_2_double_true_node_pos.put(deployment, nodeID_to_double_true_node_pos);
        Ideal_RSS_Generator.deployment_TO_nodeID_2_str_true_node_pos.put(deployment, nodeID_to_str_true_node_pos);

    }

    private static double[] find_most_probable_distance_for_rss(double rss){
        //Create instance of Maximisation
        flanagan.math.Maximisation max = new flanagan.math.Maximisation();

        // Create instance of class holding function to be minimised
        DistanceLikelihood funct = new DistanceLikelihood();

        // Set value of the constant RSS
        funct.setRSS(rss);

        // initial estimates
        double[] start = {0.0D};

        // initial step sizes
        double[] step = {0.01D};

        // convergence tolerance
        double ftol = 1e-323;

        max.suppressNoConvergenceMessage();

        // Set the distance constraints
        max.addConstraint(0, -1, min_distance);
        max.addConstraint(0, 1, max_distance);

        // Nelder and Mead optimization procedure
        max.nelderMead(funct, start, step, ftol);

        // get values at maximum
        double[] param = max.getParamValues();

        // Print results to a text file
        //max.print("MaximOutput.txt");

        double[] values = new double[]{max.getMaximum(), param[0]};

        // Output the results to screen
        //System.out.println("Maximum = " + values[0]);
        //System.out.println("Value of Distance at the maximum = " + values[1]);

        return(values);
    }

    private static double[] find_most_probable_rss_for_distance(double distance){
        //Create instance of Maximisation
        flanagan.math.Maximisation max = new flanagan.math.Maximisation();

        // Create instance of class holding function to be minimised
        RSSLikelihood funct = new RSSLikelihood();

        // Set value of the constant RSS
        funct.setDistance(distance);

        // initial estimates
        double[] start = {0.0D};

        // initial step sizes
        double[] step = {0.0001D};

        // convergence tolerance
        double ftol = 1e-323;

        max.suppressNoConvergenceMessage();

        // Set the distance constraints
        max.addConstraint(0, -1, min_rss);
        max.addConstraint(0, 1, max_rss);

        // Nelder and Mead optimization procedure
        max.nelderMead(funct, start, step, ftol);

        // get values at maximum
        double[] param = max.getParamValues();

        // Print results to a text file
        //max.print("MaximOutput.txt");

        double[] values = new double[]{max.getMaximum(), param[0]};

        // Output the results to screen
        //System.out.println("Distance: " + distance + " RSS: " + values[1] + " Likelihood: " + values[0]);

        return(values);
    }

    public static String removeLastChar(String s) {
        return Optional.ofNullable(s)
                .filter(str -> str.length() != 0)
                .map(str -> str.substring(0, str.length() - 1))
                .orElse(s);
    }
}
