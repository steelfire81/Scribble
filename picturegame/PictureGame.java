package picturegame;

import javax.swing.JOptionPane;

import pgserver.PGLobby;
import pgserver.PGServerThread;

import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;

/**
 * The PictureGame class launches a client's game.  It handles server connection, lobby selection,
 * and creates a GameplayWindow when a lobby is joined.
 * 
 * @author Billy Robbins
 * @version 2.0
 * @since 11/22/2015
 */

public class PictureGame {

	// CONSTANTS
	private static final String[] LOBBY_TYPE_OPTIONS = {"Public", "Private", "Exit"};
	private static final int LOBBY_OPTION_PUBLIC = JOptionPane.YES_OPTION;
	private static final int LOBBY_OPTION_PRIVATE = JOptionPane.NO_OPTION;
	private static final int LOBBY_OPTION_QUIT = JOptionPane.CANCEL_OPTION;
	private static final String MSG_ERR_PRIVATE_CONNECTION = "ERROR: Could not connect to private lobby";
	private static final String MSG_ERR_PUBLIC_CONNECTION = "ERROR: Could not connect to public lobby";
	private static final String MSG_ERR_INVALID_PORT = "ERROR: Please enter a valid port number";
	private static final String MSG_ERR_INVALID_HOSTNAME = "ERROR: The hostname is not valid";
	private static final String MSG_ERR_SERVER_CONNECTION = "ERROR: Could not connect to the server";
	private static final String MSG_ERR_USERNAME = "That username is already in use.  Please choose another.";
	private static final String MSG_INPUT_USERNAME = "Please enter a username";
	private static final String MSG_LOBBY_TYPE = "What type of lobby do you want to join?";
	private static final String MSG_PRIVATE_KEY = "Please enter the private game key";
	private static final String MSG_PRIVATE_OPTION = "Create or join a private game?";
	private static final String NAME_LOBBY_TYPE = "Lobby Type";
	private static final String NAME_PRIVATE_OPTION = "Create or Join";
	private static final int PRIVATE_OPTION_CREATE = JOptionPane.YES_OPTION;
	private static final String[] PRIVATE_OPTIONS = {"Create", "Join"};
	private static final int DEFAULT_PORT = 6789;
	
	// DATA MEMBERS
	private Socket socket = null;
	private DataOutputStream output;
	private DataInputStream input;
	private String username = "";
	/**
	 * Constructor for the PictureGame class
	 * 
	 * @param hostname host for socket connection
	 * @param port port for socket connection
	 * @throws IOException
	 */
	public PictureGame(String hostname, int port) throws IOException
	{
		// Connect to main server
		socket = new Socket(hostname, port);
		output = new DataOutputStream(socket.getOutputStream());
		input = new DataInputStream(new BufferedInputStream(socket.getInputStream()));
		
		// Get username
		//String username;
		boolean usernameAcceptable = false;
		while(!usernameAcceptable)
		{
			this.username = JOptionPane.showInputDialog(null, MSG_INPUT_USERNAME);
			
			if(username == null)
				System.exit(0);
			
			usernameAcceptable = serverCheckUsername(username);
			if(!usernameAcceptable)
				JOptionPane.showMessageDialog(null, MSG_ERR_USERNAME);
		}
		
		lobbySelect();
	}
    /**
     * allows you to select the lobby type
     */
	public void lobbySelect()
	{
		try
		{
			// Loop until user chooses to quit
			int lobbyType = JOptionPane.showOptionDialog(null, MSG_LOBBY_TYPE, NAME_LOBBY_TYPE, JOptionPane.YES_NO_CANCEL_OPTION,
					JOptionPane.QUESTION_MESSAGE, null, LOBBY_TYPE_OPTIONS, LOBBY_OPTION_QUIT);
			
			if(lobbyType == LOBBY_OPTION_PUBLIC)
			{
				if(joinPublicLobby())
					gameplay();
				else
					JOptionPane.showMessageDialog(null, MSG_ERR_PUBLIC_CONNECTION);
			}
			else if(lobbyType == LOBBY_OPTION_PRIVATE)
			{
				int privateOption = JOptionPane.showOptionDialog(null, MSG_PRIVATE_OPTION, NAME_PRIVATE_OPTION, JOptionPane.YES_NO_OPTION,
						JOptionPane.QUESTION_MESSAGE, null, PRIVATE_OPTIONS, PRIVATE_OPTION_CREATE);
				
				if(privateOption == PRIVATE_OPTION_CREATE)
				{
					int timeLimit = 0;
					int scoreLimit = 0;
					
					boolean valid = false;
					while(!valid)
					{
						try
						{
							timeLimit = Integer.parseInt(JOptionPane.showInputDialog("Enter a round time limit (in seconds)"));
						}
						catch(NumberFormatException nfe)
						{
							timeLimit = -1;
						}
						
						if((timeLimit >= PGLobby.TIMER_MIN) && (timeLimit <= PGLobby.TIMER_MAX))
							valid = true;
						else
							JOptionPane.showMessageDialog(null, "ERROR: Please enter a valid time limit (a number between "
									+ PGLobby.TIMER_MIN + " and " + PGLobby.TIMER_MAX + ")");
					}
					
					valid = false;
					while(!valid)
					{
						try
						{
							scoreLimit = Integer.parseInt(JOptionPane.showInputDialog("Enter a score limit"));
						}
						catch(NumberFormatException nfe)
						{
							scoreLimit = -1;
						}
						
						if((scoreLimit >= PGLobby.SCORE_MIN) && (scoreLimit <= PGLobby.SCORE_MAX))
							valid = true;
						else
							JOptionPane.showMessageDialog(null, "ERROR: Please enter a valid score limit (a number between "
									+ PGLobby.SCORE_MIN + " and " + PGLobby.SCORE_MAX + ")");
					}
					
					if(createPrivateLobby(timeLimit, scoreLimit))
						gameplay();
					else
						JOptionPane.showMessageDialog(null, MSG_ERR_PRIVATE_CONNECTION);
				}  
				else // Join a private lobby
				{
					String key = JOptionPane.showInputDialog(null, MSG_PRIVATE_KEY);
					if(key != null)
					{
						if(joinPrivateLobby(key))
							gameplay();
						else
							JOptionPane.showMessageDialog(null, MSG_ERR_PRIVATE_CONNECTION);
					}
				}
			}
			else // Close connection and quit
			{
				output.close();
				input.close();
				socket.close();
				System.exit(0);
			}
		}
		catch(IOException e)
		{
			e.printStackTrace();
			System.exit(1);
		}
	}
	
	/**
	 * Checks if the user's desired name is available
	 * 
	 * @param username the desired username
	 * @return <b>true</b> if username is available, <b>false</b>
	 * if username is taken
	 */
	private boolean serverCheckUsername(String username)
	{
		try
		{
			output.writeUTF(username);
			output.flush();
			
			String result = input.readUTF();
			return result.equals(PGServerThread.MESSAGE_USERNAME_SUCCESS);
		}
		catch(IOException e)
		{
			System.err.println("ERROR: Username check");
			e.printStackTrace();
			return false;
		}
	}
	
	/**
	 * Connects the user to a public lobby
	 * 
	 * @return <b>true</b> if connection is successful, <b>false</b>
	 * if connection fails
	 */
	private boolean joinPublicLobby()
	{
		try
		{
			output.writeUTF(PGServerThread.MESSAGE_JOIN_PUBLIC);
			output.flush();
			
			String result = input.readUTF();
			return result.equals(PGServerThread.MESSAGE_JOIN_SUCCESS);
		}
		catch(IOException e)
		{
			e.printStackTrace();
			return false;
		}
	}
	
	/**
	 * Brings up an options menu for a private lobby
	 * 
	 * @return <b>true</b> if lobby created and joined successfully,
	 * <b>false</b> if creation or join is unsuccessful
	 */
	private boolean createPrivateLobby(int timeLimit, int scoreLimit)
	{
		try
		{
			output.writeUTF(PGServerThread.MESSAGE_CREATE_PRIVATE + " " + timeLimit + " " + scoreLimit);
			output.flush();
			
			String result = input.readUTF();
			return result.equals(PGServerThread.MESSAGE_JOIN_SUCCESS);
		}
		catch(IOException e)
		{
			e.printStackTrace();
			return false;
		}
	}
	
	/**
	 * Opens a gameplay window and keeps running until gameplay is over
	 */
	private void gameplay()
	{
		new GameplayWindow(socket, this);
	}
	
	/**
	 * Asks input from user for Hostname
	 * @return User inputted hostname
	 * @author Chris Ridgely
	 * @author Steve Jean
	 * 
	 */
	private static String resolveHostname() 
	{
		boolean valid = false;
		String hostname = "";
		
		while(!valid)
		{
			hostname = JOptionPane.showInputDialog(null, "Enter hostname");
			if(hostname == null)
				System.exit(1);
			
			try 
			{
				InetAddress.getByName(hostname);
				valid = true;
			} 
			catch (UnknownHostException e) 
			{
				// TODO Auto-generated catch block
				JOptionPane.showMessageDialog(null, MSG_ERR_INVALID_HOSTNAME);
			}
		}	
		
		return hostname;
	}
	
	/**
	 * Asks input from user for Port
	 * @return User inputted port number
	 * @author Chris Ridgely
	 */
	private static int resolvePort()
	{
		int port = DEFAULT_PORT;
		boolean valid = false;
		while(!valid)
		{
			try
			{
				String portString = JOptionPane.showInputDialog(null, "Enter port");
				if(portString == null)
					System.exit(0);
				
				port = Integer.parseInt(portString);
				return port;
			}
			catch(NumberFormatException e)
			{
				JOptionPane.showMessageDialog(null, MSG_ERR_INVALID_PORT);
			}
		}
		
		return port;
	}
	
	
	/**
	 * Connects the user to a private lobby
	 * 
	 * @param key an identifier to connect the user to the desired lobby
	 * @return <b>true</b> if connection successful, <b>false</b>
	 * if connection fails
	 */
	private boolean joinPrivateLobby(String key)
	{
		try
		{
			output.writeUTF(PGServerThread.MESSAGE_JOIN_PRIVATE + key);
			output.flush();
			
			String result = input.readUTF();
			return result.equals(PGServerThread.MESSAGE_JOIN_SUCCESS);
		}
		catch(IOException e)
		{
			e.printStackTrace();
			return false;
		}
	}
	
	/**
	 * gets the username of the player
	 * @return
	 */
	public String getUsername()
	{
		return this.username;
	}
	
	/**
	 * main
	 * 
	 * @param args does nothing
	 */
	public static void main(String[] args)
	{
		try
		{
			new PictureGame(resolveHostname(), resolvePort());
		}
		catch (IOException e) {
			// TODO Auto-generated catch block
			JOptionPane.showMessageDialog(null, MSG_ERR_SERVER_CONNECTION);
		}
	}
	
}
