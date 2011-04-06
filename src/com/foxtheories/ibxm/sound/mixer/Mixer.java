/*
 * CVS LOG TRACKING
 * ----------------
 * $Log: Mixer.java,v $
 * Revision 1.4  2009/10/29 17:22:56  bfox
 * Doesn't look like tick messages will work.  Too slow.  Perhaps go to a strict timing model?  Let the client decide which tick to play?
 *
 * Revision 1.3  2009/10/27 18:13:21  bfox
 * Added better specifity to throw clause.
 *
 * Revision 1.2  2009/10/27 17:13:28  bfox
 * Added CVS logging to everything.
 *
 *
 */

package com.foxtheories.ibxm.sound.mixer;

import java.util.HashMap;
import java.util.concurrent.ArrayBlockingQueue;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;

import ibxm.Module;

import com.foxtheories.ibxm.sound.player.ClientNote;
import com.foxtheories.util.GlobalLogging;



public class Mixer {

	public enum Strategy {
		BROADCAST,
		ROUND_ROBIN,
		CHANNEL,
		INSTRUMENT;
	}
	protected Strategy strategy = null;
	
	/* all of the strategies besides broadcast create
	 * assignments using ordinal counting.  The next
	 * assignment goes to the next client.             */
	protected int round_robin;
	protected int channel_robin;
	protected int instrument_robin;
	protected HashMap<Integer,Integer> channelMap;
	protected HashMap<Integer,Integer> instrumentMap;

	/* various metrics used to calculate strategies    */
	protected int[] instrumentHist;
	protected int   nchannels;
	protected int   ninstruments;

	protected ArrayBlockingQueue<ClientNote> q;
	protected Logger logger;
	
	protected boolean song_end = false;
	
	public Mixer(Module m, Strategy s) {
		initStrategy();
		q              = new ArrayBlockingQueue<ClientNote>(1000);
		nchannels      = m.get_num_channels();
		ninstruments   = m.get_num_instruments();
		instrumentHist = m.get_instrument_histogram();
		logger = GlobalLogging.createLog("com.foxtheories.amq.consumer");
		this.strategy = s;

	}

	private void initStrategy() {
		round_robin = channel_robin = instrument_robin = 0;
		channelMap = new HashMap<Integer,Integer>();
		instrumentMap = new HashMap<Integer,Integer>();
	}

	public void setStrategy(Strategy s) {
		initStrategy();
		this.strategy = s;
	}

	public void addNote(ClientNote note) throws InterruptedException {
		if (q.remainingCapacity() == 0) {
    		GlobalLogging.pp(logger, Level.ERROR, "Mixer failed to add a note.  Queue full.");
    		GlobalLogging.pp(logger, Level.ERROR, "Clearing queue.");
    		q.clear();
    		return;
    	}
		q.add(note);
	}

	
	public ClientNote getNote() throws InterruptedException {
		return q.take();
	}
	
	
	public ClientNote[] demuxChannels(ClientNote note) {
		ClientNote[] ret = new ClientNote[nchannels];

		for (int k = 0; k < nchannels; k++) {
			ret[k] = note.blank();
		}

		for (int i = 0; i < nchannels; i++) {
			ret[i].setChannel(i, note.getChannel(i));
		}
		return ret;
	}

	
	/**
	 * Returns the given ClientNote, which contains a mix of instruments per channel, 
	 * as an array of ClientNotes where each element ClientNote contains just a single
	 * instrument. 
	 * 
	 * @param note the ClientNote to demux.
	 * @return an array of ClientNotes separating each instrument.
	 */
	public ClientNote[] demuxInstruments(ClientNote note) {
		ClientNote[] ret = new ClientNote[ninstruments];

		for (int k = 0; k < ninstruments; k++) {
			ret[k] = note.blank();
			for (int i=0; i < nchannels; i++) {
				if (note.getChannel(i)[1] == k)
					ret[k].setChannel(i, note.getChannel(i));
			}
		}
		
		return ret;
	}

	
	/**
	 * Returns an array of arrays of ClientNotes for each client.  The inner array
	 * contains ClientNotes that the client is expected to play simultaneously.
	 * This allows a many-to-one relationship allowing a Strategy with too few 
	 * clients to operate.
	 *   
	 * @param nclients the number of clients available to play notes.
	 * @return an array of ClientNotes for each client.
	 * @throws InterruptedException 
	 */
	public ClientNote[][] getNotes(int nclients) throws InterruptedException 
	{
		/*
		 * ROUND ROBIN
		 * Each successive client gets the next tick.  This 
		 * is a simple strategy that works with any number of
		 * clients.  For the effect to be noticeable, try for
		 * three more clients. 
		 */
		if (strategy == Strategy.ROUND_ROBIN) {
			ClientNote orig = this.getNote();
			ClientNote blank = orig.blank();
			ClientNote[][] ret = new ClientNote[nclients][];
			for (int i = 0; i < nclients; i++)
				if (i == (round_robin++)%nclients)
					ret[i] = new ClientNote[] { orig };
				else
					ret[i] = new ClientNote[] { blank };
			return ret;
		}
		
		/*
		 * BROADCAST
		 * Each client gets every tick.  This is a relatively
		 * simple strategy which works well for any number of 
		 * clients.
		 */
		if (strategy == Strategy.BROADCAST) {
			ClientNote orig = this.getNote();
			ClientNote[][] ret = new ClientNote[nclients][];
			for (int i = 0; i < nclients; i++)
				ret[i] = new ClientNote[] { orig };
			return ret;
		}

		
		/*
		 * CHANNEL
		 * Each client gets a channel.  This works best when
		 * the number of clients equals the number of channels
		 * in the module.  
		 * 
		 * A many-channel-to-one-client relationship exists.  
		 * Because most modules use four to eight channels, 
		 * clients above a count of eight will be idle.
		 */
		if (strategy == Strategy.CHANNEL) {
			ClientNote orig = this.getNote();
			ClientNote[] demux = this.demuxChannels(orig);
			ClientNote[][] ret = new ClientNote[nclients][];
			int[] load = new int[nclients];
			int[] index = new int[nclients];
			
			for (int i=0; i < demux.length; i++) {
				if (!channelMap.containsKey(i))
					channelMap.put(i, channel_robin++);
				load[channelMap.get(i) % nclients]++;
			}
			for (int i = 0; i < nclients; i++)
				ret[i] = new ClientNote[load[i]];
			for (int i=0; i < demux.length; i++) {
				int client = channelMap.get(i) % nclients;
				ret[client][index[client]++] = demux[i];
			}
			return ret;
		}

		/*
		 * INSTRUMENT
		 * Each client gets an instrument.  This works best when
		 * the number of clients equals the number of instruments
		 * in the module.  
		 * 
		 * A many-instrument-to-one-client relationship exists.  
		 * It's estimated the best playback effect occurs between
		 * four and twenty clients.
		 */
		if (strategy == Strategy.INSTRUMENT) {
			ClientNote orig = this.getNote();
			ClientNote[] demux = this.demuxInstruments(orig);
			ClientNote[][] ret = new ClientNote[nclients][];
			int[] load = new int[nclients];
			int[] index = new int[nclients];
			
			for (int i=0; i < demux.length; i++) {
				// dead instrument, skip
				if (instrumentHist[i] == 0)
					continue;
				if (!instrumentMap.containsKey(i))
					instrumentMap.put(i, instrument_robin++);
				int client = instrumentMap.get(i) % nclients;
				load[client]++;
			}
			for (int i = 0; i < nclients; i++)
				ret[i] = new ClientNote[load[i]];
			for (int i=0; i < demux.length; i++) {
				// dead instrument, skip
				if (instrumentHist[i] == 0)
					continue;
				int client = instrumentMap.get(i) % nclients;
				ret[client][index[client]++] = demux[i];
			}
			return ret;
		}

		return null;
	}


	
	/**
	 * Clears the note queue.  This is useful for boundary conditions, error
	 * handling, interruptions, and the like.
	 */
	public void clear() {
		q.clear();
	}

	public void endSong() {
		song_end = true;
		
	}

	public boolean isEnded() {
		return song_end;
		
	}
}
