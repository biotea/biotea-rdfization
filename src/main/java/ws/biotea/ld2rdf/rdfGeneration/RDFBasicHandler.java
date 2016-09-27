package ws.biotea.ld2rdf.rdfGeneration;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.UnsupportedEncodingException;

import javax.xml.bind.JAXBException;

import org.apache.log4j.Logger;
import org.jdom.JDOMException;


public class RDFBasicHandler extends RDFAbstractHandler {
	protected Logger logger = Logger.getLogger(RDFBasicHandler.class);
	private StringBuilder strPmcId;
	private Publication2RDF paper2rdf;
	
	public RDFBasicHandler(String datasetURL, String baseURL, String baseURLAnnotations) {
		logger = Logger.getLogger("PubMedOpenAccessRDFHandler");
		this.DATASET_URL = datasetURL;
		this.BASE_PAPER_URL = baseURL;
		this.strPmcId = new StringBuilder();
	}	
	/* (non-Javadoc)
	 * @see ld2rdf.rdfGeneration.RDFHandler#createRDFFromXML(java.io.File, java.lang.String, boolean)
	 */
	public File createRDFFromXML(File xml, String outRDFFileName, boolean sections, boolean references) throws FileNotFoundException, UnsupportedEncodingException, JAXBException {		
		return this.paper2rdf.paper2rdf(outRDFFileName, xml, sections, references);
	}

	/* (non-Javadoc)
	 * @see ld2rdf.rdfGeneration.RDFHandler#createRDFFromXMLString(java.lang.String, java.lang.String, boolean)
	 */
	public File createRDFFromXMLString(String xml, String outRDFFileName,
			boolean section, boolean references) throws JAXBException, JDOMException, IOException {
		// TODO Auto-generated method stub
		return null;
	}

	/* (non-Javadoc)
	 * @see ld2rdf.rdfGeneration.RDFHandler#createRDFFromHTML(java.io.File, java.lang.String, boolean)
	 */
	public File createRDFFromHTML(File html, String outRDFFileName,
			boolean sections, boolean references) {
		// TODO Auto-generated method stub
		return null;
	}

	/* (non-Javadoc)
	 * @see ld2rdf.rdfGeneration.RDFHandler#createRDFFromHTMLString(java.lang.String, java.lang.String)
	 */
	public File createRDFFromHTMLString(String html, String outRDFFileName) {
		// TODO Auto-generated method stub
		return null;
	}
	
	/**
	 * @return the logger
	 */
	public Logger getLogger() {
		return logger;
	}
	/**
	 * @return the strPmcId
	 */
	public StringBuilder getStrPmcId() {
		return strPmcId;
	}
	/**
	 * @param strPmcId the strPmcId to set
	 */
	public void setStrPmcId(StringBuilder strPmcId) {
		this.strPmcId = strPmcId;
	}
	/**
	 * @return the paper2rdf
	 */
	public Publication2RDF getPaper2rdf() {
		return paper2rdf;
	}
	/**
	 * @param paper2rdf the paper2rdf to set
	 */
	public void setPaper2rdf(Publication2RDF paper2rdf) {
		this.paper2rdf = paper2rdf;
	}	
}
