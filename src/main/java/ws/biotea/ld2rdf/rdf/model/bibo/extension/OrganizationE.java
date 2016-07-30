package ws.biotea.ld2rdf.rdf.model.bibo.extension;

import org.ontoware.rdf2go.exception.ModelRuntimeException;
import org.ontoware.rdf2go.model.Model;
import org.ontoware.rdf2go.model.Statement;
import org.ontoware.rdf2go.model.node.BlankNode;
import org.ontoware.rdf2go.model.node.PlainLiteral;
import org.ontoware.rdf2go.model.node.URI;
import org.ontoware.rdf2go.model.node.impl.URIImpl;

import ws.biotea.ld2rdf.rdf.model.bibo.Organization;

public class OrganizationE extends Organization{
	public static final String FOAF_NS = "http://xmlns.com/foaf/0.1/";
	public static final URI FOAF_NAME = new URIImpl(FOAF_NS + "name", false);
	
	public OrganizationE(Model model, String uriString, boolean write)
			throws ModelRuntimeException {
		super(model, uriString, write);
	}
	
	public OrganizationE ( Model model, BlankNode bnode, boolean write ) {
		super(model, RDFS_CLASS, bnode, write);
	}

	public void addName(Model model, String name) {
		PlainLiteral nameAsLiteral = model.createPlainLiteral(name);	    
	    Statement stm = model.createStatement(this.asResource(), FOAF_NAME, nameAsLiteral);
	    model.addStatement(stm); //name
	}
}
