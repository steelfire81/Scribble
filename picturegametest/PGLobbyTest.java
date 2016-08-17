package picturegametest;

import static org.junit.Assert.*;

import java.util.ArrayList;

import org.junit.Before;
import org.junit.Test;

import pgserver.PGLobby;
import pgserver.PGServerThread;
import pgserver.PGWordList;

public class PGLobbyTest {

	// Constants
	private static final int TEST_ID = 0;
	private static final String[] TEST_WORDS = {"cat", "dog", "mouse"};
	private static final String TEST_WORDLIST_NAME = "animals";
	
	// Data Members
	private PGLobby publicLobby;
	private PGLobby privateLobby;
	
	@Before
	public void initialize()
	{
		ArrayList<String> words = new ArrayList<String>();
		for(int i = 0; i < TEST_WORDS.length; i++)
			words.add(TEST_WORDS[i]);
		PGWordList wordlist = new PGWordList(TEST_WORDLIST_NAME, words);
		
		publicLobby = new PGLobby(null, TEST_ID, false, wordlist);
		privateLobby = new PGLobby(null, TEST_ID, true, wordlist);
	}
	
	@Test
	public void testGetClientList()
	{
		assertEquals(publicLobby.getClientList().size(), 0);
		publicLobby.addClient(new PGServerThread(null, null));
		assertEquals(publicLobby.getClientList().size(), 1);
	}
	
	@Test
	public void testHasSlotAvailable()
	{
		assertTrue(publicLobby.hasSlotAvailable());
		publicLobby.addClient(new PGServerThread(null, null));
		assertTrue(publicLobby.hasSlotAvailable());
	}
	
	@Test
	public void testGetLobbyID()
	{
		assertEquals(publicLobby.getLobbyID(), TEST_ID);
		assertEquals(privateLobby.getLobbyID(), TEST_ID);
	}
	
	@Test
	public void testIsPrivate()
	{
		assertFalse(publicLobby.isPrivate());
		assertTrue(privateLobby.isPrivate());
	}
	
	@Test
	public void testGenerateKey()
	{
		assertNotNull(privateLobby.generateKey());
	}
	
	@Test
	public void testGetKey()
	{
		assertNull(privateLobby.getKey());
		String key = privateLobby.generateKey();
		assertTrue(key.equals(privateLobby.getKey()));
	}
}
