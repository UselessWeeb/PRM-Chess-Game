package vn.edu.fpt.gameproject.fragment;

import android.content.Intent;
import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pInfo;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.SeekBar;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import androidx.fragment.app.Fragment;

import java.util.List;

import vn.edu.fpt.gameproject.PlayActivity;
import vn.edu.fpt.gameproject.R;
import vn.edu.fpt.gameproject.manager.WiFiDirectManager;
import vn.edu.fpt.gameproject.manager.WiFiGameManager;
import vn.edu.fpt.gameproject.model.Move;

public class CreateRoomFragment extends Fragment implements
        WiFiDirectManager.WiFiDirectListener,
        WiFiGameManager.GameStateListener {

    private WiFiDirectManager wifiDirectManager;
    private WiFiGameManager wifiGameManager;
    private EditText roomNameInput;
    private Button createRoomBtn;
    private boolean isHost = false;
    private boolean waitingForPlayer = false;

    private SeekBar sliderBoardSize;
    private TextView boardSizeValue;
    private TextView statusText;
    private Switch switchFairyPieces;
    private Switch switchEnPassant;
    private Switch switchPromotion;
    private Switch switchCastling;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_create_room, container, false);

        roomNameInput = view.findViewById(R.id.roomNameInput);
        createRoomBtn = view.findViewById(R.id.createRoomBtn);
        statusText = view.findViewById(R.id.statusText);
        sliderBoardSize = view.findViewById(R.id.sliderBoardSize);
        boardSizeValue = view.findViewById(R.id.boardSizeValue);
        switchFairyPieces = view.findViewById(R.id.switch_fairy_pieces);
        switchEnPassant = view.findViewById(R.id.switch_en_passant);
        switchPromotion = view.findViewById(R.id.switch_promotion);
        switchCastling = view.findViewById(R.id.switch_castling);

        wifiDirectManager = new WiFiDirectManager(requireContext(), this);

        sliderBoardSize.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                int boardSize = Math.max(6, progress); // Minimum is 6
                boardSizeValue.setText(boardSize + "x" + boardSize);
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {}

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {}
        });

        createRoomBtn.setOnClickListener(v -> {
            if (!waitingForPlayer) {
                String roomName = roomNameInput.getText().toString().trim();
                if (!roomName.isEmpty()) {
                    isHost = true;
                    waitingForPlayer = true;
                    wifiDirectManager.startHosting();
                    statusText.setText("Waiting for player to join...");
                    createRoomBtn.setText("Cancel");
                    roomNameInput.setEnabled(false);
                    sliderBoardSize.setEnabled(false); // Disable board size changes
                    Toast.makeText(requireContext(), "Room created: " + roomName, Toast.LENGTH_SHORT).show();
                }
            } else {
                // Cancel waiting
                waitingForPlayer = false;
                statusText.setText("");
                createRoomBtn.setText("Create Room");
                roomNameInput.setEnabled(true);
                sliderBoardSize.setEnabled(true);
                wifiDirectManager.disconnect();
                if (wifiGameManager != null) {
                    wifiGameManager.disconnect();
                }
            }
        });
        
        loadSettings();
        
        return view;
    }

    private void loadSettings() {
        switchFairyPieces.setChecked(SettingsFragment.getFairyPiecesEnabled(requireContext()));
        switchEnPassant.setChecked(SettingsFragment.getEnPassantEnabled(requireContext()));
        switchPromotion.setChecked(SettingsFragment.getPromotionEnabled(requireContext()));
        switchCastling.setChecked(SettingsFragment.getCastlingEnabled(requireContext()));
    }
    @Override
    public void onConnectionSuccess(WifiP2pInfo info) {
        if (isHost && info.groupFormed) {
            int boardSize = Math.max(6, sliderBoardSize.getProgress());
            wifiGameManager = new WiFiGameManager(this, true,
                    switchFairyPieces.isChecked(),
                    switchEnPassant.isChecked(),
                    switchPromotion.isChecked(),
                    switchCastling.isChecked());
            wifiGameManager.startConnection(info.groupOwnerAddress.getHostAddress(), boardSize);
        }
    }

    @Override
    public void onConnectionEstablished() {
        if (isHost) {
            int boardSize = Math.max(6, sliderBoardSize.getProgress());
            Intent intent = new Intent(requireActivity(), PlayActivity.class);
            intent.putExtra("BOARD_SIZE", boardSize);
            intent.putExtra("GAME_MODE", "wifi");
            intent.putExtra("IS_HOST", true);

            // Add settings to intent
            intent.putExtra("FAIRY_PIECES", switchFairyPieces.isChecked());
            intent.putExtra("EN_PASSANT", switchEnPassant.isChecked());
            intent.putExtra("PROMOTION", switchPromotion.isChecked());
            intent.putExtra("CASTLING", switchCastling.isChecked());

            startActivity(intent);
            requireActivity().finish();
        }
    }

    @Override
    public void onConnectionFailure() {
        Toast.makeText(requireContext(), "Connection failed", Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onDisconnected() {
        Toast.makeText(requireContext(), "Disconnected", Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onPermissionsMissing() {
        Toast.makeText(requireContext(), "WiFi permissions missing", Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onError(String message) {
        Toast.makeText(requireContext(), "Error: " + message, Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onPeersAvailable(List<WifiP2pDevice> peers) {
        // Not used in create room
    }

    @Override
    public void onMoveReceived(Move move) {
        // Not used in this fragment
    }

    @Override
    public void onConnectionLost() {
        Toast.makeText(requireContext(), "Game connection lost", Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onBoardSizeReceived(int boardSize) {
        // Not used by host
    }

    @Override
    public void onSettingsReceived(int receivedBoardSize, boolean fairyPieces, boolean enPassant, boolean promotion, boolean castling, boolean river) {

    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (wifiDirectManager != null) {
            wifiDirectManager.cleanup();
        }
        if (wifiGameManager != null) {
            wifiGameManager.disconnect();
        }
    }
}