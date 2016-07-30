package ws.biotea.ld2rdf.rdf.model.bibo.extension;

import org.ontoware.rdf2go.exception.ModelRuntimeException;
import org.ontoware.rdf2go.model.Model;
import org.ontoware.rdf2go.model.Statement;
import org.ontoware.rdf2go.model.node.BlankNode;
import org.ontoware.rdf2go.model.node.PlainLiteral;
import org.ontoware.rdf2go.model.node.URI;
import org.ontoware.rdf2go.model.node.impl.URIImpl;
import org.ontoware.rdf2go.vocabulary.RDFS;

import ws.biotea.ld2rdf.rdf.model.bibo.Thing;


public class GroupE extends Thing  {
	public static final String FOAF_NS = "http://xmlns.com/foaf/0.1/";
	public static final URI FOAF_NAME = new URIImpl(FOAF_NS + "name", false);
	public static final URI RDFS_CLASS = new URIImpl("http://xmlns.com/foaf/0.1/Group", false);
    public static final URI FOAF_MEMBER = new URIImpl(FOAF_NS + "member", false);
    
    private String label;
    
	public GroupE(Model model, String uriString, boolean write)
			throws ModelRuntimeException {
		super(model, RDFS_CLASS, new URIImpl(uriString,false), write);
		this.label = "";
	}
	
	public GroupE ( Model model, BlankNode bnode, boolean write ) {
		super(model, RDFS_CLASS, bnode, write);
	}
	
	public void addName(Model model, String name) {
		PlainLiteral nameAsLiteral = model.createPlainLiteral(name);	    
	    Statement stm = model.createStatement(this.asResource(), FOAF_NAME, nameAsLiteral);
	    model.addStatement(stm); //name
	    this.label = name;
	}
	
	public void addMembership( Model model, Thing thing) {    
	    Statement stm = model.createStatement(this.asResource(), FOAF_MEMBER, thing.asResource());
	    model.addStatement(stm); //name
	}

	/* (non-Javadoc)
	 * @see org.ontoware.rdfreactor.runtime.ReactorRuntimeEntity#equals(java.lang.Object)
	 */
	@Override
	public boolean equals(Object other) {
		if (other instanceof GroupE) {
			GroupE group = (GroupE)other;
			return (this.label.equalsIgnoreCase(group.label));
		} else {
			return false;
		}
	}



}
