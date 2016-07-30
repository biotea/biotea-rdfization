package ws.biotea.ld2rdf.rdfGeneration;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.UnsupportedEncodingException;

import javax.xml.bind.JAXBException;

public interface Publication2RDF {
	public final static String ISO_ENCODING = "ISO-8859-1";
	public final static String UTF_ENCODING = "UTF-8";
	/**
	 * Converts a file into RDF
	 * @param outputDir
	 * @param paper
	 * @param sections
	 * @throws JAXBException 
	 * @throws UnsupportedEncodingException 
	 * @throws FileNotFoundException 
	 */
	public File paper2rdf(String outputDir, File paper, boolean sections) throws JAXBException, FileNotFoundException, UnsupportedEncodingException;	
}
