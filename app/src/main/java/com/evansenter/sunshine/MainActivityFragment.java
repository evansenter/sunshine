package com.evansenter.sunshine;

import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.text.format.Time;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class MainActivityFragment extends Fragment {
  ArrayAdapter<String> forecastAdapter;

  public MainActivityFragment() {
  }

  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setHasOptionsMenu(true);
  }

  @Override
  public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
    // Inflate the menu; this adds items to the action bar if it is present.
    inflater.inflate(R.menu.menu_refresh, menu);
  }

  @Override
  public boolean onOptionsItemSelected(MenuItem item) {
    // Handle action bar item clicks here. The action bar will
    // automatically handle clicks on the Home/Up button, so long
    // as you specify a parent activity in AndroidManifest.xml.
    int id = item.getItemId();

    if (id == R.id.action_refresh) {
      new FetchWeatherTask().execute("94158,USA");
      return true;
    }

    return super.onOptionsItemSelected(item);
  }

  @Override
  public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
    View rootView = inflater.inflate(R.layout.fragment_main, container, false);
    String[] fakeForecast = {
        "Today - Sunny - 88/63",
        "Tomorrow - Foggy - 69/40",
        "Weds - Foggy - 70/40",
        "Thurs- Foggy - 71/40",
        "Fri - Foggy - 72/40",
        "Sat - Foggy - 73/40",
        "Sun - Foggy - 74/40",
        "Today - Sunny - 88/63",
        "Tomorrow - Foggy - 69/40",
        "Weds - Foggy - 70/40",
        "Thurs- Foggy - 71/40",
        "Fri - Foggy - 72/40",
        "Sat - Foggy - 73/40",
        "Sun - Foggy - 74/40"
    };

    List<String> weekForecast = new ArrayList<>(Arrays.asList(fakeForecast));

    forecastAdapter = new ArrayAdapter<>(
        getActivity(),
        R.layout.list_item_forecast,
        R.id.list_item_forecast_textview,
        weekForecast
    );

    ListView listView = (ListView) rootView.findViewById(R.id.listview_forecast);
    listView.setAdapter(forecastAdapter);
    listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
      @Override
      public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        Intent detailIntent = new Intent(getActivity(), DetailActivity.class);
        detailIntent.putExtra(Constants.DETAIL_DATA, forecastAdapter.getItem(position));
        startActivity(detailIntent);
      }
    });

    return rootView;
  }

  public class FetchWeatherTask extends AsyncTask<String, Void, String[]> {
    private final String LOG_TAG = FetchWeatherTask.class.getSimpleName();

    @Override
    protected String[] doInBackground(String... params) {
      if (params.length == 0) {
        return null;
      }

      HttpURLConnection urlConnection = null;
      BufferedReader reader = null;

      final String WEATHER_BASE_URL = "api.openweathermap.org";
      final String API_KEY_PARAM = "appid";
      final String QUERY_PARAM = "q";
      final String UNITS_PARAM = "units";
      final String COUNT_PARAM = "cnt";
      final String MODE_PARAM = "mode";

      final String API_KEY_VALUE = "3b1ce8b7346876befd83e4303816e37b";
      String unitsValue = "metric";
      String countValue = "7";
      final String MODE_VALUE = "json";

      try {
        String urlString = new Uri.Builder()
            .scheme("http")
            .authority(WEATHER_BASE_URL)
            .appendPath("data")
            .appendPath("2.5")
            .appendPath("forecast")
            .appendPath("daily")
            .appendQueryParameter(API_KEY_PARAM, API_KEY_VALUE)
            .appendQueryParameter(UNITS_PARAM, unitsValue)
            .appendQueryParameter(COUNT_PARAM, countValue)
            .appendQueryParameter(MODE_PARAM, MODE_VALUE)
            .build().toString() + String.format("&%s=%s", QUERY_PARAM, params[0]);

        Log.v(LOG_TAG, urlString);

        URL url = new URL(urlString);

        urlConnection = (HttpURLConnection) url.openConnection();
        urlConnection.setRequestMethod("GET");
        urlConnection.connect();

        InputStream inputStream = urlConnection.getInputStream();
        if (inputStream == null) {
          return null;
        }

        reader = new BufferedReader(new InputStreamReader(inputStream));

        StringBuilder buffer = new StringBuilder();
        String line;
        while ((line = reader.readLine()) != null) {
          buffer.append(line).append("\n");
        }

        if (buffer.length() == 0) {
          return null;
        }

        String forecastJsonStr = buffer.toString();
        String[] forecast;
        try {
          forecast = getWeatherDataFromJson(forecastJsonStr, Integer.parseInt(countValue));
          Log.v(LOG_TAG, Arrays.toString(forecast));
          return forecast;
        } catch (JSONException e) {
          Log.e(LOG_TAG, "JSON parse error", e);
        }
      } catch (IOException e) {
        Log.e(LOG_TAG, "Misc. error", e);
      } finally {
        if (urlConnection != null) {
          urlConnection.disconnect();
        }
        if (reader != null) {
          try {
            reader.close();
          } catch (final IOException e) {
            Log.e(LOG_TAG, "Error closing stream", e);
          }
        }
      }

      return null;
    }

    @Override
    protected void onPostExecute(String[] forecast) {
      if (forecast != null) {
        forecastAdapter.clear();
        for (String forecastDay : forecast) {
          forecastAdapter.add(forecastDay);
        }
      }
    }

    /* The date/time conversion code is going to be moved outside the asynctask later,
         * so for convenience we're breaking it out into its own method now.
         */
    private String getReadableDateString(long time) {
      // Because the API returns a unix timestamp (measured in seconds),
      // it must be converted to milliseconds in order to be converted to valid date.
      SimpleDateFormat shortenedDateFormat = new SimpleDateFormat("EEE MMM dd");
      return shortenedDateFormat.format(time);
    }

    /**
     * Prepare the weather high/lows for presentation.
     */
    private String formatHighLows(double high, double low) {
      // For presentation, assume the user doesn't care about tenths of a degree.
      long roundedHigh = Math.round(high);
      long roundedLow = Math.round(low);

      String highLowStr = roundedHigh + "/" + roundedLow;
      return highLowStr;
    }

    /**
     * Take the String representing the complete forecast in JSON Format and pull out the data we
     * need to construct the Strings needed for the wireframes.
     *
     * Fortunately parsing is easy:  constructor takes the JSON string and converts it into an
     * Object hierarchy for us.
     */
    private String[] getWeatherDataFromJson(String forecastJsonStr, int numDays)
        throws JSONException {

      // These are the names of the JSON objects that need to be extracted.
      final String OWM_LIST = "list";
      final String OWM_WEATHER = "weather";
      final String OWM_TEMPERATURE = "temp";
      final String OWM_MAX = "max";
      final String OWM_MIN = "min";
      final String OWM_DESCRIPTION = "main";

      JSONObject forecastJson = new JSONObject(forecastJsonStr);
      JSONArray weatherArray = forecastJson.getJSONArray(OWM_LIST);

      // OWM returns daily forecasts based upon the local time of the city that is being
      // asked for, which means that we need to know the GMT offset to translate this data
      // properly.

      // Since this data is also sent in-order and the first day is always the
      // current day, we're going to take advantage of that to get a nice
      // normalized UTC date for all of our weather./**/

      Time dayTime = new Time();
      dayTime.setToNow();

      // we start at the day returned by local time. Otherwise this is a mess.
      int julianStartDay = Time.getJulianDay(System.currentTimeMillis(), dayTime.gmtoff);

      // now we work exclusively in UTC
      dayTime = new Time();

      String[] resultStrs = new String[numDays];
      for (int i = 0; i < weatherArray.length(); i++) {
        // For now, using the format "Day, description, hi/low"
        String day;
        String description;
        String highAndLow;

        // Get the JSON object representing the day
        JSONObject dayForecast = weatherArray.getJSONObject(i);

        // The date/time is returned as a long.  We need to convert that
        // into something human-readable, since most people won't read "1400356800" as
        // "this saturday".
        long dateTime;
        // Cheating to convert this to UTC time, which is what we want anyhow
        dateTime = dayTime.setJulianDay(julianStartDay + i);
        day = getReadableDateString(dateTime);

        // description is in a child array called "weather", which is 1 element long.
        JSONObject weatherObject = dayForecast.getJSONArray(OWM_WEATHER).getJSONObject(0);
        description = weatherObject.getString(OWM_DESCRIPTION);

        // Temperatures are in a child object called "temp".  Try not to name variables
        // "temp" when working with temperature.  It confuses everybody.
        JSONObject temperatureObject = dayForecast.getJSONObject(OWM_TEMPERATURE);
        double high = temperatureObject.getDouble(OWM_MAX);
        double low = temperatureObject.getDouble(OWM_MIN);

        highAndLow = formatHighLows(high, low);
        resultStrs[i] = day + " - " + description + " - " + highAndLow;
      }

      for (String s : resultStrs) {
        Log.v(LOG_TAG, "Forecast entry: " + s);
      }
      return resultStrs;

    }
  }
}
