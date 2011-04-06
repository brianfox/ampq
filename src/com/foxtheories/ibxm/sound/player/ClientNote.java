/*
 * CVS LOG TRACKING
 * ----------------
 * $Log: ClientNote.java,v $
 * Revision 1.2  2009/10/27 17:13:28  bfox
 * Added CVS logging to everything.
 *
 *
 */

package com.foxtheories.ibxm.sound.player;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutput;
import java.io.ObjectOutputStream;
import java.io.Serializable;

public class ClientNote implements Serializable {

	private static final long serialVersionUID = 1L;
	private int[][] val;
	private int num_rows;
	private boolean silent = false;
	
	public ClientNote(int num_channels, int num_rows) {
		this.num_rows = num_rows;
		val = new int[num_channels][];
	}
	
	public void setChannel(int channel, int[] val) {
		this.val[channel] = val.clone();
	}

	public int[] getChannel(int channel) 
	{
		return val[channel];
	}

	public int getNumRows() 
	{
		return num_rows;
	}

	public static byte[] toByteArray(ClientNote[] notes) throws IOException 
	{

		ByteArrayOutputStream bos = new ByteArrayOutputStream() ;
        ObjectOutput out = new ObjectOutputStream(bos) ;
        out.writeObject(notes);
        out.close();
    
        return bos.toByteArray();
	}

	public static ClientNote[] fromByteArray(byte[] bytes) throws IOException, ClassNotFoundException 
	{

        ObjectInputStream in = new ObjectInputStream(new ByteArrayInputStream(bytes));
        ClientNote[] ret = (ClientNote[])in.readObject();
        in.close();
        return ret;
	}
	
	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		
		sb.append(String.format("Rows: %d   ", this.num_rows));
		for (int[] j : this.val)
		{
			for (int i : j) { 
				sb.append(String.format("%x", i));
				sb.append(":");
			}
			sb.append("\t");
		}
		return sb.toString();
	}

	public int getNumChannels() {
		return val.length;
	}

	public void silence() {
		silent = true;
	}

	public boolean isSilent() {
		return silent;
	}

	public ClientNote blank() {
		ClientNote ret = new ClientNote(this.num_rows, this.val.length);
		for (int i=0; i < val.length; i++) {
			if (val[i]!= null)
				ret.val[i] = new int[val[i].length];
		}
		return ret;
	}
}
