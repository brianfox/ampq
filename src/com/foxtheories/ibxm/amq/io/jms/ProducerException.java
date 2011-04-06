/*
 * CVS LOG TRACKING
 * ----------------
 * $Log: ProducerException.java,v $
 * Revision 1.2  2009/10/27 17:13:28  bfox
 * Added CVS logging to everything.
 *
 *
 */

package com.foxtheories.ibxm.amq.io.jms;

public class ProducerException extends Exception {

	private static final long serialVersionUID = 1L;

	public ProducerException(Exception e) {
		super(e);
	}

	public ProducerException(String s) {
		super(s);
	}
}
