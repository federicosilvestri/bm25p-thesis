package org.unipi.federicosilvestri.bm25p;

import java.security.SecureRandom;
import java.util.Arrays;
import java.util.List;
import java.util.Queue;

public class LineSearchRandomRestart extends IncreaseSearch {

    /**
     * Secure random for component generation.
     */
    protected static final SecureRandom SR = new SecureRandom();

    public LineSearchRandomRestart(double[] minW, double[] maxW, double wStep, int maxIterations, double maxNDCGToStop) {
        super(minW, maxW, wStep, maxIterations, maxNDCGToStop);
    }

    @Override
    protected void permutation(Queue<Integer> q, List<Integer> l) {
        /**
         * Simple method, execute the algorithm maxIterations times,
         * because the search function is non-deterministic.
         */
        l.addAll(q);

        for (int i = 0; i < maxIterations; i++) {
            search(l);
        }
    }

    @Override
    protected void search(List<Integer> permutation) {
        // create a tape for monitor the trend of eval measure
        TrendTape trendTape = new TrendTape();

        // choose a component to start
        for (Integer currentComponent : permutation) {
            logger.debug("-----> Iterating the component " + currentComponent);

            for (int i = 0; i < permutation.size(); i++) {
                if (i != currentComponent) {
                    this.currentW[i] = Math.abs(SR.nextDouble());
                }
            }

            // start by incrementing it
            linearIncrement(currentComponent, trendTape);

            logger.debug("-----> Component " + currentComponent + " iteration finished!");
            logger.info("##### -> Current w is= " + Arrays.toString(currentW));
            logger.info("##### -> Current eval is= [   " + currentEval + "   ]");

        }

        // add to to evalMap
        double currentW[] = new double[this.currentW.length];
        System.arraycopy(this.currentW, 0, currentW, 0, this.currentW.length);
        vectorEvalMap.put(currentW, currentEval);

        /*
         search for this permutation is finished, so we need to calculate the best results
         with others
         */

        // calculating best results
        this.computeBestResult();

        logger.debug("Clean up restarting with the new permutation");
        restart();
    }
}
