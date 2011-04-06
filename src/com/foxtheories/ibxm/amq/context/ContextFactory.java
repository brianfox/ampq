/*
 * CVS LOG TRACKING
 * ----------------
 * $Log: ContextFactory.java,v $
 * Revision 1.2  2009/10/27 17:13:28  bfox
 * Added CVS logging to everything.
 *
 *
 */

package com.foxtheories.ibxm.amq.context;

import java.util.Hashtable;
import java.util.Properties;

import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;


/**
 * ContextFactory creates an AMQP / JMS compatible context.  Convenience
 * methods create appropriate JMS URI addresses with the given AMQP
 * parameters.
 *  
 * @author Brian Fox
 */
public class ContextFactory {

	public static final String
            DEFAULT_INITIAL_CONTEXT_FACTORY
            = "org.apache.qpid.jndi.PropertiesFileInitialContextFactory";

    /**
     * Creates a context using the given Properties, which is expected to
     * have defined values for the keys: "user" "pass" "vhost" "clientid"
     * "broker" "port" "queue" and "key".
     *
     * @param properties a Properties object
     * @return a JMS/AMPQ ready context
     */
	public static Context getContext(Properties properties) {
		Context ctx; 
		ctx = ContextFactory.getContext(
				properties.getProperty("user"),
				properties.getProperty("pass"),
				properties.getProperty("vhost"),
				properties.getProperty("clientid"),
				properties.getProperty("broker"),
				properties.getProperty("port"),
				properties.getProperty("queue"),
				properties.getProperty("key")
		);
		return ctx;
	}


    /*
    TODO
     */
	public static Context getContext(
            String user,
            String pass,
            String vhost,
            String clientid,
            String broker,
            int port,
            String queue,
            String key
    ) {
		Context ctx;
		try {
			Hashtable<String,String> seed = new Hashtable<String,String>();
			seed.put(Context.INITIAL_CONTEXT_FACTORY, DEFAULT_INITIAL_CONTEXT_FACTORY);
			seed.put(
                    "connectionfactory.qpidConnectionfactory",
                    getConnectionFactoryUrl(user, pass, vhost, clientid, broker, port)
            );
			seed.put("destination.directQueue", getDirectQueueUrl(queue, key));
			ctx = new InitialContext(seed);
		} catch (NamingException e) {
			ctx = null;
		}
		return ctx;
	}


    /**
     * Convenience method allos getContext to be called with a String port parameter.
     *  
     * @param user JMS/AMPQ user name
     * @param pass JMS/AMPQ password
     * @param vhost virtual hostname
     * @param clientid client id
     * @param broker
     * @param port
     * @param queue
     * @param key
     * @return
     */
	public static Context getContext(
            String user,
            String pass,
            String vhost,
            String clientid,
            String broker,
            String port,
            String queue,
            String key
    ) {
		return getContext(user, pass, vhost, clientid, broker, Integer.parseInt(port), queue, key);
	}


    /**
     * Creates a JMS connection factory URL.
     *
     * @param user JMS/AMPQ user name
     * @param pass JMS/AMPQ password
     * @param client client id
     * @param vhost virtual hostname
     * @param broker broker address (FQDM or dotted IP)
     * @param port
     * @return
     */
	public static String getConnectionFactoryUrl(
            String user,
            String pass,
            String client,
            String vhost,
            String broker,
            int port
    ) {
		return String.format("amqp://%s:%s@%s/%s?brokerlist='tcp://%s:%d'", user, pass, client, vhost, broker, port);
	}

    /*
    TODO 
    */
	public static String getDirectQueueUrl(String queue, String routingkey) {
		return String.format("direct://amq.direct//%s?routingkey='%s'", queue, routingkey);
	}
}
