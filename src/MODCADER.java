import org.apache.jena.query.*;
import org.apache.jena.sparql.core.PathBlock;
import org.apache.jena.sparql.core.TriplePath;
import org.apache.jena.sparql.syntax.*;

import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

public class MODCADER {
    public static Set<Element> mfs = new HashSet<>();
    public static Set<Element> computemfs (Set<Element>  query_elements, Set<Element> failing_elements, Set<Element> succeeding_elements) {
        boolean loop_val = true;
        Set<Element> lattice_elements = query_elements;
        Set<Element> S = new HashSet<>();
        while (loop_val == true) {
            for (Element element : lattice_elements) {
                //System.out.println("Element: " + element);
                if (!failing_elements.contains(element)) {
                    Query temp_query = utils.createsubquery(element);
                    if (succeeding_elements.contains(element)) {
                        S.add(temp_query.getQueryPattern());
                        //System.out.println("Adding the query to the S set for later composition");
                    } else {
                        if (!utils.hasResults(temp_query)) {
                            MODCADER.mfs.add(temp_query.getQueryPattern());
                            System.out.println("Adding the query " + temp_query +" to the mfs set");
                        } else {
                            S.add(temp_query.getQueryPattern());
                            //System.out.println("Adding the query to the S set for later composition");
                        }
                    }
                }
            }

            if (!S.isEmpty()) {
                    //create the lattice elements
                    //System.out.println("Set S: " + S);
                    System.out.println("lattice elements size: " + lattice_elements.size());
                    lattice_elements = query_composition(S, MODCADER.mfs);
                    //System.out.println("lattice elements : " + lattice_elements);
                    S.clear();
                    System.out.println("Clearing the set S ");
            } else {
                    loop_val = false;
                }
           }

        return MODCADER.mfs;

    }



    private static Set<Element> query_composition (Set<Element> S, Set<Element> mfs) {
        Set<Element> result = new HashSet<>();
        Iterator<Element> itr1 = S.iterator();
        Set<Element> used = new HashSet<>(); // track the elements that have been used
        while (itr1.hasNext()) {
            Iterator<Element> itr2 = S.iterator(); // a new iterator
            Element s1 = itr1.next();
            used.add(s1); // track what we've used
            while (itr2.hasNext()) {
                Element s2 = itr2.next();
                if (!used.contains(s2)) {
                    if (s1 instanceof ElementGroup & s2 instanceof ElementGroup ) {
                        if (utils.hasIntersection(((ElementGroup) s1).getElements(), ((ElementGroup) s2).getElements()) & ((((ElementGroup) s1).getElements()).size() != 1)) {
                            Element composition = composeElements(s1, s2);
                            if (!compositionOverlapsMFS(composition, mfs) & !utils.contains_element(result,composition)) {
                                result.add(composition);
                            }
                        } else {
                            Element composition = composeElements(s1, s2);
                            if (!compositionOverlapsMFS(composition, mfs) & !utils.contains_element(result,composition)) {
                                result.add(composition);
                            }
                        }
                    }
                    else{
                        Element composition = composeElements(s1, s2);
                        if (!compositionOverlapsMFS(composition, mfs) & !utils.contains_element(result,composition)) {
                            result.add(composition);
                        }
                    }
                }
            }
        }
        return result;
    }
    private static Element composeElements (Element s1, Element s2){
        // Combine the query elements and create a new element
        // Here, we assume concatenation of the two elements
        Set <Element> common_elements = new HashSet<>();
        if (s1 instanceof ElementGroup & s2 instanceof ElementGroup) {
            //if (utils.hasIntersection(((ElementGroup) s1).getElements(),((ElementGroup) s2).getElements()))
                //{
                ElementGroup combinedElement = new ElementGroup();
                List<Element> elements1 = (List<Element>) ((ElementGroup) s1).getElements();
                List<Element> elements2 = (List<Element>) ((ElementGroup) s2).getElements();
                for (Element element : elements1) {
                    combinedElement.addElement(element);
                    common_elements.add(element);
                }
                for (Element element : elements2) {
                    if (!common_elements.contains(element)) {
                        combinedElement.addElement(element);
                    }
                }
                return combinedElement;
        }
        ElementGroup combinedElement = new ElementGroup();
        combinedElement.addElement(s1);
        combinedElement.addElement(s2);
        return  combinedElement;
    }

    private static boolean compositionOverlapsMFS(Element composition, Set<Element> M) {
            for (Element element : M) {
                List<Element> list1 = (List<Element>) ((ElementGroup) element).getElements();
                Set<Element> set1 = new HashSet<>(list1);
                List<Element> list2 = (List<Element>) ((ElementGroup) composition).getElements();
                Set<Element> set2 = new HashSet<>(list2);
                if(set2.containsAll(set1)) {
                    return true;
                }

            }
            /*
            if (M.contains(composition)) {
                return true;
            }

             */
        return false;
    }

    public static void main(String[] args) {
        //List <Element> elements = [ ?o  <http://example.org/p3>  <http://example.org/o3> , ?o <http://example.org/p3>  <http://example.org/o7>];
        String named_graph = "PREFIX ex:<http://www.example.org/> Select * where { " +
                "GRAPH ?g5 {" +
                "?g4 ex:p5 ex:o5." +
                " GRAPH ?g4 {  " +
                "?g3 ex:p4 ex:o4. " +
                "GRAPH  ?g3 { " +
                "?g1 ex:p2 ?g2. " +
                "GRAPH ?g1 {?s ex:p1 ex:o1.}" +
                "GRAPH ?g2 {?o ex:p3 ex:o3." +
                " ?o ex:p3 ex:o7}" +
                "}" +
                "} " +
                "}" +
                "}";

        String std_query = "PREFIX ex:<http://www.example.org/> " +
                "PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#> " +
                "PREFIX foaf: <http://xmlns.com/foaf/0.1/> " +
                "Select * where { " +
                //"?st1  rdf:type rdf:Statement; \n" +
                "?st1          rdf:subject ?s;\n" +
                "\t\t            rdf:object   ex:o1;\n" +
                "\t\t\trdf:predicate ex:p1.\t\n" +
               // "\t\t?st2     rdf:type rdf:Statement; \n" +
                "?st2           rdf:subject ?st1;\n" +
                "\t\t            rdf:object   ?st3;\n" +
                "\t\t\trdf:predicate ex:p2.\n" +
               // "\t\t?st3 \trdf:type rdf:Statement; \n" +
                "?st3           rdf:subject ?o;\n" +
                "\t\t            rdf:object   ex:o7;\n" +
                "\t\t\trdf:predicate ex:p3.\t\t\n" +
                //"?st4     rdf:type rdf:Statement; \n" +
                "?st4           rdf:subject ?st2;\n" +
                "\t\t            rdf:predicate   ex:p4;\n" +
                "\t\t\trdf:object ex:o6.\n" +
              //  "  ?st5     rdf:type rdf:Statement; \n" +
                "?st5            rdf:subject ?st4;\n" +
                "\t\t            rdf:predicate   ex:p5;\n" +
                "\t\t\trdf:object ex:o5.\n" +
                "\t\t\n" +
                "} ";

        String sg_query= "PREFIX ex:<http://www.example.org/> " +
                "PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#> " +
                "Select * where {  \t?s \t?pp1 ex:o1.\n" +
                "\t\t\t?pp1 \trdf:SingletonPropertyOf ex:p1;\n" +
                "\t\t\t\t?pp3 ?pp2.\n" +
                "?o\t?pp2 ex:o7.\n" +
                "?pp2 \trdf:SingletonPropertyOf ex:p3.\n" +
                "?pp3    rdf:SingletonPropertyOf ex:p2;\n" +
                "\t?pp4 ex:o6.\n" +
                "?pp4    rdf:SingletonPropertyOf ex:p4;\n" +
                "\t\t\t\t?pp5 ex:o5.\n" +
                "?pp5 rdf:SingletonPropertyOf ex:p5.  \n" +
                "\n" +
                "}";
        String nary_query = "PREFIX ex:<http://www.example.org/>" +
                "Select ?s ?o where { \n"+
                "?s  ex:p1 ?r1.\n"+
                "?r1 ex:relation_value_1 ex:o1.\n"+
                "?o  ex:p3 ?r2.\n"+
                "?r2 ex:relation_value_2 ex:o7.\n"+
                "ex:relation_1  ex:p2 ?r3.\n"+
                "?r3 ex:relation_value_3 ex:relation_2.\n"+
                "?r3  ex:p4 ?r4.\n"+
                "?r4 ex:relation_value_4 ex:o6.\n"+
                "?r4  ex:p5 ?r5.\n"+
                "?r5 ex:relation_value_5 ex:o5.\n"+
                "}";
        String std_query_2 = "PREFIX ex:<http://example.org/>" +
                "PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#> " +
                "PREFIX xsd: <http://www.w3.org/2001/XMLSchema#> " +
                "PREFIX foaf: <http://xmlns.com/foaf/0.1/> " +
                "Select * where { \n"+
                "?teacher  rdf:type ex:Teacher;\n"+
                "ex:teaches ex:Semantic_Web.\n"+
                // " ex:worksIn \"Nantes Université\".\n"+
                "?student       rdf:type        ex:Student; \n"+
                "ex:friendOf/foaf:knows  ex:Alice;\n"+
                "ex:knows ?teacher.\n"+
                "?statement  rdf:type rdf:Statement;\n"+
                "rdf:subject ?student;\n"+
                "rdf:object  ex:SemanticWeb;\n"+
                "rdf:predicate ex:enrolledIn;\n"+
                "ex:enrolldate  \"2023-09-13\"^^xsd:date.\n"+
                "}";
        String temp_query = "PREFIX ex:<http://example.org/>" +
                "PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#> " +
                "PREFIX xsd: <http://www.w3.org/2001/XMLSchema#> " +
                "Select ?statement where { \n"+
                "?statement rdf:predicate ex:enrolledIn;\n"+
                "}";
        String failing_query =  "PREFIX ex:<http://example.org/>" +
                "PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#> " +
                "PREFIX xsd: <http://www.w3.org/2001/XMLSchema#> " +
                "PREFIX foaf: <http://xmlns.com/foaf/0.1/> " +
                "SELECT * WHERE {\n" +
                "\n" +
                "   ?teacher       rdf:type              ex:Teacher;                      \n" +
                "                  ex:teaches            ex:Semantic_Web.                \n" +
                "   ?student       rdf:type              ex:Student;                      \n" +
                "                  ex:friendOf/foaf:knows  ex:Alice;                        \n" +
                "                  foaf:knows              ?teacher.                        \n" +
                "   ?statement     rdf:type              rdf:Statement;                   \n" +
                "                  rdf:subject           ?student;                                                         \n" +
                "                  rdf:object            ex:Semantic_Web;                 \n" +
                "                  rdf:predicate         ex:enrolledIn;\n" +
                "                  ex:enrolldate         \"2023-09-13\"^^xsd:date.\n" +
                "               }";
        Set<Element> queryElements = new HashSet<>();
        Query query = QueryFactory.create(failing_query);
        Element element = query.getQueryPattern();
        ElementGroup elementGroup = (ElementGroup) element;
        List<Element> groupElements = elementGroup.getElements();
        for (Element groupElement : groupElements) {
            if (groupElement  instanceof ElementPathBlock) {
                ElementPathBlock pathBlock = (ElementPathBlock) groupElement ;
                PathBlock pattern = pathBlock.getPattern();
                List<TriplePath> triplePaths = pattern.getList();

                for (TriplePath triplePath : triplePaths) {
                    ElementPathBlock triplePattern = new ElementPathBlock();
                    triplePattern.addTriplePath(triplePath);
                    queryElements.add(triplePattern);
                }
            }
        }
        System.out.println("Elements of Query: " + queryElements);



                Set<Element> failing_elements = new HashSet<>();
                Set<Element> succeeding_elements = new HashSet<>();
                Set<Element> S = new HashSet<>();
                Set<Element> composition1 = query_composition(queryElements,failing_elements);
                //System.out.println("Composition 1: " + composition1);
                //System.out.println("Composition 1 length: " + composition1.size());
                //Set<Element> composition2 = query_composition(composition1,failing_elements);
                //System.out.println("Composition 2: " + composition2);
                Set<Element> mfs = computemfs(queryElements,failing_elements, succeeding_elements );
                System.out.println("MFS: " + mfs);
                System.out.println("The number of executed queries while computing MFS: "+ utils.qcount);
                //Query temp_query_2 = QueryFactory.create(temp_query);
                //System.out.println(utils.hasResults(temp_query_2));

    }
}
