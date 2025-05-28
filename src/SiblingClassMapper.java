import com.google.gson.*;
import org.apache.jena.query.*;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.util.FileManager;

import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.*;

public class SiblingClassMapper {

    public static Map<Resource, Set<Resource>> mapSiblingClasses(Model ontology, Resource targetClass) {
        Map<Resource, Set<Resource>> siblingClassMap = new HashMap<>();

        // SPARQL query to retrieve sibling classes
        String queryString = "PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>"
        + "SELECT DISTINCT ?superclass ?siblingClass WHERE { "
                + "<" + targetClass.getURI() + "> rdfs:subClassOf ?superclass. "
                + "?siblingClass rdfs:subClassOf ?superclass. "
                + "}";

        // Execute the query
        Query query = QueryFactory.create(queryString);
        try (QueryExecution qexec = QueryExecutionFactory.create(query, ontology)) {
            ResultSet results = qexec.execSelect();
            while (results.hasNext()) {
                QuerySolution soln = results.nextSolution();
                RDFNode superclass = soln.get("superclass");
                RDFNode siblingClass = soln.get("siblingClass");
                if (superclass.isResource() && siblingClass.isResource()) {
                    Resource superClassRes = superclass.asResource();
                    Resource siblingClassRes = siblingClass.asResource();
                    if (!siblingClassMap.containsKey(superClassRes)) {
                        siblingClassMap.put(superClassRes, new HashSet<>());
                    }
                    siblingClassMap.get(superClassRes).add(siblingClassRes);
                }
            }
        }

        return siblingClassMap;
    }

    public static double SiblingSim (Resource class1, Resource class2, Resource superClass) {
        double sim = 0;
        String jsonFilePath = "data/statistics.json";
        JsonObject statistics = loadStatisticsFromJson(jsonFilePath);
        if (statistics != null && statistics.has("Class")) {
            JsonArray classStatistics = statistics.getAsJsonArray("Class");
            int totalInstances = getTotalInstances(jsonFilePath);
            double ic1 =-Math.log(getCountC(classStatistics, class1.getURI()) / (double) totalInstances);
            double ic2 =-Math.log(getCountC(classStatistics, class2.getURI()) / (double) totalInstances);
            double ic = -Math.log(getCountC(classStatistics, superClass.getURI()) / (double) totalInstances);
            sim = ic/ (ic1 + ic2 - ic);
        }
        return sim;
    }
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
    public static JsonObject loadStatisticsFromJson(String jsonFilePath){
        try (FileReader reader = new FileReader(jsonFilePath)) {
            return JsonParser.parseReader(reader).getAsJsonObject();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
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


    public static  List<Resource>  listClasses(Model ontology) {
        List<Resource> classList = new ArrayList<>();
        // SPARQL query to retrieve all classes
        String queryString = "PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>"
                + "PREFIX owl: <http://www.w3.org/2002/07/owl#>"
        +"SELECT DISTINCT ?class WHERE { "
                + "{ ?class a rdfs:Class } "
                + "UNION "
                + "{ ?class a owl:Class } "
                + "FILTER (?class != <http://www.w3.org/2000/01/rdf-schema#Class>) "
                + "}";

        // Execute the query
        Query query = QueryFactory.create(queryString);
        try (QueryExecution qexec = QueryExecutionFactory.create(query, ontology)) {
            ResultSet results = qexec.execSelect();
            while (results.hasNext()) {
                QuerySolution soln = results.nextSolution();
                RDFNode classNode = soln.get("class");
                if (classNode != null && classNode.isResource()) {
                    classList.add(classNode.asResource());
                    //System.out.println("Class: " + classNode.asResource().getURI());
                }
            }
        }
        return classList;
    }

    public static void main(String[] args) {
        // Example usage
        //String filePath = "/Users/e20d463s/Downloads/univ-bench.owl";
        String filePath = "data/toyexampleontology.ttl";
        // Create an RDF model
        Model ontology = ModelFactory.createDefaultModel();
        // Load your Ontology
        FileManager.get().readModel(ontology, filePath);

        // Create a JSON object to store the results
        JsonObject jsonResult = new JsonObject();

        // List classes in the ontology
        List<Resource> classList = listClasses(ontology);
        // Create a JSON array to store the results
        JsonArray jsonResultArray = new JsonArray();


        for ( Resource targetClass : classList) {
            System.out.println("the target class is " + targetClass);
           // Create a JSON object for the current target class
            JsonObject targetClassObject = new JsonObject();

            // Add the target class URI to the object
            targetClassObject.addProperty("targetClass", targetClass.getURI());

            // Create a JSON array for sibling classes and their similarities
            JsonArray siblingArray = new JsonArray();
            //Resource targetClass = ResourceFactory.createResource("http://example.org/Student");
        //Resource targetClass = ResourceFactory.createResource("http://swat.cse.lehigh.edu/onto/univ-bench.owl#AssociateProfessor");
        Map<Resource, Set<Resource>> siblingClassMap = mapSiblingClasses(ontology, targetClass);
        //System.out.println("Sibling classes of " + targetClass.getURI() + ":");
        for (Map.Entry<Resource, Set<Resource>> entry : siblingClassMap.entrySet()) {
            // Create a JSON object for the current sibling class
            JsonObject siblingObject = new JsonObject();

            // Add the sibling class URI to the object
            siblingObject.addProperty("siblingClass", entry.getKey().getURI());
            System.out.println("Superclass: " + entry.getKey().getURI());
            System.out.println("Sibling Classes: " + entry.getValue());
            for (Resource sibling : entry.getValue()) {
                double sim = SiblingSim(targetClass, sibling, entry.getKey());
                System.out.println("The similarity between " + targetClass + " and " + sibling + " is " + sim);
                // Add sibling class URI and its similarity to the JSON object
                siblingObject.addProperty(sibling.getURI(), sim);
            }
               // Add the sibling object to the sibling array
            siblingArray.add(siblingObject);
        }
            // Add the sibling array to the target class object
            targetClassObject.add("siblings", siblingArray);

            // Add the target class object to the JSON result array
            jsonResultArray.add(targetClassObject);
        }
        // Write the JSON array to a file
        try (FileWriter fileWriter = new FileWriter("data/siblingMapping2.json")) {
            Gson gson = new Gson();
            gson.toJson(jsonResultArray, fileWriter);
            System.out.println("Results saved to siblingMapping2.json");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    }

