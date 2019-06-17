package readingXML;

import java.io.File;
import java.io.FileInputStream;

import java.io.FileWriter;
import java.util.ArrayList;
import java.util.List;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import infrastructure.Response;

public class ReadXML {

	public File file = new File("D:\\QCRegressionUsingREST\\src\\test\\java\\infrastructure\\My.xml");

	public static void main(String[] args) throws Exception {
		ReadXML readXML = new ReadXML();
		// readXML.read_XML_Test_Ids();
		// readXML.read_XML_Test_Folder();
	}

	public void writeXMLUsingJava(Response response) throws Exception {
		File file = new File("D:\\QCRegressionUsingREST\\src\\test\\java\\infrastructure\\My.xml");
		FileWriter fw = new FileWriter(file);
		String xmlResponse = response.toString();
		fw.write(xmlResponse);
		fw.close();
	}

	public List<String> read_XML_Test_Ids(Response response, String expression) throws Exception {

		writeXMLUsingJava(response);

		DocumentBuilderFactory builderFactory = DocumentBuilderFactory.newInstance();

		DocumentBuilder builder = builderFactory.newDocumentBuilder();

		Document xmlDocument = builder.parse(file);

		XPath xPath = XPathFactory.newInstance().newXPath();

		// String expression = "//Entities/Entity/Fields/Field[@Name='test-id']";

		System.out.println(expression);

		List<String> values = new ArrayList<>();

		NodeList nodeList = (NodeList) xPath.compile(expression).evaluate(xmlDocument, XPathConstants.NODESET);
		System.out.println(nodeList.getLength());

		for (int i = 0; i < nodeList.getLength(); i++) {

			Node currentItem = nodeList.item(i);
			String key = currentItem.getTextContent();
			values.add(key);
		}

		System.out.println(values);

		return values;
	}

	public String read_XML_Test_Folder(Response response, String expression) throws Exception {
		writeXMLUsingJava(response);

		DocumentBuilderFactory builderFactory = DocumentBuilderFactory.newInstance();

		DocumentBuilder builder = builderFactory.newDocumentBuilder();

		Document xmlDocument = builder.parse(file);

		XPath xPath = XPathFactory.newInstance().newXPath();

		// String expression =
		// "//Entities/Entity/Fields/Field[@Name='hierarchical-path']";

		System.out.println(expression);

		String attribute = xPath.compile(expression).evaluate(xmlDocument);

		System.out.println(attribute);

		return attribute;
	}

	// public String read_XML_IDs_from_Test_Lab (Response response,String
	// expression) throws Exception
	public String read_single_attribute(Response response, String expression) throws Exception {
		writeXMLUsingJava(response);

		DocumentBuilderFactory builderFactory = DocumentBuilderFactory.newInstance();

		DocumentBuilder builder = builderFactory.newDocumentBuilder();

		Document xmlDocument = builder.parse(file);

		XPath xPath = XPathFactory.newInstance().newXPath();

		// String expression = "//Entity/Fields/Field[@Name='id']";

		System.out.println(expression);

		String folder_Id = xPath.compile(expression).evaluate(xmlDocument);

		System.out.println(folder_Id);

		return folder_Id;
	}

}
