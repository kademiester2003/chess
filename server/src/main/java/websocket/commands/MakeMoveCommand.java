package websocket.commands;

import java.util.Objects;

public class MakeMoveCommand {
    private final CommandType commandType;
    private final String authToken;
    private final Integer gameID;
    private final ChessMoveDto move;

    public MakeMoveCommand(CommandType commandType, String authToken, Integer gameID, ChessMoveDto move) {
        this.commandType = commandType;
        this.authToken = authToken;
        this.gameID = gameID;
        this.move = move;
    }

    public enum CommandType { CONNECT, MAKE_MOVE, LEAVE, RESIGN }

    public CommandType getCommandType() { return commandType; }
    public String getAuthToken() { return authToken; }
    public Integer getGameID() { return gameID; }
    public ChessMoveDto getMove() { return move; }

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

    public static class ChessMoveDto {
        public Square start;
        public Square end;
        public String promotion;

        public static class Square {
            public int row;
            public int col;
        }

        public String toReadable() {
            return squareToAlg(start) + " -> " + squareToAlg(end) + (promotion != null ? " promote=" + promotion : "");
        }

        private static String squareToAlg(Square s) {
            if (s == null) {
                return "?";
            }
            char file = (char) ('a' + s.col);
            int rank = 1 + s.row;
            return "" + file + rank;
        }
    }
}
