package connor.examples;

import java.io.*;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;

import static java.nio.file.Files.newBufferedReader;

public class FilteringDataset {

    //Charset charset = StandardCharsets.UTF_16;
    private static final Charset charset = StandardCharsets.ISO_8859_1;
    private static final boolean isEliminatingTwoTriples = false;
    private static final String filter1 = "https://clara.univ-nantes.fr/statement";
    private static final String filter2 = "http://www.w3.org/1999/02/22-rdf-syntax-ns#predicate";
    private static final String filter3 = "http://www.w3.org/1999/02/22-rdf-syntax-ns#type";

    public static void main(String[] args) throws IOException {

        String inputFolder = args[0];
        String outputFolder = args[1];

        File dir = new File(inputFolder);
        File[] directoryListing = dir.listFiles();

        File outputDir = new File(outputFolder);
        if (!outputDir.exists()) {
            outputDir.mkdir();
        }

        if(directoryListing != null) {
            for (File f : directoryListing) {
                filterAndWriteFile(f, outputFolder);
            }
        }
    }

    private static void filterAndWriteFile(File inputFile, String outputFolder) throws IOException {

        File outputFile = new File(outputFolder + inputFile.getName());
        FileOutputStream fos = new FileOutputStream(outputFile);

        BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(fos));
        BufferedReader reader = newBufferedReader(Path.of(inputFile.getPath()), charset);

        String line;
        while ((line = reader.readLine()) != null) {
            if (shouldKeepLine(line)) {
                writer.write(line);
                writer.newLine();
            }
        }
        writer.close();
    }

    private static boolean shouldKeepLine(String line) {
        if(isEliminatingTwoTriples) {
            return !(line.contains(filter1) && (line.contains(filter2) || line.contains(filter3)));
        } else {
            return !(line.contains(filter1));
        }
    }
}
