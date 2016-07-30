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

import ws.biotea.ld2rdf.rdf.model.bibo.AcademicArticle;
import ws.biotea.ld2rdf.rdf.model.bibo.Article;
import ws.biotea.ld2rdf.rdf.model.bibo.Document;
import ws.biotea.ld2rdf.rdf.model.bibo.Journal;
import ws.biotea.ld2rdf.rdf.model.bibo.Thing;
import ws.biotea.ld2rdf.rdf.model.doco.Section;

public class AcademicArticleE extends AcademicArticle {	
	public AcademicArticleE(Model model, String uriString, boolean write)
	throws ModelRuntimeException {
		super(model, uriString, write);
	}
	
	public AcademicArticleE ( Model model, BlankNode bnode, boolean write ) {
		super(model, RDFS_CLASS, bnode, write);
	}
}
