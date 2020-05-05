package org.unipi.federicosilvestri.bm25p;

import java.util.Map;

/**
 * This class implements a non linear search starting from borders of
 * vectors. ONLY for passage=10
 */
public class BorderGridSearch extends GridSearch {

    public BorderGridSearch(double[] minW, double[] maxW, double wStep, int maxIterations, double maxNDCGToStop) {
        super(minW, maxW, wStep, maxIterations, maxNDCGToStop);
    }

    @Override
    protected int nextLevel(int level, Map<Integer, Double> weights) {
        switch (level) {
            case -1:
                return 4;
            case 4:
                return 5;
            case 5:
                return 3;
            case 3:
                return 6;
            case 6:
                return 2;
            case 2:
                return 7;
            case 7:
                return 1;
            case 1:
                return 8;
            case 8:
                return 0;
            case 0:
                return 9;
            default:
                return 10;
        }
    }

    @Override
    protected boolean hasNextLevel(int level, Map<Integer, Double> weights) {
        boolean ret = level < 10;

        return ret;
    }
}
