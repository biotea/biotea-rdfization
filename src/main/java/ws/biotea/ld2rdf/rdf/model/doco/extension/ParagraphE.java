package ws.biotea.ld2rdf.rdf.model.doco.extension;

import org.ontoware.rdf2go.exception.ModelRuntimeException;
import org.ontoware.rdf2go.model.Model;
import org.ontoware.rdf2go.model.Statement;
import org.ontoware.rdf2go.model.node.BlankNode;
import org.ontoware.rdf2go.model.node.URI;
import org.ontoware.rdf2go.model.node.impl.URIImpl;
import org.ontoware.rdfreactor.runtime.Base;

import ws.biotea.ld2rdf.rdf.model.bibo.Document;
import ws.biotea.ld2rdf.rdf.model.doco.Paragraph;



public class ParagraphE extends Paragraph {
	public static final URI CITES = new URIImpl("http://purl.org/ontology/bibo/cites",false);
	public static final String DCTERMS_NS = "http://purl.org/dc/terms/";
	public static final URI DCTERMS_REFERENCES = new URIImpl(DCTERMS_NS + "references", false); 

	public ParagraphE(Model model, String uriString, boolean write)
			throws ModelRuntimeException {
		super(model, uriString, write);
	}
	
	public ParagraphE ( Model model, BlankNode bnode, boolean write ) {
		super(model, RDFS_CLASS, bnode, write);
	}
	
	public void addbiboCites(Document value) {
		Base.add(this.model, this.getResource(), CITES, value);
	}
	
	public void addDCReferences(Model model, String reference){   
		URI uri = model.createURI(reference);
	    Statement stm = model.createStatement(this.asResource(), DCTERMS_REFERENCES, uri);
	    model.addStatement(stm); //name
	}
}
