/*
 * CVS LOG TRACKING
 * ----------------
 * $Log: NamedProperties.java,v $
 * Revision 1.2  2009/10/27 17:13:28  bfox
 * Added CVS logging to everything.
 *
 *
 */

package com.foxtheories.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.util.Enumeration;
import java.util.Properties;
import java.util.TreeSet;


/**
 * AmqProperties does add additional functionality to the
 * Properties class.<br>
 * <br>
 * Properties can be organized by scope.  A String scope
 * name provides the ability to use non-unique key names.
 * This allows property keys and values to be duplicated 
 * across scopes.  Values can be retrieved as normal using 
 * the Properties methods.  Or values can be retrieved 
 * individually or in entirety by scope name.<br>
 * <br>  
 * An expected use is to hold multiple sets of values for
 * the same keys per scope.  For example, properties can
 * be stored for a "test" and "production" scope.<br>
 * <br>
 * Any property that is not specifically scoped is considered
 * global.  It will be included in mass retrieval methods.
 *  
 * @author Brian Fox
 */
public class NamedProperties extends Properties {

	private static final long serialVersionUID = 1L;

	protected TreeSet<String> scopeList;
	
	public NamedProperties() {
		super();
		scopeList = new TreeSet<String>();
	}

	
	/**
	 * Reads the properties found in the file argument.
	 * 
	 * @param f file to be read
	 * @throws IOException
	 */
	public NamedProperties(File f) throws IOException {
		FileInputStream fis = new FileInputStream(f);
		load(fis);
		fis.close();
		scopeList = new TreeSet<String>();
		Enumeration<?> e = this.propertyNames();
		while (e.hasMoreElements()) {
			String[] tokens = ((String)e.nextElement()).split("#");
			if (tokens.length > 1) {
				scopeList.add(tokens[0]);
			}
		}
	}

	/**
	 * Returns a Properties object containing all of the 
	 * root (no scope) properties in this NamedProperties object
	 * plus all of the properties associated with the scope
	 * name argument.
	 * 
	 * @param name
	 * @return
	 */
	public Properties getNamedProperties(String name) {
		Properties ret = new Properties();
		for (Object o : keySet()) {
			String [] tokens = o.toString().split("#");
			if (tokens.length == 1)
				ret.put(tokens[0], get(o.toString()));
		}
		for (Object o : keySet()) {
			String [] tokens = o.toString().split("#");
			if (tokens.length == 2 && tokens[0].compareTo(name) == 0)
				ret.put(tokens[1], get(o.toString()));
		}
		return ret;
	}
	
	/**
	 * Returns an enumeration of all the keys in this property list
	 * including root (no scope) properties and the scope properties
	 * defined by the argument.
	 * 
	 * @param scope
	 * @return
	 */
	public Enumeration<?> propertyNames(String scope) {
		return getNamedProperties(scope).propertyNames();
	}

	public Object setProperty(String scope, String key, String value) {
		scopeList.add(scope);
		return setProperty(scope + "#" + key, value);
	}

	public void setAllProperty(String key, String value) {
		setProperty(key, value);
		for (String s : scopeList)
			setProperty(s + "#" + key, value);
	}

	public void list(String scope, PrintWriter out) {
		getNamedProperties(scope).list(out);
	}

	public void list(String scope, PrintStream out) {
		getNamedProperties(scope).list(out);
	}
	
	String getNamedProperty(String scope, String key, String defaultValue) {
		return getProperty(scope + "#" + key, defaultValue);
	}

	String getNamedProperty(String scope, String key) {
		return getProperty(scope + "#" + key);
	}
	
	
}
