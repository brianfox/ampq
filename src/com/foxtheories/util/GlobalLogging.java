/*
 * CVS LOG TRACKING
 * ----------------
 * $Log: GlobalLogging.java,v $
 * Revision 1.2  2009/10/27 17:13:28  bfox
 * Added CVS logging to everything.
 *
 *
 */

package com.foxtheories.util;

import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Properties;

import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.Level;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;

public class GlobalLogging {
	
	static {
	   BasicConfigurator.configure();
	   setLevel(Level.OFF);
	}
	
	public static void setLevel(Level level) {
    	@SuppressWarnings("unchecked")
    	Enumeration<Logger> e = LogManager.getCurrentLoggers();
    	while (e.hasMoreElements()) {
    		e.nextElement().setLevel(level);
    	}
	}
	
	public static Logger createLog(String name, Properties p) {
		PropertyConfigurator.configure(p);
		Logger l = Logger.getLogger(name);
		return l;
	}

	public static Logger createLog(String name) {
		Logger l = Logger.getLogger(name);
		return l;
	}
	
	public static void pp(Logger logger, Level level, String s) {
		for (String x : s.split("\n"))
			logger.log(level, String.format(">    %s", x.trim())
		);

	}

	public static void pp(Logger logger, Level level, Properties p, String header) {
		logger.log(level, header);
		for (Object o : p.keySet())
			logger.log(
					level, 
					String.format(
							">    %s = %s", 
							o.toString().trim(), 
							p.getProperty(o.toString().trim())
					)
			);
	}

	public static void pp(Logger logger, Level level, Hashtable<?,?> h, String header) {
		logger.log(level, header);
		for (Object o : h.keySet())
			logger.log(
					level, 
					String.format(
							">    %s = %s", 
							o.toString().trim(), 
							h.get(o).toString().trim()
					)
			);
	}

	public static void pp(Logger logger, Level level, Exception e) {
		logger.log(level, "An exception occured: ");
		logger.log(level, String.format(">    %s", e.getMessage()));
		
		for (StackTraceElement s : e.getStackTrace())
			logger.log(
					level, 
					String.format(
							">    %s", 
							s.toString().trim()
					)
			);
	}
}
