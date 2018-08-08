package com.theshmuz.app.util;

import android.os.SystemClock;
import android.util.Log;

import com.theshmuz.app.D;

import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.params.HttpClientParams;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;
import org.apache.http.params.HttpProtocolParams;
import org.apache.http.util.EntityUtils;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;

import java.io.IOException;
import java.util.List;

/**
 * Created by yossie on 2/19/17.
 */

public class JSONHelpers {

    public static JSONObject getJSONPost(String url, List<NameValuePair> nameValuePairs, boolean checkStatusCode) {
        try {
            HttpResponse rp = doPost(url, nameValuePairs);
            if(!checkStatusCode || rp.getStatusLine().getStatusCode() == HttpStatus.SC_OK) //otherwise, tell the guy to try again (and check internet...)
            {
                String str = EntityUtils.toString(rp.getEntity());
                try {
                    JSONObject fullResp = (JSONObject) new JSONTokener(str).nextValue();
                    return fullResp;
                } catch (JSONException e) {
                    if(D.D) Log.e("JSONHelpers", "JSON Parse Error");
                    return null;
                }
            }
        } catch(IOException e) {
            if(D.D) e.printStackTrace();
            return null;
        } catch(ClassCastException e) {
            if(D.D) Log.e("JSONHelpers", "ClassCastException");
            return null;
        }
        return null;
    }

    public static JSONObject getJSONGet(String url, boolean checkStatusCode) {
        long startTime = 0;
        if(D.D) startTime = SystemClock.elapsedRealtime();
        try {
            HttpResponse rp = doGet(url);
            if(!checkStatusCode || rp.getStatusLine().getStatusCode() == HttpStatus.SC_OK) {
                String str = EntityUtils.toString(rp.getEntity());
                if(D.D) Log.d("JSONHelpers", "Get " + url + " size=" + str.length());
                try {
                    JSONObject fullResp = (JSONObject) new JSONTokener(str).nextValue();
                    if(D.D) Log.d("JSONHelpers", "Get took " + (SystemClock.elapsedRealtime() - startTime));
                    return fullResp;
                } catch (JSONException e) {
                    if(D.D) Log.e("JSONHelpers", "JSON Parse Error");
                    return null;
                }
            }
        } catch(IOException e) {
            if(D.D) Log.e("JSONHelpers", "IOException", e);
            return null;
        } catch(ClassCastException e) {
            if(D.D) Log.e("JSONHelpers", "ClassCastException");
            return null;
        }
        return null;
    }

    private static HttpResponse doPost(String url, List<NameValuePair> nameValuePairs) throws IOException {
        final HttpParams httpParameters = new BasicHttpParams();
        HttpConnectionParams.setConnectionTimeout(httpParameters, D.TIMEOUT_MILLIS);
        HttpConnectionParams.setSoTimeout(httpParameters, D.TIMEOUT_MILLIS);
        HttpClientParams.setRedirecting(httpParameters, false);
        HttpProtocolParams.setUserAgent(httpParameters, D.USER_AGENT);

        final HttpClient hc = new DefaultHttpClient(httpParameters);

        HttpPost post = new HttpPost(url);

        post.setEntity(new UrlEncodedFormEntity(nameValuePairs));

        return hc.execute(post);
    }

    private static HttpResponse doGet(String url) throws IOException {
        final HttpParams httpParameters = new BasicHttpParams();
        HttpConnectionParams.setConnectionTimeout(httpParameters, D.TIMEOUT_MILLIS);
        HttpConnectionParams.setSoTimeout(httpParameters, D.TIMEOUT_MILLIS);
        HttpClientParams.setRedirecting(httpParameters, false);
        HttpProtocolParams.setUserAgent(httpParameters, D.USER_AGENT);

        final HttpClient hc = new DefaultHttpClient(httpParameters);

        HttpGet get = new HttpGet(url);

        return hc.execute(get);
    }

}
