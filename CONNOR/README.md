# FORK FROM https://gitlab.inria.fr/hayats/CONNOR.git

# CONNOR: A Java library for the computation of Concepts of Neighbors on RDF graphs

This repository present CONNOR, an *Apache Jena*-based  library for the computation of n-ary Concepts of Neighbors on RDF Graphs.
The Concepts of Neighbors is a *Formal Concepts Analysis* (FCA) based graph mining method for exploring similarities.
For a given entity (or n-uples of entities) called the **target**, this method compute the similarity between this entity and the others, by regrouping them into **concepts**.
Each concept is defined by its **intension**, which is the graph pattern matched by the **target** and the element of the concept, and its **proper extension**, which are the elements specifically matched by this concept.
In addition, each concept has two numerical value, expressing a notion of distance of the elements of its proprer intension from the target: the **extensional distance** expresses the number of elements between the target and the elements of the proper extension, and the **relaxation distance** expresses the number of relaxations made to obtain the intension.

For more informations about Concepts of Neighbors, please refer to:
- ***Ferré, S., Huchard, M., Kaytoue, M., Kuznetsov, S.O., Napoli, A. (2020). Formal Concept Analysis: From Knowledge Discovery to Knowledge Processing. In: A Guided Tour of Artificial Intelligence Research. Springer, Cham.***

## Prerequisites and installation

This library is compatible with Java 11 or higher.

This library depends on [apache-jena-libs](https://mvnrepository.com/artifact/org.apache.jena/apache-jena-libs) 4.x. Please import it in your project if you want to use CONNOR.

A **JAR package** of CONNOR can be found in the [latest release](https://gitlab.inria.fr/hayats/CONNOR/-/releases).

Alternatively you can build it from sources:

```bash
$ git clone https://gitlab.inria.fr/hayats/CONNOR.git
$ cd CONNOR
$ ./gradlew build
```

The result of this build can be found in `build/libs/CONNOR.jar`

## Important classes

In this section are presented the few classes useful to an end user.

### `connor.ConnorModel`

This class encapsulates a *Jena* `InfModel` object, representing a RDF graph with an inference mechanism.

There are two ways to create an instance of `ConnorModel`:
- Creating an empty model with `ConnorModel()` and populate it with `ConnorModel.add(Statement)` or `ConnorModel.add(List<Statement>)`.
- Create a model from an existing `InfModel`  with `ConnorModel(InfModel)`. In this case, the whole model is reprocessed statement by statement.

### `connor.ConceptsOfNeighbors`

As its name tells, this class represents a concept of neighbors, and hence a graph concept too. As expected, an object
of this class is characterized by its intension (decomposed into two attributes : the list of the projection variables
and the graph pattern), its extension and its proper extension. Those elements can be accessed through the methods
`getProjectionVars()`, `getIntensionBody()`, `getExtension()` and `getProperExtension()`. In addition, the extensional 
and relaxation distances are accessible through the methods `getExtensionalDistance()` and `getRelaxationDistance()`. 


The creation of objects of this class is handled by the ConnorPartition class, and  should not be done by library users.

### `connor.ConnorPartition`

This class is central to the computation of concepts of neighbors. It takes its name from the fact that, as presented 
above, the proper extensions of the concepts of neighbors form a partition of the set of objects. This class contains
all the information needed for the computations of concepts of neighbors, such as of course the RDF graph (represented 
by a `ConnorModel` object), the concepts of neighbors once the computation is done (represented by a collection of
`ConceptOfNeighbors` objects), but also the tuple of objects (called target) for which we want to compute the concepts of
neighbors. A `ConnorPartition` object can be translated into JSON for serialization and further processing through the 
method `toJson()`.

A key aspect of this class is that it implements an anytime algorithm for the computation of concepts of neighbors. This
algorithm start from a concept with an empty intension and a proper extension containing all the elements
and, by successively trying to add elements to the intension, refines it in more specific concepts that still include 
the target. This way, the algorithm can be interrupted at each refinement step. 

To use this class, the base constructor of this object takes as argument a `ConnorModel object`, the target (represented 
by a `List<String>` of URIs), the partition domain 
and a integer `maxDepth` parameter. The partitioning domain is called `initializationTable`, which is a `connor.utils.Table` object which 
represents a set of tuples of entities. The role of this argument is to stipulate which set of tuples should appear in the extensions of
the concepts. Such a table can be created with the method `Table.ofArity(int)`, the parameter being the arity of the concepts to compute,
and ca be filled thourgh the `addInitRow(List<Node> row)` method.

Concerning the `maxDepth` parameter, its role is to set, if desired, a limit to the depth of the intension from the 
elements of the target. If set to zero, no limit is applied. 

The class method to call in order to launch the computation of concepts of neighbors is called fullPartitioning(cut). 
Taking a mutable boolean `AtomicBoolean` named `cut` as a parameter, it refines the partition until convergence or until
`cut` is switched to true.

### `connor.utils.ConnorThread`

This class encapsulates the computation of concepts of neighbors using a `ConnorPartition` in a Java thread, so that the
main thread just needs to launch this thread and switch `cut` to true when desired. A thread of this class can be 
created using the `ConnorThread(ConnorPartition, AtomicBoolean)` constructor and can be launch through the `start()` method.

## Usage example

The [`UnaryExample`](src/connor/examples/UnaryExample.java) and [`BinaryExample`](src/connor/examples/BinaryExample.java)
main classes give usage examples of CONNOR on a brief [RDF graph](data/royal.ttl). Those examples can be easily run:

```bash
$ ./gradlew runUnaryExample
$ ./gradlew runBinaryExample
```

The results of those executions can then be found in `data/charlotte_example.json` and `data/charlotte_kate_example.json`.

The following code gives a short example of usage of CONNOR:

```java
public class Example {
	public static void main(String[] args) {

		// Loading the RDF graph from data/royal.ttl
		InfModel rdfModel = ModelFactory.createInfModel(
				ReasonerRegistry.getRDFSReasoner(),
				RDFDataMgr.loadModel("data/royal.ttl", Lang.TTL)
		);

		// Creating a ConnorModel from this graph
		ConnorModel model = new ConnorModel(rdfModel);

		// Creation of the target
		List<String> target = new ArrayList<>();
		target.add("http://example.org/royal/Charlotte");

		// Creation of the initialization table
		Table table = Table.ofArity(1);
		rdfModel.listSubjects().forEachRemaining(r1 -> {
					if(r1.getURI().contains("/royal/")) {
						List<Node> row = new ArrayList<>();
						row.add(r1.asNode());
						table.addInitRow(row);
					}
		});

		// Creation of the partition
		ConnorPartition partition = new ConnorPartition(model, target, table, 0);
		
		// Creation of the thread
		AtomicBoolean cut = new AtomicBoolean(false);
		ConnorThread thread = new ConnorThread(partition, cut);
		
		// Computation of the Concepts of Neighbors
		thread.start();
		thread.join(5 * 1000); // 5*1000ms = 5s
		cut.set(true);
		if(thread.isAlive()) {
			thread.join();
		}

		// Printing of the results
		System.out.println(partition.toJson());
	}
}
```