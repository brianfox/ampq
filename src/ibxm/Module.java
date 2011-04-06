/*
 * CVS LOG TRACKING
 * ----------------
 * $Log: Module.java,v $
 * Revision 1.2  2009/10/27 17:13:28  bfox
 * Added CVS logging to everything.
 *
 *
 */

package ibxm;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutput;
import java.io.ObjectOutputStream;
import java.io.Serializable;

public class Module implements Serializable {
	private static final long serialVersionUID = 1L;
	
	public String song_title;
	public boolean linear_periods, fast_volume_slides, pal;
	public int global_volume, channel_gain;
	public int default_speed, default_tempo;
	public int restart_sequence_index;
	
	private int[] initial_panning, sequence;
	private Pattern[] patterns;
	private Instrument[] instruments;
	
	private Pattern default_pattern;
	private Instrument default_instrument;
	
	public Module() {
		song_title = IBXM.VERSION;
		set_num_channels( 1 );
		set_sequence_length( 1 );
		set_num_patterns( 0 );
		set_num_instruments( 0 );
		default_pattern = new Pattern();
		default_instrument = new Instrument();
	}
	
	
	public int[] get_instrument_histogram() {
		if (instruments == null || patterns == null)
			return new int[0];
		int[] hist = new int[instruments.length];
		int[] note = new int[5];
		int channel_length = get_num_channels(); 
		for (Pattern p : patterns) {
			for (int current_row = 0; current_row < p.num_rows; current_row++) {
				for( int channel_idx = 0; channel_idx < channel_length; channel_idx++ ) {
					p.get_note( note, current_row * channel_length + channel_idx );
					hist[note[1]]++;
				}
			}
		}
		return hist;
	}
	
	public int get_num_channels() {
		return initial_panning.length;
	}
	
	public void set_num_channels( int num_channels ) {
		if( num_channels < 1 ) {
			num_channels = 1;
		}
		initial_panning = new int[ num_channels ];
	}
	
	public int get_initial_panning( int channel ) {
		int panning;
		panning = 128;
		if( channel >= 0 && channel < initial_panning.length ) {
			panning = initial_panning[ channel ];
		}
		return panning;
	}
	
	public void set_initial_panning( int channel, int panning ) {
		if( channel >= 0 && channel < initial_panning.length ) {
			initial_panning[ channel ] = panning;
		}
	}
	
	public int get_sequence_length() {
		return sequence.length;
	}
	
	public void set_sequence_length( int sequence_length ) {
		if( sequence_length < 0 ) {
			sequence_length = 0;
		}
		sequence = new int[ sequence_length ];
	}
	
	public void set_sequence( int sequence_index, int pattern_index ) {
		if( sequence_index >= 0 && sequence_index < sequence.length ) {
			sequence[ sequence_index ] = pattern_index;
		}
	}
	
	public int get_num_patterns() {
		return patterns.length;
	}
	
	public void set_num_patterns( int num_patterns ) {
		if( num_patterns < 0 ) {
			num_patterns = 0;
		}
		patterns = new Pattern[ num_patterns ];
	}

	public Pattern get_pattern_from_sequence( int sequence_index ) {
		Pattern pattern;
		pattern = default_pattern;
		if( sequence_index >= 0 && sequence_index < sequence.length ) {
			pattern = get_pattern( sequence[ sequence_index ] );
		}
		return pattern;
	}

	public Pattern get_pattern( int pattern_index ) {
		Pattern pattern;
		pattern = null;
		if( pattern_index >= 0 && pattern_index < patterns.length ) {
			pattern = patterns[ pattern_index ];
		}
		if( pattern == null ) {
			pattern = default_pattern;
		}
		return pattern;
	}

	public void set_pattern( int pattern_index, Pattern pattern ) {
		if( pattern_index >= 0 && pattern_index < patterns.length ) {
			patterns[ pattern_index ] = pattern;
		}
	}
	
	public int get_num_instruments() {
		return instruments.length;
	}
	
	public void set_num_instruments( int num_instruments ) {
		if( num_instruments < 0 ) {
			num_instruments = 0;
		}
		instruments = new Instrument[ num_instruments ];
	}
	
	public Instrument get_instrument( int instrument_index ) {
		Instrument instrument;
		instrument = null;
		if( instrument_index > 0 && instrument_index <= instruments.length ) {
			instrument = instruments[ instrument_index - 1 ];
		}
		if( instrument == null ) {
			instrument = default_instrument;
		}
		return instrument;
	}
	
	public void set_instrument( int instrument_index, Instrument instrument ) {
		if( instrument_index > 0 && instrument_index <= instruments.length ) {
			instruments[ instrument_index - 1 ] = instrument;
		}
	}
	
	public static Module load_module( String filename ) throws IllegalArgumentException, IOException {
		DataInputStream data_input_stream = new DataInputStream( new FileInputStream(filename) );
		
		// XM format.
		byte[] xm_header = new byte[ 60 ];
		data_input_stream.readFully( xm_header );
		if (FastTracker2.is_xm(xm_header))
			return FastTracker2.load_xm( 
					xm_header, 
					data_input_stream
			);
		
		// ScreamTracker3
		byte[] s3m_header = new byte[ 96 ];
		System.arraycopy(xm_header, 0, s3m_header, 0, 60);
		data_input_stream.readFully(s3m_header, 60, 36);
		if (ScreamTracker3.is_s3m( s3m_header ))
			return ScreamTracker3.load_s3m( 
					s3m_header, 
					data_input_stream 
			);
		
		// ProTracker 
		byte[] mod_header = new byte[ 1084 ];
		System.arraycopy( s3m_header, 0, mod_header, 0, 96 );
		data_input_stream.readFully( mod_header, 96, 988 );
		return ProTracker.load_mod( 
				mod_header, 
				data_input_stream 
		);
	}
	
	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		
		sb.append(String.format("Title: %s%n", song_title));
		sb.append(String.format("Linear Periods: %s    Fast Volume Slides: %s    Pal: %s%n", linear_periods, fast_volume_slides, pal));
		sb.append(String.format("Global Volume: %8d    Channel Gain:  %8d%n", global_volume, channel_gain));
		sb.append(String.format("Default Speed: %8d    Default Tempo: %8d%n", default_speed, default_tempo));

		/*
		public int restart_sequence_index;
		
		private int[] initial_panning, sequence;
		private Pattern[] patterns;
		private Instrument[] instruments;
		
		private Pattern default_pattern;
		private Instrument default_instrument;
		public boolean linear_periods, fast_volume_slides, pal;
		public int global_volume, channel_gain;
		public int default_speed, default_tempo;
		public int restart_sequence_index;
		
		private int[] initial_panning, sequence;
		private Pattern[] patterns;
		private Instrument[] instruments;
		
		private Pattern default_pattern;
		private Instrument default_instrument;
*/
		return sb.toString();
	}
	
	public byte[] toBytes() throws IOException {

		ByteArrayOutputStream bos = new ByteArrayOutputStream() ;
        ObjectOutput out = new ObjectOutputStream(bos) ;
        out.writeObject(this);
        out.close();
    
        return bos.toByteArray();
	}

	public static Module fromBytes(byte[] bytes) throws IOException, ClassNotFoundException {

        ObjectInputStream in = new ObjectInputStream(new ByteArrayInputStream(bytes));
        Module ret = (Module)in.readObject();
        in.close();
        return ret;
	}
}

