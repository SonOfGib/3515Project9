package edu.temple.stockviewer;

import android.app.ListActivity;
import android.app.ProgressDialog;
import android.app.SearchManager;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.ref.WeakReference;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

public class SearchStocksActivity extends ListActivity {
    ListView list;
    ArrayList<StockSearchResult> results = new ArrayList<>();
    ProgressDialog pd;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_search_stocks);
        Log.d("SEARCH", "HERE");
        list = getListView();
        list.setAdapter(new StockSearchArrayAdapter(this, results));
        handleIntent(getIntent());
    }
    public void onNewIntent(Intent intent) {
        setIntent(intent);
        handleIntent(intent);
    }

    public void onListItemClick(ListView l, View v, int position, long id) {
        TextView stockSymbolView = v.getRootView().findViewById(R.id.stockSymbol);
        String stockSymbol = stockSymbolView.getText().toString();
        //creates a directory for this stock if we don't already have it.
        getDir(stockSymbol, Context.MODE_PRIVATE);
        Intent intent = new Intent();
        //notify that we have addded a new stock.
        intent.setAction("edu.temple.stockviewer.STOCK_ADD");
        intent.putExtra("symbol",stockSymbol);
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
        //take us back to the main view.
        onBackPressed();
    }

    private void handleIntent(Intent intent) {
        if (Intent.ACTION_SEARCH.equals(intent.getAction())) {
            String query = intent.getStringExtra(SearchManager.QUERY);
            doSearch(query);
        }
    }

    private void doSearch(String queryStr) {
        JsonTask task =  new JsonTask();
        task.execute(queryStr);

    }

    private class JsonTask extends AsyncTask<String, String, String> {

        protected void onPreExecute() {
            super.onPreExecute();

            pd = new ProgressDialog(SearchStocksActivity.this);
            pd.setMessage("Please wait");
            pd.setCancelable(false);
            pd.show();
        }

        protected String doInBackground(String... params) {
            HttpURLConnection connection = null;
            BufferedReader reader = null;
            try {
                URL url = new URL("http://dev.markitondemand.com/MODApis/Api/v2/Lookup/json?input="+params[0]);
                connection = (HttpURLConnection) url.openConnection();
                connection.connect();
                InputStream stream = connection.getInputStream();
                reader = new BufferedReader(new InputStreamReader(stream));
                StringBuilder buffer = new StringBuilder();
                String line = "";
                while ((line = reader.readLine()) != null) {
                    buffer.append(line).append("\n");
                    Log.d("Response: ", "> " + line);   //here u ll get whole response...... :-)

                }
                return buffer.toString();
            } catch (MalformedURLException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                if (connection != null) {
                    connection.disconnect();
                }
                try {
                    if (reader != null) {
                        reader.close();
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            return null;
        }
        @Override
        protected void onPostExecute(String result) {
            super.onPostExecute(result);
            if (pd.isShowing()){
                pd.dismiss();
            }
            //get the information out of our
            try {
                JSONArray stocks = new JSONArray(result);
                results = new ArrayList<StockSearchResult>();
                for (int i=0; i < stocks.length(); i++)
                {
                    JSONObject stock = stocks.getJSONObject(i);
                    // Pulling items from the array
                    String stockSymbol = stock.getString("Symbol");
                    String stockName = stock.getString("Name");
                    String stockExchange = stock.getString("Exchange");
                    results.add(new StockSearchResult(stockSymbol, stockName, stockExchange));
                }
                list.setAdapter(new StockSearchArrayAdapter(
                        SearchStocksActivity.this, results));
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
    }
}