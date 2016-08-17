package picturegame;

import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;

/**
 * The DrawZoneEngine class provides a backbone for the DrawZone class
 * to handle clicks on the drawzone.
 * 
 * @author Billy Robbins
 * @version 1.0
 * @since 11/21/2015
 */
public class DrawZoneEngine implements MouseListener, MouseMotionListener {

	// DATA MEMBERS
	private DrawZone parent;
	
	// Constructor
	public DrawZoneEngine(DrawZone p)
	{
		parent = p;
	}
	
	@Override
	public void mouseClicked(MouseEvent e)
	{
		parent.drawAt(e.getX(), e.getY());
	}

	@Override
	public void mouseEntered(MouseEvent e)
	{
		// Does nothing
	}

	@Override
	public void mouseExited(MouseEvent e)
	{
		// Does nothing
	}

	@Override
	public void mousePressed(MouseEvent e)
	{
		// Does nothing
	}

	@Override
	public void mouseReleased(MouseEvent e) {
		parent.mouseRelease();
	}

	@Override
	public void mouseDragged(MouseEvent e)
	{
		parent.drawAt(e.getX(), e.getY());
	}

	@Override
	public void mouseMoved(MouseEvent e)
	{
		// Does nothing
	}

}
