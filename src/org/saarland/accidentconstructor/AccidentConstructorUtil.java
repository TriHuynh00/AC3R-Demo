package org.saarland.accidentconstructor;

import org.jdom2.JDOMException;
import org.saarland.accidentelementmodel.NavigationDictionary;
import org.saarland.accidentelementmodel.Street;
import org.saarland.accidentelementmodel.VehicleAttr;
import org.saarland.configparam.AccidentParam;
import org.saarland.configparam.VelocityCode;
import org.saarland.ontologyparser.OntologyHandler;
import org.semanticweb.owlapi.vocab.DublinCoreVocabulary;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.*;
import java.nio.charset.Charset;
import java.nio.file.*;
import java.util.*;

public class AccidentConstructorUtil {

    private static String[] wordNumber = new String[]{"zero", "one", "two", "three", "four", "five", "six"};



    public static VehicleAttr[] findStrikerAndVictim(int actionWordIndex, VehicleAttr vehicle0, VehicleAttr vehicle1,
                                                        ArrayList<Integer> impactAtSteps) {
        VehicleAttr[] strikerAndVictim = new VehicleAttr[2];
        if (vehicle0.getActionList().get(impactAtSteps.get(actionWordIndex)).endsWith("hit*")) {

            strikerAndVictim[0] = vehicle1;
            strikerAndVictim[1] = vehicle0;
        } else {
            strikerAndVictim[0] = vehicle0;
            strikerAndVictim[1] = vehicle1;
        }
        return strikerAndVictim;
    }

    // Find Striker victim based only on the action list
    public static VehicleAttr[] findStrikerAndVictim(VehicleAttr vehicle0, VehicleAttr vehicle1) {
        VehicleAttr[] strikerAndVictim = new VehicleAttr[2];
        ConsoleLogger.print('d',"Find Striker and Victim 2, hit* in vehicle0 at " + vehicle0.getActionList().indexOf("hit*"));
        ConsoleLogger.print('d',"vehicle0 action " + vehicle0.getActionList().toString());

        String firstVehicleAction = vehicle0.getActionList().get(0);
        if (vehicle0.getActionList().indexOf("hit*") > -1
                || firstVehicleAction.startsWith("park") || firstVehicleAction.startsWith("stop")) {

            strikerAndVictim[0] = vehicle1;
            strikerAndVictim[1] = vehicle0;
        } else {
            strikerAndVictim[0] = vehicle0;
            strikerAndVictim[1] = vehicle1;
        }

        // If this a rear-end case, make sure that the striker has the xCoord before the victim

        return strikerAndVictim;
    }

    // Find Striker victim based only on the action list
    // If this a rear-end case, make sure that the striker has the xCoord before the victim
    public static VehicleAttr[] findStrikerAndVictimForRearEnd(VehicleAttr vehicle0, VehicleAttr vehicle1, String delimiter) {
        VehicleAttr[] strikerAndVictim = new VehicleAttr[2];
//        if (accidentType.contains("rear-end") || accidentType.contains("rearend") || accidentType.contains("rear end"))
//
//        {
            double v0XCoord = Double.parseDouble(vehicle0.getMovementPath().get(0).split(delimiter)[0]);
            double v1XCoord = Double.parseDouble(vehicle1.getMovementPath().get(0).split(delimiter)[0]);
            if (v0XCoord < v1XCoord)
            {
                strikerAndVictim[0] = vehicle0;
                strikerAndVictim[1] = vehicle1;
            }
            else
            {
                strikerAndVictim[1] = vehicle0;
                strikerAndVictim[0] = vehicle1;
            }
//        }
        return strikerAndVictim;
    }

    // Find Striker victim based only on the action list
    public static ArrayList<ArrayList<String>> fillCoordOfVehicles(ArrayList<ArrayList<String>> vehicleCoordList,
                                                                      int StepsInAccidents) {
        for (ArrayList<String> coordList : vehicleCoordList) {
            for (int i = 0; i < StepsInAccidents; i++) {
                coordList.add("0");
            }
        }
        return vehicleCoordList;
    }

    // Find Striker victim based only on the action list
    public static VehicleAttr findVehicleBasedOnId(int vehicleId, ArrayList<VehicleAttr> vehicleList) {
        for (VehicleAttr vehicleAttr : vehicleList) {
            if (vehicleAttr.getVehicleId() == vehicleId) {
                return vehicleAttr;
            }
        }
        return null;
    }

//    public static void findImpactedStepsAndVehicles(ArrayList<Integer> impactAtSteps,
//                                                       ArrayList<ArrayList<VehicleAttr>> impactedVehiclesAtSteps,
//                                                       ArrayList<VehicleAttr> vehicleList) {
//        for (int i = 0; i < vehicleList.get(0).getActionList().size(); i++) {
//            ArrayList<VehicleAttr> impactedVehicleInThisStep = new ArrayList<VehicleAttr>();
//            boolean foundImpact = false;
//            for (VehicleAttr vehicle : vehicleList) {
//                if (vehicle.getActionList().get(i).startsWith("hit")) {
//                    foundImpact = true;
//                    ConsoleLogger.print('d',"Find impact at step " + i);
//                    if (impactAtSteps.indexOf(i) == -1) {
//                        impactAtSteps.add(i);
//                    }
//
//                    impactedVehicleInThisStep.add(vehicle);
//                }
//            }
//            if (impactedVehicleInThisStep.size() > 0) {
//                impactedVehiclesAtSteps.add(impactedVehicleInThisStep);
//            }
//        }
//    }

    // Find the impacted step and vehicle of a given acting vehicle
    public static void findImpactedStepsAndVehicles(VehicleAttr actorVehicle,
                                                    ArrayList<Integer> impactAtSteps,
                                                    ArrayList<ArrayList<VehicleAttr>> impactedVehiclesAtSteps,
                                                    ArrayList<VehicleAttr> vehicleList) {
        for (int i = 0; i < actorVehicle.getActionList().size(); i++) {
            ArrayList<VehicleAttr> impactedVehicleInThisStep = new ArrayList<VehicleAttr>();
            boolean foundImpact = false;

            if (actorVehicle.getActionList().get(i).startsWith("hit")) {
                foundImpact = true;
                ConsoleLogger.print('d',"Find impact at step " + i);
                if (impactAtSteps.indexOf(i) == -1) {
                    impactAtSteps.add(i);
                }

                for (VehicleAttr vehicle : vehicleList) {
                    if (vehicle.getVehicleId() == actorVehicle.getVehicleId()) continue;
                    for (String action : vehicle.getActionList()) {
                        if (action.equals("hit*")) {
                            impactedVehicleInThisStep.add(vehicle);
                        }
                    }
                }

            }

            if (impactedVehicleInThisStep.size() > 0) {
                impactedVehiclesAtSteps.add(impactedVehicleInThisStep);
            }
        }
    }

    public static int getVelocityOfAction(String action, OntologyHandler parser) {
        try {
            return Integer.parseInt(parser.findExactConcept(action)
                .getDataProperties()
                .get("velocity"));
        } catch (Exception ex) {
            ConsoleLogger.print('e', String.format("Velocity of action %s not found, error message: \n %s",
                action, ex.toString()));
            return VelocityCode.UNKNOWN;
        }
    }

    public static double computeYCircleFunc(double radius, double xCoord) {
        double curveDirectionCode = radius < 0 ? 1 : -1;
        ConsoleLogger.print('d',"Y circle Func");
        ConsoleLogger.print('d',"xCoord : " + xCoord + " radius " + radius);
        ConsoleLogger.print('d',"computation result " + Math.abs( Math.abs(radius) - (Math.sqrt(Math.pow(radius, 2) - Math.pow(xCoord, 2)))));
        return curveDirectionCode * Math.abs( Math.abs(radius) - (Math.sqrt(Math.pow(radius, 2) - Math.pow(xCoord, 2))));
    }

    public static double computeXCircleFunc(double radius, double yCoord) {
        ConsoleLogger.print('d',"X circle Func");
        ConsoleLogger.print('d',"yCoord : " + yCoord + " radius " + radius);
        ConsoleLogger.print('d',"computation result " +
                Math.abs( Math.abs(radius) - (Math.sqrt(Math.pow(radius, 2) - Math.pow(yCoord, 2)))));
        double curveDirectionCode = radius < 0 ? 1 : -1;
        return curveDirectionCode * Math.abs( Math.abs(radius) - (Math.sqrt(Math.pow(radius, 2) - Math.pow(yCoord, 2))));
    }

    // Get all the words connected to a given word by searching the dependency list. Return a string of words connected to the
    // given baseWord
    public static String findAllConnectedWordsBottomUp(LinkedList<String> dependencyList, String baseWord, String currentConnectedWordStr,
                                                       int curDepth, int maxDepth) {

        if (curDepth >= maxDepth)
        {
            return currentConnectedWordStr;
        }
        //ConsoleLogger.print('d',"Cur depth " + curDepth + " max depth " + maxDepth);
        for (int i = dependencyList.size() - 1; i >= 0; i--) {
            String dep = dependencyList.get(i);
            if (dep.contains(baseWord))
            {
                String otherWord = getOtherWordInDep(baseWord, AccidentConstructorUtil.getWordPairFromDependency(dep));
                if (currentConnectedWordStr.contains(otherWord))
                {
                    continue;
                }
                else
                {
                    currentConnectedWordStr += "," + otherWord;
                    currentConnectedWordStr = findAllConnectedWordsTopDown(dependencyList, otherWord,
                            currentConnectedWordStr, curDepth + 1, maxDepth);
                }

            }
        }
        return currentConnectedWordStr;
    }

    public static String findAllConnectedWordsTopDown(LinkedList<String> dependencyList, String baseWord, String currentConnectedWordStr,
                                                      int curDepth, int maxDepth) {

        if (curDepth >= maxDepth)
        {
            return currentConnectedWordStr;
        }
        //ConsoleLogger.print('d',"Cur depth " + curDepth + " max depth " + maxDepth);
        for (String dep : dependencyList) {
            if (dep.contains(baseWord))
            {
                String otherWord = getOtherWordInDep(baseWord, AccidentConstructorUtil.getWordPairFromDependency(dep));
                if (currentConnectedWordStr.contains(otherWord))
                {
                    continue;
                }
                else
                {
                    currentConnectedWordStr += "," + otherWord;
                    currentConnectedWordStr = findAllConnectedWordsTopDown(dependencyList, otherWord,
                        currentConnectedWordStr, curDepth + 1, maxDepth);
                }

            }
        }
        return currentConnectedWordStr;
    }

    // ** UTIL FUNCTIONS ************************

    /* targetWordType: 0 - None
     *                 1 - Verb
     */
    public static LinkedList<String> findConnectedDependencies(LinkedList<String> dependencyList,
                                                                  LinkedList<String> taggedWords,
                                                                  String baseWordWithPosition,
                                                                  String omitDepedency,
                                                                  int targetWordType) {
        LinkedList<String> connectedDependencies = new LinkedList<String>();
        // Find every dependency that contains the based word
        for (String dependency : dependencyList) {
            if (dependency.contains(baseWordWithPosition)) {
                if (dependency.equalsIgnoreCase(omitDepedency)) {
                    continue;
                }

                String wordPair = dependency.split("\\(")[1].replace(")", "");
                String word1 = wordPair.split(",")[0].trim();
                String word2 = wordPair.split(",")[1].trim();

                String otherWord = "";

                if (word1.equalsIgnoreCase(baseWordWithPosition)) {
                    otherWord = word2;
                } else {
                    otherWord = word1;
                }

                // Check if the connected word should have a specific word class

                if (targetWordType == 0) // Not care
                {
                    connectedDependencies.add(dependency);
                } else if (targetWordType == 1) // Must be verb
                {
                    int index = Integer.parseInt(otherWord.split("-")[1]);
                    if (taggedWords.get(index - 1).split("/")[1].startsWith("VB")) {
                        connectedDependencies.add(dependency);
                    }
                } else if (targetWordType == 100) // Must be a number
                {
                    String otherWordStr = AccidentConstructorUtil.getWordFromToken(otherWord);
                    ConsoleLogger.print('d',"Extract Number dep for " + otherWord + " match regexp? "
                            + otherWordStr.matches("(\\d*)|(\\d*.\\d*)"));
                    if (otherWordStr.matches("(\\d*)|(\\d*.\\d*)")) {
                        connectedDependencies.add(dependency);
                    }
                }
            } // End checking dependency containing baseWord
        } // End Checking each Dependency
        return connectedDependencies;
    }

    /* targetWordType: 0 - None
     *                 1 - Verb
     */
    public static LinkedList<String> findConnectedDependencies(LinkedList<String> dependencyList,
                                                                  LinkedList<String> taggedWords,
                                                                  String baseWordWithPosition,
                                                                  LinkedList<String> omitDepedencies,
                                                                  int targetWordType) {
        LinkedList<String> connectedDependencies = new LinkedList<String>();
        // Find every dependency that contains the based word
        for (String dependency : dependencyList) {
            if (dependency.contains(baseWordWithPosition)) {
                boolean isOmittedDependency = false;
                for (String omitDepedency : omitDepedencies) {
                    if (dependency.equalsIgnoreCase(omitDepedency)) {
                        isOmittedDependency = true;
                        continue;
                    }
                }
                if (isOmittedDependency) {
                    continue;
                }
                String wordPair = dependency.split("\\(")[1].replace(")", "");
                String word1 = wordPair.split(",")[0].trim();
                String word2 = wordPair.split(",")[1].trim();

                String otherWord = "";

                if (word1.equalsIgnoreCase(baseWordWithPosition)) {
                    otherWord = word2;
                } else {
                    otherWord = word1;
                }

                // Check if the connected word should have a specific word class

                if (targetWordType == 0) // Not care
                {
                    connectedDependencies.add(dependency);
                } else if (targetWordType == 1) // Must be verb
                {
                    int index = Integer.parseInt(otherWord.split("-")[1]);
                    if (taggedWords.get(index - 1).split("/")[1].startsWith("VB")) {
                        connectedDependencies.add(dependency);
                    }
                }

            } // End checking dependency containing baseWord
        } // End Checking each Dependency
        return connectedDependencies;
    }

    public static LinkedList<String> findConnectedDependenciesWithKeyWord(LinkedList<String> dependencyList,
                                                                             LinkedList<String> taggedWords,
                                                                             String baseWord,
                                                                             String omitDepedency,
                                                                             int targetWordType) {
        LinkedList<String> connectedDependencies = new LinkedList<String>();
        // Find every dependency that contains the based word
        for (String dependency : dependencyList) {
            if (dependency.contains(baseWord)) {
                if (dependency.equalsIgnoreCase(omitDepedency)) {
                    continue;
                }

                String wordPair = dependency.split("\\(")[1].replace(")", "");
                String word1 = wordPair.split(",")[0].trim();
                String word2 = wordPair.split(",")[1].trim();

                String otherWord = "";

                if (AccidentConstructorUtil.getWordFromToken(word1).equals(baseWord)) {
                    otherWord = word2;
                } else {
                    otherWord = word1;
                }

                // Check if the connected word should have a specific word class

                if (targetWordType == 0) // Not care
                {
                    connectedDependencies.add(dependency);
                } else if (targetWordType == 1) // Must be verb
                {
                    int index = Integer.parseInt(otherWord.split("-")[1]);
                    if (taggedWords.get(index - 1).split("/")[1].startsWith("VB")) {
                        connectedDependencies.add(dependency);
                    }
                } else if (targetWordType == 100) // Must be a number
                {
                    String otherWordStr = AccidentConstructorUtil.getWordFromToken(otherWord);
                    if (otherWordStr.matches("(\\d*)|(\\d*.\\d*)")) {
                        connectedDependencies.add(dependency);
                    }
                }
            } // End checking dependency containing baseWord
        } // End Checking each Dependency
        return connectedDependencies;
    }

    public static String[] getWordPairFromDependency(String dependency) {
        String[] wordPair = dependency.split("\\(")[1].replace(")", "").split(",");
        wordPair[1] = wordPair[1].trim();
        return wordPair;
    }


    public static String getOtherWordInDep(String baseWord, String[] wordPair) {
        if (wordPair[0].contains(baseWord)) {
            return wordPair[1];
        } else {
            return wordPair[0];
        }
    }

    public static String getDependencyName(String dependency) {
        return dependency.split("\\(")[0];
    }

    public static String getWordFromToken(String token) {
        if (token.indexOf("-") == token.lastIndexOf("-")) {
            return token.split("-")[0].trim();
        } else {
            return token.substring(0, token.lastIndexOf("-")).trim();
        }
    }

    /*
     * Extract the position (the number after "-" sign) in a token of word-index
     */
    public static int getPositionFromToken(String token) {
        int position = -1;
        try {

            if (token.indexOf("-") == token.lastIndexOf("-")) {
                position = Integer.parseInt(token.split("-")[1].trim());
            } else {
                position = Integer.parseInt(token.substring(token.lastIndexOf("-") + 1).trim());
            }
        } catch (Exception ex) {
            ConsoleLogger.print('e', "Cannot locate index in token " + token);
        }
        return position;
    }

    /*
     * Extract the word in a POS token of word/wordType
     */
    public static String getWordFromPOSToken(String token) {
        if (token.indexOf("/") == token.lastIndexOf("/")) {
            return token.split("/")[0].trim();
        } else {
            return token.substring(0, token.lastIndexOf("/")).trim();
        }
    }

    public static String getWordTypeFromPOSToken(String token) {
        if (token.indexOf("/") == token.lastIndexOf("/")) {
            return token.split("/")[1].trim();
        } else {
            return token.substring(token.lastIndexOf("/")).trim();
        }
    }

    public static void searchTermInDepedencyList(LinkedList<String> dependencyList,
                                                    LinkedList<String> taggedWords,
                                                    String term,
                                                    LinkedList<String> omitDepedencies,
                                                    int targetWordType) {

    }

    public static String loadTemplateContent(String path) throws IOException {
        Path filePath = Paths.get(path);

        StringBuilder fileContentSB = new StringBuilder();

        List<String> fileContentList = Files.readAllLines(filePath, Charset.defaultCharset());

        for (String fileLine : fileContentList) {
            fileContentSB.append(fileLine + "\n");
        }

        return fileContentSB.toString();
    }

    public static boolean isNumeric(String str) {
        try {
            double num = Double.parseDouble(str);
        } catch (Exception ex) {
            return false;
        }
        return true;
    }

    public static String displayAs6DecimalNum(double num) {
        try {
            return AccidentParam.df6Digit.format(num);
        } catch (Exception ex) {
            return "Not convertible to 6 decimal digit";
        }
    }

    public static String readValueOfATag(String tagName, String currentFilePath)
            throws IOException, JDOMException, SAXException, ParserConfigurationException {
        File inputFile = new File(currentFilePath);

        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder = factory.newDocumentBuilder();

        Document document = builder.parse(inputFile);
        Element rootElement = document.getDocumentElement();

        NodeList targetTag = rootElement.getElementsByTagName(tagName);

        return targetTag.item(0).getTextContent();
    }

    public static String readValueOfATag(String tagName, String tagIndex, String currentFilePath)
            throws IOException, JDOMException, SAXException, ParserConfigurationException {
        File inputFile = new File(currentFilePath);

        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder = factory.newDocumentBuilder();

        Document document = builder.parse(inputFile);
        Element rootElement = document.getDocumentElement();

        NodeList targetTag = rootElement.getElementsByTagName(tagName);

        return targetTag.item(0).getTextContent();
    }

    // Replace word number with number
    public static String transformWordNumIntoNum(String text) {
        for (int i = 0; i < wordNumber.length; i++) {
            if (text.contains(wordNumber[i] + " ") || text.contains(" " + wordNumber[i])) {
                text = text.replace(" " + wordNumber[i], " " + i)
                        .replace(wordNumber[i] + " ", i + " ");
                continue;
            }
        }
        return text;
    }


    // Convert miles/h to m/s
    public static double convertMPHToMS(double mph) {
        return mph * 0.447;
    }

    // Convert miles/h to km/h
    public static double convertMPHToKMPH(double mph) {
        return mph * 1.6092;
    }

    public static double appendExtraMeterPerSecSpeed(double currentVehicleSpeed) {

        double meterPerSecSpeed = AccidentConstructorUtil.convertMPHToMS(currentVehicleSpeed);
        // If the current speed <= 20, append + 1 m/s
        if(currentVehicleSpeed <= 20)
        {
            meterPerSecSpeed += 0.447;
        }
        else if(currentVehicleSpeed >20)
        {
            meterPerSecSpeed += 0.894;
        }
        return meterPerSecSpeed;
    }

    // Remove all element that has "0" value;
    public static void removeMeaninglessCoord(ArrayList<ArrayList<String>> constructedCoordVeh)
    {
        // For NOW, remove all coord that contain only 0 to see how things happen naturally
        for (int i = 0; i < constructedCoordVeh.size(); i++)
        {
            ArrayList<String> coordList = constructedCoordVeh.get(i);
            if (coordList.size() > 1)
            {
                for (int j = coordList.size() - 1; j >= 0; j--)
                {
                    if (coordList.get(j).equals("0"))
                    {
//                        ConsoleLogger.print('d',"FOund only 0 at " + j);
                        coordList.remove(j);

//                        ConsoleLogger.print('d',"Coord list after remove " + coordList);
                    }
                }

            }
            else
            {
                if (coordList.get(0).equals("0"))
                {
                    coordList.set(0, "0:0");
                }
            }
        }
    }

    public static double computeDistanceBetweenTwo2DPoints(double x1, double y1, double x2, double y2)
    {
        return Math.sqrt(Math.pow(x1 - x2, 2) + Math.pow(y1 - y2, 2));
    }

    public static double computeDistanceBetweenTwo3DPoints(double x1, double y1, double z1, double x2, double y2, double z2)
    {
        return Math.sqrt(Math.pow(x1 - x2, 2) + Math.pow(y1 - y2, 2) + Math.pow(z1 - z2, 2));
    }

    public static double computeGradeIncrement(double xCoord1, double xCoord2, double gradeDegree)
    {
        return gradeDegree / 100 * Math.abs(xCoord1 - xCoord2);
    }

    public static String convertDirectionWordToDirectionLetter(String directionWord)
    {
        // If the street does not have navigation, define it

        String direction = directionWord.replace("bound", "");

        // if direction contains /, split the / and get first direction
        if (direction.contains("/"))
        {
            direction = direction.split("/")[0];
        }


        ConsoleLogger.print('d',"Convert direction " + direction);
        if (direction.equals("north")) {
            direction = "N";
        }
        else if (direction.equals("south")) {
            direction = "S";
        }
        else if (direction.equals("east")) {
            direction = "E";
        }
        else if (direction.equals("west")) {
            direction = "W";
        }
        else if (direction.equals("northeast")) {
            direction = "NE";
        }
        else if (direction.equals("northwest")) {
            direction = "NW";
        }
        else if (direction.equals("southeast")) {
            direction = "SE";
        }
        else if (direction.equals("southwest")) {
            direction = "SW";
        }
        else {
            ConsoleLogger.print('e',"Cannot find defined direction for " + direction);
        }
        return direction;
    }

    // Find a new (x,y) coordinate of a given length, rorated by an angle
    public static double[] computeNewCoordOfRotatedLine(double length, int angle)
    {
        double newCoord[] = new double[2];

        double sinAngleValue = Math.sin(Math.toRadians(angle)) == 0 ? 1 : Math.sin(Math.toRadians(angle));
        double cosAngleValue = Math.cos(Math.toRadians(angle)) == 0 ? 1 : Math.cos(Math.toRadians(angle));

        ConsoleLogger.print('d',"Sin of " + angle + " is " + sinAngleValue);
        ConsoleLogger.print('d',"Cos of " + angle + " is " + cosAngleValue);

        // Compute x by taking length * sin(angle)
        newCoord[0] = length * sinAngleValue;

        // Compute y by taking length * cos(angle)
        newCoord[1] = length * cosAngleValue;

        return newCoord;
    }

    public static double computeCurveCoordIfRadiusGiven(double radius, double updateCoord, double otherCoord)
    {
        if (radius != 0)
        {
            updateCoord = AccidentConstructorUtil.computeXCircleFunc(radius, otherCoord);
        }
        return updateCoord;
    }

    public static int computeOppositeAngle(int angle)
    {
        int oppositeAngle = angle + 180 > 360 ? angle + 180 - 360 : angle + 180;
        return  oppositeAngle;
    }

    /*
     *  coordDimension : 0 = x
     *                   1 = y
     *                   2 = z
     */
    public static String updateCoordElementAtDimension(int coordDimension, String baseCoord,
                                                       String newValueAtCoordDimension, String delimiter)
    {
        String[] coordElements = baseCoord.split(delimiter);
        coordElements[coordDimension] = newValueAtCoordDimension;
        String updatedCoord = coordElements[0] + delimiter + coordElements[1] + delimiter + coordElements[2];
        return updatedCoord;
    }

    public static File[] getAllFilesContainName(String fileName, File caseDamageRecordFolder)
    {
        final String caseName = fileName;
        File[] files = caseDamageRecordFolder.listFiles(new FilenameFilter() {
            @Override
            public boolean accept(File dir, String name) {
                return name.contains(caseName);
            }
        });
        return files;
    }

    public static boolean findNegationOfToken(String token, LinkedList<String> dependencyList)
    {

        for (String dependency : dependencyList)
        {
            if (dependency.contains(token) && dependency.startsWith("neg("))
            {
                return true;
            }
        }
        return false;
    }

    public static void moveFilesToAnotherFolder(String currentPath, final String destinationPath, String fileExtension)
    {
        //Path FROM = Paths.get("C:\\Users\\botes\\Documents\\BeamNG.drive\\levels\\smallgrid\\damageRecord\\");
        final CopyOption[] options = new CopyOption[]{
                StandardCopyOption.REPLACE_EXISTING
        };

        final String extension = fileExtension;

        File caseDamageRecordFolder = new File(currentPath);

        File [] files = caseDamageRecordFolder.listFiles(new FilenameFilter() {
            @Override
            public boolean accept(File dir, String name) {
                try {
                    Files.move(Paths.get(dir + "/" + name), Paths.get(destinationPath + name), options);
                } catch (IOException e) {
                    e.printStackTrace();
                }
                return name.contains(extension);
            }
        });

    }

    public static ArrayList<Double> computeDistanceAndTimeWithAcceleration(double velocity)
    {
        ArrayList<Double> distanceAndTime = new ArrayList<Double>();

        double accumulateDistance = 0;
        double accumulateVelocity = 0;
        double chosenAcceleration = AccidentParam.accelerationTo20Mph;

        if (velocity <= 40)
        {
            chosenAcceleration = AccidentParam.accelerationTo20Mph;
        }

        double reachVelocityTime = velocity / chosenAcceleration;
        double acceleratedTravellingDistance = 0.5 * chosenAcceleration * reachVelocityTime * reachVelocityTime;

        distanceAndTime.add(acceleratedTravellingDistance);
        distanceAndTime.add(reachVelocityTime);
//        // Calculate accumulated distance using formula s = v0 + a(n - 0.5) with v0 = 0 and n = i
//        for (int i = 1; ;i++)
//        {
//            accumulateDistance += chosenAcceleration * i - chosenAcceleration / 2;
//            accumulateVelocity += chosenAcceleration * i;
//            if (accumulateDistance > distance)
//            {
//                return i - 1;
//            }
//        }

        // Compute the distance

        return distanceAndTime;
    }

    public static String getRoadMaterial(Street street)
    {
        String roadMaterial = street.getStreetPropertyValue("road_material");
        if (roadMaterial.contains("dirt"))
        {
            return AccidentParam.dirtMaterial;
        }
        else
        {
            return AccidentParam.asphaltMaterial;
        }
    }

    public static double getNonCriticalDistance()
    {
        String extraDistance = System.getProperty("extraNonCriticalDistance", "0");
        try {
            return Double.parseDouble(extraDistance);
        } catch (Exception ex) {
            ConsoleLogger.print('e', "Error at getting extra non-critical distance \n" + ex);
            return 0;
        }
    }

    // Find the lane which a vehicle travels on
    public static int detectTravellingLane(String connectedWordChain)
    {
        String[] connectedWords = connectedWordChain.split(",");

        // Find the word contain "lane" string, get the index of the word, and find a number in the previous or next 2
        // connected words (detect these words by looking at the index attached to each word)
        for (int i = 0; i < connectedWords.length; i++)
        {
            String currentWord = connectedWords[i];
            if (currentWord.startsWith("lane"))
            {
                // Get index of the "lane" word
                int laneWordIndex = Integer.parseInt(currentWord.split("-")[1]);

                // Find the words adjacent to lane with index = laneWordIndex +- 2
                for (int k = connectedWords.length - 1; k >= 0; k--)
                {
                    String[] wordAndIndex = connectedWords[k].split("-");
                    // If the word is a number, and it is at most 2 positions next to the lane word, then this should
                    // be the lane number
                    if (wordAndIndex[0].matches("\\d*")
                            && Math.abs(laneWordIndex - Integer.parseInt(wordAndIndex[1])) <= 2)
                    {
                        return Integer.parseInt(wordAndIndex[0]);
                    }
                } // End reversed looping through connectedWordString
            } // End "lane" word detection
        } // End looping through connectedWordString
        return AccidentParam.RIGHTMOSTLANE; // If the lane is not found, set the vehicle at the right most lane
    }

    public static boolean searchForWordInParagragh(String keyword, String paragraph)
    {
        String[] words = paragraph.split(" ");
        boolean keywordIsFound = false;
        for (int i = 0; i < words.length; i++)
        {
            String word = words[i];

            word = word.replace(",", "")
                    .replace(".","")
                    .replace(";", "")
                    .replace(":", "");

            if (word.trim().startsWith(keyword))
            {
                keywordIsFound = true;
            }
        }
        return keywordIsFound;
    }

    // Find the point in the turning street which the vehicle should turn into based on its turn direction
    public static double[] findTurningPointInTurningStreet(Street turnStreet, String turnDirection, String movingDirection) {
        String turnStreetNodes = turnStreet.getStreetPropertyValue("road_node_list");

        String[] nodeList = turnStreetNodes.replace("[", "").split(AccidentParam.STREET_NODE_DELIMITER);

        String[] centerNodeCoord = nodeList.length == AccidentParam.ROAD_PIECE_NODE * 2 + 1 ?
            nodeList[nodeList.length / 2].trim().split(AccidentParam.beamngCoordDelimiter)
            : nodeList[nodeList.length - 1].trim().split(AccidentParam.beamngCoordDelimiter);
        double[] centerNode = {
            Double.parseDouble(centerNodeCoord[0]),
            Double.parseDouble(centerNodeCoord[1]),
            Double.parseDouble(centerNodeCoord[2])};

        String[] beforeCenterNodeCoord = nodeList.length == AccidentParam.ROAD_PIECE_NODE * 2 + 1 ?
            nodeList[nodeList.length / 2 - 1].trim().split(AccidentParam.beamngCoordDelimiter)
            : nodeList[nodeList.length - 2].trim().split(AccidentParam.beamngCoordDelimiter);
        double[] beforeCenterNode = {
            Double.parseDouble(beforeCenterNodeCoord[0]),
            Double.parseDouble(beforeCenterNodeCoord[1]),
            Double.parseDouble(beforeCenterNodeCoord[2])};

        String[] afterCenterNodeCoord = nodeList.length == AccidentParam.ROAD_PIECE_NODE * 2 + 1 ?
            nodeList[nodeList.length / 2 + 1].trim().split(AccidentParam.beamngCoordDelimiter)
            : centerNodeCoord;

        double[] afterCenterNode = {
            Double.parseDouble(afterCenterNodeCoord[0]),
            Double.parseDouble(afterCenterNodeCoord[1]),
            Double.parseDouble(afterCenterNodeCoord[2])};

        // If this is a continuous road (8 nodes + center node), use NavigationDictionary to
        // find the turning point based on the moving direction and turn direction
        if (nodeList.length == AccidentParam.ROAD_PIECE_NODE * 2 + 1) {
            HashMap<String, String> navDictionary = NavigationDictionary
                .selectDictionaryFromTravelingDirection(movingDirection);

            // pointAdjustment[0] is xCoord Adjustment, '+' for increase xCoord, '-' for decrease xCoord
            // pointAdjustment[1] is yCoord Adjustment, '+' for increase yCoord, '-' for decrease yCoord
            String[] pointAdjustment = navDictionary.get(turnDirection)
                .split(NavigationDictionary.NAVIGATION_MAP_DELIMITER);

            // xCoord of beforeCenter point < xCoord of centerPoint
            // if yCoord also matches with the pointAdjustment requirement, return the beforeCenterNodeCoord
            // Otherwise return the afterCenterNodeCoord
            if (checkCoordForTurningPoint(beforeCenterNode[0], centerNode[0], pointAdjustment[0])
                && checkCoordForTurningPoint(beforeCenterNode[1], centerNode[1], pointAdjustment[1])) {
                ConsoleLogger.print('d', "Return before center node " + Arrays.toString(beforeCenterNodeCoord) + " as turning point" );
                return beforeCenterNode;
            }
            ConsoleLogger.print('d', "Return after center node " + afterCenterNodeCoord + " as turning point ");
            return afterCenterNode;

        }
        // For non-continuous road, take the point next to the crash point as there is no other option
        else if (nodeList.length ==  AccidentParam.ROAD_PIECE_NODE + 1)
        {
            return beforeCenterNode;
        }

        return new double[] {-1000, -1000, -1000};
    }

    // Check the requirement for yCoord of turning point, return true if the yCoord of the targetYCoord
    // matches with the condition of the pointAdjustment String.
    private static boolean checkCoordForTurningPoint(double targetCoord, double centerCoord, String pointAdjustment)
    {
        if (pointAdjustment.equals("-") && targetCoord < centerCoord) {
            return true;
        } else if (pointAdjustment.equals("0") && targetCoord == centerCoord) {
            return true;
        } else return pointAdjustment.equals("+") && targetCoord > centerCoord;
    }

    public static VehicleAttr findVehicle(String vehicleName, ArrayList<VehicleAttr> vehicleList) {
        for (VehicleAttr vehicleAttr : vehicleList) {
            if (vehicleName.replace("vehicle", "").equalsIgnoreCase("" + vehicleAttr.getVehicleId())) {
                return vehicleAttr;
            }
        }
        return null;
    }

    public static String getActionBeforeImpact(VehicleAttr vehicle) {
        String actionBeforeImpact = "";

        // Find whether the action before hit is turn
        for (int z = vehicle.getActionList().size() - 1; z >= 0 ; z--)
        {
            if (vehicle.getActionList().get(z).startsWith("hit"))
            {
                actionBeforeImpact = vehicle.getActionList().get(z - 1);
            }
        }
        return actionBeforeImpact;
    }
}

