/*
 * CVS LOG TRACKING
 * ----------------
 * $Log: MachineIdentifier.java,v $
 * Revision 1.2  2009/10/27 17:13:28  bfox
 * Added CVS logging to everything.
 *
 *
 */

package com.foxtheories.ibxm.amq.context;

import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.Enumeration;

/**
 * MachineIdentifier provides various static methods for converting
 * network machine access control identifiers (MAC) to Strings.  The
 * Strings give the same uniqueness as the original MAC.  That is,
 * for burned-in addresses, uniqueness probability is high.  For
 * user or software assigned addresses, or for virtual machines,
 * uniqueness probability is as based on the methodology or mechanism
 * used to assign the MAC address.
 * <br><br>
 *
 * @author Brian Fox
 */
public class MachineIdentifier {

    /**
     * ALPHABET provides a 62 character alphabet used to encode
     * counting numbers to Strings.  This provides a "compression
     * ratio" (not sure if the meaning is the same) of about 62:10
     * over a pure String representation found in the typical
     * String.format("%d",x) format.
     *
     * 62 characters:
     * -------------
     * A-Z provides 26 characters.
     * a-Z provides 26 characters.
     * 0-9 provides 10 characters.
     * 
     */
    private final static char[] ALPHABET;
    static {
		ALPHABET = new char[62];
		int count = 0;
		char c;
		for (c = 'a'; c <= 'z'; c++) 
			ALPHABET[count++] = c;
		for (c = 'A'; c <= 'Z'; c++) 
			ALPHABET[count++] = c;
		for (c = '0'; c <= '9'; c++) 
			ALPHABET[count++] = c;
	}

    /**
     * Produces a machine identifier based on the Media Access Control identifiers
     * of <b>all the attached network interface cards</b>.  Uniqueness probability is
     * high for environments with burned-in addresses.  For other environments,
     * uniqueness is limited to the algorithm or methods used to calculate addresses.
     *
     * @return String representing a UID.
     * @throws SocketException
     */
	public static String getUniqueMacIdentifier() throws SocketException {
		StringBuilder sb = new StringBuilder();
		for (String s : getStringAddrArray()) {
			sb.append(s);
			sb.append("_");
		}
		sb.append(convertLongToString(System.nanoTime()));
		return sb.toString();
	}

    
    /**
     *
     * @return
     * @throws SocketException
     */
	public static ArrayList<String> getStringAddrArray() throws SocketException {
		ArrayList<String> ret = new ArrayList<String>();
		for (Long l : getLongAddrArray())
			ret.add(convertLongToString(l));
		return ret;
	}



    /**
     * Returns a long representation of a MAC-48 address.
     *
     * @return long
     * @throws SocketException
     */
	private static long getLongAddr(NetworkInterface in) throws SocketException {
		long l = 0;
		byte[] addr = in.getHardwareAddress();

		for (int i = addr.length - 1; i >= 0; i--) {
			l = l * 256 + ((int)addr[i] & 0xFF);
		}
		return l;
	}


    /**
     * Returns an array of longs represening all of the MAC-48 addresses for NICs
     * in this machine.
     *
     * @return long[] 
     * @throws SocketException
     */
	private static ArrayList<Long> getLongAddrArray() throws SocketException {
		ArrayList<Long> ret = new ArrayList<Long>();

		Enumeration<NetworkInterface> e = NetworkInterface.getNetworkInterfaces();
		while (e.hasMoreElements()) {
			NetworkInterface next = e.nextElement();
			byte[] addr = next.getHardwareAddress();
			if (addr == null)
				continue;
			ret.add(getLongAddr(next));
		}
		return ret;
	}


    /**
     * Converts a long to a String using ALPHABET.
     * 
     * @param l number to convert
     * @return a String representation of number
     * @throws SocketException
     */
	private static String convertLongToString(long l) throws SocketException {
		StringBuilder sb = new StringBuilder();
		while (l > 0) {
			int next = (int)(l % ALPHABET.length);
			l /= ALPHABET.length;
			sb.append(ALPHABET[next]);
		}
		return sb.toString();
	}

}
