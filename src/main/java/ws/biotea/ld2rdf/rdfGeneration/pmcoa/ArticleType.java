/**
 * 
 */
package ws.biotea.ld2rdf.rdfGeneration.pmcoa;

import org.ontoware.rdf2go.model.node.URI;
import org.ontoware.rdf2go.model.node.impl.URIImpl;

import ws.biotea.ld2rdf.util.rdfization.OntologyRDFizationPrefix;

/**
 * @author Leyla Garcia
 * http://dtd.nlm.nih.gov/archiving/tag-library/2.3/index.html?attr=article-type
 */
public enum ArticleType {
	ABSTRACT("abstract", "AcademicArticle")
	, ADDENDUM("addendum", "")
	, ANNOUNCEMENT("announcement", "")
	, ARTICLE_COMMENTARY("article-commentary", "AcademicArticle")
	, BOOK_REVIEW("book-review", "AcademicArticle")
	, BOOKS_RECEIVED("books-received", "Book")
	, BRIEF_REPORT("brief-report", "Report")
	, CALENDAR("calendar", null)
	, CASE_REPORT("case-report", "Report")
	, COLLECTION("collection", "Collection")
	, CORRECTION("correction", "Note")
	, DISCUSSION("discussion", "Document")
	, DISSERTATION("dissertation", "Thesis")
	, EDITORIAL("editorial", "Article")
	, IN_BRIEF("in-brief", "Document")
	, INTRODUCTION("introduction", "Document")
	, LETTER("letter", "Letter")
	, MEETING_REPORT("meeting-report", "Report")
	, NEWS("news", "Document")
	, OBITUARY("obituary", "Document")
	, ORATION("oration", "Document")
	, PARTIAL_RETRACTION("partial-retraction", "Note")
	, PRODUCT_REVIEW("product-review", "AcademicArticle")
	, RAPID_COMMUNICATION("rapid-communication", "Document")
	, REPLY("reply", "Document")
	, REPRINT("reprint", "AcademicArticle")
	, RESEARCH_ARTICLE("research-article", "AcademicArticle")
	, RETRACTION("retraction", "Note")
	, REVIEW_ARTICLE("review-article", "AcademicArticle")
	, ARTICLE("article", "AcademicArticle")
	, TRANSLATION("translation", "AcademicArticle")
	, OTHER("other", "Document")
	, UNKNOWN("unknown", "Document");

	private String type;
	private String biboType;
	private ArticleType(String type, String biboType) {
		this.type = type;
		this.biboType = biboType;
	}
	
	public String getType () {
		return type;
	}
	
	public String getBiboType() {
		return biboType;
	}
	
	public String getBiboTypeAsURI() {
		return OntologyRDFizationPrefix.BIBO.getURL() + biboType;
	}
	
	public URI getBiboTypeURI() {
		return new URIImpl(OntologyRDFizationPrefix.BIBO.getURL() + biboType, false);
	}
}
