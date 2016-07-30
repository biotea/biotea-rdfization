package ws.biotea.ld2rdf.rdf.model.bibo.extension;

import org.ontoware.rdf2go.exception.ModelRuntimeException;
import org.ontoware.rdf2go.model.Model;
import org.ontoware.rdf2go.model.Statement;
import org.ontoware.rdf2go.model.node.Node;
import org.ontoware.rdf2go.model.node.PlainLiteral;
import org.ontoware.rdf2go.model.node.URI;
import org.ontoware.rdf2go.model.node.impl.PlainLiteralImpl;
import org.ontoware.rdf2go.model.node.impl.URIImpl;

import ws.biotea.ld2rdf.rdf.model.bibo.Report;

public class ReportE extends Report {
	public static final String BIBO_NS = "http://purl.org/ontology/bibo/";
	public static final URI BIBO_ISSN = new URIImpl(BIBO_NS + "issn", false);
	public static final URI DCTERMS_HASPART = new URIImpl("http://purl.org/dc/terms/hasPart", false);
	public static final URI BIBO_DOI = new URIImpl(BIBO_NS + "doi", false);
	public static final URI BIBO_PMID = new URIImpl(BIBO_NS + "pmid", false);
	public static final String DCTERMS_NS = "http://purl.org/dc/terms/";
	public static final URI DCTERMS_IDENTIFIER = new URIImpl(DCTERMS_NS + "identifier", false); 
	public static final String OWL_NS = "http://www.w3.org/2002/07/owl#";
	public static final URI OWL_SAMEAS = new URIImpl(OWL_NS + "sameAs", false); 

	public ReportE(Model model, String uriString, boolean write)
			throws ModelRuntimeException {
		super(model, uriString, write);
	}
	
	public void addTitle(Model model, String title) {
		Node titleNode = new PlainLiteralImpl(title);
	    this.addbiboTitle(titleNode);
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
	
	public void addIdentifier(Model model, String keyword){
		PlainLiteral kwAsLiteral = model.createPlainLiteral(keyword);	    
	    Statement stm = model.createStatement(this.asResource(), DCTERMS_IDENTIFIER, kwAsLiteral);
	    model.addStatement(stm); //name
	}

	
	public void addSameAs(Model model, String uriString) {
		Node uriNode = model.createURI(uriString);
		Statement stm = model.createStatement(this.asResource(), OWL_SAMEAS, uriNode);
	    model.addStatement(stm); //section
	}
}
