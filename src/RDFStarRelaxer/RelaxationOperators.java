package RDFStarRelaxer;

import org.apache.jena.graph.Node;
import org.apache.jena.graph.NodeFactory;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class RelaxationOperators {
    public static String PREFIX_RDF = "http://www.w3.org/1999/02/22-rdf-syntax-ns#";
    public static String PREFIX_RDFS = "http://www.w3.org/2000/01/rdf-schema#";
/*
    public Map<Node, Integer> getSuperClasses(Node classeNode) {

        Map<Node, Integer> superNodes = new LinkedHashMap<Node, Integer>();
        List<String> toFindSuperClass = new ArrayList<String>();
        List<String> alreadyInserted = new ArrayList<String>();
        if (classeNode.isURI()) {
            toFindSuperClass.add(classeNode.getURI());
        }
        int level = 0;

        while (!toFindSuperClass.isEmpty()) {
            String currentURI = toFindSuperClass.remove(0);
            String supClasses = session.getOntology().getSuperClass(currentURI);
            level = level + 1;
            if (supClasses != null) {
                if ((!alreadyInserted.contains(supClasses))
                        && (!supClasses.substring(0, PREFIX_RDFS.length()).equals(PREFIX_RDFS))
                        && (!supClasses.substring(0, PREFIX_RDF.length()).equals(PREFIX_RDF))) {
                    Node relax_node = NodeFactory.createURI(supClasses);
                    superNodes.put(relax_node, level);
                    alreadyInserted.add(relax_node.getURI());
                    toFindSuperClass.add(relax_node.getURI());
                }
            }
        }
        return superNodes;
    }

    public Map<String, Integer> getURISuperClasses(Node classeNode) {

        Map<String, Integer> superNodes = new LinkedHashMap<String, Integer>();
        List<String> toFindSuperClass = new ArrayList<String>();
        List<String> alreadyInserted = new ArrayList<String>();
        if (classeNode.isURI()) {
            toFindSuperClass.add(classeNode.getURI());
        }
        int level = 0;

        while (!toFindSuperClass.isEmpty()) {
            String currentURI = toFindSuperClass.remove(0);
            String supClasses = session.getOntology().getSuperClass(currentURI);
            level = level + 1;
            if (supClasses != null) {
                if (!alreadyInserted.contains(supClasses)) {
                    superNodes.put(supClasses, level);
                    alreadyInserted.add(supClasses);
                    toFindSuperClass.add(supClasses);
                }
            }
        }
        return superNodes;
    }

    public Map<Node, Integer> getSuperProperty(Node property) {

        Map<Node, Integer> superPropertiesNodes = new LinkedHashMap<Node, Integer>();
        List<String> toFindSuperProperties = new ArrayList<String>();
        List<String> alreadyInserted = new ArrayList<String>();
        if (property.isURI()) {
            toFindSuperProperties.add(property.getURI());
            alreadyInserted.add(property.getURI());
        }

        int level = 0;

        while (!toFindSuperProperties.isEmpty()) {
            String currentPropertyURI = toFindSuperProperties.remove(0);
            String superProperties = session.getOntology().getSuperProperty(currentPropertyURI);

            level = level + 1;
            if (superProperties != null) {
                if (!alreadyInserted.contains(superProperties)) {
                    Node relax_property = NodeFactory.createURI(superProperties);
                    superPropertiesNodes.put(relax_property, level);
                    alreadyInserted.add(relax_property.getURI());
                    toFindSuperProperties.add(relax_property.getURI());
                }
            }
        }
        return superPropertiesNodes;
    }

    public Map<String, Integer> getURISuperProperty(Node property) {

        Map<String, Integer> superPropertiesNodes = new LinkedHashMap<String, Integer>();
        List<String> toFindSuperProperties = new ArrayList<String>();
        List<String> alreadyInserted = new ArrayList<String>();
        if (property.isURI()) {
            toFindSuperProperties.add(property.getURI());
            alreadyInserted.add(property.getURI());
        }

        int level = 0;

        while (!toFindSuperProperties.isEmpty()) {
            String currentPropertyURI = toFindSuperProperties.remove(0);
            String superProperties = session.getOntology().getSuperProperty(currentPropertyURI);

            level = level + 1;
            if (superProperties != null) {
                if (!alreadyInserted.contains(superProperties)) {
                    superPropertiesNodes.put(superProperties, level);
                    alreadyInserted.add(superProperties);
                    toFindSuperProperties.add(superProperties);
                }
            }
        }
        return superPropertiesNodes;
    }


 */

}
