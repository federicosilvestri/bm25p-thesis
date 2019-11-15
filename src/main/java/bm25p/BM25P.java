package bm25p;

import org.terrier.matching.models.WeightingModel;
import org.terrier.matching.models.WeightingModelLibrary;
import org.terrier.structures.postings.BlockPosting;
import org.terrier.structures.postings.Posting;
import org.terrier.utility.ApplicationSetup;

import java.util.Arrays;

public class BM25P extends WeightingModel{

    /**
     * Splits each document in p equal parts and multiplies the input weights w with alpha
     *
     * BM25PAlpha is the class that implements BM25P with the following params
     * - k1, k3, b are params from BM25
     * - p is the number of passages
     * - alpha is the multiplication factor for the weights distribution
     * - w is the list of weights (read as string)
     */

    private static final long serialVersionUID = 1L;
    private double k_1 = 1.2D;
    private double k_3 = 8.0D;
    private double b = 0.75D;
    private int alpha = 10;
    private int p = 10;
    String w = "[1.0,1.0,1.0,1.0,1.0,1.0,1.0,1.0,1.0,1.0]";
    double[] weights = new double[p]; // this will change to a different size in constructor


    /**
     * Initializing the parameters and creating the weight vector of size p.
     */
    public BM25P() {

        k_1 = Double.parseDouble(ApplicationSetup.getProperty("bm25p.k_1", "1.2"));
        k_3 = Double.parseDouble(ApplicationSetup.getProperty("bm25p.k_3", "8"));
        b = Double.parseDouble(ApplicationSetup.getProperty("bm25p.b", "0.75"));
        alpha = Integer.parseInt(ApplicationSetup.getProperty("bm25p.alpha", "10"));
        p = Integer.parseInt(ApplicationSetup.getProperty("bm25p.p", "10"));
        w = ApplicationSetup.getProperty("bm25p.w", "[1.0,1.0,1.0,1.0,1.0,1.0,1.0,1.0,1.0,1.0]");

        weights = new double[p];
        System.out.println("Number of passages: " + String.valueOf(p));
        String[] split_w = w.replace("[","").replace("]", "").split(",");
        weights = Arrays.stream(split_w)
                .mapToDouble(Double::parseDouble)
                .toArray();
    }

    public String getInfo() {
        return "BM25PAlpha";
    }

    public double score(double tf, double docLength) {
        //this is plain bm25 copied from TERRIER, 'cos no code reutilisation was possible
        double K = this.k_1  * (1.0D - this.b + this.b * docLength / this.averageDocumentLength);

        return WeightingModelLibrary.log((this.numberOfDocuments - this.documentFrequency + 0.5D) /
                (this.documentFrequency + 0.5D)) *  ((this.k_1 + 1.0D)  * tf / (K + tf)) *
                ((this.k_3 + 1.0D) * this.keyFrequency / (this.k_3 + this.keyFrequency));
    }

    @Override
    public double score(Posting posting) {

        double tf = 0.0;
        int docLen = posting.getDocumentLength();

        int[] tf_passage = new int[p]; //initialized to 0

        if (docLen > 0) {
            int positions[] = ((BlockPosting) posting).getPositions();
            for (int i = 0; i < positions.length; i++) {
                double pos = (double) positions[i];
                tf_passage[(int) Math.floor(pos * p / docLen)]++;
            }

            for (int i = 0; i < p; i++) {
                tf += alpha *  weights[i] *  tf_passage[i];
            }
        }

        return score(tf, docLen);
    }

    @Override
    public String toString() {
        return "BM25P";
    }
}
