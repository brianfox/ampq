/*
 * CVS LOG TRACKING
 * ----------------
 * $Log: ServerThread.java,v $
 * Revision 1.6  2009/10/29 17:22:56  bfox
 * Doesn't look like tick messages will work.  Too slow.  Perhaps go to a strict timing model?  Let the client decide which tick to play?
 *
 * Revision 1.5  2009/10/28 17:21:00  bfox
 * Stable code after many relocation changes.
 *
 * Revision 1.4  2009/10/27 17:37:20  bfox
 * Addressed a few border conditions where the module and
 * or player thread may be null.  Code now checks for nulls.
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
import java.text.SimpleDateFormat;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;

import javax.jms.BytesMessage;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.naming.NamingException;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;

import com.foxtheories.ibxm.amq.context.AmqProperties;
import com.foxtheories.ibxm.amq.io.jms.Consumer;
import com.foxtheories.ibxm.amq.io.jms.MessageType;
import com.foxtheories.ibxm.amq.io.jms.Producer;
import com.foxtheories.ibxm.amq.io.jms.ProducerException;
import com.foxtheories.ibxm.amq.io.jms.PropertyName;
import com.foxtheories.ibxm.amq.state.ClientRecord;
import com.foxtheories.ibxm.amq.state.ClientState;
import com.foxtheories.ibxm.sound.mixer.Mixer.Strategy;
import com.foxtheories.util.GlobalLogging;


public class ServerThread extends Thread {

	private AmqProperties ap;
	private Logger logger;
	private Consumer consumer;
	private ConcurrentHashMap<String,ClientRecord> clientList;
	private Module module;

	private HeartBeatThread heartbeat;
	private ServerPlayerThread player;
	
	private int sample_rate = 48000;
	private boolean open;
	
	private static final SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss"); 
	Strategy strategy = Strategy.BROADCAST;
	
	public ServerThread(AmqProperties ap, Module m) 
	{
		this.ap = ap;
		Properties p = ap.getNamedProperties(AmqProperties.CONTEXT_SERVER_QUEUE);
		logger = GlobalLogging.createLog("com.foxtheories.ibmx.server", p);
		logger.debug("Creating global server queue (serves all clients).");
		logger.debug("Creating consumer.");
		
		GlobalLogging.pp(logger, Level.DEBUG, p, "ampq_s properties");
		
		consumer = new Consumer(p);
		try {
			consumer.flush(750);
			consumer.start();
		} catch (InterruptedException e) {
			GlobalLogging.pp(logger, Level.ERROR, e);
		}
		
		clientList = new ConcurrentHashMap<String,ClientRecord>();
		this.module = m;
	}
	
	private void join(Message m) throws JMSException, ProducerException {
		String clientId = m.getStringProperty(PropertyName.CLIENT_ID.name());
		if (clientList.containsKey(clientId)) {
			logger.error("ampq_s already includes client: " + clientId);
			return;
		}
			
		ClientRecord rec = new ClientRecord(clientId, ap);
		clientList.put(clientId, rec);			
		logger.info("ampq_s created client record.");
	}

	private void pong(Message m) throws JMSException {
		String clientId = m.getStringProperty(PropertyName.CLIENT_ID.name());
		ClientRecord r = 
			clientList.get(clientId);
		if (r == null) 
		{
			logger.error("Received a reply ping for a ghost client: " + clientId);
			// TODO: Kick the client off and ask it to rejoin
		}
		else
			r.checkIn();
	}

	private void module(Message m) throws JMSException, IOException, NamingException {
		String clientId = m.getStringProperty(PropertyName.CLIENT_ID.name());
		sendModule(clientId);
	}
	
	private void sendModule(String clientId) {
		if (module == null)
			return;
		
		try {
			Producer p = clientList.get(clientId).getCommandProducer();
			BytesMessage rsp = p.createBytesMessage(MessageType.RMODULE, module.toBytes());
			rsp.setStringProperty(PropertyName.CLIENT_ID.name(), clientId);
			rsp.setIntProperty(PropertyName.SAMPLE_RATE.name(), sample_rate);
			p.sendMessage(rsp);
			logger.info(String.format("Sent module to client %s", clientId));
		} catch (Exception e) {
			GlobalLogging.pp(logger, Level.ERROR, e);
		}
	}

	private void played_tick(Message m) throws JMSException, IOException, NamingException {
		String clientId = m.getStringProperty(PropertyName.CLIENT_ID.name());
		ClientRecord r = clientList.get(clientId);
		r.mark_played_tick();
		// logger.debug(String.format("CLIENT: %s | TICK: %d | LAG: %d (ms)", clientId, x, r.getLag()));
	}

	private void report(Message m) 
	throws 
		JMSException, 
		IOException, 
		ClassNotFoundException 
	{
		BytesMessage bm = (BytesMessage)m;
		int size = bm.getIntProperty(PropertyName.BYTE_PAYLOAD_SIZE.name());
		byte[] bytes = new byte[size];
		bm.readBytes(bytes);
		ClientState cs = ClientState.fromBytes(bytes);
		System.err.println(cs);
	}

	public void run() {

		open = true;
		try {
			consumer.flush(750);
			heartbeat = new HeartBeatThread(clientList, 1000, logger);
			heartbeat.start();
			this.logger.info("Listening on server queue.");
			while(open) {
				Message m = consumer.receive(1000);
				if (m == null)
					continue;
				int i = m.getIntProperty(PropertyName.MESSAGE_TYPE.name());
				MessageType type = MessageType.fromVal(i);
				switch (type) {
					case JOIN:         join(m);         break;
					case MODULE:       module(m);       break;
					case PONG:         pong(m);         break;
					case PLAYED_TICK:  played_tick(m);  break;
					case REPORT:       report(m);       break;
					default:
						logger.error(String.format("ampq_s has no handler for message.  Type: %s", type));
				}
			}
		} catch (Exception e) {
			GlobalLogging.pp(logger, Level.ERROR, e);
		}

	}
	
	
	public void quit() {
		open = false;
		consumer.close();
		for (ClientRecord r : clientList.values()) 
			r.close();
		heartbeat.close();
	}

	public String clientList() {
		if (clientList.isEmpty())
			return "No clients exist.";
		StringBuilder sb = new StringBuilder();
		sb.append(String.format("%-30s %-20s %-20s%n", "CLIENT ID", "JOINED", "CHECKED IN"));
		sb.append(String.format("%-30s %-20s %-20s%n", "---------", "------", "----------"));
		for (ClientRecord r : clientList.values()) {
			sb.append(
					String.format(
							"%-30s %-20s %-20s%n", 
							r.getId(), 
							sdf.format(r.getJoinDate()), 
							sdf.format(r.getCheckInDate())
					)
			);
		}
		return sb.toString();
	}
	
	public void setLogLevel(Level level) {
		logger.setLevel(level);
	}
	
	public void play() {
		player = new ServerPlayerThread(clientList, module, sample_rate, logger, strategy);
		player.start();
	}

	public String instrumentList() {
		if (module == null)
			return "No module has been set.";
		
		StringBuilder sb = new StringBuilder();
		sb.append(String.format("%-6s %-30s %-6s%n", "INDEX", "NAME", "USED"));
		sb.append(String.format("%-6s %-30s %-6s%n", "-----", "----", "----"));

		int[] hist = module.get_instrument_histogram();
		for (int i=0; i < hist.length; i++) {
			if (hist[i] == 0)
				continue;
			sb.append(
					String.format(
							"%-6d %-30s %-6d%n", 
							i, 
							module.get_instrument(i).name, 
							hist[i]
					)
			);
			
		}
		return sb.toString();
	}

	public String moduleList() {
		return module.toString();
	}

	public void pause() {
		// TODO Auto-generated method stub
	}

	public void stopMod() {
		if (player != null)
			this.player.stopMod();
	}

	public void reset() {
		// TODO Auto-generated method stub
		
	}

	public void setModule(Module m) {
		stopMod();
		this.module = m;
		for (String clientId : clientList.keySet()) {
			this.sendModule(clientId);
		}
	}

	public void setStrategy(Strategy s) {
		if (player != null)
			player.setStrategy(s);
	}

}
