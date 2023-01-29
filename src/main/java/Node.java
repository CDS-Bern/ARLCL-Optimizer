import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.util.HashMap;

public class Node {

    static DecimalFormat df = new DecimalFormat("#.####");
    int id;
    CircularDistanceLikelihood cdl;

    double true_relative_x;
    double true_relative_y;
    double current_relative_x;
    double current_relative_y;

    // Create a RemoteNodeID-TrueDistance mapping
    HashMap<Integer, Double> true_distance_to_node = new HashMap<>();

    // Create a RemoteNodeID-CurrentDistance mapping
    HashMap<Integer, Double> current_distance_to_node = new HashMap<>();

    // Create a RemoteNodeID-RSS mapping
    HashMap<Integer, Double> measurement_from_node = new HashMap<>();

    public Node(int id, double true_relative_x, double true_relative_y) {

        Node.df.setRoundingMode(RoundingMode.HALF_UP);

        this.id = id;
        this.true_relative_x = true_relative_x;
        this.true_relative_y = true_relative_y;
        set_initial_random_relative_positions();

        // Attach the CircularDistanceLikelihood function to this Node
        // This function will be updated throughout every positioning iteration
        cdl = new CircularDistanceLikelihood(this);
    }

    public void set_initial_random_relative_positions(){
        // Set the extent for the initial random positioning
        final double initial_global_minPlotX = -SimApp.max_distance;
        final double initial_global_maxPlotX = SimApp.max_distance;
        final double initial_global_minPlotY = -SimApp.max_distance;
        final double initial_global_maxPlotY = SimApp.max_distance;

        // Use this to set position at 0
        update_CurrentNodePos(0, 0);

//        double randomX = initial_global_minPlotX + SimApp.random.nextDouble() * (initial_global_maxPlotX - initial_global_minPlotX);
//        double randomY = initial_global_minPlotY + SimApp.random.nextDouble() * (initial_global_maxPlotY - initial_global_minPlotY);
//        update_CurrentNodePos(randomX, randomY);
    }

    public double getDistanceToNode(Node NodeB, boolean use_true_position){
        //System.out.println("Calculating Distance between node: " + id + " and node: " + NodeB.id);
        // Calculate the distance based on the pythagorean theorem and update the corresponding property
        if (use_true_position){
            return Math.sqrt(Math.pow(NodeB.current_relative_x - current_relative_x, 2) + Math.pow(NodeB.current_relative_y - current_relative_y, 2));
        }
        else {
            return Math.sqrt(Math.pow(NodeB.true_relative_x - true_relative_x, 2) + Math.pow(NodeB.true_relative_y - true_relative_y, 2));
        }
    }

    public String get_report(){
        return "Node: " + id + ", " +
                "True Coords: [" + true_relative_x + ", " + true_relative_y + "], " +
                "Init Coords: [" + df.format(current_relative_x) + ", " + df.format(current_relative_y) + "]";
    }

    public void update_CurrentNodePos(double x, double y){
        // First update the Java object
        current_relative_x = x;
        current_relative_y = y;
    }
}