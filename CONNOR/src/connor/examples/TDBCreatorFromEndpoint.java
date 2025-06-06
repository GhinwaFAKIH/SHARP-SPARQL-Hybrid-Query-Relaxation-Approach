package connor.examples;

import org.apache.jena.query.Dataset;
import org.apache.jena.query.QueryExecution;
import org.apache.jena.query.QueryExecutionFactory;
import org.apache.jena.query.ResultSet;
import org.apache.jena.query.ResultSetFormatter;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.riot.RDFDataMgr;
import org.apache.jena.riot.RDFLanguages;
import org.apache.jena.tdb.TDBFactory;
import org.apache.jena.update.UpdateAction;
import org.apache.jena.update.UpdateFactory;
import org.apache.jena.update.UpdateRequest;

public class TDBCreatorFromEndpoint {

    public static void main(String[] args) {

        String sparqlEndpoint = args[0]; // SPARQL endpoint URL
        String tdbPath = args[1]; // Path to the TDB directory

        System.out.println("=================== CREATING TDB BACKED DATASET ===================");

        // Create or connect to a TDB-backed dataset
        Dataset dataset = TDBFactory.createDataset(tdbPath);

        // Create a model to hold the data
        Model model = ModelFactory.createDefaultModel();

        // Define the SPARQL CONSTRUCT query to get all triples
        String constructQuery = "CONSTRUCT { ?s ?p ?o } WHERE { ?s ?p ?o . FILTER(!isBlank(?s) && !isBlank(?o))}";

        try (QueryExecution qexec = QueryExecutionFactory.sparqlService(sparqlEndpoint, constructQuery)) {
            // Execute the query and load the results into the model
            qexec.execConstruct(model);
        } catch (Exception e) {
            System.out.println("Error executing SPARQL query: " + e.getMessage());
            return;
        }

        // Add the model to the TDB-backed dataset
        dataset.getDefaultModel().add(model);

        // Close the dataset after use
        dataset.close();

        System.out.println("Dataset located at: " + tdbPath);
        System.out.println("=================== TDB DATASET READY ===================");
    }
}
