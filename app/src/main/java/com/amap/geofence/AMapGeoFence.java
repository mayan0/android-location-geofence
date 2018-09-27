package com.amap.geofence;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.text.TextUtils;
import android.util.Log;

import com.amap.api.fence.GeoFence;
import com.amap.api.fence.GeoFenceClient;
import com.amap.api.fence.GeoFenceListener;
import com.amap.api.location.DPoint;
import com.amap.api.maps.AMap;
import com.amap.api.maps.CameraUpdateFactory;
import com.amap.api.maps.model.Circle;
import com.amap.api.maps.model.CircleOptions;
import com.amap.api.maps.model.LatLng;
import com.amap.api.maps.model.MarkerOptions;
import com.amap.api.maps.model.Polygon;
import com.amap.api.maps.model.PolygonOptions;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Created by ligen on 16/12/15.
 */
public class AMapGeoFence implements GeoFenceListener {
    private String TAG = "AMapGeoFence";
    private GeoFenceClient mClientInAndStayAction;
    private GeoFenceClient mClientAllAction;
    private Context mContext;
    private AMap mAMap;
    private ConcurrentMap mCustomEntitys;
    private Handler mHandler;
    private int mCustomID = 100;
    // 记录已经添加成功的围栏
    private volatile ConcurrentMap<String, GeoFence> fenceMap = new ConcurrentHashMap<String, GeoFence>();
    // 地理围栏的广播action
    static final String GEOFENCE_BROADCAST_ACTION = "com.amap.geofence";

    private ExecutorService mThreadPool;

    private static final String mPolygonFenceString6 = "116.327144,39.932266;116.333667,39.932299;116.333624,39.929666;116.328861,39.929699;116.328818,39.930324;116.327343,39.930349;116.327428,39.932192";




    public AMapGeoFence(Context context, AMap amap, Handler handler) {
        mContext = context;
        mHandler = handler;
        mThreadPool = Executors.newCachedThreadPool();
        mCustomEntitys = new ConcurrentHashMap<String, Object>();
        mAMap = amap;
        IntentFilter fliter = new IntentFilter(
                ConnectivityManager.CONNECTIVITY_ACTION);
        fliter.addAction(GEOFENCE_BROADCAST_ACTION);
        mContext.registerReceiver(mGeoFenceReceiver, fliter);
//        addFenceInAndStay();
        addFenceAll();
        mClientAllAction.addGeoFence("海淀区", "海淀");
        mClientAllAction.addGeoFence("西城区","西城");
    }

    private void addFenceInAndStay() {
        mClientInAndStayAction = new GeoFenceClient(mContext);
        mClientInAndStayAction.createPendingIntent(GEOFENCE_BROADCAST_ACTION);
        mClientInAndStayAction.setGeoFenceListener(this);
        mClientInAndStayAction.setActivateAction(GeoFenceClient.GEOFENCE_IN | GeoFenceClient.GEOFENCE_STAYED | GeoFenceClient.GEOFENCE_OUT);

//        mClientInAndStayAction.addGeoFence("麦当劳", "快餐厅", "北京", 2, String.valueOf(mCustomID));
        mCustomID++;
//        mClientInAndStayAction.addGeoFence("kfc", "快餐厅", new DPoint(39.982375,116.305292), 5000, 2, String.valueOf(mCustomID));
//        mCustomID++;
//        mClientInAndStayAction.addGeoFence("西城区", String.valueOf(mCustomID));
//        mCustomID++;
    }

    private void addFenceAll() {
        mClientAllAction = new GeoFenceClient(mContext);
        mClientAllAction.createPendingIntent(GEOFENCE_BROADCAST_ACTION);
        mClientAllAction.setGeoFenceListener(this);
        mClientAllAction.setActivateAction(GeoFenceClient.GEOFENCE_IN | GeoFenceClient.GEOFENCE_STAYED | GeoFenceClient.GEOFENCE_OUT);
        addPolygonGeoFence(mPolygonFenceString6,"住建部");

    }

    private void addPolygonGeoFence(String points,String customid) {
        String cid;
        if (TextUtils.isEmpty(customid)){
            cid = String.valueOf(mCustomID);
            mCustomID++;
        }else {
            cid = customid;
        }
        mClientAllAction.addGeoFence(Util.toAMapGeoFenceList(points), cid);
    }

    private void addCircleGeoFence(DPoint dPoint) {
        mClientAllAction.addGeoFence(dPoint, 1000, String.valueOf(mCustomID));
        mCustomID++;
    }

    private void drawPolygon(GeoFence fence) {
        Log.i(TAG, "drawPolygon-----------------------------------------------------3");
        final List<List<DPoint>> pointList = fence.getPointList();
        if (null == pointList || pointList.isEmpty()) {
            return;
        }
        List<Polygon> polygonList = new ArrayList<Polygon>();
        for (List<DPoint> subList : pointList) {
            if (subList == null) {
                continue;
            }
            List<LatLng> lst = new ArrayList<LatLng>();

            PolygonOptions polygonOption = new PolygonOptions();
            for (DPoint point : subList) {
                lst.add(new LatLng(point.getLatitude(), point.getLongitude()));
//                boundsBuilder.include(
//                        new LatLng(point.getLatitude(), point.getLongitude()));
            }
            polygonOption.addAll(lst);

            polygonOption.fillColor(mContext.getResources().getColor(R.color.fill));
            polygonOption.strokeColor(mContext.getResources().getColor(R.color.stroke));
            polygonOption.strokeWidth(4);
            Polygon polygon = mAMap.addPolygon(polygonOption);
            boolean s = ispointin( new LatLng(39.930975,116.329724),polygon);
            Log.i("MY",String.valueOf(s));
            polygonList.add(polygon);
            mCustomEntitys.put(fence.getFenceId(), polygonList);
        }
    }

    private void drawCircle(GeoFence fence) {
        CircleOptions option = new CircleOptions();
        option.fillColor(mContext.getResources().getColor(R.color.fill));
        option.strokeColor(mContext.getResources().getColor(R.color.stroke));
        option.strokeWidth(4);
        option.radius(fence.getRadius());
        DPoint dPoint = fence.getCenter();
        option.center(new LatLng(dPoint.getLatitude(), dPoint.getLongitude()));
        Circle circle = mAMap.addCircle(option);
        mCustomEntitys.put(fence.getFenceId(), circle);
    }

    public void drawFenceToMap() {
        Iterator iter = fenceMap.entrySet().iterator();
        Log.i(TAG, "drawFenceToMap添加围栏个数:" + fenceMap.size());
        while (iter.hasNext()) {
            Log.i(TAG, "-----------------------------------------------------1");
            Map.Entry entry = (Map.Entry) iter.next();
            String key = (String) entry.getKey();
            GeoFence val = (GeoFence) entry.getValue();
            if (!mCustomEntitys.containsKey(key)) {
                Log.i(TAG, "-----------------------------------------------------2");
                Log.i(TAG, "添加围栏:" + key);
                drawFence(val);
            }
        }
    }

    private void drawFence(GeoFence fence) {
        switch (fence.getType()) {
            case GeoFence.TYPE_ROUND:
            case GeoFence.TYPE_AMAPPOI:
                drawCircle(fence);
                break;
            case GeoFence.TYPE_POLYGON:
            case GeoFence.TYPE_DISTRICT:
                drawPolygon(fence);
                break;
            default:
                break;
        }

    }

    public void removeAll() {
        try {
            mClientInAndStayAction.removeGeoFence();
            mClientAllAction.removeGeoFence();
            mContext.unregisterReceiver(mGeoFenceReceiver);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static Object lock = new Object();

    @Override
    public void onGeoFenceCreateFinished(List<GeoFence> geoFenceList, int errorCode, String s) {
        if (errorCode == GeoFence.ADDGEOFENCE_SUCCESS) {
            for (GeoFence fence : geoFenceList) {
                Log.e(TAG, "fenid:" + fence.getFenceId() + " customID:" + s + " " + fenceMap.containsKey(fence.getFenceId()));
                fenceMap.putIfAbsent(fence.getFenceId(), fence);
                Log.e(TAG, "fenid:" + fence.getFenceId() + " customID:" + s + " " + fenceMap.containsKey(fence.getFenceId()));
            }
            Log.e(TAG, "回调添加成功个数:" + geoFenceList.size());
            Log.e(TAG, "回调添加围栏个数:" + fenceMap.size());
            Message message = mHandler.obtainMessage();
            message.obj = geoFenceList;
            message.what = 0;
            mHandler.sendMessage(message);
            Log.e(TAG, "添加围栏成功！！");
        } else {
            Log.e(TAG, "添加围栏失败！！！！ errorCode: " + errorCode);
            Message msg = Message.obtain();
            msg.arg1 = errorCode;
            msg.what = 1;
            mHandler.sendMessage(msg);
        }
    }


    private BroadcastReceiver mGeoFenceReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            // 接收广播
            if (intent.getAction().equals(GEOFENCE_BROADCAST_ACTION)) {
                Bundle bundle = intent.getExtras();
                String fenceID = bundle
                        .getString(GeoFence.BUNDLE_KEY_FENCEID);
                int status = bundle.getInt(GeoFence.BUNDLE_KEY_FENCESTATUS);
                int code = bundle.getInt(GeoFence.BUNDLE_KEY_LOCERRORCODE);
                String customid = bundle.getString(GeoFence.BUNDLE_KEY_CUSTOMID);
                GeoFence geoFence = bundle.getParcelable(GeoFence.BUNDLE_KEY_FENCE);
                Log.e(TAG, "fenid------onReceive--------:" + geoFence.getFenceId() + " customID:" + geoFence.getCustomId() + "-------------------------------------- " );
                Log.e(TAG, "定位失败"+code);
                StringBuffer sb = new StringBuffer();
                switch (status) {
                    case GeoFence.STATUS_LOCFAIL:
                        sb.append("定位失败");
                        Log.e(TAG, "定位失败"+code);
                        break;
                    case GeoFence.STATUS_IN:
                        sb.append("进入围栏 ").append("---"+customid);
                        Log.e(TAG, "进入围栏"+"---"+customid);
                        break;
                    case GeoFence.STATUS_OUT:
                        sb.append("离开围栏 ").append("---"+customid);
                        Log.e(TAG, "离开围栏"+"---"+customid);
                        break;
                    case GeoFence.STATUS_STAYED:
                        sb.append("停留在围栏内 ").append(fenceID);
                        break;
                    default:
                        break;
                }
                String str = sb.toString();
                Message msg = Message.obtain();
                msg.obj = str;
                msg.what = 2;
                mHandler.sendMessage(msg);
            }
        }
    };


    public boolean ispointin(LatLng point, Polygon polygon){
        polygon.contains(point);


        return polygon.contains(point);
    }

}
