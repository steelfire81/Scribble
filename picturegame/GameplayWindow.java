package picturegame;

import java.awt.FlowLayout;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.Font;
import java.net.Socket;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.BorderFactory;
import javax.swing.text.DefaultCaret;

/**
 * The GameplayWindow class provides the architecture for the main window
 * showed during gameplay.
 * 
 * @author Billy Robbins
 * @author Steve Jean
 * @version 2.0
 * @since 11/22/2015
 */
public class GameplayWindow {

	// CONSTANTS
	private static final String BUTTON_GUESS_TEXT = "Guess";
	private static final String BUTTON_CLEAR_TEXT = "Clear";
	private static final String BUTTON_CHAT_TEXT = "Send";
	private static final String FIELD_STATUS_TEXT = "Waiting for game to start..."; // Default text for the status field
	private static final String WINDOW_NAME = "SCRIBBLE";
	private static final int WINDOW_WIDTH = 770;
	private static final int WINDOW_HEIGHT = 500;
	private static final int DRAW_WIDTH = 550;
	private static final int CHAT_WIDTH = WINDOW_WIDTH - DRAW_WIDTH;
	private static final String LABEL_PLAYERS = "Players:";
	private static final String LABEL_GUESSES = "Previous Guesses:";
	private static final String TEAM_1_NAME = "Team 1:";
	private static final String TEAM_2_NAME = "Team 2:";
	private static final String LABEL_TIMER = "Timer:";
	private static final String LABEL_USER = "player:";
	
	// WINDOW ELEMENTS
	JTextField fieldStatus;
	DrawZone drawZone;
	JButton buttonBlack;
	JButton buttonBlue;
	JButton buttonRed;
	JButton buttonGreen;
	JButton buttonYellow;
	JButton buttonMagenta;
	JButton buttonOrange;
	JTextField fieldColor;
	JTextField fieldGuess;
	JButton buttonGuess;
	JButton buttonClear;
	JTextArea areaChat;
	JTextField fieldChat;
	JButton buttonChat;
	JTextArea areaPlayers;
	JTextArea areaGuesses;
	JTextField fieldTime;
	JTextField team1Score;
	JTextField team2Score;
	JTextField labelUsername;
	JPanel panelDraw;
	JPanel panelDrawZone;
	JPanel panelDrawButtons;
	
	// DATA MEMBERS
	private GameplayWindowEngine engine;
	private PictureGame parent;
	
	/**
	 * constructor for GameplayWindow
	 * 
	 * @param socket the client's socket
	 * @param parent the picturegame to which this 
	 */
	public GameplayWindow(Socket socket, PictureGame p)
	{
		parent = p;
		
		// Initialize main panel and engine
		JPanel panelMain = new JPanel(new BorderLayout());
		engine = new GameplayWindowEngine(socket, this);
		
		// North panel - status (just a text field)
		fieldStatus = new JTextField(FIELD_STATUS_TEXT);
		fieldStatus.setEditable(false);
		panelMain.add(fieldStatus, BorderLayout.NORTH);
		
		// Center panel - draw and guess window
		panelDraw = new JPanel(new BorderLayout());
		panelDrawZone = new JPanel(new BorderLayout());
		drawZone = new DrawZone(engine); // TODO: Modify to support changes to DrawZone constructor
		
		panelDrawZone.add(drawZone, BorderLayout.CENTER);


		JPanel panelDrawButtons = new JPanel(new GridLayout(8, 1));
		
		// Initialize color buttons
		buttonBlack = new JButton();
		buttonBlack.setBackground(Color.BLACK);
		buttonBlack.addActionListener(engine);
		buttonBlue = new JButton();
		buttonBlue.setBackground(Color.BLUE);
		buttonBlue.addActionListener(engine);
		buttonRed = new JButton();
		buttonRed.setBackground(Color.RED);
		buttonRed.addActionListener(engine);
		buttonGreen = new JButton();
		buttonGreen.setBackground(Color.GREEN);
		buttonGreen.addActionListener(engine);
		buttonYellow = new JButton();
		buttonYellow.setBackground(Color.YELLOW);
		buttonYellow.addActionListener(engine);
		buttonMagenta = new JButton();
		buttonMagenta.setBackground(Color.MAGENTA);
		buttonMagenta.addActionListener(engine);
		buttonOrange = new JButton();
		buttonOrange.setBackground(Color.ORANGE);
		buttonOrange.addActionListener(engine);
		fieldColor = new JTextField();
		fieldColor.setEditable(false);
		fieldColor.setBackground(Color.BLACK);
		fieldColor.setBorder(BorderFactory.createLineBorder(Color.black,4));

		panelDrawButtons.add(fieldColor);
		panelDrawButtons.add(buttonRed);
		panelDrawButtons.add(buttonBlack);
		panelDrawButtons.add(buttonOrange);
		panelDrawButtons.add(buttonYellow);
		panelDrawButtons.add(buttonGreen);
		panelDrawButtons.add(buttonBlue);
		panelDrawButtons.add(buttonMagenta);
		
		
		JPanel botButtonPanel = new JPanel(new FlowLayout());
		
		panelDrawZone.add(panelDrawButtons, BorderLayout.EAST);
		panelDraw.add(panelDrawZone, BorderLayout.CENTER);
		JPanel panelGuess = new JPanel(new BorderLayout());
		fieldGuess = new JTextField();
		fieldGuess.addKeyListener(engine);
		panelGuess.add(fieldGuess, BorderLayout.CENTER);

		buttonGuess = new JButton(BUTTON_GUESS_TEXT);
		buttonGuess.addActionListener(engine);
		buttonClear = new JButton(BUTTON_CLEAR_TEXT);
		buttonClear.addActionListener(engine);
				
		botButtonPanel.add(buttonGuess);
		botButtonPanel.add(buttonClear);
		
		//panelGuess.add(buttonGuess, BorderLayout.EAST);
		panelGuess.add(botButtonPanel,BorderLayout.EAST);
		panelDraw.add(panelGuess, BorderLayout.SOUTH);
		panelDraw.setPreferredSize(new Dimension(DRAW_WIDTH, WINDOW_HEIGHT));
		panelMain.add(panelDraw, BorderLayout.CENTER);
		
		// East panel - chat and score
		JPanel panelChatScores = new JPanel(new BorderLayout());
		JPanel panelScores = new JPanel(new GridLayout(3, 2));
		JTextField labelTimer = new JTextField(LABEL_TIMER);
		labelTimer.setEditable(false);
		panelScores.add(labelTimer);
		fieldTime = new JTextField("0");
		fieldTime.setEditable(false);
		panelScores.add(fieldTime);
		JTextField labelTeam1 = new JTextField(TEAM_1_NAME);
		labelTeam1.setEditable(false);
		panelScores.add(labelTeam1);
		team1Score = new JTextField("0");
		team1Score.setEditable(false);
		panelScores.add(team1Score);
		JTextField labelTeam2 = new JTextField(TEAM_2_NAME);
		labelTeam2.setEditable(false);
		panelScores.add(labelTeam2);
		team2Score = new JTextField("0");
		team2Score.setEditable(false);
		panelScores.add(team2Score);
		panelChatScores.add(panelScores, BorderLayout.NORTH);
		JPanel panelChat = new JPanel(new BorderLayout());
		areaChat = new JTextArea();
		DefaultCaret caret = (DefaultCaret) areaChat.getCaret();
		caret.setUpdatePolicy(DefaultCaret.ALWAYS_UPDATE);
		areaChat.setLineWrap(true);
		areaChat.setWrapStyleWord(true);
		areaChat.setEditable(false);
		JScrollPane scrollText = new JScrollPane(areaChat, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
				JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
		panelChat.add(scrollText, BorderLayout.CENTER);
		JPanel panelChatMessage = new JPanel(new BorderLayout());
		fieldChat = new JTextField();
		fieldChat.addKeyListener(engine);
		panelChatMessage.add(fieldChat, BorderLayout.CENTER);
		buttonChat = new JButton(BUTTON_CHAT_TEXT);
		buttonChat.addActionListener(engine);
		panelChatMessage.add(buttonChat, BorderLayout.EAST);
		panelChat.add(panelChatMessage, BorderLayout.SOUTH);
		panelChat.setPreferredSize(new Dimension(CHAT_WIDTH, WINDOW_HEIGHT));
		panelChatScores.add(panelChat, BorderLayout.CENTER);
		panelMain.add(panelChatScores, BorderLayout.EAST);
		
		// West panel - players list and previous guesses
		JPanel panelPlayersGuesses = new JPanel(new GridLayout(2, 1));
		JPanel panelGuessBorder = new JPanel(new BorderLayout());		

		labelUsername = new JTextField(LABEL_USER);
		labelUsername.setEditable(false);
		
		Font font = labelUsername.getFont();
		labelUsername.setFont(font.deriveFont(Font.BOLD));	
		panelGuessBorder.add(labelUsername,BorderLayout.NORTH);
			
		JPanel panelPlayers = new JPanel(new BorderLayout());
		areaPlayers = new JTextArea("");
		areaPlayers.setEditable(false);
		areaPlayers.setWrapStyleWord(true);
		JScrollPane scrollPlayers = new JScrollPane(areaPlayers, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
				JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
		panelPlayers.add(scrollPlayers, BorderLayout.CENTER);
		JTextField labelPlayers = new JTextField(LABEL_PLAYERS);
		labelPlayers.setEditable(false);
		panelPlayers.add(labelPlayers, BorderLayout.NORTH);
		panelPlayersGuesses.add(panelPlayers);
		//panelGuessBorder.add(panelPlayersGuesses,BorderLayout.CENTER);
		
		
		JPanel panelGuesses = new JPanel(new BorderLayout());
		areaGuesses = new JTextArea("");
		areaGuesses.setEditable(false);
		JScrollPane scrollGuesses = new JScrollPane(areaGuesses, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
				JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
		areaGuesses.setLineWrap(true);
		areaGuesses.setWrapStyleWord(true);
		DefaultCaret caret1 = (DefaultCaret) areaGuesses.getCaret();
		caret1.setUpdatePolicy(DefaultCaret.ALWAYS_UPDATE);
		panelGuesses.add(scrollGuesses, BorderLayout.CENTER);
		JTextField labelGuesses = new JTextField(LABEL_GUESSES);
		labelGuesses.setEditable(false);
		panelGuesses.add(labelGuesses, BorderLayout.NORTH);
		panelPlayersGuesses.add(panelGuesses);
		
		panelGuessBorder.add(panelPlayersGuesses,BorderLayout.CENTER);
		//panelMain.add(panelPlayersGuesses, BorderLayout.WEST);
		panelMain.add(panelGuessBorder, BorderLayout.WEST);
		
		// Make this all visible
		JFrame frame = new JFrame(WINDOW_NAME);
		frame.setContentPane(panelMain);
		frame.setSize(WINDOW_WIDTH, WINDOW_HEIGHT);
		frame.addWindowListener(engine);
		frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
		frame.setResizable(false);
		frame.setVisible(true);
	
			
		drawZone.reset();
		engine.initialize(parent.getUsername());
	}
	
	
	/**
	 * ends gameplay in the PictureGame
	 */
	public void close()
	{
		parent.lobbySelect();
	}
	
}
