package com.lukesegars.heatwave;

import java.util.List;

import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

public class WaveArrayAdapter extends ArrayAdapter<Wave> {
	private int resource;
	private HeatwaveDatabase database;
	
	public WaveArrayAdapter(Context context, int textViewResourceId, List<Wave> waves) {
		super(context, textViewResourceId, waves);
		resource = textViewResourceId;
		
		database = HeatwaveDatabase.getInstance(getContext());
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		LinearLayout itemView;
		
		final Wave wave = getItem(position);
		
		if (convertView == null) {
			itemView = new LinearLayout(getContext());
			LayoutInflater inflater = 
				(LayoutInflater)getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
			inflater.inflate(resource, itemView, true);
		}
		else {
			itemView = (LinearLayout) convertView;
		}
		
		ImageView btn = (ImageView)itemView.findViewById(R.id.edit_wave);
		btn.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				Intent intent = new Intent(getContext(), EditWaveActivity.class);
				intent.putExtra("waveId", wave.getId());
				
				getContext().startActivity(intent);
			}
		});
		
		TextView waveTitleField = (TextView)itemView.findViewById(R.id.wave_name);
		TextView waveSubtitleField = (TextView)itemView.findViewById(R.id.wave_member_count);
		
		waveTitleField.setText(wave.getName());
		waveSubtitleField.setText(database.getWaveMemberCount(wave) + " members");
		
		return itemView;
	}

}
