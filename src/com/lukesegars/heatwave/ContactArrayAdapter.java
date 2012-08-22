package com.lukesegars.heatwave;

import java.util.List;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.LinearLayout;
import android.widget.TextView;

public class ContactArrayAdapter extends ArrayAdapter<Contact> {
	private int resource;
	
	public ContactArrayAdapter(Context context, int textViewResourceId, List<Contact> contacts) {
		super(context, textViewResourceId, contacts);
		resource = textViewResourceId;
	}
	
	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		LinearLayout itemView;
		
		Contact contact = getItem(position);
		
		if (convertView == null) {
			itemView = new LinearLayout(getContext());
			LayoutInflater inflater = 
				(LayoutInflater)getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
			inflater.inflate(resource, itemView, true);
		}
		else {
			itemView = (LinearLayout) convertView;
		}
		
		// Generate the gauge for the contact.
		TextView contactGauge = (TextView)itemView.findViewById(R.id.contact_gauge);
		
		contactGauge.setBackgroundDrawable(getContactGauge(contact));
		contactGauge.setText(String.valueOf(contact.getScore()));
		
		// Populate all of the text fields.
		TextView contactTitle = (TextView)itemView.findViewById(R.id.contact_name);
		TextView contactSubtitle = (TextView)itemView.findViewById(R.id.contact_subtitle);
		TextView contactNote = (TextView)itemView.findViewById(R.id.contact_note);
		
		contactTitle.setText(contact.getName());
		contactSubtitle.setText(contact.getSubtext());

		Wave cWave = contact.getWave();
		contactNote.setText( (cWave != null) ? cWave.getName() : "[none]" );
		
		return itemView;
	}
	
	private Drawable getContactGauge(Contact contact) {
		Resources res = getContext().getResources();
		Drawable background = res.getDrawable(R.drawable.contact_gauge);
		GradientDrawable gd = (GradientDrawable)background.mutate();
		gd.setColor(getGaugeColor(contact));
		
		return gd;
	}
	
	private int getGaugeColor(Contact contact) {
		int[] max_cold = new int[] { 20, 40, 125 };
		int[] max_warm = new int[] { 245, 40, 15 };
		
		double ratio = contact.getScore() / 10.0;
		double inv = 1 - ratio;
		
		int r = (int)Math.round((max_warm[0] * ratio) + (max_cold[0] * inv));
		int g = (int)Math.round((max_warm[1] * ratio) + (max_cold[1] * inv));
		int b = (int)Math.round((max_warm[2] * ratio) + (max_cold[2] * inv));
		
		int rgb = 0xff;
		rgb = (rgb << 8) + r;
		rgb = (rgb << 8) + g;
		rgb = (rgb << 8) + b;
		
		return rgb;
	}

}
