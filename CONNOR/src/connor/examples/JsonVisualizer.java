package connor.examples;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import org.graphstream.graph.Graph;
import org.graphstream.graph.implementations.MultiGraph;
import org.graphstream.ui.view.Viewer;

import java.io.FileReader;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

public class JsonVisualizer {

    public static void main(String[] args) {

        // Set the UI toolkit for GraphStream (JavaFX or Swing)
        System.setProperty("org.graphstream.ui", "javafx");
        //System.setProperty("org.graphstream.ui", "swing");
        // Load JSON from file and parse it
        List<Map<String, Object>> conceptsOfNeighbours = loadJsonData("data/output3.json");

        // Create a graph using GraphStream
        Graph graph = new MultiGraph("ConceptClusters");

        // Create nodes (clusters) based on concepts and size them
        Map<String, List<String>> conceptExtensions = new HashMap<>();
        Map<String, Set<String>> extensionToConcepts = new HashMap<>();

        for (Map<String, Object> concept : conceptsOfNeighbours) {
            String conceptId = "Concept " + concept.get("id").toString();
            List<List<String>> extensions = (List<List<String>>) concept.get("extension");
            conceptExtensions.put(conceptId, extensions.stream().flatMap(Collection::stream).collect(Collectors.toList()));

            graph.addNode(conceptId).setAttribute("ui.label", conceptId);

            // Track which concepts share extensions
            for (List<String> extList : extensions) {
                for (String ext : extList) {
                    extensionToConcepts.computeIfAbsent(ext, k -> new HashSet<>()).add(conceptId);
                }
            }
        }

        // Determine overlaps and adjust positions accordingly
        for (Map.Entry<String, Set<String>> entry : extensionToConcepts.entrySet()) {
            Set<String> sharedConcepts = entry.getValue();
            if (sharedConcepts.size() > 1) {
                for (String concept : sharedConcepts) {
                    for (String otherConcept : sharedConcepts) {
                        if (!concept.equals(otherConcept)) {
                            // Adjust position of nodes to overlap them
                            graph.getNode(concept).setAttribute("xy", Math.random() * 10, Math.random() * 10);
                            graph.getNode(otherConcept).setAttribute("xy", Math.random() * 10, Math.random() * 10);
                        }
                    }
                }
            }
        }

        // Apply a visual style to show clusters clearly
        graph.setAttribute("ui.stylesheet", "node { size-mode: dyn-size; fill-color: red, yellow; }");

        // Display the graph
        Viewer viewer = graph.display();
    }

    private static List<Map<String, Object>> loadJsonData(String filePath) {
        try (FileReader reader = new FileReader(filePath)) {
            Gson gson = new Gson();
            java.lang.reflect.Type mapType = new TypeToken<Map<String, Object>>() {}.getType();
            Map<String, Object> jsonData = gson.fromJson(reader, mapType);

            // Extract conceptsOfNeighbours from JSON data
            return (List<Map<String, Object>>) jsonData.get("conceptsOfNeighbours");

        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }
}
