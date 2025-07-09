package vn.edu.fpt.gameproject.manager;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.net.wifi.p2p.WifiP2pConfig;
import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pDeviceList;
import android.net.wifi.p2p.WifiP2pInfo;
import android.net.wifi.p2p.WifiP2pManager;
import android.os.Build;
import android.util.Log;

import androidx.core.content.ContextCompat;

import java.util.ArrayList;
import java.util.List;

public class WiFiDirectManager {
    private static final String TAG = "WiFiDirectManager";
    private final WifiP2pManager manager;
    private final WifiP2pManager.Channel channel;
    private final Context context;
    private final IntentFilter intentFilter;
    private BroadcastReceiver receiver;
    private WiFiDirectListener listener;
    private List<WifiP2pDevice> peers = new ArrayList<>();

    public interface WiFiDirectListener {
        void onPeersAvailable(List<WifiP2pDevice> peers);
        void onConnectionSuccess(WifiP2pInfo info);
        void onConnectionFailure();
        void onDisconnected();
        void onPermissionsMissing();
        void onError(String message);
    }

    public WiFiDirectManager(Context context, WiFiDirectListener listener) {
        this.context = context;
        this.listener = listener;
        this.manager = (WifiP2pManager) context.getSystemService(Context.WIFI_P2P_SERVICE);
        this.channel = manager.initialize(context, context.getMainLooper(), null);

        intentFilter = new IntentFilter();
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION);
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION);
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION);
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION);

        receiver = new WiFiDirectBroadcastReceiver();
        context.registerReceiver(receiver, intentFilter);
    }

    private boolean checkPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            return ContextCompat.checkSelfPermission(context, Manifest.permission.NEARBY_WIFI_DEVICES) == PackageManager.PERMISSION_GRANTED &&
                    ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED;
        } else {
            return ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED;
        }
    }

    public void startHosting() {
        if (!checkPermissions()) {
            listener.onPermissionsMissing();
            return;
        }

        try {
            manager.createGroup(channel, new WifiP2pManager.ActionListener() {
                @Override
                public void onSuccess() {
                    Log.d(TAG, "WiFi Direct group created successfully");
                }

                @Override
                public void onFailure(int reason) {
                    String error = "Failed to create WiFi Direct group: " + reason;
                    Log.e(TAG, error);
                    listener.onError(error);
                }
            });
        } catch (SecurityException e) {
            Log.e(TAG, "SecurityException in createGroup", e);
            listener.onPermissionsMissing();
        }
    }

    public void discoverPeers() {
        if (!checkPermissions()) {
            listener.onPermissionsMissing();
            return;
        }

        try {
            manager.discoverPeers(channel, new WifiP2pManager.ActionListener() {
                @Override
                public void onSuccess() {
                    Log.d(TAG, "Discovery started");
                }

                @Override
                public void onFailure(int reason) {
                    String error = "Discovery failed: " + reason;
                    Log.e(TAG, error);
                    listener.onError(error);
                }
            });
        } catch (SecurityException e) {
            Log.e(TAG, "SecurityException in discoverPeers", e);
            listener.onPermissionsMissing();
        }
    }

    public void connectToPeer(WifiP2pDevice device) {
        if (!checkPermissions()) {
            listener.onPermissionsMissing();
            return;
        }

        WifiP2pConfig config = new WifiP2pConfig();
        config.deviceAddress = device.deviceAddress;

        try {
            manager.connect(channel, config, new WifiP2pManager.ActionListener() {
                @Override
                public void onSuccess() {
                    Log.d(TAG, "Connection initiated");
                }

                @Override
                public void onFailure(int reason) {
                    String error = "Connection failed: " + reason;
                    Log.e(TAG, error);
                    listener.onConnectionFailure();
                }
            });
        } catch (SecurityException e) {
            Log.e(TAG, "SecurityException in connectToPeer", e);
            listener.onPermissionsMissing();
        }
    }

    public void disconnect() {
        try {
            manager.removeGroup(channel, new WifiP2pManager.ActionListener() {
                @Override
                public void onSuccess() {
                    Log.d(TAG, "Disconnected");
                    listener.onDisconnected();
                }

                @Override
                public void onFailure(int reason) {
                    String error = "Disconnect failed: " + reason;
                    Log.e(TAG, error);
                    listener.onError(error);
                }
            });
        } catch (SecurityException e) {
            Log.e(TAG, "SecurityException in disconnect", e);
            listener.onPermissionsMissing();
        }
    }

    private class WiFiDirectBroadcastReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();

            if (WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION.equals(action)) {
                if (!checkPermissions()) {
                    listener.onPermissionsMissing();
                    return;
                }

                try {
                    manager.requestPeers(channel, peersList -> {
                        peers.clear();
                        peers.addAll(peersList.getDeviceList());
                        listener.onPeersAvailable(new ArrayList<>(peers));
                    });
                } catch (SecurityException e) {
                    Log.e(TAG, "SecurityException in requestPeers", e);
                    listener.onPermissionsMissing();
                }
            }
            else if (WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION.equals(action)) {
                try {
                    manager.requestConnectionInfo(channel, info -> {
                        if (info.groupFormed) {
                            listener.onConnectionSuccess(info);
                        }
                    });
                } catch (SecurityException e) {
                    Log.e(TAG, "SecurityException in requestConnectionInfo", e);
                    listener.onPermissionsMissing();
                }
            }
        }
    }

    public void cleanup() {
        try {
            if (receiver != null) {
                context.unregisterReceiver(receiver);
                receiver = null;
            }
        } catch (Exception e) {
            Log.e(TAG, "Error during cleanup", e);
        }
    }
}