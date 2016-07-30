/**
 * 
 */
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

import ws.biotea.ld2rdf.rdf.model.bibo.Issue;
import ws.biotea.ld2rdf.rdf.model.bibo.Journal;


/**
 * @author Leyla Garcia
 *
 */
public class JournalE extends Journal {
	public static final String BIBO_NS = "http://purl.org/ontology/bibo/";
	public static final URI BIBO_ISSN = new URIImpl(BIBO_NS + "issn", false);
	public static final String DCTERMS_NS = "http://purl.org/dc/terms/";
	public static final URI DCTERMS_HAS_PART = new URIImpl(DCTERMS_NS + "hasPart", false);
	public static final URI DCTERMS_IS_PART_OF = new URIImpl(DCTERMS_NS + "isPartOf", false);

	public JournalE(Model model, String uriString, boolean write)
			throws ModelRuntimeException {
		super(model, uriString, write);
	}
	
	public JournalE ( Model model, BlankNode bnode, boolean write ) {
		super(model, RDFS_CLASS, bnode, write);
	}

	public void addIssue(Model model, Issue issue) {	    
	    Statement stm = model.createStatement(this.asResource(), DCTERMS_HAS_PART, issue.asResource());
	    model.addStatement(stm); //journal hasPart issue
	    Statement inv = model.createStatement(issue.asResource(), DCTERMS_IS_PART_OF, this.asResource());
	    model.addStatement(inv); //section isPartOf document
	}
	
	public void addDocument(Model model, ws.biotea.ld2rdf.rdf.model.bibo.Document doc) {	    
	    Statement stm = model.createStatement(this.asResource(), DCTERMS_HAS_PART, doc.asResource());
	    model.addStatement(stm); //journal hasPart issue
	    Statement inv = model.createStatement(doc.asResource(), DCTERMS_IS_PART_OF, this.asResource());
	    model.addStatement(inv); //section isPartOf document
	}
	
	public void addTitle(Model model, String title) {
		Node titleNode = new PlainLiteralImpl(title);
	    this.addbiboTitle(titleNode);
	}
	
	public void addISSN(Model model, String issn) {
		PlainLiteral doiAsLiteral = model.createPlainLiteral(issn);	    
	    Statement stm = model.createStatement(this.asResource(), BIBO_ISSN, doiAsLiteral);
	    model.addStatement(stm); //name
	}
	
	

}
