package org.unipi.federicosilvestri.bm25p;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Arrays;

public abstract class SearchAlgorithm {
    /**
     * Internal logger.
     */
    protected static final Logger logger = LoggerFactory.getLogger(SearchAlgorithm.class);

    /**
     * Temporary results filename.
     */
    public static String TEMP_FILE_NAME = "tempResults.txt";

    /**
     * Final results
     */
    public static String FINAL_RESULTS_FILE_NAME = "finalResults.txt";

    /**
     * Data collection object.
     */
    protected final DataCollection dataCollection;

    /**
     * Minimal weight vector
     */
    protected final double minW[];

    /**
     * Maximum weight vector
     */
    protected final double maxW[];

    /**
     * The step for each component
     */
    protected final double wStep;

    /**
     * Minimum Eval
     */
    private double minEval;

    /**
     * Maximum Eval
     */
    private double maxEval;

    /**
     * The w vector with the minimum Eval
     */
    private double[] minimizedW;

    /**
     * The w vector with the maximum Eval
     */
    private double[] maximizedW;

    /**
     * Maximum Eval to find. If algorithm finds it, it stops, -1 if no stop
     */
    protected final double maxEvalToStop;

    /**
     * Max executions to do
     */
    protected final long maxIterations;

    /**
     * Number of iterations
     */
    protected long iterations;

    /**
     * Start time
     */
    protected long startTime;

    /**
     * End time
     */
    private long endTime;

    public SearchAlgorithm(double minW[], double maxW[], double wStep, long maxIterations, double maxEvalToStop) {
        this.minW = minW;
        this.maxW = maxW;
        this.wStep = wStep;
        this.dataCollection = new DataCollection();
        this.minEval = Double.MAX_VALUE;
        this.maxEval = Double.MIN_VALUE;
        this.maxIterations = maxIterations;
        this.maxEvalToStop = maxEvalToStop;

        if (minW.length != maxW.length) {
            throw new IllegalArgumentException("The length of minW vector must be the same of maxW vector!");
        }

        {
            boolean check = false;
            for (int i = 0; i < minW.length && !check; i++) {
                if (minW[i] != maxW[i]) {
                    check = true;
                }
            }
            if (!check) {
                throw new IllegalArgumentException("The startW vector is equal to endW vector!");
            }
        }
    }

    public final void execute() {
        iterations = 0;
        startTime = System.currentTimeMillis();
        executeAlgorithm();
        endTime = System.currentTimeMillis();
    }

    protected abstract void executeAlgorithm();

    protected void temporaryResultsWrite() {
        String results = getResults();
        try (BufferedWriter output = new BufferedWriter(new FileWriter(TEMP_FILE_NAME, true))) {
            output.write(results);
        } catch (IOException e) {
            throw new RuntimeException("Cannot write to temporary file!");
        }
    }

    protected abstract String getFinalResults();

    public final void writeFinalResults() {
        String finalResults = getFinalResults();

        try (BufferedWriter output = new BufferedWriter(new FileWriter(FINAL_RESULTS_FILE_NAME, false))) {
            output.write(finalResults);
        } catch (IOException e) {
            throw new RuntimeException("Cannot write to temporary file!");
        }
    }

    public String getResults(String w, String eval) {
        String s = "### Results ###";
        long elapsedTime = 0;
        if (endTime == 0) {
            elapsedTime = System.currentTimeMillis() - startTime;
        } else {
            elapsedTime = endTime - startTime;
        }
        s += "\nNumber of iterations: " + iterations;
        s += "\nElapsed time:" + elapsedTime / 1000 + "s";
        s += "\nMinimum Eval=" + minEval + " with w=" + Arrays.toString(minimizedW);
        s += "\nMaximum Eval=" + maxEval + " with w=" + Arrays.toString(maximizedW);
        s += "\nCurrent w=" + w;
        s += "\nCurrent Eval=" + eval;
        s += "\n### - - - - ###\n";

        return s;
    }

    public String getResults() {
        return getResults("[NOT-GIVEN]", "[NOT-GIVEN]");
    }

    /**
     * Update the internal statistics.
     *
     * @param eval calculated eval
     * @param w current weight vector
     */
    protected void updateData(double eval, double w[]) {
        if (eval < minEval) {
            minEval = eval;
            minimizedW = w;
            temporaryResultsWrite();
        }
        if (eval > maxEval) {
            maxEval = eval;
            maximizedW = w;
            temporaryResultsWrite();
        }
    }



}
