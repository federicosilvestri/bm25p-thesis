package org.unipi.federicosilvestri.bm25p.treceval;

import com.google.common.collect.Sets;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.terrier.applications.CLITool;
import org.terrier.evaluation.TrecEvalEvaluation;
import org.terrier.utility.ArrayUtils;
import org.terrier.utility.Files;
import org.terrier.utility.Rounding;
import uk.ac.gla.terrier.jtreceval.trec_eval;

public class MyTrecEval {
    protected static final Logger logger = LoggerFactory.getLogger(TrecEvalEvaluation.class);
    String qrels;
    protected String[][] output;
    protected String additional_args[];
    String resFile;

    public MyTrecEval(String[] qrels) {
        this.qrels = qrels[0];
        if (qrels.length != 1) {
            throw new IllegalArgumentException("Only one qrels file can be specified");
        } else if (!isPlatformSupported()) {
            throw new UnsupportedOperationException("Your platform is not currently supported by jtreceval");
        }
    }

    public MyTrecEval(String qrels) {
        this.qrels = qrels;
    }

    public MyTrecEval(String qrels, String measure) {
        this(qrels);
        this.additional_args = new String[] {"-m", measure};
    }

    public String[][] evaluate(String resultFilename) {
        logger.info("Evaluating result file: " + resultFilename);
        String[] args = new String[]{this.qrels, this.resFile = resultFilename};
        String complete_args[] = new String[args.length + this.additional_args.length];

        {
            int k = 0;
            for (int i = 0; i < args.length; i++) {
                complete_args[k++] = args[i];
            }
            for (int i = 0; i < this.additional_args.length; i++) {
                complete_args[k++] = this.additional_args[i];
            }
        }

        this.output = (new trec_eval()).runAndGetOutput(complete_args);

        return output;
    }

    public void writeEvaluationResult() {
        this.writeEvaluationResult(new PrintWriter(new OutputStreamWriter(System.out)));
    }

    public void writeEvaluationResult(PrintWriter out) {
        String[][] var5;
        int var4 = (var5 = this.output).length;

        for(int var3 = 0; var3 < var4; ++var3) {
            String[] line = var5[var3];
            if (line.length >= 3 && line[0].equals("map") && line[1].equals("all")) {
                System.out.println("Average Precision: " + Rounding.toString(Double.parseDouble(line[2]), 4));
            }

            out.println(ArrayUtils.join(line, '\t'));
        }

    }

    public void writeEvaluationResultOfEachQuery(String evaluationResultFilename) {
        String[] args = new String[]{"-q", this.qrels, this.resFile};
        this.output = (new trec_eval()).runAndGetOutput(args);
        this.writeEvaluationResult(evaluationResultFilename);
    }

    public void writeEvaluationResult(String resultEvalFilename) {
        try {
            PrintWriter pw = new PrintWriter(Files.writeFileWriter(resultEvalFilename));
            this.writeEvaluationResult(pw);
            pw.close();
        } catch (IOException var3) {
            throw new RuntimeException(var3);
        }
    }

    public static boolean isPlatformSupported() {
        try {
            return trec_eval.isPlatformSupported();
        } catch (UnsupportedOperationException var1) {
            return false;
        }
    }

    public static class Command extends CLITool {
        public Command() {
        }

        public String commandname() {
            return "trec_eval";
        }

        public Set<String> commandaliases() {
            return Sets.newHashSet(new String[]{"treceval"});
        }

        public String help() {
            (new trec_eval()).run(new String[]{"-h"});
            return "";
        }

        public String helpsummary() {
            return "runs the NIST standard trec_eval tool";
        }

        public int run(String[] args) throws Exception {
            (new trec_eval()).run(args);
            return 0;
        }

        public String sourcepackage() {
            return "platform";
        }
    }
}
