/*
 * CVS LOG TRACKING
 * ----------------
 * $Log: ClientThread.java,v $
 * Revision 1.5  2009/10/29 17:22:56  bfox
 * Doesn't look like tick messages will work.  Too slow.  Perhaps go to a strict timing model?  Let the client decide which tick to play?
 *
 * Revision 1.4  2009/10/28 17:21:00  bfox
 * Stable code after many relocation changes.
 *
 * Revision 1.3  2009/10/27 17:23:17  bfox
 * Renamed PropertyName from MODULE_SIZE to BYTE_PAYLOAD_SIZE.
 *
 * Revision 1.2  2009/10/27 17:13:28  bfox
 * Added CVS logging to everything.
 *
 *
 */

package com.foxtheories.ibxm.amq.threads;

import ibxm.Module;

import java.io.IOException;
import java.util.Properties;

import javax.jms.BytesMessage;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.ObjectMessage;
import javax.jms.TextMessage;

import javax.naming.NamingException;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;

import com.foxtheories.ibxm.amq.context.AmqProperties;
import com.foxtheories.ibxm.amq.context.MachineIdentifier;
import com.foxtheories.ibxm.amq.io.jms.Consumer;
import com.foxtheories.ibxm.amq.io.jms.ConsumerException;
import com.foxtheories.ibxm.amq.io.jms.MessageType;
import com.foxtheories.ibxm.amq.io.jms.Producer;
import com.foxtheories.ibxm.amq.io.jms.ProducerException;
import com.foxtheories.ibxm.amq.io.jms.PropertyName;
import com.foxtheories.ibxm.amq.state.ClientState;

import com.foxtheories.util.GlobalLogging;

public class ClientThread extends Thread {

	Producer producer;
	Consumer musicConsumer;
	Consumer commandConsumer;
	Logger commandLogger;
	Logger musicLogger;
	String id;
	ClientState cs;
	ClientPlayerThread playerThread;

	public ClientThread(AmqProperties ap) 
	throws 
		JMSException, 
		IOException, 
		NamingException, 
		ProducerException, 
		ConsumerException 
	{
		this.id = MachineIdentifier.getUniqueMacIdentifier();

		Properties p = ap.getNamedProperties(AmqProperties.CONTEXT_CLIENT_QUEUE);
		commandLogger = GlobalLogging.createLog("com.foxtheories.ibmx.client.command." + id , p);
		musicLogger = GlobalLogging.createLog("com.foxtheories.ibmx.client.music." + id , p);
		producer = new Producer(p,500);
		producer.start();

		p.setProperty("queue", id + "_music");
		p.setProperty("key", id  + "_music");
		musicConsumer = new Consumer(p);
		musicConsumer.start();

		p.setProperty("queue", id + "_command");
		p.setProperty("key", id  + "_command");
		commandConsumer = new Consumer(p);
		commandConsumer.start();

		cs = new ClientState(this.id);
		
		joinServer();
		
		//this.consumer.flush(1000);
		commandLogger.info("Client constructed.");
	}
	
	
	
	private void joinServer() throws JMSException, ProducerException {
	
		TextMessage join = producer.createTextMessage();
		join.setIntProperty(
				PropertyName.MESSAGE_TYPE.name(), 
				MessageType.JOIN.toVal()
		);
		join.setStringProperty(
				PropertyName.CLIENT_ID.name(), 
				id
		);
		producer.sendMessage(join);

		ObjectMessage om = producer.createObjectMessage();
		om.setIntProperty(
				PropertyName.MESSAGE_TYPE.name(), 
				MessageType.MODULE.toVal()
		);
		om.setStringProperty(
				PropertyName.CLIENT_ID.name(), 
				id
		);
		om.setObject(new Integer(2000));
		producer.sendMessage(om);
	}

	private void module(Message m) throws JMSException, IOException, NamingException {
		Module module = null;
		BytesMessage bm = (BytesMessage)m;
		int size = bm.getIntProperty(PropertyName.BYTE_PAYLOAD_SIZE.name());
		int sample_rate = bm.getIntProperty(PropertyName.SAMPLE_RATE.name());
		byte[] bytes = new byte[size];
		bm.readBytes(bytes);
		try {
			module = Module.fromBytes(bytes);
			if (playerThread != null)
				playerThread.quit();
			cs = new ClientState(this.id);
			playerThread = new ClientPlayerThread(
					musicConsumer, 
					producer, 
					module, 
					sample_rate, 
					musicLogger, 
					cs
			);
			playerThread.start();
			commandLogger.info(String.format("Client received module."));
			GlobalLogging.pp(commandLogger, Level.INFO, module.toString());
		} catch (ClassNotFoundException e) {
			commandLogger.info(String.format("Client failed to receive module."));
			return;
		}
	}

	private void pong() throws JMSException, IOException, NamingException, ProducerException {
		commandLogger.trace("PONG");
		BytesMessage rsp = producer.createBytesMessage();
		MessageType type = MessageType.PONG;
		rsp.setIntProperty(
				PropertyName.MESSAGE_TYPE.name(), 
				type.toVal()
		);
		rsp.setStringProperty(
				PropertyName.CLIENT_ID.name(), 
				this.id
		);
		producer.sendMessageQuietly(rsp);
	}

	@Override
	public void run() {
		try {
			while(true) {
				Message m = commandConsumer.receive();
				int i = m.getIntProperty(PropertyName.MESSAGE_TYPE.name());
				MessageType type = MessageType.fromVal(i);
				switch (type) {
					case RMODULE:    module(m);   break;
					case PING:       pong();      break;
					default:
						GlobalLogging.pp(
								commandLogger, 
								Level.ERROR, 
								"Unhandled message received: " + type
						);
						continue;
				}
				cs.bumpCommandMessagesHandled();
			}
		} catch (Exception e) {
			GlobalLogging.pp(commandLogger, Level.ERROR, e);
		} 
	}
}
