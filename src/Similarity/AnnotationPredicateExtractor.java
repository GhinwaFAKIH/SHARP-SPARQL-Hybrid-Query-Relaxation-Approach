package Similarity;

import org.apache.jena.graph.Node;
import org.apache.jena.graph.Triple;
import org.apache.jena.riot.Lang;
import org.apache.jena.riot.system.StreamRDF;
import org.apache.jena.sparql.core.Quad;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;

public class AnnotationPredicateExtractor {

    static class CustomStreamRDF implements StreamRDF {

        private Map<Node, Set<Node>> graphToPredicatesMap = new HashMap<>();
        private List<Node> graphs_id = new ArrayList<>();
        private List<Quad> quads = new ArrayList<>();

        @Override
        public void start() {

        }

        @Override
        public void base(String s) {

        }

        @Override
        public void prefix(String s, String s1) {

        }

        @Override
        public void finish() {

        }


        @Override
        public void triple(Triple triple) {
            // Check if the predicate is associated with a graph and update the map
            Node subject = triple.getSubject();
            Node predicate = triple.getPredicate();
            Node object = triple.getObject();
            System.out.println("graph_ids" + graphs_id);
            if(this.graphs_id.contains(subject) ){
                graphToPredicatesMap
                        .computeIfAbsent(subject, k -> new HashSet<>())
                        .add(predicate);
                System.out.println("annotation " + predicate);
            }
            if(this.graphs_id.contains(object) ){
                graphToPredicatesMap
                        .computeIfAbsent(object, k -> new HashSet<>())
                        .add(predicate);
            }
/*
            if (subject.isURI() || object.isURI()) {
                Node graph = subject.isURI() ? subject : object;
                graphToPredicatesMap
                        .computeIfAbsent(graph, k -> new HashSet<>())
                        .add(predicate);
            }

 */
        }

        @Override
        public void quad(Quad quad) {
            // Get all graphs IDs
            if (!quad.getGraph().getURI().equals("urn:x-arq:DefaultGraphNode"))
            //{ //graphs_id.add(quad.getGraph());
            {this.graphs_id.add(quad.getGraph());}
            Node subject = quad.getSubject();
            Node predicate = quad.getPredicate();
            Node object = quad.getObject();
            if (this.graphs_id.contains(subject)) {
                graphToPredicatesMap
                        .computeIfAbsent(subject, k -> new HashSet<>())
                        .add(predicate);
                System.out.println("annotation " + predicate);
            }
            if (this.graphs_id.contains(object)) {
                graphToPredicatesMap
                        .computeIfAbsent(object, k -> new HashSet<>())
                        .add(predicate);
            }
            quads.add(quad);
        //}

            // No need to process quads separately, handled in the triple method
            //triple(quad.asTriple());
            //System.out.println("the quad is " + quad.getGraph());
        }

        // Check if the predicate is associated with a graph and update the map
        public List<Quad> returnQauds() {
            return quads;
        }

        public List<Node> getUniqueGraphs() {
            return graphs_id;
        }


        public Map<Node, Set<Node>> getGraphToPredicatesMap() {
            return graphToPredicatesMap;
        }

    };
    private static boolean isUsedAsSubjectOrObject(Map<Node, Set<Node>> graphToPredicatesMap, Node predicate) {
        // Check if the predicate is used as subject or object in other triples
        for (Set<Node> predicates : graphToPredicatesMap.values()) {
            if (predicates.contains(predicate)) {
                return true;
            }
        }
        return false;
    }

    public static Set<Node> extractAnnotationPredicates(String filePath) throws IOException {
        CustomStreamRDF customStreamRDF = new CustomStreamRDF();

        try (InputStream input = new FileInputStream(filePath)) {
            org.apache.jena.riot.RDFParser.create()
                    .source(input)
                    .lang(Lang.NQ)
                    .parse(customStreamRDF);
        }

        Map<Node, Set<Node>> graphToPredicatesMap = customStreamRDF.getGraphToPredicatesMap();
        List<Node> uniqueGraphs = customStreamRDF.getUniqueGraphs();
        List<Quad> quads = customStreamRDF.returnQauds();
        for (Quad quad : quads) {
            ///// To continue
        }

        Set<Node> annotationPredicates = new HashSet<>();

        // Check each graph if used as subject or object and collect associated predicates
        for (Set<Node> predicates : graphToPredicatesMap.values()) {
            for (Node predicate : predicates) {
                if (isUsedAsSubjectOrObject(graphToPredicatesMap, predicate)) {
                    annotationPredicates.add(predicate);
                }
            }
        }
        System.out.println("graph to predicate map " + graphToPredicatesMap);
        return annotationPredicates;
    }

    public static void main(String[] args) {
        try {
            String filePath = "data/tests.nq";
            Set<Node> annotationPredicates = extractAnnotationPredicates(filePath);

            System.out.println("Annotation Predicates:");
            for (Node predicate : annotationPredicates) {
                System.out.println(predicate);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}


