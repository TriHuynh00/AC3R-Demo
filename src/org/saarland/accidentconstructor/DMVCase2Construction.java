package org.saarland.accidentconstructor;

import org.saarland.accidentdevelopmentanalyzer.CrashDevAnalyzer;
import org.saarland.accidentelementmodel.*;
import org.saarland.configparam.AccidentParam;
import org.saarland.crashanalyzer.CrashScenarioSummarizer;
import org.saarland.crashanalyzer.DamagedComponentAnalyzer;
import org.saarland.environmentanalyzer.EnvironmentAnalyzer;
import org.saarland.nlptools.StanfordCoreferencer;
import org.saarland.ontologyparser.OntologyHandler;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.concurrent.TimeUnit;

public class DMVCase2Construction {

    private OntologyHandler ontologyHandler;

    private AccidentConstructor accidentConstructor;

    private StanfordCoreferencer stanfordCoreferencer;

    private TestCaseRunner testCaseRunner;

    private String crashDesc = "A Cruise autonomous vehicle (“Cruise AV”), operating in autonomous mode, was traveling westbound " +
        "on Duboce Avenue between Guerrero Street and Market Street " +
        "when another vehicle made contact with the Cruise AV’s left rear corner, " +
        "damaging the lower left tail lamp assembly and left rear wheel well. " +
        "There were no injuries and police were not called.";

    // Construct the environment properties of this case
    public DMVCase2Construction(OntologyHandler ontoHandler, StanfordCoreferencer stanfordCoreferencer,
                                TestCaseRunner testRunner)
    {
        this.ontologyHandler = ontoHandler;
        this.stanfordCoreferencer = stanfordCoreferencer;
        accidentConstructor = new AccidentConstructor(ontologyHandler, "");
        testCaseRunner = testRunner;
    }

    public void constructCase() {
        constructCase1EnvironmentProp();
        contructCase1AccidentDevelopment(stanfordCoreferencer);
        constructBeamNGScenario(crashDesc);
    }

    private void constructCase1EnvironmentProp() {

        crashDesc = crashDesc.toLowerCase();
        crashDesc = accidentConstructor.replacePhrasesDMVCase(crashDesc);

        crashDesc = stanfordCoreferencer.findCoreference(crashDesc);
        crashDesc = crashDesc.trim();

        // Construct Weather and Lighting Props
        TestCaseInfo testCase1Info = new TestCaseInfo("dmvcase2");
        testCase1Info.putValToKey("weather", "clear");
        testCase1Info.setCrashType("rearend");
        accidentConstructor.setTestCase(testCase1Info);

        EnvironmentAnalyzer environmentAnalyzer = new EnvironmentAnalyzer();
        environmentAnalyzer.extractBasicRoadProperties(crashDesc, crashDesc, ontologyHandler,
                                                       accidentConstructor.getTestCase(),
                                                       accidentConstructor.getVehicleList(),
                                                       stanfordCoreferencer);

        Street street1 = testCase1Info.getStreetList().get(0);

        street1.putValToKey("lane_num", "2");
        street1.putValToKey("road_type", "street");
        street1.putValToKey("road_shape", RoadShape.STRAIGHT);
        street1.putValToKey("road_direction", "2-way");
        street1.putValToKey("curve_radius", "0");
        street1.putValToKey("road_grade_deg", "0");
//        street1.putValToKey("road_park_line", "3"); // #parking_lines : 0 - none; 1 - left only, 2 - right only, 3 - both
        street1.putValToKey("road_park_line_fill", "");

//        street2.putValToKey("lane_num", "2");
//        street2.putValToKey("road_type", "street");
//        street2.putValToKey("road_shape", RoadShape.STRAIGHT);
//        street2.putValToKey("road_direction", "2-way");
//        street2.putValToKey("curve_radius", "0");
//        street2.putValToKey("road_grade_deg", "0");
//        street2.putValToKey("road_park_line", "3");
    }

    private void contructCase1AccidentDevelopment(StanfordCoreferencer stanfordCoreferencer) {

        // TODO: Need a text preprocessor and PDF reader here
        String[] crashDescSentences = accidentConstructor.replacePhrases(crashDesc).split("\\.");

        ArrayList<VehicleAttr> vehicleList = new ArrayList<VehicleAttr>();
        accidentConstructor.setVehicleList(vehicleList);

        VehicleAttr vehicle1 = new VehicleAttr();
        VehicleAttr vehicle2 = new VehicleAttr();

        vehicle1.setVehicleId(1);
        vehicle2.setVehicleId(2);

        vehicle1.setColor("1 1 1");
        vehicle2.setColor("1 0 1");

        vehicle1.setBeamngVehicleModel("etk800");
        vehicle2.setBeamngVehicleModel("etk800");

        vehicle1.setPartConfig("vehicles/etk800/etk854t_A.pc");
        vehicle2.setPartConfig("vehicles/etk800/etk854t_A.pc");

        vehicleList.add(vehicle1);
        vehicleList.add(vehicle2);

        // Handle Coreference in each sentence
        for (int i = 0; i < crashDescSentences.length; i++) {
            String modSentence = stanfordCoreferencer.findCoreference(crashDescSentences[i]);
            crashDescSentences[i] = modSentence.trim();
        }

        // Analyze Pre-crash events
        ConsoleLogger.print('d', "Crash Description : " + crashDesc);
        CrashDevAnalyzer crashDevAnalyzer = new CrashDevAnalyzer(ontologyHandler);

        LinkedList<ActionDescription> actionList = new LinkedList<ActionDescription>();

        for (String sentence : crashDescSentences)
        {
            LinkedList<LinkedList<String>> relevantTaggedWordsAndDependencies = stanfordCoreferencer
                .findDependencies(sentence);
            crashDevAnalyzer.analyzeCrashDevelopment(relevantTaggedWordsAndDependencies,
                vehicleList, actionList);
        }

        crashDevAnalyzer.constructVehicleActionEventList(actionList, vehicleList);

        accidentConstructor.checkMissingPropertiesVehicles();

        if (actionList != null)
        {
            ConsoleLogger.print('d', "Action list has " + actionList.size() + " events");
            for (ActionDescription act : actionList)
            {
                ConsoleLogger.print('d', String.format("%s %s %s",
                    act.getSubject(), act.getVerb(), act.getVerbProps().toString()));
            }
        }

        for (VehicleAttr vehicle : vehicleList)
        {
            ConsoleLogger.print('d', String.format("Vehicle%d: %s \n Damage: %s \n",
                vehicle.getVehicleId(), vehicle.getActionList().toString(), vehicle.getDamagedComponents().toString()));
        }


    }

    private void constructBeamNGScenario(String crashDescription)
    {
        RearEndConstructor rearEndConstructor = new RearEndConstructor(accidentConstructor.getVehicleList(),
            ontologyHandler, accidentConstructor.getTestCase());
        rearEndConstructor.constructAccidentScenario(accidentConstructor.getVehicleList(), ontologyHandler);

        // If this is a non-critical case generation, remove the
        // impact coordinate (last coord), and set
        // the velocity of striker as the victim's speed + 10%
        // victim speed
        if (AccidentConstructorUtil.getNonCriticalDistance() > 0) {
            VehicleAttr[] strikerAndVictim = AccidentConstructorUtil.findStrikerAndVictimForRearEnd(
                accidentConstructor.getVehicleList().get(0), accidentConstructor.getVehicleList().get(1),
                AccidentParam.defaultCoordDelimiter);

            ArrayList<String> strikerCoordList = strikerAndVictim[0].getMovementPath();
            strikerCoordList.remove(strikerCoordList.size() - 1);

            if (strikerAndVictim[1].getVelocity() > 0
                && strikerAndVictim[1].getVelocity() < strikerAndVictim[0].getVelocity()) {
                strikerAndVictim[0].setVelocity((int) (strikerAndVictim[1].getVelocity()
                    + strikerAndVictim[1].getVelocity() * 0.1));
            }

        }

        String scenarioName = accidentConstructor.getTestCase().getName();

        try {
            if (AccidentConstructorUtil.getNonCriticalDistance() > 0) {
                scenarioName = scenarioName + "_non-critical";
            }
            // scenarioName =
            // accidentConstructor.testCase.getName().split("/")[1].split("\\.")[0];

            // ConsoleLogger.print('d', "Scenario Name Split for /: " +
            // scenarioName.split("/")[1].split("\\.")[0]);
            RoadConstructor roadConstructor = new RoadConstructor(accidentConstructor.getVehicleList(),
                accidentConstructor.getTestCase(), ontologyHandler);

            // Construct the road, vehicle, and waypoints objects
            String scenarioInfo = roadConstructor.constructRoadNodes(scenarioName);
            if (scenarioInfo.equals("fail")) {
                ConsoleLogger.print('d', "Fail to construct road due to same first coord");

                throw new Exception("Fail constructing road due to same first coord");
            }

            // Construct environment props
            EnvironmentConstructor environmentConstructor = new EnvironmentConstructor(
                accidentConstructor.getTestCase(), ontologyHandler);

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
            if (accidentConstructor.getAccidentType().contains("rear-end")
                || accidentConstructor.getAccidentType().contains("rearend")
                || accidentConstructor.getAccidentType().contains("rear end")) {
                strikerAndVictim = AccidentConstructorUtil.findStrikerAndVictimForRearEnd(
                    accidentConstructor.getVehicleList().get(0), accidentConstructor.getVehicleList().get(1),
                    AccidentParam.beamngCoordDelimiter);
            } else {
                strikerAndVictim = AccidentConstructorUtil.findStrikerAndVictim(
                    accidentConstructor.getVehicleList().get(0), accidentConstructor.getVehicleList().get(1));
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
                    strikerAndVictim[0].getStandingStreet()
                        .getStreetPropertyValue("speed_limit")
                ));
            }

            scenarioTemplateFile = scenarioTemplateFile
                .replace("$description", crashDescription)
                .replace("$strikerID", strikerAndVictim[0].getVehicleId() + "")
                .replace("$NLanes", strikerLaneNum + "").replace("$speedLimit", "" + speedLimit);

            String scenarioPath = AccidentParam.scenarioConfigFilePath + "\\" + scenarioName + ".json";

            Path scenarioConfigPath = Paths.get(scenarioPath);
            Files.write(scenarioConfigPath, scenarioTemplateFile.getBytes());

            testCaseRunner.setScenarioName(scenarioName);


            /*************************************************
             ************ END SCENARIO CONSTRUCTION **********
             *************************************************/

//            long endTime = (System.nanoTime() - startTime);
//            ConsoleLogger.print('r', String.format("Finish generating simulation %s after %d milliseconds\n",
//                scenarioName, TimeUnit.NANOSECONDS.toMillis(endTime)));
//            scenarioConstructionTime.put(scenarioName, TimeUnit.NANOSECONDS.toMillis(endTime));
            ConsoleLogger.print('d', "Final Street List");

            for (Street street : accidentConstructor.getTestCase().getStreetList()) {
                ConsoleLogger.print('d', "Street ID " + street.getStreetProp().get("road_ID"));
                street.printStreetInfo();
            }

            ConsoleLogger.print('d', "Waypoints track");
            for (VehicleAttr vehicle : accidentConstructor.getVehicleList()) {
                ConsoleLogger.print('d',
                    "Vehicle " + vehicle.getVehicleId() + " track : " + vehicle.getMovementPath().toString());
            }

            long scenarioStartTime = System.nanoTime();

            /************ BEGIN SCENARIO EXECUTION ***********/

            boolean hasCrash = testCaseRunner.runScenario(scenarioName);

            // Add BeamNG Server Socket handling here

            DamagedComponentAnalyzer crashAnalyzer = new DamagedComponentAnalyzer(accidentConstructor.getVehicleList(),
                ontologyHandler, scenarioName);

            crashAnalyzer.checkWhetherCrashOccur(hasCrash);

            /************ END SCENARIO EXECUTION ***********/
            long scenarioEndTime = System.nanoTime() - scenarioStartTime;
            ConsoleLogger.print('r', String.format("Finish running simulation after %d seconds\n",
                TimeUnit.NANOSECONDS.toSeconds(scenarioEndTime)));
        } catch (Exception ex) {
            DamagedComponentAnalyzer crashAnalyzer = new DamagedComponentAnalyzer(null, scenarioName);
            crashAnalyzer.markGenerationFailureCase();
            ConsoleLogger.print('r', "Error in generating case " + scenarioName);
            ex.printStackTrace();
            return;
        }
        CrashScenarioSummarizer csr = new CrashScenarioSummarizer();
        csr.summarizeAllScenarios();
    }

}
