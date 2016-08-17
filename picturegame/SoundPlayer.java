/**
 * This class is responsible for playing sound files.
 * 
 * @author Billy Robbins
 * @since 12/15/15
 * @version 1.0
 */

package picturegame;

import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;

public class SoundPlayer extends Thread {

	// CONSTANTS
	private static final String THREAD_NAME = "SoundPlayer";
	
	// DATA MEMBERS
	private String path;
	
	/**
	 * Constructor for SoundPlayer taking just the path to the sound
	 * 
	 * @param pathToSound filepath to the sound file
	 */
	private SoundPlayer(String pathToSound)
	{
		super(THREAD_NAME);
		path = pathToSound;
	}
	
	/**
	 * runs the thread, playing the given sound file
	 */
	public void run()
	{
		try
		{
			Clip clip = AudioSystem.getClip();
			AudioInputStream sound = AudioSystem.getAudioInputStream(
					getClass().getResource(path));
			clip.open(sound);
			clip.start();
		}
		catch(Exception e)
		{
			System.err.println("Could not play sound " + path);
			System.err.println(e.getMessage());
		}
	}
	
	/**
	 * Plays a given sound
	 * 
	 * @param path filepath to desired sound file
	 */
	public static void playSound(String path)
	{
		SoundPlayer player = new SoundPlayer(path);
		player.start();
	}
	
	
	
}
