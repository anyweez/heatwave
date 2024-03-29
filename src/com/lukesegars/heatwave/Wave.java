package com.lukesegars.heatwave;

import java.util.ArrayList;

import com.lukesegars.heatwave.caches.WaveDataCache;

import android.content.ContentValues;
import android.content.Context;
import android.util.Log;

public class Wave {
	// The number of seconds per unit entered in the UI.  The UI currently
	// asks users for wavelengths in days, which translates into 86400 seconds.
	public static final int SECONDS_PER_UNIT = 86400;
	private static WaveDataCache waveCache = WaveDataCache.getInstance();
	
	public class Fields {
		private static final long DEFAULT_ID = -1;
		private static final int DEFAULT_WAVELENGTH = -1;
		private static final String DEFAULT_NAME = "";
		
		private long id = DEFAULT_ID;
		private String name = DEFAULT_NAME;
		private int waveLength = DEFAULT_WAVELENGTH;
		
		public boolean hasId() { return id != DEFAULT_ID; }
		public long getId() { return id; }
		public void setId(long i) { id = i; }
		
		public boolean hasName() { return name != DEFAULT_NAME; }
		public String getName() { return name; }
		public void setName(String n) { name = n; }
		
		public boolean hasWaveLength() { return waveLength != DEFAULT_WAVELENGTH; }
		public int getWavelength() { return waveLength; }
		public void setWavelength(int wl) { waveLength = wl; }
		
		protected void modify(Wave.Fields f) {
			if (f.hasId()) setId(f.getId());
			if (f.hasName()) setName(f.getName());
			if (f.hasWaveLength()) setWavelength(f.getWavelength());
		}
	}

	private static HeatwaveDatabase database = null;
	private Wave.Fields fields = new Wave.Fields();
	private static Context context = null;
	
	protected boolean hasName() { return fields.hasName(); }

	public static void setContext(Context c) {
		context = c;
		database = HeatwaveDatabase.getInstance();
	}
	
	//////////////////////////////
	/// Static factory methods ///
	//////////////////////////////
	public static Wave create(String name, int wl) {
		// If the wave already exists, return an instance of that
		// object and do not create a new row in the database.
		ArrayList<Wave> waves = waveCache.getAllEntries();
		for (Wave w : waves) {
			if (w.getName().equals(name)) {
				Log.i("Wave", "Loaded pre-existing wave " + name);
				return w;
			}
		}
		
		Wave w = new Wave();
		Wave.Fields wf = w.new Fields();

		long id = database.addWave(w);
		
		wf.setName(name);
		wf.setWavelength(wl);
		wf.setId(id);

		// Modify and cache the object.  Then store in persistent storage.
		w.modify(wf);

		return w;
	}
	
	public static Wave load(long id) {
		if (waveCache.entryExists(id)) return waveCache.getEntry(id);
		else {
			Wave wave = database.fetchWave(id);
			waveCache.addEntry(wave.getId(), wave);
			
			return wave;
		}
	}
	
	public static ArrayList<Wave> loadAll() {
		if (waveCache.numEntries() == 0) {
			ArrayList<Wave> waves = database.fetchWaves();
			
			for (Wave w : waves) {
				waveCache.addEntry(w.getId(), w);
			}
		}
		
		return waveCache.getAllEntries();
	}
	
	public static Wave skeleton() { return new Wave(); }
	
	////////////////////////////
	/// Private constructors ///
	////////////////////////////
	
	private Wave() {
		fields = new Wave.Fields();
	}
	
	private Wave(Wave.Fields f) {
		fields = f;
	}
	
	//////////////////////
	/// Public methods ///
	//////////////////////
	public void modify(Wave.Fields f, boolean updateDb) {
		fields.modify(f);
		
		// Update the database records if requested (default = true).
		if (updateDb) {
			database.updateWave(this);
			waveCache.invalidateEntry(getId());
		}
		else {
			waveCache.invalidateEntry(getId(), false);
			waveCache.addEntry(getId(), this);
		}
	}
	
	public void modify(Wave.Fields f) {
		modify(f, true);
	}
	
	public long getId() {
		return fields.getId();
	}
	
	public String getName() {
		return fields.getName();
	}
	
	public int getWaveLength() {
		return fields.getWavelength();
	}
	
	public ContentValues cv() {
		ContentValues cv = new ContentValues();
		
		cv.put("name", fields.getName());
		cv.put("wavelength", fields.getWavelength());
		
		return cv;
	}
	
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + 
			((fields.getName() == null) ? 0 : fields.getName().hashCode());
		result = prime * result + fields.getWavelength();
		
		return result;
	}

	/**
	 * Automatically generated by Eclipse (woohoo!)
	 * 
	 * Checks to ensure that objects are valid and pointers to fields
	 * are valid before comparing values.  If the two waves have the
	 * same WAVE_LENGTH and NAME then they will be declared as "equal."
	 */
	@Override
	public boolean equals(Object obj) {
		if (this == obj) return true;
		if (obj == null) return false;
		if (getClass() != obj.getClass()) return false;

		Wave other = (Wave) obj;
		
		// TODO: Not sure if this first condition is accurate...
		if (!fields.hasName() || !other.hasName()) return false;
		if (!getName().equals(other.getName())) return false;
		if (fields.getWavelength() != other.getWaveLength()) return false;
		
		return true;
	}

	@Override
	public String toString() {
		return "[Wave name=" + fields.getName() + 
			"', wavelength: " + String.valueOf(fields.getWavelength()) + "]";
	}
}
