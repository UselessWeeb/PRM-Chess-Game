package vn.edu.fpt.gameproject;

import android.Manifest;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pInfo;
import android.os.Build;
import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import java.util.ArrayList;
import java.util.List;

import vn.edu.fpt.gameproject.fragment.CreateRoomFragment;
import vn.edu.fpt.gameproject.fragment.SettingsFragment;
import vn.edu.fpt.gameproject.manager.WiFiDirectManager;
import vn.edu.fpt.gameproject.manager.WiFiGameManager;
import vn.edu.fpt.gameproject.model.Move;

public class RoomListActivity extends AppCompatActivity implements
        WiFiDirectManager.WiFiDirectListener,
        WiFiGameManager.GameStateListener {

    private WiFiDirectManager wifiDirectManager;
    private WiFiGameManager wifiGameManager;
    private ListView roomListView;
    private Button btnCreateRoom;
    private Button btnReturn;
    private List<WifiP2pDevice> peers = new ArrayList<>();
    private int receivedBoardSize = 8;
    private static final int PERMISSION_REQUEST_CODE = 100;
    private String[] requiredPermissions = {
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.NEARBY_WIFI_DEVICES
    };
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_room_list);

        roomListView = findViewById(R.id.roomListView);
        btnCreateRoom = findViewById(R.id.btnCreateRoom);
        btnReturn = findViewById(R.id.btnReturn);

        roomListView.setOnItemClickListener((parent, view, position, id) -> {
            WifiP2pDevice device = peers.get(position);
            wifiDirectManager.connectToPeer(device);
            Toast.makeText(this, "Connecting to " + device.deviceName, Toast.LENGTH_SHORT).show();
        });

        btnCreateRoom.setOnClickListener(v -> {
            CreateRoomFragment fragment = new CreateRoomFragment();
            FragmentManager fragmentManager = getSupportFragmentManager();
            FragmentTransaction transaction = fragmentManager.beginTransaction();
            transaction.replace(R.id.fragment_container, fragment);
            transaction.addToBackStack(null);
            transaction.commit();
        });

        btnReturn.setOnClickListener(v -> {
            // Return to StartGame activity
            Intent intent = new Intent(this, MainActivity.class);
            startActivity(intent);
            finish();
        });

        checkPermissions();
    }

    private void checkPermissions() {
        if (hasRequiredPermissions()) {
            initializeWiFiDirect();
        } else {
            requestPermissions();
        }
    }

    private boolean hasRequiredPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            return ContextCompat.checkSelfPermission(this, Manifest.permission.NEARBY_WIFI_DEVICES) == PackageManager.PERMISSION_GRANTED
                    && ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED;
        } else {
            return ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED;
        }
    }

    private void requestPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ActivityCompat.requestPermissions(this,
                    new String[]{
                            Manifest.permission.NEARBY_WIFI_DEVICES,
                            Manifest.permission.ACCESS_FINE_LOCATION
                    },
                    PERMISSION_REQUEST_CODE
            );
        } else {
            ActivityCompat.requestPermissions(this,
                    new String[]{ Manifest.permission.ACCESS_FINE_LOCATION },
                    PERMISSION_REQUEST_CODE
            );
        }
    }

    private void showPermissionRationale() {
        String message = (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
                ? "This app needs:\n• Nearby devices permission (for WiFi Direct)\n• Location permission (for device discovery)"
                : "This app needs location permission to discover WiFi Direct devices";

        new AlertDialog.Builder(this)
                .setTitle("Required Permissions")
                .setMessage(message)
                .setPositiveButton("OK", (dialog, which) -> requestPermissions())
                .setNegativeButton("Cancel", null)
                .show();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_REQUEST_CODE) {
            if (hasRequiredPermissions()) {
                initializeWiFiDirect();
            } else {
                Toast.makeText(this, "Permissions denied - WiFi Direct features unavailable", Toast.LENGTH_SHORT).show();
            }
        }
    }


    private void initializeWiFiDirect() {
        if (hasRequiredPermissions()) {
            wifiDirectManager = new WiFiDirectManager(this, this);
            wifiDirectManager.discoverPeers();
        } else {
            Toast.makeText(this, "Cannot initialize WiFi Direct without permissions", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onPeersAvailable(List<WifiP2pDevice> peers) {
        this.peers = peers;
        List<String> peerNames = new ArrayList<>();
        for (WifiP2pDevice device : peers) {
            peerNames.add(device.deviceName);
        }

        runOnUiThread(() -> {
            ArrayAdapter<String> adapter = new ArrayAdapter<>(
                    this,
                    android.R.layout.simple_list_item_1,
                    peerNames
            );
            roomListView.setAdapter(adapter);
        });
    }

    @Override
    public void onConnectionSuccess(WifiP2pInfo info) {
        // For clients (not hosts), initialize WiFiGameManager and wait for board size
        if (!info.isGroupOwner) {
            // Get default settings from SharedPreferences
            boolean fairyPieces = SettingsFragment.getFairyPiecesEnabled(this);
            boolean enPassant = SettingsFragment.getEnPassantEnabled(this);
            boolean promotion = SettingsFragment.getPromotionEnabled(this);
            boolean castling = SettingsFragment.getCastlingEnabled(this);

            wifiGameManager = new WiFiGameManager(this, false,
                    fairyPieces, enPassant, promotion, castling);
            wifiGameManager.startConnection(info.groupOwnerAddress.getHostAddress(), 0);
        }
    }

    @Override
    public void onBoardSizeReceived(int boardSize) {
    }

    @Override
    public void onSettingsReceived(int boardSize, boolean fairyPieces,
                                   boolean enPassant, boolean promotion,
                                   boolean castling, boolean river) {
        receivedBoardSize = boardSize;

        runOnUiThread(() -> {
            Intent intent = new Intent(this, PlayActivity.class);
            intent.putExtra("BOARD_SIZE", boardSize);
            intent.putExtra("GAME_MODE", "wifi");
            intent.putExtra("IS_HOST", false);
            intent.putExtra("FAIRY_PIECES", fairyPieces);
            intent.putExtra("EN_PASSANT", enPassant);
            intent.putExtra("PROMOTION", promotion);
            intent.putExtra("CASTLING", castling);
            intent.putExtra("RIVER", river);
            startActivity(intent);
            finish();
        });
    }

    @Override
    public void onConnectionFailure() {
        Toast.makeText(this, "Connection failed", Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onDisconnected() {
        Toast.makeText(this, "Disconnected", Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onPermissionsMissing() {
        Toast.makeText(this, "WiFi permissions missing", Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onError(String message) {
        Toast.makeText(this, "Error: " + message, Toast.LENGTH_SHORT).show();
    }

    // WiFiGameManager.GameStateListener methods
    @Override
    public void onMoveReceived(Move move) {
        // Not used in this activity
    }

    @Override
    public void onConnectionEstablished() {
        // Connection established, waiting for board size
    }

    @Override
    public void onConnectionLost() {
        Toast.makeText(this, "Game connection lost", Toast.LENGTH_SHORT).show();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (wifiDirectManager != null) {
            wifiDirectManager.cleanup();
        }
        if (wifiGameManager != null) {
            wifiGameManager.disconnect();
        }
    }
}