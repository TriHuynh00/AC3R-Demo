# Automatic Crash Constructor from Crash Report (A3CR) 

This project contains the implementation and demo of AC3R, a software that generate simulations from semi-structured crash reports i.e. a document recording crash elements and narratives using a schema like XML.

The link to AC3R demo video in (YouTube)[https://www.youtube.com/watch?v=V708fDG_ux8]


## Requirements

AC3R requires the following dependencies to operate: 

1. BeamNG.research <b> unlimited edition </b> to simulate the generated crash scenarios. Please contact research@beamng.gmbh to request for the unlimited edition of BeamNG.research.
2. Java 1.7 and above to run AC3R
3. Python 3.7 or above
4. BeamNGpy 1.18 and above to execute scenario. It can be installed using pip at (Github Repo)[https://github.com/BeamNG/BeamNGpy]
5. Windows 7, 8 or 10 to run BeamNG v17

## Build

To build AC3R, Maven >= 3.3.9 is required. Run the following commands:

```
cd path-to-AC3R-project/demo
mvn package
```

Then move the generated jar in target\out\AC3R-1.0-jar-with-dependencies.jar to demo folder, and rename the jar to AC3R.jar.

## Configuration

Before executing AC3R:

1. Add Bin64 folder of BeamNG repo into Windows PATH environment variable
2. Overwrite `[BeamNG-repo]\lua\ge\extensions\scenario\scenariohelper.lua` by copying `[AC3R-repo]\beamngBaseFile\scenariohelper.lua` to `[BeamNG-repo]\lua\ge\extensions\scenario`
3. Rename `[BeamNG-repo]` to `[BeamNG.drive]` and place it under `Documents` folder

Since the BeamNG unlimited research only has one car model, the default AC3R built is configured to use only one model.

## Base Map Construction

AC3R needs a base map to load all roads, cars and other objects onto it. The steps needed to create this base map from the smallgrid map provided by BeamNG.research are:

1. Run command `BeamNG.research.x64.exe -userpath C:\Users\[your_username]\Documents\BeamNG.drive -console` and see if you can find the folder BeamNG.research under Document directory.
2. Download a smallgrid from this (link)[https://www.dropbox.com/s/0xxhg5a7wv9au14/levels.7z?dl=0] and place it as level folder in BeamNG.research. The expected directory should be `\Documents\BeamNG.research\levels\smallgrid\`
3. Run command `BeamNG.research.x64.exe -userpath C:\Users\[your_username]\Documents\BeamNG.drive -console`. If the modified grid map is loaded successfully, Congratulation :D


After the base map is saved, create the following folders in **C:\Users\[your_username]\Documents\BeamNG.drive\levels\smallgrid** to store crash damage information from BeamNG and stored results of reconstructed crashes:

1. damageRecord
2. prevCrashInfoRecord
3. previousRecord
4. testResultSummary
5. verifiedCrashInfoRecord

## Demo

The demonstration of AC3R capability is placed in the demo folder.

To run AC3R, starts the demo/StartAC3R.bat by double-click it or running the following commands:

```
cd path-to-AC3R-project/demo
./StartAC3R.bat
```

The accidentCases contains the sample crash reports. When AC3R prompts a file chooser GUI, select the crash report(s) in accidentCases folder as inputs to AC3R. Then the simulations are generated and run in BeamNG.
