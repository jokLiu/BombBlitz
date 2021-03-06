package test.networking;

import java.io.IOException;
import java.net.SocketException;
import java.util.List;
import java.util.Random;
import java.util.Scanner;

import bomber.game.Block;
import bomber.game.KeyboardState;
import bomber.game.Map;
import bomber.networking.ClientServerLobbyRoom;
import bomber.networking.ClientServerPlayer;
import bomber.networking.ClientThread;
import bomber.networking.ServerPacketEncoder;
import bomber.networking.ClientPacketEncoder;

public class TestClientMain {

	public static void main(String[] args) {
		String hostname = null;
		int port = -1;
		if (args.length >= 2) {
			hostname = args[0];
			port = Integer.parseInt(args[1]);
		} else {
			System.out.println("Usage: <hostname> <port>");
			System.exit(1);
		}

		ClientThread client = null;
		try {
			client = new ClientThread(hostname, port);
		} catch (SocketException e) {
			System.out.println(e);
			System.exit(1);
		}

		TestClientNetListener netListener = new TestClientNetListener(client);
		client.addNetListener(netListener);

		Thread clientThread = new Thread(client);
		clientThread.start();

		Scanner scanner = new Scanner(System.in);
		while (scanner.hasNextLine()) {
			String line = scanner.nextLine();
			String[] cmds = line.split("\\s+");
			try {
				if (cmds.length < 1) {

					pInvalid();

				} else if (cmds.length == 1) {

					if (cmds[0].equals("exit")) {

						break;

					} else if (cmds[0].equals("isconnected")) {

						System.out.println(client.isConnected());

					} else if (cmds[0].equals("disconnect")) {

						client.disconnect();

					} else if (cmds[0].equals("id") || cmds[0].equals("clientid")) {

						System.out.println("clientID: " + client.getClientID());

					} else if (cmds[0].equals("upl") || cmds[0].equals("updateplayerlist")) {

						client.updatePlayerList();

					} else if (cmds[0].equals("players")) {

						List<ClientServerPlayer> playerList = client.getPlayerList();
						System.out.printf("Size: %d\n", playerList.size());
						for (ClientServerPlayer p : playerList) {
							System.out.printf("ID: %d, Name: %s, inRoom: %b", p.getID(), p.getName(), p.isInRoom());
							if (p.isInRoom()) {
								System.out.printf(", Room ID: %d\n", p.getRoomID());
							} else {
								System.out.println();
							}
						}

					} else if (cmds[0].equals("url") || cmds[0].equals("updateroomlist")) {

						client.updateRoomList();

					} else if (cmds[0].equals("rooms")) {

						List<ClientServerLobbyRoom> roomList = client.getRoomList();
						System.out.printf("Size: %d\n", roomList.size());
						for (ClientServerLobbyRoom r : roomList) {
							System.out.printf(
									"ID: %d, Name: %s, Number of players: %d, Max players: %d, inGame: %b, Map ID: %d\n",
									r.getID(), r.getName(), r.getPlayerNumber(), r.getMaxPlayer(), r.isInGame(),
									r.getMapID());
							for (int id : r.getPlayerID()) {
								System.out.printf("Player ID: %d\n", id);
							}
						}

					} else if (cmds[0].equals("isinlobby")) {

						System.out.println("inLobby: " + client.isInLobby());

					} else if (cmds[0].equals("isinroom")) {

						System.out.println("inRoom: " + client.isInRoom());

					} else if (cmds[0].equals("roomid")) {

						System.out.println("roomID: " + client.getRoomID());

					} else if (cmds[0].equals("mapid")) {

						System.out.println("mapID: " + client.getMapID());

					} else if (cmds[0].equals("leaveroom")) {

						client.leaveRoom();

					} else if (cmds[0].equals("isingame")) {

						System.out.println("inGame: " + client.isInGame());

					} else if (cmds[0].equals("sendmove")) {

						// just send a random move
						short random = (short) new Random().nextInt();
						KeyboardState k = ServerPacketEncoder.shortToKeyboardState(random);
						client.sendMove(k);

					} else if (cmds[0].equals("addai")) {

						client.addAI();

					} else if (cmds[0].equals("removeai")) {

						client.removeAI();

					} else if (cmds[0].equals("sendmap") || cmds[0].equals("addmap")) {

						Block[][] customGridMap = new Block[][] {
								{ Block.SOLID, Block.SOLID, Block.SOLID, Block.SOLID, Block.SOLID },
								{ Block.SOLID, Block.BLANK, Block.BLANK, Block.BLANK, Block.SOLID },
								{ Block.SOLID, Block.BLANK, Block.BLANK, Block.BLANK, Block.SOLID },
								{ Block.SOLID, Block.BLANK, Block.BLANK, Block.BLANK, Block.SOLID },
								{ Block.SOLID, Block.SOFT, Block.SOFT, Block.SOFT, Block.SOLID },

								{ Block.SOLID, Block.SOLID, Block.SOFT, Block.SOLID, Block.SOLID },
								{ Block.SOLID, Block.SOLID, Block.SOFT, Block.SOLID, Block.SOLID },
								{ Block.SOLID, Block.SOLID, Block.BLANK, Block.SOLID, Block.SOLID },
								{ Block.SOLID, Block.SOLID, Block.BLANK, Block.SOLID, Block.SOLID },
								{ Block.SOLID, Block.SOLID, Block.BLANK, Block.SOLID, Block.SOLID },

								{ Block.SOLID, Block.SOFT, Block.SOFT, Block.SOFT, Block.SOLID },
								{ Block.SOLID, Block.BLANK, Block.BLANK, Block.SOFT, Block.SOLID },
								{ Block.SOLID, Block.BLANK, Block.BLANK, Block.SOFT, Block.SOLID },
								{ Block.SOLID, Block.SOFT, Block.BLANK, Block.SOFT, Block.SOLID },
								{ Block.SOLID, Block.SOLID, Block.SOLID, Block.SOLID, Block.SOLID } };
						Map customMap = new Map("default map", customGridMap, null);

						client.addRoomMap(customMap);

					} else {

						pInvalid();

					}

				} else if (cmds.length == 2) {

					if (cmds[0].equals("connect")) {

						client.connect(cmds[1]);

					} else if (cmds[0].equals("joinroom")) {

						int roomID = -1;
						try {
							roomID = Integer.parseInt(cmds[1]);
						} catch (NumberFormatException e) {
							System.out.println("Failed to parse room id");
							continue;
						}

						client.joinRoom(roomID);

					} else if (cmds[0].equals("ready")) {

						if (cmds[1].equals("true")) {
							client.readyToPlay(true);
						} else {
							client.readyToPlay(false);
						}

					} else if (cmds[0].equals("setroomname")) {

						client.setRoomName(cmds[1]);

					} else if (cmds[0].equals("setmaxplayer")) {

						byte maxPlayer = 4;
						try {
							maxPlayer = Byte.parseByte(cmds[1]);
						} catch (NumberFormatException e) {
							System.out.println("Failed to parse max player");
							continue;
						}

						client.setRoomMaxPlayer(maxPlayer);

					} else if (cmds[0].equals("setmapid")) {

						int mapID = -1;
						try {
							mapID = Integer.parseInt(cmds[1]);
						} catch (NumberFormatException e) {
							System.out.println("Failed to parse map id");
							continue;
						}

						client.setRoomMapID(mapID);

					} else if (cmds[0].equals("sendraw")) {

						byte[] data = null;
						data = cmds[1].getBytes("UTF-8");
						client.sendRaw(data);

					} else {

						pInvalid();

					}

				} else if (cmds.length == 3) {
					if (cmds[0].equals("setaidiff")) {

						byte id;
						try {
							id = Byte.parseByte(cmds[1]);
						} catch (NumberFormatException e) {
							System.out.println("Failed to parse AI id");
							continue;
						}

						byte difficulty;
						try {
							difficulty = Byte.parseByte(cmds[2]);
						} catch (NumberFormatException e) {
							System.out.println("Failed to parse AI difficulty");
							continue;
						}

						client.setAIDifficulty(id, ClientPacketEncoder.byteToAIDifficulty(difficulty));

					} else {

						pInvalid();

					}
				} else if (cmds.length == 4) {

					if (cmds[0].equals("createroom")) {

						byte maxPlayer = 4;
						try {
							maxPlayer = Byte.parseByte(cmds[2]);
						} catch (NumberFormatException e) {
							maxPlayer = 4;
						}

						int mapID = 0;
						try {
							mapID = Integer.parseInt(cmds[3]);
						} catch (NumberFormatException e) {
							System.out.println("Failed to parse map id");
							continue;
						}

						client.createRoom(cmds[1], maxPlayer, mapID);

					} else {

						pInvalid();

					}
				} else {

					pInvalid();

				}
			} catch (IOException e) {
				System.out.println(e);
				break;
			}

		}

		scanner.close();
		client.exit();

	}

	private static void pInvalid() {
		System.out.println("Invalid command");
	}

}
