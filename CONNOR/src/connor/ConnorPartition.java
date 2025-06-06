package connor;

import org.apache.jena.sparql.expr.E_Equals;
import com.hp.hpl.jena.sparql.expr.Expr;
import connor.matchTree.LazyJoinResults;
import connor.matchTree.MatchTree;
import connor.utils.*;
import org.apache.jena.graph.Node;
import org.apache.jena.query.Query;
import org.apache.jena.query.QueryFactory;
import org.apache.jena.rdf.model.ResourceFactory;
import org.apache.jena.sparql.core.TriplePath;
import org.apache.jena.sparql.core.Var;
import org.apache.jena.sparql.syntax.*;

import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Class used for the representation and computation of concepts of neighbours for a given graph and a given target
 *
 * @author hayats
 * @author nk-fouque
 */
public class ConnorPartition implements Jsonable {
	private final ConnorModel model;
	private final List<String> target;
	private final PriorityQueue<ConceptOfNeighbours> preConcepts;
	private final List<ConceptOfNeighbours> concepts;
	

	private int nextClusterId;

	private final int maxDescriptionDepth;

	private final Map<Node, Var> varsByUri = new HashMap<>(); // map uris and literals to vars
	private final Map<Node, Integer> uriDepth = new HashMap<>();
	private int nextKey = 0; // for var numbering
	private int partitioningSteps = 0;

	/**
	 * Standard constructor to create a partition before computation of the concepts of neighbors
	 *
	 * @param model      the model connected to the partition
	 * @param targetUris the uris of the target of the concepts of neighbours
	 * @param initTable  the set of examples to partition
	 */
	public ConnorPartition(ConnorModel model, List<String> targetUris, Table initTable, int maxDepth) {
		this.target = targetUris;
		this.nextClusterId = 0;
		this.model = model;

		this.maxDescriptionDepth = maxDepth;

		int i = 0;
		List<Var> projectionVars = new ArrayList<>();
		for(String uri : this.target) {
			Var var = Var.alloc("Neighbor_" + i);
			projectionVars.add(var);
			this.addVar(ResourceFactory.createResource(uri).asNode(), var);
			i++;
		}

		this.target.forEach(uri -> this.setDepth(ResourceFactory.createResource(uri).asNode(), 0));

		Set<Element> initialDescription = ElementUtils.getInitialDescription(this.target, this);

		String Q1 = "PREFIX ub: <http://swat.cse.lehigh.edu/onto/univ-bench.owl#>" +
				"PREFIX xsd: <http://www.w3.org/2001/XMLSchema#>" +
				"SELECT DISTINCT ?Neighbor_0 WHERE {\n" +
				"    ?Neighbor_0 a ub:Professor;\n" +
				"      ub:memberOf ?dept.\n" +
				"    FILTER (?dept = <http://www.department403.university4.edu>) .\n" +
				"}";

		String Q3 = "PREFIX ub: <http://swat.cse.lehigh.edu/onto/univ-bench.owl#>" +
				"PREFIX xsd: <http://www.w3.org/2001/XMLSchema#>" +
				"SELECT DISTINCT ?Neighbor_0 WHERE {\n" +
				"    ?Neighbor_0 a ub:Publication ;\n" +
				"       ub:publicationAuthor ?z .\n" +
				"    ?z ub:researchInterest ?researchInterest.\n"  +
				"    FILTER (?researchInterest = \"Gene Expression Analysis\"^^xsd:string) .\n" +
				"}";

		String Q4 = "PREFIX ub: <http://swat.cse.lehigh.edu/onto/univ-bench.owl#>" +
						"PREFIX xsd: <http://www.w3.org/2001/XMLSchema#>" +
						"SELECT ?Neighbor_0 WHERE {\n" +
						"    ?Neighbor_0 ub:advisor ?Y .\n" +
						"    ?Y ub:headOf ?headOfDepartment .\n" +
						"    FILTER (?headOfDepartment = <http://www.department3.0.university0.edu>)\n" +
						"}";




		String sparqlQueryStringFilter =
				"PREFIX ub: <http://swat.cse.lehigh.edu/onto/univ-bench.owl#>" +
						"PREFIX xsd: <http://www.w3.org/2001/XMLSchema#>" +
						"SELECT ?Neighbor_0 WHERE {\n" +
						"    ?Neighbor_0 a ub:GraduateStudent ;\n" +
						"        ub:takesCourse ?course .\n" +
						"    ?degree ub:undergraduateDegreeFrom ?x ;\n" +
						"            ub:yearOfAward ?year .\n" +
						"    FILTER (?course = <http://www.department0.university0.edu/graduateCourse0>)\n" +
						"    FILTER (?year = \"2010\"^^xsd:integer)\n" +
						"}";

		String Q5 =
				"PREFIX ub: <http://swat.cse.lehigh.edu/onto/univ-bench.owl#> " +
						"PREFIX xsd: <http://www.w3.org/2001/XMLSchema#> " +
						"SELECT DISTINCT ?student_name ?advisor WHERE { " +
						"    ?student ub:name ?student_name . " +
						"    ?student ub:advisor ?advisor . " +
						"    ?student ub:takesCourse ?course . " +
						"    <<?student ub:takesCourse ?course>> ub:semester ?semester ; " +
						"                                         ub:courseYear ?courseYear . " +
						"    FILTER (?course = <http://www.department0.university0.edu/undergraduateCourse1>) " +
						"    FILTER (?semester = \"Autumn\") " +
						"    FILTER (?courseYear = \"2022\"^^xsd:integer) " +
						"} ";



/*
		// Parse the query using Apache Jena
		Query query = QueryFactory.create(Q1);

		// Extract the WHERE clause (the body of the query)
		ElementGroup queryPattern = (ElementGroup) query.getQueryPattern();
		// Initialize the set of elements from the body of the query
		Set<Element> initialDescription = new HashSet<>();

		// Recursively extract elements from the query pattern
		for (Element element : queryPattern.getElements()) {
			extractElements(element, initialDescription);
		}

		System.out.println("initial Description: " + initialDescription);

 */

		// Initialize the set of elements from the body of the query
		//Set<Element> initialDescription = new HashSet<>(queryPattern.getElements());
		//Set<Element> initialDescription = ElementUtils.getInitialDescription(this.target, this);


		this.preConcepts = new PriorityQueue<>();
		this.preConcepts.add(new ConceptOfNeighbours(initialDescription, projectionVars, this.getNextClusterId(), initTable));

		this.concepts = new ArrayList<>();

	}

	/**
	 * Used to set the depth of a node, i.e the distance from the target node to this node. Shouldn't be used by the user.
	 */
	public void setDepth(Node node, int depth) {
		if(!this.uriDepth.containsKey(node) || this.uriDepth.get(node) > depth) {
			this.uriDepth.put(node, depth);
		}
	}

	/**
	 * Used to get the depth of a node, i.e the distance from the target nodes to this node.
	 */
	public int getDepth(Node node) {
		if (uriDepth.get(node) != null) {
			return this.uriDepth.get(node);
		}
		else if (uriDepth.get(node) == null) {
			System.out.println(" the node depth is null of:  " + node);
		}
		// To think about it
		return 0;
	}

	public ConnorModel getModel() {
		return this.model;
	}

	private int getNextClusterId() {
		this.nextClusterId++;
		return nextClusterId;
	}

	/**
	 * Add an entry to the precomputed answers. Should not be used by the user.
	 *
	 * @param element The element for which the answer has been computed
	 * @param res     The computed answer
	 */
	public void addAns(Element element, Table res) {
		this.model.addAns(element, res);
	}

	private void addVar(Node node, Var var) {
		this.varsByUri.put(node, var);
	}

	/**
	 * Return a var for a given uri, either a new one or a pre-existing one
	 *
	 * @param node the uri corresponding to the var to create
	 * @return The var corresponding to the uri
	 */
	public Var getOrCreateVar(Node node) {
		if(this.varsByUri.containsKey(node)) {
			return this.varsByUri.get(node);
		} else {
			Var var = Var.alloc("x" + this.nextKey);
			this.nextKey++;
			this.varsByUri.put(node, var);
			return var;
		}
	}

	/**
	 * Return the set of possible answers to the query composed of only one element, if it has already been computed
	 *
	 * @param elt The element of the query
	 * @return The table representing the answers
	 */
	public Table getAnswer(Element elt) throws TableException {
		return this.model.getAnswer(elt);
	}

	/**
	 * Execute one step of the partitioning algorithm.
	 *
	 * @return true if the partitioning is over, false otherwise
	 * @throws PartitionException if we try to resume a finished partitioning
	 * @throws TableException     if there are errors during table operations
	 */
	private boolean oneStepPartitioning() throws PartitionException, TableException {
		if(this.preConcepts.isEmpty()) {
			throw new PartitionException("Tried to resume finished partition");
		}

		this.partitioningSteps++;
		// Track if it's the first iteration
		boolean isFirstIteration = true;
		// Selection of the pre-concept to refine
		/*
		if(isFirstIteration) {
			ConceptOfNeighbours preConcept = this.preConcepts.poll();
			Iterator<Element> iter = preConcept.getAvailableElements().iterator();
		}

		 */
		ConceptOfNeighbours preConcept = this.preConcepts.poll(); // The concept to refine
		System.out.println("concepts to refine are " + preConcept.getAvailableElements());

		if(preConcept.getAvailableElements().isEmpty()) { // if the concept is already totally refined
			System.out.println("Cannot refine this concept: " + preConcept);
			this.preConcepts.remove(preConcept);
			this.concepts.add(preConcept);
			System.out.println("the preconcept: " + preConcept);
			return this.preConcepts.isEmpty();
		}
			// Selection of the element used for the refinement
			Iterator<Element> iter = preConcept.getAvailableElements().iterator();
			Set<Var> mentionedVars;
			Element elt;
			do {
				elt = iter.next();
				mentionedVars = ElementUtils.mentioned(elt);
			} while (iter.hasNext() && !preConcept.connected(mentionedVars));
			if (!preConcept.connected(mentionedVars)) {
				this.preConcepts.remove(preConcept);
				this.concepts.add(preConcept);
				return this.preConcepts.isEmpty();
			}
			System.out.println("Processing single element: " + elt);

		// Intensions of the new (pre-)concepts
		Set<Element> intensionWithoutElt = preConcept.getIntensionBody();
		//Set<Element> intensionWithoutElt = preConcept.getIntensionBody();

		//System.out.println("the intension body of the preconcept: " + intensionWithoutElt);
		Set<Element> intensionWithElt = new HashSet<>(intensionWithoutElt);
		intensionWithElt.add(elt);

		// Match-trees of the new (pre-)concepts
		MatchTree matchTreeWithoutElt = preConcept.getMatchTree();
		//System.out.println("the preconcept match tree variables: " + preConcept.getMatchTree().getMatchSet().getSchema());
		//System.out.println("the preconcept variables: " + preConcept.getMatchSet().getSchema());
		//System.out.println("the match tree without element: " + matchTreeWithoutElt.getMatchSet().getSchema());
		MatchTree matchTreeWithElt = new MatchTree(matchTreeWithoutElt);
		//System.out.println("match tree with element: " + matchTreeWithElt.getMatchSet().getSchema());
		MatchTree newMatchTree = new MatchTree(elt, this, preConcept.getConnectedVars());
		//System.out.println("new match tree: " + newMatchTree.getMatchSet().getSchema());
		LazyJoinResults result = matchTreeWithElt.lazyJoin(newMatchTree);
		matchTreeWithElt = result.getMatchTree();

		// Proper extensions of the new (pre-)concepts
		Table properExtWithElt;
		Table properExtWithoutElt;
		//System.out.println("the match tree with element: " + matchTreeWithElt.getMatchSet().getSchema());
		//System.out.println("the preconcept variables: " + preConcept.getMatchTree().getMatchSet().getSchema());
		properExtWithElt = preConcept.getProperExtension().intersect(matchTreeWithElt.getMatchSet().projection(preConcept.getMatchTree().getMatchSet().getSchema()));
		//properExtWithElt = preConcept.getProperExtension().intersect(matchTreeWithElt.getMatchSet().projection(preConcept.getProjectionVars()));
		properExtWithoutElt = preConcept.getProperExtension().difference(properExtWithElt);

		this.preConcepts.remove(preConcept);

		if(!properExtWithElt.isEmpty()) {
			// Creation of the concept including the element
			Set<Element> availableEltsWithElt = new HashSet<>(preConcept.getAvailableElements());
			availableEltsWithElt.remove(elt);

			Set<Element> removedEltsWithElt = new HashSet<>(preConcept.getRemovedElements());
			removedEltsWithElt.add(elt);

			Set<Var> connectedVarsWithElt = new HashSet<>(preConcept.getConnectedVars());
			connectedVarsWithElt.addAll(ElementUtils.mentioned(elt));

			int extDistWithElt = matchTreeWithElt.getMatchSet().projection(preConcept.getMatchTree().getMatchSet().getSchema()).size();

			//int extDistWithElt = matchTreeWithElt.getMatchSet().projection(preConcept.getProjectionVars()).size();

			ConceptOfNeighbours preConceptWithElt = new ConceptOfNeighbours(
					this.getNextClusterId(),
					preConcept.getMatchTree().getMatchSet().getSchema(),
					//preConcept.getProjectionVars(),
					intensionWithElt,
					properExtWithElt,
					availableEltsWithElt,
					removedEltsWithElt,
					connectedVarsWithElt,
					matchTreeWithElt,
					extDistWithElt,
					preConcept.getRelaxationDistance()
			);

			if(availableEltsWithElt.isEmpty()) {
				this.concepts.add(preConceptWithElt);
			} else {
				this.preConcepts.add(preConceptWithElt);
			}
		}

		if(!properExtWithoutElt.isEmpty()) {
			// Creation of the concept excluding the element
			Set<Element> removedEltsWithoutElt = preConcept.getRemovedElements();
			removedEltsWithoutElt.add(elt);

			Set<Element> availableEltsWithoutElt = preConcept.getAvailableElements();
			availableEltsWithoutElt.remove(elt);
			Set<Element> relaxation = ElementUtils.relax(elt, this);
			relaxation.removeAll(removedEltsWithoutElt);
			availableEltsWithoutElt.addAll(relaxation);

			Set<Var> connectedVarsWithoutElt = preConcept.getConnectedVars();

			//int extDistWithElt = matchTreeWithElt.getMatchSet().projection(preConcept.getMatchTree().getMatchSet().getSchema()).size();
			//System.out.println("the preconcept variables for extDistWithElt: " + preConcept.getMatchTree().getMatchSet().getSchema());
			int extDistWithoutElt = matchTreeWithoutElt.getMatchSet().projection(preConcept.getMatchTree().getMatchSet().getSchema()).size();
			//int extDistWithoutElt = matchTreeWithoutElt.getMatchSet().projection(preConcept.getProjectionVars()).size();

			ConceptOfNeighbours preConceptWithElt = new ConceptOfNeighbours(
					this.getNextClusterId(),
					preConcept.getMatchTree().getMatchSet().getSchema(),
					//preConcept.getProjectionVars(),
					intensionWithoutElt,
					properExtWithoutElt,
					availableEltsWithoutElt,
					removedEltsWithoutElt,
					connectedVarsWithoutElt,
					matchTreeWithoutElt,
					extDistWithoutElt,
					preConcept.getRelaxationDistance() + 1
			);

			if(availableEltsWithoutElt.isEmpty()) {
				this.concepts.add(preConceptWithElt);
			} else {
				this.preConcepts.add(preConceptWithElt);
			}
		}
		System.out.println("the concepts to be refined is empty: " + this.preConcepts.isEmpty());
		return this.preConcepts.isEmpty();
	}

	/**
	 * Run the partitioning algorithm, interruptible thanks to an AtomicBoolean.
	 *
	 * @param cut The atomic boolean used to interrupt the partitioning. When switched to true, the algorithm will be interrupted after the current step.
	 */
	public void fullPartitioning(AtomicBoolean cut) throws PartitionException, TableException {
		boolean isOver = false;
		System.out.println("the cut: " + cut + " the isOver: " + isOver);
		while(!cut.get() && !isOver) {
			isOver = this.oneStepPartitioning();
		}
		System.out.println("the cut: " + cut + " the isOver: " + isOver);
	}

	/**
	 * Used to saturate the intensions of the concepts (in type only as for today)
	 */
	public void saturateIntensions() {
		for(ConceptOfNeighbours concept : this.concepts) {
			concept.saturateIntension(this.model);
		}

		for(ConceptOfNeighbours concept : this.preConcepts) {
			concept.saturateIntension(this.model);
		}
	}

	/**
	 * @return the value set as the max description depth, i.e the max depth of the intensions of the concepts of the partition.
	 */
	public int getMaxDescriptionDepth() {
		return maxDescriptionDepth;
	}

	/**
	 * @return Serialize the partition as a JSON object for further processing
	 */
	@Override
	public String toJson() {
		StringBuilder res = new StringBuilder();
		StringBuilder uris = new StringBuilder();
		for(String uri : this.target) {
			if(uris.toString().equals("")) {
				uris.append("[");
			} else {
				uris.append(",");
			}
			uris.append("\"").append(uri).append("\"");
		}
		uris.append("]");
		res.append("{\n\"target\":").append(uris).append(",\n");
		res.append("\"fully_processed\":").append(this.preConcepts.isEmpty()).append(",\n");
		res.append("\"conceptsOfNeighbours\":[\n\t");
		List<ConceptOfNeighbours> concepts = new ArrayList<>();
		concepts.addAll(this.preConcepts);
		concepts.addAll(this.concepts);
		concepts.sort(Comparator.comparingInt(ConceptOfNeighbours::getExtensionalDistance));
		StringBuilder conceptsString = new StringBuilder();

		concepts.forEach(c -> {
			if(!conceptsString.toString().equals("")) {
				conceptsString.append(",");
			}
			conceptsString.append("\t").append(c.toJson().replaceAll("\n", "\n\t").replaceAll("\"\",", ""));
		});
		res.append(conceptsString);
		res.append("]\n}");
		return res.toString();
	}

	/**
	 * @return the number of fully refined concepts
	 */
	public int getNbConcepts() {
		return this.concepts.size();
	}

	/**
	 * @return the number of partially refined concepts
	 */
	public int getNbPreConcepts() {
		return this.preConcepts.size();
	}

	/**
	 * @return the number of refining step made during the computation of concepts
	 */
	public int getPartitioningSteps() {
		return partitioningSteps;
	}

	/*
	// Function to extract elements, handling both ElementPathBlock and other types
	private void extractElements(Element element, Set<Element> elements, Set<TriplePath> triplePaths) {
		if (element instanceof ElementPathBlock) {
			ElementPathBlock pathBlock = (ElementPathBlock) element;
			pathBlock.patternElts().forEachRemaining(triplePath -> {
				triplePaths.add(triplePath);
			});
			System.out.println("the triple paths are: " + triplePaths);
		} else if (element instanceof ElementGroup) {
			ElementGroup group = (ElementGroup) element;
			for (Element subElement : group.getElements()) {
				extractElements(subElement, elements, triplePaths);
			}
		} else {
			// Add handling for other element types if necessary
			elements.add(element);
		}
	}

	 */

	private void extractElements(Element element, Set<Element> elements) {
		if (element instanceof ElementPathBlock) {
			ElementPathBlock pathBlock = (ElementPathBlock) element;
			pathBlock.patternElts().forEachRemaining(triplePath -> {
				ElementTriplesBlock tripleBlock = new ElementTriplesBlock();
				tripleBlock.addTriple(triplePath.asTriple());
				elements.add(tripleBlock);
			});
		} else if (element instanceof ElementGroup) {
			ElementGroup group = (ElementGroup) element;
			for (Element subElement : group.getElements()) {
				extractElements(subElement, elements);
			}
		}else if (element instanceof ElementFilter) {
			ElementFilter filter = (ElementFilter) element;
			E_Equals expr = (E_Equals) filter.getExpr();
			// Process the filter expression as needed
			// For now, we just add the filter element to the set
			elements.add(filter);
		} else {
			// Add handling for other element types if necessary
			elements.add(element);
		}
	}


}
