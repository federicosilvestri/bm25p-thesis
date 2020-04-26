package org.unipi.federicosilvestri.bm25p.treceval;

import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.TreeSet;

public class SerializableMap extends HashMap<Double, Double> {
    public final String filepath;

    public SerializableMap(String filepath) {
        super();

        if (filepath == null) {
            throw new NullPointerException();
        }

        this.filepath = filepath;
    }

    public void write() throws IOException {
        FileWriter fw = new FileWriter(filepath);
        TreeSet<Double> orderedSet = new TreeSet<>(keySet());

        for (Double key : orderedSet) {
            String line = "" + key + "," + get(key) + "\n";
            fw.write(line);
        }

        fw.close();
    }
}
