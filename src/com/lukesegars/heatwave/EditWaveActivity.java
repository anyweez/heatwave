package com.lukesegars.heatwave;

import com.lukesegars.heatwave.caches.ContactDataCache;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.text.InputType;
import android.view.View;
import android.widget.Button;
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
		
		// If a wave was specified, attach a listener to the delete button.
		if (isEditingWave()) {
			Bundle extras = getIntent().getExtras();
			preloadWave(extras.getLong("waveId"));

			Button btn = (Button)findViewById(R.id.delete_wave_btn);
			btn.setOnClickListener(new View.OnClickListener() {
				public void onClick(View v) {
					database.removeWave(target);
					// TODO: Can we do a partial invalidation here?  Not sure
					// how easy or fast that would be...
					ContactDataCache.getInstance().invalidateByWave(target);
//					ContactDataCache.getInstance().invalidateAll();
					finish();
				}
			});
		}
		// If no wave was specified, we're creating a new one and the delete button
		// makes no sense.  Hide it.
		else {
			Button btn = (Button)findViewById(R.id.delete_wave_btn);
			btn.setVisibility(Button.INVISIBLE);
			
			TextView btn_lbl = (TextView)findViewById(R.id.delete_wave_warning);
			btn_lbl.setVisibility(TextView.INVISIBLE);
		}
	}

	private boolean isEditingWave() {
		Bundle extras = getIntent().getExtras();
		return extras != null && extras.containsKey("waveId");
	}
	
	private void preloadWave(long waveId) {
		database = HeatwaveDatabase.getInstance();
		target = database.fetchWave(waveId);
		
		// Update the UI.
		EditText wave_name = (EditText) findViewById(R.id.wave_name);
		wave_name.setText(target.getName());
		
		EditText wave_length = (EditText) findViewById(R.id.wave_length);
		wave_length.setText(String.valueOf(target.getWaveLength() / Wave.SECONDS_PER_UNIT));
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
				EditText name_field = (EditText)v.getRootView().findViewById(R.id.wave_name);
				EditText wl_field = (EditText)v.getRootView().findViewById(R.id.wave_length);
				
				// If we're creating a wave (not editing a pre-existing one).
				if (!isEditingWave()) {
					// Save the new wave to the database 
					int waveDays = Integer.parseInt(wl_field.getText().toString());

					// Create the wave.
					Wave newWave = Wave.create(name_field.getText().toString(), 
						waveDays * Wave.SECONDS_PER_UNIT);
					
					// Pass the user along to the wave member selector.
					Intent intent = new Intent(getApplicationContext(), WaveMemberActivity.class);
					intent.putExtra("waveId", newWave.getId());
					
					startActivity(intent);
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
