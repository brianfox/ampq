/*
 * CVS LOG TRACKING
 * ----------------
 * $Log: AmpqClient.java,v $
 * Revision 1.3  2009/10/28 17:21:00  bfox
 * Stable code after many relocation changes.
 *
 * Revision 1.2  2009/10/27 17:13:28  bfox
 * Added CVS logging to everything.
 *
 *
 */

package com.foxtheories.ibxm.apps.dist;

import com.foxtheories.ibxm.amq.context.AmqProperties;
import com.foxtheories.ibxm.amq.threads.ClientThread;

public class AmpqClient {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		try {
			AmqProperties p = new AmqProperties();
			p.setAllProperty("broker","192.168.148.128");
			ClientThread c = new ClientThread(p);
			c.start();
			
		} catch (Exception e) {
			e.printStackTrace();
		} 
	}

}
