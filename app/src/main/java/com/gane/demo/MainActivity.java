package com.gane.demo;

import android.graphics.Bitmap;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.webkit.WebView;
import android.widget.FrameLayout;
import android.widget.ImageView;

import com.gane.layercrop.LayerCropView;

public class MainActivity extends AppCompatActivity {

    boolean flag = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {


        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        final WebView webView = (WebView) findViewById(R.id.web_view);
        webView.loadUrl("http://www.jianshu.com");

//        final LayerCropView layerCrop = (LayerCropView) findViewById(R.id.layer_crop);
        final LayerCropView layerCrop = new LayerCropView(this);


        final ImageView image = (ImageView) findViewById(R.id.image);
        final FrameLayout layout = (FrameLayout) findViewById(R.id.layout);

        findViewById(R.id.button).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (flag) {
//                    layout.addView(layerCrop);
                    flag = !flag;
                    layerCrop.attach(webView);
                } else {

                    FrameLayout.LayoutParams lp = (FrameLayout.LayoutParams) webView.getLayoutParams();
                    Bitmap bitmap = layerCrop.crop(lp.leftMargin, lp.topMargin, lp.rightMargin, lp.bottomMargin);
                image.setImageBitmap(bitmap);
                image.setVisibility(View.VISIBLE);
                layout.setVisibility(View.GONE);
                }

            }
        });

    }
}
