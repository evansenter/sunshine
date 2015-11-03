package com.evansenter.sunshine;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

public class DetailActivityFragment extends Fragment {
  private final String LOG_TAG = DetailActivityFragment.class.getSimpleName();

  public DetailActivityFragment() {
  }

  @Override
  public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
    View rootView = inflater.inflate(R.layout.fragment_detail, container, false);
    Intent intent = getActivity().getIntent();
    String forecast = intent.getStringExtra(Constants.DETAIL_DATA);
    Log.v(LOG_TAG, forecast);

    return rootView;
  }


}
