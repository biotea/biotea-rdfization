/**
 * 
 */
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
import ws.biotea.ld2rdf.rdf.model.doco.Section;


/**
 * @author Leyla Garcia
 *
 */
public class SectionE extends Section { 
	public static final String DOCO_NS = "http://purl.org/spar/doco/"; 
	public static final URI CITES = new URIImpl("http://purl.org/ontology/bibo/cites",false);
	public static final String DCTERMS_NS = "http://purl.org/dc/terms/";
	public static final URI DCTERMS_REFERENCES = new URIImpl(DCTERMS_NS + "references", false); 
	public static final URI DCTERMS_HAS_PART = new URIImpl(DCTERMS_NS + "hasPart", false);
	public static final URI DCTERMS_IS_PART_OF = new URIImpl(DCTERMS_NS + "isPartOf", false);

	public SectionE(Model model, String uriString, boolean write)
			throws ModelRuntimeException {
		super(model, uriString, write);
	}
	
	public SectionE ( Model model, BlankNode bnode, boolean write ) {
		super(model, RDFS_CLASS, bnode, write);
	}

	public void addSection(Model model, Section subsection) {
		Statement stm = model.createStatement(this.asResource(), DCTERMS_HAS_PART, subsection.asResource());
	    model.addStatement(stm); //section hasPart sub-section
	    Statement inv = model.createStatement(subsection.asResource(), DCTERMS_IS_PART_OF, this.asResource());
	    model.addStatement(inv); //sub-section isPartOf section
	}
	
	public void addParagraph(Model model, Paragraph paragraph) {
		Statement stm = model.createStatement(this.asResource(), DCTERMS_HAS_PART, paragraph.asResource());
	    model.addStatement(stm); //section hasPart paragraph
	    Statement inv = model.createStatement(paragraph.asResource(), DCTERMS_IS_PART_OF, this.asResource());
	    model.addStatement(inv); //paragraph isPartOf section
	}
	
	public void addDCReferences(Model model, String reference){   
		URI uri = model.createURI(reference);
	    Statement stm = model.createStatement(this.asResource(), DCTERMS_REFERENCES, uri);
	    model.addStatement(stm); //name
	}
	
	public void addbiboCites(Document value) {
		Base.add(this.model, this.getResource(), CITES, value);
	}
}
