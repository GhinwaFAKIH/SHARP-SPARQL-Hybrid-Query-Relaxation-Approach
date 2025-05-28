package RDFStarRelaxer;

import org.apache.jena.query.QueryExecution;
import org.apache.jena.query.QueryExecutionFactory;
import org.apache.jena.query.QuerySolution;
import org.apache.jena.query.ResultSet;
import org.apache.jena.rdf.model.*;
import org.apache.jena.riot.RDFDataMgr;
import org.apache.jena.util.FileManager;
import org.apache.jena.vocabulary.RDFS;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ShaclMapping {
    public static Map<String, Map<String, String>> getRelationshipDomainRangeMap(String ontologyFilePath) {
        Model model = FileManager.get().readModel(null, ontologyFilePath, "TTL");

        Map<String, Map<String, String>> relationshipDomainRangeMap = new HashMap<>();

        StmtIterator relationshipIterator = model.listStatements(null, null, (RDFNode) null);

        while (relationshipIterator.hasNext()) {
            Statement statement = relationshipIterator.nextStatement();
            Resource relationship = statement.getSubject();

            if (!relationship.isURIResource()) {
                // Skip non-URI resources (not a relationship)
                continue;
            }

            String relationshipURI = relationship.getURI();

            if (!relationshipDomainRangeMap.containsKey(relationshipURI)) {
                relationshipDomainRangeMap.put(relationshipURI, new HashMap<>());
            }

            // Check for RDFS domain
            StmtIterator domainIterator = model.listStatements(relationship, RDFS.domain, (RDFNode) null);
            while (domainIterator.hasNext()) {
                Resource domain = domainIterator.nextStatement().getObject().asResource();
                relationshipDomainRangeMap.get(relationshipURI).put("domain", domain.getURI());
            }

            // Check for RDFS range
            StmtIterator rangeIterator = model.listStatements(relationship, RDFS.range, (RDFNode) null);
            while (rangeIterator.hasNext()) {
                Resource range = rangeIterator.nextStatement().getObject().asResource();
                relationshipDomainRangeMap.get(relationshipURI).put("range", range.getURI());
            }
        }

        return relationshipDomainRangeMap;
    }
    public static Map<String, List<Map<String, String>>> getShaclMap () {
        String shacl_path = "data/SHACLshapes.ttl";
        Model model = RDFDataMgr.loadModel(shacl_path);

        String queryString = "PREFIX sh: <http://www.w3.org/ns/shacl#>\n" +
                "PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>\n" +
                "SELECT ?cls ?path ?allowed WHERE {" +
                "  ?sh a sh:NodeShape ." +
                "  ?sh sh:targetClass ?cls ." +
                "  ?sh sh:property ?prop ." +
                "  ?prop sh:path ?path ." +
                "  {" +
                "    ?prop sh:class ?allowed" +
                "  }" +
                "  UNION" +
                "  {" +
                "    ?prop sh:or/rdf:rest*/rdf:first/sh:class ?allowed" +
                "  }" +
                "}";

        QueryExecution qexec = QueryExecutionFactory.create(queryString, model);
        Map<String, List<Map<String, String>>> shaclRelationshipMap = new HashMap<>();

        try {
            ResultSet results = qexec.execSelect();
            while (results.hasNext()) {
                QuerySolution soln = results.nextSolution();
                Resource cls = soln.getResource("cls");
                Resource path = soln.getResource("path");
                Resource allowed = soln.getResource("allowed");

                String rel = path.toString();
                if (!shaclRelationshipMap.containsKey(rel)) {
                    shaclRelationshipMap.put(rel, new ArrayList<>());
                }

                Map<String, String> entry = new HashMap<>();
                entry.put("subject", cls.toString());
                entry.put("object", allowed.toString());

                shaclRelationshipMap.get(rel).add(entry);
            }
        } finally {
            qexec.close();
        }

        return shaclRelationshipMap;


    }
}
