package RDFStarRelaxer;

import Similarity.QuerySimilarity;
import com.google.gson.JsonElement;
import org.apache.jena.graph.Node;
import com.google.gson.JsonObject;
import com.google.gson.JsonArray;
import com.google.gson.JsonParser;
import org.apache.jena.graph.NodeFactory;
import org.apache.jena.graph.Node_Triple;
import org.apache.jena.graph.Triple;
import org.apache.jena.query.Query;
import org.apache.jena.query.QueryExecution;
import org.apache.jena.query.QueryExecutionFactory;
import org.apache.jena.query.QueryFactory;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.NodeIterator;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.ResourceFactory;
import org.apache.jena.sparql.core.TriplePath;
import org.apache.jena.sparql.path.P_Path0;
import org.apache.jena.sparql.path.P_Path2;
import org.apache.jena.sparql.path.Path;
import org.apache.jena.sparql.path.PathFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.apache.jena.vocabulary.OWL;
import org.apache.jena.datatypes.RDFDatatype;
import org.apache.jena.datatypes.xsd.XSDDatatype;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.*;

import static org.apache.jena.vocabulary.RDF.type;
import static org.apache.jena.vocabulary.RDFS.subClassOf;
import static org.apache.jena.vocabulary.RDFS.subPropertyOf;

public class TriplePatternRelaxer {
        protected static final Logger log = LoggerFactory.getLogger(TriplePatternRelaxer.class);
        // Assuming the CSV file has two columns: 'node' and 'most_similar_node'
       // Specify the path of entity mapping matrix
        private static final String csvFilePath = "Benchmark_similarity_matrices/undergraduateCourses_similarity_matrix.csv";
        //"Benchmark_similarity_matrices/graduateCourses_similarity_matrix.csv";
                //"Benchmark_similarity_matrices/department_similarity_matrix.csv";
                //"Benchmark_similarity_matrices/headOfRange_similarity_matrix.csv";
        private static final String DELIMITER = ",";




        // Assuming the CSV file has two columns: 'literal' and 'most_similar_literal'
        // Specify the path of literal mapping matrix
        private static final String literal_matrix_path_3 = "data/semester_similarity_matrix.csv";
        //"data/similarity_matrix.csv";

        // Map to store similarity mappings from CSV
        private static Map<String, Map<String, Double>> similarityInstanceMap = new HashMap<>();
        private static Map<String, Map<String, Double>> similarityLiteralMap1 = new HashMap<>();
        private static Map<Integer, Map<Integer, Double>> similarityLiteralMap2 = new HashMap<>();



    // Method to load the similarity matrix
    public static void loadSimilarityMatrix2 (String csvFilePath) throws IOException {
        similarityInstanceMap = loadSimilarityMatrix(csvFilePath);
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


    // This method will load the CSV similarity matrix into a map
    public static Map<String, Map<String, Double>> loadSimilarityMatrix(String csvFilePath) throws IOException {
        Map<String, Map<String, Double>> similarityMap  = new HashMap<>();

        try (BufferedReader br = new BufferedReader(new FileReader(csvFilePath))) {
            String line;
            List<String> instances = new ArrayList<>();

            // First line should contain the headers (instance names)
            if ((line = br.readLine()) != null) {
                String[] headers = line.split(",");
                for (int i = 1; i < headers.length; i++) {  // Start from index 1 to skip the first column header
                    instances.add(headers[i].trim());
                }
            }

            // Reading the matrix
            while ((line = br.readLine()) != null) {
                String[] values = line.split(",");
                String rowInstance = values[0].trim();  // First value in the row is the instance name
                Map<String, Double> similarityRow = new HashMap<>();

                for (int colIndex = 1; colIndex < values.length; colIndex++) {  // Start from index 1 to skip the row instance name
                    double similarityValue = Double.parseDouble(values[colIndex]);
                    String colInstance = instances.get(colIndex - 1);  // Adjust the index to match the headers
                    similarityRow.put(colInstance, similarityValue);
                }
                similarityMap.put(rowInstance, similarityRow);
            }
        }
        return similarityMap;
    }




        public static Set<TriplePath> relax2 (TriplePath triple, Model ontology, QuerySimilarity Similarity) throws IOException {
            //System.out.println("The triple to be relaxed is :" + triple);
            Set<TriplePath> relaxedTriples = new HashSet<>();
            //TriplePath relaxedTriple = null;
            if (triple.getPredicate() !=null) {
                Node subject = triple.getSubject();
                Node object = triple.getObject();
                if (!triple.getSubject().isNodeTriple() && !triple.getObject().isNodeTriple()) {
                    if (!triple.getPredicate().isVariable()) {
                        if (triple.getPredicate().getURI().equals(type.getURI())) {
                            //rdf:type -> type relaxation, sibling relaxation
                            // Ensure the object is NOT owl:Class before proceeding
                            if (!triple.getObject().equals(OWL.Class.asNode())) {
                            //System.out.println("type relaxation to be done");
                            TriplePath relaxedTriple = typeRelaxation(triple, ontology);
                            if(relaxedTriple != null) {
                                relaxedTriples.add(relaxedTriple);
                            }
                            // Sibling relaxation
                            System.out.println("triple to be relaxed using sibling relaxation: " + triple);
                            Set<TriplePath> siblingRelaxedTriples = siblingRelaxation(triple, ontology);
                            System.out.println("the relaxed triples from sibling relaxation: " + siblingRelaxedTriples);
                            relaxedTriples.addAll(siblingRelaxedTriples);
                            //relaxedTriple = typeRelaxation(triple, ontology);
                            //System.out.println("the relaxed triple is " + relaxedTriple);
                        } 
                    } else {
                            if (isObjectInstance(object)) {
                                //System.out.println("object could be relaxed using instance relaxation");
                                //TriplePath relaxedTriple = instanceRelaxation(triple, "object");
                                //if (relaxedTriple != null) {
                                //relaxedTriples.add(relaxedTriple);}
                                // Object instance relaxation
                                Set<TriplePath> relaxedObjectTriples = instanceRelaxation(triple, "object", 10); // Use top 10 similar instances
                                if (relaxedObjectTriples != null && !relaxedObjectTriples.isEmpty()) {
                                    relaxedTriples.addAll(relaxedObjectTriples);
                                } else {
                                    // fallback to simple relaxation
                                    TriplePath simpleRelaxedTriple = simpleRelaxation(triple);
                                    if (simpleRelaxedTriple != null) {
                                        relaxedTriples.add(simpleRelaxedTriple);
                                    }
                                }
                                //relaxedTriples.addAll(relaxedObjectTriples);
                                //System.out.println("relaxed triples is " + relaxedTriples);
                            } else if (isObjectLiteral(object)) {
                                //System.out.println("object could be relaxed using literal relaxation");
                                System.out.println("it is literal relaxation");
                                TriplePath relaxedTriple = null;
                                //System.out.println("the datatype uri: "+ object.getLiteralDatatype().getURI().equals("http://www.w3.org/2001/XMLSchema#string"));
                                if (object.getLiteralDatatype().getURI().equals("http://www.w3.org/2001/XMLSchema#integer")) {
                                    loadLiteralMap2(literal_matrix_path);
                                    Set<TriplePath> relaxedObjectTriples = literalRelaxationInteger(triple, "object", similarityLiteralMap2, 10);
                                    if (relaxedObjectTriples != null && !relaxedObjectTriples.isEmpty()) {
                                        relaxedTriples.addAll(relaxedObjectTriples);
                                    } else {
                                        // fallback to simple relaxation
                                        TriplePath simpleRelaxedTriple = simpleRelaxation(triple);
                                        if (simpleRelaxedTriple != null) {
                                            relaxedTriples.add(simpleRelaxedTriple);
                                        }
                                    }
                                } 
                                else if (object.getLiteralDatatype().getURI().equals("http://www.w3.org/2001/XMLSchema#string")) {
                                    loadLiteralMap1(literal_matrix_path_3);
                                    List<TriplePath> relaxedObjectTriples = literalRelaxationString(triple, "object", similarityLiteralMap1);
                                    if (relaxedObjectTriples != null && !relaxedObjectTriples.isEmpty()) {
                                        relaxedTriples.addAll(relaxedObjectTriples);
                                    } else {
                                        // fallback to simple relaxation
                                        TriplePath simpleRelaxedTriple = simpleRelaxation(triple);
                                        if (simpleRelaxedTriple != null) {
                                            relaxedTriples.add(simpleRelaxedTriple);
                                        }
                                    }
                                }
                                
                        }
                            if (isObjectInstance(subject)) {
                                // Subject instance relaxation
                                Set<TriplePath> relaxedSubjectTriples = instanceRelaxation(triple, "subject", 5); // Use top 10 similar instances
                                relaxedTriples.addAll(relaxedSubjectTriples);
                                //TriplePath relaxedTriple = instanceRelaxation(triple, "subject");
                                //if (reiteralaxedTriple != null) {
                                    //relaxedTriples.add(relaxedTriple);}
                            }
                            // predicate is not a variable and not a rdf:type -> property relaxation
                            TriplePath relaxedTriple = propertyRelaxation(triple, ontology);
                            if(relaxedTriple != null) {
                                relaxedTriples.add(relaxedTriple);
                            }
                        }
                    }
                    // simple relax
                    if (relaxedTriples.isEmpty()) {
                        TriplePath relaxedTriple = simpleRelaxation(triple);
                        System.out.println("it is simple relaxation");
                        //System.out.println("relaxed triple (with simple relaxation) is " + relaxedTriple);
                        if(relaxedTriple != null) {
                            relaxedTriples.add(relaxedTriple);
                        }
                    }
                }
            }
            // property path relaxation
            /* 
            else {
                System.out.println("property path relaxation");
                TriplePath relaxedTriple1 = propertyRelaxation(triple, ontology);
                if(relaxedTriple1 != null) {
                    relaxedTriples.add(propertyRelaxation(triple, ontology));
                }
                Node subject = triple.getSubject();
                Node object = triple.getObject();
                if (isObjectInstance(object)) {
                    TriplePath relaxedTriple = instanceRelaxation(triple, "object");
                    if (relaxedTriple != null) {
                        relaxedTriples.add(instanceRelaxation(triple, "object"));}
                } else if (isObjectLiteral(object)) {
                    TriplePath relaxedTriple = instanceRelaxation(triple, "object");
                    if (relaxedTriple != null) {
                        relaxedTriples.add(literalRelaxation(triple, "object", similarityLiteralMap));}
                }
                if (isObjectInstance(subject)) {
                    TriplePath relaxedTriple = instanceRelaxation(triple, "object");
                    if (relaxedTriple != null) {
                        relaxedTriples.add(instanceRelaxation(triple, "object"));}
                } else if (isObjectLiteral(subject)) {
                    TriplePath relaxedTriple = instanceRelaxation(triple, "object");
                    if (relaxedTriple != null) {
                        relaxedTriples.add(literalRelaxation(triple, "object", similarityLiteralMap));}
                }
            }
            */
            if(triple.getSubject().isNodeTriple()) {
                Triple subject = triple.getSubject().getTriple();
                TriplePath subjtriplepath = new TriplePath(subject);
                //System.out.println("the subject triple path is " + subjtriplepath);
                Set<TriplePath> relaxedsubjTriples = relax2(subjtriplepath, ontology, Similarity);
                //TriplePath relaxedsubj = relax (subjtriplepath, ontology, Similarity);
                /*
                if (relaxedsubj != null) {
                    Node_Triple node_triple = new Node_Triple(relaxedsubj.getSubject(), relaxedsubj.getPredicate(), relaxedsubj.getObject());
                    Triple relaxedtriple = new Triple(node_triple, triple.getPredicate(), triple.getObject());
                    relaxedTriple = new TriplePath(relaxedtriple);
                    //System.out.println("the relaxed triple pattern is " + relaxedTriple);
                }
                 */
                if (!relaxedsubjTriples.isEmpty()) {
                    for (TriplePath relaxedsubjTriple : relaxedsubjTriples) {
                        Node_Triple node_triple = new Node_Triple(relaxedsubjTriple.getSubject(), relaxedsubjTriple.getPredicate(), relaxedsubjTriple .getObject());
                        Triple relaxedtriple = new Triple(node_triple, triple.getPredicate(), triple.getObject());
                        relaxedTriples.add(new TriplePath(relaxedtriple));
                        //System.out.println("the relaxed triple pattern is " + relaxedTriple);
                    }
                }
                // Add here set of relaxed triples when relaxing the object
                TriplePath intermediate_triple = new TriplePath (generateUniqueVariable(), PathFactory.pathLink(triple.getPredicate()), triple.getObject());
                Set<TriplePath> relaxedobjTriples = relax2(intermediate_triple, ontology, Similarity);
                if (!relaxedobjTriples.isEmpty()) {
                    for (TriplePath relaxedobjTriple : relaxedobjTriples) {
                        Triple subjectpath = triple.getSubject().getTriple();
                        TriplePath subjtriplepath2 = new TriplePath(subjectpath);
                        Node_Triple node_triple = new Node_Triple (subjtriplepath2.getSubject(),subjtriplepath2.getPredicate(),subjtriplepath2.getObject());
                        Triple relaxedtriple = new Triple(node_triple, relaxedobjTriple.getPredicate(), relaxedobjTriple.getObject());
                        relaxedTriples.add(new TriplePath(relaxedtriple));
                        System.out.println("the relaxed triple pattern is " + relaxedtriple);

                    }
                }
            }
            if(triple.getObject().isNodeTriple()) {
                Triple object = triple.getObject().getTriple();
                TriplePath objtriplepath = new TriplePath(object);
                Set<TriplePath> relaxedobjTriples = relax2(objtriplepath, ontology, Similarity);
                if (!relaxedobjTriples.isEmpty()) {
                    for (TriplePath relaxedobjTriple : relaxedobjTriples) {
                        Node_Triple node_triple = new Node_Triple(relaxedobjTriple.getSubject(), relaxedobjTriple.getPredicate(), relaxedobjTriple.getObject());
                        Triple relaxedtriple = new Triple(node_triple, triple.getPredicate(), triple.getObject());
                        relaxedTriples.add(new TriplePath(relaxedtriple));
                    }
                }
            }
            return relaxedTriples;
        }

        public static TriplePath relax(TriplePath triple, Model ontology, QuerySimilarity Similarity) {
            //System.out.println("The triple to be relaxed is :" + triple);
            TriplePath relaxedTriple = null;
            //TriplePath relaxedTriple = null;
            if (triple.getPredicate() !=null) {
                Node subject = triple.getSubject();
                Node object = triple.getObject();
                if (!triple.getSubject().isNodeTriple() && !triple.getObject().isNodeTriple()) {
                    if (!triple.getPredicate().isVariable()) {
                        if (triple.getPredicate().getURI().equals(type.getURI())) {
                            //rdf:type -> type relaxation
                            relaxedTriple = typeRelaxation(triple, ontology);
                            //relaxedTriple = typeRelaxation(triple, ontology);
                        } else {
                            // predicate is not a variable and not a rdf:type -> property relaxation
                            relaxedTriple = propertyRelaxation(triple, ontology);
                        }
                    }
                    // simple relax
                    if (relaxedTriple == null) {
                        relaxedTriple = simpleRelaxation(triple);
                    }
                }
            }
            // property path relaxation
            else {
                relaxedTriple = propertyPathRelaxation(triple, ontology);
            }
            if(triple.getSubject().isNodeTriple()) {
                Triple subject = triple.getSubject().getTriple();
                TriplePath subjtriplepath = new TriplePath(subject);
                TriplePath relaxedsubj = relax (subjtriplepath, ontology, Similarity);

                if (relaxedsubj != null) {
                    Node_Triple node_triple = new Node_Triple(relaxedsubj.getSubject(), relaxedsubj.getPredicate(), relaxedsubj.getObject());
                    Triple relaxedtriple = new Triple(node_triple, triple.getPredicate(), triple.getObject());
                    relaxedTriple = new TriplePath(relaxedtriple);
                }



            }
            if(triple.getObject().isNodeTriple()) {
                Triple object = triple.getObject().getTriple();
                TriplePath objtriplepath = new TriplePath(object);
                TriplePath relaxedobj = relax (objtriplepath, ontology, Similarity);

                if (relaxedobj != null) {
                    Node_Triple node_triple = new Node_Triple(relaxedobj.getSubject(), relaxedobj.getPredicate(), relaxedobj.getObject());
                    Triple relaxedtriple = new Triple(node_triple, triple.getPredicate(), triple.getObject());
                    relaxedTriple = new TriplePath(relaxedtriple);
                }
                //relax (objtriplepath, ontology, Similarity);
            }
            return relaxedTriple;
        }

        private static TriplePath simpleRelaxation(TriplePath originalTriple) {
            TriplePath relaxedTriple = null;
                if (!originalTriple.getObject().isVariable()) {
                    // on object
                    if (!originalTriple.getPredicate().isVariable() && originalTriple.getPredicate().getURI().equals(type.getURI())) {
                        // if class is relaxed to a variable, then we do the same for rdf:type predicate.
                        relaxedTriple = new TriplePath(originalTriple.getSubject(), PathFactory.pathLink(generateUniqueVariable()), generateUniqueVariable());
                    } else {
                        relaxedTriple = new TriplePath(originalTriple.getSubject(), PathFactory.pathLink(originalTriple.getPredicate()), generateUniqueVariable());
                    }
                } else if (!originalTriple.getSubject().isVariable()) {
                    // on subject
                    relaxedTriple = new TriplePath(generateUniqueVariable(), PathFactory.pathLink(originalTriple.getPredicate()), originalTriple.getObject());
                } else if (!originalTriple.getPredicate().isVariable()) {
                    // on predicate
                    relaxedTriple = new TriplePath(originalTriple.getSubject(), PathFactory.pathLink(generateUniqueVariable()), originalTriple.getObject());
                }

                return relaxedTriple;

        }

        private static TriplePath propertyRelaxation(TriplePath originalTriple, Model ontology) {
            String propertyURI = originalTriple.getPredicate().getURI();
            NodeIterator superPropertyIter = ontology.listObjectsOfProperty(ResourceFactory.createResource(propertyURI), subPropertyOf);
            if (superPropertyIter.hasNext()) {
                RDFNode superProperty = superPropertyIter.next();
                return new TriplePath(originalTriple.getSubject(), PathFactory.pathLink(NodeFactory.createURI(superProperty.asResource().getURI())), originalTriple.getObject());
            } else {
                return null;
            }
        }
        private static TriplePath propertyPathRelaxation ( TriplePath originalTriple, Model ontology) {
            Path propertypath = originalTriple.getPath();
            P_Path2 step = (P_Path2) propertypath;
            P_Path0 leftPredicate = (P_Path0) step.getLeft();
            P_Path0 rightPredicate = (P_Path0) step.getRight();
            NodeIterator superPropertyIter1 = ontology.listObjectsOfProperty(ResourceFactory.createResource(leftPredicate.getNode().getURI()), subPropertyOf);
            NodeIterator superPropertyIter2 = ontology.listObjectsOfProperty(ResourceFactory.createResource(rightPredicate.getNode().getURI()), subPropertyOf);
            if (superPropertyIter1.hasNext()) {
                RDFNode superProperty1 = superPropertyIter1.next();
                Path newPath1 = PathFactory.pathLink(NodeFactory.createURI(superProperty1.asResource().getURI()));
                Path newPath2 = PathFactory.pathLink(NodeFactory.createURI(rightPredicate.getNode().getURI()));
                // Combine the relaxed predicates into a single path
                Path newPath = PathFactory.pathAlt(newPath1, newPath2);
                System.out.println("property path is relaxed to " + new TriplePath(originalTriple.getSubject(), newPath , originalTriple.getObject()));
                return new TriplePath(originalTriple.getSubject(), newPath , originalTriple.getObject());
            }
            return new TriplePath(originalTriple.getSubject(), PathFactory.pathLink(generateUniqueVariable()), originalTriple.getObject());
        }

 
        private static TriplePath typeRelaxation(TriplePath originalTriple, Model ontology) {
            String classURI = originalTriple.getObject().getURI();

            NodeIterator superClassIter = ontology.listObjectsOfProperty(ResourceFactory.createResource(classURI), subClassOf);

            while (superClassIter.hasNext()) {
                RDFNode superClass = superClassIter.next();

                // Check if superClass is a resource and has a URI
                if (superClass.isResource() && superClass.asResource().getURI() != null) {
                    String superClassURI = superClass.asResource().getURI();
                    return new TriplePath(originalTriple.getSubject(), originalTriple.getPath(), NodeFactory.createURI(superClassURI));
                }
            }

            // If no valid superclass is found
            //System.err.println("No valid superclass found for class: " + classURI);
            return null;
        }
        private static Set<TriplePath> siblingRelaxation(TriplePath originalTriple, Model ontology) {
            String jsonFilePath = "data/lubm_statistics_siblings.json";
            JsonObject statistics = loadStatisticsFromJson(jsonFilePath);
            Set<TriplePath> siblingTriples = new HashSet<>();
            String originalClassURI = originalTriple.getObject().getURI();
        
            // Extract superclass URI
            // Step 1: Find the superclass of the original class
            NodeIterator superClassIter = ontology.listObjectsOfProperty(ResourceFactory.createResource(originalClassURI), subClassOf);

            while (superClassIter.hasNext()) {
                RDFNode superClass = superClassIter.next();

                // Check if superClass is a resource and has a URI
                if (superClass.isResource() && superClass.asResource().getURI() != null) {
                    String superClassURI = superClass.asResource().getURI();
                    if (superClassURI == null) {
                        System.err.println("Superclass URI is null. Cannot find siblings.");
                        return siblingTriples; // Exit early if superclass is not found
                    }
                }
                
            }
            List<String> siblingList = new ArrayList<>();
            // Find sibling classes
            JsonArray siblingClasses = statistics.getAsJsonArray("SiblingClasses"); // Change to actual array name
            if (siblingClasses == null) {
                System.err.println("Sibling classes array is null. Check JSON structure.");
                return siblingTriples; // Exit early if the array is null
            }
            // Step 3: Create TriplePaths for each sibling class and add to the result set
            for (JsonElement entry : siblingClasses) {
                JsonObject entryObject = entry.getAsJsonObject();
                String classURI = entryObject.get("class").getAsString();
                // Check if this is the target class
                if (classURI.equals(originalClassURI)) {
                    // Get the siblings array
                    JsonArray siblingsArray = entryObject.getAsJsonArray("siblings");
                    System.out.println("the siblings array " + siblingsArray);
                    for (JsonElement sibling : siblingsArray) {
                        siblingList.add(sibling.getAsString());
                    }
                    break; // Exit loop after finding the target class
                }
            }          
            for (String sibling : siblingList) {
                // Create TriplePath for the sibling class
                TriplePath siblingTriple = new TriplePath(
                    originalTriple.getSubject(),
                    originalTriple.getPath(),
                    NodeFactory.createURI(sibling)
                );
                siblingTriples.add(siblingTriple);
            }
            System.out.println("the siblings: " + siblingList);
            return siblingTriples;
        }
/*       

        // Sibling relaxation method
        private static Set<TriplePath> siblingRelaxation(TriplePath originalTriple, Model ontology) {
            String jsonFilePath = "data/lubm_statistics-2.json";
            Set<TriplePath> siblingTriples = new HashSet<>();
            String classURI = originalTriple.getObject().getURI();
            // Step 1: Find the superclass of the original class
            NodeIterator superClassIter = ontology.listObjectsOfProperty(ResourceFactory.createResource(classURI), subClassOf);

            while (superClassIter.hasNext()) {
                RDFNode superClass = superClassIter.next();

                // Check if superClass is a resource and has a URI
                if (superClass.isResource() && superClass.asResource().getURI() != null) {
                    String superClassURI = superClass.asResource().getURI();
                }
            }
            // Step 2: Read the JSON statistics file
            try {
                JsonObject jsonObject = loadStatisticsFromJson(jsonFilePath);
                //JSONObject jsonObject = new JSONObject(content);
                
                // Step 3: Retrieve sibling classes
                JsonArray siblingsArray = jsonObject.getAsJsonArray("siblings");
                
                // Step 4: Iterate through the JSON array to find siblings
                for (JsonElement entry : siblingsArray) {
                    JsonObject entryObject = entry.getAsJsonObject();
                    if (entryObject.has("count") && entryObject.has("class")) {
                        String entryClass = entryObject.getAsJsonPrimitive("class").getAsString();
                        // Check if this entryClass is a sibling of the original class
                        if (!entryClass.equals(classURI)) {  // Exclude the original class
                            // Create TriplePath for the sibling class
                            TriplePath siblingTriple = new TriplePath(
                                originalTriple.getSubject(),
                                originalTriple.getPath(),
                                NodeFactory.createURI(entryClass)
                            );
                            siblingTriples.add(siblingTriple);
                        }
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();  // Handle any exceptions, such as file not found or JSON parsing errors
            }
            return siblingTriples;
        }
*/
    // This method will get the top 10 most similar instances
    public static List<Map.Entry<String, Double>> getTopNSimilar(String targetInstance, Map<String, Map<String, Double>> similarityMap, int topN) {
        if (!similarityMap.containsKey(targetInstance)) {
            return Collections.emptyList();
        }

        Map<String, Double> similarityScores = similarityMap.get(targetInstance);

        // Sorting the instances based on similarity score in descending order
        List<Map.Entry<String, Double>> sortedSimilarities = new ArrayList<>(similarityScores.entrySet());
        sortedSimilarities.sort((e1, e2) -> Double.compare(e2.getValue(), e1.getValue()));

        // Return the top N similar instances
        return sortedSimilarities.subList(0, Math.min(topN, sortedSimilarities.size()));
    }


    private static Set<TriplePath> instanceRelaxation(TriplePath originalTriple, String term, int k) throws IOException {
        Set<TriplePath> relaxedTriples = new HashSet<>();
        loadSimilarityMatrix2(csvFilePath);

        String originalNode;
        if ("subject".equalsIgnoreCase(term)) {
            originalNode = originalTriple.getSubject().toString();
        } else if ("object".equalsIgnoreCase(term)) {
            originalNode = originalTriple.getObject().toString();
        } else {
            // Unsupported term
            return relaxedTriples;
        }

        // Remove angle brackets for comparison
        originalNode = originalNode.replaceAll("[<>]", "");

        // Check if the original node exists in the CSV map
        if (!similarityInstanceMap.containsKey(originalNode)) {
            System.out.println("Node is " + originalNode);
            System.out.println("Node not found in the similarity map");
            return relaxedTriples;
        }

        // Get the top k similar instances
        List<Map.Entry<String, Double>> topKSimilarInstances = getTopNSimilar(originalNode, similarityInstanceMap, k);

        for (Map.Entry<String, Double> entry : topKSimilarInstances) {
            String similarNode = entry.getKey().replaceAll("[<>]", "");

            // Skip the original node
            if (similarNode.equals(originalNode)) {
                continue;
            }

            // Create relaxed triples
            TriplePath relaxedTriple = null;
            if ("subject".equalsIgnoreCase(term)) {
                relaxedTriple = new TriplePath(NodeFactory.createURI(similarNode), originalTriple.getPath(), originalTriple.getObject());
            } else { // "object"
                relaxedTriple = new TriplePath(originalTriple.getSubject(), originalTriple.getPath(), NodeFactory.createURI(similarNode));
            }

            relaxedTriples.add(relaxedTriple);
        }

        return relaxedTriples;
    }

    // Method to find the most similar instance excluding the original instance
    private static TriplePath instanceRelaxation(TriplePath originalTriple, String term) throws IOException {
        loadSimilarityMatrix2(csvFilePath);
        String originalNode;
        if ("subject".equalsIgnoreCase(term)) {
            originalNode = originalTriple.getSubject().toString();
        } else if ("object".equalsIgnoreCase(term)) {
            originalNode = originalTriple.getObject().toString();
        } else {
            // Unsupported term
            return null;
        }

        // Remove the angle brackets for comparison
        originalNode = originalNode.replaceAll("[<>]", "");

        // Check if the original node exists in the similarity map
        if (!similarityInstanceMap.containsKey(originalNode)) {
            System.out.println("Not found in similarity mapping");
            return null; // Node not found, return null
        }

        Map<String, Double> similarityScores = similarityInstanceMap.get(originalNode);

        // Find the most similar instance excluding the original instance
        String mostSimilarNode = null;
        double highestSimilarity = -1.0;

        for (Map.Entry<String, Double> entry : similarityScores.entrySet()) {
            if (!entry.getKey().equals(originalNode) && entry.getValue() > highestSimilarity) {
                mostSimilarNode = entry.getKey();
                highestSimilarity = entry.getValue();
            }
        }

        if (mostSimilarNode == null) {
            System.out.println("No similar node found");
            return null; // No similar node found
        }

        // Replace the original node with the most similar node
        TriplePath relaxedTriple;
        if ("subject".equalsIgnoreCase(term)) {
            relaxedTriple = new TriplePath(NodeFactory.createURI(mostSimilarNode), originalTriple.getPath(), originalTriple.getObject());
        } else { // "object"
            relaxedTriple = new TriplePath(originalTriple.getSubject(), originalTriple.getPath(), NodeFactory.createURI(mostSimilarNode));
        }

        return relaxedTriple;
    }

    



        public static TriplePath propertypathRelaxation (TriplePath originalTriple) {
            String propertyURI = originalTriple.getPredicate().getURI();
            // Get the original path
            Path originalPath = originalTriple.getPath();

            // Create a new path by appending the suffix
            Path newPath = PathFactory.pathOneOrMore1(originalPath);
            //Path newPath = PathFactory.pathAlt(originalPath, PathFactory.pathOneOrMore1((Path) type.asNode()));

            return new TriplePath(originalTriple.getSubject(), newPath, originalTriple.getObject());
        }

        private static Node generateUniqueVariable() {
            String varName = UUID.randomUUID().toString().replace("-", "");
            return NodeFactory.createVariable(varName);
        }
/*
        public static boolean isObjectInstance(Node object) {
            if(object.isURI()) {
                System.out.println(object + " is an instance");
            }
            return object.isURI(); // Assuming instances are represented as URIs
        }

 */

public static Set<TriplePath> literalRelaxationInteger(TriplePath originalTriple, String term, Map<Integer, Map<Integer, Double>> similarityLiteralMap, int k) {
    Set<TriplePath> relaxedTriples = new HashSet<>();

    if (!"object".equalsIgnoreCase(term)) {
        return relaxedTriples; // Unsupported term
    }

    if (!originalTriple.getObject().isLiteral() || !originalTriple.getObject().getLiteralDatatype().equals(XSDDatatype.XSDinteger)) {
        return relaxedTriples; // Not a valid integer literal
    }

    int originalLiteral = originalTriple.getObject().getLiteralValue().hashCode();
    RDFDatatype originalDatatype = originalTriple.getObject().getLiteralDatatype();
    //System.out.println("The integer literal to be relaxed is: " + originalLiteral);

    // Check if the literal exists in the similarity map
    Map<Integer, Double> similarLiterals = similarityLiteralMap.get(originalLiteral);
    if (similarLiterals == null) {
        return relaxedTriples; // No similar literals found
    }

    // Sort literals by similarity
    List<Map.Entry<Integer, Double>> sortedSimilarLiterals = new ArrayList<>(similarLiterals.entrySet());
    sortedSimilarLiterals.sort((a, b) -> Double.compare(b.getValue(), a.getValue())); // Sort by similarity in descending order

    int count = 0;
    for (Map.Entry<Integer, Double> entry : sortedSimilarLiterals) {
        if (count >= k) break; // Only take the top k similar literals

        Integer similarLiteralValue = entry.getKey();

        // Skip the original literal
        if (similarLiteralValue.equals(originalLiteral)) {
            continue;
        }

        // Create a new node for the relaxed literal
        Node relaxedLiteral = NodeFactory.createLiteralByValue(similarLiteralValue, XSDDatatype.XSDinteger);
        //System.out.println("Adding relaxed integer literal: " + relaxedLiteral);

        // Create a new triple with the relaxed literal
        TriplePath relaxedTriple = new TriplePath(
                originalTriple.getSubject(),
                originalTriple.getPath(),
                relaxedLiteral
        );

        relaxedTriples.add(relaxedTriple);
        count++;
    }

    return relaxedTriples;
}


public static TriplePath literalRelaxationInteger(TriplePath originalTriple, String term, Map<Integer, Map<Integer, Double>> similarityLiteralMap) {
    // Ensure the term is "object" for now, as only object literals are supported
    if (!"object".equalsIgnoreCase(term)) {
        return null; // Unsupported term, return null
    }


    // Get the original literal's lexical form and check if it is an integer
    if (!originalTriple.getObject().isLiteral() || !originalTriple.getObject().getLiteralDatatype().equals(XSDDatatype.XSDinteger)) {
        return null; // Not an integer literal, return null
    }


    int originalLiteral = originalTriple.getObject().getLiteralValue().hashCode();
    RDFDatatype originalDatatype = originalTriple.getObject().getLiteralDatatype();
    System.out.println("The integer literal to be relaxed is: " + originalLiteral);


    // Check if the original literal exists in the similarity map
    Map<Integer, Double> similarLiterals = similarityLiteralMap.get(originalLiteral);
    if (similarLiterals == null) {
        return null; // Literal not found, return null
    }


    // Find the most similar integer literal based on the highest similarity score
    Integer mostSimilarLiteral = null;
    double maxSimilarity = Double.NEGATIVE_INFINITY;
    for (Map.Entry<Integer, Double> entry : similarLiterals.entrySet()) {
        if (!entry.getKey().equals(originalLiteral) && entry.getValue() > maxSimilarity) {
            maxSimilarity = entry.getValue();
            mostSimilarLiteral = entry.getKey();
        }
    }


    if (mostSimilarLiteral == null) {
        return null; // No suitable replacement found
    }


    // Create a new integer literal node
    Node relaxedLiteral = NodeFactory.createLiteralByValue(mostSimilarLiteral, XSDDatatype.XSDinteger);
    System.out.println("Most relaxed integer literal: " + relaxedLiteral);


    // Replace the original integer literal with the most similar one
    return new TriplePath(
            originalTriple.getSubject(),
            originalTriple.getPath(),
            relaxedLiteral
    );
}


private static List<TriplePath> literalRelaxationString(
    TriplePath originalTriple, 
    String term, 
    Map<String, Map<String, Double>> similarityLiteralMap) {

    List<TriplePath> relaxedTriples = new ArrayList<>();

    if (!"object".equalsIgnoreCase(term)) {
        return relaxedTriples;
    }

    String originalLiteral = originalTriple.getObject().getLiteralLexicalForm();
    RDFDatatype originalDatatype = originalTriple.getObject().getLiteralDatatype();
    System.out.println("The literal to be relaxed is: " + originalLiteral + " (Type: " + originalDatatype + ")");

    Map<String, Double> similarLiterals = similarityLiteralMap.get(originalLiteral);
    if (similarLiterals == null) {
        return relaxedTriples;
    }

    for (Map.Entry<String, Double> entry : similarLiterals.entrySet()) {
        String similarLiteral = entry.getKey();

        if (similarLiteral.equalsIgnoreCase(originalLiteral)) continue;

        Node relaxedLiteral;
        try {
            if (originalDatatype != null) {
                if (originalDatatype.equals(XSDDatatype.XSDinteger)) {
                    relaxedLiteral = NodeFactory.createLiteralByValue(Integer.parseInt(similarLiteral), XSDDatatype.XSDinteger);
                } else if (originalDatatype.equals(XSDDatatype.XSDdecimal)) {
                    relaxedLiteral = NodeFactory.createLiteralByValue(Double.parseDouble(similarLiteral), XSDDatatype.XSDdecimal);
                } else {
                    relaxedLiteral = NodeFactory.createLiteral(similarLiteral, originalDatatype);
                }
            } else {
                relaxedLiteral = NodeFactory.createLiteral(similarLiteral);
            }

            TriplePath relaxedTriple = new TriplePath(
                originalTriple.getSubject(),
                originalTriple.getPath(),
                relaxedLiteral
            );
            relaxedTriples.add(relaxedTriple);
        } catch (NumberFormatException e) {
            System.err.println("Skipping literal due to number format error: " + similarLiteral);
        }
    }

    return relaxedTriples;
}


            public static JsonObject loadStatisticsFromJson(String jsonFilePath){
                try (FileReader reader = new FileReader(jsonFilePath)) {
                    return JsonParser.parseReader(reader).getAsJsonObject();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                return null;
            }


    public static boolean isObjectInstance(Node object) {
        String endpoint ="http://localhost:3030/Benchdataset/query";
        if (object.isURI()) {
            String queryStr = "PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#> " +
                    "PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#> " +
                    "PREFIX owl: <http://www.w3.org/2002/07/owl#> " +
                    "ASK WHERE { <" + object.getURI() + "> a owl:Class. " +
                    "FILTER NOT EXISTS { <" + object.getURI() + "> a ?type . " +
                    "?type a owl:Class. } }";

            Query query = QueryFactory.create(queryStr);
            try (QueryExecution qexec = QueryExecutionFactory.sparqlService(endpoint, query)) {
                boolean isClass = qexec.execAsk();
                if (isClass) {
                    return false;
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            //System.out.println(object + " is an instance");
            return true;
        }
        return false;
    }
        public static boolean isObjectLiteral(Node object) {
            // Implement logic to check if object is a literal
            System.out.println("is literal " + object + " " + object.isLiteral());
            return object.isLiteral();
        }

    }

