/**
 * 
 */
package ws.biotea.ld2rdf.rdfGeneration.pmcoa;

import org.ontoware.rdf2go.model.node.URI;
import org.ontoware.rdf2go.model.node.impl.URIImpl;

import ws.biotea.rdfization.util.OntologyRDFizationPrefix;

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
	, BOOKS_RECEIVED("books-received", null)
	, BRIEF_REPORT("brief-report", "Report")
	, CALENDAR("calendar", null)
	, CASE_REPORT("case-report", "Report")
	, COLLECTION("collection", "Collection")
	, CORRECTION("correction", null)
	, DISCUSSION("discussion", null)
	, DISSERTATION("dissertation", "Thesis")
	, EDITORIAL("editorial", null)
	, IN_BRIEF("in-brief", null)
	, INTRODUCTION("introduction", null)
	, LETTER("letter", "Letter")
	, MEETING_REPORT("meeting-report", "Report")
	, NEWS("news", null)
	, OBITUARY("obituary", null)
	, ORATION("oration", null)
	, PARTIAL_RETRACTION("partial-retraction", null)
	, PRODUCT_REVIEW("product-review", "AcademicArticle")
	, RAPID_COMMUNICATION("rapid-communication", null)
	, REPLY("reply", null)
	, REPRINT("reprint", "AcademicArticle")
	, RESEARCH_ARTICLE("research-article", "AcademicArticle")
	, RETRACTION("retraction", null)
	, REVIEW_ARTICLE("review-article", "AcademicArticle")
	, ARTICLE("article", "AcademicArticle")
	, TRANSLATION("translation", "AcademicArticle")
	, OTHER("other", "Document")
	, UNKNOWN("unknown", null);

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
