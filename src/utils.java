import org.apache.jena.query.*;
import org.apache.jena.sparql.syntax.Element;
import org.apache.jena.sparql.syntax.ElementGroup;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class utils {
    public static int qcount=0;
    public static Query createsubquery (Element element) {
        if(element instanceof ElementGroup){
            Element elementGroup = new ElementGroup();
            List<Element> elements= (List <Element>) ((ElementGroup) element).getElements();
            for (Element element1 : elements){
                ((ElementGroup) elementGroup).addElement(element1);
            }
            // Create a Query object from the Element
            Query subQuery = QueryFactory.create();
            subQuery.setQueryPattern(elementGroup);
            subQuery.setQuerySelectType();
            subQuery.setQueryResultStar(true);
            return subQuery;
        }
        // Create an example Element representing the query pattern
        Element elementGroup = new ElementGroup();
        // Add your query pattern elements to the ElementGroup
        ((ElementGroup) elementGroup).addElement(element);
        // Create a Query object from the Element
        Query subQuery = QueryFactory.create();
        subQuery.setQueryPattern(elementGroup);
        subQuery.setQuerySelectType();
        subQuery.setQueryResultStar(true);
        return subQuery;
    }
    public static boolean hasIntersection(List<Element> list1, List<Element> list2) {
        // Convert the first list to a set for faster lookup
        Set<Element> set1 = new HashSet<>(list1);

        // Check if any element in the second list is present in the set
        for (Element element : list2) {
            if (set1.contains(element)) {
                return true;
            }
        }
        return false;
    }
    public static boolean contains_element (Set<Element> A, Element element) {
        for (Element a : A) {
            List<Element> list1 = (List<Element>) ((ElementGroup) a).getElements();
            Set<Element> set1 = new HashSet<>(list1);
            List<Element> list2 = (List<Element>) ((ElementGroup) element).getElements();
            Set<Element> set2 = new HashSet<>(list2);
            if (set1.equals(set2)) {
                return true;
            }
        }
        return false;
    }
    public static boolean hasResults(Query query) {
        try (QueryExecution queryExecution = QueryExecutionFactory.sparqlService("http://localhost:3000/closure/query", query)) {
            ResultSet resultSet = queryExecution.execSelect();
            qcount++;
            return resultSet.hasNext();
        } catch (Exception e) {
            // Handle any exceptions that occur during query execution
            e.printStackTrace();
            return false;
        }
    }
}
