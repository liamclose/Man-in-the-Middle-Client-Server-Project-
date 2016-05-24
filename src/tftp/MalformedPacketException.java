package tftp;

public class MalformedPacketException extends Exception {
	
	/**
	 * Exists as  class to be thrown in the Message.validate function in order to
	 * avoid having to catch generic exceptions.
	 */
	private static final long serialVersionUID = 1L;

	public MalformedPacketException(String message) {
		super(message);
	}

}
