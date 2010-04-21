package games.stendhal.common.constants;

import static org.junit.Assert.assertEquals;
import games.stendhal.common.constants.Nature;

import org.junit.Test;

public class DamageTypeTest {
	/**
	 * Test parsing various strings give expected results
	 */
	@Test
	public void checkParsing() {
		assertEquals(Nature.CUT, Nature.parse("cut"));
		assertEquals(Nature.ICE, Nature.parse("ice"));
		assertEquals(Nature.FIRE, Nature.parse("fire"));
		assertEquals(Nature.LIGHT, Nature.parse("light"));
		assertEquals(Nature.DARK, Nature.parse("dark"));
		// Default damage; do something even if someone has made a typo
		assertEquals(Nature.CUT, Nature.parse("cuddle"));
		assertEquals(Nature.CUT, Nature.parse(null));
	}
}
