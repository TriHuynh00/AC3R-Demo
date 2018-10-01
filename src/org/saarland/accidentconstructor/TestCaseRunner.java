package org.saarland.accidentconstructor;

import com.sun.glass.ui.Screen;
import org.saarland.configparam.AccidentParam;
import org.saarland.configparam.FilePathsConfig;

import java.awt.*;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.nio.file.CopyOption;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;

public class TestCaseRunner {

    private Robot bot;
    private final int scenarioLoadTime = 33000; // ms
    private final int scenarioRunTime = 30000; // ms


    public TestCaseRunner()
    {
        try {
            bot = new Robot();
        } catch (AWTException e) {
            ConsoleLogger.print('d',"Error at declaring Robot in TestCaseRunner constructor");
            e.printStackTrace();
        }
    }

    public boolean runScenario(String scenarioName)
    {
        //moveCrashInfoFilesToPrevRecordFolder();

//        AccidentConstructorUtil.moveFilesToAnotherFolder(FilePathsConfig.damageRecordLocation,
//                FilePathsConfig.previousRecordLocation, ".log");
//
//        AccidentConstructorUtil.moveFilesToAnotherFolder(FilePathsConfig.allTestRecordLocation,
//                FilePathsConfig.prevCrashInfoRecordLocation, ".vcr");

        startScenarioUsingShell(scenarioName);

        return checkCrashFile(scenarioName);
    }

    private void moveCrashInfoFilesToPrevRecordFolder(String currentPath, String destinationPath, String fileExtension)
    {
        //Path FROM = Paths.get("C:\\Users\\botes\\Documents\\BeamNG.drive\\levels\\smallgrid\\damageRecord\\");
        final CopyOption[] options = new CopyOption[]{
                StandardCopyOption.REPLACE_EXISTING
        };

        File caseDamageRecordFolder = new File(FilePathsConfig.damageRecordLocation);

        File [] files = caseDamageRecordFolder.listFiles(new FilenameFilter() {
            @Override
            public boolean accept(File dir, String name) {
            try {
                Files.move(Paths.get(dir + "/" + name), Paths.get(AccidentParam.userFolder + "BeamNG.drive\\levels\\smallgrid\\previousRecord\\" + name), options);
            } catch (IOException e) {
                e.printStackTrace();
            }
            return name.contains(".log");
            }
        });

    }



    private void startScenarioUsingShell(String scenarioName)
    {
        try
        {
            ConsoleLogger.print('d',"Run BeamNG scenario Name " + scenarioName);
            Process p = Runtime.getRuntime().exec(FilePathsConfig.BeamNGProgramPath
                    + " " + " -userpath " + AccidentParam.beamNGUserPath
                    + " -lua "
                    + " require('scenario/scenariosLoader').startByPath(\"levels/smallgrid/scenarios/" + scenarioName + ".json\")");
        }
        catch(IOException e1) {e1.printStackTrace();}
        catch(Exception e2) {e2.printStackTrace();}

        ConsoleLogger.print('d',"Done executing scenario");

        // Sleep for 10 seconds for loading scenario
        try {
            Thread.sleep(scenarioLoadTime);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        tryToClickScenarioStartButton();

    }

    private void tryToClickScenarioStartButton()
    {
        int screenHeight = (int) Toolkit.getDefaultToolkit().getScreenSize().getHeight();
        //ConsoleLogger.print('d',"Screen Height " + Toolkit.getDefaultToolkit().getScreenSize().getHeight());
        for (int i = (int) (screenHeight * 0.7); i < screenHeight * 0.95; i += 20)
        {
            bot.mouseMove(1000, i);
            bot.mousePress(InputEvent.BUTTON1_MASK);
            bot.mouseRelease(InputEvent.BUTTON1_MASK);
            try {
                Thread.sleep(200);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    private boolean checkCrashFile(String scenarioName)
    {
        final String caseName = scenarioName;
        // Begin checking crash file
        File caseDamageRecordFolder = new File(FilePathsConfig.damageRecordLocation);

        File[] files =  AccidentConstructorUtil.getAllFilesContainName(caseName, caseDamageRecordFolder);

        long startTime = System.currentTimeMillis();
        long currentTime = System.currentTimeMillis();
        // wait for 30s
        while (files.length == 0 && currentTime - startTime <= scenarioRunTime)
        {
            // Update file every second
            if (currentTime % 1000 <= 3) {

                ConsoleLogger.print('n',"Update File ");
                files = AccidentConstructorUtil.getAllFilesContainName(caseName, caseDamageRecordFolder);
                ConsoleLogger.print('d',"Current File Found = " + files.length);
            }
            currentTime = System.currentTimeMillis();
        }

        return chechScenarioEnd(files, startTime);
    }


    private boolean chechScenarioEnd(File[] files, long startTime) {
        // Analyze crash record
        try {


            if (files.length > 0) {
                Thread.sleep(1000);
                ConsoleLogger.print('d',"Files length > 0");
                for (File caseDamageRecords : files) {
                    ConsoleLogger.print('d',caseDamageRecords);
                }

                bot.keyPress(KeyEvent.VK_ALT);
                bot.keyPress(KeyEvent.VK_F4);
                Thread.sleep(500);
                bot.keyRelease(KeyEvent.VK_ALT);
                bot.keyRelease(KeyEvent.VK_F4);

            }
            if (System.currentTimeMillis() - startTime <= 30000) {
                ConsoleLogger.print('d',"Within time");
                return true;
            } else {
                ConsoleLogger.print('d',"Timed out!");
                return false;
            }
        }
        catch (Exception ex)
        {
            ConsoleLogger.print('d',"Error at checking scenario end");
            ex.printStackTrace();
            return false;
        }
    }
}
