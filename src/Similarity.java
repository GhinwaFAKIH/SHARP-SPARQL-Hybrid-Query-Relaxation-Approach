import com.google.gson.*;
import org.apache.jena.query.Query;
import org.apache.jena.query.QueryFactory;
import org.apache.jena.sparql.core.TriplePath;
import org.apache.jena.sparql.syntax.ElementPathBlock;
import org.apache.jena.sparql.syntax.ElementVisitorBase;
import org.apache.jena.sparql.syntax.ElementWalker;
import org.json.simple.JSONObject;

import java.io.*;
import java.util.stream.Collectors;

public class Similarity {
    private static class QueryTraversalVisitor extends ElementVisitorBase {
        private final StringBuilder triplePatterns = new StringBuilder();
        public static JsonObject compareQueries(String query1st, String query2st) {
            Query query1 = QueryFactory.create(query1st);
            Query query2 = QueryFactory.create(query2st);

            String[] patterns1 = getTriplePatterns(query1);
            String[] patterns2 = getTriplePatterns(query2);

            int triplepat = 30;


            // Move the array outside the loop
            double sim = 0.0;
            for (int i = 0; i < triplepat; i++) {
                if (!patterns1[i].equals(patterns2[i])) {
                    System.out.println("relaxing " + patterns1[i] + " to " + patterns2[i]);
                    if (patterns2[i].startsWith("?")) {
                        //sim = Math.cbrt(0.1);
                        sim = 2.0/3.0;
                        System.out.println("it is simple relaxation and the similarity is cubic root of 0.1");
                        //System.out.println("it is simple relaxation and the similarity is 2/3");
                    } else {
                        if (checkTermType(patterns1[i]).equals("Class")) {
                            sim = tripsim(" type relaxation", patterns1[i], patterns2[i]);
                            System.out.println("it is type relaxation and the similarity is " + sim);
                        } else if (checkTermType(patterns1[i]).equals("Property")) {
                            System.out.println("it is property relaxation and the similarity is " + tripsim(" property relaxation", patterns1[i], patterns2[i]));
                            sim = tripsim(" property relaxation", patterns1[i], patterns2[i]);
                        }
                    }

                }
            }
            // Save the information to JSON
            JsonObject relaxationInfo = new JsonObject();
            relaxationInfo.addProperty("Query", query2.toString());
            //relaxationInfo.addProperty("pattern2", patterns2[i]);
            relaxationInfo.addProperty("Similarity", sim);
            System.out.println("the json file " + relaxationInfo);
            return relaxationInfo;
        }
        @Override
        public void visit(ElementPathBlock el) {
            // Extract information about triple patterns
            for (TriplePath triple : el.getPattern().getList()) {
                String triplePattern = triple.getSubject() + " " + triple.getPredicate() + " " + triple.getObject();
                triplePatterns.append(triplePattern).append(" ");
            }
           // return null;
        }

        public String[] getTriplePatterns() {
            return triplePatterns.toString().trim().split("\\s+");
        }

        private static String[] getTriplePatterns(Query query) {
            // Traverse the query using ElementWalker
            QueryTraversalVisitor visitor = new QueryTraversalVisitor();
            ElementWalker.walk(query.getQueryPattern(), visitor);

            // Split triple patterns
            return visitor.getTriplePatterns();
        }

        public static String checkTermType(String term) {
            String jsonFilePath = "data/statistics.json";
            try (FileReader reader = new FileReader(jsonFilePath)) {
                // Parse the JSON file
                JsonObject jsonObject = JsonParser.parseReader(reader).getAsJsonObject();

                // Check if the term is in the "Class" section
                JsonArray classArray = jsonObject.getAsJsonArray("Class");
                for (int i = 0; i < classArray.size(); i++) {
                    JsonObject classObject = classArray.get(i).getAsJsonObject();
                    String classTerm = classObject.getAsJsonPrimitive("class").getAsString();
                    if (term.equals(classTerm)) {
                        return "Class";
                    }
                }

                // Check if the term is in the "Property" section
                JsonArray propertyArray = jsonObject.getAsJsonArray("Property");
                for (int i = 0; i < propertyArray.size(); i++) {
                    JsonObject propertyObject = propertyArray.get(i).getAsJsonObject();
                    String propertyTerm = propertyObject.getAsJsonPrimitive("prop").getAsString();
                    if (term.equals(propertyTerm)) {
                        return "Property";
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }

            // Term not found in either section
            return "Unknown";
        }
/*
        public static void querycomparator(String query1st, String query2st) {
            Query query1 = QueryFactory.create(query1st);
            Query query2 = QueryFactory.create(query2st);

            String[] patterns1 = getTriplePatterns(query1);
            String[] patterns2 = getTriplePatterns(query2);
            // Convert arrays to strings for printing
            String patterns1String = String.join(", ", patterns1);
            String patterns2String = String.join(", ", patterns2);
            int triplepat = 30;

            //System.out.println("patterns 1 " + patterns1String);
            //System.out.println("patterns 2 " + patterns2String);
            for (int i = 0; i < triplepat; i++) {
                if (!patterns1[i].equals(patterns2[i])) {
                    System.out.println("relaxing " + patterns1[i] + " to " + patterns2[i]);
                    if(patterns2[i].startsWith("?")){
                        System.out.println("it is simple relaxation and the similarity is 2/3");
                    }
                    else {
                        double sim = tripsim(" type relaxation", patterns1[i], patterns2[i]);
                        if( sim != 1) {
                        System.out.println("it is type relaxation and the similarity is " + tripsim(" type relaxation",patterns1[i],patterns2[i])); }
                        else{
                        System.out.println("it is property relaxation and the similarity is " +tripsim(" property relaxation",patterns1[i],patterns2[i])); }
                    }
                }
            }

        }

 */
        private static JsonObject loadStatisticsFromJson(String jsonFilePath) {
            try (FileReader reader = new FileReader(jsonFilePath)) {
                return JsonParser.parseReader(reader).getAsJsonObject();
            } catch (IOException e) {
                e.printStackTrace();
            }
            return null;
        }
        public static void saveToJson(JsonObject jsonObject, String outputPath) {
            try (FileWriter writer = new FileWriter(outputPath)) {
                Gson gson = new GsonBuilder().setPrettyPrinting().create();
                writer.write(gson.toJson(jsonObject));
                writer.flush();
                //writer.write(jsonObject.toString());
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        public static double simclass(String class1, String class2) {
            String jsonFilePath = "data/statistics.json";
            JsonObject statistics = loadStatisticsFromJson(jsonFilePath);
            if (statistics != null && statistics.has("Class")) {
                JsonArray classStatistics = statistics.getAsJsonArray("Class");
                int INST1 = getCountC(classStatistics, class1);
                int INST2 = getCountC(classStatistics, class2);
                int total_count = 54;
                System.out.println("instances of class "  + class1 +" are " +  INST1);
                System.out.println("instances of class "  + class2 +" are " +  INST2);
                System.out.println("similarity is  "  + Math.log((7.0/54.0)));
                return (double) Math.log((double)INST2/total_count)/Math.log((double)INST1/total_count) ;
            }
            return 1; // Return 0.0 if statistics are not available
        }
        public static double simprop(String prop1, String prop2) {
            String jsonFilePath = "data/statistics.json";
            JsonObject statistics = loadStatisticsFromJson(jsonFilePath);
            if (statistics != null && statistics.has("Property")) {
                JsonArray propStatistics = statistics.getAsJsonArray("Property");
                int INST1 = getCountP(propStatistics, prop1);
                int INST2 = getCountP(propStatistics, prop2);
                int total_count = 122;
                return (double) Math.log((double)INST2/total_count)/Math.log((double)INST1/total_count);
            }
            return 1; // Return 0.0 if statistics are not available
        }


        public static double tripsim(String relaxtype, String initial, String finale) {
            //System.out.println("the type is " + relaxtype);
            double sim = 0;
            if (" simple relaxation".equals(relaxtype)) {
                //sim = Math.cbrt(0.1);
                sim = 2.0 / 3.0;
            } else if (" type relaxation".equals(relaxtype)) {
                //sim = Math.cbrt(simclass(initial, finale));
                sim = 2.0 / 3.0 + (simclass(initial, finale)) / 3.0;
            } else if (" property relaxation".equals(relaxtype)) {
                //sim = Math.cbrt(simprop(initial, finale));
                sim = 2.0 / 3.0 + (simprop(initial, finale)) / 3.0;
            }
            //System.out.println("the sim is " + sim);
            return sim;
        }

        private static int getCountC(JsonArray stats, String key) {
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
        private static int getCountP(JsonArray stats, String key) {
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

        public static void main(String[] args) {
            // Create a JSON file
            JSONObject similarities = new JSONObject();

            String filePath = "data/relaxtype"; // Replace with the actual file path
            // File containing SPARQL queries
            String queriesFile = "data/Queries";
            String prefixes = "PREFIX xsd: <http://www.w3.org/2001/XMLSchema#> \n" +
                    "          PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#> \n" +
                    "          PREFIX ex: <http://example.org/> \n" +
                    "          PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#> \n" +
                    "          PREFIX owl: <http://www.w3.org/2002/07/owl#> \n" +
                    "          PREFIX foaf: <http://xmlns.com/foaf/0.1/> \n" +
                    "          ";
            String initialquery = " Select * where {\n" +
                    " ?teacher       rdf:type              ex:Teacher;\n" +
                    "                  ex:teaches            ex:Semantic_Web.\n" +
                    "   ?student       rdf:type              ex:Student;\n" +
                    "                  ex:friendOf/foaf:knows  ex:Alice;\n" +
                    "                  foaf:knows              ?teacher.\n" +
                    "   ?statement     rdf:type              rdf:Statement;\n" +
                    "                  rdf:subject           ?student;\n" +
                    "                  rdf:object            ex:Semantic_Web;\n" +
                    "                  rdf:predicate         ex:enrolledIn;\n" +
                    "                  ex:enrolldate         \"2023-09-13\"^^xsd:date. }";

            //double sim = simclass("http://example.org/Student", "http://example.org/Person");
            //System.out.println("The similarity is " + sim);

            try (BufferedReader reader = new BufferedReader(new FileReader(queriesFile))) {
                // Read the entire content of the file as a single query
                String content = reader.lines().collect(Collectors.joining(System.lineSeparator()));

                // Split the content into individual queries based on a delimiter (e.g., ";")
                String[] queries = content.split("//");
                JsonArray relaxationsArray = new JsonArray();
                for (String query : queries) {
                    System.out.println("query "+query);
                    //querycomparator(prefixes + initialquery, prefixes + query);
                    JsonObject relaxationInfo = compareQueries(prefixes + initialquery, prefixes + query);
                    relaxationsArray.add(relaxationInfo);
                }
                System.out.println("the json array is " + relaxationsArray);
                JsonObject resultJson = new JsonObject();
                resultJson.add("relaxations", relaxationsArray);
                saveToJson(resultJson, "data/similarity_add.json");
            } catch (IOException e) {
                e.printStackTrace();
            }

            try (BufferedReader reader = new BufferedReader(new FileReader("data/similarity_add.json"))) {
                // Read the entire content of the JSON file
                String content = reader.lines().collect(Collectors.joining(System.lineSeparator()));

                // Parse the JSON content
                JsonObject resultJson = JsonParser.parseString(content).getAsJsonObject();

                // Get the "relaxations" array
                JsonArray relaxationsArray = resultJson.getAsJsonArray("relaxations");

                // Define the output file path
                String outputFile = "data/similarity_info_add.txt";

                // Write query and similarity information to the text file
                try (BufferedWriter writer = new BufferedWriter(new FileWriter(outputFile))) {
                    int queryNumber = 1;
                    for (JsonElement relaxationElement : relaxationsArray) {
                        JsonObject relaxationInfo = relaxationElement.getAsJsonObject();

                        // Extract query and similarity information
                        String query = relaxationInfo.getAsJsonPrimitive("Query").getAsString();
                        double similarity = relaxationInfo.getAsJsonPrimitive("Similarity").getAsDouble();

                        // Write information to the text file
                        writer.write("Query " + "Q" + queryNumber + ":\n");
                        writer.write(query + "\n");
                        writer.write("Similarity: " + similarity + "\n");
                        writer.write("------------------------------\n");
                        queryNumber++;
                    }

                    System.out.println("Query and similarity information saved to " + outputFile);
                } catch (IOException e) {
                    e.printStackTrace();
                }

            } catch (IOException e) {
                e.printStackTrace();
            }


        }


    }
}
