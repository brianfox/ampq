package com.foxtheories.ibxm.amq.threads;

import ibxm.Module;

import java.io.IOException;

import javax.jms.BytesMessage;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.naming.NamingException;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;

import com.foxtheories.ibxm.amq.io.jms.Consumer;
import com.foxtheories.ibxm.amq.io.jms.MessageType;
import com.foxtheories.ibxm.amq.io.jms.Producer;
import com.foxtheories.ibxm.amq.io.jms.ProducerException;
import com.foxtheories.ibxm.amq.io.jms.PropertyName;
import com.foxtheories.ibxm.amq.state.ClientState;
import com.foxtheories.ibxm.sound.player.ClientIBXM;
import com.foxtheories.ibxm.sound.player.ClientNote;
import com.foxtheories.util.GlobalLogging;

public class ClientPlayerThread extends Thread {

	public final static int POOL_SIZE = 24; 

	private Logger logger;
	private Module module;
	private ClientIBXM[] ibxm;
	private int sample_rate;
	private boolean running;
	private int next_ibxm;
	private Consumer musicConsumer;
	private Producer serverProducer;
	private ClientState cs;

	public ClientPlayerThread(
			Consumer musicConsumer, 
			Producer serverProducer,
			Module module,
			int sample_rate, 
			Logger logger,
			ClientState cs
	) {
		this.module = module;
		this.musicConsumer = musicConsumer;
		this.sample_rate = sample_rate;
		this.running = false;
		this.next_ibxm = 0;
		this.logger = logger;
		this.serverProducer = serverProducer;
		this.cs = cs;
		createPool();
	}

	private void createPool() {
		if (ibxm != null) 
			for (ClientIBXM c : ibxm) 
				if (c != null && c.isAlive())
					c.interrupt();

		ibxm = new ClientIBXM[POOL_SIZE];
		for (int i=0; i < POOL_SIZE; i++) {
			ibxm[i] = new ClientIBXM(sample_rate, module);
			ibxm[i].start();
		}
	}
	
	private void play_tick(Message m) throws JMSException, IOException, NamingException {
		BytesMessage bm = (BytesMessage)m;
		int size = bm.getIntProperty(PropertyName.BYTE_PAYLOAD_SIZE.name());
		byte[] bytes = new byte[size];
		bm.readBytes(bytes);
		try {
			
			ClientNote[] notes = ClientNote.fromByteArray(bytes);

			logger.trace(String.format("Client received tick."));
			next_ibxm = 0;
			if (notes.length > ibxm.length) {
				logger.error(String.format("Client received too many notes.  Received: %d  Max: %d.", notes.length, ibxm.length));
				return;
			}
			for (int i=0; i < notes.length; i++) 
				ibxm[next_ibxm++].addNote(notes[i]);
			for (int i=next_ibxm; i < ibxm.length; i++) 
				ibxm[i].addNote(notes[0].blank());
			report();
		} catch (Exception e) {
			GlobalLogging.pp(logger, Level.ERROR, e);
			return;
		}
	}

	private void report() throws ProducerException, IOException {
		ClientState state;
		BytesMessage report = serverProducer.createBytesMessage(MessageType.REPORT, cs.toBytes());
		//report.setStringProperty(
		//		PropertyName.CLIENT_ID.name(), 
		//		this.getId()
		//);
		serverProducer.sendMessageQuietly(report);
	}
	
	private void endSong(Message m) throws ProducerException, IOException {
		running = false;
	}
	
	
	@Override
	public void run() 
	{
		running = true;
		while (running) {
			try 
			{
				cs.setRemainingCapacity(musicConsumer.getRemainingCapacity());
				cs.setQSize(musicConsumer.getSize());
				Message m = musicConsumer.receive();
				int i = m.getIntProperty(PropertyName.MESSAGE_TYPE.name());
				MessageType type = MessageType.fromVal(i);
				switch (type) {
					case PLAY_TICK:    play_tick(m);    break;
					case END_SONG:     endSong(m);      break;
					default:
						GlobalLogging.pp(logger, Level.ERROR, "Unhandled message received: " + type);
						continue;
				}
				cs.bumpMusicMessagesHandled();
			}
			catch (Exception e) 
			{
				e.printStackTrace();
			}
		}
	}

	public void quit() {
		running = false;
		
	}
}