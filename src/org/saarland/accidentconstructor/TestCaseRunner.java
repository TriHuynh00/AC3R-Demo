package org.saarland.accidentconstructor;

//import org.msgpack.MessagePack;
import org.apache.xerces.impl.io.ASCIIReader;
import org.msgpack.core.MessagePack;
import org.msgpack.annotation.Message;
import org.msgpack.core.MessageBufferPacker;
import org.msgpack.core.MessageUnpacker;
import org.saarland.configparam.AccidentParam;
import org.saarland.configparam.FilePathsConfig;

import java.awt.*;
import java.awt.event.InputEvent;
import java.io.*;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.file.CopyOption;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;

public class TestCaseRunner {

    private Robot bot;
    private final int scenarioLoadTime = 30000; // ms
    private final int scenarioRunTime = 36000; // ms
    private ProcessBuilder processBuilder;
    private Process p;

    private ServerSocket beamngServerSocket;
    private Socket beamngClient;
    private DataInputStream dataInputStream;
    private DataOutputStream dataOutputStream;

    private String scenarioName;

    public long getStartTime() {
        return startTime;
    }

    public void setStartTime(long startTime) {
        this.startTime = startTime;
    }

    private long startTime = 0;

    private boolean hasCrashStatus = false;
    private String host;
    private int port = 64256;

    public void setScenarioName(String scenarioName) {
        this.scenarioName = "levels/smallgrid/scenarios/" + scenarioName + ".json";
    }

    public void setHasCrashStatus(boolean status)
    {
        hasCrashStatus = status;
    }

    private Runnable beamNGThread;

    @Message
    public static class LoadBeamngScenario {
        // public fields are serialized.
        public String type;
        public String path;
    }

    @Message
    public static class StartBeamngScenario {
        // public fields are serialized.
        public String type;
    }

    public TestCaseRunner()
    {

        try {
            beamngServerSocket = new ServerSocket(port, 0, InetAddress.getByName("localhost"));
            ConsoleLogger.print('r', "Server Started at " + beamngServerSocket.getInetAddress() + " " + beamngServerSocket.getLocalPort());
        } catch (Exception ex) {
            ConsoleLogger.print('e', "Error at init BeamNG server \n" + ex.toString());
        }
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

        return startScenarioUsingShell(scenarioName);

//        return checkCrashFile(scenarioName);
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
                Files.move(Paths.get(dir + "/" + name),
                        Paths.get(AccidentParam.userFolder + "BeamNG.research\\levels\\smallgrid\\previousRecord\\" + name), options);
            } catch (IOException e) {
                e.printStackTrace();
            }
            return name.contains(".log");
            }
        });

    }



    private boolean startScenarioUsingShell(String scenarioName)
    {
//         try
//         {
//             ConsoleLogger.print('d',"Run BeamNG scenario Name " + scenarioName);
//             final String sceneName = scenarioName;
//             Thread thread = new Thread(new Runnable() {
//                 String sn = sceneName;
//                 @Override
//                 public void run() {
//                     controlBeamngClient(sn);
//                 }
//             });
//             thread.start();
//             ConsoleLogger.print('d', AccidentParam.beamNGUserPath);

// //            Thread.sleep(scenarioLoadTime / 2);
//         }

//         catch(Exception e2) {e2.printStackTrace();}

        ConsoleLogger.print('d',"Run BeamNG scenario Name " + scenarioName);
        controlBeamngClient(scenarioName);

        ConsoleLogger.print('d',"Done executing scenario");

        // Sleep for 5 seconds for loading scenario
        try {
            long beginTime = System.currentTimeMillis();
            long currentTime = System.currentTimeMillis();
            // wait for 30s
            while (currentTime - beginTime <= scenarioLoadTime + 3000)
            {
                if (getStartTime() > 0)
                {

                    ConsoleLogger.print('d', "Start time is set");
                    break;
                }
                currentTime = System.currentTimeMillis();
            }
            ConsoleLogger.print('d', "Start time is " + getStartTime());
        } catch (Exception e) {
            e.printStackTrace();
        }
        return checkCrashFile(scenarioName);
        //tryToClickScenarioStartButton();

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

        long beginTime = System.currentTimeMillis();
        long currentTime = System.currentTimeMillis();
        // wait for 30s
        while (files.length == 0 && currentTime - beginTime <= scenarioRunTime)
        {
            // Update file every second
            if (currentTime % 1000 <= 3) {

                //ConsoleLogger.print('n',"Update File ");
                files = AccidentConstructorUtil.getAllFilesContainName(caseName, caseDamageRecordFolder);
                //ConsoleLogger.print('d',"Current File Found = " + files.length);
            }
            currentTime = System.currentTimeMillis();
        }

        return chechScenarioEnd(files, beginTime);
    }


    private boolean chechScenarioEnd(File[] files, long beginTime) {
        // Analyze crash record
        boolean noTimeout = false;
        try {

            if (files.length > 0) {
                Thread.sleep(2000);
                ConsoleLogger.print('d',"Files length > 0");
                for (File caseDamageRecords : files) {
                    ConsoleLogger.print('d', caseDamageRecords);
                }


//                bot.keyPress(KeyEvent.VK_ALT);
//                bot.keyPress(KeyEvent.VK_F4);
//                Thread.sleep(500);
//                bot.keyRelease(KeyEvent.VK_ALT);
//                bot.keyRelease(KeyEvent.VK_F4);

            }
            if (System.currentTimeMillis() - beginTime <= scenarioRunTime) {

                ConsoleLogger.print('d',"Within time");
                noTimeout = true;

            } else {
                ConsoleLogger.print('d',"Timed out!");
                noTimeout = false;
            }
            Thread.sleep(1000);
            hasCrashStatus = true;
            p.destroy();
            setStartTime(0);
            ConsoleLogger.print('d', "destroy BeamNG, close connection");
            return noTimeout;
        }
        catch (Exception ex)
        {
            ConsoleLogger.print('e',"Error at checking scenario end");
            ex.printStackTrace();
            return false;
        }
    }

    private void controlBeamngClient(String scenarioName) {
        try {
            while (true)
            {
                // String cmdExec = FilePathsConfig.BeamNGProgramPath
                        // + " -userpath " + AccidentParam.beamNGUserPath
                        // + " -rhost 127.0.0.1 -rport " + port
                        // + " -lua registerCoreModule('util_researchGE') ";

                String cmdExec = "python " + AccidentParam.beamNGpyPath + " " + scenarioName;
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

        } catch (Exception ex) {
            ConsoleLogger.print('e', "Error at control BeamNG client \n" + ex.toString());

        }

    }

    private void startScenario() {
        LoadBeamngScenario loadBeamngScenario = new LoadBeamngScenario();
        loadBeamngScenario.type = "LoadScenario";
        loadBeamngScenario.path = scenarioName;
        ConsoleLogger.print('d', "Command BeamNG to load scenario at " + scenarioName);
        try {
//            ByteArrayOutputStream out = new ByteArrayOutputStream();
//            Packer packer = messagePack.createPacker(out);
            MessageBufferPacker messagePack = MessagePack.newDefaultBufferPacker();
            messagePack.packMapHeader(2);
            messagePack.packString("type");
            messagePack.packString("LoadScenario");
            messagePack.packString("path");
            messagePack.packString(scenarioName);
            byte[] loadScenarioCmd = messagePack.toByteArray();

            sendCommandToClient(loadScenarioCmd);

            Thread.sleep(28000);

            ConsoleLogger.print('r', "Scenario loaded");
            triggerScenario();


        } catch (Exception ex) {
            ConsoleLogger.print('e', "Error At sending start scenario command \n"
                    + ex.toString());
        }
    }

    private void triggerScenario() {
        try {
            MessageBufferPacker messagePack = MessagePack.newDefaultBufferPacker();
            messagePack.packMapHeader(1);
            messagePack.packString("type");
            messagePack.packString("StartScenario");
//            StartBeamngScenario startBeamngScenario = new StartBeamngScenario();
//            startBeamngScenario.type = "StartScenario";
            byte[] startScenarioCmd = messagePack.toByteArray();

            sendCommandToClient(startScenarioCmd);

            setStartTime(System.currentTimeMillis() + 200);
        } catch (Exception ex) {
            ConsoleLogger.print('e', "Start Scenario Err \n" + ex.toString());
        }
    }

    private boolean sendCommandToClient(byte[] message) {
        try {
            //ConsoleLogger.print('d', "Length formated = " + String.format("%016d", message.length));
            dataOutputStream.writeBytes(String.format("%016d", message.length));
//            dataOutputStream.write((message.length + "\n").getBytes());
            dataOutputStream.write(message);
            ConsoleLogger.print('d', "send " + message.toString());
//            dataOutputStream.write(message.length);
//            dataOutputStream.write(message);
            return true;
        } catch (Exception ex) {
            return false;
        }
    }

}
