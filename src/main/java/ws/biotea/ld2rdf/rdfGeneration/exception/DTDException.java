package ws.biotea.ld2rdf.rdfGeneration.exception;

public class DTDException extends Exception {
	private static final long serialVersionUID = 1L;
	public DTDException() {
		super();
	}
    public DTDException(String message) {
		super(message);
	} 
    public DTDException(String message, Throwable cause) {
		super(message, cause);
	}
    public DTDException(Throwable cause) {
    	super(cause);
    } 
}
