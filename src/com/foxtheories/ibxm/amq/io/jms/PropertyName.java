package com.foxtheories.ibxm.amq.io.jms;

/**
 * PropertyName enumerates the various JMS/AMQP properties which
 * can be sent within a message header.  The enumeration is always
 * preferred over String named properties for naming safety.  It
 * also helps a great deal with refactoring when necessary.
 * 
 * @author bfox
 *
 */
public enum PropertyName {
	
	MESSAGE_TYPE("message_type"),
	CLIENT_ID   ("client_id"   ),
	BYTE_PAYLOAD_SIZE ("module_size" ),
	SAMPLE_RATE ("sample_rate" ),
	TICK_PAUSE  ("tick_pause"  )
	;

	private String name;
	
	PropertyName(String name) {
		this.name = name;
	}
	
	public String val() {
		return name;
	}
}
