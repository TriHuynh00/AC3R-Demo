package org.saarland.ontologyparser;

import org.saarland.accidentconstructor.ConsoleLogger;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.*;
import org.semanticweb.owlapi.reasoner.*;
import org.semanticweb.owlapi.reasoner.structural.StructuralReasonerFactory;
import org.semanticweb.owlapi.util.DefaultPrefixManager;

import java.io.File;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Set;

public class OntologyHandler {

    private final PrefixManager pm = new DefaultPrefixManager(
            "file:/D:/University/MasterProgram/Thesis/AlessioTopic/Draft/tri-huynh-minh/ontology/TrafficAccidentOntology#");

    private LinkedList<String> keyWordList;
    private LinkedList<AccidentConcept> accidentConceptList;
    private LinkedList<AccidentConcept> roadConcepts;


    public LinkedList<AccidentConcept> getRoadConcepts() {
        return roadConcepts;
    }

    public void setRoadConcepts(LinkedList<AccidentConcept> roadConcepts) {
        this.roadConcepts = roadConcepts;
    }

    private boolean finishedLoading = false;

    public boolean isFinishedLoading() {
        return finishedLoading;
    }

    public void readOntology() throws OWLOntologyCreationException {
        keyWordList = new LinkedList<String>();
        accidentConceptList = new LinkedList<AccidentConcept>();
        roadConcepts = new LinkedList<AccidentConcept>();

        OWLOntologyManager manager = OWLManager.createOWLOntologyManager();
        IRI remoteOntology = IRI.create(new File("ontology/AccidentOntology.owl"));
        OWLOntology accidentOntology = manager.loadOntology(remoteOntology);

        ConsoleLogger.print('d',"Loaded ontology: " + accidentOntology);

        OWLDataFactory factory = manager.getOWLDataFactory();

        OWLReasonerFactory reasonerFactory = new StructuralReasonerFactory();
        ConsoleProgressMonitor progressMonitor = new ConsoleProgressMonitor();
        OWLReasonerConfiguration config = new SimpleConfiguration(progressMonitor);
        OWLReasoner reasoner = reasonerFactory.createReasoner(accidentOntology, config);

        reasoner.precomputeInferences();

        // Get All Leaf Level instances

        ConsoleLogger.print('d',"Read Ontology Structure");

        String[] interestingConceptList = {"vehicle", "pedestrian"};

        for (String importantConcept : interestingConceptList)
        {
            keyWordList.add(importantConcept);
        }

        for(OWLClass owlClass : accidentOntology.getClassesInSignature()){
//            Set<OWLClass> subclasses = reasoner.getSubClasses(owlClass,false).getFlattened();
//            ConsoleLogger.print('d',"Subclass of " + owlClass.toStringID());
//            ConsoleLogger.print('d',"Print all classes");
            String owlClassFullName = owlClass.toStringID();
            int leafLvlStrIndex = owlClassFullName.indexOf("-leaflvl");
            if (leafLvlStrIndex > 0){
                // Get all instances of a class, which contain key concepts
                NodeSet<OWLNamedIndividual> individualsInSignature = reasoner.getInstances(owlClass, true);
//                ConsoleLogger.print('d',"Instances=>");
                Set<OWLNamedIndividual> individualsList = individualsInSignature.getFlattened();
                for (OWLNamedIndividual keyWord : individualsList)
                {

                    String keyWordStr = keyWord.toStringID();
                    String keyWordName = keyWordStr.substring(keyWordStr.indexOf("#") + 1);
//                    ConsoleLogger.print('d',keyWordName);
                    keyWordList.add(keyWordName);

                    AccidentConcept accidentConcept = new AccidentConcept();
                    accidentConcept.setConceptName(keyWordName);
//                    ConsoleLogger.print('d',"Class=>" + owlClassFullName.substring(owlClassFullName.indexOf("#") + 1, leafLvlStrIndex));
                    accidentConcept.setConceptGroup(owlClassFullName.substring(owlClassFullName.indexOf("#") + 1, leafLvlStrIndex));

//                    ConsoleLogger.print('d',"Obj Prop " + factory.getOWLObjectProperty("isVehicleType", pm).getIndividualsInSignature());
//                    ConsoleLogger.print('d',"ParentClass=>" + reasoner.getSuperClasses(owlClass, false).getFlattened().toString());
                    Set<OWLClass> superClassesList = reasoner.getSuperClasses(owlClass, false).getFlattened();
                    for (OWLClass superClass : superClassesList)
                    {
                        String superClassName = superClass.toStringID();
                        if (superClassName.contains("-interestinglvl"))
                        {
                            String categoryStr = superClassName.substring(superClassName.indexOf("#") + 1,
                                    superClassName.indexOf("-interestinglvl"));
                            accidentConcept.setCategory(categoryStr);

                            if (categoryStr.equals("road"))
                            {
                                roadConcepts.add(accidentConcept);

                            }

                        }
                        else if (superClassName.contains("-toplvl"))
                        {
                            accidentConcept.setInputGroup(superClassName.substring(superClassName.indexOf("#") + 1,
                                    superClassName.indexOf("-toplvl")));
                        }

                    }

                    Set<OWLDataPropertyAssertionAxiom> dataProperties = accidentOntology.getDataPropertyAssertionAxioms(keyWord);

                    if (dataProperties.size() > 0) {
                        HashMap<String, String> dataPropMap = new HashMap<String, String>();
                        for (OWLDataPropertyAssertionAxiom prop : dataProperties) {
                            String propName = prop.getDataPropertiesInSignature().toArray()[0].toString();
                            propName = propName.substring(propName.indexOf("#") + 1, propName.indexOf(">"));
//                            ConsoleLogger.print('d',propName + " " + prop.getObject().getLiteral());

                            dataPropMap.put(propName, prop.getObject().getLiteral());

                        }
                        accidentConcept.setDataProperties(dataPropMap);
                    }
                    accidentConceptList.add(accidentConcept);
                }
//                ConsoleLogger.print('d',"---------------");
            }
        }

        ConsoleLogger.print('d',"Key Word List: " + keyWordList);

        // Construct a cache list of key concepts from Ontology

//        for (AccidentConcept concept : accidentConceptList)
//        {
//            ConsoleLogger.print('d',"=======================================");
//            ConsoleLogger.print('d',"Concept name: " + concept.getConceptName());
//            ConsoleLogger.print('d',"Concept group: " + concept.getConceptGroup());
//            ConsoleLogger.print('d',"Category: " + concept.getCategory());
//            ConsoleLogger.print('d',"Input Group: " + concept.getInputGroup());
//            ConsoleLogger.print('d',"Data Properties " + concept.getDataProperties());
//        }
        finishedLoading = true;
    }

    // ***************** Ontology Extraction Functions *******************
    public AccidentConcept findConcept(String keyword)
    {
        for (AccidentConcept concept : accidentConceptList)
        {
            if (concept.getConceptName().contains(keyword))
            {
                ConsoleLogger.print('d',"Concept " + keyword + " Found!");
                return concept;
            }
        }
        return null;
    }

    public AccidentConcept findExactConcept(String keyword)
    {
        for (AccidentConcept concept : accidentConceptList)
        {
            if (concept.getConceptName().equals(keyword))
            {
                ConsoleLogger.print('d',"Exact Concept " + keyword + " Found!");
                return concept;
            }
        }
        return null;
    }

    public AccidentConcept findExactConceptInKeyword(String keyword)
    {
        for (AccidentConcept concept : accidentConceptList)
        {
            if (keyword.contains(concept.getConceptName()))
            {
                ConsoleLogger.print('d',"Keyword " + keyword + " contain concept " + concept.getConceptName() + "!");
                return concept;
            }
        }
        return null;
    }

    public boolean isExactConceptExist1(String keyword)
    {
        for (String concept : keyWordList)
        {
            if (concept.contains(keyword))
            {
                ConsoleLogger.print('d',"Concept " + keyword + " Exists!");
                return true;
            }
        }
        return false;
    }

    public boolean isExactConceptExist(String keyword)
    {
        for (String concept : keyWordList)
        {
            if (concept.equals(keyword))
            {
                ConsoleLogger.print('d',"Exact Concept " + keyword + " Exists!");
                return true;
            }
        }
        return false;
    }

    public int findVelocityOfAction(String action)
    {
        AccidentConcept concept = findExactConcept(action);
        if (concept != null)
        {
            return Integer.parseInt(concept.getDataProperties().get("velocity"));
        }
        else
        {
            return -1000;
        }

    }
}
