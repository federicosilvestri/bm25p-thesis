package org.unipi.federicosilvestri.bm25p;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.apache.commons.math3.util.Precision;
import org.terrier.applications.batchquerying.TRECQuerying;
import org.terrier.querying.IndexRef;
import org.terrier.utility.ApplicationSetup;
import org.unipi.federicosilvestri.bm25p.treceval.MyTrecEval;
import org.unipi.federicosilvestri.bm25p.treceval.SerializableMap;

import javax.xml.crypto.Data;
import java.io.File;
import java.util.Arrays;

public class DataCollection {

    /**
     * Registered instance of BM25P
     */
    public static BM25P bm25PwiredInstance = null;

    /**
     * Boolean variable to check if BM25P is registered after one invocation
     * of processQueries.
     */
    public static boolean registeredControl;

    /**
     * Use only for data collection purpose!
     * @param bm25P
     */
    public static void register(BM25P bm25P) {
        if (bm25PwiredInstance == null) {
            bm25PwiredInstance = bm25P;
            logger.info("BM25P Registered!");
        }
    }

    protected static final Logger logger = LoggerFactory.getLogger(DataCollection.class);

    /**
     * Where we want to store the evaluation information (recall, NDCG, ...).
     */
    protected final String evaluationDir;
    /**
     * The file where trec_eval can find the expected results.
     */
    protected final String qrelsFile;
    /**
     * The file where trec_eval can find the calculated results.
     */
    protected final String trecResultsFile;

    /**
     * The directory of NDCG eval.
     */
    protected final File NDCGEvaluationDir;

    /**
     * The directory of recall eval.
     */
    protected final File recallEvaluationDir;

    /**
     * The trec querying object.
     */
    protected TRECQuerying trecQuerying;

    /**
     * Trec evaluation
     */
    protected final MyTrecEval trecEvalEvaluation;

    public DataCollection() {
        evaluationDir = ApplicationSetup.getProperty("org.unipi.federicosilvestri.evaluationDir", "var/eval/");
        qrelsFile = ApplicationSetup.getProperty("org.unipi.federicosilvestri.qrelsFile", null);

        if (qrelsFile == null) {
            throw new NullPointerException("You must set org.unipi.federicosilvestri.qrelsFile paramater in terrier config!");
        }

        IndexRef indexRef = IndexRef.of(ApplicationSetup.TERRIER_INDEX_PATH + "/" + ApplicationSetup.TERRIER_INDEX_PREFIX + ".properties");
        this.trecQuerying = new TRECQuerying(indexRef);
        trecQuerying.intialise();
        this.trecEvalEvaluation = new MyTrecEval(this.qrelsFile);

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

    public void linearCollect() throws Exception {
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
            logger.info("Working on passage " + p);
            SerializableMap ndcgMap = new SerializableMap(NDCGEvaluationDir.getAbsolutePath() + "/passage_" + p + ".csv");
            SerializableMap recallMap = new SerializableMap(recallEvaluationDir.getAbsolutePath() + "/passage_" + p + ".csv");

            for (double w_p = START_W; w_p < END_W; w_p += W_STEP) {
                double w[] = STD_W;
                w[p] = Precision.round(w_p, W_STEP_PRECISION);

                // executing the pipeline
                executeRetrievePipeline(w);

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

    public void executeRetrievePipeline(double w[]) {
        logger.info("# # ");
        logger.info("# # Starting BATCHRETRIEVAL->EVALUATE process");
        logger.info("# # Using w vector = " + Arrays.toString(w));
        logger.info("# # ");

        // updating weights
        if (bm25PwiredInstance == null && !DataCollection.registeredControl) {
            // BM25P classes it's not created yet, setup the properties and invoke query processing!
            ApplicationSetup.setProperty("org.unipi.federicosilvestri.bm25p.w", Arrays.toString(w));
            DataCollection.registeredControl = true;
        } else if (bm25PwiredInstance == null && DataCollection.registeredControl) {
            throw new RuntimeException("This is bad! BM25P class is not registered to DataCollection class!");
        } else {
            bm25PwiredInstance.setWeights(w);
        }

        // retrieve
        trecQuerying.processQueries();
        trecQuerying.close();
    }

    public double getNDCGMeasure() {
        // execute the evaluation
        String[][] result = trecEvalEvaluation.evaluate(trecResultsFile, "ndcg");

        double ndcg = Double.parseDouble(result[0][2]);

        logger.info("# # ");
        logger.info("# # Process BATCHRETRIEVAL->EVALUATE completed");
        logger.info("# # NDCG=" + ndcg);
        logger.info("# # ");

        return ndcg;
    }

    public double getRecallMeasure() {
        // execute the evaluation
        String[][] result = trecEvalEvaluation.evaluate(trecResultsFile, "recall");

        double recall = Double.parseDouble(result[0][2]);

        logger.info("# # ");
        logger.info("# # Process BATCHRETRIEVAL->EVALUATE completed");
        logger.info("# # RECALL=" + recall);
        logger.info("# # ");

        return recall;
    }
}

