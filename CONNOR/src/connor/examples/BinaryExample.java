package connor.examples;

import connor.ConnorModel;
import connor.ConnorPartition;
import connor.utils.ConnorThread;
import connor.utils.Table;
import connor.utils.TableException;
import org.apache.jena.graph.Node;
import org.apache.jena.query.Dataset;
import org.apache.jena.rdf.model.InfModel;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.reasoner.ReasonerRegistry;
import org.apache.jena.riot.Lang;
import org.apache.jena.riot.RDFDataMgr;
import org.apache.jena.tdb.TDBFactory;

import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

public class BinaryExample {
    public static void main(String[] args) throws IOException, InterruptedException, TableException {
        String input = args[0];
        String output = args[1];
        String instance1 = args[2];
        String instance2 = args[2];
        int exec_time = 500;
        String filter = "http://www.department";
        boolean filterTable = true;

        // Create or connect to a TDB-backed dataset
        Dataset dataset = TDBFactory.createDataset(input);
        Model mdl = dataset.getDefaultModel();
        InfModel rdfModel = ModelFactory.createInfModel(ReasonerRegistry.getTransitiveReasoner(), mdl);
        //InfModel rdfModel = ModelFactory.createInfModel(ReasonerRegistry.getRDFSReasoner(), RDFDataMgr.loadModel(input, Lang.TTL));

        ConnorModel model = new ConnorModel(rdfModel);

        List<String> target = new ArrayList<>();
        target.add(instance1);
        target.add(instance2);

        Table table = Table.ofArity(2);
        rdfModel.listSubjects().forEachRemaining(r1 -> {
            if (r1.getURI() != null && (!filterTable || r1.getURI().contains(filter))) {
                rdfModel.listSubjects().forEachRemaining(r2 -> {
                    if (r2.getURI() != null && (!filterTable || r2.getURI().contains(filter))) {
                        List<Node> row = new ArrayList<>();
                        row.add(r1.asNode());
                        row.add(r2.asNode());
                        table.addInitRow(row);
                    }
                });
            }
        });

        rdfModel.listObjects().forEachRemaining(r1 -> {
            // Cast to Node first and check if it has a URI
            if (r1.isResource() && r1.asResource().getURI() != null && (!filterTable || r1.asResource().getURI().contains(filter))) {
                rdfModel.listObjects().forEachRemaining(r2 -> {
                    if (r2.isResource() && r2.asResource().getURI() != null && (!filterTable || r2.asResource().getURI().contains(filter))) {
                        List<Node> row = new ArrayList<>();
                        row.add(r1.asNode());
                        row.add(r2.asNode());
                        table.addInitRow(row);
                    }
                });
            }
        });
/*
        rdfModel.listSubjects().forEachRemaining(r1 -> {
            if(!filterTable || r1.getURI().contains(filter)) {
                rdfModel.listSubjects().forEachRemaining(r2 -> {
                    if(!filterTable || r2.getURI().contains(filter)) {
                        List<Node> row = new ArrayList<>();
                        row.add(r1.asNode());
                        row.add(r2.asNode());
                        table.addInitRow(row);
                    }
                });
            }
        });
        rdfModel.listObjects().forEachRemaining(r1 -> {
            if(!filterTable || r1.asNode().getURI().contains(filter)) {
                rdfModel.listObjects().forEachRemaining(r2 -> {
                    if(!filterTable || r2.asNode().getURI().contains(filter)) {
                        List<Node> row = new ArrayList<>();
                        row.add(r1.asNode());
                        row.add(r2.asNode());
                        table.addInitRow(row);
                    }
                });
            }
        });

 */
        System.out.printf("Initialization table size: %d\n", table.size());

        ConnorPartition partition = new ConnorPartition(model, target, table, 2);
        AtomicBoolean cut = new AtomicBoolean(false);
        ConnorThread thread = new ConnorThread(partition, cut);
        thread.start();
        thread.join(exec_time * 1000);
        cut.set(true);
        System.out.println("Waiting for the thread to finish");
        if(thread.isAlive()) {
            thread.join();
        }
        System.out.printf("Theoretical max exec time: %d seconds\n", exec_time);
        System.out.printf("Real exec time: %f seconds\n", (float) thread.getExecTime() / 1000000000);
        int nbConcepts = partition.getNbConcepts();
        int nbPreConcepts = partition.getNbPreConcepts();
        System.out.printf("Nb of concepts: %d + %d = %d\n", nbConcepts, nbPreConcepts, nbConcepts + nbPreConcepts);
        System.out.printf("Nb of partitioning steps: %d\n", partition.getPartitioningSteps());

        FileWriter writer = new FileWriter(output);
        writer.write(partition.toJson());
        writer.close();
    }
}

