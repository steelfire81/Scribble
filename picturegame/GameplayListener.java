package picturegame;

import java.awt.Color;
import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.net.Socket;
import java.util.Scanner;

import javax.swing.JOptionPane;

import pgserver.PGServer;

/**
 * The GameplayListener class listens for network interactions with a client's
 * gameplay window.
 * 
 * @author Billy Robbins
 * @version 2.0
 * @since 11/22/2015
 */
public class GameplayListener extends Thread {

	// CONSTANTS
	private static final String MSG_DRAWING = "Now Drawing: ";
	private static final String MSG_GUESSING = "Now Guessing";
	private static final String MSG_START = "GAME IS STARTING";
	private static final String MSG_ROUND_OVER = "Round over!";
	private static final String MSG_CORRECT = "Correct guess!";
	private static final String MSG_WAIT = "Waiting...";
	
	// DATA MEMBERS
	private Socket socket = null;
	private GameplayWindowEngine parent;
	private boolean active;
	
	/**
	 * constructor for GameplayListener
	 * 
	 * @param s the socket connecting this client and the server
	 * @param p parent GameplayWindowEngine
	 */
	public GameplayListener(Socket s, GameplayWindowEngine p)
	{
		super("GameplayListener");
		socket = s;
		parent = p;
		active = false;
	}
	
	/**
	 * start listening for incoming messages
	 * 
	 */
	public void run()
	{
		active = true;
		
		try
		{
			DataInputStream input = new DataInputStream(new BufferedInputStream(socket.getInputStream()));
			
			while(active)
			{
				String message = input.readUTF();
				boolean parsed = false;
				
				while(!parsed)
				{
					try
					{
						if(message.startsWith(PGServer.HEADER_CHAT))
						{
							String chatMessage = message.substring(PGServer.HEADER_CHAT.length());
							parent.chatMessageReceived(chatMessage);
							parsed = true;
						}
						else if(message.startsWith(PGServer.HEADER_DRAWING))
						{
							if(message.contains(PGServer.DRAWING_RELEASE))
							{
								parent.networkMouseRelease();
								parsed = true;
							}
							else if(message.contains(PGServer.DRAWING_CLEAR))
							{
								parent.networkClear();
								parsed = true;
							}
							else
							{
								Scanner drawMessageScan = new Scanner(message.substring(PGServer.HEADER_DRAWING.length()));
								int x = drawMessageScan.nextInt();
								int y = drawMessageScan.nextInt();
								int rgb = drawMessageScan.nextInt();
								parent.drawingReceivedAt(x, y, new Color(rgb));
								drawMessageScan.close();
								parsed = true;
							}
						}
						else if(message.startsWith(PGServer.HEADER_ROLE))
						{
							if(message.startsWith(PGServer.HEADER_ROLE + PGServer.ROLE_DRAW))
							{
								parent.setDrawing(true);
								
								String word = message.substring(PGServer.HEADER_ROLE.length() + PGServer.ROLE_DRAW.length());
								String drawingMessage = MSG_DRAWING + " " + word;
								parent.setStatus(drawingMessage);
								parent.chatMessageReceived(drawingMessage);
								parsed = true;
							}
							else // Guessing
							{
								parent.setDrawing(false);
								
								parent.setStatus(MSG_GUESSING);
								parent.chatMessageReceived(MSG_GUESSING);
								parsed = true;
							}
						}
						else if(message.startsWith(PGServer.HEADER_GAME_UPDATE))
						{
							if(message.startsWith(PGServer.HEADER_GAME_UPDATE + PGServer.UPDATE_START))
							{
								parent.chatMessageReceived(MSG_START);
								parsed = true;
							}
							else if(message.startsWith(PGServer.HEADER_GAME_UPDATE + PGServer.UPDATE_ROUND_END))
							{
								parent.chatMessageReceived(MSG_ROUND_OVER);
								parent.roundEnd();
								parent.setStatus(MSG_WAIT);
								parsed = true;
							}
							else if(message.startsWith(PGServer.HEADER_GAME_UPDATE + PGServer.UPDATE_CORRECT))
							{
								parent.correctGuess();
								parent.chatMessageReceived(MSG_CORRECT);
								parsed = true;
							}
							else if(message.startsWith(PGServer.HEADER_GAME_UPDATE + PGServer.UPDATE_SCORE))
							{
								parseScoreUpdate(message.substring(PGServer.HEADER_GAME_UPDATE.length() + PGServer.UPDATE_SCORE.length()));
								parsed = true;
							}
							else if(message.startsWith(PGServer.HEADER_GAME_UPDATE + PGServer.UPDATE_GAME_END))
							{
								parent.gameEnded(message.substring(PGServer.HEADER_GAME_UPDATE.length() + PGServer.UPDATE_GAME_END.length()));
								parsed = true;
							}
							else if(message.startsWith(PGServer.HEADER_GAME_UPDATE + PGServer.UPDATE_REBALANCING))
							{
								parent.chatMessageReceived(PGServer.UPDATE_REBALANCING);
								parsed = true;
							}
						}
						else if(message.startsWith(PGServer.HEADER_CLIENT_LIST))
						{
							parent.updatePlayerList(message.substring(PGServer.HEADER_CLIENT_LIST.length()));
							parsed = true;
						}
						else if(message.startsWith(PGServer.HEADER_GUESS_LIST))
						{
							parent.updateGuessList(message.substring(PGServer.HEADER_GUESS_LIST.length()));
							parsed = true;
						}
						else if(message.startsWith(PGServer.HEADER_TIMER))
						{
							parent.updateTime(Integer.parseInt(message.substring(PGServer.HEADER_TIMER.length())));
							parsed = true;
						}
						else if(message.startsWith(PGServer.HEADER_REMOVED))
						{
							active = false;
							parsed = true;
						}
						else
						{
							System.err.println("ERROR: Could not parse message - " + message);
							parsed = true;
						}
					}
					catch(NullPointerException npe){}
					catch(IllegalArgumentException iae){}
				}
			}
		}
		catch(IOException e)
		{
			JOptionPane.showMessageDialog(null, "ERROR: Connection to server lost");
			System.exit(1);
		}
	}
	
	/**
	 * takes a score update and sets scoreboard to updated values
	 * 
	 * @param scores string containing first score followed by space
	 * and then second score (as formatted by PGServer.guessReceived)
	 */
	private void parseScoreUpdate(String scores)
	{
		Scanner scoreScan = new Scanner(scores);
		parent.updateScore(scoreScan.nextInt(), scoreScan.nextInt());
		scoreScan.close();
	}
	
}
