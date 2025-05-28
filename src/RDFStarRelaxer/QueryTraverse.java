package RDFStarRelaxer;

import Similarity.QuerySimilarity;
import org.apache.jena.graph.Triple;
import org.apache.jena.query.Query;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.sparql.algebra.Algebra;
import org.apache.jena.sparql.algebra.Op;
import org.apache.jena.sparql.algebra.op.OpBGP;
import org.apache.jena.sparql.algebra.op.OpTriple;
import org.apache.jena.sparql.core.TriplePath;
import org.apache.jena.sparql.syntax.Element;

public class QueryTraverse {
    private RelaxationOperators model_operators;
    static boolean quotedsubj = false;
    static boolean quotedobj = false;




    public static void queryvisitor(Query query, Model ontology, QuerySimilarity Similarity) {
        // Get the algebra expression of the query
        Op algebraOp = Algebra.compile(query);

        // Print the algebra expression
        System.out.println("Algebra Expression:");
        System.out.println(algebraOp.toString());

        // Optionally, you can print the query's elements (triple patterns, filters, etc.)
        Element queryPattern = query.getQueryPattern();
        System.out.println("\nQuery Elements:");
        System.out.println(queryPattern.toString());
        OpBGP opBGP = (OpBGP) algebraOp;
        for(Triple bgptriple: ((OpBGP) algebraOp).getPattern().getList()) {
            triplevisitor(bgptriple, ontology, Similarity);
        }

    }

    public static TriplePath triplevisitor(Triple bgptriple, Model ontology, QuerySimilarity Similarity)
    {
        OpTriple opTriple = new OpTriple(bgptriple);
        Triple triple = opTriple.getTriple();
        TriplePath triplepath = new TriplePath(triple);
        TriplePath relaxedtriple = null;
        //TriplePath relaxedtriple = relax(triplepath, ontology, Similarity);
        return relaxedtriple;
        /*
        Node subject = triple.getSubject();
        Node predicate = triple.getPredicate();
        Node object = triple.getObject();
        if(subject.isNodeTriple()){
            System.out.println("the subject is " + subject);
            quotedsubj = true;
            TriplePath relaxedtriple = relax(new TriplePath(subject.getTriple()), ontology);
        }
        if(object.isNodeTriple()){
            System.out.println("the object is " + object);
            quotedobj = true;
            TriplePath relaxedtriple = relax(new TriplePath(object.getTriple()), ontology);

        }

         */

    }
/*
    private static Triple triplerelax(Node nodetriple) {
        Triple triple = nodetriple.getTriple();
        Node subject = triple.getSubject();
        Node predicate = triple.getPredicate();
        Node object = triple.getObject();
        Node relaxedsubj = noderelax(subject);
        Node relaxedpred = noderelax(predicate);
        Node relaxedobj = noderelax(object);
        return null;
    }


    private Map<Node, Integer> relax_predicat(Node original_node) {

        Map<Node, Integer> relaxed_node = new LinkedHashMap<Node, Integer>();

        if (original_node.isURI()) {
            relaxed_node.put(original_node, 0);
            relaxed_node.putAll(RelaxationOperators.getSuperProperty(original_node));
            Node var_node = NodeFactory.createVariable(HelperRelax.getNewPredicat());
            relaxed_node.put(var_node, SUPRESS_NODE_LEVEL);
        } else if (original_node.isLiteral()) {
            relaxed_node.put(original_node, 0);
            // predicat relaxation
            Node var_node = NodeFactory.createVariable(HelperRelax.getNewPredicat());
            relaxed_node.put(var_node, SUPRESS_NODE_LEVEL);
        }

        else if (original_node.isConcrete()) {
            relaxed_node.put(original_node, 0);
            // release relaxation
            Node var_node = NodeFactory.createVariable(HelperRelax.getNewPredicat());
            relaxed_node.put(var_node, SUPRESS_NODE_LEVEL);
        } else {
            // variables relaxation (join)
        }

        return relaxed_node;
    }
    private static Node noderelax(Node node) {

        return null;
    }

     */
}
