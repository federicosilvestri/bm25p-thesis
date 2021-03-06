package org.unipi.federicosilvestri.bm25p;

import java.util.*;

/**
 * With this algorithm it's not possible
 * to parallelize the work using the vector space subdivision.
 * You need to use the permutation space division instead.
 */
public class IncreaseSearch extends SearchAlgorithm {

    protected static class TrendTape {
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
         * The number of elements to store before predict
         */
        private int preferredElements;

        /**
         * Minimum elements for trend
         */
        private final int minElementsForTrend;

        TrendTape(int minElementsForTrend) {
            assert (minElementsForTrend > 0);
            this.minElementsForTrend = minElementsForTrend;
            this.values = new ArrayList<>();
            this.tape = new ArrayList<>();
            this.preferredElements = minElementsForTrend;
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
            return (this.tape.size() % preferredElements) == 0;
        }

        void setPreferredElements(int preferredElements) {
            assert (preferredElements > 0);
            this.preferredElements = preferredElements;
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
    protected double[] currentW;

    /**
     * Current eval.
     */
    protected double currentEval;

    /**
     * The maximum bonus increments to give in case of stall situation.
     */
    public static final int MAX_INCREMENT_BONUS = 4;

    /**
     * It maintains the association between w vector and eval.
     */
    protected final Map<double[], Double> vectorEvalMap;

    /**
     * The comparator of Queue elements used for permutations.
     */
    protected final Comparator<Integer> queueComparator;

    public IncreaseSearch(double[] minW, double[] maxW, double wStep, int maxIterations, double maxNDCGToStop) {
        super(minW, maxW, wStep, maxIterations, maxNDCGToStop);
        /*
        il numero di step necessari per eseguire tutto l'algoritmo è di O(n!). Quindi è altissimo, nel nostro caso 10!
        è ineseguibile, direi. Ma possiamo ridurlo a (10-k)! dove k è il numero di elementi centrali da rimuovere.
        Per esempio per fare una ricerca sui bordi "spessi" 3 si riduce a 6!.

        L'algoritmo è deterministico, ma non è facile capire cosa faccia a causa del suo dinamismo del trend, cioè
        in base a come NDCG si comporta in funzione dei pesi cercati w.
         */
        this.vectorEvalMap = new HashMap<>();
        this.queueComparator = buildQueueComparator();
    }

    /**
     * Build the comparator for the queue.
     * It allows you to create a different visit type of permutation tree.
     * Example: you can create a DFS or BFS visits.
     *
     * @return comparator between two integers
     */
    protected Comparator<Integer> buildQueueComparator() {
        return (i1, i2) -> (i1 - i2);
    }

    @Override
    public void executeAlgorithm() {
        currentW = new double[minW.length];
        restart();
        PriorityQueue<Integer> q = new PriorityQueue<>(this.minW.length, this.queueComparator);
        LinkedList<Integer> l = new LinkedList<>();

        // adding all indexes to queue
        for (int i = 0; i < minW.length; i++) {
            // here we can set the priority for index
            q.add(i);
        }

        // execute the initial permutations
        permutation(q, l);
    }

    /**
     * This function restarts the current
     * vector to initial vector. Use it to not break data encapsulation.
     */
    protected void restart() {
        System.arraycopy(this.minW, 0, this.currentW, 0, this.minW.length);
    }

    protected void permutation(Queue<Integer> q, List<Integer> l) {
        if (q.isEmpty()) {
            /*
            We are in a leaf!
            execute the search with this permutation of array
             */
            if (this.iterations < this.maxIterations) {
                search(l);
            } else {
                // we have to return!
                return;
            }
        } else {
            Iterator<Integer> it = q.iterator();
            while (it.hasNext()) {
                Integer cit = it.next();
                PriorityQueue<Integer> q1 = new PriorityQueue<>(this.minW.length - l.size(), this.queueComparator);
                q1.addAll(q);
                q1.remove(cit);
                LinkedList<Integer> l1 = new LinkedList<>(l);
                l1.add(cit);
                permutation(q1, l1);
            }
        }
    }

    protected void computeBestResult() {
        /*
        we have to choose the best association between eval and v
         */

        for (double w[] : this.vectorEvalMap.keySet()) {
            double eval = this.vectorEvalMap.get(w);
            // updating
            super.updateData(eval, w);
        }
    }

    protected void search(List<Integer> permutation) {
        logger.info("Executing the search on permutation: " + Arrays.toString(permutation.toArray()));

        // create a tape for monitor the trend of eval measure
        TrendTape trendTape = new TrendTape();

        // choose a component to start
        for (Integer currentComponent : permutation) {
            logger.debug("-----> Iterating the component " + currentComponent);

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

    protected void linearIncrement(int component, TrendTape tape) {
        // linear incrementation
        boolean stop = false;
        int incrementBonus = 0;
        double workingW[] = new double[this.currentW.length];
        double workingEval = -1;
        System.arraycopy(this.currentW, 0, workingW, 0, this.currentW.length);

        for (double w_c = workingW[component]; w_c < this.maxW[component] && !stop; w_c += wStep) {
            super.iterations += 1;
            /*
            calculate the evaluation with current vector,
            by executing the pipeline and invoking trec_eval.
             */
            workingW[component] = w_c;
            super.dataCollection.executeRetrievePipeline(workingW);
            workingEval = super.dataCollection.getEval();
            tape.add(w_c, workingEval);

            if (tape.isTrendReady() || w_c == this.maxW[component] + wStep) {
                // calculate the trend
                int trend = tape.calculateTrend();
                logger.debug("Trend is ready, it's=" + trend);

                switch (trend) {
                    case -1: {
                        // we need to rollback to a positive value
                        double best[] = tape.getBest();
                        workingW[component] = best[0];
                        workingEval = best[1];
                        stop = true;
                        logger.debug("TREND DECISION: STOOP :(");
                    }
                    break;
                    case 0: {
                        /* we have no benefits, we have to choose to stop or continue the incrementation.
                         In this implementation we give a possibility to continue the incrementation and stops later
                         by using a bonus.
                         */
                        if (incrementBonus < MAX_INCREMENT_BONUS) {
                            incrementBonus += 1;
                            logger.debug("TREND DECISION: CONTINUE WITH BONUS :)");
                        } else {
                            // stop the incrementation
                            stop = true;
                            logger.debug("TREND DECISION: STOP, NO BONUS LEFT :(");
                            /*
                            we have two choices:
                            1. keep the current value for the component
                            2. rollback to the first minimum value that maximize the eval

                            implementation choice = 2
                             */
                            double best[] = tape.getBest();
                            workingW[component] = best[0];
                            workingEval = best[1];
                        }
                    }
                    break;
                    case 1:
                        logger.debug("TREND DECISION: CONTINUE :)");
                        // we have a positive response, continue the incrementation
                        break;
                    default:
                        throw new UnsupportedOperationException("You cannot pass a value != {-1, 0, 1}!");
                }
            }
        }

        // now we can update the object values
        this.currentW = workingW;
        this.currentEval = workingEval;

        updateData(workingEval, workingW);

        // clear the tape!
        tape.clear();
    }

    @Override
    public String getResults() {
        String s = "##### BEGIN INCR SEARCH CURRENT RESULTS ##### \n";

        s += "Iterations=" + super.iterations + "\n";
        s += "startW=" + Arrays.toString(super.minW) + "\n";
        s += "endW=" + Arrays.toString(super.maxW) + "\n";
        s += "wStep=" + super.wStep + "\n";
        s += "Eval NDCG cut=" + DataCollection.DEFAULT_NDCG_CUT + "\n";
        s += "Current Eval=" + this.currentEval + "\n";
        s += "Current w=" + Arrays.toString(this.currentW) + "\n";

        s += "##### END INCR SEARCH CURRENT RESULTS #####\n\n";
        return s;
    }

}
