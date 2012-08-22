package com.lukesegars.heatwave;

import java.util.List;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
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
		
		Wave wave = getItem(position);
		
		if (convertView == null) {
			itemView = new LinearLayout(getContext());
			LayoutInflater inflater = 
				(LayoutInflater)getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
			inflater.inflate(resource, itemView, true);
		}
		else {
			itemView = (LinearLayout) convertView;
		}
		
		TextView waveTitleField = (TextView)itemView.findViewById(R.id.wave_title);
		TextView waveSubtitleField = (TextView)itemView.findViewById(R.id.wave_subtitle);
		
		waveTitleField.setText(wave.getName());
		waveSubtitleField.setText(database.getWaveMemberCount(wave) + " members");
		
		return itemView;
	}

}
