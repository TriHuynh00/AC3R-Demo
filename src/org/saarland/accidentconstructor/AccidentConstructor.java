package org.saarland.accidentconstructor;

import java.awt.*;
import java.io.*;
import java.lang.reflect.Array;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import javax.swing.*;

//import org.jdom2.Element;
import org.saarland.accidentdevelopmentanalyzer.CrashDevAnalyzer;
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

    private ProcessBuilder processBuilder;

    private Process p;

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

    public AccidentConstructor(OntologyHandler parser, String accidentFilePath) {
        ontoParser = parser;
        testCase = new TestCaseInfo(accidentFilePath);
    }

    /*
     * Command line parsing interface
     */

    public interface AC3RCLI {

        @Option(defaultToNull = true, longName = "reports", description = "Name or path of reports to be used.")
        List<File> getReports();

        @Option(defaultToNull = true, longName = "path", description = "Path of scenario data will be generated.")
        String getScenarioDataPath();

        @Option(helpRequest = true, description = "Usage details on command-line arguments")
        boolean getHelp();
    }

    public static void main(String[] args) {
        int databaseType = 1; // 0 - MMUCC ; 1 - plain textual crash description
        // Parsing of input parameters
        boolean useGUI = true;
        File[] selectedFiles = null;
        String scenarioDataPath = "";

        FilenameFilter xmlFilter = new FilenameFilter() {
            @Override
            public boolean accept(File f, String name) {
                // We want to find only .c files
                return name.endsWith(".xml");
            }
        };

        try {
            AC3RCLI result = CliFactory.parseArguments(AC3RCLI.class, args);
            if (result.getReports() != null && !result.getReports().isEmpty()) {
                ArrayList<File> reportFiles = new ArrayList<File>();

                ConsoleLogger.print('d', "File List");

                for (File selectedFile : result.getReports().toArray(new File[]{})) {
//                    ConsoleLogger.print('d', selectedFile.getPath());
                    if (selectedFile.getPath().endsWith("\\*")) {
                        File directory = new File(selectedFile.getPath().replace("\\*", ""));

                        for (File file :  directory.listFiles(xmlFilter)) {
                            ConsoleLogger.print('d', file);
                            reportFiles.add(file);
                        }

                    }
                    else {
                        if (selectedFile.exists()) {
                            reportFiles.add(selectedFile);
                            ConsoleLogger.print('d', selectedFile.getPath());
                        } else {
                            ConsoleLogger.print('d', "Reject " + selectedFile.getPath() + ", File does not exist");
                        }

                    }

                }
                selectedFiles = reportFiles.toArray(new File[]{});
                ConsoleLogger.print('d', "Number of selected files " + selectedFiles.length);
                useGUI = false;
            }
            if (result.getScenarioDataPath() != null && !result.getScenarioDataPath().isEmpty()) {
                scenarioDataPath = result.getScenarioDataPath();
                if (!scenarioDataPath.endsWith("\\")) {
                    scenarioDataPath = scenarioDataPath + "\\";
                }
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

        NavigationDictionary.init();
        ConsoleLogger.print('r', "Navigation Dictionary initialized!");



        // Test plain textual crash description
//        if (System.getProperty("CRASHDBTYPE", "NMVCSS").equals("DMV")) {
//            useGUI = false;
//
//            DMVCase1Construction case1Construction = new DMVCase1Construction(ontologyHandler, stanfordCoreferencer,
//                                                                              testCaseRunner);
//            case1Construction.constructCase();
//
//            return;
//        }

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

                RoadConstructor baseRoadConstructor = new RoadConstructor(accidentConstructor.vehicleList,
                        accidentConstructor.testCase, ontologyHandler);
                baseRoadConstructor.constructBaseRoad();

                /******************************************
                 ******** END ENVIRONMENT ANALYSIS ********
                 ******************************************/

                // if (blockSignal) continue;

                /***************************************************
                 ******** BEGIN ACCIDENT DEVELOPMENT ANALYSIS ******
                 ***************************************************/

                LinkedList<LinkedList<ActionDescription>> storylineActionList = new LinkedList<LinkedList<ActionDescription>>();

                CrashDevAnalyzer crashDevAnalyzer = new CrashDevAnalyzer(ontologyHandler);

                LinkedList<ActionDescription> actionList = new LinkedList<ActionDescription>();

                for (String sentence : storyline) {
                    if (sentence.equalsIgnoreCase("") || sentence == null) {
                        continue;
                    }
                    Stemmer stemmer = new Stemmer();

                    LinkedList<LinkedList<String>> relevantTaggedWordsAndDependencies = stanfordCoreferencer
                            .findDependencies(sentence);

                    crashDevAnalyzer.analyzeCrashDevelopment(relevantTaggedWordsAndDependencies,
                            accidentConstructor.vehicleList, actionList);

//                    actionChain = accidentConstructor.extractActionChains(
//                        relevantTaggedWordsAndDependencies, ontologyHandler, stemmer, environmentAnalyzer);
//                    }
//                    accidentConstructor.constructVehicleActionList(actionList, ontologyHandler);
                }

//                crashDevAnalyzer.removeIndexInActionWord(actionList);
                crashDevAnalyzer.constructVehicleActionEventList(actionList, accidentConstructor.vehicleList);

                // If the vehicle has only 1 action, and it is a static one, then it will be impacted at last
                for (VehicleAttr vehicle : accidentConstructor.vehicleList) {
                    ConsoleLogger.print('d',
                            "Vehicle " + vehicle.getVehicleId() + " Actions : " + vehicle.getActionList().toString());
                    if (vehicle.getActionList().size() == 1){
                        int actionVelocity = Integer.parseInt(
                            ontologyHandler.findExactConcept(vehicle.getActionList().get(0))
                            .getDataProperties().get("velocity"));
                        if (actionVelocity == 0) {
                            vehicle.getActionList().add("hit*");
                        }
                    }
                }



                accidentConstructor.pruneActionTree(accidentConstructor.vehicleList);

                // SUPER PRUNE, APPLY ONLY TO 2 VEHICLES CASES, every action
                // after FIRST HIT ARE IGNORED
                accidentConstructor.superPrune(accidentConstructor.vehicleList);

                ConsoleLogger.print('d', "After pruning actions for scenario " + scenarioName);

                // Add the turn side into a turn or drift action
                for (VehicleAttr vehicle : accidentConstructor.vehicleList) {
                    String vehicleActionList = vehicle.getActionList().toString();
                    ConsoleLogger.print('d',
                            "Vehicle " + vehicle.getVehicleId() + " Actions : " + vehicleActionList);
                    if (vehicleActionList.contains("turn") || vehicleActionList.contains("drift")) {
                        ArrayList<Integer> recordedTurnActionIndexList = new ArrayList<Integer>();
                        for (int y = 0; y < vehicle.getActionList().size(); y++) {
                            if (vehicle.getActionList().get(y).contains("turn")

                                || vehicle.getActionList().get(y).contains("drift")) {
                                if (vehicle.getActionDescriptionList().get(y).getVerbProps().size() > 0) {
                                    String turnSide = vehicle.getActionDescriptionList().get(y).getVerbProps().get(0);
                                    vehicle.getActionList().set(y, "turn " + turnSide);
                                }

                            }
                        }

                    }
                }

                ConsoleLogger.print('d', "After post processing actions for scenario " + scenarioName);
                for (VehicleAttr vehicle : accidentConstructor.vehicleList) {
                    String vehicleActionList = vehicle.getActionList().toString();
                    ConsoleLogger.print('d',
                            "Vehicle " + vehicle.getVehicleId() + " Actions : " + vehicleActionList);
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
                    // Record the lapConfig in the scenario's JSON file
                    // Take the values from RoadConstructor class
                    ArrayList<String> waypointNameList = new ArrayList<>();
                    waypointNameList = roadConstructor.getWaypointNameList();
                    String lapConfig = waypointNameList.stream().collect(Collectors.joining(", ")).replace("'", "\"");
                    ConsoleLogger.print('d', "Lap Config: ");
                    ConsoleLogger.print('d', lapConfig);
                    ConsoleLogger.print('d', "End From Accd");
                    // Record the speed limit in the scenario's JSON file, if
                    // speed_limit is not specified, set it as -1
                    if (!strikerAndVictim[0].getStandingStreet().getStreetPropertyValue("speed_limit").equals("")) {
                        speedLimit = AccidentConstructorUtil.convertMPHToKMPH(Double.parseDouble(
                                strikerAndVictim[0].getStandingStreet().getStreetPropertyValue("speed_limit")));
                    }

                    scenarioTemplateFile = scenarioTemplateFile
                            .replace("$description", accidentContext[0] + "\n" + accidentContext[1])
                            .replace("$strikerID", strikerAndVictim[0].getVehicleId() + "")
                            .replace("$NLanes", strikerLaneNum + "")
                            .replace("$speedLimit", "" + speedLimit)
                            .replace("$lapConfig", lapConfig);

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

                /************ BEGIN SCENARIO DATA FILE ***********/
//                if (!useGUI) {
//
//                }

                // Map action to road segment
                for (VehicleAttr vehicle : accidentConstructor.vehicleList) {
                    ConsoleLogger.print('d',
                            "vehicle " + vehicle.getVehicleId() +
                                    " action & coord = " + accidentConstructor.mapActionToRoadSegment(vehicle));
                }

                accidentConstructor.generateScenarioJSONData(scenarioDataPath, scenarioName);
//                System.exit(0);
//                accidentConstructor.controlBeamNgAlgorithm(scenarioName);

                /************ END SCENARIO DATA FILE ***********/

                /************ BEGIN SCENARIO EXECUTION ***********/

                // boolean hasCrash = testCaseRunner.runScenario(scenarioName);

                // Add BeamNG Server Socket handling here

                // DamagedComponentAnalyzer crashAnalyzer = new DamagedComponentAnalyzer(accidentConstructor.vehicleList,
                // ontologyHandler, scenarioName);

                // crashAnalyzer.checkWhetherCrashOccur(hasCrash);
//                boolean hasCrash = testCaseRunner.runScenario(scenarioName);
//                DamagedComponentAnalyzer crashAnalyzer = new DamagedComponentAnalyzer(accidentConstructor.vehicleList, ontologyHandler, scenarioName);
//                crashAnalyzer.checkWhetherCrashOccur(hasCrash);
//                ConsoleLogger.print('d', "Finish running scenario");

                /************ END SCENARIO EXECUTION ***********/
//                long scenarioEndTime = System.nanoTime() - scenarioStartTime;
//                ConsoleLogger.print('r', String.format("Finish running simulation after %d seconds\n",
//                        TimeUnit.NANOSECONDS.toSeconds(scenarioEndTime)));

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
//        CrashScenarioSummarizer csr = new CrashScenarioSummarizer();
//        csr.summarizeAllScenarios();
//
//        long total = 0;
//        for (String key : scenarioConstructionTime.keySet()) {
//            ConsoleLogger.print('d', key + "," + scenarioConstructionTime.get(key));
//            total += scenarioConstructionTime.get(key);
//        }
//        ConsoleLogger.print('d', "Average: " + total / scenarioConstructionTime.size() + " ms");

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
                                    VehicleAttr travellingVehicle = AccidentConstructorUtil.findVehicle(actor, vehicleList);
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
                            String linkedWords = AccidentConstructorUtil.findAllConnectedWordsTopDown(dependencyList,
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
                                VehicleAttr actorVehicleObj = AccidentConstructorUtil.findVehicle(actor, vehicleList);
                                if (actorVehicleObj != null
                                        && actorVehicleObj.getTravelOnLaneNumber() == AccidentParam.RIGHTMOSTLANE) {

                                    String actConnectedWords = AccidentConstructorUtil
                                            .findAllConnectedWordsTopDown(dependencyList, action, action, 0, 5);

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
                                            VehicleAttr vehicleRef = AccidentConstructorUtil.findVehicle(
                                                    actor, vehicleList);

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
                                    VehicleAttr vehicleRef = AccidentConstructorUtil.findVehicle(actor, vehicleList);
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
                                                    .findAllConnectedWordsTopDown(dependencyList, actor, actor, 0, 2);

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

                                        // Attempt to detect the side of the parking action
                                        if (stemmedAction.equals("park")) {
                                            ConsoleLogger.print('d', "Found park action ");
                                            // Find from related word the park
                                            // location (left or right of the
                                            // road)
                                            LinkedList<String> relatedWordDependencies = AccidentConstructorUtil
                                                    .findConnectedDependencies(dependencyList, tagWordList,
                                                            relatedWordWithIndex, dependencyOfAction, 0);

                                            VehicleAttr actingVehicle = AccidentConstructorUtil.findVehicle(actor, vehicleList);

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
                                                            VehicleAttr travellingVehicle =
                                                                    AccidentConstructorUtil.findVehicle(actor, vehicleList);

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
                                                VehicleAttr attackerVehAttr = AccidentConstructorUtil.findVehicle(
                                                        attacker, vehicleList);
                                                if (attackerVehAttr.getDamagedComponents().size() == 0) {
                                                    ConsoleLogger.print('d', "Find all connected words " + attacker);
                                                    String wordChain = AccidentConstructorUtil.findAllConnectedWordsTopDown(
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
                        VehicleAttr foundVehicle = AccidentConstructorUtil.findVehicle(
                                vehicleName.split(" ")[0], vehicleList);
                        if (foundVehicle != null) {
                            foundVehicle.getActionList().add(impactIndicatorVerb);
                            processedVehicles.add(vehicleName);
                        }
                    }
                } else {
                    VehicleAttr foundVehicle = AccidentConstructorUtil.findVehicle(
                            actDes.getSubject().split(" ")[0], vehicleList);
                    ConsoleLogger.print('d', "Found Subject vehicle? " + foundVehicle == null ? "No"
                            : "Yes, found vehicle" + foundVehicle.getVehicleId());
                    if (foundVehicle != null) {
                        foundVehicle.getActionList().add(impactIndicatorVerb);
                        processedVehicles.add("vehicle" + foundVehicle.getVehicleId());
                    }
                }

                // Append action to the vehicle(s) in the object list
                for (String vehicleName : impactedVehicles) {
                    VehicleAttr foundVehicle = AccidentConstructorUtil.findVehicle(vehicleName, vehicleList);
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
                        VehicleAttr foundVehicle = AccidentConstructorUtil.findVehicle(vehicleName.split(" ")[0], vehicleList);
                        if (foundVehicle != null) {
                            foundVehicle.getActionList().add(impactIndicatorVerb);
                            processedVehicles.add(vehicleName);
                        }
                    }
                } else {
                    VehicleAttr foundVehicle = AccidentConstructorUtil.findVehicle(actDes.getSubject().split(" ")[0], vehicleList);
                    ConsoleLogger.print('d', "Found Subject vehicle? " + foundVehicle == null ? "No"
                            : "Yes, found vehicle" + foundVehicle.getVehicleId());
                    if (foundVehicle != null) {
                        foundVehicle.getActionList().add(impactIndicatorVerb);
                        processedVehicles.add("vehicle" + foundVehicle.getVehicleId());
                    }
                }

                // Append action to the vehicle(s) in the object list
                for (String vehicleName : impactedVehicles) {
                    VehicleAttr foundVehicle = AccidentConstructorUtil.findVehicle(vehicleName, vehicleList);
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

//    private void removeDuplicateAction(ArrayList<VehicleAttr> vehicleList) {
//        LinkedList<LinkedList<String>> vehicleActionList = new LinkedList<LinkedList<String>>();
//
//        for (int i = 0; i < vehicleList.size(); i++) {
//            vehicleActionList.add(vehicleList.get(i).getActionList());
//        }
//        // Remove duplicate actions
//        for (int i = 0; i < vehicleActionList.get(0).size() - 1; i++) {
//            boolean allVehicleSameAction = false;
//            // Check if all next action for each vehicle is the same
//            for (int j = 0; j < vehicleActionList.size(); j++) {
//                // ConsoleLogger.print('d',"Word of vehicle %d at pos %d is %s;
//                // at [%d, %d] is %s \n",
//                // j, i, vehicleActionList.get(j).get(i),
//                // j, i + 1, vehicleActionList.get(j).get(i + 1));
//
//                // Only prune if there are more than 1 duplicate action
//
//                if (i + 1 < vehicleActionList.get(j).size()
//                    && vehicleActionList.get(j).get(i).equals(vehicleActionList.get(j).get(i + 1))) {
//                    allVehicleSameAction = true;
//                } else {
//                    allVehicleSameAction = false;
//                    break;
//                }
//            }
//            if (allVehicleSameAction) {
//                for (int j = 0; j < vehicleActionList.size(); j++) {
//                    vehicleActionList.get(j).remove(i);
//                }
//                i--;
//            }
//        }
//    }

    private void removeDuplicateAction(ArrayList<VehicleAttr> vehicleList) {
        LinkedList<LinkedList<String>> vehicleActionList = new LinkedList<LinkedList<String>>();

        for (int i = 0; i < vehicleList.size(); i++) {
            vehicleActionList.add(vehicleList.get(i).getActionList());
        }
        // Remove duplicate actions
//        for (int i = 0; i < vehicleActionList.get(0).size() - 1; i++) {
        for (int i = 0; i < vehicleActionList.size(); i++) {
            boolean allVehicleSameAction = false;
            // Check if all next action for each vehicle is the same
            LinkedList<String> currVehicleActionList = vehicleActionList.get(i);
            for (int j = currVehicleActionList.size() - 1; j > 0 ; j--) {
                // ConsoleLogger.print('d',"Word of vehicle %d at pos %d is %s;
                // at [%d, %d] is %s \n",
                // j, i, vehicleActionList.get(j).get(i),
                // j, i + 1, vehicleActionList.get(j).get(i + 1));

                // Only prune if there are more than 1 duplicate action

                if (j > 0 && currVehicleActionList.get(j).equals(currVehicleActionList.get(j - 1))) {
                    currVehicleActionList.remove(j);
                }
            }
        }
    }

    private void superPrune(ArrayList<VehicleAttr> vehicleList) {
        LinkedList<LinkedList<String>> vehicleActionList = new LinkedList<LinkedList<String>>();
        LinkedList<LinkedList<ActionDescription>> vehicleActionDescriptionList = new LinkedList<LinkedList<ActionDescription>>();

        for (int i = 0; i < vehicleList.size(); i++) {
            vehicleActionList.add(vehicleList.get(i).getActionList());
            vehicleActionDescriptionList.add(vehicleList.get(i).getActionDescriptionList());
        }

        for (int c = 0; c < vehicleActionList.size(); c++) {
            for (int i = 0; i < vehicleActionList.get(c).size(); i++) {
                // Check if the action of this step is equal to another step
                if (vehicleActionList.get(c).get(i).equals("hit") ||
                    vehicleActionList.get(c).get(i).equals("hit*")) {
                    ConsoleLogger.print('d', "hit word reached super prune at " + i);
                    // Prune everything behind this index
//                    for (int e = 0; e < vehicleActionList.size(); e++) {
                        for (int k = vehicleActionList.get(c).size() - 1; k > i; k--) {
                            vehicleActionList.get(c).remove(k);
                            vehicleActionDescriptionList.get(c).remove(k);
                            ConsoleLogger.print('d', "Removed VehicleList c=" + c + " k=" + k);
                        }
//                    }
                    for (int e = 0; e < vehicleActionList.size(); e++) {
                        ConsoleLogger.print('d', "VehicleList e=" + e + " :" + vehicleActionList.get(e));

                    }
                    break;
                }
            }
        }
    }

    // If there are only two actions, and the first one is hit, swap the two actions
    private void swapHitAction(ArrayList<VehicleAttr> vehicleList){
        LinkedList<String> vehicleActionList = new LinkedList<String>();

        for (VehicleAttr vehicle : vehicleList) {
            vehicleActionList = vehicle.getActionList();

            if (vehicleActionList.size() == 2) {
                // If the first action is hit, and the second action is a static one (park, stop),
                // Swap the two actions
                if (vehicleActionList.get(0).startsWith("hit")
                    && ontoParser.findExactConcept(vehicleActionList.get(1)).getDataProperties()
                    .get("velocity").equals("0")) {
                    String temp = vehicleActionList.get(1);
                    vehicleActionList.set(1, vehicleActionList.get(0));
                    vehicleActionList.set(0, temp);
                }
            }
        }


    }

    // Remove N/A and duplicate actions
    private void pruneActionTree(ArrayList<VehicleAttr> vehicleList) {
        removeInvalidActions(vehicleList);
        removeDuplicateAction(vehicleList);
        swapHitAction(vehicleList);


        ConsoleLogger.print('d', "After preprocessing");

        for (VehicleAttr vehicle : vehicleList) {
            ConsoleLogger.print('d',
                    "Vehicle " + vehicle.getVehicleId() + " Actions : " + vehicle.getActionList().toString());
        }


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

    public void checkMissingPropertiesVehicles() {
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

                vehicle.setTravellingDirection("E");
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

            // If the first action in vehicle's event is hit, then add a travelling action before it.
            // This only apply to DMV crash scenarios
            if (System.getProperty("CRASHDBTYPE", "NVMCSS").equals("DMV"))
            {
                if (vehicle.getActionList().get(0).startsWith("hit"))
                {
                    vehicle.getActionList().add(0, "travel");
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
    public String replacePhrases(String paragraph) {
        String[] phrases = new String[] { "speed limit", "stop sign", "traffic sign", "traffic light" };
        for (String phrase : phrases) {
            String[] phraseWords = phrase.split(" ");
            paragraph = paragraph.replace(phrase, phraseWords[0] + "_" + phraseWords[1]);
        }

        if (paragraph.contains("made contact") || paragraph.contains("make contact")) {
            paragraph = paragraph.replace("made contact", "impact");
            paragraph = paragraph.replace("make contact", "impact");
        }
        return paragraph;
    }

    public String replacePhrasesDMVCase(String paragraph) {
        replacePhrases(paragraph);
        String[] vehicle2Phrases = new String[] {"another vehicle", "other vehicle", "passenger vehicle"};
        String[] vehicle1Phrases = new String[] {" av", " autonomous vehicle"};
        for (String phrase : vehicle2Phrases) {
            paragraph = paragraph.replace(phrase, "vehicle2");
        }

        for (String phrase : vehicle1Phrases) {
            paragraph = paragraph.replace(phrase, " vehicle1");
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
                } else { // If this is not an anonymous vehicle, set it as suspected vehicle
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

    private static String formatJSONKey(String str) {
        return "\n\"" + str + "\"" + ":";
    }
    private static String formatJSONValueString(String str) {
        return "\"" + str + "\",";
    }

    private static String removeLastChar(String str) {
        return removeLastChars(str, 1);
    }

    private static String removeLastChars(String str, int chars) {
        return str.substring(0, str.length() - chars);
    }

    private String writeRoadObjects(ArrayList<Street> streetList) {
        String roadData = "";
        for (Street street : streetList) {
            roadData += '{';
            // Structuring data format for road properties in JSON
            String roadName = formatJSONKey("name") + formatJSONValueString("road" + street.getStreetPropertyValue("road_ID"));
            String roadType = formatJSONKey("road_type") + formatJSONValueString(street.getStreetPropertyValue("road_type"));
            String roadShape = formatJSONKey("road_shape") + formatJSONValueString(street.getStreetPropertyValue("road_shape"));
            String roadNodeList = "road_node_list";
            String[] paths = street.getStreetPropertyValue(roadNodeList)
                    .replaceAll(" ", ",").split(";");
            List<String> pathList = Arrays.asList(paths);
            ArrayList<String> points = new ArrayList<String>();
            /* Now we only take 1st and last elements of road node list */
            points.add('[' + pathList.get(0) + ']');
            points.add('[' + pathList.get(pathList.size() - 1) + ']');
            /* Following code will take all elements to json data but not now
            for(String point: pathList){
                points.add("[" + point + "]");
            }
            */
            roadNodeList = formatJSONKey(roadNodeList) + points.toString();

            // Update scenario JSON data
            roadData += roadName + roadType + roadShape + roadNodeList + "\n";
            roadData += "},";
        }
        return removeLastChar(roadData);
    }

    private String writeVehicleObjects(ArrayList<VehicleAttr> vehicleList) {
        String vehicleData = "";
        for (VehicleAttr vehicle : vehicleList) {
            vehicleData += '{';
            String vName = formatJSONKey("name") + formatJSONValueString("vehicle" + vehicle.getVehicleId());
            String vColor = formatJSONKey("color") + formatJSONValueString(vehicle.getColor());

            // Get rotation degree
            String travelDirection = vehicle.getTravellingDirection();
            ArrayList<Double> rotDeg;
            switch (travelDirection) {
                case "W":
                    rotDeg = new ArrayList<>(Arrays.asList(-0.00993774, -0.03947598, 0.70593404, 0.7071068));
                    break;
                case "N":
                    rotDeg = new ArrayList<>(Arrays.asList(-1.40540931e-02, -5.58274572e-02,  9.98341513e-01, -4.37275224e-08));
                    break;
                case "E":
                    rotDeg = new ArrayList<>(Arrays.asList(0.00993774, 0.03947598, -0.70593404,  0.7071068));
                    break;
                case "S":
                    rotDeg = new ArrayList<>(Arrays.asList(3.45267143e-04, 0.00000000e+00, 0.00000000e+00, 9.99999940e-01));
                    break;
                case "SW":
                    rotDeg = new ArrayList<>(Arrays.asList(-0.00593952, -0.02359371,  0.42191736,  0.90630778));
                    break;
                default:
                    rotDeg = new ArrayList<>(Arrays.asList(0.00673789,  0.02676513, -0.47863063,  0.87758244));
            }
            String vRotQuat = formatJSONKey("rot_quat") + rotDeg.toString() + ",";

            // Get damage components of each vehicle
            String damageComponentData = vehicle.getDamagedComponents().size() == 0  ?
                    "any" : vehicle.getDamagedComponents().get(0);
            String vDamage = formatJSONKey("damage_components") + formatJSONValueString(damageComponentData);
            String vDriving = formatJSONKey("driving_actions") + "[ ";
            HashMap<String, LinkedList> vehicleActionAndCoord = mapActionToRoadSegment(vehicle);
            VehicleTrajectoryFactory vtf = new VehicleTrajectoryFactory(vehicle, vehicleActionAndCoord);
            ArrayList<VehicleTrajectory> vehicleTrajectories = vtf.generateVehicleTrajectories();

            for (VehicleTrajectory v : vehicleTrajectories) {
                vDriving += '{';
                vDriving += formatJSONKey("name") + formatJSONValueString(v.getAction());
                vDriving += formatJSONKey("trajectory") + v.getTrajectory().toString() + ",";
                vDriving += formatJSONKey("speed") + vehicle.getVelocity();
                vDriving += "},";
            }
            vDriving = removeLastChar(vDriving) + "]";

            vehicleData += vName + vColor + vRotQuat + vDamage + vDriving;
            vehicleData += "},";
        }
        vehicleData = removeLastChar(vehicleData);
        return vehicleData;
    }

    private String writeExpectedCrashDataFromReport(ArrayList<VehicleAttr> vehicleList) {
        String crashData = "";
        for (VehicleAttr vehicle : vehicleList) {
            if (vehicle.getDamagedComponents().size() > 0) {
                crashData += '{';
                String vName = "v" + vehicle.getVehicleId();

                // Get damage components of each vehicle
                String damageComponentData = "";
                for(String components: vehicle.getDamagedComponents()) {
                    damageComponentData += "{\"name\":\""+ components+"\",\"damage\":0},";
                }
                damageComponentData = removeLastChar(damageComponentData);
                String vDamage = '[' + damageComponentData + ']';
                crashData += formatJSONKey(vName) + vDamage;
                crashData += "},";
            }
        }
        if (crashData.length() > 0) {
            crashData = removeLastChar(crashData);
        }
        return crashData;
    }

    private void generateScenarioJSONData(String scenarioDataPath, String scenarioName) {
        ConsoleLogger.print('r', "\n\nStart to write scenario data file");
        if (scenarioDataPath.isEmpty()) {
            scenarioDataPath = AccidentParam.scenarioConfigFilePath + "\\";
        }
        ConsoleLogger.print('r', "\n\nThe scenario data file will be written at " + scenarioDataPath);
        scenarioDataPath = scenarioDataPath + scenarioName + "_data.json"; // Append file name
        String scenarioData = "{"; // Starting json file
        scenarioData += formatJSONKey("name") + formatJSONValueString(scenarioName);

        try (FileWriter scenarioDataWriter = new FileWriter(scenarioDataPath)) {
            /* Start writing road objects */
            scenarioData += formatJSONKey("roads") + '[' + writeRoadObjects(this.testCase.getStreetList()) + "],";

            /* Start writing vehicle objects */
            scenarioData += formatJSONKey("vehicles") + '[' + writeVehicleObjects(this.vehicleList) + "],";

            /* Start writing expected crash data from report */
            scenarioData += formatJSONKey("expected_crash_components") + '[' + writeExpectedCrashDataFromReport(this.vehicleList) + "]";

            /* End json file */
            scenarioData += '}';

            scenarioDataWriter.write(scenarioData);
            ConsoleLogger.print('r', "Successfully wrote to the file.");
        } catch (IOException e) {
            ConsoleLogger.print('r', "An error occurred in writing scenario data file.");
            e.printStackTrace();
        }
    }

    private void controlBeamNgAlgorithm(String scenarioName) {
        try {
            while (true)
            {
                // String cmdExec = FilePathsConfig.BeamNGProgramPath
                // + " -userpath " + AccidentParam.beamNGUserPath
                // + " -rhost 127.0.0.1 -rport " + port
                // + " -lua registerCoreModule('util_researchGE') ";

                String cmdExec = "python " + AccidentParam.beamNGpyPlusPath + " " + scenarioName + "_data.json";
                ConsoleLogger.print('d', "cmdExec: ");
                ConsoleLogger.print('d', cmdExec);

                processBuilder = new ProcessBuilder();
                processBuilder.command("cmd.exe", "/c", cmdExec);
                p = processBuilder.inheritIO().start();

                ConsoleLogger.print('r', "Listening to BeamNG client");
                // beamngClient = beamngServerSocket.accept();
                // dataInputStream = new DataInputStream(beamngClient.getInputStream());
                // dataOutputStream = new DataOutputStream(beamngClient.getOutputStream());
                ConsoleLogger.print('r', "BeamNG Client accepted");

                ConsoleLogger.print('r', "Load Scenario");

                // startScenario();
//                while (hasCrashStatus == false)
//                {
//                    // Keep looping until a crash file is generated, or scenario is timed out
//                }
//
//                hasCrashStatus = false;
//                Thread.sleep(1000);
                // beamngClient.close();
                ConsoleLogger.print('r', "Close BeamNG connection ");
                break;
            }
//            System.exit(0);

        } catch (Exception ex) {
            ConsoleLogger.print('e', "Error at control BeamNG client \n" + ex.toString());

        }
    }

    private HashMap<String, LinkedList> mapActionToRoadSegment(VehicleAttr vehicle) {

        // Loop through each action to determine road segment
        LinkedList<String> vehicleActionList = vehicle.getActionList();
        ArrayList<String> vehiclePath = vehicle.getMovementPath();
        HashMap<String, LinkedList> actionAndCoordinate = new HashMap<String, LinkedList>();

        LinkedList<String> turnActionAndCoord = new LinkedList<String>();
        LinkedList<String> impactActionAndCoord = new LinkedList<String>();
        LinkedList<String> followActionAndCoord = new LinkedList<String>();
        LinkedList<String> stopActionAndCoord = new LinkedList<String>();

        int currentCoordIndex = vehiclePath.size() >= 2 ? vehiclePath.size() - 2 : vehiclePath.size() - 1;

        boolean isRearEndCrash = false;

        if (testCase.getCrashType().contains("rear end")
            || testCase.getCrashType().contains("rear-end")
            || testCase.getCrashType().contains("rearend")
            || testCase.getCrashType().contains("forward impact")) {
            isRearEndCrash = true;
            ConsoleLogger.print('d', "Parsing JSON action for rearend case");
            // set the impact as the last point in vehicle path for rear-end cases

            if (!testCase.getCrashType().equals("forward impact")
                || (testCase.getCrashType().equals("forward impact") && vehiclePath.size() == 1)) {

                impactActionAndCoord.add(outputTrajectoryFormat(
                    vehiclePath.get(vehiclePath.size() - 1)));
            } else {
                impactActionAndCoord.add(outputTrajectoryFormat(
                    vehiclePath.get(vehiclePath.size() - 2)));
            }

            // Make a movement from the penultimate to final point
            if (vehiclePath.size() > 1) {
                followActionAndCoord.add(outputTrajectoryFormat(
                    vehiclePath.get(vehiclePath.size() - 2),
                    vehiclePath.get(vehiclePath.size() - 1)));
            }

        } else {
            // Find the last point which other cars also share
            if (vehicleList.size() == 2) {
                VehicleAttr otherVehicle = vehicleList.get(1 - (vehicle.getVehicleId() - 1));

                // If the last 2 coords of other vehicle match one of the last two coords of current vehicle
                // it is the crash point
                for (int i = vehiclePath.size() - 2; i < vehiclePath.size(); i++) {
                    String currentPoint = vehiclePath.get(i);
                    boolean foundCrashPoint = false;
                    for (int j = otherVehicle.getMovementPath().size() - 2;
                         j < vehicle.getMovementPath().size(); j++) {

                        String otherPoint = otherVehicle.getMovementPath().get(j);

                        if (currentPoint.trim().equals(otherPoint.trim())) {

                            impactActionAndCoord.add(outputTrajectoryFormat(currentPoint));
                            foundCrashPoint = true;
                            break;
                        }
                    }
                    if (foundCrashPoint) break;

                }

            }
        }

        // Loop through each action
        for (int i = vehicleActionList.size() - 2; i >= 0; i--) {
            String currentAction = vehicleActionList.get(i);
            int turnAngle = 0;

            if (currentAction.contains(" ")) {
                currentAction = currentAction.split(" ")[0];
            }

            ConsoleLogger.print('d', "current Action is " + currentAction);

            // if the action is a turn, map 3 points: 1 point before the currentCoordIndex
            // , the currentCoordIndex, and 1 point after the currentCoordIndex
            AccidentConcept currentActionConcept = ontoParser.findExactConcept(currentAction);

            if (currentActionConcept.getDataProperties().get("turn_angle") != null)
                turnAngle = Integer.parseInt(
                    currentActionConcept.getDataProperties().get("turn_angle"));

            if ((currentAction.startsWith("turn") || turnAngle >= 45)
                && currentCoordIndex - 1 >= 0 && currentCoordIndex + 1 < vehiclePath.size()) {

                ConsoleLogger.print('d', "Turn Action is " + currentAction);

                turnActionAndCoord.add(outputTrajectoryFormat(
                    vehiclePath.get(currentCoordIndex - 1),
                    vehiclePath.get(currentCoordIndex),
                    vehiclePath.get(currentCoordIndex + 1)
                ));

                currentCoordIndex -= 1;
            } else {

                int velocity = Integer.parseInt(
                    ontoParser.findExactConcept(currentAction).getDataProperties().get("velocity"));

                if ((velocity > 0 || velocity < 0) && velocity < 1000) {
                    if (turnAngle == 0 && currentCoordIndex - 1 >= 0) {

                        String actionAndCoord = outputTrajectoryFormat(
                            vehiclePath.get(currentCoordIndex - 1),
                            vehiclePath.get(currentCoordIndex));

                        followActionAndCoord.add(actionAndCoord);

                        // set the impact as the 2nd last point in vehicle path for non-rear-end cases
                        if (!isRearEndCrash) {
                            if (currentCoordIndex - 2 >= 0) {

                                // If the action before impact is a turn,
                                // then the action after impact is a follow action
                                if (i == vehicleActionList.size() - 2) {

                                    followActionAndCoord.add(outputTrajectoryFormat(
                                        vehiclePath.get(vehiclePath.size() - 2),
                                        vehiclePath.get(vehiclePath.size() - 1)));
                                }
                            }
                        }
                        currentCoordIndex--;


                    }
                } else if (velocity == 0) {
                    stopActionAndCoord.add(String.format("[%s]",
                        vehiclePath.get(currentCoordIndex).replaceAll(" ", ",")));

                    if (currentCoordIndex - 1 >= 0) {

                        followActionAndCoord.add(outputTrajectoryFormat(
                            vehiclePath.get(currentCoordIndex - 1),
                            vehiclePath.get(currentCoordIndex)));
                    }

                    currentCoordIndex--;
                }
            }
        }
        // Final check:  if there is no point being covered in the action coordinates,
        // add that point as a straight action
        for (int k = vehiclePath.size() - 1; k >= 0 ; k--) {
            String unassignedCoord = outputTrajectoryFormat(vehiclePath.get(k));
            if (!followActionAndCoord.toString().contains(unassignedCoord)
                && !stopActionAndCoord.toString().contains(unassignedCoord)
                && !turnActionAndCoord.toString().contains(unassignedCoord)
                && !impactActionAndCoord.toString().contains(unassignedCoord))
            {

                if (k == vehiclePath.size() - 1) {
                    followActionAndCoord.add(outputTrajectoryFormat(
                        vehiclePath.get(k - 1),
                        vehiclePath.get(k)));
                } else {
                    followActionAndCoord.add(outputTrajectoryFormat(
                        vehiclePath.get(k),
                        vehiclePath.get(k + 1)));
                }
            }
        }

        actionAndCoordinate.put("follow", followActionAndCoord);
        actionAndCoordinate.put("turn", turnActionAndCoord);
        actionAndCoordinate.put("stop", stopActionAndCoord);
        actionAndCoordinate.put("impact", impactActionAndCoord);
        return actionAndCoordinate;
    }

    private String outputTrajectoryFormat(String... coords) {
        String coordArraysStr = String.format("[%s]",
            coords[0].replace(" ", ","));

        for (int i = 1; i < coords.length; i++) {
            coordArraysStr += String.format(", [%s]", coords[i].replaceAll(" ", ","));
        }
        String trajectoryJSON = String.format("[[%s]]", coordArraysStr);
        return coordArraysStr;
    }

}
