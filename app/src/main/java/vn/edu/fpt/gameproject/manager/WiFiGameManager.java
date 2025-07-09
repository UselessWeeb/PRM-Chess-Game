package vn.edu.fpt.gameproject.manager;

import android.util.Log;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;

import vn.edu.fpt.gameproject.fragment.SettingsFragment;
import vn.edu.fpt.gameproject.model.Color;
import vn.edu.fpt.gameproject.model.Move;

public class WiFiGameManager {
    private static final String TAG = "WiFiGameManager";
    private static final int PORT = 8888;
    private static final int CONNECTION_TIMEOUT = 5000;
    private static final int BUFFER_SIZE = 1024;

    private GameStateListener listener;
    private ServerSocket serverSocket;
    private Socket clientSocket;
    private InputStream inputStream;
    private OutputStream outputStream;
    private Thread receiveThread;
    private boolean isHost;
    private boolean isRunning = false;
    private int boardSize;
    private boolean fairyPieces;
    private boolean enPassant;
    private boolean promotion;
    private boolean castling;

    public interface GameStateListener {
        void onMoveReceived(Move move);
        void onConnectionEstablished();
        void onConnectionLost();
        void onError(String message);
        void onBoardSizeReceived(int boardSize); // New method for board size

        void onSettingsReceived(int receivedBoardSize, boolean fairyPieces, boolean enPassant, boolean promotion, boolean castling);
    }

    public WiFiGameManager(GameStateListener listener, boolean isHost,
                           boolean fairyPieces, boolean enPassant,
                           boolean promotion, boolean castling) {
        this.listener = listener;
        this.isHost = isHost;
        this.fairyPieces = fairyPieces;
        this.enPassant = enPassant;
        this.promotion = promotion;
        this.castling = castling;
    }

    public void startConnection(String hostAddress, int boardSize) {
        this.boardSize = boardSize;
        new Thread(() -> {
            try {
                if (isHost) {
                    // Host logic (White plays first)
                    serverSocket = new ServerSocket(PORT);
                    clientSocket = serverSocket.accept();
                    sendBoardSize(); // Send settings to client
                    listener.onConnectionEstablished();
                } else {
                    // Client logic (Black plays second)
                    clientSocket = new Socket(hostAddress, PORT);
                    waitForBoardSize(); // Wait for host settings
                    listener.onConnectionEstablished();

                    // Force client to wait for host's first move
                    if (listener instanceof GameManager) {
                        ((GameManager) listener).setCurrentTurn(Color.WHITE);
                    }
                }
                startReceiving();
            } catch (IOException e) {
                listener.onError("Connection failed: " + e.getMessage());
            }
        }).start();
    }

    private void sendBoardSize() {
        try {
            String settingsMsg = String.format("SETTINGS:%d:%b:%b:%b:%b",
                    boardSize,
                    fairyPieces,
                    enPassant,
                    promotion,
                    castling);

            outputStream.write(settingsMsg.getBytes());
            outputStream.flush();
            Log.d(TAG, "Sent board size and settings");
        } catch (IOException e) {
            Log.e(TAG, "Error sending board size and settings", e);
        }
    }

    private void waitForBoardSize() {
        try {
            byte[] buffer = new byte[BUFFER_SIZE];
            int bytesRead = inputStream.read(buffer);
            if (bytesRead > 0) {
                String received = new String(buffer, 0, bytesRead);
                Log.d(TAG, "Received: " + received);

                if (received.startsWith("SETTINGS:")) {
                    String[] parts = received.split(":");
                    int receivedBoardSize = Integer.parseInt(parts[1]);
                    boolean fairyPieces = Boolean.parseBoolean(parts[2]);
                    boolean enPassant = Boolean.parseBoolean(parts[3]);
                    boolean promotion = Boolean.parseBoolean(parts[4]);
                    boolean castling = Boolean.parseBoolean(parts[5]);

                    // You'll need to add these to your GameStateListener interface
                    listener.onSettingsReceived(receivedBoardSize, fairyPieces, enPassant, promotion, castling);
                }
            }
        } catch (IOException | NumberFormatException | ArrayIndexOutOfBoundsException e) {
            Log.e(TAG, "Error receiving board size and settings", e);
        }
    }

    private void startReceiving() {
        receiveThread = new Thread(() -> {
            byte[] buffer = new byte[BUFFER_SIZE];

            while (isRunning && clientSocket != null && clientSocket.isConnected()) {
                try {
                    int bytesRead = inputStream.read(buffer);
                    if (bytesRead == -1) {
                        break;
                    }
                    if (bytesRead > 0) {
                        String received = new String(buffer, 0, bytesRead);
                        Log.d(TAG, "Received: " + received);

                        // Skip board size messages during game
                        if (received.startsWith("BOARD_SIZE:")) {
                            continue;
                        }

                        Move move = Move.fromString(received);
                        listener.onMoveReceived(move);
                    }
                } catch (IOException e) {
                    if (isRunning) {
                        String error = "Receive error: " + e.getMessage();
                        Log.e(TAG, error, e);
                        listener.onError(error);
                        listener.onConnectionLost();
                    }
                    break;
                } catch (IllegalArgumentException e) {
                    String error = "Invalid move format: " + e.getMessage();
                    Log.e(TAG, error, e);
                    listener.onError(error);
                }
            }
            cleanup();
        });
        receiveThread.start();
    }

    public void sendMove(Move move) {
        new Thread(() -> {
            if (outputStream == null || !isRunning) {
                listener.onError("Not connected to opponent");
                return;
            }

            try {
                String moveStr = move.toString();
                Log.d(TAG, "Sending: " + moveStr);
                outputStream.write(moveStr.getBytes());
                outputStream.flush();
            } catch (IOException e) {
                String error = "Send error: " + e.getMessage();
                Log.e(TAG, error, e);
                listener.onError(error);
                listener.onConnectionLost();
                cleanup();
            }
        }).start();
    }

    public void disconnect() {
        isRunning = false;
        cleanup();
    }

    private void cleanup() {
        try {
            if (receiveThread != null) {
                receiveThread.interrupt();
            }
            if (inputStream != null) {
                inputStream.close();
            }
            if (outputStream != null) {
                outputStream.close();
            }
            if (clientSocket != null) {
                clientSocket.close();
            }
            if (serverSocket != null) {
                serverSocket.close();
            }
        } catch (IOException e) {
            Log.e(TAG, "Cleanup error", e);
        } finally {
            inputStream = null;
            outputStream = null;
            clientSocket = null;
            serverSocket = null;
            receiveThread = null;
        }
    }

    public boolean isConnected() {
        return isRunning &&
                clientSocket != null &&
                clientSocket.isConnected() &&
                !clientSocket.isClosed();
    }
}