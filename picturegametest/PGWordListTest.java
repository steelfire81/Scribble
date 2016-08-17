package picturegametest;

import static org.junit.Assert.*;

import java.util.ArrayList;

import org.junit.Before;
import org.junit.Test;

import pgserver.PGWordList;

public class PGWordListTest {

	// Constants
	private static final String NAME = "animals";
	private static final String[] WORDS = {"cat", "dog", "mouse"};
	
	// Data Members
	private PGWordList wordlist;
	
	@Before
	public void initialize()
	{
		ArrayList<String> words = new ArrayList<String>();
		for(int i = 0; i < WORDS.length; i++)
			words.add(WORDS[i]);
		
		wordlist = new PGWordList(NAME, words);
	}
	
	@Test
	public void testGetName()
	{
		assertTrue(wordlist.getName().equals(NAME));
	}
	
	@Test
	public void testGetWords()
	{		
		boolean[] found = new boolean[WORDS.length];
		assertNotNull(wordlist.getWords());
		for(int i = 0; i < wordlist.getWords().size(); i++)
		{
			String curr = wordlist.getWords().get(i);
			for(int j = 0; j < WORDS.length; j++)
				if(curr.equals(WORDS[j]))
				{
					found[j] = true;
					break;
				}
		}
		
		for(int i = 0; i < found.length; i++)
			assertTrue(found[i]);
	}
	
	@Test
	public void testGetRandomWord()
	{
		ArrayList<String> usedWords = new ArrayList<String>();
		
		for(int i = 0; i < wordlist.getWords().size(); i++)
		{
			String curr = wordlist.getRandomWord();
			for(int j = 0; j < usedWords.size(); j++)
				if(curr.equals(usedWords.get(j)))
					fail(); // Should not return duplicate word until entire list has been used
			
			usedWords.add(curr);
		}
		
		// At this point, all words have been used and list should reset
		String postResetWord = wordlist.getRandomWord();
		assertNotNull(postResetWord);
		boolean found = false;
		for(int i = 0; i < usedWords.size(); i++)
			if(usedWords.get(i).equals(postResetWord))
			{
				found = true;
				break;
			}
		assertTrue(found);
	}
	
	@Test
	public void testAllUsed()
	{
		for(int i = 0; i < wordlist.getWords().size(); i++)
			wordlist.getRandomWord();
		
		assertTrue(wordlist.allUsed());
	}
	
	@Test
	public void testReset()
	{
		for(int i = 0; i < wordlist.getWords().size(); i++)
			wordlist.getRandomWord();
		
		assertTrue(wordlist.allUsed());
		wordlist.reset();
		assertFalse(wordlist.allUsed());
	}
	
}
