package RDFStarRelaxer;

import Similarity.QuerySimilarity;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import org.apache.jena.query.*;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.sparql.core.TriplePath;
import org.apache.jena.sparql.expr.*;
import org.apache.jena.sparql.syntax.ElementFilter;
import org.apache.jena.sparql.syntax.ElementPathBlock;
import org.apache.jena.sparql.syntax.ElementVisitorBase;
import org.apache.jena.sparql.syntax.ElementWalker;
import org.eclipse.rdf4j.repository.sail.SailRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileReader;
import java.io.IOException;
import java.util.*;

import static RDFStarRelaxer.RelaxedQuery.hasResults;


public class SHARPQueryRelaxer extends QueryRelaxer {
    protected static final Logger log = LoggerFactory.getLogger(SHARPQueryRelaxer.class);
    //String endpoint = "http://localhost:3000/closure/query";
    String endpoint = "http://localhost:3030/Benchdataset/query";
    String StatPath = "data/lubm_statistics-2.json";
    private ArrayList<ArrayList<TriplePath>> MFSs = null;

    public ArrayList<RelaxedQuery> relax(RelaxedQuery originalQuery, RelaxedQuery queryToRelax, Model ontology, QuerySimilarity querySimilarity, String reifModel) {
        //System.out.println("the query pattern is " + queryToRelax.getQueryPattern());
        ArrayList<RelaxedQuery> relaxedQueries = new ArrayList<>();
        // Load the statistics from the JSON file
        JsonObject propertyStatsMap = loadPropertyStats("data/lubm_statistics-2.json");

        if (MFSs == null) {
            // first exec, MFSs should contains MFS of original query
            this.MFSs = originalQuery.FindAllMFS(repo);
        }
        ElementWalker.walk(queryToRelax.getQueryPattern(), new ElementVisitorBase() {
            public void visit(ElementPathBlock el) {
                //System.out.println("the path block element is " + el);
                Iterator<TriplePath> tps = el.patternElts();
                while (tps.hasNext()) {
                    TriplePath triple = tps.next();
                    Set<TriplePath> relaxedTriples = null;
                    try {
                        relaxedTriples = TriplePatternRelaxer.relax2(triple, ontology, querySimilarity);
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                    for (TriplePath relaxedTriple : relaxedTriples) {
                        if (relaxedTriple != null) {
                            if (reifModel == "RDF-Star") {
                                RelaxedQuery relaxedQuery = queryToRelax.cloneRDFStar();
                                switchTriple(relaxedQuery, triple, relaxedTriple);
                                relaxedQuery.setNeedToEvaluate(OBFSQueryRelaxer.OBFSCheckNecessity(triple, relaxedTriple, querySimilarity) && OMBSCheckNecessity(triple, relaxedTriple, relaxedQuery, MFSs, repo));
                                relaxedQuery.incrementLevel();
                                relaxedQueries.add(relaxedQuery);
                            } else {
                                RelaxedQuery relaxedQuery = queryToRelax.clone();
                                switchTriple(relaxedQuery, triple, relaxedTriple);
                                relaxedQuery.setNeedToEvaluate(OBFSQueryRelaxer.OBFSCheckNecessity(triple, relaxedTriple, querySimilarity) && OMBSCheckNecessity(triple, relaxedTriple, relaxedQuery, MFSs, repo));
                                relaxedQuery.incrementLevel();
                                relaxedQueries.add(relaxedQuery);
                            }
                        }
                    }
                }
                //return null;
            }
            public void visit(ElementFilter el) {
                Expr expr = el.getExpr();
                List<ElementFilter> newFilters = new ArrayList<>();
                //ElementFilter newFilter = null;
                /*
                String property_uri = findPropertyForFilter(el, queryToRelax);
                System.out.println("the property is " + property_uri);
                String minQueryStr = "SELECT (MIN(?year) AS ?minYear) WHERE { ?s " + property_uri + " ?year . FILTER (datatype(?year) = xsd:integer) }";
                int MinValue_2 = 0;
                try (QueryExecution qe = QueryExecutionFactory.sparqlService(endpoint, minQueryStr)) {
                    ResultSet results = qe.execSelect();
                    if (results.hasNext()) {
                        QuerySolution soln = results.nextSolution();
                        MinValue_2 = soln.getLiteral("minYear").getInt();
                    }
                }
                System.out.println("the minimum value is " + MinValue_2);

                 */

                //int MinValue = 2007;
                //int MaxValue = 2025;
                // Handle BETWEEN expressions first

                if (expr instanceof E_LogicalAnd) {
                    ExprFunction2 andFunc = (ExprFunction2) expr;
                    Expr leftCondition = andFunc.getArg1();
                    Expr rightCondition = andFunc.getArg2();

                    if (leftCondition instanceof ExprFunction2 && rightCondition instanceof ExprFunction2) {
                        ExprFunction2 leftFunc = (ExprFunction2) leftCondition;
                        ExprFunction2 rightFunc = (ExprFunction2) rightCondition;

                        if ((leftFunc instanceof E_GreaterThanOrEqual || leftFunc instanceof E_GreaterThan) &&
                                (rightFunc instanceof E_LessThanOrEqual || rightFunc instanceof E_LessThan)) {

                            Expr leftArg = leftFunc.getArg1();
                            Expr leftVal = leftFunc.getArg2();
                            Expr rightVal = rightFunc.getArg2();
                            ExprVar variable_name = leftArg.getExprVar();
                            String predicate_uri = findPropertyForFilter(variable_name, queryToRelax);
                            /*
                            String minQueryStr = "PREFIX xsd: <http://www.w3.org/2001/XMLSchema#>" +
                                    "SELECT (MIN(?year) AS ?minYear) WHERE { ?s " + "<" + predicate_uri + ">" + " ?year . FILTER (datatype(?year) = xsd:integer) }";
                            String maxQueryStr = "PREFIX xsd: <http://www.w3.org/2001/XMLSchema#>" +
                                    "SELECT (MAX(?year) AS ?maxYear) WHERE { ?s " + "<" + predicate_uri + ">" + " ?year . FILTER (datatype(?year) = xsd:integer) }";
                            MinValue = 0;
                            MaxValue = 0;
                            try (QueryExecution qe = QueryExecutionFactory.sparqlService(endpoint, minQueryStr)) {
                                ResultSet results = qe.execSelect();
                                if (results.hasNext()) {
                                    QuerySolution soln = results.nextSolution();
                                    MinValue = soln.getLiteral("minYear").getInt();
                                }
                            }
                            System.out.println("the minimum value is " + MinValue);
                            try (QueryExecution qe = QueryExecutionFactory.sparqlService(endpoint, maxQueryStr)) {
                                ResultSet results = qe.execSelect();
                                if (results.hasNext()) {
                                    QuerySolution soln = results.nextSolution();
                                    MaxValue = soln.getLiteral("maxYear").getInt();
                                }
                            }
                            System.out.println("the maximum value is " + MaxValue);

                             */

                            int minValue = 0;
                            int maxValue = 0;
                            JsonArray propertyArray = propertyStatsMap.getAsJsonArray("Property");
                            //System.out.println("the json array is " + propertyArray);
                            for (JsonElement propElement : propertyArray) {
                                JsonObject propObject = propElement.getAsJsonObject();
                                if (propObject.get("prop").getAsString().equals(predicate_uri)) {
                                    if (propObject.has("min") && propObject.has("max")) {
                                        minValue = propObject.get("min").getAsInt();
                                        maxValue = propObject.get("max").getAsInt();
                                    }
                                    break;
                                }
                            }

                            //System.out.println("The minimum value is " + minValue);
                            //System.out.println("The maximum value is " + maxValue);

                            if (leftVal.isConstant() && rightVal.isConstant()) {
                                NodeValue leftValue = leftVal.getConstant();
                                NodeValue rightValue = rightVal.getConstant();

                                if (leftValue.isNumber() && rightValue.isNumber()) {
                                    int currentMinValue = leftValue.getInteger().intValue();
                                    int currentMaxValue = rightValue.getInteger().intValue();

                                    // Iteratively expand the filter until reaching the dataset boundaries
                                    while (currentMinValue > minValue || currentMaxValue < maxValue) {
                                        if (currentMinValue > minValue) {
                                            currentMinValue--;
                                        }
                                        if (currentMaxValue < maxValue) {
                                            currentMaxValue++;
                                        }

                                        NodeValue newLeftValue = NodeValue.makeInteger(currentMinValue);
                                        NodeValue newRightValue = NodeValue.makeInteger(currentMaxValue);
                                        Expr newRightExpr = (rightFunc instanceof E_LessThan) ?
                                                new E_LessThan(leftArg, newRightValue) :
                                                new E_LessThanOrEqual(leftArg, newRightValue);
                                        Expr newLeftExpr = (leftFunc instanceof E_GreaterThan) ?
                                                new E_GreaterThan(leftArg, newLeftValue) :
                                                new E_GreaterThanOrEqual(leftArg, newLeftValue);
                                        //System.out.println("the new BETWEEN expression is " + newRightExpr + " AND " + newRightExpr);

                                        // Create new filter clause
                                        Expr newAndExpr = new E_LogicalAnd(newLeftExpr, newRightExpr);
                                        //newFilter = new ElementFilter(newAndExpr);
                                        newFilters.add(new ElementFilter(newAndExpr));
                                        //System.out.println("the new BETWEEN filter is " + newFilter);

                                    }
                                }
                            }
                        }
                    }
                }
                // Handle other binary expressions
                else if (expr instanceof ExprFunction2) {
                    ExprFunction2 func = (ExprFunction2) expr;
                    Expr left = func.getArg1();
                    Expr right = func.getArg2();
                    NodeValue rightValue = right.getFunction().getArgs().get(1).getConstant();
                    int currentValue = rightValue.getInteger().intValue();
                    ExprVar variable_name = left.getExprVar();
                    String predicate_uri = findPropertyForFilter(variable_name, queryToRelax);

                    int minValue = 0;
                    int maxValue = 0;
                    JsonArray propertyArray = propertyStatsMap.getAsJsonArray("Property");
                    //System.out.println("the json array is " + propertyArray);
                    for (JsonElement propElement : propertyArray) {
                        JsonObject propObject = propElement.getAsJsonObject();
                        if (propObject.get("prop").getAsString().equals(predicate_uri)) {
                            if (propObject.has("min") && propObject.has("max")) {
                                minValue = propObject.get("min").getAsInt();
                                maxValue = propObject.get("max").getAsInt();
                            }
                            break;
                        }
                    }

                    // Iteratively expand the filter until reaching the dataset boundaries
                    while (currentValue > minValue || currentValue < maxValue) {
                        if ((func instanceof E_GreaterThan || func instanceof E_GreaterThanOrEqual) && currentValue > minValue) {
                            currentValue--;
                        } else if ((func instanceof E_LessThan || func instanceof E_LessThanOrEqual) && currentValue < maxValue) {
                            currentValue++;
                        }

                        //NodeValue leftValue = left.getFunction().getArgs().get(1).getConstant();

                        if (rightValue.isNumber()) {
                            System.out.println("it is a constant number");
                            NodeValue newValue = NodeValue.makeInteger(currentValue);
                            Expr newExpr = null;

                            if (func instanceof E_GreaterThan) {
                                newExpr = new E_GreaterThan(left, newValue);
                            } else if (func instanceof E_LessThan) {
                                newExpr = new E_LessThan(left, newValue);
                            } else if (func instanceof E_GreaterThanOrEqual) {
                                newExpr = new E_GreaterThanOrEqual(left, newValue);
                            } else if (func instanceof E_LessThanOrEqual) {
                                newExpr = new E_LessThanOrEqual(left, newValue);
                            }
                            //System.out.println("the new expression is " + newExpr);
                            // Create new filter clause
                            newFilters.add(new ElementFilter(newExpr));
                            //newFilter = new ElementFilter(newExpr);

                        }
                    }
                }
                // If neither condition is met
                else {
                    System.out.println("Expression type not handled");
                }
                // Generate relaxed queries for each filter in the list
                while (!newFilters.isEmpty()) {
                    ElementFilter newFilter = newFilters.remove(0);
                    if (newFilter != null) {
                        RelaxedQuery relaxedQuery = queryToRelax.cloneRDFStar();
                        relaxedQuery.replaceFilter(el, newFilter);
                        //here I have to compute the similarity differently
                        relaxedQuery.setSimilarity(0.9);  // Set the similarity value
                        relaxedQuery.incrementLevel();
                        relaxedQueries.add(relaxedQuery);
                    }
                }
            }

        });
        System.out.println("the relaxed queries are: " + relaxedQueries);
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
                // why evaluating this here ?
                String queryString = relaxedMfsQuery.serialize() + " LIMIT 1";
                ResultSet res = hasResults(QueryFactory.create(queryString), endpoint);
                //TupleQueryResult res = relaxedMfsQuery.mayHaveAResult(repo);
                if (res == null) {
                    itr.add(relaxedMfs);
                    queryRepaired = false;
                }
            }
        }
        return queryRepaired && mfsRelaxed;
    }

    private JsonObject loadPropertyStats(String StatPath) {
        try (FileReader reader = new FileReader(StatPath)) {
            Gson gson = new Gson();
            return gson.fromJson(reader, JsonObject.class);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return new JsonObject();
    }

    // Implement logic to find the property associated with the filter based on your data structure
    public static String findPropertyForFilter(ExprVar filter_var, Query queryToRelax) {
        final String[] property_uri = {null}; // Array to capture the property URI found
        ElementWalker.walk(queryToRelax.getQueryPattern(), new ElementVisitorBase() {
            @Override
            public void visit(ElementPathBlock el) {
                el.getPattern().forEach(triple -> {
                    if (triple.getObject().toString().equals(filter_var.toString())) {
                        property_uri[0] = triple.getPredicate().getURI();
                        System.out.println("Found predicate URI: " + property_uri[0]);
                    }
                });
            }
        });

        return property_uri[0];
    }
}