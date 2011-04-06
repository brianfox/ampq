/*
 * CVS LOG TRACKING
 * ----------------
 * $Log: MessageType.java,v $
 * Revision 1.3  2009/10/29 17:22:56  bfox
 * Doesn't look like tick messages will work.  Too slow.  Perhaps go to a strict timing model?  Let the client decide which tick to play?
 *
 * Revision 1.2  2009/10/27 17:13:28  bfox
 * Added CVS logging to everything.
 *
 *
 */

package com.foxtheories.ibxm.amq.io.jms;


/**
 * The MessageType enum is a simple protocol enumeration that defines 
 * message names and integer values for various messages sent between
 * client and server.
 * 
 * @author Brian Fox
 */
public enum MessageType {
	
	/* server incoming */
	JOIN        (1000, "join"        ),
	MODULE      (1001, "module"      ),

	/* client incoming */
	PING        (2000, "ping"        ),
	PLAY_TICK   (2001, "play_tick"   ),
	QUIT        (2002, "quit"        ),
	RMODULE     (2003, "rmodule"     ),
	PLAY		(2004, "play"        ),	
	
	/* client outgoing */
	PONG        (3000, "pong"        ),
	PLAYED_TICK (3001, "played_tick" ),
	BYE         (3002, "bye"         ),
	REPORT		(3003, "report"      ),
	END_SONG    (3004, "end_song"    ),  	

	/* either */
	TEXT		(9000, "text"        ), 
	;

	private int i;
	private String name;
	
	MessageType(int i, String name) {
		this.i = i; 
		this.name = name;
	}
	
	public int toVal() {
		return i;
	}

	public static MessageType fromVal(int i) {
		for (MessageType m : MessageType.values())
			if (m.i == i)
				return m;
		return null;
	}

	@Override
	public String toString() {
		return name;
	}
}
