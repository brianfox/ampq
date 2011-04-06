/*
 * CVS LOG TRACKING
 * ----------------
 * $Log: Producer.java,v $
 * Revision 1.5  2009/10/28 17:21:00  bfox
 * Stable code after many relocation changes.
 *
 * Revision 1.4  2009/10/27 18:32:37  bfox
 * Created a createBytesMessage that accepts a byte array.
 *
 * Revision 1.3  2009/10/27 18:11:13  bfox
 * Corrected log text for constructor.
 *
 * Revision 1.2  2009/10/27 17:13:28  bfox
 * Added CVS logging to everything.
 *
 *
 */

package com.foxtheories.ibxm.amq.io.jms;

import java.util.Properties;
import java.util.concurrent.ArrayBlockingQueue;

import javax.jms.BytesMessage;
import javax.jms.Connection;
import javax.jms.ConnectionFactory;
import javax.jms.Destination;
import javax.jms.ExceptionListener;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageProducer;
import javax.jms.ObjectMessage;
import javax.jms.Session;
import javax.jms.TextMessage;
import javax.naming.Context;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;

import com.foxtheories.ibxm.amq.context.ContextFactory;
import com.foxtheories.util.GlobalLogging;

public class Producer extends Thread
{
    private static final int QUEUE_SIZE = 50000;
    private boolean open;
    
    private ArrayBlockingQueue<Message> q;
    private Context                     ctx;
    private Connection                  connection;
    private Session                     session;
    private MessageProducer             messageProducer;
    private Logger 						logger;

    public Producer (Properties p, int ttl) 
    {
    	q      = new ArrayBlockingQueue<Message>(QUEUE_SIZE);
    	logger = GlobalLogging.createLog("com.foxtheories.amq.producer", p);

    	try {
            logger.info("--CREATING PRODUCER / START--");
    		ctx = ContextFactory.getContext(p);
            GlobalLogging.pp(logger, Level.DEBUG, ctx.getEnvironment(), "Context:");
	        Destination destination = (Destination)ctx.lookup("directQueue");
	        ConnectionFactory conFac = (ConnectionFactory)ctx.lookup("qpidConnectionfactory");
	        connection = conFac.createConnection();

	        connection.setExceptionListener(
	        		new ExceptionListener() 
	        		{
						public void onException(JMSException e) 
						{ 
					        GlobalLogging.pp(logger, Level.ERROR, e);
						}
	        		}
	        );
	        
	        session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
	        messageProducer = session.createProducer(destination);
	        messageProducer.setTimeToLive(ttl);
            logger.info("--CREATING PRODUCER / FINISHED--");
		} catch (Exception e) {
            logger.info("--CREATING PRODUCER / FAILED--");
            GlobalLogging.pp(logger, Level.ERROR, e);
		}
    	open = true;
    }

    
    public void genTestMessage(String id) {
    	try {
	        TextMessage m = session.createTextMessage("Message " + id);
	        q.add(m);
    	} catch (Exception e){
	        GlobalLogging.pp(logger, Level.ERROR, e);
    	}
    }

    public void sendMessage(Message m) {
    	if (q.remainingCapacity() == 0) {
    		GlobalLogging.pp(logger, Level.ERROR, "Producer failed to send message.  Queue full.");
    		return;
    	}
    	try {
	        q.add(m);
    	} catch (Exception e){
	        GlobalLogging.pp(logger, Level.ERROR, e);
    	}
    }

    public void sendMessageQuietly(Message m) {
    	try {
    		m.setBooleanProperty("quiet", true);
	        sendMessage(m);
    	} catch (Exception e){
	        GlobalLogging.pp(logger, Level.ERROR, e);
    	}
    }

    public void close() {
    	open = false;
    }
    
    public TextMessage createTextMessage() {
    	try {
    		return session.createTextMessage();
    	} catch (Exception e){
	        GlobalLogging.pp(logger, Level.ERROR, e);
    	}
    	return null;
    }

    public ObjectMessage createObjectMessage() {
    	try {
    		return session.createObjectMessage();
    	} catch (Exception e){
	        GlobalLogging.pp(logger, Level.ERROR, e);
    	}
    	return null;
    }

	public BytesMessage createBytesMessage() {
    	try {
    		return session.createBytesMessage();
    	} catch (Exception e){
	        GlobalLogging.pp(logger, Level.ERROR, e);
    	}
    	return null;
	}

	public BytesMessage createBytesMessage(MessageType type, byte[] bytes) throws ProducerException {
    	try {
    		BytesMessage m = session.createBytesMessage();
			m.setIntProperty(
					PropertyName.MESSAGE_TYPE.name(),
					type.toVal()
			);
			m.setIntProperty(PropertyName.BYTE_PAYLOAD_SIZE.name(), bytes.length);
			m.writeBytes(bytes);
    		return m; 
    	} catch (Exception e){
	        ProducerException pe = new ProducerException("Could not send bytes message.");
	        pe.initCause(e);
	        throw pe;
    	}
	}

    @Override
    public void run()
    {
        try
        {
            while (open) {
            	Message m = q.take();
            	messageProducer.send(m);
            	if (!m.propertyExists("quiet") || m.getBooleanProperty("quiet") == false) {
	    	        logger.trace("Sent " + m.getClass().getSimpleName() + " message.");
	    	        GlobalLogging.pp(logger, Level.TRACE, m.toString());
            	}
            }
            connection.close();
            ctx.close();
        }
        catch (Exception e)
        {
	        GlobalLogging.pp(logger, Level.ERROR, e);
        }
    }


}
