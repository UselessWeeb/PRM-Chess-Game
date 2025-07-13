package vn.edu.fpt.gameproject;

import android.app.AlertDialog;
import android.content.ClipData;
import android.icu.text.SimpleDateFormat;
import android.os.Bundle;
import android.text.InputType;
import android.view.DragEvent;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.GridLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.gson.Gson;

import java.io.File;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import vn.edu.fpt.gameproject.manager.GameManager;
import vn.edu.fpt.gameproject.model.BoardState;
import vn.edu.fpt.gameproject.model.Color;
import vn.edu.fpt.gameproject.model.Constants;
import vn.edu.fpt.gameproject.model.Piece;
import vn.edu.fpt.gameproject.model.PieceType;

public class CustomBoardActivity extends AppCompatActivity {
    private Piece[][] board;
    private int boardSize;
    private ImageView[][] cellViews;
    private List<ImageView> whitePieces = new ArrayList<>();
    private List<ImageView> blackPieces = new ArrayList<>();
    private Button saveButton;
    private GridLayout gridBoard;
    private LinearLayout whitePiecesContainer, blackPiecesContainer;
    private boolean isEditing = false;
    private String originalBoardName = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_custom_board);

        gridBoard = findViewById(R.id.grid_board);
        saveButton = findViewById(R.id.save_button);
        whitePiecesContainer = findViewById(R.id.white_pieces_container);
        blackPiecesContainer = findViewById(R.id.black_pieces_container);

        // Initialize cellViews array first
        boardSize = getIntent().hasExtra("BOARD_STATE") ?
                new Gson().fromJson(getIntent().getStringExtra("BOARD_STATE"), BoardState.class).getBoardSize() :
                getIntent().getIntExtra("BOARD_SIZE", 8);

        board = new Piece[boardSize][boardSize];
        cellViews = new ImageView[boardSize][boardSize];

        // Check if we're editing an existing board
        if (getIntent().hasExtra("BOARD_STATE")) {
            isEditing = true;
            String boardJson = getIntent().getStringExtra("BOARD_STATE");
            BoardState boardState = new Gson().fromJson(boardJson, BoardState.class);
            originalBoardName = boardState.getName();

            // Load pieces from board state
            for (BoardState.PiecePosition pp : boardState.getPieces()) {
                board[pp.getRow()][pp.getCol()] = new Piece(pp.getType(), pp.getColor());
            }

            setTitle("Edit Board: " + originalBoardName);
        } else {
            setTitle("Create New Board");
        }

        setupBoard();
        setupPieces();

        saveButton.setOnClickListener(v -> saveBoard());

        if (isEditing) {
            checkBoardValidity();
        }
    }

    private void setupBoard() {
        gridBoard.removeAllViews();
        gridBoard.setRowCount(boardSize);
        gridBoard.setColumnCount(boardSize);

        int screenWidth = getResources().getDisplayMetrics().widthPixels;
        int padding = (int) (16 * getResources().getDisplayMetrics().density);
        int availableWidth = screenWidth - (padding * 2);
        int tileSize = availableWidth / boardSize;

        for (int row = 0; row < boardSize; row++) {
            for (int col = 0; col < boardSize; col++) {
                ImageView cell = new ImageView(this);
                GridLayout.LayoutParams params = new GridLayout.LayoutParams();
                params.width = tileSize;
                params.height = tileSize;
                params.rowSpec = GridLayout.spec(row);
                params.columnSpec = GridLayout.spec(col);
                cell.setLayoutParams(params);

                // Initialize with empty tile first
                cell.setImageResource((row + col) % 2 == 0 ?
                        R.drawable.white_tiles : R.drawable.black_tiles);
                cell.setScaleType(ImageView.ScaleType.CENTER_CROP);

                cell.setTag(new int[]{row, col});
                cell.setOnDragListener(new PieceDragListener());
                cell.setOnClickListener(v -> handleCellClick(v));

                gridBoard.addView(cell);
                cellViews[row][col] = cell;
            }
        }

        // Now that all cells are created, update pieces if editing
        if (isEditing) {
            for (int row = 0; row < boardSize; row++) {
                for (int col = 0; col < boardSize; col++) {
                    if (board[row][col] != null) {
                        updateCellImage(row, col, board[row][col]);
                    }
                }
            }
        }
    }

    private void setupPieces() {
        // Clear existing pieces (if any)
        whitePiecesContainer.removeAllViews();
        blackPiecesContainer.removeAllViews();
        whitePieces.clear();
        blackPieces.clear();

        // Create ONE of each white piece (except king, which is handled separately)
        for (PieceType type : PieceType.values()) {
            if (type != PieceType.KING) {
                ImageView piece = createPieceView(type, Color.WHITE);
                whitePiecesContainer.addView(piece);
                whitePieces.add(piece);
            }
        }

        // Create ONE of each black piece (except king)
        for (PieceType type : PieceType.values()) {
            if (type != PieceType.KING) {
                ImageView piece = createPieceView(type, Color.BLACK);
                blackPiecesContainer.addView(piece);
                blackPieces.add(piece);
            }
        }

        // Add ONE white king (unique)
        ImageView whiteKing = createPieceView(PieceType.KING, Color.WHITE);
        whitePiecesContainer.addView(whiteKing);
        whitePieces.add(whiteKing);

        // Add ONE black king (unique)
        ImageView blackKing = createPieceView(PieceType.KING, Color.BLACK);
        blackPiecesContainer.addView(blackKing);
        blackPieces.add(blackKing);
    }

    private ImageView createPieceView(PieceType type, Color color) {
        ImageView piece = new ImageView(this);
        piece.setImageResource(getPieceDrawable(type, color));
        piece.setTag(new Piece(type, color));

        // Set fixed size for pieces
        int pieceSize = (int) (60 * getResources().getDisplayMetrics().density); // 60dp
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(pieceSize, pieceSize);
        params.setMargins(8, 8, 8, 8);
        piece.setLayoutParams(params);
        piece.setScaleType(ImageView.ScaleType.FIT_CENTER);

        setupPieceDrag(piece);
        return piece;
    }

    private void setupPieceDrag(ImageView pieceView) {
        pieceView.setOnLongClickListener(v -> {
            Piece piece = (Piece) v.getTag();
            if (piece != null) {
                ClipData clipData = ClipData.newPlainText("piece", "drag");
                View.DragShadowBuilder shadowBuilder = new View.DragShadowBuilder(v);

                // Store both the piece and the view in a custom object
                DragData dragData = new DragData(piece, v);
                boolean dragStarted = v.startDragAndDrop(clipData, shadowBuilder, dragData, 0);

                if (dragStarted) {
                    return true;
                }
            }
            return false;
        });
    }

    // Add this helper_tile class at the bottom of your activity
    private static class DragData {
        final Piece piece;
        final View view;

        DragData(Piece piece, View view) {
            this.piece = piece;
            this.view = view;
        }
    }

    private class PieceDragListener implements View.OnDragListener {
        @Override
        public boolean onDrag(View v, DragEvent event) {
            int[] position = (int[]) v.getTag();
            int row = position[0];
            int col = position[1];

            switch (event.getAction()) {
                case DragEvent.ACTION_DRAG_STARTED:
                    return true;

                case DragEvent.ACTION_DRAG_ENTERED:
                    v.setAlpha(0.5f);
                    return true;

                case DragEvent.ACTION_DRAG_EXITED:
                    v.setAlpha(1.0f);
                    return true;

                case DragEvent.ACTION_DROP:
                    v.setAlpha(1.0f);

                    DragData dragData = (DragData) event.getLocalState();
                    if (dragData != null) {
                        Piece draggedPiece = dragData.piece;
                        View draggedView = dragData.view;

                        // Remove existing piece if it's a king (to enforce one king per side)
                        if (draggedPiece.getPieceType() == PieceType.KING) {
                            removePieceFromBoard(draggedPiece);
                        }

                        // Place the new piece
                        board[row][col] = draggedPiece;
                        updateCellImage(row, col, draggedPiece);

                        // If dragged from container, make the source piece visible again
                        if (draggedView.getParent() == whitePiecesContainer ||
                                draggedView.getParent() == blackPiecesContainer) {
                            draggedView.setVisibility(View.VISIBLE);
                        }

                        checkBoardValidity();
                        return true;
                    }
                    return false;

                case DragEvent.ACTION_DRAG_ENDED:
                    v.setAlpha(1.0f);

                    // Make the source piece visible again if drag failed
                    DragData endDragData = (DragData) event.getLocalState();
                    if (endDragData != null && !event.getResult()) {
                        View endView = endDragData.view;
                        if (endView.getParent() == whitePiecesContainer ||
                                endView.getParent() == blackPiecesContainer) {
                            endView.setVisibility(View.VISIBLE);
                        }
                    }
                    return true;

                default:
                    return false;
            }
        }
    }

    private void removePieceFromBoard(Piece draggedPiece) {
        // Only remove existing pieces if they are KINGS (to enforce one king per side)
        for (int r = 0; r < boardSize; r++) {
            for (int c = 0; c < boardSize; c++) {
                Piece existingPiece = board[r][c];
                if (existingPiece != null &&
                        existingPiece.getPieceType() == draggedPiece.getPieceType() &&
                        existingPiece.getColor() == draggedPiece.getColor()) {

                    // Only remove if it's a king (to enforce one king per side)
                    if (draggedPiece.getPieceType() == PieceType.KING) {
                        board[r][c] = null;
                        updateCellImage(r, c, null);
                        return;
                    }
                }
            }
        }
    }

    private void updateCellImage(int row, int col, Piece piece) {
        ImageView cell = cellViews[row][col];
        if (cell == null) return;

        if (piece == null) {
            cell.setImageResource((row + col) % 2 == 0 ?
                    R.drawable.white_tiles : R.drawable.black_tiles);
        } else {
            cell.setImageResource(getPieceDrawable(piece.getPieceType(), piece.getColor()));
        }
        cell.setScaleType(ImageView.ScaleType.CENTER_CROP);
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

    private void handleCellClick(View v) {
        int[] position = (int[]) v.getTag();
        int row = position[0];
        int col = position[1];

        Piece piece = board[row][col];
        if (piece != null) {
            board[row][col] = null;
            updateCellImage(row, col, null);
            checkBoardValidity();
            Toast.makeText(this, "Piece removed", Toast.LENGTH_SHORT).show();
        }
    }

    private void checkBoardValidity() {
        boolean hasWhiteKing = false;
        boolean hasBlackKing = false;
        int whiteKingCount = 0;
        int blackKingCount = 0;

        for (int r = 0; r < boardSize; r++) {
            for (int c = 0; c < boardSize; c++) {
                Piece piece = board[r][c];
                if (piece != null && piece.getPieceType() == PieceType.KING) {
                    if (piece.getColor() == Color.WHITE) {
                        hasWhiteKing = true;
                        whiteKingCount++;
                    } else {
                        hasBlackKing = true;
                        blackKingCount++;
                    }
                }
            }
        }

        boolean isValid = hasWhiteKing && hasBlackKing &&
                whiteKingCount == 1 && blackKingCount == 1
                && isBoardValid();

        saveButton.setEnabled(isValid);

        // Show appropriate message
        if (!isValid) {
            String message = "";
            if (whiteKingCount == 0 && blackKingCount == 0) {
                message = "Place both kings on the board";
            } else if (whiteKingCount == 0) {
                message = "Place the white king on the board";
            } else if (blackKingCount == 0) {
                message = "Place the black king on the board";
            } else if (whiteKingCount > 1) {
                message = "Remove extra white kings (only 1 allowed)";
            } else if (blackKingCount > 1) {
                message = "Remove extra black kings (only 1 allowed)";
            }
            if (!message.isEmpty()) {
                Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
            }
        } else {
            Toast.makeText(this, "Board is valid - ready to save!", Toast.LENGTH_SHORT).show();
        }
    }

    private boolean isBoardValid() {
        // Create a temporary GameManager to validate the board
        GameManager tempManager = new GameManager(boardSize, null);

        // Disable special rules for validation since we're just checking basic validity
        tempManager.setSpecialRules(false, false, false, false);

        // Convert our board to GameManager's format
        for (int row = 0; row < boardSize; row++) {
            for (int col = 0; col < boardSize; col++) {
                if (board[row][col] != null) {
                    tempManager.addPiece(row, col,
                            board[row][col].getPieceType(),
                            board[row][col].getColor());
                }
            }
        }
        tempManager.updateGameState();

        return !tempManager.isGameOver();
    }

    private void saveBoard() {
        if (isEditing) {
            saveBoardToJsonFile(originalBoardName);
        } else {
            showSaveDialog();
        }
    }

    private void showSaveDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(isEditing ? "Update Board Configuration" : "Save Board Configuration");

        final EditText input = new EditText(this);
        input.setInputType(InputType.TYPE_CLASS_TEXT);

        if (isEditing) {
            input.setText(originalBoardName);
        } else {
            // Generate default name with timestamp for new boards
            String timestamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
            String defaultName = Constants.DEFAULT_NAME + timestamp;
            input.setText(defaultName);
        }
        input.setSelectAllOnFocus(true);

        builder.setView(input);

        builder.setPositiveButton(isEditing ? "Update" : "Save", (dialog, which) -> {
            String boardName = input.getText().toString().trim();
            if (boardName.isEmpty()) {
                if (!isEditing) {
                    String timestamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
                    boardName = Constants.DEFAULT_NAME + timestamp;
                } else {
                    boardName = originalBoardName;
                }
            }
            saveBoardToJsonFile(boardName);
        });

        builder.setNegativeButton("Cancel", null);
        builder.show();
    }

    private void saveBoardToJsonFile(String boardName) {
        // Create the board state object
        BoardState boardState = new BoardState();
        boardState.setName(boardName);
        boardState.setBoardSize(boardSize);
        boardState.setCreatedAt(new Date());
        boardState.setPieces(new ArrayList<>());

        // Populate piece positions
        for (int r = 0; r < boardSize; r++) {
            for (int c = 0; c < boardSize; c++) {
                Piece piece = board[r][c];
                if (piece != null) {
                    BoardState.PiecePosition pp = new BoardState.PiecePosition();
                    pp.setRow(r);
                    pp.setCol(c);
                    pp.setType(piece.getPieceType());
                    pp.setColor(piece.getColor());
                    boardState.getPieces().add(pp);
                }
            }
        }

        // Convert to JSON
        Gson gson = new Gson();
        String json = gson.toJson(boardState);

        try {
            File dir = new File(getFilesDir(), Constants.SAVED_BOARDS_DIR);
            if (!dir.exists()) {
                dir.mkdir();
            }

            // If editing, delete the old file first
            if (isEditing && !boardName.equals(originalBoardName)) {
                File oldFile = new File(dir, originalBoardName + ".json");
                if (oldFile.exists()) {
                    oldFile.delete();
                }
            }

            // Create/update the file
            File file = new File(dir, boardName + ".json");
            FileOutputStream fos = new FileOutputStream(file);
            fos.write(json.getBytes());
            fos.close();

            Toast.makeText(this, "Board " + (isEditing ? "updated" : "saved") + " successfully!", Toast.LENGTH_SHORT).show();
            finish();
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "Failed to " + (isEditing ? "update" : "save") + " board", Toast.LENGTH_SHORT).show();
        }
    }
}