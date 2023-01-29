import org.jzy3d.maths.Range;

public class MapField {

    static double global_minPlotX = 0.0D;
    static double global_maxPlotX = 0.0D;
    static double global_minPlotY = 0.0D;
    static double global_maxPlotY = 0.0D;

    public MapField() {
    }

    public static void updateMapExtent() {

        //Set a flag that will show the first iteration
        boolean first_check = true;

        // Loop throughout all Nodes
        for (Node node: SimApp.nodeID_to_nodeObject.values()){

            if (first_check){
                first_check = false;

                // Reset the extent to the position of current Node
                global_minPlotX = node.current_relative_x;
                global_maxPlotX = node.current_relative_x;
                global_minPlotY = node.current_relative_y;
                global_maxPlotY = node.current_relative_y;
            }
            else {
                // Set the extents
                if (global_minPlotX > node.current_relative_x){ //todo change this into maps var
                    global_minPlotX = node.current_relative_x;
                }
                if (global_maxPlotX < node.current_relative_x){
                    global_maxPlotX = node.current_relative_x;
                }
                if (global_minPlotY > node.current_relative_y){
                    global_minPlotY = node.current_relative_y;
                }
                if (global_maxPlotY < node.current_relative_y){
                    global_maxPlotY = node.current_relative_y;
                }

                // Since we want the map to have 1:1 ratio, we need to adjust the above extent
                // First we need to calculate the range

                double x_range = global_maxPlotX - global_minPlotX;
                double y_range = global_maxPlotY - global_minPlotY;

                // Calculate the extent difference and split it to add it evenly to the other (shorter) range
                double split_extent_diff = (x_range-y_range)/2.;

                if (split_extent_diff>0){
                    // A positive half difference means that x range is bigger than the y range
                    // Therefore adjust the y range
                    global_minPlotY = global_minPlotY-split_extent_diff;
                    global_maxPlotY = global_maxPlotY+split_extent_diff;
                }
                else{
                    // We change the signs below because the range difference in this case is negative
                    global_minPlotX = global_minPlotX+split_extent_diff;
                    global_maxPlotX = global_maxPlotX-split_extent_diff;
                }
            }
        }

        // Update finally the extent by 100 on each side to ensure everything is visible
        global_minPlotX = global_minPlotX - 1;
        global_maxPlotX = global_maxPlotX + 1;
        global_minPlotY = global_minPlotY - 1;
        global_maxPlotY = global_maxPlotY + 1;

    }

    public static Range getMinMaxRange() {
        return new Range(
                Math.min(global_minPlotX, global_minPlotY),
                Math.max(global_maxPlotX, global_maxPlotY)
        );
    }

    public static Range getXRange() {
        return new Range(global_minPlotX, global_maxPlotX);
    }

    public static Range getYRange() {
        return new Range(global_minPlotY, global_maxPlotY);
    }
}
