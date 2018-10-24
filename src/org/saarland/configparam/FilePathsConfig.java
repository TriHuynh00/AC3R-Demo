package org.saarland.configparam;

public class FilePathsConfig {

    public static String accidentFolderLocation = "accidentCases/";
    private static String baseLevelPath = AccidentParam.beamNGUserPath + "\\levels\\smallgrid";
//    private static String baseLevelPath = "D:\\Games\\trunk" + "\\levels\\smallgrid\\";

    public static String damageRecordLocation  = baseLevelPath + "\\damageRecord\\";
    public static String previousRecordLocation  =  baseLevelPath + "\\previousRecord\\";
    public static String allTestRecordLocation  =  baseLevelPath + "\\verifiedCrashInfoRecord\\";
    public static String prevCrashInfoRecordLocation  =  baseLevelPath + "\\prevCrashInfoRecord\\";
    public static String testResultSummary  =  baseLevelPath + "\\testResultSummary\\";
    public static String absoluteScenarioFilePath = "levels/smallgrid/scenarios/";
    public static String BeamNGProgramPath = "BeamNG.research.x64.exe";
}
