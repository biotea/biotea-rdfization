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

public class PmcOpenAccessHelper {
	private RDFBasicHandler handler;
	Logger logger = Logger.getLogger(PmcOpenAccessHelper.class);	
	
	public PmcOpenAccessHelper() {
		this.handler = new RDFBasicHandler(GlobalArticleConfig.BIOTEA_PMC_DATASET, GlobalArticleConfig.BASE_URL, GlobalArticleConfig.BASE_URL_AO);
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
	public File rdfizeFile(File subdir, String outputDir, boolean sections, boolean references) throws JAXBException, FileNotFoundException, UnsupportedEncodingException, SAXException, ParserConfigurationException, DTDException, ArticleTypeException, PMCIdException {
		logger.info("===RDFize " + subdir.getName() + "===");
		File outRDF = null; 
		//1. Create RDF used as a mechanism for improving information retrieval over tagged resources as well as to facilitate the discovery of shared conceptualizations[2,3].
		this.handler.setStrPmcId(new StringBuilder());
		this.handler.setPaper2rdf(new PmcOpenAccess2RDF(subdir, this.handler.getStrPmcId()));
		String pmc = this.handler.getStrPmcId().toString();
		this.handler.setPaperURLId(GlobalArticleConfig.getArticleRdfUri(pmc));
		this.handler.setDocumentPaperId(pmc);
		outRDF = new File (outputDir + "/PMC" + pmc + ".rdf");
		if (!outRDF.exists()) {
			outRDF = this.handler.createRDFFromXML(subdir, outputDir, sections, references);
		}	
		return outRDF;
	}
}
