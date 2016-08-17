package pgserver;

/**
 * The Timer class runs for a PGLobby and constantly ticks down, alerting
 * the lobby at each tick of the current time.
 * 
 * @author Billy Robbins
 * @version 1.0
 * @since 11/22/2015
 */
public class Timer extends Thread {

	// CONSTANTS
	private static final int TICK_MILLISECONDS = 1000;
	
	// DATA MEMBERS
	private PGLobby parent;
	private int currentTime;
	private boolean active;
	
	/**
	 * constructor
	 * 
	 * @param p the lobby to which this timer belongs
	 */
	public Timer(PGLobby p)
	{
		parent = p;
		currentTime = 0;
		active = false;
	}
	
	@Override
	/**
	 * begins timer ticks
	 */
	public void run()
	{
		active = true;
		
		while(active)
		{
			try
			{
				sleep(TICK_MILLISECONDS);
				
				if(currentTime > 0)
					currentTime -= TICK_MILLISECONDS / 1000;
				if(currentTime < 0)
					currentTime = 0;
				
				tick();
			}
			catch(InterruptedException e)
			{
				System.err.println("Interrupt called on timer (this shouldn't happen)");
			}
		}
	}
	
	/**
	 * sets the timer to the given time
	 * 
	 * @param t time from which to count down
	 */
	public void setCurrentTime(int t)
	{
		currentTime = t;
	}
	
	/**
	 * tells the lobby that the timer has ticked and the current time
	 */
	private void tick()
	{
		parent.timerTick(currentTime);
	}
	
	/**
	 * stops the timer
	 */
	public void deactivate()
	{
		active = false;
	}
	
	/**
	 * returns whether or not this timer is active
	 * 
	 * @return <b>true</b> if active, <b>false</b> otherwise
	 */
	public boolean isActive()
	{
		return active;
	}
	
}
