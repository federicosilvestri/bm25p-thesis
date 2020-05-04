package org.unipi.federicosilvestri.bm25p;

import org.apache.commons.math3.util.Precision;
import org.terrier.applications.batchquerying.TRECQuerying;
import org.terrier.utility.ApplicationSetup;
import org.unipi.federicosilvestri.bm25p.treceval.MyTrecEval;
import org.unipi.federicosilvestri.bm25p.treceval.SerializableMap;

import java.io.File;
import java.util.Arrays;

public class LinearSearch {

    /**
     * Where we want to store the evaluation information (recall, NDCG, ...).
     */
    private final String evaluationDir;
    /**
     * The file where trec_eval can find the expected results.
     */
    private final String qrelsFile;
    /**
     * The file where trec_eval can find the calculated results.
     */
    private final String trecResultsFile;

    /**
     * The directory of NDCG eval.
     */
    private final File NDCGEvaluationDir;

    /**
     * The directory of recall eval.
     */
    private final File recallEvaluationDir;

    public LinearSearch() {
        evaluationDir = ApplicationSetup.getProperty("org.unipi.federicosilvestri.evaluationDir", "var/eval/");
        qrelsFile = ApplicationSetup.getProperty("org.unipi.federicosilvestri.qrelsFile", null);

        if (qrelsFile == null) {
            throw new NullPointerException("You must set org.unipi.federicosilvestri.qrelsFile paramater in terrier config!");
        }

        String resultDir = ApplicationSetup.getProperty("trec.results", null);
        String resultFile = ApplicationSetup.getProperty("trec.results.file", null);

        trecResultsFile = "var/" + resultDir + resultFile;

        // creating output directories
        File evaluationDirFile = new File((evaluationDir));
        evaluationDirFile.mkdir();

        NDCGEvaluationDir = new File(evaluationDir + "ndcg/");
        recallEvaluationDir = new File(evaluationDir + "recall/");
        NDCGEvaluationDir.mkdir();
        recallEvaluationDir.mkdir();
    }

    public void search() throws Exception {
        final int P = 10;
        final double STD_W[] = new double[]{1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 0.1, 1.0, 1.0};
        final double W_STEP = 1; // the step of search. Each computation executes a sum between w[p] and W_STEP
        final int W_STEP_PRECISION = 1; // the precision of W_STEP
        final double START_W = 1.0;
        final double END_W = 10;

        // check for consistence
        assert (END_W > START_W);
        assert (STD_W.length == P);

        // for each passage we need to change the value in a linspace
        for (int p = 0; p < P; p++) {
            System.out.println("Working on passage " + p);
            SerializableMap ndcgMap = new SerializableMap(NDCGEvaluationDir.getAbsolutePath() + "/passage_" + p + ".csv");
            SerializableMap recallMap = new SerializableMap(recallEvaluationDir.getAbsolutePath() + "/passage_" + p + ".csv");

            for (double w_p = START_W; w_p < END_W; w_p += W_STEP) {
                double w[] = STD_W;
                w[p] = Precision.round(w_p, W_STEP_PRECISION);

                // executing the pipeline
                executeRetrievePipeline(w, P);

                // getting measures
                double ndcg_eval = getNDCGMeasure();
                double recall = getRecallMeasure();

                ndcgMap.put(w[p], ndcg_eval);
                recallMap.put(w[p], recall);
            }

            // write specified file with the couple w_p, ndcg
            ndcgMap.write();
            // same for recall
            recallMap.write();
        }
    }

    private void executeRetrievePipeline(double w[], int passages) {
        System.out.println("# # ");
        System.out.println("# # Starting BATCHRETRIEVAL->EVALUATE process");
        System.out.println("# # Using w vector = " + Arrays.toString(w));
        System.out.println("# # ");

        ApplicationSetup.setProperty("org.unipi.federicosilvestri.bm25p.w", Arrays.toString(w));
        ApplicationSetup.setProperty("org.unipi.federicosilvestri.bm25p.p", "" + passages);

        // retrieve
        TRECQuerying trecQuerying = new TRECQuerying();
        trecQuerying.intialise();
        trecQuerying.processQueries();
    }

    private double getNDCGMeasure() {
        // execute the evaluation
        MyTrecEval trecEvalEvaluation = new MyTrecEval(this.qrelsFile, "ndcg");
        String[][] result = trecEvalEvaluation.evaluate(trecResultsFile);

        double ndcg = Double.parseDouble(result[0][2]);

        System.out.println("# # ");
        System.out.println("# # Process BATCHRETRIEVAL->EVALUATE completed");
        System.out.println("# # NDCG=" + ndcg);
        System.out.println("# # ");

        return ndcg;
    }

    private double getRecallMeasure() {
        // execute the evaluation
        MyTrecEval trecEvalEvaluation = new MyTrecEval(this.qrelsFile, "recall");
        String[][] result = trecEvalEvaluation.evaluate(trecResultsFile);

        double recall = Double.parseDouble(result[0][2]);

        System.out.println("# # ");
        System.out.println("# # Process BATCHRETRIEVAL->EVALUATE completed");
        System.out.println("# # RECALL=" + recall);
        System.out.println("# # ");

        return recall;
    }
}

