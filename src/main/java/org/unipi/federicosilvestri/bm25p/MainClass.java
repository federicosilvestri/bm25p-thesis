package org.unipi.federicosilvestri.bm25p;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.terrier.utility.ApplicationSetup;

import java.net.URL;
import java.util.Arrays;

public class MainClass {

    /**
     * Internal Logger.
     */
    private static final Logger logger = LoggerFactory.getLogger(MainClass.class);

    /**
     * User directory.
     */
    public static final String USER_DIR = System.getProperty("user.dir");

    /**
     * Current wStep
     */
    private double wStep;

    /**
     * Current startW
     */
    private double[] startW;

    /**
     * Current endW
     */
    private double[] endW;


    public static void setupTerrierEnv() {
        System.setProperty("terrier.home", USER_DIR);
        System.setProperty("terrier.etc", USER_DIR + "/etc/");
    }


    private void calculateSearchPartition(int divisions, int processNumber) {
        double newStartW[] = new double[startW.length];
        double newEndW[] = new double[endW.length];

        for (int i = 0; i < startW.length; i++) {
            double spaceSize = (endW[i] - startW[i]) / wStep;
            double spacePartitionSize = spaceSize / divisions; // it must be an integer!
            /*
            if space partition size is not an integer, we must round it to the next integer
            and limit the last iteration of machine. --->
             */

            newStartW[i] = startW[i] + processNumber * wStep * spacePartitionSize;
            newEndW[i] = startW[i] + (processNumber + 1) * wStep * spacePartitionSize;

            // --->
            if (newEndW[i] > endW[i]) {
                logger.info("Caution, space size is not divisible for spacePartitionSize, the load will not be perfectly balanced!");
                newEndW[i] = endW[i];
            }

        }

        // update vectors
        this.startW = newStartW;
        this.endW = newEndW;

        /*
        Setting the application parameters.
         */
        ApplicationSetup.setProperty("org.unipi.federicosilvestri.startW", Arrays.toString(startW));
        ApplicationSetup.setProperty("org.unipi.federicosilvestri.endW", Arrays.toString(endW));
    }

    public static String arrayString(double arr[]) {
        String s = "[";
        for (int i = 0; i < arr.length - 1; i++) {
            s += String.format("%.3f", arr[i]) + ", ";
        }
        s += String.format("%.3f]", arr[arr.length - 1]);

        return s;
    }

    private MainClass(String args[]) {
        {
            String startWString = ApplicationSetup.getProperty("org.unipi.federicosilvestri.startW", null);
            if (startWString == null) {
                throw new IllegalArgumentException("You must configure org.unipi.federicosilvestri.startW!");
            }

            String splitW[] = startWString.replace("[", "").replace("]", "").split(",");
            startW = Arrays.stream(splitW).mapToDouble(Double::parseDouble).toArray();
        }

        {
            String endWString = ApplicationSetup.getProperty("org.unipi.federicosilvestri.endW", null);
            if (endWString == null) {
                throw new IllegalArgumentException("You must configure org.unipi.federicosilvestri.endW!");
            }

            String splitW[] = endWString.replace("[", "").replace("]", "").split(",");
            endW = Arrays.stream(splitW).mapToDouble(Double::parseDouble).toArray();
        }

        {
            String wStepString = ApplicationSetup.getProperty("org.unipi.federicosilvestri.wStep", null);
            if (wStepString == null) {
                throw new IllegalArgumentException("You must configure org.unipi.federicosilvestri.wStep!");
            }

            wStep = Double.parseDouble(wStepString);
        }

        if (args.length != 3) {
            throw new IllegalArgumentException("You must pass search type, divisions and processNumber! Available search types: lings,nlings,incrs");
        }

        int divisions;
        int processNumber;

        try {
            divisions = Integer.parseInt(args[1]);
            processNumber = Integer.parseInt(args[2]);
        } catch (NumberFormatException nfe) {
            throw new RuntimeException("Bad numbers! divisions or process number ar not integers!");
        }

        // logging informations
        logger.info("####### START PARTITIONS INFORMATIONS #######");
        logger.info("------- TOTAL -------");
        logger.info("Total startW=" + arrayString(this.startW));
        logger.info("Total endW=" + arrayString(this.endW));
        logger.info("wStep=" + wStep);
        logger.info("Divisions=" + divisions);

        logger.info("------- CURRENT PARTITION -------");
        calculateSearchPartition(divisions, processNumber);
        logger.info("Process number=" + processNumber);
        logger.info("startW=" + arrayString(this.startW));
        logger.info("endW=" + arrayString(this.endW));

        logger.info("####### END PARTITIONS INFORMATIONS #######\n\n\n");


        SearchAlgorithm sa = null;
        switch (args[0]) {
            case "lings":
                System.out.println("Executing linear Grid Search");
                sa = new GridSearch(startW, endW, wStep, -1, Double.MAX_VALUE);
                break;
            case "nlings":
                System.out.println("Executing non-linear Grid Search");
                sa = new BorderGridSearch(startW, endW, wStep, -1, Double.MAX_VALUE);
                break;
            case "incrs":
                System.out.println("Executing Increment Search");
                sa = new IncreaseSearch(startW, endW, wStep, -1, Double.MAX_VALUE);
                break;
            default:
                throw new IllegalArgumentException("Bad search type!");

        }

        sa.execute();
        System.out.println(sa.getResults());
    }


    public static void main(String args[]) {
        // locate the logging configuration file
        URL resource = MainClass.class.getClassLoader().getResource("logback.xml");

        if (resource == null) {
            throw new RuntimeException("Cannot locate logback.xml!");
        }

        // setup Terrier environment
        setupTerrierEnv();

        new MainClass(args);
    }
}
