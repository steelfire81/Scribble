package picturegame;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.image.BufferedImage;
import javax.swing.JPanel;

/**
 * The DrawZone class provides a panel the user can draw in and
 * the client's listened can update when another user is drawing.
 * 
 * @author Billy Robbins
 * @version 1.0
 * @since 11/21/2015
 */
public class DrawZone extends JPanel {

	// CONSTANTS
	private static final Color COLOR_BACKGROUND = Color.WHITE;
	
	// DATA MEMBERS
	private Color currentColor;
	private BufferedImage image;
	private boolean enabled;
	private GameplayWindowEngine gameplayEngine;
	private int lastX;
	private int lastY;
	
	// Constructor
	public DrawZone(GameplayWindowEngine e)
	{
		DrawZoneEngine engine = new DrawZoneEngine(this);
		this.addMouseListener(engine);
		this.addMouseMotionListener(engine);
		this.setBackground(COLOR_BACKGROUND);
		gameplayEngine = e;
		enabled = false;
	}
	
	/**
	 * resets the drawzone to an empty rectangle of color COLOR_BACKGROUND
	 */
	public void reset()
	{
		image = new BufferedImage(this.getWidth(), this.getHeight(), BufferedImage.TYPE_INT_RGB);
		currentColor = Color.BLACK;
		lastX = -1;
		lastY = -1;
		
		
		// Set background to default color
		Graphics g = image.createGraphics();
		g.setColor(COLOR_BACKGROUND);
		g.fillRect(0, 0, image.getWidth(), image.getHeight());
		
		repaint();
	}
	
	/**
	 * Sets the color drawing will be from now on
	 * 
	 * @param c new color
	 */
	public void setColor(Color c)
	{
		currentColor = c;
	}
	
	/**
	 * draws the pixel at a coordinate
	 * 
	 * @param x x coordinate
	 * @param y y coordinate
	 */
	public void drawAt(int x, int y)
	{
		if(enabled)
		{
			if(lastX == -1)
				lastX = x;
			if(lastY == -1)
				lastY = y;
			
			Graphics g = image.createGraphics();
			g.setColor(currentColor);
			g.drawLine(lastX, lastY, x, y);
			lastX = x;
			lastY = y;
			repaint();
			
			// Send to other users via gameplay engine
			gameplayEngine.drawingAt(x, y, currentColor);
		}
	}
	
	/**
	 * draws pixels received from the network
	 * 
	 * @param x x coordinate
	 * @param y y coordinate
	 * @param c color
	 */
	public void drawReceivedAt(int x, int y, Color c)
	{
		if(lastX == -1)
			lastX = x;
		if(lastY == -1)
			lastY = y;
		
		Graphics g = image.createGraphics();
		g.setColor(c);
		g.drawLine(lastX, lastY, x, y);
		lastX = x;
		lastY = y;
		repaint();
	}
	
	/**
	 * Updates the panel with the new image
	 */
	@Override
	protected void paintComponent(Graphics g)
	{
		super.paintComponent(g);
		g.drawImage(image, 0, 0, null);
	}
	
	/**
	 * Makes it so a user can draw in this drawzone
	 */
	public void enableDrawing()
	{
		enabled = true;
	}
	
	/**
	 * Makes it so a user can't draw in this drawzone
	 */
	public void disableDrawing()
	{
		enabled = false;
	}
	
	/**
	 * Removes the last x/y coordinates
	 */
	public void mouseRelease()
	{
		if(enabled)
		{
			lastX = -1;
			lastY = -1;
			gameplayEngine.mouseRelease();
		}
	}
	
	/**
	 * Removes the last x/y coordinates if someone else is drawing
	 */
	public void networkMouseRelease()
	{
		lastX = -1;
		lastY = -1;
	}
	
}
