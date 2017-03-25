/**
 * 
 */
package ws.biotea.ld2rdf.rdfGeneration.pmcoa;  

import org.ontoware.rdf2go.RDF2Go;
import org.ontoware.rdf2go.model.Model;
import org.ontoware.rdf2go.model.Statement;
import org.ontoware.rdf2go.model.node.PlainLiteral;
import org.ontoware.rdf2go.model.node.Resource;
import org.ontoware.rdf2go.model.node.impl.URIImpl;
import pubmed.openAccess.jaxb.generated.*;
import ws.biotea.ld2rdf.rdf.model.bibo.Thing;
import ws.biotea.ld2rdf.rdf.model.bibo.extension.ListOfElements;
import ws.biotea.ld2rdf.rdfGeneration.RDFHandler;
import ws.biotea.ld2rdf.rdfGeneration.exception.ArticleTypeException;
import ws.biotea.ld2rdf.rdfGeneration.exception.DTDException;
import ws.biotea.ld2rdf.rdfGeneration.exception.PMCIdException;
import ws.biotea.ld2rdf.rdfGeneration.jats.GlobalArticleConfig;
import ws.biotea.ld2rdf.util.ResourceConfig;
import ws.biotea.ld2rdf.util.mapping.MappingConfig;
import ws.biotea.ld2rdf.util.mapping.Namespace;
import ws.biotea.ld2rdf.util.Conversion;
import ws.biotea.ld2rdf.util.HtmlUtil;

import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class PmcOpenAccess2MappedRDF extends PmcOpenAccess2AbstractRDF {
	private ListOfElements mainListOfSections;	
	
	public PmcOpenAccess2MappedRDF(File paper, StringBuilder str, String suffix, String bioteaBase, String bioteaDataset, 
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
		Thing document;		
		File outputFile = null;
		model = createAndOpenModel();
		boolean fatalError = false;
		//create document
		try {
			document = new Thing(model, MappingConfig.getClass(this.bioteaBase, "bibo", "Document"), basePaper, true);
			String type = this.articleType.replace('-', '_').toUpperCase();
			if (ArticleType.valueOf(type).getBiboType() != null) {
				document = new Thing(model, MappingConfig.getClass(this.bioteaBase, "bibo", ArticleType.valueOf(type).getBiboType()), basePaper, true);
			}
			this.addDatatypeLiteral(model, document, "dcterms", "description", this.articleType);
			
			this.processBasic(model, document, paper.getName());
			logger.info("=== basic processed");
			this.processAuthors(model, document, paper.getName());
			logger.info("=== authors processed");
			this.processAbstractAndKeywords(model, document);
			logger.info("=== abstract and keywords processed");
			this.processReferences(model, document, references);
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
		if (this.sections) {
			Thing documentSections;
			Model modelSections;
			File outputFileSections = null;
			boolean fatalErrorSections = false;
			modelSections = createAndOpenModel();
			try {
				documentSections = new Thing(modelSections, MappingConfig.getClass(this.bioteaBase, "bibo", "Document"), basePaper, true);
				String type = this.articleType.replace('-', '_').toUpperCase();
				if (ArticleType.valueOf(type).getBiboType() != null) {
					documentSections = new Thing(modelSections, MappingConfig.getClass(this.bioteaBase, "bibo", ArticleType.valueOf(type).getBiboType()), basePaper, true);
				}
				this.addDatatypeLiteral(modelSections, documentSections, "dcterms", "title", this.articleTitle);

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
					Thing secDoco = new Thing(modelSections, MappingConfig.getClass(this.bioteaBase, "doco", "Section"), global.BASE_URL_SECTION + "undefined-section", true);					
					if (processElementsInSection(modelSections, documentSections, "undefined-section", secDoco, itrPara)) {
						this.mainListOfSections = this.linkSection(modelSections, documentSections, secDoco, this.mainListOfSections, this.mainListOfSectionsURI);
					}
				}				
				logger.info("=== sections processed");
			} catch (Exception e) {//something went so wrong
				logger.fatal("- FATAL ERROR SECTIONS - " + pmcID + " threw an uncaugth error: " + e.getMessage());
				fatalErrorSections = true;
			} finally {
				if (fatalError || fatalErrorSections) {
					logger.info("=== END of sections rdfization with a fatal error " + pmcID + " (pubmedID: " + pubmedID + "), (doi: " + doi + ")");
				} else {
					outputFileSections = serializeAndCloseModel(modelSections, outputDir + "/" + PREFIX + pmcID + "_" + this.suffix + "_sections.rdf");		
					logger.info("=== END of sections rdfization OK " + pmcID + " (pubmedID: " + pubmedID + "), (doi: " + doi + ")");
				}
			}
			return outputFileSections;
		}
		return outputFile;
		//Supplementary material part of sections as well, not supported yet
	}
	/**
	 * Creates and open an RDF model.
	 * @return
	 */
	protected Model createAndOpenModel() {
		Model myModel = RDF2Go.getModelFactory().createModel();
		myModel.open();	
		for (Namespace namespace: MappingConfig.getAllNamespaces(this.bioteaBase)) {
			myModel.setNamespace(namespace.getNamespace(), namespace.getUrl());
		}
		return (myModel);
	}
	
	
	/**
	 * Processes doi, pubmed, journal, and title.
	 */
	private void processBasic(Model model, Thing document, String paper) {
		String mainId = "";
		String articleId = pmcID;
		if (ResourceConfig.getIdTag().equals("pmc")) {
			articleId = pmcID;
			mainId = "pmc:" + this.pmcID;
		} else if (ResourceConfig.getIdTag().equals("pmid")) {
			articleId = pubmedID;
			mainId = "pmid:" + this.pubmedID;
		}
		
		String[] sameAsLinks = ResourceConfig.getConfigSameAs(this.suffix);
		if (sameAsLinks != null) {
			for (String sameAsLink:sameAsLinks) {
				String[] params = {articleId};
				document.addSameAs(model, Conversion.replaceParameter(sameAsLink, params));
			}
		}
		
		this.addDatatypeLiteral(model, document, "dcterms", "identifier", mainId);
		if (MappingConfig.getIdentifier(this.bioteaBase) != null) {
			//main identifier also goes directly to model
			PlainLiteral idAsLiteral = model.createPlainLiteral(mainId);
			Statement stm = model.createStatement(document.asResource(), new URIImpl(MappingConfig.getIdentifier(this.bioteaBase)), idAsLiteral);
		    model.addStatement(stm);		    
		}
		
		this.addObjectProperty(model, document, GlobalArticleConfig.pmcURI + pmcID, "prov", "wasDerivedFrom");
		String now = HtmlUtil.getDateAndTime();
		this.addDatatypeLiteral(model, document, "dcterms", "created", now);
		this.addDatatypeLiteral(model, document, "prov", "generatedAtTime", now);
		this.addObjectProperty(model, document, GlobalArticleConfig.RDF4PMC_AGENT, "dcterms", "creator");
		this.addObjectProperty(model, document, GlobalArticleConfig.RDF4PMC_AGENT, "prov", "wasAttributedTo");
		this.addObjectProperty(model, document, this.bioteaDataset, 
			ws.biotea.ld2rdf.util.OntologyPrefix.SIO.getURL() + "SIO_001278");
		Resource resPMC = new URIImpl(GlobalArticleConfig.pmcURI + pmcID, true);	
		document.addSeeAlso(resPMC);	
		
		//pubmedID
		if (pubmedID != null) {
			this.addDatatypeLiteral(model, document, "bibo", "pmid", this.pubmedID);
		    //Relations between PMC-RDF and identifiers.org/bio2rdf.org sameAS
		    if (ResourceConfig.withBio()) {
		    	document.addSameAs(model, ResourceConfig.IDENTIFIERS_ORG_PUBMED + pubmedID);
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
			this.addDatatypeLiteral(model, document, "bibo", "doi", this.doi);
		    document.addSameAs(model, GlobalArticleConfig.doiURI + doi);
		    if (!global.getUriStyle().equals(ResourceConfig.bio2rdf)) {
		    	document.addSameAs(model, this.global.DOI_DOCUMENT + doi);
		    }
		}
		
		//license
		try {
			String license = article.getFront().getArticleMeta().getPermissions().getLicenses().get(0).getHref();
			this.addObjectProperty(model, document, license, "dcterms", "license");
		} catch (Exception e) {}		
	    
	    //Journal
		Thing journal = null;
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
						} else {
							journalTitle += processElement(model, document, ser, null,null);
						}			
					}
				} catch (Exception e1) {}
			} 	
			//issn
			journalISSN = article.getFront().getJournalMeta().getIssns().get(0).getContent().get(0).toString();
			journalTitleInURI = journalISSN.replaceAll(RDFHandler.CHAR_NOT_ALLOWED, "-");
			//journal creation
			if ((journalISSN != null)  && (journalISSN.length() != 0)) {//journal by issn
				journal = new Thing(model, MappingConfig.getClass(this.bioteaBase, "bibo", "Journal"), this.global.BASE_URL_JOURNAL_ISSN + journalTitleInURI, true);				
				this.addDatatypeLiteral(model, journal, "dcterms", "title", journalTitle);
				for (Issn issn: article.getFront().getJournalMeta().getIssns()) {
					this.addDatatypeLiteral(model, journal, "bibo", "issn", issn.getContent().get(0).toString());
			    }
				Resource resISSN = new URIImpl(GlobalArticleConfig.NLM_JOURNAL_CATALOG + journalISSN, true);
				journal.addSeeAlso(resISSN); //link to NLM catalog
			} else if ((journalTitle != null) && (journalTitle.length() != 0)) { //journal by name
				journalTitleInURI = journalTitle.replaceAll(RDFHandler.CHAR_NOT_ALLOWED, "-");
				journal = new Thing(model, MappingConfig.getClass(this.bioteaBase, "bibo", "Journal"), this.global.BASE_URL_JOURNAL_NAME + journalTitleInURI, true);
				this.addDatatypeLiteral(model, journal, "dcterms", "title", journalTitle);
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
    			Thing issue = null;
    			if ((issueNumberInURI != null)  && (issueNumberInURI.length() != 0)) {    			        			
        			if ((journalISSN != null)  && (journalISSN.length() != 0)) {//journal by issn then issn:<id>/<issue_id>
        				String[] params = {"issn", journalTitleInURI};
        				String issueURI = Conversion.replaceParameter(this.global.BASE_URL_ISSUE, params);
            			issue = new Thing(model, MappingConfig.getClass(this.bioteaBase, "bibo", "Issue"), issueURI + issueNumberInURI, true);    		
            			this.addDatatypeLiteral(model, issue, "bibo", "issue", issueNumber);
            		} else { //name:<journal_name>/<issue_id>
            			String[] params = {"name", journalTitleInURI};
        				String issueURI = Conversion.replaceParameter(this.global.BASE_URL_ISSUE, params);
            			issue = new Thing(model, MappingConfig.getClass(this.bioteaBase, "bibo", "Issue"), issueURI + issueNumberInURI, true);
        			}
        			this.addHasPartIsPartOf(model, journal, issue);
        			this.addHasPartIsPartOf(model, issue, document);
        		} else {
        			this.addHasPartIsPartOf(model, journal, document);
        		} 
    		}
    	} catch (Exception e) {	
    		this.addHasPartIsPartOf(model, journal, document);
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
			Thing publisher;
			if (publisherPMCId != null) {//we create the publisher with the id
				publisher = new Thing(model, MappingConfig.getClass(this.bioteaBase, "foaf", "Organization"), this.global.BASE_URL_PUBLISHER_ID + publisherPMCId, true );
				this.addDatatypeLiteral(model, publisher, "foaf", "name", publisherName);
				this.addDatatypeLiteral(model, publisher, "dcterms", "identifier", publisherPMCId);
			} else {//we create the publisher with the name
				publisher = new Thing(model, MappingConfig.getClass(this.bioteaBase, "foaf", "Organization"), 
						this.global.BASE_URL_PUBLISHER_NAME + publisherName.replaceAll(RDFHandler.CHAR_NOT_ALLOWED, "-"), true );
				this.addDatatypeLiteral(model, publisher, "foaf", "name", publisherName);							
			}
			this.addObjectProperty(model, journal, publisher, "dcterms", "publisher");
			this.addObjectProperty(model, document, publisher, "dcterms", "publisher");
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
	    	    		} 
	    	    	}
	    		}
	    	}
        	this.addDatatypeLiteral(model, document, "dcterms", "issued", date);    
		} catch (Exception e) {
			logger.info(paper + ": Date or month not processed");
		}
    	try {
    		String volume = article.getFront().getArticleMeta().getVolume().getContent().get(0).toString();
    		this.addDatatypeLiteral(model, document, "bibo", "volume", volume);
    	} catch (Exception e) {
    		logger.info(paper + ": Volumen not processed");
    	}    	
		try {
			String pageStart = article.getFront().getArticleMeta().getFpage().getContent();
			this.addDatatypeLiteral(model, document, "bibo", "pageStart", pageStart);
		} catch (Exception e) {
			logger.info(paper + ": Start page not processed");
		}
    	try {
    		String pageEnd = article.getFront().getArticleMeta().getLpage().getContent();
    		this.addDatatypeLiteral(model, document, "bibo", "pageEnd", pageEnd);
    	} catch (Exception e) {
    		logger.info(paper + ": End page not processed");
    	}    
    	
    	//Title
    	try {    		
    	    this.articleTitle = "";
    		for (Object ser: article.getFront().getArticleMeta().getTitleGroup().getArticleTitle().getContent()) {
    			if (ser instanceof String) {
    				this.articleTitle += ser.toString();
    			} else {
    				this.articleTitle += processElement(model, document, ser, null, null);
    			}		
    		}
    		this.addDatatypeLiteral(model, document, "dcterms", "title", this.articleTitle);
    	} catch (Exception e) {
    		logger.info(paper + ": Article title not processed");
    	}	
	}	
	/**
	 * Processes authors.
	 */
	private void processAuthors(Model model, Thing document, String paper) {
		try {
			//List of Authors
		    ArrayList<org.ontoware.rdfreactor.schema.rdfs.Class> listOfAuthors = new ArrayList<org.ontoware.rdfreactor.schema.rdfs.Class>();
			for (Object obj: article.getFront().getArticleMeta().getContribGroupsAndAfvesAndXS()) {
				if (obj instanceof ContribGroup) {
					ContribGroup group = (ContribGroup)obj;
					for (Object objGr: group.getContribsAndAddressesAndAfves()) {
						if (objGr instanceof Contrib) {
							Contrib contrib = (Contrib)objGr;
							if (contrib.getContribType().equals("author")) {
								//Author
								Thing person = null;
								for (Object objAut: contrib.getAnonymousesAndCollabsAndNames()) {
									if (objAut instanceof Name) {
										Name name = (Name)objAut;
										person = (Thing)createPerson(model, global.BASE_URL_PERSON_PMC, name, true);
										this.addObjectProperty(model, person, this.basePaper, "foaf", "publications");
									} else if (objAut instanceof Email) {
										try {
											Email email = (Email)objAut;
											String str = email.getContent().get(0).toString();
											this.addObjectProperty(model, person, "mailto:" + str, "foaf", "mbox");
									    } catch (Exception e) {}
									} else if (objAut instanceof Address) {
										Address address = (Address) objAut;
										for (Object detailAddress : address.getAddrLinesAndCountriesAndFaxes()) {
											if (detailAddress instanceof Email) {
												try {
													Email email = (Email) detailAddress;
													String str = email.getContent().get(0).toString();
													this.addObjectProperty(model, person, "mailto:" + str, "foaf", "mbox");
											    } catch (Exception e) {}
											}
										}
									} else if (objAut instanceof Xref) {
									} else if (objAut instanceof Collab) {
										Collab collab = (Collab)obj;
										String orgName = collab.getContent().get(0).toString();
										String idOrg = orgName.replaceAll(RDFHandler.CHAR_NOT_ALLOWED, "-");										
										Thing org = new Thing(model, MappingConfig.getClass(this.bioteaBase, "foaf", "Organization"), 
											global.BASE_URL_ORGANIZATION_PMC + idOrg, true);
										listOfAuthors.add(org);
									}
								}
								if (person != null) {
									listOfAuthors.add(person);
								}							
							} else if (contrib.getContribType().equals("collab")) {
								//Collaborator
								Thing org = null;
								for (Object objAut: contrib.getAnonymousesAndCollabsAndNames()) {
									if (objAut instanceof Collab) {
										Collab collab = (Collab)obj;
										String orgName = collab.getContent().get(0).toString();
										String idOrg = orgName.replaceAll(RDFHandler.CHAR_NOT_ALLOWED, "-");
										org = new Thing(model, MappingConfig.getClass(this.bioteaBase, "foaf", "Organization"), 
											global.BASE_URL_ORGANIZATION_PMC + idOrg, true ); 
										listOfAuthors.add(org);
									}
								}		
							}
						}
					}
				}
			}
		    //list in rdf: ResourceConfig.BIOTEA_URL + "/{0}/pmc_resource/" + pmcID + "/";
			String[] params = {"listOfAuthors", "1"};
			ListOfElements loa = new ListOfElements(model, Conversion.replaceParameter(global.BASE_URL_LIST_PMC, params), true); 
		    loa.addMembersInOrder(model, listOfAuthors); //add members to list
		    this.addObjectProperty(model, document, loa, "bibo", "authorList");
		} catch (Exception e){
			logger.info(paper + ": Authors not processed.");
		}			
	}
	
	/**
	 * Processes abstract and keywords.
	 */
	private void processAbstractAndKeywords(Model model, Thing document) {
		//Abstract
	    String docAbstract = "";
		for (Abstract ab: article.getFront().getArticleMeta().getAbstracts()) {
			docAbstract += getAbstractText(model, document, ab);			
		}
		this.addDatatypeLiteral(model, document, "bibo", "abstract", docAbstract);
	    
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
						} else {
							key += processElement(model, document, cont, null, null);
						}
						this.addDatatypeLiteral(model, document, "bibo", "shortDescription", key);
					}
				} 
			}
		}
	}
	
	/**
	 * Determines the type of the reference and process it according to its type: Citation, MixedCitation, ElementCitation, or NlmCitation
	 */
	private void processReferences(Model model, Thing document, boolean withMetadata) {
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
	private Thing processReferenceCreateArticle(Model model, Thing document, String url, String pubmed, String doiReference, Ref ref, ReferenceType type, boolean withMetadata) {		
		Thing docReference = null;		

		//create document
		docReference = new Thing(model, MappingConfig.getClass(this.bioteaBase, "bibo", "Document"), url, true);
		if (type == ReferenceType.JOURNAL_ARTICLE) {
			docReference = new Thing(model, MappingConfig.getClass(this.bioteaBase, "bibo", "AcademicArticle"), url, true);
		} else if (type == ReferenceType.BOOK) {
			docReference = new Thing(model, MappingConfig.getClass(this.bioteaBase, "bibo", "Book"), url, true);
		} else if (type == ReferenceType.BOOK_SECTION) {
			docReference = new Thing(model, MappingConfig.getClass(this.bioteaBase, "bibo", "BookSection"), url, true);
		} else if (type == ReferenceType.REPORT) {
			docReference = new Thing(model, MappingConfig.getClass(this.bioteaBase, "bibo", "Report"), url, true);	
		} else if (type == ReferenceType.THESIS) {
			docReference = new Thing(model, MappingConfig.getClass(this.bioteaBase, "bibo", "Thesis"), url, true);
		} else if (type == ReferenceType.CONFERENCE_PROCS) {
			docReference = new Thing(model, MappingConfig.getClass(this.bioteaBase, "bibo", "Proceedings"), url, true);
		} else if (type == ReferenceType.CONFERENCE_PAPER) {
			docReference = new Thing(model, MappingConfig.getClass(this.bioteaBase, "bibo", "AcademicArticle"), url, true);
		} else if (type == ReferenceType.PATENT) {
			docReference = new Thing(model, MappingConfig.getClass(this.bioteaBase, "bibo", "Patent"), url, true);
		} else if (type == ReferenceType.DATABASE) {
			docReference = new Thing(model, MappingConfig.getClass(this.bioteaBase, "bibo", "WebPage"), url, true);
		} else if (type == ReferenceType.WEBPAGE) {
			docReference = new Thing(model, MappingConfig.getClass(this.bioteaBase, "bibo", "WebPage"), url, true);	
		} else if (type == ReferenceType.COMMUN) {
			docReference = new Thing(model, MappingConfig.getClass(this.bioteaBase, "bibo", "PersonalCommunicationDocument"), url, true);
		} else if (type == ReferenceType.DISCUSSION) {
			docReference = new Thing(model, MappingConfig.getClass(this.bioteaBase, "bibo", "WebPage"), url, true);	
		} else if (type == ReferenceType.BLOG) {
			docReference = new Thing(model, MappingConfig.getClass(this.bioteaBase, "bibo", "WebPage"), url, true);
		} else if (type == ReferenceType.WIKI) {
			docReference = new Thing(model, MappingConfig.getClass(this.bioteaBase, "bibo", "WebPage"), url, true);
		} else if (type == ReferenceType.SOFTWARE) {
			docReference = new Thing(model, MappingConfig.getClass(this.bioteaBase, "bibo", "Standard"), url, true);
		} else if (type == ReferenceType.STANDARD) {
			docReference = new Thing(model, MappingConfig.getClass(this.bioteaBase, "bibo", "Standard"), url, true);
		}			
		
		if (pubmed != null) {			
			if (withMetadata) {
				this.addDatatypeLiteral(model, docReference, "dcterms", "identifier", "pmid:" + pubmed);
				this.addDatatypeLiteral(model, docReference, "bibo", "pmid", pubmed);
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
						this.addDatatypeLiteral(model, document, "dcterms", "identifier", "doi:" + doiReference);
						this.addDatatypeLiteral(model, document, "bibo", "doi", doiReference);
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
				this.addDatatypeLiteral(model, document, "dcterms", "identifier", "doi:" + doiReference);
				this.addDatatypeLiteral(model, document, "bibo", "doi", doiReference);
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
	 * Creates a list of Thing with the authors of an article, represented in a list of objects, only Name objects are processed.
	 * @param lst
	 * @return
	 */
	private List<org.ontoware.rdfreactor.schema.rdfs.Class> processReferenceCreateListOfAuthors(Model model, Thing document, List<Object> lst, String personBaseURL){
		ArrayList<org.ontoware.rdfreactor.schema.rdfs.Class> listOfAuthorsRef = new ArrayList<org.ontoware.rdfreactor.schema.rdfs.Class>();
		for (Object objPerson: lst) {
	    	if (objPerson instanceof Name) {
	    		//Author
	    		Name name = (Name)objPerson;	
	    		Thing person = (Thing)createPerson(model, personBaseURL, name, true);			    
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
	protected void processReferenceAllTypeCitation(Model model, Thing document, Ref ref, List<Object> content, ReferenceType type, Class<?> clazz, boolean withMetadata) {
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
		
		Thing docReference = null;
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
		
		if (!withMetadata && ((pubmedReference != null) || (doiReference != null))) {			
			docReference = processReferenceCreateArticle(model, document, publicationLink, pubmedReference, doiReference, ref, type, withMetadata);
			//We need to keep the reference format as a document resource so we can link sections and paragraphs to it		
			if (pubmedReference != null) {
				docReference.addSameAs(model, global.BASE_URL_REF + this.getRefId(ref));
			} else if (doiReference != null) {
				docReference.addSameAs(model, global.BASE_URL_REF + this.getRefId(ref));
			}
			//References of the document
			this.addCitation(model, document, docReference);
			return;
		} else {
			withMetadata = true;
		}		
		
		
		//common
		List<org.ontoware.rdfreactor.schema.rdfs.Class> listOfAuthorsRef = new ArrayList<org.ontoware.rdfreactor.schema.rdfs.Class>();		
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
    	
    	//conference
    	String confName = null;
    	String confDate = null;
    	String confLoc = null;
    	
    	//Others
    	String comment = null;
    	String dateInCitation = null;
    	String accessDate = null;  	
		
    			
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
				} else if ((pg.getPersonGroupType() != null) && (pg.getPersonGroupType().equals("translator"))) {
				} else {
					listOfAuthorsRef = processReferenceCreateListOfAuthors(model, document, ((PersonGroup)obj).getContent(), personBaseURL); //"author"
				}				
			} else if (obj instanceof Name) {
				Name name = (Name)obj;	 
				Thing person = (Thing)createPerson(model, personBaseURL, name, true);			    
		    	listOfAuthorsRef.add(person);			    		    			
			} else if (obj instanceof Collab) {
				Collab collab = (Collab)obj;
				String orgName = collab.getContent().get(0).toString();
				String idOrg = orgName.replaceAll(RDFHandler.CHAR_NOT_ALLOWED, "-");
				Thing org = new Thing(model, MappingConfig.getClass(this.bioteaBase, "foaf", "Organization"), organizationBaseURL + idOrg, true );
				listOfAuthorsRef.add(org);
			} else if (obj instanceof AccessDate) { //nlm-citation
				try {
					accessDate = processListOfElements(model, document, ((AccessDate)obj).getContent(), null, null);
				} catch (Exception e){}
			} else if (obj instanceof TransTitle) { //nlm-citation
				try {
					transTitle = processListOfElements(model, document, ((TransTitle)obj).getContent(), null, null);
				} catch (Exception e){}
			} else if (obj instanceof PubId) {
				//it was already processed		
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
        			Thing journal = new Thing(model, MappingConfig.getClass(this.bioteaBase, "bibo", "Journal"), 
        					this.global.BASE_URL_JOURNAL_NAME + journalTitleInURI, true); 
        			this.addDatatypeLiteral(model, journal, "dcterms", "title", sourceTitle);
        		    //Journal, issue-date, volume, pages, and document        		    
        			this.addDatatypeLiteral(model, docReference, "dcterms", "title", articleTitle);
    		    	this.addDatatypeLiteral(model, docReference, "bibo", "volume", volume);
        		    if (publisherName != null) {
        		    	Thing publisherOrg = new Thing(model, MappingConfig.getClass(this.bioteaBase, "foaf", "Organization"), 
        		    			this.global.BASE_URL_PUBLISHER_NAME + publisherName.replaceAll(RDFHandler.CHAR_NOT_ALLOWED, "-"), true );
        		    	this.addDatatypeLiteral(model, publisherOrg, "foaf", "name", publisherName);
        		    	this.addObjectProperty(model, journal, publisherOrg, "dcterms", "publisher");
        		    	this.addObjectProperty(model, docReference, publisherOrg, "dcterms", "publisher");
        		    }        		    
        		    //Issue
        		    if (journal != null) {
        		    	if (issueNumber != null) { //name:<journal_name>/<issue_id>
        		    		String[] params = {"name", journalTitleInURI};
            				String issueURI = Conversion.replaceParameter(this.global.BASE_URL_ISSUE, params);
                    		Thing issue = new Thing(model, MappingConfig.getClass(this.bioteaBase, "bibo", "Issue"), 
                				issueURI + issueNumber.replaceAll(RDFHandler.CHAR_NOT_ALLOWED, "-"), true);
                    		this.addHasPartIsPartOf(model, issue, docReference);
                    		this.addHasPartIsPartOf(model, journal, issue);
                		} else {
                			this.addHasPartIsPartOf(model, journal, docReference);
                		}
        		    }            		
        		}
        		
	    	} else if (type == ReferenceType.BOOK) { //book
	    		if (sourceId != null) { 
	    			this.addDatatypeLiteral(model, docReference, "dcterms", "title", sourceTitle);
        		}
	    	} else if (type == ReferenceType.BOOK_SECTION) { //Book section
				this.addDatatypeLiteral(model, docReference, "dcterms", "title", sectionTitle == null ? articleTitle : sectionTitle);
		    	this.addDatatypeLiteral(model, docReference, "dcterms", "title", "(Book title) " + sourceTitle);
	    	} else if (type == ReferenceType.REPORT) {
	    		if (sourceId != null) {	 
	    			this.addDatatypeLiteral(model, docReference, "dcterms", "title", sourceTitle);
    		    	this.addDatatypeLiteral(model, docReference, "dcterms", "identifier", pubIdOther);
				}
	    	} else if (type == ReferenceType.THESIS) { 
				this.addDatatypeLiteral(model, docReference, "dcterms", "title", sourceTitle);
		    	this.addDatatypeLiteral(model, docReference, "dcterms", "title", "(Chapter title) " + sectionTitle);
	    	} else if (type == ReferenceType.CONFERENCE_PAPER) {//paper (article) and proceedings (book)
    			this.addDatatypeLiteral(model, docReference, "dcterms", "title", sectionTitle);
		    	docReference.addComment("Proceedings title: " + sourceTitle); 
    		    //proceedings
    		    Thing proc = new Thing(model, MappingConfig.getClass(this.bioteaBase, "bibo", "Proceedings"), proceedingsBaseURL + this.getRefId(ref), true); 
    		    this.addDatatypeLiteral(model, proc, "dcterms", "title", sourceTitle);
		    	this.addDatatypeLiteral(model, proc, "bibo", "numPges", numPages);
		    	this.addDatatypeLiteral(model, proc, "bibo", "edition", edition);
    		    if (publisherName != null) {
    		    	Thing publisherOrg = new Thing(model, MappingConfig.getClass(this.bioteaBase, "foaf", "Organization"), 
    		    			this.global.BASE_URL_PUBLISHER_NAME + publisherName.replaceAll(RDFHandler.CHAR_NOT_ALLOWED, "-"), true );
    		    	this.addDatatypeLiteral(model, publisherOrg, "foaf", "name", publisherName);
        		    this.addObjectProperty(model, proc, publisherOrg, "dcterms", "publisher");
    		    }
        		if (year != null) {
    		    	if (month != null) {
    		    		this.addDatatypeLiteral(model, proc, "dcterms", "issued", year + " " + month);
    		    	} else {
    		    		this.addDatatypeLiteral(model, proc, "dcterms", "issued", year);
    		    	}    		    			    
    		    }        		      	
        		String refListURL = null;
    		    String[] params = {GlobalArticleConfig.listOfEditorsAndTranslators, ""};		    		           		
        		if (pubmedReference != null) {
    				params[1] = pubmedReference;
    				refListURL = Conversion.replaceParameter(this.global.BASE_URL_LIST_PUBMED, params);    				
        		} else if (doiReference != null) {
        			params[1] = doiReference;
    				refListURL = Conversion.replaceParameter(this.global.BASE_URL_LIST_DOI, params);    				
        		} else {
        			params[1] = this.getRefId(ref);
    				refListURL = Conversion.replaceParameter(this.global.BASE_URL_REF_LIST, params);    				
        		}
        		if ((listOfAuthorsRef != null) && (listOfAuthorsRef.size() != 0)) {  
        			for(org.ontoware.rdfreactor.schema.rdfs.Class agent: listOfAuthorsRef) {
        				if (agent instanceof Thing) {
        					this.addObjectProperty(model, agent, publicationLink, "foaf", "publications");
        				}
        			}        			
        			if (clazz.equals(MixedCitation.class)) {
        				ListOfElements loaRef = null;
            			loaRef = new ListOfElements(model, refListURL, true); 
            			loaRef.addMembersInOrder(model, listOfAuthorsRef); //add members to list
            			this.addObjectProperty(model, proc, loaRef, "bibo", "authorList");
            		}    	    				                    		     	
            	}
            	this.addObjectProperty(model, docReference, proc, "bibo", "reproducedIn");
    		    //conference
    		    String confYear = confDate != null ? confDate.substring(0,4).replaceAll(" ", "") : "";
    		    String confNameId = confName != null ? confName.replaceAll(" ", "") : "";
    		    String confLocId = confLoc != null ? confLoc.replaceAll(" ", "") : "";
    		    String confId = (confNameId + "_" + confLocId + "_" + confYear).replaceAll(RDFHandler.CHAR_NOT_ALLOWED, "-");
    		    Thing conf = new Thing(model, MappingConfig.getClass(this.bioteaBase, "bibo", "Conference"), conferenceBaseURL + confId, true); 
		    	this.addDatatypeLiteral(model, conf, "foaf", "name", confName);    		    	
		    	this.addDatatypeLiteral(model, conf, "dcterms", "created", confDate);
    		    this.addObjectProperty(model, docReference, conf, "bibo", "presentedAt");
	    	} else if (type == ReferenceType.CONFERENCE_PROCS) { //only proceedings (book)
	    		this.addDatatypeLiteral(model, docReference, "dcterms", "titel", sourceTitle);
    		    //conference
    		    String confYear = confDate != null ? confDate.substring(0,4).replaceAll(" ", "") : "";
    		    String confNameId = confName != null ? confName.replaceAll(" ", "") : "";
    		    String confLocId = confLoc != null ? confLoc.replaceAll(" ", "") : "";
    		    String confId = (confNameId + "_" + confLocId + "_" + confYear).replaceAll(RDFHandler.CHAR_NOT_ALLOWED, "-");
    		    Thing conf = new Thing(model, MappingConfig.getClass(this.bioteaBase, "bibo", "Conference"), conferenceBaseURL + confId, true); 
		    	this.addDatatypeLiteral(model, conf, "foaf", "name", confName);
		    	this.addDatatypeLiteral(model, conf, "dcterms", "created", confDate);
    		    docReference.addbiboProducedin(conf);    			
	    	} else if (type == ReferenceType.PATENT) {
	    		this.addDatatypeLiteral(model, docReference, "dcterms", "title", articleTitle);
    		    if (sourceTitle != null) {
    		    	docReference.addComment("Source: " + sourceTitle);
    		    }
    		    docReference.addComment("Note: Inventors, asignees, and others are described in the Authors list.");
	    	} else if (type == ReferenceType.DATABASE) { //webpage
	    		this.addDatatypeLiteral(model, docReference, "dcterms", "title", sourceTitle);
	    	} else if (type == ReferenceType.WEBPAGE) { //webpage
	    		this.addDatatypeLiteral(model, docReference, "dcterms", "title", sourceTitle);
	    	} else if (type == ReferenceType.COMMUN) { //personal communication
	    		this.addDatatypeLiteral(model, docReference, "dcterms", "title", sourceTitle);
	    	} else if (type == ReferenceType.DISCUSSION) { //webpage with article title
	    		this.addDatatypeLiteral(model, docReference, "dcterms", "title", articleTitle);	    		
    		    if (sourceTitle != null) {
    		    	docReference.addComment("Discussion/List group: " + sourceTitle);
    		    }
	    	} else if (type == ReferenceType.BLOG) { //webpage
	    		this.addDatatypeLiteral(model, docReference, "dcterms", "title", sourceTitle);
	    	} else if (type == ReferenceType.WIKI) { //webpage
	    		this.addDatatypeLiteral(model, docReference, "dcterms", "title", sourceTitle);
	    	} else if (type == ReferenceType.SOFTWARE) { //standard
	    		this.addDatatypeLiteral(model, docReference, "dcterms", "title", sourceTitle);
	    	} else if (type == ReferenceType.STANDARD) { //standard
	    		this.addDatatypeLiteral(model, docReference, "dcterms", "title", sourceTitle);
	    	} else if (type == ReferenceType.OTHER) { //standard
	    		this.addDatatypeLiteral(model, docReference, "dcterms", "title", sourceTitle);
	    	} 		
    		//common
			this.addDatatypeLiteral(model, docReference, "bibo", "numPages", numPages);		    
    		this.addDatatypeLiteral(model, docReference, "dcterms", "title", transTitle);
    		this.addDatatypeLiteral(model, docReference, "bibo", "edition", edition);
		    if (publisherName != null) {
		    	Thing publisherOrg = new Thing(model, MappingConfig.getClass(this.bioteaBase, "foaf", "Organization"),
		    			this.global.BASE_URL_PUBLISHER_NAME + publisherName.replaceAll(RDFHandler.CHAR_NOT_ALLOWED, "-"), true );
		    	this.addDatatypeLiteral(model, publisherOrg, "foaf", "name", publisherName);
		    	this.addObjectProperty(model, docReference, publisherOrg, "dcterms", "publisher");
		    }
    		if (year != null) {
		    	if (month != null) {
		    		this.addDatatypeLiteral(model, docReference, "dcterms", "issued", year + " " + month);		    		
		    	} else {
		    		this.addDatatypeLiteral(model, docReference, "dcterms", "issued", year);
		    	}    		    			    
		    }
	    	this.addDatatypeLiteral(model, docReference, "bibo", "pageStart", pageStart);
	    	this.addDatatypeLiteral(model, docReference, "bibo", "pageEnd", pageEnd);
		    if (comment != null) {
		    	docReference.addComment(comment);
		    }
		    if (dateInCitation != null) {
		    	docReference.addComment("Access date (aka date in citation): " + dateInCitation);
		    }
		    if (accessDate != null) {
		    	docReference.addComment("Access date: " + accessDate);
		    }
		    //list of authors in rdf		    
    		String refAuthorsURL = null;
		    String[] params3 = {GlobalArticleConfig.listOfAuthors, ""};
    		if (pubmedReference != null) {
				params3[1] = pubmedReference;
				refAuthorsURL = Conversion.replaceParameter(this.global.BASE_URL_LIST_PUBMED, params3);
    		} else if (doiReference != null) {
    			params3[1] = doiReference;
				refAuthorsURL = Conversion.replaceParameter(this.global.BASE_URL_LIST_DOI, params3);
    		} else {
    			params3[1] = this.getRefId(ref);
				refAuthorsURL = Conversion.replaceParameter(this.global.BASE_URL_REF_LIST, params3);
    		}
        	if ((listOfAuthorsRef != null) && (listOfAuthorsRef.size() != 0)) {
        		for(org.ontoware.rdfreactor.schema.rdfs.Class agent: listOfAuthorsRef) {
    				if (agent instanceof Thing) {
    					this.addObjectProperty(model, agent, publicationLink, "foaf", "publications");
    				}
    			}  
        		ListOfElements loaRef = null;
        		if (type != ReferenceType.CONFERENCE_PROCS) { // authors belongs to paper rather than proceedings        			
        			loaRef = new ListOfElements(model, refAuthorsURL, true);    	    				        
            		loaRef.addMembersInOrder(model, listOfAuthorsRef); //add members to list
            		this.addObjectProperty(model, docReference, loaRef, "bibo", "authorList");
        		}        		
        	}
        	//References of the document
        	this.addCitation(model, document, docReference);
		} catch (Exception e ) {
		}    		
	}
	/**
	 * Processes an element, it identifies and links to cross-refs as well as dismisses italics, bolds, etc. 
	 * @param obj
	 * @return
	 */
	@SuppressWarnings("rawtypes")
	private String processElement(Model model, Thing document, Object obj, Thing secDoco, String secDocoURI) {
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
					this.addObjectProperty(model, document, str, "dcterms", "references");
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
				processFigureTable(model, link, secDoco, secDocoURI, document, basePaper, "Figure");
				return "(see also " + link + ")";
			} catch (Exception e) {
				return "";
			}
		} else if (obj instanceof InlineFormula) {
			InlineFormula formula = (InlineFormula)obj;	
			try {
				InlineGraphic g = (InlineGraphic)(formula.getContent().get(0));
				String link = global.INLINE_FORM_FIG_LINK + g.getHref();
				processFigureTable(model, link, secDoco, secDocoURI, document, basePaper, "Figure");				
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
				processFigureTable(model, link, secDoco, secDocoURI, document, basePaper, "Figure");				
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
	private void processFigureTable(Model model, String link, Thing elem, String elemURI, Thing doc, String docURI, String type) {
		Thing imageTable = new Thing(model, MappingConfig.getClass(this.bioteaBase, "doco", type), link, true);
		this.addObjectProperty(model, elem, link, "dcterms", "references");
		this.addObjectProperty(model, doc, link, "dcterms", "references");
		this.addObjectProperty(model, imageTable, elemURI, "dcterms", "isReferencedBy");
		this.addObjectProperty(model, imageTable, docURI, "dcterms", "isReferencedBy");
	}
	/**
	 * Processes a list of objects.
	 * @param list
	 * @return
	 */
	private String processListOfElements(Model model, Thing document, List<Object> list, Thing secDoco, String secDocoURI){
		String str = "";
		for (Object obj: list) {
			str += processElement(model, document, obj, secDoco, secDocoURI);
		}
		return str;
	}
	
	private String getAbstractText(Model model, Thing document, Abstract ab) {
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
	private String processParagraphInAbstractAsText(Model model, Thing document, P para) {
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
	private String processAbstractAsSection(Abstract ab, Model model, Thing document) {
		//Title
		String title = "Abstract";
		//add section
		String secDocoURI = global.BASE_URL_SECTION + title;
		Thing secDoco = new Thing(model, MappingConfig.getClass(this.bioteaBase, "doco", "Section"), secDocoURI, true);
		this.mainListOfSections = this.linkSection(model, document, secDoco, this.mainListOfSections, this.mainListOfSectionsURI);
		//title
		this.addDatatypeLiteral(model, secDoco, "dcterms", "title", title);
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
		Thing paragraph = new Thing(model, MappingConfig.getClass(this.bioteaBase, "doco", "Paragraph"), paragraphURI, true);
		
		String[] params = {"listOfParagraphs", "" + this.paragraphCounter};
		secDoco.setListOfParagraphs(this.linkParagraph(model, secDoco, paragraph, text, secDoco.getListOfParagraphs(), Conversion.replaceParameter(global.BASE_URL_LIST_PMC, params)));		
		
		return text;
	}
	/**
	 * Processes a paragraph within the abstract.
	 * @param secDoco Abstract as section
	 * @param secDocoURI Abstract URI
	 * @param para Paragraph to be processed
	 * @return
	 */
	private String processParagraphInAbstract(Model model, Thing document, Thing secDoco, String secDocoURI, P para) {
		String text = "";
		//text
		List<String[]> references = new ArrayList<String[]>();
		for (Object paraObj: para.getContent()) {
			String str = processElement(model, document, paraObj, null, null); 
			if (str.startsWith("http")) {
				this.addObjectProperty(model, secDoco, str, "dcterms", "references");
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
					this.addCitation(model, secDoco, global.BASE_URL_REF + ref);
				} else if (ref.startsWith("Fig") || ref.startsWith("fig")){
					processFigureTable(model, GlobalArticleConfig.pmcURI + pmcID + "/figure/" + ref, secDoco, secDocoURI, document, basePaper, "Figure");
				} else if (ref.startsWith("Tab") || ref.startsWith("tab")) {
					processFigureTable(model, GlobalArticleConfig.pmcURI + pmcID + "/table/" + ref, secDoco, secDocoURI, document, basePaper, "Table");
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
	private void processSection(Sec section, Model model, Thing document, Object parent, String parentTitleInURL) {
		if ( (parent != null) && !(parent instanceof Thing) ) {
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
		Thing secDoco;
		if (parent == null) {
			secDoco = new Thing(model, MappingConfig.getClass(this.bioteaBase, "doco", "Section"), global.BASE_URL_SECTION + titleInURL, true);
		} else {
			secDoco = new Thing(model, MappingConfig.getClass(this.bioteaBase, "doco", "Section"), global.BASE_URL_SECTION + parentTitleInURL + "_" + titleInURL, true);
		}
		this.mainListOfSections = this.linkSection(model, document, secDoco, this.mainListOfSections, this.mainListOfSectionsURI);
		//title
		this.addDatatypeLiteral(model, secDoco, "dcterms", "title", title);
		//link to parent
		if (parent != null) {			
			if (parent instanceof Thing) {
				String[] params = {"listOfSections", "" + this.sectionCounter};
				ListOfElements loe = this.linkSection(model, (Thing)parent, secDoco, ((Thing)parent).getListOfSections(), Conversion.replaceParameter(global.BASE_URL_LIST_PMC, params));
				((Thing)parent).setListOfSections(loe);
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
				
		//process sections
		Iterator<Sec> itr = section.getSecs().iterator();
		while (itr.hasNext()){
			Sec sec = itr.next();
			this.sectionCounter++;
			if (parent == null) {
				processSection(sec, model, document, secDoco, titleInURL);
			} else {
				processSection(sec, model, document, secDoco, parentTitleInURL + "_" + titleInURL);
			}			
	    }
	}	
	
	private boolean processElementsInSection(Model model, Thing document, String titleInURL, Thing secDoco, Iterator<Object> itrPara) {
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
								this.addObjectProperty(model, document, str, "dcterms", "references");
								this.addObjectProperty(model, secDoco, str, "dcterms", "references");
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
	
	private void processParagraph(Model model, Thing document, String titleInURL, int countPara, Thing secDoco, P para) {
		String[] params = {titleInURL};
		String paragraphURI = Conversion.replaceParameter(global.BASE_URL_PARAGRAPH, params) + countPara;
		Thing paragraph = new Thing(model, MappingConfig.getClass(this.bioteaBase, "doco", "Paragraph"), paragraphURI, true);
					
		String text = "";
		//text
		List<String[]> references = new ArrayList<String[]>();
		for (Object paraObj: para.getContent()) {
			String str = processElement(model, document, paraObj, secDoco, global.BASE_URL_SECTION + titleInURL); 
			if (str.startsWith("http")) {
				this.addObjectProperty(model, paragraph, str, "dcterms", "references");
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
					this.addCitation(model, paragraph, global.BASE_URL_REF + ref);
				} else if (ref.startsWith("Fig") || ref.startsWith("fig")) {
					processFigureTable(model, global.BASE_URL_EXT_FIGURE + ref, paragraph, paragraphURI, document, basePaper, "Figure");
				} else if (ref.startsWith("Tab") || ref.startsWith("tab")) {
					processFigureTable(model, global.BASE_URL_EXT_TABLE + ref, paragraph, paragraphURI, document, basePaper, "Table");
				} 
			}
		}
		
		String[] paramsPara = {"listOfParagraphs", "" + this.paragraphCounter};
		secDoco.setListOfParagraphs(this.linkParagraph(model, secDoco, paragraph, text, secDoco.getListOfParagraphs(), Conversion.replaceParameter(global.BASE_URL_LIST_PMC, paramsPara)));
	}
	
	private void addCitation(Model model, Thing origin, String reference) {
		this.addObjectProperty(model, origin, reference, "bibo", "cites");
		this.addObjectProperty(model, reference, origin, "bibo", "citedBy");
	}
	
	private void addCitation(Model model, Thing origin, Thing reference) {
		this.addObjectProperty(model, origin, reference, "bibo", "cites");
		this.addObjectProperty(model, reference, origin, "bibo", "citedBy");
	}
	
	private void addHasPartIsPartOf(Model model, Thing whole, Thing element) {
		this.addObjectProperty(model, whole, element, "dcterms", "hasPart");
		this.addObjectProperty(model, element, whole, "dcterms", "isPartOf");
	}
	
	private ListOfElements linkSection(Model model, Thing document, Thing secDoco, ListOfElements loe, String uri) {
		this.addHasPartIsPartOf(model, document, secDoco);
		if (loe == null) {
			loe = new ListOfElements(model, uri, true); 
			this.addObjectProperty(model, document, loe, "biotea", "sectionList");
		}
		loe.addMember(model, secDoco, true);
		return loe;
	}
	
	private ListOfElements linkParagraph(Model model, Thing secDoco, Thing paragraph, String text, ListOfElements loe, String uri) {
		this.addHasPartIsPartOf(model, secDoco, paragraph);
		this.addDatatypeLiteral(model, paragraph, "rdf", "value", text);
				
		if (loe == null) {
			loe = new ListOfElements(model, uri, true); 
			this.addObjectProperty(model, secDoco, loe, "biotea", "paragraphList");
		}
		loe.addMember(model, paragraph, true);
		return loe;
	}
}
