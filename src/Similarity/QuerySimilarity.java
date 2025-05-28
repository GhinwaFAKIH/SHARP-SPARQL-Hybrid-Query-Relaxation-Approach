package Similarity;

import RDFStarRelaxer.RelaxedQuery;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import org.apache.jena.sparql.core.TriplePath;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Map;



public class QuerySimilarity {
    protected static final Logger log = LoggerFactory.getLogger(QuerySimilarity.class);

    private String endpoint;
    // compute the similarity of the relaxedQuery compared to originalQuery
    public void compute(RelaxedQuery relaxedQuery, String computationMethod) throws IOException {
        relaxedQuery.setSimilarity(1.0);
        TriplePath relaxedTriple;
        TriplePath originalTriple;
        double tripleWeight = 1.0;
        for (Map.Entry<TriplePath, TriplePath> entry : relaxedQuery.getOriginalTriples().entrySet()) {
            relaxedTriple = entry.getKey();
            originalTriple = entry.getValue();
            // Sim(Q,Q') = P1..n(tripleWeight * sim(tpi, tpi')
            /*
            System.out.println("the similarity of relaxed triple pattern " + relaxedTriple + "is " + TriplePatternSimilarity.compute_with_add(
                    originalTriple,
                    relaxedTriple));

             */
            if (originalTriple != null) {
                relaxedQuery.setSimilarity(relaxedQuery.getSimilarity() * (tripleWeight * TriplePatternSimilarity.compute(
                        originalTriple,
                        relaxedTriple, computationMethod)
                ));
            }
        }
    }
    public int getTriplesNumber(String propertyURI, String jsonFilePath) {
        JsonObject statistics = TriplePatternSimilarity.loadStatisticsFromJson(jsonFilePath);
        if (statistics != null && statistics.has("Property")) {
            JsonArray propStatistics = statistics.getAsJsonArray("Property");
            return TriplePatternSimilarity.getCountP(propStatistics, propertyURI);
        }
        return 0;
    }

        public int getInstancesNumber(String classURI, String jsonFilePath){
            JsonObject statistics = TriplePatternSimilarity.loadStatisticsFromJson(jsonFilePath);
            if (statistics != null && statistics.has("Class")) {
                JsonArray classStatistics = statistics.getAsJsonArray("Class");
                return TriplePatternSimilarity.getCountC(classStatistics, classURI);
            }
            return 0;
        }
    }
