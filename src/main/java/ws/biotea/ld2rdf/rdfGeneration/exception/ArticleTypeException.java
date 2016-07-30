package ws.biotea.ld2rdf.rdfGeneration.exception;

public class ArticleTypeException extends Exception {
	private static final long serialVersionUID = 1L;
	public ArticleTypeException() {
		super();
	}
    public ArticleTypeException(String message) {
		super(message);
	} 
    public ArticleTypeException(String message, Throwable cause) {
		super(message, cause);
	}
    public ArticleTypeException(Throwable cause) {
    	super(cause);
    } 
}
