package org.unipi.federicosilvestri.bm25p;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.terrier.utility.ApplicationSetup;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.*;

/**
 * This class executes a grid search on discretized vector space w,
 * limited by a minimum and a maximum.
 */
public class GridSearch {

    protected static final Logger logger = LoggerFactory.getLogger(GridSearch.class);

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
     * Maximum NDCG to find. If algorithm finds it, it stops, -1 if no stop
     */
    private double maxNDCGToStop;

    /**
     * Max executions to do
     */
    private int maxIterations;

    /**
     * Number of iterations
     */
    private int iterations;

    /**
     * Start time
     */
    private long startTime;

    /**
     * End time
     */
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
        TreeMap<Integer, Double> v = new TreeMap<>();
        search(nextLevel(-1, v), v);
        endTime = System.currentTimeMillis();
    }

    /**
     * Return a stack of possible values for the i-st component
     *
     * @param component
     * @return
     */
    protected ArrayList<Double> getValuesFor(int component) {
        ArrayList<Double> arrayList = new ArrayList<>();

        for (double h = minW[component]; h <= maxW[component]; h += wStep) {
            arrayList.add(h);
        }

        return arrayList;
    }

    private int search(int level, Map<Integer, Double> v) {
        if (!hasNextLevel(level, v)) {
            // we are in a leaf
            double w[] = constructWVector(v);
            int eval = evaluate(w);
            // take a breath, and invoke GC
            System.gc();

            if (eval < 0 || (maxIterations > 0 && iterations >= maxIterations)) {
                // stop the iterations
                return -1;
            }
        } else {
            ArrayList<Double> values = getValuesFor(level);
            for (Double value : values) {
                TreeMap<Integer, Double> newV = new TreeMap<>(v);
                newV.put(level, value);
                int newLevel = nextLevel(level, newV);
                int dir = search(newLevel, newV);

                if (dir < 0) {
                    return -1;
                }
            }
        }

        return 0;
    }

    /**
     * This function is the core of the search, because it represents the function
     * that associates tree depth with vector index.
     *
     * @param level   level of the tree
     * @param weights weigth vector
     * @return
     */
    protected int nextLevel(int level, Map<Integer, Double> weights) {
        int ret = level + 1;


        assert (ret >= 0);
        assert (ret < this.minW.length);
        return ret;
    }

    protected boolean hasNextLevel(int level, Map<Integer, Double> weights) {
        return level < this.minW.length;
    }

    private double[] constructWVector(Map<Integer, Double> v) {
        double w[] = new double[this.minW.length];

        for (Integer key : v.keySet()) {
            Double value = v.get(key);
            w[key] = value;
        }

        return w;
    }

    protected int evaluate(double w[]) {
        iterations += 1;
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

    protected void temporaryResultsWrite() {
        String results = getResults();
        try (BufferedWriter output = new BufferedWriter(new FileWriter(TEMP_FILE_NAME, true))) {
            output.write(results);
        } catch (IOException e) {
            throw new RuntimeException("Cannot write to temporary file!");
        }
    }

    public String getResults(String w, String ndcg) {
        String s = "### Results ###";
        long elapsedTime = 0;
        if (endTime == 0) {
            elapsedTime = System.currentTimeMillis() - startTime;
        } else {
            elapsedTime = endTime - startTime;
        }
        s += "\nNumber of iterations: " + iterations;
        s += "\nElapsed time:" + elapsedTime / 1000 + "s";
        s += "\nMinimum NDCG=" + minNDCG + " with w=" + Arrays.toString(minimizedW);
        s += "\nMaximum NDCG=" + maxNDCG + " with w=" + Arrays.toString(maximizedW);
        s += "\nCurrent w=" + w;
        s += "\nCurrent ndcg=" + ndcg;
        s += "\n### - - - - ###\n";

        return s;
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
        s += "\nElapsed time:" + elapsedTime / 1000 + "s";
        s += "\nMinimum NDCG=" + minNDCG + " with w=" + Arrays.toString(minimizedW);
        s += "\nMaximum NDCG=" + maxNDCG + " with w=" + Arrays.toString(maximizedW);
        s += "\n### - - - - ###\n";

        return s;
    }
}
