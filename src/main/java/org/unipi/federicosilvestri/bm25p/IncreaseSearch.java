package org.unipi.federicosilvestri.bm25p;

import java.util.ArrayList;
import java.util.Arrays;

public class IncreaseSearch extends SearchAlgorithm {

    private static class TrendTape {
        /**
         * Default capacity
         */
        public static final int MIN_ELEMENTS_FOR_TREND = 5;

        /**
         * Associated values (a vector of tested w_c)
         */
        private final ArrayList<Double> values;

        /**
         * The tape array, immutable.
         * It contains the evaluation (e.g. NDCG, recall, precision, ...)
         */
        private final ArrayList<Double> tape;

        /**
         * Minimum elements for trend
         */
        private final int minElementsForTrend;

        TrendTape(int minElementsForTrend) {
            assert (minElementsForTrend > 0);
            this.minElementsForTrend = minElementsForTrend;
            this.values = new ArrayList<>();
            this.tape = new ArrayList<>();
        }

        TrendTape() {
            this(TrendTape.MIN_ELEMENTS_FOR_TREND);
        }

        /**
         * Return true if a trend is ready
         *
         * @param w    weight vector component
         * @param eval evaluation to insert
         * @return true if a trend is ready, false if not
         */
        void add(Double w, Double eval) {
            values.add(w);
            tape.add(eval);
        }

        boolean isTrendReady() {
            return this.values.size() >= this.minElementsForTrend;
        }

        void clear() {
            tape.clear();
            values.clear();
        }

        int calculateTrend() {
            // first we need to calculate the difference vector
            double[] dv = new double[tape.size()];

            for (int i = 1; i < tape.size(); i++) {
                dv[i] = Math.signum(tape.get(i) - tape.get(i - 1));
            }

//            logger.info("DV = " + Arrays.toString(dv));

            double totalDv = dv[0];
            for (int i = 1; i < dv.length; i++) {
                totalDv += dv[i];
            }

            return (int) Math.signum(totalDv);
        }


        /**
         * It searches the best evaluation and returns
         * the associated value.
         *
         * @return double array that contains in the first position the value and in the second the evaluation.
         */
        public double[] getBest() {
            double maxEval = tape.get(0);
            int index = 0;

            for (int i = 1; i < tape.size(); i++) {
                if (tape.get(i) > maxEval) {
                    maxEval = tape.get(i);
                    index = i;
                }
            }

            return new double[]{values.get(index), maxEval};
        }

        /**
         * Get the first elements in the tape.
         *
         * @return an array that contains in the first position the value and in the second the eval.
         */
        public double[] getFirst() {
            return new double[]{values.get(0), tape.get(0)};
        }

    }

    /**
     * Current w vector.
     */
    private double[] currentW;

    /**
     * Current eval.
     */
    private double currentEval;

    /**
     * The maximum bonus increments to give in case of stall situation.
     */
    public static final int MAX_INCREMENT_BONUS = 4;

    public IncreaseSearch(double[] minW, double[] maxW, double wStep, int maxIterations, double maxNDCGToStop) {
        super(minW, maxW, wStep, maxIterations, maxNDCGToStop);
    }

    @Override
    public void executeAlgorithm() {
        currentW = new double[minW.length];
        System.arraycopy(this.minW, 0, this.currentW, 0, this.minW.length);
        search(-1);
    }

    /**
     * Get the next component to iterate, given a cardinal index.
     * -1 is the starter index.
     *
     * @param index iteration number (cardinal index)
     * @return the next cardinal index
     */
    protected int getNextComponent(int index) {
        return index + 1;
    }

    protected boolean hasNextComponent(int component) {
        return component < minW.length - 1;
    }

    private void search(int currentComponent) {
        // create a tape for monitor the trend of eval measure
        TrendTape trendTape = new TrendTape();

        // choose a component to start
        while (hasNextComponent(currentComponent)) {
            // get the component
            currentComponent = getNextComponent(currentComponent);

            logger.info("-----> Iterating the component " + currentComponent);

            // start by incrementing it
            linearIncrement(currentComponent, trendTape);

            logger.info("-----> Component " + currentComponent + " iteration finished!");
            logger.info("##### -> Current eval is= [   " + currentEval + "   ]");
        }
    }

    private void linearIncrement(int component, TrendTape tape) {
        // linear incrementation
        boolean stop = false;
        int incrementBonus = 0;

        for (double w_c = this.currentW[component]; w_c < this.maxW[component] && !stop; w_c += wStep) {
            /*
            calculate the evaluation with current vector,
            by executing the pipeline and invoking trec_eval.
             */
            this.currentW[component] = w_c;
            super.dataCollection.executeRetrievePipeline(this.currentW);
            double eval = super.dataCollection.getNDCGMeasure();

            // update the data
            super.updateData(eval, this.currentW);

            tape.add(w_c, eval);
            if (tape.isTrendReady()) {
                // calculate the trend
                int trend = tape.calculateTrend();

//                logger.info("Trend is " + trend);

                switch (trend) {
                    case -1: {
                        // we need to rollback to a positive value
                        double best[] = tape.getBest();
                        this.currentW[component] = best[0];
                        this.currentEval = best[1];
                    }
                    break;
                    case 0: {
                        /* we have no benefits, we have to choose to stop or continue the incrementation.
                         In this implementation we give a possibility to continue the incrementation and stops later.
                         */
                        if (incrementBonus < MAX_INCREMENT_BONUS) {
                            incrementBonus += 1;
                        } else {
                            // stop the incrementation
                            stop = true;

                            /*
                            we have two choices:
                            1. keep the current value for the component
                            2. rollback to the first minimum value that maximize the eval

                            implementation choice = 2
                             */
                            double best[] = tape.getBest();
                            this.currentW[component] = best[0];
                            this.currentEval = best[1];
                        }
                    }
                    break;
                    case 1:
                        // we have a positive response, continue the incrementation
                        break;
                }
            }
        }

        // clear the tape!
        tape.clear();
    }

}
