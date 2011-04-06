/*
 * CVS LOG TRACKING
 * ----------------
 * $Log: StringUtil.java,v $
 * Revision 1.2  2009/10/27 17:13:28  bfox
 * Added CVS logging to everything.
 *
 *
 */

package com.foxtheories.util;

import java.util.ArrayList;

public class StringUtil {

    /*
    The hexidecimal alphabet put in ordinal format.
    Useful for numeric to String conversions.
    */
	private static final char[] HEX_ALPHA = new char[]{
            '0', '1', '2', '3',
            '4', '5', '6', '7',
            '8', '9', 'A', 'B',
            'C', 'D', 'E', 'F'
    };


	/**
     * "Block concatenates" an arbitrary number of Strings.
     * Block concatenation is defined as concatenating block A,
     * which contains any number of carriage returns or line breaks,
     * and block B, which also has an arbitrary number of line
     * breaks, such that block A line 1 is traditionally concatenated
     * with block B, line 1 and block A line 2 is traditionally
     * concatenated with block B line 2, and so forth.  For two blocks
     * with uneven line counts, the shorter block is padded with
     * whitespace.
     * 
     * @param strings String array to block paste
     * @return
     */
    public static String blockConcatenate(
			String... strings
    ) {
		StringBuilder sb = new StringBuilder();
        for (String s : strings) {
            /* primer */
            if (sb.length() == 0) {
                sb.append(s);
                continue;
            }

            String[] tokens1 = sb.toString().split("[\\n\\r]");
            String[] tokens2 = s.split("[\\n\\r]");
            
            ArrayList<String> list1 = new ArrayList<String>();
            ArrayList<String> list2 = new ArrayList<String>();
            for (String t : tokens1)
            	if (t.length() > 0)
            		list1.add(t);
            for (String t : tokens2)
            	if (t.length() > 0)
            		list2.add(t);
            
            int max1 = 0;
            int max2 = 0;

            for (String z : tokens1)
                if (z.length() > max1)
                    max1 = z.length();

            for (String z : tokens2)
                if (z.length() > max2)
                    max2 = z.length();

            String format1 = "%-" + max1 + "s";
            String format2 = "%-" + max2 + "s";

            sb = new StringBuilder();
            for (int count=0; count < (list1.size() > list2.size() ? list1.size() : list2.size()); count++) {
                if (count >= list1.size())
                    sb.append(String.format(format1, ""));
                else
                    sb.append(String.format(format1, list1.get(count)));

                if (count >= list2.size())
                    sb.append(String.format(format2, ""));
                else
                    sb.append(String.format(format2, list2.get(count)));
                sb.append(String.format("%n"));
            }
        }
		return sb.toString();
	}


    /**
     * 
     * @param text
     * @param width
     * @return
     */
    public static String wrapText(
            String text,
            int width,
            boolean squashLines
    )
    {
        int linecount = 0;
        StringBuilder ret = new StringBuilder();
        StringBuilder nextword = new StringBuilder();
        for (char c : text.toCharArray()) {
            if (c == '\n' || c == '\r') {
                if (!squashLines) {
                	ret.append(nextword);
                    nextword = new StringBuilder();
                    ret.append(c);
                    linecount = 0;
                }
                continue;
            }
            if (Character.isWhitespace(c)) {
                if (linecount == 0 && nextword.length() == 0) 
                    continue;
                ret.append(nextword);
                linecount += nextword.length();
                nextword =  new StringBuilder();
                if (linecount > width) {
                    ret.append(String.format("%n"));
                    linecount = 0;
                    continue;
                }
            }
            nextword.append(c);
        }
        ret.append(nextword);
        return ret.toString();
    }


	public static String hexString(byte[] bytes) {
		StringBuilder sb = new StringBuilder();
		for (byte b : bytes)
			sb.append(hex(b) + " ");
		return sb.toString();
	}

	public static String hex(byte b) {
		return new String(new char[] {HEX_ALPHA[b >>> 4],HEX_ALPHA[b & 0xF]});
	}

}
