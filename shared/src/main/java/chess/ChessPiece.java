package chess;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Objects;

/**
 * Represents a single chess piece
 * <p>
 * Note: You can add to this class, but you may not alter
 * signature of the existing methods.
 */
public class ChessPiece {

    private final ChessGame.TeamColor pieceColor;
    private final ChessPiece.PieceType type;

    @Override
    public String toString() {
        return "ChessPiece{" +
                "pieceColor=" + pieceColor +
                ", type=" + type +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        ChessPiece that = (ChessPiece) o;
        return pieceColor == that.pieceColor && type == that.type;
    }

    @Override
    public int hashCode() {
        return Objects.hash(pieceColor, type);
    }

    public ChessPiece(ChessGame.TeamColor pieceColor, ChessPiece.PieceType type) {
        this.pieceColor = pieceColor;
        this.type = type;
    }

    /**
     * The various different chess piece options
     */
    public enum PieceType {
        KING,
        QUEEN,
        BISHOP,
        KNIGHT,
        ROOK,
        PAWN
    }

    /**
     * @return Which team this chess piece belongs to
     */
    public ChessGame.TeamColor getTeamColor() {
        return pieceColor;
    }

    /**
     * @return which type of chess piece this piece is
     */
    public PieceType getPieceType() {
        return type;
    }

    /**
     * Calculates all the positions a chess piece can move to
     * Does not take into account moves that are illegal due to leaving the king in
     * danger
     *
     * @return Collection of valid moves
     */
    public Collection<ChessMove> pieceMoves(ChessBoard board, ChessPosition myPosition) {
        //throw new RuntimeException("Not implemented");
        Collection<ChessMove> validMoves = new ArrayList<>();
        PieceType piece = getPieceType();
        int row = myPosition.getRow();
        int col = myPosition.getColumn();

        if (piece == PieceType.BISHOP) {
            for (int i = 1; i <= 8; i++) {
                for (int j = 1; j <= 8; j++) {
                    ChessPosition endPosition = new ChessPosition(i, j);
                    if (Math.abs(row - i) == Math.abs(col - j) && row != i && col != j) {
                        if (board.getPiece(endPosition) == null || board.getPiece(endPosition).getTeamColor() != pieceColor) {
                                ChessMove move = new ChessMove(myPosition, endPosition, null);
                                validMoves.add(move);
                        }
                    }
                }
            }
        }

        if (piece == PieceType.KNIGHT) {
            for (int i = 1; i <= 8; i++) {
                for (int j = 1; j <= 8; j++) {
                    ChessPosition endPosition = new ChessPosition(i, j);
                    if ((Math.abs(row - i) == 2 && Math.abs(col - j) == 1) || (Math.abs(row - i) == 1 && Math.abs(col - j) == 2)) {
                        if (board.getPiece(endPosition) == null || board.getPiece(endPosition).getTeamColor() != pieceColor) {
                            ChessMove move = new ChessMove(myPosition, endPosition, null);
                            validMoves.add(move);
                        }
                    }
                }
            }
        }

        return validMoves;
    }
}
