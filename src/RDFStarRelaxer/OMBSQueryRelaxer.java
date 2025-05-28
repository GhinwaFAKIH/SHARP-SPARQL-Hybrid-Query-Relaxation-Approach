package RDFStarRelaxer;

import Similarity.QuerySimilarity;
import org.apache.jena.query.QueryFactory;
import org.apache.jena.query.ResultSet;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.sparql.core.TriplePath;
import org.apache.jena.sparql.syntax.ElementPathBlock;
import org.apache.jena.sparql.syntax.ElementVisitorBase;
import org.apache.jena.sparql.syntax.ElementWalker;
import org.eclipse.rdf4j.repository.sail.SailRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.ListIterator;

import static RDFStarRelaxer.RelaxedQuery.hasResults;


public class OMBSQueryRelaxer extends QueryRelaxer {
    protected static final Logger log = LoggerFactory.getLogger(OMBSQueryRelaxer.class);
    //String endpoint = "http://localhost:3000/closure/query";
    String endpoint = "http://localhost:3030/Benchdataset/query";
    private ArrayList<ArrayList<TriplePath>> MFSs = null;

    public ArrayList<RelaxedQuery> relax(RelaxedQuery originalQuery, RelaxedQuery queryToRelax, Model ontology, QuerySimilarity querySimilarity, String reifModel) {
        ArrayList<RelaxedQuery> relaxedQueries = new ArrayList<>();
        if (MFSs == null) {
            // first exec, MFSs should contains MFS of original query
            this.MFSs = originalQuery.FindAllMFS(repo);
        }
        ElementWalker.walk(queryToRelax.getQueryPattern(), new ElementVisitorBase() {
            public void visit(ElementPathBlock el) {
                Iterator<TriplePath> tps = el.patternElts();
                while (tps.hasNext()) {
                    TriplePath triple = tps.next();
                    //System.out.println("the triple to be relaxed is " + triple +" and " + " the property is " + triple.getPath());
                    //TriplePath relaxedTriple = null;
                    TriplePath relaxedTriple = TriplePatternRelaxer.relax(triple, ontology, querySimilarity);
                    if (relaxedTriple != null) {

                        if (reifModel=="RDF-Star") {
                            RelaxedQuery relaxedQuery = queryToRelax.cloneRDFStar();
                            //System.out.println("query to be relaxed: " + relaxedQuery.cloneRDFStar());
                            switchTriple(relaxedQuery, triple, relaxedTriple);
                            //System.out.println("the relaxed query: " + relaxedQuery.cloneRDFStar());
                            relaxedQuery.setNeedToEvaluate(OBFSQueryRelaxer.OBFSCheckNecessity(triple, relaxedTriple, querySimilarity) && OMBSCheckNecessity(triple, relaxedTriple, relaxedQuery, MFSs, repo));
                            relaxedQuery.incrementLevel();
                            relaxedQueries.add(relaxedQuery);
                        }
                        else {

                            RelaxedQuery relaxedQuery = queryToRelax.clone();
                            //System.out.println("query to be relaxed: " + relaxedQuery.clone());
                            switchTriple(relaxedQuery, triple, relaxedTriple);
                            //System.out.println("the relaxed query: " + relaxedQuery.clone());
                            relaxedQuery.setNeedToEvaluate(OBFSQueryRelaxer.OBFSCheckNecessity(triple, relaxedTriple, querySimilarity) && OMBSCheckNecessity(triple, relaxedTriple, relaxedQuery, MFSs, repo));
                            relaxedQuery.incrementLevel();
                            relaxedQueries.add(relaxedQuery);
                         }

                    }
                }
                //return null;
            }
        });
        return relaxedQueries;
    }

    private Boolean OMBSCheckNecessity(TriplePath triple, TriplePath relaxedTriple, RelaxedQuery relaxedQuery, ArrayList<ArrayList<TriplePath>> MFSs, SailRepository repo) {
        Boolean mfsRelaxed = false;
        Boolean queryRepaired = true;
        for (ListIterator<ArrayList<TriplePath>> itr = MFSs.listIterator(); itr.hasNext();) {
            ArrayList<TriplePath> mfs = itr.next();
            if (mfs.contains(triple)) {
                mfsRelaxed = true;
                ArrayList<TriplePath> relaxedMfs = new ArrayList<>(mfs);
                mfs.remove(triple);
                mfs.add(relaxedTriple);
                RelaxedQuery relaxedMfsQuery = relaxedQuery.cloneRDFStar(relaxedMfs);
                ResultSet res = hasResults(QueryFactory.create(relaxedMfsQuery.serialize()), endpoint);
                //TupleQueryResult res = relaxedMfsQuery.mayHaveAResult(repo);
                if (res == null) {
                    itr.add(relaxedMfs);
                    queryRepaired = false;
                }
            }
        }
        return queryRepaired && mfsRelaxed;
    }
}