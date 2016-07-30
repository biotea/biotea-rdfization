/**
 * 
 */
package ws.biotea.ld2rdf.rdf.model.bibo.extension;  

import org.ontoware.rdf2go.exception.ModelRuntimeException;
import org.ontoware.rdf2go.model.Model;
import org.ontoware.rdf2go.model.Statement;
import org.ontoware.rdf2go.model.node.BlankNode;
import org.ontoware.rdf2go.model.node.PlainLiteral;
import org.ontoware.rdf2go.model.node.Resource;
import org.ontoware.rdf2go.model.node.URI;
import org.ontoware.rdf2go.model.node.impl.URIImpl;

import ws.biotea.ld2rdf.rdf.model.bibo.Person;


/**
 * @author Leyla Garcia
 *
 */
public class PersonE extends Person {
	public static final String FOAF_NS = "http://xmlns.com/foaf/0.1/";
	public static final URI FOAF_NAME = new URIImpl(FOAF_NS + "name", false);
	public static final URI FOAF_FAMILYNAME = new URIImpl(FOAF_NS + "familyName", false);
	public static final URI FOAF_GIVENNAME = new URIImpl(FOAF_NS + "givenName", false);
	public static final URI FOAF_ONLINEACCOUNT = new URIImpl(FOAF_NS + "OnlineAccount", false);
	public static final URI FOAF_PROP_ACCOUNT = new URIImpl(FOAF_NS + "account", false);
	public static final URI FOAF_PROP_MBOX = new URIImpl(FOAF_NS + "mbox", false);
	public static final URI FOAF_PROP_ACCOUNT_NAME = new URIImpl(FOAF_NS + "accountName", false);
	public static final URI FOAF_PROP_PUBLICATIONS = new URIImpl(FOAF_NS + "publications", false);
	
	public PersonE(Model model, String uriString, boolean write)
			throws ModelRuntimeException {
		super(model, uriString, write);
	}
	
	public PersonE ( Model model, BlankNode bnode, boolean write ) {
		super(model, RDFS_CLASS, bnode, write);
	}

	public void addName(Model model, String name) {
		PlainLiteral nameAsLiteral = model.createPlainLiteral(name);	    
	    Statement stm = model.createStatement(this.asResource(), FOAF_NAME, nameAsLiteral);
	    model.addStatement(stm); //name
	}
	
	public void addFamilyName (Model model, String name) {
		PlainLiteral nameAsLiteral = model.createPlainLiteral(name);	    
	    Statement stm = model.createStatement(this.asResource(), FOAF_FAMILYNAME, nameAsLiteral);
	    model.addStatement(stm); //name
	}
	
	public void addGivenName (Model model, String name) {
		PlainLiteral nameAsLiteral = model.createPlainLiteral(name);	    
	    Statement stm = model.createStatement(this.asResource(), FOAF_GIVENNAME, nameAsLiteral);
	    model.addStatement(stm); //name
	}
	
	public void addOnlineAccount( Model model, String account, String nodeUrl) {
		//BlankNode bnode = model.createBlankNode();
		Resource res = new URIImpl(nodeUrl, false);
		model.addStatement(this, FOAF_PROP_ACCOUNT, res);
		model.addStatement(res, RDF_TYPE, FOAF_ONLINEACCOUNT);
		PlainLiteral nameAsLiteral = model.createPlainLiteral(account);	    
	    Statement stm = model.createStatement(res, FOAF_PROP_ACCOUNT_NAME, nameAsLiteral);
	    model.addStatement(stm); //name
	}
	
	public void addMBox(Model model, String email) {
		Resource res = new URIImpl("mailto:" + email, false);
		model.addStatement(this, FOAF_PROP_MBOX, res);
	}
	
	public void addPublications( Model model, String publicationLink) {
		Statement stm = model.createStatement(this.asResource(), FOAF_PROP_PUBLICATIONS, new URIImpl(publicationLink, false));
		model.addStatement(stm);
	}
}
