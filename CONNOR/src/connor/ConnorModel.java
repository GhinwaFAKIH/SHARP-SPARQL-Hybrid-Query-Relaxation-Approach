package connor;

import connor.utils.Table;
import connor.utils.TableException;
import org.apache.jena.graph.Node;
import org.apache.jena.rdf.model.*;
import org.apache.jena.reasoner.ReasonerRegistry;
import org.apache.jena.sparql.core.Var;
import org.apache.jena.sparql.engine.binding.BindingFactory;
import org.apache.jena.sparql.engine.binding.BindingBuilder;
import org.apache.jena.sparql.syntax.Element;
import org.apache.jena.sparql.syntax.ElementTriplesBlock;
import org.apache.jena.vocabulary.RDF;

import java.util.*;

/**
 * Representation of a RDF graph conceived for the computation of concepts of neighbours
 * Encapsulate an
 *
 * @author nk-fouque
 * @author hayats
 */
public class ConnorModel {
	/* RDF inference model of the graph */
	private final InfModel model;

	/* Hashmaps in order to fasten the access to the triples */
	private final Map<Node, Map<Property, List<Node>>> triples = new HashMap<>();
	private final Map<Node, Map<Property, List<Node>>> reversedTriples = new HashMap<>();

	/* Precomputed answers*/
	private final Map<Property, Set<List<Node>>> answersTriples = new HashMap<>();
	private final Map<Node, Set<Node>> answersTypes = new HashMap<>();

	/**
	 * Create a conceptsOfNeighbours.ConceptualKNNModel based on a pre-existing {@link org.apache.jena.rdf.model.InfModel}
	 *
	 * @param model the pre-existing InfModel
	 */
	public ConnorModel(InfModel model) {
		this.model = model;

		StmtIterator iterator = model.getRawModel().listStatements();
		iterator.forEachRemaining(stmt -> {
			RDFNode subj = stmt.getSubject();
			Property prop = stmt.getPredicate();
			RDFNode obj = stmt.getObject();

			Map<Property, List<Node>> propertiesFrom = triples.computeIfAbsent(subj.asNode(), m -> new HashMap<>());
			List<Node> thatPropertyFrom = propertiesFrom.computeIfAbsent(prop, l -> new ArrayList<>());
			thatPropertyFrom.add(obj.asNode());

			Map<Property, List<Node>> propertiesTo = reversedTriples.computeIfAbsent(obj.asNode(), m -> new HashMap<>());
			List<Node> thatPropertyTo = propertiesTo.computeIfAbsent(prop, l -> new ArrayList<>());
			thatPropertyTo.add(subj.asNode());
		});
	}

	/**
	 * Create an empty model
	 */
	public ConnorModel() {
		this(ModelFactory.createInfModel(ReasonerRegistry.getRDFSReasoner(), ModelFactory.createDefaultModel()));
	}

	/**
	 * Add a statement to the model
	 *
	 * @param stmt the {@link org.apache.jena.rdf.model.Statement Statement} to add
	 * @return the model
	 */
	public ConnorModel add(Statement stmt) {
		this.model.add(stmt);

		RDFNode subj = stmt.getSubject();
		Property prop = stmt.getPredicate();
		RDFNode obj = stmt.getObject();

		Map<Property, List<Node>> propertiesFrom = triples.computeIfAbsent(subj.asNode(), m -> new HashMap<>());
		List<Node> thatPropertyFrom = propertiesFrom.computeIfAbsent(prop, l -> new ArrayList<>());
		thatPropertyFrom.add(obj.asNode());

		Map<Property, List<Node>> propertiesTo = reversedTriples.computeIfAbsent(obj.asNode(), m -> new HashMap<>());
		List<Node> thatPropertyTo = propertiesTo.computeIfAbsent(prop, l -> new ArrayList<>());
		thatPropertyTo.add(subj.asNode());

		return this;
	}

	/**
	 * Add a list of statements to the model
	 *
	 * @param statements the list of {@link org.apache.jena.rdf.model.Statement statements}
	 * @return the model
	 */
	public ConnorModel add(List<Statement> statements) {
		statements.forEach(this::add);
		return this;
	}

	/**
	 * Create a Resource attached to the model
	 *
	 * @param uri the URI of the resource
	 * @return the created Resource
	 */
	public Resource createResource(String uri) {
		return this.model.createResource(uri);
	}

	/**
	 * Create a Property attached to the model
	 *
	 * @param prefix prefix of the Property's URI
	 * @param uri    suffix of the Property's URI
	 * @return the created Property
	 */
	public Property createProperty(String prefix, String uri) {
		return this.model.createProperty(prefix, uri);
	}

	/**
	 * create a Statement attached to the model
	 *
	 * @param s the subject of the statement
	 * @param p the property of the statement
	 * @param o the object of the statement
	 * @return the created Statement
	 */
	public Statement createStatement(Resource s, Property p, RDFNode o) {
		return this.model.createStatement(s, p, o);
	}

	/**
	 * Create a Literal attached to the model
	 *
	 * @param s the name of the Literal
	 * @return the created Literal
	 */
	public RDFNode createLiteral(String s) {
		return this.model.createLiteral(s);
	}

	/**
	 * Remove a statement (triple) from the model
	 *
	 * @param statement the @link{Statement} to remove
	 */
	public void remove(Statement statement) {
		this.model.remove(statement);

		Map<Property, List<Node>> propertiesFrom = this.triples.get(statement.getSubject().asNode());
		List<Node> thatPropertyFrom = propertiesFrom.get(statement.getPredicate());
		thatPropertyFrom.remove(statement.getObject().asNode());
		if(thatPropertyFrom.isEmpty()) {
			propertiesFrom.remove(statement.getPredicate());
			if(propertiesFrom.isEmpty()) {
				triples.remove(statement.getSubject().asNode());
			}
		}
		Map<Property, List<Node>> propertiesTo = this.reversedTriples.get(statement.getObject().asNode());
		List<Node> thatPropertyTo = propertiesTo.get(statement.getPredicate());
		thatPropertyTo.remove(statement.getSubject().asNode());
		if(thatPropertyFrom.isEmpty()) {
			propertiesFrom.remove(statement.getPredicate());
			if(propertiesFrom.isEmpty()) {
				triples.remove(statement.getObject().asNode());
			}
		}
	}

	/**
	 * Remove a list of statements from the model.
	 *
	 * @param statements The list of statements to remove
	 */
	public void remove(List<Statement> statements) {
		statements.forEach(this::remove);
	}

	/**
	 * @return the set of triples of the graph as a hashmap of hashmaps.
	 */
	public Map<Node, Map<Property, List<Node>>> getTriples() {
		return triples;
	}

	/**
	 * @return  the set of triples as a hashmap of hashmaps, accessed by the object first.
	 */
	public Map<Node, Map<Property, List<Node>>> getReversedTriples() {
		return reversedTriples;
	}

	/**
	 * @return the InfModel encapsulated in this model
	 */
	public InfModel getModel() {
		return model;
	}

	/**
	 * Add an entry to the precomputed answers
	 * Should not be used by the user
	 *
	 * @param element The element for which the answer has been computed
	 * @param res     The computed answer
	 */
	public void addAns(Element element, Table res) {
		if(element instanceof ElementTriplesBlock) {
			Var subjectVar = (Var) (((ElementTriplesBlock) element).getPattern().get(0).getSubject()); // necessarily a var, as constants are managed through filters
			Property property = ResourceFactory.createProperty(((ElementTriplesBlock) element).getPattern().get(0).getPredicate().getURI());
			Node object = ((ElementTriplesBlock) element).getPattern().get(0).getObject();

			int subjIndex = res.getSchema().indexOf(subjectVar);

			if(property.equals(RDF.type)) {
				Set<Node> answer = new HashSet<>();
				res.rows().forEachRemaining(row -> answer.add(row.get(subjIndex)));
				this.answersTypes.put(object, answer);
			} else {
				Var objectVar = (Var) object;

				int objIndex = res.getSchema().indexOf(objectVar);

				Set<List<Node>> answer = new HashSet<>();
				res.rows().forEachRemaining(row -> {
					List<Node> ans = new ArrayList<>();
					ans.add(row.get(subjIndex));
					ans.add(row.get(objIndex));
					answer.add(ans);
				});
				this.answersTriples.put(property, answer);

			}
		}
	}

	/**
	 * Return the set of possible answers to the query composed of only one element, if it has already been computed
	 *
	 * @param element The element of the query
	 * @return The table representing the answers
	 */
	public Table getAnswer(Element element) throws TableException {
		if(element instanceof ElementTriplesBlock) {
			Var subjectVar = (Var) (((ElementTriplesBlock) element).getPattern().get(0).getSubject()); // necessarily a var, as constants are managed through filters
			Property property = ResourceFactory.createProperty(((ElementTriplesBlock) element).getPattern().get(0).getPredicate().getURI());
			Node object = ((ElementTriplesBlock) element).getPattern().get(0).getObject();

			if(property.equals(RDF.type)) {
				if(!this.answersTypes.containsKey(object)) {
					return null;
				}
				Set<Node> answers = this.answersTypes.get(object);

				List<Var> vars = new ArrayList<>();
				vars.add(subjectVar);
				Table table = new Table(vars);
				answers.forEach(node -> table.addBinding(BindingFactory.binding(subjectVar, node)));
				return table;
			} else {
				if(!this.answersTriples.containsKey(property)) {
					return null;
				}
				Set<List<Node>> answers = this.answersTriples.get(property);
				Var objectVar = (Var) object;

				List<Var> vars = new ArrayList<>();
				vars.add(subjectVar);
				vars.add(objectVar);
				Table table = new Table(vars);

				answers.forEach(list -> {
					BindingBuilder builder = BindingFactory.builder();
					builder.add(subjectVar, list.get(0));
					builder.add(objectVar, list.get(1));
					table.addBinding(builder.build());
				});
				return table;
			}
		}

		return null;
	}
}
