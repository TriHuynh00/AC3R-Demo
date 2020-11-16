# Automatic Crash Constructor from Crash Report (A3CR) 

This project contains the implementation and demo of AC3R, a software that generate simulations from semi-structured crash reports i.e. a document recording crash elements and narratives using a schema like XML.

The link to AC3R demo video in YouTube is:
https://www.youtube.com/watch?v=V708fDG_ux8

## Requirements

AC3R requires the following dependencies to operate: 

1. BeamNG.research <b> unlimited edition </b> to simulate the generated crash scenarios. Please contact research@beamng.gmbh to request for the unlimited edition of BeamNG.research.
2. Java 1.7 and above to run AC3R
3. Windows 7, 8 or 10 to run BeamNG 

## Build

To build AC3R, Maven >= 3.3.9 is required. Run the following commands:

cd path-to-AC3R-project/demo
mvn package

Then move the generated jar in target\out\AC3R-1.0-jar-with-dependencies.jar to demo folder, and rename the jar to AC3R.jar.

## Configuration

Before executing AC3R:

1. Add Bin64 folder of BeamNG repo into Windows PATH environment variable
2. Overwrite [BeamNG-repo]\lua\ge\extensions\scenario\scenariohelper.lua by copying [AC3R-repo]\beamngBaseFile\scenariohelper.lua to [BeamNG-repo]\lua\ge\extensions\scenario
3. Overwrite [BeamNG-repo]\lua\vehicle\beamstate.lua by copying [AC3R-repo]\beamngBaseFile\beamstate.lua to [BeamNG-repo]\lua\vehicle

Since the BeamNG unlimited research only has one car model, the default AC3R built is configured to use only one model.

## Base Map Construction

AC3R needs a base map to load all roads, cars and other objects onto it. The steps needed to create this base map from the smallgrid map provided by BeamNG.research are:

1. Open BeamNG Executable in [BeamNG repository]/Bin64/BeamNG.research.x64.exe
2. Click on scenario tab
3. Click left arrow on the top left corner
4. Choose "Grid, Small, Pure" map
5. Select the "Default" spawn point
6. After BeamNG loads the map, press F11 
7. Look on the Scene Tree panel on the rightmost of BeamNG GUI, expand MissionGroup folder, select grassland terrain
8. In **Transform** panel, set position X = -300, Y = -300, Z = 0
9. In **Misc** panel, set squareSize = 5, maxHeight = 1, baseTexSize = 1024; lightMapSize = 1024
10. Press F3 to open the Terrain Painter 
11. Choose "New Layer", in Terrain Material panel, scroll down and choose "Grassy", then click "Apply&Select"
12. The preview of terrain material display green color, choose "AutoPaint" button to paint the grass layer on the entire map.
13. Select "File" tab in the menu bar, choose "Save Level As", and select the destination as **C:\Users\\[your_username]\BeamNG.drive\levels"**. Specify the level name as **smallgrid**
14. Run command **BeamNG.research.x64.exe -userpath C:\Users\[your_username]\Documents\BeamNG.drive -console**, and repeat step 1 to 5. If the modified grid map is loaded successfully, Congratulation :D

After the base map is saved, create the following folders in **C:\Users\[your_username]\Documents\BeamNG.drive\levels\smallgrid** to store crash damage information from BeamNG and stored results of reconstructed crashes:

1. damageRecord
2. prevCrashInfoRecord
3. previousRecord
4. testResultSummary
5. verifiedCrashInfoRecord

## Demo

The demonstration of AC3R capability is placed in the demo folder.

To run AC3R, starts the demo/StartAC3R.bat by double-click it or running the following commands:

cd path-to-AC3R-project/demo
./StartAC3R.bat

The accidentCases contains the sample crash reports. When AC3R prompts a file chooser GUI, select the crash report(s) in accidentCases folder as inputs to AC3R. Then the simulations are generated and run in BeamNG.
