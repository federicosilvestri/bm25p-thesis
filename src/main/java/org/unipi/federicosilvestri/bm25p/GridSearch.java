package org.unipi.federicosilvestri.bm25p;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.terrier.utility.ApplicationSetup;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

/**
 * This class executes a grid search on discretized vector space w,
 * limited by a minimum and a maximum.
 */
public final class GridSearch {

    protected static final Logger logger = LoggerFactory.getLogger(GridSearch.class);

    public static void main(String args[]) {
        MainClass.setupTerrierEnv();

        double startW[];
        {
            String startWString = ApplicationSetup.getProperty("org.unipi.federicosilvestri.startW", null);
            if (startWString == null) {
                throw new IllegalArgumentException("You must configure org.unipi.federicosilvestri.startW!");
            }

            String splitW[] = startWString.replace("[", "").replace("]", "").split(",");
            startW = Arrays.stream(splitW).mapToDouble(Double::parseDouble).toArray();
        }

        double endW[];
        {
            String endWString = ApplicationSetup.getProperty("org.unipi.federicosilvestri.endW", null);
            if (endWString == null) {
                throw new IllegalArgumentException("You must configure org.unipi.federicosilvestri.endW!");
            }

            String splitW[] = endWString.replace("[", "").replace("]", "").split(",");
            endW = Arrays.stream(splitW).mapToDouble(Double::parseDouble).toArray();
        }

        double wStep;
        {
            String wStepString = ApplicationSetup.getProperty("org.unipi.federicosilvestri.wStep", null);
            if (wStepString == null) {
                throw new IllegalArgumentException("You must configure org.unipi.federicosilvestri.wStep!");
            }

            wStep = Double.parseDouble(wStepString);
        }

        GridSearch gs = new GridSearch(startW, endW, wStep, -1, Double.MAX_VALUE);

        logger.info("Starting a new GridSearch with:");
        logger.info("startW=" + Arrays.toString(startW));
        logger.info("endW=" + Arrays.toString(endW));
        logger.info("wStep=" + wStep);
        logger.info("maxIterations=" + (-1));
        logger.info("MaxNDCGToStop=" + Double.MAX_VALUE);

        gs.execute();

        logger.info(gs.getResults());
    }

    public static String TEMP_FILE_NAME = "tempResults.txt";

    private final int PASSAGES = 10;

    /**
     * Data collection object.
     */
    private final DataCollection dataCollection;

    /**
     * Minimal weight vector
     */
    private final double minW[];

    /**
     * Maximum weight vector
     */
    private final double maxW[];

    /**
     * The step for each component
     */
    private final double wStep;

    /**
     * Default start
     */
    public static final double MIN_W[] = new double[]{0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0};

    /**
     * Default end
     */
    public static final double MAX_W[] = new double[]{1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0};

    /**
     * Default w_step
     */
    public static final double W_STEP = 0.5;

    /**
     * Minimum NDCG
     */
    private double minNDCG;

    /**
     * Maximum NDCG
     */
    private double maxNDCG;

    /**
     * The w vector with the minimum NDCG
     */
    private double[] minimizedW;

    /**
     * The w vector with the maximum NDCG
     */
    private double[] maximizedW;

    /**
     * Maximum NDCG to find. If algorithm finds it, it stops, -1 if no stop.
     */
    private double maxNDCGToStop;

    /**
     * Max executions to do.
     */
    private int maxIterations;

    /**
     * Number of iterations
     */
    private int iterations;

    private long startTime;
    private long endTime;



    public GridSearch(double minW[], double maxW[], double wStep, int maxIterations, double maxNDCGToStop) {
        this.minW = minW;
        this.maxW = maxW;
        this.wStep = wStep;
        this.dataCollection = new DataCollection();
        this.minNDCG = Double.MAX_VALUE;
        this.maxNDCG = Double.MIN_VALUE;
        this.maxIterations = maxIterations;
        this.maxNDCGToStop = maxNDCGToStop;

        if (minW.length != maxW.length) {
            throw new IllegalArgumentException("The length of minW vector must be the same of maxW vector!");
        }
    }

    public GridSearch() {
        this(MIN_W, MAX_W, W_STEP, -1, Double.MAX_VALUE);
    }

    public void execute() {
        iterations = 0;
        startTime = System.currentTimeMillis();
        search(0, new LinkedList<>());
        endTime = System.currentTimeMillis();
    }

    /**
     * Return a stack of possible values for the i-st component
     *
     * @param component
     * @return
     */
    private List<Double> getValuesFor(int component) {
        LinkedList<Double> stack = new LinkedList<>();

        for (double h = minW[component]; h <= maxW[component]; h += wStep) {
            stack.add(h);
        }

        return stack;
    }

    private int search(int level, List<Double> v) {
        iterations += 1;

        if (level >= minW.length) {
            // we are in a leaf
            double w[] = constructWVector(v);
            int eval = evaluate(w);

            if (eval < 0 || (maxIterations > 0 && iterations >= maxIterations)) {
                // stop the iterations
                return -1;
            }

            // take a breath, and invoke GC
            System.gc();
            v = null;
        } else {
            List<Double> values = getValuesFor(level);
            for (Double value : values) {
                LinkedList<Double> newV = new LinkedList<>(v);
                newV.add(value);
                int dir = search(level + 1, newV);

                if (dir < 0) {
                    return -1;
                }
            }
        }

        return 0;
    }

    private double[] constructWVector(List<Double> v) {
        double w[] = new double[v.size()];

        for (int i = 0; i < v.size(); i++) {
            w[i] = v.get(i);
        }

        return w;
    }

    private int evaluate(double w[]) {
        dataCollection.executeRetrievePipeline(w, PASSAGES);
        double ndcg = dataCollection.getNDCGMeasure();

        // updating min,max
        if (ndcg > maxNDCG) {
            maxNDCG = ndcg;
            maximizedW = w;
            temporaryResultsWrite();
        }
        if (ndcg < minNDCG) {
            minNDCG = ndcg;
            minimizedW = w;
            temporaryResultsWrite();
        }

        // return + to continue, - to stop
        if (ndcg > maxNDCGToStop) {
            return -1;
        }

        return 1;
    }

    private void temporaryResultsWrite() {
        String results = getResults();
        try (BufferedWriter output = new BufferedWriter(new FileWriter(TEMP_FILE_NAME, true))){
            output.write(results);
        } catch (IOException e) {
            throw new RuntimeException("Cannot write to temporary file!");
        }
    }

    public String getResults() {
        String s = "### Results ###";
        long elapsedTime = 0;
        if (endTime == 0) {
            elapsedTime = System.currentTimeMillis() - startTime;
        } else {
            elapsedTime = endTime - startTime;
        }
        s += "\nNumber of iterations: " + iterations;
        s += "\nElapsed time:" + elapsedTime/1000 + "s";
        s += "\nMinimum NDCG=" + minNDCG + " with w=" + Arrays.toString(minimizedW);
        s += "\nMaximum NDCG=" + maxNDCG + " with w=" + Arrays.toString(maximizedW);
        s += "\n### - - - - ###\n";

        return s;
    }

}
