import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.apache.jena.query.*;
import org.apache.jena.rdf.model.RDFNode;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import java.io.FileWriter;
import java.io.IOException;
import java.util.*;

public class Statistics {

    public static void main(String[] args) {
        computestat();
    }

    public static void computestat() {
        // SPARQL endpoint URL
        //String endpointURL = "http://localhost:3000/closure/query";
        String endpointURL = "http://localhost:3000/lubm10-05-07/query";
        // SPARQL query to retrieve instances for each class
        String statclass = "PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>" +
        "PREFIX owl: <http://www.w3.org/2002/07/owl#>" +
                 "SELECT ?class (COUNT(?instance) AS ?count) WHERE { ?class a owl:Class.\n" +
                "  ?instance a ?class.\n } GROUP BY ?class";
        String statprop = "SELECT ?prop (COUNT(*) AS ?count) WHERE { ?a ?prop ?c } GROUP BY ?prop";
        String total_triples = "SELECT (COUNT(*) AS ?count) WHERE { ?s ?p ?o }";
        String total_classes = "PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>" +
        "PREFIX owl: <http://www.w3.org/2002/07/owl#>" +
        "SELECT (COUNT(DISTINCT ?instance) AS ?count) WHERE {" +
            "{ ?instance a ?class." +
                "?class a rdfs:Class. } " +
            "UNION" +
            "{ ?instance a ?class." +
        "?class a owl:Class." +
           " }" +
        "}";

        String total_literals = "SELECT (COUNT(DISTINCT ?literal) AS ?count)"+
                "WHERE {"+
                  "?subject ?predicate ?literal ."+
                 " FILTER(isLiteral(?literal))"+
                "} ";
                

        String sibling_classes = "PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>" +
                "SELECT ?class ?siblings WHERE {\n" +
                // "  ?parent rdfs:subClassOf ?superClass .\n" +
                "  \n" +
                "  ?class rdfs:subClassOf ?parent .\n" +
                "  ?siblings rdfs:subClassOf ?parent .\n" +
                "  \n" +
                "  FILTER NOT EXISTS {\n" +
                "    ?intermediate rdfs:subClassOf ?parent .\n" +
                "    ?class rdfs:subClassOf ?intermediate .\n" +
                "    ?siblings rdfs:subClassOf ?intermediate .\n" +
                "    FILTER (?class != ?intermediate && ?siblings != ?intermediate)\n" +
                "  }\n" +
                "  FILTER (?class != ?siblings)\n" +
                " \n" +
                "}";

        // SPARQL query to retrieve the frequency of property-object pairs
        String propObjFreqQuery = "PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#> " +
                "SELECT ?property ?object (COUNT(*) AS ?count) WHERE { " +
                "  ?s ?property ?object . " +
                //"  FILTER(?property != rdf:type) " +
                "} GROUP BY ?property ?object " +
                "ORDER BY DESC(?count)";

        // JSON Object to store statistics
        JSONObject statistics = new JSONObject();

        // Execute the class statistics query
        QueryExecution queryExecution = QueryExecutionFactory.sparqlService(endpointURL, QueryFactory.create(statclass));
        addStatistics(queryExecution, statistics, "Class");


        // Execute the property statistics query
        queryExecution = QueryExecutionFactory.sparqlService(endpointURL, QueryFactory.create(statprop));
        addPropertyStatistics(queryExecution, statistics, endpointURL);

        // Execute the total triples count query
        queryExecution = QueryExecutionFactory.sparqlService(endpointURL, QueryFactory.create(total_triples));
        addCount(queryExecution, statistics, "Total Triples");

        // Execute the total classes instances count query
        queryExecution = QueryExecutionFactory.sparqlService(endpointURL, QueryFactory.create(total_classes));
        addCount(queryExecution, statistics, "Total Classes Instances");

        // Execute the total literals count query
        queryExecution = QueryExecutionFactory.sparqlService(endpointURL, QueryFactory.create(total_literals));
        addCount(queryExecution, statistics, "Total Predicates Literals");

        // Execute the sibling classes query
        queryExecution = QueryExecutionFactory.sparqlService(endpointURL, QueryFactory.create(sibling_classes));
        addSiblingStatistics(queryExecution, statistics, endpointURL);

        // Execute the property-object frequency query
        //queryExecution = QueryExecutionFactory.sparqlService(endpointURL, QueryFactory.create(propObjFreqQuery));
        //addPropertyObjectFrequency(queryExecution, statistics);

        //System.out.println("the statistics are " + statistics);


        // Write the statistics to a JSON file
        try (FileWriter file = new FileWriter("data/lubm_statistics_siblings.json")) {
            Gson gson = new GsonBuilder().setPrettyPrinting().create();
            file.write(gson.toJson(statistics));
            file.flush();
            System.out.println("Statistics saved to lubm_statistics-3.json");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }



    private static void addStatistics(QueryExecution queryExecution, JSONObject statistics, String category) {
        ResultSet resultSet = queryExecution.execSelect();
        List<String> resultVars = resultSet.getResultVars();
        JSONArray statList = (JSONArray) statistics.getOrDefault(category, new JSONArray());
        //JSONArray statList = new JSONArray();

        while (resultSet.hasNext()) {
            QuerySolution solution = resultSet.nextSolution();
            Map<String, String> statValues = new HashMap<>();
            for (String var : resultVars) {
                statValues.put(var, solution.get(var).toString());
            }
            statList.add(statValues);
        }
        statistics.put(category, statList);
    }
    /*
    private static void addSiblingStatistics(QueryExecution queryExecution, JSONObject statistics, String endpointURL) {
        ResultSet resultSet = queryExecution.execSelect();
        Map<String, List<String>> siblingMap = new HashMap<>();

        // Retrieve sibling relationships
        while (resultSet.hasNext()) {
            QuerySolution solution = resultSet.nextSolution();
            String classUri = solution.get("class").toString();
            String siblingUri = solution.get("siblings").toString();

            siblingMap.putIfAbsent(classUri, new ArrayList<>());
            siblingMap.get(classUri).add(siblingUri);
        }

        // Calculate intersection and union of instances for siblings
        JSONArray siblingStats = new JSONArray();
        for (Map.Entry<String, List<String>> entry : siblingMap.entrySet()) {
            String classUri = entry.getKey();
            List<String> siblings = entry.getValue();

            JSONObject siblingObject = new JSONObject();
            siblingObject.put("class", classUri);
            siblingObject.put("siblings", siblings);

            // Calculate instances
            Set<String> unionInstances = new HashSet<>();
            Map<String, Set<String>> siblingInstances = new HashMap<>();
            for (String sibling : siblings) {
                Set<String> instances = getInstancesForClass(endpointURL, sibling);
                siblingInstances.put(sibling, instances);
                unionInstances.addAll(instances);
            }

            // Calculate intersections
            Map<String, Integer> intersections = new HashMap<>();
            for (String sibling : siblings) {
                Set<String> siblingSet = siblingInstances.get(sibling);
                for (String otherSibling : siblings) {
                    if (!sibling.equals(otherSibling)) {
                        Set<String> otherSiblingSet = siblingInstances.get(otherSibling);
                        Set<String> intersection = new HashSet<>(siblingSet);
                        intersection.retainAll(otherSiblingSet);
                        intersections.put(sibling + " ∩ " + otherSibling, intersection.size());
                    }
                }
            }

            siblingObject.put("unionCount", unionInstances.size());
            siblingObject.put("intersections", intersections);
            System.out.println("the siblings statistics are: " + siblingObject);

            siblingStats.add(siblingObject);
        }

        statistics.put("SiblingClasses", siblingStats);
    }

     */
    private static void addSiblingStatistics(QueryExecution queryExecution, JSONObject statistics, String endpointURL) {
        ResultSet resultSet = queryExecution.execSelect();
        Map<String, List<String>> siblingMap = new HashMap<>();

        // Retrieve sibling relationships
        while (resultSet.hasNext()) {
            QuerySolution solution = resultSet.nextSolution();
            String classUri = solution.get("class").toString();
            String siblingUri = solution.get("siblings").toString();

            siblingMap.putIfAbsent(classUri, new ArrayList<>());
            siblingMap.get(classUri).add(siblingUri);
        }

        // Calculate instances and intersections/unions for each class
        JSONArray siblingStats = new JSONArray();
        for (Map.Entry<String, List<String>> entry : siblingMap.entrySet()) {
            String classUri = entry.getKey();
            List<String> siblings = entry.getValue();

            JSONObject siblingObject = new JSONObject();
            siblingObject.put("class", classUri);
            siblingObject.put("siblings", siblings);

            // Retrieve instances of the class
            Set<String> classInstances = getInstancesForClass(endpointURL, classUri);

            // Prepare for storing intersections and unions
            Map<String, Map<String, Integer>> intersections = new HashMap<>();
            Map<String, Integer> unionCounts = new HashMap<>();

            // Calculate instances and intersections/unions for each sibling
            for (String sibling : siblings) {
                Set<String> siblingInstances = getInstancesForClass(endpointURL, sibling);

                // Calculate union
                Set<String> union = new HashSet<>(classInstances);
                union.addAll(siblingInstances);
                unionCounts.put(sibling, union.size());

                // Calculate intersection
                Set<String> intersection = new HashSet<>(classInstances);
                intersection.retainAll(siblingInstances);
                intersections.put(sibling, Collections.singletonMap("intersectionCount", intersection.size()));
            }

            siblingObject.put("unionCounts", unionCounts);
            siblingObject.put("intersections", intersections);

            siblingStats.add(siblingObject);
        }

        statistics.put("SiblingClasses", siblingStats);
    }

    private static Set<String> getInstancesForClass(String endpointURL, String classUri) {
        Set<String> instances = new HashSet<>();
        String query = "SELECT ?instance WHERE { ?instance a <" + classUri + "> }";
        QueryExecution queryExecution = QueryExecutionFactory.sparqlService(endpointURL, QueryFactory.create(query));
        ResultSet resultSet = queryExecution.execSelect();
        while (resultSet.hasNext()) {
            QuerySolution solution = resultSet.nextSolution();
            instances.add(solution.get("instance").toString());
        }
        return instances;
    }

    private static void addPropertyStatistics(QueryExecution queryExecution, JSONObject statistics, String endpointURL) {
        ResultSet resultSet = queryExecution.execSelect();
        JSONArray statList = (JSONArray) statistics.getOrDefault("Property", new JSONArray());

        while (resultSet.hasNext()) {
            QuerySolution solution = resultSet.nextSolution();
            Map<String, String> statValues = new HashMap<>();

            String property = solution.get("prop").toString();
            statValues.put("prop", property);
            statValues.put("count", solution.get("count").toString());

            // Check if the property has rdfs:range as xsd:integer
            String rangeCheckQueryStr = "PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>" +
                    "PREFIX xsd: <http://www.w3.org/2001/XMLSchema#>" +
                    "SELECT ?range WHERE { <" + property + "> rdfs:range ?range . FILTER(?range = xsd:integer) }";
            QueryExecution rangeCheckQuery = QueryExecutionFactory.sparqlService(endpointURL, QueryFactory.create(rangeCheckQueryStr));
            ResultSet rangeCheckResult = rangeCheckQuery.execSelect();

            if (rangeCheckResult.hasNext()) {
                // If the property has integer range, execute the min and max queries
                String minQueryStr = "SELECT (MIN(?value) AS ?min) WHERE { ?s <" + property + "> ?value . FILTER(datatype(?value) = <http://www.w3.org/2001/XMLSchema#integer>) }";
                String maxQueryStr = "SELECT (MAX(?value) AS ?max) WHERE { ?s <" + property + "> ?value . FILTER(datatype(?value) = <http://www.w3.org/2001/XMLSchema#integer>) }";

                QueryExecution minQuery = QueryExecutionFactory.sparqlService(endpointURL, QueryFactory.create(minQueryStr));
                QueryExecution maxQuery = QueryExecutionFactory.sparqlService(endpointURL, QueryFactory.create(maxQueryStr));

                statValues.put("min", getMinMaxValue(minQuery, "min"));
                statValues.put("max", getMinMaxValue(maxQuery, "max"));
            } else {
                statValues.put("min", "N/A");
                statValues.put("max", "N/A");
            }

            statList.add(statValues);
        }
        statistics.put("Property", statList);
    }
    private static String getMinMaxValue(QueryExecution queryExecution, String varName) {
        ResultSet resultSet = queryExecution.execSelect();
        if (resultSet.hasNext()) {
            QuerySolution solution = resultSet.nextSolution();
            RDFNode valueNode = solution.get(varName);
            if (valueNode != null && valueNode.isLiteral()) {
                return valueNode.asLiteral().getString();
            }
        }
        return "N/A";
    }
    private static String getMinMaxValue(QueryExecution queryExecution) {
        ResultSet resultSet = queryExecution.execSelect();
        if (resultSet.hasNext()) {
            QuerySolution solution = resultSet.nextSolution();
            RDFNode valueNode = solution.get("min") != null ? solution.get("min") : solution.get("max");
            if (valueNode != null && valueNode.isLiteral()) {
                return valueNode.asLiteral().getString();
            }
        }
        return "N/A";
    }
    private static void addCount(QueryExecution queryExecution, JSONObject statistics, String category) {
        ResultSet resultSet = queryExecution.execSelect();
        // Assuming the result set has only one row with one column, which is the count
        if (resultSet.hasNext()) {
            QuerySolution solution = resultSet.nextSolution();
            RDFNode countNode = solution.get("count");
            if (countNode != null && countNode.isLiteral()) {
                String countValue = countNode.asLiteral().getString();
                statistics.put(category, countValue);
            }
        }
        //statistics.put(category, resultSet.getRowNumber());
    }
    private static void addPropertyObjectFrequency(QueryExecution queryExecution, JSONObject statistics) {
        ResultSet resultSet = queryExecution.execSelect();
        JSONArray propObjFreqList = new JSONArray();

        while (resultSet.hasNext()) {
            QuerySolution solution = resultSet.nextSolution();
            JSONObject propObjFreq = new JSONObject();
            propObjFreq.put("property", solution.get("property").toString());
            propObjFreq.put("object", solution.get("object").toString());
            propObjFreq.put("count", solution.get("count").toString());
            propObjFreqList.add(propObjFreq);
        }
        statistics.put("PropertyObjectFrequency", propObjFreqList);
    }
}
