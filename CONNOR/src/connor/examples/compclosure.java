import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.util.FileManager;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.formats.TurtleOntologyFormat;
import org.semanticweb.owlapi.io.FileDocumentSource;
import org.semanticweb.owlapi.model.*;
import org.semanticweb.owlapi.reasoner.*;
import org.semanticweb.owlapi.reasoner.structural.StructuralReasonerFactory;
import org.semanticweb.owlapi.vocab.PrefixOWLOntologyFormat;
import com.clarkparsia.pellet.owlapiv3.PelletReasonerFactory;
import com.clarkparsia.pellet.owlapiv3.PelletReasoner;
import java.util.HashSet;
import org.apache.jena.rdf.model.StmtIterator;
import java.io.InputStream;
import org.apache.jena.rdf.model.ResourceFactory;
import org.apache.jena.rdf.model.Statement;
import org.apache.jena.vocabulary.DCTerms;
import java.io.FileOutputStream;











import java.io.File;
import java.io.IOException;
import java.util.Set;

public class compclosure {


    // Utility method to get the set of supercategories for a category

    private static Set<Resource> getSupercategories(Resource category, OWLReasoner reasoner) {
        Set<Resource> supercategories = new HashSet<>();
        OWLDataFactory dataFactory = reasoner.getRootOntology().getOWLOntologyManager().getOWLDataFactory();
        OWLClass categoryClass = dataFactory.getOWLClass(category.getURI());
        //System.out.println(categoryClass);
        NodeSet<OWLClass> supercategoryNodes = reasoner.getSuperClasses(categoryClass, false);
        System.out.println(supercategoryNodes);
        for (Node<OWLClass> supercategoryNode : supercategoryNodes) {
            for (OWLClass supercategory : supercategoryNode.getEntities()) {
                if (!supercategory.isOWLThing()) {
                    supercategories.add(ResourceFactory.createResource(supercategory.getIRI().toString()));
                }
            }
        }
        return supercategories;
    }

    public static void main(String[] args) throws OWLOntologyCreationException, InconsistentOntologyException, OWLOntologyStorageException, IOException {
        {
            // Load the ontology
            OWLOntologyManager manager = OWLManager.createOWLOntologyManager();
            File file = new File("data/categories/categories_skos.ttl");
            FileDocumentSource source = new FileDocumentSource(file);
            OWLOntology ontology = manager.loadOntologyFromOntologyDocument(source);
            long numAxioms = ontology.axioms().count();
            System.out.println("The ontology has " + numAxioms + " axioms.");




            // Merge the ontology and the instances
            //OWLOntology mergedOntology = manager.createOntology();
            //.addAxioms(mergedOntology, ontology.getAxioms());
            //manager.addAxioms(mergedOntology, instances.getAxioms());

            // Create a Pellet reasoner and set the timeout
            //OWLReasonerConfiguration config = new SimpleConfiguration(500);

            // Create a Pellet reasoner
            OWLReasoner reasoner = PelletReasonerFactory.getInstance().createReasoner(ontology);


            // Set cycle detection to false
            //reasoner.getOptions().setUseCycleDetection(true);


            // Precompute inferences
            reasoner.precomputeInferences(InferenceType.CLASS_HIERARCHY);

            // Get all classes in the ontology
            Set<OWLClass> classes = ontology.getClassesInSignature();

            // Iterate over the classes and print their subclasses
            //for (OWLClass cls : classes) {
            //NodeSet<OWLClass> subclasses = reasoner.getSubClasses(cls, true);
            //System.out.println("Subclasses of " + cls + ": " + subclasses);
            //}
            /*
            // Compute the closure
            OWLOntology closureOntology = manager.createOntology();
            for (OWLClass cls : classes) {
                NodeSet<OWLClass> subClasses = reasoner.getSubClasses(cls, true);
                for (OWLClass subClass : subClasses.getFlattened()) {
                    if (!subClass.isOWLThing()) { // Exclude owl:Thing from the closure
                        OWLDataFactory factory = manager.getOWLDataFactory();
                        OWLObjectProperty broader = factory.getOWLObjectProperty(IRI.create("http://www.w3.org/2004/02/skos/core#broader"));
                        OWLObjectSomeValuesFrom someValuesFrom = factory.getOWLObjectSomeValuesFrom(broader, cls);
                        OWLSubClassOfAxiom axiom = factory.getOWLSubClassOfAxiom(subClass, someValuesFrom);
                        manager.addAxiom(closureOntology, axiom);
                    }
                }

        }
             */
            /*
            // Compute the subcategory relationships using the SKOS hierarchy
            OWLDataFactory dataFactory = manager.getOWLDataFactory();
            OWLClass categoryClass = dataFactory.getOWLClass(IRI.create("http://www.w3.org/2004/02/skos/core#Concept"));
            Set<OWLClass> categories = reasoner.getSubClasses(categoryClass, true).getFlattened();
            //Set<OWLClass> categories = categoryClass.getSubClasses(reasoner.getRootOntology());
            Set<String> subcategories = new HashSet<>();
            for (OWLClass category : categories) {
                NodeSet<OWLClass> subcategoryNodes = reasoner.getSubClasses(category, false);
                System.out.println(supercategoryNodes);
                for (Node<OWLClass> subcategoryNode : subcategoryNodes) {
                    for (OWLClass subcategory : subcategoryNode.getEntities()) {
                        subcategories.add(subcategory.getIRI().toString());
                    }
                }
            }
            */

            // Load the DBpedia data file

            InputStream dataStream = FileManager.get().open("data/categories/categories_article.nt");
            Model dataModel = ModelFactory.createDefaultModel();
            dataModel.read(dataStream, null, "N-Triples");

            // Create a new model to store the inferred triples
            Model inferredModel = ModelFactory.createDefaultModel();

            // Iterate over the resources and their categories
            StmtIterator stmts = dataModel.listStatements();
            while (stmts.hasNext()) {
                Statement stmt = stmts.next();
                Resource resource = stmt.getSubject();
                Resource category = stmt.getObject().asResource();

            // Print the direct superclasses



            // Get the supercategories for this category
                Set<Resource> supercategories = getSupercategories(category, reasoner);
                //System.out.println(supercategories);

            // Add inferred triples linking the resource to each supercategory
                for (Resource supercategory : supercategories) {
                    inferredModel.add(resource, DCTerms.subject, supercategory);
                }
            }



            // Save the inferred model to a file
            File inferredFile = new File("data/categories/inferred_categories.ttl");
            FileOutputStream out = new FileOutputStream(inferredFile);
            inferredModel.write(out, "TURTLE");
            out.close();
        }
    }
}
    /*

    // Save the closure ontology
            File closureFile = new File("data/categories/closure.owl");
            manager.saveOntology(closureOntology, IRI.create(closureFile.toURI()));

            System.out.println("Closure ontology saved to " + closureFile.getAbsolutePath());

        }
    }
}
     */

