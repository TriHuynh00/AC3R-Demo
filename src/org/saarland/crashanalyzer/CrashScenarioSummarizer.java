package org.saarland.crashanalyzer;

import org.joda.time.DateTime;
import org.saarland.accidentconstructor.AccidentConstructorUtil;
import org.saarland.accidentconstructor.ConsoleLogger;
import org.saarland.configparam.AccidentParam;
import org.saarland.configparam.FilePathsConfig;

import java.io.*;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedList;

public class CrashScenarioSummarizer {

    private final int ALL_MATCHED_INDEX = 0;

    private final int PARTIALLY_MATCHED_INDEX = 1;

    private final int UNVERIFIED_MATCHED_INDEX = 2;

    private final int NO_MATCH_INDEX = 3;

    private final int GENERATION_FAIL_INDEX = 4;
    
    public CrashScenarioSummarizer(){}

    public void summarizeAllScenarios()
    {
        int[] crashResultCounters = new int[] {0, 0, 0, 0, 0};
        
        int perfectlyMatchedCount = 0;

        int partiallyMatchedCount = 0;

        int unverifiedCrashCount = 0;

        int noCrashCount = 0;

        int generationFailCount = 0;
        
        ArrayList<LinkedList<String>> allCasesName = new ArrayList<LinkedList<String>>();
        for (int i = 0; i < crashResultCounters.length; i++)
        {
            allCasesName.add(new LinkedList<String>());
        }
        
        File allTestRecordFolder = new File(FilePathsConfig.allTestRecordLocation);

        try
        {
            File[] allCrashFiles = AccidentConstructorUtil.getAllFilesContainName("vcr", allTestRecordFolder);

            for (File crashFile : allCrashFiles) {
                String content = Files.readAllLines(Paths.get(crashFile.getAbsolutePath()), Charset.defaultCharset()).get(0);
                ConsoleLogger.print('d',"Content " + content);

                analyzeRecord(crashResultCounters, allCasesName, content);
            }

            StringBuilder summaryStrBuilder = new StringBuilder();

            ConsoleLogger.print('r',"----------------------------------");

            ConsoleLogger.print('r',"SUMMARY OF TEST SUITE");

            DateTime today = new DateTime();

            int totalCasesNumber = 0;

            for (int noOfCase : crashResultCounters)
            {
                totalCasesNumber += noOfCase;
            }

            summaryStrBuilder.append("Test Result run at " + today.toString() + "\n");

            summaryStrBuilder.append("Total Number Of Cases: " + totalCasesNumber + "\n");

            summaryStrBuilder.append("Perfectly Matched Crash Case: " + crashResultCounters[ALL_MATCHED_INDEX] + "\n");

            summaryStrBuilder.append("Partially Matched Crash Case: " + crashResultCounters[PARTIALLY_MATCHED_INDEX] + "\n");

            summaryStrBuilder.append("Unverified Crash Case: " + crashResultCounters[UNVERIFIED_MATCHED_INDEX] + "\n");

            summaryStrBuilder.append("No Crash Case: " + crashResultCounters[NO_MATCH_INDEX] + "\n");

            summaryStrBuilder.append("Generation Failure Case: " + crashResultCounters[GENERATION_FAIL_INDEX] + "\n\n");

            summaryStrBuilder.append("Perfectly Matched Case Names " + allCasesName.get(ALL_MATCHED_INDEX) + "\n");

            summaryStrBuilder.append("Partially Matched Case Names " + allCasesName.get(PARTIALLY_MATCHED_INDEX) + "\n");

            summaryStrBuilder.append("Unverified Matched Case Names " + allCasesName.get(UNVERIFIED_MATCHED_INDEX) + "\n");

            summaryStrBuilder.append("No Crash Case Names: " + allCasesName.get(NO_MATCH_INDEX) + "\n");

            summaryStrBuilder.append("Generation Failure Cases Names: " + allCasesName.get(GENERATION_FAIL_INDEX) + "\n");

            String summaryFileName = "testSummary_"
                    + today.getDayOfMonth() + "-" + today.getMonthOfYear() + "-" + today.getYear() + "-"
                    + today.hourOfDay().get() + "h" + today.minuteOfHour().get() + "m.txt";

            WriteFileUtil.writeToFileAt(FilePathsConfig.testResultSummary + summaryFileName, summaryStrBuilder.toString());

            ConsoleLogger.print('r',summaryStrBuilder.toString());



        } catch (IOException e) {
            e.printStackTrace();
            //exception handling left as an exercise for the reader
        }
    }

    /**
     * Check whether a crash occurs in the simulation. If yes, determine if the crash matched perfectly, partially,
     * or unverifiably based on the record
     *
     * @param crashResultCounters - The current counters of no crash / perfect / partial / unverifiable cases
     * @param allCasesName - The name of crash scenario which falls into the 4 types
     * @param record - the string containing the crash information of the vehicles in a given simulation
     */
    private void analyzeRecord(int[] crashResultCounters, ArrayList<LinkedList<String>> allCasesName, String record)
    {
        // PS - 0
        // P  - 1
        // S  - 2
        if (record.split(":").length < 2)
        {
            return;
        }

        int[] crashTypeInRecordCounter = new int[] {0,0};

        String[] recordElements = record.split(":");

        boolean isPerfectMatch = false;

        HashSet<String> perfectMatchVehicleSet = new HashSet<>();
        HashSet<String> partVehicleSet = new HashSet<>();
        HashSet<String> unverifiedMatchVehicleSet = new HashSet<>();
        HashSet<String> notMatchVehicleSet = new HashSet<>();
        HashSet<String> failGenerationSet = new HashSet<>();
        // Found a no crash case
        if (recordElements[1].contains(AccidentParam.NO_CRASH_STR))
        {
            crashResultCounters[NO_MATCH_INDEX]++;
            allCasesName.get(NO_MATCH_INDEX).add(recordElements[0]);
        }
        else if (recordElements[1].contains(AccidentParam.FAILED_TO_GENERATE))
        {
            crashResultCounters[GENERATION_FAIL_INDEX]++;
            allCasesName.get(GENERATION_FAIL_INDEX).add(recordElements[0]);
        }
        else// if (!record.contains(CrashResultCode.ALL_MATCHED))
        {
            // Loop through the vehicle's damage element
            for (int i = 1; i < recordElements.length; i++) 
            {
                if (recordElements[i].equals(""))
                {
                    continue;
                }
                String[] vehicleCrashedRecord = recordElements[i].split("-");
                String vehicleID = vehicleCrashedRecord[0];
                for (int k = 1; k < vehicleCrashedRecord.length; k++) 
                {
                    String crashMatchStatus = vehicleCrashedRecord[k];
                    ConsoleLogger.print('d',"Crash Match status i=" + i + " k=" + k + " is " + crashMatchStatus);
//                    if (crashMatchStatus.equals(""))
//                    {
//                        continue;
//                    }
                    // An unverified crash is recorded, this is definitely an unverified crash
                    if (crashMatchStatus.equals(CrashResultCode.UNVERIFIED_CRASH)) 
                    {
                        //crashResultCounters[UNVERIFIED_MATCHED_INDEX]++;
                        //allCasesName.get(UNVERIFIED_MATCHED_INDEX).add(recordElements[0]);
                        unverifiedMatchVehicleSet.add(vehicleID);
                    }
                    // If there is a matched side or position, count it as partially matched
                    else if (crashMatchStatus.equals(CrashResultCode.POSITION_MATCHED)
                            || crashMatchStatus.equals(CrashResultCode.SIDE_MATCHED))
                    {
                        //crashResultCounters[PARTIALLY_MATCHED_INDEX]++;
                        //allCasesName.get(PARTIALLY_MATCHED_INDEX).add(recordElements[0]);
                        partVehicleSet.add(vehicleID);
                    }
                    // If there is a perfect match, keep analyzing until the last car
                    else if (crashMatchStatus.equals(CrashResultCode.ALL_MATCHED)) 
                    {
//                        ConsoleLogger.print('d',"Perfect matched true");
                        perfectMatchVehicleSet.add(vehicleID);
//                        isPerfectMatch = true;
                    }
                    else
                    {
                        notMatchVehicleSet.add(vehicleID);
//                        crashResultCounters[UNVERIFIED_MATCHED_INDEX]++;
//                        allCasesName.get(UNVERIFIED_MATCHED_INDEX).add(recordElements[0]);
//                        return;
                    }
                }
            }


            // Record a perfect match case
            if (perfectMatchVehicleSet.size() == recordElements.length - 1)
            {
                crashResultCounters[ALL_MATCHED_INDEX]++;
                allCasesName.get(ALL_MATCHED_INDEX).add(recordElements[0]);
            }
            else if (perfectMatchVehicleSet.size() > 0)
            {
                crashResultCounters[PARTIALLY_MATCHED_INDEX]++;
                allCasesName.get(PARTIALLY_MATCHED_INDEX).add(recordElements[0]);
            }
            else if (unverifiedMatchVehicleSet.size() == recordElements.length - 1)
            {
                crashResultCounters[UNVERIFIED_MATCHED_INDEX]++;
                allCasesName.get(UNVERIFIED_MATCHED_INDEX).add(recordElements[0]);
            }
            else
            {
                crashResultCounters[NO_MATCH_INDEX]++;
                allCasesName.get(NO_MATCH_INDEX).add(recordElements[0]);
            }



        } // End checking non-crash case
    }
    
    private String getCaseNameOfCrashRecord(String crashRecord)
    {
        return crashRecord.split(":")[0];
    }

}