package com.lukesegars.heatwave;

import android.app.Activity;
import android.os.Bundle;
import android.text.InputType;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;

public class EditWaveActivity extends Activity {
	private static final String TAG = "EditWaveActivity";
	private HeatwaveDatabase database;
	
	private Wave target = null;
	@Override
	protected void onCreate(Bundle saved) {
		super.onCreate(saved);
		setContentView(R.layout.activity_edit_wave);
		
		// Make it so that only numbers will be provided into the wave length
		// field.
		TextView wave_length_field = (TextView) findViewById(R.id.wave_length);
		wave_length_field.setRawInputType(InputType.TYPE_CLASS_NUMBER);
		
		setButtonListeners();
		Bundle extras = getIntent().getExtras();
		if ( extras != null && extras.containsKey("waveId") ) {
			preloadWave(extras.getInt("waveId"));
		}
	}
	
	private void preloadWave(int waveId) {
		database = HeatwaveDatabase.getInstance(getApplicationContext());

		Wave w = database.fetchWave(waveId);
		target = w;
		
		// Update the UI.
		EditText wave_name = (EditText) findViewById(R.id.wave_name);
		wave_name.setText(w.getName());
		
		EditText wave_length = (EditText) findViewById(R.id.wave_length);
		wave_length.setText(String.valueOf(w.getWaveLength() / Wave.SECONDS_PER_UNIT));
	}
	
	private void setButtonListeners() {
		findViewById(R.id.cancel_btn).setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				// Finish this activity.  There's nothing to save.
				finish();
			}
		});
		
		findViewById(R.id.save_btn).setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				HeatwaveDatabase db = HeatwaveDatabase.getInstance(v.getContext());
				
				EditText name_field = (EditText)v.getRootView().findViewById(R.id.wave_name);
				EditText wl_field = (EditText)v.getRootView().findViewById(R.id.wave_length);
				
				// If we're creating a wave (not editing a pre-existing one).
				if (target == null) {
					// Save the new wave to the database 
					int waveDays = Integer.parseInt(wl_field.getText().toString());

					// Create the wave.
					Wave.create(name_field.getText().toString(), waveDays * Wave.SECONDS_PER_UNIT);
				}
				// If we're editing a pre-existing one.
				else {
					Wave.Fields wf = target.new Fields();
					int waveDays = Integer.parseInt(wl_field.getText().toString());
					
					wf.setName(name_field.getText().toString());
					wf.setWavelength(waveDays * Wave.SECONDS_PER_UNIT);
					
					target.modify(wf);
				}
				
				finish();
			}
		});
	}
}
