package websocket.messages;

import websocket.GameDTO;

public class LoadGameMessage extends ServerMessage{
    public GameDTO game;

    public LoadGameMessage(GameDTO game) {
        super(ServerMessageType.LOAD_GAME);
        this.game = game;
    }

    public GameDTO getGame() { return game; }
}
