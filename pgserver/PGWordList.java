package pgserver;

import java.util.ArrayList;
import java.util.Random;

/**
 * The PGWordList class maintains a list of words and keeps
 * track of what words have already been played.
 * 
 * @author Billy Robbins
 * @version 1.0
 * @since 11/20/2015
 */
public class PGWordList {
	
	// DATA MEMBERS
	private String name;
	private ArrayList<String> words;
	private boolean[] used;
	
	/**
	 * constructor for the PGWordList class
	 * 
	 * @param n the name of the word list (a descriptor)
	 * @param w the list of words
	 */
	public PGWordList(String n, ArrayList<String> w)
	{
		name = n;
		words = w;
		
		used = new boolean[words.size()];
		reset();
	}
	
	/**
	 * returns the name (descriptor) of this word list
	 * 
	 * @return this word list's name (descriptor)
	 */
	public String getName()
	{
		return name;
	}
	
	/**
	 * returns the list of words belonging to this word list
	 * 
	 * @return ArrayList with all words in this word list
	 */
	public ArrayList<String> getWords()
	{
		return words;
	}
	
	/**
	 * gets a random word from this word list that has not been used
	 * 
	 * @return a word from the list that has not been used
	 */
	public String getRandomWord()
	{
		// If all words in this list have been used, reset the list
		if(allUsed())
			reset();
		
		ArrayList<String> unusedWords = new ArrayList<String>();
		for(int i = 0; i < words.size(); i++)
			if(!used[i])
				unusedWords.add(words.get(i));
		
		Random rand = new Random();
		String word = unusedWords.get(rand.nextInt(unusedWords.size()));
		
		// Set this word to used
		for(int i = 0; i < words.size(); i++)
			if(words.get(i).equals(word))
			{
				used[i] = true;
				break;
			}
		
		return word;
	}
	
	/**
	 * says whether or not all words in this list have been used
	 * 
	 * @return <b>true</b> if all words in this list have been used,
	 * <b>false</b> if any words have not been used
	 */
	public boolean allUsed()
	{
		for(int i = 0; i < used.length; i++)
			if(!used[i])
				return false;
		
		return true;
	}
	
	/**
	 * sets used value for all words in this list to false
	 */
	public void reset()
	{
		for(int i = 0; i < used.length; i++)
			used[i] = false;
	}
}
