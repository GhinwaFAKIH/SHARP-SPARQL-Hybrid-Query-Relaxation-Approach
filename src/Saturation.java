import org.apache.jena.query.Dataset;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Statement;
import org.apache.jena.rdf.model.StmtIterator;
import org.apache.jena.reasoner.Reasoner;
import org.apache.jena.reasoner.rulesys.GenericRuleReasoner;
import org.apache.jena.reasoner.rulesys.Rule;
import org.apache.jena.shared.PrefixMapping;
import org.apache.jena.tdb.TDBFactory;
import org.apache.jena.util.FileManager;
import org.apache.jena.util.PrintUtil;

import java.io.*;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class Saturation {



    public static void main(String[] args) {
        String TDB_PATH = "data/tdb";
        Dataset dataset = TDBFactory.createDataset(TDB_PATH);



        // Create an RDF model
        Model model = ModelFactory.createDefaultModel();




        // Load your RDF dataset
        //FileManager.get().readModel(model, "data/toyexample.ttl");
        String directoryPath = "/Users/e20d463s/Documents/CLARA/lubm_chunks";
        String ontology_path = "/Users/e20d463s/Documents/LUBM100/univ-bench.owl";



        // Get list of files in the directory
        File directory = new File(directoryPath);
        File[] files = directory.listFiles();

        if (files != null) {
            for (File file : files) {
                // Create an empty model
                if (file.getName().equals(".DS_Store")) {
                    continue;
                }
                System.out.println("the file path is " + file.getAbsolutePath());
                // Use StreamRDF to read the file incrementally
                //RDFDataMgr.parse(streamRDF, new FileInputStream(file), Lang.NTRIPLES);
                InputStream in = FileManager.get().open(String.valueOf(file));
                System.out.println("reading file  " + String.valueOf(file));
                // Use FileManager to read the OWL file
                FileManager.get().readModel(model, String.valueOf(file));
                System.out.println("the file is read " + String.valueOf(file));
                //loadRDFFile(dataset, String.valueOf(file));

            }
        } else {
            System.out.println("No files found in the directory.");
        }

        //FileManager.get().readModel(model, "/Users/e20d463s/Documents/CLARA/lubm_chunks/chunkaa.nt");

        // Load your ontology (in this example, we'll use a simple ontology)
        //FileManager.get().readModel(model, "data/toyexampleontology.ttl");
        FileManager.get().readModel(model, ontology_path);
        //loadOWLFile(dataset, ontology_path);

        //FileManager.get().readModel(model, "/Users/e20d463s/Documents/LUBM100/univ-bench.owl");



        // Create an OWL micro reasoner
        // Create an RDFS rule reasoner
        //Reasoner reasoner = ReasonerRegistry.getRDFSReasoner();
        // Create a TransitiveReasoner
        //Reasoner reasoner = new TransitiveReasoner();
        //Reasoner reasoner = ReasonerRegistry.getRDFSReasoner();


        // Create a set of custom rules for subclass and subproperty reasoning
        List<Rule> rules = new ArrayList<>();
        // Add rules for subclass inference
        rules.add(Rule.parseRule("[subClass1: (?a rdf:type ?b) (?b rdfs:subClassOf ?c) -> (?a rdf:type ?c)]"));
        rules.add(Rule.parseRule("[subClass2: (?c1 rdfs:subClassOf ?c2) (?c2 rdfs:subClassOf ?c3) -> (?c1 rdfs:subClassOf ?c3)]"));
        // Add rules for subproperty inference
        rules.add(Rule.parseRule("[subProperty1: (?a ?p ?b) (?p rdfs:subPropertyOf ?q) -> (?a ?q ?b)]"));
        rules.add(Rule.parseRule("[subProperty2: (?p1 rdfs:subPropertyOf ?p2) (?p2 rdfs:subPropertyOf ?p3) -> (?p1 rdfs:subPropertyOf ?p3)]"));
        // Add rules for transitivity
        rules.add(Rule.parseRule("[type: (?c1 rdf:type ?c2) (?c2 rdf:type ?c3) -> (?c1 rdf:type ?c3)]"));
        // Add rules for range and domain
        rules.add(Rule.parseRule("[rangedom1: (?a rdfs:domain ?c) (?s ?a ?o) -> (?s rdf:type ?c)]"));
        rules.add(Rule.parseRule("[rangedom2: (?a rdfs:range ?d) (?s ?a ?o) -> (?o rdf:type ?d)]"));
        // Create a GenericRuleReasoner with custom rules
        Reasoner customReasoner = new GenericRuleReasoner(rules);


            // Attach the reasoner to the model
        //Model inferredModel = ModelFactory.createInfModel(customReasoner, model);
        //System.out.println("the dataset " + dataset.isEmpty());
        Model inferredModel = ModelFactory.createInfModel(customReasoner, model);
        System.out.print("the inference model is created.");
        //System.out.print(inferredModel.listStatements());
            // Specify the output file path (replace "output.ttl" with your desired file name)
            //String outputFile2 = "data/inferred_triples.ttl";
            String outputFile2 = "data/lubm_std_saturated.nt";
        // Write the inferred model to a Turtle-formatted file
        /*
        try (FileOutputStream fos = new FileOutputStream(outputFile2)) {
            RDFDataMgr.write(fos, inferredModel, RDFFormat.NTRIPLES);
            System.out.println("Inferred model written to " + outputFile2);
        } catch (Exception e) {
            e.printStackTrace();
        }
        */

            //Write the inferred model to a ntriples-formatted file
            //RDFDataMgr.write(System.out, inferredModel, RDFFormat.NTRIPLES);
            //System.out.println("the inferred model is written to n-triples formatted file.");


            // Define the output file to save the inferred triples
            //String outputFile = "data/inferred_triples_with_prefixes.ttl";

        // Get the default model from the dataset
        //Model model = dataset.getDefaultModel();

        try (BufferedWriter writer = new BufferedWriter(new FileWriter(outputFile2))) {

                // Write prefix declarations to the output file
                PrefixMapping prefixMapping = model.getGraph().getPrefixMapping();
                System.out.println("the prefix mapping is " + prefixMapping);
                Iterator<String> prefixIter = prefixMapping.getNsPrefixMap().keySet().iterator();
                while (prefixIter.hasNext()) {
                    String prefix = prefixIter.next();
                    String namespace = prefixMapping.getNsPrefixURI(prefix);
                    System.out.println("the prefix is " + prefix + " and the namespace is " + namespace);
                    writer.write("@prefix " + prefix + ": <" + namespace + "> .");
                    writer.write("\n");
                }
                writer.write("\n");
                System.out.println("prefixes written");


                // Query the inferred model and write triples to the output file
                StmtIterator iter = inferredModel.listStatements();
                System.out.println("the statements are listed");

                while (iter.hasNext()) {
                    Statement stmt = iter.nextStatement();
                    System.out.println("stmt is " + stmt);
                    String tripleStr = PrintUtil.print(stmt);
                    System.out.println("the triple to be written is " + tripleStr);
                    writer.write(tripleStr);
                    writer.write("\n");
                }
                System.out.println("Inferred triples saved to " + outputFile2);
            } catch (IOException e) {
                e.printStackTrace();
            }



        }
    private static void loadRDFFile(Dataset dataset, String rdfFilePath) {
        InputStream in = FileManager.get().open(rdfFilePath);
        if (in == null) {
            throw new IllegalArgumentException("File: " + rdfFilePath + " not found");
        }
        Model model = dataset.getDefaultModel();
        System.out.println("reading file  " + rdfFilePath);
        FileManager.get().readModel(model, rdfFilePath);
        //model.read(in, null, "N-TRIPLES");
        System.out.println("the file is read " + rdfFilePath);
    }
    private static void loadOWLFile(Dataset dataset, String rdfFilePath) {
        InputStream in = FileManager.get().open(rdfFilePath);
        if (in == null) {
            throw new IllegalArgumentException("File: " + rdfFilePath + " not found");
        }
        Model model = dataset.getDefaultModel();
        System.out.println("reading file  " + rdfFilePath);
        // Use FileManager to read the OWL file
        FileManager.get().readModel(model, rdfFilePath);
        System.out.println("the file is read " + rdfFilePath);
        // Commit the changes to the dataset
        //dataset.commit();
    }
    }

