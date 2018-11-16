package org.saarland;

import java.io.File;
import java.io.IOException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.junit.Assert;
import org.junit.Test;
import org.saarland.accidentconstructor.AccidentConstructorUtil;
import org.saarland.accidentconstructor.ConsoleLogger;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

public class XMLParserTest {

	@Test
	public void splitParagraphs() {
		String original = "" // First paragraph
				+ "This single-vehicle crash occurred in a rural mountainous area on a 2-lane asphalt roadway with various curves and elevations."
				+ "  The westbound lane curves left then right and is uphill."
				+ "  There is a downhill embankment on the eastbound roadside with no guardrail present."
				+ "  The posted speed limit is 64 kmph (40 mph)."
				+ "  The crash occurred at noon during daylight hours with no adverse weather or road conditions present.\r\r\n\r\r\n"
				+ "" // Second paragraph
				+ "Vehicle one (V1), a 1993 Mazda 626 4-door sedan, was westbound when it crossed the eastbound lane to the left and departed the left road edge."
				+ "  V1 struck the embankment and a mesquite bush with its front-end plane."
				+ "  V 1 rotated clock-wise then tripped and flipped 2 quarter turns to the left where it came to rest on its top facing west."
				+ "  V1 was towed due to damage.\r\r\n\r\r\n" + "" // Third
																	// paragraph
				+ "The driver, a eighteen (18) year-old female, and her two passengers were traveling up the hill towards a mountain recreation area."
				+ "  The driver was unable to be contacted."
				+ " According to the police report, the driver had consumed an unknown amount of alcohol."
				+ " Many beer cans were present in the vehicle during the vehicle inspection."
				+ "  Statements in the police report indicate the driver missed the curve and drove in a straight path off the left side of the road."
				+ "  The medical records also confirmed the driver had consumed alcohol, but no test results were included for the driver.\r\r\n\r\r\n"
				+ "" // Fourth paragraph
				+ "The critical pre-crash event was coded: this vehicle traveling, off the edge of the road on the left side."
				+ " The critical reason for the critical  event was coded driver related factor: poor directional control (e.g., failing to control vehicle with skill ordinarily expected)."
				+ "  The driver failed to negotiate the curve and departed the roadway in a straight path.\r\r\n";

		Assert.assertEquals(4, AccidentConstructorUtil.getParagraphs(original).length);
	}

	@Test
	public void splitParagraphsFor2007074433845() {
		String original = "" // 1st
				+ "The crash occurred on a 2-lane undivided roadway with a posted speed limit of 89 KPH (55 MPH).  Vehicle #1 was negotiating a curve to the right with a radius of curvature of 468.75 meters and a 3.2% superelevation.  The roadway also had a -2.7% grade.  The weather was clear, the roadway dry and it was daylight at the time of this early morning crash.\r\r\n\r\r\n"
				+ "" // 2nd
				+ "     Vehicle #1, a 1995 Dodge Neon, was traveling south on the 2 lane undivided roadway negotiating the curve to the right.  Operator of Vehicle #1 fell asleep and drove across the northbound lane and off the left side of the roadway.  Vehicle #1 continued and struck 2 mailbox posts with its front.  Vehicle #1 continued a short distance and came to rest facing south. \r\r\n\r\r\n"
				+ "" // 3rd
				+ "     The Dodge Neon was driven by a belted 18-year-old female who was transported, treated and released at a local hospital for a possible injury to the knee. She stated that she was sound asleep and did not know any details of the crash.  The driver was not taking any medications.  She wears glasses for reading only. Vehicle #1 was towed due to damage. \r\r\n\r\r\n"
				+ "" // 4th
				+ "     The Critical Precrash Event for Vehicle #1 was off the edge on the left side of the travel lane.  The Critical Reason for the Critical Event was that the driver fell asleep, which was a critical non-performance error. This was chosen due to the driver\'s statements.  It should be noted that she worked on call, had only 5 hours of sleep prior to the crash and had \"been out\" the previous night.  Police did not consider either alcohol or illegal drugs to be involved and ordered no tests.";
		Assert.assertEquals(4, AccidentConstructorUtil.getParagraphs(original).length);
	}

	/*
	 * /Users/gambi/AC3R/paper/dataset/allCases/2005002229162.xml
	 * /Users/gambi/AC3R/paper/dataset/allCases/2007074433845.xml
	 * /Users/gambi/AC3R/paper/dataset/allCases/2007074531367.xml
	 * /Users/gambi/AC3R/paper/dataset/allCases/2007074531689.xml
	 * /Users/gambi/AC3R/paper/dataset/allCases/2007074531773.xml
	 * /Users/gambi/AC3R/paper/dataset/allCases/2007074595187.xml
	 * /Users/gambi/AC3R/paper/dataset/allCases/2007074595665.xml
	 * /Users/gambi/AC3R/paper/dataset/allCases/2007074751969.xml
	 * /Users/gambi/AC3R/paper/dataset/allCases/2007075584589.xml
	 * /Users/gambi/AC3R/paper/dataset/allCases/2007075584744.xml
	 * /Users/gambi/AC3R/paper/dataset/allCases/2007075584809.xml
	 * /Users/gambi/AC3R/paper/dataset/allCases/2007075702149.xml
	 * /Users/gambi/AC3R/paper/dataset/allCases/2007076197431.xml
	 * /Users/gambi/AC3R/paper/dataset/allCases/2007076197911.xml
	 */
	@Test
	public void splitParagraphFromXML() throws ParserConfigurationException, SAXException, IOException {
		DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
		DocumentBuilder builder = factory.newDocumentBuilder();
	        
		// Remove <= and >= signs in the XML file
		File inputFile = new File("/Users/gambi/AC3R/paper/dataset/allCases/2005002229162.xml");
		Document document = builder.parse(inputFile);
		Element rootElement = document.getDocumentElement();
		// Find Summary
		NodeList summaryTag = rootElement.getElementsByTagName("SUMMARY");
		
		String storyline = summaryTag.item(0).getTextContent()
				.replace("\\t", "\t")
				.replace("\\r","\r")
				.replace("\\n","\n")
				.replace("\\'", "'");

		storyline = AccidentConstructorUtil.transformWordNumIntoNum(storyline);
		
		Assert.assertEquals(4, AccidentConstructorUtil.getParagraphs(storyline).length);

	}
}
