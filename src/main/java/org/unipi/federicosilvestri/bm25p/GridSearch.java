package org.unipi.federicosilvestri.bm25p;

import java.util.*;

/**
 * This class executes a grid search on discretized vector space w,
 * limited by a minimum and a maximum.
 */
public class GridSearch extends SearchAlgorithm {

    public GridSearch(double[] minW, double[] maxW, double wStep, int maxIterations, double maxNDCGToStop) {
        super(minW, maxW, wStep, maxIterations, maxNDCGToStop);
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
        double ndcg = dataCollection.getEval();

        // updating stats
        super.updateData(ndcg, w);

        // return + to continue, - to stop
        if (ndcg > maxEvalToStop) {
            return -1;
        }

        return 1;
    }
}
