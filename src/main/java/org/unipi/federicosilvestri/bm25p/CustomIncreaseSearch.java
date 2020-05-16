package org.unipi.federicosilvestri.bm25p;

import java.util.*;

/**
 * This class is a customized increase search because the method
 * permutation allow you to set a specific permutation of w vector components.
 */
public class CustomIncreaseSearch extends IncreaseSearch {

    protected final Set<Integer[]> permutations;

    public CustomIncreaseSearch(double[] minW, double[] maxW, double wStep, int maxIterations, double maxNDCGToStop) {
        super(minW, maxW, wStep, maxIterations, maxNDCGToStop);
        permutations = new HashSet<>();
        permutations.add(new Integer[] {0,1,2,3,4,5,6,7,8,9});
        permutations.add(new Integer[] {9,8,7,6,5,4,3,2,1,0});
        permutations.add(new Integer[] {0,9,1,8,2,7,3,6,4,5});
        permutations.add(new Integer[] {6,5,7,4,8,3,9,2,1,0});
    }

    @Override
    protected void permutation(Queue<Integer> q, List<Integer> l) {
        for (Integer p[] : permutations) {
            logger.info("####### -> Start another permutation <- #######");
            logger.info("Permutation : " + Arrays.toString(p));

            super.search(Arrays.asList(p));
            logger.debug("CurrentW = " + Arrays.toString(this.currentW));
        }
    }
}
