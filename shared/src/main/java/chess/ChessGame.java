package chess;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Objects;

/**
 * For a class that can manage a chess game, making moves on a board
 * <p>
 * Note: You can add to this class, but you may not alter
 * signature of the existing methods.
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
     * Set's which teams turn it is
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
     * Gets a valid moves for a piece at the given location
     *
     * @param startPosition the piece to get valid moves for
     * @return Set of valid moves for requested piece, or null if no piece at
     * startPosition
     */
    public Collection<ChessMove> validMoves(ChessPosition startPosition) {
        ChessPiece piece = board.getPiece(startPosition);
        if (piece == null) {
            return null;
        }
        Collection<ChessMove> moves = piece.pieceMoves(board, startPosition);
        Collection<ChessMove> validMoves = new ArrayList<>();
        for (ChessMove move : moves) {
            ChessPosition newStartPosition = move.getStartPosition();
            ChessPosition newEndPosition = move.getEndPosition();
            ChessPiece newPiece = board.getPiece(newEndPosition);
            board.addPiece(newEndPosition, piece);
            board.addPiece(newStartPosition, null);
            if (!isInCheck(piece.getTeamColor())) {
                validMoves.add(move);
            }
            board.addPiece(newEndPosition, newPiece);
            board.addPiece(newStartPosition, piece);
        }
        return validMoves;
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
                //System.out.println(validMove.toString());
                if (validMove.equals(move)) {
                    moveIsInBoard = true;
                    break;
                }
            }
        }
        ChessPiece piece = board.getPiece(move.getStartPosition());
        if (moveIsInBoard && piece.getTeamColor() == teamTurn) {
            ChessPosition startPosition = move.getStartPosition();
            ChessPosition endPosition = move.getEndPosition();
            board.addPiece(endPosition, piece);
            board.addPiece(startPosition, null);
            if (piece.getTeamColor() == TeamColor.WHITE) {
                setTeamTurn(TeamColor.BLACK);
            }
            else {
                setTeamTurn(TeamColor.WHITE);
            }
        }
        else {
            if (!moveIsInBoard) {
                throw new InvalidMoveException("Invalid move. Move not in valid moves");
            }
            else {
                throw new InvalidMoveException("Invalid move. Not correct team turn");
            }
        }
    }

    public ChessPosition findPosition(ChessPiece piece) {
        for (int i = 1; i <= 8; i++) {
            for (int j = 1; j <= 8; j++) {
                ChessPosition position = new ChessPosition(i, j);
                if (board.getPiece(position) != null && board.getPiece(position).equals(piece)) {
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
        for (int i = 1; i <= 8; i++) {
            for (int j = 1; j <= 8; j++) {
                ChessPosition position = new ChessPosition(i, j);
                if (board.getPiece(position) != null && board.getPiece(position).getTeamColor() != teamColor) {
                    Collection<ChessMove> validMoves = board.getPiece(position).pieceMoves(board, position);
                    for (ChessMove validMove : validMoves) {
                        if (validMove.getEndPosition().equals(kingPosition)) {
                            return true;
                        }
                    }
                }
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
        for (int i = 1; i <= 8; i++) {
            for (int j = 1; j <= 8; j++) {
                ChessPosition position = new ChessPosition(i, j);
                ChessPiece piece = board.getPiece(position);
                if (piece != null && piece.getTeamColor() == teamColor) {
                    Collection<ChessMove> validMoves = board.getPiece(position).pieceMoves(board, position);
                    for (ChessMove move : validMoves) {
                        ChessPosition endPosition = move.getEndPosition();
                        ChessPiece endPiece = board.getPiece(endPosition);
                        board.addPiece(endPosition, piece);
                        board.addPiece(position, null);
                        if (!isInCheck(teamColor)) {
                            return false;
                        }
                        board.addPiece(endPosition, endPiece);
                        board.addPiece(position, piece);
                    }
                }
            }
        }
        return validMoves(kingPosition) != null;
    }

    /**
     * Determines if the given team is in stalemate, which here is defined as having
     * no valid moves while not in check.
     *
     * @param teamColor which team to check for stalemate
     * @return True if the specified team is in stalemate, otherwise false
     */
    public boolean isInStalemate(TeamColor teamColor) {
        throw new RuntimeException("Not implemented");
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
