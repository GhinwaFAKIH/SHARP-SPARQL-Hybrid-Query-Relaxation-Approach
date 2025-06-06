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
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

public class Example {

	public static void main(String[] args) throws IOException, InterruptedException, TableException {

		String tdbPath = args[0];
		String output = args[1];
		String instance1 = args[2];
		//String instance2 = args[3];
		//String instance3 = args[4];
		//String instance4 = args[5];

		final int exec_time = 70;
		//String filter = "clara.univ-nantes.fr/resource/";
		String filter = "http://www.";
		boolean filterTable = true;

		// Create or connect to a TDB-backed dataset
		Dataset dataset = TDBFactory.createDataset(tdbPath);
		Model mdl = dataset.getDefaultModel();
		InfModel rdfModel = ModelFactory.createInfModel(ReasonerRegistry.getTransitiveReasoner(), mdl);
		//InfModel rdfModel = ModelFactory.createInfModel(ReasonerRegistry.getRDFSReasoner(), mdl);

		double maxMemory = (double) Runtime.getRuntime().maxMemory() / 1048576;
		System.out.println("Maximum memory (MB) that the JVM can use for the program: " + maxMemory);

		// Creating a ConnorModel from this graph
		ConnorModel connorModel = new ConnorModel(rdfModel);

		// Creation of the target
		List<String> target = new ArrayList<>();
		target.add(instance1);
		//target.add(instance2);
		//target.add(instance3);
		//target.add(instance4);

		Table table = Table.ofArity(1);
		rdfModel.listSubjects().forEachRemaining(r1 -> {
				//System.out.println("the node is " + r1);
			if(!filterTable || r1.isResource() || r1.getURI().contains(filter)) {
				List<Node> row = new ArrayList<>();
				row.add(r1.asNode());
				table.addInitRow(row);
			}
		});

		rdfModel.listObjects().forEachRemaining(r1 -> {
			if(r1.asNode().isURI()) {
				if(!filterTable ||  r1.asNode().getURI().contains(filter)) {
					List<Node> row = new ArrayList<>();
					row.add(r1.asNode());
					table.addInitRow(row);
				}
			}
		});
		
		System.out.printf("Initialization table size: %d\n", table.size());

		// Creation of the partition
		ConnorPartition partition = new ConnorPartition(connorModel, target, table, 7);
		AtomicBoolean cut = new AtomicBoolean(false);
		ConnorThread thread = new ConnorThread(partition, cut);

		// Computation of the Concepts of Neighbors
		thread.start();
		thread.join(exec_time * 1000);
		cut.set(true);

		System.out.println("Waiting for the thread to finish");

		if(thread.isAlive()) {
			thread.join();
		}

		System.out.printf("Theoretical max exec time: %d seconds\n", exec_time);
		System.out.printf("Real exec time: %f seconds\n", (float) thread.getExecTime() / 1000000000);
		final int nbConcepts = partition.getNbConcepts();
		final int nbPreConcepts = partition.getNbPreConcepts();
		System.out.printf("Nb of concepts: %d + %d = %d\n", nbConcepts, nbPreConcepts, nbConcepts + nbPreConcepts);
		System.out.printf("Nb of partitioning steps: %d\n", partition.getPartitioningSteps());

		FileWriter writer = new FileWriter(output);
		writer.write(partition.toJson());
		writer.flush();
		writer.close();

		// Close the dataset after use dataset.close();
		dataset.close();
	}
}
