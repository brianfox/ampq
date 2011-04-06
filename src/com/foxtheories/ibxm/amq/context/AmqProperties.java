/*
 * CVS LOG TRACKING
 * ----------------
 * $Log: AmqProperties.java,v $
 * Revision 1.2  2009/10/27 17:13:28  bfox
 * Added CVS logging to everything.
 *
 *
 */

package com.foxtheories.ibxm.amq.context;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Enumeration;
import java.util.Properties;

import com.foxtheories.util.NamedProperties;


/**
 * AmqProperties provides default properties for AMQ
 * IBXM related applications.<br>
 * <br>
 * AmqProperties supports three scopes: DEFAULT, CLIENT, and SERVER.
 * Each scope provides a AMQP context including broker IP address,
 * port, user name, password, virtual host, and client id.  It also
 * provides a more generic JMS context which included queue names.
 * Finally, each scope provides Apache log4j settings.<br>
 * <br>
 *
 * @author Brian Fox
 */
public class AmqProperties extends NamedProperties {

	private static final long serialVersionUID = 1L;
	public static String DEFAULT_PREFIX   = "ibxm_player_";
	public static String DEFAULT_USER     = "guest";
	public static String DEFAULT_PASS     = "guest";
	public static String DEFAULT_VHOST    = "vhost";
	public static String DEFAULT_CLIENTID = "clientid";
	public static String DEFAULT_BROKER   = "127.0.0.1";
	public static String DEFAULT_PORT     = "5672";
	
	public static String CONTEXT_CLIENT_QUEUE  = "CLIENT";
	public static String CONTEXT_SERVER_QUEUE  = "SERVER";

	static Properties standard; 
	static {

		/* GLOBAL */
		standard = new Properties();
		standard.put("user",     DEFAULT_USER              );
		standard.put("pass",     DEFAULT_PASS              );
		standard.put("vhost",    DEFAULT_VHOST             );
		standard.put("clientid", DEFAULT_CLIENTID          );
		standard.put("broker",   DEFAULT_BROKER            );
		standard.put("port",     DEFAULT_PORT              );
		standard.put("queue",    DEFAULT_PREFIX + "global" ); 
		standard.put("key",      DEFAULT_PREFIX + "global" );

		/* GLOBAL LOG4J */
		standard.put("log4j.rootLogger",                           "INFO, A1"                         );
		standard.put("log4j.appender.A1"    ,                      "org.apache.log4j.ConsoleAppender" );
		standard.put("log4j.appender.A1.layout",                   "org.apache.log4j.PatternLayout"   );
		standard.put("log4j.appender.A1.layout.ConversionPattern", "[%p]\t%d{yyyy-MM-dd\tHH:mm:ss}\t%t\t%c\t%m%n"                  );

		/* CLIENT */
		standard.put(CONTEXT_SERVER_QUEUE+"#user",     DEFAULT_USER              );
		standard.put(CONTEXT_SERVER_QUEUE+"#pass",     DEFAULT_PASS              );
		standard.put(CONTEXT_SERVER_QUEUE+"#vhost",    DEFAULT_VHOST             );
		standard.put(CONTEXT_SERVER_QUEUE+"#clientid", DEFAULT_CLIENTID          );
		standard.put(CONTEXT_SERVER_QUEUE+"#broker",   DEFAULT_BROKER            );
		standard.put(CONTEXT_SERVER_QUEUE+"#port",     DEFAULT_PORT              );
		standard.put(CONTEXT_SERVER_QUEUE+"#queue",    DEFAULT_PREFIX + "server" ); 
		standard.put(CONTEXT_SERVER_QUEUE+"#key",      DEFAULT_PREFIX + "server" );

		/* SERVER */
		standard.put(CONTEXT_CLIENT_QUEUE+"#user",     DEFAULT_USER              );
		standard.put(CONTEXT_CLIENT_QUEUE+"#pass",     DEFAULT_PASS              );
		standard.put(CONTEXT_CLIENT_QUEUE+"#vhost",    DEFAULT_VHOST             );
		standard.put(CONTEXT_CLIENT_QUEUE+"#clientid", DEFAULT_CLIENTID          );
		standard.put(CONTEXT_CLIENT_QUEUE+"#broker",   DEFAULT_BROKER            );
		standard.put(CONTEXT_CLIENT_QUEUE+"#port",     DEFAULT_PORT              );
		standard.put(CONTEXT_CLIENT_QUEUE+"#queue",    DEFAULT_PREFIX + "server" ); 
		standard.put(CONTEXT_CLIENT_QUEUE+"#key",      DEFAULT_PREFIX + "server" );  
		
	}

	
	
	/**
	 * Creates an AmqProperties object using default values.
	 */
	public AmqProperties() {
		loadDefaults();
	}

    /**
     * Creates an AmqProperties object with key/value pairs loaded from File f.
     * For any key found in the default key/value pairs but not found in File f,
     * the default will be used.
     * 
     * @param f
     * @throws IOException if an IOException occurs while handling File f
     */
	public AmqProperties(File f) throws IOException {
		loadDefaults();
		FileInputStream fis = new FileInputStream(f);
		load(fis);
		fis.close();
	}

	private void loadDefaults() {
		for (Object o : standard.keySet()) {
			put(o.toString(), standard.get(o));
		}
		Enumeration<?> e = propertyNames();
		while (e.hasMoreElements()) {
			String[] tokens = ((String)e.nextElement()).split("#");
			if (tokens.length > 1) {
				scopeList.add(tokens[0]);
			}
		}

	}


}