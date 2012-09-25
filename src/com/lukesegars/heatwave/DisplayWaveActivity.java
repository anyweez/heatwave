package com.lukesegars.heatwave;

import java.util.ArrayList;

import android.app.ListActivity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListAdapter;
import android.widget.ListView;

public class DisplayWaveActivity extends ListActivity {
	private static final String TAG = "DisplayWaveActivity";
//	private static final int MENU_WAVE_EDIT = 0;
//	private static final int MENU_WAVE_DELETE = 1;
	
	private HeatwaveDatabase database;
	private WaveArrayAdapter waveAdapter;
	private ArrayList<Wave> waves;

	@Override
	protected void onCreate(Bundle saved) {
		super.onCreate(saved);
		setContentView(R.layout.activity_display_waves);
		registerForContextMenu(getListView());

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
    			startActivity(i);
    			return true;
    		default:
    			return super.onOptionsItemSelected(item);
    	}
	}
	
//	@Override
//	public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo) {
//		super.onCreateContextMenu(menu, v, menuInfo);
//		
//		menu.setHeaderTitle("Modify wave");
//		menu.add(0, MENU_WAVE_EDIT, 0, "Edit wave");
//		menu.add(0, MENU_WAVE_DELETE, 0, "Delete wave");
//	}
	
//	@Override
//	public boolean onContextItemSelected(MenuItem item) {
//		AdapterView.AdapterContextMenuInfo info = 
//			(AdapterView.AdapterContextMenuInfo) item.getMenuInfo();
//
//		int id = (int)waveAdapter.getItemId(info.position);
//		Wave w = waveAdapter.getItem(id);
//
//		if (item.getItemId() == MENU_WAVE_EDIT) {
//			Intent i = new Intent(this, EditWaveActivity.class);
//			i.putExtra("waveId", w.getId());
//			startActivity(i);
//		}
//		else if (item.getItemId() == MENU_WAVE_DELETE) {
//			database.removeWave(w);
//			waveAdapter.remove(w);
//		}
//		waveAdapter.notifyDataSetChanged();
//
//		return false;
//	}
}
