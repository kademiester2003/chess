package websocket.commands;

import chess.ChessPosition;

import java.util.Objects;

public class MakeMoveCommand {
    private final CommandType commandType;
    private final String authToken;
    private final Integer gameID;
    public final Move move;

    public MakeMoveCommand(CommandType commandType, String authToken, Integer gameID, Move move) {
        this.commandType = commandType;
        this.authToken = authToken;
        this.gameID = gameID;
        this.move = move;
    }

    public enum CommandType { CONNECT, MAKE_MOVE, LEAVE, RESIGN }

    public CommandType getCommandType() { return commandType; }
    public String getAuthToken() { return authToken; }
    public Integer getGameID() { return gameID; }
    public Move getMove() { return move; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof MakeMoveCommand that)) return false;
        return getCommandType() == that.getCommandType() &&
                Objects.equals(getAuthToken(), that.getAuthToken()) &&
                Objects.equals(getGameID(), that.getGameID()) &&
                Objects.equals(getMove(), that.getMove());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getCommandType(), getAuthToken(), getGameID(), getMove());
    }

    public static class Move {
        public ChessPosition start;
        public ChessPosition end;
        public String promotion;

        public String toReadable() {
            if (promotion != null) {
                return start + " -> " + end + " promote=" + promotion;
            }
            return start + " -> " + end;
        }
    }
}
