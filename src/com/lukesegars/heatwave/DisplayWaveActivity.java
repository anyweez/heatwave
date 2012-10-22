package com.lukesegars.heatwave;

import java.util.ArrayList;

import android.app.ListActivity;
import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ListView;

public class DisplayWaveActivity extends ListActivity {
	private static final String TAG = "DisplayWaveActivity";
	
	private HeatwaveDatabase database;
	private WaveArrayAdapter waveAdapter;
	private ArrayList<Wave> waves;

	@Override
	protected void onCreate(Bundle saved) {
		super.onCreate(saved);
		setContentView(R.layout.activity_display_waves);

		database = HeatwaveDatabase.getInstance(this);
		
		waves = new ArrayList<Wave>();
		waveAdapter = new WaveArrayAdapter(this, R.layout.display_wave_row, waves);
		
		getListView().setAdapter(waveAdapter);
		loadWaves();
	}
	
	@Override
    public void onListItemClick(ListView lv, View v, int position, long id) {
		Wave w = waveAdapter.getItem(position);
		
		// Pull up a list of contacts and highlight the ones that are included
		// in the wave identified by waveId.
		Intent intent = new Intent(this, WaveMemberActivity.class);
		intent.putExtra("waveId", w.getId());

		startActivity(intent);
	}
	
	@Override
	protected void onPause() {
		super.onPause();
	}

	@Override
	public void onResume() {
		super.onResume();
		
		loadWaves();
	}
	
	private void loadWaves() {
		ArrayList<Wave> dbWaves = database.fetchWaves();
		waveAdapter.clear();
		
		for (Wave w : dbWaves) waveAdapter.add(w);
		waveAdapter.notifyDataSetChanged();
	}
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.activity_wave_list, menu);
		return true;
	}
	
	@Override
    public boolean onOptionsItemSelected(MenuItem item) {
    	switch (item.getItemId()) {
    		case R.id.menu_new_wave:
    			Intent i = new Intent(this, EditWaveActivity.class);
				i.setFlags(Intent.FLAG_ACTIVITY_PREVIOUS_IS_TOP);
    			startActivity(i);
    			return true;
    		default:
    			return super.onOptionsItemSelected(item);
    	}
	}
}
