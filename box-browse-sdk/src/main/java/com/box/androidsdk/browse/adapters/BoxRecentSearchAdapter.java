package com.box.androidsdk.browse.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import com.box.androidsdk.browse.R;

import java.util.List;

public class BoxRecentSearchAdapter extends ArrayAdapter<String> {

    List<String> mHistory;

    public BoxRecentSearchAdapter(Context context, List<String> objects) {
        super(context, R.layout.box_browsesdk_search_recent_item, objects);
        mHistory = objects;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        if (convertView == null) {
            convertView = LayoutInflater.from(getContext()).inflate(R.layout.box_browsesdk_search_recent_item, parent, false);
        }

        TextView textView = (TextView) convertView.findViewById(R.id.text);
        textView.setText(mHistory.get(position));

        return convertView;
    }
}
