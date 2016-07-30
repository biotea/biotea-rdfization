package ws.biotea.ld2rdf.rdfGeneration.exception;

public class PMCIdException extends Exception {
	private static final long serialVersionUID = 1L;
	public PMCIdException() {
		super();
	}
    public PMCIdException(String message) {
		super(message);
	} 
    public PMCIdException(String message, Throwable cause) {
		super(message, cause);
	}
    public PMCIdException(Throwable cause) {
    	super(cause);
    } 
}
