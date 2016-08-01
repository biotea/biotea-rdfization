/**
 * 
 */
package ws.biotea.ld2rdf.rdfGeneration.pmcoa;  

import org.apache.log4j.Logger;
import org.ontoware.rdf2go.RDF2Go;
import org.ontoware.rdf2go.exception.ModelRuntimeException;
import org.ontoware.rdf2go.model.Model;
import org.ontoware.rdf2go.model.node.Node;
import org.ontoware.rdf2go.model.node.PlainLiteral;
import org.ontoware.rdf2go.model.node.Resource;
import org.ontoware.rdf2go.model.node.impl.PlainLiteralImpl;
import org.ontoware.rdf2go.model.node.impl.URIImpl;
import pubmed.openAccess.jaxb.generated.*;
import pubmed.openAccess.jaxb.generated.Article;
import pubmed.openAccess.jaxb.generated.Issue;
import ws.biotea.ld2rdf.rdf.model.bibo.*;
import ws.biotea.ld2rdf.rdf.model.bibo.Conference;
import ws.biotea.ld2rdf.rdf.model.bibo.extension.*;
import ws.biotea.ld2rdf.rdf.model.doco.Appendix;
import ws.biotea.ld2rdf.rdf.model.doco.DocoTable;
import ws.biotea.ld2rdf.rdf.model.doco.Figure;
import ws.biotea.ld2rdf.rdf.model.doco.extension.ParagraphE;
import ws.biotea.ld2rdf.rdf.model.doco.extension.SectionE;
import ws.biotea.ld2rdf.rdfGeneration.Publication2RDF;
import ws.biotea.ld2rdf.rdfGeneration.RDFHandler;
import ws.biotea.ld2rdf.rdfGeneration.exception.ArticleTypeException;
import ws.biotea.ld2rdf.rdfGeneration.exception.DTDException;
import ws.biotea.ld2rdf.rdfGeneration.exception.PMCIdException;
import ws.biotea.ld2rdf.rdfGeneration.jats.GlobalArticleConfig;
import ws.biotea.ld2rdf.util.ResourceConfig;
import ws.biotea.ld2rdf.util.Conversion;
import ws.biotea.ld2rdf.util.HtmlUtil;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class PmcOpenAccess2RDF implements Publication2RDF {
	private static Logger logger = Logger.getLogger(PmcOpenAccess2RDF.class);	
	private int elementCount = 1;
	//Basic paper
	private String basePaper;
	private String pubmedID;
	private String pmcID;
	private String articleType;
	private String doi;	
	private Article article;
	private GlobalArticleConfig global;
	private final String PREFIX = ResourceConfig.getDatasetPrefix().toUpperCase();
	//private static int count = 1;
	//Sections
	
//	private Document documentSections;
	
	public PmcOpenAccess2RDF(File paper, StringBuilder str) throws JAXBException, DTDException, ArticleTypeException, PMCIdException {
		JAXBContext jc = JAXBContext.newInstance("pubmed.openAccess.jaxb.generated");
		Unmarshaller unmarshaller = jc.createUnmarshaller(); 	
		try {
			article = (Article) unmarshaller.unmarshal(paper);
		} catch (Exception e) {
			logger.fatal("- FATAL DTD ERROR - " + paper.getName() + " cannot be unmarshalled: " + e.getMessage());
			throw new DTDException (e);
		}
		this.articleType = "";
		try {
			this.articleType = article.getArticleType();			
		} catch (Exception e) {
			throw new ArticleTypeException ("Article type is undefined: " + e.getMessage());
		}
		if (this.articleType == null) {
			throw new ArticleTypeException ("Article type is null, file cannot be processed");
		}		
		//url
		pubmedID = null; doi = null;
		for(pubmed.openAccess.jaxb.generated.ArticleId id: article.getFront().getArticleMeta().getArticleIds()) {
			if (id.getPubIdType().equals("pmid")) {
				pubmedID = id.getContent();
			} else if (id.getPubIdType().equals("doi")) {
				doi = id.getContent();
			} else if (id.getPubIdType().equals("pmc")) {
				pmcID = id.getContent();
			}				
		}
		
		if ((pmcID == null) && (ResourceConfig.getIdTag().equals("pmc"))) {
			throw new PMCIdException("No " + PREFIX + " id was found, file cannot be processed");
		} 
		if ((pubmedID == null) && (ResourceConfig.getIdTag().equals("pmid"))) {
			throw new PMCIdException("No " + PREFIX + " id was found, file cannot be processed");
		}
		
		str.delete(0, str.length());
		if (ResourceConfig.getIdTag().equals("pmc")) {
			str.append(pmcID); //file name
		} else if (ResourceConfig.getIdTag().equals("pmid")) { //TODO try it out!
			str.append(pubmedID); //file name
		} else { //TODO how to support others?
			throw new PMCIdException("No valid " + PREFIX + " field " + ResourceConfig.getIdTag() + 
				" was configured, file cannot be processed");
		}
		
		logger.info("=== ARTICLE-TYPE (" + paper.getName() + " - " + pmcID + "): " + this.articleType);
		this.global = new GlobalArticleConfig(pmcID);	
		this.basePaper = GlobalArticleConfig.getArticleRdfUri(pmcID);	
	}
	
	/**
	 * Converts a file into RDF
	 * @param outputDir
	 * @param paper
	 * @param sections
	 * @throws JAXBException 
	 * @throws UnsupportedEncodingException 
	 * @throws FileNotFoundException 
	 */
	public File paper2rdf(String outputDir, File paper, boolean sections) throws JAXBException, FileNotFoundException, UnsupportedEncodingException {	
		logger.info("=== INIT Rdfization of " + paper.getName());		
				
		if (pmcID == null) {
			throw new NullPointerException("No pmc id was found, file cannot be processed");
		}		
		
		// getting model
		Model model;			
		Document document;		
		File outputFile = null;
		model = createAndOpenModel();
		boolean fatalError = false;
		//create document
		try {
			document = new Document(model, basePaper, true);	
			//logger.info("FLAG 2: " + this.pmcID + " - " + this.basePaper);
			String type = this.articleType.replace('-', '_').toUpperCase();
			if (ArticleType.valueOf(type).getBiboType() != null) {
			    model.addStatement(document.asResource(), Thing.RDF_TYPE, ArticleType.valueOf(type).getBiboTypeURI()); //rdf:type
			}
			document.addDCDescription(model, this.articleType);
			
			//TODO Read an existing document (do we really need to do this? Not so far...)
			//model.readFrom(new FileReader(file));
		    
			this.processBasic(model, document, paper.getName());
			logger.info("=== basic processed");
			this.processAuthors(model, document, paper.getName());
			logger.info("=== authors processed");
			this.processAbstractAndKeywords(model, document);
			logger.info("=== abstract and keywords processed");
			this.processReferences(model, document);
			logger.info("=== references processed");
		} catch (Exception e) {//something went so wrong
			logger.fatal("- FATAL ERROR - " + pmcID + " threw an uncaugth error: " + e.getMessage());
			fatalError = true;
		} finally {
			//close and write model
			if (fatalError) {
				//outputFile = serializeAndCloseModel(model, outputDir + "/" + PREFIX + pmcID + ".rdf");		
				logger.info("=== END of rdfization with a fatal error " + pmcID + " (pubmedID: " + pubmedID + "), (doi: " + doi + ")");
			} else {
				outputFile = serializeAndCloseModel(model, outputDir + "/" + PREFIX + pmcID + ".rdf");		
				logger.info("=== END of rdfization OK " + pmcID + " (pubmedID: " + pubmedID + "), (doi: " + doi + ")");
			}
			
		}
		
		//process sections		
		if (sections) {
			Document documentSections;
			Model modelSections;
			File outputFileSections = null;
			boolean fatalErrorSections = false;
			modelSections = createAndOpenModel();
			try {
				documentSections = new Document(modelSections, basePaper, true);
				String docAbstract = "";
				for (Abstract ab: article.getFront().getArticleMeta().getAbstracts()) {
					docAbstract += processAbstractAsSection(ab, modelSections, documentSections, null);			
				}
				//process not-in-section-paragraphs
				SectionE secDoco = new SectionE(modelSections, global.BASE_URL_SECTION + "undefined-section", true);
				Iterator<Object> itrPara = article.getBody().getAddressesAndAlternativesAndArraies().iterator();			
				if (processElementsInSection(modelSections, documentSections, "undefined-section", secDoco, itrPara)) {
					documentSections.addSection(modelSections, secDoco);
				}								
				//process sections
				for (Sec section:article.getBody().getSecs()) {										
					processSection(section, modelSections, documentSections, null, null);
				}			
				logger.info("=== sections processed");
			} catch (Exception e) {//something went so wrong
				logger.fatal("- FATAL ERROR SECTIONS - " + pmcID + " threw an uncaugth error: " + e.getMessage());
				fatalErrorSections = true;
			} finally {
				if (fatalError || fatalErrorSections) {
					//close and write model
					//outputFileSections = serializeAndCloseModel(modelSections, outputDir + "/" + PREFIX + pmcID + "_sections.rdf");		
					logger.info("=== END of sections rdfization with a fatal error " + pmcID + " (pubmedID: " + pubmedID + "), (doi: " + doi + ")");
				} else {
					//close and write model
					outputFileSections = serializeAndCloseModel(modelSections, outputDir + "/" + PREFIX + pmcID + "_sections.rdf");		
					logger.info("=== END of sections rdfization OK " + pmcID + " (pubmedID: " + pubmedID + "), (doi: " + doi + ")");
				}
			}
			return outputFileSections;
		}
		return outputFile;
		//Supplementary material part of sections as well, not supported yet
		/*for (Object obj: article.getBack().getAcksAndAppGroupsAndBios()) {
			if (obj instanceof FnGroup) {
				FnGroup fn = (FnGroup)obj;
				for (Object o: fn.getFnsAndXS()) {
					if (o instanceof Fn) {
						Fn foot = (Fn)o;
						for (P p: foot.getPS()) {
							for (Object fnObj: p.getContent()) {
								if (fnObj instanceof ExtLink)
								try {
									String str = ((ExtLink)fnObj).getContent().get(0).toString();
									if (str.startsWith("http")) {
										document.addDCReferences(model, str);
									}
								} catch (Exception e) {;}	
							}
						}
					}
				}
			} else if (obj instanceof Sec) {
				processSection((Sec)obj, model, document, null, null);
			} else if (obj instanceof AppGroup) {
				AppGroup appG = (AppGroup)obj;
				for (Object o: appG.getAppsAndRefLists()) {
					if (o instanceof App) {
						App app = (App)o;
						String title = processListOfElements(app.getTitle().getContent(), null, null);
						String titleInURL = title.replaceAll(RDFHandler.CHAR_NOT_ALLOWED, "-");
						if (titleInURL.length() == 0) {
							title = app.getId();
							titleInURL = title.replaceAll(RDFHandler.CHAR_NOT_ALLOWED, "-");
						}
						Appendix appendix = new Appendix(model, BASE_URL_APPENDIX + titleInURL, true);
						document.addAppendix(model, appendix);
						//title
						Node titleNode = new PlainLiteralImpl(title);
						appendix.adddocoTitle(titleNode);
						for (Sec sec: app.getSecs()) {
							processSection(sec, model, document, appendix, titleInURL);
						}
					}
				}															
			}
		}*/
	}
	/**
	 * Creates and open an RDF model.
	 * @return
	 */
	private Model createAndOpenModel() {
		Model myModel = RDF2Go.getModelFactory().createModel();
		myModel.open();					
		myModel.setNamespace(ws.biotea.ld2rdf.util.OntologyPrefix.OWL.getNS(), ws.biotea.ld2rdf.util.OntologyPrefix.OWL.getURL());
		myModel.setNamespace(ws.biotea.ld2rdf.util.OntologyPrefix.RDF.getNS(), ws.biotea.ld2rdf.util.OntologyPrefix.RDF.getURL());
		myModel.setNamespace(ws.biotea.ld2rdf.util.OntologyPrefix.RDFS.getNS(), ws.biotea.ld2rdf.util.OntologyPrefix.RDFS.getURL());
		myModel.setNamespace(ws.biotea.rdfization.util.OntologyRDFizationPrefix.BIBO.getNS(), ws.biotea.rdfization.util.OntologyRDFizationPrefix.BIBO.getURL());
		myModel.setNamespace(ws.biotea.rdfization.util.OntologyRDFizationPrefix.DOCO.getNS(), ws.biotea.rdfization.util.OntologyRDFizationPrefix.DOCO.getURL());
		myModel.setNamespace(ws.biotea.ld2rdf.util.OntologyPrefix.FOAF.getNS(), ws.biotea.ld2rdf.util.OntologyPrefix.FOAF.getURL());
		myModel.setNamespace(ws.biotea.ld2rdf.util.OntologyPrefix.DCTERMS.getNS(), ws.biotea.ld2rdf.util.OntologyPrefix.DCTERMS.getURL());
		myModel.setNamespace(ws.biotea.ld2rdf.util.OntologyPrefix.FOAF.getNS(), ws.biotea.ld2rdf.util.OntologyPrefix.FOAF.getURL());		
		myModel.setNamespace(ws.biotea.ld2rdf.util.OntologyPrefix.XSP.getNS(), ws.biotea.ld2rdf.util.OntologyPrefix.XSP.getURL());		
		myModel.setNamespace(ws.biotea.ld2rdf.util.OntologyPrefix.RDF.getNS(), ws.biotea.ld2rdf.util.OntologyPrefix.RDF.getURL());
		myModel.setNamespace(ws.biotea.ld2rdf.util.OntologyPrefix.PROV.getNS(), ws.biotea.ld2rdf.util.OntologyPrefix.PROV.getURL());
		myModel.setNamespace(ws.biotea.ld2rdf.util.OntologyPrefix.VOID.getNS(), ws.biotea.ld2rdf.util.OntologyPrefix.VOID.getURL());
		return (myModel);
	}
	/**
	 * Closes and serializes an RDF model with UTF-8 enconding.
	 * @param myModel
	 * @param outputFile
	 * @throws FileNotFoundException
	 * @throws UnsupportedEncodingException
	 */
	private File serializeAndCloseModel(Model myModel, String outputFile) throws FileNotFoundException, UnsupportedEncodingException {
		//Writer writer = new OutputStreamWriter(new FileOutputStream(new File("./ontology/" + doi + ".rdf")), Charset.forName("ISO-8859-1"));
	    
	    File file = new File(outputFile);
	    PrintWriter pw = new PrintWriter(file, Publication2RDF.UTF_ENCODING);
	    try {
			myModel.writeTo(pw);
		} catch (ModelRuntimeException e) {
			logger.error("===ERROR=== model serialization (ModelRuntimeException): " + e.getMessage());
			throw new FileNotFoundException("ModelRuntimeException " + e.getMessage());
		} catch (IOException e) {
			logger.error("===ERROR=== model serialization (IOException): " + e.getMessage());
			throw new FileNotFoundException("IOException " + e.getMessage());
		}
	    //model.writeTo(writer);
	    myModel.close();
	    //serialization
//      String serialization = myModel.serialize(Syntax.RdfXml);
//      serialization = serialization.replaceFirst(Publication2RDF.UTF_ENCODING, Publication2RDF.ISO_ENCODING);	
//	    File file = new File(outputFile); 
//	    PrintWriter pw = new PrintWriter(file, Publication2RDF.ISO_ENCODING);
//		pw.write(serialization);
		pw.close();
		return file;
	}
	
	/**
	 * Processes doi, pubmed, journal, and title.
	 */
	private void processBasic(Model model, Document document, String paper) {
		document.addIdentifier(model, "pmc:" + pmcID);	
		//document.addSource(model, GlobalArticleConfig.pmcURI + pmcID);
		document.addProvWasDerivedFrom(model, GlobalArticleConfig.pmcURI + pmcID);
		//document.addIsFormatedVersionOf(model, GlobalArticleConfig.pmcURI + pmcID);
		String now = HtmlUtil.getDateAndTime();
		document.addDCCreated(model, now);
		document.addProvGeneratedAt(model, now);
		document.addDCCreator(model, GlobalArticleConfig.RDF4PMC_AGENT);
		document.addProvWasAttributedTo(model, GlobalArticleConfig.RDF4PMC_AGENT);
		document.addInDataset(model, GlobalArticleConfig.BIOTEA_PMC_DATASET);
		Resource resPMC = new URIImpl(GlobalArticleConfig.pmcURI + pmcID, true);	
		document.addSeeAlso(resPMC);	
		
		//pubmedID
		if (pubmedID != null) {
			document.addPMID(model, pubmedID);			
		    document.addIdentifier(model, "pmid:" + pubmedID);		
		    //Relations between PMC-RDF and identifiers.org/bio2rdf.org sameAS
		    if (ResourceConfig.withBio()) {
		    	document.addSameAs(model, ResourceConfig.IDENTIFIERS_ORG_PUBMED + pubmedID);
			    document.addSameAs(model, ResourceConfig.BIO2RDF_PUBMED + pubmedID);
		    }		    
		    if (!global.getUriStyle().equals(ResourceConfig.bio2rdf)) {
		    	document.addSameAs(model, GlobalArticleConfig.PUBMED_DOCUMENT + pubmedID);
		    }		    		
		    //relations between PMC-RDF and PubMed
		    Resource resPubMed = new URIImpl(GlobalArticleConfig.pubMedURI + pubmedID, true);	
		    document.addSeeAlso(resPubMed);		    
		}
		
		//doi
		if (doi != null) {
		    document.addDOI(model, doi);
		    document.addIdentifier(model, "doi:" + doi);
		    //relations between PMC-RDF and DOI
//		    Resource resDOI = new URIImpl(global.doiURI + doi, true);
//		    document.addSeeAlso(resDOI);
		    document.addSameAs(model, GlobalArticleConfig.doiURI + doi);
		    if (!global.getUriStyle().equals(ResourceConfig.bio2rdf)) {
		    	document.addSameAs(model, GlobalArticleConfig.DOI_DOCUMENT + doi);
		    }
		}
		
		//license
		try {
			String license = article.getFront().getArticleMeta().getPermissions().getLicenses().get(0).getHref();
			document.addDCLicense(model, license);
		} catch (Exception e) {}		
	    
	    //Journal
		JournalE journal = null;
		String journalTitle = null;
		String journalISSN = null;
		String journalTitleInURI = "";				
		try {  
			try { //title
				journalTitle = article.getFront().getJournalMeta().getJournalTitles().get(0);
			} catch (Exception e) {
				try {
					journalTitle = "";
					for (Object ser: article.getFront().getJournalMeta().getJournalTitleGroups().get(0).getJournalTitles().get(0).getContent()) {
						if (ser instanceof String) {
							journalTitle += ser.toString();
						} else if (ser instanceof JAXBElement<?>) {
							JAXBElement<?> elem = (JAXBElement<?>)ser;
							journalTitle += processElement(model, document, elem, null,null);
						}			
					}
				} catch (Exception e1) {}
			} 	
			//issn
			journalISSN = article.getFront().getJournalMeta().getIssns().get(0).getContent().get(0).toString();
			journalTitleInURI = journalISSN.replaceAll(RDFHandler.CHAR_NOT_ALLOWED, "-");
			//journal creation
			if ((journalISSN != null)  && (journalISSN.length() != 0)) {//journal by issn
				journal = new JournalE(model, GlobalArticleConfig.BASE_URL_JOURNAL_ISSN + journalTitleInURI, true); //model.createBlankNode(pmcID + "_journal_" + journalTitleInURI)
				if ((journalTitle != null) && (journalTitle.length() != 0)) {
					journal.addTitle(model, journalTitle);
				}
				for (Issn issn: article.getFront().getJournalMeta().getIssns()) {
		    		journal.addISSN(model, issn.getContent().get(0).toString());
			    }
				Resource resISSN = new URIImpl(GlobalArticleConfig.NLM_JOURNAL_CATALOG + journalISSN, true);
				journal.addSeeAlso(resISSN); //link to NLM catalog
			} else if ((journalTitle != null) && (journalTitle.length() != 0)) { //journal by name
				journalTitleInURI = journalTitle.replaceAll(RDFHandler.CHAR_NOT_ALLOWED, "-");
				journal = new JournalE(model, GlobalArticleConfig.BASE_URL_JOURNAL_NAME + journalTitleInURI, true); //model.createBlankNode(pmcID + "_journal_" + journalTitleInURI)
			    journal.addTitle(model, journalTitle);	
			} else {// no journal
				logger.error(paper + ": Journal title or ISSN both empty.");
			}						    		 
		} catch (Exception e) { 
			logger.error(paper + ": Journal title or ISSN not processed.");
		}	
		
		//Journal issue
    	try {
    		String issueNumber = article.getFront().getArticleMeta().getIssue().getContent().get(0).toString();
    		String issueNumberInURI = issueNumber.replaceAll(RDFHandler.CHAR_NOT_ALLOWED, "-");
    		if (journal != null) {
    			ws.biotea.ld2rdf.rdf.model.bibo.Issue issue = null;
    			if ((issueNumberInURI != null)  && (issueNumberInURI.length() != 0)) {    			        			
        			if ((journalISSN != null)  && (journalISSN.length() != 0)) {//journal by issn then issn:<id>/<issue_id>
        				String[] params = {"issn", journalTitleInURI};
        				String issueURI = Conversion.replaceParameter(GlobalArticleConfig.BASE_URL_ISSUE, params);
            			issue = new ws.biotea.ld2rdf.rdf.model.bibo.Issue(model, issueURI + issueNumberInURI, true);    		
                		//document.addIssue(model, issue);
                		issue.addbiboIssue(issueNumber);
            		} else { //name:<journal_name>/<issue_id>
            			String[] params = {"name", journalTitleInURI};
        				String issueURI = Conversion.replaceParameter(GlobalArticleConfig.BASE_URL_ISSUE, params);
            			issue = new ws.biotea.ld2rdf.rdf.model.bibo.Issue(model, issueURI + issueNumberInURI, true);    		
                		//document.addIssue(model, issue);
        			}
        			journal.addIssue(model, issue);
        			issue.addDocument(model, document);
            		//issue.addJournal(model, journal);
        		} else {
        			/*if ((journalISSN != null)  && (journalISSN.length() != 0)) {//journal by issn then issn:<id>/<issue_id>
        				String[] params = {"issn", journalTitleInURI};
        				String issueURI = Conversion.replaceParameter(this.BASE_URL_ISSUE, params);
            			issue = new ws.biotea.ld2rdf.rdf.model.bibo.Issue(model, issueURI + (elementCount++), true);    		
                		//document.addIssue(model, issue);
                		issue.addbiboIssue(issueNumber);
            		} else { //name:<journal_name>/<issue_id>
            			String[] params = {"name", journalTitleInURI};
        				String issueURI = Conversion.replaceParameter(this.BASE_URL_ISSUE, params);
            			issue = new ws.biotea.ld2rdf.rdf.model.bibo.Issue(model, issueURI + (elementCount++), true);    		
                		//document.addIssue(model, issue);
        			}*/
            		//issue.addJournal(model, journal);
        			journal.addDocument(model, document);
        		} 
    		}
    	} catch (Exception e) {	
    		journal.addDocument(model, document);
    	}
    	
		//Publisher
		try {
			String publisherName = article.getFront().getJournalMeta().getPublisher().getPublisherName().getContent().get(0).toString();
			String publisherPMCId = null;
			try {
			 
				for (ArticleId id:article.getFront().getArticleMeta().getArticleIds()) {
					if (id.getPubIdType().equals("publisher-id")) {
						publisherPMCId = id.getContent();
					}
				}
			} catch (Exception eId) {}
			OrganizationE publisher;
			if (publisherPMCId != null) {//we create the publisher with the id
				publisher = new OrganizationE(model, GlobalArticleConfig.BASE_URL_PUBLISHER_ID + publisherPMCId, true ); 
				publisher.addName(model, publisherName);
				PlainLiteral id = model.createPlainLiteral(publisherPMCId);					    
			    model.addStatement(publisher.asResource(), Document.DCTERMS_IDENTIFIER, id); //id
			} else {//we create the publisher with the name
				publisher = new OrganizationE(model, GlobalArticleConfig.BASE_URL_PUBLISHER_NAME + publisherName.replaceAll(RDFHandler.CHAR_NOT_ALLOWED, "-"), true ); 
				publisher.addName(model, publisherName);							
			}
			journal.addbiboPublisher(publisher);
		    document.addbiboPublisher(publisher); 
	    } catch (Exception e) {}
	    
	    //publication dates
		try {
	    	String date = null;
	    	//String month = null;
	    	for (PubDate pubDate: article.getFront().getArticleMeta().getPubDates()) {
	    		if (pubDate.getPubType().equals("epub")) {
	    			for (Object obj: pubDate.getDaiesAndMonthsAndYears()) {
	    	    		if (obj instanceof Year) {
	    	    			date = ((Year)obj).getContent();
	    	    		} /*else if (obj instanceof Month) {
	    	    			month = ((Month)obj).getContent();
	    	    		}*/
	    	    	}
	    		}
	    	}
	    	if (date != null) {
	    		/*if (month != null) {
	    			Node dateNode = new PlainLiteralImpl(date + " " + month);
	            	document.addbiboIssued(dateNode);
	    		} else {*/
	    			Node dateNode = new PlainLiteralImpl(date);
	            	document.addbiboIssued(dateNode);
	    		//}    		
	    	}    
		} catch (Exception e) {
			logger.info(paper + ": Date or month not processed");
		}
    	try {
    		String volume = article.getFront().getArticleMeta().getVolume().getContent().get(0).toString();
    		document.addbiboVolume(volume);
    	} catch (Exception e) {
    		logger.info(paper + ": Volumen not processed");
    	}    	
		try {
			String pageStart = article.getFront().getArticleMeta().getFpage().getContent();
			document.addbiboPagestart(pageStart);
		} catch (Exception e) {
			logger.info(paper + ": Start page not processed");
		}
    	try {
    		String pageEnd = article.getFront().getArticleMeta().getLpage().getContent();
    		document.addbiboPageend(pageEnd);
    	} catch (Exception e) {
    		logger.info(paper + ": End page not processed");
    	}    
    	
    	//Title
    	try {    		
    	    String title = "";
    		for (Object ser: article.getFront().getArticleMeta().getTitleGroup().getArticleTitle().getContent()) {
    			if (ser instanceof String) {
    				title += ser.toString();
    			} else if (ser instanceof JAXBElement<?>) {
    				JAXBElement<?> elem = (JAXBElement<?>)ser;
    				title += processElement(model, document, elem, null, null);
    			}			
    		}
    	    document.addTitle(model, title);
    	} catch (Exception e) {
    		logger.info(paper + ": Article title not processed");
    	}	
	}	
	/**
	 * Processes authors.
	 */
	private void processAuthors(Model model, Document document, String paper) {
		try {
			//List of Authors
		    ArrayList<Agent> listOfAuthors = new ArrayList<Agent>();
			for (Object obj: article.getFront().getArticleMeta().getContribGroupsAndAfvesAndXS()) {
				if (obj instanceof ContribGroup) {
					ContribGroup group = (ContribGroup)obj;
					for (Object objGr: group.getContribsAndAddressesAndAfves()) {
						if (objGr instanceof Contrib) {
							Contrib contrib = (Contrib)objGr;
							if (contrib.getContribType().equals("author")) {
								//Author
								PersonE person = null;
								for (Object objAut: contrib.getAnonymousesAndCollabsAndNames()) {
									if (objAut instanceof Name) {
										Name name = (Name)objAut;
										String givenNames = "";
										try {
											givenNames = name.getGivenNames().getContent().get(0).toString();
										} catch (Exception e) {}
										String surname = name.getSurname().getContent().get(0).toString();
										String idPerson = givenNames + surname;
										idPerson = idPerson.replaceAll(RDFHandler.CHAR_NOT_ALLOWED, "-");
									    person = new PersonE(model, global.BASE_URL_PERSON_PMC + idPerson, true); //model.createBlankNode(pmcID + "_author_" + idPerson)
									    person.addName(model, givenNames + " " + surname);
									    person.addGivenName(model, givenNames);
									    person.addFamilyName(model, surname);
									    person.addPublications(model, this.basePaper);
									} else if (objAut instanceof Email) {
										try {
											Email email = (Email)objAut;
											String str = email.getContent().get(0).toString();
									    	person.addMBox(model, str);
									    } catch (Exception e) {}
									} else if (objAut instanceof Address) {
										Address address = (Address) objAut;
										for (Object detailAddress : address.getAddrLinesAndCountriesAndFaxes()) {
											if (detailAddress instanceof Email) {
												try {
													Email email = (Email) detailAddress;
													String str = email.getContent().get(0).toString();
											    	person.addMBox(model, str);
											    } catch (Exception e) {}
											}
										}
									} else if (objAut instanceof Xref) {
										/*Xref xref = (Xref)objAut;
										if (xref.getRefType().equals("aff")) {
											if (person != null) {
												processAffiliation(xref, person); 
											}
										}*/
									} else if (objAut instanceof Collab) {
										Collab collab = (Collab)obj;
										String orgName = collab.getContent().get(0).toString();
										String idOrg = orgName.replaceAll(RDFHandler.CHAR_NOT_ALLOWED, "-");										
										OrganizationE org = new OrganizationE(model, global.BASE_URL_ORGANIZATION_PMC + idOrg, true ); //model.createBlankNode(pmcID + "_author_" + idOrg) 
										listOfAuthors.add(org);
									}
								}
								if (person != null) {
									listOfAuthors.add(person);
								}							
							} else if (contrib.getContribType().equals("collab")) {
								//Collaborator
								OrganizationE org = null;
								for (Object objAut: contrib.getAnonymousesAndCollabsAndNames()) {
									if (objAut instanceof Collab) {
										Collab collab = (Collab)obj;
										String orgName = collab.getContent().get(0).toString();
										String idOrg = orgName.replaceAll(RDFHandler.CHAR_NOT_ALLOWED, "-");
										org = new OrganizationE(model, global.BASE_URL_ORGANIZATION_PMC + idOrg, true ); //model.createBlankNode(pmcID + "_author_" + idOrg) 
										listOfAuthors.add(org);
									}
								}		
							}
						}
					}
				}
			}
		    //list in rdf: ResourceConfig.BIOTEA_URL + "/{0}/pmc_resource/" + pmcID + "/";
			String[] params = {"listOfAuthors"};
		    ListOfAuthorsE loa = new ListOfAuthorsE(model, Conversion.replaceParameter(global.BASE_URL_LIST_PMC, params), true); //model.createBlankNode(pmcID + "_listAuthors")
		    loa.addMembersInOrder(model, listOfAuthors); //add members to list
		    document.addbiboListofauthors(loa); //add list to document
		} catch (Exception e){
			logger.info(paper + ": Authors not processed.");
		}			
	}
	/**
	 * Processes the affiliation for authors, creates a group and add the members.
	 * Note: not in use
	 * @param xref
	 * @param Agent
	 */
	@Deprecated
	private void processAffiliation(Model model, Document document, Xref xref, Agent agent) {		
		for (String refId: xref.getRid().split(" ")) { //author's affiliations
			boolean found = false;
			for (Object affil: article.getFront().getArticleMeta().getContribGroupsAndAfvesAndXS()) { //list of affiliations
				if (affil instanceof Aff) {
					Aff aff = (Aff)affil;
					if (refId.equalsIgnoreCase(aff.getId())) {
						for (Object objAff: aff.getContent()) { //affiliation content
							if (objAff instanceof AddrLine) {
								AddrLine addr = (AddrLine)objAff;
								for (Object objAddr: addr.getContent()) { //address line content
									if (objAddr instanceof String) {
										if (agent != null) {
											String groupId = objAddr.toString().replaceAll(RDFHandler.CHAR_NOT_ALLOWED, "-");
											GroupE groupe = new GroupE(model, global.BASE_URL_GROUP_PMC + groupId, true); //model.createBlankNode(pmcID + "_group_" + groupId)
										    groupe.addName(model, objAddr.toString());
										    groupe.addMember(agent);
										}
										found = true;
										break;
									}
								}
							}
							if (found) break;	
						}											    
					}					
				}
				if (found) break;	
			}
		}
	}
	
	/**
	 * Processes abstract and keywords.
	 */
	private void processAbstractAndKeywords(Model model, Document document) {
		//Abstract
	    String docAbstract = "";
		for (Abstract ab: article.getFront().getArticleMeta().getAbstracts()) {
			docAbstract += getAbstractText(model, document, ab);			
		}
	    document.addbiboAbstract(docAbstract);
	    
	    //Keywords
		//Collection<String> docKeywords = new ArrayList<String>();
		for (KwdGroup keywords: article.getFront().getArticleMeta().getKwdGroups()) {
			for (Object obj: keywords.getKwdsAndCompoundKwdsAndXS()) {
				if (obj instanceof Kwd) {
					Kwd keyword = (Kwd)obj;
					for (Object cont: keyword.getContent()) {
						String key = "";
						if (cont instanceof String) {
							key += cont.toString();
						} else if (cont instanceof JAXBElement<?>) {
							JAXBElement<?> elem = (JAXBElement<?>)cont;
							key += processElement(model, document, elem, null, null);
						}
						document.addbiboShortDescription(key);
					}
				} 
			}
		}
	}
	
	/**
	 * Determines the type of the reference and process it according to its type: Citation, MixedCitation, ElementCitation, or NlmCitation
	 */
	private void processReferences(Model model, Document document) {
		//References
	    for (Object obj: article.getBack().getAcksAndAppGroupsAndBios()) {
	    	if (obj instanceof RefList) {
	    		RefList refList = (RefList)obj;
	    		for (Object objRef: refList.getAddressesAndAlternativesAndArraies()) {
	    			if (objRef instanceof Ref) {	    				
	    				Ref ref = (Ref)objRef;	    				
	    				for (Object objMix: ref.getElementCitationsAndMixedCitationsAndNlmCitations()) {
	    					if (objMix instanceof Citation) {
	    						Citation citation = (Citation)objMix;
	    						//System.out.println("SIMPLE");
	    						processSimpleCitation(model, document, citation, ref);
	    					} else if (objMix instanceof MixedCitation) {
	    						MixedCitation citation = (MixedCitation)objMix;
	    						//System.out.println("MIXED");
	    						processMixedCitation(model, document, citation, ref);
	    					} else if (objMix instanceof ElementCitation) {
	    						ElementCitation citation = (ElementCitation)objMix;
	    						//System.out.println("CITATION");
	    						processElementCitation(model, document, citation, ref);
	    					} else if (objMix instanceof NlmCitation) {
	    						NlmCitation citation = (NlmCitation)objMix;
	    						//System.out.println("NLM");
	    						processNlmCitation(model, document, citation, ref);
	    					} 
	    				}	    				
	    			}
	    		}
	    	}	    	
	    }
	}
	/**
	 * Creates a reference to an article, URL could point to id, doi, or ref id.
	 * @param pubmed
	 * @param doiReference
	 * @param ref
	 * @return
	 */
	private Document processReferenceCreateArticle(Model model, Document document, String url, String pubmed, String doiReference, Ref ref, ReferenceType type) {		
		Document docReference = null;		

		//create document
		if (type == ReferenceType.JOURNAL_ARTICLE) {
			docReference = new AcademicArticle(model, url, true);			
		} else if (type == ReferenceType.BOOK) {
			docReference = new Book(model, url, true);			
		} else if (type == ReferenceType.BOOK_SECTION) {
			docReference = new BookSection(model, url, true);			
		} else if (type == ReferenceType.REPORT) {
			docReference = new Report(model, url, true);	
		} else if (type == ReferenceType.THESIS) {
			docReference = new Thesis(model, url, true);	
		} else if (type == ReferenceType.CONFERENCE_PROCS) {
			docReference = new Proceedings(model, url, true);
		} else if (type == ReferenceType.CONFERENCE_PAPER) {
			docReference = new AcademicArticle(model, url, true);	
		} else if (type == ReferenceType.PATENT) {
			docReference = new ws.biotea.ld2rdf.rdf.model.bibo.Patent(model, url, true);	
		} else if (type == ReferenceType.DATABASE) {
			docReference = new ws.biotea.ld2rdf.rdf.model.bibo.Webpage(model, url, true);	
		} else if (type == ReferenceType.WEBPAGE) {
			docReference = new ws.biotea.ld2rdf.rdf.model.bibo.Webpage(model, url, true);	
		} else if (type == ReferenceType.COMMUN) {
			docReference = new ws.biotea.ld2rdf.rdf.model.bibo.PersonalCommunicationDocument(model, url, true);	
		} else if (type == ReferenceType.DISCUSSION) {
			docReference = new ws.biotea.ld2rdf.rdf.model.bibo.Webpage(model, url, true);	
		} else if (type == ReferenceType.BLOG) {
			docReference = new ws.biotea.ld2rdf.rdf.model.bibo.Webpage(model, url, true);	
		} else if (type == ReferenceType.WIKI) {
			docReference = new ws.biotea.ld2rdf.rdf.model.bibo.Webpage(model, url, true);	
		} else if (type == ReferenceType.SOFTWARE) {
			docReference = new ws.biotea.ld2rdf.rdf.model.bibo.Standard(model, url, true);	
		} else if (type == ReferenceType.STANDARD) {
			docReference = new ws.biotea.ld2rdf.rdf.model.bibo.Standard(model, url, true);	
		} else { //other
			docReference = new ws.biotea.ld2rdf.rdf.model.bibo.Document(model, url, true);	
		}			
		
		if (pubmed != null) {
			docReference.addIdentifier(model, "pmid:" + pubmed);
			docReference.addPMID(model, pubmed);
			docReference.addSeeAlso(new URIImpl(GlobalArticleConfig.pubMedURI + pubmed)); //seeAlso for webpages
			docReference.addSameAs(model, ResourceConfig.IDENTIFIERS_ORG_PUBMED + pubmed); //sameAs for entities
			docReference.addSameAs(model, ResourceConfig.BIO2RDF_PUBMED + pubmed); //sameAs for entities
			if (doiReference != null) {
				try {
					docReference.addDOI(model, doiReference);
					docReference.addIdentifier(model, "doi:" + doiReference);
					//docReference.addSeeAlso(new URIImpl(global.doiURI + doiReference)); //seeAlso for webpages
					docReference.addSameAs(model, GlobalArticleConfig.doiURI + doiReference);
					if (!global.getUriStyle().equals(ResourceConfig.bio2rdf)) {
						docReference.addSameAs(model, GlobalArticleConfig.DOI_DOCUMENT + doiReference); //same as from pubmed to doi entity
					}
				} catch (Exception e) {	}
			}	
		} else if (doiReference != null) {
			docReference.addDOI(model, doiReference);
			docReference.addIdentifier(model, "doi:" + doiReference);
			//docReference.addSeeAlso(new URIImpl(global.doiURI + doiReference)); //seeAlso for webpages
			docReference.addSameAs(model, GlobalArticleConfig.doiURI + doiReference);
			if (!global.getUriStyle().equals(ResourceConfig.bio2rdf)) {
				docReference.addSameAs(model, GlobalArticleConfig.DOI_DOCUMENT + doiReference); //same as from pubmed to doi entity
			}
		}
		
		return (docReference);
	}
	/**
	 * Creates a list of PersonE with the authors of an article, represented in a list of objects, only Name objects are processed.
	 * @param lst
	 * @return
	 */
	private List<Agent> processReferenceCreateListOfAuthors(Model model, Document document, List<Object> lst, String personBaseURL){
		ArrayList<Agent> listOfAuthorsRef = new ArrayList<Agent>();
		for (Object objPerson: lst) {
	    	if (objPerson instanceof Name) {
	    		//Author
	    		Name name = (Name)objPerson;	
	    		String givenNames = "";
				try {
					givenNames = name.getGivenNames().getContent().get(0).toString();
				} catch (Exception e) {}
				String surname = name.getSurname().getContent().get(0).toString();
				String idPerson = (givenNames + surname).replaceAll(RDFHandler.CHAR_NOT_ALLOWED, "-");
				PersonE person  = new PersonE(model, personBaseURL + idPerson, true); //model.createBlankNode(pmcID + "_reference_" + this.getRefId(ref) + "_" + type + "_" + idPerson)
			    person.addName(model, givenNames + " " + surname);
			    person.addGivenName(model, givenNames);
			    person.addFamilyName(model, surname);			    
			    listOfAuthorsRef.add(person);			    
	    	}	    	    				    	
	    }
		return (listOfAuthorsRef);
	}
	/**
	 * Creates the reference to a journal article, its journal, and list of authors.
	 * @param ref
	 * @param content
	 */
	@SuppressWarnings("rawtypes")
	private void processReferenceAllTypeCitation(Model model, Document document, Ref ref, List<Object> content, ReferenceType type, Class clazz) {
		//common
		List<Agent> listOfAuthorsRef = new ArrayList<Agent>();
		List<Agent> listOfEditorsRef = new ArrayList<Agent>();
		List<Agent> listOfTranslatorsRef = new ArrayList<Agent>();		
		String sourceTitle = null;
    	String sourceId =  null;
    	String year = null;
    	String month = null;
    	String volume = null;
    	String issueNumber = null;
    	String pageStart = null;
    	String pageEnd = null;
    	String pubmedReference = null;
    	String doiReference = null;
    	
    	//journal article
    	String articleTitle = null;
    	String transTitle = null;
    	
    	//book and report
    	String sectionTitle = null;
    	String sectionId =  null;		
		String edition = null;
		String publisherName = null;
    	String numPages = null;
    	String pageCount = null;
    	String publisherLocation = null;
    	
    	//report
    	String pubIdOther = null;
    	
    	//conference
    	String confName = null;
    	String confDate = null;
    	String confLoc = null;
    	
    	//Others
    	String comment = null;
    	String dateInCitation = null;
    	String accessDate = null;
    	String otherTitle = "";  	
		
    	//Process a reference
		for (Object obj: content) {
			if (obj instanceof PubId) {
				PubId pubId = (PubId)obj;
				if (pubId.getPubIdType().equals("doi")) {
					doiReference = pubId.getContent();
				} else if (pubId.getPubIdType().equals("pmid")) {
					pubmedReference = pubId.getContent();
				} else if (pubId.getPubIdType().equals("other")) {
					pubIdOther = pubId.getContent(); 	
				}			
			} 
		}
		String publicationLink = null; 
		String personBaseURL = null;
		String organizationBaseURL = null;
		String proceedingsBaseURL = null;
		String conferenceBaseURL = null;
		if (pubmedReference != null) {
			publicationLink = GlobalArticleConfig.PUBMED_DOCUMENT + pubmedReference;
			String[] params = {pubmedReference};
			personBaseURL = Conversion.replaceParameter(GlobalArticleConfig.BASE_URL_PERSON_PUBMED, params);
			organizationBaseURL = Conversion.replaceParameter(GlobalArticleConfig.BASE_URL_ORGANIZATION_PUBMED, params);
			proceedingsBaseURL = Conversion.replaceParameter(GlobalArticleConfig.BASE_URL_PROCEEDINGS_PUBMED, params);
			conferenceBaseURL = Conversion.replaceParameter(GlobalArticleConfig.BASE_URL_CONFERENCE_PUBMED, params);
		} else if (doiReference != null) {
			publicationLink = GlobalArticleConfig.DOI_DOCUMENT + doiReference;
			String[] params = {doiReference};
			personBaseURL = Conversion.replaceParameter(GlobalArticleConfig.BASE_URL_PERSON_DOI, params);
			organizationBaseURL = Conversion.replaceParameter(GlobalArticleConfig.BASE_URL_ORGANIZATION_DOI, params);
			proceedingsBaseURL = Conversion.replaceParameter(GlobalArticleConfig.BASE_URL_PROCEEDINGS_DOI, params);
			conferenceBaseURL = Conversion.replaceParameter(GlobalArticleConfig.BASE_URL_CONFERENCE_DOI, params);
		} else {
			publicationLink = global.BASE_URL_REF + this.getRefId(ref);
			personBaseURL = global.BASE_URL_PERSON_PMC;
			organizationBaseURL = global.BASE_URL_ORGANIZATION_PMC;
			proceedingsBaseURL = global.BASE_URL_PROCEEDINGS_PMC;
			conferenceBaseURL = global.BASE_URL_CONFERENCE_PMC;
		}		
		//process other information
		for (Object obj: content) {
			if (obj instanceof ArticleTitle) { //nlm-citation & citation: used for chapter-title
    			try {
    				if (articleTitle == null) {
    					articleTitle = processListOfElements(model, document, ((ArticleTitle)obj).getContent(), null, null);
    				}    				
    			} catch (Exception e) {}
    		} else if (obj instanceof Source) {
    			try {
    				sourceTitle = processListOfElements(model, document, ((Source)obj).getContent(), null, null);
    				sourceId = sourceTitle.replaceAll(" ", "");
    			} catch (Exception e) {}
    		} else if (obj instanceof ChapterTitle) {
    			try {
    				sectionTitle = processListOfElements(model, document, ((Source)obj).getContent(), null, null);
    				sectionId = sectionTitle.replaceAll(" ", "");
    			} catch (Exception e) {}
    		} else if (obj instanceof PublisherName) {
    			try {
    				publisherName = processListOfElements(model, document, ((PublisherName)obj).getContent(), null, null);
    			} catch (Exception e) {}
    		} else if (obj instanceof PublisherLoc) {
    			try {
    				publisherLocation = processListOfElements(model, document, ((PublisherLoc)obj).getContent(), null, null);
    			} catch (Exception e) {}
    		} else if (obj instanceof Size) {
    			try {
    				numPages = processListOfElements(model, document, ((Size)obj).getContent(), null, null);
    			} catch (Exception e) {}
    		} else if (obj instanceof Edition) {
    			try {
    				edition = processListOfElements(model, document, ((Edition)obj).getContent(), null, null);
    			} catch (Exception e) {}
    		} else if (obj instanceof Year) {
    			try {
    				year = ((Year)obj).getContent();
    			} catch (Exception e) {}
    		} else if (obj instanceof Month) {
    			try {
    				month = ((Month)obj).getContent();
    			} catch (Exception e) {}
    		} else if (obj instanceof Volume) {
    			try {
    				volume = processListOfElements(model, document, ((Volume)obj).getContent(), null, null);
    			} catch (Exception e) {}
    		} else if (obj instanceof Issue) {
    			try {
    				issueNumber = processListOfElements(model, document, ((Issue)obj).getContent(), null, null);
    			} catch (Exception e) {}
    		} else if (obj instanceof Fpage) {
    			try {
    				pageStart = ((Fpage)obj).getContent();
    			} catch (Exception e) {}
			} else if (obj instanceof Lpage) {
				try {
					pageEnd = ((Lpage)obj).getContent();
    			} catch (Exception e) {}
			} else if (obj instanceof ConfName) {
				try {
					confName = processListOfElements(model, document, ((ConfName)obj).getContent(), null, null);
				} catch (Exception e){}
			} else if (obj instanceof ConfDate) {
				try {
					confDate = processListOfElements(model, document, ((ConfDate)obj).getContent(), null, null);
				} catch (Exception e){}
			} else if (obj instanceof ConfLoc) {
				try {
					confLoc = processListOfElements(model, document, ((ConfLoc)obj).getContent(), null, null);
				} catch (Exception e){}
			} else if (obj instanceof Comment) {
				try {
					comment = processListOfElements(model, document, ((Comment)obj).getContent(), null, null);
				} catch (Exception e){}
			} else if (obj instanceof DateInCitation) {
				try {
					dateInCitation = processListOfElements(model, document, ((DateInCitation)obj).getContent(), null, null);
				} catch (Exception e){}
			} else if (obj instanceof PersonGroup) {
				PersonGroup pg = (PersonGroup)obj;
				if ((pg.getPersonGroupType() != null) && (pg.getPersonGroupType().equals("editor"))) {
					listOfEditorsRef = processReferenceCreateListOfAuthors(model, document, ((PersonGroup)obj).getContent(), personBaseURL); //"editor"
				} else if ((pg.getPersonGroupType() != null) && (pg.getPersonGroupType().equals("translator"))) {
					listOfTranslatorsRef = processReferenceCreateListOfAuthors(model, document, ((PersonGroup)obj).getContent(), personBaseURL); //"translator"
				} else {
					listOfAuthorsRef = processReferenceCreateListOfAuthors(model, document, ((PersonGroup)obj).getContent(), personBaseURL); //"author"
				}				
			} else if (obj instanceof Name) {
				Name name = (Name)obj;	 
				String givenNames = "";
				try {
					givenNames = name.getGivenNames().getContent().get(0).toString();
				} catch (Exception e) {}
				String surname = name.getSurname().getContent().get(0).toString();
				String idPerson = (givenNames + surname).replaceAll(RDFHandler.CHAR_NOT_ALLOWED, "-");				
		    	PersonE person  = new PersonE(model, personBaseURL + idPerson, true); //model.createBlankNode(pmcID + "_authorOrEditorOrTranslator_" + idPerson)
			    person.addName(model, givenNames + " " + surname);
			    person.addGivenName(model, givenNames);
			    person.addFamilyName(model, surname);			    
		    	listOfAuthorsRef.add(person);			    		    			
			} else if (obj instanceof Collab) {
				Collab collab = (Collab)obj;
				String orgName = collab.getContent().get(0).toString();
				String idOrg = orgName.replaceAll(RDFHandler.CHAR_NOT_ALLOWED, "-");
				OrganizationE org = new OrganizationE(model, organizationBaseURL + idOrg, true ); //model.createBlankNode(pmcID + "_author_" + idOrg) 
				listOfAuthorsRef.add(org);
			} else if (obj instanceof AccessDate) { //nlm-citation
				try {
					accessDate = processListOfElements(model, document, ((AccessDate)obj).getContent(), null, null);
				} catch (Exception e){}
			} else if (obj instanceof PageCount) { //nlm-citation
				try {
					pageCount = ((PageCount)obj).getCount();
				} catch (Exception e){}
			} else if (obj instanceof TransTitle) { //nlm-citation
				try {
					transTitle = processListOfElements(model, document, ((TransTitle)obj).getContent(), null, null);
				} catch (Exception e){}
			} else if (obj instanceof PubId) {
				//it was already processed		
			} else {
				try {
					otherTitle += this.processElement(model, document, obj, null, null);
				} catch (Exception e) {}
			}
		}						    
		//Reference
		Document docReference = null;
		if ((type == ReferenceType.BOOK) && (sectionId != null)) {
			docReference = processReferenceCreateArticle(model, document, publicationLink, pubmedReference, doiReference, ref, ReferenceType.BOOK_SECTION);
			type = ReferenceType.BOOK_SECTION;
		} else if ((type == ReferenceType.BOOK) && (articleTitle != null)) {//nlm-citation
			docReference = processReferenceCreateArticle(model, document, publicationLink, pubmedReference, doiReference, ref, ReferenceType.BOOK_SECTION);
			type = ReferenceType.BOOK_SECTION;
		} else if ((type == ReferenceType.CONFERENCE_PROCS) && (articleTitle != null)) {
			docReference = processReferenceCreateArticle(model, document, publicationLink, pubmedReference, doiReference, ref, ReferenceType.CONFERENCE_PAPER);
			type = ReferenceType.CONFERENCE_PAPER;
		} else {
			docReference = processReferenceCreateArticle(model, document, publicationLink ,pubmedReference, doiReference, ref, type);
		}
		//We need to keep the reference format as a document resource so we can link sections and paragraphs to it		
		if (pubmedReference != null) {
			docReference.addSameAs(model, global.BASE_URL_REF + this.getRefId(ref));
		} else if (doiReference != null) {
			docReference.addSameAs(model, global.BASE_URL_REF + this.getRefId(ref));
		}
    	try {
    		if (type == ReferenceType.JOURNAL_ARTICLE) {
        		//Journal
        		if (sourceId != null) {
        			String journalTitleInURI = sourceId.replaceAll(RDFHandler.CHAR_NOT_ALLOWED, "-");
        			JournalE journal = new JournalE(model, GlobalArticleConfig.BASE_URL_JOURNAL_NAME + journalTitleInURI, true); //model.createBlankNode(pmcID + "_journal_" + sourceId.replaceAll(RDFHandler.CHAR_NOT_ALLOWED, "-"))
        		    journal.addTitle(model, sourceTitle);
        		    //Journal, issue-date, volume, pages, and document        		    
            		if (articleTitle != null) {
            			docReference.addTitle(model, articleTitle);
            		}
        		    if (volume != null) {
        		    	docReference.addbiboVolume(volume);
        		    }
        		    OrganizationE publisherOrg = new OrganizationE(model, GlobalArticleConfig.BASE_URL_PUBLISHER_NAME + publisherName.replaceAll(RDFHandler.CHAR_NOT_ALLOWED, "-"), true ); 
        		    publisherOrg.addName(model, publisherName);	
        		    journal.addbiboPublisher(publisherOrg);
        		    docReference.addbiboPublisher(publisherOrg);
        		    //Issue
        		    if (journal != null) {
        		    	if (issueNumber != null) { //name:<journal_name>/<issue_id>
        		    		String[] params = {"name", journalTitleInURI};
            				String issueURI = Conversion.replaceParameter(GlobalArticleConfig.BASE_URL_ISSUE, params);
                    		ws.biotea.ld2rdf.rdf.model.bibo.Issue issue = 
                    			new ws.biotea.ld2rdf.rdf.model.bibo.Issue(model, issueURI + issueNumber.replaceAll(RDFHandler.CHAR_NOT_ALLOWED, "-"), true);    		
                    		docReference.addIssue(model, issue);
                    		//issue.addbiboIssue(issueNumber);
                    		//issue.addJournal(model, journal); 
                    		journal.addIssue(model, issue);
                    		issue.addDocument(model, docReference);
                		} else {
                			/*String[] params = {"name", journalTitleInURI};
            				String issueURI = Conversion.replaceParameter(this.BASE_URL_ISSUE, params);
                			ws.biotea.ld2rdf.rdf.model.bibo.Issue issue = 
                    			new ws.biotea.ld2rdf.rdf.model.bibo.Issue(model, issueURI + (elementCount++), true);*/    		
                			//docReference.addIssue(model, issue);
                			//issue.addJournal(model, journal);  
                			journal.addDocument(model, docReference);
                		}
        		    }            		
        		}
        		
	    	} else if (type == ReferenceType.BOOK) { //book
	    		if (sourceId != null) { 
        			docReference.addTitle(model, sourceTitle);        		  
        		}
	    	} else if (type == ReferenceType.BOOK_SECTION) { //Book section
	    		if (sectionId != null) {
	    			if (sectionTitle != null) {
	    				docReference.addTitle(model, sectionTitle);
	    			}
	    		    if (sourceTitle != null) {
	    		    	docReference.addDCTitle(model, "(Book title) " + sourceTitle); //did not find other way
	    		    }	    	    	
	    		}
	    	} else if (type == ReferenceType.REPORT) {
	    		if (sourceId != null) {	 
	    			docReference.addTitle(model, sourceTitle);
	    		    if (pubIdOther != null) {
	    		    	docReference.addIdentifier(model, pubIdOther);
	    		    } 
				}
	    	} else if (type == ReferenceType.THESIS) {
	    		if (sourceId != null) {	 
	    			if (sourceTitle != null) {
	    				docReference.addTitle(model, sectionTitle);
	    			}
	    		    if (sectionTitle != null) {
	    		    	docReference.addDCTitle(model, "(Chapter title) " + sectionTitle); //did not find other way
	    		    }
				}
	    	} else if (type == ReferenceType.CONFERENCE_PAPER) {//paper (article) and proceedings (book)
	    		if (articleTitle != null) {
    				docReference.addTitle(model, sectionTitle);
    			}    		    
    		    if (sourceTitle != null) {
    		    	docReference.addComment("Proceedings title: " + sourceTitle); 
    		    }
    		    //proceedings
    		    Document proc = new Proceedings(model, proceedingsBaseURL + this.getRefId(ref), true); //model.createBlankNode(pmcID + "_referencedDocument(Proceedings)_" + pmcID + "_" + this.getRefId(ref))
    		    proc.addTitle(model, sourceTitle); 
    		    if (numPages != null) {
    		    	proc.addbiboNumberofpages(numPages);
    		    }
    		    if (edition != null) {
    		    	proc.addbiboEdition(edition);
    		    }
    		    if (publisherName != null) {
    		    	OrganizationE publisherOrg = new OrganizationE(model, GlobalArticleConfig.BASE_URL_PUBLISHER_NAME + publisherName.replaceAll(RDFHandler.CHAR_NOT_ALLOWED, "-"), true ); 
        		    publisherOrg.addName(model, publisherName);	
        		    proc.addbiboPublisher(publisherOrg);
    		    }
    		    if (publisherLocation != null) {// TODO: publisher location    		    	
    		    	//Node publisherNode = new PlainLiteralImpl(publisherLocation);
    		    	//proc.addbiboPublisher(publisherNode);
    		    }
        		if (year != null) {
    		    	if (month != null) {
    		    		Node dateNode = new PlainLiteralImpl(year + " " + month);		 
    		    		proc.addbiboIssued(dateNode);
    		    	} else {
    		    		Node dateNode = new PlainLiteralImpl(year);		 
    		    		proc.addbiboIssued(dateNode);
    		    	}    		    			    
    		    }        		      	
        		String refListURL = null;
    		    String[] params = {GlobalArticleConfig.listOfEditorsAndTranslators, ""};		    		   
        		String refEditorsURL = null;
        		String[] params1 = {GlobalArticleConfig.listOfEditors, ""};
        		String refTranslatorsURL = null;
        		String[] params2 = {GlobalArticleConfig.listOfTranslators, ""};
        		if (pubmedReference != null) {
    				params[1] = pubmedReference;
    				refListURL = Conversion.replaceParameter(GlobalArticleConfig.BASE_URL_LIST_PUBMED, params);
    				params1[1] = pubmedReference;
    				refEditorsURL = Conversion.replaceParameter(GlobalArticleConfig.BASE_URL_LIST_PUBMED, params1);
    				params2[1] = pubmedReference;
    				refTranslatorsURL = Conversion.replaceParameter(GlobalArticleConfig.BASE_URL_LIST_PUBMED, params2);
        		} else if (doiReference != null) {
        			params[1] = doiReference;
    				refListURL = Conversion.replaceParameter(GlobalArticleConfig.BASE_URL_LIST_DOI, params);
    				params1[1] = doiReference;
    				refEditorsURL = Conversion.replaceParameter(GlobalArticleConfig.BASE_URL_LIST_DOI, params1);
    				params2[1] = doiReference;
    				refTranslatorsURL = Conversion.replaceParameter(GlobalArticleConfig.BASE_URL_LIST_DOI, params2);
        		} else {
        			params[1] = this.getRefId(ref);
    				refListURL = Conversion.replaceParameter(global.BASE_URL_REF_LIST, params);
    				params1[1] = this.getRefId(ref);
    				refEditorsURL = Conversion.replaceParameter(global.BASE_URL_REF_LIST, params1);
    				params2[1] = this.getRefId(ref);
    				refTranslatorsURL = Conversion.replaceParameter(global.BASE_URL_REF_LIST, params2);
        		}
        		if ((listOfAuthorsRef != null) && (listOfAuthorsRef.size() != 0)) {  
        			for(Agent agent: listOfAuthorsRef) {
        				if (agent instanceof PersonE) {
        					((PersonE)agent).addPublications(model, publicationLink);
        				}
        			}        			
        			if (clazz.equals(MixedCitation.class)) {
        				ListOfAuthorsE loaRef = null;
            			loaRef = new ListOfAuthorsE(model, refListURL, true); //model.createBlankNode(pmcID + "_listEditorsAndTranslatorsReference_" + this.getRefId(ref))
            			loaRef.addMembersInOrder(model, listOfAuthorsRef); //add members to list
            			proc.addbiboListofauthors(loaRef); //add list to document  
            		}    	    				                    		     	
            	}
            	if ((listOfEditorsRef != null) && (listOfEditorsRef.size() != 0)) {
            		for(Agent agent: listOfEditorsRef) {
        				if (agent instanceof PersonE) {
        					((PersonE)agent).addPublications(model, publicationLink);
        				}
        			} 
            		ListOfAuthorsE loaRef = new ListOfAuthorsE(model, refEditorsURL, true); //model.createBlankNode(pmcID + "_listEditorsReference_" + this.getRefId(ref))
        		    loaRef.addMembersInOrder(model, listOfAuthorsRef); //add members to list
        		    proc.addbiboListofeditors(loaRef); //add list to document		        	
            	}
            	if ((listOfTranslatorsRef != null) && (listOfTranslatorsRef.size() != 0)) {
            		for(Agent agent: listOfTranslatorsRef) {
        				if (agent instanceof PersonE) {
        					((PersonE)agent).addPublications(model, publicationLink);
        				}
        			} 
            		ListOfAuthorsE loaRef = new ListOfAuthorsE(model, refTranslatorsURL, true); // model.createBlankNode(pmcID + "_listlistOfTranslatorsReference_" + this.getRefId(ref))
        		    loaRef.addMembersInOrder(model, listOfAuthorsRef); //add members to list
        		    proc.addbiboListofeditors(loaRef); //add list to document		        	
            	}
            	docReference.addbiboReproducedIn(proc);
    		    //conference
    		    String confYear = confDate != null ? confDate.substring(0,4).replaceAll(" ", "") : "";
    		    String confNameId = confName != null ? confName.replaceAll(" ", "") : "";
    		    String confLocId = confLoc != null ? confLoc.replaceAll(" ", "") : "";
    		    String confId = (confNameId + "_" + confLocId + "_" + confYear).replaceAll(RDFHandler.CHAR_NOT_ALLOWED, "-");
    		    Conference conf = new Conference(model, conferenceBaseURL + confId, true); //model.createBlankNode(pmcID + "_conference_" + confId)
    		    if (confName != null) {
    		    	Node node = new PlainLiteralImpl(confName);	
    		    	conf.addbiboName(node);
    		    }    		    
    		    if (confLoc != null) {
    		    	conf.addPlace(model, confLoc);
    		    }
    		    if (confDate != null) {
    		    	Node node = new PlainLiteralImpl(confDate);
    		    	conf.addbiboDate(node);
    		    }
    		    docReference.addbiboPresentedat(conf);
    		    proc.addbiboProducedin(conf);
	    	} else if (type == ReferenceType.CONFERENCE_PROCS) { //only proceedings (book) 	    		    		    
	    		docReference.addTitle(model, sourceTitle); 
    		    //conference
    		    String confYear = confDate != null ? confDate.substring(0,4).replaceAll(" ", "") : "";
    		    String confNameId = confName != null ? confName.replaceAll(" ", "") : "";
    		    String confLocId = confLoc != null ? confLoc.replaceAll(" ", "") : "";
    		    String confId = (confNameId + "_" + confLocId + "_" + confYear).replaceAll(RDFHandler.CHAR_NOT_ALLOWED, "-");
    		    Conference conf = new Conference(model, conferenceBaseURL + confId, true); //model.createBlankNode(pmcID + "_conference_" + confId)
    		    if (confName != null) {
    		    	Node node = new PlainLiteralImpl(confName);	
    		    	conf.addbiboName(node);
    		    }    		    
    		    if (confLoc != null) {
    		    	conf.addPlace(model, confLoc);
    		    }
    		    if (confDate != null) {
    		    	Node node = new PlainLiteralImpl(confDate);
    		    	conf.addbiboDate(node);
    		    }
    		    docReference.addbiboProducedin(conf);    			
	    	} else if (type == ReferenceType.PATENT) {
	    		if (articleTitle != null) {
    				docReference.addTitle(model, sectionTitle);
    			}
    		    if (sourceTitle != null) {
    		    	docReference.addComment("Source: " + sourceTitle);
    		    }
    		    docReference.addComment("Note: Inventors, asignees, and others are described in the Authors list.");
	    	} else if (type == ReferenceType.DATABASE) { //webpage
	    		if (sourceTitle != null) {
    				docReference.addTitle(model, sectionTitle);
    			}
	    	} else if (type == ReferenceType.WEBPAGE) { //webpage
	    		if (sourceTitle != null) {
    				docReference.addTitle(model, sectionTitle);
    			}
	    	} else if (type == ReferenceType.COMMUN) { //personal communication
	    		if (sourceTitle != null) {
    				docReference.addTitle(model, sectionTitle);
    			}
	    	} else if (type == ReferenceType.DISCUSSION) { //webpage with article title
	    		if (articleTitle != null) {
    				docReference.addTitle(model, sectionTitle);
    			}
    		    if (sourceTitle != null) {
    		    	docReference.addComment("Discussion/List group: " + sourceTitle);
    		    }
	    	} else if (type == ReferenceType.BLOG) { //webpage
	    		if (sourceTitle != null) {
    				docReference.addTitle(model, sectionTitle);
    			}
	    	} else if (type == ReferenceType.WIKI) { //webpage
	    		if (sourceTitle != null) {
    				docReference.addTitle(model, sectionTitle);
    			}
	    	} else if (type == ReferenceType.SOFTWARE) { //standard
	    		if (sourceTitle != null) {
    				docReference.addTitle(model, sectionTitle);
    			}
	    	} else if (type == ReferenceType.STANDARD) { //standard
	    		if (sourceTitle != null) {
    				docReference.addTitle(model, sectionTitle);
    			}
	    	} else if (type == ReferenceType.OTHER) { //standard
	    		if (sourceTitle != null) {
    				docReference.addTitle(model, sectionTitle);
    			}
	    	} 		
    		//common
    		if (numPages != null) {
		    	docReference.addbiboNumberofpages(numPages);
		    } else {
		    	if (pageCount != null) { //try with pageCount
		    		docReference.addbiboNumberofpages(pageCount);
		    	}
		    }
    		if (transTitle != null) {
    			docReference.addTitle(model, transTitle);
    		}
		    if (edition != null) {
		    	docReference.addbiboEdition(edition);
		    }
		    if (publisherName != null) {
		    	OrganizationE publisherOrg = new OrganizationE(model, GlobalArticleConfig.BASE_URL_PUBLISHER_NAME + publisherName.replaceAll(RDFHandler.CHAR_NOT_ALLOWED, "-"), true ); 
    		    publisherOrg.addName(model, publisherName);	
    		    docReference.addbiboPublisher(publisherOrg);
		    	//Node publisherNode = new PlainLiteralImpl(publisherName);
			    //docReference.addbiboPublisher(publisherNode);
		    }
		    if (publisherLocation != null) {// TODO: publisher location
		    	//Node publisherNode = new PlainLiteralImpl(publisherLocation);
		    	//proc.addbiboPublisher(publisherNode);
		    }
    		if (year != null) {
		    	if (month != null) {
		    		Node dateNode = new PlainLiteralImpl(year + " " + month);		 
    			    docReference.addbiboIssued(dateNode);
		    	} else {
		    		Node dateNode = new PlainLiteralImpl(year);		 
    			    docReference.addbiboIssued(dateNode);
		    	}    		    			    
		    }
		    if (pageStart != null) {
		    	docReference.addbiboPagestart(pageStart);
		    }
		    if (pageEnd != null) {
		    	docReference.addbiboPageend(pageEnd);
		    }
		    if (comment != null) {
		    	docReference.addComment(comment);
		    }
		    //otherTitle is just garbage
		    /*if ((otherTitle != null) && (otherTitle.length() != 0)) {
		    	docReference.addTitle(model, otherTitle);
		    }*/
		    if (dateInCitation != null) {
		    	docReference.addComment("Access date (aka date in citation): " + dateInCitation);
		    }
		    if (accessDate != null) {
		    	docReference.addComment("Access date: " + accessDate);
		    }
		    //list of authors in rdf
		    String refListURL = null;
		    String[] params = {GlobalArticleConfig.listOfAuthorsEditorsAndTranslators, ""};		    		   
    		String refEditorsURL = null;
    		String[] params1 = {GlobalArticleConfig.listOfEditors, ""};
    		String refTranslatorsURL = null;
    		String[] params2 = {GlobalArticleConfig.listOfTranslators, ""};
    		String refAuthorsURL = null;
		    String[] params3 = {GlobalArticleConfig.listOfAuthors, ""};
    		if (pubmedReference != null) {
				params[1] = pubmedReference;
				refListURL = Conversion.replaceParameter(GlobalArticleConfig.BASE_URL_LIST_PUBMED, params);
				params1[1] = pubmedReference;
				refEditorsURL = Conversion.replaceParameter(GlobalArticleConfig.BASE_URL_LIST_PUBMED, params1);
				params2[1] = pubmedReference;
				refTranslatorsURL = Conversion.replaceParameter(GlobalArticleConfig.BASE_URL_LIST_PUBMED, params2);
				params3[1] = pubmedReference;
				refAuthorsURL = Conversion.replaceParameter(GlobalArticleConfig.BASE_URL_LIST_PUBMED, params3);
    		} else if (doiReference != null) {
    			params[1] = doiReference;
				refListURL = Conversion.replaceParameter(GlobalArticleConfig.BASE_URL_LIST_DOI, params);
				params1[1] = doiReference;
				refEditorsURL = Conversion.replaceParameter(GlobalArticleConfig.BASE_URL_LIST_DOI, params1);
				params2[1] = doiReference;
				refTranslatorsURL = Conversion.replaceParameter(GlobalArticleConfig.BASE_URL_LIST_DOI, params2);
				params3[1] = doiReference;
				refAuthorsURL = Conversion.replaceParameter(GlobalArticleConfig.BASE_URL_LIST_DOI, params3);
    		} else {
    			params[1] = this.getRefId(ref);
				refListURL = Conversion.replaceParameter(global.BASE_URL_REF_LIST, params);
				params1[1] = this.getRefId(ref);
				refEditorsURL = Conversion.replaceParameter(global.BASE_URL_REF_LIST, params1);
				params2[1] = this.getRefId(ref);
				refTranslatorsURL = Conversion.replaceParameter(global.BASE_URL_REF_LIST, params2);
				params3[1] = this.getRefId(ref);
				refAuthorsURL = Conversion.replaceParameter(global.BASE_URL_REF_LIST, params3);
    		}
        	if ((listOfAuthorsRef != null) && (listOfAuthorsRef.size() != 0)) {
        		for(Agent agent: listOfAuthorsRef) {
    				if (agent instanceof PersonE) {
    					((PersonE)agent).addPublications(model, publicationLink);
    				}
    			}  
        		ListOfAuthorsE loaRef = null;
        		if (type != ReferenceType.CONFERENCE_PROCS) { // authors belongs to paper rather than proceedings
        			if (clazz.equals(MixedCitation.class)) {
            			loaRef = new ListOfAuthorsE(model, refListURL, true); //model.createBlankNode(pmcID + "_listAuthorsAndEditorsAndTranslatorsReference_" + this.getRefId(ref))
            		} else {
            			loaRef = new ListOfAuthorsE(model, refAuthorsURL, true); //model.createBlankNode(pmcID + "_listAuthorsReference_" + this.getRefId(ref))        			    
            		}    	    				        
            		loaRef.addMembersInOrder(model, listOfAuthorsRef); //add members to list
        		    docReference.addbiboListofauthors(loaRef); //add list to document
        		}        		
        	}
        	if ((listOfEditorsRef != null) && (listOfEditorsRef.size() != 0)) {
        		for(Agent agent: listOfEditorsRef) {
    				if (agent instanceof PersonE) {
    					((PersonE)agent).addPublications(model, publicationLink);
    				}
    			} 
        		ListOfAuthorsE loaRef = new ListOfAuthorsE(model, refEditorsURL, true); //model.createBlankNode(pmcID + "_listEditorsReference_" + this.getRefId(ref))
    		    loaRef.addMembersInOrder(model, listOfAuthorsRef); //add members to list
    		    docReference.addbiboListofeditors(loaRef); //add list to document		        	
        	}
        	if ((listOfTranslatorsRef != null) && (listOfTranslatorsRef.size() != 0)) {
        		for(Agent agent: listOfTranslatorsRef) {
    				if (agent instanceof PersonE) {
    					((PersonE)agent).addPublications(model, publicationLink);
    				}
    			}
        		ListOfAuthorsE loaRef = new ListOfAuthorsE(model, refTranslatorsURL, true); //model.createBlankNode(pmcID + "_listlistOfTranslatorsReference_" + this.getRefId(ref))
    		    loaRef.addMembersInOrder(model, listOfAuthorsRef); //add members to list
    		    docReference.addbiboListofeditors(loaRef); //add list to document		        	
        	}
        	//References of the document
    	    document.addbiboCites(docReference);
    	    docReference.addbiboCitedby(document);
		} catch (Exception e ) {}    		
	}
	/**
	 * Processes an Citation type and creates the bibliographic reference.
	 * @param citation
	 * @param ref
	 */
	private void processSimpleCitation(Model model, Document document, Citation citation, Ref ref) {		
		try {	    	
	    	if (citation.getPublicationType().contains("book")) {
	    		processReferenceAllTypeCitation(model, document, ref, citation.getAll(), ReferenceType.BOOK, Citation.class);
	    	} else if (citation.getPublicationType().contains("journal")) {
	    		processReferenceAllTypeCitation(model, document, ref, citation.getAll(), ReferenceType.JOURNAL_ARTICLE, Citation.class);
	    	} else if (citation.getPublicationType().contains("confproc")) {
	    		processReferenceAllTypeCitation(model, document, ref, citation.getAll(), ReferenceType.CONFERENCE_PROCS, Citation.class);
	    	} else if (citation.getPublicationType().contains("thesis")) {
	    		processReferenceAllTypeCitation(model, document, ref, citation.getAll(), ReferenceType.THESIS, Citation.class);
	    	} else if (citation.getPublicationType().contains("report")) {
	    		processReferenceAllTypeCitation(model, document, ref, citation.getAll(), ReferenceType.REPORT, Citation.class);
	    	} else if (citation.getPublicationType().contains("gov")) {
	    		processReferenceAllTypeCitation(model, document, ref, citation.getAll(), ReferenceType.GOV, Citation.class);
	    	} else if (citation.getPublicationType().contains("patent")) {
	    		processReferenceAllTypeCitation(model, document, ref, citation.getAll(), ReferenceType.PATENT, Citation.class);
	    	} else if (citation.getPublicationType().contains("standard")) {
	    		processReferenceAllTypeCitation(model, document, ref, citation.getAll(), ReferenceType.STANDARD, Citation.class);
	    	} else if (citation.getPublicationType().contains("web")) {
	    		processReferenceAllTypeCitation(model, document, ref, citation.getAll(), ReferenceType.WEBPAGE, Citation.class);
	    	} else if (citation.getPublicationType().contains("discussion")) {
	    		processReferenceAllTypeCitation(model, document, ref, citation.getAll(), ReferenceType.DISCUSSION, Citation.class);
	    	} else if (citation.getPublicationType().contains("list")) {
	    		processReferenceAllTypeCitation(model, document, ref, citation.getAll(), ReferenceType.DISCUSSION, Citation.class);
	    	} else if (citation.getPublicationType().contains("commun")) {
	    		processReferenceAllTypeCitation(model, document, ref, citation.getAll(), ReferenceType.COMMUN, Citation.class);
	    	} else if (citation.getPublicationType().contains("blog")) {
	    		processReferenceAllTypeCitation(model, document, ref, citation.getAll(), ReferenceType.BLOG, Citation.class);
	    	} else if (citation.getPublicationType().contains("wiki")) {
	    		processReferenceAllTypeCitation(model, document, ref, citation.getAll(), ReferenceType.WIKI, Citation.class);
	    	} else if (citation.getPublicationType().contains("database")) {
	    		processReferenceAllTypeCitation(model, document, ref, citation.getAll(), ReferenceType.DATABASE, Citation.class);
	    	} else if (citation.getPublicationType().equals("other")) {
	    		processReferenceAllTypeCitation(model, document, ref, citation.getAll(), ReferenceType.OTHER, Citation.class);	    		
	    	} else if (citation.getPublicationType().equals("display-unstructured")) {
	    		;	    		
	    	} else {
	    		logger.warn("WARNING - reference " + this.getRefId(ref) + " could not be processed (type not recognized)");		    
	    	}
    	} catch (Exception e) {
    		try {
    			if (citation.getCitationType().contains("book")) {
    	    		processReferenceAllTypeCitation(model, document, ref, citation.getAll(), ReferenceType.BOOK, Citation.class);
    	    	} else if (citation.getCitationType().contains("journal")) {
    	    		processReferenceAllTypeCitation(model, document, ref, citation.getAll(), ReferenceType.JOURNAL_ARTICLE, Citation.class);
    	    	} else if (citation.getCitationType().contains("confproc")) {
    	    		processReferenceAllTypeCitation(model, document, ref, citation.getAll(), ReferenceType.CONFERENCE_PROCS, Citation.class);
    	    	} else if (citation.getCitationType().contains("thesis")) {
    	    		processReferenceAllTypeCitation(model, document, ref, citation.getAll(), ReferenceType.THESIS, Citation.class);
    	    	} else if (citation.getCitationType().contains("report")) {
    	    		processReferenceAllTypeCitation(model, document, ref, citation.getAll(), ReferenceType.REPORT, Citation.class);
    	    	} else if (citation.getCitationType().contains("gov")) {
    	    		processReferenceAllTypeCitation(model, document, ref, citation.getAll(), ReferenceType.GOV, Citation.class);
    	    	} else if (citation.getCitationType().contains("patent")) {
    	    		processReferenceAllTypeCitation(model, document, ref, citation.getAll(), ReferenceType.PATENT, Citation.class);
    	    	} else if (citation.getCitationType().contains("standard")) {
    	    		processReferenceAllTypeCitation(model, document, ref, citation.getAll(), ReferenceType.STANDARD, Citation.class);
    	    	} else if (citation.getCitationType().contains("web")) {
    	    		processReferenceAllTypeCitation(model, document, ref, citation.getAll(), ReferenceType.WEBPAGE, Citation.class);
    	    	} else if (citation.getCitationType().contains("discussion")) {
    	    		processReferenceAllTypeCitation(model, document, ref, citation.getAll(), ReferenceType.DISCUSSION, Citation.class);
    	    	} else if (citation.getCitationType().contains("list")) {
    	    		processReferenceAllTypeCitation(model, document, ref, citation.getAll(), ReferenceType.DISCUSSION, Citation.class);
    	    	} else if (citation.getCitationType().contains("commun")) {
    	    		processReferenceAllTypeCitation(model, document, ref, citation.getAll(), ReferenceType.COMMUN, Citation.class);
    	    	} else if (citation.getCitationType().contains("blog")) {
    	    		processReferenceAllTypeCitation(model, document, ref, citation.getAll(), ReferenceType.BLOG, Citation.class);
    	    	} else if (citation.getCitationType().contains("wiki")) {
    	    		processReferenceAllTypeCitation(model, document, ref, citation.getAll(), ReferenceType.WIKI, Citation.class);
    	    	} else if (citation.getCitationType().contains("database")) {
    	    		processReferenceAllTypeCitation(model, document, ref, citation.getAll(), ReferenceType.DATABASE, Citation.class);
    	    	} else if (citation.getCitationType().equals("other")) {
    	    		processReferenceAllTypeCitation(model, document, ref, citation.getAll(), ReferenceType.OTHER, Citation.class);	    		
    	    	} else if (citation.getCitationType().equals("display-unstructured")) {
    	    		;	    		
    	    	} else {
    	    		logger.warn("WARNING - reference " + this.getRefId(ref) + " could not be processed (type not recognized)");		    
    	    	}
    		} catch (Exception ex) {
        		logger.warn("WARNING - reference " + this.getRefId(ref) + " could not be processed: " + ex.getMessage());
    		}    		
    	}
	}
	/**
	 * Processes an NlmCitation type and creates the bibliographic reference.
	 * @param citation
	 * @param ref
	 */
	private void processNlmCitation(Model model, Document document, NlmCitation citation, Ref ref) {
		try {	    	
	    	if (citation.getPublicationType().contains("book")) {
	    		processReferenceAllTypeCitation(model, document, ref, citation.getAll(), ReferenceType.BOOK, NlmCitation.class);
	    	} else if (citation.getPublicationType().contains("journal")) {
	    		processReferenceAllTypeCitation(model, document, ref, citation.getAll(), ReferenceType.JOURNAL_ARTICLE, NlmCitation.class);
	    	} else if (citation.getPublicationType().contains("confproc")) {
	    		processReferenceAllTypeCitation(model, document, ref, citation.getAll(), ReferenceType.CONFERENCE_PROCS, NlmCitation.class);
	    	} else if (citation.getPublicationType().contains("thesis")) {
	    		processReferenceAllTypeCitation(model, document, ref, citation.getAll(), ReferenceType.THESIS, NlmCitation.class);
	    	} else if (citation.getPublicationType().contains("report")) {
	    		processReferenceAllTypeCitation(model, document, ref, citation.getAll(), ReferenceType.REPORT, NlmCitation.class);
	    	} else if (citation.getPublicationType().contains("gov")) {
	    		processReferenceAllTypeCitation(model, document, ref, citation.getAll(), ReferenceType.GOV, NlmCitation.class);
	    	} else if (citation.getPublicationType().contains("patent")) {
	    		processReferenceAllTypeCitation(model, document, ref, citation.getAll(), ReferenceType.PATENT, NlmCitation.class);
	    	} else if (citation.getPublicationType().contains("standard")) {
	    		processReferenceAllTypeCitation(model, document, ref, citation.getAll(), ReferenceType.STANDARD, NlmCitation.class);
	    	} else if (citation.getPublicationType().contains("web")) {
	    		processReferenceAllTypeCitation(model, document, ref, citation.getAll(), ReferenceType.WEBPAGE, NlmCitation.class);
	    	} else if (citation.getPublicationType().contains("discussion")) {
	    		processReferenceAllTypeCitation(model, document, ref, citation.getAll(), ReferenceType.DISCUSSION, NlmCitation.class);
	    	} else if (citation.getPublicationType().contains("list")) {
	    		processReferenceAllTypeCitation(model, document, ref, citation.getAll(), ReferenceType.DISCUSSION, NlmCitation.class);
	    	} else if (citation.getPublicationType().contains("commun")) {
	    		processReferenceAllTypeCitation(model, document, ref, citation.getAll(), ReferenceType.COMMUN, NlmCitation.class);
	    	} else if (citation.getPublicationType().contains("blog")) {
	    		processReferenceAllTypeCitation(model, document, ref, citation.getAll(), ReferenceType.BLOG, NlmCitation.class);
	    	} else if (citation.getPublicationType().contains("wiki")) {
	    		processReferenceAllTypeCitation(model, document, ref, citation.getAll(), ReferenceType.WIKI, NlmCitation.class);
	    	} else if (citation.getPublicationType().contains("database")) {
	    		processReferenceAllTypeCitation(model, document, ref, citation.getAll(), ReferenceType.DATABASE, NlmCitation.class);
	    	} else if (citation.getPublicationType().equals("other")) {
	    		processReferenceAllTypeCitation(model, document, ref, citation.getAll(), ReferenceType.OTHER, NlmCitation.class);	    		
	    	} else {
	    		logger.warn("WARNING - reference " + this.getRefId(ref) + " could not be processed (type not recognized)");		    
	    	}
    	} catch (Exception e) {
    		logger.warn("WARNING - reference " + this.getRefId(ref) + " could not be processed: " + e.getMessage());
    	}
	}
	/**
	 * Processes a MixedCitation type and creates the bibliographic reference.
	 * @param citation
	 * @param ref
	 */
	private void processMixedCitation(Model model, Document document, MixedCitation citation, Ref ref) {
		try {	    	
	    	if (citation.getPublicationType().contains("book")) {
	    		processReferenceAllTypeCitation(model, document, ref, citation.getContent(), ReferenceType.BOOK, MixedCitation.class);
	    	} else if (citation.getPublicationType().contains("journal")) {
	    		processReferenceAllTypeCitation(model, document, ref, citation.getContent(), ReferenceType.JOURNAL_ARTICLE, MixedCitation.class);
	    	} else if (citation.getPublicationType().contains("confproc")) {
	    		processReferenceAllTypeCitation(model, document, ref, citation.getContent(), ReferenceType.CONFERENCE_PROCS, MixedCitation.class);
	    	} else if (citation.getPublicationType().contains("thesis")) {
	    		processReferenceAllTypeCitation(model, document, ref, citation.getContent(), ReferenceType.THESIS, MixedCitation.class);
	    	} else if (citation.getPublicationType().contains("report")) {
	    		processReferenceAllTypeCitation(model, document, ref, citation.getContent(), ReferenceType.REPORT, MixedCitation.class);
	    	} else if (citation.getPublicationType().contains("gov")) {
	    		processReferenceAllTypeCitation(model, document, ref, citation.getContent(), ReferenceType.GOV, MixedCitation.class);
	    	} else if (citation.getPublicationType().contains("patent")) {
	    		processReferenceAllTypeCitation(model, document, ref, citation.getContent(), ReferenceType.PATENT, MixedCitation.class);
	    	} else if (citation.getPublicationType().contains("standard")) {
	    		processReferenceAllTypeCitation(model, document, ref, citation.getContent(), ReferenceType.STANDARD, MixedCitation.class);
	    	} else if (citation.getPublicationType().contains("web")) {
	    		processReferenceAllTypeCitation(model, document, ref, citation.getContent(), ReferenceType.WEBPAGE, MixedCitation.class);
	    	} else if (citation.getPublicationType().contains("discussion")) {
	    		processReferenceAllTypeCitation(model, document, ref, citation.getContent(), ReferenceType.DISCUSSION, MixedCitation.class);
	    	} else if (citation.getPublicationType().contains("list")) {
	    		processReferenceAllTypeCitation(model, document, ref, citation.getContent(), ReferenceType.DISCUSSION, MixedCitation.class);
	    	} else if (citation.getPublicationType().contains("commun")) {
	    		processReferenceAllTypeCitation(model, document, ref, citation.getContent(), ReferenceType.COMMUN, MixedCitation.class);
	    	} else if (citation.getPublicationType().contains("blog")) {
	    		processReferenceAllTypeCitation(model, document, ref, citation.getContent(), ReferenceType.BLOG, MixedCitation.class);
	    	} else if (citation.getPublicationType().contains("wiki")) {
	    		processReferenceAllTypeCitation(model, document, ref, citation.getContent(), ReferenceType.WIKI, MixedCitation.class);
	    	} else if (citation.getPublicationType().contains("database")) {
	    		processReferenceAllTypeCitation(model, document, ref, citation.getContent(), ReferenceType.DATABASE, MixedCitation.class);
	    	} else if (citation.getPublicationType().equals("other")) {
	    		processReferenceAllTypeCitation(model, document, ref, citation.getContent(), ReferenceType.OTHER, MixedCitation.class);	    		
	    	} else {
	    		logger.warn("WARNING - reference " + this.getRefId(ref) + " could not be processed (type not recognized)");		    
	    	}
    	} catch (Exception e) {
    		logger.warn("WARNING - reference " + this.getRefId(ref) + " could not be processed: " + e.getMessage());
    	}
	}
	/**
	 * Processes a ElementCitation type and creates the bibliographic reference.
	 * @param citation
	 * @param ref
	 */
	private void processElementCitation(Model model, Document document, ElementCitation citation, Ref ref) {
		try {	    	
			citation.getInlineSupplementaryMaterialsAndRelatedArticlesAndRelatedObjects();
	    	if (citation.getPublicationType().contains("book")) {
	    		processReferenceAllTypeCitation(model, document, ref, citation.getInlineSupplementaryMaterialsAndRelatedArticlesAndRelatedObjects(), ReferenceType.BOOK, ElementCitation.class);
	    	} else if (citation.getPublicationType().contains("journal")) {
	    		processReferenceAllTypeCitation(model, document, ref, citation.getInlineSupplementaryMaterialsAndRelatedArticlesAndRelatedObjects(), ReferenceType.JOURNAL_ARTICLE, ElementCitation.class);
	    	} else if (citation.getPublicationType().contains("confproc")) {
	    		processReferenceAllTypeCitation(model, document, ref, citation.getInlineSupplementaryMaterialsAndRelatedArticlesAndRelatedObjects(), ReferenceType.CONFERENCE_PROCS, ElementCitation.class);
	    	} else if (citation.getPublicationType().contains("thesis")) {
	    		processReferenceAllTypeCitation(model, document, ref, citation.getInlineSupplementaryMaterialsAndRelatedArticlesAndRelatedObjects(), ReferenceType.THESIS, ElementCitation.class);
	    	} else if (citation.getPublicationType().contains("report")) {
	    		processReferenceAllTypeCitation(model, document, ref, citation.getInlineSupplementaryMaterialsAndRelatedArticlesAndRelatedObjects(), ReferenceType.REPORT, ElementCitation.class);
	    	} else if (citation.getPublicationType().contains("gov")) {
	    		processReferenceAllTypeCitation(model, document, ref, citation.getInlineSupplementaryMaterialsAndRelatedArticlesAndRelatedObjects(), ReferenceType.GOV, ElementCitation.class);
	    	} else if (citation.getPublicationType().contains("patent")) {
	    		processReferenceAllTypeCitation(model, document, ref, citation.getInlineSupplementaryMaterialsAndRelatedArticlesAndRelatedObjects(), ReferenceType.PATENT, ElementCitation.class);
	    	} else if (citation.getPublicationType().contains("standard")) {
	    		processReferenceAllTypeCitation(model, document, ref, citation.getInlineSupplementaryMaterialsAndRelatedArticlesAndRelatedObjects(), ReferenceType.STANDARD, ElementCitation.class);
	    	} else if (citation.getPublicationType().contains("web")) {
	    		processReferenceAllTypeCitation(model, document, ref, citation.getInlineSupplementaryMaterialsAndRelatedArticlesAndRelatedObjects(), ReferenceType.WEBPAGE, ElementCitation.class);
	    	} else if (citation.getPublicationType().contains("discussion")) {
	    		processReferenceAllTypeCitation(model, document, ref, citation.getInlineSupplementaryMaterialsAndRelatedArticlesAndRelatedObjects(), ReferenceType.DISCUSSION, ElementCitation.class);
	    	} else if (citation.getPublicationType().contains("list")) {
	    		processReferenceAllTypeCitation(model, document, ref, citation.getInlineSupplementaryMaterialsAndRelatedArticlesAndRelatedObjects(), ReferenceType.DISCUSSION, ElementCitation.class);
	    	} else if (citation.getPublicationType().contains("commun")) {
	    		processReferenceAllTypeCitation(model, document, ref, citation.getInlineSupplementaryMaterialsAndRelatedArticlesAndRelatedObjects(), ReferenceType.COMMUN, ElementCitation.class);
	    	} else if (citation.getPublicationType().contains("blog")) {
	    		processReferenceAllTypeCitation(model, document, ref, citation.getInlineSupplementaryMaterialsAndRelatedArticlesAndRelatedObjects(), ReferenceType.BLOG, ElementCitation.class);
	    	} else if (citation.getPublicationType().contains("wiki")) {
	    		processReferenceAllTypeCitation(model, document, ref, citation.getInlineSupplementaryMaterialsAndRelatedArticlesAndRelatedObjects(), ReferenceType.WIKI, ElementCitation.class);
	    	} else if (citation.getPublicationType().contains("database")) {
	    		processReferenceAllTypeCitation(model, document, ref, citation.getInlineSupplementaryMaterialsAndRelatedArticlesAndRelatedObjects(), ReferenceType.DATABASE, ElementCitation.class);
	    	} else if (citation.getPublicationType().equals("other")) {
	    		processReferenceAllTypeCitation(model, document, ref, citation.getInlineSupplementaryMaterialsAndRelatedArticlesAndRelatedObjects(), ReferenceType.OTHER, ElementCitation.class);	    		
	    	} else {
	    		logger.warn("WARNING - reference " + this.getRefId(ref) + " could not be processed (type not recognized)");		    
	    	}
    	} catch (Exception e) {
    		logger.warn("WARNING - reference " + this.getRefId(ref) + " could not be processed: " + e.getMessage());
    	}
	}
	/**
	 * Processes an element, it identifies and links to cross-refs as well as dismisses italics, bolds, etc. 
	 * @param obj
	 * @return
	 */
	@SuppressWarnings("rawtypes")
	private String processElement(Model model, Document document, Object obj, SectionE secDoco, String secDocoURI) {
		if (obj instanceof NamedContent) {
			try {
				String str = ((NamedContent)obj).getContent().get(0).toString();
				if (str.startsWith("pubmed.openAccess.jaxb.generated")) {
					return processListOfElements(model, document,  ((NamedContent)obj).getContent(), secDoco, secDocoURI );
				} else {
					return str;
				}
			} catch (Exception e) {
				return "";
			}	
		}
		if (obj instanceof Sc) {
			try {
				String str = ((Sc)obj).getContent().get(0).toString();
				if (str.startsWith("pubmed.openAccess.jaxb.generated")) {
					return processListOfElements(model, document,  ((Sc)obj).getContent(), secDoco, secDocoURI );
				} else {
					return str;
				}
			} catch (Exception e) {
				return "";
			}	
		} else if (obj instanceof Xref) {
			Xref internal = (Xref) obj;
			if (internal.getRid().startsWith("Fig") || internal.getRid().startsWith("fig")) {
				return "[" + GlobalArticleConfig.pmcURI + pmcID + "/figure/" + internal.getRid() + "]";
			} else if (internal.getRid().startsWith("Tab") || internal.getRid().startsWith("Tab")) {
				return "[" + GlobalArticleConfig.pmcURI + pmcID + "/table/" + internal.getRid() + "]";
			} else {
				return ("[" + internal.getRid() + "]");
			}
		} else if (obj instanceof Bold) {
			try {
				String str = ((Bold)obj).getContent().get(0).toString();
				if (str.startsWith("pubmed.openAccess.jaxb.generated")) {
					return processListOfElements(model, document,  ((Bold)obj).getContent(), secDoco, secDocoURI );
				} else {
					return str;
				}
			} catch (Exception e) {
				return "";
			}			
		} else if (obj instanceof Italic) {
			try {
				String str = ((Italic)obj).getContent().get(0).toString();
				if (str.startsWith("pubmed.openAccess.jaxb.generated")) {
					return processListOfElements(model, document,  ((Italic)obj).getContent(), secDoco, secDocoURI );
				} else {
					return str;
				}
			} catch (Exception e) {
				return "";
			}			
		} else if (obj instanceof Underline) {
			try {
				String str = ((Underline)obj).getContent().get(0).toString();
				if (str.startsWith("pubmed.openAccess.jaxb.generated")) {
					return processListOfElements(model, document,  ((Underline)obj).getContent(), secDoco, secDocoURI );
				} else {
					return str;
				}
			} catch (Exception e) {
				return "";
			}			
		} else if (obj instanceof ExtLink) {
			try {
				String str = ((ExtLink)obj).getContent().get(0).toString();
				if (str.startsWith("http")) {
					document.addDCReferences(model, str);
				}
				return str;
			} catch (Exception e) {
				return "";
			}			
		} else if (obj instanceof pubmed.openAccess.jaxb.generated.List) {
			pubmed.openAccess.jaxb.generated.List list = (pubmed.openAccess.jaxb.generated.List)obj;
			String str = "";
			for (Object object: list.getListItemsAndXS()) {
				if (object instanceof ListItem) {
					ListItem listItem = (ListItem)object;
					for (Object item: listItem.getPSAndDefListsAndLists()) {
						str += "(*) " + processElement(model, document, item, secDoco, secDocoURI) + ". "; 
					}
				}
			}
			return (str);
		} else if (obj instanceof pubmed.openAccess.jaxb.generated.P) {
			pubmed.openAccess.jaxb.generated.P paragraph = (pubmed.openAccess.jaxb.generated.P)obj;
			String str = "";
			for (Object p: paragraph.getContent()) {
				str += processElement(model, document, p, secDoco, secDocoURI);
			}
			return str;
		} else if (obj instanceof Sup) {
			Sup sup = (Sup)obj;
			String str = "";
			for (Object object: sup.getContent()) {
				str += processElement(model, document, object, secDoco, secDocoURI);
			}
			return str;			
		} else if (obj instanceof Sub) {
			Sub sub = (Sub)obj;
			String str = "";
			for (Object object: sub.getContent()) {
				str += processElement(model, document, object, secDoco, secDocoURI);
			}
			return str;			
		} else if ((obj instanceof Fig) || (obj instanceof TableWrap)) {
			return "";
		} else if (obj instanceof ChemStruct) {			
			ChemStruct chem = (ChemStruct)obj; 	
			try {
				Graphic g = (Graphic)(chem.getContent().get(0));
				String link = global.CHEM_STRUCT_FIG_LINK + g.getHref();
				Document image = new Document(model, link, true);
			    model.addStatement(image.asResource(), Thing.RDF_TYPE, Figure.RDFS_CLASS); //rdf:type Document
				secDoco.addDCReferences(model, link);
				document.addDCReferences(model, link);
				image.addDCIsReferencedBy(model, secDocoURI);
				image.addDCIsReferencedBy(model, basePaper);
				return "(see also " + link + ")";
			} catch (Exception e) {
				return "";
			}
		} else if (obj instanceof InlineFormula) {
			InlineFormula formula = (InlineFormula)obj;	
			try {
				InlineGraphic g = (InlineGraphic)(formula.getContent().get(0));
				String link = global.INLINE_FORM_FIG_LINK + g.getHref();
				Document image = new Document(model, link, true);
			    model.addStatement(image.asResource(), Thing.RDF_TYPE, Figure.RDFS_CLASS); //rdf:type Document
				secDoco.addDCReferences(model, link);
				document.addDCReferences(model, link);
				image.addDCIsReferencedBy(model, secDocoURI);
				image.addDCIsReferencedBy(model, basePaper);
				return " (see also " + link + ")";
			} catch (Exception e) {
				return "";
			}
		} else if (obj instanceof Graphic) {
			Graphic graphic = (Graphic)obj;
			if (graphic.getHref() != null) {
				return graphic.getHref();
			}
			return "";
		} else if (obj instanceof InlineGraphic) {
			try {
				InlineGraphic g = (InlineGraphic)(obj);
				String link = global.INLINE_FORM_FIG_LINK + g.getHref();
				Document image = new Document(model, link, true);
			    model.addStatement(image.asResource(), Thing.RDF_TYPE, Figure.RDFS_CLASS); //rdf:type Document
				secDoco.addDCReferences(model, link);
				document.addDCReferences(model, link);
				image.addDCIsReferencedBy(model, secDocoURI);
				image.addDCIsReferencedBy(model, basePaper);
				return " (see also " + link + ")";
			} catch (Exception e) {
				return "";
			}
		} else if (obj instanceof JAXBElement<?>) {
			//System.out.println("JAXB: " + obj.getClass());
			JAXBElement<?> elem = (JAXBElement<?>)obj;
			if (elem.getValue() instanceof String) {
				return elem.getValue().toString();
			} else if (elem.getValue() instanceof List<?>){
				String str = "";
				List<?> lst = (List<?>) elem;
				for (Object elemLst: lst){
					if (elemLst instanceof String) {
						str += elemLst.toString();
					} else {
						str += processElement(model, document, (JAXBElement<?>)elemLst, secDoco, secDocoURI);
					}
				}
				return (str);
			} else if (elem.getValue() instanceof JAXBElement){
				JAXBElement internal = (JAXBElement) elem; 
				return (internal.getValue().toString());
			} else {
				return (elem.toString());
			}
		} else {
			if (obj.toString().startsWith("pubmed.openAccess.jaxb.generated")) {
				logger.warn("- WARNING - Element cannot be processed as String " + obj.toString() + " at " + secDocoURI);
				return ("");
			}
			String str;
			try {
				str = URLDecoder.decode(obj.toString(), "UTF-8");
			} catch (Exception e) {
				str = obj.toString();
			}
			return (str);
		}
	}
	/**
	 * Processes a list of objects.
	 * @param list
	 * @return
	 */
	private String processListOfElements(Model model, Document document, List<Object> list, SectionE secDoco, String secDocoURI){
		String str = "";
		for (Object obj: list) {
			str += processElement(model, document, obj, secDoco, secDocoURI);
		}
		return str;
	}
	
	private String getAbstractText(Model model, Document document, Abstract ab) {
		//process paragraphs
		Iterator<Object> itrPara = ab.getAddressesAndAlternativesAndArraies().iterator();		
		String text = "";
		if (itrPara.hasNext()){
			Object obj = itrPara.next();
			if (obj instanceof P) {				
				text += processParagraphInAbstractAsText(model, document, (P)obj);				
			} 	    	
	    }
		Iterator<Sec> itrSec = ab.getSecs().iterator();
		while (itrSec.hasNext()) {
			Sec sec = itrSec.next();
			try {
				text += sec.getTitle().getContent().get(0).toString() + ": ";
				Iterator<Object> itrParaSec = sec.getAddressesAndAlternativesAndArraies().iterator();		
				if (itrParaSec.hasNext()){
					Object obj = itrParaSec.next();
					if (obj instanceof P) {				
						text += processParagraphInAbstractAsText(model, document, (P)obj) + " ";				
					} 	    	
			    }
			} catch (Exception e) {;}
		}
		return text;
	}
	/**
	 * Processes a paragraph within the abstract.
	 * @param secDoco Abstract as section
	 * @param secDocoURI Abstract URI
	 * @param para Paragraph to be processed
	 * @return
	 */
	private String processParagraphInAbstractAsText(Model model, Document document, P para) {
		String text = "";
		//text
		for (Object paraObj: para.getContent()) {
			String str = processElement(model, document, paraObj, null, null); 			
			text += str;			
		}		
		return text;
	}
	/**
	 * Processes the abstract as a section.
	 * @param ab
	 * @param model
	 * @param document
	 * @param parent
	 */
	private String processAbstractAsSection(Abstract ab, Model model, Document document, SectionE parent) {
		//Title
		String title = "Abstract";
		//add section
		String secDocoURI = global.BASE_URL_SECTION + title;
		SectionE secDoco = new SectionE(model, secDocoURI, true);
		document.addSection(model, secDoco);		
		//title
		Node titleNode = new PlainLiteralImpl(title);
		secDoco.adddocoTitle(titleNode);
		//link to parent
		if (parent != null) {
			parent.addSection(model, secDoco);
		}
		//process paragraphs
		Iterator<Object> itrPara = ab.getAddressesAndAlternativesAndArraies().iterator();		
		String text = "";
		if (itrPara.hasNext()){
			Object obj = itrPara.next();
			if (obj instanceof P) {				
				text += processParagraphInAbstract(model, document, secDoco, secDocoURI, (P)obj);				
			} 	    	
	    }
		Iterator<Sec> itrSec = ab.getSecs().iterator();
		while (itrSec.hasNext()) {
			Sec sec = itrSec.next();
			try {
				text += sec.getTitle().getContent().get(0).toString() + ": ";
				Iterator<Object> itrParaSec = sec.getAddressesAndAlternativesAndArraies().iterator();		
				if (itrParaSec.hasNext()){
					Object obj = itrParaSec.next();
					if (obj instanceof P) {				
						text += processParagraphInAbstract(model, document, secDoco, secDocoURI, (P)obj) + " ";				
					} 	    	
			    }
			} catch (Exception e) {;}
		}
		//paragraph		
		String[] params = {title};
		String paragraphURI = Conversion.replaceParameter(global.BASE_URL_PARAGRAPH, params) + "1";
		ParagraphE paragraph = new ParagraphE(model, paragraphURI, true);
		secDoco.addParagraph(model, paragraph);		
		PlainLiteral textNode = model.createPlainLiteral(text);
	    model.addStatement(paragraph.asResource(), new URIImpl(PmcOpenAccessHelper.TEXT_PROPERTY, false), textNode); //text
		return text;
	}
	/**
	 * Processes a paragraph within the abstract.
	 * @param secDoco Abstract as section
	 * @param secDocoURI Abstract URI
	 * @param para Paragraph to be processed
	 * @return
	 */
	private String processParagraphInAbstract(Model model, Document document, SectionE secDoco, String secDocoURI, P para) {
		String text = "";
		//text
		List<String[]> references = new ArrayList<String[]>();
		for (Object paraObj: para.getContent()) {
			String str = processElement(model, document, paraObj, null, null); 
			if (str.startsWith("http")) {
				secDoco.addDCReferences(model, str);
			}
			text += str;
			if (paraObj instanceof Xref) {
				Xref internal = (Xref) paraObj;
				references.add(internal.getRid().split(" "));
			} 
		}
		//references
		for (String[] refs:references) {			
			for (String ref:refs) { 						
				if ( !( ref.startsWith("Fig") || ref.startsWith("Tab") || ref.startsWith("fig") || ref.startsWith("tab") ) ) {
					//Even if it is a pubmed or DOI reference, we still have the reference format as a document resource so we can link sections and paragraphs to it
					Document docReference = new Document(model, global.BASE_URL_REF + ref, true); //model.createBlankNode(pmcID + "_reference_" + pmcID + "_" + ref)
					secDoco.addbiboCites(docReference);
				} else if (ref.startsWith("Fig") || ref.startsWith("fig")){
					Document image = new Document(model, GlobalArticleConfig.pmcURI + pmcID + "/figure/" + ref, true);
				    model.addStatement(image.asResource(), Thing.RDF_TYPE, Figure.RDFS_CLASS); //rdf:type Document
					secDoco.addDCReferences(model, GlobalArticleConfig.pmcURI + pmcID + "/figure/" + ref);
					document.addDCReferences(model, GlobalArticleConfig.pmcURI + pmcID + "/figure/" + ref);
					image.addDCIsReferencedBy(model, secDocoURI);
					image.addDCIsReferencedBy(model, basePaper);
				} else if (ref.startsWith("Tab") || ref.startsWith("tab")) {
					Document table = new Document(model, GlobalArticleConfig.pmcURI + pmcID + "/table/" + ref, true);
				    model.addStatement(table.asResource(), Thing.RDF_TYPE, DocoTable.RDFS_CLASS); //rdf:type Document
					secDoco.addDCReferences(model, GlobalArticleConfig.pmcURI + pmcID + "/table/" + ref);
					document.addDCReferences(model, GlobalArticleConfig.pmcURI + pmcID + "/table/" + ref);
					table.addDCIsReferencedBy(model, secDocoURI);
					table.addDCIsReferencedBy(model, basePaper);
				}
			}
		}
		return text;
	}
	/**
	 * Processes one section.
	 * @param section
	 * @param model
	 * @param document
	 * @param parent
	 */
	private void processSection(Sec section, Model model, Document document, Object parent, String parentTitleInURL) {
		if ( (parent != null) && 
			 ( !((parent instanceof SectionE) || (parent instanceof Appendix)) ) ) {
			return;
		}
		//Title
		
		String title = "";
		try {
			title = processListOfElements(model, document, section.getTitle().getContent(), null, null);
		} catch (NullPointerException npe) {
			if (section.getSecType() == null) {
				title = section.getId() == null ? "id_" + (elementCount++): section.getId();				
			} else {
				title = section.getId() == null ? "id_" + (elementCount++): section.getId();
				if (!section.getSecType().equalsIgnoreCase("supplementary-material")) {
					title += section.getSecType() == null ? "secType_" + (elementCount++) : section.getSecType();
				} else {
					title = null;
				}
			}
		}
		if ((title == null) || (title.length() == 0)) {
			title = "no-title_" + (elementCount++);
		}
		String titleInURL = title.replaceAll(RDFHandler.CHAR_NOT_ALLOWED, "-");
		if (titleInURL.length() == 0) {
			title = section.getId();
			titleInURL = title.replaceAll(RDFHandler.CHAR_NOT_ALLOWED, "-");
		}
		//add section
		SectionE secDoco;
		if (parent == null) {
			secDoco = new SectionE(model, global.BASE_URL_SECTION + titleInURL, true);
		} else {
			secDoco = new SectionE(model, global.BASE_URL_SECTION + parentTitleInURL + "_" + titleInURL, true);
		}		 
		document.addSection(model, secDoco);
		//title
		Node titleNode = new PlainLiteralImpl(title);
		secDoco.adddocoTitle(titleNode);
		//link to parent
		if (parent != null) {
			if (parent instanceof SectionE) {
				((SectionE)parent).addSection(model, secDoco);
			} else if (parent instanceof Appendix) {
				((Appendix)parent).addSection(model, secDoco);
			}
			
		}
		//process paragraphs
		Iterator<Object> itrPara = section.getAddressesAndAlternativesAndArraies().iterator();	
		if (parent == null) {
			processElementsInSection(model, document, titleInURL, secDoco, itrPara);
		} else {
			processElementsInSection(model, document, parentTitleInURL + "_" + titleInURL, secDoco, itrPara);
		}
				
		//process sections
		Iterator<Sec> itr = section.getSecs().iterator();
		while (itr.hasNext()){
			Sec sec = itr.next();
			if (parent == null) {
				processSection(sec, model, document, secDoco, titleInURL);
			} else {
				processSection(sec, model, document, secDoco, parentTitleInURL + "_" + titleInURL);
			}
			
	    }
	}	
	
	private boolean processElementsInSection(Model model, Document document, String titleInURL, SectionE secDoco, Iterator<Object> itrPara) {
		int countPara = 0;
		boolean processed = false;
		while (itrPara.hasNext()){
			Object obj = itrPara.next();
			if (obj instanceof P) {
				countPara++;
				processParagraph(model, document, titleInURL, countPara, secDoco, (P)obj);
			} else if (obj instanceof SupplementaryMaterial) {
				SupplementaryMaterial supp = (SupplementaryMaterial)obj;
				for (Object o: supp.getAltTextsAndLongDescsAndEmails()) {
					if (o instanceof ExtLink) {
						try {
							String str = ((ExtLink)obj).getContent().get(0).toString();
							if (str.startsWith("http")) {
								processed = true;
								document.addDCReferences(model, str);
								secDoco.addDCReferences(model, str);
							}
						} catch (Exception e) {;}	
					}
				}
				for (Object o: supp.getDispFormulasAndDispFormulaGroupsAndChemStructWraps()) {
					if (o instanceof P) {
						countPara++;
						processParagraph(model, document, titleInURL, countPara, secDoco, (P)o);
					}
				}
			}	    	
	    }
		return ((countPara > 0) || (processed));
	}
	
	private void processParagraph(Model model, Document document, String titleInURL, int countPara, SectionE secDoco, P para) {
		String[] params = {titleInURL};
		String paragraphURI = Conversion.replaceParameter(global.BASE_URL_PARAGRAPH, params) + countPara;
		ParagraphE paragraph = new ParagraphE(model, paragraphURI, true);
		secDoco.addParagraph(model, paragraph);
					
		String text = "";
		//text
		List<String[]> references = new ArrayList<String[]>();
		for (Object paraObj: para.getContent()) {
			String str = processElement(model, document, paraObj, secDoco, global.BASE_URL_SECTION + titleInURL); 
			if (str.startsWith("http")) {
				paragraph.addDCReferences(model, str);
 				//this.document.addDCReferences(model, str); //already added when process the paragraph
			}
			text += str;
			if (paraObj instanceof Xref) {
				Xref internal = (Xref) paraObj;
				references.add(internal.getRid().split(" "));
			} 
		}
		//references
		for (String[] refs:references) {
			for (String ref:refs) {
				if ( !( ref.startsWith("Fig") || ref.startsWith("Tab") ) ){ 
					//Even if it is a pubmed or DOI reference, we still have the reference format as a document resource so we can link sections and paragraphs to it
					Document docReference = new Document(model, global.BASE_URL_REF + ref, true);
					paragraph.addbiboCites(docReference);
					docReference.addbiboCitedby(paragraph);
				} else if (ref.startsWith("Fig") || ref.startsWith("fig")) {
					Document image = new Document(model, global.BASE_URL_EXT_FIGURE + ref, true);
				    model.addStatement(image.asResource(), Thing.RDF_TYPE, Figure.RDFS_CLASS); //rdf:type Document
					paragraph.addDCReferences(model, global.BASE_URL_EXT_FIGURE + ref);
					document.addDCReferences(model, global.BASE_URL_EXT_FIGURE + ref);
					image.addDCIsReferencedBy(model, paragraphURI);
					image.addDCIsReferencedBy(model, basePaper);
				} else if (ref.startsWith("Tab") || ref.startsWith("tab")) {
					Document table = new Document(model, global.BASE_URL_EXT_TABLE + ref, true);
				    model.addStatement(table.asResource(), Thing.RDF_TYPE, DocoTable.RDFS_CLASS); //rdf:type Document
					paragraph.addDCReferences(model, global.BASE_URL_EXT_TABLE + ref);
					document.addDCReferences(model, global.BASE_URL_EXT_TABLE + ref);
					table.addDCIsReferencedBy(model, paragraphURI);
					table.addDCIsReferencedBy(model, basePaper);
				} 
			}
		}		
		PlainLiteral textNode = model.createPlainLiteral(text);
	    model.addStatement(paragraph.asResource(), new URIImpl(PmcOpenAccessHelper.TEXT_PROPERTY, false), textNode); //text		
	}
	/**
	 * Gets the reference id without non-allowed chars.
	 * @param ref
	 * @return
	 */
	private String getRefId(Ref ref) {
		String refId = null;
		if (ref.getId() == null) {
			try {
				refId = ref.getLabel().getContent().get(0).toString().replaceAll(RDFHandler.CHAR_NOT_ALLOWED, "-");
			} catch (Exception e) {
				refId = "NA";
			}
		} else {
			refId = ref.getId();
		}
		return refId;
	}	
}
