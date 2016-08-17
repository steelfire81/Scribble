package pgserver;

import java.awt.Color;
import java.io.*;
import java.net.*;
//import java.net.ServerSocket;
import java.util.ArrayList;
import java.util.Scanner;

/**
 * The PGServer class launches and maintains a server that handles connections
 * from clients.
 * 
 * @author Billy Robbins
 * @author Steve Jean
 * @version 2.0
 * @since 11/22/2015
 */
public class PGServer {
	
	// CONSTANTS - Message Headers (used during gameplay only)
	public static final String HEADER_CHAT = "CHAT: ";
	public static final String HEADER_DRAWING = "DRAWING: ";
	public static final String HEADER_ROLE = "ROLE: ";
	public static final String HEADER_GAME_UPDATE = "GAME: ";
	public static final String HEADER_CLIENT_LIST = "CLIENTS: ";
	public static final String HEADER_GUESS_LIST = "GUESSES: ";
	public static final String HEADER_TIMER = "TIMER: ";
	public static final String HEADER_PLAYER = "PLAYER: ";
	public static final String HEADER_REMOVED = "REMOVED";
	
	// CONSTANTS - Game Update Messages
	public static final String UPDATE_START = "Starting";
	public static final String UPDATE_CORRECT = "Correct";
	public static final String UPDATE_SCORE = "Score ";
	public static final String UPDATE_ROUND_END = "Round End ";
	public static final String UPDATE_GAME_END = "Game End ";
	public static final String UPDATE_WAITING_PLAYERS = "Waiting For More Players";
	public static final String UPDATE_REBALANCING = "Rebalancing Teams";
	
	// CONSTANTS - Drawing messages
	public static final String DRAWING_RELEASE = "Release";
	public static final String DRAWING_CLEAR = "Clear";
	
	// CONSTANTS - Role Messages
	public static final String ROLE_DRAW = "Drawing ";
	public static final String ROLE_GUESS = "Guessing";
	
	// CONSTANTS - Other messages
	public static final String MSG_TIMEOUT = "Round ended with a timeout";
	
	// CONSTANTS - other
	private static final int PORT_DEFAULT = 6789;
	private static final String WORDLIST_FILE = "wordlists";
	private static final String ALL_WORDLIST_NAME = "all";
	
	// DATA MEMBERS
	ArrayList<PGServerThread> clientThreads;
	ArrayList<String> activeUsernames;
	ArrayList<String> activeKeys;
	ArrayList<PGLobby> lobbyList;
	ArrayList<PGWordList> wordLists;
	private int currentLobbyID;
	
	/**
	 * constructor for PGServer
	 * 
	 * @param port port on which to listen for clients
	 * @throws IOException
	 */
	public PGServer(int port, ArrayList<PGWordList> wl) throws IOException
	{
		currentLobbyID = 0;
		boolean active = true;
		
		clientThreads = new ArrayList<PGServerThread>();
		activeUsernames = new ArrayList<String>();
		activeKeys = new ArrayList<String>();
		lobbyList = new ArrayList<PGLobby>();
		wordLists = wl;
		
		ServerSocket socket = new ServerSocket(port);

		System.out.println("Socket opened on port " + port);
		System.out.println("Server address: " + InetAddress.getLocalHost());
		System.out.println();
		
		while(active)
		{
			PGServerThread thread = new PGServerThread(socket.accept(), this);
			System.out.println("New client: " + thread.getAddress());
			thread.start();
			clientThreads.add(thread);
		}
	}
	
	/**
	 * Tries to add a new username to the active username list
	 * 
	 * @param username the new username to be added
	 * @return <b>true</b> if the username is available, <b>false</b>
	 * if the username is already taken
	 */
	public boolean addUsername(String username)
	{
		if(username == "") // Empty username not allowed
			return false;
		else if(activeUsernames.contains(username))
			return false;
		else
		{
			System.out.println("Reserving username " + username);
			activeUsernames.add(username);
			return true;
		}
	}
	
	/**
	 * adds a client thread to a specific lobby
	 * 
	 * @param client thread connected to the client
	 * @return the lobby number the client has been added to
	 */
	public int addToPublicLobby(PGServerThread client)
	{
		if(lobbyList.size() == 0)
		{
			PGLobby newLobby = new PGLobby(this, currentLobbyID, false, compileAllWordLists());
			System.out.println("Creating new lobby with ID " + currentLobbyID);
			currentLobbyID++;
			newLobby.addClient(client);
			System.out.println("Adding " + client.getAddress() + " to lobby " + newLobby.getLobbyID());
			lobbyList.add(newLobby);
			return newLobby.getLobbyID();
		}
		else
		{
			for(int i = 0; i < lobbyList.size(); i++)
			{
				PGLobby target = lobbyList.get(i);
				if(target != null && target.hasSlotAvailable() && !target.isPrivate())
				{
					target.addClient(client);
					System.out.println("Adding " + client.getAddress() + " to lobby " + target.getLobbyID());
					
					// Update all players' client lists
					for(int j = 0; j < target.getClientList().size(); j++)
					{
						PGServerThread curr = target.getClientList().get(j);
						if(curr != client)
							sendLobbyPlayerList(curr);
					}
					
					return target.getLobbyID();
				}
			}
			
			PGLobby newLobby = new PGLobby(this, currentLobbyID, false, compileAllWordLists());
			System.out.println("Creating new lobby with ID " + currentLobbyID);
			currentLobbyID++;
			newLobby.addClient(client);
			System.out.println("Adding " + client.getAddress() + " to lobby " + newLobby.getLobbyID());
			lobbyList.add(newLobby);
			return newLobby.getLobbyID();
		}
	}
	
	/**
	 * try to add the client to a private lobby with specified key
	 * 
	 * @param client the thread associated with the joining client
	 * @param key the key entered by the user
	 * @return <b>-1</b> if lobby could not be joined/found, joined lobby's
	 * ID otherwise
	 */
	public int addToPrivateLobby(PGServerThread client, String key)
	{
		for(int i = 0; i < lobbyList.size(); i++)
		{
			PGLobby target = lobbyList.get(i);
			if(target != null && target.isPrivate() && target.hasSlotAvailable() && target.getKey().equals(key))
			{
				target.addClient(client);
				System.out.println("Adding " + client.getAddress() + "to PRIVATE lobby " + target.getLobbyID());
				
				// Update all players' client lists
				for(int j = 0; j < target.getClientList().size(); j++)
				{
					PGServerThread curr = target.getClientList().get(j);
					if(curr != client)
						sendLobbyPlayerList(curr);
				}
				
				return target.getLobbyID();
			}
		}
		
		// Could not place in lobby
		return -1;
	}
	
	/**
	 * creates a new private lobby for the specified client
	 * 
	 * @param client client creating a private lobby
	 * @return the key associated with the private lobby
	 */
	public String createPrivateLobby(PGServerThread client, int timer, int score)
	{
		PGLobby newLobby = new PGLobby(this, currentLobbyID, true, compileAllWordLists());
		newLobby.setScoreLimit(score);
		newLobby.setTimeLimit(timer);
		System.out.println("Creating new PRIVATE lobby with ID " + currentLobbyID);
		currentLobbyID++;
		newLobby.addClient(client);
		System.out.println("Adding " + client.getAddress() + "to PRIVATE lobby " + newLobby.getLobbyID());
		client.setLobby(newLobby.getLobbyID());
		
		// Generate key for new lobby
		boolean keyValid = false;
		while(!keyValid)
		{
			String key = newLobby.generateKey();
			keyValid = true;
			for(int i = 0; i < activeKeys.size(); i++)
				if(activeKeys.get(i).equals(key))
				{
					keyValid = false;
					break;
				}
		}
		
		lobbyList.add(newLobby);
		return newLobby.getKey();
	}
	
	/**
	 * sends a chat message out to a given client's lobby
	 * 
	 * @param message the message (already prefixed with the username)
	 * @param client the client trying to send the message
	 */
	public void sendChatMessage(String message, PGServerThread client)
	{
		int lobbyID = client.getLobby();
		PGLobby lobby;
		
		for(int i = 0; i < lobbyList.size(); i++)
		{
			lobby = lobbyList.get(i);
			if(lobby != null && (lobby.getLobbyID() == lobbyID))
			{
				System.out.println("Chat message \"" + message + "\" to lobby " + lobbyID);
				
				ArrayList<PGServerThread> clientsInLobby = lobby.getClientList();
				for(int j = 0; j < clientsInLobby.size(); j++)
				{
					PGServerThread curr = clientsInLobby.get(j);
					if(curr != null)
					{
						System.out.println("Sending to " + curr.getAddress());
						curr.sendMessage(HEADER_CHAT + message);
					}
				}
				break;
			}
		}
	}
	
	/**
	 * read in word lists
	 * 
	 * @return all word lists
	 */
	public static ArrayList<PGWordList> initializeWordLists() throws IOException
	{
		System.out.println("Initializing word lists..."); // Debug
		
		Scanner wlScan = new Scanner(PGServer.class.getResourceAsStream(WORDLIST_FILE));
		ArrayList<String> wordListNames = new ArrayList<String>();
		while(wlScan.hasNextLine())
			wordListNames.add(wlScan.nextLine());
		wlScan.close();
		
		ArrayList<PGWordList> wordLists = new ArrayList<PGWordList>();
		for(int i = 0; i < wordListNames.size(); i++)
		{
			System.out.println("\tAdding word list: " + wordListNames.get(i)); // Debug
			
			Scanner wordScan = new Scanner(PGServer.class.getResourceAsStream(wordListNames.get(i)));
			ArrayList<String> words = new ArrayList<String>();
			
			while(wordScan.hasNextLine())
				words.add(wordScan.nextLine());
			wordScan.close();
			
			wordLists.add(new PGWordList(wordListNames.get(i), words));
		}
		
		return wordLists;
	}
	
	/**
	 * removes a client from a lobby
	 * 
	 * @param client the client to remove from a lobby
	 * @param id the lobby id
	 */
	public void removeFromLobby(PGServerThread client, int id)
	{
		findLobby(id).removeClient(client);
	}
	
	/**
	 * finds a lobby with a given id
	 * @param lobbyID the id to find
	 * @return the lobby, or <b>null</b> if lobby isn't found
	 */
	public PGLobby findLobby(int lobbyID)
	{
		for(int i = 0; i < lobbyList.size(); i++)
			if(lobbyList.get(i).getLobbyID() == lobbyID)
				return lobbyList.get(i);
		
		return null;
	}
	
	/**
	 * creates a word lists with all word lists combined (used for
	 * public games)
	 * 
	 * @return a word list containing all other word lists
	 */
	private PGWordList compileAllWordLists()
	{
		ArrayList<String> allWords = new ArrayList<String>();
		for(int i = 0; i < wordLists.size(); i++)
		{
			ArrayList<String> wordList = wordLists.get(i).getWords();
			for(int word = 0; word < wordList.size(); word++)
				allWords.add(wordList.get(word));
		}
		
		return new PGWordList(ALL_WORDLIST_NAME, allWords);
	}
	
	/**
	 * starts playing a lobby if the lobby is ready
	 * 
	 * @param lobbyID the lobby to start
	 * @return <b>true</b> if lobby starting, <b>false</b> if
	 * lobby is not starting
	 */
	public boolean startLobby(int lobbyID)
	{
		PGLobby lobby = null;
		for(int i = 0; i < lobbyList.size(); i++)
		{
			if(lobbyList.get(i).getLobbyID() == lobbyID)
			{
				lobby = lobbyList.get(i);
				break;
			}
		}
		if(lobby == null)
			return false;
		else if(!lobby.ready())
			return false;
		else if(lobby.isPlaying())
			return false;
		else
		{
			System.out.println("Starting lobby " + lobbyID);
			
			// Tell clients the game is starting
			ArrayList<PGServerThread> clientList = lobby.getClientList();
			for(int i = 0; i < clientList.size(); i++)
				clientList.get(i).sendMessage(HEADER_GAME_UPDATE + UPDATE_START);
			
			// Start the lobby
			lobby.start();
			
			// Send updated player lists
			for(int i = 0; i < clientList.size(); i++)
				sendLobbyPlayerList(clientList.get(i));
			
			return true;
		}
	}
	
	/**
	 * send out roles to all users in a lobby
	 * 
	 * @param lobby the lobby to notify of roles
	 */
	public void sendRoles(PGLobby lobby)
	{
		if(lobby.isPlaying())
		{
			ArrayList<PGServerThread> clientList = lobby.getClientList();
			for(int i = 0; i < clientList.size(); i++)
			{
				PGServerThread curr = clientList.get(i);
				if(curr.isDrawing())
					curr.sendMessage(HEADER_ROLE + ROLE_DRAW + lobby.getWord());
				else
					curr.sendMessage(HEADER_ROLE + ROLE_GUESS);
			}
		}
	}
	
	/**
	 * send role to a single client
	 * 
	 * @param client client to whom role is sent
	 */
	public void sendRole(PGServerThread client)
	{
		PGLobby lobby = findLobby(client.getLobby());
		if(lobby.isPlaying())
		{
			if(client.isDrawing())
				client.sendMessage(HEADER_ROLE + ROLE_DRAW + lobby.getWord());
			else
				client.sendMessage(HEADER_ROLE + ROLE_GUESS);
		}
	}
	
	/**
	 * handles user guesses
	 * 
	 * @param guess the word guessed
	 * @param guesser thread corresponding to client who made the guess
	 */
	public void guessReceived(String guess, PGServerThread guesser)
	{
		System.out.println("Lobby " + guesser.getLobby() + ": " + guesser.getUsername() + " guessed " + guess);
		
		PGLobby lobby = findLobby(guesser.getLobby());
		
		lobby.guessReceived(guess, guesser);
		
		// send updated guess list to all players on team
		for(int i = 0; i < lobby.getClientList().size(); i++)
			if(lobby.getClientList().get(i).getTeam() == guesser.getTeam())
				sendGuessList(lobby.getClientList().get(i));
	}
	
	/**
	 * handles drawing input and sends out drawing info to all players on team
	 * 
	 * @param x x coordinate
	 * @param y y coordinate
	 * @param c color
	 * @param drawer user who drew this
	 */
	public void drawingReceived(int x, int y, Color c, PGServerThread drawer)
	{
		PGLobby lobby = findLobby(drawer.getLobby());
		ArrayList<PGServerThread> clientList = lobby.getClientList();
		int team = drawer.getTeam();
		
		for(int i = 0; i < clientList.size(); i++)
		{
			PGServerThread client = clientList.get(i);
			if((client != drawer) && (client.getTeam() == team))
				client.sendMessage(HEADER_DRAWING + x + " " + y + " " + c.getRGB());
		}
	}
	
	/**
	 * tells all players on a team that the drawer has released the mouse
	 * 
	 * @param drawer the drawer who released their mouse
	 */
	public void drawingReleaseReceived(PGServerThread drawer)
	{
		PGLobby lobby = findLobby(drawer.getLobby());
		ArrayList<PGServerThread> clientList = lobby.getClientList();
		int team = drawer.getTeam();
		
		for(int i = 0; i < clientList.size(); i++)
		{
			PGServerThread client = clientList.get(i);
			if((client != drawer) && (client.getTeam() == team))
				client.sendMessage(HEADER_DRAWING + DRAWING_RELEASE);
		}
	}
	
	/**
	 * tells all players on a team that the drawer has cleared the drawing
	 * 
	 * @param drawer the drawer who cleared the drawing
	 */
	public void drawingClearReceived(PGServerThread drawer)
	{
		PGLobby lobby = findLobby(drawer.getLobby());
		ArrayList<PGServerThread> clientList = lobby.getClientList();
		int team = drawer.getTeam();
		
		for(int i = 0; i < clientList.size(); i++)
		{
			PGServerThread client = clientList.get(i);
			if((client != drawer) && (client.getTeam() == team))
				client.sendMessage(HEADER_DRAWING + DRAWING_CLEAR);
		}
	}
	
	/**
	 * removes a client from the server
	 * 
	 * @param client the client to remove
	 */
	public void removeUser(PGServerThread client)
	{
		// Remove user from lobby
		if(client.getCurrentState() == PGServerThread.STATE_IN_LOBBY)
			findLobby(client.getLobby()).removeClient(client);
		
		// TODO: If this client was the drawer, lose the round for that team
		
		// Remove username from database
		String username = client.getUsername();
		System.out.println("Relinquishing username " + username);
		for(int i = 0; i < activeUsernames.size(); i++)
			if(activeUsernames.get(i).equals(username))
			{
				activeUsernames.remove(i);
				break;
			}
	}
	
	/**
	 * sends the specified client the current list of players on the server
	 * 
	 * @param client client requesting updated player list
	 */
	public void sendLobbyPlayerList(PGServerThread client)
	{		
		String playerList = "";
		ArrayList<PGServerThread> clientList = findLobby(client.getLobby()).getClientList();
		
		if(findLobby(client.getLobby()).isPlaying())
		{
			playerList += "TEAM ONE:\n";
			
			for(int i = 0; i < clientList.size(); i++)
			{
				PGServerThread curr = clientList.get(i);
				if(curr.getTeam() == PGLobby.TEAM_1)
					playerList += " - " + curr.getUsername() + "\n";
			}
			
			playerList += "\nTEAM TWO:\n";
			for(int i = 0; i < clientList.size(); i++)
			{
				PGServerThread curr = clientList.get(i);
				if(curr.getTeam() == PGLobby.TEAM_2)
					playerList += " - " + curr.getUsername() + "\n";
			}
			
			playerList += "\nNOT ON TEAM:\n";
			for(int i = 0; i < clientList.size(); i++)
			{
				PGServerThread curr = clientList.get(i);
				if(curr.getTeam() == PGLobby.TEAM_WAIT)
					playerList += " - " + curr.getUsername() + "\n";
			}
		}
		else
			for(int i = 0; i < clientList.size(); i++)
				playerList += " - " + clientList.get(i).getUsername() + "\n";
		
		client.sendMessage(HEADER_CLIENT_LIST + playerList);
	}
	
	/**
	 * sends a list of the client's team's guesses to a specified
	 * client
	 * 
	 * @param client to whom the guess list will be sent
	 */
	public void sendGuessList(PGServerThread client)
	{
		String guessList = "";
		if(client.getTeam() == PGLobby.TEAM_1)
		{
			ArrayList<String> guesses = findLobby(client.getLobby()).getTeam1Guesses();
			for(int i = 0; i < guesses.size(); i++)
				guessList += guesses.get(i) + "\n";
			
			client.sendMessage(HEADER_GUESS_LIST + guessList);
		}
		else if(client.getTeam() == PGLobby.TEAM_2)
		{
			ArrayList<String> guesses = findLobby(client.getLobby()).getTeam2Guesses();
			for(int i = 0; i < guesses.size(); i++)
				guessList += guesses.get(i) + "\n";
			
			client.sendMessage(HEADER_GUESS_LIST + guessList);
		}
			
	}
	
	/**
	 * sends the client the current time left in the lobby
	 * 
	 * @param client client to send the time to
	 * @param t current time for the lobby
	 */
	public void sendTimerUpdate(PGServerThread client, int t)
	{
		client.sendMessage(HEADER_TIMER + t);
	}
	
	/**
	 * tell clients in a lobby that the round is over
	 * 
	 * @param lobby the lobby whose round has just ended
	 * @param correct whether or not the round ended on a correct guess
	 * @param guesserName the person who guessed it (or null if no one)
	 * @param word the word guessed (or null if no one)
	 */
	public void roundEnded(PGLobby lobby, boolean correct, String guesserName, String word)
	{
		ArrayList<PGServerThread> clientList = lobby.getClientList();
		
		if(correct)
		{
			for(int i = 0; i < clientList.size(); i++)
			{
				PGServerThread curr = clientList.get(i);
				curr.sendMessage(HEADER_GAME_UPDATE + UPDATE_ROUND_END);
				if(curr.getUsername().equals(guesserName))
					curr.sendMessage(HEADER_GAME_UPDATE + UPDATE_CORRECT);
				curr.sendMessage(HEADER_GAME_UPDATE + UPDATE_SCORE + lobby.getTeam1Score() + " " + lobby.getTeam2Score());
				curr.sendMessage(HEADER_CHAT + guesserName + " correctly guessed " + word);
			}
		}
		else // Timeout
		{
			for(int i = 0; i < clientList.size(); i++)
			{
				PGServerThread curr = clientList.get(i);
				curr.sendMessage(HEADER_GAME_UPDATE + UPDATE_ROUND_END);
				curr.sendMessage(HEADER_CHAT + MSG_TIMEOUT);
			}
		}
		
		lobby.endRound();
	}
	
	/**
	 * tells members of a lobby that a drawer has disconnected
	 * 
	 * @param lobby lobby from whom player was disconnected
	 * @param team player who disconnected's team
	 */
	public void drawerDisconnected(PGLobby lobby, int team)
	{
		ArrayList<PGServerThread> clientList = lobby.getClientList();
		for(int i = 0; i < clientList.size(); i++)
		{
			PGServerThread curr = clientList.get(i);
			curr.sendMessage(HEADER_GAME_UPDATE + UPDATE_ROUND_END);
			curr.sendMessage(HEADER_GAME_UPDATE + UPDATE_SCORE + lobby.getTeam1Score() + " " + lobby.getTeam2Score());
			curr.sendMessage(HEADER_CHAT + "Team " + team + "'s drawer disconnected, starting new round");
		}
		
		lobby.endRound();
	}
	
	/**
	 * tells all users in a lobby their game is over
	 * 
	 * @param lobby lobby that has ended
	 */
	public void lobbyGameOver(PGLobby lobby, String message)
	{
		ArrayList<PGServerThread> clientList = lobby.getClientList();
		for(int i = 0; i < clientList.size(); i++)
		{
			PGServerThread curr = clientList.get(i);
			curr.sendMessage(HEADER_GAME_UPDATE + UPDATE_GAME_END + message);
			curr.sendMessage(HEADER_GAME_UPDATE + UPDATE_SCORE + lobby.getTeam1Score() + " " + lobby.getTeam2Score());
			sendLobbyPlayerList(curr);
		}
	}
	
	/**
	 * main - starts a PGServer
	 * 
	 * @param args does nothing
	 */
	public static void main(String[] args)
	{   
		int portNumber = 0;
		if(args.length == 1)
		{ 	try
			{
				portNumber = Integer.parseInt(args[0]);
			}
			catch(NumberFormatException e1)
			{
				System.out.println("INVALID PORT NUMBER");
				System.out.println("MUST BE A NUMBER");
				System.exit(1);
			}
		}
		else
		{
		   System.out.println("USAGE: PGServer [PORT NUMBER]");
		   System.exit(1);
		}
		try
		{   
		   new PGServer(portNumber, initializeWordLists());
		}
	       catch(BindException e0)
		{
           System.out.println("INVALID PORT NUMBER");
		   System.exit(1);
		}	
		catch(IOException e)
		{
		   e.printStackTrace();
		   System.exit(1);
		}
		
	}
	
}
