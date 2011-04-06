/*
 * CVS LOG TRACKING
 * ----------------
 * $Log: ServerCommandList.java,v $
 * Revision 1.2  2009/10/27 17:13:28  bfox
 * Added CVS logging to everything.
 *
 *
 */

package com.foxtheories.ibxm.apps.dist;

import com.foxtheories.util.StringUtil;
import java.util.regex.Pattern;

/**
 * 
 * @author bfox
 */
public enum ServerCommandList {


	/* INFORMATIVE COMMANDS */
	/* INFORMATIVE COMMANDS */
	/* INFORMATIVE COMMANDS */

	SHOW_CLIENTS    (
			"show clients",     
			"show\\s+clients",     
			Type.info,
            "Show all the clients attached to this server"
	),
	SHOW_INSTRUMENTS(
			"show instruments", 
			"show\\s+instruments", 
			Type.info,
            "Show all the instruments used in the current Module file"
	),
	SHOW_MODULE     (
			"show module",      
			"show\\s+module",      
			Type.info,
            "Show the details of the current Module file"
	),
	SHOW_CREDITS    (
			"show credits",     
			"show\\s+credits",     
			Type.info,
            "Show author credits of contributing code"
	),
	SHOW_AMQP       (
			"show amqp",        
			"show\\s+amqp",        
			Type.info,
            "Show AMQP performance statistics"
	),
	SHOW_LAG        (
			"show lag",         
			"show\\s+lag",         
			Type.info,
            "Show injected synchronization pause statistics"
	),
	

	/* PLAYBACK COMMANDS */
	/* PLAYBACK COMMANDS */
	/* PLAYBACK COMMANDS */
	PLAY (
			"play", 
			"play",  
			Type.tape,
            "Play the current Module file"
	),
	PAUSE(
			"pause", 
			"pause", 
			Type.tape,
            "Pause/Restart playback (toggle)"
	),
	STOP (
			"stop" , 
			"stop",  
			Type.tape,
            "Stop play, playback cannot be restarted"
	),
	RESET(
			"reset", 
			"reset", 
			Type.tape,
            "Reset the current playback to the beginning of the Module file"
	),

	/* SYSTEM COMMANDS */
	/* SYSTEM COMMANDS */
	/* SYSTEM COMMANDS */
	LOAD(
			"load \"<filename>\"", 
			"load\\s+\".*\"", 
			Type.system,
            "Load the specified Module file"
	),
	QUIT(
			"quit",                
			"quit",           
			Type.system,
            "Inform each client that the server is closing, then quit"
	),
    HELP(
    		"help",                
    		"help",           
    		Type.system,
            "Show help"
    ),

	/* MISC COMMANDS */
	/* MISC COMMANDS */
	/* MISC COMMANDS */
	STRATEGY_BROADCAST  (
			"strategy broadcast" , 
			"strategy\\s+broadcast",  
			Type.strategy,
            "Switch to broadcast mixing.  Each clients receives each tick."
	),
	STRATEGY_ROUND_ROBIN(
			"strategy roundrobin", 
			"strategy\\s+roundrobin", 
			Type.strategy,
            "Switch to round robin mixing.  Clients are arranged in a circular queue.  " +
            "The next tick is distributed to head of the queue and the queue is " +
            "advanced."
	),
	STRATEGY_INSTRUMENT (
			"strategy instrument", 
			"strategy\\s+instrument", 
			Type.strategy,
            "Switch to instrument mixing.  Clients are assigned specific instruments.  " +
            "Assignment is based on first encounter.  When an instrument is introduced, " +
            "it is assigned to the client with the fewest number of instruments."
	),
	STRATEGY_CHANNEL    (
			"strategy channel"   , 
			"strategy\\s+channel   ", 
			Type.strategy,
            "Switch to channel mixing.  Clients are assigned specific channels.  " +
            "Channels are fairly limited, usually four to a file.  Channel assignment is " +
            "similar to instrument mixing.  The least tasked client gets the next channel."
	),
	;

	private final static String HELP_INTRO = 
    	"AMPQ Server commands can be issued at any time.  " +
    	"A list of commands grouped by function follows.";

	private final static String HELP_TITLE = ":::AMPQ SERVER HELP:::"; 
	
	enum Type {
	    info     ("Informative Commands"),
	    tape     ("Playback Commands"),
	    system   ("System Commands"),
	    strategy ("Mixing Strategy Commands");

	    private String title;

	    private Type(String title) {
	        this.title = title;    
	    }

	    public String title() {
	        return title;
	    }
	}

    /* 
     * HELP SYSTEM BELOW
     * HELP SYSTEM BELOW
     * HELP SYSTEM BELOW
     * 
     * Most of the code below shouldn't need text changes.  It builds
     * up the enum, populates the static help string, and adds a few
     * help methods to the class.
     */
	
    private static String helpString;

    static {
        StringBuilder sb = new StringBuilder();
        sb.append(String.format("%n%s%n%n", HELP_TITLE));
        sb.append(StringUtil.wrapText(HELP_INTRO, 70, false));
        sb.append(String.format("%n%n"));
        int maxcommand = 0;
        for (ServerCommandList c : ServerCommandList.values()) {
            if (c.title.length() > maxcommand)
                maxcommand = c.title.length();
        }
        String format = "%-" + maxcommand + "s";
        for (Type t : Type.values()) {
            sb.append(String.format("%s%n", t.title()));
            for (ServerCommandList c : ServerCommandList.values()) {
                if (c.t == t) {
                    String x = String.format(format, c.title);
                    String y = StringUtil.wrapText(c.help,  70-maxcommand, true);
                    sb.append(StringUtil.blockConcatenate("    ", x, " - ", y));
                }
            }
            sb.append(String.format("%n%n"));
        }
        helpString = sb.toString();
    }
	
	/* -------------------------  BOILER PLATE ENUM CODE BELOW ---------------------*/
	private String title;
	private Pattern pattern;
    private Type t;
    private String help;
	
	ServerCommandList(String title, String regex, Type type, String help)
	{ 
		this.title = title;
		this.pattern = Pattern.compile(regex);
        this.t = type;
        this.help = help;
	}

    public String toString() { return title; }

	public static ServerCommandList fromString(String s) {

		s = s.trim();
		for (ServerCommandList c : ServerCommandList.values())
		{
			if (c.pattern.matcher(s).matches())
				return c;
		}
		return null;
	}

    public static String help() {
        return helpString;
    }
}

