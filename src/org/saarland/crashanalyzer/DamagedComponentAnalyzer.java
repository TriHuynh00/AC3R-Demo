package org.saarland.crashanalyzer;

import org.saarland.accidentconstructor.AccidentConstructorUtil;
import org.saarland.accidentconstructor.ConsoleLogger;
import org.saarland.accidentelementmodel.VehicleAttr;
import org.saarland.configparam.AccidentParam;
import org.saarland.configparam.FilePathsConfig;
import org.saarland.ontologyparser.OntologyHandler;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.LinkedList;

public class DamagedComponentAnalyzer {

    private ArrayList<VehicleAttr> vehicleList;

    private OntologyHandler ontologyHandler;

    private String scenarioName;

    public DamagedComponentAnalyzer(ArrayList<VehicleAttr> vehicleList, String name)
    {
        this.vehicleList = vehicleList;
        scenarioName = name;
    }

    public DamagedComponentAnalyzer(ArrayList<VehicleAttr> vehicleList, OntologyHandler parser, String name)
    {
        this.vehicleList = vehicleList;
        ontologyHandler = parser;
        scenarioName = name;
    }

    // When a damaged component (back, rear, front, side) is found, this function will find which whether a
    // component of another car.
    public String findSideOfCrashedComponents(LinkedList<String> dependencyList, String damagedComponent,
                                              String actorID, LinkedList<String> taggedWordList)
    {

        LinkedList<String> dependencies = AccidentConstructorUtil.findConnectedDependencies
                (dependencyList, taggedWordList, damagedComponent, "", 0);

        // Scan for the word left or right in the dependencies
        damagedComponent = AccidentConstructorUtil.getWordFromToken(damagedComponent);

        String vehicleName = "";

        String relatedWords = AccidentConstructorUtil.findAllConnectedWordsBottomUp(
            dependencyList, damagedComponent, damagedComponent, 0, 1);

//        for (String dependency : dependencies)
        for (String word : relatedWords.split(","))
        {
            // If we found that this word has a connection to another vehicle not the actorID, we know that this component
            // belongs to another vehicle
//            if (word.contains("vehicle"))
            if (word.startsWith("vehicle"))
            {
//                String[] wordPair = AccidentConstructorUtil.getWordPairFromDependency(word);
//                for (String token : wordPair)
//                {
//                    String word = AccidentConstructorUtil.getWordFromToken(token);
//                    if (word.matches("vehicle\\d+"))
//                    {
                        vehicleName = AccidentConstructorUtil.getWordFromToken(word);
                        ConsoleLogger.print('d',"Found " + vehicleName + " has damage at " + damagedComponent);
//                    }
//                }
            }
            boolean foundDmgSide = false;
            // Record left/right side of impact
            for (String side : AccidentParam.LEFTRIGHTARR)
            {
                if (word.startsWith(side + "-"))
                {
                    damagedComponent += " " + side;
                    foundDmgSide = true;
                }
            }

            if (foundDmgSide) break;
//            else if (word.startsWith(AccidentParam.LEFTRIGHTARR[0]))
//            {
//                damagedComponent += " left";
//            }
//            else if (word.contains("right-"))
//            {
//                damagedComponent += " right";
//            }
        }

        VehicleAttr damagedVehicle = null;

        if (!vehicleName.equals("") && !vehicleName.equals("vehicle") && !vehicleName.equals("vehicles"))
        {
            System.out.println("Vehicle name " + vehicleName + " after replace is " + vehicleName.replace("vehicle", ""));
            damagedVehicle = AccidentConstructorUtil.findVehicleBasedOnId(
                    Integer.parseInt(vehicleName.replace("vehicle", "")), vehicleList);
        }
        else
        {
            damagedVehicle = AccidentConstructorUtil.findVehicleBasedOnId(
                    Integer.parseInt(actorID.replace("vehicle", "")), vehicleList);
        }


        ConsoleLogger.print('d',"Damaged vehicle is " + damagedVehicle.getVehicleId() + " with damaged component " + damagedComponent);

        // Check if damaged component and position already exists
        boolean hasDmgComponent = false;
        for (String dmgComponent : damagedVehicle.getDamagedComponents()) {
            if (dmgComponent.contains(damagedComponent)) {
                hasDmgComponent = true;
                break;

            }
        }

        if (!hasDmgComponent) {
            damagedVehicle.getDamagedComponents().add(damagedComponent);
            ConsoleLogger.print('d', "Update Damaged vehicle is " + damagedVehicle.getVehicleId() +
                " with damaged component " + damagedComponent);
        }

        return damagedComponent;
    }

    public void markGenerationFailureCase()
    {
        writeDamageAnalysisResult(scenarioName + ":" + AccidentParam.FAILED_TO_GENERATE);
    }

    public boolean checkWhetherCrashOccur(boolean hasCrash)
    {
        // Read all the files that contain scenario name
        File caseDamageRecordFolder = new File(FilePathsConfig.damageRecordLocation);
        File[] files = AccidentConstructorUtil.getAllFilesContainName(scenarioName, caseDamageRecordFolder);

        // If there is only 1 file, check whether this is a crash file
        if (!hasCrash)
        {
            writeDamageAnalysisResult(scenarioName + ":" + AccidentParam.NO_CRASH_STR);
            return false;
        }
        else if (files.length == 1)
        {
            // If this is a noCrash file, stop here and record that the test case fails
            if (files[0].getName().contains(AccidentParam.NO_CRASH_STR))
            {
                writeDamageAnalysisResult(scenarioName + ":" + AccidentParam.NO_CRASH_STR);
            }
        }
        // More than 1 file => read the crash components
        else if (files.length > 1)
        {
            ArrayList<String> damageSummary = new ArrayList<String>();

            for (File crashComponentFile : files)
            {
                try {
                    String crashData = Files.readAllLines(Paths.get(
                            FilePathsConfig.damageRecordLocation + crashComponentFile.getName()),
                            Charset.defaultCharset()).get(0);

                    ConsoleLogger.print('d',"Data for file " + crashComponentFile.getName() + " " + crashData);

                    damageSummary.add(summarizeDamageComponent(crashData));


                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

            ConsoleLogger.print('d',"Damage Summary");
            ConsoleLogger.print('d', damageSummary);

            // Verify whether crashed component matches with the description
            verifyCrashedComponents(damageSummary);
        }


        return false;
    }

    private String summarizeDamageComponent(String damageData)
    {
        String damageSummary = "";

        // Split vehicle and damage components
        String[] damagedComponents = damageData.split(":");

        int vehicleId = -1;
        for (String component : damagedComponents)
        {

            if (component.startsWith("v"))
            {
                vehicleId = Integer.parseInt(component.replace("v", ""));
                damageSummary += vehicleId + ":";

            }
            else if (component.contains("-"))
            {
                String damagedComponent = component.split("-")[0];

                String translatedDamageExpr = ""; // expand the abbreviation recorded in damagedComponent

                // Unknown damaged parts
                if (damagedComponent.equals("NA"))
                {
                    translatedDamageExpr += "unknown";
                }
                // Find Front, Middle, Rear damage position
                else if (damagedComponent.startsWith("F"))
                {
                    translatedDamageExpr += "front ";
                }
                else if (damagedComponent.startsWith("M"))
                {
                    translatedDamageExpr += "middle ";
                }
                else if (damagedComponent.startsWith("R"))
                {
                    translatedDamageExpr += "rear ";
                }

                if (damagedComponent.endsWith("L"))
                {
                    translatedDamageExpr += "left";
                }
                else if (damagedComponent.endsWith("R"))
                {
                    translatedDamageExpr += "right";
                }

                translatedDamageExpr += ":";

                damageSummary += translatedDamageExpr;
            }
        }

        return damageSummary;
    }

    /**
     * Verify whether recorded crashed components from BeamNG matches with the crash components that are extracted from
     * the crash description
     * @param damageSummary a list of damaged components for each vehicle, the vehicle ID is the index of the array plus 1
     */
    private void verifyCrashedComponents(ArrayList<String> damageSummary)
    {
        String verificationResult = scenarioName + ":";
        // Compare each damaged component from the scenario to the recorded damaged one of the vehicle
        for (int i = 0; i < damageSummary.size(); i++)
        {
            String[] damagedComponents = damageSummary.get(i).split(":");

            int vehicleID = Integer.parseInt(damagedComponents[0]);

            VehicleAttr currentVehicle = AccidentConstructorUtil.findVehicleBasedOnId(vehicleID, vehicleList);

            verificationResult += vehicleID;

            // Strange record, skip
            if (damagedComponents.length <= 1)
            {
                verificationResult += "-U";
                continue;
            }


            // Damage too small, record as unknown and skip to the next vehicle
            if (damagedComponents[1].equals("unknown") || damagedComponents[1].equals(""))
            {
                verificationResult += "-U:";
                continue;
            }

            ConsoleLogger.print('d',"Vehicle #" + currentVehicle.getVehicleId() + " damaged components " + currentVehicle.getDamagedComponents());

//            for (String recordedDamage : currentVehicle.getDamagedComponents())
//            {
            // If the vehicle does not have any damage component, state "any"
            String recordedDamage = currentVehicle.getDamagedComponents().size() == 0  ?
                    "any" : currentVehicle.getDamagedComponents().get(0);

            String[] recordedDamagePositionAndSide = recordedDamage.split(" ");

            // TODO: Need to modify the ontology to handle the same component - diff name problem more conveniently

            if (recordedDamagePositionAndSide[0].equals("back") || recordedDamagePositionAndSide[0].equals("tail"))
            {
                recordedDamagePositionAndSide[0] = recordedDamagePositionAndSide[0]
                        .replace(recordedDamagePositionAndSide[0], "rear");
            }
            else if (recordedDamagePositionAndSide[0].equals("head"))
            {
                recordedDamagePositionAndSide[0] = recordedDamagePositionAndSide[0]
                        .replace(recordedDamagePositionAndSide[0], "front");
            }

            for (int k = 1; k < damagedComponents.length; k++)
            {
                verificationResult += "-";
                String[] damagedComponentPosAndSide = damagedComponents[k].split(" ");

                // Damage for this car in the crash report is any, if there is any spotted damage, record it as total match
                if (recordedDamagePositionAndSide[0].equals("any") && (!recordedDamagePositionAndSide[0].equals("")
                        || !recordedDamagePositionAndSide[0].contains("U")))
                {
                    ConsoleLogger.print('d',"Found ANY matched components for vehicle#" + vehicleID);
                    verificationResult += "PS";
                }
                // Verify if crashed position matched
                if (damagedComponentPosAndSide[0].equals(recordedDamagePositionAndSide[0])
                        || recordedDamagePositionAndSide[0].equals("side"))
                {
                    ConsoleLogger.print('d',"Found matched components for vehicle#" + vehicleID);
                    verificationResult += "P";
                }

                if (recordedDamagePositionAndSide.length == 1 &&  !recordedDamagePositionAndSide[0].equals("any") // Only damaged position is recorded, not side
                    || (damagedComponentPosAndSide.length == 2 && recordedDamagePositionAndSide.length == 2
                    && damagedComponentPosAndSide[1].equals(recordedDamagePositionAndSide[1])) )
                {
                    ConsoleLogger.print('d',"Found matched components for vehicle#" + vehicleID);
                    verificationResult += "S";
                }
            } // End loop through simulated damaged components
//            } // End loop through vehicle record damaged components
            verificationResult += ":";
        } // End loop through all damaged components in simulation
        writeDamageAnalysisResult(verificationResult);
    }

    /**
     * Record whether the crashed components in the simulation matches with the description of the crash report
     * @param result the String contains [scenarioName]:[vehicle1DamageComponentMatched]:[vehicle2DamageComponentMatched]
     * @return
     */
    private boolean writeDamageAnalysisResult(String result)
    {
        ConsoleLogger.print('d',"Damage Analysis Result " + result);

        WriteFileUtil.writeToFileAt(FilePathsConfig.allTestRecordLocation + scenarioName + "-vcr.txt", result);

        return false;
    }


}
