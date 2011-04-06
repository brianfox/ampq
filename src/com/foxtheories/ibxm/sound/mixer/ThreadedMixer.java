/*
 * CVS LOG TRACKING
 * ----------------
 * $Log: ThreadedMixer.java,v $
 * Revision 1.2  2009/10/27 17:13:28  bfox
 * Added CVS logging to everything.
 *
 *
 */

package com.foxtheories.ibxm.sound.mixer;

import ibxm.Module;

import java.util.ArrayList;

import com.foxtheories.ibxm.sound.mixer.Mixer;
import com.foxtheories.ibxm.sound.player.ClientIBXM;
import com.foxtheories.ibxm.sound.player.ClientNote;



public class ThreadedMixer extends Mixer {

	private ArrayList<ClientIBXM> clients;

	public ThreadedMixer(Module m) {
		super(m, null);
		clients = new ArrayList<ClientIBXM>();
	}

	public void addClient(ClientIBXM client) {
		clients.add(client);
	}

	@Override
	public void addNote(ClientNote n) throws InterruptedException {
		clients.get(0).addNote(n);
	}
}
