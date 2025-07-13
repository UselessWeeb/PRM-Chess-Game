package vn.edu.fpt.gameproject.model;

import java.util.ArrayList;
import java.util.List;

import vn.edu.fpt.gameproject.manager.GameManager;

public class Piece {
    PieceType type;
    Color color;
    boolean hasMoved = false; // Track if piece has moved (important for pawns, kings, rooks)

    public Piece(PieceType type, Color color) {
        this.type = type;
        this.color = color;
    }

    public PieceType getPieceType() {
        return type;
    }

    public void setPieceType(PieceType type) {
        this.type = type;
    }

    public Color getColor() {
        return color;
    }

    public void setColor(Color color) {
        this.color = color;
    }

    public boolean isHasMoved() {
        return hasMoved;
    }

    public void setHasMoved(boolean hasMoved) {
        this.hasMoved = hasMoved;
    }

    public List<int[]> getValidMoves(int row, int col, Piece[][] board, GameManager gameManager) {
        List<int[]> moves = new ArrayList<>();

        switch (this.type) {
            case PAWN:
                getPawnMoves(row, col, board, moves, gameManager);
                break;
            case ROOK:
                getRookMoves(row, col, board, moves, gameManager);
                break;
            case KNIGHT:
                getKnightMoves(row, col, board, moves, gameManager);
                break;
            case BISHOP:
                getBishopMoves(row, col, board, moves, gameManager);
                break;
            case QUEEN:
                getQueenMoves(row, col, board, moves, gameManager);
                break;
            case KING:
                getKingMoves(row, col, board, moves, gameManager);
                break;
            case ARCHBISHOP:
                getArchbishopMoves(row, col, board, moves, gameManager);
                break;
            case CHANCELLOR:
                getChancellorMoves(row, col, board, moves, gameManager);
                break;
        }

        return moves;
    }

    private void getPawnMoves(int row, int col, Piece[][] board, List<int[]> moves, GameManager gameManager) {
        int direction = (color == Color.WHITE) ? -1 : 1;
        int startRow = (color == Color.WHITE) ? board.length - 2 : 1;

        // Forward move
        if (isValidSquare(row + direction, col, board) && board[row + direction][col] == null) {
            if (!isRiverCrossing(row, col, row + direction, col, gameManager)) {
                moves.add(new int[]{row + direction, col});
            }

            // Double move from starting position
            if (row == startRow && board[row + 2*direction][col] == null) {
                if (!isRiverCrossing(row, col, row + 2*direction, col, gameManager)) {
                    moves.add(new int[]{row + 2*direction, col});
                }
            }
        }

        // Captures
        int[] captureCols = {col - 1, col + 1};
        for (int captureCol : captureCols) {
            if (isValidSquare(row + direction, captureCol, board)) {
                Piece target = board[row + direction][captureCol];
                if (target != null && target.color != this.color) {
                    if (!isRiverCrossing(row, col, row + direction, captureCol, gameManager)) {
                        moves.add(new int[]{row + direction, captureCol});
                    }
                }
            }
        }

        // TODO: Implement en passant
    }

    private void getRookMoves(int row, int col, Piece[][] board, List<int[]> moves, GameManager gameManager) {
        int[][] directions = {{-1, 0}, {1, 0}, {0, -1}, {0, 1}}; // Up, Down, Left, Right

        for (int[] dir : directions) {
            for (int i = 1; i < board.length; i++) {
                int newRow = row + dir[0] * i;
                int newCol = col + dir[1] * i;

                if (!isValidSquare(newRow, newCol, board)) break;

                // Check if this move crosses the river
                if (isRiverCrossing(row, col, newRow, newCol, gameManager)) {
                    break;
                }

                Piece target = board[newRow][newCol];
                if (target == null) {
                    moves.add(new int[]{newRow, newCol});
                } else {
                    if (target.color != this.color) {
                        moves.add(new int[]{newRow, newCol});
                    }
                    break;
                }
            }
        }
    }

    private void getKnightMoves(int row, int col, Piece[][] board, List<int[]> moves, GameManager gameManager) {
        int[][] knightMoves = {
                {-2, -1}, {-2, 1}, {-1, -2}, {-1, 2},
                {1, -2}, {1, 2}, {2, -1}, {2, 1}
        };

        for (int[] move : knightMoves) {
            int newRow = row + move[0];
            int newCol = col + move[1];

            if (isValidSquare(newRow, newCol, board)) {
                if (!isRiverCrossing(row, col, newRow, newCol, gameManager)) {
                    Piece target = board[newRow][newCol];
                    if (target == null || target.color != this.color) {
                        moves.add(new int[]{newRow, newCol});
                    }
                }
            }
        }
    }

    private void getBishopMoves(int row, int col, Piece[][] board, List<int[]> moves, GameManager gameManager) {
        int[][] directions = {{-1, -1}, {-1, 1}, {1, -1}, {1, 1}}; // Diagonal directions

        for (int[] dir : directions) {
            for (int i = 1; i < board.length; i++) {
                int newRow = row + dir[0] * i;
                int newCol = col + dir[1] * i;

                if (!isValidSquare(newRow, newCol, board)) break;

                // Check if this move crosses the river
                if (isRiverCrossing(row, col, newRow, newCol, gameManager)) {
                    break;
                }

                Piece target = board[newRow][newCol];
                if (target == null) {
                    moves.add(new int[]{newRow, newCol});
                } else {
                    if (target.color != this.color) {
                        moves.add(new int[]{newRow, newCol});
                    }
                    break;
                }
            }
        }
    }

    private void getQueenMoves(int row, int col, Piece[][] board, List<int[]> moves, GameManager gameManager) {
        // Queen combines rook and bishop moves
        getRookMoves(row, col, board, moves, gameManager);
        getBishopMoves(row, col, board, moves, gameManager);
    }

    private void getKingMoves(int row, int col, Piece[][] board, List<int[]> moves, GameManager gameManager) {
        int[][] kingMoves = {
                {-1, -1}, {-1, 0}, {-1, 1},
                {0, -1},           {0, 1},
                {1, -1},  {1, 0}, {1, 1}
        };

        for (int[] move : kingMoves) {
            int newRow = row + move[0];
            int newCol = col + move[1];

            if (isValidSquare(newRow, newCol, board)) {
                if (!isRiverCrossing(row, col, newRow, newCol, gameManager)) {
                    Piece target = board[newRow][newCol];
                    if (target == null || target.color != this.color) {
                        moves.add(new int[]{newRow, newCol});
                    }
                }
            }
        }

        // TODO: Implement castling
    }

    private void getArchbishopMoves(int row, int col, Piece[][] board, List<int[]> moves, GameManager gameManager) {
        // Archbishop = Bishop + Knight
        getBishopMoves(row, col, board, moves, gameManager);
        getKnightMoves(row, col, board, moves, gameManager);
    }

    private void getChancellorMoves(int row, int col, Piece[][] board, List<int[]> moves, GameManager gameManager) {
        // Chancellor = Rook + Knight
        getRookMoves(row, col, board, moves, gameManager);
        getKnightMoves(row, col, board, moves, gameManager);
    }

    private boolean isValidSquare(int row, int col, Piece[][] board) {
        return row >= 0 && row < board.length && col >= 0 && col < board[0].length;
    }

    private boolean isRiverCrossing(int fromRow, int fromCol, int toRow, int toCol, GameManager gameManager) {
        if (!gameManager.isRiverEnabled() || gameManager.canCrossRiver(this.type)) {
            return false;
        }

        return gameManager.isRiverBetween(fromRow, toRow);
    }
}