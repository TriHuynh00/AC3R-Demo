package org.saarland.xmlmodules;

import org.jdom2.JDOMException;
import org.saarland.accidentconstructor.AccidentConstructor;
import org.saarland.accidentconstructor.AccidentConstructorUtil;
import org.saarland.accidentconstructor.ConsoleLogger;
import org.saarland.accidentelementmodel.VehicleAttr;
import org.saarland.configparam.AccidentParam;
import org.saarland.ontologyparser.AccidentConcept;
import org.saarland.ontologyparser.OntologyHandler;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class XMLAccidentCaseParser {

    private File inputFile;

    private static String currentFilePath;

    private DocumentBuilderFactory factory;

    private DocumentBuilder builder;

    private static Element rootElement;

    public XMLAccidentCaseParser(String filePath) throws ParserConfigurationException {
        currentFilePath = filePath;

        factory = DocumentBuilderFactory.newInstance();
        builder = factory.newDocumentBuilder();
    }

    public String[] parseAccidentXmlFile(AccidentConstructor accidentConstructor, OntologyHandler parser)
            throws IOException, JDOMException, SAXException, ParserConfigurationException
    {
        // Remove <= and >= signs in the XML file
        inputFile = new File(currentFilePath);
//        removeSpecialCharsInXMLFile(inputFile);

        String xmlDatabase = "CIREN"; // "NMVCCS"

        HashMap<String, String> xmlTagCrashDB = new HashMap<String, String>();

        if (xmlDatabase.equals(AccidentParam.DB_CRASH_CIREN)) {
            xmlTagCrashDB.put("Vehicle", "GeneralVehicleForm");
            xmlTagCrashDB.put("Make", "Make");
            xmlTagCrashDB.put("BodyType", "Model");
            xmlTagCrashDB.put("Transport", "InTransport");
            xmlTagCrashDB.put("ModelYear", "ModelYear");

        } else {
            xmlTagCrashDB.put("Vehicle", "VEHICLE");
            xmlTagCrashDB.put("Make", "XML_MAKEMODEL");
            xmlTagCrashDB.put("BodyType", "BODY_TYPE");
            xmlTagCrashDB.put("Transport", "TRANSPORT");
            xmlTagCrashDB.put("ModelYear", "MODELYEAR");
        }

        Document document = builder.parse(inputFile);
        rootElement = document.getDocumentElement();

        accidentConstructor.setVehicleList(new ArrayList<VehicleAttr>());

        // Find Weather Info
        NodeList accidentTypeTag = rootElement.getElementsByTagName("CONFIGDESC");
        if (accidentTypeTag.item(0) == null) {
            // Read crash type in NMMVCS XML file
            accidentTypeTag = rootElement.getElementsByTagName("CRASH_TYPE");

            if (xmlDatabase.equals(AccidentParam.DB_CRASH_CIREN))
                accidentTypeTag = rootElement.getElementsByTagName("CrashType");

        }

        accidentConstructor.setAccidentType(accidentTypeTag.item(0).getTextContent().trim().toLowerCase());

        ConsoleLogger.print('d', "Accident Type " + accidentConstructor.getAccidentType());
        accidentConstructor.getTestCase().setCrashType(accidentTypeTag.item(0).getTextContent().trim().toLowerCase());

        // Find Auto Info
        NodeList vehicleTags = rootElement.getElementsByTagName(xmlTagCrashDB.get("Vehicle"));
//            NodeList vehicleTags = rootElement.getElementsByTagName("Vehicles");
        ConsoleLogger.print('d', "Vehicle Tag length " + vehicleTags.getLength());

        for (int i = 0; i < vehicleTags.getLength(); i++) {
            String yearMakeModelStr = "";
            Node vehicleInfo = vehicleTags.item(i);
            Node vehicleInfoIterator = vehicleInfo.getFirstChild();
            VehicleAttr vehicle = new VehicleAttr();

            if (xmlDatabase.equals(AccidentParam.DB_CRASH_CIREN)) {
                ConsoleLogger.print('d', vehicleInfo.getAttributes().getNamedItem("VehicleNumber"));

                vehicle.setVehicleId(Integer.parseInt(
                        vehicleInfo.getAttributes().getNamedItem("VehicleNumber").getNodeValue().trim())
                );

                // Get into Vehicle Tags
                vehicleInfoIterator = vehicleInfoIterator.getFirstChild();
            }

            while (vehicleInfoIterator.getNextSibling() != null) {
                ConsoleLogger.print('d', "1st Node Name " + vehicleInfoIterator.getNodeName());

                // Get Make Model Value
                if (vehicleInfoIterator.getNodeName().equalsIgnoreCase(xmlTagCrashDB.get("Make"))) {
                    ConsoleLogger.print('d', "Found MAKEMODEL");
                    yearMakeModelStr += vehicleInfoIterator.getTextContent().trim();
                    ConsoleLogger.print('d', "Make Model " + i + " " + vehicleInfoIterator.getNodeName() + " " + vehicleInfoIterator.getTextContent().trim());
                }
                // Get Vehicle ID
//                vehicleInfoIterator = vehicleInfoIterator.getNextSibling().getNextSibling();

//                ConsoleLogger.print('d', "Next2 : " + vehicleInfoIterator.getNodeName());

                else if (vehicleInfoIterator.getNodeName().trim().equalsIgnoreCase("VEHNUMBER")) {
                    ConsoleLogger.print('d', "Found VEHNUMBER");
                    vehicle.setVehicleId(Integer.parseInt(vehicleInfoIterator.getAttributes().getNamedItem("VehicleNumber").getNodeValue().trim()));
                    ConsoleLogger.print('d', "Vehicle ID " + i + " " + vehicleInfoIterator.getTextContent().trim());
                }



                // Finalize Year + Make + Model Value
//                vehicleInfoIterator = vehicleInfoIterator.getNextSibling().getNextSibling();


                else if (vehicleInfoIterator.getNodeName().trim().equalsIgnoreCase(xmlTagCrashDB.get("ModelYear"))) {
                    //                if (vehicleInfoIterator.getNodeName().trim().equalsIgnoreCase("Year")) {
                    ConsoleLogger.print('d', "Found MODELYEAR");
                    yearMakeModelStr = vehicleInfoIterator.getTextContent() + " " + yearMakeModelStr.toLowerCase()
                            .replace(" | ", " ")
                            .replace(" / ", " ")
                            .replace("/ ", " ")
                            .replace("| ", " ")
                            .replace(" /", " ")
                            .replace(" |", " ")
                            .replace("|", " ")
                            .replace("/", " ");
                    vehicle.setYearMakeModel(yearMakeModelStr);
                    ConsoleLogger.print('d', "ModelYear : " + yearMakeModelStr);
                }

                // SKIP MAKE and MODEL Tags

                //            vehicleInfoIterator = vehicleInfoIterator.getNextSibling().getNextSibling();
                //            vehicleInfoIterator = vehicleInfoIterator.getNextSibling().getNextSibling();
                //            vehicleInfoIterator = vehicleInfoIterator.getNextSibling().getNextSibling();
                else if (vehicleInfoIterator.getNodeName().trim().equalsIgnoreCase(xmlTagCrashDB.get("BodyType"))) {
                    //            if (vehicleInfoIterator.getNodeName().trim().equalsIgnoreCase("Model")) {

                    String defaultJBeamStyle = "etk800";
                    String defaultPartConfig = "vehicles/etk800/etk854t_A.pc";

                    String useDefaultCarModel = System.getProperty("defaultCarModel", "true");

                    ConsoleLogger.print('r', "USE DEFAULT CAR MODEL " + useDefaultCarModel);

                    String bodyType = vehicleInfoIterator.getTextContent().trim().replace(" ", "_").toLowerCase();
                    ConsoleLogger.print('d', "Found BODY_TYPE " + bodyType);
                    AccidentConcept bodyTypeConcept = parser.findExactConceptInKeyword(bodyType);

                    if (bodyTypeConcept != null && bodyTypeConcept.getLeafLevelName().equals("car_model")) {
                        vehicle.setVehicleType(bodyType);
                    }

                    if (useDefaultCarModel.equals("true")) {
                        vehicle.setPartConfig(defaultPartConfig);
                        vehicle.setBeamngVehicleModel(defaultJBeamStyle);
                    } else {
                        try {
                            if (bodyTypeConcept.getLeafLevelName().equals("car_model")) {

                                if (bodyTypeConcept.getDataProperties() != null) {
                                    vehicle.setPartConfig(bodyTypeConcept.getDataProperties().get("partconfig"));
                                    vehicle.setBeamngVehicleModel(bodyTypeConcept.getDataProperties().get("jbeam"));
                                } else {
                                    vehicle.setPartConfig(defaultPartConfig);
                                    vehicle.setBeamngVehicleModel(defaultJBeamStyle);
                                }
                            }
                        } catch (Exception ex) {
                            ConsoleLogger.print('d', "Exception at extracting body type \n" + ex.toString());
                            vehicle.setPartConfig(defaultPartConfig);
                            vehicle.setBeamngVehicleModel(defaultJBeamStyle);
                        }
                    }
                }

//                // Skip HIT_CLASS
//                vehicleInfoIterator = vehicleInfoIterator.getNextSibling().getNextSibling();
//
//
//                // Get Color
//                vehicleInfoIterator = vehicleInfoIterator.getNextSibling().getNextSibling();

                else if (vehicleInfoIterator.getNodeName().trim().equalsIgnoreCase("COLOR")) {

                    String defaultColorCode = "1 1 1"; // Set default as white
                    String colorName = vehicleInfoIterator.getTextContent().toLowerCase().trim().replace(" ", "_");
                    ConsoleLogger.print('d', "Found COLOR " + colorName);
                    String colorCode = defaultColorCode;

                    String[] colorVals = colorName.split("/");

                    for (String color : colorVals) {

                        colorCode = readColorValue(color, parser);
                        if (!colorCode.equals("none")) {
                            vehicle.setColor(colorCode);
                            break;
                        }
                    }
                    if (colorCode.equals("none")) {
                        vehicle.setColor(defaultColorCode);
                    }

                    //                AccidentConcept colorConcept = parser.findConcept(colorName);
                    //                // Find if the color is in the Ontology
                    //                try {
                    //                    if (colorConcept.getLeafLevelName().equals("color")) {
                    //
                    //                        if (colorConcept.getDataProperties() != null) {
                    //                            vehicle.setColor(colorConcept.getDataProperties().get("rgb_code"));
                    //                        } else {
                    //                            vehicle.setColor(defaultColorCode);
                    //                        }
                    //                    } else {
                    //                        vehicle.setColor(defaultColorCode);
                    //                    }
                    //                } catch (Exception ex) {
                    //                    ConsoleLogger.print('d', "Exception at extracting color \n" + ex.toString());
                    //                    vehicle.setColor(defaultColorCode);
                    //                }

                }

                // Get Whether the car is travelling
                else if (vehicleInfoIterator.getNodeName().trim().equalsIgnoreCase(xmlTagCrashDB.get("Transport"))) {
                    ConsoleLogger.print('d', "Found TRANSPORT");
                    String transportStatus = vehicleInfoIterator.getTextContent().trim().toLowerCase();
                    if (transportStatus.equalsIgnoreCase("not in transport")
                            || transportStatus.equalsIgnoreCase("No")) {
                        vehicle.setOnStreet(0);
                    } else {
                        vehicle.setOnStreet(1);
                    }

                }
                vehicleInfoIterator = vehicleInfoIterator.getNextSibling();
            }
            ConsoleLogger.print('d', "-------------");
            ConsoleLogger.print('r', String.format("Vehicle %d Info \n" +
                            "YearMakeModel: %s \n " +
                            "Type: %s \n " +
                            "Color: %s \n " +
                            "Onstreet %d \n",
                    //"On Street: %s \n ",
                    vehicle.getVehicleId(),
                    vehicle.getYearMakeModel(),
                    vehicle.getBeamngVehicleModel(),
                    vehicle.getColor(),
                    vehicle.getOnStreet()));

            accidentConstructor.getVehicleList().add(vehicle);
        }

        // Find Summary
        //NodeList summaryTag = rootElement.getElementsByTagName("SUMMARY");
        NodeList summaryTag = rootElement.getElementsByTagName("Summary");
        ConsoleLogger.print('d', summaryTag.getLength());

        String storyline = summaryTag.item(0).getTextContent().trim().replace("\t", "");

        storyline = AccidentConstructorUtil.transformWordNumIntoNum(storyline);

//        ConsoleLogger.print('d',storyline);

        storyline = normalizeSpecialChars(storyline);

        String[] paragraphs = storyline.split("\n\n");

        ConsoleLogger.print('d', "Para Len " + paragraphs.length);

        ConsoleLogger.print('d', "Para 0: " + paragraphs[0]);

        // Replace Vehicle Model Name with Vehicle[ID]
        ConsoleLogger.print('d', "Before: " + paragraphs[1]);

        for (VehicleAttr vehicleAttr : accidentConstructor.getVehicleList()) {
            ConsoleLogger.print('d', "vehicleAttr: " + vehicleAttr.getVehicleId());
            String replacedModelNameStr = paragraphs[1].toLowerCase().replace(vehicleAttr.getYearMakeModel(), "vehicle" + vehicleAttr.getVehicleId() + " ");
            if (replacedModelNameStr.equals(paragraphs[1].toLowerCase())) {
                String makeYear = vehicleAttr.getYearMakeModel().split(" ")[0];
                ConsoleLogger.print('d', "Make year extraction " + makeYear);
                replacedModelNameStr = replacedModelNameStr.replace(vehicleAttr.getYearMakeModel().replace(makeYear, "").trim(), "vehicle" + (vehicleAttr.getVehicleId())).trim();
                for (String model : vehicleAttr.getYearMakeModel().split(" ")) {
                    replacedModelNameStr = replacedModelNameStr.replace(model, "vehicle" + (vehicleAttr.getVehicleId()));
                }
                Pattern multipleVehiclePattern = Pattern.compile("(vehicle\\d.){2,3}");
                Matcher matcherMultipleVehiclePattern = multipleVehiclePattern.matcher(replacedModelNameStr);
                while (matcherMultipleVehiclePattern.find()) {
                    String multiVehicle = matcherMultipleVehiclePattern.group();
                    replacedModelNameStr = replacedModelNameStr.replace(multiVehicle,
                            multiVehicle.split(" ")[0] + multiVehicle.substring(multiVehicle.length() - 1));
                }

            }
            paragraphs[1] = replacedModelNameStr;
//            paragraphs[1].replace(paragraphs[1].substring())

        }

        paragraphs[1] = paragraphs[1].replace("vehicle #", "vehicle");
        paragraphs[1] = paragraphs[1].replace("Vehicle #", "vehicle");


//        paragraphs[1] = paragraphs[1].replaceAll(",\\w", ", ");
        // Fix "vehicle [ID]" typo
        Pattern vehicleSpaceNumberPattern = Pattern.compile("((vehicle)|(Vehicle)) \\d");
        Pattern commaWordPattern = Pattern.compile(",[A-Za-z0-9]");
        Pattern vAsVehiclePattern = Pattern.compile("v\\d");

        Matcher matcherVehicleSpaceNumber = vehicleSpaceNumberPattern.matcher(paragraphs[1]);
        Matcher matcherCommaWord = commaWordPattern.matcher(paragraphs[1]);
        Matcher matcherVAsVehicle = vAsVehiclePattern.matcher(paragraphs[1]);

        while (matcherVehicleSpaceNumber.find()) {
            String typoString = matcherVehicleSpaceNumber.group();
            paragraphs[1] = paragraphs[1].replace(typoString, typoString.replace(" ", "") + " ");
        }

        while (matcherCommaWord.find()) {
            String typoString = matcherCommaWord.group();
            paragraphs[1] = paragraphs[1].replace(typoString, typoString.replace(",", ", "));
        }

        while (matcherVAsVehicle.find()) {
            String typoString = matcherVAsVehicle.group();
            paragraphs[1] = paragraphs[1].replace(typoString, typoString.replace("v", "vehicle"));
        }

//        Pattern vehicleSpaceWithoutNumberPattern = Pattern.compile(" ((vehicle)|(Vehicle)) ");
//        Matcher matcherVehicleSpaceWithoutNumberPattern = vehicleSpaceWithoutNumberPattern.matcher(paragraphs[1]);
        // We can surely infer the ID of the other vehicle, if there are up to 2 vehicles involved in the scenario
//        int involvedVehiclesNum = accidentConstructor.getVehicleList().size();
//        if (involvedVehiclesNum <= 2)
//        {
//            paragraphs[1] = paragraphs[1].replace(" vehicle ", " vehicle" + involvedVehiclesNum + " ");
//        }
        paragraphs[1] = paragraphs[1].replace("  ", " ").replace(" ,", ",");
        ConsoleLogger.print('d', "After: " + paragraphs[1]);

        String[] accidentContext = {paragraphs[0], paragraphs[1]};
        return accidentContext;

    }



    public String readRoadCurveValue(String filePath)
            throws IOException, JDOMException, SAXException, ParserConfigurationException {
        File inputFile = new File(filePath);

        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder = factory.newDocumentBuilder();

        Document document = builder.parse(inputFile);
        Element rootElement = document.getDocumentElement();

        NodeList roadwayAlignTag = rootElement.getElementsByTagName("ROADWAY_ALIGN");

        return roadwayAlignTag.item(0).getTextContent();
    }

    // Mode 2: Read tags that can have > 1 instance
    //      1: Read tag that only has 1 instance (eg. environment properties)
    public static String readMultipleTagWithNode(String mainTagName, String specificTagName, int mode) {
        if (mode == 1) {

            NodeList targetTag = rootElement.getElementsByTagName(specificTagName);
            ConsoleLogger.print('d',"Read single Tag name " + specificTagName + " value is " + targetTag.item(0).getTextContent());
            return targetTag.item(0).getTextContent();
        } else if (mode == 2) {
            NodeList targetTags = rootElement.getElementsByTagName(mainTagName);
            for (int i = 1; i <= targetTags.getLength(); i++) {
                Node tagInfo = targetTags.item(i);
                Node tagInfoIterator = tagInfo.getFirstChild().getNextSibling();

                while (!tagInfoIterator.getNodeName().trim().equalsIgnoreCase(specificTagName)) {
                    tagInfoIterator = tagInfoIterator.getNextSibling().getNextSibling();
                }
                return tagInfoIterator.getTextContent();
            }
        }
        return "";
    }

    public static String readTagOfAGivenOrder(String mainTagName, String specificTagName, int objectIndex) {
        try {
            NodeList targetTags = rootElement.getElementsByTagName(specificTagName);

            Node tagInfo = targetTags.item(objectIndex);

            if (tagInfo != null) {
                ConsoleLogger.print('d', "tagInfo " + tagInfo.getNodeName());

                Node tagInfoIterator = tagInfo.getFirstChild();

                ConsoleLogger.print('d', "tagInfoIterator " + tagInfoIterator.getTextContent());

                return AccidentConstructorUtil.transformWordNumIntoNum(tagInfoIterator.getTextContent().trim());
            }

        } catch (Exception ex) {
            ConsoleLogger.print('d', "Error when read tag of a given order " +
                    "with specificTagName = " + specificTagName);
            ex.printStackTrace();

        }

        return "";
    }

    private String readColorValue(String colorName, OntologyHandler parser)
    {
        String colorCode = "none"; // Set default as none
        AccidentConcept colorConcept = parser.findConcept(colorName);
        // Find if the color is in the Ontology
        try {
            if (colorConcept.getLeafLevelName().equals("color")) {

                if (colorConcept.getDataProperties() != null) {
                    return colorConcept.getDataProperties().get("rgb_code");
                } else {
                    return colorCode;
                }
            }
            else
            {
                return colorCode;
            }
        } catch (Exception ex) {
            ConsoleLogger.print('e', "Exception at extracting color \n" + ex.toString());
            return colorCode;
        }

    }

    private String normalizeSpecialChars(String text)
    {
        text = text.replace("\\r\\r\\n", "\n");
        text = text.replace("\\r\\n", "\n");
        text = text.replace("\\t", "");
        text = text.replace("\\'", "'");
        return text;
    }
}