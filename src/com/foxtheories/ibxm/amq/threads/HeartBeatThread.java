/*
 * CVS LOG TRACKING
 * ----------------
 * $Log: HeartBeatThread.java,v $
 * Revision 1.3  2009/10/28 17:21:00  bfox
 * Stable code after many relocation changes.
 *
 * Revision 1.2  2009/10/27 17:13:28  bfox
 * Added CVS logging to everything.
 *
 *
 */

package com.foxtheories.ibxm.amq.threads;

import java.util.concurrent.ConcurrentHashMap;

import javax.jms.TextMessage;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;

import com.foxtheories.ibxm.amq.io.jms.MessageType;
import com.foxtheories.ibxm.amq.io.jms.Producer;
import com.foxtheories.ibxm.amq.io.jms.PropertyName;
import com.foxtheories.ibxm.amq.state.ClientRecord;
import com.foxtheories.util.GlobalLogging;


public class HeartBeatThread extends Thread {
	int 	                 interval;
	boolean                  open = false;
	ConcurrentHashMap<String,ClientRecord> clientList;
	Logger                   logger;
	
	HeartBeatThread(ConcurrentHashMap<String,ClientRecord> clientList, int interval, Logger logger) {
		this.interval = interval;
		this.clientList = clientList;
		this.logger = logger;
	}
	
	void close() {
		open = false;
	}

	
	@Override
	public void run() {
		open = true;
		try {
			while (open) {
				for (ClientRecord r : clientList.values()) {
					logger.trace("PING " + r.getId());
					Producer p = r.getCommandProducer();
					TextMessage m = p.createTextMessage();
					MessageType type = MessageType.PING;
					m.setIntProperty(PropertyName.MESSAGE_TYPE.name(), type.toVal());
					m.setIntProperty(PropertyName.MESSAGE_TYPE.name(), type.toVal());
					p.sendMessageQuietly(m);
				}
				sleep(interval);
			}
		} catch (Exception e) {
	        GlobalLogging.pp(logger, Level.ERROR, e);
		} 
				
	}
}
