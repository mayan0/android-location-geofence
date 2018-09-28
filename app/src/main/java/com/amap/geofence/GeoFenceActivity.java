package com.amap.geofence;

import android.app.Activity;
import android.graphics.Color;
import android.os.Handler;
import android.os.Message;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.amap.api.location.AMapLocation;
import com.amap.api.location.AMapLocationClient;
import com.amap.api.location.AMapLocationClientOption;
import com.amap.api.location.AMapLocationListener;
import com.amap.api.maps.AMap;
import com.amap.api.maps.CameraUpdateFactory;
import com.amap.api.maps.LocationSource;
import com.amap.api.maps.MapView;
import com.amap.api.maps.UiSettings;
import com.amap.api.maps.model.BitmapDescriptorFactory;
import com.amap.api.maps.model.MyLocationStyle;
import com.amap.geofence.presenter.AMapGeoFence;

import java.lang.ref.WeakReference;
import java.util.List;

public class GeoFenceActivity extends Activity implements LocationSource, AMapLocationListener, View.OnClickListener {

    private AMap mAMap;
    private MapView mapView;
    private AMapGeoFence mAMapGeoFence;
    private TextView mtv;
    /**
     * 用于显示当前的位置
     * <p>
     * 示例中是为了显示当前的位置，在实际使用中，单独的地理围栏可以不使用定位接口
     * </p>
     */
    private AMapLocationClient mlocationClient;
    private OnLocationChangedListener mListener;
    private AMapLocationClientOption mLocationOption;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mapView = (MapView) findViewById(R.id.map);
        mapView.onCreate(savedInstanceState);// 此方法必须重写
        setUpMapIfNeeded();
        mAMapGeoFence = new AMapGeoFence(this.getApplicationContext(), mAMap, handler);
    }


    /**
     * 方法必须重写
     */
    @Override
    protected void onResume() {
        super.onResume();
        mapView.onResume();
        setUpMapIfNeeded();
    }

    /**
     * 方法必须重写
     */
    @Override
    protected void onPause() {
        super.onPause();
        mapView.onPause();
    }

    /**
     * 方法必须重写
     */
    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        mapView.onSaveInstanceState(outState);
    }

    /**
     * 方法必须重写
     */
    @Override
    protected void onDestroy() {
        super.onDestroy();
        mapView.onDestroy();
        mAMapGeoFence.removeAll();
    }

    private void setUpMapIfNeeded() {
        if (mAMap == null) {
            mAMap =mapView.getMap();
            UiSettings uiSettings = mAMap.getUiSettings();
            if (uiSettings != null) {
                uiSettings.setRotateGesturesEnabled(false);
                uiSettings.setMyLocationButtonEnabled(true); // 设置默认定位按钮是否显示
            }
            mAMap.setLocationSource(this);// 设置定位监听
            mAMap.setMyLocationStyle(
                    new MyLocationStyle().radiusFillColor(Color.argb(0, 0, 0, 0))
                            .strokeColor(Color.argb(0, 0, 0, 0)).myLocationIcon(BitmapDescriptorFactory.fromResource(R.mipmap.navi_map_gps_locked)));
            mAMap.setMyLocationEnabled(true);// 设置为true表示显示定位层并可触发定位，false表示隐藏定位层并不可触发定位，默认是false
            // 设置定位的类型为定位模式 ，可以由定位、跟随或地图根据面向方向旋转几种
            mAMap.setMyLocationType(AMap.LOCATION_TYPE_LOCATE);
            mAMap.moveCamera(CameraUpdateFactory.zoomTo(15));
        }
        findViewById(R.id.check).setOnClickListener(this);
    }

    Handler handler = new MyHandler(this);

    @Override
    public void onClick(View v) {
        switch (v.getId()){
            case R.id.check:
               List<String> result = mAMapGeoFence.checkLocationinfence();
               Toast.makeText(GeoFenceActivity.this,"当前位置在围栏："+result.toString(),Toast.LENGTH_LONG).show();
        }
    }


    private static class MyHandler extends Handler{
        WeakReference<GeoFenceActivity> mMainActivityWeakReference;

        public MyHandler(GeoFenceActivity mainActivity) {
            mMainActivityWeakReference = new WeakReference<>(mainActivity);
        }

        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            GeoFenceActivity mainActivity = mMainActivityWeakReference.get();
            switch (msg.what) {
                case 0:
//                    Toast.makeText(getApplicationContext(), "添加围栏成功",
//                            Toast.LENGTH_SHORT).show();
                    mainActivity.mAMapGeoFence.drawFenceToMap();
                    break;
                case 1:
                    int errorCode = msg.arg1;
                    Toast.makeText(mainActivity.getApplicationContext(),
                            "添加围栏失败 " + errorCode, Toast.LENGTH_SHORT).show();
                    break;
                case 2:
                    String statusStr = (String) msg.obj;
                    Toast.makeText(mainActivity.getApplicationContext(), statusStr,
                            Toast.LENGTH_SHORT).show();
                    break;
                default:
                    break;
            }
        }
    }

    @Override
    public void onLocationChanged(AMapLocation amapLocation) {
        if (mListener != null && amapLocation != null) {
            if (amapLocation != null && amapLocation.getErrorCode() == 0) {
                mListener.onLocationChanged(amapLocation);// 显示系统小蓝点
                mAMapGeoFence.setMyLocation(amapLocation);

            } else {
                String errText = "定位失败," + amapLocation.getErrorCode() + ": "
                        + amapLocation.getErrorInfo();
                Log.e("AmapErr", errText);

            }
        }/**/
    }

    @Override
    public void activate(OnLocationChangedListener onLocationChangedListener) {
        mListener = onLocationChangedListener;
        if (mlocationClient == null) {
            mlocationClient = new AMapLocationClient(this);
            mLocationOption = new AMapLocationClientOption();
            // 设置定位监听
            mlocationClient.setLocationListener(this);
            // 设置为高精度定位模式
            mLocationOption.setLocationMode(AMapLocationClientOption.AMapLocationMode.Hight_Accuracy);

            // 设置定位参数
            mlocationClient.setLocationOption(mLocationOption);
            mlocationClient.startLocation();
        }
    }

    @Override
    public void deactivate() {
        mListener = null;
        if (mlocationClient != null) {
            mlocationClient.stopLocation();
            mlocationClient.onDestroy();
        }
        mlocationClient = null;
    }


}
