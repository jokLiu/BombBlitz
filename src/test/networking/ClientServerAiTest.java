package test.networking;

import static org.junit.Assert.*;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import bomber.AI.AIDifficulty;
import bomber.networking.ClientServerAI;

public class ClientServerAiTest {
	private ClientServerAI ai;

	@Before
	public void setUp() throws Exception {
		ai = new ClientServerAI((byte) 133, AIDifficulty.EXTREME);
	}

	@After
	public void tearDown() throws Exception {
	}

	@Test
	public void test() {
		assertEquals((byte) 133, ai.getID());

		assertEquals(AIDifficulty.EXTREME, ai.getDifficulty());
	}

}
