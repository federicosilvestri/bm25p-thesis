package org.unipi.federicosilvestri.bm25p;

import java.security.SecureRandom;
import java.util.*;

public class RandomIncreaseSearch extends IncreaseSearch {
    private static final class PermArray implements Comparable<PermArray> {
        private static final SecureRandom SR = new SecureRandom();

        static final PermArray createRandPerm(int size) {
            assert (size > 0);
            Integer[] inte = new Integer[size];
            Set<Integer> s = new TreeSet<>();

            for (int i = 0; i < size; i++) {
                do {
                    inte[i] = SR.nextInt(size);
                } while(s.contains(inte[i]));
                s.add(inte[i]);
            }

            return new PermArray(inte);
        }

        Integer[] perm;

        private PermArray(Integer perm[]) {
            this.perm = perm;
        }

        @Override
        public boolean equals(Object o) {
            try {
                PermArray p = (PermArray) o;
                return Arrays.toString(p.perm).equals(Arrays.toString(perm));
            } catch (ClassCastException cce) {
                throw new RuntimeException("You have not passed a PermArray object!");
            }
        }

        @Override
        public String toString() {
            return Arrays.toString(this.perm);
        }

        @Override
        public int compareTo(PermArray permArray) {
            return equals(permArray) ? 1 : 0;
        }
    }

    public RandomIncreaseSearch(double[] minW, double[] maxW, double wStep, int maxIterations, double maxNDCGToStop) {
        super(minW, maxW, wStep, maxIterations, maxNDCGToStop);
    }

    @Override
    protected void permutation(Queue<Integer> q, List<Integer> l) {
        HashSet<PermArray> permutations = new HashSet<>();

        for (int i = 0; i < maxIterations; i++) {
            PermArray p;
            do {
                p = PermArray.createRandPerm(minW.length);
            } while (permutations.contains(p));
            permutations.add(p);

            logger.info("Trying with permutation : " + Arrays.toString(p.perm));
            super.search(Arrays.asList(p.perm));
            super.temporaryResultsWrite();

            // add results to container
            this.vectorEvalMap.put(currentW, currentEval);

            // restart
            restart();
            logger.info("Restarting with new permutation!");
        }
    }
}
