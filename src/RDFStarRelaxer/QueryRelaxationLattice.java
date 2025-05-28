package RDFStarRelaxer;

import Similarity.QuerySimilarity;
import org.apache.jena.query.*;
import org.apache.jena.rdf.model.Model;
import org.eclipse.rdf4j.federated.algebra.StatementSource;
import org.eclipse.rdf4j.query.algebra.StatementPattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.*;

public class QueryRelaxationLattice {
    protected static final Logger log = LoggerFactory.getLogger(QueryRelaxationLattice.class);
    private RelaxedQuery originalQuery;
    private QueryRelaxer queryRelaxer;
    private PriorityQueue<RelaxedQuery> priorityQueue;
    private Model ontology;
    private String computationMethod;
    private String reifModel;
    private double minSimilarity;
    private Map<StatementPattern, List<StatementSource>> stmtToSources;

    private QuerySimilarity querySimilarity;
    public boolean hasNext() {
        return (this.priorityQueue.size() > 0);
    }
    public QueryRelaxationLattice(RelaxedQuery originalQuery, Model ontology, double minSimilarity, QueryRelaxer queryRelaxer, String computationMethod, String reifModel) throws IOException {
        this.priorityQueue = new PriorityQueue<>(Collections.reverseOrder());
        this.originalQuery = originalQuery;
        this.ontology = ontology;
        this.minSimilarity = minSimilarity;
        this.queryRelaxer = queryRelaxer;
        this.computationMethod = computationMethod;
        this.reifModel = reifModel;
        this.querySimilarity = new QuerySimilarity();
        System.out.println("the original query is " + originalQuery);
        ArrayList<RelaxedQuery> relaxedQueries = this.queryRelaxer.relax(this.originalQuery ,this.originalQuery ,this.ontology, this.querySimilarity, reifModel);
        //System.out.println("the relaxed queries: " + relaxedQueries);
        for (RelaxedQuery relaxedQuery : relaxedQueries) {
            querySimilarity.compute(relaxedQuery, computationMethod);
            //System.out.println("the relaxed query is " + relaxedQuery.cloneRDFStar());
            if (relaxedQuery.getSimilarity() >= this.minSimilarity) { this.priorityQueue.add(relaxedQuery); }
        }
        int length = relaxedQueries.size();
        System.out.println("length of query relaxation lattice: " + length);
        int length2 = priorityQueue.size();
        System.out.println("length of partial query relaxation lattice: " + length2);
    }
    // Method to get and print top K results based on similarity


    public void printTopKResults(int k) {
        System.out.println("Top " + k + " unique results from relaxed queries:");

        Set<String> uniqueResults = new HashSet<>();  // Track unique result strings
        List<Result> finalResults = new ArrayList<>();

        // Execute each relaxed query and collect results until we have k unique results
        while (!this.priorityQueue.isEmpty() && uniqueResults.size() < k) {
            RelaxedQuery relaxedQuery = this.priorityQueue.poll();

            // Serialize the query and add LIMIT k to it
            Query query = QueryFactory.create(relaxedQuery.serialize() + " LIMIT " + k);
            System.out.println("Executing query: " + query);

            // Execute the query using a SPARQL endpoint
            QueryExecution qexec = QueryExecutionFactory.sparqlService("http://localhost:3030/Benchdataset/query", query);

            try {
                ResultSet resultSet = qexec.execSelect();

                // Iterate over the result set
                while (resultSet.hasNext() && uniqueResults.size() < k) {
                    QuerySolution solution = resultSet.nextSolution();

                    // Convert the solution to a string (or use a unique identifier)
                    String solutionString = solution.toString();

                    // Add only unique solutions to the set
                    if (uniqueResults.add(solutionString)) {
                        // If it's a new result, wrap it in a Result object
                        finalResults.add(new Result(solution, relaxedQuery.getSimilarity()));
                    }
                }
            } finally {
                qexec.close();
            }
        }

        // Sort the final results by similarity (descending order)
        finalResults.sort((r1, r2) -> Double.compare(r2.getSimilarity(), r1.getSimilarity()));

        // Print top k unique results
        for (int i = 0; i < Math.min(k, finalResults.size()); i++) {
            Result result = finalResults.get(i);
            System.out.println("Result: " + result.getQuerySolution() + " | Similarity: " + result.getSimilarity());
        }

        if (uniqueResults.size() < k) {
            System.out.println("Only " + uniqueResults.size() + " unique results found.");
        }
    }
    public void writeTopKResultsToFile(int k, String filePath) {
        System.out.println("Writing top " + k + " unique results to file from relaxed queries...");
    
        Set<String> seenFirstVariableValues = new HashSet<>();
        List<Result> finalResults = new ArrayList<>();
        boolean hasReachedK = false;
    
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(filePath))) {
            System.out.println("priority Queue: " + priorityQueue);
    
            while (!this.priorityQueue.isEmpty() && !hasReachedK) {
                RelaxedQuery relaxedQuery = this.priorityQueue.poll();
                Query query = QueryFactory.create(relaxedQuery.serialize());
                System.out.println("Executing query: " + query);
    
                QueryExecution qexec = QueryExecutionFactory.sparqlService(
                    "http://localhost:3030/Benchdataset/query", query
                );
    
                int newResultsInThisQuery = 0;
    
                try {
                    ResultSet resultSet = qexec.execSelect();
    
                    while (resultSet.hasNext()) {
                        QuerySolution solution = resultSet.nextSolution();
                        List<String> resultVars = resultSet.getResultVars();
                        if (resultVars.isEmpty()) continue; // no variables in result
                        String firstVarName = resultVars.get(0);
                        //System.out.println("the variable name: " + firstVarName);   
                        if (!solution.contains(firstVarName)) continue;
                        String firstVarValue = solution.get(firstVarName).toString();
    
                        if (seenFirstVariableValues.add(firstVarValue)) {
                            finalResults.add(new Result(solution, relaxedQuery.getSimilarity()));
                            writer.write("X = " + firstVarValue + " | Similarity: " + relaxedQuery.getSimilarity());
                            writer.newLine();
                            writer.write("Relaxed Query: " + query);
                            writer.newLine();
                            newResultsInThisQuery++;
                        }
                    }
    
                } finally {
                    qexec.close();
                }
    
                System.out.println(newResultsInThisQuery + " new unique results found in this query.");
                System.out.println("Total unique so far: " + seenFirstVariableValues.size());
    
                // Only stop *after* writing the full results of the query that caused us to pass k
                if (seenFirstVariableValues.size() >= k) {
                    hasReachedK = true;
                }
            }
    
            if (seenFirstVariableValues.size() < k) {
                writer.write("Only " + seenFirstVariableValues.size() + " unique results found.");
                writer.newLine();
            }
    
        } catch (IOException e) {
            System.err.println("Error writing to file: " + e.getMessage());
        }
    }
    

    private static class Result {
        private QuerySolution querySolution;
        private double similarity;

        public Result(QuerySolution querySolution, double similarity) {
            this.querySolution = querySolution;
            this.similarity = similarity;
        }

        public QuerySolution getQuerySolution() {
            return querySolution;
        }

        public double getSimilarity() {
            return similarity;
        }
    }



    public RelaxedQuery next() throws IOException {
        RelaxedQuery nextMostSimilarQuery = this.priorityQueue.poll();
        if (nextMostSimilarQuery != null) {
            ArrayList<RelaxedQuery> relaxedQueries = this.queryRelaxer.relax(this.originalQuery, nextMostSimilarQuery, this.ontology, this.querySimilarity, reifModel);
            for (RelaxedQuery relaxedQuery : relaxedQueries) {
                this.querySimilarity.compute(relaxedQuery, computationMethod);
                if (relaxedQuery.getSimilarity() >= this.minSimilarity) { this.priorityQueue.add(relaxedQuery); }
            }
        } else {
            throw new NoSuchElementException("RelaxedLattice has no more relaxed queries to generate");
        }
        return nextMostSimilarQuery;
    }

    public int sizeOfRemaining() {
        return this.priorityQueue.size();
    }

}
