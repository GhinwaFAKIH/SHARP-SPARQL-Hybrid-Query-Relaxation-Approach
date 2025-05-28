package Similarity;

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
import org.apache.jena.graph.Node_Literal;
import org.apache.jena.datatypes.RDFDatatype;



import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import static RDFStarRelaxer.TriplePatternRelaxer.isObjectInstance;
import static RDFStarRelaxer.TriplePatternRelaxer.loadSimilarityMatrix;

public class TriplePatternSimilarity {
    protected static final Logger log = LoggerFactory.getLogger(TriplePatternSimilarity.class);

    // SPARQL endpoint URL
    //private static final String SPARQL_ENDPOINT_URL = "http://localhost:3000/closure/query";
    private static final String SPARQL_ENDPOINT_URL = "http://localhost:3030/Benchdataset/query";

    // JSON file path for statistics
    //Specify the path of the statistics file here
    private static final String JSON_STATISTICS_PATH = "data/lubm_statistics_siblings.json";


    // Assuming the CSV file has two columns: 'node' and 'most_similar_node'
    //Specify the path of the entity mapping matrix file here
    private static final String csvFilePath =  "Benchmark_similarity_matrices/undergraduateCourses_similarity_matrix.csv";
    //"Benchmark_similarity_matrices/graduateCourses_similarity_matrix.csv";
    //"Benchmark_similarity_matrices/department_similarity_matrix.csv";
    //"Benchmark_similarity_matrices/headOfRange_similarity_matrix.csv";
    //private static final String CSV_INSTANCE_PATH = "data/similarity_instance_mapping.csv";
    //private static final String literal_matrix_path = "data/similarity_mapping_literals.csv";
    //Specify the path of the literal mapping matrix file here
    private static final String literal_matrix_path_3 = //"data/similarity_matrix.csv";
    "data/semester_similarity_matrix.csv";
    private static final String DELIMITER = ",";

    // Map to store similarity mappings from CSV   // Map to store similarity mappings from CSV
    private static Map<String, Map<String, Double>> similarityInstanceMap = new HashMap<>();
    private static Map<String, Map<String, Double>> similarityLiteralMap1 = new HashMap<>();
    private static Map<Integer, Map<Integer, Double>> similarityLiteralMap2 = new HashMap<>();
    private static Map<String, Map<String, Double>> similarityMap = new HashMap<>();

    private void loadSimilarityMap() {
        try (BufferedReader br = new BufferedReader(new FileReader(CSV_INSTANCE_PATH))) {
            String line;
            boolean isFirstLine = true; // Flag to skip header

            while ((line = br.readLine()) != null) {
                if (isFirstLine) {
                    isFirstLine = false;
                    continue; // Skip header line
                }
                String[] parts = line.split(DELIMITER);
                if (parts.length == 3) {
                    String instance1 = parts[0].trim();
                    String instance2 = parts[1].trim();
                    double similarity = Double.parseDouble(parts[2].trim());

                    similarityMap
                            .computeIfAbsent(instance1, k -> new HashMap<>())
                            .put(instance2, similarity);

                    // Optionally store the reverse relationship
                    similarityMap
                            .computeIfAbsent(instance2, k -> new HashMap<>())
                            .put(instance1, similarity);

                } else {
                    System.err.println("Skipping invalid line: " + line);
                }
            }
            System.out.println("Loaded similarity map: " + similarityMap);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static Double extractSimilarity(String csvFile, String entity1, String entity2) {
        String line;
        String csvSplitBy = ", ";

        try (BufferedReader br = new BufferedReader(new FileReader(csvFile))) {
            br.readLine(); // Skip header
            while ((line = br.readLine()) != null) {
                String[] data = line.split(csvSplitBy);
                String mostSimilarInstance = data[0];
                String similarInstance = data[1];
                double similarity = Double.parseDouble(data[2]);

                if ((mostSimilarInstance.equals(entity1) && similarInstance.equals(entity2)) ||
                        (mostSimilarInstance.equals(entity2) && similarInstance.equals(entity1))) {
                    return similarity;
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null; // Return null if no match is found
    }
/*
    public static Double getSimilarity(String instance1, String instance2) {
        System.out.println("the instance 1 is " + "<" + instance1 +">");
        System.out.println("the instance 2 is " + "<" + instance2 +">");
        System.out.println("the mapping is " +  similarityMap);
        if (similarityMap.containsKey(instance1) && similarityMap.get("<" + instance1 +">").containsKey("<" + instance2 + ">")) {
            return similarityMap.get("<" + instance1 +">").get("<" + instance2 + ">");
        }
        return null;
    }
    private static void loadSimilarityMappings(String filePath, Map<String, String> map) {
        try (BufferedReader br = new BufferedReader(new FileReader(filePath))) {
            String line;
            while ((line = br.readLine()) != null) {
                String[] parts = line.split(",");
                map.put(parts[0].trim(), parts[1].trim());
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }



    // Load similarity mappings from CSV into the map
    static {
        loadSimilarityMappings(CSV_INSTANCE_PATH, similarityInstanceMap);
        loadSimilarityMappings(CSV_LITERAL_PATH, similarityLiteralMap);
    }

 */
    public static double compute(TriplePath originalTriple, TriplePath relaxedTriple, String computationMethod) throws IOException {
        switch (computationMethod) {
            case "add":
                return compute_with_add(originalTriple, relaxedTriple);
            case "multiply":
                return compute_with_multiply(originalTriple, relaxedTriple);
            default:
                throw new IllegalArgumentException("Invalid computation method: " + computationMethod);
        }
    }
    public static double compute_with_add(TriplePath originalTriple, TriplePath relaxedTriple) throws IOException {
        if(originalTriple.getSubject().isNodeTriple()) {
            TriplePath originaltriple = new TriplePath (originalTriple.getSubject().getTriple());
            TriplePath relaxedtriple = new TriplePath (originalTriple.getSubject().getTriple());
            return (subjectSimilarity(originaltriple, relaxedtriple, "add") +
                    predicateSimilarity(originalTriple.getPredicate(), relaxedTriple.getPredicate(),originalTriple, relaxedTriple) +
                    objectSimilarity(originalTriple.getObject(), relaxedTriple.getObject(), originalTriple, relaxedTriple)
            )/3;
        } else if (originalTriple.getObject().isNodeTriple()) {
            TriplePath originaltriple = new TriplePath (originalTriple.getObject().getTriple());
            TriplePath relaxedtriple = new TriplePath (originalTriple.getObject().getTriple());
            return (subjectSimilarity(originalTriple.getSubject(), relaxedTriple.getSubject(),originalTriple, relaxedTriple) +
                    predicateSimilarity(originalTriple.getPredicate(), relaxedTriple.getPredicate(),originalTriple, relaxedTriple) +
                    objectSimilarity(originaltriple,relaxedtriple, "add")
            )/3;
        }
        return (subjectSimilarity(originalTriple.getSubject(), relaxedTriple.getSubject(), originalTriple, relaxedTriple) +
                predicateSimilarity(originalTriple.getPredicate(), relaxedTriple.getPredicate(),originalTriple, relaxedTriple) +
                objectSimilarity(originalTriple.getObject(), relaxedTriple.getObject(), originalTriple, relaxedTriple)
        )/3;
    }
    public static double compute_with_multiply(TriplePath originalTriple, TriplePath relaxedTriple) throws IOException {
        if(originalTriple.getSubject().isNodeTriple()) {
            TriplePath originaltriple = new TriplePath (originalTriple.getSubject().getTriple());
            TriplePath relaxedtriple = new TriplePath (originalTriple.getSubject().getTriple());
            return Math.cbrt(subjectSimilarity(originaltriple,relaxedtriple, "multiply") *
                    predicateSimilarity(originaltriple.getPredicate(), relaxedtriple.getPredicate(),originalTriple, relaxedTriple) *
                    objectSimilarity(originalTriple.getObject(), relaxedTriple.getObject(),originalTriple, relaxedTriple)
            );
        } else if (originalTriple.getObject().isNodeTriple()) {
            TriplePath originaltriple = new TriplePath (originalTriple.getObject().getTriple());
            TriplePath relaxedtriple = new TriplePath (originalTriple.getObject().getTriple());
            return Math.cbrt(subjectSimilarity(originalTriple.getSubject(), relaxedTriple.getSubject(),originalTriple, relaxedTriple) *
                    predicateSimilarity(originalTriple.getPredicate(), relaxedTriple.getPredicate(),originalTriple, relaxedTriple) *
                    objectSimilarity(originaltriple,relaxedtriple,"multiply")
            );
        }
        System.out.println( "triple sim: " + Math.cbrt(subjectSimilarity(originalTriple.getSubject(), relaxedTriple.getSubject(),originalTriple, relaxedTriple) *
        predicateSimilarity(originalTriple.getPredicate(), relaxedTriple.getPredicate(),originalTriple, relaxedTriple) *
        objectSimilarity(originalTriple.getObject(), relaxedTriple.getObject(), originalTriple, relaxedTriple)
));
        return Math.cbrt(subjectSimilarity(originalTriple.getSubject(), relaxedTriple.getSubject(),originalTriple, relaxedTriple) *
                predicateSimilarity(originalTriple.getPredicate(), relaxedTriple.getPredicate(),originalTriple, relaxedTriple) *
                objectSimilarity(originalTriple.getObject(), relaxedTriple.getObject(), originalTriple, relaxedTriple)
        );
    }

    private static double subjectSimilarity(TriplePath originalTriple, TriplePath relaxedTriple) throws IOException {
        Node originalSubject = originalTriple.getSubject();
        Node relaxedSubject = relaxedTriple.getSubject();
        if (originalSubject.equals(relaxedSubject)) {
            return 1.0;
        } else if (relaxedSubject.isVariable()){
            // it's a simple relaxation
            // To do is to compute the ratio of number of triples having subject, predicate or object node over all triples
            if (!relaxedTriple.getPredicate().isVariable()) {
                JsonObject statistics = loadStatisticsFromJson(JSON_STATISTICS_PATH);
                if (statistics != null && statistics.has("Property")) {
                    JsonArray propStatistics = statistics.getAsJsonArray("Property");
                    int totalTriples = getTotalTriples(JSON_STATISTICS_PATH);
                    return - Math.log(getCountP(propStatistics, relaxedTriple.getPredicate().getURI()) / (double) totalTriples);
                }
            }
            else if (!relaxedTriple.getObject().isVariable()) {
                    int totalTriples = getTotalTriples(JSON_STATISTICS_PATH);
                    return  - Math.log(getCountO(relaxedTriple.getObject().getURI()) / (double) totalTriples);

            }
        }
        else if (isObjectInstance(originalSubject)) {
        // in this case compute the similarity between instances
            Node subj_class = getClassOf (originalSubject);
            JsonObject statistics = loadStatisticsFromJson(JSON_STATISTICS_PATH);
            if (statistics != null && statistics.has("Class")) {
                JsonArray classStatistics = statistics.getAsJsonArray("Class");
                int totalInstances = getTotalInstances(JSON_STATISTICS_PATH);
                int totalTriples = getTotalTriples(JSON_STATISTICS_PATH);
                double IC_class = -Math.log(getCountC(classStatistics, subj_class.getURI()) / (double) totalInstances);
                double IC_instance = -Math.log(getTriplesI (originalSubject, "subject") / (double) totalTriples);
                double sim_inst = IC_class/IC_instance;
                double mapping_value = getSimilarity(originalSubject.toString(), relaxedSubject.toString(), csvFilePath);
                //double mapping_value = getMappingValue(originalSubject, relaxedSubject, similarityInstanceMap);
                //double min = getMinSimilarity(originalSubject, similarityInstanceMap);
                double min = getMinimumSimilarity(originalSubject.toString(), csvFilePath);
                return sim_inst * mapping_value/min ;
            }
        }
        else if (originalSubject.isLiteral()) {
            // not considering this case 
        }
        else {
            // it's a class relaxation
            String originalClassURI = originalSubject.getURI();
            String superClassURI = relaxedSubject.getURI();
            JsonObject statistics = loadStatisticsFromJson(JSON_STATISTICS_PATH);
            if (statistics != null && statistics.has("Class")) {
                JsonArray classStatistics = statistics.getAsJsonArray("Class");
                //int totalInstances = 54;
                int totalInstances = getTotalInstances(JSON_STATISTICS_PATH);
                return (-Math.log(getCountC(classStatistics, superClassURI) / (double) totalInstances)) /
                        (-Math.log(getCountC(classStatistics, originalClassURI) / (double) totalInstances));
            }
        }
        return -1;
    }

    // Get the mapping value for a given instance/literal
    private static double getMappingValue(Node originalNode, Node relaxedNode, Map<String, String> similarityMap) {
        System.out.println("the mapping is " + similarityMap);
        // Check if the nodes are literals
        if (!originalNode.isLiteral() || !relaxedNode.isLiteral()) {
            return 0.0; // Return 0 if either node is not a literal
        }
        String originalLiteral = originalNode.getLiteralLexicalForm();
        String relaxedLiteral = relaxedNode.getLiteralLexicalForm();

        // Create the key based on the lexical forms of the literals
        String key = originalLiteral + "_" + relaxedLiteral;
        // Check if the mapping exists in the map
        if (similarityMap.containsKey(key)) {
            return Double.parseDouble(similarityMap.get(key));
        } else {
            // If mapping not found, return 0.0
            return 0.0;
        }
    }

    // Get the minimum similarity value for a given instance/literal
    public static double getMinSimilarity(Node instance, Map<String, String> similarityMap) {
        double minSimilarity = Double.MAX_VALUE;
        for (Map.Entry<String, String> entry : similarityMap.entrySet()) {
            String key = entry.getKey();
            String[] literals = key.split("_");
            if (literals[0].equals(instance.getLiteralLexicalForm()) || literals[1].equals(instance.getLiteralLexicalForm())) {
                double similarity = Double.parseDouble(entry.getValue());
                if (similarity < minSimilarity) {
                    minSimilarity = similarity;
                }
            }
        }
        return minSimilarity;
    }

    public static Double getMinimumSimilarity(String targetInstance, String csvFilePath) throws IOException {
        similarityInstanceMap = loadSimilarityMatrix(csvFilePath);
        //System.out.println(similarityMap);
        //System.out.println(targetInstance);
        if (!similarityInstanceMap.containsKey(targetInstance)) {
            System.out.println("Target instance not found in the similarity map.");
            System.out.println("the target is " + targetInstance);
            return null;
        }
        Map<String, Double> similarityScores = similarityInstanceMap.get(targetInstance);
        //System.out.println("sim" +similarityScores);
        Double minSimilarity = Double.MAX_VALUE;
        // Iterate through the similarity scores for the target instance
        for (Double similarityValue : similarityScores.values()) {
            if (similarityValue < minSimilarity) {
                minSimilarity = similarityValue;
            }
        }
        return (minSimilarity == Double.MAX_VALUE) ? null : minSimilarity; // Return null if no values found
    }




    // Get class of an instance
    private static Node getClassOf(Node originalSubject) {
        Node classOfInstance = null;
        // Construct SPARQL query
        String queryString =
        "PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#> " +
        "PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#> " +
        "PREFIX owl: <http://www.w3.org/2002/07/owl#> " +
        "SELECT ?class WHERE { " +
        "  <" + originalSubject.getURI() + "> rdf:type ?class. " +
        "  FILTER NOT EXISTS { " +
        "    ?moreSpecificType rdfs:subClassOf ?class. " +
        "    <" + originalSubject.getURI() + "> rdf:type ?moreSpecificType. " +
        "  } " +
        "  FILTER (?class != owl:Class) " + 
        "}";
       // String queryString = "PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>"+
           //"SELECT ?class WHERE {<" + originalSubject.getURI() + "> rdf:type ?class}";
        Query query = QueryFactory.create(queryString);

        try (QueryExecution qexec = QueryExecutionFactory.sparqlService(SPARQL_ENDPOINT_URL, query)) {
            // Execute query
            ResultSet results = qexec.execSelect();
            if (results.hasNext()) {
                QuerySolution soln = results.nextSolution();
                RDFNode classNode = soln.get("class");
                if (classNode.isURIResource()) {
                    classOfInstance = classNode.asNode();
                }
            }
        }
        return classOfInstance;
    }


    private static double getCountO(String objectURI) {
        // SPARQL query to count triples with the given URI as object
        String query = "SELECT (COUNT(*) AS ?count) WHERE { ?s ?p <" + objectURI + "> }";
        // Create a QueryExecution instance
        QueryExecution queryExecution = QueryExecutionFactory.sparqlService(SPARQL_ENDPOINT_URL, query);
        // Execute the query
        ResultSet results = queryExecution.execSelect();
        // Get the count from the result set
        long count = 0;
        if (results.hasNext()) {
            count = results.next().getLiteral("count").getLong();
        }
        // Close the QueryExecution to free up resources
        queryExecution.close();
        return count;
    }
    private static double getCountS(String subjectURI) {
        // SPARQL query to count triples with the given URI as object
        String query = "SELECT (COUNT(*) AS ?count) WHERE { <" + subjectURI + "> ?p ?o  }";
        // Create a QueryExecution instance
        QueryExecution queryExecution = QueryExecutionFactory.sparqlService(SPARQL_ENDPOINT_URL, query);
        // Execute the query
        ResultSet results = queryExecution.execSelect();
        // Get the count from the result set
        long count = 0;
        if (results.hasNext()) {
            count = results.next().getLiteral("count").getLong();
        }
        // Close the QueryExecution to free up resources
        queryExecution.close();
        return count;
    }

    private static double subjectSimilarity(Node originalSubject, Node relaxedSubject, TriplePath originalTriple, TriplePath relaxedTriple) {
        if (originalSubject.equals(relaxedSubject)) {
            return 1.0;
        }  else if (relaxedSubject.isVariable()){
            // it's a simple relaxation
            // To do is to compute the ratio of number of triples having subject, predicate or object node over all triples
            if (!relaxedTriple.getPredicate().isVariable()) {
                JsonObject statistics = loadStatisticsFromJson(JSON_STATISTICS_PATH);
                if (statistics != null && statistics.has("Property")) {
                    JsonArray propStatistics = statistics.getAsJsonArray("Property");
                    int totalTriples = getTotalTriples(JSON_STATISTICS_PATH);
                    return - Math.log(getCountP(propStatistics, relaxedTriple.getPredicate().getURI()) / (double) totalTriples);
                }
            }
            else if (!relaxedTriple.getObject().isVariable()) {
                    int totalTriples = getTotalTriples(JSON_STATISTICS_PATH);
                    return  - Math.log(getCountO(relaxedTriple.getObject().getURI()) / (double) totalTriples);

            }
        }
        else {
            // it's a class relaxation
            String originalClassURI = originalSubject.getURI();
            String superClassURI = relaxedSubject.getURI();
            JsonObject statistics = loadStatisticsFromJson(JSON_STATISTICS_PATH);
            if (statistics != null && statistics.has("Class")) {
                JsonArray classStatistics = statistics.getAsJsonArray("Class");
                //int totalInstances = 54;
                int totalInstances = getTotalInstances(JSON_STATISTICS_PATH);
                return -Math.log(getCountC(classStatistics, superClassURI) / (double) totalInstances) /
                        -Math.log(getCountC(classStatistics, originalClassURI) / (double) totalInstances);
            }
        }
        return -1;
    }

    private static double subjectSimilarity( TriplePath originalSubject, TriplePath relaxedSubject, String computationMethod) throws IOException {
        if (originalSubject.equals(relaxedSubject)) {
            return 1.0;
        }
        else {
            compute( originalSubject, relaxedSubject, computationMethod);
        }
        return -1;
    }
    private static double objectSimilarity( TriplePath originalObject, TriplePath relaxedObject, String computationMethod) throws IOException {
        if (originalObject.equals(relaxedObject)) {
            return 1.0;
        }
        else {
            compute( originalObject, relaxedObject, computationMethod);
     
        }
        return -1;
    }
    private static double predicateSimilarity(TriplePath originalTriple, TriplePath relaxedTriple) {
        Node originalPredicate = originalTriple.getPredicate();
        Node relaxedPredicate = relaxedTriple.getPredicate();
        if (originalPredicate != null ) {
            if (originalPredicate.equals(relaxedPredicate)) {
                return 1.0;
            } else if (relaxedPredicate.isVariable()) {
                // it's a simple relaxation
                // To do is to compute the ratio of number of triples having subject, predicate or object node over all triples
                if (!relaxedTriple.getObject().isVariable()) {
                    int totalTriples = getTotalTriples(JSON_STATISTICS_PATH);
                    double sims = - Math.log(getCountO(relaxedTriple.getObject().getURI()) / (double) totalTriples);
                    System.out.println("the similarity for simple relaxation is: " +  sims);
                    return - Math.log(getCountO(relaxedTriple.getObject().getURI()) / (double) totalTriples);
                }
                else if (!relaxedTriple.getSubject().isVariable()) {
                    int totalTriples = getTotalTriples(JSON_STATISTICS_PATH);
                    double sims = - Math.log(getCountS(relaxedTriple.getSubject().getURI()) / (double) totalTriples);
                    System.out.println("the similarity for simple relaxation is: " +  sims);
                    return - Math.log(getCountS(relaxedTriple.getSubject().getURI()) / (double) totalTriples);

                }
            } else {
                // it's a property relaxation
                String originalPropertyURI = originalPredicate.getURI();
                String superPropertyURI = relaxedPredicate.getURI();
                JsonObject statistics = loadStatisticsFromJson(JSON_STATISTICS_PATH);
                if (statistics != null && statistics.has("Property")) {
                    JsonArray propStatistics = statistics.getAsJsonArray("Property");
                    int totalTriples = getTotalTriples(JSON_STATISTICS_PATH);
                    return -Math.log(getCountP(propStatistics, superPropertyURI) / (double) totalTriples) /
                            -Math.log(getCountP(propStatistics, originalPropertyURI) / (double) totalTriples);
                }
            }
        } else {
            return 0.1;
        }
        return -1;
    }

    private static double predicateSimilarity(Node originalPredicate, Node relaxedPredicate, TriplePath originalTriple, TriplePath relaxedTriple) {
        if (originalPredicate != null ) {
            if (originalPredicate.equals(relaxedPredicate)) {
                return 1.0;
            } else if (relaxedPredicate.isVariable()) {
                // it's a simple relaxation
                // To do is to compute the ratio of number of triples having subject, predicate or object node over all triples
                if (!relaxedTriple.getObject().isVariable()) {
                    int totalTriples = getTotalTriples(JSON_STATISTICS_PATH);
                    double sims = - Math.log(getCountO(relaxedTriple.getObject().getURI()) / (double) totalTriples);
                    System.out.println("the similarity for simple relaxation is: " +  sims);
                    return - Math.log(getCountO(relaxedTriple.getObject().getURI()) / (double) totalTriples);
                }
                else if (!relaxedTriple.getSubject().isVariable()) {
                    int totalTriples = getTotalTriples(JSON_STATISTICS_PATH);
                    double sims = - Math.log(getCountS(relaxedTriple.getSubject().getURI()) / (double) totalTriples);
                    System.out.println("the similarity for simple relaxation is: " +  sims);
                    return - Math.log(getCountS(relaxedTriple.getSubject().getURI()) / (double) totalTriples);

                }
            }else {
                // it's a property relaxation
                String originalPropertyURI = originalPredicate.getURI();
                String superPropertyURI = relaxedPredicate.getURI();
                JsonObject statistics = loadStatisticsFromJson(JSON_STATISTICS_PATH);
                if (statistics != null && statistics.has("Property")) {
                    JsonArray propStatistics = statistics.getAsJsonArray("Property");
                    int totalTriples = getTotalTriples(JSON_STATISTICS_PATH);
                    return -Math.log(getCountP(propStatistics, superPropertyURI) / (double) totalTriples) /
                            -Math.log(getCountP(propStatistics, originalPropertyURI) / (double) totalTriples);
                }
            }
        } else {
            return 0.1;
        }
        return -1;
    }
    public int getCountLiteralCluster(JsonArray propStatistics, String propertyURI) {
        int count = 0;
        for (JsonElement element : propStatistics) {
            JsonObject propertyStats = element.getAsJsonObject();
            String uri = propertyStats.get("URI").getAsString();
            if (uri.equals(propertyURI)) {
                count = propertyStats.get("LiteralCount").getAsInt();
                break;
            }
        }
        return count;
    }

    private static double objectSimilarity(TriplePath originalTriple, TriplePath relaxedTriple) throws IOException {
        Node originalObject = originalTriple.getObject();
        String predicate = relaxedTriple.getPredicate().getURI();
        Node relaxedObject = relaxedTriple.getObject();
        if (originalObject.equals(relaxedObject)) {
            return 1.0;
        } else if (relaxedObject.isVariable()) {
            // it's a simple relaxation
            // To do is to compute the ratio of number of triples having subject, predicate or object node over all triples
            if (!relaxedTriple.getPredicate().isVariable()) {
                JsonObject statistics = loadStatisticsFromJson(JSON_STATISTICS_PATH);
                if (statistics != null && statistics.has("Property")) {
                    JsonArray propStatistics = statistics.getAsJsonArray("Property");
                    int totalTriples = getTotalTriples(JSON_STATISTICS_PATH);
                    System.out.println("simple relax" + - Math.log(getCountP(propStatistics, relaxedTriple.getPredicate().getURI()) / (double) totalTriples));
                    return  - Math.log(getCountP(propStatistics, relaxedTriple.getPredicate().getURI()) / (double) totalTriples);
                }
            }
            else if (!relaxedTriple.getSubject().isVariable()) {
                int totalTriples = getTotalTriples(JSON_STATISTICS_PATH);
                return  - Math.log(getCountS(relaxedTriple.getSubject().getURI()) / (double) totalTriples);

            }
        } else if (relaxedObject.isLiteral() && relaxedObject != originalObject ) {
            // compute the similarity of object literal relaxation
            //String relaxedLiteral = relaxedObject.asLiteral().getString();
            //String originalLiteral = originalObject.asLiteral().getString();
            int totalLiterals = getTotalLiterals(JSON_STATISTICS_PATH);
            int totalTriples = getTotalTriples(JSON_STATISTICS_PATH);
            //System.out.println("original literal is string " + originalObject.toString().equals("Gene Expression Analysis"));
            //Modify in case of numeric literal not string   
            Object value1 = originalObject.getLiteral().getValue();  
            Object value2 = relaxedObject.getLiteral().getValue(); 
            double mapping_value = 0;
            double min = 0;
            if(originalObject.getLiteralDatatype().getURI().equals("http://www.w3.org/2001/XMLSchema#integer")) 
            {
                mapping_value = getSimilarityL(Integer.parseInt(String.valueOf(value1)), Integer.parseInt(String.valueOf(value2)), literal_matrix_path);
                min = getMinimumSimilarity2(Integer.parseInt(String.valueOf(value1)), literal_matrix_path);
            }
            else if (originalObject.getLiteralDatatype().getURI().equals("http://www.w3.org/2001/XMLSchema#string")) 
            {
                mapping_value = getSimilarityL(originalObject.toString(), relaxedObject.toString(), literal_matrix_path_3);
                min = getMinimumSimilarity2(originalObject.toString(), literal_matrix_path_3);
            }
            //double mapping_value = getSimilarityL(originalObject.toString(), relaxedObject.toString(), literal_matrix_path);
            System.out.println("the mapping value: " + mapping_value);
            JsonObject statistics = loadStatisticsFromJson(JSON_STATISTICS_PATH);
            if (statistics != null && statistics.has("Predicate")) {
                JsonArray litStatistics = statistics.getAsJsonArray("Predicate");
            double IC_cluster = -Math.log(getCountL(litStatistics, predicate) / (double) totalLiterals); //to be modified after creating the cluster for the literals of ub:researchInterest
            System.out.println("the IC cluster: " + IC_cluster);
            double IC_literal = -Math.log(getTriplesL(originalObject) / (double) totalTriples);
            System.out.println("the IC literal: " + IC_literal);
            double sim_inst = IC_cluster/IC_literal;
            System.out.println("min: " + min);
            double sim = sim_inst * mapping_value/min ;
            System.out.println("sim: " + sim);
            return sim; 
            }
        }
        //else if (relaxedObject.isURI() && originalTriple.getPredicate().getURI().equals(type.getURI())) {
            // compute the similarity of object instance relaxation
            else if (isObjectInstance(originalObject)) {
                // in this case compute the similarity between instances
                Node obj_class = getClassOf (originalObject);
                JsonObject statistics = loadStatisticsFromJson(JSON_STATISTICS_PATH);
                if (statistics != null && statistics.has("Class")) {
                    JsonArray classStatistics = statistics.getAsJsonArray("Class");
                    int totalInstances = getTotalInstances(JSON_STATISTICS_PATH);
                    int totalTriples = getTotalTriples(JSON_STATISTICS_PATH);
                    double IC_class = -Math.log(getCountC(classStatistics, obj_class.getURI()) / (double) totalInstances);
                    double IC_instance = -Math.log(getTriplesI(originalObject, "subject") / (double) totalTriples);
                    double sim_inst = IC_class/IC_instance;
                    double mapping_value = getSimilarity(originalObject.toString(), relaxedObject.toString(), csvFilePath);
                    //double mapping_value = getMappingValue(originalObject, relaxedObject, similarityInstanceMap);
                    double min = getMinimumSimilarity(originalObject.toString(), csvFilePath);
                    //double min = getMinSimilarity(originalObject, similarityInstanceMap);
                    double sim = sim_inst * mapping_value/min ;
                    return sim; 
                }
        } else {
            // it's a type (class) relaxation
            String originalClassURI = originalObject.getURI();
            String superClassURI = relaxedObject.getURI();
            JsonObject statistics = loadStatisticsFromJson(JSON_STATISTICS_PATH);
            if (statistics != null && statistics.has("Class")) {
                JsonArray classStatistics = statistics.getAsJsonArray("Class");
                int totalInstances = getTotalInstances(JSON_STATISTICS_PATH);
                return -Math.log(getCountC(classStatistics, superClassURI) / (double) totalInstances) /
                        -Math.log(getCountC(classStatistics, originalClassURI)/ (double) totalInstances);
            }
        }
        return -1;
    }
    private static double objectSimilarity(Node originalObject, Node relaxedObject, TriplePath originalTriple, TriplePath relaxedTriple) throws IOException {
        if (originalObject.equals(relaxedObject)) {
            return 1.0;
        } else if (relaxedObject.isVariable()) {
            // it's a simple relaxation
            // To do is to compute the ratio of number of triples having subject, predicate or object node over all triples
            if (!relaxedTriple.getPredicate().isVariable()) {
                JsonObject statistics = loadStatisticsFromJson(JSON_STATISTICS_PATH);
                if (statistics != null && statistics.has("Property")) {
                    JsonArray propStatistics = statistics.getAsJsonArray("Property");
                    int totalTriples = getTotalTriples(JSON_STATISTICS_PATH);
                    System.out.println("simple relax " + - Math.log(getCountP(propStatistics, relaxedTriple.getPredicate().getURI()) / (double) totalTriples));
                    return  - 1/(Math.log(getCountP(propStatistics, relaxedTriple.getPredicate().getURI()) / (double) totalTriples));
                }
            }
            else if (!relaxedTriple.getSubject().isVariable()) {
                int totalTriples = getTotalTriples(JSON_STATISTICS_PATH);
                return  - Math.log(getCountS(relaxedTriple.getSubject().getURI()) / (double) totalTriples);
            }
        }
        else if (relaxedObject.isLiteral() && relaxedObject != originalObject ) {
            //compute the similarity of object literal relaxation
            //String relaxedLiteral = relaxedObject.asLiteral().getString();
            //String originalLiteral = originalObject.asLiteral().getString();
            int totalLiterals = getTotalLiterals(JSON_STATISTICS_PATH);
            int totalTriples = getTotalTriples(JSON_STATISTICS_PATH);
            //System.out.println("the obj is same or not " + originalObject.getLiteral().toString().equals("Gene Expression Analysis"));
            System.out.println("the literal value is: " + originalObject.getLiteral().getValue());
            Object value1 = originalObject.getLiteral().getValue();  
            Object value2 = relaxedObject.getLiteral().getValue(); 
            double mapping_value = 0;
            double min = 0;
            if(originalObject.getLiteralDatatype().getURI().equals("http://www.w3.org/2001/XMLSchema#integer")) 
            {
                mapping_value = getSimilarityL(Integer.parseInt(String.valueOf(value1)), Integer.parseInt(String.valueOf(value2)), literal_matrix_path);
                min = getMinimumSimilarity2(Integer.parseInt(String.valueOf(value1)), literal_matrix_path);
            }
            else if(originalObject.getLiteralDatatype().getURI().equals("http://www.w3.org/2001/XMLSchema#string"))
            {
                System.out.println("the original literal is: " + originalObject.getLiteralLexicalForm().toString());
                mapping_value = getSimilarityL(originalObject.getLiteralLexicalForm().toString(), relaxedObject.getLiteralLexicalForm().toString(), literal_matrix_path_3);
                min = getMinimumSimilarity2(originalObject.getLiteralLexicalForm().toString(), literal_matrix_path_3);
            }
           
            //System.out.println("the mapping value: " + mapping_value);
            
            JsonObject statistics = loadStatisticsFromJson(JSON_STATISTICS_PATH);
            if (statistics != null && statistics.has("Predicate")) {
                JsonArray litStatistics = statistics.getAsJsonArray("Predicate");
            double IC_cluster = -Math.log(getCountL(litStatistics, relaxedTriple.getPredicate().getURI()) / (double) totalLiterals); //to be modified after creating the cluster for the literals of ub:researchInterest
            System.out.println("the IC cluster: " + IC_cluster);
            System.out.println("num: " + getTriplesL(originalObject));
            double IC_literal = 0;
            if (getTriplesL(originalObject) == 0) {
                    IC_literal = -Math.log(1/ (double) totalTriples);
            }
            else {
                IC_literal = -Math.log(getTriplesL(originalObject) / (double) totalTriples);
            }
            //double IC_literal = -Math.log(getTriplesL(originalObject) / (double) totalTriples);
            System.out.println("the IC literal: " + IC_literal);
            double sim_cluster = IC_cluster/IC_literal;
            /* 
            System.out.println("min: " + min);
            double sim = sim_inst * mapping_value/min ;
            System.out.println("sim: " + sim);
            */
            //double IC_cluster = -Math.log(getCountLiteralCluster(classStatistics, originalSubject.getURI()) / (double) totalLiterals);
            //double IC_literal = -Math.log(getTriplesL(originalSubject) / (double) totalTriples);
            //double sim_cluster = IC_cluster/IC_literal;
            //double mapping_value = getMappingValue(originalSubject, relaxedSubject, similarityLiteralMap);
            //double min = getMinSimilarity(originalSubject, similarityLiteralMap);
            double f1=(mapping_value-min)/(1-min);
            double f2= sim_cluster*((1-mapping_value)/(1-min));
            return f1+f2;
            //return sim; 
            }
        }
        else if (isObjectInstance(originalObject)) {
            // in this case compute the similarity between instances
            Node obj_class = getClassOf(originalObject);
            System.out.println("original object: " + originalObject);
            System.out.println("type class: " + obj_class);
            JsonObject statistics = loadStatisticsFromJson(JSON_STATISTICS_PATH);
            if (statistics != null && statistics.has("Class")) {
                JsonArray classStatistics = statistics.getAsJsonArray("Class");
                int totalInstances = getTotalInstances(JSON_STATISTICS_PATH);
                int totalTriples = getTotalTriples(JSON_STATISTICS_PATH);
                double IC_class = -Math.log(getCountC(classStatistics, obj_class.getURI()) / (double) totalInstances);
                double IC_instance = -Math.log(getTriplesI(originalObject, "subject") / (double) totalTriples);
                double sim_inst = IC_class/IC_instance;
                double mapping_value = getSimilarity(originalObject.toString(), relaxedObject.toString(), csvFilePath);
                System.out.println("relaxed object: " + relaxedObject);
                //double mapping_value = getMappingValue(originalObject, relaxedObject, similarityInstanceMap);
                System.out.println("the mapping similarity is " + mapping_value );
                double min = getMinimumSimilarity(originalObject.toString(), csvFilePath);
                //double min = getMinSimilarity(originalObject, similarityInstanceMap);
                System.out.println("the minimum similarity is " + min);
                double f1=(mapping_value-min)/(1-min);
                System.out.println("f1: " + f1); 
                System.out.println("sim_inst: " + sim_inst);
                double f2= sim_inst*((1-mapping_value)/(1-min));
                System.out.println("f2: " + f2);
                if(Double.isInfinite(f2)){
                    return 0;
                }
                return f1+f2;
        }
    }
        else {
            // it's a type (class) relaxation
            String originalClassURI = originalObject.getURI();
            String superClassURI = relaxedObject.getURI();
            JsonObject statistics = loadStatisticsFromJson(JSON_STATISTICS_PATH);
            if (statistics != null && statistics.has("Class")) {
                JsonArray classStatistics = statistics.getAsJsonArray("Class");
                int totalInstances = getTotalInstances(JSON_STATISTICS_PATH);
                // Get intersection and union counts for the two classes
                int intersectionCount = getIntersectionCount(statistics, originalClassURI.toString(), superClassURI.toString());
                int unionCount = getUnionCount(statistics, originalClassURI.toString(), superClassURI.toString());
                System.out.println("The two classes are: " + originalClassURI.toString() + "and " + superClassURI.toString());
                System.out.println("intersection count: " + intersectionCount);
                System.out.println("union count: " + unionCount);
                // Calculate similarity based on the provided equation
                double unionRatio = (double) unionCount / totalInstances;
                double intersectionRatio = (double) intersectionCount / totalInstances;
                double similarity = -Math.log(unionRatio) / -Math.log(intersectionRatio);
                System.out.println("Similarity between classes: " + similarity);
                return similarity;
                //return -Math.log(getCountC(classStatistics, superClassURI) / (double) totalInstances) /
                        //-Math.log(getCountC(classStatistics, originalClassURI)/ (double) totalInstances);
            }
        }
        return -1;
    }

    public static int getIntersectionCount(JsonObject statistics, String class1, String class2) {
        JsonArray siblingClasses = statistics.getAsJsonArray("SiblingClasses");
        
        for (JsonElement siblingGroupElement : siblingClasses) {
            JsonObject siblingGroup = siblingGroupElement.getAsJsonObject();
            
            if (siblingGroup.get("class").getAsString().equals(class1) && 
                siblingGroup.getAsJsonObject("intersections").has(class2)) {
                return siblingGroup.getAsJsonObject("intersections")
                                   .getAsJsonObject(class2)
                                   .get("intersectionCount").getAsInt();
            }
        }
        return -1; // Return -1 if intersection not found
    }
    
    public static int getUnionCount(JsonObject statistics, String class1, String class2) {
        JsonArray siblingClasses = statistics.getAsJsonArray("SiblingClasses");
        
        for (JsonElement siblingGroupElement : siblingClasses) {
            JsonObject siblingGroup = siblingGroupElement.getAsJsonObject();
            
            if (siblingGroup.get("class").getAsString().equals(class1) && 
                siblingGroup.getAsJsonObject("unionCounts").has(class2)) {
                return siblingGroup.getAsJsonObject("unionCounts")
                                   .get(class2).getAsInt();
            }
        }
        return -1; // Return -1 if union not found
    }

    public static void loadLiteralMap1(String csvFilePath) {
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
                similarityLiteralMap1.put(rowLabel, rowMap);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    public static void loadLiteralMap2(String csvFilePath) {
            try (BufferedReader br = new BufferedReader(new FileReader(csvFilePath))) {
                // Read the first line to get the column labels (years)
                String headerLine = br.readLine();
                if (headerLine == null) {
                    System.out.println("Empty file.");
                    return;
                }
                String[] labels = headerLine.split(",");
                int[] years = new int[labels.length - 1];
        
                // Parse column labels (years) as integers
                for (int i = 1; i < labels.length; i++) {
                    years[i - 1] = Integer.parseInt(labels[i].trim());
                }
        
                // Process each line of the CSV file
                String line;
                while ((line = br.readLine()) != null) {
                    String[] values = line.split(",");
                    int rowYear = Integer.parseInt(values[0].trim());
                    Map<Integer, Double> rowMap = new LinkedHashMap<>();
        
                    // Skip the first value (row label) and parse the rest as doubles
                    for (int i = 1; i < values.length; i++) {
                        int colYear = years[i - 1];
                        double similarityValue = Double.parseDouble(values[i].trim());
                        rowMap.put(colYear, similarityValue);
                    }
                    similarityLiteralMap2.put(rowYear, rowMap);
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
    }

    public static Double getSimilarityL(Integer integer1, Integer integer2, String csvFilePath) throws IOException {
        loadLiteralMap2(csvFilePath);
        double similarity = 0;
        System.out.println("the integer value is: " + integer1);
        System.out.println("the map: " + similarityLiteralMap1);
        Map<Integer, Double> similarLiterals = similarityLiteralMap2.get(integer1);
        System.out.println("the similar literals is: " + similarLiterals);
        //System.out.println("similar literals: " + similarLiterals);
        if (similarLiterals == null) {
            return null; // Literal not found, return null
        }
        for (Map.Entry<Integer, Double> entry : similarLiterals.entrySet()) {
            if (entry.getKey().equals(integer2)) { 
                similarity = entry.getValue();
            }
        }
        return similarity;
    }
    
 
    public static Double getSimilarityL(String literal1, String literal2, String csvFilePath) throws IOException {
        loadLiteralMap1(csvFilePath);
        double similarity = 0;
        //System.out.println("the map is : " + similarityLiteralMap2);
        //System.out.println("the original literal is : " + literal1.toString().trim().toLowerCase());
        //System.out.println(literal1.toString().trim().toLowerCase().equals("gene expression analysis")); 
        Map<String, Double> similarLiterals = similarityLiteralMap1.get(literal1);
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
        loadLiteralMap1(csvFilePath);
        //System.out.println("the map is : " + similarityLiteralMap2);
        //System.out.println("the original literal is : " + literal1.toString().trim().toLowerCase());
        //System.out.println(literal1.toString().trim().toLowerCase().equals("gene expression analysis")); 
        Map<String, Double> similarLiterals = similarityLiteralMap1.get(literal1);
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
    public static Double getMinimumSimilarity2(Integer integer1, String csvFilePath) throws IOException {
        loadLiteralMap2(csvFilePath);
        //System.out.println("the map is : " + similarityLiteralMap2);
        //System.out.println("the original literal is : " + literal1.toString().trim().toLowerCase());
        //System.out.println(literal1.toString().trim().toLowerCase().equals("gene expression analysis")); 
        Map<Integer, Double> similarLiterals = similarityLiteralMap2.get(integer1);
        //System.out.println("similar literals: " + similarLiterals);
        if (similarLiterals == null) {
            return null; // Literal not found, return null
        }
        // Find the most similar literal by checking the highest similarity score
        double minSimilarity = 1;
        for (Map.Entry<Integer, Double> entry : similarLiterals.entrySet()) {
            if (!entry.getKey().equals(integer1) && entry.getValue() < minSimilarity) {
                minSimilarity = entry.getValue();
            }
        }
        return minSimilarity;
    }


    public static Double getSimilarity(String instance1, String instance2, String csvFilePath) throws IOException {
        similarityInstanceMap = loadSimilarityMatrix(csvFilePath);
        // Debugging map size
        System.out.println("Map size: " + similarityInstanceMap.size());
        //System.out.println("the map is: " + similarityInstanceMap);
        // Check if the first instance exists in the similarity map
        if (similarityInstanceMap.containsKey(instance1)) {
            // Get the map for the similar instances and their scores
            Map<String, Double> innerMap = similarityInstanceMap.get(instance1);
            // Check if the second instance exists in the inner map and return the similarity value
            if (innerMap.containsKey(instance2)) {
                System.out.println("similarity between instances: " + innerMap.get(instance2));
                return innerMap.get(instance2);
            }
        }
        // If either instance is not found, return null or a default value (e.g., 0.0)
        return null;
    }
    

    public static int getTriplesI (Node instance, String pos) {
        int triplesCount = 0;

        // Construct SPARQL query based on the position (subject or object)
        String queryString = "SELECT (COUNT(*) AS ?count) WHERE {";
        if (pos.equalsIgnoreCase("subject")) {
            queryString += "?s ?p <" + instance.getURI() + ">. ";
        } else if (pos.equalsIgnoreCase("object")) {
            queryString += "<" + instance.getURI() + "> ?p ?o. ";
        }
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

    public static String getLiteralType(Node node) {
        if (node.isLiteral()) {
            Node_Literal lit = (Node_Literal) node;
            RDFDatatype datatype = lit.getLiteralDatatype();
            
            if (datatype != null) {
                String datatypeURI = datatype.getURI();
                if (datatypeURI.equals("http://www.w3.org/2001/XMLSchema#integer")) {
                    return "integer";
                } else if (datatypeURI.equals("http://www.w3.org/2001/XMLSchema#string")) {
                    return "string";
                } else {
                    return "other: " + datatypeURI; // For other types like double, boolean, etc.
                }
            } else {
                return "unknown"; // No datatype (could be an untyped literal)
            }
        }
        return "not a literal";
    }

    public static int getTriplesL(Object literal1) {
        int triplesCount = 0;
    
        // Ensure that literal1 is a Jena Node
        if (literal1 instanceof Node) {
            Node node = (Node) literal1;
    
            // Ensure it's a literal node
            if (node.isLiteral()) {
                Node_Literal lit = (Node_Literal) node;
                RDFDatatype datatype = lit.getLiteralDatatype();
                String literalValue;
    
                // Check datatype to determine if it's an integer or string
                if (datatype != null && "http://www.w3.org/2001/XMLSchema#integer".equals(datatype.getURI())) {
                    literalValue = lit.getLiteralLexicalForm(); // Use the raw integer value
                } else { 
                    literalValue = "\"" + lit.getLiteralLexicalForm().replace("\"", "\\\"") + "\""; // Escape quotes for strings
                }
    
                // Construct SPARQL query
                String queryString = "SELECT (COUNT(*) AS ?count) WHERE { ?s ?p " + literalValue + " . }";
    
                // Execute the query
                try (QueryExecution qexec = QueryExecutionFactory.sparqlService(SPARQL_ENDPOINT_URL, QueryFactory.create(queryString))) {
                    ResultSet results = qexec.execSelect();
                    if (results.hasNext()) {
                        QuerySolution soln = results.nextSolution();
                        triplesCount = soln.getLiteral("count").getInt();
                    }
                } catch (Exception e) {
                    System.err.println("SPARQL query execution error: " + e.getMessage());
                }
            } else {
                System.err.println("Error: Node is not a literal.");
            }
        } else {
            System.err.println("Error: literal1 is not a Jena Node.");
        }
    
        return triplesCount;
    }
    /* 
    public static int getTriplesL (Node literal) {
        int triplesCount = 0;
        // Construct SPARQL query based on the position (subject or object)
        String queryString = "SELECT (COUNT(*) AS ?count) WHERE {";
        queryString += "?s ?p " + literal + ". ";
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
    */


    
    public static int getCountC(JsonArray stats, String key){
            for (JsonElement entry : stats) {
                JsonObject entryObject = entry.getAsJsonObject();
                if (entryObject.has("count") && entryObject.has("class")) {
                    String entryClass = entryObject.getAsJsonPrimitive("class").getAsString();
                    if (entryClass.equals(key)) {
                        // Extract count from the "count" field
                        String countString = entryObject.getAsJsonPrimitive("count").getAsString();
                        // Parse the count value from the string (assuming it's an integer)
                        return Integer.parseInt(countString.split("\\^\\^")[0]);
                    }
                }
            }
            return 0; // Return 0 if count is not found
        }

    protected static int getCountP(JsonArray stats, String key){
            for (JsonElement entry : stats) {
                JsonObject entryObject = entry.getAsJsonObject();
                if (entryObject.has("count") && entryObject.has("prop")) {
                    String entryClass = entryObject.getAsJsonPrimitive("prop").getAsString();
                    if (entryClass.equals(key)) {
                        // Extract count from the "count" field
                        String countString = entryObject.getAsJsonPrimitive("count").getAsString();
                        // Parse the count value from the string (assuming it's an integer)
                        return Integer.parseInt(countString.split("\\^\\^")[0]);
                    }
                }
            }
            return 0; // Return 0 if count is not found
        }

    public static JsonObject loadStatisticsFromJson(String jsonFilePath){
            try (FileReader reader = new FileReader(jsonFilePath)) {
                return JsonParser.parseReader(reader).getAsJsonObject();
            } catch (IOException e) {
                e.printStackTrace();
            }
            return null;
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
    public static int getTotalTriples(String jsonFilePath) {
        JsonObject statistics = loadStatisticsFromJson(jsonFilePath);
        if (statistics.has("Total Triples")) {
            String countString = statistics.getAsJsonPrimitive("Total Triples").getAsString();
            // Parse the count value from the string (assuming it's an integer)
            return Integer.parseInt(countString.split("\\^\\^")[0]);
        }
        return -1;
    }
    public static int getTotalInstances(String jsonFilePath) {
        JsonObject statistics = loadStatisticsFromJson(jsonFilePath);
        if (statistics.has("Total Classes Instances")) {
            String countString = statistics.getAsJsonPrimitive("Total Classes Instances").getAsString();
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



    }