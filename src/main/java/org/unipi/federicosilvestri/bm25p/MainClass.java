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

        if (args.length == 0) {
            throw new IllegalArgumentException("You must pass search type! Available search types: lings,nlings,incrs");
        }

        SearchAlgorithm sa = null;
        switch (args[1]) {
            case "lings":
                sa = new GridSearch(startW, endW, wStep, -1, Double.MAX_VALUE);
                break;
            case "nlings":
                sa = new BorderGridSearch(startW, endW, wStep, -1, Double.MAX_VALUE);
                break;
            case "incrs":
                sa = new IncreaseSearch(startW, endW, wStep, -1, Double.MAX_VALUE);
                break;
            default:
                throw new IllegalArgumentException("Bad search type!");

        }

        sa.execute();
        System.out.println(sa.getResults());
    }

    public static void setupTerrierEnv() {
        System.setProperty("terrier.home", USER_DIR);
        System.setProperty("terrier.etc", USER_DIR + "/etc/");
    }

}
