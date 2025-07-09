package vn.edu.fpt.gameproject.model;

import java.util.Date;
import java.util.List;

public class BoardState {
    private String name;
    private int boardSize;
    private List<PiecePosition> pieces;
    private Date createdAt;

    private Color currentTurn;

    public static class PiecePosition {
        private int row;
        private int col;
        private PieceType type;
        private Color color;

        public int getRow() {
            return row;
        }

        public void setRow(int row) {
            this.row = row;
        }

        public int getCol() {
            return col;
        }

        public void setCol(int col) {
            this.col = col;
        }

        public PieceType getType() {
            return type;
        }

        public void setType(PieceType type) {
            this.type = type;
        }

        public Color getColor() {
            return color;
        }

        public void setColor(Color color) {
            this.color = color;
        }
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getBoardSize() {
        return boardSize;
    }

    public void setBoardSize(int boardSize) {
        this.boardSize = boardSize;
    }

    public List<PiecePosition> getPieces() {
        return pieces;
    }

    public void setPieces(List<PiecePosition> pieces) {
        this.pieces = pieces;
    }

    public Date getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Date createdAt) {
        this.createdAt = createdAt;
    }

    public Color getCurrentTurn() {
        return currentTurn;
    }

    public void setCurrentTurn(Color currentTurn) {
        this.currentTurn = currentTurn;
    }
}