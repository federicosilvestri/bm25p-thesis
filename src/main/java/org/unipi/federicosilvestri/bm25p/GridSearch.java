package org.unipi.federicosilvestri.bm25p;

import java.util.*;

/**
 * This class executes a grid search on discretized vector space w,
 * limited by a minimum and a maximum.
 */
public class GridSearch extends SearchAlgorithm {

    /**
     * NDCG cuts
     */
    public static final int[] NDCG_CUTS = new int[] {5, 10};

    /**
     * Minimum evals
     */
    protected double[] minEvals;

    /**
     * Maximum evals
     */
    protected double[] maxEvals;

    /**
     * Min evals w vector
     */
    protected double[][] minEvalsw;

    /**
     * Max evals w vector
     */
    protected double[][] maxEvalsw;

    public GridSearch(double[] minW, double[] maxW, double wStep, int maxIterations, double maxNDCGToStop) {
        super(minW, maxW, wStep, maxIterations, maxNDCGToStop);
        minEvals = new double[NDCG_CUTS.length];
        maxEvals = new double[NDCG_CUTS.length];

        for (int i = 0; i < NDCG_CUTS.length; i++) {
            minEvals[i] = Double.MAX_VALUE;
            maxEvals[i] = Double.MIN_VALUE;
        }

        minEvalsw = new double[NDCG_CUTS.length][];
        maxEvalsw = new double[NDCG_CUTS.length][];
    }

    @Override
    public void executeAlgorithm() {
        TreeMap<Integer, Double> v = new TreeMap<>();
        search(nextLevel(-1, v), v);
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
        dataCollection.executeRetrievePipeline(w);
        double ndcg[] = dataCollection.getNDCGMeasures(GridSearch.NDCG_CUTS);

        // updating stats
        updateData(ndcg, w);

        return 1;
    }

    protected void updateData(double[] ndcg, double[] w) {
        for (int i = 0; i < ndcg.length; i++) {
            if (ndcg[i] < this.minEvals[i]) {
                this.minEvals[i] = ndcg[i];
                this.minEvalsw[i] = w;
                // super.temporaryResultsWrite();
            }
            if (ndcg[i] > this.maxEvals[i]) {
                this.maxEvals[i] = ndcg[i];
                this.maxEvalsw[i] = w;
                super.temporaryResultsWrite();
            }
        }
    }

    @Override
    public String getResults(String w, String eval) {
        String s = "### Results ###\n";

//        for (int i = 0; i < GridSearch.NDCG_CUTS.length; i++) {
//            s += "MINIMUM NDCG@" + GridSearch.NDCG_CUTS[i];
//            s += "=" + this.minEvals[i] + "\n";
//            s += "w=" + Arrays.toString(this.minEvalsw[i]) + "\n";
//        }
//        s += "- - - - - -\n";
        for (int i = 0; i < GridSearch.NDCG_CUTS.length; i++) {
            s += "MAXIMUM NDCG@" + GridSearch.NDCG_CUTS[i] + "\n";
            s += "eval=" + this.maxEvals[i] + " ";
            s += "w=" + Arrays.toString(this.maxEvalsw[i]) + "\n";
        }

        s += "###\n\n";

        return s;
    }
}
