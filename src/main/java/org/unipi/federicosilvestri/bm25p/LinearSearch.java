package org.unipi.federicosilvestri.bm25p;

import org.apache.commons.math3.util.Precision;
import org.terrier.applications.CLITool;
import org.terrier.utility.ApplicationSetup;
import org.unipi.federicosilvestri.bm25p.treceval.MyTrecEval;
import org.unipi.federicosilvestri.bm25p.treceval.SerializableMap;

import java.io.File;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Arrays;

public class LinearSearch {

    public static final String USER_DIR = System.getProperty("user.dir");
    public static final File TERRIER_FULL_JAR = new File(USER_DIR + "/modules/assemblies/target/terrier-project-5.2-jar-with-dependencies.jar");
    public static final File LOGBACK_FILE = new File(USER_DIR + "/etc/logback.xml");
    public static final String OUTPUT_DATA_DIR = USER_DIR + "/var/output_data/";

    public static void main(String args[]) throws Exception {
        // loading other classes for terrier
        addToClasspath(TERRIER_FULL_JAR);
        // adding logback
        addToClasspath(LOGBACK_FILE);

        // First we need to setup terrier environment
        setupTerrierEnv();

        // execute the search
        search();
    }

    public static void addToClasspath(File file) {
        try {
            URL url = file.toURI().toURL();

            URLClassLoader classLoader = new URLClassLoader(
                    new URL[] {file.toURI().toURL()},
                    LinearSearch.class.getClassLoader()
            );
            Method method = URLClassLoader.class.getDeclaredMethod("addURL", URL.class);
            method.setAccessible(true);
            method.invoke(classLoader, url);
        } catch (Exception e) {
            throw new RuntimeException("Unexpected exception", e);
        }
    }

    private static void setupTerrierEnv() {
        System.setProperty("terrier.home", USER_DIR);
        System.setProperty("terrier.etc", USER_DIR + "/etc/");
    }

    private static void search() throws Exception {
        final int P = 10;
        final double STD_W[] = new double[]{1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 0.1, 1.0, 1.0};
        final double W_STEP = 1; // the step of search. Each computation executes a sum between w[p] and W_STEP
        final int W_STEP_PRECISION = 1; // the precision of W_STEP
        final double START_W = 1.0;
        final double END_W = 10;

        // check for consistence
        assert(END_W > START_W);
        assert(STD_W.length == P);

       // for each passage we need to change the value in a linspace
        for (int p = 0; p < P; p++) {
            System.out.println("Working on passage " + p);
            SerializableMap ndcgMap = new SerializableMap(OUTPUT_DATA_DIR + "ndcg/passage_" + p + ".csv");
            SerializableMap recallMap = new SerializableMap(OUTPUT_DATA_DIR + "recall/passage_" + p + ".csv");

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

    private static void executeRetrievePipeline(double w[], int passages) {
        System.out.println("# # ");
        System.out.println("# # Starting BATCHRETRIEVE->EVALUATE process");
        System.out.println("# # Using w vector = " + Arrays.toString(w));
        System.out.println("# # ");

        ApplicationSetup.setProperty("org.unipi.federicosilvestri.bm25p.w", Arrays.toString(w));
        ApplicationSetup.setProperty("org.unipi.federicosilvestri.bm25p.p", "" + passages);

        try {
            CLITool.main(new String[]{"batchretrieve"});
        } catch (Exception e) {
            throw new RuntimeException("An exception was thrown by CLITool", e);
        }
    }

    private static double getNDCGMeasure() {
        // execute the evaluation
        MyTrecEval trecEvalEvaluation = new MyTrecEval("share/vaswani_npl/qrels", "ndcg");
        String[][] result = trecEvalEvaluation.evaluate("var/output/results.txt");

        double ndcg = Double.parseDouble(result[0][2]);

        System.out.println("# # ");
        System.out.println("# # Process BATCHRETRIEVE->EVALUATE completed");
        System.out.println("# # NDCG=" + ndcg);
        System.out.println("# # ");

        return ndcg;
    }

    private static double getRecallMeasure() {
        // execute the evaluation
        MyTrecEval trecEvalEvaluation = new MyTrecEval("share/vaswani_npl/qrels", "recall");
        String[][] result = trecEvalEvaluation.evaluate("var/output/results.txt");

        double recall = Double.parseDouble(result[0][2]);

        System.out.println("# # ");
        System.out.println("# # Process BATCHRETRIEVE->EVALUATE completed");
        System.out.println("# # RECALL=" + recall);
        System.out.println("# # ");

        return recall;
    }
}

