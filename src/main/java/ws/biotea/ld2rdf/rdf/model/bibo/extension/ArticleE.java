package ws.biotea.ld2rdf.rdf.model.bibo.extension;

import org.ontoware.rdf2go.exception.ModelRuntimeException;
import org.ontoware.rdf2go.model.Model;
import org.ontoware.rdf2go.model.Statement;
import org.ontoware.rdf2go.model.node.BlankNode;
import org.ontoware.rdf2go.model.node.Node;
import org.ontoware.rdf2go.model.node.PlainLiteral;
import org.ontoware.rdf2go.model.node.URI;
import org.ontoware.rdf2go.model.node.impl.PlainLiteralImpl;
import org.ontoware.rdf2go.model.node.impl.URIImpl;

import ws.biotea.ld2rdf.rdf.model.bibo.Article;
import ws.biotea.ld2rdf.rdf.model.bibo.Journal;
import ws.biotea.ld2rdf.rdf.model.doco.Section;

public class ArticleE extends Article {
	public static final String BIBO_NS = "http://purl.org/ontology/bibo/";
	public static final URI BIBO_DOI = new URIImpl(BIBO_NS + "doi", false);
	public static final URI BIBO_PMID = new URIImpl(BIBO_NS + "pmid", false);
	public static final String DCTERMS_NS = "http://purl.org/dc/terms/";
	public static final URI DCTERMS_SUBJECT = new URIImpl(DCTERMS_NS + "subject", false);
	public static final URI DCTERMS_PUBLISHER = new URIImpl(DCTERMS_NS + "publisher", false);
	public static final URI DCTERMS_IDENTIFIER = new URIImpl(DCTERMS_NS + "identifier", false); 
	public static final String DOCO_NS = "http://purl.org/spar/doco/"; 
	public static final String OWL_NS = "http://www.w3.org/2002/07/owl#";
	public static final URI OWL_SAMEAS = new URIImpl(OWL_NS + "sameAs", false); 

	public ArticleE(Model model, String uriString, boolean write)
	throws ModelRuntimeException {
		super(model, uriString, write);
	}
	
	public ArticleE ( Model model, BlankNode bnode, boolean write ) {
		super(model, RDFS_CLASS, bnode, write);
	}

	public void addDOI(Model model, String doi) {
		PlainLiteral doiAsLiteral = model.createPlainLiteral(doi);	    
	    Statement stm = model.createStatement(this.asResource(), BIBO_DOI, doiAsLiteral);
	    model.addStatement(stm); //name
	}
	
	public void addPMID(Model model, String pubmedID) {
		PlainLiteral doiAsLiteral = model.createPlainLiteral(pubmedID);	    
	    Statement stm = model.createStatement(this.asResource(), BIBO_PMID, doiAsLiteral);
	    model.addStatement(stm); //name
	}
	
	public void addTitle(Model model, String title) {
		Node titleNode = new PlainLiteralImpl(title);
	    this.addbiboTitle(titleNode);
	}
	
	public void addKeyword(Model model, String keyword){
		PlainLiteral kwAsLiteral = model.createPlainLiteral(keyword);	    
	    Statement stm = model.createStatement(this.asResource(), DCTERMS_SUBJECT, kwAsLiteral);
	    model.addStatement(stm); //name
	}
	
	public void addIdentifier(Model model, String keyword){
		PlainLiteral kwAsLiteral = model.createPlainLiteral(keyword);	    
	    Statement stm = model.createStatement(this.asResource(), DCTERMS_IDENTIFIER, kwAsLiteral);
	    model.addStatement(stm); //name
	}
	
	public void addSection(Model model, Section section) {
		Statement stm = model.createStatement(this.asResource(), DCTERMS_HAS_PART, section.asResource());
	    model.addStatement(stm); //article hasPart section
	    Statement inv = model.createStatement(section, DCTERMS_IS_PART_OF, this.asResource());
	    model.addStatement(inv); //section isPartOf article
	}
	
	public void addPublisherJournal(Model model, Journal journal) {
		Statement stm = model.createStatement(this.asResource(), DCTERMS_PUBLISHER, journal.asResource());
	    model.addStatement(stm); //journal
	}
	
	public void addSameAs(Model model, String uriString) {
		Node uriNode = model.createURI(uriString);
		Statement stm = model.createStatement(this.asResource(), OWL_SAMEAS, uriNode);
	    model.addStatement(stm); //section
	}
}
