package picturegame;

import java.awt.Color;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import javax.swing.JOptionPane;
import pgserver.PGServerThread;

/**
 * The GameplayWindowEngine class is the backbone for client-side actions
 * in the GameplayWindow and handles events that the GameplayListener hears
 * on the network side.
 * 
 * @author Billy Robbins
 * @author Steve Jean
 * @version 2.0
 * @since 11/22/2015
 */
public class GameplayWindowEngine implements ActionListener, WindowListener, KeyListener {
	
	// CONSTANTS - Message Prefixes
	public static final String HEADER_CHAT = "CHAT: ";
	public static final String HEADER_DRAWING = "DRAW: ";
	public static final String HEADER_GUESS = "GUESS: ";
	public static final String HEADER_INFO = "INFO";
	public static final String HEADER_USER = "Player: ";	
	public static final String HEADER_CLEAR = "CLEAR";
	
	// CONSTANTS - Drawing messages
	public static final String DRAWING_RELEASE = "Release";
	
	// CONSTANTS - Sound files
	/**
	 * path to a sound file to be played when a round ends by correct guess
	 * CREDIT: the provided sound file was downloaded from http://www.freesound.org/people/erkanozan/sounds/51743/
	 *         and is licensed under the Creative Commons 0 License
	 */
	private static final String SOUND_GUESS_CORRECT = "round_over_guess.wav";
	
	/**
	 * path to a sound file to be played any time a round ends
	 * CREDIT: the provided sound file was downloaded from https://freesound.org/people/domrodrig/sounds/116779/
	 *         and is licensed under the Creative Commons 0 License
	 */
	private static final String SOUND_TIMEOUT = "round_over_timeout.wav";
	
	// CONSTANTS - Error Messages
	private static final String MSG_ERR_CHAT_SEND = "ERROR: Could not send chat message";
	private static final String MSG_ERR_DRAWING_SEND = "ERROR: Could not sent drawing update";
	private static final String MSG_ERR_CONNECTION = "ERROR: Could not connect to server";
	private static final String MSG_ERR_GUESS_SEND = "ERROR: Could not send guess";
	private static final String MSG_ERR_INFO = "ERROR: Could not update lobby info";
	
	// DATA MEMBERS
	private GameplayWindow parent;
	private DataOutputStream output;
	private GameplayListener listener;
	private boolean isDrawing = false; 
	
	/**
	 * constructor for the GameplayWindowEngine class
	 * 
	 * @param s socket to which this window is connected
	 * @param p the window to which this engine belongs
	 */
	public GameplayWindowEngine(Socket s, GameplayWindow p)
	{
		parent = p;
		
		try
		{
			
			output = new DataOutputStream(s.getOutputStream());
			listener = new GameplayListener(s, this);
			listener.start();
			 
		}
		catch(IOException e)
		{
			e.printStackTrace();
			System.exit(1);
		}
	}
	
	/**
	 * called whenever an action is performed in the window
	 */
	public void actionPerformed(ActionEvent e)
	{
		if((e.getSource() == parent.buttonChat) && !parent.fieldChat.getText().equals(""))
			sendChatMessage(parent.fieldChat.getText());
		else if((e.getSource() == parent.buttonGuess) && !parent.fieldGuess.getText().equals(""))
			guess(parent.fieldGuess.getText());
		else if(e.getSource() == parent.buttonBlack)
			{
			parent.drawZone.setColor(parent.buttonBlack.getBackground());
			parent.fieldColor.setBackground(Color.BLACK);
			}
		else if(e.getSource() == parent.buttonBlue)
			{
			parent.drawZone.setColor(parent.buttonBlue.getBackground());
			parent.fieldColor.setBackground(Color.BLUE);
			}
		else if(e.getSource() == parent.buttonGreen)
			{
			parent.drawZone.setColor(parent.buttonGreen.getBackground());
			parent.fieldColor.setBackground(Color.GREEN);
			}
		else if(e.getSource() == parent.buttonYellow)
			{
			parent.drawZone.setColor(parent.buttonYellow.getBackground());
			parent.fieldColor.setBackground(Color.YELLOW);
			}
		else if(e.getSource() == parent.buttonOrange)
			{
			parent.drawZone.setColor(parent.buttonOrange.getBackground());
			parent.fieldColor.setBackground(Color.ORANGE);
			}
		else if(e.getSource() == parent.buttonMagenta)
			{
			parent.drawZone.setColor(parent.buttonMagenta.getBackground());
			parent.fieldColor.setBackground(Color.MAGENTA);
			}
		else if(e.getSource() == parent.buttonRed)
			{
			parent.drawZone.setColor(parent.buttonRed.getBackground());
			parent.fieldColor.setBackground(Color.RED);
			}
		else if((e.getSource() == parent.buttonClear) && isDrawing)
			{
				try
				{
					output.writeUTF(HEADER_CLEAR);
					output.flush();
				}
				catch(IOException clearException)
				{
					parent.areaChat.append(MSG_ERR_DRAWING_SEND + "\n");
				}
				parent.drawZone.reset();
			}
		
	}
	
	/**
	 * send a chat message when the send button is clicked
	 * 
	 * @param message chat message to send
	 */
	private void sendChatMessage(String message)
	{
		parent.fieldChat.setText("");
		
		try
		{
			output.writeUTF(HEADER_CHAT + message);
			output.flush();
		}
		catch(IOException e)
		{
			parent.areaChat.append(MSG_ERR_CHAT_SEND + "\n");
		}
	}
	
	/**
	 * called when a listened receives a new chat message that
	 * adds the message to the chat area
	 * 
	 * @param message incoming chat message
	 */
	public void chatMessageReceived(String message)
	{
		parent.areaChat.append(message + "\n");
	}
	
	/**
	 * sets the window's status message
	 * 
	 * @param message the new status message
	 */
	public void setStatus(String message)
	{
		parent.fieldStatus.setText(message);
	}
	
	/**
	 * initialize window with information about the lobby
	 */
	public void getLobbyInfo()
	{
		try
		{
			output.writeUTF(HEADER_INFO);
			output.flush();
		}
		catch(IOException e)
		{
			parent.areaChat.append(MSG_ERR_CONNECTION + "\n");
		}
	}
	
	/**
	 * tells engine if this user is drawing or not
	 * 
	 * @param d whether or not this user is drawing
	 */
	public void setDrawing(boolean d)
	{
		parent.drawZone.reset();
		if(d)
			enableDrawzone();
		else
			disableDrawzone();
	}

	@Override
	public void windowActivated(WindowEvent arg0)
	{
		//does nothing	

	}

	@Override
	public void windowClosed(WindowEvent arg0)
	{	
		try
		{
			output.writeUTF(PGServerThread.MESSAGE_LEAVE_LOBBY);
			output.flush();
		}
		catch(IOException e)
		{
			e.printStackTrace();
			System.exit(1);
		}
		
		parent.close();
	}

	@Override
	public void windowClosing(WindowEvent arg0)
	{
		// Does nothing
	}

	@Override
	public void windowDeactivated(WindowEvent arg0)
	{
		// Does nothing
	}

	@Override
	public void windowDeiconified(WindowEvent arg0)
	{
		// Does nothing
	}

	@Override
	public void windowIconified(WindowEvent arg0)
	{
		// Does nothing
	}

	@Override
	public void windowOpened(WindowEvent arg0)
	{
		// Does nothing
	}

	@Override
	public void keyTyped(KeyEvent e)
	{
		// Does nothing
	}

	@Override
	public void keyPressed(KeyEvent e)
	{
		if((e.getKeyCode() == KeyEvent.VK_ENTER) && !parent.fieldChat.getText().equals(""))
			sendChatMessage(parent.fieldChat.getText());
		else if((e.getKeyCode()== KeyEvent.VK_ENTER) && !parent.fieldGuess.getText().equals(""))
			guess(parent.fieldGuess.getText());

	}

	@Override
	public void keyReleased(KeyEvent e)
	{
		// Does nothing
	}
	
	/**
	 * updates the scoreboard panel with new score
	 * 
	 * @param team1score team 1's score
	 * @param team2score team 2's score
	 */
	public void updateScore(int team1score, int team2score)
	{
		parent.team1Score.setText(Integer.toString(team1score));
		parent.team2Score.setText(Integer.toString(team2score));
	}
	
	/**
	 * Allow user to draw in their drawzone
	 */
	public void enableDrawzone()
	{
		isDrawing = true;
		parent.drawZone.enableDrawing();
		parent.buttonGuess.setEnabled(false);
		parent.buttonClear.setEnabled(true);
	}
	
	/**
	 * Disallow user from drawing in their drawzone
	 */
	public void disableDrawzone()
	{
		
		parent.drawZone.disableDrawing();
		parent.buttonGuess.setEnabled(true);
		parent.buttonClear.setEnabled(false);
	}
	
	/**
	 * Called when drawzone is drawn on
	 * @param x x coordinate
	 * @param y y coordinate
	 * @param c color
	 */
	public void drawingAt(int x, int y, Color c)
	{
		try
		{
			output.writeUTF(HEADER_DRAWING + x + " " + y + " " + c.getRGB());
			output.flush();
		}
		catch(IOException e)
		{
			parent.areaChat.append(MSG_ERR_DRAWING_SEND + "\n");
		}
	}
	
	/**
	 * called when drawing update is received
	 * 
	 * @param x x coordinate
	 * @param y y coordinate
	 * @param c color
	 */
	public void drawingReceivedAt(int x, int y, Color c)
	{
		parent.drawZone.drawReceivedAt(x, y, c);
	}
	
	/**
	 * sends a guess to the server
	 * 
	 * @param guess the guessed word/phrase
	 */
	public void guess(String guess)
	{
		parent.fieldGuess.setText("");
		
		try
		{
			output.writeUTF(HEADER_GUESS + guess);
			output.flush();
		}
		catch(IOException e)
		{
			parent.areaChat.append(MSG_ERR_GUESS_SEND + "\n");
		}
	}
	
	
	/**
	 * Tells other players on team that mouse has been lifted
	 */
	public void mouseRelease()
	{
		try
		{
			output.writeUTF(HEADER_DRAWING + DRAWING_RELEASE);
			output.flush();
		}
		catch(IOException e)
		{
			parent.areaChat.append(MSG_ERR_DRAWING_SEND + "\n");
		}
	}
	
	/**
	 * Lifts the drawer's mouse off the drawing
	 */
	public void networkMouseRelease()
	{
		parent.drawZone.networkMouseRelease();
	}
	
	/**
	 * updates the list of players on the server
	 * 
	 * @param playerList the list of players as a single string
	 */
	public void updatePlayerList(String playerList)
	{
		parent.areaPlayers.setText(playerList);
	}
	
	public void updateCurrentPlayer(String player)
	{
		parent.labelUsername.setText(HEADER_USER + player);
	}
	
	/**
	 * updates the list of guesses for the team
	 * 
	 * @param guessList the list of guesses for the team
	 */
	public void updateGuessList(String guessList)
	{
		parent.areaGuesses.setText(guessList);
	}
	
	/**
	 * updates timer field
	 * 
	 * @param time current round time
	 */
	public void updateTime(int time)
	{
		parent.fieldTime.setText(Integer.toString(time));
	}
	
	/**
	 * gets lobby information for this window
	 */
	public void initialize(String username)
	{
		try
		{
			updateCurrentPlayer(username);
			output.writeUTF(HEADER_INFO);
			output.flush();
		}
		catch(IOException e)
		{
			parent.areaChat.append(MSG_ERR_INFO);
		}
	}
	
	/**
	 * lets the player know the game has ended
	 * 
	 * @param message message to display to the user
	 */
	public void gameEnded(String message)
	{
		JOptionPane.showMessageDialog(parent.drawZone, message);
		parent.drawZone.reset();
		parent.drawZone.disableDrawing();
		parent.fieldStatus.setText("Waiting...");
	}
	
	/**
	 * handles a network drawer clearing the drawing
	 */
	public void networkClear()
	{
		parent.drawZone.reset();
	}
	
	/**
	 * notifies the player the guess entered was correct
	 */
	public void correctGuess()
	{
		SoundPlayer.playSound(SOUND_GUESS_CORRECT);
	}
	
	/**
	 * notifies the player the round has ended
	 */
	public void roundEnd()
	{
		SoundPlayer.playSound(SOUND_TIMEOUT);
		parent.areaGuesses.setText("");
	}
}
