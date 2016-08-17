package pgserver;

import java.util.ArrayList;
import java.util.Random;

/**
 * The PGLobby class maintains information about a single lobby, including
 * the list of clients, the lobby's game state, the score, etc.
 * 
 * @author Billy Robbins
 * @author Steve Jean
 * @version 2.0
 * @since 11/22/2015
 */
public class PGLobby {

	// CONSTANTS
	public static final int TIMER_MIN = 30;
	public static final int TIMER_MAX = 120;
	public static final int SCORE_MIN = 1;
	public static final int SCORE_MAX = 20;
	public static final int TEAM_1 = 1;
	public static final int TEAM_2 = 2;
	public static final int TEAM_WAIT = -1;
	private static final int MIN_PLAYERS = 4; // players needed to start game
	private static final int MAX_PLAYERS = 10;
	private static final int KEY_LENGTH = 8;
	private static final int UNICODE_OFFSET = 64;
	private static final int DEFAULT_ROUND_TIME = 90;
	private static final int DEFAULT_POST_ROUND_TIME = 10;
	private static final int DEFAULT_SCORE_LIMIT = 7;
	private static final String TEAM_1_WIN = "Team 1 wins!  Restarting...";
	private static final String TEAM_2_WIN = "Team 2 wins!  Restarting...";
	private static final String NO_WIN = "Not enough players. Ending game...";
	
	// DATA MEMBERS
	private PGServer parent;
	private ArrayList<PGServerThread> clientThreads;
	private ArrayList<PGServerThread> waitingClientThreads;
	private int lobbyID;
	private int numPlayers;
	private boolean isPrivate;
	private String key; // private key (if necessary)
	private PGWordList wordList;
	private String currentWord;
	private ArrayList<PGServerThread> team1;
	private ArrayList<PGServerThread> team2;
	private PGServerThread team1drawer;
	private PGServerThread team2drawer;
	private int team1score;
	private int team2score;
	private ArrayList<String> team1guesses;
	private ArrayList<String> team2guesses;
	private boolean active;
	private boolean waiting;
	private boolean inRound;
	private Timer timer;
	private int customTimer;
	private int customScore;
	
	/**
	 * constructor for a picture game lobby
	 * 
	 * @param id this lobby's given ID number
	 */
	public PGLobby(PGServer par, int id, boolean p, PGWordList wl)
	{
		customTimer = DEFAULT_ROUND_TIME;
		customScore = DEFAULT_SCORE_LIMIT;
		
		parent = par;
		numPlayers = 0;
		lobbyID = id;
		isPrivate = p;
		wordList = wl;
		clientThreads = new ArrayList<PGServerThread>();
	    waitingClientThreads = new ArrayList<PGServerThread>();
		active = false;
		waiting = false;
		team1score = 0;
		team2score = 0;
		inRound = false;
		team1guesses = new ArrayList<String>();
		team2guesses = new ArrayList<String>();
		timer = new Timer(this);
	
	}
	
	/**
	 * set the time limit per round
	 * 
	 * @param timer custom time limit
	 */
	public void setTimeLimit(int timer)
	{
		if(isPrivate && (timer >= TIMER_MIN) && (timer <= TIMER_MAX ))
			customTimer = timer;
	}
	
	/**
	 * set the score limit
	 * 
	 * @param score custom score limit
	 */
	public void setScoreLimit(int score)
	{
		if(isPrivate && (score >= SCORE_MIN) && (score <= SCORE_MAX))
			customScore = score;
	}
	
	/**
	 * get a list of clients on the server
	 * 
	 * @return an ArrayList containing all clients on the server
	 */
	public ArrayList<PGServerThread> getClientList()
	{
		return clientThreads;
	}
	
	/**
	 * add a new client to the server
	 * 
	 * @param newClientThread thread representing the client to be added
	 */
	public void addClient(PGServerThread newClientThread)
	{
		clientThreads.add(newClientThread);
		numPlayers++;
		
		if(active && !waiting)
			placeOnTeam(newClientThread);
		if(inRound)
			newClientThread.setDrawing(false);
		if(active && waiting)
		{
			waitingClientThreads.add(newClientThread);
			newClientThread.setTeam(TEAM_WAIT);
		}
	}
	
	/**
	 * remove a client from the lobby
	 * 
	 * @param client the client to remove
	 */
	public void removeClient(PGServerThread client)
	{
		numPlayers--;
		
		clientThreads.remove(client);
		if(team1 != null)
			team1.remove(client);
		if(team2 != null)
			team2.remove(client);
		if(waitingClientThreads != null)
			waitingClientThreads.remove(client);
		
		if(inRound && client.isDrawing())
			parent.drawerDisconnected(this, client.getTeam());
		
		for(int i = 0; i < clientThreads.size(); i++)
			parent.sendLobbyPlayerList(clientThreads.get(i));
		
		// Check if lobby drops below min players
		if((clientThreads.size() < 4) && active)
		{
			active = false;
			waiting = false;
			inRound = false;
			
			reset();
			parent.lobbyGameOver(this, NO_WIN);
		}
	}
	
	/**
	 * get whether or not this lobby has slots available
	 * 
	 * @return <b>true</b> if lobby has under 10 players,
	 * <b>false</b> if lobby has 10 players
	 */
	public boolean hasSlotAvailable()
	{
		return numPlayers < MAX_PLAYERS;
	}
	
	/**
	 * get the ID number for this lobby
	 * 
	 * @return this lobby's ID number
	 */
	public int getLobbyID()
	{
		return lobbyID;
	}
	
	/**
	 * returns whether or not this lobby is private
	 * 
	 * @return <b>true</b> if lobby is private, <b>false</b>
	 * if not
	 */
	public boolean isPrivate()
	{
		return isPrivate;
	}
	
	/**
	 * generates a random String key for private lobbies
	 * 
	 * @return a randomly generated String key
	 */
	public String generateKey()
	{
		Random rng = new Random();
		key = "";
		
		for(int i = 0; i < KEY_LENGTH; i++)
		{
			int next = rng.nextInt(26);
			key += Character.toString((char)(next + UNICODE_OFFSET));
		}
		
		return key;
	}
	
	/**
	 * returns the key for this lobby
	 * 
	 * @return this lobby's private key
	 */
	public String getKey()
	{
		return key;
	}
	
	/**
	 * says if this lobby is ready to begin
	 * 
	 * @return <b>true</b> if lobby has enough players,
	 * <b>false</b> otherwise
	 */
	public boolean ready()
	{
		return numPlayers >= MIN_PLAYERS;
	}
	
	/**
	 * Places each player on a team
	 */
	public void placeOnTeam(PGServerThread client)
	{
		if(team1.size() > team2.size())
		{
			team2.add(client);
			client.setTeam(TEAM_2);
		}
		else if(team2.size() > team1.size())
		{
			team1.add(client);
			client.setTeam(TEAM_1);
		}
		else
		{
			Random rand = new Random();
			int result = rand.nextInt(2);
			
			if(result == 0)
			{
				team1.add(client);
				client.setTeam(TEAM_1);
			}
			else
			{
				team2.add(client);
				client.setTeam(TEAM_2);
			}
			
		}
	}
	
	/**
	 * Place on team helper method. Iterates through client list and hides the for loop.
	 * 
	 * @see placeOnTeam()
	 * @param numClients
	 * @author Chris Ridgely
	 */
	public void placeOnTeamIterator(int numClients)
	{
		for(int i = 0; i < numClients; i++)
			placeOnTeam(clientThreads.get(i));
	}

	/**
	 * Rebalances teams
	 * 
	 * @see placeOnTeamIterator(int numClients)
	 * @see updatePlayerList()
	 * @author Chris Ridgely
	 */
	public void balanceTeams()
	{
		ArrayList<PGServerThread> clientList = getClientList();
		for(int i = 0; i < clientList.size(); i++)
			clientList.get(i).sendMessage(PGServer.HEADER_GAME_UPDATE + PGServer.UPDATE_REBALANCING);
	
		team1.clear();
		team2.clear();
		
		placeOnTeamIterator(clientThreads.size());
		updatePlayerList();
	}
	
	/**
	 * starts the lobby
	 */
	public void start()
	{
		// Set lobby state to active
		active = true;
		waiting = false;
		
		// Add all players to teams
		team1 = new ArrayList<PGServerThread>();
		team2 = new ArrayList<PGServerThread>();
		placeOnTeamIterator(clientThreads.size());
		
		// Set wait time
		inRound = false;
		timer.setCurrentTime(DEFAULT_POST_ROUND_TIME);
		
		// Start timer
		if(!timer.isActive())
			timer.start(); 
	}
	
	/**
	 * chooses one drawer for each team randomly
	 */
	private void selectDrawers()
	{		
		Random drawSelect = new Random();
		
		// Select team 1 drawer
		team1drawer = team1.get(drawSelect.nextInt(team1.size()));
		for(int i = 0; i < team1.size(); i++)
		{
			PGServerThread client = team1.get(i);
			if(client == team1drawer)
				client.setDrawing(true);
			else
				client.setDrawing(false);
		}
		
		team2drawer = team2.get(drawSelect.nextInt(team2.size()));
		for(int i = 0; i < team2.size(); i++)
		{
			PGServerThread client = team2.get(i);
			if(client == team2drawer)
				client.setDrawing(true);
			else
				client.setDrawing(false);
		}
	}
	
	/**
	 * says the current word the drawers should draw
	 * 
	 * @return the current word being drawn
	 */
	public String getWord()
	{
		return currentWord;
	}
	
	/**
	 * checks if a guess is correct, ending the round if 
	 * 
	 * @param guess the guess for the word being drawn
	 * @param guesser the thread of the client who guessed
	 * @return <b>true</b> if guess was correct, <b>false</b>
	 * if not
	 */
	public void guessReceived(String guess, PGServerThread guesser)
	{	
		if(inRound)
		{
			if(guesser.getTeam() == TEAM_1)
				team1guesses.add(guess);
			else if(guesser.getTeam() == TEAM_2)
				team2guesses.add(guess);
			
			if(guess.equalsIgnoreCase(currentWord) && 
					(guesser.getTeam() == TEAM_1 || guesser.getTeam() == TEAM_2)) // Correct guess
			{
				
				if(guesser.getTeam() == TEAM_1)
				{
					team1score++;
				}
				else if(guesser.getTeam() == TEAM_2)
				{
					team2score++;
				}
				
				parent.roundEnded(this, true, guesser.getUsername(), guess);
			}
		}
	}
	
	/**
	 * starts a new round
	 */
	public void startRound()
	{
		// Reset guess lists
		team1guesses.clear();
		team2guesses.clear();
	
		// Check for balance issues 
		if(clientThreads.size() < 4) // below min players
		{
			active = false;
			waiting = false;
			inRound = false;
			
			reset();
			parent.lobbyGameOver(this, NO_WIN);
			return;
		}
		
		// Rebalance if team has below 1 player
		if((team1.size() < 2) || (team2.size() < 2))
			balanceTeams();
		
		// Select drawers for each team
		selectDrawers();
		
		// Select word
		currentWord = wordList.getRandomWord();
		
		// Send out roles
		parent.sendRoles(this);
		
		// Start timer
		timer.setCurrentTime(customTimer);
		
		inRound = true;
		
		waiting = true;
	}
	
	/**
	 * ends a round and checks if the game is over, setting
	 * internal lobby state to say the game has been won if
	 * it is over
	 */
	public void endRound()
	{
		inRound = false;
		
		waiting = false;
		
		// TODO: Add post round time, let players vote
		if(team1score == customScore)
		{
			reset();
			parent.lobbyGameOver(this, TEAM_1_WIN);
		}
		else if(team2score == customScore)
		{
			reset();
			parent.lobbyGameOver(this, TEAM_2_WIN);
		}
		
		if(waitingClientThreads.size() > 0)
		{
			for(int i = 0; i < waitingClientThreads.size(); i++)
				placeOnTeam(waitingClientThreads.get(i));
			
			waitingClientThreads = new ArrayList<PGServerThread>();
		}
		
		// Update player lists
		updatePlayerList();
		
		// Begin wait time
		timer.setCurrentTime(DEFAULT_POST_ROUND_TIME);
	}
	
	/**
	 * Refreshes the server's team division
	 */
	private void updatePlayerList() 
	{
		for(int i = 0; i < clientThreads.size(); i++)
			parent.sendLobbyPlayerList(clientThreads.get(i));
	}
	
	/**
	 * takes all players off teams and puts them on new teams
	 */
	private void reset()
	{
		team1score = 0;
		team2score = 0;
		wordList.reset();
		team1.clear();
		team2.clear();
		for(int i = 0; i < clientThreads.size(); i++)
			placeOnTeam(clientThreads.get(i));
	}
	
	/**
	 * says if this lobby is in the middle of a round
	 * 
	 * @return <b>true</b> if the lobby is playing a round,
	 * <b>false</b> otherwise
	 */
	public boolean isInRound()
	{
		return inRound;
	}
	
	/**
	 * gets the score of team 1
	 * 
	 * @return team 1's score
	 */
	public int getTeam1Score()
	{
		return team1score;
	}
	
	/**
	 * gets the score of team 2
	 * 
	 * @return team 2's score
	 */
	public int getTeam2Score()
	{
		return team2score;
	}
	
	/**
	 * gives a list of guesses players on team 1 have made
	 * 
	 * @return list of guesses team 1 has made
	 */
	public ArrayList<String> getTeam1Guesses()
	{
		return team1guesses;
	}
	
	/**
	 * gives a list of guesses players on team 2 have made
	 * 
	 * @return list of guesses team 2 has made
	 */
	public ArrayList<String> getTeam2Guesses()
	{
		return team2guesses;
	}
	
	/**
	 * says if this lobby is playing a game
	 * 
	 * @return <b>true</b> if this lobby is playing,
	 * <b>false</b> otherwise
	 */
	public boolean isPlaying()
	{
		return active;
	}
	
	/**
	 * checks if timer is up and notifies players of time
	 * 
	 * @param time time left in the timer
	 */
	public void timerTick(int time)
	{
		if(active)
		{
			for(int i = 0; i < clientThreads.size(); i++)
				parent.sendTimerUpdate(clientThreads.get(i), time);
			
			if(time == 0 && inRound) // Round timeout
			{
				parent.roundEnded(this, false, null, null);
			}
			else if(time == 0 && !inRound)
			{
				startRound();
			}
		}
	}
}
