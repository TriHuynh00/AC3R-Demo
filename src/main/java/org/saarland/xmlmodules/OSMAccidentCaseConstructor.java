package org.saarland.xmlmodules;

import org.saarland.accidentelementmodel.VehicleAttr;
import org.w3c.dom.Attr;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.File;
import java.util.ArrayList;
import java.util.LinkedList;

public class OSMAccidentCaseConstructor {
    public void constructOSMTestCase(ArrayList<VehicleAttr> vehicleList, String filename)
    {
        try {
            DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
            Document doc = dBuilder.newDocument();

            // root element
            Element rootElement = doc.createElement("osm");
            doc.appendChild(rootElement);

            // Construct nodes storing the properties of vehicle

            for (VehicleAttr vehicle : vehicleList)
            {
                Element vehicleNode = doc.createElement("node");

                vehicleNode.setAttribute("id", "vehicle" + vehicle.getVehicleId());

                Element vehicleInitSpeedProp = doc.createElement("initSpeed");
                vehicleInitSpeedProp.appendChild(doc.createTextNode(vehicle.getVelocity() + ""));

                vehicleNode.appendChild(vehicleInitSpeedProp);

                rootElement.appendChild(vehicleNode);
            }

            // Construct movement path for each vehicle
            for (VehicleAttr vehicle : vehicleList)
            {

                ArrayList<String> vehicleMovementPath = vehicle.getMovementPath();
                for (int i = 0; i < vehicleMovementPath.size(); i++)
                {
                    Element vehicleNode = doc.createElement("node");

                    String[] xyPoint = vehicleMovementPath.get(i).split(":");

                    vehicleNode.setAttribute("id", "vehicle" + vehicle.getVehicleId() + "point" + i);

                    vehicleNode.setAttribute("xCoord", xyPoint[0]);

                    vehicleNode.setAttribute("yCoord", xyPoint[1]);

                    rootElement.appendChild(vehicleNode);
                }

                // Group vehicle to its corresponding movement path
                Element vehicleWayNode = doc.createElement("way");
                vehicleWayNode.setAttribute("id", "vehicle" + vehicle.getVehicleId() + "movementPath");

                Element refVehicleNode = doc.createElement("nd");
                refVehicleNode.setAttribute("id", vehicle.getVehicleId() + "");

                vehicleWayNode.appendChild(refVehicleNode);

                for (int i = 0; i < vehicleMovementPath.size(); i++)
                {
                    Element coordNode = doc.createElement("nd");

                    coordNode.setAttribute("ref", "vehicle" + vehicle.getVehicleId() + "point" + i);

                    vehicleWayNode.appendChild(coordNode);
                }

                rootElement.appendChild(vehicleWayNode);

            }

            // write the content into xml file
            TransformerFactory transformerFactory = TransformerFactory.newInstance();
            Transformer transformer = transformerFactory.newTransformer();
            transformer.setOutputProperty(OutputKeys.INDENT, "yes");
            transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2");
            DOMSource source = new DOMSource(doc);
            StreamResult result = new StreamResult(new File(filename));
            transformer.transform(source, result);

            // Output to console for testing
            StreamResult consoleResult = new StreamResult(System.out);
            transformer.transform(source, consoleResult);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
