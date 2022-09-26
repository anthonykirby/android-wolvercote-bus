package org.anthony.wolvercotebus;

public class FatalError extends RuntimeException {
	private static final long serialVersionUID = 4689653461622257435L;

	public FatalError(String message, Exception e) {
		super(message, e);
	}
}
