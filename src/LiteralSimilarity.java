import java.util.HashMap;
import java.util.Map;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.apache.jena.graph.Node;
import org.apache.jena.query.*;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.sparql.core.TriplePath;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.LinkedHashMap;


import java.io.BufferedReader;
import java.io.FileReader;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import static RDFStarRelaxer.TriplePatternRelaxer.isObjectInstance;
import static RDFStarRelaxer.TriplePatternRelaxer.loadSimilarityMatrix;

public class LiteralSimilarity {
    private static Map<String, Map<String, Double>> similarityLiteralMap2 = new HashMap<>();
    private static final String SPARQL_ENDPOINT_URL = "http://localhost:3030/Benchdataset/query";


        
    public static Double getSimilarityL(String literal1, String literal2, String csvFilePath) throws IOException {
        loadLiteralMap(csvFilePath);
        double similarity = 0;
        Map<String, Double> similarLiterals = similarityLiteralMap2.get(literal1);
        //System.out.println("similar literals: " + similarLiterals);
        if (similarLiterals == null) {
            return null; // Literal not found, return null
        }
        for (Map.Entry<String, Double> entry : similarLiterals.entrySet()) {
            if (entry.getKey().equals(literal2)) { 
                similarity = entry.getValue();
            }
        }
        return similarity;
    }

    public static Double getMinimumSimilarity2(String literal1, String csvFilePath) throws IOException {
        loadLiteralMap(csvFilePath);
        //System.out.println("the map is : " + similarityLiteralMap2);
        //System.out.println("the original literal is : " + literal1.toString().trim().toLowerCase());
        //System.out.println(literal1.toString().trim().toLowerCase().equals("gene expression analysis")); 
        Map<String, Double> similarLiterals = similarityLiteralMap2.get(literal1);
        //System.out.println("similar literals: " + similarLiterals);
        if (similarLiterals == null) {
            return null; // Literal not found, return null
        }
        // Find the most similar literal by checking the highest similarity score
        double minSimilarity = 1;
        for (Map.Entry<String, Double> entry : similarLiterals.entrySet()) {
            if (!entry.getKey().equals(literal1) && entry.getValue() < minSimilarity) {
                minSimilarity = entry.getValue();
            }
        }
        return minSimilarity;
    }

    public static void loadLiteralMap(String csvFilePath) {
        try (BufferedReader br = new BufferedReader(new FileReader(csvFilePath))) {
            // Read the first line to get the labels
            String headerLine = br.readLine();
            if (headerLine == null) {
                System.out.println("Empty file.");
                return;
            }
            String[] labels = headerLine.split(",");

            // Process each line of the CSV file
            String line;
            while ((line = br.readLine()) != null) {
                String[] values = line.split(",");
                String rowLabel = values[0];
                Map<String, Double> rowMap = new LinkedHashMap<>();

                // Skip the first value (row label) and parse the rest as doubles
                for (int i = 1; i < values.length; i++) {
                    String colLabel = labels[i];
                    double similarityValue = Double.parseDouble(values[i]);
                    rowMap.put(colLabel, similarityValue);
                }
                similarityLiteralMap2.put(rowLabel, rowMap);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    public static int getTotalTriples(String jsonFilePath) {
        JsonObject statistics = loadStatisticsFromJson(jsonFilePath);
        if (statistics.has("Total Triples")) {
            String countString = statistics.getAsJsonPrimitive("Total Triples").getAsString();
            // Parse the count value from the string (assuming it's an integer)
            return Integer.parseInt(countString.split("\\^\\^")[0]);
        }
        return -1;
    }
    public static int getTotalLiterals(String jsonFilePath) {
        JsonObject statistics = loadStatisticsFromJson(jsonFilePath);
        if (statistics.has("Total Predicates Literals")) {
            String countString = statistics.getAsJsonPrimitive("Total Predicates Literals").getAsString();
            // Parse the count value from the string (assuming it's an integer)
            return Integer.parseInt(countString.split("\\^\\^")[0]);
        }
        return -1;
    }
    public static JsonObject loadStatisticsFromJson(String jsonFilePath){
        try (FileReader reader = new FileReader(jsonFilePath)) {
            return JsonParser.parseReader(reader).getAsJsonObject();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }
    public static int getTriplesL (String literal) {
        int triplesCount = 0;
        // Enclose the literal in quotes and escape it properly
        String escapedLiteral = "\"" + literal.replace("\"", "\\\"") + "\"";

        // Construct SPARQL query based on the position (subject or object)
        String queryString = "SELECT (COUNT(*) AS ?count) WHERE {";
        queryString += "?s ?p " + escapedLiteral + ". ";
        queryString += "}";

        // Create the query
        Query query = QueryFactory.create(queryString);

        try (QueryExecution qexec = QueryExecutionFactory.sparqlService(SPARQL_ENDPOINT_URL, query)) {
            // Execute the query
            ResultSet results = qexec.execSelect();
            if (results.hasNext()) {
                QuerySolution soln = results.nextSolution();
                triplesCount = soln.getLiteral("count").getInt();
            }
        }
        return triplesCount;
    }
    public static int getCountL(JsonArray stats, String key){
        for (JsonElement entry : stats) {
            JsonObject entryObject = entry.getAsJsonObject();
            if (entryObject.has("distinctLiteralCount") && entryObject.has("predicate")) {
                String entryClass = entryObject.getAsJsonPrimitive("predicate").getAsString();
                if (entryClass.equals(key)) {
                    // Extract count from the "count" field
                    String countString = entryObject.getAsJsonPrimitive("distinctLiteralCount").getAsString();
                    // Parse the count value from the string (assuming it's an integer)
                    return Integer.parseInt(countString.split("\\^\\^")[0]);
                }
            }
        }
        return 0; // Return 0 if count is not found
    }
    public static Double computeSimL (String literal1, String literal2, String JSON_STATISTICS_PATH, String literal_matrix_path) {
        double sim = 0;
        try {
        double mapping_value = getSimilarityL(literal1, literal2, literal_matrix_path);
        //System.out.println("the mapping value: " + mapping_value);
        double min = getMinimumSimilarity2(literal1, literal_matrix_path);
        int totalLiterals = getTotalLiterals(JSON_STATISTICS_PATH);
        int totalTriples = getTotalTriples(JSON_STATISTICS_PATH);
        JsonObject statistics = loadStatisticsFromJson(JSON_STATISTICS_PATH);
        if (statistics != null && statistics.has("Predicate")) {
            JsonArray litStatistics = statistics.getAsJsonArray("Predicate");
        double IC_cluster = -Math.log(getCountL(litStatistics, "http://swat.cse.lehigh.edu/onto/univ-bench.owl#researchInterest") / (double) totalLiterals); //to be modified after creating the cluster for the literals of ub:researchInterest
        //System.out.println("the IC cluster: " + IC_cluster);
        double IC_literal = -Math.log(getTriplesL(literal1) / (double) totalTriples);
        //System.out.println("the IC literal: " + IC_literal);
        double sim_inst = IC_cluster/IC_literal;
        //System.out.println("min: " + min);
        sim = sim_inst * mapping_value/min ;
        //System.out.println("sim: " + sim);
        }
    }
    catch (IOException e) {
        e.printStackTrace(); // Print stack trace for debugging
    }
    return sim; 
}
// Normalize a value to the range [0, 1]
public static double normalize(double value, double min, double max) {
    // If max and min are the same, avoid division by zero
    if (max == min) return 0.5;  // You can return any default value (e.g., 0.5) in this case
    return (value - min) / (max - min);
}
    public static void main (String[] args) {
            String JSON_STATISTICS_PATH = "data/lubm_statistics_siblings.json";
            String literal_matrix_path = "data/similarity_matrix.csv";
            String outputFilePath = "data/normalized_similarities.csv"; // Output file path
            // Map to store similarity results: {literal1 -> {literal2 -> similarity}}
            Map<String, Map<String, Double>> similarityResults = new HashMap<>();
            // Variables to track min and max similarity values
            double minSim = Double.MAX_VALUE;
            double maxSim = Double.MIN_VALUE;
            loadLiteralMap(literal_matrix_path);
            for (String literal1 : similarityLiteralMap2.keySet()) {
                for (String literal2 : similarityLiteralMap2.keySet()) {
                    if (!literal1.equals(literal2)) {
                        double sim = computeSimL(literal1,literal2,JSON_STATISTICS_PATH,literal_matrix_path);
                        // Track the min and max similarity values
                        minSim = Math.min(minSim, sim);
                        maxSim = Math.max(maxSim, sim);
                        // If the map for literal1 does not exist, create it
                        similarityResults.putIfAbsent(literal1, new HashMap<>());
                        // Store similarity in the map for literal1 -> literal2
                        similarityResults.get(literal1).put(literal2, sim);
                    }
        }
    }
    System.out.println("The similarity results map: " + similarityResults); // Normalize and write results to a file
    try (BufferedWriter writer = new BufferedWriter(new FileWriter(outputFilePath))) {
        // Write the header for the CSV
        writer.write("Literal1,Literal2,OriginalSimilarity,NormalizedSimilarity\n");

        // Apply normalization and write to the CSV file
        for (String literal1 : similarityResults.keySet()) {
            Map<String, Double> innerMap = similarityResults.get(literal1);
            for (String literal2 : innerMap.keySet()) {
                double sim = innerMap.get(literal2);
                // Normalize the similarity score
                double normalizedSim = normalize(sim, minSim, maxSim);
                // Write the result to the CSV file
                writer.write(literal1 + "," + literal2 + "," + sim + "," + normalizedSim + "\n");
            }
        }
        System.out.println("Similarity results saved to " + outputFilePath);
    } catch (IOException e) {
        e.printStackTrace();
    }
}
}