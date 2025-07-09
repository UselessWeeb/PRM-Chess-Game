package vn.edu.fpt.gameproject.utils;

import java.util.List;
import java.util.Random;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import vn.edu.fpt.gameproject.manager.GameManager;
import vn.edu.fpt.gameproject.model.Color;
import vn.edu.fpt.gameproject.model.Difficulty;
import vn.edu.fpt.gameproject.model.Piece;
import vn.edu.fpt.gameproject.model.PieceType;

public class ChessAI {

    private Difficulty difficulty;
    private GameManager gameManager;
    private Random random;
    private int movesSinceLastCapture = 0;
    private int[] lastMove = null;

    public ChessAI(GameManager gameManager, Difficulty difficulty) {
        this.gameManager = gameManager;
        this.difficulty = difficulty;
        this.random = new Random();
    }

    public void makeMove() {
        if (gameManager.getCurrentTurn() != Color.BLACK || gameManager.isGameOver()) {
            return;
        }

        List<int[]> allMoves = gameManager.getAllValidMovesForColor(Color.BLACK);
        if (allMoves.isEmpty()) {
            gameManager.handleNoValidMoves();
            return;
        }

        int[] selectedMove = selectMove(allMoves, Color.BLACK);
        if (selectedMove != null) {
            // Check if this move captures a piece to reset repetition counter
            Piece target = gameManager.getBoard()[selectedMove[2]][selectedMove[3]];
            if (target != null) {
                movesSinceLastCapture = 0;
            } else {
                movesSinceLastCapture++;
            }

            lastMove = selectedMove.clone();
            gameManager.movePiece(selectedMove[0], selectedMove[1], selectedMove[2], selectedMove[3]);
        }
    }

    private int[] selectMove(List<int[]> moves, Color color) {
        switch (difficulty) {
            case EASY:
                return selectRandomMove(moves);
            case MEDIUM:
                return selectMediumMove(moves, color);
            case HARD:
                return selectBestMove(moves, color);
            default:
                return selectRandomMove(moves);
        }
    }

    private int[] selectRandomMove(List<int[]> moves) {
        return moves.get(random.nextInt(moves.size()));
    }

    private int[] selectMediumMove(List<int[]> moves, Color color) {
        // Check for immediate checkmate
        int[] checkmateMove = findCheckmateMove(moves, color);
        if (checkmateMove != null) {
            return checkmateMove;
        }

        // First, avoid moves that lose pieces
        List<int[]> safeMoves = new ArrayList<>();
        for (int[] move : moves) {
            if (!isMoveDangerous(move, color)) {
                safeMoves.add(move);
            }
        }

        // If no safe moves, use all moves
        if (safeMoves.isEmpty()) {
            safeMoves = moves;
        }

        // Prefer captures from safe moves
        for (int[] move : safeMoves) {
            Piece target = gameManager.getBoard()[move[2]][move[3]];
            if (target != null && target.getColor() != color) {
                return move; // Safe capture
            }
        }

        return selectRandomMove(safeMoves);
    }

    private int[] selectBestMove(List<int[]> moves, Color color) {
        // Check for immediate checkmate first
        int[] checkmateMove = findCheckmateMove(moves, color);
        if (checkmateMove != null) {
            return checkmateMove;
        }

        // Check for check moves if no checkmate
        int[] checkMove = findCheckMove(moves, color);

        // Evaluate all moves and sort by score
        List<MoveScore> moveScores = new ArrayList<>();
        for (int[] move : moves) {
            int score = evaluateMove(move, color);
            moveScores.add(new MoveScore(move, score));
        }

        // Sort moves by score (highest first)
        Collections.sort(moveScores, new Comparator<MoveScore>() {
            @Override
            public int compare(MoveScore a, MoveScore b) {
                return Integer.compare(b.score, a.score);
            }
        });

        // In endgame, prioritize moves that make progress
        if (isEndgame()) {
            int[] endgameMove = selectEndgameMove(moveScores, color);
            if (endgameMove != null) {
                return endgameMove;
            }
        }

        // If we have a good check move and it's in top moves, prefer it
        if (checkMove != null) {
            for (MoveScore ms : moveScores) {
                if (java.util.Arrays.equals(ms.move, checkMove) && ms.score > moveScores.get(0).score - 20) {
                    return checkMove;
                }
            }
        }

        return moveScores.get(0).move;
    }

    private int[] findCheckmateMove(List<int[]> moves, Color color) {
        for (int[] move : moves) {
            if (isCheckmateMove(move, color)) {
                return move;
            }
        }
        return null;
    }

    private int[] findCheckMove(List<int[]> moves, Color color) {
        for (int[] move : moves) {
            if (doesMoveGiveCheck(move, color)) {
                return move;
            }
        }
        return null;
    }

    private boolean isCheckmateMove(int[] move, Color color) {
        // Simulate the move
        Piece[][] board = gameManager.getBoard();
        Piece movingPiece = board[move[0]][move[1]];
        Piece originalTarget = board[move[2]][move[3]];

        // Make the move
        board[move[2]][move[3]] = movingPiece;
        board[move[0]][move[1]] = null;

        Color opponentColor = (color == Color.WHITE) ? Color.BLACK : Color.WHITE;
        boolean isCheckmate = isKingInCheck(opponentColor) &&
                gameManager.getAllValidMovesForColor(opponentColor).isEmpty();

        // Restore board
        board[move[0]][move[1]] = movingPiece;
        board[move[2]][move[3]] = originalTarget;

        return isCheckmate;
    }

    private boolean doesMoveGiveCheck(int[] move, Color color) {
        // Simulate the move
        Piece[][] board = gameManager.getBoard();
        Piece movingPiece = board[move[0]][move[1]];
        Piece originalTarget = board[move[2]][move[3]];

        // Make the move
        board[move[2]][move[3]] = movingPiece;
        board[move[0]][move[1]] = null;

        Color opponentColor = (color == Color.WHITE) ? Color.BLACK : Color.WHITE;
        boolean givesCheck = isKingInCheck(opponentColor);

        // Restore board
        board[move[0]][move[1]] = movingPiece;
        board[move[2]][move[3]] = originalTarget;

        return givesCheck;
    }

    private boolean isKingInCheck(Color color) {
        // Find king position
        Piece[][] board = gameManager.getBoard();
        for (int row = 0; row < gameManager.getBoardSize(); row++) {
            for (int col = 0; col < gameManager.getBoardSize(); col++) {
                Piece piece = board[row][col];
                if (piece != null && piece.getPieceType() == PieceType.KING && piece.getColor() == color) {
                    return isPieceUnderAttack(row, col, color);
                }
            }
        }
        return false;
    }

    private int[] selectEndgameMove(List<MoveScore> moveScores, Color color) {
        Color opponentColor = (color == Color.WHITE) ? Color.BLACK : Color.WHITE;

        // Find opponent king position
        int[] opponentKingPos = findKingPosition(opponentColor);
        if (opponentKingPos == null) return null;

        // Avoid repetitive moves
        List<MoveScore> nonRepetitiveMoves = new ArrayList<>();
        for (MoveScore ms : moveScores) {
            if (!isRepetitiveMove(ms.move)) {
                nonRepetitiveMoves.add(ms);
            }
        }

        if (!nonRepetitiveMoves.isEmpty()) {
            // Prefer moves that restrict opponent king's mobility
            for (MoveScore ms : nonRepetitiveMoves.subList(0, Math.min(3, nonRepetitiveMoves.size()))) {
                if (restrictsKingMobility(ms.move, opponentKingPos, color)) {
                    return ms.move;
                }
            }
            return nonRepetitiveMoves.get(0).move;
        }

        // If all moves are repetitive, pick the best scoring non-repetitive one
        return moveScores.get(0).move;
    }

    private boolean isRepetitiveMove(int[] move) {
        if (lastMove == null) return false;

        // Check if this move undoes the last move
        return (move[0] == lastMove[2] && move[1] == lastMove[3] &&
                move[2] == lastMove[0] && move[3] == lastMove[1]);
    }

    private boolean restrictsKingMobility(int[] move, int[] kingPos, Color color) {
        // Check if the move reduces the number of squares the king can move to
        Piece[][] board = gameManager.getBoard();
        Piece movingPiece = board[move[0]][move[1]];
        Piece originalTarget = board[move[2]][move[3]];

        // Count king's mobility before move
        int mobilityBefore = countKingMobility(kingPos, (color == Color.WHITE) ? Color.BLACK : Color.WHITE);

        // Make the move
        board[move[2]][move[3]] = movingPiece;
        board[move[0]][move[1]] = null;

        // Count king's mobility after move
        int mobilityAfter = countKingMobility(kingPos, (color == Color.WHITE) ? Color.BLACK : Color.WHITE);

        // Restore board
        board[move[0]][move[1]] = movingPiece;
        board[move[2]][move[3]] = originalTarget;

        return mobilityAfter < mobilityBefore;
    }

    private int countKingMobility(int[] kingPos, Color kingColor) {
        int mobility = 0;
        int[][] directions = {{-1,-1}, {-1,0}, {-1,1}, {0,-1}, {0,1}, {1,-1}, {1,0}, {1,1}};

        for (int[] dir : directions) {
            int newRow = kingPos[0] + dir[0];
            int newCol = kingPos[1] + dir[1];

            if (newRow >= 0 && newRow < gameManager.getBoardSize() &&
                    newCol >= 0 && newCol < gameManager.getBoardSize()) {

                Piece piece = gameManager.getBoard()[newRow][newCol];
                if (piece == null || piece.getColor() != kingColor) {
                    // Check if this square is safe
                    if (!isPieceUnderAttack(newRow, newCol, kingColor)) {
                        mobility++;
                    }
                }
            }
        }

        return mobility;
    }

    private int[] findKingPosition(Color color) {
        Piece[][] board = gameManager.getBoard();
        for (int row = 0; row < gameManager.getBoardSize(); row++) {
            for (int col = 0; col < gameManager.getBoardSize(); col++) {
                Piece piece = board[row][col];
                if (piece != null && piece.getPieceType() == PieceType.KING && piece.getColor() == color) {
                    return new int[]{row, col};
                }
            }
        }
        return null;
    }

    private boolean isEndgame() {
        int totalPieces = 0;
        Piece[][] board = gameManager.getBoard();

        for (int row = 0; row < gameManager.getBoardSize(); row++) {
            for (int col = 0; col < gameManager.getBoardSize(); col++) {
                if (board[row][col] != null) {
                    totalPieces++;
                }
            }
        }

        return totalPieces <= 8; // Endgame when 8 or fewer pieces remain
    }

    private int evaluateMove(int[] move, Color color) {
        int score = 0;
        Piece[][] board = gameManager.getBoard();
        Piece movingPiece = board[move[0]][move[1]];
        Piece targetPiece = board[move[2]][move[3]];

        // Heavily reward checkmate
        if (isCheckmateMove(move, color)) {
            return 10000;
        }

        // Reward check moves
        if (doesMoveGiveCheck(move, color)) {
            score += 100;
        }

        // Material gain/loss
        if (targetPiece != null && targetPiece.getColor() != color) {
            score += getPieceValue(targetPiece.getPieceType());
        }

        // Piece safety - heavily penalize moves that lose pieces
        if (isMoveDangerous(move, color)) {
            score -= getPieceValue(movingPiece.getPieceType()) * 2;
        }

        // Endgame specific evaluation
        if (isEndgame()) {
            score += evaluateEndgameMove(move, color);
        } else {
            // Opening/middlegame evaluation
            score += evaluateOpeningMiddlegameMove(move, color);
        }

        // Penalize repetitive moves heavily
        if (isRepetitiveMove(move)) {
            score -= 50;
        }

        // Encourage progress (avoid stalemate situations)
        if (movesSinceLastCapture > 10) {
            score += evaluateMoveProgress(move, color);
        }

        return score;
    }

    private int evaluateEndgameMove(int[] move, Color color) {
        int score = 0;
        Color opponentColor = (color == Color.WHITE) ? Color.BLACK : Color.WHITE;
        int[] opponentKingPos = findKingPosition(opponentColor);

        if (opponentKingPos != null) {
            // Drive opponent king to edge/corner
            int centerX = gameManager.getBoardSize() / 2;
            int centerY = gameManager.getBoardSize() / 2;
            int distanceFromCenter = Math.abs(opponentKingPos[0] - centerX) + Math.abs(opponentKingPos[1] - centerY);
            score += distanceFromCenter * 10;

            // Bring our pieces closer to opponent king
            int distanceToKing = Math.abs(move[2] - opponentKingPos[0]) + Math.abs(move[3] - opponentKingPos[1]);
            score += (gameManager.getBoardSize() - distanceToKing) * 5;

            // Restrict opponent king mobility
            if (restrictsKingMobility(move, opponentKingPos, color)) {
                score += 30;
            }
        }

        return score;
    }

    private int evaluateOpeningMiddlegameMove(int[] move, Color color) {
        int score = 0;
        Piece movingPiece = gameManager.getBoard()[move[0]][move[1]];

        // Piece development (only in opening)
        if (!movingPiece.isHasMoved() && isOpeningPhase()) {
            score += 15;
        }

        // Center control
        int centerX = gameManager.getBoardSize() / 2;
        int centerY = gameManager.getBoardSize() / 2;
        int distanceFromCenter = Math.abs(move[2] - centerX) + Math.abs(move[3] - centerY);
        score += (gameManager.getBoardSize() - distanceFromCenter) * 2;

        // King safety
        if (movingPiece.getPieceType() == PieceType.KING) {
            if (isOpeningPhase()) {
                score -= 20; // Strongly discourage moving king in opening
            } else {
                score -= 5; // Less penalty in middlegame
            }
        }

        return score;
    }

    private int evaluateMoveProgress(int[] move, Color color) {
        int score = 0;

        // Reward moves that advance pieces
        if (color == Color.BLACK) {
            score += (move[2] - move[0]) * 2; // Moving down the board
        } else {
            score += (move[0] - move[2]) * 2; // Moving up the board
        }

        // Reward aggressive moves
        Color opponentColor = (color == Color.WHITE) ? Color.BLACK : Color.WHITE;
        int[] opponentKingPos = findKingPosition(opponentColor);
        if (opponentKingPos != null) {
            int oldDistance = Math.abs(move[0] - opponentKingPos[0]) + Math.abs(move[1] - opponentKingPos[1]);
            int newDistance = Math.abs(move[2] - opponentKingPos[0]) + Math.abs(move[3] - opponentKingPos[1]);
            if (newDistance < oldDistance) {
                score += 10;
            }
        }

        return score;
    }

    private boolean isMoveDangerous(int[] move, Color color) {
        // Simulate the move and check if the piece is under attack
        Piece[][] board = gameManager.getBoard();
        Piece movingPiece = board[move[0]][move[1]];
        Piece originalTarget = board[move[2]][move[3]];

        // Simulate move
        board[move[2]][move[3]] = movingPiece;
        board[move[0]][move[1]] = null;

        // Check if the piece is under attack in new position
        boolean isDangerous = isPieceUnderAttack(move[2], move[3], color);

        // Restore board
        board[move[0]][move[1]] = movingPiece;
        board[move[2]][move[3]] = originalTarget;

        return isDangerous;
    }

    private boolean isPieceUnderAttack(int row, int col, Color color) {
        Color opponentColor = (color == Color.WHITE) ? Color.BLACK : Color.WHITE;
        List<int[]> opponentMoves = gameManager.getAllValidMovesForColor(opponentColor);

        for (int[] move : opponentMoves) {
            if (move[2] == row && move[3] == col) {
                return true;
            }
        }
        return false;
    }

    private boolean isOpeningPhase() {
        // Simple heuristic: if many pieces haven't moved, it's opening
        int unmovedPieces = 0;
        Piece[][] board = gameManager.getBoard();

        for (int row = 0; row < gameManager.getBoardSize(); row++) {
            for (int col = 0; col < gameManager.getBoardSize(); col++) {
                Piece piece = board[row][col];
                if (piece != null && !piece.isHasMoved()) {
                    unmovedPieces++;
                }
            }
        }

        return unmovedPieces > (gameManager.getBoardSize() * 2); // Arbitrary threshold
    }

    private boolean isKingInDanger(Color color) {
        return isKingInCheck(color);
    }

    private boolean doesMoveProtectKing(int[] move, Color color) {
        // Simulate the move and check if king is still in check
        Piece[][] board = gameManager.getBoard();
        Piece movingPiece = board[move[0]][move[1]];
        Piece originalTarget = board[move[2]][move[3]];

        // Make the move
        board[move[2]][move[3]] = movingPiece;
        board[move[0]][move[1]] = null;

        boolean kingStillInDanger = isKingInCheck(color);

        // Restore board
        board[move[0]][move[1]] = movingPiece;
        board[move[2]][move[3]] = originalTarget;

        return !kingStillInDanger;
    }

    private boolean doesMoveAttackKing(int[] move, Color color) {
        return doesMoveGiveCheck(move, color);
    }

    public int[] getSuggestedMove() {
        if (gameManager.getCurrentTurn() != Color.WHITE || gameManager.isGameOver()) {
            return null;
        }

        List<int[]> allMoves = gameManager.getAllValidMovesForColor(Color.WHITE);
        if (allMoves.isEmpty()) {
            return null;
        }

        return selectMove(allMoves, Color.WHITE);
    }

    private int getPieceValue(PieceType type) {
        switch (type) {
            case PAWN: return 10;
            case KNIGHT: return 30;
            case BISHOP: return 30;
            case ROOK: return 50;
            case QUEEN: return 90;
            case KING: return 1000;
            case ARCHBISHOP: return 35; // Slightly better than bishop
            case CHANCELLOR: return 55; // Slightly better than rook
            default: return 0;
        }
    }

    // Helper class to store move with its evaluation score
    private static class MoveScore {
        int[] move;
        int score;

        MoveScore(int[] move, int score) {
            this.move = move;
            this.score = score;
        }
    }
}