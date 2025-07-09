package vn.edu.fpt.gameproject.model;

public class Move {
    private int fromRow;
    private int fromCol;
    private int toRow;
    private int toCol;

    public Move(int fromRow, int fromCol, int toRow, int toCol) {
        this.fromRow = fromRow;
        this.fromCol = fromCol;
        this.toRow = toRow;
        this.toCol = toCol;
    }

    // Getters
    public int getFromRow() { return fromRow; }
    public int getFromCol() { return fromCol; }
    public int getToRow() { return toRow; }
    public int getToCol() { return toCol; }

    // Serialization to string
    @Override
    public String toString() {
        return fromRow + "," + fromCol + "," + toRow + "," + toCol;
    }

    // Deserialization from string
    public static Move fromString(String moveStr) {
        String[] parts = moveStr.split(",");
        if (parts.length != 4) {
            throw new IllegalArgumentException("Invalid move string format");
        }
        return new Move(
                Integer.parseInt(parts[0]),
                Integer.parseInt(parts[1]),
                Integer.parseInt(parts[2]),
                Integer.parseInt(parts[3])
        );
    }
}