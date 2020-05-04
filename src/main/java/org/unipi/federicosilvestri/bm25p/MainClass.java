package org.unipi.federicosilvestri.bm25p;

public class MainClass {

	public static final String USER_DIR = System.getProperty("user.dir");

	public static void main(String args[]) throws Exception {
		// setup Terrier environment
		setupTerrierEnv();

		 DataCollection ls = new DataCollection();
		 ls.linearCollect();
	}

	public static void setupTerrierEnv() {
		System.setProperty("terrier.home", USER_DIR);
		System.setProperty("terrier.etc", USER_DIR + "/etc/");
	}

}
