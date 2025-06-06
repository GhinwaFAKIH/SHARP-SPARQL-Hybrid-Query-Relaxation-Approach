package connor;

import connor.matchTree.MatchTree;
import connor.utils.ElementUtils;
import connor.utils.Jsonable;
import connor.utils.Table;
import connor.utils.TableException;
import org.apache.jena.graph.Node;
import org.apache.jena.graph.Triple;
import org.apache.jena.sparql.core.Var;
import org.apache.jena.sparql.syntax.Element;
import org.apache.jena.sparql.syntax.ElementTriplesBlock;
import org.apache.jena.vocabulary.RDF;
import org.apache.jena.vocabulary.RDFS;

import java.util.*;

/**
 * Class used for the representation of a concept of neighbours (or cluster)
 *
 * @author hayats
 * @author nk-fouque
 */
public class ConceptOfNeighbours implements Jsonable, Comparable<ConceptOfNeighbours> {
	private final int id;

	private final List<Var> projectionVars;
	private final Set<Element> intensionBody;

	private final MatchTree matchTree;
	private final Table properExtension;
	private final Table extension;

	private final Set<Element> availableElements;
	private final Set<Element> removedElements;

	private final Set<Var> connectedVars;

	private final int extensionalDistance;
	private final int relaxationDistance;

	/**
	 * Create the initial concept
	 *
	 * @param elements       the intension of the initial concept
	 * @param projectionVars the vars linked to the targeted uris
	 * @param id             the id  of the concept
	 * @param initTable      the extension of the concept
	 */
	protected ConceptOfNeighbours(Set<Element> elements, List<Var> projectionVars, int id, Table initTable) {
		this.id = id;
		this.projectionVars = projectionVars;
		this.intensionBody = new HashSet<>(); // Empty intension
		this.extensionalDistance = initTable.size();
		this.relaxationDistance = 0;

		this.properExtension = initTable;
		this.extension = initTable;
		this.availableElements = elements;
		this.removedElements = new HashSet<>();
		this.matchTree = new MatchTree(new ArrayList<>(projectionVars), initTable);
		this.connectedVars = new HashSet<>(projectionVars);
	}

	/**
	 * @return the elements excluded from the intension of this concept during the computation
	 */
	public Set<Element> getRemovedElements() {
		return removedElements;
	}

	/**
	 * Create a ConceptOfNeighbours form all its attributes.
	 *
	 * @param extDist
	 */
	protected ConceptOfNeighbours(int id, List<Var> projectionVars, Set<Element> intensionBody, Table properExtension, Set<Element> availableElements, Set<Element> removedElements, Set<Var> connectedVars, MatchTree matchTree, int extDist, int relaxDist) throws TableException {
		this.id = id;

		this.projectionVars = projectionVars;
		this.intensionBody = intensionBody;

		this.matchTree = matchTree;
		this.properExtension = properExtension;
		this.extension = matchTree.getMatchSet().projection(this.projectionVars);

		this.availableElements = availableElements;
		this.removedElements = removedElements;
		this.connectedVars = connectedVars;

		this.extensionalDistance = extDist;
		this.relaxationDistance = relaxDist;
	}

	/**
	 * @return the elements available to refine the concept
	 */
	public Set<Element> getAvailableElements() {
		return availableElements;
	}

	/**
	 * @return the intension of the concept, i.e. the graph pattern matched by the extension of the concept
	 */
	public Set<Element> getIntensionBody() {
		return intensionBody;
	}

	/**
	 * @return true if all the vars of the set are connected to the concepts, false elsewhere.
	 */
	public boolean connected(Set<Var> vars) {
		for(Var v : vars) {
			if(connectedVars.contains(v)) {
				return true;
			}
		}
		return false;
	}

	/**
	 * @return the match-set representing the extension of the concept formatted as a match-tree.
	 */
	public MatchTree getMatchTree() {
		return matchTree;
	}

	/**
	 * @return the proper extension of a concept, i.e. the element of the extension that
	 * are not in the extension of more general concepts, as a {@link connor.utils.Table Table}.
	 */
	public Table getProperExtension() {
		return properExtension;
	}

	/**
	 * @return the list of projection {@link org.apache.jena.sparql.core.Var vars} of the intension
	 */
	public List<Var> getProjectionVars() {
		return projectionVars;
	}

	/**
	 * @return the set of the {@link org.apache.jena.sparql.core.Var vars} used in the intension of the concept
	 */
	public Set<Var> getConnectedVars() {
		return connectedVars;
	}

	/**
	 * @return the distance of this concept from the target, given in terms of number of relaxations
	 */
	public int getRelaxationDistance() {
		return relaxationDistance;
	}

	/**
	 * @return the extension of the concept as a {@link connor.utils.Table Table}
	 */
	public Table getExtension() {
		return this.extension;
	}

	/**
	 * @return a JSON-formatted string traducing the concept
	 */
	@Override
	public String toJson() {
		String res = "{\n";
		res += "\"id\":" + id + ",\n";
		res += "\"numberOfRelaxation\":" + this.relaxationDistance + ",\n";
		res += "\"extensionalDistance\":" + this.extensionalDistance + ",\n";
		res += "\"intension\":" + ElementUtils.setToJson(this.intensionBody) + ",\n";
		res += "\"answers\":" + this.properExtension.toJson() + ",\n";
		res += "\"extension\":" + this.getExtension().toJson();
		res += "\n}";
		return res;
	}

	/**
	 * @return the distance of the concept from its target, given in terms of number of tuples between the target and the concept
	 */
	public int getExtensionalDistance() {
		return extensionalDistance;
	}

	/**
	 * Compare the size of the intension of two concepts
	 * @param conceptOfNeighbours the concept to which compare
	 * @return the difference between the size of the intension of the two concepts
	 */
	@Override
	public int compareTo(ConceptOfNeighbours conceptOfNeighbours) {
		return this.intensionBody.size() - conceptOfNeighbours.intensionBody.size();
	}

	/**
	 * Saturates in type the intension
	 * @param model the {@link connor.ConnorModel model} from which the subClassOf trples are used
	 */
	public void saturateIntension(ConnorModel model) {
		Set<ElementTriplesBlock> typesTriples = new HashSet<>();
		for(Element element : intensionBody) {
			if(element instanceof ElementTriplesBlock) {
				String property = ((ElementTriplesBlock) element).getPattern().get(0).getPredicate().getURI();
				if(property.equals((RDF.type).getURI())) {
					typesTriples.add((ElementTriplesBlock) element);
				}
			}
		}


		boolean hasChanged;
		do {
			hasChanged = false;
			Set<ElementTriplesBlock> newTriples = new HashSet<>();
			for(ElementTriplesBlock triple : typesTriples) {
				Node subject = triple.getPattern().get(0).getSubject();
				Node object = triple.getPattern().get(0).getObject();

				if(model.getTriples().containsKey(object) && model.getTriples().get(object).containsKey(RDFS.subClassOf)) {
					for(Node type : model.getTriples().get(object).get(RDFS.subClassOf)) {
						ElementTriplesBlock newTriple = new ElementTriplesBlock();
						newTriple.addTriple(new Triple(subject, RDF.type.asNode(), type));

						if(!typesTriples.contains(newTriple)) {
							hasChanged = true;
							newTriples.add(newTriple);
						}
					}
				}
			}
			typesTriples.addAll(newTriples);
		} while(hasChanged);

		this.intensionBody.addAll(typesTriples);

	}

}
