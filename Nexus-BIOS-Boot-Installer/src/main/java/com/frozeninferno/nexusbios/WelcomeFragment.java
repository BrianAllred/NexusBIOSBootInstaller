package com.frozeninferno.nexusbios;

import android.app.Activity;
import android.app.Fragment;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

/**
 * Created by Brian on 12/16/13.
 */
public class WelcomeFragment extends Fragment {

    private static final String ARG_SECTION_NUMBER = "section_number";

    public WelcomeFragment() {
    }

    public static WelcomeFragment newInstance(int sectionNumber) {
        WelcomeFragment fragment = new WelcomeFragment();
        Bundle args = new Bundle();
        args.putInt(ARG_SECTION_NUMBER, sectionNumber);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_welcome, container, false);
        TextView textView = (TextView) rootView.findViewById(R.id.section_label);
        textView.setText(R.string.about_header);
        textView.append(String.format(getResources().getString(R.string.about_section), "\n"));
        return rootView;
    }

    @Override
    public void onAttach(Activity activity) {
        Log.v("WelcomeTitle", Integer.toString(getArguments().getInt(ARG_SECTION_NUMBER)));
        super.onAttach(activity);
        ((MainActivity) activity).onSectionAttached(
                getArguments().getInt(ARG_SECTION_NUMBER));
    }
}

