/*
 * CVS LOG TRACKING
 * ----------------
 * $Log: LocalShow.java,v $
 * Revision 1.2  2009/10/27 17:13:28  bfox
 * Added CVS logging to everything.
 *
 *
 */

package com.foxtheories.ibxm.apps.local;

import ibxm.Module;

import com.foxtheories.ibxm.sound.mixer.ThreadedMixer;
import com.foxtheories.ibxm.sound.player.ClientIBXM;
import com.foxtheories.ibxm.sound.player.ServerIBXM;


public class LocalShow {
	
	public static void main( String[] args ) throws Exception {
		if( args.length < 1 ) {
			System.err.println( "Usage: java ibxm.Player <module file>" );
			System.exit( 0 );
		}
		
		Module m = Module.load_module(args[0]);
				
		ThreadedMixer mixer = new ThreadedMixer(m);

		for (int i=0; i < 32; i++) {
			ClientIBXM client = new ClientIBXM(48000, m);
			client.start();
			mixer.addClient(client);
		}
		
		ServerIBXM server = new ServerIBXM(48000, m, mixer);
		server.start();
	}
	
	
}
