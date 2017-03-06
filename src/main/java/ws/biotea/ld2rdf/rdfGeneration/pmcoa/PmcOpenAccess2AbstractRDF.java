/**
 * 
 */
package ws.biotea.ld2rdf.rdfGeneration.pmcoa;  

import org.apache.log4j.Logger;
import org.ontoware.rdf2go.exception.ModelRuntimeException;
import org.ontoware.rdf2go.model.Model;
import org.ontoware.rdf2go.model.Statement;
import org.ontoware.rdf2go.model.node.Node;
import org.ontoware.rdf2go.model.node.PlainLiteral;
import org.ontoware.rdf2go.model.node.URI;
import org.ontoware.rdf2go.model.node.impl.URIImpl;

import pubmed.openAccess.jaxb.generated.*;
import ws.biotea.ld2rdf.rdf.model.bibo.Document;
import ws.biotea.ld2rdf.rdf.model.bibo.Thing;
import ws.biotea.ld2rdf.rdf.model.bibo.extension.*;
import ws.biotea.ld2rdf.rdfGeneration.Publication2RDF;
import ws.biotea.ld2rdf.rdfGeneration.RDFHandler;
import ws.biotea.ld2rdf.rdfGeneration.exception.ArticleTypeException;
import ws.biotea.ld2rdf.rdfGeneration.exception.DTDException;
import ws.biotea.ld2rdf.rdfGeneration.exception.PMCIdException;
import ws.biotea.ld2rdf.rdfGeneration.jats.GlobalArticleConfig;
import ws.biotea.ld2rdf.util.Conversion;
import ws.biotea.ld2rdf.util.ResourceConfig;
import ws.biotea.ld2rdf.util.mapping.DatatypeProperty;
import ws.biotea.ld2rdf.util.mapping.MappingConfig;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.util.Calendar;
import java.util.List;

public abstract class PmcOpenAccess2AbstractRDF implements Publication2RDF {	
	protected int nodeCounter;
	protected Logger logger;	
	protected final String NON_INITIAL = "[a-z ,;.-]";
	protected int elementCount;
	//Basic paper
	protected Article article;
	protected String basePaper;
	protected String pubmedID;
	protected String pmcID;
	protected String articleType;
	protected String doi;	
	protected String articleTitle;
	protected GlobalArticleConfig global;
	protected final String PREFIX = ResourceConfig.getDatasetPrefix().toUpperCase();
	//Sections
	protected int sectionCounter = 1;
	protected int paragraphCounter = 1;
	protected final String[] listOfsectionsParam = {"listOfSections", "1"};
	protected final String mainListOfSectionsURI;
	
	protected PmcOpenAccess2AbstractRDF(File paper, StringBuilder str) throws JAXBException, DTDException, ArticleTypeException, PMCIdException {
		this.logger = Logger.getLogger(this.getClass());
		this.nodeCounter = 0;
		this.elementCount = 1;
		
		JAXBContext jc = JAXBContext.newInstance("pubmed.openAccess.jaxb.generated");
		Unmarshaller unmarshaller = jc.createUnmarshaller(); 	
		try {
			this.article = (Article) unmarshaller.unmarshal(paper);
		} catch (Exception e) {
			logger.fatal("- FATAL DTD ERROR - " + paper.getName() + " cannot be unmarshalled: " + e.getMessage());
			throw new DTDException (e);
		}
		this.articleType = "";
		try {
			this.articleType = this.article.getArticleType();			
		} catch (Exception e) {
			throw new ArticleTypeException ("Article type is undefined: " + e.getMessage());
		}
		if (this.articleType == null) {
			throw new ArticleTypeException ("Article type is null, file cannot be processed");
		}		
		//url
		this.pubmedID = null; this.doi = null;
		for(pubmed.openAccess.jaxb.generated.ArticleId id: article.getFront().getArticleMeta().getArticleIds()) {
			if (id.getPubIdType().equals("pmid")) {
				this.pubmedID = id.getContent();
			} else if (id.getPubIdType().equals("doi")) {
				this.doi = id.getContent();
			} else if (id.getPubIdType().equals("pmc")) {
				this.pmcID = id.getContent();
			}				
		}
		
		if ((this.pmcID == null) && (ResourceConfig.getIdTag().equals("pmc"))) {
			throw new PMCIdException("No " + PREFIX + " id was found, file cannot be processed");
		} 
		if ((this.pubmedID == null) && (ResourceConfig.getIdTag().equals("pmid"))) {
			throw new PMCIdException("No " + PREFIX + " id was found, file cannot be processed");
		}
		
		str.delete(0, str.length());
		if (ResourceConfig.getIdTag().equals("pmc")) {
			str.append(this.pmcID); //file name
		} else if (ResourceConfig.getIdTag().equals("pmid")) { //TODO try it out!
			str.append(this.pubmedID); //file name
		} else { //TODO how to support others?
			throw new PMCIdException("No valid " + PREFIX + " field " + ResourceConfig.getIdTag() + 
				" was configured, file cannot be processed");
		}
		
		this.logger.info("=== ARTICLE-TYPE (" + paper.getName() + " - " + pmcID + "): " + this.articleType);
		this.global = new GlobalArticleConfig(pmcID);	
		this.basePaper = GlobalArticleConfig.getArticleRdfUri(pmcID);
		this.mainListOfSectionsURI = Conversion.replaceParameter(global.BASE_URL_LIST_PMC, listOfsectionsParam);
	} 
	
	/**
	 * Creates and open an RDF model.
	 * @return
	 */
	protected abstract Model createAndOpenModel();

	/**
	 * Closes and serializes an RDF model with UTF-8 enconding.
	 * @param myModel
	 * @param outputFile
	 * @throws FileNotFoundException
	 * @throws UnsupportedEncodingException
	 */
	protected File serializeAndCloseModel(Model myModel, String outputFile) throws FileNotFoundException, UnsupportedEncodingException {
		File file = new File(outputFile);
	    PrintWriter pw = new PrintWriter(file, ResourceConfig.UTF_ENCODING);
	    try {
			myModel.writeTo(pw);
		} catch (ModelRuntimeException e) {
			logger.error("===ERROR=== model serialization (ModelRuntimeException): " + e.getMessage());
			throw new FileNotFoundException("ModelRuntimeException " + e.getMessage());
		} catch (IOException e) {
			logger.error("===ERROR=== model serialization (IOException): " + e.getMessage());
			throw new FileNotFoundException("IOException " + e.getMessage());
		}
	    myModel.close();
	    pw.close();
		return file;
	}
	
	/**
	 * Adds a datatype property to the model from document to literal, the datatype property is specified by a namespace and dtpName.
	 * It checks the mapping properties to see if the dtp can be directly mapped (dtp.isReified() false). 
	 * If not (dtp.isReified() true), it adds an extra level to "reify" the dtp.
	 * @param model
	 * @param document
	 * @param namespace
	 * @param dtpName
	 * @param literal
	 */
	protected void addDatatypeLiteral(Model model, org.ontoware.rdfreactor.schema.rdfs.Class document, String namespace, String dtpName, String literal) {
		if ((literal != null) && (literal.length() != 0)) {
			DatatypeProperty dtp = MappingConfig.getDatatypeProperty(namespace, dtpName);
			if (dtp != null) {
				PlainLiteral descAsLiteral = model.createPlainLiteral(literal);
				if (dtp.isReified()) {
					String nodeURL = global.BASE_URL_OTHER + namespace + "_" + dtpName + "_node" + (++nodeCounter) + "_" + Calendar.getInstance().getTimeInMillis(); 
					Thing node = new Thing(model, dtp.getClassName(), nodeURL, true );
					URI uri = new URIImpl(dtp.getOpName(), false);
					model.addStatement(document.asResource(), uri, node.asResource());
					Statement stm = model.createStatement(node.asResource(), new URIImpl(dtp.getDtpName(), false), descAsLiteral);
				    model.addStatement(stm);
				} else {
					Statement stm = model.createStatement(document.asResource(), new URIImpl(dtp.getDtpName(), false), descAsLiteral);
				    model.addStatement(stm);
				}				
			}
		}		
	}
	
	/**
	 * Adds an object property to the model from the RDF class "from" to the RDF class "to", the object property is specified by a namespace and an opName.
	 * @param model
	 * @param from
	 * @param to
	 * @param namespace
	 * @param opName
	 */
	protected void addObjectProperty(Model model, org.ontoware.rdfreactor.schema.rdfs.Class from, org.ontoware.rdfreactor.schema.rdfs.Class to, String namespace, String opName) {
		String str = MappingConfig.getObjectProperty(namespace, opName);
		if (str != null) {
			URI uri = new URIImpl(str, false);
			model.addStatement(from.asResource(), uri, to.asResource());
		}
	}
	
	/**
	 * Adds an object property to the model from the RDF class "from" to URL string "to", the object property is specified by a namespace and an opName.
	 * @param model
	 * @param from
	 * @param to
	 * @param namespace
	 * @param opName
	 */
	protected void addObjectProperty(Model model, org.ontoware.rdfreactor.schema.rdfs.Class from, String to, String namespace, String opName) {
		Node uriNodeTo = model.createURI(to);
		String str = MappingConfig.getObjectProperty(namespace, opName);
		if (str != null) {
			URI uri = new URIImpl(str, false);
			model.addStatement(from.asResource(), uri, uriNodeTo);
		}
	}
	
	/**
	 * Adds an object property to the model from the URL string "from" to the RDF class "to", the object property is specified by a namespace and an opName.
	 * @param model
	 * @param from
	 * @param to
	 * @param namespace
	 * @param opName
	 */
	protected void addObjectProperty(Model model, String from, org.ontoware.rdfreactor.schema.rdfs.Class to, String namespace, String opName) {
		Node uriNodeFrom = model.createURI(from);
		String str = MappingConfig.getObjectProperty(namespace, opName);
		if (str != null) {
			URI uri = new URIImpl(str, false);
			model.addStatement(uriNodeFrom.asResource(), uri, to.asResource());
		}
	}
	
	/**
	 * Adds an object property to the model from the URL string "from" to the string URL "to", the object property is specified by URL string.
	 * Use carefully as the URL string property could be anything!
	 * @param model
	 * @param from
	 * @param to
	 * @param property
	 */
	protected void addObjectProperty(Model model, Thing from, String to, String property) {
		Node uriNodeTo = model.createURI(to);
		URI uriProperty = new URIImpl(property, false);
		model.addStatement(from.asResource(), uriProperty, uriNodeTo);
	}
	
	/**
	 * Gets the reference id without non-allowed chars.
	 * @param ref
	 * @return
	 */
	protected String getRefId(Ref ref) {
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
	
	protected Thing createPerson(Model model, String personBaseURL, Name name, boolean asThing) {
		String givenNames = "";
		try {
			givenNames = name.getGivenNames().getContent().get(0).toString();
		} catch (Exception e) {}
		String surname = name.getSurname().getContent().get(0).toString();
		String idPerson = givenNames.replaceAll(NON_INITIAL, "") + surname;
		idPerson = idPerson.replaceAll(RDFHandler.CHAR_NOT_ALLOWED, "-");
		if (asThing) {
			Thing person = new Thing(model, MappingConfig.getClass("foaf", "Person"), personBaseURL + idPerson, true);
			this.addDatatypeLiteral(model, person, "foaf", "name", givenNames + " " + surname);
			this.addDatatypeLiteral(model, person, "foaf", "givenName", givenNames);
			this.addDatatypeLiteral(model, person, "foaf", "familyName", surname);
		    return person;
		} else {
			PersonE person = new PersonE(model, personBaseURL + idPerson, true);
		    person.addName(model, givenNames + " " + surname);
		    person.addGivenName(model, givenNames);
		    person.addFamilyName(model, surname);
		    return person;
		}
	}
	/**
	 * Gets the reference type from the publication type.
	 * @param citation
	 * @return
	 */
	private ReferenceType getReferenceTypeFromPublication(GenericCitation citation) {
		if (citation.getPublicationType().contains("book")) {
    		return ReferenceType.BOOK;
    	} else if (citation.getPublicationType().contains("journal")) {
    		return ReferenceType.JOURNAL_ARTICLE;
    	} else if (citation.getPublicationType().contains("confproc")) {
    		return ReferenceType.CONFERENCE_PROCS;
    	} else if (citation.getPublicationType().contains("thesis")) {
    		return ReferenceType.THESIS;
    	} else if (citation.getPublicationType().contains("report")) {
    		return ReferenceType.REPORT;
    	} else if (citation.getPublicationType().contains("gov")) {
    		return ReferenceType.GOV;
    	} else if (citation.getPublicationType().contains("patent")) {
    		return ReferenceType.PATENT;
    	} else if (citation.getPublicationType().contains("standard")) {
    		return ReferenceType.STANDARD;
    	} else if (citation.getPublicationType().contains("web")) {
    		return ReferenceType.WEBPAGE;
    	} else if (citation.getPublicationType().contains("discussion")) {
    		return ReferenceType.DISCUSSION;
    	} else if (citation.getPublicationType().contains("list")) {
    		return ReferenceType.DISCUSSION;
    	} else if (citation.getPublicationType().contains("commun")) {
    		return ReferenceType.COMMUN;
    	} else if (citation.getPublicationType().contains("blog")) {
    		return ReferenceType.BLOG;
    	} else if (citation.getPublicationType().contains("wiki")) {
    		return ReferenceType.WIKI;
    	} else if (citation.getPublicationType().contains("database")) {
    		return ReferenceType.DATABASE;
    	} else if (citation.getPublicationType().equals("other")) {
    		return ReferenceType.OTHER;	    		
    	} else {
    		return null;	    		
    	}
	}
	/**
	 * Gets the reference type from the citation type.
	 * @param citation
	 * @return
	 */
	private ReferenceType getReferenceTypeFromCitation(Citation citation) {
		if (citation.getCitationType().contains("book")) {
    		return ReferenceType.BOOK;
    	} else if (citation.getCitationType().contains("journal")) {
    		return ReferenceType.JOURNAL_ARTICLE;
    	} else if (citation.getCitationType().contains("confproc")) {
    		return ReferenceType.CONFERENCE_PROCS;
    	} else if (citation.getCitationType().contains("thesis")) {
    		return ReferenceType.THESIS;
    	} else if (citation.getCitationType().contains("report")) {
    		return ReferenceType.REPORT;
    	} else if (citation.getCitationType().contains("gov")) {
    		return ReferenceType.GOV;
    	} else if (citation.getCitationType().contains("patent")) {
    		return ReferenceType.PATENT;
    	} else if (citation.getCitationType().contains("standard")) {
    		return ReferenceType.STANDARD;
    	} else if (citation.getCitationType().contains("web")) {
    		return ReferenceType.WEBPAGE;
    	} else if (citation.getCitationType().contains("discussion")) {
    		return ReferenceType.DISCUSSION;
    	} else if (citation.getCitationType().contains("list")) {
    		return ReferenceType.DISCUSSION;
    	} else if (citation.getCitationType().contains("commun")) {
    		return ReferenceType.COMMUN;
    	} else if (citation.getCitationType().contains("blog")) {
    		return ReferenceType.BLOG;
    	} else if (citation.getCitationType().contains("wiki")) {
    		return ReferenceType.WIKI;
    	} else if (citation.getCitationType().contains("database")) {
    		return ReferenceType.DATABASE;
    	} else if (citation.getCitationType().equals("other")) {
    		return ReferenceType.OTHER;	    		
    	} else {
    		return null;	    		
    	}
	}
	/**
	 * Processes a Citation type and creates the bibliographic reference.
	 * @param citation
	 * @param ref
	 */
	protected void processSimpleCitation(Model model, Thing document, Citation citation, Ref ref, boolean withMetadata) {		
		try {	
			ReferenceType refType = getReferenceTypeFromPublication(citation); 
			if (refType != null) {
				processReferenceAllTypeCitation(model, document, ref, citation.getAll(), refType, Citation.class, withMetadata);
			} else {
				logger.warn("WARNING - reference " + this.getRefId(ref) + " could not be processed (type not recognized)");
			}
    	} catch (Exception e) {
    		try {
    			ReferenceType refType = getReferenceTypeFromCitation(citation); 
    			if (refType != null) {
    				processReferenceAllTypeCitation(model, document, ref, citation.getAll(), refType, Citation.class, withMetadata);
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
	protected void processNlmCitation(Model model, Thing document, NlmCitation citation, Ref ref, boolean withMetadata) {
		try {	
			ReferenceType refType = getReferenceTypeFromPublication(citation); 
			if (refType != null) {
				processReferenceAllTypeCitation(model, document, ref, citation.getAll(), refType, NlmCitation.class, withMetadata);
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
	protected void processMixedCitation(Model model, Thing document, MixedCitation citation, Ref ref, boolean withMetadata) {
		try {	
			ReferenceType refType = getReferenceTypeFromPublication(citation); 
			if (refType != null) {
				processReferenceAllTypeCitation(model, document, ref, citation.getContent(), refType, MixedCitation.class, withMetadata);
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
	protected void processElementCitation(Model model, Thing document, ElementCitation citation, Ref ref, boolean withMetadata) {
		try {	    	
			ReferenceType refType = getReferenceTypeFromPublication(citation); 
			if (refType != null) {
				processReferenceAllTypeCitation(model, document, ref, citation.getInlineSupplementaryMaterialsAndRelatedArticlesAndRelatedObjects(), refType, ElementCitation.class, withMetadata);
			} else {
				logger.warn("WARNING - reference " + this.getRefId(ref) + " could not be processed (type not recognized)");
			}	
    	} catch (Exception e) {
    		logger.warn("WARNING - reference " + this.getRefId(ref) + " could not be processed: " + e.getMessage());
    	}
	}
	protected abstract void processReferenceAllTypeCitation(Model model, Thing document, Ref ref, List<Object> content, ReferenceType type, Class<?> clazz, boolean withMetadata);
	protected abstract void processReferenceAllTypeCitation(Model model, Document document, Ref ref, List<Object> content, ReferenceType type, Class<?> clazz, boolean withMetadata);
	/**
	 * Processes the affiliation for authors, creates a group and add the members.
	 * Note: not in use
	 * @param xref
	 * @param Agent
	 */
	/*
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
	*/
}
