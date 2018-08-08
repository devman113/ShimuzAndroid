package com.theshmuz.app.activity;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.view.Menu;
import android.view.MenuItem;
import android.webkit.WebView;

import com.theshmuz.app.R;

/**
 * Created by yossie on 5/29/17.
 */

public class InternalLink extends AppCompatActivity {

    private WebView webView;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        webView = new WebView(this);

        webView.loadUrl("https://store.theshmuz.com/raffle-splash.html");

        setContentView(webView);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.internal_link, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menuOpenBrowser:
                String webUrl = webView.getUrl();
                final Intent openIntent = new Intent(Intent.ACTION_VIEW);
                openIntent.setData(Uri.parse(webUrl));
                startActivity(Intent.createChooser(openIntent, "Open in Browser"));
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

}
