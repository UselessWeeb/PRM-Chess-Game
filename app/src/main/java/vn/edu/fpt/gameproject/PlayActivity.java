package vn.edu.fpt.gameproject;

import android.app.AlertDialog;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.gridlayout.widget.GridLayout;

import com.google.gson.Gson;

import java.io.PipedReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Stack;

import vn.edu.fpt.gameproject.fragment.PromotionDialogFragment;
import vn.edu.fpt.gameproject.fragment.SettingsFragment;
import vn.edu.fpt.gameproject.manager.GameManager;
import vn.edu.fpt.gameproject.model.BoardState;
import vn.edu.fpt.gameproject.model.Color;
import vn.edu.fpt.gameproject.model.Difficulty;
import vn.edu.fpt.gameproject.model.Piece;
import vn.edu.fpt.gameproject.model.PieceType;

public class PlayActivity extends AppCompatActivity {

    private int boardSize = 6;
    private GridLayout gridBoard;
    private FrameLayout[][] cells;
    private GameManager gameManager;
    private ImageView selectedPieceView = null;
    private int selectedRow = -1, selectedCol = -1;
    private final List<int[]> highlightedTiles = new ArrayList<>();
    private TextView statusText;
    private Difficulty aiDifficulty = Difficulty.MEDIUM;
    private LinearLayout capturedWhitePieces;
    private LinearLayout capturedBlackPieces;
    private ImageButton helpButton;
    private Button returnBtn;
    private Button undoBtn;
    private Button resetBtn;
    private boolean isHelperActive = false;
    private Stack<BoardState> moveHistory = new Stack<>();
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_play);

        String gameMode = getIntent().getStringExtra("GAME_MODE");
        boolean isAIMode = "ai".equals(gameMode);

        if (isAIMode) {
            // Get difficulty from intent or use default
            String difficulty = getIntent().getStringExtra("AI_DIFFICULTY");
            if ("easy".equals(difficulty)) {
                aiDifficulty = Difficulty.EASY;
            } else if ("hard".equals(difficulty)) {
                aiDifficulty = Difficulty.HARD;
            }
        }

        initializeGame();
        if ("wifi".equals(gameMode)) {
            boolean isHost = getIntent().getBooleanExtra("IS_HOST", false);
            String hostAddress = getIntent().getStringExtra("HOST_ADDRESS");

            if (hostAddress != null) {
                gameManager.setupWiFiConnection(hostAddress, isHost);
            }
        }
        helpButton = findViewById(R.id.help_btn);
        helpButton.setOnClickListener(v -> toggleHelper());

        returnBtn = findViewById(R.id.return_btn);
        undoBtn = findViewById(R.id.undo_btn);
        resetBtn = findViewById(R.id.btn_reset);

        returnBtn.setOnClickListener(v -> returnToMainMenu());
        undoBtn.setOnClickListener(v -> undoMove());
        resetBtn.setOnClickListener(v -> resetGame());

        updateStatusText();

        if (isAIMode) {
            gameManager.setAIMode(true, aiDifficulty);
            helpButton.setVisibility(View.VISIBLE);
        }
    }

    private void updateNewGameButton(){
        findViewById(R.id.btn_reset).setEnabled(moveHistory.size() > 1);
    }

    private void updateUndoButton() {
        findViewById(R.id.undo_btn).setEnabled(moveHistory.size() > 1);
    }

    private void undoMove() {
        if (moveHistory.size() < 2) {
            Toast.makeText(this, "No moves to undo", Toast.LENGTH_SHORT).show();
            return;
        }

        moveHistory.pop();
        BoardState previousState = moveHistory.peek();

        gameManager.loadBoardState(previousState);

        gameManager.setCurrentTurn(previousState.getCurrentTurn());

        int tileSize = getResources().getDisplayMetrics().widthPixels / boardSize;
        refreshBoardDisplay(tileSize);
        updateStatusText();
        updateCapturedPiecesDisplay();
        updateUndoButton();
        updateNewGameButton();
    }

    private void saveCurrentState() {
        BoardState currentState = new BoardState();
        currentState.setBoardSize(boardSize);

        List<BoardState.PiecePosition> pieces = new ArrayList<>();
        Piece[][] board = gameManager.getBoard();

        for (int row = 0; row < boardSize; row++) {
            for (int col = 0; col < boardSize; col++) {
                Piece piece = board[row][col];
                if (piece != null) {
                    BoardState.PiecePosition pos = new BoardState.PiecePosition();
                    pos.setRow(row);
                    pos.setCol(col);
                    pos.setType(piece.getPieceType());
                    pos.setColor(piece.getColor());
                    pieces.add(pos);
                }
            }
        }

        currentState.setPieces(pieces);
        currentState.setCurrentTurn(gameManager.getCurrentTurn()); // Needed for proper turn restoration

        moveHistory.push(currentState);
        updateNewGameButton();
        updateUndoButton();
    }

    private void returnToMainMenu() {
        finish();
    }

    private void handleAIMove() {
        if (gameManager.isGameOver()) return;

        gameManager.makeAIMoveIfNeeded();
    }

    private void initializeGame() {
        // Check if we have a saved board state
        String boardStateJson = getIntent().getStringExtra("BOARD_STATE");
        if (boardStateJson != null) {
            // Load from saved board state
            BoardState boardState = new Gson().fromJson(boardStateJson, BoardState.class);
            boardSize = boardState.getBoardSize();
            gameManager = new GameManager(boardSize, this);
            loadBoardState(boardState);
        } else {
            // Load default board with size from intent
            boardSize = getIntent().getIntExtra("BOARD_SIZE", 8);
            gameManager = new GameManager(boardSize, this);
        }

        // Apply settings
        gameManager.setSpecialRules(
                SettingsFragment.getPromotionEnabled(this),
                SettingsFragment.getEnPassantEnabled(this),
                SettingsFragment.getCastlingEnabled(this)
        );

        cells = new FrameLayout[boardSize][boardSize];

        gridBoard = findViewById(R.id.grid_board);
        statusText = findViewById(R.id.status_text);

        capturedWhitePieces = findViewById(R.id.captured_white_pieces);
        capturedBlackPieces = findViewById(R.id.captured_black_pieces);

        int tileSize = getResources().getDisplayMetrics().widthPixels / boardSize;

        createChessBoard(tileSize);
        updateBoardDisplay(tileSize);
        updateCapturedPiecesDisplay();
        saveCurrentState();
    }

    private void loadBoardState(BoardState boardState) {
        // Clear the board first
        gameManager.clearBoard();

        // Add all pieces from the board state
        for (BoardState.PiecePosition piecePos : boardState.getPieces()) {
            gameManager.addPiece(
                    piecePos.getRow(),
                    piecePos.getCol(),
                    piecePos.getType(),
                    piecePos.getColor()
            );
        }

        // Update game state
        gameManager.updateGameState();
    }

    private void createChessBoard(int tileSize) {
        gridBoard.removeAllViews();
        gridBoard.setRowCount(boardSize);
        gridBoard.setColumnCount(boardSize);

        for (int row = 0; row < boardSize; row++) {
            for (int col = 0; col < boardSize; col++) {
                FrameLayout cell = new FrameLayout(this);

                GridLayout.LayoutParams params = new GridLayout.LayoutParams();
                params.width = tileSize;
                params.height = tileSize;
                params.rowSpec = GridLayout.spec(row);
                params.columnSpec = GridLayout.spec(col);
                cell.setLayoutParams(params);

                // Add tile background
                ImageView tile = new ImageView(this);
                tile.setImageResource((row + col) % 2 == 0 ?
                        R.drawable.white_tiles : R.drawable.black_tiles);
                tile.setLayoutParams(new FrameLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                ));
                tile.setScaleType(ImageView.ScaleType.CENTER_CROP);
                cell.addView(tile);

                // Add click listener for empty tiles
                final int finalRow = row;
                final int finalCol = col;
                cell.setOnClickListener(v -> handleTileClick(finalRow, finalCol));

                gridBoard.addView(cell);
                cells[row][col] = cell;
            }
        }
    }

    private void handleTileClick(int row, int col) {
        if (gameManager.isGameOver()) {
            showGameOverDialog();
            return;
        }

        if (isHelperActive) {
            hideHelper();
            helpButton.setImageResource(R.drawable.help_btn);
            isHelperActive = false;
        }

        Piece[][] board = gameManager.getBoard();
        Piece clickedPiece = board[row][col];

        // If there's a selected piece and we click on a highlighted tile, try to move
        if (selectedPieceView != null && selectedRow != -1 && selectedCol != -1) {
            if (isTileHighlighted(row, col)) {
                // Try to move the piece
                if (gameManager.movePiece(selectedRow, selectedCol, row, col)) {
                    updateBoardDisplay(getResources().getDisplayMetrics().widthPixels / boardSize);
                    updateStatusText();
                    saveCurrentState();
                    if (gameManager.isGameOver()) {
                        Toast.makeText(this, gameManager.getGameStatus(), Toast.LENGTH_LONG).show();
                    } else {
                        // Trigger AI move if in AI mode
                        handleAIMove();
                    }
                }
                clearHighlights();
                return;
            }
        }
        clearHighlights();

        // If clicking on a piece of the current player, select it
        if (clickedPiece != null && clickedPiece.getColor() == gameManager.getCurrentTurn()) {
            selectedRow = row;
            selectedCol = col;
            selectedPieceView = findPieceViewAt(row, col);

            List<int[]> validMoves = gameManager.getValidMovesForPiece(row, col);
            highlightMoves(validMoves);
        }
    }

    private boolean isTileHighlighted(int row, int col) {
        for (int[] tile : highlightedTiles) {
            if (tile[0] == row && tile[1] == col) {
                return true;
            }
        }
        return false;
    }

    private ImageView findPieceViewAt(int row, int col) {
        FrameLayout cell = cells[row][col];
        for (int i = 0; i < cell.getChildCount(); i++) {
            if (cell.getChildAt(i) instanceof ImageView) {
                ImageView imageView = (ImageView) cell.getChildAt(i);
                // Check if this is a piece view (not the tile background)
                if (cell.getChildCount() > 1 && i > 0) {
                    return imageView;
                }
            }
        }
        return null;
    }

    private int getPieceDrawable(PieceType type, Color color) {
        boolean isWhite = color == Color.WHITE;
        switch (type) {
            case PAWN: return isWhite ? R.drawable.white_pawn : R.drawable.black_pawn;
            case ROOK: return isWhite ? R.drawable.white_rook : R.drawable.black_rook;
            case KNIGHT: return isWhite ? R.drawable.white_horse : R.drawable.black_horse;
            case BISHOP: return isWhite ? R.drawable.white_bishop : R.drawable.black_bishop;
            case QUEEN: return isWhite ? R.drawable.white_queen : R.drawable.black_queen;
            case KING: return isWhite ? R.drawable.white_king : R.drawable.black_king;
            case ARCHBISHOP: return isWhite ? R.drawable.white_archbishop : R.drawable.black_archbishop;
            case CHANCELLOR: return isWhite ? R.drawable.white_chancellor : R.drawable.black_chancellor;
            default: return isWhite ? R.drawable.white_pawn : R.drawable.black_pawn;
        }
    }

    private void addPieceView(int row, int col, int resId, int tileSize) {
        ImageView pieceView = new ImageView(this);
        pieceView.setImageResource(resId);
        pieceView.setLayoutParams(new FrameLayout.LayoutParams(tileSize, tileSize));
        pieceView.setScaleType(ImageView.ScaleType.CENTER_INSIDE);

        // The click listener is now handled by the cell itself
        cells[row][col].addView(pieceView);
    }

    private void highlightMoves(List<int[]> moves) {
        for (int[] move : moves) {
            int r = move[0], c = move[1];
            FrameLayout tile = cells[r][c];
            for (int i = 0; i < tile.getChildCount(); i++) {
                View view = tile.getChildAt(i);
                if (view.getTag() != null && view.getTag().equals("highlight")) {
                    view.setVisibility(View.VISIBLE);
                    view.bringToFront();
                    tile.invalidate();
                    break;
                }
            }
            highlightedTiles.add(move);
        }
    }

    private void clearHighlights() {
        for (int[] tile : highlightedTiles) {
            int r = tile[0], c = tile[1];
            FrameLayout cell = cells[r][c];

            // Find the highlight tile and hide it
            for (int i = 0; i < cell.getChildCount(); i++) {
                View view = cell.getChildAt(i);
                if (view.getTag() != null && view.getTag().equals("highlight")) {
                    view.setVisibility(View.INVISIBLE);
                    break;
                }
            }
        }

        highlightedTiles.clear();
        selectedPieceView = null;
        selectedRow = -1;
        selectedCol = -1;
    }

    public void updateStatusText() {
        if (statusText != null) {
            statusText.setText(gameManager.getGameStatus());
        }
    }

    public void resetGame() {
        boolean wasAIMode = gameManager != null && gameManager.getChessAI() != null;
        moveHistory.clear();
        gameManager = new GameManager(boardSize, this);
        if (wasAIMode) {
            gameManager.setAIMode(true, aiDifficulty);
        }
        clearHighlights();
        hideHelper();
        updateBoardDisplay(getResources().getDisplayMetrics().widthPixels / boardSize);
        updateStatusText();
        updateNewGameButton();
        updateUndoButton();
        updateCapturedPiecesDisplay();
        saveCurrentState();
    }

    private void updateBoardDisplay(int tileSize) {
        refreshBoardDisplay(tileSize);
    }

    public void showPromotionDialog() {
        PromotionDialogFragment dialog = new PromotionDialogFragment(gameManager);
        dialog.show(getSupportFragmentManager(), "PromotionDialog");
    }

    public void refreshBoardDisplay(int tileSize) {
        Piece[][] board = gameManager.getBoard();

        // Clear all views except the tile background
        for (int row = 0; row < boardSize; row++) {
            for (int col = 0; col < boardSize; col++) {
                FrameLayout cell = cells[row][col];
                while (cell.getChildCount() > 1) {
                    cell.removeViewAt(1);
                }
            }
        }

        // Add highlight tiles on top of everything
        for (int row = 0; row < boardSize; row++) {
            for (int col = 0; col < boardSize; col++) {
                FrameLayout cell = cells[row][col];
                ImageView highlightTile = new ImageView(this);
                highlightTile.setImageResource((row + col) % 2 == 0 ?
                        R.drawable.highlighted_white_tile : R.drawable.highlighted_black_tile);
                highlightTile.setLayoutParams(new FrameLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT));
                highlightTile.setScaleType(ImageView.ScaleType.CENTER_CROP);
                highlightTile.setVisibility(View.INVISIBLE);
                highlightTile.setTag("highlight");
                cell.addView(highlightTile);

                //create suggest tile
                ImageView suggestTile = new ImageView(this);
                suggestTile.setImageResource(R.drawable.helper_tile);
                suggestTile.setLayoutParams(new FrameLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT));
                suggestTile.setScaleType(ImageView.ScaleType.CENTER_CROP);
                suggestTile.setVisibility(View.INVISIBLE);
                suggestTile.setTag("target");
                cell.addView(suggestTile);
            }
        }

        // Add pieces to the board
        for (int row = 0; row < boardSize; row++) {
            for (int col = 0; col < boardSize; col++) {
                Piece piece = board[row][col];
                if (piece != null) {
                    addPieceView(row, col, getPieceDrawable(piece.getPieceType(), piece.getColor()), tileSize);
                }
            }
        }
        updateCapturedPiecesDisplay();
    }

    public void updateCapturedPiecesDisplay() {
        // Clear existing captured pieces views
        capturedWhitePieces.removeAllViews();
        capturedBlackPieces.removeAllViews();

        List<Piece> capturedPiecesList = gameManager.getCapturedPieces();
        int capturedPieceSize = 40; // Size in dp for captured pieces

        for (Piece capturedPiece : capturedPiecesList) {
            ImageView pieceView = new ImageView(this);
            pieceView.setImageResource(getPieceDrawable(capturedPiece.getPieceType(), capturedPiece.getColor()));

            // Convert dp to pixels
            int sizeInPixels = (int) (capturedPieceSize * getResources().getDisplayMetrics().density);
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(sizeInPixels, sizeInPixels);
            params.setMargins(4, 0, 4, 0); // Add small margin between pieces

            pieceView.setLayoutParams(params);
            pieceView.setScaleType(ImageView.ScaleType.CENTER_INSIDE);

            // Add to appropriate section based on piece color
            if (capturedPiece.getColor() == Color.WHITE) {
                capturedWhitePieces.addView(pieceView);
            } else {
                capturedBlackPieces.addView(pieceView);
            }
        }
    }

    public void showGameOverDialog() {
        if (gameManager.isGameOver()) {
            String message;
            if (gameManager.getWinner() == null) {
                message = "Game ended in a draw!";
            } else {
                message = (gameManager.getWinner() == Color.WHITE ? "White" : "Black") + " wins!";
            }

            new AlertDialog.Builder(this)
                    .setTitle("Game Over")
                    .setMessage(message)
                    .setPositiveButton("New Game", (dialog, which) -> resetGame())
                    .setNegativeButton("Close", null)
                    .setCancelable(false)
                    .show();
        }
    }

    private void toggleHelper() {
        isHelperActive = !isHelperActive;

        if (isHelperActive) {
            showAISuggestion();
        } else {
            hideHelper();
            helpButton.setImageResource(R.drawable.help_btn);
        }
    }

    private void showAISuggestion() {
        // Clear any existing highlights first
        hideHelper();

        // Get the AI's suggested move
        int[] suggestedMove = gameManager.getChessAI().getSuggestedMove();
        if (suggestedMove == null) return;

        // Show the suggestion indicators
        showSuggestionIndicator(suggestedMove[0], suggestedMove[1]); // From position
        showSuggestionIndicator(suggestedMove[2], suggestedMove[3]); // To position
    }

    private void showSuggestionIndicator(int row, int col) {
        FrameLayout cell = cells[row][col];
        for (int i = 0; i < cell.getChildCount(); i++) {
            View child = cell.getChildAt(i);
            if (child instanceof ImageView && "target".equals(child.getTag())) {
                child.setVisibility(View.VISIBLE);
                child.bringToFront();
                break;
            }
        }
    }

    private void hideHelper() {
        // Hide all suggestion indicators
        for (int row = 0; row < boardSize; row++) {
            for (int col = 0; col < boardSize; col++) {
                FrameLayout cell = cells[row][col];
                for (int i = 0; i < cell.getChildCount(); i++) {
                    View child = cell.getChildAt(i);
                    if (child instanceof ImageView && "target".equals(child.getTag())) {
                        child.setVisibility(View.INVISIBLE);
                    }
                }
            }
        }
        isHelperActive = false;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (gameManager != null && gameManager.isWiFiConnected()) {
            gameManager.disconnectWiFi(); // Add this method to GameManager
        }
    }
}