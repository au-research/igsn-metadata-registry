package au.edu.ardc.igsn.exception;

public class RecordNotFoundException extends RuntimeException{


	/**
	 * 
	 */
	private static final long serialVersionUID = -7739829828275418819L;

	public RecordNotFoundException(String id){
		super("Record " + id + " Doesn't exist");
	}
}
