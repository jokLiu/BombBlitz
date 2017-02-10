package bomber.networking;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

/**
 * 
 * Methods for encoding and decoding more complex packets for the client
 *
 */
public class ClientPacketEncoder {

	/**
	 * Decode List of ClientServerPlayer from bytes in MSG_S_LOBBY_PLAYERLIST
	 * format. The first three bytes in the destination byte array are reserved
	 * for message type and sequence number and will be ignored. The caller
	 * should ensure the first three bytes are correct before calling this
	 * method.
	 * 
	 * @param src
	 *            the source byte array
	 * @param length
	 *            the length of the data in the byte array
	 * @return a list of ClientServerPlayer
	 * @throws IOException
	 */
	public static List<ClientServerPlayer> decodePlayerList(byte[] src, int length) throws IOException {
		if (src == null) {
			throw new IOException("src is null");
		}

		if (length < 1 + 2 + 4 + 4 + 4 + 4 || length > src.length) {
			throw new IOException("length is invalid");
		}

		ByteBuffer buffer = ByteBuffer.wrap(src, 0, length);
		buffer.position(3);

		// int totalPlayers = buffer.getInt();
		// int packetIndex = buffer.getInt();
		// int maxIndex = buffer.getInt();
		buffer.position(15);
		int numPlayers = buffer.getInt();

		// System.out.printf("%d %d %d %d\n", totalPlayers, packetIndex,
		// maxIndex, numPlayers);

		buffer.position(19);
		List<ClientServerPlayer> playerList = new ArrayList<ClientServerPlayer>();
		for (int i = 0; i < numPlayers; i++) {
			if (length < buffer.position() + 4) {
				throw new IOException("packet format is invalid");
			}
			int id = buffer.getInt();

			if (length < buffer.position() + 1) {
				throw new IOException("packet format is invalid");
			}
			byte nameLength = buffer.get();

			if (nameLength < 1 || length < buffer.position() + nameLength) {
				throw new IOException("packet format is invalid");
			}
			byte[] nameData = new byte[nameLength];
			buffer.get(nameData);
			String name = new String(nameData, 0, nameLength, "UTF-8");

			playerList.add(new ClientServerPlayer(id, name));
		}

		if (length != buffer.position()) {
			throw new IOException("packet format is invalid");
		}

		return playerList;
	}

	/**
	 * Decode List of ClientServerRoom from bytes in MSG_S_LOBBY_ROOMLIST
	 * format. The first three bytes in the destination byte array are reserved
	 * for message type and sequence number and will be ignored. The caller
	 * should ensure the first three bytes are correct before calling this
	 * method.
	 * 
	 * @param src
	 *            the source byte array
	 * @param length
	 *            the length of the data in the byte array
	 * @return a list of ClientServerRoom
	 * @throws IOException
	 */
	public static List<ClientServerLobbyRoom> decodeRoomList(byte[] src, int length) throws IOException {
		if (src == null) {
			throw new IOException("src is null");
		}

		if (length < 1 + 2 + 4 + 4 + 4 + 4 || length > src.length) {
			throw new IOException("length is invalid");
		}

		ByteBuffer buffer = ByteBuffer.wrap(src, 0, length);
		buffer.position(3);

		// int totalPlayers = buffer.getInt();
		// int packetIndex = buffer.getInt();
		// int maxIndex = buffer.getInt();
		buffer.position(15);
		int numRooms = buffer.getInt();

		// System.out.printf("%d %d %d %d\n", totalPlayers, packetIndex,
		// maxIndex, numPlayers);

		buffer.position(19);
		List<ClientServerLobbyRoom> roomList = new ArrayList<ClientServerLobbyRoom>();
		for (int i = 0; i < numRooms; i++) {
			// get room id
			if (length < buffer.position() + 4) {
				throw new IOException("packet format is invalid");
			}
			int id = buffer.getInt();

			// get room name length
			if (length < buffer.position() + 1) {
				throw new IOException("packet format is invalid");
			}
			byte nameLength = buffer.get();

			// get room name
			if (nameLength < 1 || length < buffer.position() + nameLength) {
				throw new IOException("packet format is invalid");
			}
			byte[] nameData = new byte[nameLength];
			buffer.get(nameData);
			String name = new String(nameData, 0, nameLength, "UTF-8");

			// get player number
			if (length < buffer.position() + 1) {
				throw new IOException("packet format is invalid");
			}
			byte playerNumber = buffer.get();

			// get max player limit
			if (length < buffer.position() + 1) {
				throw new IOException("packet format is invalid");
			}
			byte maxPlayer = buffer.get();

			// get inGame boolean flag
			if (length < buffer.position() + 1) {
				throw new IOException("packet format is invalid");
			}
			boolean inGame = false;
			if (buffer.get() == 1) {
				inGame = true;
			}

			// get game map ID
			if (length < buffer.position() + 4) {
				throw new IOException("packet format is invalid");
			}
			int mapID = buffer.getInt();

			roomList.add(new ClientServerLobbyRoom(id, name, playerNumber, maxPlayer, inGame, mapID));
		}

		if (length != buffer.position()) {
			throw new IOException("packet format is invalid");
		}

		return roomList;
	}

}