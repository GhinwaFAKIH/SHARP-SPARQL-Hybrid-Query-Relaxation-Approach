import RDFStarRelaxer.RelaxedQuery;
import RDFStarRelaxer.TriplePatternRelaxer;
import org.apache.jena.graph.Node;
import org.apache.jena.graph.NodeFactory;
import org.apache.jena.graph.Triple;
import org.apache.jena.query.Query;
import org.apache.jena.query.QueryFactory;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.sparql.core.TriplePath;
import org.apache.jena.sparql.syntax.Element;
import org.apache.jena.sparql.syntax.ElementGroup;
import org.apache.jena.sparql.syntax.ElementPathBlock;
import org.apache.jena.util.FileManager;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static RDFStarRelaxer.ShaclMapping.getShaclMap;


public class Main {


    public static void main(String[] args) {
        // Create a sample SPARQL query
        //String queryString = "PREFIX ex:<http://example.org/>" + "SELECT ?s ?o WHERE { << ?s ex:p1 ex:o1 >> ex:p2 ?o . " +
        //"?s ex:p3 ex:o3.}";
        //Query query = QueryFactory.create(queryString);
        //String queryString2 = "PREFIX ex:<http://example.org/>" + "SELECT *  WHERE { << << ?s ex:p1 ex:o1 >> ex:p2 << ?o ex:p3 ex:o3>> >> ex:p4 ex:o4.} ";
        //Query query2 = QueryFactory.create(queryString2);

        //TraversingQuery.visitor(query);
        //TraversingRDFstarQuery.failingnestedquery (query2, query2);

        String endpointURL = "http://localhost:3000/closure/query";
        String outputPath1 = "superclasses.json";
        String outputPath2 = "superproperties.json";
        String query_string = " PREFIX ex:<http://www.example.org/>" +
                "SELECT * WHERE { << ?s ?p ?o>> ex:p1 ex:o1." +
                " <<?s ?p ?o>> ex:p2 ex:o2. }";

        String rdfstar = "PREFIX ex:<http://example.org/>" +
                "PREFIX rdf:<http://www.w3.org/1999/02/22-rdf-syntax-ns#>" +
                "SELECT * WHERE { << ?s rdf:type ex:Student >> ex:attends ex:Semantic_Web.}";
        String querytoRelax ="PREFIX xsd: <http://www.w3.org/2001/XMLSchema#> \n" +
                "                             PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#> \n" +
                "                             PREFIX ex: <http://example.org/> \n" +
                "                             PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#> \n" +
                "                             PREFIX owl: <http://www.w3.org/2002/07/owl#> \n" +
                "                             PREFIX foaf: <http://xmlns.com/foaf/0.1/> \n" +
                "SELECT ?student WHERE { " +
                "\n" +
                "   ?teacher       rdf:type              ex:Teacher;\n" +
                "                  ex:teaches            ex:Semantic_Web.\n" +
                "   ?student       rdf:type              ex:Student;\n" +
                //"                  ex:friendOf/foaf:knows  ex:Alice;\n" +
                "                  foaf:knows              ?teacher.\n" +
                "   ?statement     rdf:type              rdf:Statement;\n" +
                "                  rdf:subject           ?student;\n" +
                "                  rdf:object            ex:Semantic_Web;\n" +
                "                  rdf:predicate         ex:enrolledIn;\n" +
                "                  ex:enrolldate         \"2023-09-13\"^^xsd:date.\n" +
                "             }";
        String querytoRelaxStar = "PREFIX xsd: <http://www.w3.org/2001/XMLSchema#> \n" +
                "                             PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#> \n" +
                "                             PREFIX ex: <http://example.org/> \n" +
                "                             PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#> \n" +
                "                             PREFIX owl: <http://www.w3.org/2002/07/owl#> \n" +
                "                             PREFIX foaf: <http://xmlns.com/foaf/0.1/> \n" +
                "SELECT ?student WHERE { " +
                "\n" +
                "   ?teacher       rdf:type              ex:Teacher;\n" +
                "                  ex:teaches            ex:Semantic_Web.\n" +
                "   ?student       rdf:type              ex:Student;\n" +
                //"                  ex:friendOf/foaf:knows  ex:Alice;\n" +
                "                  foaf:knows              ?teacher.\n" +
                "<<?student ex:enrolledIn ex:Semantic_Web >> ex:enrolldate  \"2023-09-13\"^^xsd:date.\n" +
                "             }";

        String querytoRelaxQuad = "PREFIX xsd: <http://www.w3.org/2001/XMLSchema#> \n" +
                "                             PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#> \n" +
                "                             PREFIX ex: <http://example.org/> \n" +
                "                             PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#> \n" +
                "                             PREFIX owl: <http://www.w3.org/2002/07/owl#> \n" +
                "                             PREFIX foaf: <http://xmlns.com/foaf/0.1/> \n" +
                "SELECT ?student WHERE { " +
                "\n" +
                "   ?teacher       rdf:type              ex:Teacher;\n" +
                "                  ex:teaches            ex:Semantic_Web.\n" +
                "   ?student       rdf:type              ex:Student;\n" +
                //"                  ex:friendOf/foaf:knows  ex:Alice;\n" +
                "                  foaf:knows              ?teacher.\n" +
                "GRAPH ?g { \n" +
                "    ?student ex:enrolledIn ex:Semantic_Web. }" +
                "    ?g       ex:enrolldate \"2023-09-13\"^^xsd:date. \n" +
                "           }";


        RelaxedQuery relaxedQuery = new RelaxedQuery();
        Query relaxedquery = QueryFactory.create(querytoRelaxStar);
        Element pattern  = relaxedquery.getQueryPattern();
        // Assuming the pattern is an instance of ElementGroup
        if (pattern instanceof ElementGroup) {
            ElementGroup elementGroup = (ElementGroup) pattern;
            List<Element> elements = elementGroup.getElements();

            // Now 'elements' contains the list of elements in the group
            // You can iterate over this list and handle each element as needed
            for (Element element : elements) {
                if (element instanceof ElementPathBlock) {
                    ElementPathBlock pathBlock = (ElementPathBlock) element;
                    relaxedQuery.setRDFStarQueryPattern(pathBlock);
                    System.out.println(relaxedQuery.serialize());
                    /*
                    List<TriplePath> triples = pathBlock.getPattern().getList();
                    System.out.println(triples);
                    relaxedQuery.clone((ArrayList<TriplePath>) triples);

                     */
                    // Now 'triples' contains the list of TriplePath objects for this path block
                    // You can iterate over this list and perform any necessary operations
                }
            }
        }

        //ArrayList<TriplePath> triples = (ArrayList<TriplePath>) pathBlock.getPattern().getList();
        RelaxedQuery relaxedQuery2 = new RelaxedQuery();
        QueryFactory.parse(relaxedQuery2, querytoRelaxQuad, null, null);
        System.out.println("query pattern " + relaxedQuery2.getQueryPattern());
        //RelaxedQuery querytorelax2 = (RelaxedQuery) (RelaxedQuery) querytorelax;

/*
        Query rdfstarquery = QueryFactory.create(rdfstar);
        Element queryPattern = rdfstarquery.getQueryPattern();
        System.out.println("\nQuery Elements:");
        // Get the algebra expression of the query
        Op algebraOp = Algebra.compile(rdfstarquery);
        OpBGP opBGP = (OpBGP) algebraOp;
        List triples_list = ((OpBGP) algebraOp).getPattern().getList();
        Triple rdfstartriple = (Triple) (Triple) triples_list.get(0);

 */
        /*
        for(Triple bgptriple: ((OpBGP) algebraOp).getPattern().getList()) {
            Triple rdfstartriple = bgptriple;
            System.out.println("The triple pattern is: " + bgptriple);
        }

         */

        //Query query = QueryFactory.create(query_string);
        //queryvisitor(query);
        //Create json file containing all superclasses and superproperties
        //execQuery(endpointURL, LIST_SUPER_CLASSES, outputPath1);
        //execQuery(endpointURL, LIST_SUPER_PROPERTIES, outputPath2);

        // Create an RDF model
        Model ontology = ModelFactory.createDefaultModel();

        // Load your RDF dataset
        FileManager.get().readModel(ontology, "data/toyexampleontology.ttl");
        // Example triple pattern components
        String subject = "student";

        // Create Triple instance
        Triple triple = new Triple(
                NodeFactory.createVariable(subject),
                NodeFactory.createURI("http://www.w3.org/1999/02/22-rdf-syntax-ns#type"),
                NodeFactory.createURI("http://example.org/Student")  // Replace with the actual namespace for ex
        );// Create nodes for variables ?p and ?o

        Node pred = NodeFactory.createURI("http://example.org/attends");
        Node obj = NodeFactory.createURI("http://example.org/Semantic_Web");

        ArrayList<RelaxedQuery> relaxedQueries = new ArrayList<>();
        //relaxedQueries = QueryRelaxer.relax(relaxedQuery2,ontology);
        System.out.println("The relaxed queries are : " + relaxedQueries );

        TriplePath originalTriple = new TriplePath(triple);
        // Create an instance of TriplePatternRelaxer
        TriplePatternRelaxer relaxer = new TriplePatternRelaxer();

        // Apply relaxation on the original triple pattern

        System.out.println("The original triple is : " + originalTriple);
        //TriplePath relaxedTriple = relaxer.relax(originalTriple, ontology);
        //System.out.println("The relaxed triple is : " + relaxedTriple);
        //TriplePath relaxedtriple = propertypathRelaxation(originalTriple);
        //System.out.println("the relaxed triple is " + relaxedtriple);
        Map<String, List<Map<String, String>>> shaclRelationshipMap = getShaclMap();

        // Print the result
        for (Map.Entry<String, List<Map<String, String>>> entry : shaclRelationshipMap.entrySet()) {
            System.out.println("Relationship: " + entry.getKey());
            for (Map<String, String> values : entry.getValue()) {
                System.out.println("  Subject: " + values.get("subject"));
                System.out.println("  Object: " + values.get("object"));
            }
            System.out.println();
        }
    }
}




