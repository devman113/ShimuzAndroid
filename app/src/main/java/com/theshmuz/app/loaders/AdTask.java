package com.theshmuz.app.loaders;

import java.io.IOException;

import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
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

import com.theshmuz.app.D;
import com.theshmuz.app.Shmuz;
import com.theshmuz.app.business.Adver;

import android.os.AsyncTask;
import android.util.Log;

public class AdTask extends AsyncTask<Void, Void, LoadAdResulter> {

    public static final String AD_CMD = "com.theshmuz.app.ads";

    public Shmuz context;

    public AdTask(Shmuz app) {
        this.context = app;
    }

    @Override
    protected LoadAdResulter doInBackground(Void... params) {

        final HttpParams httpParameters = new BasicHttpParams();
        HttpConnectionParams.setConnectionTimeout(httpParameters, D.TIMEOUT_MILLIS);
        HttpConnectionParams.setSoTimeout(httpParameters, D.TIMEOUT_MILLIS);
        HttpClientParams.setRedirecting(httpParameters, false);
        HttpProtocolParams.setUserAgent(httpParameters, D.USER_AGENT);

        final HttpClient hc = new DefaultHttpClient(httpParameters);

        Adver result = new Adver();

        try {
            String url = D.BACKEND + "/ads";
            HttpGet get = new HttpGet(url);
            HttpResponse rp = hc.execute(get);
            if(rp.getStatusLine().getStatusCode() == HttpStatus.SC_OK) {
                String str = EntityUtils.toString(rp.getEntity());
                try {
                    LoadAdResulter resulter = new LoadAdResulter();

                    JSONObject fullResp = (JSONObject) new JSONTokener(str).nextValue();

                    JSONObject popupObj = (JSONObject) fullResp.get("popup");

                    result.popupType = popupObj.getInt("type");
                    result.popupId = popupObj.isNull("id") ? null : popupObj.getString("id");
                    if(result.popupType == Adver.POPUP_TYPE_WEBURL) {
                        result.popupData1 = popupObj.getString("url");
                    }
                    else if(result.popupType == Adver.POPUP_TYPE_HTML) {
                        result.popupData1 = popupObj.getString("html");
                    }
                    else if(result.popupType == Adver.POPUP_TYPE_PLAIN1) {
                        result.popupData1 = popupObj.getString("url");
                        result.popupData2 = popupObj.getString("title");
                        result.popupData3 = popupObj.getString("text");
                    }
                    resulter.adver = result;

                    JSONObject prerollObj = (JSONObject) fullResp.get("preroll");

                    if(fullResp.has("forceVer")) {
                        resulter.forceVersion = fullResp.getInt("forceVer");
                    }

                    return resulter;


                } catch (JSONException e) {
                    if(D.D) Log.e("UpdateService", "JSON Parse Error");
                    return null;
                }
            }
            else {
                return null;
            }

        } catch(IOException e){
            if(D.D) e.printStackTrace();
            return null;
        } catch(ClassCastException e){
            if(D.D) Log.e("UpdateService", "ClassCastException");
            return null;
        }
    }

    @Override
    protected void onPostExecute(LoadAdResulter resulter) {
        if(context == null) return;

        if(resulter == null) {
            if(D.D) Log.e("AdTask onPostExecute", "Got null newAd");
            Adver newAd = new Adver();
            context.gotNewAdver(newAd);
        }
        else {
            context.gotNewAdver(resulter.adver);
            if(resulter.forceVersion > 0) context.gotNewForceVersion(resulter.forceVersion);
        }

        context.adTask = null;
    }

}
