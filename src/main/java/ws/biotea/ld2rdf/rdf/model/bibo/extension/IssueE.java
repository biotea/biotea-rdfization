/**
 * 
 */
package ws.biotea.ld2rdf.rdf.model.bibo.extension;

import org.ontoware.rdf2go.exception.ModelRuntimeException;
import org.ontoware.rdf2go.model.Model;
import org.ontoware.rdf2go.model.node.Node;
import org.ontoware.rdf2go.model.node.URI;
import org.ontoware.rdf2go.model.node.impl.DatatypeLiteralImpl;
import org.ontoware.rdf2go.model.node.impl.URIImpl;
import org.ontoware.rdf2go.vocabulary.XSD;

import ws.biotea.ld2rdf.rdf.model.bibo.Issue;


/**
 * @author Leyla Garcia
 *
 */
public class IssueE extends Issue {
	public static final String BIBO_NS = "http://purl.org/ontology/bibo/";
	public static final URI DCTERMS_HASPART = new URIImpl("http://purl.org/dc/terms/hasPart", false);

	public IssueE(Model model, String uriString, boolean write)
			throws ModelRuntimeException {
		super(model, uriString, write);
	}

	public void addIssuedDate(Model model, String date) {	    
		Node dateNode = new DatatypeLiteralImpl(date, XSD._date);
	    this.addbiboIssued(dateNode);
	}
	
	public void addIssueNumber(Model model, String number) {	    
		Node numberNode = new DatatypeLiteralImpl(number, XSD._int);
	    this.addbiboIssued(numberNode);
	}
	
	public void addIssueNumber(Model model, int number) {	    
		Node numberNode = new DatatypeLiteralImpl("" + number, XSD._int);
	    this.addbiboIssued(numberNode);
	}
}
