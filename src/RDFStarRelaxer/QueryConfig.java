package RDFStarRelaxer;

import org.apache.jena.query.*;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;

public class QueryConfig {
    // SPARQL endpoint URL
    //String endpointURL = "http://localhost:3000/closure/query";
    public static String RDF_PREFIX = "PREFIX rdf:  <http://www.w3.org/1999/02/22-rdf-syntax-ns#> "
            + "PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>";

    // List of super class
    public static String LIST_SUPER_CLASSES = RDF_PREFIX + "SELECT ?directsuperclasses ?classe "
            + "WHERE { ?classe rdfs:subClassOf ?directsuperclasses "
            + "FILTER(?directsuperclasses != ?classe && isIRI(?classe) && isIRI(?directsuperclasses)) " + "OPTIONAL { "
            + "?classe rdfs:subClassOf ?othersuperclasse ." + "?othersuperclasse rdfs:subClassOf ?directsuperclasses ."
            + "FILTER( ?othersuperclasse != ?classe && ?othersuperclasse != ?directsuperclasses )" + "} "
            + "FILTER (!bound(?othersuperclasse) ) " + "}";

    // List of super property
    public static String LIST_SUPER_PROPERTIES = RDF_PREFIX + "SELECT ?directsuperproperty ?property "
            + "WHERE { ?property rdfs:subPropertyOf ?directsuperproperty "
            + "FILTER(?directsuperproperty != ?property && isIRI(?property) && isIRI(?directsuperproperty)) "
            + "OPTIONAL { " + "?property rdfs:subPropertyOf ?othersuperproperty ."
            + "?othersuperproperty rdfs:subPropertyOf ?directsuperproperty ."
            + "FILTER( ?othersuperproperty != ?property && ?othersuperproperty != ?directsuperproperty )" + "} "
            + "FILTER (!bound(?othersuperproperty) ) " + "}";

    public static void execQuery (String endpointURL, String query_string, String outputPath) {
        Query query = QueryFactory.create(query_string);
        QueryExecution queryExecution = QueryExecutionFactory.sparqlService(endpointURL, query);
        try {
            // Execute the query and obtain the result set
            ResultSet resultSet = queryExecution.execSelect();
            try (OutputStream outputStream = new FileOutputStream(outputPath)) {
                ResultSetFormatter.outputAsJSON(outputStream, resultSet);
                System.out.println("Results saved to: " + outputPath);
            }
            catch (IOException e) {
                e.printStackTrace();
                 }
        }
        finally {
            // Close the query execution to release resources
            queryExecution.close();
        }
    }

}
