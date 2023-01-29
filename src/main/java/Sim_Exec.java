public class Sim_Exec {
    static { /* works fine! ! */
        System.setProperty("java.awt.headless", "false"); //todo UBELIX requires this to be set to true
        System.out.println("Headless mode: " + java.awt.GraphicsEnvironment.isHeadless());
    }

    public static void main(String[] argv){
        //argv = "out_path=E:/Export/ARLCL_UWB_Estimations/;rss_db_path=D:/evaluated_uwb-rpi_samples/;scenarios_path=E:/various/Combos/stefi_15_combos.txt;eval_id=3282;seed=3282;end_iter=100;opt_iter=1000;threads=1;cycles=50;ftol=1e-2".split(";");
        SimApp.run(argv);
    }
}
