public class BM25P extends WeightingModel {

	private static final long serialVersionUID = 1L;
	private double k_1 = 1.2D;
	private double k_3 = 8.0D;
	private double b = 0.75D;
	private int alpha = 10;
	private int p = 10;
	private String w = "[1.0,1.0,1.0,1.0,1.0,1.0,1.0,1.0,1.0,1.0]";
	private double[] weights = new double[p];
	
	public double score(double tf, double docLength) {
		double K = this.k_1 * (1.0D - this.b + this.b * docLength / this.averageDocumentLength);

		return WeightingModelLibrary
				.log((this.numberOfDocuments - this.documentFrequency + 0.5D) / (this.documentFrequency + 0.5D))
				* ((this.k_1 + 1.0D) * tf / (K + tf))
				* ((this.k_3 + 1.0D) * this.keyFrequency / (this.k_3 + this.keyFrequency));
	}

	@Override
	public double score(Posting posting) {
		double tf = 0.0;
		int docLen = posting.getDocumentLength();

		int[] passagesTF = new int[p];

		if (docLen > 0) {
			int positions[] = ((BlockPosting) posting).getPositions();

			for (int i = 0; i < positions.length; i++) {
				double pos = (double) positions[i];
				passagesTF[(int) Math.floor(pos * p / docLen)]++;
			}

			
			for (int i = 0; i < p; i++) {
				tf += alpha * weights[i] * passagesTF[i];
			}
		}

		return score(tf, docLen);
	}

}
