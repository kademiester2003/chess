package chess;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Objects;

/**
 * For a class that can manage a chess game, making moves on a board.
 * Note: You can add to this class, but you may not alter
 * the signature of existing methods.
 */
public class ChessGame {

    private TeamColor teamTurn = TeamColor.WHITE;
    private ChessBoard board;

    public ChessGame() {
        board = new ChessBoard();
        board.resetBoard();
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        ChessGame chessGame = (ChessGame) o;
        return teamTurn == chessGame.teamTurn && Objects.deepEquals(board, chessGame.board);
    }

    @Override
    public int hashCode() {
        return Objects.hash(teamTurn, board);
    }

    /**
     * @return Which team's turn it is
     */
    public TeamColor getTeamTurn() {
        return teamTurn;
    }

    /**
     * Sets which team's turn it is
     *
     * @param team the team whose turn it is
     */
    public void setTeamTurn(TeamColor team) {
        teamTurn = team;
    }

    /**
     * Enum identifying the 2 possible teams in a chess game
     */
    public enum TeamColor {
        WHITE,
        BLACK
    }

    /**
     * Gets valid moves for a piece at the given location
     *
     * @param startPosition the piece to get valid moves for
     * @return Set of valid moves for requested piece, or null if no piece at startPosition
     */
    public Collection<ChessMove> validMoves(ChessPosition startPosition) {
        ChessPiece piece = board.getPiece(startPosition);
        if (piece == null) {
            return null;
        }

        Collection<ChessMove> possibleMoves = piece.pieceMoves(board, startPosition);
        Collection<ChessMove> validMoves = new ArrayList<>();

        for (ChessMove move : possibleMoves) {
            if (movePreservesCheckSafety(piece, move)) {
                validMoves.add(move);
            }
        }
        return validMoves;
    }

    private boolean movePreservesCheckSafety(ChessPiece piece, ChessMove move) {
        ChessPosition start = move.getStartPosition();
        ChessPosition end = move.getEndPosition();
        ChessPiece capturedPiece = board.getPiece(end);

        board.addPiece(end, piece);
        board.addPiece(start, null);

        boolean safe = !isInCheck(piece.getTeamColor());

        board.addPiece(end, capturedPiece);
        board.addPiece(start, piece);
        return safe;
    }

    /**
     * Makes a move in a chess game
     *
     * @param move chess move to perform
     * @throws InvalidMoveException if move is invalid
     */
    public void makeMove(ChessMove move) throws InvalidMoveException {
        boolean moveIsInBoard = false;
        Collection<ChessMove> validMoves = validMoves(move.getStartPosition());
        if (validMoves != null) {
            for (ChessMove validMove : validMoves) {
                if (validMove.equals(move)) {
                    moveIsInBoard = true;
                    break;
                }
            }
        }

        ChessPiece piece = board.getPiece(move.getStartPosition());
        if (moveIsInBoard && piece.getTeamColor() == teamTurn) {
            executeMove(move, piece);
            toggleTeamTurn(piece.getTeamColor());
        } else {
            if (!moveIsInBoard) {
                throw new InvalidMoveException("Invalid move. Move not in valid moves");
            } else {
                throw new InvalidMoveException("Invalid move. Not correct team turn");
            }
        }
    }

    private void executeMove(ChessMove move, ChessPiece piece) {
        ChessPosition startPosition = move.getStartPosition();
        ChessPosition endPosition = move.getEndPosition();

        if (move.getPromotionPiece() == null) {
            board.addPiece(endPosition, piece);
        } else {
            board.addPiece(endPosition, new ChessPiece(piece.getTeamColor(), move.getPromotionPiece()));
        }
        board.addPiece(startPosition, null);
    }

    private void toggleTeamTurn(TeamColor currentTeam) {
        if (currentTeam == TeamColor.WHITE) {
            setTeamTurn(TeamColor.BLACK);
        } else {
            setTeamTurn(TeamColor.WHITE);
        }
    }

    public ChessPosition findPosition(ChessPiece piece) {
        for (int i = 1; i <= 8; i++) {
            for (int j = 1; j <= 8; j++) {
                ChessPosition position = new ChessPosition(i, j);
                ChessPiece current = board.getPiece(position);
                if (current != null && current.equals(piece)) {
                    return position;
                }
            }
        }
        return null;
    }

    /**
     * Determines if the given team is in check
     *
     * @param teamColor which team to check for check
     * @return True if the specified team is in check
     */
    public boolean isInCheck(TeamColor teamColor) {
        ChessPosition kingPosition = findPosition(new ChessPiece(teamColor, ChessPiece.PieceType.KING));
        if (kingPosition == null) {
            return false;
        }

        for (int i = 1; i <= 8; i++) {
            if (isRowThreateningKing(i, teamColor, kingPosition)) {
                return true;
            }
        }
        return false;
    }

    private boolean isRowThreateningKing(int row, TeamColor teamColor, ChessPosition kingPosition) {
        for (int col = 1; col <= 8; col++) {
            ChessPosition pos = new ChessPosition(row, col);
            ChessPiece piece = board.getPiece(pos);
            if (piece == null || piece.getTeamColor() == teamColor) {
                continue;
            }

            if (canPieceAttackKing(piece, pos, kingPosition)) {
                return true;
            }
        }
        return false;
    }

    private boolean canPieceAttackKing(ChessPiece piece, ChessPosition pos, ChessPosition kingPos) {
        for (ChessMove move : piece.pieceMoves(board, pos)) {
            if (move.getEndPosition().equals(kingPos)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Determines if the given team is in checkmate
     *
     * @param teamColor which team to check for checkmate
     * @return True if the specified team is in checkmate
     */
    public boolean isInCheckmate(TeamColor teamColor) {
        ChessPiece king = new ChessPiece(teamColor, ChessPiece.PieceType.KING);
        ChessPosition kingPosition = findPosition(king);

        if (!isInCheck(teamColor)) {
            return false;
        }

        for (int i = 1; i <= 8; i++) {
            if (!rowLeadsToCheckmate(i, teamColor)) {
                return false;
            }
        }

        return true;
    }

    private boolean rowLeadsToCheckmate(int row, TeamColor teamColor) {
        for (int col = 1; col <= 8; col++) {
            ChessPosition pos = new ChessPosition(row, col);
            ChessPiece piece = board.getPiece(pos);

            if (piece == null || piece.getTeamColor() != teamColor) {
                continue;
            }
            if (pieceCanPreventCheck(teamColor, piece, pos)) {
                return false;
            }
        }
        return true;
    }

    private boolean pieceCanPreventCheck(TeamColor teamColor, ChessPiece piece, ChessPosition pos) {
        for (ChessMove move : piece.pieceMoves(board, pos)) {
            ChessPosition end = move.getEndPosition();
            ChessPiece captured = board.getPiece(end);

            board.addPiece(end, piece);
            board.addPiece(pos, null);

            boolean stillInCheck = isInCheck(teamColor);

            board.addPiece(end, captured);
            board.addPiece(pos, piece);

            if (!stillInCheck) {
                return true;
            }
        }
        return false;
    }

    /**
     * Determines if the given team is in stalemate (no valid moves but not in check)
     *
     * @param teamColor which team to check for stalemate
     * @return True if the specified team is in stalemate
     */
    public boolean isInStalemate(TeamColor teamColor) {
        if (isInCheck(teamColor)) {
            return false;
        }

        for (int i = 1; i <= 8; i++) {
            for (int j = 1; j <= 8; j++) {
                ChessPosition position = new ChessPosition(i, j);
                ChessPiece piece = board.getPiece(position);

                if (piece == null || piece.getTeamColor() != teamColor) {
                    continue;
                }

                Collection<ChessMove> moves = validMoves(position);
                if (moves != null && !moves.isEmpty()) {
                    return false;
                }
            }
        }
        return true;
    }

    /**
     * Sets this game's chessboard with a given board
     *
     * @param board the new board to use
     */
    public void setBoard(ChessBoard board) {
        this.board = board;
    }

    /**
     * Gets the current chessboard
     *
     * @return the chessboard
     */
    public ChessBoard getBoard() {
        return board;
    }

    public static void main(String[] args) {
        ChessGame game = new ChessGame();
        ChessBoard board = game.getBoard();
        System.out.println(board.toString());
    }
}
