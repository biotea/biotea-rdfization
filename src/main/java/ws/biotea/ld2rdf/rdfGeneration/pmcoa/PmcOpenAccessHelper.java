package ws.biotea.ld2rdf.rdfGeneration.pmcoa;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.UnsupportedEncodingException;

import javax.xml.bind.JAXBException;
import javax.xml.parsers.ParserConfigurationException;

import org.apache.log4j.Logger;
import org.xml.sax.SAXException;

import ws.biotea.ld2rdf.rdfGeneration.RDFBasicHandler;
import ws.biotea.ld2rdf.rdfGeneration.exception.ArticleTypeException;
import ws.biotea.ld2rdf.rdfGeneration.exception.DTDException;
import ws.biotea.ld2rdf.rdfGeneration.exception.PMCIdException;
import ws.biotea.ld2rdf.rdfGeneration.jats.GlobalArticleConfig;
import ws.biotea.ld2rdf.util.ResourceConfig;

public class PmcOpenAccessHelper {
	private RDFBasicHandler handler;
	Logger logger = Logger.getLogger(PmcOpenAccessHelper.class);	
	
	public PmcOpenAccessHelper(String base, String dataset) {
		this.handler = new RDFBasicHandler(dataset, ResourceConfig.getBioteaURL(base), 
				GlobalArticleConfig.getBaseURLAO(base));
	}
	/**
	 * RDFizes a file
	 * @param subdir File to rdfize
	 * @param outputDir Output dir for generated RDF
	 * @param sections Should sections be processed?
	 * @throws JAXBException
	 * @throws FileNotFoundException
	 * @throws UnsupportedEncodingException
	 * @throws ParserConfigurationException 
	 * @throws SAXException 
	 * @throws PMCIdException 
	 * @throws ArticleTypeException 
	 * @throws DTDException 
	 */
	public File rdfizeFile(File subdir, String outputDir, String bioteaBase, String bioteaDataset, boolean sections, 
		boolean references, String suffix, boolean useBio2RDF) 
		throws JAXBException, FileNotFoundException, UnsupportedEncodingException, SAXException, ParserConfigurationException, DTDException, ArticleTypeException, PMCIdException {
		logger.info("===RDFize " + subdir.getName() + "===");
		File outRDF = null; 
		//1. Create RDF used as a mechanism for improving information retrieval over tagged resources as well as to facilitate the discovery of shared conceptualizations[2,3].
		this.handler.setStrPmcId(new StringBuilder());
		if (useBio2RDF) {
			this.handler.setPaper2rdf(new PmcOpenAccess2MappedRDF(subdir, this.handler.getStrPmcId(), suffix, bioteaBase, bioteaDataset, sections, references));
		} else if (ResourceConfig.getMappingFile().length() != 0) {
			this.handler.setPaper2rdf(new PmcOpenAccess2MappedRDF(subdir, this.handler.getStrPmcId(), suffix, bioteaBase, bioteaDataset, sections, references));
		} else {
			this.handler.setPaper2rdf(new PmcOpenAccess2RDF(subdir, this.handler.getStrPmcId(), suffix, bioteaBase, bioteaDataset, sections, references));
		}
		String pmc = this.handler.getStrPmcId().toString();
		this.handler.setPaperURLId(GlobalArticleConfig.getArticleRdfUri(bioteaBase, pmc));
		this.handler.setDocumentPaperId(pmc);		
		outRDF = new File (outputDir + "/PMC" + pmc + suffix + ".rdf");
		if (!outRDF.exists()) {
			outRDF = this.handler.createRDFFromXML(subdir, outputDir);
		}	
		return outRDF;
	}
}
