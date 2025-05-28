package RDFStarRelaxer;

import Similarity.QuerySimilarity;
import org.apache.jena.graph.Node;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.sparql.core.TriplePath;
import org.apache.jena.sparql.syntax.*;
import org.eclipse.rdf4j.repository.sail.SailRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.ListIterator;

public abstract class QueryRelaxer {
    protected static final Logger log = LoggerFactory.getLogger(QueryRelaxer.class);
    protected SailRepository repo;

    public abstract ArrayList<RelaxedQuery> relax(RelaxedQuery originalQuery, RelaxedQuery queryToRelax, Model ontology, QuerySimilarity querySimilarity, String reifModel);
   /* {
        ArrayList<RelaxedQuery> relaxedQueries = new ArrayList<>();
        ElementWalker.walk(queryToRelax.getQueryPattern(), new ElementVisitorBase() {
                    public void visit(ElementPathBlock el) {
                        System.out.println("the element " + el);
                        Iterator<TriplePath> tps = el.patternElts();
                        while (tps.hasNext()) {
                            TriplePath triple = tps.next();
                            System.out.println("triple pattern " + triple);
                            TriplePath relaxedTriple = TriplePatternRelaxer.relax(triple, ontology);
                            System.out.println("the relaxed triple " + relaxedTriple);
                            if (relaxedTriple != null) {
                                RelaxedQuery relaxedQuery = queryToRelax.clone();
                                System.out.println("query to relax " + relaxedQuery);
                                //RelaxedQuery relaxedQuery = queryToRelax.clone();
                                switchTriple(relaxedQuery, triple, relaxedTriple);
                                System.out.println("the relaxed query " + relaxedQuery);
                                //relaxedQuery.setNeedToEvaluate(OBFSCheckNecessity(triple, relaxedTriple, querySimilarity) && OMBSCheckNecessity(triple, relaxedTriple, relaxedQuery, MFSs, repo));
                                relaxedQuery.incrementLevel();
                                relaxedQueries.add(relaxedQuery);
                            }
                        }
                    }

                });
        return relaxedQueries;
    }

    */

    protected static void switchTriple(RelaxedQuery query, TriplePath oldTriple, TriplePath relaxedTriple) {
        ElementWalker.walk(query.getQueryPattern(), new ElementVisitorBase() {
            public void visit(ElementPathBlock el) {
                ListIterator<TriplePath> tps = el.getPattern().iterator();
                while (tps.hasNext()) {
                    TriplePath triple = tps.next();
                    if (triple.equals(oldTriple)) {
                        tps.remove();
                        //System.out.println("predicate of relaxed triple: " + relaxedTriple);
                        //System.out.println("type of relaxed triple: " + relaxedTriple.isTriple());
                        if (relaxedTriple.isTriple()) {
                            if (!isSPO(relaxedTriple)) {
                                // SPO's are deleted from query
                                tps.add(relaxedTriple);
                            }
                        }
                        query.updateOriginalTriples(oldTriple, relaxedTriple);
                        break;
                    }
                }
                //return null;
            }
        });
    }


    protected static void switchTripleQuad(RelaxedQuery query, TriplePath oldTriple, TriplePath relaxedTriple) {
        ElementWalker.walk(query.getQueryPattern(), new ElementVisitorBase() {
            public void visit(ElementPathBlock el) {
                // Process triple patterns in the main query pattern
                processTriplePatterns(el.getPattern().iterator());
                //return null;
            }

            public void visit(ElementNamedGraph namedGraph) {
                // Process triple patterns inside GRAPH clauses
                Node graphNode = namedGraph.getGraphNameNode();
                Element graphElement = namedGraph.getElement();

                if (graphElement instanceof ElementPathBlock) {
                    ElementPathBlock graphPathBlock = (ElementPathBlock) graphElement;
                    processTriplePatterns(graphPathBlock.getPattern().iterator());
                }
            }

            private void processTriplePatterns(ListIterator<TriplePath> triplePathIterator) {
                while (triplePathIterator.hasNext()) {
                    TriplePath triple = triplePathIterator.next();
                    if (triple.equals(oldTriple)) {
                        // Remove the old triple
                        triplePathIterator.remove();

                        if (!isSPO(relaxedTriple)) {
                            // Add the relaxed triple
                            triplePathIterator.add(relaxedTriple);
                        }

                        query.updateOriginalTriples(oldTriple, relaxedTriple);
                        break;
                    }
                }
            }
        });
    }


    protected static Boolean isSPO(TriplePath triple) {
        if (!triple.getSubject().isVariable()) {
            return false;
        } else if (!triple.getPredicate().isVariable()) {
            return false;
        } else if (!triple.getObject().isVariable()) {
            return false;
        }
        return true;
    }
}


