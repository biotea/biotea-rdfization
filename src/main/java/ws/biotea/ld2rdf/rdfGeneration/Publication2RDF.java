package ws.biotea.ld2rdf.rdfGeneration;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.UnsupportedEncodingException;

import javax.xml.bind.JAXBException;

public interface Publication2RDF {	
	/**
	 * Converts a file into RDF
	 * @param outputDir
	 * @param paper
	 * @param sections
	 * @throws JAXBException 
	 * @throws UnsupportedEncodingException 
	 * @throws FileNotFoundException 
	 */
	public File paper2rdf(String outputDir, File paper, boolean sections, boolean references) throws JAXBException, FileNotFoundException, UnsupportedEncodingException;	
}
