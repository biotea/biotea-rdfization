package ws.biotea.ld2rdf.rdfGeneration;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.UnsupportedEncodingException;

import javax.xml.bind.JAXBException;

import org.jdom.JDOMException;

import ws.biotea.ld2rdf.util.ResourceConfig;

public interface RDFHandler {
	public static final String PUBMED_URL = ResourceConfig.getPubMedURL();//"http://www.ncbi.nlm.nih.gov/pubmed/";
	public static final String DOI_URL = ResourceConfig.getDOIURL();//"http://dx.doi.org/";			
	public static final String CHAR_NOT_ALLOWED = ResourceConfig.CHAR_NOT_ALLOWED;
	
	/**
	 * Creates an RDF file from an XML file.
	 * @param xml XML file
	 * @param outRDFFileName Out file absolute name.
	 * @return RDF file name
	 * @throws JAXBException 
	 * @throws UnsupportedEncodingException 
	 * @throws FileNotFoundException 
	 */
	public File createRDFFromXML(File xml, String outRDFFileName, boolean sections, boolean references) throws FileNotFoundException, UnsupportedEncodingException, JAXBException;
	/**
	 * Creates an RDF file from an XML given as string.
	 * @param xml XML string
	 * @param outRDFFileName Out file absolute name.
	 * @return RDF file name
	 * @throws JAXBException 
	 * @throws IOException 
	 * @throws JDOMException 
	 */
	public File createRDFFromXMLString(String xml, String outRDFFileName, boolean sections, boolean references) throws JAXBException, JDOMException, IOException;
	/**
	 * Creates an RDF file from an HTML file.
	 * @param html HTML file
	 * @param outRDFFileName Out file absolute name.
	 * @return RDF file name
	 */
	public File createRDFFromHTML(File html, String outRDFFileName, boolean sections, boolean references);
	/**
	 * Creates an RDF file from an HTML given as string.
	 * @param html HTML string
	 * @param outRDFFileName Out file absolute name.
	 * @return RDF file name
	 */
	public File createRDFFromHTMLString(String html, String outRDFFileName);
}