package edu.sv.cmu.datacollectiononline.util;

import java.io.File;
import java.io.IOException;
import java.io.StringWriter;

import android.provider.Settings.Secure;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.*;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import edu.sv.cmu.datacollectiononline.MainActivity;
import android.os.Environment;
public class XMLWriter {


	private DocumentBuilder dbuilder;
	private DocumentBuilderFactory dbFactory;
	private Document doc;

	public static final String rootElementName = "edu.sv.cmu.datacollectiononline.xml";
	public static final String deviceId = "deviceId";
	public static final String sensorRoot = "sensorRoot";
	public static final String timeStamp = "timeStamp";
	public static final String imageFrameId = "imageFrameId";
	public static final String promptId = "promptId";
	public static final String promptString = "promptString";

	//public static final String[] sensorNames = {"Accelerometer","GPS","Proximity","Magnometer","Orientation"};




	private static final String SDCARD_PATH = Environment.getExternalStorageDirectory().getAbsolutePath();
	private static final String XMLPATH = SDCARD_PATH+"/xmlFiles";

	public void initXMLFile(){
		try {
			dbFactory = DocumentBuilderFactory.newInstance();
			dbuilder = dbFactory.newDocumentBuilder();
			doc = dbuilder.newDocument();
			Element e_root = doc.createElement(rootElementName);
			doc.appendChild(e_root);

			Element e_deviceId = doc.createElement(deviceId);
			e_root.appendChild(e_deviceId);
			e_deviceId.setNodeValue("0");

			Element e_promptId = doc.createElement(promptId);
			e_root.appendChild(e_promptId);
			e_deviceId.setNodeValue("0");

			Element e_sensor = doc.createElement(sensorRoot);
			e_root.appendChild(e_sensor);

			/* populate sensor Elements */
			Element accelerometer = doc.createElement("ACCCELE");
			e_sensor.appendChild(accelerometer);
			accelerometer.setAttribute("x_acceleration","0");
			accelerometer.setAttribute("y_acceleration","0");
			accelerometer.setAttribute("z_acceleration","0");

			Element gps = doc.createElement("GPS");
			e_sensor.appendChild(gps);
			gps.setAttribute("longitude", "0");
			gps.setAttribute("latitude","0");

			Element orientation = doc.createElement("ORIENTATION");
			e_sensor.appendChild(orientation);
			orientation.setAttribute("azimuth", "0");
			orientation.setAttribute("pitch", "0");
			orientation.setAttribute("roll", "0");

			Element magnometer = doc.createElement("MAGNOMETER");
			e_sensor.appendChild(magnometer);

			/* Time Stamp */
			Element e_time = doc.createElement(timeStamp);
			e_root.appendChild(e_time);
			e_time.setAttribute("start_time","0");
			e_time.setAttribute("end_time","0");
			/* image Frame Id*/
			Element e_imgId = doc.createElement(imageFrameId);
			e_root.appendChild(e_imgId);


			System.out.println("Init XML created");


		} catch (ParserConfigurationException e) {
			e.printStackTrace();
		} 

	}

	public void populateXMLFile(ContextParams cp){
		try {
			/* init */
			dbFactory = DocumentBuilderFactory.newInstance();
			dbuilder = dbFactory.newDocumentBuilder();
			doc = dbuilder.newDocument();
			/* root node*/
			Element e_root = doc.createElement(rootElementName);
			doc.appendChild(e_root);
			/* device id*/
			Element e_deviceId = doc.createElement(deviceId);
			e_root.appendChild(e_deviceId);
			e_deviceId.setAttribute(deviceId,MainActivity.ANDROID_ID);
			/* prompt id + prompt String */
			Element e_promptId = doc.createElement(promptId);
			e_root.appendChild(e_promptId);
			e_deviceId.setAttribute(promptId, Integer.toString(cp.promptId));
			e_deviceId.setAttribute(promptString, cp.promptStr);
			
			
			/* Sensor Element*/
			Element e_sensor = doc.createElement(sensorRoot);
			e_root.appendChild(e_sensor);

			/* populate sensor Elements */
			Element accelerometer = doc.createElement(cp.ACCELEROMETER);
			e_sensor.appendChild(accelerometer);
			accelerometer.setAttribute("x_acceleration", Float.toString(cp.linear_acceleration[0]));
			accelerometer.setAttribute("y_acceleration",Float.toString(cp.linear_acceleration[1]));
			accelerometer.setAttribute("z_acceleration",Float.toString(cp.linear_acceleration[2]));

			/* gps */
			Element gps = doc.createElement(cp.GPS);
			e_sensor.appendChild(gps);
			gps.setAttribute("longitude", Double.toString(cp.longitude));
			gps.setAttribute("latitude",Double.toString(cp.latitude));

			/* orientation */
			Element orientation = doc.createElement(cp.ORIENTATION);
			e_sensor.appendChild(orientation);
			orientation.setAttribute("azimuth", Float.toString(cp.azimuth));
			orientation.setAttribute("pitch", Float.toString(cp.pitch));
			orientation.setAttribute("roll", Float.toString(cp.roll));

			Element magnometer = doc.createElement(cp.MAGNOMETER);
			e_sensor.appendChild(magnometer);

			/* Time Stamp */
			Element e_time = doc.createElement(timeStamp);
			e_root.appendChild(e_time);
			e_time.setAttribute("start_time",Long.toString(cp.start_time));
			e_time.setAttribute("end_time",Long.toString(cp.end_time));
			
			/* image Frame Id*/
			Element e_imgId = doc.createElement(imageFrameId);
			e_root.appendChild(e_imgId);

			//writeToXMLFile(XMLPATH + MainActivity.ANDROID_ID + System.currentTimeMillis()+".xml");
			System.out.println("XMLFile Created");


		} catch (ParserConfigurationException e) {
			e.printStackTrace();
		} 

	}
	
	/* write to XML formatted String */
	public String writeToXMLString(){
		String output = null;
		TransformerFactory tf;
		Transformer transformer;
		try {
			tf = TransformerFactory.newInstance();
			transformer = tf.newTransformer();
			transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
			StringWriter writer = new StringWriter();
			transformer.transform(new DOMSource(doc), new StreamResult(writer));
			output = writer.getBuffer().toString().replaceAll("\n|\r", "");

		} catch (TransformerConfigurationException e) {

			e.printStackTrace();
		} catch (TransformerException e) {

			e.printStackTrace();
		}
		return output;


	}
	
	/* 
	 * write to XML formatted file
	 * filename excludes SDCARD Path
	 */
	public void writeToXMLFile(String filename){

		/* checking suffix */
		filename = XMLPATH + filename;
		TransformerFactory transFactory = TransformerFactory.newInstance();
		Transformer transformer;
		try {
			transformer = transFactory.newTransformer();
			DOMSource source = new DOMSource(doc);
			StreamResult result = new StreamResult(filename);

			transformer.transform(source, result);

		} catch (TransformerConfigurationException e) {
			e.printStackTrace();
		} catch (TransformerException e) {
			e.printStackTrace();
		}

		System.out.println("write to file" + filename);
	}

	/* 
	 * modify an attribute
	 * @element_name
	 * @attr_name
	 * @value
	 */
	public void modifyElement(String element_name, String attr_name, String value){
		if(doc !=null){
			Node node = doc.getElementsByTagName(element_name).item(0);
			NamedNodeMap attr = node.getAttributes();
			attr.getNamedItem(attr_name).setNodeValue(value);

		}
	}

	/* Parse XML file*/
	public void readXML(String filename){
		if(filename.split(".") == null){
			filename = filename + ".xml";
		}
		File xmlFile = new File(filename);
		dbFactory = DocumentBuilderFactory.newInstance();
		try {
			dbuilder = dbFactory.newDocumentBuilder();
			doc = dbuilder.parse(xmlFile);

		} catch (ParserConfigurationException e) {
			e.printStackTrace();
		} catch (SAXException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}

		Node rootNode = doc.getDocumentElement();
		NodeList nList = rootNode.getChildNodes();
		for(int i=0;i<nList.getLength();i++){
			Node node = nList.item(i);
			System.out.println(node.getNodeName());

			if(node.getNodeType() == Node.ELEMENT_NODE){

				//Element element = (Element) node;
				NodeList list = node.getChildNodes();
				if(list!=null){

				}
				NamedNodeMap map = node.getAttributes();
				for(int j = 0;j<map.getLength();j++){
					System.out.println(map.item(j).getNodeName()+":"+ map.item(j).getNodeValue());
				}
			}
		}


	}


}
