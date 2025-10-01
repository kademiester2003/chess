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

        //if no piece, return null
        if (piece == null) {
            return null;
        }

        //Get list of valid moves
        Collection<ChessMove> moves = piece.pieceMoves(board, startPosition);

        //Check for moves that leave the King in check.
        //If move does not leave in King in check, add to this new list that will be returned
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
        //ensure move is among the valid moves for this piece
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

        //Check that the move is valid (in Valid moves and is correct team turn)
        ChessPiece piece = board.getPiece(move.getStartPosition());
        if (moveIsInBoard && piece.getTeamColor() == teamTurn) {
            ChessPosition startPosition = move.getStartPosition();
            ChessPosition endPosition = move.getEndPosition();
            if (move.getPromotionPiece() == null) {
                board.addPiece(endPosition, piece);
            }
            else {
                board.addPiece(endPosition, new ChessPiece(piece.getTeamColor(), move.getPromotionPiece()));
            }
            board.addPiece(startPosition, null);

            //set team turn
            if (piece.getTeamColor() == TeamColor.WHITE) {
                setTeamTurn(TeamColor.BLACK);
            }
            else {
                setTeamTurn(TeamColor.WHITE);
            }
        }

        //error handling
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
        //new function. Takes in piece and returns the position of the first of its piece it finds
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
        //get King's position for this team
        ChessPosition kingPosition = findPosition(new ChessPiece(teamColor, ChessPiece.PieceType.KING));

        //iterate through the board
        for (int i = 1; i <= 8; i++) {
            for (int j = 1; j <= 8; j++) {
                ChessPosition position = new ChessPosition(i, j);

                //if piece at space on board is on opposing side, iterate through valid moves
                if (board.getPiece(position) != null && board.getPiece(position).getTeamColor() != teamColor) {
                    Collection<ChessMove> validMoves = board.getPiece(position).pieceMoves(board, position);
                    for (ChessMove validMove : validMoves) {

                        //if the valid move can hit the king, the king is in check
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
        //get King's position
        ChessPiece king = new ChessPiece(teamColor, ChessPiece.PieceType.KING);
        ChessPosition kingPosition = findPosition(king);

        //iterate through board
        for (int i = 1; i <= 8; i++) {
            for (int j = 1; j <= 8; j++) {
                ChessPosition position = new ChessPosition(i, j);
                ChessPiece piece = board.getPiece(position);

                //if piece is on team
                if (piece != null && piece.getTeamColor() == teamColor) {
                    Collection<ChessMove> validMoves = board.getPiece(position).pieceMoves(board, position);

                    //iterate through possible moves for piece
                    for (ChessMove move : validMoves) {
                        ChessPosition endPosition = move.getEndPosition();
                        ChessPiece endPiece = board.getPiece(endPosition);
                        board.addPiece(endPosition, piece);
                        board.addPiece(position, null);

                        //if piece can prevent King from being in check, team is not in checkmate
                        if (!isInCheck(teamColor)) {
                            return false;
                        }
                        board.addPiece(endPosition, endPiece);
                        board.addPiece(position, piece);
                    }
                }
            }
        }
        //if King doesn't have any valid moves (or above part didn't call false), then team is in Checkmate
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
        //if team is in Check, cannot be in stalemate
        if (isInCheck(teamColor)) {
            return false;
        }

        //iterate through board
        for (int i = 1; i <= 8; i++) {
            for (int j = 1; j <= 8; j++) {
                ChessPosition position = new ChessPosition(i, j);
                ChessPiece piece = board.getPiece(position);

                //if piece is on team, continue
                if (piece != null && piece.getTeamColor() == teamColor) {
                    Collection<ChessMove> validMoves = validMoves(position);

                    //if piece can make a move, team is not in stalemate
                    if (!validMoves.isEmpty()) {
                        return false;
                    }
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
