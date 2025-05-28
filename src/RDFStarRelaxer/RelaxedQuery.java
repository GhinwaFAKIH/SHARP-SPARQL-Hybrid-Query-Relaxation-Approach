package RDFStarRelaxer;

import org.apache.jena.query.*;
import org.apache.jena.sparql.core.TriplePath;
import org.apache.jena.sparql.syntax.*;
import org.eclipse.rdf4j.query.QueryLanguage;
import org.eclipse.rdf4j.query.TupleQuery;
import org.eclipse.rdf4j.query.TupleQueryResult;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.eclipse.rdf4j.repository.RepositoryException;
import org.eclipse.rdf4j.repository.sail.SailRepository;
import org.eclipse.rdf4j.sail.SailException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

public class RelaxedQuery extends Query implements Comparable<RelaxedQuery>, Cloneable {
    protected static final Logger log = LoggerFactory.getLogger(RelaxedQuery.class);

    private double similarity;
    // to link a relaxedTriple to its OriginalTriple
    private HashMap<TriplePath, TriplePath> originalTriples;
    private int level;
    private boolean needToEvaluate;
    // Minimal Failling Subqueries
    private ArrayList<ArrayList<TriplePath>> MFSs;

    private String endpoint;

    private ResultSet res;
    //private TupleQueryResult res;

    public RelaxedQuery() {
        super();
        this.level = 0;
        this.similarity = 1.0;
        this.originalTriples = new HashMap<>();
        this.needToEvaluate = true;
        this.MFSs = new ArrayList<>();
        this.endpoint = "http://localhost:3030/Benchdataset/query";
    }

    public double getSimilarity() {
        return this.similarity;
    }

    public int getLevel() {
        return level;
    }

    public void setSimilarity(double similarity) {
        this.similarity = similarity;
    }

    public void incrementLevel() {
        this.level += 1;
    }

    public boolean needToEvaluate() {
        return this.needToEvaluate;
    }

    public void setNeedToEvaluate(Boolean needToEvaluate) {
        this.needToEvaluate = needToEvaluate;
    }

    public HashMap<TriplePath, TriplePath> getOriginalTriples() {
        return this.originalTriples;
    }

    public ArrayList<ArrayList<TriplePath>> getMFSs() { return this.MFSs; }

    // to call after query is parsed
    // to call after query is parsed
    public void initOriginalTriples() {
        originalTriples.clear();
        ElementWalker.walk(this.getQueryPattern(), new ElementVisitorBase() {
            public void visit(ElementPathBlock el) {
                Iterator<TriplePath> tps = el.patternElts();
                TriplePath triple;
                while (tps.hasNext()) {
                    triple = tps.next();
                    originalTriples.put(triple, triple);
                }
            }
        });
    }

    public void updateOriginalTriples(TriplePath oldTriple, TriplePath relaxedTriple) {
        TriplePath originalTriple = this.originalTriples.get(oldTriple);
        this.originalTriples.remove(oldTriple);
        this.originalTriples.put(relaxedTriple, originalTriple);
    }


    @Override
    public int compareTo(RelaxedQuery o) {
        if (this.similarity == o.similarity) {
            return this.serialize().compareTo(o.serialize());
        } else {
            return (this.similarity < o.similarity ? -1 : +1);
        }
    }

    @Override
    public RelaxedQuery clone()  {
        RelaxedQuery clone = new RelaxedQuery();
        QueryFactory.parse(clone, this.serialize(), null, null);
        clone.similarity = this.similarity;
        clone.level = this.getLevel();
        clone.originalTriples = (HashMap<TriplePath, TriplePath>) this.originalTriples.clone();
        return clone;
    }
    public RelaxedQuery cloneRDFStar()  {
        RelaxedQuery clone = new RelaxedQuery();
        //System.out.println(this.serialize());
        Query relaxedquery = QueryFactory.create(this.serialize());
        Element pattern  = relaxedquery.getQueryPattern();
        if (pattern instanceof ElementGroup) {
            ElementGroup elementGroup = (ElementGroup) pattern;
            //List<Element> elements = elementGroup.getElements();
            clone.setRDFStarQueryPattern(elementGroup);
            /*
            for (Element element : elements) {
                if (element instanceof ElementPathBlock) {
                    ElementPathBlock pathBlock = (ElementPathBlock) element;
                    clone.setRDFStarQueryPattern(pathBlock);
                }
                else if (element instanceof ElementFilter) {
                    ElementFilter filter = (ElementFilter) element;
                    // Add filter to the clone
                    ((ElementGroup) clone.getQueryPattern()).addElementFilter(filter);
                }
            }

             */
        }
        //QueryFactory.parse(clone, this.serialize(), null, null);
        clone.similarity = this.similarity;
        clone.level = this.getLevel();
        clone.originalTriples = (HashMap<TriplePath, TriplePath>) this.originalTriples.clone();
        return clone;
    }
    /*
    // Method to set the query pattern directly using RDF-star syntax
    public void setRDFStarQueryPattern(ElementPathBlock pattern) {
        //ElementPathBlock pattern = new ElementPathBlock();
        // Assuming your RDF-star query is in the form "<< ?s ?p ?o >> ?p2 ?o2"
        //pattern.addTriplePath(TriplePath.create("<<?s ?p ?o>> ?p2 ?o2"));
        //System.out.println(this.serialize());
        this.setQueryPattern(pattern);
        this.setQueryResultStar(true);
        this.setQuerySelectType();
    }

     */
    public void setRDFStarQueryPattern(Element pattern) {
        //ElementPathBlock pattern = new ElementPathBlock();
        // Assuming your RDF-star query is in the form "<< ?s ?p ?o >> ?p2 ?o2"
        //pattern.addTriplePath(TriplePath.create("<<?s ?p ?o>> ?p2 ?o2"));
        //System.out.println(this.serialize());
        if (pattern instanceof ElementPathBlock || pattern instanceof ElementGroup) {
            this.setQueryPattern(pattern);
            this.setQueryResultStar(true);
            this.setQuerySelectType();
        }
    }



    public RelaxedQuery clone(ArrayList<TriplePath> triples)  {
        RelaxedQuery clone = this.clone();
        ElementWalker.walk(clone.getQueryPattern(), new ElementVisitorBase() {
            public void visit(ElementPathBlock el) {
                Iterator<TriplePath> tps = el.patternElts();
                while (tps.hasNext()) {
                    TriplePath triple = tps.next();
                    if (!triples.contains(triple)) {
                        tps.remove();
                        break;
                    }
                }
            }
        });
        return clone;
    }

    public RelaxedQuery cloneRDFStar (ArrayList<TriplePath> triples)  {
        RelaxedQuery clone = this.cloneRDFStar();
        ElementWalker.walk(clone.getQueryPattern(), new ElementVisitorBase() {
            public void visit(ElementPathBlock el) {
                Iterator<TriplePath> tps = el.patternElts();
                while (tps.hasNext()) {
                    TriplePath triple = tps.next();
                    if (!triples.contains(triple)) {
                        tps.remove();
                        break;
                    }
                }
            }
        });
        return clone;
    }


    @Override
    public String toString() {
        return super.toString() +
                "_________________________________\nSimilarity:" + this.similarity +
                " Level:" + this.getLevel() + " Evaluation: " + this.needToEvaluate;
    }



    protected ArrayList<ArrayList<TriplePath>> pxss(ArrayList<TriplePath> MFS) {
        ArrayList<ArrayList<TriplePath>> pxss = new ArrayList<>();
        ArrayList<TriplePath> queryTriples = this.getTriples();
        if (queryTriples.size() > 1) {
            for (TriplePath MFSTriple : MFS) {
                ArrayList<TriplePath> pxs = new ArrayList<>(queryTriples);
                pxs.remove(MFSTriple);
                pxss.add(pxs);
            }
        }
        return pxss;
    }


    public void removeTriple(TriplePath triple) {
        ElementWalker.walk(this.getQueryPattern(), new ElementVisitorBase() {
            public void visit(ElementPathBlock el) {
                Iterator<TriplePath> tps = el.patternElts();
                while (tps.hasNext()) {
                    TriplePath triple2 = tps.next();
                    if (triple.equals(triple2)) {
                        tps.remove();
                        break;
                    }
                }
            }
        });
    }

    public void addTriple(TriplePath triple) {
        ElementWalker.walk(this.getQueryPattern(), new ElementVisitorBase() {
            public void visit(ElementPathBlock el) {
                el.addTriple(triple);
            }
        });
    }
    public void addTriple(ArrayList<TriplePath> triples) {
        ElementWalker.walk(this.getQueryPattern(), new ElementVisitorBase() {
            public void visit(ElementPathBlock el) {
                for (TriplePath triple : triples) {
                    el.addTriple(triple);
                }
            }
        });
    }
    /*
    private ArrayList<TriplePath> findAnMFS(SailRepository repo) {
        RelaxedQuery query = this.cloneRDFStar();
        ArrayList<TriplePath> MFS = new ArrayList<>();
        //System.out.println(this.getQueryPattern());
        ElementWalker.walk(this.getQueryPattern(), new ElementVisitorBase() {
            public void visit(ElementPathBlock el) {
                    //System.out.println("el " + el);
                    Iterator<TriplePath> tps = el.patternElts();
                    while (tps.hasNext()) {
                        TriplePath triple = tps.next();
                        //System.out.println("triple " + tps.next());
                        query.removeTriple(triple);
                        RelaxedQuery evalQuery = query.cloneRDFStar();
                        evalQuery.addTriple(MFS);
                        System.out.println("query to execute " + evalQuery);
                        ResultSet res = hasResults(evalQuery, endpoint);
                        //TupleQueryResult res = evalQuery.mayHaveAResult(repo);
                        if (res != null) {
                            MFS.add(triple);
                        }
                    }
                }
        });
        return MFS;
    }


     */
    private ArrayList<TriplePath> findAnMFS(SailRepository repo) {
        RelaxedQuery query = this.cloneRDFStar();
        //System.out.println("the cloned query is " + query);
        ArrayList<TriplePath> MFS = new ArrayList<>();

        Element queryPattern = this.getQueryPattern();
        if (queryPattern == null) {
            // Handle null query pattern
            return MFS;
        }

        ElementWalker.walk(queryPattern, new ElementVisitorBase() {
            public void visit(ElementPathBlock el) {
                if (el != null) { // Check for null element
                    Iterator<TriplePath> tps = el.patternElts();
                    while (tps.hasNext()) {
                        TriplePath triple = tps.next();
                        query.removeTriple(triple);
                        RelaxedQuery evalQuery = query.cloneRDFStar();
                        evalQuery.addTriple(MFS);
                        //System.out.println("query to execute" + evalQuery);
                        //System.out.println("query to execute" + evalQuery.serialize());
                        String QuerySt = evalQuery.serialize();
                        //System.out.println("the query is " + QuerySt );
                        if (!QuerySt.isEmpty()) {
                            Query evalQuery2 = QueryFactory.create(QuerySt);
                            ResultSet res = hasResults(evalQuery2, endpoint);
                            //System.out.println("the query is executed");
                            if (res != null) {
                                MFS.add(triple);
                            }
                        }
                    }
                }
            }
        });
        return MFS;
    }

    public ArrayList<ArrayList<TriplePath>> FindAllMFS(SailRepository repo) {
        //System.out.println("the query cloning is " + this.cloneRDFStar());
        ArrayList<TriplePath> MFS = this.findAnMFS(repo);
        ArrayList<ArrayList<TriplePath>> pxss = this.pxss(MFS);
        ArrayList<ArrayList<TriplePath>> MFSs = new ArrayList<>();
        MFSs.add(MFS);
        System.out.println("the MFSs are " + MFSs);
        ArrayList<ArrayList<TriplePath>> XSSs = new ArrayList<>();
        while (!pxss.isEmpty()) {
            ArrayList<TriplePath> pxs = pxss.get(0);
            //System.out.println("the pxs " + pxs);
            RelaxedQuery query = this.cloneRDFStar(pxs);
            ResultSet res = hasResults(QueryFactory.create(query.serialize()), endpoint);
            //TupleQueryResult res = query.mayHaveAResult(repo);
            if (res != null) {
                if (!XSSs.contains(pxs)) { XSSs.add(pxs); }
                pxss.remove(pxs);
            } else {
                ArrayList<TriplePath> MFS2 = query.findAnMFS(repo);
                if (!MFSs.contains(MFS2)) { MFSs.add(MFS2); }
                for (ListIterator<ArrayList<TriplePath>> itr = pxss.listIterator(); itr.hasNext();) {
                    ArrayList<TriplePath> px = itr.next();
                    itr.remove();
                    if (px.containsAll(MFS2)) {
                        // MFS included in px
                        RelaxedQuery query2 = this.clone(px);
                        ArrayList<ArrayList<TriplePath>> pxssToAdd = new ArrayList<>();
                        for (ArrayList<TriplePath> px2 : query2.pxss(MFS2)) {
                            Boolean px2Included = false;
                            for (ArrayList<TriplePath> px1 : pxss) {
                                if (px1.containsAll(px2)) {
                                    px2Included = true;
                                    break;
                                }
                            }
                            if (!px2Included) {
                                pxssToAdd.add(px2);
                            }
                        }
                        pxssToAdd.forEach(itr::add);
                    }
                }
            }
        }
        log.info("MFS" + MFSs.toString());
        return MFSs;
    }

    public TupleQueryResult mayHaveAResult(SailRepository repo) {
        try {
            // Open a connection to the repository
            RepositoryConnection conn = repo.getConnection();

            TupleQuery query = repo.getConnection().prepareTupleQuery(QueryLanguage.SPARQL, this.serialize());

            // Evaluate the query
            TupleQueryResult res = query.evaluate();

            // Close the connection
            conn.close();

            // Return the query result
            return res;
        } catch (RepositoryException | SailException ex) {
            // Handle repository or sail exceptions
            ex.printStackTrace();
            return null;
        }
    }

    public static ResultSet hasResults (Query query, String endpoint) {
        try (QueryExecution queryExecution = QueryExecutionFactory.sparqlService(endpoint, query)) {
            ResultSet res = queryExecution.execSelect();
            //System.out.println(" the result is " + res);
            return res;
        } catch (Exception e) {
            // Handle any exceptions that occur during query execution
            e.printStackTrace();
            return null;
        }
    }

    public void replaceFilter(ElementFilter oldFilter, ElementFilter newFilter) {
        ElementGroup pattern = (ElementGroup) this.getQueryPattern();
        List<Element> elements = pattern.getElements();
        for (int i = 0; i < elements.size(); i++) {
            Element element = elements.get(i);
            if (element.equals(oldFilter)) {
                // Replace the filter with the new one
                elements.set(i, newFilter);
                break;  // Assuming only one filter needs replacement
            }
        }
        ElementGroup new_pattern = new ElementGroup();
        new_pattern.getElements().addAll(elements);
        this.setRDFStarQueryPattern(new_pattern);
    }


    public ArrayList<TriplePath> getTriples() {
        ArrayList<TriplePath> triples = new ArrayList<>();
        ElementWalker.walk(this.getQueryPattern(), new ElementVisitorBase() {
            public void visit(ElementPathBlock el) {
                Iterator<TriplePath> tps = el.patternElts();
                while (tps.hasNext()) {
                    TriplePath triple = tps.next();
                    triples.add(triple);
                }
            }
        });
        return triples;
    }
}