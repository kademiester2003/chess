package chess;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
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
        return switch (type) {
            case BISHOP -> getBishopMoves(board, myPosition);
            case ROOK -> getRookMoves(board, myPosition);
            case QUEEN -> getQueenMoves(board, myPosition);
            case KNIGHT -> getKnightMoves(board, myPosition);
            case KING -> getKingMoves(board, myPosition);
            case PAWN -> getPawnMoves(board, myPosition);
        };
    }

    private Collection<ChessMove> getBishopMoves(ChessBoard board, ChessPosition pos) {
        return lineMoves(board, pos, List.of(
                new int[]{1, 1}, new int[]{-1, 1}, new int[]{1, -1}, new int[]{-1, -1}
        ));
    }

    private Collection<ChessMove> getRookMoves(ChessBoard board, ChessPosition pos) {
        return lineMoves(board, pos, List.of(
                new int[]{1, 0}, new int[]{-1, 0}, new int[]{0, 1}, new int[]{0, -1}
        ));
    }

    private Collection<ChessMove> getQueenMoves(ChessBoard board, ChessPosition pos) {
        Collection<ChessMove> moves = new ArrayList<>();
        moves.addAll(getBishopMoves(board, pos));
        moves.addAll(getRookMoves(board, pos));
        return moves;
    }

    private Collection<ChessMove> getKnightMoves(ChessBoard board, ChessPosition pos) {
        Collection<ChessMove> moves = new ArrayList<>();
        int[][] offsets = {
                {2, 1}, {2, -1}, {-2, 1}, {-2, -1},
                {1, 2}, {-1, 2}, {1, -2}, {-1, -2}
        };

        for (int[] offset : offsets) {
            addMoveIfValid(board, pos, pos.offset(offset[0], offset[1]), moves);
        }

        return moves;
    }

    private Collection<ChessMove> getKingMoves(ChessBoard board, ChessPosition pos) {
        Collection<ChessMove> moves = new ArrayList<>();
        for (int dRow = -1; dRow <= 1; dRow++) {
            for (int dCol = -1; dCol <= 1; dCol++) {
                if (dRow == 0 && dCol == 0) {
                    continue;
                }
                addMoveIfValid(board, pos, pos.offset(dRow, dCol), moves);
            }
        }
        return moves;
    }

    private Collection<ChessMove> getPawnMoves(ChessBoard board, ChessPosition pos) {
        Collection<ChessMove> moves = new ArrayList<>();
        int row = pos.getRow();
        int forward = (pieceColor == ChessGame.TeamColor.WHITE) ? 1 : -1;

        // Single forward
        ChessPosition oneForward = pos.offset(forward, 0);
        if (isInsideBoard(oneForward) && board.getPiece(oneForward) == null) {
            addPawnMove(pos, oneForward, moves);
            // Double forward
            ChessPosition twoForward = pos.offset(2 * forward, 0);
            if (isPawnStartingRow(row) && board.getPiece(twoForward) == null) {
                moves.add(new ChessMove(pos, twoForward, null));
            }
        }

        // Diagonal captures
        for (int dc : new int[]{-1, 1}) {
            ChessPosition diag = pos.offset(forward, dc);
            if (!isInsideBoard(diag)) {
                continue;
            }
            ChessPiece target = board.getPiece(diag);
            if (target != null && target.getTeamColor() != pieceColor) {
                addPawnMove(pos, diag, moves);
            }
        }

        return moves;
    }

    // Helper Methods

    private boolean isPawnStartingRow(int row) {
        return (pieceColor == ChessGame.TeamColor.WHITE && row == 2)
                || (pieceColor == ChessGame.TeamColor.BLACK && row == 7);
    }

    private boolean isInsideBoard(ChessPosition pos) {
        return pos != null && pos.getRow() >= 1 && pos.getRow() <= 8
                && pos.getColumn() >= 1 && pos.getColumn() <= 8;
    }

    /**
     * Adds a move to the list if it’s within the board and not blocked by a same-color piece.
     */
    private void addMoveIfValid(ChessBoard board, ChessPosition start, ChessPosition end, Collection<ChessMove> moves) {
        if (!isInsideBoard(end)) {
            return;
        }
        ChessPiece target = board.getPiece(end);
        if (target == null || target.getTeamColor() != pieceColor) {
            moves.add(new ChessMove(start, end, null));
        }
    }

    /**
     * Handles pawn promotions automatically.
     */
    private void addPawnMove(ChessPosition start, ChessPosition end, Collection<ChessMove> moves) {
        int promotionRow = (pieceColor == ChessGame.TeamColor.WHITE) ? 8 : 1;
        if (end.getRow() == promotionRow) {
            for (PieceType promo : List.of(PieceType.QUEEN, PieceType.ROOK, PieceType.BISHOP, PieceType.KNIGHT)) {
                moves.add(new ChessMove(start, end, promo));
            }
        } else {
            moves.add(new ChessMove(start, end, null));
        }
    }

    /**
     * Handles sliding pieces (Bishop, Rook, Queen).
     */
    private Collection<ChessMove> lineMoves(ChessBoard board, ChessPosition start, List<int[]> directions) {
        Collection<ChessMove> moves = new ArrayList<>();
        for (int[] dir : directions) {
            int dRow = dir[0], dCol = dir[1];
            int row = start.getRow() + dRow;
            int col = start.getColumn() + dCol;

            while (isInsideBoard(new ChessPosition(row, col))) {
                ChessPosition pos = new ChessPosition(row, col);
                ChessPiece target = board.getPiece(pos);
                if (target == null) {
                    moves.add(new ChessMove(start, pos, null));
                } else {
                    if (target.getTeamColor() != pieceColor) {
                        moves.add(new ChessMove(start, pos, null));
                    }
                    break;
                }
                row += dRow;
                col += dCol;
            }
        }
        return moves;
    }
}
