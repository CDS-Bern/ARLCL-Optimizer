import org.jzy3d.chart.ContourChart;
import org.jzy3d.plot3d.rendering.canvas.Quality;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.AffineTransform;
import java.awt.image.AffineTransformOp;
import java.awt.image.BufferedImage;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static java.util.concurrent.TimeUnit.SECONDS;

public class SimApp extends Frame {

    static long min_cycle_time;
    static long startTime;
    static Frame app;
    static Container chart_component_container_even = new Container();
    static Container chart_component_container_odd = new Container();
    static int chart_plot_size;

    // This data structure will store each evaluation case
    static String[] eval_scenario;
    static boolean headless_mode;
    static boolean results_per_step;
    static boolean results_per_cycle;
    static boolean ble_model;
    static boolean uwb_model;
    static boolean export_ProductLikelihood_WolframPlot;
    static boolean stop_optimization;
    static boolean resume_flag;
    static int plotResolution;

    // Optimization's Parameters
    static int min_effective_measurement;
    static int kNearestNeighbours_for_BeliefsStrength;
    static int initial_Map_Extend;
    static int threads;
    static int optimization_iterations_per_thread;
    static int max_optimization_time_per_thread;
    static double ftol;
    static long seed;
    static int step_size;
    static int optimization_cycles;
    static int ending_eval_iteration;
    static int evaluated_iteration;
    static String clean_evaluated_scenario_name;
    static String input_file_path;
    static String input_file_extension;
    static String outpath_results_folder_path;
    static String output_iteration_results_folder_path;
    // Create a map having <Node IDs, Node Objects> as <key, value> pairs
    static LinkedHashMap<Integer, Node> nodeID_to_nodeObject;
    static List<Integer> OrderedByBeliefsStrength_NodeIDs;
    static List<Integer> temp_OrderedRemoteNodeIDs;
    static List<Integer> OrderedByLastCycleOrientation_NodeIDs;

    static ArrayList<Integer> effective_remoteNodes;
    static DecimalFormat two_decimals_formatter = new DecimalFormat("#.##");
    static WnAdapter window_adapter;
    static String previous_valid_project_name = "";
    static String previous_valid_plot_resolution = "";
    static Component chart_component_even;
    static Component chart_component_odd;
    static Rectangle chart_components_bounds;
    static ContourChart chart_even;
    static ContourChart chart_odd;
    static CustomTextArea outputTerminal;
    static CustomTextArea productLikelihood_WolframPlot_LabelArea;
    static CustomTextArea rangingModel_LabelArea;
    static CustomTextArea resultsPer_LabelArea;
    static CustomTextArea optimizationParameters_LabelArea;
    static CustomTextArea loaded_db_name_LabelArea;
    static CustomTextArea kNearestNeighbours_for_BeliefsStrength_LabelArea;
    static CustomTextArea max_optimization_time_per_thread_LabelArea;
    static CustomTextArea optimization_cycles_LabelArea;
    static CustomTextArea initial_step_size_LabelArea;
    static CustomTextArea min_effective_measurement_value_LabelArea;
    static CustomTextArea plotResolution_LabelArea;
    static CustomTextArea ftol_LabelArea;
    static CustomTextArea optimization_iterations_per_thread_LabelArea;
    static CustomTextArea initial_Map_Extend_LabelArea;
    static CustomTextArea seed_LabelArea;
    static CustomTextArea evaluated_iteration_LabelArea;
    static CustomTextArea threads_LabelArea;
    static CustomTextField projectName_inputTextField;
    static CustomTextField kNearestNeighbours_for_BeliefsStrength_inputTextField;
    static CustomTextField max_optimization_time_per_thread_inputTextField;
    static CustomTextField optimization_cycles_inputTextField;
    static CustomTextField initial_step_size_inputTextField;
    static CustomTextField min_effective_measurement_inputTextField;
    static CustomTextField plotResolution_inputTextField;
    static CustomTextField ftol_inputTextField;
    static CustomTextField optimization_iterations_per_thread_inputTextField;
    static CustomTextField initial_Map_Extend_inputTextField;
    static CustomTextField seed_inputTextField;
    static CustomTextField evaluated_iteration_inputTextField;
    static CustomTextField threads_inputTextField;
    static CustomButton openDB_Btn;
    static CustomButton clearTerminalBtn;
    static CustomCheckbox auto_resumer_btn;
    static CustomToggleButton go_Toggle_btn;
    static CustomButton resume_btn;
    static CustomCheckbox results_per_step_btn;
    static CustomCheckbox results_per_cycle_btn;
    static CustomCheckbox ble_model_btn;
    static CustomCheckbox uwb_model_btn;
    static CustomCheckbox export_ProductLikelihood_WolframPlot_function_btn;
    static Thread t1;
    static Thread controller_thread;
    static SimpleDateFormat day_formatter = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss");
    static ScheduledExecutorService scheduler;
    static ScheduledFuture<?> scheduled_auto_resumer;
    static int cycleCounter;
    static int stepCounter;

    final static Random random = new Random();

    public static void run(String[] argv){
        SimApp.headless_mode = argv.length != 0;

        if (headless_mode){
            System.out.println("Executing ARLCL Optimizer in Headless mode");

            // The argument should look like that

            System.out.println("Params: " + Arrays.toString(argv));
            // Get each argument
            HashMap<String, String> str_arguments = new HashMap<>();

            String input_rss_folder_path = null;
            String eval_scenarios_path = null;
            int evaluation_id = 0;

            // This section is for parsing the arguments when these come with the equality sign
            try {
                for (String argument : argv) {
                    String[] key_value_pair = argument.split("=");
                    str_arguments.put(key_value_pair[0], key_value_pair[1]);
                }

                // Set the seed
                SimApp.seed = Long.parseLong(str_arguments.get("seed"));
                SimApp.random.setSeed(SimApp.seed);

                SimApp.ftol = Double.parseDouble(str_arguments.get("ftol"));
                SimApp.outpath_results_folder_path = str_arguments.get("out_path");
                input_rss_folder_path = str_arguments.get("rss_db_path");
                eval_scenarios_path = str_arguments.get("scenarios_path");
                evaluation_id = Integer.parseInt(str_arguments.get("eval_id"));
                SimApp.ending_eval_iteration = Integer.parseInt(str_arguments.get("end_iter"));
                SimApp.optimization_iterations_per_thread = Integer.parseInt(str_arguments.get("opt_iter"));
                SimApp.max_optimization_time_per_thread = Integer.parseInt(str_arguments.get("max_opt_time"));
                SimApp.threads = Integer.parseInt(str_arguments.get("threads"));
                SimApp.optimization_cycles = Integer.parseInt(str_arguments.get("cycles"));
                SimApp.kNearestNeighbours_for_BeliefsStrength = Integer.parseInt(str_arguments.get("k_Beliefs"));
                SimApp.min_effective_measurement = Integer.parseInt(str_arguments.get("min_effect"));
                SimApp.plotResolution = Integer.parseInt(str_arguments.get("plot_res"));
                SimApp.results_per_step = false;
                SimApp.results_per_cycle = true;
            }
            catch (Exception e) {
                e.printStackTrace();
                System.out.println("Zip exists already. Exiting");
                // At this point we can close the program
                System.exit(0);
            }

            // Initiate the iteration index
            SimApp.evaluated_iteration = 0;

            // Parse the evaluation scenarios from the combos file
            eval_scenario = parseEvalScenarios(evaluation_id, eval_scenarios_path);

            // This section is for setting our hardcoded minimal arguments
            String deployment_type = eval_scenario[0];
            String swarmIDs = eval_scenario[1];
            String sample_size = eval_scenario[2];

            SimApp.clean_evaluated_scenario_name = deployment_type + "_" + swarmIDs + "_" + sample_size;
            SimApp.input_file_path = input_rss_folder_path + SimApp.clean_evaluated_scenario_name + ".smpl";

            String zip_name = SimApp.outpath_results_folder_path + SimApp.clean_evaluated_scenario_name + ".zip";
            // Check to see if there is any .zip file so that we can cancel the process completely
            File zip_file = new File(zip_name);
            if (zip_file.exists()){
                // At this point we can close the program
                System.exit(0);
            }

            executeHeadlessOptimizationJobs();
        }
        else {
            System.out.println("Executing ARLCL Optimizer in GUI mode");

            // We ensure first that the required wolfram math executable is available right next to the optimizer's Jar
            try {
                app = new SimApp();
            } catch (Exception e) {
                System.out.println("SimApp failure");
                throw new RuntimeException(e);
            }
        }
    }

    private static void executeHeadlessOptimizationJobs() {
        // Execute the following simulation setup as many times as the user has selected
        while (SimApp.evaluated_iteration < SimApp.ending_eval_iteration + 1){
            try {
                resetDataStructures();
                headlessInit();
                Core.init();
                System.out.println("Start Optimizing");
                SimApp.go_Toggle_btn.setClicked(true);
            } catch (Exception e) {
                e.printStackTrace();
                // At this point we can close the program
                System.exit(0);
            }

            // Wait for almost "infinite" amount of time for the auto resume scheduler to finish.
            // Then we shall continue with the next iteration
            try {
                scheduler.awaitTermination(10000000, SECONDS); // TODO: Hardcoded to be changed
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            // Increment one step and continue
            SimApp.evaluated_iteration++;
        }

        System.out.println("Optimization finished");

        // We now zip the folder and wipe everything
        try {
            System.out.println("Storing results");
            SimApp.storeResults();

            File directoryToBeDeleted = new File(SimApp.outpath_results_folder_path + SimApp.clean_evaluated_scenario_name);
            deleteDirectory(directoryToBeDeleted);

        } catch (Exception e) {
            e.printStackTrace();
        }
        System.out.println("Proper exit");
        // At this point we can close the program
        System.exit(0);
    }

    private static void zipFile(File fileToZip, String fileName, ZipOutputStream zipOut) throws IOException {
        if (fileToZip.isHidden()) {
            return;
        }
        if (fileToZip.isDirectory()) {
            if (fileName.endsWith("/")) {
                zipOut.putNextEntry(new ZipEntry(fileName));
                zipOut.closeEntry();
            } else {
                zipOut.putNextEntry(new ZipEntry(fileName + "/"));
                zipOut.closeEntry();
            }
            File[] children = fileToZip.listFiles();
            for (File childFile : children) {
                zipFile(childFile, fileName + "/" + childFile.getName(), zipOut);
            }
            return;
        }
        FileInputStream fis = new FileInputStream(fileToZip);
        ZipEntry zipEntry = new ZipEntry(fileName);
        zipOut.putNextEntry(zipEntry);
        byte[] bytes = new byte[1024];
        int length;
        while ((length = fis.read(bytes)) >= 0) {
            zipOut.write(bytes, 0, length);
        }
        fis.close();
    }

    static void storeResults() throws IOException {
        System.out.println(SimApp.outpath_results_folder_path + SimApp.clean_evaluated_scenario_name);
        String sourceFile = SimApp.outpath_results_folder_path + SimApp.clean_evaluated_scenario_name;
        FileOutputStream fos = new FileOutputStream(sourceFile+".zip");
        ZipOutputStream zipOut = new ZipOutputStream(fos);
        File fileToZip = new File(sourceFile);

        zipFile(fileToZip, fileToZip.getName(), zipOut);
        zipOut.close();
        fos.close();
    }

    private static String[] parseEvalScenarios(long lines_to_skip, String eval_scenarios_path) {
        String[] temp_args = null;
        try {
            Path in_path = Paths.get(eval_scenarios_path);
            Stream<String> file_stream = Files.lines(in_path);

            String strLine = file_stream.skip(lines_to_skip).findFirst().get();
            System.out.println("Entry at line " + lines_to_skip + ": " + strLine);
            temp_args = new String[3];

            String[] split_array = strLine.split(" ");

            temp_args[0] = split_array[0];
            temp_args[1] = split_array[1].replace("(", "").replace(")", "");
            temp_args[2] = split_array[2];

        } catch (IOException e) {
            e.printStackTrace();
        }

        return temp_args;
    }

    private static void resetDataStructures() {
        System.out.println("Preparing data structures");
        SimApp.min_cycle_time = Long.MAX_VALUE;
        SimApp.cycleCounter = 0;
        SimApp.stepCounter = 0;

        SimApp.nodeID_to_nodeObject = new LinkedHashMap<>();
        SimApp.OrderedByBeliefsStrength_NodeIDs = new ArrayList<>();
        SimApp.temp_OrderedRemoteNodeIDs = new ArrayList<>();
        SimApp.OrderedByLastCycleOrientation_NodeIDs = new ArrayList<>();

        SimApp.stop_optimization = false;
    }

    public SimApp() throws Exception {
        setLayout(null);
        setTitle("Swarm Positioning");

        final int window_width = 1600;
        final int window_height = 1000;
        final int bar_height = 32;
        final int settings_panel_height = 430;

        final int tiny_gap = 5;
        final int medium_gap = 20;

        final int small_text_height = 25;
        final int medium_text_height = 40;

        chart_plot_size = window_height-bar_height-medium_text_height-1;

        final int c1_x = chart_plot_size + 2;
        final int c1_content_width = 120;
        final int c2_x = c1_x + c1_content_width + tiny_gap;
        final int c2_content_width = 120;
        final int c3_x = c2_x + c2_content_width + tiny_gap;
        final int c4_content_width = 70;
        final int c3_content_width = window_width - c3_x - c4_content_width - medium_gap;
        final int c4_x = c3_x + c3_content_width;

        final int r2_y = bar_height + window_height - settings_panel_height + small_text_height + tiny_gap;
        final int r2_height = 80;

        setEmptyChartComponents();

        SimApp.chart_component_odd.setBounds(0, 0, chart_plot_size, chart_plot_size);
        SimApp.chart_components_bounds = SimApp.chart_component_odd.getBounds(); // Bounds to be used for all future charts
        SimApp.chart_component_even.setBounds(SimApp.chart_components_bounds);

        SimApp.chart_component_container_odd.setBounds(0, bar_height, chart_plot_size, chart_plot_size);
        SimApp.chart_component_container_even.setBounds(0, bar_height, chart_plot_size, chart_plot_size);

        SimApp.chart_component_container_odd.add(SimApp.chart_component_odd, 0);
        SimApp.chart_component_container_even.add(SimApp.chart_component_even, 0);

        add(SimApp.chart_component_container_odd, BorderLayout.CENTER);
        add(SimApp.chart_component_container_even, BorderLayout.CENTER, 0);

        SimApp.outputTerminal = new CustomTextArea("",2,40, TextArea.SCROLLBARS_VERTICAL_ONLY);

        SimApp.outputTerminal.setEditable(false);
        SimApp.outputTerminal.setBounds(c1_x, bar_height,window_width-c1_x-tiny_gap,window_height-settings_panel_height);
        add((TextArea) SimApp.outputTerminal.getTextArea());

        SimApp.clearTerminalBtn = new CustomButton("clear");
        SimApp.clearTerminalBtn.addActionListener(new clearTerminalBtnAdapter());
        SimApp.clearTerminalBtn.setBounds(c4_x,bar_height + window_height - settings_panel_height,40,18);
        add((Button) SimApp.clearTerminalBtn.getButton());


        int openDB_Btn_width = 100;

        SimApp.openDB_Btn = new CustomButton("DB load");
        SimApp.openDB_Btn.addActionListener(new openDBBtnAdapter());
        SimApp.openDB_Btn.setBounds(0, window_height-medium_text_height, openDB_Btn_width,small_text_height+tiny_gap);
        add((Button) SimApp.openDB_Btn.getButton());

        SimApp.loaded_db_name_LabelArea = new CustomTextArea("",1,1, TextArea.SCROLLBARS_NONE);
        SimApp.loaded_db_name_LabelArea.setFont(new Font("Arial", Font.ITALIC, 13));
        SimApp.loaded_db_name_LabelArea.setBackground(Color.black);
        SimApp.loaded_db_name_LabelArea.setEnabled(true);
        SimApp.loaded_db_name_LabelArea.setFocusable(false);
        SimApp.loaded_db_name_LabelArea.setEditable(false);
        SimApp.loaded_db_name_LabelArea.setBounds(openDB_Btn_width, window_height-medium_text_height,window_width - openDB_Btn_width,small_text_height+tiny_gap);
        add((TextArea) SimApp.loaded_db_name_LabelArea.getTextArea());



        // This Section is for the "Results per:"
        CheckboxGroup plot_export_group = new CheckboxGroup();
        SimApp.results_per_step_btn = new CustomCheckbox("Step", false, plot_export_group);
        SimApp.results_per_step_btn.setBounds(c1_x + tiny_gap, r2_y + small_text_height, c1_content_width-medium_gap, small_text_height);
        add((Checkbox) SimApp.results_per_step_btn.getCheckbox());

        SimApp.results_per_cycle_btn = new CustomCheckbox("Cycle", true, plot_export_group);
        SimApp.results_per_cycle_btn.setBounds(c1_x + tiny_gap, r2_y + 2 * small_text_height, c1_content_width-medium_gap, small_text_height);
        add((Checkbox) SimApp.results_per_cycle_btn.getCheckbox());

        SimApp.resultsPer_LabelArea = new CustomTextArea("Results per:",2,1, TextArea.SCROLLBARS_NONE);
        SimApp.resultsPer_LabelArea.setBackground(Color.lightGray);
        SimApp.resultsPer_LabelArea.setFont(new Font("Arial", Font.BOLD, 13));
        SimApp.resultsPer_LabelArea.setEnabled(true);
        SimApp.resultsPer_LabelArea.setFocusable(false);
        SimApp.resultsPer_LabelArea.setEditable(false);
        SimApp.resultsPer_LabelArea.setBounds(c1_x, r2_y, c1_content_width, r2_height);
        add((TextArea) SimApp.resultsPer_LabelArea.getTextArea());



        // This Section is for the selection of the Ranging Model
        CheckboxGroup ranging_model_group = new CheckboxGroup();
        SimApp.ble_model_btn = new CustomCheckbox("BLE [dBm]", true, ranging_model_group);
        SimApp.ble_model_btn.setBounds(c2_x + tiny_gap, r2_y + small_text_height, 70, small_text_height);
        SimApp.ble_model_btn.addItemListener(new resetOptimizationParametersPanelAdapter());
        add((Checkbox) SimApp.ble_model_btn.getCheckbox());

        SimApp.uwb_model_btn = new CustomCheckbox("UWB [t]", false, ranging_model_group);
        SimApp.uwb_model_btn.setBounds(c2_x + tiny_gap, r2_y + 2 * small_text_height, 70, small_text_height);
        SimApp.uwb_model_btn.addItemListener(new resetOptimizationParametersPanelAdapter());
        add((Checkbox) SimApp.uwb_model_btn.getCheckbox());

        SimApp.rangingModel_LabelArea = new CustomTextArea("Rang. Model:",2,1, TextArea.SCROLLBARS_NONE);
        SimApp.rangingModel_LabelArea.setBackground(Color.lightGray);
        SimApp.rangingModel_LabelArea.setFont(new Font("Arial", Font.BOLD, 13));
        SimApp.rangingModel_LabelArea.setEnabled(true);
        SimApp.rangingModel_LabelArea.setFocusable(false);
        SimApp.rangingModel_LabelArea.setEditable(false);
        SimApp.rangingModel_LabelArea.setBounds(c2_x, r2_y, c1_content_width, r2_height);
        add((TextArea) SimApp.rangingModel_LabelArea.getTextArea());



        // This Section is for the Plot Export Properties
        final int export_ProductLikelihood_Label_y = r2_y + r2_height + tiny_gap;
        SimApp.plotResolution_LabelArea = new CustomTextArea("Contours [0,+]:",1,1, TextArea.SCROLLBARS_NONE);
        SimApp.plotResolution_LabelArea.setBounds(c2_x, export_ProductLikelihood_Label_y, c2_content_width, small_text_height);
        SimApp.plotResolution_LabelArea.setBackground(Color.lightGray);
        SimApp.plotResolution_LabelArea.setEnabled(true);
        SimApp.plotResolution_LabelArea.setFocusable(false);
        add((TextArea) SimApp.plotResolution_LabelArea.getTextArea());

        final int plotResolution_inputTextArea_y = export_ProductLikelihood_Label_y + small_text_height;
        SimApp.plotResolution_inputTextField = new CustomTextField("30");
        SimApp.plotResolution_inputTextField.setBounds(c2_x, plotResolution_inputTextArea_y, c2_content_width, small_text_height);
        SimApp.plotResolution_inputTextField.addTextListener(
                new integerGreaterThanBoundEnsurer(SimApp.plotResolution_inputTextField, 0));
        add((TextField) SimApp.plotResolution_inputTextField.getTextField());

        final int export_ProductLikelihood_WolframPlot_function_Label_height = (plotResolution_inputTextArea_y + small_text_height) - export_ProductLikelihood_Label_y;
        final int export_ProductLikelihood_WolframPlot_function_btn_y = plotResolution_inputTextArea_y - 2;
        SimApp.export_ProductLikelihood_WolframPlot_function_btn = new CustomCheckbox("Export function", false, null);
        SimApp.export_ProductLikelihood_WolframPlot_function_btn.setBounds(c1_x + tiny_gap, export_ProductLikelihood_WolframPlot_function_btn_y, c1_content_width-tiny_gap, small_text_height);
        add((Checkbox) SimApp.export_ProductLikelihood_WolframPlot_function_btn.getCheckbox());

        SimApp.productLikelihood_WolframPlot_LabelArea = new CustomTextArea("Likelihood Plot",2,1, TextArea.SCROLLBARS_NONE);
        SimApp.productLikelihood_WolframPlot_LabelArea.setBackground(Color.lightGray);
        SimApp.productLikelihood_WolframPlot_LabelArea.setFont(new Font("Arial", Font.BOLD, 13));
        SimApp.productLikelihood_WolframPlot_LabelArea.setEnabled(true);
        SimApp.productLikelihood_WolframPlot_LabelArea.setFocusable(false);
        SimApp.productLikelihood_WolframPlot_LabelArea.setEditable(false);
        SimApp.productLikelihood_WolframPlot_LabelArea.setBounds(c1_x, export_ProductLikelihood_Label_y, c2_x - c1_x, export_ProductLikelihood_WolframPlot_function_Label_height);
        add((TextArea) SimApp.productLikelihood_WolframPlot_LabelArea.getTextArea());



        // This Section is for the Optimization Starter
        int go_Toggle_btn_y = export_ProductLikelihood_Label_y + export_ProductLikelihood_WolframPlot_function_Label_height + tiny_gap;
        SimApp.go_Toggle_btn = new CustomToggleButton("START OPT");
        SimApp.go_Toggle_btn.addActionListener(new executeOptimizationJobInGuiAdapter());
        SimApp.go_Toggle_btn.setBounds(c1_x, go_Toggle_btn_y, c1_content_width, medium_text_height);
        SimApp.go_Toggle_btn.setVisible(false);
        add((JToggleButton) SimApp.go_Toggle_btn.getToggleButton());

        SimApp.resume_btn = new CustomButton("Resume >>");
        SimApp.resume_btn.addActionListener(new resumeOptimizationJobInGuiAdapter());
        SimApp.resume_btn.setBounds(c2_x, go_Toggle_btn_y, c1_content_width, medium_text_height);
        SimApp.resume_btn.setVisible(false);
        add((Button) SimApp.resume_btn.getButton());

        int auto_resumer_btn_y = go_Toggle_btn_y + medium_text_height + tiny_gap;
        SimApp.auto_resumer_btn = new CustomCheckbox("Auto resume", false, null);
        SimApp.auto_resumer_btn.setBounds(c2_x + tiny_gap, auto_resumer_btn_y, c2_content_width, small_text_height);
        SimApp.auto_resumer_btn.setVisible(false);
        add((Checkbox) SimApp.auto_resumer_btn.getCheckbox());



        // This Section is for the "Optimization's parameters:"
        final int min_effective_rss_LabelArea_y = r2_y + small_text_height;
        SimApp.min_effective_measurement_value_LabelArea = new CustomTextArea("Min Effective Measurement\n(Units depend on the ranging technology):",1,1, TextArea.SCROLLBARS_NONE);
        SimApp.min_effective_measurement_value_LabelArea.setBounds(c3_x, min_effective_rss_LabelArea_y, c3_content_width, medium_text_height);
        SimApp.min_effective_measurement_value_LabelArea.setBackground(Color.lightGray);
        SimApp.min_effective_measurement_value_LabelArea.setEnabled(true);
        SimApp.min_effective_measurement_value_LabelArea.setFocusable(false);
        add((TextArea) SimApp.min_effective_measurement_value_LabelArea.getTextArea());

        int min_effective_measurement_inputTextArea_y = min_effective_rss_LabelArea_y + (medium_text_height-small_text_height)/2;
        SimApp.min_effective_measurement_inputTextField = new CustomTextField("80");
        SimApp.min_effective_measurement_inputTextField.setBounds(c4_x, min_effective_measurement_inputTextArea_y, c4_content_width, small_text_height);
        SimApp.min_effective_measurement_inputTextField.addTextListener(
                new integerGreaterThanBoundEnsurer(SimApp.min_effective_measurement_inputTextField, 1));
        add((TextField) SimApp.min_effective_measurement_inputTextField.getTextField());


        final int kNearestNeighbours_for_BeliefsStrength_LabelArea_y = min_effective_rss_LabelArea_y + medium_text_height;
        SimApp.kNearestNeighbours_for_BeliefsStrength_LabelArea = new CustomTextArea("k Nearest Nodes for Effectiveness Check [2,+]:",1,1, TextArea.SCROLLBARS_NONE);
        SimApp.kNearestNeighbours_for_BeliefsStrength_LabelArea.setBounds(c3_x, kNearestNeighbours_for_BeliefsStrength_LabelArea_y, c3_content_width, small_text_height);
        SimApp.kNearestNeighbours_for_BeliefsStrength_LabelArea.setBackground(Color.lightGray);
        SimApp.kNearestNeighbours_for_BeliefsStrength_LabelArea.setEnabled(true);
        SimApp.kNearestNeighbours_for_BeliefsStrength_LabelArea.setFocusable(false);
        add((TextArea) SimApp.kNearestNeighbours_for_BeliefsStrength_LabelArea.getTextArea());

        SimApp.kNearestNeighbours_for_BeliefsStrength_inputTextField = new CustomTextField("6");
        SimApp.kNearestNeighbours_for_BeliefsStrength_inputTextField.setBounds(c4_x, kNearestNeighbours_for_BeliefsStrength_LabelArea_y, c4_content_width, small_text_height);
        SimApp.kNearestNeighbours_for_BeliefsStrength_inputTextField.addTextListener(
                new integerGreaterThanBoundEnsurer(SimApp.kNearestNeighbours_for_BeliefsStrength_inputTextField, 2));
        add((TextField) SimApp.kNearestNeighbours_for_BeliefsStrength_inputTextField.getTextField());


        final int initial_Map_Extend_LabelArea_y = kNearestNeighbours_for_BeliefsStrength_LabelArea_y + small_text_height;
        SimApp.initial_Map_Extend_LabelArea = new CustomTextArea("Extent of Random Positions Initialization [0,+]:",1,1, TextArea.SCROLLBARS_NONE);
        SimApp.initial_Map_Extend_LabelArea.setBounds(c3_x, initial_Map_Extend_LabelArea_y, c3_content_width, small_text_height);
        SimApp.initial_Map_Extend_LabelArea.setBackground(Color.lightGray);
        SimApp.initial_Map_Extend_LabelArea.setEnabled(true);
        SimApp.initial_Map_Extend_LabelArea.setFocusable(false);
        add((TextArea) SimApp.initial_Map_Extend_LabelArea.getTextArea());

        SimApp.initial_Map_Extend_inputTextField = new CustomTextField("1000");
        SimApp.initial_Map_Extend_inputTextField.setBounds(c4_x, initial_Map_Extend_LabelArea_y, c4_content_width, small_text_height);
        SimApp.initial_Map_Extend_inputTextField.addTextListener(
                new integerGreaterThanBoundEnsurer(SimApp.initial_Map_Extend_inputTextField, 0));
        add((TextField) SimApp.initial_Map_Extend_inputTextField.getTextField());


        final int seed_LabelArea_y = initial_Map_Extend_LabelArea_y + small_text_height;
        SimApp.seed_LabelArea = new CustomTextArea("Seed for Random Positions Initialization [0,+]:",1,1, TextArea.SCROLLBARS_NONE);
        SimApp.seed_LabelArea.setBounds(c3_x, seed_LabelArea_y, c3_content_width, small_text_height);
        SimApp.seed_LabelArea.setBackground(Color.lightGray);
        SimApp.seed_LabelArea.setEnabled(true);
        SimApp.seed_LabelArea.setFocusable(false);
        add((TextArea) SimApp.seed_LabelArea.getTextArea());

        SimApp.seed_inputTextField = new CustomTextField("0");
        SimApp.seed_inputTextField.setBounds(c4_x, seed_LabelArea_y, c4_content_width, small_text_height);
        SimApp.seed_inputTextField.addTextListener(
                new integerGreaterThanBoundEnsurer(SimApp.seed_inputTextField, 0));
        add((TextField) SimApp.seed_inputTextField.getTextField());


        final int eval_iteration_LabelArea_y = seed_LabelArea_y + small_text_height;
        SimApp.evaluated_iteration_LabelArea = new CustomTextArea("Iteration in scenario to evaluate [0,+]:",1,1, TextArea.SCROLLBARS_NONE);
        SimApp.evaluated_iteration_LabelArea.setBounds(c3_x, eval_iteration_LabelArea_y, c3_content_width, small_text_height);
        SimApp.evaluated_iteration_LabelArea.setBackground(Color.lightGray);
        SimApp.evaluated_iteration_LabelArea.setEnabled(true);
        SimApp.evaluated_iteration_LabelArea.setFocusable(false);
        add((TextArea) SimApp.evaluated_iteration_LabelArea.getTextArea());

        SimApp.evaluated_iteration_inputTextField = new CustomTextField("0");
        SimApp.evaluated_iteration_inputTextField.setBounds(c4_x, eval_iteration_LabelArea_y, c4_content_width, small_text_height);
        SimApp.evaluated_iteration_inputTextField.addTextListener(
                new integerGreaterThanBoundEnsurer(SimApp.evaluated_iteration_inputTextField, 0));
        add((TextField) SimApp.evaluated_iteration_inputTextField.getTextField());


        final int threads_LabelArea_y = eval_iteration_LabelArea_y + small_text_height;
        SimApp.threads_LabelArea = new CustomTextArea("Optimization Threads [1,+]:",1,1, TextArea.SCROLLBARS_NONE);
        SimApp.threads_LabelArea.setBounds(c3_x, threads_LabelArea_y, c3_content_width, small_text_height);
        SimApp.threads_LabelArea.setBackground(Color.lightGray);
        SimApp.threads_LabelArea.setEnabled(true);
        SimApp.threads_LabelArea.setFocusable(false);
        add((TextArea) SimApp.threads_LabelArea.getTextArea());

        SimApp.threads_inputTextField = new CustomTextField("1");
        SimApp.threads_inputTextField.setBounds(c4_x, threads_LabelArea_y, c4_content_width, small_text_height);
        SimApp.threads_inputTextField.addTextListener(
                new integerGreaterThanBoundEnsurer(SimApp.threads_inputTextField, 1));
        add((TextField) SimApp.threads_inputTextField.getTextField());


        final int optimization_iterations_per_thread_LabelArea_y = threads_LabelArea_y + small_text_height;
        SimApp.optimization_iterations_per_thread_LabelArea = new CustomTextArea("Optimization Iterations per Thread [1,+]:",1,1, TextArea.SCROLLBARS_NONE);
        SimApp.optimization_iterations_per_thread_LabelArea.setBounds(c3_x, optimization_iterations_per_thread_LabelArea_y, c3_content_width, small_text_height);
        SimApp.optimization_iterations_per_thread_LabelArea.setBackground(Color.lightGray);
        SimApp.optimization_iterations_per_thread_LabelArea.setEnabled(true);
        SimApp.optimization_iterations_per_thread_LabelArea.setFocusable(false);
        add((TextArea) SimApp.optimization_iterations_per_thread_LabelArea.getTextArea());

        SimApp.optimization_iterations_per_thread_inputTextField = new CustomTextField("100");
        SimApp.optimization_iterations_per_thread_inputTextField.setBounds(c4_x, optimization_iterations_per_thread_LabelArea_y, c4_content_width, small_text_height);
        SimApp.optimization_iterations_per_thread_inputTextField.addTextListener(
                new integerGreaterThanBoundEnsurer(SimApp.optimization_iterations_per_thread_inputTextField, 1));
        add((TextField) SimApp.optimization_iterations_per_thread_inputTextField.getTextField());


        final int max_optimization_time_per_thread_LabelArea_y = optimization_iterations_per_thread_LabelArea_y + small_text_height;
        SimApp.max_optimization_time_per_thread_LabelArea = new CustomTextArea("Max Step-Opt. Runtime per Thread (ms) [1,+]:",1,1, TextArea.SCROLLBARS_NONE);
        SimApp.max_optimization_time_per_thread_LabelArea.setBounds(c3_x, max_optimization_time_per_thread_LabelArea_y, c3_content_width, small_text_height);
        SimApp.max_optimization_time_per_thread_LabelArea.setBackground(Color.lightGray);
        SimApp.max_optimization_time_per_thread_LabelArea.setEnabled(true);
        SimApp.max_optimization_time_per_thread_LabelArea.setFocusable(false);
        add((TextArea) SimApp.max_optimization_time_per_thread_LabelArea.getTextArea());

        SimApp.max_optimization_time_per_thread_inputTextField = new CustomTextField("6000");
        SimApp.max_optimization_time_per_thread_inputTextField.setBounds(c4_x, max_optimization_time_per_thread_LabelArea_y, c4_content_width, small_text_height);
        SimApp.max_optimization_time_per_thread_inputTextField.addTextListener(
                new integerGreaterThanBoundEnsurer(SimApp.max_optimization_time_per_thread_inputTextField, 6000));
        add((TextField) SimApp.max_optimization_time_per_thread_inputTextField.getTextField());


        final int ftol_LabelArea_y = max_optimization_time_per_thread_LabelArea_y + small_text_height;
        SimApp.ftol_LabelArea = new CustomTextArea("Optimization's ftol: [1e-..]",1,1, TextArea.SCROLLBARS_NONE);
        SimApp.ftol_LabelArea.setBounds(c3_x, ftol_LabelArea_y, c3_content_width, small_text_height);
        SimApp.ftol_LabelArea.setBackground(Color.lightGray);
        SimApp.ftol_LabelArea.setEnabled(true);
        SimApp.ftol_LabelArea.setFocusable(false);
        add((TextArea) SimApp.ftol_LabelArea.getTextArea());

        SimApp.ftol_inputTextField = new CustomTextField("2");
        SimApp.ftol_inputTextField.setBounds(c4_x, ftol_LabelArea_y, c4_content_width, small_text_height);
        SimApp.ftol_inputTextField.addTextListener(
                new integerGreaterThanBoundEnsurer(SimApp.ftol_inputTextField, 1));
        add((TextField) SimApp.ftol_inputTextField.getTextField());


        final int initial_step_size_LabelArea_y = ftol_LabelArea_y + small_text_height;
        SimApp.initial_step_size_LabelArea = new CustomTextArea("Optimization's Initial Step Size: [1,+]",1,1, TextArea.SCROLLBARS_NONE);
        SimApp.initial_step_size_LabelArea.setBounds(c3_x, initial_step_size_LabelArea_y, c3_content_width, small_text_height);
        SimApp.initial_step_size_LabelArea.setBackground(Color.lightGray);
        SimApp.initial_step_size_LabelArea.setEnabled(true);
        SimApp.initial_step_size_LabelArea.setFocusable(false);
        add((TextArea) SimApp.initial_step_size_LabelArea.getTextArea());

        SimApp.initial_step_size_inputTextField = new CustomTextField("10");
        SimApp.initial_step_size_inputTextField.setBounds(c4_x, initial_step_size_LabelArea_y, c4_content_width, small_text_height);
        SimApp.initial_step_size_inputTextField.addTextListener(
                new integerGreaterThanBoundEnsurer(SimApp.initial_step_size_inputTextField, 1));
        add((TextField) SimApp.initial_step_size_inputTextField.getTextField());


        final int optimization_cycles_LabelArea_y = initial_step_size_LabelArea_y + small_text_height;
        SimApp.optimization_cycles_LabelArea = new CustomTextArea("Optimization Cycles [1,+]:",1,1, TextArea.SCROLLBARS_NONE);
        SimApp.optimization_cycles_LabelArea.setBounds(c3_x, optimization_cycles_LabelArea_y, c3_content_width, small_text_height);
        SimApp.optimization_cycles_LabelArea.setBackground(Color.lightGray);
        SimApp.optimization_cycles_LabelArea.setEnabled(true);
        SimApp.optimization_cycles_LabelArea.setFocusable(false);
        add((TextArea) SimApp.optimization_cycles_LabelArea.getTextArea());

        SimApp.optimization_cycles_inputTextField = new CustomTextField("50");
        SimApp.optimization_cycles_inputTextField.setBounds(c4_x, optimization_cycles_LabelArea_y, c4_content_width, small_text_height);
        SimApp.optimization_cycles_inputTextField.addTextListener(
                new integerGreaterThanBoundEnsurer(SimApp.optimization_cycles_inputTextField, 1));
        add((TextField) SimApp.optimization_cycles_inputTextField.getTextField());


        final int optimizationSettings_Label_height = (optimization_cycles_LabelArea_y + small_text_height) - r2_y;
        SimApp.optimizationParameters_LabelArea = new CustomTextArea("Optimization's Parameters",2,1, TextArea.SCROLLBARS_NONE);
        SimApp.optimizationParameters_LabelArea.setFont(new Font("Arial", Font.BOLD, 13));
        SimApp.optimizationParameters_LabelArea.setBackground(Color.lightGray);
        SimApp.optimizationParameters_LabelArea.setEnabled(true);
        SimApp.optimizationParameters_LabelArea.setFocusable(false);
        SimApp.optimizationParameters_LabelArea.setEditable(false);
        SimApp.optimizationParameters_LabelArea.setBounds(c3_x, r2_y, c3_content_width, optimizationSettings_Label_height);
        add((TextArea) SimApp.optimizationParameters_LabelArea.getTextArea());


        // TODO: Experimentation options
        // This Section is for the Optimization's Order
//        final int optimization_order_Label_y = r2_y;
//        CheckboxGroup optimization_order_group = new CheckboxGroup();
//        Sim_App.spatial_direction_btn = new CustomCheckbox("Direction", false, optimization_order_group);
//        Sim_App.spatial_direction_btn.setBounds(1485, 846, 70, small_textfield_height);
//        add((Checkbox) Sim_App.spatial_direction_btn.getCheckbox());
//
//        Sim_App.rss_density_check_btn = new CustomCheckbox("Density", true, optimization_order_group);
//        Sim_App.rss_density_check_btn.setBounds(1485, 866, 70, small_textfield_height);
//        add((Checkbox) Sim_App.rss_density_check_btn.getCheckbox());
//
//        Sim_App.optimization_order_Label = new CustomTextArea("Opt. Order:",2,1, TextArea.SCROLLBARS_NONE);
//        Sim_App.optimization_order_Label.setBackground(Color.lightGray);
//        Sim_App.optimization_order_Label.setFont(new Font("Arial", Font.BOLD, 13));
//        Sim_App.optimization_order_Label.setEnabled(true);
//        Sim_App.optimization_order_Label.setFocusable(false);
//        Sim_App.optimization_order_Label.setEditable(false);
//        Sim_App.optimization_order_Label.setBounds(c2_x, optimization_order_Label_y, 90, r2_height);
//        add((TextArea) Sim_App.optimization_order_Label.getTextArea());



        // This Section is for the Project Name/db Setup
//        String timeStamp = new SimpleDateFormat("yyyyMMddHHmmss").format(new Date());
//        projectName_inputTextArea = new CustomTextField(timeStamp);
//        Sim_App.projectName_LabelArea = new CustomTextArea("Export to Folder:",1,1, TextArea.SCROLLBARS_NONE);
//        Sim_App.projectName_LabelArea.setBounds(c3_x, export_ProductLikelihood_Label_y, normal_gap, small_text_height);
//        Sim_App.projectName_LabelArea.setBackground(Color.lightGray);
//        Sim_App.projectName_LabelArea.setEnabled(true);
//        Sim_App.projectName_LabelArea.setFocusable(false);
//        add((TextArea) Sim_App.projectName_LabelArea.getTextArea());
//
//        Sim_App.projectName_inputTextField = new CustomTextField(Sim_App.clean_evaluated_scenario_name);
//        Sim_App.projectName_inputTextField.setBounds(c3_x + normal_gap, export_ProductLikelihood_Label_y, c3_content_width - normal_gap, small_text_height);
//        Sim_App.projectName_inputTextField.addTextListener(new projectName_inputTextArea_Ensurer());
//        add((TextField) Sim_App.projectName_inputTextField.getTextField());



        setSize(window_width, window_height);
        setLocation(10,10);

        SimApp.window_adapter = new WnAdapter();
        addWindowListener(SimApp.window_adapter);

        setBackground(Color.lightGray);
        setResizable(false);

        setVisible(true);
        toFront();

        // Initiate the automated resumer
        //auto_optimization_resumer(); // TODO: Remove
    }

    private static void resetChartCanvas() {
        try {SimApp.chart_odd.dispose();}
        catch (Exception ignored) {}

        try {SimApp.chart_even.dispose();}
        catch (Exception ignored) {}

        try {SimApp.chart_component_container_odd.remove(SimApp.chart_component_odd);}
        catch (Exception ignored) {}

        try {SimApp.chart_component_container_even.remove(SimApp.chart_component_even);}
        catch (Exception ignored) {}

        setEmptyChartComponents();

        SimApp.chart_component_odd.setBounds(SimApp.chart_components_bounds);
        SimApp.chart_component_even.setBounds(SimApp.chart_components_bounds);

        try {SimApp.chart_component_container_odd.add(SimApp.chart_component_odd, 0);}
        catch (Exception ignored) {}

        try {SimApp.chart_component_container_even.add(SimApp.chart_component_even, 0);}
        catch (Exception ignored) {}
    }

    private static void setEmptyChartComponents() {
        SimApp.chart_odd = new ContourChart(Quality.Advanced().setHiDPIEnabled(true));
        SimApp.chart_odd.view2d();
        SimApp.chart_even = new ContourChart(Quality.Advanced().setHiDPIEnabled(true));
        SimApp.chart_even.view2d();

        SimApp.chart_component_odd = (Component) SimApp.chart_odd.getCanvas();
        SimApp.chart_component_even = (Component) SimApp.chart_even.getCanvas();
    }

    public static void changeConfigurationPanelEnabledState(boolean state) {
        SimApp.openDB_Btn.setEnabled(state);
        SimApp.results_per_step_btn.setEnabled(state);
        SimApp.results_per_cycle_btn.setEnabled(state);
        SimApp.ble_model_btn.setEnabled(state);
        SimApp.uwb_model_btn.setEnabled(state);
        SimApp.plotResolution_inputTextField.setEnabled(state);
        SimApp.export_ProductLikelihood_WolframPlot_function_btn.setEnabled(state);
        SimApp.min_effective_measurement_inputTextField.setEnabled(state);
        SimApp.kNearestNeighbours_for_BeliefsStrength_inputTextField.setEnabled(state);
        SimApp.initial_Map_Extend_inputTextField.setEnabled(state);
        SimApp.seed_inputTextField.setEnabled(state);
        SimApp.evaluated_iteration_inputTextField.setEnabled(state);
        SimApp.threads_inputTextField.setEnabled(state);
        SimApp.optimization_iterations_per_thread_inputTextField.setEnabled(state);
        SimApp.max_optimization_time_per_thread_inputTextField.setEnabled(state);
        SimApp.ftol_inputTextField.setEnabled(state);
        SimApp.initial_step_size_inputTextField.setEnabled(state);
        SimApp.optimization_cycles_inputTextField.setEnabled(state);
    }

    // This function is used during the headless execution
    public static void headlessInit() {
        SimApp.outputTerminal = new CustomTextArea("");
        SimApp.go_Toggle_btn = new CustomToggleButton("GO");
        SimApp.projectName_inputTextField = new CustomTextField(SimApp.clean_evaluated_scenario_name);

        // On the very first iteration, ensure that folder structure is as supposed to be
        if (SimApp.evaluated_iteration == 0){
            // First ensure that the given output path folder path to store the estimations is valid
            SimApp.ensureFolder(SimApp.outpath_results_folder_path);

            // Then, ensure that the given project name exists as a folder
            SimApp.ensureFolder(SimApp.outpath_results_folder_path + SimApp.clean_evaluated_scenario_name + "/");
        }

        // Construct the destination path for the results of current iteration
        SimApp.output_iteration_results_folder_path = SimApp.outpath_results_folder_path + SimApp.clean_evaluated_scenario_name + "/" + SimApp.evaluated_iteration + "/";

        // Check whether current iteration has already been done before
        boolean iteration_results_already_available = SimApp.handlePreviousIterationResults(false);

        // Initiate the automated resumer
        autoOptimizationResumer();

        // Proceed or not with current iteration depending on the existence of previous results
        if (iteration_results_already_available){
            // Stop the auto-resumer for the current optimization process
            SimApp.scheduled_auto_resumer.cancel(true);
            SimApp.scheduler.shutdown();
        }
    }

    // This method takes care of the proper folder structure.
    // After any handling, it returns False only if we have already executed current evaluation iteration
    private static boolean handlePreviousIterationResults(boolean force_clean) {
        // Construct the destination path for the results of current iteration
        SimApp.output_iteration_results_folder_path = Paths.get(
                SimApp.outpath_results_folder_path,
                SimApp.clean_evaluated_scenario_name,
                String.valueOf(SimApp.evaluated_iteration)
        ).toString();

        System.out.println("\nChecking for previous results at: " + SimApp.output_iteration_results_folder_path);

        // Then, ensure that the given project name exists as a folder
        if (SimApp.ensureFolder(SimApp.output_iteration_results_folder_path)){
            // Being here means that this iteration has not been executed before
            System.out.println("Destination folder: " + SimApp.output_iteration_results_folder_path + " has been created");
            return false;
        }
        else{
            System.out.println("Destination folder: " + SimApp.output_iteration_results_folder_path + " already exists");

            // Being here means that this iteration has been executed before.
            // Therefore, we need to check whether the previous results have been successfully delivered
            File result_file = new File(
                    Paths.get(SimApp.output_iteration_results_folder_path,"results.log").toString()
            );
            boolean result_file_exists = result_file.exists();

            if (result_file_exists && !force_clean){
                // Being here means that a result file has been located. Therefore, we need to skip this iteration
                System.out.println("Previous results have finished. Proceeding to next iteration");
                return true;
            }
            else{
                // Being here means that a result file has not been.
                // Therefore, we wipe current folder, and we start from the beginning
                System.out.println("Previous results have not finished.");

                File directoryToBeDeleted = new File(SimApp.output_iteration_results_folder_path);

                boolean folder_deleted = deleteDirectory(directoryToBeDeleted);
                System.out.println("Deleting directory: " + SimApp.output_iteration_results_folder_path + " = " + folder_deleted);

                boolean folder_created = directoryToBeDeleted.mkdirs();
                System.out.println("Creating directory: " + SimApp.output_iteration_results_folder_path + " = " + folder_created);

                return false;
            }
        }
    }

    static void appendToTextArea(String text){
        String current_text = SimApp.outputTerminal.getText();
        SimApp.outputTerminal.setText(current_text + text + "\n");
    }

    static void resetTextArea(){
        SimApp.outputTerminal.setText("");
    }

    static void setPropertiesAsAvailable(boolean enabled){

        SimApp.projectName_inputTextField.setEnabled(enabled);

        // Update the map-exporting toggler's state
        if (!SimApp.headless_mode){
            SimApp.results_per_step_btn.setEnabled(enabled);
            SimApp.results_per_cycle_btn.setEnabled(enabled);
        }

        SimApp.kNearestNeighbours_for_BeliefsStrength_inputTextField.setEnabled(enabled);
        SimApp.max_optimization_time_per_thread_inputTextField.setEnabled(enabled);
        SimApp.min_effective_measurement_inputTextField.setEnabled(enabled);

        SimApp.plotResolution_inputTextField.setEnabled(enabled);

        if (!SimApp.headless_mode){
            SimApp.export_ProductLikelihood_WolframPlot_function_btn.setEnabled(enabled);
        }
    }

    static private void resume(){

        // Execute everything in a new thread to avoid coming in conflict with the GUI's thread
        SimApp.t1 = new Thread(() -> {
            SimApp.go_Toggle_btn.setEnabled(false);

            String results_per_selection;

            // Get the state of the results_per CustomCheckbox
            if (SimApp.headless_mode || !SimApp.results_per_step_btn.getState()){
                results_per_selection = "Cycle";
            }
            else{
                results_per_selection = "Step";
            }

            // If we are performing an optimization based on Density, we need to mention the utilised parameter
            String kNN_for_beliefs_strength_check = "\n";
            if (SimApp.headless_mode){
                kNN_for_beliefs_strength_check = "\nkNN to consider for the Beliefs-Strength check: " + SimApp.kNearestNeighbours_for_BeliefsStrength + " Neighbors\n";
            }

            // If we are exporting also Wolfram features, we need to mention which these are
            String likelihoods_export = "None]\n";
            if (SimApp.headless_mode || SimApp.export_ProductLikelihood_WolframPlot_function_btn.getState()){
                likelihoods_export = "Wolfram Plot]\n";
            }

            String summary_msg ="\n=========== Optimization Initiated ===========" +
                    "\nExport folder: " + SimApp.output_iteration_results_folder_path +
                    "\nScenario: " + SimApp.clean_evaluated_scenario_name +
                    "\nEvaluated iteration in scenario: " + SimApp.evaluated_iteration +
                    "\nMin effective measurement value: " + SimApp.min_effective_measurement + "units" +
                    "\nExtent of positions initialization: " +  SimApp.initial_Map_Extend +
                    "\nRandom seed: " +  SimApp.seed +
                    "\nThread workers: " +  SimApp.threads +
                    "\nOptimization iterations per thread: " + SimApp.optimization_iterations_per_thread +
                    "\nMax step optimization runtime (per thread): " + SimApp.max_optimization_time_per_thread +
                    "\nOptimization's ftol: " + SimApp.ftol +
                    "\nOptimization's step size: " + SimApp.step_size +
                    "\nStop Cycle: " + SimApp.optimization_cycles +
                    "\nResults per: " + results_per_selection +
                    kNN_for_beliefs_strength_check +
                    "Likelihoods export: [" + likelihoods_export;

            SimApp.appendToTextArea(summary_msg);

            setPropertiesAsAvailable(false);

            try {
                System.out.println("Resuming Headless Positioning");
                Core.resumeSwarmPositioning();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }

            setPropertiesAsAvailable(true);
            SimApp.go_Toggle_btn.setEnabled(true);
        });
        SimApp.t1.start();
    }

    private static void finishOptimization() {
        changeConfigurationPanelEnabledState(true);
        SimApp.go_Toggle_btn.setClicked(false);
        SimApp.go_Toggle_btn.setText("GO");
        SimApp.resume_btn.setVisible(false);
        SimApp.auto_resumer_btn.setVisible(false);
        SimApp.auto_resumer_btn.setState(false);
    }

    private static void prepareInitializationLog() {
        String results_per_selection;
        // Get the state of the results_per_step_btn CustomCheckbox
        if (SimApp.results_per_step_btn.getState()){
            results_per_selection = "Step";
        }
        else{
            results_per_selection = "Cycle";
        }

        String kNN_for_beliefs_strength_check = "\nkNN to consider for the Beliefs-Strength check: " + SimApp.kNearestNeighbours_for_BeliefsStrength + " Neighbors\n";
        String likelihoods_export = "None]\n";
        if (SimApp.export_ProductLikelihood_WolframPlot_function_btn.getState()){
            likelihoods_export = "Wolfram Plot]\n";
        }
        String summary_msg ="\n=========== Optimization Initiated ===========" +
                "\nExport folder: " + SimApp.output_iteration_results_folder_path +
                "\nScenario: " + SimApp.clean_evaluated_scenario_name +
                "\nEvaluated iteration in scenario: " + SimApp.evaluated_iteration +
                "\nMin effective measurement value: " + SimApp.min_effective_measurement + "units" +
                "\nExtent of positions initialization: " +  SimApp.initial_Map_Extend +
                "\nRandom seed: " +  SimApp.seed +
                "\nThread workers: " +  SimApp.threads +
                "\nOptimization iterations per thread: " + SimApp.optimization_iterations_per_thread +
                "\nMax step optimization runtime (per thread): " + SimApp.max_optimization_time_per_thread +
                "\nOptimization's ftol: " + SimApp.ftol +
                "\nOptimization's step size: " + SimApp.step_size +
                "\nStop Cycle: " + SimApp.optimization_cycles +
                "\nResults per: " + results_per_selection +
                kNN_for_beliefs_strength_check +
                "Likelihoods export: [" + likelihoods_export;

        SimApp.appendToTextArea(summary_msg);
    }

    private static void getGuiOptimizationParameters() {
        // Set user's optimization parameters
        // Booleans
        SimApp.results_per_step = SimApp.results_per_step_btn.getState();
        SimApp.results_per_cycle = SimApp.results_per_cycle_btn.getState();
        SimApp.ble_model = SimApp.ble_model_btn.getState();
        SimApp.uwb_model = SimApp.uwb_model_btn.getState();
        SimApp.export_ProductLikelihood_WolframPlot = SimApp.export_ProductLikelihood_WolframPlot_function_btn.getState();
        SimApp.plotResolution = Integer.parseInt(SimApp.plotResolution_inputTextField.getText());

        // Values
        SimApp.min_effective_measurement = Integer.parseInt(SimApp.min_effective_measurement_inputTextField.getText());
        SimApp.kNearestNeighbours_for_BeliefsStrength = Integer.parseInt(SimApp.kNearestNeighbours_for_BeliefsStrength_inputTextField.getText());
        SimApp.initial_Map_Extend = Integer.parseInt(SimApp.initial_Map_Extend_inputTextField.getText());
        SimApp.evaluated_iteration = Integer.parseInt(SimApp.evaluated_iteration_inputTextField.getText());
        SimApp.threads = Integer.parseInt(SimApp.threads_inputTextField.getText());
        SimApp.optimization_iterations_per_thread = Integer.parseInt(SimApp.optimization_iterations_per_thread_inputTextField.getText());
        SimApp.max_optimization_time_per_thread = Integer.parseInt(SimApp.max_optimization_time_per_thread_inputTextField.getText());
        SimApp.ftol = Integer.parseInt(SimApp.ftol_inputTextField.getText());
        SimApp.step_size = Integer.parseInt(SimApp.initial_step_size_inputTextField.getText());
        SimApp.optimization_cycles = Integer.parseInt(SimApp.optimization_cycles_inputTextField.getText());

        MathEngine.bestLikelihood = Double.NEGATIVE_INFINITY; // POSITIVE_INFINITY // NEGATIVE_INFINITY;
        MathEngine.swarmPositioningOptimizers = new Optimizer[SimApp.threads];
        SimApp.seed = Long.parseLong(SimApp.seed_inputTextField.getText());
        SimApp.random.setSeed(SimApp.seed);
    }

    private static void autoOptimizationResumer() {
        scheduler = Executors.newScheduledThreadPool(1);

        final Runnable auto_resumer = () -> {
            //System.out.println("Sim_App.autoResume.getState():" + Sim_App.autoResume.getState() + " Sim_App.autoResume.isEnabled():" + Sim_App.autoResume.isEnabled());

            // Check if the user has just enabled the auto resumer
            if (SimApp.go_Toggle_btn.isClicked() && SimApp.go_Toggle_btn.isEnabled()){
                resume();
            }
        };

        scheduled_auto_resumer = scheduler.scheduleAtFixedRate(auto_resumer, 0, 100, MILLISECONDS);
    }

    static boolean ensureFolder(String selected_path){
        File directory = new File(selected_path);
        return directory.mkdirs();
    }

    static boolean deleteDirectory(File directoryToBeEmptied) {

        File[] allContents = directoryToBeEmptied.listFiles();
        if (allContents != null) {
            for (File file : allContents) {
                deleteDirectory(file);
            }
        }
        return directoryToBeEmptied.delete();
    }

    static void writeString2File(String filename, String data){

//        System.out.println("Data for exporting: " + data);

        try (PrintWriter out = new PrintWriter(filename)) {
            out.println(data);
            // Make a check here to see if the list containing the effective_nodes has the same size as the entire Node db-1
            // This means that there is no non-Effective Node. Hence, we need to exclude Plot B
            if (SimApp.effective_remoteNodes.size() == (SimApp.nodeID_to_nodeObject.size()-1)){
                out.println("Show[model, plotA, plotC]");
            }
            else{
                out.println("Show[model, plotA, plotB, plotC]");
            }

        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
    }

    // This listener ensures an Arithmetic text of less than 500 value
    static private class integerGreaterThanBoundEnsurer implements TextListener {

        CustomTextField attachedTextField;
        int min_value;
        String previous_valid_value;


        integerGreaterThanBoundEnsurer(CustomTextField attachedTextField, int min_value){
            this.attachedTextField = attachedTextField;
            this.min_value = min_value;
            this.previous_valid_value = String.valueOf(min_value);
        }

        public void textValueChanged(TextEvent evt) {

            String current_text = attachedTextField.getText();

            if (current_text.length() == 0){
                attachedTextField.setText(this.previous_valid_value);
            }

            // We do this check and corresponding update to be able to escape the infinite loop
            else if (!current_text.equals(previous_valid_value)){

                // Check whether we have exceeded the maximum allowed filename length
                String new_text = current_text.replaceAll("[^\\p{N}]+", "");

                try {
                    int plot_res = Integer.parseInt(new_text);

                    if (plot_res >= min_value){
                        previous_valid_value = new_text;
                        attachedTextField.setText(new_text);
                    }
                    else{
                        attachedTextField.setText(previous_valid_value);
                    }
                } catch (Exception e) {
                    attachedTextField.setText(previous_valid_value);
                }

                attachedTextField.setCaretPosition(attachedTextField.getText().length());
            }
        }
    }

    // This listener ensures an Arithmetic text of less than 500 value
    static private class resetOptimizationParametersPanelAdapter implements ItemListener {
        public void itemStateChanged(ItemEvent evt) {

            String user_selection = evt.getItem().toString();

            if (user_selection.contains("UWB")){
                // System.out.println("User changed to UWB model");
                // This reset's the parameters to be the same as the ones used in our paper
                SimApp.min_effective_measurement_inputTextField.setText("60");
                SimApp.threads_inputTextField.setText("1");
                SimApp.optimization_iterations_per_thread_inputTextField.setText("1000");
                SimApp.ftol_inputTextField.setText("2");
                SimApp.initial_step_size_inputTextField.setText("10");
                SimApp.max_optimization_time_per_thread_inputTextField.setText("1000000");
                SimApp.optimization_cycles_inputTextField.setText("50");
                SimApp.kNearestNeighbours_for_BeliefsStrength_inputTextField.setText("6");
            }
            else if (user_selection.contains("BLE")){
                // System.out.println("User changed to BLE model");
                // This reset's the parameters to be the same as the ones used in our paper
                // TODO
            }
        }
    }

    static private class openDBBtnAdapter implements ActionListener {
        public void actionPerformed(ActionEvent e) {
            final JFrame frame = new JFrame();
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.setLayout(new GridLayout(0, 1));

            FileDialog fd = new FileDialog(frame, "Test", FileDialog.LOAD);
            fd.setVisible(true);

            // Check whether the user has selected a valid file
            if (fd.getFile() != null){
                int dotIndex = fd.getFile().lastIndexOf(".");
                input_file_extension = fd.getFile().substring(dotIndex);

                SimApp.clean_evaluated_scenario_name = fd.getFile().substring(0, dotIndex);

                SimApp.input_file_path = Paths.get(
                        fd.getDirectory(),
                        fd.getFile()
                ).toString();

                SimApp.outpath_results_folder_path = fd.getDirectory();
                SimApp.loaded_db_name_LabelArea.setBackground(Color.white);
                //SimApp.loaded_db_name_LabelArea.setText(SimApp.clean_evaluated_scenario_name + " at " + fd.getDirectory());
                SimApp.loaded_db_name_LabelArea.setText(SimApp.clean_evaluated_scenario_name);
                SimApp.go_Toggle_btn.setVisible(true);

                SimApp.resetChartCanvas();
                System.gc();

                //System.out.println(evaluated_scenario_name + " database in " + input_file_path + " loaded."); // TODO: Add it on terminal
            }
        }
    }

    static private class resumeOptimizationJobInGuiAdapter implements ActionListener {
        public void actionPerformed(ActionEvent e) {
            SimApp.resume_flag = true;
        }
    }

    static private class executeOptimizationJobInGuiAdapter implements ActionListener {
        public void actionPerformed(ActionEvent e) {
            // This method is only accessible from GUI, where only a single evaluation ID from the loaded DB file can be
            // executed. This ID is the one that the user set in the panel with the optimization's parameters.
            if (SimApp.go_Toggle_btn.isClicked()){
                System.out.println("Starting Cooperative Localization Optimization");

                SimApp.go_Toggle_btn.setText("Stop");

                try {
                    getGuiOptimizationParameters();

                    // After getting the parameter inputs, disable the panel so that no changes can be made
                    changeConfigurationPanelEnabledState(false);

                    resetDataStructures();

                    // Only one iteration will be performed since we are in a graphical environment.
                    // Ensure that the required exporting folder structure exists

                    String evaluated_scenario_destination_path = Paths.get(
                            SimApp.outpath_results_folder_path,
                            SimApp.clean_evaluated_scenario_name
                    ).toString();

                    System.out.println("Ensuring scenario root folder" + evaluated_scenario_destination_path);
                    SimApp.ensureFolder(evaluated_scenario_destination_path);

                    // Check whether current iteration has already been done before
                    boolean iteration_results_already_available =  SimApp.handlePreviousIterationResults(true);

                    if (!iteration_results_already_available){

                        resetTextArea();

                        // Create a controller thread to run separately from the GUI.
                        // In headless mode this is not necessary
                        SimApp.controller_thread = new Thread(() -> {
                            // Prepare the log about the used optimization's settings
                            prepareInitializationLog();

                            try {
                                Core.init();
                                while (!SimApp.stop_optimization){
                                    // If the auto_resumer_btn is not enabled, we will enter a pause state.
                                    // Activate the resuming button for the user to be able to proceed manually.
                                    SimApp.resume_btn.setVisible(true);
                                    SimApp.auto_resumer_btn.setVisible(true);

                                    if (SimApp.auto_resumer_btn.getState() || resume_flag){

                                        resume_flag = false;

                                        // Hide the resumer button
                                        SimApp.resume_btn.setVisible(false);

                                        startTime = System.nanoTime();
                                        Core.resumeSwarmPositioningInGUIMode();

                                        // We completed executing this optimization part. If  the auto_resumer_btn is
                                        // not enabled, we are entering a pause state. Therefore, activate the resuming
                                        // button for the user to be able to proceed manually.
                                        SimApp.resume_btn.setVisible(true);
                                        System.gc();
                                    }
                                    else{
                                        Thread.sleep(1000);
                                    }
                                }
                                finishOptimization();
                            } catch (Exception ex) {
                                ex.printStackTrace();
                                throw new RuntimeException(ex);
                            }
                        });
                        SimApp.controller_thread.start();
                    }
                } catch (Exception ex) {
                    ex.printStackTrace();
                    StackTraceElement[] stackTraceElements = ex.getStackTrace();
                    for (StackTraceElement ste: stackTraceElements) {
                        SimApp.appendToTextArea(ste.toString());
                    }
                    SimApp.appendToTextArea(ex.toString());
                }
            }
            else {
                // Being here means that the user clicked to stop the optimization
//                System.out.println("Stopping Optimization");
                SimApp.appendToTextArea("Force stop at: " + SimApp.day_formatter.format(new Date()));
                SimApp.stop_optimization = true;
            }
        }
    }

    static private class clearTerminalBtnAdapter implements ActionListener {
        public void actionPerformed(ActionEvent e) {
            SimApp.outputTerminal.setText("");
        }
    }

    class WnAdapter extends WindowAdapter {
        public void windowClosing(WindowEvent event) {
            dispose();
            System.exit(0);
        }
    }

    // This listener ensures an Arithmetic text of less than 500 value
    static private class plotResolutionInputTextAreaEnsurer implements TextListener {
        public void textValueChanged(TextEvent evt) {

            String current_text = SimApp.plotResolution_inputTextField.getText();

            if (current_text.length() == 0){
                SimApp.plotResolution_inputTextField.setText("1");
            }

            // We do this check and corresponding update to be able to escape the infinite loop
            else if (!current_text.equals(SimApp.previous_valid_plot_resolution)){

                // Check whether we have exceeded the maximum allowed filename length
                String new_text = current_text.replaceAll("[^\\p{N}]+", "");

                try {
                    int plot_res = Integer.parseInt(new_text);

                    if (plot_res<501){
                        SimApp.previous_valid_plot_resolution = new_text;
                        SimApp.plotResolution_inputTextField.setText(new_text);
                    }
                    else{
                        SimApp.plotResolution_inputTextField.setText(SimApp.previous_valid_plot_resolution);
                    }
                } catch (Exception e) {
                    SimApp.plotResolution_inputTextField.setText(SimApp.previous_valid_plot_resolution);
                }

                SimApp.plotResolution_inputTextField.setCaretPosition(SimApp.plotResolution_inputTextField.getText().length());
            }
        }
    }

    // This listener ensures an Alpharithmetic text of less than 16 Chars length
    static private class projectName_inputTextArea_Ensurer implements TextListener {
        public void textValueChanged(TextEvent evt) {

            String current_text = SimApp.projectName_inputTextField.getText();

            //SimApp.resumeBtn.setEnabled(current_text.length() != 0);

            // We do this check and corresponding update to be able to escape the infinite loop
            if (!current_text.equals(SimApp.previous_valid_project_name)){

                // Check whether we have exceeded the maximum allowed filename length
                if (current_text.length() > 15){
                    SimApp.projectName_inputTextField.setText(SimApp.previous_valid_project_name);
                }
                else{
                    String new_text = current_text.replaceAll("[^\\p{L}\\p{N} ]+", "");
                    SimApp.previous_valid_project_name = new_text;
                    SimApp.projectName_inputTextField.setText(new_text);
                }
                SimApp.projectName_inputTextField.setCaretPosition(SimApp.projectName_inputTextField.getText().length());
            }
        }
    }

    static private class stopBtnAdapter implements ActionListener {
        public void actionPerformed(ActionEvent e) {
//            stopOptimization();
        }
    }

    // Keep in mind that when the user requests from the optimization to cancel, he needs to wait for some time until
    // the optimization reaches the point where stopping flag is checked
    private static void stopOptimization(){
//        SimApp.go_Toggle_btn.setClicked(false);

//        Date date = new Date();

//        appendToTextArea("Force stop at: " + SimApp.day_formatter.format(date));

        // Interrupt all the Optimizers
//        for (Optimizer optimizer: MathEngine.swarmPositioningOptimizers){
//            optimizer.interrupt();
//        }

        // Interrupt also the Optimizer Handler
//        SimApp.t1.interrupt();

        // In case we are in-between a likelihood export process, cancel it
//        if (SimApp.rendering_wolfram_data){
//        }

//        while (true){
//            // Make the loop break for a bit to not suffocate the thread while waiting
//            try {
//                Thread.sleep(100);
//            } catch (InterruptedException ignore) {}
//            // Wait until the optimization has been stopped
//            if(!SimApp.optimization_running){
//                setPropertiesAsAvailable(true);
//                break;
//            }
//        }

//        if (MathEngine.NodePos_Results_filename != null){
//            // Dump all results in a log file
//            try (PrintWriter out = new PrintWriter(MathEngine.NodePos_Results_filename)) {
//                String current_logs = SimApp.outputTerminal.getText();
//                if (SimApp.headless_mode){
//                    out.println(current_logs);
//                }
//                else {
//                    out.println(current_logs.substring(0, current_logs.lastIndexOf("=========== Optimization Initiated ===========")));
//                }
//            } catch (FileNotFoundException exception) {
//                exception.printStackTrace();
//            }
//        }
    }

    private static void loadImgToGUI(String last_generated_image_path){

        // Get the last generated IMG result
        File fileInput = new File(last_generated_image_path);

        System.out.println("Input plot path: " + fileInput);

        BufferedImage last_generated_image;
        BufferedImage scaledImage = null;

        try {
            last_generated_image = ImageIO.read(fileInput);

            // Scale the image
            final int w = last_generated_image.getWidth();
            final int h = last_generated_image.getHeight();
            scaledImage = new BufferedImage((int) (w * 0.9),(int) (h * 0.9), BufferedImage.TYPE_INT_ARGB);
            final AffineTransform at = AffineTransform.getScaleInstance(0.9, 0.9);
            final AffineTransformOp ato = new AffineTransformOp(at, AffineTransformOp.TYPE_BICUBIC);
            scaledImage = ato.filter(last_generated_image, scaledImage);

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void executeOptimizationJobsInGui() {

        try {
            Core.init();
            System.out.println("Start Optimizing");
            SimApp.go_Toggle_btn.setClicked(true);
        } catch (Exception e) {
            e.printStackTrace();
            // At this point we can close the program
            System.exit(0); // TODD add that to headless
        }

        System.out.println("Optimization finished");
    }

    private static Integer getTotalIterations() throws Exception {
        Integer total_iterations = null;

        // Open the file
        FileInputStream fstream = new FileInputStream(SimApp.input_file_path + SimApp.input_file_extension);
        BufferedReader br = new BufferedReader(new InputStreamReader(fstream));

        String strLine;

        //Read File Line By Line
        while ((strLine = br.readLine()) != null){
            if (strLine.startsWith("#RSS_")){
                //System.out.println(strLine.replace("#\n", "").replace("#RSS_", ""));
                total_iterations = Integer.parseInt(strLine.substring(5, strLine.lastIndexOf("#")));
            }
        }

        fstream.close();

        return total_iterations;
    }
}