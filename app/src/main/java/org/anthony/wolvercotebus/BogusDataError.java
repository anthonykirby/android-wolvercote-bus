package org.anthony.wolvercotebus;

import com.google.gson.JsonElement;

public class BogusDataError extends RuntimeException {
	private static final long serialVersionUID = 4689653461622257435L;
	public final JsonElement je;

	public BogusDataError(String message) {
		super(message);
		je = null;
	}
	public BogusDataError(String message, JsonElement je) {
		super(message);
		this.je = je;
	}
}
