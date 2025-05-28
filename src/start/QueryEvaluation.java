package start;

import RDFStarRelaxer.*;
import org.aksw.simba.start.QueryProvider;
import org.apache.commons.io.FileUtils;
import org.apache.jena.query.*;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.riot.RDFDataMgr;
import org.apache.jena.sparql.core.ResultBinding;
import org.apache.jena.sparql.syntax.Element;
import org.apache.jena.sparql.syntax.ElementGroup;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

        public class QueryEvaluation {
            protected static final Logger log = LoggerFactory.getLogger(start.QueryEvaluation.class);
            protected static Model ontology = RDFDataMgr.loadModel("data/univ-bench2.owl");
            protected static  final double minSimilarity = 0;
            private HashMap<String, String> results = new HashMap<>();
            String endpoint = "";
            private long startQueryExecTime = 0;
            private String strategy;

            private QueryRelaxer queryRelaxer;
            private Boolean relax;

            private Boolean error;
            static {
                try {
                    ClassLoader.getSystemClassLoader().loadClass("org.slf4j.LoggerFactory"). getMethod("getLogger", ClassLoader.getSystemClassLoader().loadClass("java.lang.String")).
                            invoke(null,"ROOT");
                } catch (Throwable e) {
                    e.printStackTrace();
                }
            }
            QueryProvider qp;
            public QueryEvaluation(String strategy,QueryRelaxer queryRelaxer, Boolean relax) throws Exception {
                qp = new QueryProvider("queries");
                this.results.put("Query", null);
                this.results.put("FirstResultTime", null);
                this.results.put("totalExecTime", null);
                this.results.put("nbGeneratedRelaxedQueries", "0");
                this.results.put("nbEvaluatedRelaxedQueries", "0");
                this.results.put("nbRes", "0");
                this.results.put("ResultSimilarity", "0.0");
                this.results.put("validResult", "false");
                this.results.put("hasResult", "false");

                this.strategy = strategy;
                this.queryRelaxer = queryRelaxer;
                this.relax = relax;
                this.error = false;
                if (strategy.equals("PAPER")) {
                    this.ontology = RDFDataMgr.loadModel("data/univ-bench2.owl");
                }
            }

            public static void main(String[] args) throws Exception
            {
                //String strategy = args[0];
                String strategy = "SHARP";
                //String strategy = "OMBS";
                //String reifModel = "Named Graphs";
                String reifModel = "RDF-Star";
                String computationMethod = "multiply";
                boolean relax = true;
                if (args.length > 1) {
                    args[1] = "Relax";
                    if (args[1].equals("noRelax")) {
                        relax = false;
                    }
                }
/*
        if (args[1].equals("noRelax")) { relax = false;}

 */
                strategy = strategy.toUpperCase();
                QueryRelaxer queryRelaxer = null;
                if (strategy.equals("OBFS")) {
                    queryRelaxer = new OBFSQueryRelaxer();
                } else if (strategy.equals("OMBS")) {
                    queryRelaxer = new OMBSQueryRelaxer();
                } else if (strategy.equals("SHARP")) {
                    queryRelaxer = new SHARPQueryRelaxer();
                }

                String endpoint ="http://localhost:3030/Benchdataset/query";
                //String queries = args[2];
                String queries = "Q6";
                // String queries = "S1 S2 S3 S4 S5 S6 S7 S8 S9 S10 S11 S12 S13 S14 C1 C2 C3 C4 C5 C6 C7 C8 C9 C10 L1 L2 L3 L4 L5 L6 L7 L8 CH1 CH2 CH3 CH4 CH5 CH6 CH7 CH8";

                multyEvaluate(queries, 1, endpoint, strategy, queryRelaxer, relax, computationMethod, reifModel);
                System.exit(0);
            }




            static void multyEvaluate(String queries, int num, String endpoint, String strategy, QueryRelaxer queryRelaxer, Boolean relax, String computationMethod, String reifModel) throws Exception {
                start.QueryEvaluation qeval = new start.QueryEvaluation(strategy, queryRelaxer, relax);
                for (int i = 0; i < num; ++i) {
                    qeval.evaluate(queries, endpoint, computationMethod, reifModel);
                }
            }


    private void evaluate(String queries, String endpoint, String computationMethod, String reifModel) throws Exception {
        List<String> qnames = Arrays.asList(queries.split(" "));
        String queryFileName;
        SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd_HH:mm:ss");
        Date date = new Date();
        String execFileName = "results/" + formatter.format(date) + this.strategy + "relax_" + this.relax +".csv";
        BufferedWriter ExecutionWriter = new BufferedWriter(new FileWriter(execFileName));
        // CSV header
        ExecutionWriter.write(String.join(";", this.results.keySet()) + "\n");
        ExecutionWriter.flush();
        for (String curQueryName : qnames)
        {
            this.results.put("Query", curQueryName);
            execute(curQueryName, endpoint, computationMethod, reifModel);
            long totalExecTime = System.currentTimeMillis() - this.startQueryExecTime;
            log.info(curQueryName + ": Query execution time (msec): "+ totalExecTime);
            this.results.put("totalExecTime", Long.toString(totalExecTime));
            ExecutionWriter.write(String.join(";", this.results.values()) + "\n");
            ExecutionWriter.flush();
        }
        ExecutionWriter.close();
}
    public void execute(String curQueryName, String endpoint, String computationMethod, String reifModel) throws Exception {
        // We only search for the first result
        this.startQueryExecTime = System.currentTimeMillis();
        String curQuery = qp.getQuery(curQueryName);
        System.out.println("the initial query is " + curQuery);
        ResultSet res = null;
        //TupleQueryResult res = null;
        try {
            if (this.results.get("hasResult").equals("false") || this.error) {
                //TupleQuery query = repo.getConnection().prepareTupleQuery(QueryLanguage.SPARQL, curQuery);
                Query query = QueryFactory.create(curQuery);
                QueryExecution qexec = QueryExecutionFactory.sparqlService(endpoint, query);
                res = qexec.execSelect();
                //res = query.evaluate();
                // Here, we resolved all license conflicts
                int nbGeneratedRelaxedQueries = 0;
                int nbEvaluatedRelaxedQueries = 0;
                double ResultSimilarity = 0.0;
                RelaxedQuery relaxedQuery = new RelaxedQuery();
                if (reifModel == "RDF-Star") {
                    Element pattern = query.getQueryPattern();
                    if (pattern instanceof ElementGroup) {
                        ElementGroup elementGroup = (ElementGroup) pattern;
                        List<Element> elements = elementGroup.getElements();
                        relaxedQuery.setRDFStarQueryPattern(elementGroup);
                    }
                    //QueryFactory.parse(relaxedQuery, curQuery, null, null);
                    relaxedQuery.initOriginalTriples();
                }
                else if (reifModel == "Named Graphs") {
                    QueryFactory.parse(relaxedQuery, curQuery, null, null);
                    relaxedQuery.initOriginalTriples();
                }
                // Iterate over the ResultSet and print each result
                while (res.hasNext()) {
                    QuerySolution solution = res.next();
                    System.out.println("Result: " + solution );
                }

                //res = relaxedQuery.mayHaveAResult(repo);
                if (relax && (res == null || !res.hasNext())) {
                    //We have to relax
                    QueryRelaxationLattice relaxationLattice = new QueryRelaxationLattice(relaxedQuery, ontology, minSimilarity, this.queryRelaxer, computationMethod, reifModel);
                    // Print the top k results
                    relaxationLattice.writeTopKResultsToFile(30, "SHARP_results/test-Q6");
                    /* 
                    for (int k = 20; k <= 50; k++) {
                        // Construct the file name dynamically for each value of k
                        String fileName = "SHARP_results/Q3-" + k;
                        // Call the method with the current value of k and the corresponding file name
                        relaxationLattice.writeTopKResultsToFile(k, fileName);
                    }
                    */
                    System.out.println("results printed");
                    //System.out.println("relaxation lattice " + relaxationLattice);
                    log.info("--------Evaluated Relaxed Queries:-----------\n");
                    System.out.println("finished");
                    nbGeneratedRelaxedQueries += relaxationLattice.sizeOfRemaining();
                }


                // we found a query that return at least 1 result.
                this.results.put("nbGeneratedRelaxedQueries", Integer.toString(Integer.parseInt(this.results.get("nbGeneratedRelaxedQueries")) + nbGeneratedRelaxedQueries));
                this.results.put("nbEvaluatedRelaxedQueries", Integer.toString(Integer.parseInt(this.results.get("nbEvaluatedRelaxedQueries")) + nbEvaluatedRelaxedQueries));
                this.results.put("ResultSimilarity", Double.toString(Math.max(Double.parseDouble(this.results.get("ResultSimilarity")), ResultSimilarity)));
                // Now we can execute the query with FedX
                // TODO Uncomment next to execute query
                log.info("results of the query are:");
                while (res != null && res.hasNext()) {
                    ResultBinding row = (ResultBinding) res.next();
                    this.results.put("hasResult", "true");
                    log.info(row.toString());
                    this.results.put("nbRes", Integer.toString(Integer.parseInt(this.results.get("nbRes")) + 1));
                    // only one result
                }
                this.results.put("validResult", "true");
                long FirstResultTime = System.currentTimeMillis() - this.startQueryExecTime;
                this.results.put("FirstResultTime", Long.toString(FirstResultTime));
                log.info(this.results.toString());
                //log.info(curQueryName + ": Query result have to be protected with one of the following licenses:" + licenseChecker.getLabelLicenses(consistentLicenses) + "\n");
            }
        } catch (Throwable e) {
            long FirstResultTime = System.currentTimeMillis() - this.startQueryExecTime;
            //this.results.put("FirstResultTime", Long.toString(FirstResultTime));
            e.printStackTrace();
            log.error("", e);
            if (this.results.get("validResult").equals("false")) {
                File f = new File("results/" + curQueryName + " " + strategy + "relax_" + this.relax + ".error.txt");
                ByteArrayOutputStream os = new ByteArrayOutputStream();
                PrintStream ps = new PrintStream(os);
                e.printStackTrace(ps);
                ps.flush();
                FileUtils.write(f, os.toString("UTF8"));
                this.error = true;
            }
        } finally {
            if (null != res) {
                res.close();
            }


        }



            }



    }


