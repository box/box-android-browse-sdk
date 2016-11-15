package com.box.androidsdk.browse.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.box.androidsdk.browse.R;

import java.util.List;

/**
 * Adapter that will be used to display recent search terms
 */
public class BoxRecentSearchAdapter extends ArrayAdapter<String> {

    /**
     * The interface Box recent search listener.
     */
    public interface BoxRecentSearchListener {
        /**
         * This method is called when user tries to delete a recent search term.
         *
         * @param position the position of the item in the list
         */
        void onCloseClicked(int position);
    }

    List<String> mHistory;
    BoxRecentSearchListener mListener;

    /**
     * Instantiates a new recent search adapter.
     *
     * @param context  the context
     * @param objects  list of recent search terms
     * @param listener listener to get callbacks if user tries to delete the search term from history
     */
    public BoxRecentSearchAdapter(Context context, List<String> objects, BoxRecentSearchListener listener) {
        super(context, R.layout.box_browsesdk_search_recent_item, objects);
        mHistory = objects;
        mListener = listener;
    }

    @Override
    public View getView(final int position, View convertView, ViewGroup parent) {
        if (convertView == null) {
            convertView = LayoutInflater.from(getContext()).inflate(R.layout.box_browsesdk_search_recent_item, parent, false);
        }

        TextView textView = (TextView) convertView.findViewById(R.id.text);
        textView.setText(mHistory.get(position));

        ImageView imageView = (ImageView) convertView.findViewById(R.id.close);
        imageView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mListener.onCloseClicked(position);
            }
        });

        return convertView;
    }
}
