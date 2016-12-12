package ws.biotea.ld2rdf.rdf.model.bibo.extension;

import java.util.Collection;

import org.ontoware.rdf2go.exception.ModelRuntimeException;
import org.ontoware.rdf2go.model.Model;
import org.ontoware.rdf2go.model.Statement;
import org.ontoware.rdf2go.model.node.BlankNode;
import org.ontoware.rdf2go.model.node.URI;
import org.ontoware.rdf2go.model.node.impl.URIImpl;
import org.ontoware.rdf2go.vocabulary.RDF;
import org.ontoware.rdf2go.vocabulary.RDFS;

public class ListOfElements extends org.ontoware.rdfreactor.schema.rdfs.Class implements ListE<org.ontoware.rdfreactor.schema.rdfs.Class>{
	private static final long serialVersionUID = 1L;
	public static final URI RDFS_CLASS = new URIImpl(RDF.Seq.toString(), false);
	private int size = 0;
	
	/**
	 * Returns a Java wrapper over an RDF object, identified by a URI, given as a String.
	 * Creating two wrappers for the same URI is legal.
	 * @param model RDF2GO Model implementation, see http://rdf2go.ontoware.org
	 * @param uriString a URI given as a String
	 * @param write if true, the statement (this, rdf:type, TYPE) is written to the model
	 * @throws ModelRuntimeException if URI syntax is wrong
	 */
	public ListOfElements ( Model model, String uriString, boolean write) throws ModelRuntimeException {		
		super(model, RDFS_CLASS, new URIImpl(uriString, false), write);
	}
	
	public ListOfElements ( Model model, BlankNode bnode, boolean write ) {
		super(model, RDFS_CLASS, bnode, write);
	}
	
	public void addMember(Model model, org.ontoware.rdfreactor.schema.rdfs.Class thing, boolean indexed) {
		this.size++;
    	if (indexed) {
    		Statement stm = model.createStatement(this.asResource(), RDF.li(size), thing.asResource());
            model.addStatement(stm);
    	} else {
    		Statement stm = model.createStatement(this.asResource(), RDFS.member, thing.asResource());
            model.addStatement(stm);
    	}    	    	
    }
	
    public void addMember(Model model, org.ontoware.rdfreactor.schema.rdfs.Class thing, int index) {
    	this.size++;
    	if (index > 0) {
    		Statement stm = model.createStatement(this.asResource(), RDF.li(index), thing.asResource());
            model.addStatement(stm);
    	} else {
    		Statement stm = model.createStatement(this.asResource(), RDFS.member, thing.asResource());
            model.addStatement(stm);
    	}    	    	
    }

    public void addMembers(Model model, Collection<org.ontoware.rdfreactor.schema.rdfs.Class> collection) {
    	for (org.ontoware.rdfreactor.schema.rdfs.Class thing:collection) {
    		this.addMember(model, thing, 0); 
    	}
    }
    
    public void addMembersInOrder(Model model, Collection<org.ontoware.rdfreactor.schema.rdfs.Class> collection) {
    	int i = 1;
    	for (org.ontoware.rdfreactor.schema.rdfs.Class thing:collection) {
    		this.addMember(model, thing, new Integer(i));
            i++;
    	}
    }
    
    public int size() {
    	return this.size;
    }
}
