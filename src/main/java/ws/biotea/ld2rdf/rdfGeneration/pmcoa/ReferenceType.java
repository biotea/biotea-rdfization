package ws.biotea.ld2rdf.rdfGeneration.pmcoa;

public enum ReferenceType {
	JOURNAL_ARTICLE("journal"),
	BOOK("book"),
	BOOK_SECTION("bookSection"),
	CONFERENCE_PROCS("confproc"), //proceedings
	CONFERENCE_PAPER("confpaper"), //paper in proceedings
	THESIS("thesis"), //both, complete or partial
	GOV("gov"),
	PATENT("patent"),
	STANDARD("standard"),
	DATABASE("database"),
	WEBPAGE("webpage"),
	COMMUN("commun"),
	DISCUSSION("discussion"),
	BLOG("blog"),
	WIKI("wiki"),
	REPORT("report"),
	SOFTWARE("software"),
	OTHER("other");
	
	private String type;
	private ReferenceType(String type) {
		this.type = type;
	}
	public String getType() {
		return type;
	}
}