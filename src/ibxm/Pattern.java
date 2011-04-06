/*
 * CVS LOG TRACKING
 * ----------------
 * $Log: Pattern.java,v $
 * Revision 1.2  2009/10/27 17:13:28  bfox
 * Added CVS logging to everything.
 *
 *
 */

package ibxm;

import java.io.Serializable;

public class Pattern implements Serializable {

	private static final long serialVersionUID = 1L;

	public int num_rows;
	
	private int data_offset, note_index;
	private byte[] pattern_data;
	private int[] inst_histogram;

	public Pattern() {
		num_rows = 1;
		set_pattern_data( new byte[ 0 ], 0 );
		inst_histogram = new int[256];
	}
	
	public void set_pattern_data( byte[] data, int num_channels ) {
		if( data != null ) {
			pattern_data = data;
		}
		data_offset = 0;
		note_index = 0;
		int current_row = 0;
		int[] note = new int[5];
		while (current_row < 0)
			for (int i=0; i < num_channels; i++) {
				get_note( note, current_row * num_channels + i );
				inst_histogram[note[1]]++;
			}
		// while
	}

	public void get_note( int[] note, int index ) {
		if( index < note_index ) {
			note_index = 0;
			data_offset = 0;
		}
		while( note_index <= index ) {
			data_offset = next_note( data_offset, note );
			note_index += 1;
		}
	}

	public int next_note( int data_offset, int[] note ) {
		int bitmask, field;
		if( data_offset < 0 ) {
			data_offset = pattern_data.length;
		}
		bitmask = 0x80;
		if( data_offset < pattern_data.length ) {
			bitmask = pattern_data[ data_offset ] & 0xFF;
		}
		if( ( bitmask & 0x80 ) == 0x80 ) {
			data_offset += 1;
		} else {
			bitmask = 0x1F;
		}
		for( field = 0; field < 5; field++ ) {
			note[ field ] = 0;
			if( ( bitmask & 0x01 ) == 0x01 ) {
				if( data_offset < pattern_data.length ) {
					note[ field ] = pattern_data[ data_offset ] & 0xFF;
					data_offset += 1;
				}
			}
			bitmask = bitmask >> 1;
		}
		return data_offset;
	}
}

