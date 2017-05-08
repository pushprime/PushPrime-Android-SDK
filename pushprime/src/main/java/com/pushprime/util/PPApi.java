package com.pushprime.util;

import android.content.Context;
import android.os.AsyncTask;
import android.os.Build;
import android.telecom.Call;
import android.util.Pair;

import com.pushprime.PushPrime;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;

import javax.net.ssl.HttpsURLConnection;


/**
 * Created by PushPrime on 10/7/2016.
 */

/**
 * Communicates with PushPrime API, this class was specifically written to work with PushPrime API and should not be used in any other case.
 */
public class PPApi extends AsyncTask<Void, Integer, PPApi.Result> {

    public static final String BASE_URL = "https://pushprime.com/api";

    public String endPoint = "";

    public String method = "GET";

    public List<Pair<String, String>> params = new ArrayList<>();

    public Callback callback;

    public static PPApi Builder(){
        return new PPApi();
    }

    public PPApi setEndPoint(String endPoint) {
        this.endPoint = endPoint;
        return this;
    }

    public PPApi setMethod(String method) {
        this.method = method;
        return this;
    }

    public PPApi setParameter(String key, String value){
        this.params.add(new Pair<>(key, value));
        return this;
    }

    public PPApi setCallback(Callback callback){
        this.callback = callback;
        return this;
    }

    public void send(){
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB)
            this.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
        else
            this.execute();
    }


    @Override
    protected Result doInBackground(Void... params) {
        Result result = new Result();
        try {
            String endPointUrl = BASE_URL + endPoint;

            URL url = new URL(endPointUrl);
            HttpsURLConnection con = (HttpsURLConnection) url.openConnection();

            PPLog.print("\nSending request to URL : " + url);

            con.setRequestProperty("User-Agent", "PushPrime-Android-SDK");
            con.setRequestProperty("token", PushPrime.sharedHandler().pushPrimeApiKey);

            if(this.params.size() > 0){
                con.setRequestMethod("POST");
                String glue = "";
                String urlParameters = "";
                for(int i = 0; i < this.params.size(); i++){
                    Pair<String, String> param = this.params.get(i);
                    urlParameters += glue + URLEncoder.encode(param.first, "UTF-8") + "="+ URLEncoder.encode(param.second, "UTF-8");
                    glue = "&";
                }
                con.setDoOutput(true);// Should be part of code only for .Net web-services else no need for PHP
                DataOutputStream wr = new DataOutputStream(con.getOutputStream());
                wr.writeBytes(urlParameters);
                wr.flush();
                wr.close();
                PPLog.print("Post parameters : " + urlParameters);
            }

            result.statusCode = con.getResponseCode();
            PPLog.print("Response Code : " + result.statusCode);

            BufferedReader in = new BufferedReader(
                    new InputStreamReader(con.getInputStream()));
            String inputLine;
            StringBuffer response = new StringBuffer();

            while ((inputLine = in.readLine()) != null) {
                response.append(inputLine);
            }

            in.close();
            String responseString = response.toString();
            result.response = new JSONObject(responseString);
            PPLog.print("Response is " + responseString);
        } catch (MalformedURLException exception){
            result.exception = exception;
        } catch (IOException exception){
            result.exception = exception;
        } catch (JSONException exception){
            result.exception = exception;
        }
        return result;
    }

    @Override
    protected void onPostExecute(Result result) {
        super.onPostExecute(result);
        if(this.callback != null){
            if(result.exception == null){
                this.callback.onSuccess(result.statusCode, result.response);
            } else {
                this.callback.onError(result.statusCode, result.exception);
            }
        }
    }

    public interface Callback{

        void onSuccess(int statusCode, JSONObject content);

        void onError(int statusCode, Exception exception);
    }

    class Result{

        int statusCode = 0;

        JSONObject response;

        Exception exception;
    }
}
