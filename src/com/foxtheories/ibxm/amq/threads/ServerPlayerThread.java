/*
 * CVS LOG TRACKING
 * ----------------
 * $Log: ServerPlayerThread.java,v $
 * Revision 1.5  2009/10/29 17:22:56  bfox
 * Doesn't look like tick messages will work.  Too slow.  Perhaps go to a strict timing model?  Let the client decide which tick to play?
 *
 * Revision 1.4  2009/10/28 17:21:00  bfox
 * Stable code after many relocation changes.
 *
 * Revision 1.3  2009/10/27 18:12:28  bfox
 * Constructor now passes the Strategy correctly to the Mixer.
 * Some cleanup of imports.
 *
 * Revision 1.2  2009/10/27 17:13:28  bfox
 * Added CVS logging to everything.
 *
 *
 */

package com.foxtheories.ibxm.amq.threads;

import ibxm.Module;

import java.io.IOException;
import java.util.concurrent.ConcurrentHashMap;

import javax.jms.BytesMessage;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.TextMessage;

import org.apache.log4j.Logger;

import com.foxtheories.ibxm.amq.io.jms.MessageType;
import com.foxtheories.ibxm.amq.io.jms.Producer;
import com.foxtheories.ibxm.amq.io.jms.ProducerException;
import com.foxtheories.ibxm.amq.io.jms.PropertyName;
import com.foxtheories.ibxm.amq.state.ClientRecord;
import com.foxtheories.ibxm.sound.mixer.Mixer;
import com.foxtheories.ibxm.sound.mixer.Mixer.Strategy;
import com.foxtheories.ibxm.sound.player.ClientNote;
import com.foxtheories.ibxm.sound.player.ServerIBXM;

public class ServerPlayerThread extends Thread {

	private ConcurrentHashMap<String,ClientRecord>  clientList;
	private Logger                   logger;
	private ServerIBXM               ibxm;
	private Mixer                    mixer;
	

	public ServerPlayerThread(
			ConcurrentHashMap<String,ClientRecord> clientList, 
			Module module,
			int sampleRate, 
			Logger logger,
			Strategy strategy
	) 
	{
		this.clientList = clientList;
		this.logger = logger;
		this.mixer = new Mixer(module, strategy);
		if (ibxm != null && ibxm.isAlive()) {
			ibxm.interrupt();
		}
		mixer.clear();
		ibxm = new ServerIBXM(sampleRate, module, mixer);
		

	}

	@Override
	public void run() 
	{
		try 
		{
			int tick_count = 0;
			ibxm.start();

			while(!mixer.isEnded()) 
			{
				ClientNote[][] notes = mixer.getNotes(clientList.size());
				
				int count = 0;
				for (ClientRecord r : clientList.values()) 
				{
					Producer p = r.getMusicProducer();
					try 
					{
						p.sendMessage(noteToMessage(p, notes[count], tick_count));
						r.mark_play_tick();
					}
					catch (ArrayIndexOutOfBoundsException e) 
					{
						logger.debug("Waiting for client notes array to settle.");
					}
					count++;
				}
				tick_count++;
			}
			endSong();
		}
		catch (Exception e) 
		{
			e.printStackTrace();
		}
	}

	private void endSong() throws JMSException {
		for (ClientRecord r : clientList.values()) 
		{
			Producer p = r.getMusicProducer();
			TextMessage m = p.createTextMessage();
			m.setIntProperty(
					PropertyName.MESSAGE_TYPE.name(), 
					MessageType.END_SONG.toVal()
			);
			p.sendMessage(m);
		}
	}
	
	private Message noteToMessage(Producer p, ClientNote[] notes, int tickCount) 
		throws 
		IOException, 
		JMSException, 
		ProducerException 
	{
		BytesMessage m = p.createBytesMessage(MessageType.PLAY_TICK, ClientNote.toByteArray(notes));
		m.setIntProperty(
				PropertyName.MESSAGE_TYPE.name(), 
				MessageType.PLAY_TICK.toVal()
		);
		return m;
	}

	public void stopMod() {
		ibxm.stopMod();
		
	}

	public void setStrategy(Strategy s) {
		this.mixer.setStrategy(s);
		
	}
}