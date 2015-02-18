package pnnl.goss.core;

public class ResponseError extends Response {

	private static final long serialVersionUID = -6531221350777233341L;
	
	private String message;
	
	public ResponseError(String message) {
		this.message = message;
	}

	public String getMessage() {
		return message;
	}

	public void setMessage(String message) {
		this.message = message;
	}

}
