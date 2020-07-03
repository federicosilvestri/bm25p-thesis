public class IncreaseSearch extends SearchAlgorithm {

    protected static class TrendTape {
        public static final int MIN_ELEMENTS_FOR_TREND = 5;
        private final ArrayList<Double> values;
        private final ArrayList<Double> tape;
        private int preferredElements;
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

        public double[] getFirst() {
            return new double[]{values.get(0), tape.get(0)};
        }

    }
    
    protected double[] currentW;
    protected double currentEval;
    public static final int MAX_INCREMENT_BONUS = 4;
    protected final Map<double[], Double> vectorEvalMap;
    protected final Comparator<Integer> queueComparator;

    public IncreaseSearch(double[] minW, double[] maxW, double wStep, int maxIterations, double maxNDCGToStop) {
        super(minW, maxW, wStep, maxIterations, maxNDCGToStop);
        this.vectorEvalMap = new HashMap<>();
        this.queueComparator = buildQueueComparator();
    }

    protected Comparator<Integer> buildQueueComparator() {
        return (i1, i2) -> (i1 - i2);
    }

    @Override
    public void executeAlgorithm() {
        currentW = new double[minW.length];
        restart();
        PriorityQueue<Integer> q = new PriorityQueue<>(this.minW.length, this.queueComparator);
        LinkedList<Integer> l = new LinkedList<>();

        for (int i = 0; i < minW.length; i++) {
            q.add(i);
        }
        
        permutation(q, l);
    }

    protected void restart() {
        System.arraycopy(this.minW, 0, this.currentW, 0, this.minW.length);
    }

    protected void permutation(Queue<Integer> q, List<Integer> l) {
        if (q.isEmpty()) {
            if (this.iterations < this.maxIterations) {
                search(l);
            } else {
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
        for (double w[] : this.vectorEvalMap.keySet()) {
            double eval = this.vectorEvalMap.get(w);
            super.updateData(eval, w);
        }
    }

    protected void search(List<Integer> permutation) {
        TrendTape trendTape = new TrendTape();
        
        for (Integer currentComponent : permutation) {
            linearIncrement(currentComponent, trendTape);
        }

        double currentW[] = new double[this.currentW.length];
        System.arraycopy(this.currentW, 0, currentW, 0, this.currentW.length);
        vectorEvalMap.put(currentW, currentEval);

        this.computeBestResult();
        restart();
    }

    protected void linearIncrement(int component, TrendTape tape) {
        boolean stop = false;
        int incrementBonus = 0;
        double workingW[] = new double[this.currentW.length];
        double workingEval = -1;
        System.arraycopy(this.currentW, 0, workingW, 0, this.currentW.length);

        for (double w_c = workingW[component]; w_c < this.maxW[component] && !stop; w_c += wStep) {
            super.iterations += 1;
            workingW[component] = w_c;
            super.dataCollection.executeRetrievePipeline(workingW);
            workingEval = super.dataCollection.getEval();
            tape.add(w_c, workingEval);

            if (tape.isTrendReady() || w_c == this.maxW[component] + wStep) {
                int trend = tape.calculateTrend();

                switch (trend) {
                    case -1: {
                        double best[] = tape.getBest();
                        workingW[component] = best[0];
                        workingEval = best[1];
                        stop = true;
                    }
                    break;
                    case 0: {
                        if (incrementBonus < MAX_INCREMENT_BONUS) {
                            incrementBonus += 1;
                        } else {
                            stop = true;
                            double best[] = tape.getBest();
                            workingW[component] = best[0];
                            workingEval = best[1];
                        }
                    }
                    break;
                    case 1:
                        break;
                }
            }
        }

        this.currentW = workingW;
        this.currentEval = workingEval;
        tape.clear();
    }
}
