/**
 * 
 */
package ws.biotea.ld2rdf.rdfGeneration.pmcoa;  

import org.ontoware.rdf2go.RDF2Go;
import org.ontoware.rdf2go.model.Model;
import org.ontoware.rdf2go.model.node.Node;
import org.ontoware.rdf2go.model.node.PlainLiteral;
import org.ontoware.rdf2go.model.node.Resource;
import org.ontoware.rdf2go.model.node.impl.PlainLiteralImpl;
import org.ontoware.rdf2go.model.node.impl.URIImpl;
import pubmed.openAccess.jaxb.generated.*;
import pubmed.openAccess.jaxb.generated.Issue;
import ws.biotea.ld2rdf.rdf.model.bibo.*;
import ws.biotea.ld2rdf.rdf.model.bibo.Conference;
import ws.biotea.ld2rdf.rdf.model.bibo.extension.*;
import ws.biotea.ld2rdf.rdf.model.doco.Appendix;
import ws.biotea.ld2rdf.rdf.model.doco.DocoTable;
import ws.biotea.ld2rdf.rdf.model.doco.Figure;
import ws.biotea.ld2rdf.rdf.model.doco.extension.ParagraphE;
import ws.biotea.ld2rdf.rdf.model.doco.extension.SectionE;
import ws.biotea.ld2rdf.rdfGeneration.RDFHandler;
import ws.biotea.ld2rdf.rdfGeneration.exception.ArticleTypeException;
import ws.biotea.ld2rdf.rdfGeneration.exception.DTDException;
import ws.biotea.ld2rdf.rdfGeneration.exception.PMCIdException;
import ws.biotea.ld2rdf.rdfGeneration.jats.GlobalArticleConfig;
import ws.biotea.ld2rdf.util.ResourceConfig;
import ws.biotea.ld2rdf.util.ClassesAndProperties;
import ws.biotea.ld2rdf.util.Conversion;
import ws.biotea.ld2rdf.util.HtmlUtil;

import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

public class PmcOpenAccess2RDF extends PmcOpenAccess2AbstractRDF {
	public PmcOpenAccess2RDF(File paper, StringBuilder str, String suffix, String bioteaBase, String bioteaDataset, 
			boolean sections, boolean references) throws JAXBException, DTDException, ArticleTypeException, PMCIdException {
		super(paper, str, suffix, bioteaBase, bioteaDataset, sections, references);	
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
	public File paper2rdf(String outputDir, File paper) throws JAXBException, FileNotFoundException, UnsupportedEncodingException {	
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
			this.processReferences(model, document, this.references);
			logger.info("=== references processed");
		} catch (Exception e) {//something went so wrong
			logger.fatal("- FATAL ERROR - " + pmcID + " threw an uncaugth error: " + e.getMessage());
			fatalError = true;
		} finally {
			//close and write model
			if (fatalError) {
				//outputFile = serializeAndCloseModel(model, outputDir + "/" + PREFIX + pmcID + suffix + ".rdf");		
				logger.info("=== END of rdfization with a fatal error " + pmcID + " (pubmedID: " + pubmedID + "), (doi: " + doi + ")");
			} else {
				outputFile = serializeAndCloseModel(model, outputDir + "/" + PREFIX + pmcID + "_" + this.suffix + ".rdf");		
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
				documentSections.addTitle(modelSections, this.articleTitle);
				String type = this.articleType.replace('-', '_').toUpperCase();
				if (ArticleType.valueOf(type).getBiboType() != null) {
					modelSections.addStatement(documentSections.asResource(), Thing.RDF_TYPE, ArticleType.valueOf(type).getBiboTypeURI()); //rdf:type
				}
				
				for (Abstract ab: article.getFront().getArticleMeta().getAbstracts()) {
					processAbstractAsSection(ab, modelSections, documentSections);			
				}
				//process sections
				for (Sec section:article.getBody().getSecs()) {										
					processSection(section, modelSections, documentSections, null, null);
				}
				//process not-in-section-paragraphs			
				Iterator<Object> itrPara = article.getBody().getAddressesAndAlternativesAndArraies().iterator();
				if (itrPara.hasNext()) {
					SectionE secDoco = new SectionE(modelSections, global.BASE_URL_SECTION + "undefined-section", true);
					if (processElementsInSection(modelSections, documentSections, "undefined-section", secDoco, itrPara)) {
						documentSections.addSection(modelSections, secDoco, mainListOfSectionsURI);
					}								
				}
				logger.info("=== sections processed");
			} catch (Exception e) {//something went so wrong
				logger.fatal("- FATAL ERROR SECTIONS - " + pmcID + " threw an uncaugth error: " + e.getMessage());
				fatalErrorSections = true;
			} finally {
				if (fatalError || fatalErrorSections) {
					//close and write model
					//outputFileSections = serializeAndCloseModel(modelSections, outputDir + "/" + PREFIX + pmcID + suffix + "_sections.rdf");		
					logger.info("=== END of sections rdfization with a fatal error " + pmcID + " (pubmedID: " + pubmedID + "), (doi: " + doi + ")");
				} else {
					//close and write model
					outputFileSections = serializeAndCloseModel(modelSections, outputDir + "/" + PREFIX + pmcID + "_" + this.suffix + "_sections.rdf");		
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
	protected Model createAndOpenModel() {
		Model myModel = RDF2Go.getModelFactory().createModel();
		myModel.open();					
		myModel.setNamespace(ws.biotea.ld2rdf.util.OntologyPrefix.OWL.getNS(), ws.biotea.ld2rdf.util.OntologyPrefix.OWL.getURL());
		myModel.setNamespace(ws.biotea.ld2rdf.util.OntologyPrefix.RDF.getNS(), ws.biotea.ld2rdf.util.OntologyPrefix.RDF.getURL());
		myModel.setNamespace(ws.biotea.ld2rdf.util.OntologyPrefix.RDFS.getNS(), ws.biotea.ld2rdf.util.OntologyPrefix.RDFS.getURL());
		myModel.setNamespace(ws.biotea.ld2rdf.util.rdfization.OntologyRDFizationPrefix.BIBO.getNS(), ws.biotea.ld2rdf.util.rdfization.OntologyRDFizationPrefix.BIBO.getURL());
		myModel.setNamespace(ws.biotea.ld2rdf.util.OntologyPrefix.BIOTEA.getNS(), ws.biotea.ld2rdf.util.OntologyPrefix.BIOTEA.getURL());
		myModel.setNamespace(ws.biotea.ld2rdf.util.rdfization.OntologyRDFizationPrefix.DOCO.getNS(), ws.biotea.ld2rdf.util.rdfization.OntologyRDFizationPrefix.DOCO.getURL());
		myModel.setNamespace(ws.biotea.ld2rdf.util.OntologyPrefix.FOAF.getNS(), ws.biotea.ld2rdf.util.OntologyPrefix.FOAF.getURL());
		myModel.setNamespace(ws.biotea.ld2rdf.util.OntologyPrefix.DCTERMS.getNS(), ws.biotea.ld2rdf.util.OntologyPrefix.DCTERMS.getURL());
		myModel.setNamespace(ws.biotea.ld2rdf.util.OntologyPrefix.FOAF.getNS(), ws.biotea.ld2rdf.util.OntologyPrefix.FOAF.getURL());		
		myModel.setNamespace(ws.biotea.ld2rdf.util.OntologyPrefix.XSP.getNS(), ws.biotea.ld2rdf.util.OntologyPrefix.XSP.getURL());		
		myModel.setNamespace(ws.biotea.ld2rdf.util.OntologyPrefix.RDF.getNS(), ws.biotea.ld2rdf.util.OntologyPrefix.RDF.getURL());
		myModel.setNamespace(ws.biotea.ld2rdf.util.OntologyPrefix.PROV.getNS(), ws.biotea.ld2rdf.util.OntologyPrefix.PROV.getURL());
		myModel.setNamespace(ws.biotea.ld2rdf.util.OntologyPrefix.VOID.getNS(), ws.biotea.ld2rdf.util.OntologyPrefix.VOID.getURL());
		myModel.setNamespace(ws.biotea.ld2rdf.util.OntologyPrefix.SIO.getNS(), ws.biotea.ld2rdf.util.OntologyPrefix.SIO.getURL());
		myModel.setNamespace(ws.biotea.ld2rdf.util.OntologyPrefix.WIKI_DATA.getNS(), ws.biotea.ld2rdf.util.OntologyPrefix.WIKI_DATA.getURL());
		return (myModel);
	}
	
	/**
	 * Processes doi, pubmed, journal, and title.
	 */
	private void processBasic(Model model, Document document, String paper) {
		String articleId = pmcID;
		if (ResourceConfig.getIdTag().equals("pmc")) {
			articleId = pmcID;
			document.addIdentifier(model, "pmc:" + pmcID);	
			document.addProvWasDerivedFrom(model, GlobalArticleConfig.pmcURI + pmcID);
		} else if (ResourceConfig.getIdTag().equals("pmid")) {
			articleId = pubmedID;
			document.addIdentifier(model, "pmid:" + pubmedID);	
			document.addProvWasDerivedFrom(model, GlobalArticleConfig.pubMedURI + pubmedID);
		}
		System.out.println("FLAG 0 ");
		String[] sameAsLinks = ResourceConfig.getConfigSameAs(this.suffix);
		System.out.println("FLAG 1 " + sameAsLinks);
		if (sameAsLinks != null) {
			for (String sameAsLink:sameAsLinks) {
				System.out.println("FLAG 2 " + sameAsLink);
				String[] params = {articleId};
				document.addSameAs(model, Conversion.replaceParameter(sameAsLink, params));
			}
		}
		
		String now = HtmlUtil.getDateAndTime();
		document.addDCCreated(model, now);
		document.addProvGeneratedAt(model, now);
		document.addDCCreator(model, GlobalArticleConfig.RDF4PMC_AGENT);
		document.addProvWasAttributedTo(model, GlobalArticleConfig.RDF4PMC_AGENT);
		document.addInDataset(model, this.bioteaDataset);
		Resource resPMC = new URIImpl(GlobalArticleConfig.pmcURI + pmcID, true);	
		document.addSeeAlso(resPMC);	
		
		//pubmedID
		if (pubmedID != null) {
			document.addPMID(model, pubmedID);			
		    document.addIdentifier(model, "pmid:" + pubmedID);		
		    //Relations between PMC-RDF and identifiers.org/bio2rdf.org sameAS
		    if (ResourceConfig.withBio()) {
		    	document.addSameAs(model, ResourceConfig.IDENTIFIERS_ORG_PUBMED + pubmedID);
		    	document.addSameAs(model, ResourceConfig.LINKED_LIFE_DATA + pubmedID);
		    	Resource resIdOrgPubMed = new URIImpl(ResourceConfig.IDENTIFIERS_ORG_PAGE_PUBMED + pubmedID, true);
		    	document.addSeeAlso(resIdOrgPubMed);
			    document.addSameAs(model, ResourceConfig.BIO2RDF_PUBMED + pubmedID);
		    }		    
		    if (!global.getUriStyle().equals(ResourceConfig.bio2rdf)) {
		    	document.addSameAs(model, this.global.PUBMED_DOCUMENT + pubmedID);
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
		    	document.addSameAs(model, this.global.DOI_DOCUMENT + doi);
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
				journal = new JournalE(model, this.global.BASE_URL_JOURNAL_ISSN + journalTitleInURI, true); //model.createBlankNode(pmcID + "_journal_" + journalTitleInURI)
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
				journal = new JournalE(model, this.global.BASE_URL_JOURNAL_NAME + journalTitleInURI, true); //model.createBlankNode(pmcID + "_journal_" + journalTitleInURI)
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
        				String issueURI = Conversion.replaceParameter(this.global.BASE_URL_ISSUE, params);
            			issue = new ws.biotea.ld2rdf.rdf.model.bibo.Issue(model, issueURI + issueNumberInURI, true);    		
                		issue.addbiboIssue(issueNumber);
            		} else { //name:<journal_name>/<issue_id>
            			String[] params = {"name", journalTitleInURI};
        				String issueURI = Conversion.replaceParameter(this.global.BASE_URL_ISSUE, params);
            			issue = new ws.biotea.ld2rdf.rdf.model.bibo.Issue(model, issueURI + issueNumberInURI, true);    		
        			}
        			journal.addIssue(model, issue);
        			issue.addDocument(model, document);
        		} else {
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
				publisher = new OrganizationE(model, this.global.BASE_URL_PUBLISHER_ID + publisherPMCId, true ); 
				publisher.addName(model, publisherName);
				PlainLiteral id = model.createPlainLiteral(publisherPMCId);					    
			    model.addStatement(publisher.asResource(), Document.DCTERMS_IDENTIFIER, id); //id
			} else {//we create the publisher with the name
				publisher = new OrganizationE(model, this.global.BASE_URL_PUBLISHER_NAME + publisherName.replaceAll(RDFHandler.CHAR_NOT_ALLOWED, "-"), true ); 
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
    	    this.articleTitle = "";
    		for (Object ser: article.getFront().getArticleMeta().getTitleGroup().getArticleTitle().getContent()) {
    			if (ser instanceof String) {
    				this.articleTitle += ser.toString();
    			} else if (ser instanceof JAXBElement<?>) {
    				JAXBElement<?> elem = (JAXBElement<?>)ser;
    				this.articleTitle += processElement(model, document, elem, null, null);
    			}			
    		}
    	    document.addTitle(model, this.articleTitle);
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
		    Collection<org.ontoware.rdfreactor.schema.rdfs.Class> listOfAuthors = new ArrayList<org.ontoware.rdfreactor.schema.rdfs.Class>();
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
										person = (PersonE)createPerson(model, global.BASE_URL_PERSON_PMC, name, false);										
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

			String[] params = {"listOfAuthors", "1"};
		    ListOfElements loa = new ListOfElements(model, Conversion.replaceParameter(global.BASE_URL_LIST_PMC, params), true);
		    loa.addMembersInOrder(model, listOfAuthors); 
		    document.addbiboListofauthors(loa); 
		} catch (Exception e){
			logger.info(paper + ": Authors not processed.");
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
	private void processReferences(Model model, Document document, boolean withMetadata) {
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
	    						processSimpleCitation(model, document, citation, ref, withMetadata);
	    					} else if (objMix instanceof MixedCitation) {
	    						MixedCitation citation = (MixedCitation)objMix;
	    						//System.out.println("MIXED");
	    						processMixedCitation(model, document, citation, ref, withMetadata);
	    					} else if (objMix instanceof ElementCitation) {
	    						ElementCitation citation = (ElementCitation)objMix;
	    						//System.out.println("CITATION");
	    						processElementCitation(model, document, citation, ref, withMetadata);
	    					} else if (objMix instanceof NlmCitation) {
	    						NlmCitation citation = (NlmCitation)objMix;
	    						//System.out.println("NLM");
	    						processNlmCitation(model, document, citation, ref, withMetadata);
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
	private Document processReferenceCreateArticle(Model model, Document document, String url, String pubmed, String doiReference, Ref ref, ReferenceType type, boolean withMetadata) {		
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
			if (withMetadata) {
				docReference.addIdentifier(model, "pmid:" + pubmed);
				docReference.addPMID(model, pubmed);
			}			
			docReference.addSeeAlso(new URIImpl(GlobalArticleConfig.pubMedURI + pubmed)); //seeAlso for webpages
			docReference.addSameAs(model, ResourceConfig.IDENTIFIERS_ORG_PUBMED + pubmed); //sameAs for entities
			Resource resIdOrgPubMed = new URIImpl(ResourceConfig.IDENTIFIERS_ORG_PAGE_PUBMED + pubmed, true);
	    	docReference.addSeeAlso(resIdOrgPubMed);
			if (!url.equals(ResourceConfig.BIO2RDF_PUBMED + pubmed)) {
				docReference.addSameAs(model, ResourceConfig.BIO2RDF_PUBMED + pubmed); //sameAs for entities
			}			
			if (doiReference != null) {
				try {
					if (withMetadata) {
						docReference.addDOI(model, doiReference);
						docReference.addIdentifier(model, "doi:" + doiReference);
					}
					//docReference.addSeeAlso(new URIImpl(global.doiURI + doiReference)); //seeAlso for webpages
					docReference.addSameAs(model, GlobalArticleConfig.doiURI + doiReference);
					if (!global.getUriStyle().equals(ResourceConfig.bio2rdf)) {
						docReference.addSameAs(model, this.global.DOI_DOCUMENT + doiReference); //same as from pubmed to doi entity
					}
				} catch (Exception e) {	}
			}	
		} else if (doiReference != null) {
			if (withMetadata) {
				docReference.addDOI(model, doiReference);
				docReference.addIdentifier(model, "doi:" + doiReference);
			}
			//docReference.addSeeAlso(new URIImpl(global.doiURI + doiReference)); //seeAlso for webpages
			docReference.addSameAs(model, GlobalArticleConfig.doiURI + doiReference);
			if (!global.getUriStyle().equals(ResourceConfig.bio2rdf)) {
				docReference.addSameAs(model, this.global.DOI_DOCUMENT + doiReference); //same as from pubmed to doi entity
			}
		}
		
		return (docReference);
	}
	/**
	 * Creates a list of PersonE with the authors of an article, represented in a list of objects, only Name objects are processed.
	 * @param lst
	 * @return
	 */
	private Collection<org.ontoware.rdfreactor.schema.rdfs.Class> processReferenceCreateListOfAuthors(Model model, Document document, List<Object> lst, String personBaseURL){
		ArrayList<org.ontoware.rdfreactor.schema.rdfs.Class> listOfAuthorsRef = new ArrayList<org.ontoware.rdfreactor.schema.rdfs.Class>();
		for (Object objPerson: lst) {
	    	if (objPerson instanceof Name) {
	    		//Author
	    		Name name = (Name)objPerson;	
	    		PersonE person = (PersonE)createPerson(model, personBaseURL, name, false);			    
			    listOfAuthorsRef.add(person);			    
	    	}	    	    				    	
	    }
		return (listOfAuthorsRef);
	}
	protected void processReferenceAllTypeCitation(Model model, Thing document, Ref ref, List<Object> content, ReferenceType type, Class<?> clazz, boolean withMetadata) {
		processReferenceDocAllTypeCitation(model, (Document)document, ref, content, type, clazz, withMetadata);
	}
	/**
	 * Creates the reference to a journal article, its journal, and list of authors.
	 * @param ref
	 * @param content
	 */	
	private void processReferenceDocAllTypeCitation(Model model, Document document, Ref ref, List<Object> content, ReferenceType type, Class<?> clazz, boolean withMetadata) {
		String pubmedReference = null;
		String doiReference = null;
    	String pubIdOther = null;
    	
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
		
		Document docReference = null;
		String publicationLink = null; 
		String personBaseURL = null;
		String organizationBaseURL = null;
		String proceedingsBaseURL = null;
		String conferenceBaseURL = null;
		if (pubmedReference != null) {
			publicationLink = this.global.PUBMED_DOCUMENT + pubmedReference;
			String[] params = {pubmedReference};
			personBaseURL = Conversion.replaceParameter(this.global.BASE_URL_PERSON_PUBMED, params);
			organizationBaseURL = Conversion.replaceParameter(this.global.BASE_URL_ORGANIZATION_PUBMED, params);
			proceedingsBaseURL = Conversion.replaceParameter(this.global.BASE_URL_PROCEEDINGS_PUBMED, params);
			conferenceBaseURL = Conversion.replaceParameter(this.global.BASE_URL_CONFERENCE_PUBMED, params);
		} else if (doiReference != null) {
			publicationLink = this.global.DOI_DOCUMENT + doiReference;
			String[] params = {doiReference};
			personBaseURL = Conversion.replaceParameter(this.global.BASE_URL_PERSON_DOI, params);
			organizationBaseURL = Conversion.replaceParameter(this.global.BASE_URL_ORGANIZATION_DOI, params);
			proceedingsBaseURL = Conversion.replaceParameter(this.global.BASE_URL_PROCEEDINGS_DOI, params);
			conferenceBaseURL = Conversion.replaceParameter(this.global.BASE_URL_CONFERENCE_DOI, params);
		} else {
			publicationLink = global.BASE_URL_REF + this.getRefId(ref);
			personBaseURL = global.BASE_URL_PERSON_PMC;
			organizationBaseURL = global.BASE_URL_ORGANIZATION_PMC;
			proceedingsBaseURL = global.BASE_URL_PROCEEDINGS_PMC;
			conferenceBaseURL = global.BASE_URL_CONFERENCE_PMC;
		}
		
		if (!withMetadata) {			
			docReference = processReferenceCreateArticle(model, document, publicationLink, pubmedReference, doiReference, ref, type, withMetadata);
			//We need to keep the reference format as a document resource so we can link sections and paragraphs to it		
			if (pubmedReference != null) {
				docReference.addSameAs(model, global.BASE_URL_REF + this.getRefId(ref));
			} else if (doiReference != null) {
				docReference.addSameAs(model, global.BASE_URL_REF + this.getRefId(ref));
			}
			//References of the document
    	    document.addbiboCites(docReference);
    	    docReference.addbiboCitedby(document);
			return;
		}
		
		
		//common
		Collection<org.ontoware.rdfreactor.schema.rdfs.Class> listOfAuthorsRef = new ArrayList<org.ontoware.rdfreactor.schema.rdfs.Class>();
		//List<Agent> listOfEditorsRef = new ArrayList<Agent>();
		//List<Agent> listOfTranslatorsRef = new ArrayList<Agent>();		
		String sourceTitle = null;
    	String sourceId =  null;
    	String year = null;
    	String month = null;
    	String volume = null;
    	String issueNumber = null;
    	String pageStart = null;
    	String pageEnd = null;
    	
    	
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
    	
    	//conference
    	String confName = null;
    	String confDate = null;
    	String confLoc = null;
    	
    	//Others
    	String comment = null;
    	String dateInCitation = null;
    	String accessDate = null;
    	String otherTitle = "";  	
    			
		//process other information
		for (Object obj: content) {
			if (obj == null) {
				continue;
			}
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
					//TODO: make it clever so it will correctly process editors mixed as a single person
					//listOfEditorsRef = processReferenceCreateListOfAuthors(model, document, ((PersonGroup)obj).getContent(), personBaseURL); //"editor"
				} else if ((pg.getPersonGroupType() != null) && (pg.getPersonGroupType().equals("translator"))) {
					//TODO: make it clever so it will correctly process translators mixed as a single person
					//listOfTranslatorsRef = processReferenceCreateListOfAuthors(model, document, ((PersonGroup)obj).getContent(), personBaseURL); //"translator"
				} else {
					listOfAuthorsRef = processReferenceCreateListOfAuthors(model, document, ((PersonGroup)obj).getContent(), personBaseURL); //"author"
				}				
			} else if (obj instanceof Name) {
				Name name = (Name)obj;	 
				PersonE person = (PersonE)createPerson(model, personBaseURL, name, false);			    
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
		if ((type == ReferenceType.BOOK) && (sectionId != null)) {
			docReference = processReferenceCreateArticle(model, document, publicationLink, pubmedReference, doiReference, ref, ReferenceType.BOOK_SECTION, withMetadata);
			type = ReferenceType.BOOK_SECTION;
		} else if ((type == ReferenceType.BOOK) && (articleTitle != null)) {//nlm-citation
			docReference = processReferenceCreateArticle(model, document, publicationLink, pubmedReference, doiReference, ref, ReferenceType.BOOK_SECTION, withMetadata);
			type = ReferenceType.BOOK_SECTION;
		} else if ((type == ReferenceType.CONFERENCE_PROCS) && (articleTitle != null)) {
			docReference = processReferenceCreateArticle(model, document, publicationLink, pubmedReference, doiReference, ref, ReferenceType.CONFERENCE_PAPER, withMetadata);
			type = ReferenceType.CONFERENCE_PAPER;
		} else {
			docReference = processReferenceCreateArticle(model, document, publicationLink, pubmedReference, doiReference, ref, type, withMetadata);
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
        			JournalE journal = new JournalE(model, this.global.BASE_URL_JOURNAL_NAME + journalTitleInURI, true); //model.createBlankNode(pmcID + "_journal_" + sourceId.replaceAll(RDFHandler.CHAR_NOT_ALLOWED, "-"))
        		    journal.addTitle(model, sourceTitle);
        		    //Journal, issue-date, volume, pages, and document        		    
            		if (articleTitle != null) {
            			docReference.addTitle(model, articleTitle);
            		}
        		    if (volume != null) {
        		    	docReference.addbiboVolume(volume);
        		    }
        		    if (publisherName != null) {
        		    	OrganizationE publisherOrg = new OrganizationE(model, this.global.BASE_URL_PUBLISHER_NAME + publisherName.replaceAll(RDFHandler.CHAR_NOT_ALLOWED, "-"), true ); 
            		    publisherOrg.addName(model, publisherName);	
            		    journal.addbiboPublisher(publisherOrg);
            		    docReference.addbiboPublisher(publisherOrg);
        		    }        		    
        		    //Issue
        		    if (journal != null) {
        		    	if (issueNumber != null) { //name:<journal_name>/<issue_id>
        		    		String[] params = {"name", journalTitleInURI};
            				String issueURI = Conversion.replaceParameter(this.global.BASE_URL_ISSUE, params);
                    		ws.biotea.ld2rdf.rdf.model.bibo.Issue issue = 
                    			new ws.biotea.ld2rdf.rdf.model.bibo.Issue(model, issueURI + issueNumber.replaceAll(RDFHandler.CHAR_NOT_ALLOWED, "-"), true);    		
                    		docReference.addIssue(model, issue);
                    		journal.addIssue(model, issue);
                		} else {
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
    		    	OrganizationE publisherOrg = new OrganizationE(model, this.global.BASE_URL_PUBLISHER_NAME + publisherName.replaceAll(RDFHandler.CHAR_NOT_ALLOWED, "-"), true ); 
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
    				refListURL = Conversion.replaceParameter(this.global.BASE_URL_LIST_PUBMED, params);
    				params1[1] = pubmedReference;
    				refEditorsURL = Conversion.replaceParameter(this.global.BASE_URL_LIST_PUBMED, params1);
    				params2[1] = pubmedReference;
    				refTranslatorsURL = Conversion.replaceParameter(this.global.BASE_URL_LIST_PUBMED, params2);
        		} else if (doiReference != null) {
        			params[1] = doiReference;
    				refListURL = Conversion.replaceParameter(this.global.BASE_URL_LIST_DOI, params);
    				params1[1] = doiReference;
    				refEditorsURL = Conversion.replaceParameter(this.global.BASE_URL_LIST_DOI, params1);
    				params2[1] = doiReference;
    				refTranslatorsURL = Conversion.replaceParameter(this.global.BASE_URL_LIST_DOI, params2);
        		} else {
        			params[1] = this.getRefId(ref);
    				refListURL = Conversion.replaceParameter(global.BASE_URL_REF_LIST, params);
    				params1[1] = this.getRefId(ref);
    				refEditorsURL = Conversion.replaceParameter(global.BASE_URL_REF_LIST, params1);
    				params2[1] = this.getRefId(ref);
    				refTranslatorsURL = Conversion.replaceParameter(global.BASE_URL_REF_LIST, params2);
        		}
        		if ((listOfAuthorsRef != null) && (listOfAuthorsRef.size() != 0)) {  
        			for(org.ontoware.rdfreactor.schema.rdfs.Class agent: listOfAuthorsRef) {
        				if (agent instanceof PersonE) {
        					((PersonE)agent).addPublications(model, publicationLink);
        				}
        			}        			
        			if (clazz.equals(MixedCitation.class)) {
        				ListOfElements loaRef = null;
            			loaRef = new ListOfElements(model, refListURL, true); //model.createBlankNode(pmcID + "_listEditorsAndTranslatorsReference_" + this.getRefId(ref))
            			loaRef.addMembersInOrder(model, listOfAuthorsRef); //add members to list
            			proc.addbiboListofauthors(loaRef); //add list to document  
            		}    	    				                    		     	
            	}
            	/*if ((listOfEditorsRef != null) && (listOfEditorsRef.size() != 0)) {
            		for(Agent agent: listOfEditorsRef) {
        				if (agent instanceof PersonE) {
        					((PersonE)agent).addPublications(model, publicationLink);
        				}
        			} 
            		ListOfElements loaRef = new ListOfElements(model, refEditorsURL, true); //model.createBlankNode(pmcID + "_listEditorsReference_" + this.getRefId(ref))
        		    loaRef.addMembersInOrder(model, listOfAuthorsRef); //add members to list
        		    proc.addbiboListofeditors(loaRef); //add list to document		        	
            	}
            	if ((listOfTranslatorsRef != null) && (listOfTranslatorsRef.size() != 0)) {
            		for(Agent agent: listOfTranslatorsRef) {
        				if (agent instanceof PersonE) {
        					((PersonE)agent).addPublications(model, publicationLink);
        				}
        			} 
            		ListOfElements loaRef = new ListOfElements(model, refTranslatorsURL, true); // model.createBlankNode(pmcID + "_listlistOfTranslatorsReference_" + this.getRefId(ref))
        		    loaRef.addMembersInOrder(model, listOfAuthorsRef); //add members to list
        		    proc.addbiboListofeditors(loaRef); //add list to document		        	
            	}*/
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
    				docReference.addTitle(model, articleTitle);
    			}
    		    if (sourceTitle != null) {
    		    	docReference.addComment("Source: " + sourceTitle);
    		    }
    		    docReference.addComment("Note: Inventors, asignees, and others are described in the Authors list.");
	    	} else if (type == ReferenceType.DATABASE) { //webpage
	    		if (sourceTitle != null) {
    				docReference.addTitle(model, sourceTitle);
    			}
	    	} else if (type == ReferenceType.WEBPAGE) { //webpage
	    		if (sourceTitle != null) {
    				docReference.addTitle(model, sourceTitle);
    			}
	    	} else if (type == ReferenceType.COMMUN) { //personal communication
	    		if (sourceTitle != null) {
    				docReference.addTitle(model, sourceTitle);
    			}
	    	} else if (type == ReferenceType.DISCUSSION) { //webpage with article title
	    		if (articleTitle != null) {
    				docReference.addTitle(model, articleTitle);
    			}
    		    if (sourceTitle != null) {
    		    	docReference.addComment("Discussion/List group: " + sourceTitle);
    		    }
	    	} else if (type == ReferenceType.BLOG) { //webpage
	    		if (sourceTitle != null) {
    				docReference.addTitle(model, sourceTitle);
    			}
	    	} else if (type == ReferenceType.WIKI) { //webpage
	    		if (sourceTitle != null) {
    				docReference.addTitle(model, sourceTitle);
    			}
	    	} else if (type == ReferenceType.SOFTWARE) { //standard
	    		if (sourceTitle != null) {
    				docReference.addTitle(model, sourceTitle);
    			}
	    	} else if (type == ReferenceType.STANDARD) { //standard
	    		if (sourceTitle != null) {
    				docReference.addTitle(model, sourceTitle);
    			}
	    	} else if (type == ReferenceType.OTHER) { //standard
	    		if (sourceTitle != null) {
    				docReference.addTitle(model, sourceTitle);
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
		    	OrganizationE publisherOrg = new OrganizationE(model, this.global.BASE_URL_PUBLISHER_NAME + publisherName.replaceAll(RDFHandler.CHAR_NOT_ALLOWED, "-"), true ); 
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
				refListURL = Conversion.replaceParameter(this.global.BASE_URL_LIST_PUBMED, params);
				params1[1] = pubmedReference;
				refEditorsURL = Conversion.replaceParameter(this.global.BASE_URL_LIST_PUBMED, params1);
				params2[1] = pubmedReference;
				refTranslatorsURL = Conversion.replaceParameter(this.global.BASE_URL_LIST_PUBMED, params2);
				params3[1] = pubmedReference;
				refAuthorsURL = Conversion.replaceParameter(this.global.BASE_URL_LIST_PUBMED, params3);
    		} else if (doiReference != null) {
    			params[1] = doiReference;
				refListURL = Conversion.replaceParameter(this.global.BASE_URL_LIST_DOI, params);
				params1[1] = doiReference;
				refEditorsURL = Conversion.replaceParameter(this.global.BASE_URL_LIST_DOI, params1);
				params2[1] = doiReference;
				refTranslatorsURL = Conversion.replaceParameter(this.global.BASE_URL_LIST_DOI, params2);
				params3[1] = doiReference;
				refAuthorsURL = Conversion.replaceParameter(this.global.BASE_URL_LIST_DOI, params3);
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
        		for(org.ontoware.rdfreactor.schema.rdfs.Class agent: listOfAuthorsRef) {
    				if (agent instanceof PersonE) {
    					((PersonE)agent).addPublications(model, publicationLink);
    				}
    			}  
        		ListOfElements loaRef = null;
        		if (type != ReferenceType.CONFERENCE_PROCS) { // authors belongs to paper rather than proceedings
        			if (clazz.equals(MixedCitation.class)) {
            			loaRef = new ListOfElements(model, refListURL, true); //model.createBlankNode(pmcID + "_listAuthorsAndEditorsAndTranslatorsReference_" + this.getRefId(ref))
            		} else {
            			loaRef = new ListOfElements(model, refAuthorsURL, true); //model.createBlankNode(pmcID + "_listAuthorsReference_" + this.getRefId(ref))        			    
            		}    	    				        
            		loaRef.addMembersInOrder(model, listOfAuthorsRef); //add members to list
        		    docReference.addbiboListofauthors(loaRef); //add list to document
        		}        		
        	}
        	/*
        	if ((listOfEditorsRef != null) && (listOfEditorsRef.size() != 0)) {
        		for(Agent agent: listOfEditorsRef) {
    				if (agent instanceof PersonE) {
    					((PersonE)agent).addPublications(model, publicationLink);
    				}
    			} 
        		ListOfElements loaRef = new ListOfElements(model, refEditorsURL, true); //model.createBlankNode(pmcID + "_listEditorsReference_" + this.getRefId(ref))
    		    loaRef.addMembersInOrder(model, listOfAuthorsRef); //add members to list
    		    docReference.addbiboListofeditors(loaRef); //add list to document		        	
        	}
        	if ((listOfTranslatorsRef != null) && (listOfTranslatorsRef.size() != 0)) {
        		for(Agent agent: listOfTranslatorsRef) {
    				if (agent instanceof PersonE) {
    					((PersonE)agent).addPublications(model, publicationLink);
    				}
    			}
        		ListOfElements loaRef = new ListOfElements(model, refTranslatorsURL, true); //model.createBlankNode(pmcID + "_listlistOfTranslatorsReference_" + this.getRefId(ref))
    		    loaRef.addMembersInOrder(model, listOfAuthorsRef); //add members to list
    		    docReference.addbiboListofeditors(loaRef); //add list to document		        	
        	}*/
        	//References of the document
    	    document.addbiboCites(docReference);
    	    docReference.addbiboCitedby(document);
		} catch (Exception e ) {
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
	private String processAbstractAsSection(Abstract ab, Model model, Document document) {
		//Title
		String title = "Abstract";
		//add section
		String secDocoURI = global.BASE_URL_SECTION + title;
		SectionE secDoco = new SectionE(model, secDocoURI, true);
		document.addSection(model, secDoco, mainListOfSectionsURI);		
		//title
		Node titleNode = new PlainLiteralImpl(title);
		secDoco.adddocoTitle(titleNode);		
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
		String[] paramsTitle = {title};
		String paragraphURI = Conversion.replaceParameter(global.BASE_URL_PARAGRAPH, paramsTitle) + "1";
		ParagraphE paragraph = new ParagraphE(model, paragraphURI, true);
		String[] params = {"listOfParagraphs", "" + this.paragraphCounter};
		secDoco.addParagraph(model, paragraph, Conversion.replaceParameter(global.BASE_URL_LIST_PMC, params));		
		PlainLiteral textNode = model.createPlainLiteral(text);
	    model.addStatement(paragraph.asResource(), new URIImpl(ClassesAndProperties.TEXT_PROPERTY, false), textNode); //text
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
		document.addSection(model, secDoco, mainListOfSectionsURI);
		//title
		Node titleNode = new PlainLiteralImpl(title);
		secDoco.adddocoTitle(titleNode);
		//link to parent
		if (parent != null) { 
			String[] params = {"listOfSections", "" + this.sectionCounter};
			if (parent instanceof SectionE) {
				((SectionE)parent).addSection(model, secDoco, Conversion.replaceParameter(global.BASE_URL_LIST_PMC, params));
			} else if (parent instanceof Appendix) {
				((Appendix)parent).addSection(model, secDoco, Conversion.replaceParameter(global.BASE_URL_LIST_PMC, params));
			}
			
		}
		//process paragraphs
		this.paragraphCounter++;
		Iterator<Object> itrPara = section.getAddressesAndAlternativesAndArraies().iterator();	
		if (parent == null) {
			processElementsInSection(model, document, titleInURL, secDoco, itrPara);
		} else {
			processElementsInSection(model, document, parentTitleInURL + "_" + titleInURL, secDoco, itrPara);
		}
				
		//process subsections
		Iterator<Sec> itr = section.getSecs().iterator();
		while (itr.hasNext()) {
			Sec sec = itr.next();
			this.sectionCounter++;
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
		String[] paramsPara = {"listOfParagraphs", "" + this.paragraphCounter};
		secDoco.addParagraph(model, paragraph, Conversion.replaceParameter(global.BASE_URL_LIST_PMC, paramsPara));
					
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
	    model.addStatement(paragraph.asResource(), new URIImpl(ClassesAndProperties.TEXT_PROPERTY, false), textNode); //text		
	}
}
