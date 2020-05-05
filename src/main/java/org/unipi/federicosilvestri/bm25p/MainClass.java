package org.unipi.federicosilvestri.bm25p;

import org.terrier.utility.ApplicationSetup;

import java.util.Arrays;

public class MainClass {

	public static final String USER_DIR = System.getProperty("user.dir");

	public static void main(String args[]) throws Exception {
		// setup Terrier environment
		setupTerrierEnv();

		double[] startW;
		{
			String startWString = ApplicationSetup.getProperty("org.unipi.federicosilvestri.startW", null);
			if (startWString == null) {
				throw new IllegalArgumentException("You must configure org.unipi.federicosilvestri.startW!");
			}

			String splitW[] = startWString.replace("[", "").replace("]", "").split(",");
			startW = Arrays.stream(splitW).mapToDouble(Double::parseDouble).toArray();
		}

		double endW[];
		{
			String endWString = ApplicationSetup.getProperty("org.unipi.federicosilvestri.endW", null);
			if (endWString == null) {
				throw new IllegalArgumentException("You must configure org.unipi.federicosilvestri.endW!");
			}

			String splitW[] = endWString.replace("[", "").replace("]", "").split(",");
			endW = Arrays.stream(splitW).mapToDouble(Double::parseDouble).toArray();
		}

		double wStep;
		{
			String wStepString = ApplicationSetup.getProperty("org.unipi.federicosilvestri.wStep", null);
			if (wStepString == null) {
				throw new IllegalArgumentException("You must configure org.unipi.federicosilvestri.wStep!");
			}

			wStep = Double.parseDouble(wStepString);
		}

		GridSearch gs = new BorderGridSearch(startW, endW, wStep, -1, Double.MAX_VALUE);
		gs.execute();
		System.out.println(gs.getResults());

		 // DataCollection ls = new DataCollection();
		 // ls.linearCollect();
	}

	public static void setupTerrierEnv() {
		System.setProperty("terrier.home", USER_DIR);
		System.setProperty("terrier.etc", USER_DIR + "/etc/");
	}

}
