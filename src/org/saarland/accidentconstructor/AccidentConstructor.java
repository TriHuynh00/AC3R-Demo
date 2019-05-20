package org.saarland.accidentconstructor;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.TimeUnit;

import javax.swing.JFileChooser;

//import org.jdom2.Element;
import org.saarland.accidentelementmodel.ActionDescription;
import org.saarland.accidentelementmodel.NavigationDictionary;
import org.saarland.accidentelementmodel.Street;
import org.saarland.accidentelementmodel.TestCaseInfo;
import org.saarland.accidentelementmodel.VehicleAttr;
import org.saarland.configparam.AccidentParam;
import org.saarland.configparam.FilePathsConfig;
import org.saarland.crashanalyzer.CrashScenarioSummarizer;
import org.saarland.crashanalyzer.DamagedComponentAnalyzer;
import org.saarland.environmentanalyzer.EnvironmentAnalyzer;
import org.saarland.nlptools.StanfordCoreferencer;
import org.saarland.nlptools.Stemmer;
import org.saarland.ontologyparser.AccidentConcept;
import org.saarland.ontologyparser.OntologyHandler;
import org.saarland.xmlmodules.XMLAccidentCaseParser;

import com.lexicalscope.jewel.cli.ArgumentValidationException;
import com.lexicalscope.jewel.cli.CliFactory;
import com.lexicalscope.jewel.cli.Option;

public class AccidentConstructor {

    private String[] actorKeywords = { "vehicle", "pedestrian" };

    private ArrayList<VehicleAttr> vehicleList;

    private String accidentType = "";

    private OntologyHandler ontoParser;

    private TestCaseInfo testCase;

    private String[] roadWords = new String[] { "roadway", "street", "road" };

    public TestCaseInfo getTestCase() {
        return testCase;
    }

    public void setTestCase(TestCaseInfo testCase) {
        this.testCase = testCase;
    }

    public ArrayList<VehicleAttr> getVehicleList() {
        return vehicleList;
    }

    public void setVehicleList(ArrayList<VehicleAttr> vehicleList) {
        this.vehicleList = vehicleList;
    }

    public String getAccidentType() {
        return accidentType;
    }

    public void setAccidentType(String accidentType) {
        this.accidentType = accidentType;
    }

    private VehicleAttr findVehicle(String vehicleName) {
        for (VehicleAttr vehicleAttr : vehicleList) {
            if (vehicleName.replace("vehicle", "").equalsIgnoreCase("" + vehicleAttr.getVehicleId())) {
                return vehicleAttr;
            }
        }
        return null;
    }

    public AccidentConstructor(OntologyHandler parser, String accidentFilePath) {
        ontoParser = parser;
        testCase = new TestCaseInfo(accidentFilePath);
    }

    /*
     * Command line parsing interface
     */

    public interface AC3RCLI {

        @Option(defaultToNull = true, longName = "reports")
        public List<File> getReports();
    }

    public static void main(String[] args) {

        // Parsing of input parameters
        boolean useGUI = true;
        File[] selectedFiles = null;

        try {
            AC3RCLI result = CliFactory.parseArguments(AC3RCLI.class, args);
            if (result.getReports() != null && !result.getReports().isEmpty()) {
                selectedFiles = result.getReports().toArray(selectedFiles);
                useGUI = false;
            }
        } catch (ArgumentValidationException e) {
            e.printStackTrace();
            System.exit(-1);
        }

        TreeMap<String, Long> scenarioConstructionTime = new TreeMap<String, Long>();
        boolean blockSignal = true;

        // Move previous test and crash info verification files into another
        // folders
        AccidentConstructorUtil.moveFilesToAnotherFolder(FilePathsConfig.damageRecordLocation,
                FilePathsConfig.previousRecordLocation, ".log");

        AccidentConstructorUtil.moveFilesToAnotherFolder(FilePathsConfig.allTestRecordLocation,
                FilePathsConfig.prevCrashInfoRecordLocation, ".vcr");

        // AccidentParam.userFolder = System.getProperty("user.home") +
        // "\\Documents\\";
        ConsoleLogger.print('d', "User folder " + AccidentParam.userFolder);

        // BeamNGServerSocket beamNGServerSocket = new BeamNGServerSocket();
        TestCaseRunner testCaseRunner = new TestCaseRunner();

        ConsoleLogger.print('r', "Loading Ontology");

        OntologyHandler ontologyHandler = new OntologyHandler();

        try {
            ontologyHandler.readOntology();
        } catch (Exception e) {
            ConsoleLogger.print('e', "READ ONTOLOGY EXCEPTION");
            e.printStackTrace();
        }

        ConsoleLogger.print('r', "Loading Coreferencer");
        long startTimeCoref = System.nanoTime();
        StanfordCoreferencer stanfordCoreferencer = new StanfordCoreferencer();
        long endTimeCoref = (System.nanoTime() - startTimeCoref);
        ConsoleLogger.print('r', String.format("Finish Loading Coreferencer after %d seconds",
                TimeUnit.NANOSECONDS.toSeconds(endTimeCoref)));
        ConsoleLogger.print('r', "Coreferencer loaded");

        ConsoleLogger.print('r', "Please Select Crash Reports");

        if (useGUI) {

            JFileChooser fileChooser = new JFileChooser();

            fileChooser.setMultiSelectionEnabled(true);

            // Display the File Chooser to choose crash report

            int fileStat = fileChooser.showOpenDialog(null);

            if (fileStat == JFileChooser.APPROVE_OPTION) {
                selectedFiles = fileChooser.getSelectedFiles();

                for (File selectedFile : selectedFiles) {
                    ConsoleLogger.print('d', "Selected File " + selectedFile.getAbsolutePath());
                }
            }

        }
        
        if (selectedFiles == null) {
            return;
        }
        
        
        for (File selectedFile : selectedFiles) {
            String scenarioName = "";
            try {
                long startTime = System.nanoTime();
                String accidentFilePath = selectedFile.getAbsolutePath().replace("\\", "/");// FilePathsConfig.accidentFolderLocation
                                                                                            // +
                                                                                            // "CustomSideSwipe_Case1.xml";

                // TODO: check if the scenario actually exists
                scenarioName = accidentFilePath.substring(accidentFilePath.lastIndexOf("/") + 1).replace(".xml", "");

                ConsoleLogger.print('r', "\n Constructing Scenario " + scenarioName + "\n");

                AccidentConstructor accidentConstructor = new AccidentConstructor(ontologyHandler, scenarioName);
                EnvironmentAnalyzer environmentAnalyzer = new EnvironmentAnalyzer();

                XMLAccidentCaseParser xmlAccidentCaseParser = new XMLAccidentCaseParser(accidentFilePath);
                // StanfordCoreferencer stanfordCoreferencer = new
                // StanfordCoreferencer();

                LinkedList<AccidentConcept> roadConceptList = ontologyHandler.getRoadConcepts();

                ConsoleLogger.print('n', "Road Concept: ");
                for (AccidentConcept concept : roadConceptList) {
                    ConsoleLogger.print('n', concept.getConceptName() + " ");
                }
                ConsoleLogger.print('d', "");

                long startParsingFileTime = System.nanoTime();

                /*
                 * Preprocess the crash report - Normalize paragraphs in a crash
                 * report - Identify the number of cars and their properties
                 * (color, body type)
                 */
                String[] accidentContext = xmlAccidentCaseParser.parseAccidentXmlFile(accidentConstructor,
                        ontologyHandler);

                String[] storyline = accidentConstructor.replacePhrases(accidentContext[1]).split("\\.");

                long endParsingFileTime = System.nanoTime() - startParsingFileTime;

                ConsoleLogger.print('d', String.format("Finish Processing report after %d milliseconds",
                        TimeUnit.NANOSECONDS.toMillis(endParsingFileTime)));

                long startCorefTime = System.nanoTime();

                // Replace coreferenced phrases (anaphora) by the referred noun
                // (antecedent) in the paragraph.
                for (int i = 0; i < storyline.length; i++) {
                    String modSentence = stanfordCoreferencer.findCoreference(storyline[i]);
                    storyline[i] = modSentence.trim();
                }

                long endCorefTime = System.nanoTime() - startCorefTime;

                ConsoleLogger.print('d', String.format("Finish Parsing Coref after %d milliseconds",
                        TimeUnit.NANOSECONDS.toMillis(endCorefTime)));

                /******************************************
                 ****** BEGIN ENVIRONMENT ANALYSIS ********
                 ******************************************/
                long envNADStart = System.nanoTime();

                // ------- NEW BASIC ROAD ANALYZER ------
                accidentContext[0] = accidentConstructor.replacePhrases(accidentContext[0]).toLowerCase();
                accidentContext[1] = accidentConstructor.replacePhrases(accidentContext[1]).toLowerCase();

                if (System.getProperty("enableEnvironmentPropertiesConstruction") != null) {
                    ConsoleLogger.print('d', "Analyze Weather And Lighting Prop");
                    environmentAnalyzer.analyzeWeatherAndLightingProperties(accidentContext[0], ontologyHandler,
                            accidentConstructor.getTestCase(), accidentConstructor.vehicleList, stanfordCoreferencer);
                }

                environmentAnalyzer.extractBasicRoadProperties(accidentContext[0], accidentContext[1], ontologyHandler,
                        accidentConstructor.getTestCase(), accidentConstructor.vehicleList, stanfordCoreferencer);

                // if (blockSignal) continue;

                // String[] environmentParagraph = accidentContext[0].split("\\.
                // ");
                // for (int i = 0; i < environmentParagraph.length; i++) {
                // LinkedList<LinkedList<String>>
                // environmenTaggedWordsAndDependencies =
                // stanfordCoreferencer.findDependencies(environmentParagraph[i]);
                ////
                // environmentAnalyzer.extractEnvironmentProp
                // (environmenTaggedWordsAndDependencies, ontologyHandler,
                // accidentConstructor.testCase,
                // accidentConstructor.vehicleList);
                // }

                // Handle missing or empty environment properties here
                environmentAnalyzer.checkMissingEnvironmentProperties(accidentConstructor.testCase);

                accidentConstructor.testCase.printTestCaseInfo();

                ConsoleLogger.print('d', "Finish Analysing environment");

                ConsoleLogger.print('d', "Street Info in AccidentConstructor of scenario " + scenarioName);

                for (Street street : accidentConstructor.testCase.getStreetList()) {
                    ConsoleLogger.print('d', String.format(
                            "Street %s, cardinal direction %s, is_single_road %s, type %s, lane num %s, number of travel direction %s",
                            street.getStreetPropertyValue("road_ID"), street.getStreetPropertyValue("road_navigation"),
                            street.getStreetPropertyValue("is_single_road_piece"),
                            street.getStreetPropertyValue("road_type"), street.getStreetPropertyValue("lane_num"),
                            street.getStreetPropertyValue("road_direction")));
                }

                /******************************************
                 ******** END ENVIRONMENT ANALYSIS ********
                 ******************************************/

                // if (blockSignal) continue;

                /***************************************************
                 ******** BEGIN ACCIDENT DEVELOPMENT ANALYSIS ******
                 ***************************************************/

                LinkedList<LinkedList<ActionDescription>> storylineActionList = new LinkedList<LinkedList<ActionDescription>>();

                for (String sentence : storyline) {
                    if (sentence.equalsIgnoreCase("") || sentence == null) {
                        continue;
                    }
                    Stemmer stemmer = new Stemmer();
                    LinkedList<LinkedList<String>> relevantTaggedWordsAndDependencies = stanfordCoreferencer
                            .findDependencies(sentence);
                    LinkedList<ActionDescription> actionChain = accidentConstructor.extractActionChains(
                            relevantTaggedWordsAndDependencies, ontologyHandler, stemmer, environmentAnalyzer);
                    // LinkedList<LinkedList<String>>
                    // environmenTaggedWordsAndDependencies =
                    // stanfordCoreferencer.findDependencies(environmentParagraph[i]);
                    //
                    // environmentAnalyzer.extractEnvironmentProp
                    // (environmenTaggedWordsAndDependencies, ontologyHandler,
                    // accidentConstructor.testCase);
                    accidentConstructor.constructVehicleActionList(actionChain, ontologyHandler);
                }

                for (VehicleAttr vehicle : accidentConstructor.vehicleList) {
                    ConsoleLogger.print('d',
                            "Vehicle " + vehicle.getVehicleId() + " Actions : " + vehicle.getActionList().toString());
                }

                accidentConstructor.pruneActionTree(accidentConstructor.vehicleList);

                // SUPER PRUNE, APPLY ONLY TO 2 VEHICLES CASES, every action
                // after FIRST HIT ARE IGNORED
                accidentConstructor.superPrune(accidentConstructor.vehicleList);

                ConsoleLogger.print('d', "After pruning actions for scenario " + scenarioName);

                for (VehicleAttr vehicle : accidentConstructor.vehicleList) {
                    ConsoleLogger.print('d',
                            "Vehicle " + vehicle.getVehicleId() + " Actions : " + vehicle.getActionList().toString());
                }

                // if (blockSignal) continue;

                boolean foundAccidentType = false;

                ConsoleLogger.print('d', "Street List Before Constructing Coord");
                for (Street street : accidentConstructor.testCase.getStreetList()) {
                    ConsoleLogger.print('d', "Street ID " + street.getStreetProp().get("road_ID"));
                    street.printStreetInfo();
                }

                // Unknown important vehicle properties, such as traveling road,
                // are handled in this function
                accidentConstructor.checkMissingPropertiesVehicles();

                long envNADEnd = System.nanoTime() - envNADStart;
                // ConsoleLogger.print('r', String.format("Finish Extracting
                // Environment and Accident Development after %d milliseconds",
                // TimeUnit.NANOSECONDS.toMillis(envNADEnd)));
                ConsoleLogger.print('r', "\nFinish Extracting Environment and Accident Development\n");

                // if (blockSignal)
                // continue;

                /*************************************************
                 ******** END ACCIDENT DEVELOPMENT ANALYSIS ******
                 *************************************************/

                long simulationGenStartTime = System.nanoTime();

                NavigationDictionary.init();
                ConsoleLogger.print('r', "Navigation Dictionary initialized!");

                /*************************************************
                 ************ BEGIN TRAJECTORY PLANNING **********
                 *************************************************/

                // Analyze the action list of each vehicle, then create the
                // Crash Scenario based on crash type
                if (accidentConstructor.accidentType.toLowerCase().contains("rear-end")
                        || accidentConstructor.accidentType.toLowerCase().contains("rearend")
                        || accidentConstructor.accidentType.toLowerCase().contains("rear end")) {
                    foundAccidentType = true;
                    RearEndConstructor rearEndConstructor = new RearEndConstructor(accidentConstructor.vehicleList,
                            ontologyHandler, accidentConstructor.testCase);
                    rearEndConstructor.constructAccidentScenario(accidentConstructor.vehicleList, ontologyHandler);

                    // If this is a non-critical case generation, remove the
                    // impact coordinate (last coord), and set
                    // the velocity of striker as the victim's speed + 10%
                    // victim speed
                    if (AccidentConstructorUtil.getNonCriticalDistance() > 0) {
                        VehicleAttr[] strikerAndVictim = AccidentConstructorUtil.findStrikerAndVictimForRearEnd(
                                accidentConstructor.vehicleList.get(0), accidentConstructor.vehicleList.get(1),
                                AccidentParam.defaultCoordDelimiter);

                        ArrayList<String> strikerCoordList = strikerAndVictim[0].getMovementPath();
                        strikerCoordList.remove(strikerCoordList.size() - 1);

                        if (strikerAndVictim[1].getVelocity() > 0
                                && strikerAndVictim[1].getVelocity() < strikerAndVictim[0].getVelocity()) {
                            strikerAndVictim[0].setVelocity((int) (strikerAndVictim[1].getVelocity()
                                    + strikerAndVictim[1].getVelocity() * 0.1));
                        }

                    }
                } else if (accidentConstructor.accidentType.toLowerCase().contains("forward impact")) {
                    foundAccidentType = true;
                    FrontCollisionConstructor frontCollisionConstructor = new FrontCollisionConstructor();
                    frontCollisionConstructor.constructAccidentScenario(accidentConstructor.vehicleList,
                            ontologyHandler, accidentConstructor.testCase);
                } else if (accidentConstructor.accidentType.toLowerCase().contains("sideswipe")) {
                    foundAccidentType = true;
                    ConsoleLogger.print('d', "Sideswipe Accident Detected");
                    SideswipeConstructor sideswipeConstructor = new SideswipeConstructor();
                    sideswipeConstructor.constructAccidentScenario(accidentConstructor.vehicleList, ontologyHandler,
                            accidentConstructor.testCase);
                } else // if
                       // (accidentConstructor.accidentType.contains("straight
                       // paths"))
                {
                    foundAccidentType = true;
                    ConsoleLogger.print('d', "Straight paths Accident Detected");
                    StraightPathConstructor straightPathConstructor = new StraightPathConstructor();
                    straightPathConstructor.constructAccidentScenario(accidentConstructor.vehicleList, ontologyHandler,
                            accidentConstructor.testCase);
                }

                /*************************************************
                 ************ END TRAJECTORY PLANNING ************
                 *************************************************/

                long simulationGenEndTime = System.nanoTime() - simulationGenStartTime;
                ConsoleLogger.print('d', String.format("Finish Generating simulation after %d milliseconds",
                        TimeUnit.NANOSECONDS.toMillis(simulationGenEndTime)));

                long simulationConstructionStartTime = System.nanoTime();

                /*************************************************
                 ************ BEGIN SCENARIO CONSTRUCTION ********
                 *************************************************/

                // This part afterward, is to construct the scenario from the
                // environment props and vehicle coordinates
                if (foundAccidentType) {
                    if (AccidentConstructorUtil.getNonCriticalDistance() > 0) {
                        scenarioName = scenarioName + "_non-critical";
                    }
                    // scenarioName =
                    // accidentConstructor.testCase.getName().split("/")[1].split("\\.")[0];

                    // ConsoleLogger.print('d', "Scenario Name Split for /: " +
                    // scenarioName.split("/")[1].split("\\.")[0]);
                    RoadConstructor roadConstructor = new RoadConstructor(accidentConstructor.vehicleList,
                            accidentConstructor.testCase, ontologyHandler);

                    // Construct the road, vehicle, and waypoints objects
                    String scenarioInfo = roadConstructor.constructRoadNodes(scenarioName);
                    if (scenarioInfo.equals("fail")) {
                        ConsoleLogger.print('d', "Fail to construct road due to same first coord");

                        throw new Exception("Fail constructing road due to same first coord");
                    }

                    // Construct environment props
                    EnvironmentConstructor environmentConstructor = new EnvironmentConstructor(
                            accidentConstructor.testCase, ontologyHandler);

                    scenarioInfo += environmentConstructor.contructEnvironmentObjects();

                    ConsoleLogger.print('d', scenarioInfo);

                    scenarioInfo += ("\n };");
                    byte[] buffer = scenarioInfo.toString().getBytes();

                    // Write Result to File
                    ConsoleLogger.print('d', "Scenario Name " + scenarioName);
                    Path finalResultPath = Paths
                            .get(AccidentParam.finalResultLocation + "\\" + scenarioName + ".prefab");
                    Files.write(finalResultPath, buffer);

                    // Construct Scenario Config file
                    String scenarioTemplateFile = AccidentConstructorUtil
                            .loadTemplateContent(AccidentParam.scenarioJsonFilePath);

                    VehicleAttr[] strikerAndVictim = new VehicleAttr[2];

                    // Find the right striker and victim ID. The striker vehicle
                    // is assigned to drive the ego-car
                    if (accidentConstructor.accidentType.contains("rear-end")
                            || accidentConstructor.accidentType.contains("rearend")
                            || accidentConstructor.accidentType.contains("rear end")) {
                        strikerAndVictim = AccidentConstructorUtil.findStrikerAndVictimForRearEnd(
                                accidentConstructor.vehicleList.get(0), accidentConstructor.vehicleList.get(1),
                                AccidentParam.beamngCoordDelimiter);
                    } else {
                        strikerAndVictim = AccidentConstructorUtil.findStrikerAndVictim(
                                accidentConstructor.vehicleList.get(0), accidentConstructor.vehicleList.get(1));
                    }
                    scenarioTemplateFile = scenarioTemplateFile.replace("$name", scenarioName);

                    int strikerLaneNum = Integer
                            .parseInt(strikerAndVictim[0].getStandingStreet().getStreetPropertyValue("lane_num"));

                    String roadDirection = strikerAndVictim[0].getStandingStreet()
                            .getStreetPropertyValue("road_direction");

                    // if roadDirection is 2-way, divide the total lane number
                    // by 2 and get the ceiling
                    ConsoleLogger.print('d', String.format("roadDirection of %d is null ? %s, if false it is %s",
                            strikerAndVictim[0].getVehicleId(), (roadDirection.equals("")), roadDirection));
                    if (roadDirection.equals("2-way") || roadDirection.equals("")) {
                        strikerLaneNum = (int) Math.ceil(strikerLaneNum / 2.0);
                    }

                    ConsoleLogger.print('d',
                            String.format("strikerLaneNum is %d ", (int) Math.ceil(strikerLaneNum / 2.0)));

                    double speedLimit = -1;
                    // Record the speed limit in the scenario's JSON file, if
                    // speed_limit is not specified, set it as -1
                    if (!strikerAndVictim[0].getStandingStreet().getStreetPropertyValue("speed_limit").equals("")) {
                        speedLimit = AccidentConstructorUtil.convertMPHToKMPH(Double.parseDouble(
                                strikerAndVictim[0].getStandingStreet().getStreetPropertyValue("speed_limit")));
                    }

                    scenarioTemplateFile = scenarioTemplateFile
                            .replace("$description", accidentContext[0] + "\n" + accidentContext[1])
                            .replace("$strikerID", strikerAndVictim[0].getVehicleId() + "")
                            .replace("$NLanes", strikerLaneNum + "").replace("$speedLimit", "" + speedLimit);

                    String scenarioPath = AccidentParam.scenarioConfigFilePath + "\\" + scenarioName + ".json";

                    Path scenarioConfigPath = Paths.get(scenarioPath);
                    Files.write(scenarioConfigPath, scenarioTemplateFile.getBytes());

                    testCaseRunner.setScenarioName(scenarioName);
                }

                /*************************************************
                 ************ END SCENARIO CONSTRUCTION **********
                 *************************************************/

                long endTime = (System.nanoTime() - startTime);
                ConsoleLogger.print('r', String.format("Finish generating simulation %s after %d milliseconds\n",
                        scenarioName, TimeUnit.NANOSECONDS.toMillis(endTime)));
                scenarioConstructionTime.put(scenarioName, TimeUnit.NANOSECONDS.toMillis(endTime));
                ConsoleLogger.print('d', "Final Street List");

                for (Street street : accidentConstructor.testCase.getStreetList()) {
                    ConsoleLogger.print('d', "Street ID " + street.getStreetProp().get("road_ID"));
                    street.printStreetInfo();
                }

                ConsoleLogger.print('d', "Waypoints track");
                for (VehicleAttr vehicle : accidentConstructor.vehicleList) {
                    ConsoleLogger.print('d',
                            "Vehicle " + vehicle.getVehicleId() + " track : " + vehicle.getMovementPath().toString());
                }

                long scenarioStartTime = System.nanoTime();

                /************ BEGIN SCENARIO EXECUTION ***********/

                boolean hasCrash = testCaseRunner.runScenario(scenarioName);

                // Add BeamNG Server Socket handling here

                DamagedComponentAnalyzer crashAnalyzer = new DamagedComponentAnalyzer(accidentConstructor.vehicleList,
                        ontologyHandler, scenarioName);

                crashAnalyzer.checkWhetherCrashOccur(hasCrash);

                /************ END SCENARIO EXECUTION ***********/
                long scenarioEndTime = System.nanoTime() - scenarioStartTime;
                ConsoleLogger.print('r', String.format("Finish running simulation after %d seconds\n",
                        TimeUnit.NANOSECONDS.toSeconds(scenarioEndTime)));

            } catch (Exception e) {

                // If there is an exception during the scenario generation
                // process, set the case as generation failure
                DamagedComponentAnalyzer crashAnalyzer = new DamagedComponentAnalyzer(null, scenarioName);
                crashAnalyzer.markGenerationFailureCase();
                ConsoleLogger.print('r', "Error in generating case " + scenarioName);
                e.printStackTrace();
                continue;
            }
        }

        // Summarize the number of total / partial / no match, and generation
        // failure scenarios
        CrashScenarioSummarizer csr = new CrashScenarioSummarizer();
        csr.summarizeAllScenarios();

        long total = 0;
        for (String key : scenarioConstructionTime.keySet()) {
            ConsoleLogger.print('d', key + "," + scenarioConstructionTime.get(key));
            total += scenarioConstructionTime.get(key);
        }
        ConsoleLogger.print('d', "Average: " + total / scenarioConstructionTime.size() + " ms");

    }

    // *********** ACCIDENT ANALYZER ***************
    // Extract the actions of vehicles from the crash reports
    private LinkedList<ActionDescription> extractActionChains(
            LinkedList<LinkedList<String>> relevantTaggedWordsAndDependencies, OntologyHandler parser, Stemmer stemmer,
            EnvironmentAnalyzer environmentAnalyzer) {

        ConsoleLogger.print('d', "Tagged Word List");
        LinkedList<String> tagWordList = relevantTaggedWordsAndDependencies.get(0);
        ConsoleLogger.print('d', tagWordList.toString());

        LinkedList<ActionDescription> actionList = new LinkedList<ActionDescription>();

        LinkedList<String> dependencyList = relevantTaggedWordsAndDependencies.get(1);

        DamagedComponentAnalyzer damagedComponentAnalyzer = new DamagedComponentAnalyzer(vehicleList, parser,
                testCase.getName());

        String mainActorInSentence = "";

        for (String dependency : dependencyList) {
            try {
                ConsoleLogger.print('d', "Analyze Accident Dependency " + dependency);

                // Looking at nsubj to find action of actor
                if (dependency.startsWith("nsubj")) {

                    // Get actor and action pair, then put it in
                    // ActionDescription
                    String[] actionAndActor = AccidentConstructorUtil.getWordPairFromDependency(dependency);

                    ActionDescription actionDescription = new ActionDescription();

                    String action = AccidentConstructorUtil.getWordFromToken(actionAndActor[0]);
                    String actor = AccidentConstructorUtil.getWordFromToken(actionAndActor[1]);
                    ConsoleLogger.print('d', "First found Actor is " + actor);
                    // If the actor does not follow "vehicle\d" pattern, then
                    // try to find whether this actor is a vehicle
                    if (!actor.startsWith("vehicle") || !actor.startsWith("pedestrian")) {
                        // Perhaps this is a vehicle name, find in the
                        // yearMakeModel value to determine the right vehicle
                        boolean hiddenVehicleFound = false;
                        for (VehicleAttr vehicleAttr : vehicleList) {
                            if (vehicleAttr.getYearMakeModel().toLowerCase().contains(actor.toLowerCase())) {
                                actor = "vehicle" + vehicleAttr.getVehicleId();
                                hiddenVehicleFound = true;
                                break;
                            }
                        }

                        // If not a vehicle name, // Find the real actor behind
                        // the subject
                        if (!hiddenVehicleFound) {
                            for (String actorDependency : dependencyList) {
                                if (actorDependency.startsWith("nmod:of") && actorDependency.contains(actionAndActor[1])
                                        && (actorDependency.contains("vehicle")
                                                || actorDependency.contains("pedestrian"))) {
                                    String[] actorWordPair = AccidentConstructorUtil
                                            .getWordPairFromDependency(actorDependency);
                                    if (actorWordPair[1].startsWith("vehicle")) {
                                        actor = AccidentConstructorUtil.getWordFromToken(actorWordPair[1]);
                                        break;
                                    }
                                }
                            }
                        } // End process not a vehicle name
                    }

                    // Only proceed this relationship if we have a valid actor,
                    // for now.
                    ConsoleLogger.print('d', "Actor is " + actor);
                    if (actor.startsWith("vehicle") || actor.startsWith("pedestrian")) {
                        mainActorInSentence = actor;
                        actionDescription.setSubject(actor);

                        int actionWordIndex = AccidentConstructorUtil.getPositionFromToken(actionAndActor[0]);
                        // int actionWordIndex =
                        // Integer.parseInt(actionAndActor[0].split("-")[1]);

                        ConsoleLogger.print('d', "ActionWordIndex of " + actionAndActor[0] + " : " + actionWordIndex);

                        ConsoleLogger.print('d', "TagWordList get " + (actionWordIndex - 1) + " : "
                                + tagWordList.get(actionWordIndex - 1));

                        // If the assumed action is not a verb, then find a verb
                        // from the connected dependencies
                        if (!tagWordList.get(actionWordIndex - 1).split("/")[1].startsWith("VB")) {
                            LinkedList<String> connectedDependencies = AccidentConstructorUtil
                                    .findConnectedDependencies(dependencyList, tagWordList, actionAndActor[0],
                                            dependency, 1);
                            ConsoleLogger.print('d', "Connected Dependencies: ");
                            for (String connectedDependency : connectedDependencies) {
                                ConsoleLogger.print('d', connectedDependencies);

                                // Check if the second word is not the assumed
                                // action
                                String[] wordPair = AccidentConstructorUtil
                                        .getWordPairFromDependency(connectedDependency);

                                if (!wordPair[1].equalsIgnoreCase(actionAndActor[0])) {
                                    action = wordPair[1].split("-")[0];
                                }

                                // Try to see if we can infer direction from
                                // misinterpreted action
                                String direction = AccidentConstructorUtil.getWordFromToken(actionAndActor[0]);
                                ConsoleLogger.print('d', "direction " + direction);
                                ConsoleLogger.print('d', "Direction concept " + parser.findExactConcept(direction));

                                AccidentConcept directionConcept = parser.findExactConcept(direction);
                                if (directionConcept != null
                                        && directionConcept.getLeafLevelName().equals("vehicle_direction")) {
                                    VehicleAttr travellingVehicle = findVehicle(actor);
                                    if (travellingVehicle != null) {
                                        assignDirectionToRoad(direction, travellingVehicle);
                                        // travellingVehicle.setStandingStreet(rightDirectionStreet);

                                    } // End checking if the actor can be found
                                } // End processing travelling direction
                            } // End looping through indirect dependencies
                            ConsoleLogger.print('d', "End Connected Dependencies: ");
                        }

                        String stemmedAction = stemmer.stem(action).trim();

                        ConsoleLogger.print('d', "Stem of " + action + " is " + stemmedAction);

                        actionDescription.setVerb(stemmedAction);

                        ConsoleLogger.print('d',
                                "Find Concept of " + action + " is " + parser.isExactConceptExist(stemmedAction));

                        // For the word make or made, check if this is a made or
                        // make contact
                        if (stemmedAction.equals("made") || stemmedAction.equals("make")) {
                            String linkedWords = AccidentConstructorUtil.findAllConnectedWords(dependencyList,
                                    stemmedAction, "", 0, 1);

                            if (linkedWords.contains("contact")) {
                                ConsoleLogger.print('d', stemmedAction + " contact found in " + linkedWords);
                                stemmedAction = "contact";
                                actionDescription.setVerb(stemmedAction);
                            }
                        }

                        // If the action exists, find (possible adverb) and
                        // objects
                        if (parser.isExactConceptExist(stemmedAction)) {
                            // actionList.add(actionDescription);

                            LinkedList<String> verbPropList = new LinkedList<String>();

                            // If it is a travel action, find direction
                            if (stemmedAction.equals("travel")) {

                                // Find the lane number which this vehicle is
                                // travelling on
                                VehicleAttr actorVehicleObj = findVehicle(actor);
                                if (actorVehicleObj != null
                                        && actorVehicleObj.getTravelOnLaneNumber() == AccidentParam.RIGHTMOSTLANE) {

                                    String actConnectedWords = AccidentConstructorUtil
                                            .findAllConnectedWords(dependencyList, action, action, 0, 5);

                                    ConsoleLogger.print('d',
                                            "Connected words of " + action + " is " + actConnectedWords);

                                    actorVehicleObj.setTravelOnLaneNumber(
                                            AccidentConstructorUtil.detectTravellingLane(actConnectedWords));

                                    ConsoleLogger.print('d',
                                            "Travelling lane of vehicle " + actorVehicleObj.getVehicleId() + " is "
                                                    + actorVehicleObj.getTravelOnLaneNumber());
                                } // End Finding Travelling lane

                                LinkedList<String> directionDependencies = AccidentConstructorUtil
                                        .findConnectedDependencies(dependencyList, tagWordList, stemmedAction, "", 0);

                                for (String directionDependency : directionDependencies) {

                                    String[] directionWordPair = AccidentConstructorUtil
                                            .getWordPairFromDependency(directionDependency);

                                    String word0 = AccidentConstructorUtil.getWordFromToken(directionWordPair[0]);
                                    String word1 = AccidentConstructorUtil.getWordFromToken(directionWordPair[1]);

                                    ConsoleLogger.print('d', "Direction dependency " + directionDependency + " word0 "
                                            + word0 + " word1 " + word1);

                                    AccidentConcept word0Concept = parser.findExactConcept(stemmer.stem(word0));
                                    AccidentConcept word1Concept = parser.findExactConcept(stemmer.stem(word1));

                                    // Check if the dependency contains moving
                                    // direction
                                    if (word0Concept != null && word1Concept != null) {
                                        String directionWord = "";
                                        // Find the dependency
                                        if (word0Concept.getLeafLevelName().equals("vehicle_direction")) {
                                            directionWord = word0;
                                        } else if (word1Concept.getLeafLevelName().equals("vehicle_direction")) {
                                            directionWord = word1;
                                        }
                                        ConsoleLogger.print('d',
                                                "Found direction word attached to travel " + directionWord);

                                        ConsoleLogger.print('d', "Actor is " + actor);

                                        // If the actor contains vehicle number,
                                        // set the travelling action to that
                                        // vehicle
                                        if (actor.matches("vehicle\\d") && !directionWord.equals("")) {
                                            VehicleAttr vehicleRef = findVehicle(actor.replace("vehicle", ""));

                                            String direction = AccidentConstructorUtil
                                                    .convertDirectionWordToDirectionLetter(directionWord);
                                            vehicleRef.setTravellingDirection(direction);
                                            // Set standing street based on
                                            // direction

                                            // If there is only 1 street found,
                                            // assign the direction of the
                                            // vehicle to the
                                            // road, if the street has no
                                            // specified direction
                                            if (testCase.getStreetList().size() == 1) {
                                                Street onlyStreet = testCase.getStreetList().get(0);

                                                if (onlyStreet.getStreetPropertyValue("road_navigation").equals("")) {
                                                    onlyStreet.putValToKey("road_navigation", direction);
                                                    vehicleRef.setStandingStreet(onlyStreet);
                                                }
                                            }

                                            // Else, loop through the road and
                                            // hopefully we got the right road
                                            // assigned to
                                            // the right car in the end
                                            else {
                                                for (Street street : testCase.getStreetList()) {
                                                    if (street.getStreetPropertyValue("road_navigation")
                                                            .equals(direction)) {
                                                        vehicleRef.setStandingStreet(street);
                                                        break;
                                                    }
                                                }
                                            }

                                        } // End assign direction to actor
                                    } // End checking if both concepts are null
                                      // or not

                                } // End looping through the direction
                                  // dependency

                            }
                            // Processing direction + "bound" travel word
                            else if (stemmedAction.matches("\\w+bound")) {
                                ConsoleLogger.print('d', "Processing [direction]bound travel action " + stemmedAction);
                                // If the actor contains vehicle number, set the
                                // travelling action to that vehicle
                                if (actor.matches("vehicle\\d") && !stemmedAction.equals("")) {
                                    VehicleAttr vehicleRef = findVehicle(actor);
                                    assignDirectionToRoad(stemmedAction, vehicleRef);
                                }
                            }

                            // Process impact action, attempt to infer the
                            // damaged side of involved vehicle
                            else {
                                AccidentConcept actionConcept = parser.findExactConcept(stemmedAction);
                                // Check if action is a vehicle action
                                if (actionConcept != null
                                        && actionConcept.getLeafLevelName().equals("vehicle_action")) {
                                    HashMap<String, String> actionDataProp = actionConcept.getDataProperties();
                                    // Check if the action is a collision action
                                    if (actionDataProp.get("is_collision_verb").equalsIgnoreCase("true")) {

                                        // Find the vehicle name, attempt to
                                        // find the damaged sidde
                                        if (actor.matches("vehicle\\d+")) {
                                            String wordChain = AccidentConstructorUtil
                                                    .findAllConnectedWords(dependencyList, actor, actor, 0, 2);

                                            for (String elem : wordChain.split(",")) {
                                                AccidentConcept elemConcept = ontoParser.findExactConcept(
                                                        AccidentConstructorUtil.getWordFromToken(elem));

                                                if (elemConcept != null && elemConcept.getLeafLevelName()
                                                        .equals("vehicle_impact_side")) {
                                                    String finalVictimDmgSide = damagedComponentAnalyzer
                                                            .findSideOfCrashedComponents(dependencyList, elem, actor,
                                                                    tagWordList);

                                                    ConsoleLogger.print('d', "Final victim damaged side in nsubjpass "
                                                            + finalVictimDmgSide);
                                                }
                                            }

                                        } else {
                                            // Check if this is a vehicle impact
                                            // side
                                            AccidentConcept actorConcept = parser.findExactConcept(actor);
                                            if (actorConcept != null && actorConcept.getLeafLevelName()
                                                    .equalsIgnoreCase("vehicle_impact_side")) {
                                                ConsoleLogger.print('d',
                                                        "In subjpass known actor damaged side " + actor);
                                            }

                                        }
                                    } // end checking action is a collision verb
                                }
                            }

                            // Find dependencies that contain the verb
                            for (String dependencyOfAction : dependencyList) {

                                // If the supposed action is not an action,
                                // replace with an action
                                if (dependencyOfAction.contains(actionAndActor[0])) {
                                    // Skip the same nsub dependency
                                    if (dependencyOfAction.equalsIgnoreCase(dependency)) {
                                        continue;
                                    } else // Process the dependency
                                    {
                                        String[] wordPair = AccidentConstructorUtil
                                                .getWordPairFromDependency(dependencyOfAction);
                                        String relatedWordWithIndex = wordPair[1].trim();
                                        // If the first word in the pair is not
                                        // the action, set it as related word
                                        if (wordPair[1].split("-")[0].equalsIgnoreCase(action)) {
                                            relatedWordWithIndex = wordPair[0];
                                        }

                                        String relatedWord = relatedWordWithIndex.split("-")[0];
                                        ConsoleLogger.print('d', "Related Word " + relatedWord);

                                        AccidentConcept relatedWordConcept = ontoParser.findExactConcept(relatedWord);

                                        // Attempt to detect the side of the
                                        // parking action
                                        if (stemmedAction.equals("park")) {
                                            ConsoleLogger.print('d', "Found park action ");
                                            // Find from related word the park
                                            // location (left or right of the
                                            // road)
                                            LinkedList<String> relatedWordDependencies = AccidentConstructorUtil
                                                    .findConnectedDependencies(dependencyList, tagWordList,
                                                            relatedWordWithIndex, dependencyOfAction, 0);

                                            VehicleAttr actingVehicle = findVehicle(actor);

                                            // If the related word is a pavement
                                            // type, record the data
                                            if (relatedWordConcept != null
                                                    && relatedWordConcept.getLeafLevelName().equals("pavement")) {
                                                ConsoleLogger.print('d', "Found pavement type " + relatedWord);

                                                if (actingVehicle.getStandingStreet() != null) {
                                                    actingVehicle.getStandingStreet().putValToKey("pavement_type",
                                                            relatedWord);
                                                }
                                                // If there is only 1 road,
                                                // assign the pavement type to
                                                // that road
                                                else if (testCase.getStreetList().size() == 1) {
                                                    testCase.getStreetList().get(0).putValToKey("pavement_type",
                                                            relatedWord);
                                                }
                                            }
                                            // Find the word "left" or "right"
                                            for (String relatedWordDependency : relatedWordDependencies) {
                                                String[] relatedWordDepWordPair = AccidentConstructorUtil
                                                        .getWordPairFromDependency(relatedWordDependency);

                                                String word1 = AccidentConstructorUtil
                                                        .getWordFromToken(relatedWordDepWordPair[0]);
                                                String word2 = AccidentConstructorUtil
                                                        .getWordFromToken(relatedWordDepWordPair[1]);

                                                if (word1.equals("left") || word2.equals("left")) {
                                                    actingVehicle.setStandingRoadSide("left");
                                                } else if (word1.equals("right") || word2.equals("right")) {
                                                    actingVehicle.setStandingRoadSide("right");
                                                }

                                                ConsoleLogger.print('d', "Determined standing road side: "
                                                        + actingVehicle.getStandingRoadSide());

                                            }

                                        } // End analyzing park action

                                        // Detect travelling distance
                                        if (relatedWord.matches("\\d*m")) {
                                            verbPropList.add(relatedWord);
                                            ConsoleLogger.print('d', "Found traveling distance = " + relatedWord);
                                        }

                                        // Scan at most three times
                                        if (parser.isExactConceptExist(relatedWord)) {
                                            if (!verbPropList.contains(relatedWordWithIndex)) {
                                                verbPropList.add(relatedWordWithIndex);
                                            }
                                            for (String dependencyOfRelatedWord : dependencyList) {

                                                if (dependencyOfRelatedWord.equalsIgnoreCase(dependencyOfAction)
                                                        || dependencyOfRelatedWord.equalsIgnoreCase(dependency)) {
                                                    continue;
                                                }

                                                if (dependencyOfRelatedWord.contains(relatedWordWithIndex)) {

                                                    String[] wordPairOfRelatedWord = AccidentConstructorUtil
                                                            .getWordPairFromDependency(dependencyOfRelatedWord);
                                                    // wordPairOfRelatedWord =
                                                    // wordPairOfRelatedWord[1].replace(")",
                                                    // "").split(",");
                                                    ConsoleLogger.print('d',
                                                            "Word Pair Related Word 0 " + wordPairOfRelatedWord[0]);
                                                    ConsoleLogger.print('d', "Word Pair Related Word 1 "
                                                            + wordPairOfRelatedWord[1].trim());
                                                    String otherWord = "";

                                                    // Find if there is a speed
                                                    // being attached to this
                                                    // action
                                                    if (relatedWordWithIndex.startsWith("mph")
                                                            && dependencyOfRelatedWord.startsWith("nummod")) {
                                                        // Extract mph speed
                                                        String[] measureUnitAndVelocity = AccidentConstructorUtil
                                                                .getWordPairFromDependency(dependencyOfRelatedWord);
                                                        String travelingVelocity = AccidentConstructorUtil
                                                                .getWordFromToken(measureUnitAndVelocity[1]);
                                                        if (travelingVelocity.contains("-")) {
                                                            travelingVelocity = travelingVelocity.split("-")[1];
                                                        }
                                                        verbPropList.add("mph:" + travelingVelocity);
                                                        ConsoleLogger.print('d', "Found traveling velocity = "
                                                                + travelingVelocity + " mph");
                                                        // if the actor is a
                                                        // vehicle, extract the
                                                        // ID and set the speed
                                                        // to the vehicle
                                                        if (actor.matches("vehicle\\d*")) {
                                                            int actorID = Integer
                                                                    .parseInt(actor.replace("vehicle", ""));
                                                            VehicleAttr actorObject = AccidentConstructorUtil
                                                                    .findVehicleBasedOnId(actorID, vehicleList);
                                                            actorObject
                                                                    .setVelocity(Integer.parseInt(travelingVelocity));
                                                            ConsoleLogger.print('d', "Add speed " + travelingVelocity
                                                                    + " to vehicle#" + actorObject.getVehicleId());
                                                        }

                                                    }

                                                    // Find the otherWord
                                                    otherWord = AccidentConstructorUtil.getOtherWordInDep(
                                                            wordPairOfRelatedWord[0], wordPairOfRelatedWord);
                                                    otherWord = AccidentConstructorUtil.getWordFromToken(otherWord);
                                                    // if
                                                    // (wordPairOfRelatedWord[0].equalsIgnoreCase(relatedWordWithIndex))
                                                    // {
                                                    // otherWord =
                                                    // wordPairOfRelatedWord[1].trim();
                                                    // } else {
                                                    // otherWord =
                                                    // wordPairOfRelatedWord[0];
                                                    // }
                                                    // TODO: May need to add
                                                    // pedestrian here
                                                    ConsoleLogger.print('d', "Other word is " + otherWord);
                                                    if (otherWord.startsWith("vehicle")) {
                                                        for (VehicleAttr vehicleAttr : vehicleList) {
                                                            String vehicleName = "vehicle" + vehicleAttr.getVehicleId();
                                                            if (otherWord.split("-")[0].equalsIgnoreCase(vehicleName)) {
                                                                ConsoleLogger.print('d', "Vehicle Name " + vehicleName);
                                                                // If the Second
                                                                // word is the
                                                                // subject,
                                                                // append to the
                                                                // subject
                                                                // section
                                                                if (vehicleName.equalsIgnoreCase(
                                                                        actionDescription.getSubject())) {
                                                                    actionDescription
                                                                            .setSubject(actionDescription.getSubject()
                                                                                    + " " + relatedWordWithIndex);
                                                                } else // Add
                                                                       // the
                                                                       // vehicle
                                                                       // into
                                                                       // obj
                                                                       // list
                                                                {
                                                                    verbPropList.add(vehicleName + "<>" + relatedWord);
                                                                    verbPropList.remove(relatedWordWithIndex);
                                                                }
                                                                break;
                                                            }
                                                        }
                                                    }
                                                    // Processing other
                                                    // filtering here
                                                    else if (parser.isExactConceptExist(otherWord)) {
                                                        AccidentConcept conceptOfOtherWord = parser
                                                                .findExactConcept(otherWord);

                                                        // If this is a vehicle
                                                        // direction, infer the
                                                        // road the vehicle
                                                        // stands
                                                        if (conceptOfOtherWord.getLeafLevelName()
                                                                .equals("vehicle_direction")) {
                                                            VehicleAttr travellingVehicle = findVehicle(actor);

                                                            if (travellingVehicle != null) {
                                                                assignDirectionToRoad(otherWord, travellingVehicle);
                                                            } // End checking if
                                                              // a vehicle is
                                                              // detected
                                                        } // End checking
                                                          // vehicle direction
                                                        else if (!otherWord.startsWith("mph")) {
                                                            verbPropList.add(otherWord);
                                                        }

                                                    }
                                                } // End checking if this
                                                  // dependency contains the
                                                  // related word
                                            } // End looping through
                                              // dependencies of related word

                                        } // End checking the relation of a word
                                          // to a verb
                                    } // End else of processing the depedencies
                                } // End checking dependency has target action
                            } // End for dependency loop
                            actionDescription.setVerbProps(verbPropList);
                            // If the vehicle ID is not specified, when there
                            // are less than 2 vehicles, assume vehicle1 is the
                            // striker
                            if (actor.equalsIgnoreCase("vehicle")) {
                                if (vehicleList.size() <= 2) {
                                    actor = actor + "1";
                                    actionDescription.setSubject(actor);
                                }
                            } else if (actor.equalsIgnoreCase("vehicles")) {
                                if (vehicleList.size() <= 2) {
                                    ConsoleLogger.print('d', "Handle Both Vehicles case");

                                    actionDescription.setSubject("vehicle1 vehicle2");
                                }
                            }
                            actionList.add(actionDescription);

                        } // End checking if the action exists in the Ontology
                    } // End Check if the actor is pedestrian or vehicle
                    else {
                        // EnvironmentAnalyzer environmentAnalyzer = new
                        // EnvironmentAnalyzer();
                        AccidentConcept actorConcept = parser.findExactConcept(actor);
                        Street currentStreet = null;

                        boolean roadTypeDetected = false;
                        String roadWordInThisSentence = "";

                        // Check the entire string to see if road word exists
                        for (int k = 0; k < roadWords.length; k++) {
                            String roadTypeStr = roadWords[k];
                            if (tagWordList.contains(roadTypeStr)) {
                                ConsoleLogger.print('d', "Found road_type word " + roadTypeStr);
                                roadWordInThisSentence = roadTypeStr;
                                roadTypeDetected = true;
                                break;
                            }
                        }

                        // Find the determinant of the road_type word, a = new
                        // street, the = last street
                        if (roadTypeDetected) {
                            for (int k = 0; k < tagWordList.size(); k++) {
                                // Locate the road word
                                if (tagWordList.get(k).startsWith(roadWordInThisSentence)) {
                                    // Find the determinant by scanning the last
                                    // 7 words
                                    for (int s = k; s > k - 7 || s == 0; s--) {
                                        String tagWord = tagWordList.get(s);
                                        if (tagWord.endsWith("/DT")) {
                                            if (tagWord.startsWith("a")) {
                                                ConsoleLogger.print('d', "Find a DT ");
                                                currentStreet = testCase.createNewStreet();
                                            } else if (tagWord.startsWith("the")) {
                                                // TODO: Need to find street
                                                // property to determine the
                                                // right road
                                                currentStreet = testCase.getStreetList()
                                                        .get(testCase.getStreetList().size());
                                            }
                                        }
                                    } // End finding DT
                                    break;
                                } // End finding road_type word
                            } // End looping through tagWordList
                        } // End identifying street if road word exists

                        // Check if this actor is an environment property
                        if (actorConcept != null && actorConcept.getInputGroup().equals("environment_properties")) {
                            if (actorConcept.getLeafLevelName().equals("road_shape")) {
                                // If there is no street avail, create a new one
                                if (!roadTypeDetected) {
                                    if (testCase.getStreetList().size() == 0) {
                                        currentStreet = testCase.createNewStreet();
                                    } else {
                                        currentStreet = environmentAnalyzer.getPreviousStreet();
                                    }
                                }

                                environmentAnalyzer.analyzeRoadShape(actor, testCase, dependencyList, parser,
                                        currentStreet);
                            }
                        }
                    }
                } // End check if dependency starts with nsubj
                  // If we cannot find the impact from the actor, try to search
                  // from the collided object
                  // TODO: Check if the impact action is already found in this
                  // sentence
                else if (dependency.startsWith("dobj")) {
                    String[] wordPair = AccidentConstructorUtil.getWordPairFromDependency(dependency);

                    // Check if there is a vehicle action in this dependency
                    for (int m = 0; m < wordPair.length; m++) {
                        String word = wordPair[m];
                        String stemmedWord = stemmer.stem(AccidentConstructorUtil.getWordFromToken(word));
                        AccidentConcept wordConcept = parser.findExactConcept(stemmedWord);

                        // Found action word
                        if (wordConcept != null && wordConcept.getLeafLevelName().equals("vehicle_action")) {
                            wordPair[m] = word.replace(AccidentConstructorUtil.getWordFromToken(word), stemmedWord);
                        } else // Find any possible relation between this word
                               // and the name of a vehicle
                        {
                            ConsoleLogger.print('d', "Checking Names of word " + word);
                            String suspectVehicleName = AccidentConstructorUtil.getWordFromToken(word);
                            for (VehicleAttr anyVehicle : vehicleList) {
                                if (anyVehicle.getYearMakeModel().contains(suspectVehicleName)) {
                                    // Check apostrophe to ensure that this is
                                    // indeed the name of a car
                                    LinkedList<String> relatedDeps = AccidentConstructorUtil.findConnectedDependencies(
                                            dependencyList, tagWordList, word, dependency, 0);

                                    for (String dep : relatedDeps) {

                                        if (dep.contains("appos") && dep.contains("vehicle")) {
                                            String vehicleWord = AccidentConstructorUtil.getOtherWordInDep(word,
                                                    AccidentConstructorUtil.getWordPairFromDependency(dep));

                                            ConsoleLogger.print('d', "Vehicle word appos is " + vehicleWord);

                                            if (vehicleWord.matches("vehicle\\d+-\\d+")) {
                                                wordPair[m] = vehicleWord;

                                                // Find the attacker
                                                String attacker = findSuspectedVehicle(dependencyList, tagWordList,
                                                        suspectVehicleName, damagedComponentAnalyzer);
                                                VehicleAttr attackerVehAttr = findVehicle(attacker);
                                                if (attackerVehAttr.getDamagedComponents().size() == 0) {
                                                    ConsoleLogger.print('d', "Find all connected words " + attacker);
                                                    String wordChain = AccidentConstructorUtil.findAllConnectedWords(
                                                            dependencyList, attacker, attacker, 0, 2);
                                                    ConsoleLogger.print('d', "Word chain " + wordChain);

                                                    // Attempt to guess the
                                                    // damaged side and
                                                    // components
                                                    for (String elem : wordChain.split(",")) {
                                                        AccidentConcept elemConcept = ontoParser.findExactConcept(
                                                                AccidentConstructorUtil.getWordFromToken(elem));

                                                        if (elemConcept != null && elemConcept.getLeafLevelName()
                                                                .equals("vehicle_impact_side")) {
                                                            String finalAttackerDmgSide = damagedComponentAnalyzer
                                                                    .findSideOfCrashedComponents(dependencyList, elem,
                                                                            attacker, tagWordList);

                                                            ConsoleLogger.print('d', "Final attacker damaged side "
                                                                    + finalAttackerDmgSide);
                                                        }
                                                    }
                                                }

                                            }
                                        }
                                    }
                                }
                            } // end looping through vehicle
                        }
                        ConsoleLogger.print('d', "Word " + m + " is " + wordPair[m]);
                    }

                    AccidentConcept conceptWord0 = parser
                            .findExactConcept(AccidentConstructorUtil.getWordFromToken(wordPair[0]));
                    AccidentConcept conceptWord1 = parser
                            .findExactConcept(AccidentConstructorUtil.getWordFromToken(wordPair[1]));

                    // Check if both word is in the concept
                    if ((conceptWord0 != null) && // ||
                                                  // wordPair[0].matches("vehicle\\d+-\\d+"))
                                                  // &&
                            (conceptWord1 != null)) { // ||
                                                      // wordPair[1].matches("vehicle\\d+-\\d+"))
                                                      // ) {
                        String action = "";
                        String objectWord = "";
                        String impactedVehicle = "";

                        HashMap<String, String> actionDataProp = conceptWord0.getDataProperties();

                        // Check if the first word is a vehicle_action
                        if (conceptWord0.getLeafLevelName().equalsIgnoreCase("vehicle_action")
                                && actionDataProp.get("is_collision_verb").equalsIgnoreCase("true")) {
                            action = wordPair[0];
                        } else if (conceptWord1.getLeafLevelName().equalsIgnoreCase("vehicle_action")
                                && actionDataProp.get("is_collision_verb").equalsIgnoreCase("true")) {
                            action = wordPair[1];
                        }

                        // Check if the second word describes the impact side
                        if (!action.equals("") && conceptWord1 != null && conceptWord1.getLeafLevelName() != null) {
                            conceptWord1 = parser
                                    .findExactConcept(AccidentConstructorUtil.getWordFromToken(wordPair[1]));
                            if (conceptWord1 != null && conceptWord1.getLeafLevelName() != null)
                                ConsoleLogger.print('d', "Concept of " + wordPair[1].split("-")[0] + " is "
                                        + conceptWord1.getLeafLevelName());
                        }

                        if (// wordPair[1].matches("vehicle\\d+-\\d+") ||
                        (conceptWord1 != null
                                && conceptWord1.getLeafLevelName().equalsIgnoreCase("vehicle_impact_side"))) {
                            objectWord = wordPair[1];
                        } else if (// wordPair[1].matches("vehicle\\d+-\\d+") ||
                        (conceptWord0.getLeafLevelName().equalsIgnoreCase("vehicle_impact_side"))) {
                            objectWord = wordPair[0];
                        }

                        ConsoleLogger.print('d', "dobj Action Word: " + action);
                        ConsoleLogger.print('d', "Obj Word: " + objectWord);

                        // Find affected vehicle by looking at nmod:of
                        for (String objWordDependency : dependencyList) {
                            // look at nmod:of to find possession of actor

                            if (objectWord.matches("vehicle\\d+-\\d+")) {
                                impactedVehicle = AccidentConstructorUtil.getWordFromToken(objectWord);
                                // damagedComponentAnalyzer.findSideOfCrashedComponents(dependencyList,
                                // objectWord, impactedVehicle, tagWordList);
                            } else if (objWordDependency.startsWith("nmod:of") && objWordDependency.contains(objectWord)
                                    && objWordDependency.contains("vehicle")) {
                                String[] hitVehicleWordPair = AccidentConstructorUtil
                                        .getWordPairFromDependency(objWordDependency);
                                String vehicleName = AccidentConstructorUtil.getOtherWordInDep(objectWord,
                                        hitVehicleWordPair);
                                ConsoleLogger.print('d', "OBJ vehicle " + vehicleName);

                                // Find the impacted vehicle by looking at the
                                // vehicle ID
                                if (vehicleName.matches("vehicle\\d+-\\d+")) {

                                    impactedVehicle = AccidentConstructorUtil.getWordFromToken(vehicleName);
                                    ConsoleLogger.print('d',
                                            "Found impacted vehicle " + impactedVehicle + " object word " + objectWord);

                                    // record the crashed components for the
                                    // victim vehicle
                                    if (!objectWord.equals("")) {
                                        damagedComponentAnalyzer.findSideOfCrashedComponents(dependencyList, objectWord,
                                                impactedVehicle, tagWordList);
                                    }
                                } else {
                                    // A vehicle with no number, attempt to find
                                    // the right car based on what we know
                                    if (vehicleName.startsWith("vehicle-")) {
                                        ConsoleLogger.print('d', "No specific impacted vehicle, attempt to find");
                                        LinkedList<String> anonVehicleDeps = AccidentConstructorUtil
                                                .findConnectedDependencies(dependencyList, tagWordList, vehicleName,
                                                        objWordDependency, 0);

                                        for (String anonVehicleDep : anonVehicleDeps) {
                                            // Found a parked car
                                            if (anonVehicleDep.contains("park")) {
                                                // Scan our vehicle list to find
                                                // a parked car
                                                for (VehicleAttr vehicle : vehicleList) {
                                                    if (vehicle.getOnStreet() < 1) {
                                                        ConsoleLogger.print('d',
                                                                "Found parked vehicle " + vehicle.getVehicleId());
                                                        impactedVehicle = "vehicle" + vehicle.getVehicleId();

                                                        // Find the damaged side
                                                        // of the vehicle based
                                                        // on whether the obj
                                                        // word is a damaged
                                                        // side word or a
                                                        // vehicle ID
                                                        if (!objectWord.startsWith("vehicle-")) {
                                                            AccidentConcept objectWordConcept = parser
                                                                    .findExactConcept(AccidentConstructorUtil
                                                                            .getWordFromToken(objectWord));

                                                            if (objectWordConcept != null && objectWordConcept
                                                                    .getLeafLevelName().equals("vehicle_impact_side")) {
                                                                String finalVictimDmgSide = damagedComponentAnalyzer
                                                                        .findSideOfCrashedComponents(dependencyList,
                                                                                objectWord, impactedVehicle,
                                                                                tagWordList);

                                                                ConsoleLogger.print('d', "Final victim damaged side "
                                                                        + finalVictimDmgSide);
                                                            }
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                } // End infering anonymous vehicle
                            }
                        }

                        // TODO: Trace the action word to find the subject USING
                        // RECURSIVE FUNC

                        // Get other vehicle except the impacted vehicle in the
                        // sentence, set this one as subject vehicle
                        HashSet<String> suspectedVehicles = new HashSet<String>();
                        String subjectVehicle = "";
                        for (String suspectedDependency : dependencyList) {

                            String otherWord = "";
                            String suspectedActor = "";

                            if (suspectedDependency.contains("vehicle") && !suspectedDependency
                                    .contains(AccidentConstructorUtil.getWordFromToken(impactedVehicle) + "-")) {

                                ConsoleLogger.print('d', "suspected Dep with veh " + suspectedDependency);
                                String[] suspectedWordPair = AccidentConstructorUtil
                                        .getWordPairFromDependency(suspectedDependency);
                                if (suspectedWordPair[0].contains("vehicle")
                                        && !suspectedWordPair[0].startsWith("vehicle-")) {
                                    ConsoleLogger.print('d', "suspected Dep get word 0: " + suspectedWordPair[0]);
                                    suspectedActor = AccidentConstructorUtil.getWordFromToken(suspectedWordPair[0]);

                                    suspectedVehicles.add(suspectedActor);
                                    otherWord = suspectedWordPair[1];
                                } else {
                                    // If this is not an anonymous vehicle, set
                                    // it as suspected vehicle
                                    if (!suspectedWordPair[1].startsWith("vehicle-")) {
                                        ConsoleLogger.print('d', "suspected Dep get word 1: " + suspectedWordPair[1]);
                                        suspectedActor = AccidentConstructorUtil.getWordFromToken(suspectedWordPair[1]);
                                        suspectedVehicles.add(suspectedActor);
                                        otherWord = suspectedWordPair[0];
                                    }
                                }
                            }

                            // Find the concept of other word that describe the
                            // crash component
                            String otherWordStr = AccidentConstructorUtil.getWordFromToken(otherWord);
                            AccidentConcept otherWordConcept = parser.findExactConcept(otherWordStr);
                            if (!impactedVehicle.equals("") && otherWordConcept != null
                                    && otherWordConcept.getLeafLevelName().equals("vehicle_impact_side")) {
                                ConsoleLogger.print('d', "Found impacted vehicle side " + otherWordStr);

                                // Find whether a left or right key word are
                                // assigned to the impacted side
                                String damagedSide = damagedComponentAnalyzer.findSideOfCrashedComponents(
                                        dependencyList, otherWord, suspectedActor, tagWordList);
                                ConsoleLogger.print('d', "Final damaged Side " + damagedSide);

                            }
                        }

                        Iterator<String> susVehicleIter = suspectedVehicles.iterator();
                        while (susVehicleIter.hasNext()) {

                            String susVeh = susVehicleIter.next();
                            ConsoleLogger.print('d', "Sus vehicle: " + susVeh);
                            if (susVeh.equals("vehicle") || !susVeh.startsWith("vehicle")) {
                                susVehicleIter.remove();
                                ConsoleLogger.print('d', suspectedVehicles);
                            }
                        }

                        if (suspectedVehicles.size() == 1) {
                            subjectVehicle = suspectedVehicles.iterator().next();
                        }

                        ConsoleLogger.print('d', "Suspected Vehicle Len: " + suspectedVehicles.size()
                                + " Subject Vehicle " + subjectVehicle);

                        for (VehicleAttr vehicle : vehicleList) {
                            ConsoleLogger.print('d', "Damaged side of vehicle#" + vehicle.getVehicleId());
                            ConsoleLogger.print('d', vehicle.getDamagedComponents());
                        }

                        // If Subject vehicle and action is found, check whether
                        // there is any action description the same as this
                        boolean foundSameAction = false;
                        for (ActionDescription actDes : actionList) {
                            if (!actDes.getSubject().equals("") && actDes.getSubject().equals(subjectVehicle)
                                    && actDes.getVerb().equals(action.split("-")[0])) {

                                for (String verbProp : actDes.getVerbProps()) {
                                    if (verbProp.contains(impactedVehicle)) {
                                        foundSameAction = true;
                                        break; // Found the exact same action
                                               // description
                                    }
                                }

                            }
                        }
                        ConsoleLogger.print('d', "Found Same Action? " + foundSameAction);
                        if (!foundSameAction) {
                            ActionDescription crashActDes = new ActionDescription();
                            crashActDes.setSubject(subjectVehicle);
                            crashActDes.setVerb(action.split("-")[0]);
                            LinkedList<String> crashVerbProp = new LinkedList<String>();
                            crashVerbProp.add(impactedVehicle);
                            crashActDes.setVerbProps(crashVerbProp);
                            actionList.add(crashActDes);
                            ConsoleLogger.print('d', "add Crash Act Des: Subject: " + crashActDes.getSubject()
                                    + " Verb " + crashActDes.getVerb() + " Obj " + crashActDes.getVerbProps().get(0));
                        }

                    } // End check if both words exists in the Ontology
                } // End checking dobj dependency
                else {
                    if (dependency.contains("park")) {
                        inferParkingPosition(dependency, dependencyList, tagWordList);
                    }
                } // End checking other dependencies type other than nsubj and
                  // dobj

            } catch (Exception ex) {
                ConsoleLogger.print('e', "Error at extract Action chain " + ex.toString());
                continue;
            }
        }
        // for (ActionDescription actDes : actionList)
        // ConsoleLogger.print('d', "Actor " + actDes.getSubject() + " Action "
        // + actDes.getVerb() + " Verb Prop List " + actDes.getVerbProps());

        return actionList;
    }

    private String constructVehicleActionList(LinkedList<ActionDescription> storylineActionList,
            OntologyHandler parser) {
        for (ActionDescription actDes : storylineActionList) {
            ConsoleLogger.print('d', "--------------------------");
            ConsoleLogger.print('d', "Actor " + actDes.getSubject() + " Action " + actDes.getVerb() + " Verb Prop List "
                    + actDes.getVerbProps());
            ConsoleLogger.print('d', "Action Data Properties:");

            String verb = actDes.getVerb().split("-")[0];
            AccidentConcept verbConcept = parser.findExactConcept(verb);

            if (verbConcept == null) {
                continue;
            }

            // If the verb concept is a vehicle direction, it implies that this
            // is the travel action
            if (verbConcept.getLeafLevelName().equals("vehicle_direction")) {
                // add the direction to the prop list
                actDes.getVerbProps().add(verb);

                actDes.setVerb("travel");
                ConsoleLogger.print('d', "------------Updated Actdes--------------");
                ConsoleLogger.print('d', "Actor " + actDes.getSubject() + " Action " + actDes.getVerb()
                        + " Verb Prop List " + actDes.getVerbProps());
                ConsoleLogger.print('d', "Action Data Properties:");

                verb = "travel";
            }

            // If the verb has no actor or object, skip it
            if (actDes.getSubject().equals("")) {
                return "";
            }

            HashMap<String, String> actionDataProp = parser.findExactConcept(verb).getDataProperties();
            if (actionDataProp == null) {
                continue;
            }

            Set<Map.Entry<String, String>> actionDataPropEntry = actionDataProp.entrySet();
            for (Map.Entry<String, String> dataProp : actionDataPropEntry) {
                ConsoleLogger.print('d', dataProp.getKey() + " : " + dataProp.getValue());
            }

            // This action is made only for single vehicle because no object is
            // collided
            if (actionDataProp.get("is_collision_verb") != null
                    && actionDataProp.get("is_collision_verb").equalsIgnoreCase("false")) {
                if (actDes.getSubject().split(" ")[0].startsWith("vehicle")) {
                    for (VehicleAttr vehicleAttr : vehicleList) {
                        VehicleAttr modifiedVehicleAttr = vehicleAttr;
                        if (actDes.getSubject().contains("vehicle" + modifiedVehicleAttr.getVehicleId())) {
                            modifiedVehicleAttr.getActionList().add(actDes.getVerb());
                            // Set velocity
                            int velocity = Integer.parseInt(actionDataProp.get("velocity"));
                            if (modifiedVehicleAttr.getVelocity() == -1) {
                                modifiedVehicleAttr.setVelocity(velocity);
                            }
                            // modifiedVehicleAttr.setVelocity(velocity);

                            // Set Initial Positions and direction of movement
                            // if velocity does not indicate stop
                            // or impact movement
                            if (velocity > 0 && velocity < 1000) {
                                // Loop through the verb properties to find the
                                // direction of movement
                                for (String verbProp : actDes.getVerbProps()) {
                                    // Check if this indicates the direction of
                                    // vehicle
                                    String movementDirection = "";
                                    if (verbProp.contains("-")) {
                                        movementDirection = verbProp.split("-")[0];
                                    }
                                    // TODO : If MPH found, set speed to travel
                                    // action
                                    if (movementDirection != "" && parser.findExactConcept(movementDirection)
                                            .getLeafLevelName().contains("vehicle_direction")) {
                                        String direction = AccidentConstructorUtil
                                                .convertDirectionWordToDirectionLetter(movementDirection);
                                        modifiedVehicleAttr.setTravellingDirection(direction);
                                    }
                                }
                            } else if (velocity == 0) {
                                // Check if the action is park and the location
                                // is on the pavement
                                if (actDes.getVerb().equals("park")) {
                                    Street modifiedVehicleStandingStreet = modifiedVehicleAttr.getStandingStreet();

                                    // If can't find the standing street of a
                                    // vehicle, but there is only 1 road being
                                    // described in the first paragraph, take
                                    // that road
                                    if (modifiedVehicleStandingStreet == null && testCase.getStreetList().size() == 1) {
                                        modifiedVehicleStandingStreet = testCase.getStreetList().get(0);
                                        modifiedVehicleAttr.setStandingStreet(modifiedVehicleStandingStreet);
                                        ConsoleLogger.print('d', "Set standing street for modified vehicle "
                                                + modifiedVehicleAttr.getVehicleId());
                                    }

                                    if (actDes.getVerbProps().size() == 0) {
                                        // If there is no park line, but we
                                        // can't find a pavement, so this car is
                                        // on the line
                                        if (!modifiedVehicleStandingStreet.getStreetPropertyValue("road_park_line")
                                                .equals("0")) {
                                            ConsoleLogger.print('d',
                                                    "Set car " + modifiedVehicleAttr.getVehicleId() + " on park line");
                                            modifiedVehicleAttr.setOnStreet(-1);
                                        }
                                    }
                                    for (String verbProp : actDes.getVerbProps()) {
                                        AccidentConcept verbPropConcept = parser
                                                .findExactConcept(AccidentConstructorUtil.getWordFromToken(verbProp));
                                        // If the car is park on the pavement,
                                        // set the onStreet prop of the vehicle
                                        // to 0
                                        ConsoleLogger.print('d', "Verb prop is pavement " + verbProp);

                                        if (verbPropConcept.getLeafLevelName().contains("pavement")
                                                || verbPropConcept.getLeafLevelName().contains("curb")) {
                                            modifiedVehicleAttr.setOnStreet(0);
                                            modifiedVehicleStandingStreet.putValToKey("road_park_line", "0");
                                        }
                                    }

                                    // If there is no sign indicating that the
                                    // car is parking on the street, it is
                                    // parking
                                    // on a parking line
                                    if (!modifiedVehicleStandingStreet.getStreetPropertyValue("road_park_line")
                                            .equals("0")) {
                                        ConsoleLogger.print('d', "No sign that vehicle "
                                                + modifiedVehicleAttr.getVehicleId() + " is parking on curb");
                                        modifiedVehicleAttr.setOnStreet(-1);
                                    }
                                }
                            }

                            ConsoleLogger.print('d', "V = " + modifiedVehicleAttr.getVelocity());
                            ConsoleLogger.print('d', "MD = " + modifiedVehicleAttr.getTravellingDirection());
                            ConsoleLogger.print('d', "Onstreet = " + modifiedVehicleAttr.getOnStreet());
                        } else // Assign N/A or passive action to other vehicles
                        {
                            // Check if the vehicle is somehow contained in the
                            // object list
                            if (actDes.getVerbProps().size() > 0) {
                                for (String verbProp : actDes.getVerbProps()) {
                                    String firstWord = verbProp;
                                    if (verbProp.contains(" ")) {
                                        firstWord = verbProp.split(" ")[0];
                                    }

                                    if (("vehicle" + modifiedVehicleAttr.getVehicleId()).equalsIgnoreCase(firstWord)) {
                                        modifiedVehicleAttr.getActionList().add(actDes.getVerb() + "/pasiv");
                                    } else {
                                        // Append the last action in the list,
                                        // if not avail, add "N/A"
                                        if (modifiedVehicleAttr.getActionList().size() > 0) {
                                            modifiedVehicleAttr.getActionList()
                                                    .add(modifiedVehicleAttr.getActionList().getLast());
                                        } else {
                                            modifiedVehicleAttr.getActionList().add("N/A");
                                        }
                                        break;
                                    }
                                }
                            } else {
                                // Append the last action in the list, if not
                                // avail, add "N/A"
                                if (modifiedVehicleAttr.getActionList().size() > 0) {
                                    if (modifiedVehicleAttr.getActionList().getLast().startsWith("hit")) {
                                        modifiedVehicleAttr.getActionList().add("endHit");
                                    } else {
                                        modifiedVehicleAttr.getActionList()
                                                .add(modifiedVehicleAttr.getActionList().getLast());
                                    }

                                } else {
                                    modifiedVehicleAttr.getActionList().add("N/A");
                                }
                            }
                        }

                        vehicleList.set(vehicleList.indexOf(vehicleAttr), modifiedVehicleAttr);
                    } // End assign action for each vehicle
                } // End checking vehicle subjects
            } // End checking non-collision verb
            else // Set the action of impacted vehicles to be impacted
            {
                ConsoleLogger.print('d', "Collision indicator verb " + actDes.getVerb());
                ArrayList<String> impactedVehicles = new ArrayList<String>();
                ArrayList<String> processedVehicles = new ArrayList<String>();
                String impactIndicatorVerb = "hit";

                for (String verbProp : actDes.getVerbProps()) {

                    String firstWord = verbProp;
                    if (verbProp.contains(" ")) {
                        firstWord = verbProp.split(" ")[0];
                    }
                    if (firstWord.startsWith("vehicle")) {
                        impactedVehicles.add(firstWord.split("<>")[0]);
                    }
                }

                // Append actions to the vehicle(s) in the subject list
                if (actDes.getSubject().contains("/")) {
                    ArrayList<String> vehiclesInSubjects = new ArrayList<String>();
                    for (String vehicleName : vehiclesInSubjects) {
                        VehicleAttr foundVehicle = findVehicle(vehicleName.split(" ")[0]);
                        if (foundVehicle != null) {
                            foundVehicle.getActionList().add(impactIndicatorVerb);
                            processedVehicles.add(vehicleName);
                        }
                    }
                } else {
                    VehicleAttr foundVehicle = findVehicle(actDes.getSubject().split(" ")[0]);
                    ConsoleLogger.print('d', "Found Subject vehicle? " + foundVehicle == null ? "No"
                            : "Yes, found vehicle" + foundVehicle.getVehicleId());
                    if (foundVehicle != null) {
                        foundVehicle.getActionList().add(impactIndicatorVerb);
                        processedVehicles.add("vehicle" + foundVehicle.getVehicleId());
                    }
                }

                // Append action to the vehicle(s) in the object list
                for (String vehicleName : impactedVehicles) {
                    VehicleAttr foundVehicle = findVehicle(vehicleName);
                    if (foundVehicle != null) {
                        foundVehicle.getActionList().add(impactIndicatorVerb + "*");
                        processedVehicles.add(vehicleName);
                    }
                }

                ConsoleLogger.print('d', "Processed Veh list " + processedVehicles);

                for (VehicleAttr remainingVehicle : vehicleList) {
                    // Skip the vehicle if found
                    ConsoleLogger.print('d', "index of " + ("vehicle" + remainingVehicle.getVehicleId()) + " is "
                            + processedVehicles.indexOf("vehicle" + remainingVehicle.getVehicleId()));
                    if (processedVehicles.indexOf("vehicle" + remainingVehicle.getVehicleId()) > -1) {
                        continue;
                    } else // Mark this one as not impacted
                    {
                        String previousAction = remainingVehicle.getActionList().getLast();
                        if (previousAction.startsWith("hit")) {
                            remainingVehicle.getActionList().add("endHit");
                        } else {
                            remainingVehicle.getActionList().add(previousAction);
                        }

                    }
                }

            }
            ConsoleLogger.print('d', "--------------------------");

        }
        return "";
    }

    private String constructVehicleActionList2(LinkedList<ActionDescription> storylineActionList,
            OntologyHandler parser) {
        for (ActionDescription actDes : storylineActionList) {
            ConsoleLogger.print('d', "--------------------------");
            ConsoleLogger.print('d', "Actor " + actDes.getSubject() + " Action " + actDes.getVerb() + " Verb Prop List "
                    + actDes.getVerbProps());
            ConsoleLogger.print('d', "Action Data Properties:");

            String verb = actDes.getVerb().split("-")[0];
            AccidentConcept verbConcept = parser.findExactConcept(verb);

            if (verbConcept == null || !verbConcept.getLeafLevelName().equals("vehicle_action")
                    || !verbConcept.getLeafLevelName().equals("vehicle_direction")) {
                continue;
            }

            // If the verb has no actor or object, skip it
            if (actDes.getSubject().equals("")) {
                continue;
            }

            // If the verb concept is a vehicle direction, it implies that this
            // is the travel action
            if (verbConcept.getLeafLevelName().equals("vehicle_direction")) {
                // add the direction to the prop list
                actDes.getVerbProps().add(verb);

                actDes.setVerb("travel");
                ConsoleLogger.print('d', "------------Updated Actdes--------------");
                ConsoleLogger.print('d', "Actor " + actDes.getSubject() + " Action " + actDes.getVerb()
                        + " Verb Prop List " + actDes.getVerbProps());
                ConsoleLogger.print('d', "Action Data Properties:");

                verb = "travel";
            }
            HashMap<String, String> actionDataProp = parser.findExactConcept(verb).getDataProperties();

            Set<Map.Entry<String, String>> actionDataPropEntry = actionDataProp.entrySet();
            for (Map.Entry<String, String> dataProp : actionDataPropEntry) {
                ConsoleLogger.print('d', dataProp.getKey() + " : " + dataProp.getValue());
            }

            // This action is made only for single vehicle because no object is
            // collided
            if (actionDataProp.get("is_collision_verb") != null
                    && actionDataProp.get("is_collision_verb").equalsIgnoreCase("false")) {
                if (actDes.getSubject().split(" ")[0].startsWith("vehicle")) {
                    for (VehicleAttr vehicleAttr : vehicleList) {
                        VehicleAttr modifiedVehicleAttr = vehicleAttr;
                        if (actDes.getSubject().contains("vehicle" + modifiedVehicleAttr.getVehicleId())) {
                            modifiedVehicleAttr.getActionList().add(actDes.getVerb());
                            // Set velocity
                            int velocity = Integer.parseInt(actionDataProp.get("velocity"));
                            if (modifiedVehicleAttr.getVelocity() == -1) {
                                modifiedVehicleAttr.setVelocity(velocity);
                            }
                            // modifiedVehicleAttr.setVelocity(velocity);

                            // Set Initial Positions and direction of movement
                            // if velocity does not indicate stop
                            // or impact movement
                            if (velocity > 0 && velocity < 1000) {
                                // Loop through the verb properties to find the
                                // direction of movement
                                for (String verbProp : actDes.getVerbProps()) {
                                    // Check if this indicates the direction of
                                    // vehicle
                                    String movementDirection = "";
                                    if (verbProp.contains("-")) {
                                        movementDirection = verbProp.split("-")[0];
                                    }
                                    // TODO : If MPH found, set speed to travel
                                    // action
                                    if (movementDirection != "" && parser.findExactConcept(movementDirection)
                                            .getLeafLevelName().contains("vehicle_direction")) {
                                        String direction = AccidentConstructorUtil
                                                .convertDirectionWordToDirectionLetter(movementDirection);
                                        modifiedVehicleAttr.setTravellingDirection(direction);
                                    }
                                }
                            } else if (velocity == 0) {
                                // Check if the action is park and the location
                                // is on the pavement
                                if (actDes.getVerb().equals("park")) {
                                    Street modifiedVehicleStandingStreet = modifiedVehicleAttr.getStandingStreet();

                                    // If can't find the standing street of a
                                    // vehicle, but there is only 1 road being
                                    // described in the first paragraph, take
                                    // that road
                                    if (modifiedVehicleStandingStreet == null && testCase.getStreetList().size() == 1) {
                                        modifiedVehicleStandingStreet = testCase.getStreetList().get(0);
                                        modifiedVehicleAttr.setStandingStreet(modifiedVehicleStandingStreet);
                                        ConsoleLogger.print('d', "Set standing street for modified vehicle "
                                                + modifiedVehicleAttr.getVehicleId());
                                    }

                                    if (actDes.getVerbProps().size() == 0) {
                                        // If there is no park line, but we
                                        // can't find a pavement, so this car is
                                        // on the line
                                        if (!modifiedVehicleStandingStreet.getStreetPropertyValue("road_park_line")
                                                .equals("0")) {
                                            ConsoleLogger.print('d',
                                                    "Set car " + modifiedVehicleAttr.getVehicleId() + " on park line");
                                            modifiedVehicleAttr.setOnStreet(-1);
                                        }
                                    }
                                    for (String verbProp : actDes.getVerbProps()) {
                                        AccidentConcept verbPropConcept = parser
                                                .findExactConcept(AccidentConstructorUtil.getWordFromToken(verbProp));
                                        // If the car is park on the pavement,
                                        // set the onStreet prop of the vehicle
                                        // to 0
                                        ConsoleLogger.print('d', "Verb prop is pavement " + verbProp);

                                        if (verbPropConcept.getLeafLevelName().contains("pavement")
                                                || verbPropConcept.getLeafLevelName().contains("curb")) {
                                            modifiedVehicleAttr.setOnStreet(0);
                                            modifiedVehicleStandingStreet.putValToKey("road_park_line", "0");
                                        }
                                    }

                                    // If there is no sign indicating that the
                                    // car is parking on the street, it is
                                    // parking
                                    // on a parking line
                                    if (!modifiedVehicleStandingStreet.getStreetPropertyValue("road_park_line")
                                            .equals("0")) {
                                        ConsoleLogger.print('d', "No sign that vehicle "
                                                + modifiedVehicleAttr.getVehicleId() + " is parking on curb");
                                        modifiedVehicleAttr.setOnStreet(-1);
                                    }
                                }
                            }

                            ConsoleLogger.print('d', "V = " + modifiedVehicleAttr.getVelocity());
                            ConsoleLogger.print('d', "MD = " + modifiedVehicleAttr.getTravellingDirection());
                            ConsoleLogger.print('d', "Onstreet = " + modifiedVehicleAttr.getOnStreet());
                        } else // Assign N/A or passive action to other vehicles
                        {
                            // Check if the vehicle is somehow contained in the
                            // object list
                            if (actDes.getVerbProps().size() > 0) {
                                for (String verbProp : actDes.getVerbProps()) {
                                    String firstWord = verbProp;
                                    if (verbProp.contains(" ")) {
                                        firstWord = verbProp.split(" ")[0];
                                    }

                                    if (("vehicle" + modifiedVehicleAttr.getVehicleId()).equalsIgnoreCase(firstWord)) {
                                        modifiedVehicleAttr.getActionList().add(actDes.getVerb() + "/pasiv");
                                    } else {
                                        // Append the last action in the list,
                                        // if not avail, add "N/A"
                                        if (modifiedVehicleAttr.getActionList().size() > 0) {
                                            modifiedVehicleAttr.getActionList()
                                                    .add(modifiedVehicleAttr.getActionList().getLast());
                                        } else {
                                            modifiedVehicleAttr.getActionList().add("N/A");
                                        }
                                        break;
                                    }
                                }
                            } else {
                                // Append the last action in the list, if not
                                // avail, add "N/A"
                                if (modifiedVehicleAttr.getActionList().size() > 0) {
                                    if (modifiedVehicleAttr.getActionList().getLast().startsWith("hit")) {
                                        modifiedVehicleAttr.getActionList().add("endHit");
                                    } else {
                                        modifiedVehicleAttr.getActionList()
                                                .add(modifiedVehicleAttr.getActionList().getLast());
                                    }

                                } else {
                                    modifiedVehicleAttr.getActionList().add("N/A");
                                }
                            }
                        }

                        vehicleList.set(vehicleList.indexOf(vehicleAttr), modifiedVehicleAttr);
                    } // End assign action for each vehicle
                } // End checking vehicle subjects
            } // End checking non-collision verb
            else // Set the action of impacted vehicles to be impacted
            {
                ConsoleLogger.print('d', "Collision indicator verb " + actDes.getVerb());
                ArrayList<String> impactedVehicles = new ArrayList<String>();
                ArrayList<String> processedVehicles = new ArrayList<String>();
                String impactIndicatorVerb = "hit";

                for (String verbProp : actDes.getVerbProps()) {

                    String firstWord = verbProp;
                    if (verbProp.contains(" ")) {
                        firstWord = verbProp.split(" ")[0];
                    }
                    if (firstWord.startsWith("vehicle")) {
                        impactedVehicles.add(firstWord.split("<>")[0]);
                    }
                }

                // Append actions to the vehicle(s) in the subject list
                if (actDes.getSubject().contains("/")) {
                    ArrayList<String> vehiclesInSubjects = new ArrayList<String>();
                    for (String vehicleName : vehiclesInSubjects) {
                        VehicleAttr foundVehicle = findVehicle(vehicleName.split(" ")[0]);
                        if (foundVehicle != null) {
                            foundVehicle.getActionList().add(impactIndicatorVerb);
                            processedVehicles.add(vehicleName);
                        }
                    }
                } else {
                    VehicleAttr foundVehicle = findVehicle(actDes.getSubject().split(" ")[0]);
                    ConsoleLogger.print('d', "Found Subject vehicle? " + foundVehicle == null ? "No"
                            : "Yes, found vehicle" + foundVehicle.getVehicleId());
                    if (foundVehicle != null) {
                        foundVehicle.getActionList().add(impactIndicatorVerb);
                        processedVehicles.add("vehicle" + foundVehicle.getVehicleId());
                    }
                }

                // Append action to the vehicle(s) in the object list
                for (String vehicleName : impactedVehicles) {
                    VehicleAttr foundVehicle = findVehicle(vehicleName);
                    if (foundVehicle != null) {
                        foundVehicle.getActionList().add(impactIndicatorVerb + "*");
                        processedVehicles.add(vehicleName);
                    }
                }

                ConsoleLogger.print('d', "Processed Veh list " + processedVehicles);

                for (VehicleAttr remainingVehicle : vehicleList) {
                    // Skip the vehicle if found
                    ConsoleLogger.print('d', "index of " + ("vehicle" + remainingVehicle.getVehicleId()) + " is "
                            + processedVehicles.indexOf("vehicle" + remainingVehicle.getVehicleId()));
                    if (processedVehicles.indexOf("vehicle" + remainingVehicle.getVehicleId()) > -1) {
                        continue;
                    } else // Mark this one as not impacted
                    {
                        String previousAction = remainingVehicle.getActionList().getLast();
                        if (previousAction.startsWith("hit")) {
                            remainingVehicle.getActionList().add("endHit");
                        } else {
                            remainingVehicle.getActionList().add(previousAction);
                        }

                    }
                }

            }
            ConsoleLogger.print('d', "--------------------------");

        }
        return "";
    }

    private void removeInvalidActions(ArrayList<VehicleAttr> vehicleList) {
        // Replace N/A by the nearest valid action for each vehicle
        for (VehicleAttr vehicleAttr : vehicleList) {
            LinkedList<String> vehicleActionList = vehicleAttr.getActionList();

            if (vehicleActionList.get(0).equals("N/A")) {
                // If all actions are N/A, meaning the NLP cannot infer the
                // action of the vehicle, then we need to infer
                // from the XML file
                boolean allNAActions = true;
                for (int i = 1; i < vehicleActionList.size(); i++) {
                    if (!vehicleActionList.get(i).equals("N/A")) {
                        allNAActions = false;
                        break;
                    }
                }

                // If not all actions are N/A, search a non-impact action and
                // assign it to the N/A action
                String theChosenAction = "";
                if (!allNAActions) {
                    // Search for non-impact action
                    for (int j = 1; j < vehicleActionList.size(); j++) {
                        if (!vehicleActionList.get(j).equals("N/A") && !vehicleActionList.get(j).startsWith("hit")) {
                            theChosenAction = vehicleActionList.get(j);
                            break;
                        }
                    }
                }
                if (theChosenAction.equals("") || theChosenAction.equals("endHit")) {
                    if (vehicleAttr.getOnStreet() == 0 || vehicleAttr.getOnStreet() == -1) {
                        theChosenAction = "park";
                    } else {
                        theChosenAction = "travel";
                    }
                }

                ConsoleLogger.print('d', "chosen action " + theChosenAction);

                // Replace N/A with non-impact action
                for (int i = 0; i < vehicleActionList.size(); i++) {
                    if (vehicleActionList.get(i).equals("N/A")) {
                        vehicleActionList.set(i, theChosenAction);
                    }
                }
            }

            vehicleAttr.setActionList(vehicleActionList);
        } // End looping through vehicle
    }

    private void removeDuplicateAction(ArrayList<VehicleAttr> vehicleList) {
        LinkedList<LinkedList<String>> vehicleActionList = new LinkedList<LinkedList<String>>();

        for (int i = 0; i < vehicleList.size(); i++) {
            vehicleActionList.add(vehicleList.get(i).getActionList());
        }
        // Remove duplicate actions
        for (int i = 0; i < vehicleActionList.get(0).size() - 1; i++) {
            boolean allVehicleSameAction = false;
            // Check if all next action for each vehicle is the same
            for (int j = 0; j < vehicleActionList.size(); j++) {
                // ConsoleLogger.print('d',"Word of vehicle %d at pos %d is %s;
                // at [%d, %d] is %s \n",
                // j, i, vehicleActionList.get(j).get(i),
                // j, i + 1, vehicleActionList.get(j).get(i + 1));

                // Only prune if there are more than 1 duplicate action

                if (vehicleActionList.get(j).get(i).equals(vehicleActionList.get(j).get(i + 1))) {
                    allVehicleSameAction = true;
                } else {
                    allVehicleSameAction = false;
                    break;
                }
            }
            if (allVehicleSameAction) {
                for (int j = 0; j < vehicleActionList.size(); j++) {
                    vehicleActionList.get(j).remove(i);
                }
                i--;
            }
        }
    }

    private void superPrune(ArrayList<VehicleAttr> vehicleList) {
        LinkedList<LinkedList<String>> vehicleActionList = new LinkedList<LinkedList<String>>();

        for (int i = 0; i < vehicleList.size(); i++) {
            vehicleActionList.add(vehicleList.get(i).getActionList());
        }

        for (int c = 0; c < vehicleActionList.size(); c++) {
            for (int i = 0; i < vehicleActionList.get(c).size(); i++) {
                // Check if the action of this step is equal to another step
                if (vehicleActionList.get(c).get(i).equals("hit")) {
                    ConsoleLogger.print('d', "hit word reached super prune at " + i);
                    // Prune everything behind this index
                    for (int e = 0; e < vehicleActionList.size(); e++) {
                        for (int k = vehicleActionList.get(e).size() - 1; k > i; k--) {
                            vehicleActionList.get(e).remove(k);
                            ConsoleLogger.print('d', "Removed VehicleList e=" + e + " k=" + k);
                        }
                    }
                    for (int e = 0; e < vehicleActionList.size(); e++) {
                        ConsoleLogger.print('d', "VehicleList e=" + e + " :" + vehicleActionList.get(e));

                    }
                    break;
                }
            }
        }
    }

    // Remove N/A and duplicate actions
    private void pruneActionTree(ArrayList<VehicleAttr> vehicleList) {
        removeInvalidActions(vehicleList);
        removeDuplicateAction(vehicleList);

        ConsoleLogger.print('d', "After preprocessing");

        for (VehicleAttr vehicle : vehicleList) {
            ConsoleLogger.print('d',
                    "Vehicle " + vehicle.getVehicleId() + " Actions : " + vehicle.getActionList().toString());
        }

        LinkedList<LinkedList<String>> vehicleActionList = new LinkedList<LinkedList<String>>();

        for (int i = 0; i < vehicleList.size(); i++) {
            vehicleActionList.add(vehicleList.get(i).getActionList());
        }

        for (int i = vehicleActionList.get(0).size() - 1; i > 0 && vehicleActionList.get(0).size() > 2; i--) {
            boolean prunable = true;
            boolean hitWordFound = false;
            // Loop through each vehicle
            for (int c = 0; c < vehicleActionList.size(); c++) {
                // Check if the action of this step is equal to another step
                if (vehicleActionList.get(c).get(i).equals("hit")) {
                    ConsoleLogger.print('d', "hit word reached");
                    hitWordFound = true;
                    continue;
                } else if (vehicleActionList.get(c).get(i).equals("hit*")) {
                    ConsoleLogger.print('d', "hit* condition reached");
                    hitWordFound = true;
                    // If previous word is a normal traveling action, then can't
                    // prune
                    if (!ontoParser.findExactConcept(vehicleActionList.get(c).get(i - 1)).getDataProperties()
                            .get("velocity").equals("20")) {

                        prunable = false;
                        break;
                    } else {
                        // Do NOT prune if the previous word is different from
                        // the previous previous word
                        if (i - 2 >= 0) {
                            if (!vehicleActionList.get(c).get(i - 1).equals(vehicleActionList.get(c).get(i - 2))) {
                                ConsoleLogger.print('d', "2 previous words are not the same");
                                prunable = false;
                                break;
                            }
                        }
                    }
                } else if (!vehicleActionList.get(c).get(i).equals(vehicleActionList.get(c).get(i - 1))) {
                    prunable = false;
                    break;
                } else {
                    ConsoleLogger.print('d', "Else condition");
                    // If the previous action is a stop or park action, leave it
                    // like that
                    if (ontoParser.findExactConcept(vehicleActionList.get(c).get(i - 1)).getDataProperties()
                            .get("velocity").equals("0")) {
                        prunable = false;
                        break;
                    }
                }
            } // End looping through vehicle

            if (!hitWordFound) {
                break;
            }

            if (!prunable) {
                continue;
            } else {
                for (LinkedList<String> actionList : vehicleActionList) {
                    actionList.remove(i - 1);
                }
            }
        } // End looping through action list
    }

    private void assignDirectionToRoad(String directionWord, VehicleAttr travellingVehicle) {
        String direction = AccidentConstructorUtil.convertDirectionWordToDirectionLetter(directionWord);
        Street rightDirectionStreet = null;
        travellingVehicle.setTravellingDirection(direction);
        if (direction.equals("W") || direction.equals("E")) {
            // If there are only 2 street, set standing roadside as street2
            if (testCase.getStreetList().size() == 2) {
                // Select the right street of east/west
                for (Street street : testCase.getStreetList()) {
                    if (street.getStreetPropertyValue("road_navigation").equals("W")
                            || street.getStreetPropertyValue("road_navigation").equals("E")) {
                        rightDirectionStreet = street;
                    }
                }

                // Assume the first road is always E
                if (rightDirectionStreet == null) {
                    rightDirectionStreet = testCase.getStreetList().get(0);
                    rightDirectionStreet.putValToKey("road_navigation", direction);
                }
            } // End checking if there are only 2 streets
              // If there is only 1 street found, assign the direction of the
              // vehicle to the
              // road, if the street has no specified direction
            else if (testCase.getStreetList().size() == 1) {
                Street onlyStreet = testCase.getStreetList().get(0);

                if (onlyStreet.getStreetPropertyValue("road_navigation").equals("")) {
                    onlyStreet.putValToKey("road_navigation", "E");
                    travellingVehicle.setStandingStreet(onlyStreet);
                }
            }
        } // End processing westbound direction
        else {
            ConsoleLogger.print('d', "2.Detect other moving direction " + direction);
            // Select the right street of other direction
            for (Street street : testCase.getStreetList()) {
                String streetNavigation = street.getStreetPropertyValue("road_navigation");
                if (streetNavigation.equals("N") || streetNavigation.equals("S") || streetNavigation.equals("NE")
                        || streetNavigation.equals("SE") || streetNavigation.equals("NW")
                        || streetNavigation.equals("SW"))

                {
                    ConsoleLogger.print('d', "Find right street ID " + street.getStreetPropertyValue("road_ID")
                            + " with direction " + streetNavigation);
                    rightDirectionStreet = street;
                }
            }

            // Assume the first road is always E
            if (rightDirectionStreet == null) {
                // Find the empty road navigation from the second street and
                // assign the value there
                if (testCase.getStreetList().size() == 1) {

                    rightDirectionStreet = testCase.getStreetList().get(0);
                    rightDirectionStreet.putValToKey("road_navigation", "E");
                    ConsoleLogger.print('d',
                            "assign " + "E" + " to road #" + rightDirectionStreet.getStreetPropertyValue("road_ID"));
                } else {
                    for (int i = testCase.getStreetList().size() - 1; i >= 0; i--) {
                        if (testCase.getStreetList().get(i).getStreetPropertyValue("road_navigation").equals("")) {
                            rightDirectionStreet = testCase.getStreetList().get(i);
                            rightDirectionStreet.putValToKey("road_navigation", direction);
                            break;
                        }
                    }
                }

            }
        }
        // Set the angle of the standing street if it is diagonal road
        // rightDirectionStreet.putValToKey("road_angle", "" +
        // fuzzAngleOfRoad(direction));

        travellingVehicle.setStandingStreet(rightDirectionStreet);

    }

    private void checkMissingPropertiesVehicles() {
        for (Street street : testCase.getStreetList()) {
            if (street.getStreetPropertyValue("curve_direction").equals("left")) {
                ConsoleLogger.print('d', "Set radius to negative value for left curve");
                street.putValToKey("curve_radius",
                        (Double.parseDouble(street.getStreetPropertyValue("curve_radius")) * -1) + "");
            }

            // if (street.getStreetPropertyValue("road_angle").equals("0")
            // && !street.getStreetPropertyValue("road_navigation").equals("S")
            // )
            // {
            // switch (street.getStreetPropertyValue("road_navigation")){
            // case "W":
            // street.putValToKey("road_angle", "90");
            // break;
            // case "N":
            // street.putValToKey("road_angle", "180");
            // break;
            // case "E":
            // street.putValToKey("road_angle", "270");
            // break;
            // default:
            // break;
            // }
            // }
        }

        for (VehicleAttr vehicle : vehicleList) {
            // If there is only 1 road, then set moving of all vehicle to east
            // direction
            if (testCase.getStreetList().size() == 1) {
                Street onlyStreet = testCase.getStreetList().get(0);
                onlyStreet.putValToKey("road_navigation", "E");

                vehicle.setTravellingDirection(onlyStreet.getStreetPropertyValue("road_navigation"));
                vehicle.setStandingStreet(onlyStreet);
                ConsoleLogger.print('d', "Set direction " + onlyStreet.getStreetPropertyValue("road_navigation")
                        + " to vehicle #" + vehicle.getVehicleId());

            } // End processing single street case

            // If no parking side is specified, set the vehicle to park on the
            // right
            if (vehicle.getStandingRoadSide().equals("")) {
                vehicle.setStandingRoadSide("right");
            }

            if (vehicle.getStandingStreet() == null) {
                ConsoleLogger.print('d', "Find null standing street for vehicle #" + vehicle.getVehicleId());
                for (Street road : testCase.getStreetList()) {
                    ConsoleLogger.print('d', "Vehicle travel direction is " + vehicle.getTravellingDirection()
                            + " current street direction is " + road.getStreetPropertyValue("road_navigation"));
                    if (NavigationDictionary.isDirectionsInSameAxis(vehicle.getTravellingDirection(),
                            road.getStreetPropertyValue("road_navigation"))) {
                        vehicle.setStandingStreet(road);
                        break;
                    }
                }
            }

        } // End looping through all vehicles

    }

    private void inferParkingPosition(String dependency, LinkedList<String> dependencyList,
            LinkedList<String> tagWordList) {
        ConsoleLogger.print('d', "Infer Parking Pos ");
        // Check for unknown parking car
        for (VehicleAttr vehicle : vehicleList) {
            // Find a parked car, but don't know where to park
            if ((vehicle.getOnStreet() == 0 || vehicle.getOnStreet() == -1)
                    && vehicle.getStandingRoadSide().equals("")) {
                // Find the index of the parked word as an anchor to find the
                // park direction
                String[] parkDepWordPair = AccidentConstructorUtil.getWordPairFromDependency(dependency);
                int parkWordIndex = -1;

                for (String parkDepWord : parkDepWordPair) {
                    if (parkDepWord.startsWith("park")) {
                        parkWordIndex = AccidentConstructorUtil.getPositionFromToken(parkDepWord);
                        break;
                    }
                }

                // TODO: Speculate the standing road side by scanning the next
                // and prev 10 words
                if (vehicle.getStandingRoadSide().equals("")) {
                    // Attempt to find parkingSide
                    String[] parkingSides = new String[] { "left", "right" };

                    for (String parkingSide : parkingSides) {
                        // Find the dependency that contains "side" or a
                        // pavement word
                        LinkedList<String> parkDirectionDependencies = AccidentConstructorUtil
                                .findConnectedDependencies(dependencyList, tagWordList, parkingSide, "", 0);

                        for (String parkDirectionDependency : parkDirectionDependencies) {
                            String[] parkDirectionWordPair = AccidentConstructorUtil
                                    .getWordPairFromDependency(parkDirectionDependency);
                            String parkingPlaceIndicator = "";

                            String parkingSideWord = parkingSide;

                            // Find the direction word
                            if (parkDirectionWordPair[0].startsWith(parkingSide)) {
                                parkingSideWord = parkDirectionWordPair[0];
                                parkingPlaceIndicator = parkDirectionWordPair[1];
                            } else if (parkDirectionWordPair[1].startsWith(parkingSide)) {
                                parkingPlaceIndicator = parkDirectionWordPair[0];
                                parkingSideWord = parkDirectionWordPair[1];
                            }

                            // If we find this is left/right side/pavement,
                            // check whether the direction word
                            // is within 10 words from the park index
                            String parkingPlaceIndicatorConceptGroup = "";
                            AccidentConcept parkingPlaceIndicatorConcept = ontoParser
                                    .findExactConcept(parkingPlaceIndicator);
                            if (parkingPlaceIndicatorConcept != null) {
                                parkingPlaceIndicatorConceptGroup = parkingPlaceIndicatorConcept.getLeafLevelName();
                            }

                            if (AccidentConstructorUtil.getWordFromToken(parkingPlaceIndicator).equals("side")
                                    || parkingPlaceIndicatorConceptGroup.equals("pavement")) {
                                if (Math.abs(AccidentConstructorUtil.getPositionFromToken(parkingSideWord)
                                        - parkWordIndex) <= 10) {
                                    ConsoleLogger.print('d', "Found word direction " + parkingSideWord
                                            + " near parkWordIndex " + parkWordIndex);

                                    if (parkingPlaceIndicatorConceptGroup.equals("pavement")) {
                                        ConsoleLogger.print('d', "Find parking pavement indicator");
                                        vehicle.setOnStreet(0);
                                        vehicle.setStandingRoadSide(parkingSide);
                                    } else if (parkingPlaceIndicator.startsWith("side")) {
                                        ConsoleLogger.print('d', "Find parking side");

                                        // Find whether the "side" word is
                                        // connected to a road component
                                        LinkedList<String> parkedRoadComponentDependencies = AccidentConstructorUtil
                                                .findConnectedDependencies(dependencyList, tagWordList,
                                                        parkingPlaceIndicator, parkDirectionDependency, 0);

                                        for (String parkedRoadComponentDependency : parkedRoadComponentDependencies) {
                                            ConsoleLogger.print('d',
                                                    "Processing parkedRoad Dep " + parkedRoadComponentDependency);

                                            String[] parkedRoadComponentWordPair = AccidentConstructorUtil
                                                    .getWordPairFromDependency(parkedRoadComponentDependency);

                                            String roadComponentWord = AccidentConstructorUtil.getWordFromToken(
                                                    AccidentConstructorUtil.getOtherWordInDep(parkingPlaceIndicator,
                                                            parkedRoadComponentWordPair));

                                            ConsoleLogger.print('d', "Road component word " + roadComponentWord);

                                            AccidentConcept roadComponentConcept = ontoParser
                                                    .findExactConcept(roadComponentWord);

                                            if (roadComponentConcept != null
                                                    && roadComponentConcept.getLeafLevelName().equals("road_type")) {
                                                ConsoleLogger.print('d',
                                                        "Found the road component " + roadComponentWord);
                                                vehicle.setStandingRoadSide(parkingSide);
                                                vehicle.setOnStreet(0);
                                            }
                                        }
                                    }
                                } else {
                                    break;
                                }
                            }

                        }
                    } // end checking parking side dependency

                } // End checking if the car has no specified parking side
            } // End checking if a car is set as park but has no parking side
        } // End looping through each vehicle/
    }

    /*
     * Replace space by underscore in certain compound words. For example:
     * "stop sign" becomes "stop_sigh"
     * 
     * @param paragraph: The input paragraph string of which compound words are
     * modified
     */
    private String replacePhrases(String paragraph) {
        String[] phrases = new String[] { "speed limit", "stop sign", "traffic sign", "traffic light" };
        for (String phrase : phrases) {
            String[] phraseWords = phrase.split(" ");
            paragraph = paragraph.replace(phrase, phraseWords[0] + "_" + phraseWords[1]);

        }
        return paragraph;
    }

    /*
     * In an impact description sentence, find the name of the striker from the
     * victim i.e. the vehicle which is impacted
     *
     */
    private String findSuspectedVehicle(LinkedList<String> dependencyList, LinkedList<String> tagWordList,
            String impactedVehicle, DamagedComponentAnalyzer damagedComponentAnalyzer) {
        // Get other vehicle except the impacted vehicle in the sentence, set
        // the other vehicle as striker (subject)
        HashSet<String> suspectedVehicles = new HashSet<String>();
        String subjectVehicle = "";
        for (String suspectedDependency : dependencyList) {

            String otherWord = "";
            String suspectedActor = "";

            if (suspectedDependency.contains("vehicle")
                    && !suspectedDependency.contains(AccidentConstructorUtil.getWordFromToken(impactedVehicle) + "-")) {

                ConsoleLogger.print('d', "suspected Dep with veh " + suspectedDependency);
                String[] suspectedWordPair = AccidentConstructorUtil.getWordPairFromDependency(suspectedDependency);

                // If a vehicle with a specific vehicle ID is given, record it
                // as suspected striker
                if (suspectedWordPair[0].contains("vehicle") && !suspectedWordPair[0].startsWith("vehicle-")) {
                    ConsoleLogger.print('d', "suspected Dep get word 0: " + suspectedWordPair[0]);
                    suspectedActor = AccidentConstructorUtil.getWordFromToken(suspectedWordPair[0]);

                    suspectedVehicles.add(suspectedActor);
                    otherWord = suspectedWordPair[1];
                } else // If this is not an anonymous vehicle, set it as
                       // suspected vehicle
                {
                    if (!suspectedWordPair[1].startsWith("vehicle-")) {
                        ConsoleLogger.print('d', "suspected Dep get word 1: " + suspectedWordPair[1]);
                        suspectedActor = AccidentConstructorUtil.getWordFromToken(suspectedWordPair[1]);
                        suspectedVehicles.add(suspectedActor);
                        otherWord = suspectedWordPair[0];
                    }
                }
            }

            // Find the concept of other word that describe the crash component
            String otherWordStr = AccidentConstructorUtil.getWordFromToken(otherWord);
            AccidentConcept otherWordConcept = ontoParser.findExactConcept(otherWordStr);

            if (!impactedVehicle.equals("") && otherWordConcept != null
                    && otherWordConcept.getLeafLevelName().equals("vehicle_impact_side")) {
                ConsoleLogger.print('d', "Found impacted vehicle side " + otherWordStr);

                // Find whether a left or right key word are assigned to the
                // impacted side
                String damagedSide = damagedComponentAnalyzer.findSideOfCrashedComponents(dependencyList, otherWord,
                        suspectedActor, tagWordList);
                ConsoleLogger.print('d', "Final damaged Side " + damagedSide);
            }
        }

        Iterator<String> suspectVehicleIter = suspectedVehicles.iterator();

        // Remove nouns that do not specify any vehicle identity
        while (suspectVehicleIter.hasNext()) {

            String susVeh = suspectVehicleIter.next();
            ConsoleLogger.print('d', "Sus vehicle: " + susVeh);
            if (susVeh.equals("vehicle") || !susVeh.startsWith("vehicle")) {
                suspectVehicleIter.remove();
                ConsoleLogger.print('d', suspectedVehicles);
            }
        }

        if (suspectedVehicles.size() == 1) {
            subjectVehicle = suspectedVehicles.iterator().next();
        }

        ConsoleLogger.print('d',
                "Suspected Vehicle Len: " + suspectedVehicles.size() + " Subject Vehicle " + subjectVehicle);
        return subjectVehicle;
    }

}
