package vn.edu.fpt.gameproject.manager;

import android.os.Handler;
import android.os.Looper;
import android.widget.Toast;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;

import vn.edu.fpt.gameproject.PlayActivity;
import vn.edu.fpt.gameproject.fragment.PromotionDialogFragment;
import vn.edu.fpt.gameproject.fragment.SettingsFragment;
import vn.edu.fpt.gameproject.model.BoardState;
import vn.edu.fpt.gameproject.model.Color;
import vn.edu.fpt.gameproject.model.Difficulty;
import vn.edu.fpt.gameproject.model.Move;
import vn.edu.fpt.gameproject.model.Piece;
import vn.edu.fpt.gameproject.model.PieceType;
import vn.edu.fpt.gameproject.utils.ChessAI;

public class GameManager implements PromotionDialogFragment.OnPieceSelectedListener, WiFiGameManager.GameStateListener{

    private Piece[][] board;
    private Color currentTurn;
    private boolean gameOver;
    private Color winner;
    private int boardSize;
    private List<Piece> capturedPieces;
    private int[] whiteKingPos;
    private int[] blackKingPos;
    private ChessAI chessAI;
    private boolean aiMode = false;
    private boolean whiteKingSideCastle;
    private boolean whiteQueenSideCastle;
    private boolean blackKingSideCastle;
    private boolean blackQueenSideCastle;
    private int[] enPassantTarget = null;
    private WiFiGameManager wifiGameManager;
    private final WeakReference<PlayActivity> activityRef;
    private boolean promotionEnabled = true;
    private boolean enPassantEnabled = true;
    private boolean castlingEnabled = true;
    private boolean riverEnabled = false;
    private int riverRow = -1;
    public ChessAI getChessAI() {
        return chessAI;
    }

    public boolean isPromotionEnabled() {
        return promotionEnabled;
    }

    public void setPromotionEnabled(boolean promotionEnabled) {
        this.promotionEnabled = promotionEnabled;
    }

    public boolean isEnPassantEnabled() {
        return enPassantEnabled;
    }

    public void setEnPassantEnabled(boolean enPassantEnabled) {
        this.enPassantEnabled = enPassantEnabled;
    }

    public boolean isCastlingEnabled() {
        return castlingEnabled;
    }

    public void setCastlingEnabled(boolean castlingEnabled) {
        this.castlingEnabled = castlingEnabled;
    }

    public void setRiverEnabled(boolean enabled) {
        this.riverEnabled = enabled;
    }

    public boolean isRiverEnabled() {
        return riverEnabled;
    }

    public int getRiverRow() {
        if (riverEnabled) {
            riverRow = boardSize / 2 - 1;
        } else {
            riverRow = -1;
        }
        return riverRow;
    }

    public boolean canCrossRiver(PieceType type) {
        switch(type) {
            case KNIGHT:
            case ARCHBISHOP:
            case CHANCELLOR:
                return false;
            case PAWN:
            case ROOK:
            case BISHOP:
            case QUEEN:
            case KING:
            default:
                return true;
        }
    }

    public boolean isRiverBetween(int fromRow, int toRow) {
        if (!riverEnabled) return false;
        return (fromRow <= riverRow && toRow > riverRow) ||
                (fromRow > riverRow && toRow <= riverRow);
    }

    public GameManager(int boardSize, PlayActivity activity) {
        this.boardSize = boardSize;
        this.board = new Piece[boardSize][boardSize];
        this.activityRef = new WeakReference<>(activity);
        this.currentTurn = Color.WHITE;
        this.gameOver = false;
        this.capturedPieces = new ArrayList<>();
        this.whiteKingPos = new int[2];
        this.blackKingPos = new int[2];
        initializeBoard();
    }

    private void initializeBoard() {
        // Clear the board
        clearBoard();

        // Standard initialization only if no custom board was loaded
        int lastRow = boardSize - 1;
        int pawnRowWhite = lastRow - 1;
        int pawnRowBlack = 1;

        // Add pawns
        for (int col = 0; col < boardSize; col++) {
            board[pawnRowWhite][col] = new Piece(PieceType.PAWN, Color.WHITE);
            board[pawnRowBlack][col] = new Piece(PieceType.PAWN, Color.BLACK);
        }

        // Setup back rows
        setupBackRow(0, Color.BLACK);
        setupBackRow(lastRow, Color.WHITE);

        // Find and store king positions
        findKingPositions();
    }

    private void setupBackRow(int row, Color color) {
        boolean useFairyPieces = false;
        if (activityRef != null && activityRef.get() != null) {
            useFairyPieces = SettingsFragment.getFairyPiecesEnabled(activityRef.get());
        }

        PieceType[] pieceOrder = useFairyPieces ?
                getPieceOrderForBoardSize() :
                getStandardPieceOrderForBoardSize();

        for (int col = 0; col < boardSize; col++) {
            if (col < pieceOrder.length) {
                board[row][col] = new Piece(pieceOrder[col], color);
            } else {
                // Fill remaining columns with bishops if board is larger than standard setup
                board[row][col] = new Piece(PieceType.BISHOP, color);
            }
        }
    }

    private PieceType[] getStandardPieceOrderForBoardSize() {
        if (boardSize <= 6) {
            return new PieceType[]{
                    PieceType.ROOK, PieceType.KNIGHT, PieceType.KING,
                    PieceType.QUEEN, PieceType.KNIGHT, PieceType.ROOK
            };
        } else {
            return new PieceType[]{
                    PieceType.ROOK, PieceType.KNIGHT, PieceType.BISHOP,
                    PieceType.QUEEN, PieceType.KING, PieceType.BISHOP,
                    PieceType.KNIGHT, PieceType.ROOK
            };
        }
    }

    private PieceType[] getPieceOrderForBoardSize() {
        if (boardSize <= 6) {
            return new PieceType[]{
                    PieceType.ROOK, PieceType.KNIGHT, PieceType.KING,
                    PieceType.QUEEN, PieceType.KNIGHT, PieceType.ROOK
            };
        } else if (boardSize == 8) {
            return new PieceType[]{
                    PieceType.ROOK, PieceType.KNIGHT, PieceType.BISHOP,
                    PieceType.QUEEN, PieceType.KING, PieceType.BISHOP,
                    PieceType.KNIGHT, PieceType.ROOK
            };
        } else {
            // Extended setup for larger boards
            PieceType[] order = new PieceType[boardSize];
            int mid = boardSize / 2;

            // Fill outermost pieces
            order[0] = PieceType.ROOK;
            order[1] = PieceType.KNIGHT;
            order[2] = PieceType.BISHOP;
            order[boardSize - 3] = PieceType.BISHOP;
            order[boardSize - 2] = PieceType.KNIGHT;
            order[boardSize - 1] = PieceType.ROOK;

            // Fill middle with special pieces
            order[mid - 2] = PieceType.ARCHBISHOP;
            order[mid - 1] = PieceType.QUEEN;
            order[mid] = PieceType.KING;
            order[mid + 1] = PieceType.CHANCELLOR;

            // Fill remaining with bishops
            for (int i = 3; i < boardSize - 3; i++) {
                if (order[i] == null) {
                    order[i] = PieceType.BISHOP;
                }
            }

            return order;
        }
    }

    private void findKingPositions() {
        for (int row = 0; row < boardSize; row++) {
            for (int col = 0; col < boardSize; col++) {
                Piece piece = board[row][col];
                if (piece != null && piece.getPieceType() == PieceType.KING) {
                    if (piece.getColor() == Color.WHITE) {
                        whiteKingPos[0] = row;
                        whiteKingPos[1] = col;
                    } else {
                        blackKingPos[0] = row;
                        blackKingPos[1] = col;
                    }
                }
            }
        }
    }

    private boolean isValidCastling(int fromRow, int fromCol, int toCol) {
        Piece king = board[fromRow][fromCol];
        if (king == null || king.getPieceType() != PieceType.KING || king.isHasMoved()) {
            return false;
        }

        // Check if king is in check
        if (isKingInCheck(king.getColor())) {
            return false;
        }

        int direction = toCol > fromCol ? 1 : -1; // King-side or queen-side
        int rookCol = direction > 0 ? boardSize - 1 : 0;
        Piece rook = board[fromRow][rookCol];

        // Check rook exists and hasn't moved
        if (rook == null || rook.getPieceType() != PieceType.ROOK || rook.isHasMoved()) {
            return false;
        }

        // Check if squares between are empty
        int start = Math.min(fromCol, rookCol) + 1;
        int end = Math.max(fromCol, rookCol);
        for (int col = start; col < end; col++) {
            if (board[fromRow][col] != null) {
                return false;
            }
        }

        // Check if king moves through attacked squares
        for (int col = fromCol; col != toCol; col += direction) {
            if (wouldMoveLeaveKingInCheck(fromRow, fromCol, fromRow, col)) {
                return false;
            }
        }

        return true;
    }

    public boolean movePiece(int fromRow, int fromCol, int toRow, int toCol) {
        if (gameOver || !isValidMove(fromRow, fromCol, toRow, toCol)) {
            return false;
        }

        PlayActivity activity = activityRef.get();

        Piece piece = board[fromRow][fromCol];

        activity.playMoveSound();

        // Handle castling only if enabled
        if (castlingEnabled && piece.getPieceType() == PieceType.KING && Math.abs(fromCol - toCol) == 2) {
            // This is a castling move
            int direction = toCol > fromCol ? 1 : -1;
            int rookCol = direction > 0 ? boardSize - 1 : 0;
            int newRookCol = fromCol + direction;

            // Move the rook
            board[fromRow][newRookCol] = board[fromRow][rookCol];
            board[fromRow][rookCol] = null;
        }

        // Update castling rights when king or rook moves (only if castling is enabled)
        if (castlingEnabled) {
            if (piece.getPieceType() == PieceType.KING) {
                if (piece.getColor() == Color.WHITE) {
                    whiteKingSideCastle = false;
                    whiteQueenSideCastle = false;
                } else {
                    blackKingSideCastle = false;
                    blackQueenSideCastle = false;
                }
            } else if (piece.getPieceType() == PieceType.ROOK) {
                if (piece.getColor() == Color.WHITE) {
                    if (fromCol == 0) whiteQueenSideCastle = false;
                    if (fromCol == boardSize - 1) whiteKingSideCastle = false;
                } else {
                    if (fromCol == 0) blackQueenSideCastle = false;
                    if (fromCol == boardSize - 1) blackKingSideCastle = false;
                }
            }
        }

        // Check if the move captures a piece
        if (board[toRow][toCol] != null) {
            handleCapture(toRow, toCol);
            //capture sound
            activity.playCaptureSound();
        }

        // Move the piece
        board[toRow][toCol] = piece;
        board[fromRow][fromCol] = null;
        piece.setHasMoved(true);

        // Set en passant target only if en passant is enabled
        if (enPassantEnabled && piece.getPieceType() == PieceType.PAWN && Math.abs(fromRow - toRow) == 2) {
            enPassantTarget = new int[]{fromRow + (toRow - fromRow)/2, fromCol};
        } else {
            enPassantTarget = null;
        }

        // Check for pawn promotion only if promotion is enabled
        if (promotionEnabled && piece.getPieceType() == PieceType.PAWN &&
                (toRow == 0 || toRow == boardSize - 1)) {
            if (activity != null) {
                //promotion sound
                activity.playPromotionSound();
                activity.showPromotionDialog();
            }
            return true;
        }

        // Update king position if a king was moved
        if (piece.getPieceType() == PieceType.KING) {
            if (piece.getColor() == Color.WHITE) {
                whiteKingPos[0] = toRow;
                whiteKingPos[1] = toCol;
            } else {
                blackKingPos[0] = toRow;
                blackKingPos[1] = toCol;
            }
        }

        currentTurn = (currentTurn == Color.WHITE) ? Color.BLACK : Color.WHITE;

        if (!gameOver) {
            checkGameEnd();
        }

        if (wifiGameManager != null && wifiGameManager.isConnected() &&
                currentTurn == Color.WHITE) {
            wifiGameManager.sendMove(new Move(fromRow, fromCol, toRow, toCol));
        }

        if (activity != null && gameOver) {
            new Handler(Looper.getMainLooper()).post(activity::showGameOverDialog);
        }

        return true;
    }

    private boolean isValidMove(int fromRow, int fromCol, int toRow, int toCol) {
        Piece piece = board[fromRow][fromCol];

        // Check en passant only if enabled
        if (enPassantEnabled && piece.getPieceType() == PieceType.PAWN &&
                fromCol != toCol && board[toRow][toCol] == null) {
            return enPassantTarget != null &&
                    toRow == enPassantTarget[0] &&
                    toCol == enPassantTarget[1];
        }

        if (piece == null || piece.getColor() != currentTurn) {
            return false;
        }

        List<int[]> validMoves = piece.getValidMoves(fromRow, fromCol, board, this);
        for (int[] move : validMoves) {
            if (move[0] == toRow && move[1] == toCol) {
                // Check if this move would leave the king in check
                return !wouldMoveLeaveKingInCheck(fromRow, fromCol, toRow, toCol);
            }
        }

        if (castlingEnabled && piece.getPieceType() == PieceType.KING &&
                Math.abs(fromCol - toCol) == 2) {
            return isValidCastling(fromRow, fromCol, toCol);
        }

        return false;
    }

    private boolean wouldMoveLeaveKingInCheck(int fromRow, int fromCol, int toRow, int toCol) {
        // Simulate the move
        Piece movingPiece = board[fromRow][fromCol];
        Piece capturedPiece = board[toRow][toCol];

        board[toRow][toCol] = movingPiece;
        board[fromRow][fromCol] = null;

        // Update king position if king is being moved
        int[] originalKingPos = null;
        if (movingPiece.getPieceType() == PieceType.KING) {
            originalKingPos = movingPiece.getColor() == Color.WHITE ?
                    new int[]{whiteKingPos[0], whiteKingPos[1]} :
                    new int[]{blackKingPos[0], blackKingPos[1]};

            if (movingPiece.getColor() == Color.WHITE) {
                whiteKingPos[0] = toRow;
                whiteKingPos[1] = toCol;
            } else {
                blackKingPos[0] = toRow;
                blackKingPos[1] = toCol;
            }
        }

        boolean inCheck = isKingInCheck(currentTurn);

        // Restore the board
        board[fromRow][fromCol] = movingPiece;
        board[toRow][toCol] = capturedPiece;

        // Restore king position if it was moved
        if (originalKingPos != null) {
            if (movingPiece.getColor() == Color.WHITE) {
                whiteKingPos[0] = originalKingPos[0];
                whiteKingPos[1] = originalKingPos[1];
            } else {
                blackKingPos[0] = originalKingPos[0];
                blackKingPos[1] = originalKingPos[1];
            }
        }

        return inCheck;
    }

    private boolean isInsufficientMaterial() {
        int pieceCount = 0;
        boolean hasNonKingPieces = false;

        for (int row = 0; row < boardSize; row++) {
            for (int col = 0; col < boardSize; col++) {
                Piece piece = board[row][col];
                if (piece != null) {
                    pieceCount++;
                    if (piece.getPieceType() != PieceType.KING) {
                        hasNonKingPieces = true;

                        if (piece.getPieceType() == PieceType.PAWN ||
                                piece.getPieceType() == PieceType.ROOK ||
                                piece.getPieceType() == PieceType.QUEEN) {
                            return false;
                        }
                    }
                }
            }
        }

        if (!hasNonKingPieces) {
            return true;
        }

        return pieceCount == 3;
    }

    private void handleCapture(int row, int col) {
        Piece capturedPiece = board[row][col];
        if (capturedPiece != null) {
            capturedPieces.add(capturedPiece);
            board[row][col] = null;

            // Check if the captured piece is the king
            if (capturedPiece.getPieceType() == PieceType.KING) {
                gameOver = true;
                winner = currentTurn;
            }
        }
    }

    private void checkGameEnd() {
        if (gameOver) return; // Already game over

        if (isInsufficientMaterial()) {
            gameOver = true;
            winner = null;
            return;
        }

        // Check the current player's situation (the player who needs to move next)
        boolean currentPlayerInCheck = isKingInCheck(currentTurn);
        List<int[]> allValidMoves = getAllValidMovesForColor(currentTurn);

        if (allValidMoves.isEmpty()) {
            gameOver = true;
            if (currentPlayerInCheck) {
                // Checkmate - the player who just moved wins
                winner = (currentTurn == Color.WHITE) ? Color.BLACK : Color.WHITE;
            } else {
                // Stalemate - draw
                winner = null;
            }
        }
    }

    private boolean isKingInCheck(Color kingColor) {
        int[] kingPos = (kingColor == Color.WHITE) ? whiteKingPos : blackKingPos;
        Color enemyColor = (kingColor == Color.WHITE) ? Color.BLACK : Color.WHITE;

        // Check if any enemy piece can attack the king
        for (int row = 0; row < boardSize; row++) {
            for (int col = 0; col < boardSize; col++) {
                Piece piece = board[row][col];
                if (piece != null && piece.getColor() == enemyColor) {
                    List<int[]> moves = piece.getValidMoves(row, col, board, this);
                    for (int[] move : moves) {
                        if (move[0] == kingPos[0] && move[1] == kingPos[1]) {
                            return true;
                        }
                    }
                }
            }
        }
        return false;
    }

    public List<int[]> getAllValidMovesForColor(Color color) {
        List<int[]> allMoves = new ArrayList<>();

        for (int row = 0; row < boardSize; row++) {
            for (int col = 0; col < boardSize; col++) {
                Piece piece = board[row][col];
                if (piece != null && piece.getColor() == color) {
                    List<int[]> pieceMoves = piece.getValidMoves(row, col, board, this);
                    for (int[] move : pieceMoves) {
                        if (!wouldMoveLeaveKingInCheckForColor(row, col, move[0], move[1], color)) {
                            allMoves.add(new int[]{row, col, move[0], move[1]});
                        }
                    }
                }
            }
        }
        return allMoves;
    }

    private boolean wouldMoveLeaveKingInCheckForColor(int fromRow, int fromCol, int toRow, int toCol, Color color) {
        // Simulate the move
        Piece movingPiece = board[fromRow][fromCol];
        Piece capturedPiece = board[toRow][toCol];

        board[toRow][toCol] = movingPiece;
        board[fromRow][fromCol] = null;

        // Update king position if king is being moved
        int[] originalKingPos = null;
        if (movingPiece.getPieceType() == PieceType.KING) {
            originalKingPos = color == Color.WHITE ?
                    new int[]{whiteKingPos[0], whiteKingPos[1]} :
                    new int[]{blackKingPos[0], blackKingPos[1]};

            if (color == Color.WHITE) {
                whiteKingPos[0] = toRow;
                whiteKingPos[1] = toCol;
            } else {
                blackKingPos[0] = toRow;
                blackKingPos[1] = toCol;
            }
        }

        boolean inCheck = isKingInCheck(color);

        // Restore the board
        board[fromRow][fromCol] = movingPiece;
        board[toRow][toCol] = capturedPiece;

        // Restore king position if it was moved
        if (originalKingPos != null) {
            if (color == Color.WHITE) {
                whiteKingPos[0] = originalKingPos[0];
                whiteKingPos[1] = originalKingPos[1];
            } else {
                blackKingPos[0] = originalKingPos[0];
                blackKingPos[1] = originalKingPos[1];
            }
        }

        return inCheck;
    }

    public List<int[]> getValidMovesForPiece(int row, int col) {
        Piece piece = board[row][col];
        if (piece == null || piece.getColor() != currentTurn) {
            return new ArrayList<>();
        }

        List<int[]> validMoves = piece.getValidMoves(row, col, board, this);
        List<int[]> legalMoves = new ArrayList<>();

        for (int[] move : validMoves) {
            if (!wouldMoveLeaveKingInCheck(row, col, move[0], move[1])) {
                legalMoves.add(move);
            }
        }

        return legalMoves;
    }

    public String getGameStatus() {
        PlayActivity activity = activityRef.get();
        if (!gameOver) {
            boolean inCheck = isKingInCheck(currentTurn);
            if (inCheck) activity.playCheckSound();
            return (currentTurn == Color.WHITE ? "White" : "Black") + "'s turn" +
                    (inCheck ? " (Check!)" : "");
        } else {
            if (winner == null) {
                activity.playWinSound();
                if (isInsufficientMaterial()) {
                    return "Draw by insufficient material!";
                } else {
                    return "Game ended in a draw (Stalemate)!";
                }
            } else {
                if (aiMode){
                    //ai only consider win when white win
                    if (winner == Color.WHITE) activity.playWinSound();
                    else activity.playLoseSound();
                } else {
                    //pvp everyone win
                    activity.playWinSound();
                }
                return (winner == Color.WHITE ? "White" : "Black") + " wins by checkmate!";
            }
        }
    }

    @Override
    public void onPieceSelected(PieceType pieceType) {
        // Find the pawn that needs to be promoted
        for (int row = 0; row < boardSize; row++) {
            for (int col = 0; col < boardSize; col++) {
                Piece piece = board[row][col];
                if (piece != null && piece.getPieceType() == PieceType.PAWN &&
                        (row == 0 || row == boardSize - 1)) {
                    // Promote the pawn
                    piece.setPieceType(pieceType);

                    // Switch turns
                    currentTurn = (currentTurn == Color.WHITE) ? Color.BLACK : Color.WHITE;

                    // Update the board display
                    PlayActivity activity = activityRef.get();
                    if (activity != null) {
                        int tileSize = activity.getResources().getDisplayMetrics().widthPixels / boardSize;
                        activity.refreshBoardDisplay(tileSize);
                        activity.updateStatusText();

                        // If it's now AI's turn, let it move
                        if (aiMode && currentTurn == Color.BLACK) {
                            makeAIMoveIfNeeded();
                        }
                    }

                    checkGameEnd();
                    return;
                }
            }
        }
    }

    public void setAIMode(boolean aiMode, Difficulty difficulty) {
        this.aiMode = aiMode;
        if (aiMode) {
            this.chessAI = new ChessAI(this, difficulty);
        } else {
            this.chessAI = null;
        }
    }

    public void makeAIMoveIfNeeded() {
        if (aiMode && chessAI != null && currentTurn == Color.BLACK && !gameOver) {
            List<int[]> validMoves = getAllValidMovesForColor(Color.BLACK);
            if (validMoves.isEmpty()) {
                // AI has no valid moves - this should trigger checkmate/stalemate
                checkGameEnd();
            } else {
                chessAI.makeMove();
            }

            PlayActivity activity = activityRef.get();
            if (activity != null) {
                int tileSize = activity.getResources().getDisplayMetrics().widthPixels / boardSize;
                activity.refreshBoardDisplay(tileSize);
                activity.updateStatusText();
                if (gameOver) {
                    new Handler(Looper.getMainLooper()).post(activity::showGameOverDialog);
                }
            }
        }
    }

    @Override
    public void onMoveReceived(Move move) {
        new Handler(Looper.getMainLooper()).post(() -> {
            movePiece(move.getFromRow(), move.getFromCol(), move.getToRow(), move.getToCol());
            PlayActivity activity = activityRef.get();
            if (activity != null) {
                int tileSize = activity.getResources().getDisplayMetrics().widthPixels / boardSize;
                activity.refreshBoardDisplay(tileSize);
                activity.updateStatusText();

                if (gameOver) {
                    activity.showGameOverDialog();
                }
            }
        });
    }

    // Add these new methods to support custom board loading
    public void clearBoard() {
        for (int row = 0; row < boardSize; row++) {
            for (int col = 0; col < boardSize; col++) {
                board[row][col] = null;
            }
        }
        capturedPieces.clear();
        gameOver = false;
        winner = null;
        currentTurn = Color.WHITE;
    }

    public void loadBoardState(BoardState boardState) {
        clearBoard();
        this.boardSize = boardState.getBoardSize();
        this.board = new Piece[boardSize][boardSize];

        for (BoardState.PiecePosition piecePos : boardState.getPieces()) {
            addPiece(piecePos.getRow(), piecePos.getCol(), piecePos.getType(), piecePos.getColor());
        }

        findKingPositions();
        updateGameState();
    }

    public void addPiece(int row, int col, PieceType type, Color color) {
        if (row >= 0 && row < boardSize && col >= 0 && col < boardSize) {
            board[row][col] = new Piece(type, color);
            if (type == PieceType.KING) {
                if (color == Color.WHITE) {
                    whiteKingPos[0] = row;
                    whiteKingPos[1] = col;
                } else {
                    blackKingPos[0] = row;
                    blackKingPos[1] = col;
                }
            }
        }
    }

    public void updateGameState() {
        // Check if any kings are missing
        boolean whiteKingExists = false;
        boolean blackKingExists = false;

        for (int row = 0; row < boardSize; row++) {
            for (int col = 0; col < boardSize; col++) {
                Piece piece = board[row][col];
                if (piece != null && piece.getPieceType() == PieceType.KING) {
                    if (piece.getColor() == Color.WHITE) {
                        whiteKingExists = true;
                    } else {
                        blackKingExists = true;
                    }
                }
            }
        }

        if (!whiteKingExists || !blackKingExists) {
            gameOver = true;
            winner = whiteKingExists ? Color.WHITE : Color.BLACK;
        }
        // Add check for insufficient material
        else if (isInsufficientMaterial()) {
            gameOver = true;
            winner = null;
        }
        else {
            checkGameEnd();
        }
    }

    @Override
    public void onConnectionEstablished() {

    }

    @Override
    public void onConnectionLost() {
        PlayActivity activity = activityRef.get();
        if (activity != null) {
            activity.runOnUiThread(() -> {
                Toast.makeText(activity, "Connection lost. Returning to lobby.", Toast.LENGTH_SHORT).show();
                activity.finish(); // Return to the previous activity
            });
        }
    }

    @Override
    public void onError(String message) {
        PlayActivity activity = activityRef.get();
        if (activity != null) {
            activity.runOnUiThread(() ->
                    Toast.makeText(activity, "Error: " + message, Toast.LENGTH_SHORT).show()
            );
        }
    }

    @Override
    public void onBoardSizeReceived(int boardSize) {
        this.boardSize = boardSize;
        this.board = new Piece[boardSize][boardSize];
        initializeBoard(); // Reinitialize the board

        PlayActivity activity = activityRef.get();
        if (activity != null) {
            activity.runOnUiThread(() -> {
                int tileSize = activity.getResources().getDisplayMetrics().widthPixels / boardSize;
                activity.refreshBoardDisplay(tileSize);
            });
        }
    }

    @Override
    public void onSettingsReceived(int receivedBoardSize, boolean fairyPieces,
                                   boolean enPassant, boolean promotion, boolean castling, boolean river) {
        // Update board size if changed
        if (this.boardSize != receivedBoardSize) {
            this.boardSize = receivedBoardSize;
            this.board = new Piece[boardSize][boardSize];
            initializeBoard();
        }

        setSpecialRules(promotion, enPassant, castling, river);

        PlayActivity activity = activityRef.get();
        if (activity != null) {
            activity.runOnUiThread(() -> {
                int tileSize = activity.getResources().getDisplayMetrics().widthPixels / boardSize;
                activity.refreshBoardDisplay(tileSize);
                activity.updateStatusText();
            });
        }
    }

    public void setSpecialRules(boolean promotion, boolean enPassant, boolean castling, boolean riverEnabled) {
        this.promotionEnabled = promotion;
        this.enPassantEnabled = enPassant;
        this.castlingEnabled = castling;
        this.riverEnabled = riverEnabled;

        // If castling is disabled, mark all rooks and kings as having moved
        if (!castling) {
            for (int row = 0; row < boardSize; row++) {
                for (int col = 0; col < boardSize; col++) {
                    Piece piece = board[row][col];
                    if (piece != null &&
                            (piece.getPieceType() == PieceType.KING ||
                                    piece.getPieceType() == PieceType.ROOK)) {
                        piece.setHasMoved(true);
                    }
                }
            }
            whiteKingSideCastle = false;
            whiteQueenSideCastle = false;
            blackKingSideCastle = false;
            blackQueenSideCastle = false;
        }

        // If en passant is disabled, clear any existing en passant target
        if (!enPassant) {
            enPassantTarget = null;
        }
    }

    public void setupWiFiConnection(String hostAddress, boolean isHost) {
        if (wifiGameManager == null) {
            // Get current settings
            boolean fairyPieces = SettingsFragment.getFairyPiecesEnabled(activityRef.get());
            boolean enPassant = SettingsFragment.getEnPassantEnabled(activityRef.get());
            boolean promotion = SettingsFragment.getPromotionEnabled(activityRef.get());
            boolean castling = SettingsFragment.getCastlingEnabled(activityRef.get());

            wifiGameManager = new WiFiGameManager(this, isHost,
                    fairyPieces, enPassant, promotion, castling);
            wifiGameManager.startConnection(hostAddress, boardSize);
        }
    }

    public boolean isWiFiConnected() {
        return wifiGameManager != null && wifiGameManager.isConnected();
    }

    public void disconnectWiFi() {
        if (wifiGameManager != null) {
            wifiGameManager.disconnect();
            wifiGameManager = null;
        }
    }

    public void handleNoValidMoves() {
        if (!gameOver) {
            checkGameEnd();

            PlayActivity activity = activityRef.get();
            if (activity != null && gameOver) {
                new Handler(Looper.getMainLooper()).post(() -> {
                    activity.updateStatusText();
                    activity.showGameOverDialog();
                });
            }
        }
    }

    // Getters
    public Piece[][] getBoard() { return board; }
    public Color getCurrentTurn() { return currentTurn; }
    public void setCurrentTurn(Color turn){
        currentTurn = turn;
    }
    public boolean isGameOver() { return gameOver; }
    public Color getWinner() { return winner; }
    public List<Piece> getCapturedPieces() { return capturedPieces; }
    public int getBoardSize() { return boardSize; }
}