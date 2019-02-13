package ws.biotea.ld2rdf.rdfGeneration.pmcoa;

import java.util.Calendar;

import org.ontoware.rdf2go.RDF2Go;
import org.ontoware.rdf2go.model.Model;
import org.ontoware.rdf2go.model.Statement;
import org.ontoware.rdf2go.model.node.Node;
import org.ontoware.rdf2go.model.node.PlainLiteral;
import org.ontoware.rdf2go.model.node.Resource;
import org.ontoware.rdf2go.model.node.URI;
import org.ontoware.rdf2go.model.node.impl.URIImpl;

import ws.biotea.ld2rdf.rdf.model.bibo.Document;
import ws.biotea.ld2rdf.rdf.model.bibo.Thing;
import ws.biotea.ld2rdf.rdfGeneration.jats.GlobalArticleConfig;
import ws.biotea.ld2rdf.util.Conversion;
import ws.biotea.ld2rdf.util.HtmlUtil;
import ws.biotea.ld2rdf.util.ResourceConfig;
import ws.biotea.ld2rdf.util.mapping.DatatypeProperty;
import ws.biotea.ld2rdf.util.mapping.MappingConfig;
import ws.biotea.ld2rdf.util.mapping.Namespace;

public class Biotea2Mapping {
	private String mapping;
	private String bioteaBase;
	private String articleType;
	private String pmcID;
	private String pubmedID;
	private String doi;
	private String bioteaDataset;
	
	private GlobalArticleConfig global;
	private int nodeCounter;
	
	public Biotea2Mapping(String mapping, String articleType, String pmcID, String pubmedID, String doi) {
		this.mapping = mapping;
		this.articleType = articleType;
		this.pmcID = pmcID;
		this.pubmedID = pubmedID;
		this.doi = doi;
		
		this.bioteaBase = ResourceConfig.getConfigBase(this.mapping); //TODO: will be empty for schema.org
		this.bioteaDataset = ResourceConfig.getConfigDataset(this.mapping);
		
		this.global = new GlobalArticleConfig(bioteaBase, pmcID);
		this.nodeCounter = 0;
	}
	
	public Model mapArticle(Document artDocument, Model artModel) {
		Model mapModel = createAndOpenModel();
		
		Thing mapDocument;
		String basePaper = GlobalArticleConfig.getArticleRdfUri(this.bioteaBase, this.pmcID);
		String useAsBase = this.bioteaBase;
		if (this.bioteaBase.length() == 0) {
			basePaper = ResourceConfig.getDOIURL() + this.doi;
			useAsBase = this.mapping;
		}
		
		mapDocument = new Thing(mapModel, MappingConfig.getClass(useAsBase, "bibo", "Document"), basePaper, true);
		
		String type = this.articleType.replace('-', '_').toUpperCase();
		if (ArticleType.valueOf(type).getBiboType() != null) {
			mapDocument = new Thing(mapModel, MappingConfig.getClass(useAsBase, "bibo", ArticleType.valueOf(type).getBiboType()), basePaper, true);
		}
		
		this.addDatatypeLiteral(mapModel, mapDocument, "dcterms", "description", this.articleType);
		//TODO: version and on the others too!!!
		mapBasic(artDocument, artModel, mapDocument, mapModel);
		//mapAuthors();
		//mapAbstractAndKeywords();
		//mapReferences();
		return mapModel;
	}
	
	public Model mapSections(Model sections) {
		Model mappedSections = createAndOpenModel();
		return mappedSections;
	}
	
	private Model createAndOpenModel() {
		String useAsBase = this.bioteaBase;
		boolean additional = true;
		if (this.bioteaBase.length() == 0) {
			useAsBase = this.mapping;
			additional = false;
		}
		
		Model myModel = RDF2Go.getModelFactory().createModel();
		myModel.open();	
		for (Namespace namespace: MappingConfig.getAllNamespaces(useAsBase, additional)) {
			myModel.setNamespace(namespace.getNamespace(), namespace.getUrl());
		}
		
		return (myModel);
	}
	
	/**
	 * Adds a datatype property to the mapModel from mapDocument to literal, the datatype property is specified by a namespace and dtpName.
	 * It checks the mapping properties to see if the dtp can be directly mapped (dtp.isReified() false). 
	 * If not (dtp.isReified() true), it adds an extra level to "reify" the dtp.
	 * @param mapModel
	 * @param mapDocument
	 * @param namespace
	 * @param dtpName
	 * @param literal
	 */
	protected void addDatatypeLiteral(Model mapModel, org.ontoware.rdfreactor.schema.rdfs.Class mapDocument, String namespace, String dtpName, String literal) {
		String useAsBase = this.bioteaBase; 
		if (this.bioteaBase.length() == 0) {
			useAsBase = this.mapping;
		}
		if ((literal != null) && (literal.length() != 0)) {
			DatatypeProperty dtp = MappingConfig.getDatatypeProperty(useAsBase, namespace, dtpName);
			if (dtp != null) {
				PlainLiteral descAsLiteral = mapModel.createPlainLiteral(literal);
				if (dtp.isReified()) {
					String nodeURL = global.BASE_URL_OTHER + namespace + "_" + dtpName + "_node" + (++nodeCounter) + "_" + Calendar.getInstance().getTimeInMillis(); 
					Thing node = new Thing(mapModel, dtp.getClassName(), nodeURL, true );
					URI uri = new URIImpl(dtp.getOpName(), false);
					mapModel.addStatement(mapDocument.asResource(), uri, node.asResource());
					Statement stm = mapModel.createStatement(node.asResource(), new URIImpl(dtp.getDtpName(), false), descAsLiteral);
				    mapModel.addStatement(stm);
				} else {
					Statement stm = mapModel.createStatement(mapDocument.asResource(), new URIImpl(dtp.getDtpName(), false), descAsLiteral);
				    mapModel.addStatement(stm);
				}				
			}
		}		
	}
	
	/**
	 * Adds an object property to the mapModel from the RDF class "from" to URL string "to", the object property is specified by a namespace and an opName.
	 * @param mapModel
	 * @param from
	 * @param to
	 * @param namespace
	 * @param opName
	 */
	protected void addObjectProperty(Model mapModel, org.ontoware.rdfreactor.schema.rdfs.Class from, String to, String namespace, String opName) {
		if (this.bioteaBase.length() == 0) {
			this.addDatatypeLiteral(mapModel, from, namespace, opName, to);
			return;
		}
		
		Node uriNodeTo = mapModel.createURI(to);
		String str = MappingConfig.getObjectProperty(this.bioteaBase, namespace, opName);
		if (str != null) {
			URI uri = new URIImpl(str, false);
			mapModel.addStatement(from.asResource(), uri, uriNodeTo);
		}
	}
	
	private void addSeeAlso(Thing mapDocument, Model mapModel, String alsoUri) {
		if (this.bioteaBase.length() == 0) {
			this.addDatatypeLiteral(mapModel, mapDocument, "rdfs", "seeAlso", alsoUri);
		} else {
			Resource resPMC = new org.ontoware.rdf2go.model.node.impl.URIImpl(alsoUri, true);	
			mapDocument.addSeeAlso(resPMC);
		}
	}
	
	private void addSameAs(Thing mapDocument, Model mapModel, String sameAsLink, String[] params) {
		if (this.bioteaBase.length() == 0) {
			this.addDatatypeLiteral(mapModel, mapDocument, "owl", "sameAs", Conversion.replaceParameter(sameAsLink, params));
		} else {
			mapDocument.addSameAs(mapModel, Conversion.replaceParameter(sameAsLink, params));
		}
	}
	
	private void addSameAs(Thing mapDocument, Model mapModel, String sameAsLink) {
		if (this.bioteaBase.length() == 0) {
			this.addDatatypeLiteral(mapModel, mapDocument, "owl", "sameAs", sameAsLink);
		} else {
			mapDocument.addSameAs(mapModel, sameAsLink);
		}		
	}
	
	private void mapBasic(Document artDocument, Model artModel, Thing mapDocument, Model mapModel) {	
		String mainId = "";
		String articleId = this.pmcID;
		if (ResourceConfig.getIdTag().equals("pmc")) {
			articleId = this.pmcID;
			mainId = "pmc:" + this.pmcID;
		} else if (ResourceConfig.getIdTag().equals("pmid")) {
			articleId = this.pubmedID;
			mainId = "pmid:" + this.pubmedID;
		} else if (ResourceConfig.getIdTag().equals("doi") || (this.bioteaBase.length() == 0)) {
			articleId = this.doi;
			mainId = "doi:" + this.doi;
		}
		
		String[] sameAsLinks = ResourceConfig.getConfigSameAs(this.mapping);
		if (sameAsLinks != null) {
			for (String sameAsLink:sameAsLinks) {
				String[] params = {articleId};
				this.addSameAs(mapDocument, mapModel, sameAsLink, params);
			}
		}
		
		this.addDatatypeLiteral(mapModel, mapDocument, "dcterms", "identifier", mainId);
		if (MappingConfig.getIdentifier(this.bioteaBase) != null) {
			//main identifier also goes directly to Model
			PlainLiteral idAsLiteral = mapModel.createPlainLiteral(mainId);
			Statement stm = mapModel.createStatement(mapDocument.asResource(), new URIImpl(MappingConfig.getIdentifier(this.bioteaBase)), idAsLiteral);
		    mapModel.addStatement(stm);		    
		}
		
		this.addObjectProperty(mapModel, mapDocument, GlobalArticleConfig.pmcURI + pmcID, "prov", "wasDerivedFrom");
		String now = HtmlUtil.getDateAndTime();
		this.addDatatypeLiteral(mapModel, mapDocument, "dcterms", "created", now);
		this.addDatatypeLiteral(mapModel, mapDocument, "prov", "generatedAtTime", now);
		this.addObjectProperty(mapModel, mapDocument, GlobalArticleConfig.RDF4PMC_AGENT, "dcterms", "creator");
		this.addObjectProperty(mapModel, mapDocument, GlobalArticleConfig.RDF4PMC_AGENT, "prov", "wasAttributedTo");
		this.addObjectProperty(mapModel, mapDocument, this.bioteaDataset, "sio", "SIO_001278");
		this.addSeeAlso(mapDocument, mapModel, GlobalArticleConfig.pmcURI + pmcID);
		
		
		//pubmedID
		if (pubmedID != null) {
			this.addDatatypeLiteral(mapModel, mapDocument, "bibo", "pmid", "pmid:" + this.pubmedID);
		    //Relations between PMC-RDF and identifiers.org/bio2rdf.org sameAS
		    if (ResourceConfig.withBio()) {
		    	this.addSameAs(mapDocument, mapModel, ResourceConfig.IDENTIFIERS_ORG_PUBMED + pubmedID);
		    	this.addSeeAlso(mapDocument, mapModel, ResourceConfig.IDENTIFIERS_ORG_PAGE_PUBMED + pubmedID);
		    	this.addSameAs(mapDocument, mapModel, ResourceConfig.BIO2RDF_PUBMED + pubmedID);
		    }		    		    		    	
		    //relations between PMC-RDF and PubMed
		    if (this.bioteaBase.length() != 0) {
		    	if (!global.getUriStyle().equals(ResourceConfig.bio2rdf)) {
			    	this.addSameAs(mapDocument, mapModel, this.global.PUBMED_DOCUMENT + pubmedID);
			    }
		    	this.addSeeAlso(mapDocument, mapModel, GlobalArticleConfig.pubMedURI + pubmedID);
		    }		    
		}
		
		//doi
		if (doi != null) {
			this.addDatatypeLiteral(mapModel, mapDocument, "bibo", "doi", "doi:" + this.doi);
			if (this.bioteaBase.length() != 0) {
				this.addSameAs(mapDocument, mapModel, GlobalArticleConfig.doiURI + doi);
				if (!global.getUriStyle().equals(ResourceConfig.bio2rdf)) {
			    	this.addSameAs(mapDocument, mapModel, this.global.DOI_DOCUMENT + doi);
			    }
			}					    
		}
	}

}
