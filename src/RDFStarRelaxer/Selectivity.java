package RDFStarRelaxer;

import com.github.jsonldjava.shaded.com.google.common.reflect.TypeToken;
import com.google.gson.Gson;
import org.apache.jena.sparql.core.TriplePath;

import java.io.FileReader;
import java.io.IOException;
import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Selectivity {
    private static final String JSON_STATISTICS_PATH = "data/lubm_statistics-3.json";
    private static Map<String, Integer> propertyFrequencyMap = new HashMap<>();
    private static Map<String, Integer> propertyObjectFrequencyMap = new HashMap<>();
    private static int totalResources;
    private static int totalTriples;

    public static void loadStatistics(String JSON_STATISTICS_PATH) {
        try (FileReader reader = new FileReader(JSON_STATISTICS_PATH)) {
            Gson gson = new Gson();
            Type type = new TypeToken<Map<String, Object>>(){}.getType();
            Map<String, Object> statistics = gson.fromJson(reader, type);

            // Load total resources and total triples
            totalResources = Integer.parseInt((String) statistics.get("Total Classes Instances"));
            totalTriples = Integer.parseInt((String) statistics.get("Total Triples"));

            // Load property frequencies
            List<Map<String, String>> propertyList = (List<Map<String, String>>) statistics.get("Property");
            for (Map<String, String> property : propertyList) {
                propertyFrequencyMap.put(property.get("prop"), Integer.parseInt(property.get("count")));
            }

            // Load property-object frequencies
            List<Map<String, String>> propObjFreqList = (List<Map<String, String>>) statistics.get("PropertyObjectFrequency");
            for (Map<String, String> propObjFreq : propObjFreqList) {
                String key = propObjFreq.get("property") + "_" + propObjFreq.get("object");
                propertyObjectFrequencyMap.put(key, Integer.parseInt(propObjFreq.get("count")));
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    public static double TriplePatternSelectivity(TriplePath triplePattern) {
        double selS = 1.0 / totalResources;

        String predicate = triplePattern.getPredicate().toString();
        int tp = propertyFrequencyMap.getOrDefault(predicate, 0);
        double selP = (double) tp / totalTriples;

        String object = triplePattern.getObject().toString();
        String propertyObjectKey = predicate + "_" + object;
        int freqPo = propertyObjectFrequencyMap.getOrDefault(propertyObjectKey, 0);
        double selO = (double) freqPo / tp;

        return selS * selP * selO;
    }
    public static double calculateQuerySelectivity(List<TriplePath> queryTriplePatterns) {
        double selectivity = 1.0;
        for (TriplePath triplePattern : queryTriplePatterns) {
            selectivity *= TriplePatternSelectivity(triplePattern);
        }
        return selectivity;
    }

}
