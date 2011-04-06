/*
 * CVS LOG TRACKING
 * ----------------
 * $Log: Consumer.java,v $
 * Revision 1.3  2009/10/29 17:22:56  bfox
 * Doesn't look like tick messages will work.  Too slow.  Perhaps go to a strict timing model?  Let the client decide which tick to play?
 *
 * Revision 1.2  2009/10/27 17:13:28  bfox
 * Added CVS logging to everything.
 *
 *
 */

package com.foxtheories.ibxm.amq.io.jms;

import java.util.Properties;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.TimeUnit;

import javax.jms.Connection;
import javax.jms.ConnectionFactory;
import javax.jms.Destination;
import javax.jms.ExceptionListener;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageConsumer;
import javax.jms.Session;
import javax.naming.Context;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;

import com.foxtheories.ibxm.amq.context.ContextFactory;
import com.foxtheories.util.GlobalLogging;

/**
 * Consumer provides a Thread wrapper to the MessageConsumer class.  A Consumer object
 * provides blocking receive methods which allow other threads executing a receive to
 * wait gracefully until the next Message is available.
 *
 * @author Brian Fox 
 */
public class Consumer extends Thread
{
    private static final int QUEUE_SIZE = 50;
    private boolean open;
    
    private ArrayBlockingQueue<Message> q;
    private Context                     ctx;
    private Connection                  connection;
    private Logger 						logger;
    private MessageConsumer             messageConsumer;
    
    public Consumer(Properties p) 
    {
    	logger = GlobalLogging.createLog("com.foxtheories.amq.consumer", p);
        logger.info("--CREATING CONSUMER / START--");
    	q      = new ArrayBlockingQueue<Message>(QUEUE_SIZE);
        
     	try {
    		ctx = ContextFactory.getContext(p);
	        Destination destination = (Destination)ctx.lookup("directQueue");
	        ConnectionFactory conFac = (ConnectionFactory)ctx.lookup("qpidConnectionfactory");
	        Connection connection = conFac.createConnection();
	        GlobalLogging.pp(logger, Level.DEBUG, ctx.getEnvironment(), "Context:");
    		
	
	        connection.setExceptionListener(
	        		new ExceptionListener() 
	        		{
						public void onException(JMSException e) 
						{ 
					        GlobalLogging.pp(logger, Level.ERROR, e);
						}
	        		}
	        );
	
	        Session session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
	        messageConsumer = session.createConsumer(destination);
	        logger.debug("Created AMQ Consumer");
	        connection.start();
	        logger.info("--CREATING CONSUMER / FINISHED--");
	    }
		catch (Exception e) 
		{
            logger.info("--CREATING CONSUMER / FAILED--");
            GlobalLogging.pp(logger, Level.ERROR, e);
		}
		open = true;
    
    }

    public Message receive() {
    	try {
    		return q.take();
    	}
    	catch (Exception e) {
	        GlobalLogging.pp(logger, Level.ERROR, e);
    	}
    	return null;
    }


    public void flush(int timeout) throws InterruptedException {
    	while (true) 
    	{
    		Message x = q.poll(timeout, TimeUnit.MILLISECONDS);
    		if (x == null)
    			return;
    	}
    }

    public Message receive(int timeout) {
    	try {
    		return q.poll(timeout,TimeUnit.MILLISECONDS);
    	}
    	catch (Exception e) {
	        GlobalLogging.pp(logger, Level.ERROR, e);
    	}
    	return null;
    }

	@Override
	public void run() {
        try
        {
			while (open)
			{
		    	Message m = messageConsumer.receive();
		    	if (q.remainingCapacity() == 0) {
		    		GlobalLogging.pp(logger, Level.ERROR, "Consumer failed to receive message.  Queue full.");
		    		continue;
		    	}
				q.put(m);
            	if (!m.propertyExists("quiet") || m.getBooleanProperty("quiet") == false) {
    		        logger.trace("Received message: " + m.getJMSMessageID());
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

	public void close() {
		// TODO Auto-generated method stub
		
	}

	public int getRemainingCapacity() {
		return q.remainingCapacity();
	}

	public int getSize() {
		return q.size();
	}

}
