package connor.examples;

import org.apache.jena.query.Dataset;
import org.apache.jena.riot.Lang;
import org.apache.jena.riot.RDFDataMgr;
import org.apache.jena.tdb.TDBFactory;

import java.io.File;
import java.io.IOException;

public class TDBCreator {

    public static void main(String[] args) {

        String inputFolder = args[0];
        String tdbPath = args[1];

        System.out.println("=================== CREATING TDB BACKED DATASET ===================");
        // Create or connect to a TDB-backed dataset
        Dataset dataset = TDBFactory.createDataset(tdbPath);

        // Load the data in the TDB-backed dataset
        File dir = new File(inputFolder);
        File[] directoryListing = dir.listFiles();

        if (directoryListing == null)
            throw new RuntimeException("No Files ???");

        for (File f : directoryListing) {

            try {
                RDFDataMgr.read(dataset, f.getAbsolutePath(), Lang.NTRIPLES);
            } catch (Exception e) {
                System.out.println("Error Reading the file: " + f.getName());
                System.out.println(e.getMessage());
            }
        }

        // Close the dataset after use dataset.close();
        dataset.close();

        System.out.println("Dataset located at: " + tdbPath);
        System.out.println("=================== TDB DATASET READY ===================");
    }
}
