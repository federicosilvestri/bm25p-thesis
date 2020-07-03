public class GridSearch extends SearchAlgorithm {
    public static final int[] NDCG_CUTS = new int[]{5, 10};
    protected double[] minEvals;
    protected double[] maxEvals;
    protected double[][] minEvalsw;
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

    protected ArrayList<Double> getValuesFor(int component) {
        ArrayList<Double> arrayList = new ArrayList<>();

        for (double h = minW[component]; h <= maxW[component]; h += wStep) {
            arrayList.add(h);
        }

        return arrayList;
    }

    private int search(int level, Map<Integer, Double> v) {
        if (!hasNextLevel(level, v)) {
            double w[] = constructWVector(v);
            int eval = evaluate(w);

            if (eval < 0 || (maxIterations > 0 && iterations >= maxIterations)) {
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
        updateData(ndcg, w);

        return 1;
    }

    protected void updateData(double[] ndcg, double[] w) {
        for (int i = 0; i < ndcg.length; i++) {
            if (ndcg[i] < this.minEvals[i]) {
                this.minEvals[i] = ndcg[i];
                this.minEvalsw[i] = w;
                
            }
            if (ndcg[i] > this.maxEvals[i]) {
                this.maxEvals[i] = ndcg[i];
                this.maxEvalsw[i] = w;
            }
        }
    }
}
