/**
 * 
 */
package ws.biotea.ld2rdf.rdfGeneration.pmcoa;  

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;

import org.ontoware.rdf2go.RDF2Go;
import org.ontoware.rdf2go.model.Model;
import org.ontoware.rdf2go.model.node.Node;
import org.ontoware.rdf2go.model.node.impl.PlainLiteralImpl;

import pubmed.openAccess.jaxb.generated.Abstract;
import pubmed.openAccess.jaxb.generated.Bold;
import pubmed.openAccess.jaxb.generated.ChemStruct;
import pubmed.openAccess.jaxb.generated.ExtLink;
import pubmed.openAccess.jaxb.generated.Fig;
import pubmed.openAccess.jaxb.generated.Graphic;
import pubmed.openAccess.jaxb.generated.InlineFormula;
import pubmed.openAccess.jaxb.generated.InlineGraphic;
import pubmed.openAccess.jaxb.generated.Italic;
import pubmed.openAccess.jaxb.generated.ListItem;
import pubmed.openAccess.jaxb.generated.NamedContent;
import pubmed.openAccess.jaxb.generated.P;
import pubmed.openAccess.jaxb.generated.Ref;
import pubmed.openAccess.jaxb.generated.Sc;
import pubmed.openAccess.jaxb.generated.Sec;
import pubmed.openAccess.jaxb.generated.Sub;
import pubmed.openAccess.jaxb.generated.Sup;
import pubmed.openAccess.jaxb.generated.SupplementaryMaterial;
import pubmed.openAccess.jaxb.generated.TableWrap;
import pubmed.openAccess.jaxb.generated.Underline;
import pubmed.openAccess.jaxb.generated.Xref;
import ws.biotea.ld2rdf.rdf.model.bibo.Document;
import ws.biotea.ld2rdf.rdf.model.bibo.Thing;
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
import ws.biotea.ld2rdf.util.Conversion;
import ws.biotea.ld2rdf.util.ResourceConfig;

public class PmcOpenAccessText2PlainText extends PmcOpenAccess2AbstractRDF {
	private PrintWriter outPlainTextPW;
	public PmcOpenAccessText2PlainText(File paper, StringBuilder str, String suffix, String bioteaBase, String bioteaDataset, 
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
			throw new NullPointerException("No pmc id was found, file cannot be processed " + paper.getName());
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
			this.processBasic(model, document, paper.getName());
			logger.info("=== basic processed");
		} catch (Exception e) {//something went so wrong
			logger.fatal("- FATAL ERROR - " + pmcID + " threw an uncaugth error, file " + paper.getName() + ": " + e.getMessage());
			fatalError = true;
		} finally {
			model.close();
			if (fatalError) {		
				logger.info("=== END of rdfization with a fatal error " + pmcID + " (pubmedID: " + pubmedID + "), (doi: " + doi + ")");
			} else {
				outputFile = new File(outputDir + "/" + PREFIX + pmcID + "_" + this.suffix + "_sections.txt");
				this.outPlainTextPW = new PrintWriter(outputFile, ResourceConfig.UTF_ENCODING);
				this.outPlainTextPW.println(PREFIX + pmcID + ".ArticleTitle.Text." + this.articleTitle);
				logger.info("=== END of rdfization OK " + pmcID + " (pubmedID: " + pubmedID + "), (doi: " + doi + ")");
			}
			
		}
		
		//process sections		
		if (sections) {
			Document documentSections;
			Model modelSections;
			boolean fatalErrorSections = false;
			modelSections = createAndOpenModel();
			try {
				documentSections = new Document(modelSections, basePaper, true);
				documentSections.addTitle(modelSections, this.articleTitle);
				
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
				logger.fatal("- FATAL ERROR SECTIONS - " + pmcID + " threw an uncaugth error, file  " + paper.getName() + ":" + e.getMessage());
				fatalErrorSections = true;
			} finally {
				modelSections.close();
				this.outPlainTextPW.close();				
				if (fatalError || fatalErrorSections) {		
					logger.info("=== END of sections rdfization with a fatal error " + pmcID + " (pubmedID: " + pubmedID + "), (doi: " + doi + ")");
				} else {		
					logger.info("=== END of sections rdfization OK " + pmcID + " (pubmedID: " + pubmedID + "), (doi: " + doi + ")");
				}
			}
		}
		return outputFile;
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
	 * Closes and serializes an RDF model with UTF-8 enconding.
	 * @param myModel
	 * @param outputFile
	 * @throws FileNotFoundException
	 * @throws UnsupportedEncodingException
	 */
	protected File serializeAndCloseModel(Model myModel, String outputFile) throws FileNotFoundException, UnsupportedEncodingException {
		return null;
	}
	
	/**
	 * Processes doi, pubmed, journal, and title.
	 */
	private void processBasic(Model model, Document document, String paper) {
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
    	    document.addTitle(model, this.articleTitle);
    	} catch (Exception e) {
    		logger.info(paper + ": Article title not processed");
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
		this.outPlainTextPW.println(PREFIX + pmcID + ".Abstract.para_1.Text." + text);
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
		this.outPlainTextPW.println(PREFIX + pmcID + ".SectionTitle." + titleInURL + ".Text." + title);
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
		this.outPlainTextPW.println(PREFIX + this.pmcID + ".Section." + titleInURL + ".para_" + countPara + ".Text." + text);		
	}

	@Override
	protected void processReferenceAllTypeCitation(Model model, Thing document, Ref ref, List<Object> content,
			ReferenceType type, Class<?> clazz, boolean withMetadata) {	
	}
}
