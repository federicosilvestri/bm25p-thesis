package org.unipi.federicosilvestri.bm25p;

import java.util.Arrays;

public class UnmanagedSearch extends CustomIncreaseSearch {
    public UnmanagedSearch(double[] minW, double[] maxW, double wStep, int maxIterations, double maxNDCGToStop) {
        super(minW, maxW, wStep, maxIterations, maxNDCGToStop);
    }

    @Override
    protected void linearIncrement(int component, IncreaseSearch.TrendTape tape) {
        /*
         linear incrementation without trend function, this is similar to Grid Search.
         */
        for (double w_c = this.currentW[component]; w_c < this.maxW[component]; w_c += wStep) {
            super.iterations += 1;
            /*
            calculate the evaluation with current vector,
            by executing the pipeline and invoking trec_eval.
             */
            this.currentW[component] = w_c;
            logger.debug(Arrays.toString(this.currentW));
            super.dataCollection.executeRetrievePipeline(this.currentW);
            this.currentEval = super.dataCollection.getEval();

            // update the data
            super.updateData(this.currentEval, this.currentW);
        }
    }
}
