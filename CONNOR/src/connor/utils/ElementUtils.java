package connor.utils;

import connor.ConnorModel;
import connor.ConnorPartition;
import org.apache.jena.graph.Node;
import org.apache.jena.graph.Triple;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.ResourceFactory;
import org.apache.jena.sparql.core.Var;
import org.apache.jena.sparql.engine.binding.BindingBuilder;
import org.apache.jena.sparql.engine.binding.BindingFactory;
import org.apache.jena.sparql.expr.*;
import org.apache.jena.sparql.expr.nodevalue.NodeValueNode;
import org.apache.jena.sparql.syntax.Element;
import org.apache.jena.sparql.syntax.ElementFilter;
import org.apache.jena.sparql.syntax.ElementTriplesBlock;
import org.apache.jena.vocabulary.RDF;
import org.apache.jena.vocabulary.RDFS;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Class containing static methods for operations on the Element class
 *
 * @author hayats
 * @author nk-fouque
 */

public class ElementUtils {
	/**
	 * Compute which variables are used in a query element
	 *
	 * @param element The element to get the variables from
	 * @return The variables used in this element
	 */
	public static Set<Var> mentioned(Element element) {
		Set<Var> vars = new HashSet<>();
		if(element instanceof ElementFilter) {
			vars.addAll(((ElementFilter) element).getExpr().getVarsMentioned());
		} else // if (element instanceof ElementPathBlock)
		{
			vars.addAll((new E_Exists(element)).getVarsMentioned());
		}
		return vars;
	}

	/**
	 * Simulate the execution of an element as a query on the model
	 *
	 * @param element The Element to use as query
	 * @param partition The partition containing the model
	 * @return A Table containing the answers to the query
	 */
	public static Table answer(Element element, ConnorPartition partition) throws TableException {
		Table ans = partition.getAnswer(element);
		// If the answer has been already computed
		if(ans != null) {
			return ans;
		}

		Table res = null;
		ConnorModel model = partition.getModel();

		if(element instanceof ElementFilter) {// If the element is a "equal" filter: return what the var is equal to
			Expr expr = ((ElementFilter) element).getExpr();
			if(expr instanceof E_Equals) {
				Var var = ((E_Equals) expr).getArg1().asVar();
				List<Var> schema = new ArrayList<>();
				schema.add(var);
				res = new Table(schema);
				Node node = ((NodeValue) (((E_Equals) expr).getArg2())).asNode();
				res.addBinding(BindingFactory.binding(var, node));
			}

		} else if(element instanceof ElementTriplesBlock) {// If the element is a triple
			Var subject = (Var) (((ElementTriplesBlock) element).getPattern().get(0).getSubject()); // necessarily a var, as constants are managed through filters
			Property property = ResourceFactory.createProperty(((ElementTriplesBlock) element).getPattern().get(0).getPredicate().getURI());
			Node object = ((ElementTriplesBlock) element).getPattern().get(0).getObject();

			if(object.isVariable()) { // most of the time
				Var objVar = (Var) object;
				List<Var> schema = new ArrayList<>();
				schema.add(subject);
				schema.add(objVar);
				final Table table = new Table(schema);

				model.getModel().listSubjectsWithProperty(property).forEachRemaining(subj ->
						model.getModel().listObjectsOfProperty(subj, property).forEachRemaining(obj -> {
							BindingBuilder builder = BindingFactory.builder();
							builder.add(subject, subj.asNode());
							builder.add(objVar, obj.asNode());
							table.addBinding(builder.build());
						})
				);

				res = table;
			} else { // only happens for types and ontology triples
				//System.out.println("the property URI is " + property.getURI());
				//System.out.println("the object is  a URI:" + object.isURI());
				//System.out.println("the object node is " + object);
				//System.out.println("the object URI is " + object.getURI());
				if(object.isURI()) {
					RDFNode rdfObject = ResourceFactory.createResource(object.getURI());

				List<Var> schema = new ArrayList<>();
				schema.add(subject);
				final Table table = new Table(schema);

				model.getModel().listSubjectsWithProperty(property, rdfObject).forEachRemaining(subj ->
						table.addBinding(BindingFactory.binding(subject, subj.asNode()))
				);

				res = table;
				}
			}

		}
		if(res !=null){
		partition.addAns(element, res);}
		return res;
	}

	/**
	 * Return the description of a set of node, i.e the set of Elements representing the triples linked to those nodes
	 *
	 * @param uriList the nodes to describe
	 * @return the descriptions of the nodes
	 */
	public static Set<Element> getNodesDescription(List<String> uriList, ConnorPartition partition) {
		Set<Element> description = new HashSet<>();
		for(String uri : uriList) {
			Node node = ResourceFactory.createResource(uri).asNode();
			description.addAll(ElementUtils.getNodeDescription(node, partition));
		}
		return description;
	}

	private static Set<Element> getNodeDescription(Node node, ConnorPartition partition) {
		int nodeDepth = partition.getDepth(node);
		if(partition.getMaxDescriptionDepth() >= 0 && nodeDepth >= partition.getMaxDescriptionDepth()) {
			return new HashSet<>();
		}

		Set<Element> description = new HashSet<>();

		ConnorModel model = partition.getModel();

		if(model.getTriples().containsKey(node)) {
			model.getTriples().get(node).forEach((property, objects) -> { // describe the node by the triples having the node as subject
				if(property.equals(RDF.type)) { // use the types as labels
					objects.forEach(rdfNode -> {
						ElementTriplesBlock tripleElement = new ElementTriplesBlock();
						tripleElement.addTriple(Triple.create(partition.getOrCreateVar(node), property.asNode(), rdfNode));
						description.add(tripleElement);
					});
				} else { //
					objects.forEach(object -> {
						partition.setDepth(object, nodeDepth + 1);
						Var var = partition.getOrCreateVar(object);
						ElementTriplesBlock triple = new ElementTriplesBlock();
						triple.addTriple(Triple.create(partition.getOrCreateVar(node), property.asNode(), var));
						ElementFilter filter = new ElementFilter((new E_Equals(new ExprVar(var), new NodeValueNode(object))));
						description.add(triple);
						description.add(filter);
					});
				}
			});
		}

		if(model.getReversedTriples().containsKey(node)) {
			model.getReversedTriples().get(node).forEach((property, subjects) -> { // describe the node by the triples having the node as object
				if(!property.equals(RDF.type)) { // avoid to describe the classes by their members
					subjects.forEach(subject -> {
						partition.setDepth(subject, nodeDepth + 1);
						Var var = partition.getOrCreateVar(subject);
						ElementTriplesBlock triple = new ElementTriplesBlock();
						triple.addTriple(Triple.create(var, property.asNode(), partition.getOrCreateVar(node)));
						ElementFilter filter = new ElementFilter(new E_Equals(new ExprVar(var), new NodeValueNode(subject)));
						description.add(triple);
						description.add(filter);
					});
				}
			});
		}
		return description;
	}

	public static Set<Element> getInitialDescription(List<String> uriList, ConnorPartition partition) {
		Set<Element> description = new HashSet<>();

		for(String uri : uriList) {
			Node node = ResourceFactory.createResource(uri).asNode();
			ElementFilter filter = new ElementFilter(new E_Equals(new ExprVar(partition.getOrCreateVar(node)), new NodeValueNode(node)));
			description.add(filter);
		}

		return description;
	}

	/**
	 * Compute the relaxation of an element on a RDF model. A filter is relaxed by the description
	 * of the node, a triple is relaxed by its direct super-properties and a rdf:type is relaxed
	 * by its superclasses.
	 *
	 * @param elt       the element to relax
	 * @param partition the partition on which the element has to be relaxed
	 * @return The set of element representing the relaxation of elt
	 */
	public static Set<Element> relax(Element elt, ConnorPartition partition) {
		Set<Element> relaxation;

		if(elt instanceof ElementFilter) {
			Expr expr = ((ElementFilter) elt).getExpr();
			Node node = ((NodeValue) ((E_Equals) expr).getArg2()).asNode();
			relaxation = ElementUtils.getNodeDescription(node, partition);
		} else {
			relaxation = new HashSet<>();

			Triple triple = ((ElementTriplesBlock) elt).getPattern().get(0);
			Node prop = triple.getPredicate();
			Node object = triple.getObject();
			Node subject = triple.getSubject();

			if(prop.equals(RDF.type.asNode())) {
				if(partition.getModel().getTriples().containsKey(object)) {
					if(partition.getModel().getTriples().get(object).containsKey(RDFS.subClassOf)) {
						partition.getModel().getTriples().get(object).get(RDFS.subClassOf).forEach(superClass -> {
							ElementTriplesBlock superType = new ElementTriplesBlock();
							superType.addTriple(new Triple(subject, RDF.type.asNode(), superClass));
							relaxation.add(superType);
						});
					}
				}
			} else {
				if(partition.getModel().getTriples().containsKey(prop)) {
					if(partition.getModel().getTriples().get(prop).containsKey(RDFS.subPropertyOf)) {
						partition.getModel().getTriples().get(prop).get(RDFS.subPropertyOf).forEach(superProp -> {
							ElementTriplesBlock superPropElt = new ElementTriplesBlock();
							superPropElt.addTriple(new Triple(subject, superProp, object));
							relaxation.add(superPropElt);
						});
					}
					if(partition.getModel().getTriples().get(prop).containsKey(RDFS.domain)) {
						partition.getModel().getTriples().get(prop).get(RDFS.domain).forEach(type -> {
							ElementTriplesBlock domainType = new ElementTriplesBlock();
							domainType.addTriple(new Triple(subject, RDF.type.asNode(), type));
							relaxation.add(domainType);
						});
					}
					if(partition.getModel().getTriples().get(prop).containsKey(RDFS.range)) {
						partition.getModel().getTriples().get(prop).get(RDFS.range).forEach(type -> {
							ElementTriplesBlock rangeType = new ElementTriplesBlock();
							rangeType.addTriple(new Triple(object, RDF.type.asNode(), type));
							relaxation.add(rangeType);
						});
					}
				}
			}
		}
		return relaxation;
	}

	/**
	 * Function to export a set of element as a JSON array
	 *
	 * @param set the set to transcode
	 * @return a JSON-formatted string
	 */
	public static String setToJson(Set<Element> set) {
		StringBuilder res = new StringBuilder("[");
		for(Element e : set) {
			if(!res.toString().equals("[")) {
				res.append(",");
			}
			res.append("\"").append(e.toString().replaceAll("([^\\\\])\"", "$1\\\\\"")).append("\"");
		}
		res.append("]");
		return res.toString();
	}
}