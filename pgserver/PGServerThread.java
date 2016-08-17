package pgserver;

import java.awt.Color;
import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;
import java.util.Scanner;

import picturegame.GameplayWindowEngine;

/**
 * The PGServerThread class handles connections to a single client and
 * maintains that client's information.
 * 
 * @author Billy Robbins
 * @version 2.0
 * @since 11/22/2015
 */
public class PGServerThread extends Thread {
	
	// CONSTANTS - Other
	public static final int STATE_USERNAME = 0;
	public static final int STATE_LOBBY_SELECTION = 1;
	public static final int STATE_IN_LOBBY = 2;
	public static final String MESSAGE_USERNAME_SUCCESS = "USERNAME CLAIMED SUCCESSFULLY";
	public static final String MESSAGE_USERNAME_FAILURE = "USERNAME NOT CLAIMED";
	public static final String MESSAGE_CREATE_PRIVATE = "CREATE PRIVATE";
	public static final String MESSAGE_JOIN_PUBLIC = "PUBLIC";
	public static final String MESSAGE_JOIN_PRIVATE = "PRIVATE ";
	public static final String MESSAGE_JOIN_SUCCESS = "LOBBY JOINED SUCCESSFULLY";
	public static final String MESSAGE_JOIN_FAILURE = "LOBBY JOIN FAILED";
	public static final String MESSAGE_LEAVE_LOBBY = "LEAVE LOBBY";
	
	// DATA MEMBERS
	private Socket socket = null;
	private PGServer parent;
	private DataInputStream input;
	private DataOutputStream output;
	private int currentState;
	private String username;
	private int lobbyID;
	private boolean drawing;
	private int team;
	
	/**
	 * Constructor for the PGServerThread class
	 * 
	 * @param clientSocket a socket connected to a client
	 * @param p the server to which this thread belongs
	 */
	public PGServerThread(Socket clientSocket, PGServer p)
	{
		super("Picture Game Server Thread");
		this.socket = clientSocket;
		parent = p;
		currentState = STATE_USERNAME;
	}
	
	@Override
	/**
	 * Called when the thread starts
	 */
	public void run()
	{
		boolean active = true;
		
		try
		{
			input = new DataInputStream(new BufferedInputStream(socket.getInputStream()));
			output = new DataOutputStream(socket.getOutputStream());
			
			while(active)
			{
				String message = input.readUTF();
				
				if(currentState == STATE_USERNAME) // try setting username
				{
					if(parent.addUsername(message))
					{
						output.writeUTF(MESSAGE_USERNAME_SUCCESS);
						output.flush();
						currentState = STATE_LOBBY_SELECTION;
						username = message;
					}
					else
					{
						output.writeUTF(MESSAGE_USERNAME_FAILURE);
						output.flush();
					}
				}
				else if(currentState == STATE_LOBBY_SELECTION)
				{
					if(message.equals(MESSAGE_JOIN_PUBLIC))
					{
						lobbyID = parent.addToPublicLobby(this);
						output.writeUTF(MESSAGE_JOIN_SUCCESS);
						output.flush();
						currentState = STATE_IN_LOBBY;
						parent.startLobby(lobbyID);
					}
					if(message.startsWith(MESSAGE_CREATE_PRIVATE))
					{
						// Parse options
						String options = message.substring(MESSAGE_CREATE_PRIVATE.length());
						Scanner optionScan = new Scanner(options);
						int timer = optionScan.nextInt();
						int score = optionScan.nextInt();
						optionScan.close();
						
						parent.createPrivateLobby(this, timer, score);
						output.writeUTF(MESSAGE_JOIN_SUCCESS);
						output.flush();
						currentState = STATE_IN_LOBBY;
						
						sendMessage(PGServer.HEADER_CHAT + "Key: " + parent.findLobby(lobbyID).getKey());
					}
					else if(message.startsWith(MESSAGE_JOIN_PRIVATE))
					{
						try
						{
							String key = message.substring(MESSAGE_JOIN_PRIVATE.length());
							int result = parent.addToPrivateLobby(this, key);
							if(result == -1)
								sendMessage(MESSAGE_JOIN_FAILURE);
							else
							{
								lobbyID = result;
								output.writeUTF(MESSAGE_JOIN_SUCCESS);
								output.flush();
								currentState = STATE_IN_LOBBY;
								parent.startLobby(lobbyID);
							}
						}
						catch(NullPointerException e)
						{
							sendMessage(MESSAGE_JOIN_FAILURE);
						}
					}
				}
				else if(currentState == STATE_IN_LOBBY)
				{
					if(message.equals(MESSAGE_LEAVE_LOBBY))
					{
						System.out.println("Removing " + username + " from lobby");
						parent.removeFromLobby(this, lobbyID);
						sendMessage(PGServer.HEADER_REMOVED);
						currentState = STATE_LOBBY_SELECTION;
					}
					else if(message.startsWith(GameplayWindowEngine.HEADER_CHAT))
					{
						String chatMessage = message.substring(GameplayWindowEngine.HEADER_CHAT.length()); // Remove header
						parent.sendChatMessage(username + ": " + chatMessage, this);
					}
					else if(message.startsWith(GameplayWindowEngine.HEADER_DRAWING))
					{
						// Handle drawing message (only if user is the drawer)
						if(drawing)
						{
							if(message.startsWith(GameplayWindowEngine.HEADER_DRAWING + GameplayWindowEngine.DRAWING_RELEASE))
							{
								parent.drawingReleaseReceived(this);
							}
							else
							{
								// Get coordinates and color and send them up to the server
								Scanner colorParser = new Scanner(message.substring(GameplayWindowEngine.HEADER_DRAWING.length()));
								int x = colorParser.nextInt();
								int y = colorParser.nextInt();
								int rgb = colorParser.nextInt();
								parent.drawingReceived(x, y, new Color(rgb), this);
								colorParser.close();
							}
						}
					}
					else if(message.startsWith(GameplayWindowEngine.HEADER_CLEAR))
					{
						// Only handle if drawing
						if(drawing)
							parent.drawingClearReceived(this);
					}
					else if(message.startsWith(GameplayWindowEngine.HEADER_GUESS))
					{
						// Handle a guess (only if the user is not the drawer)
						if(!drawing)
						{
							String guess = message.substring(GameplayWindowEngine.HEADER_GUESS.length());
							parent.guessReceived(guess, this);
						}
					}
					else if(message.startsWith(GameplayWindowEngine.HEADER_INFO))
					{
						// Update player list
						parent.sendLobbyPlayerList(this);
						
						// Check to see if lobby can start
						parent.startLobby(lobbyID);
						
						// Try sending role
						parent.sendRole(this);
					}
					else
					{
						System.err.println("ERROR: Could not parse message - " + message);
					}
				}
			}
		}
		catch(IOException e)
		{
			System.err.println("ERROR: Lost connection to " + getAddress());
			parent.removeUser(this);
			active = false;
		}
	}
	
	/**
	 * get the IP address to which this thread is connected
	 * 
	 * @return the socket's connected IP address
	 */
	public InetAddress getAddress()
	{
		return socket.getInetAddress();
	}
	
	/**
	 * get the state in which this thread is
	 * 
	 * @return state corresponding to one of PGServerThread's
	 * state constants
	 */
	public int getCurrentState()
	{
		return currentState;
	}
	
	/**
	 * return the username for this thread's client
	 * 
	 * @return the connected client's username
	 */
	public String getUsername()
	{
		return username;
	}
	
	/**
	 * return the ID number of the lobby to which this
	 * thread's client belongs
	 * 
	 * @return client's lobby ID number
	 */
	public int getLobby()
	{
		return lobbyID;
	}
	
	/**
	 * set the current lobby ID of this thread
	 * 
	 * @param l current lobby ID
	 */
	public void setLobby(int l)
	{
		lobbyID = l;
	}
	
	/**
	 * sends a message out to the connected client
	 * 
	 * @param message a String message to send
	 */
	public void sendMessage(String message)
	{
		try
		{
			output.writeUTF(message);
			output.flush();
		}
		catch(IOException e)
		{
			System.err.println("ERROR: Could not send message");
			e.printStackTrace();
		}
	}
	
	/**
	 * says whether or not this user is drawing
	 * 
	 * @return <b>true</b> if this user is drawing, <b>false</b>
	 * if guessing
	 */
	public boolean isDrawing()
	{
		return drawing;
	}
	
	/**
	 * tells this thread if its connected client is drawing
	 * 
	 * @param d whether or not this thread's client is drawing
	 */
	public void setDrawing(boolean d)
	{
		drawing = d;
	}
	
	/**
	 * gives the team this player is on
	 * 
	 * @return int corresponding to this player's team
	 */
	public int getTeam()
	{
		return team;
	}
	
	/**
	 * sets the team this player is on
	 * 
	 * @param t team on which this player has been placed
	 */
	public void setTeam(int t)
	{
		team = t;
	}
}
