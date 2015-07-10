/**
 * 
 */
package com.williamoneil;

/**
 * @author Gudipati
 *
 */
public class ApplicationException extends Exception {

	/**
	 * default serial version id
	 */
	private static final long serialVersionUID = 1L;

	/**
	 * 
	 */
	public ApplicationException() {
		super();
	}

	/**
	 * @param msg
	 * @param error
	 */
	public ApplicationException(String msg, Throwable error) {
		super(msg, error);
	}

	/**
	 * @param msg
	 */
	public ApplicationException(String msg) {
		super(msg);
	}

	/**
	 * @param arg0
	 */
	public ApplicationException(Throwable error) {
		super(error);
	}
}
