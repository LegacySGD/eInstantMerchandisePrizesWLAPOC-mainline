import static org.junit.Assert.assertEquals;

import javax.xml.xpath.XPathExpressionException;

import org.apache.log4j.Logger;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import com.igt.gle.game.data.wrapper.outcome.JSONOutcomeWrapper;
import com.igt.gle.game.data.wrapper.outcome.ODERevealDataOutcomeWrapper;
import com.igt.gle.manager.EngineManager;
import com.igt.gle.ode.proxy.TestODEProxy;
import com.igt.gle.random.proxy.TestRandomNumberProxy;
import com.igt.gle.tools.outcome.OutcomeSigner;
import com.igt.gle.util.InputUtil;
import com.igt.gle.util.InputUtil.PlayerInputBuilder;
import com.igt.gle.util.InputUtil.Wager;
import com.igt.gle.util.OutcomeUtil;
import com.igt.util.xml.JXPathUtil;
import com.igt.util.xml.XMLUtil;

public class MerchandisePrizesWLAPOCEngineManagerTest
{
	private Logger logger;
	private EngineManager manager = new EngineManager();
	private Element gameParams;
	private Element previousOutcome;

	private String buyAction = "BUY";
	private String tryAction = "TRY";

	/**
	 * @throws java.lang.Exception
	 */
	@SuppressWarnings("deprecation")
	@Before
	public void setUp() throws Exception
	{
		logger = Logger.getLogger(MerchandisePrizesWLAPOCEngineManagerTest.class);
		gameParams = XMLUtil.parseResource("paymodel/MerchandisePrizesWLAPOC/GameParams.xml").getDocumentElement();
		previousOutcome = XMLUtil.parseResource("paymodel/MerchandisePrizesWLAPOC/InitOutcome.xml").getDocumentElement();
		previousOutcome = XMLUtil.getElement(OutcomeSigner.sign(previousOutcome));
	}

	@After
	public void cleanUp()
		throws Exception
	{
		TestRandomNumberProxy.reset();
		TestODEProxy.reset();
	}

	@Test
	public void test_buy_NoWin_RevealOneTime()
			throws Exception
	{
		Element playerInput;
		ODERevealDataOutcomeWrapper odeRevealDataOutcomeWrapper = new ODERevealDataOutcomeWrapper();
		
		// Wager
		playerInput = new PlayerInputBuilder()
				.appendActionInput(buyAction)
				.appendWagerInput(new Wager("Wager1", 1))
				.appendJSONInput("{'testData':0}").build();
		Node outcome = manager.play(InputUtil.buildInput(gameParams, previousOutcome, playerInput));
		logger.debug(outcome);
		
		assertEquals("Scenario", JXPathUtil.getString(outcome, "//NextStage/text()"));
		assertSettledPendingPayout(outcome, 0, 1, 0);
		
		// Scenario
		playerInput = new PlayerInputBuilder().appendActionInput("play").build();
		TestODEProxy.setJsonResponse("{'prizeValue':0,'moreStuff':'toTest','prettyPrint':'forGhst','prizeDivision':9}");
		outcome = OutcomeUtil.getSignedData(outcome, "Outcome");
		outcome = manager.play(InputUtil.buildInput(gameParams, XMLUtil.getElement(outcome), playerInput));
		logger.debug(outcome);
		
		assertEquals("Reveal", JXPathUtil.getString(outcome, "//NextStage/text()"));
		assertSettledPendingPayout(outcome, 0, 1, 0);
		
		// Reveal 1
		playerInput = new PlayerInputBuilder()
				.appendActionInput("play")
				.appendJSONInput("{'revealStatus':0,'revealData':'test message'}").build();
		outcome = OutcomeUtil.getSignedData(outcome, "Outcome");
		outcome = manager.play(InputUtil.buildInput(gameParams, XMLUtil.getElement(outcome), playerInput));
		logger.debug(outcome);
		
		assertJsonContent(
				"{'revealData':'test message'}",
				JXPathUtil.getString(outcome, "//JSONOutcome[@name='ODERevealData']/text()"),
				odeRevealDataOutcomeWrapper);
		assertEquals("Wager", JXPathUtil.getString(outcome, "//NextStage/text()"));
		assertSettledPendingPayout(outcome, 1, 0, 0);
	}
	
	@Test
	public void test_buy_2x_100CreditsWin_NoMerchPrize_RevealThreeTimes()
		throws Exception
	{
		Element playerInput;
		ODERevealDataOutcomeWrapper odeRevealDataOutcomeWrapper = new ODERevealDataOutcomeWrapper();
		
		// Wager
		playerInput = new PlayerInputBuilder()
				.appendActionInput(buyAction)
				.appendWagerInput(new Wager("Wager1", 2))
				.appendJSONInput("{'testData':0}").build();
		Node outcome = manager.play(InputUtil.buildInput(gameParams, previousOutcome, playerInput));
		logger.debug(outcome);
		
		assertEquals("Scenario", JXPathUtil.getString(outcome, "//NextStage/text()"));
		assertSettledPendingPayout(outcome, 0, 2, 0);
		
		// Scenario
		playerInput = new PlayerInputBuilder().appendActionInput("play").build();
		TestODEProxy.setJsonResponse("{'prizeValue':100,'moreStuff':'toTest','prettyPrint':'forGhst','prizeDivision':4}");
		outcome = OutcomeUtil.getSignedData(outcome, "Outcome");
		outcome = manager.play(InputUtil.buildInput(gameParams, XMLUtil.getElement(outcome), playerInput));
		logger.debug(outcome);
		
		assertEquals("Reveal", JXPathUtil.getString(outcome, "//NextStage/text()"));
		assertSettledPendingPayout(outcome, 0, 2, 0);
		
		// Reveal 1 - with reveal data
		playerInput = new PlayerInputBuilder()
				.appendActionInput("play")
				.appendJSONInput("{'revealStatus':1,'revealData':'msg1'}").build();
		outcome = OutcomeUtil.getSignedData(outcome, "Outcome");
		outcome = manager.play(InputUtil.buildInput(gameParams, XMLUtil.getElement(outcome), playerInput));
		logger.debug(outcome);
		
		assertJsonContent(
				"{'revealData':'msg1'}",
				JXPathUtil.getString(outcome, "//JSONOutcome[@name='ODERevealData']/text()"),
				odeRevealDataOutcomeWrapper);
		assertEquals("Reveal", JXPathUtil.getString(outcome, "//NextStage/text()"));
		assertSettledPendingPayout(outcome, 0, 2, 0);
		
		// Reveal 2 - without reveal data
		playerInput = new PlayerInputBuilder()
				.appendActionInput("play")
				.appendJSONInput("{'revealStatus':1,'revealData':null}").build();
		outcome = OutcomeUtil.getSignedData(outcome, "Outcome");
		outcome = manager.play(InputUtil.buildInput(gameParams, XMLUtil.getElement(outcome), playerInput));
		logger.debug(outcome);
		
		assertJsonContent(
				"{'revealData':'msg1'}",
				JXPathUtil.getString(outcome, "//JSONOutcome[@name='ODERevealData']/text()"),
				odeRevealDataOutcomeWrapper);
		assertEquals("Reveal", JXPathUtil.getString(outcome, "//NextStage/text()"));
		assertSettledPendingPayout(outcome, 0, 2, 0);
		
		// Reveal 3 (complete) - with reveal data
		playerInput = new PlayerInputBuilder()
				.appendActionInput("play")
				.appendJSONInput("{'revealStatus':0,'revealData':'msg2'}").build();
		outcome = OutcomeUtil.getSignedData(outcome, "Outcome");
		outcome = manager.play(InputUtil.buildInput(gameParams, XMLUtil.getElement(outcome), playerInput));
		logger.debug(outcome);
		
		assertJsonContent(
				"{'revealData':'msg2'}",
				JXPathUtil.getString(outcome, "//JSONOutcome[@name='ODERevealData']/text()"),
				odeRevealDataOutcomeWrapper);
		assertEquals("Wager", JXPathUtil.getString(outcome, "//NextStage/text()"));
		assertSettledPendingPayout(outcome, 2, 0, 100);
	}
	
	@Test
	public void test_buy_2500x_MerchPrize_RevealThreeTimes()
		throws Exception
	{
		int pricePoint = 2500;
		Element playerInput;
		ODERevealDataOutcomeWrapper odeRevealDataOutcomeWrapper = new ODERevealDataOutcomeWrapper();
		
		// Wager
		playerInput = new PlayerInputBuilder()
				.appendActionInput(buyAction)
				.appendWagerInput(new Wager("Wager1", pricePoint))
				.appendJSONInput("{'testData':0}").build();
		Node outcome = manager.play(InputUtil.buildInput(gameParams, previousOutcome, playerInput));
		logger.debug(outcome);
		
		assertEquals("Scenario", JXPathUtil.getString(outcome, "//NextStage/text()"));
		assertSettledPendingPayout(outcome, 0, pricePoint, 0);
		
		// Scenario
		playerInput = new PlayerInputBuilder().appendActionInput("play").build();
		TestODEProxy.setJsonResponse("{'prizeValue':0,'prizeDivision':2}");
		outcome = OutcomeUtil.getSignedData(outcome, "Outcome");
		outcome = manager.play(InputUtil.buildInput(gameParams, XMLUtil.getElement(outcome), playerInput));
		logger.debug(outcome);
		
		assertEquals("MerchandisePrize", JXPathUtil.getString(outcome, "//NextStage/text()"));
		assertSettledPendingPayout(outcome, 0, pricePoint, 0);
		
		// MerchandisePrize
		playerInput = new PlayerInputBuilder().appendNonCashPrizeInput("01-01-02", "Second Tier Merchandise Prize", 2, 50).build();
		TestODEProxy.setJsonResponse("{'prizeValue':0,'prizeDivision':2}");
		outcome = OutcomeUtil.getSignedData(outcome, "Outcome");
		outcome = manager.play(InputUtil.buildInput(gameParams, XMLUtil.getElement(outcome), playerInput));
		logger.debug(outcome);
		
		assertEquals("Reveal", JXPathUtil.getString(outcome, "//NextStage/text()"));
		assertSettledPendingPayout(outcome, 0, pricePoint, 0);
		
		// Reveal 1 - with reveal data
		playerInput = new PlayerInputBuilder()
				.appendActionInput("play")
				.appendJSONInput("{'revealStatus':1,'revealData':'msg1'}").build();
		outcome = OutcomeUtil.getSignedData(outcome, "Outcome");
		outcome = manager.play(InputUtil.buildInput(gameParams, XMLUtil.getElement(outcome), playerInput));
		logger.debug(outcome);
		
		assertJsonContent(
				"{'revealData':'msg1'}",
				JXPathUtil.getString(outcome, "//JSONOutcome[@name='ODERevealData']/text()"),
				odeRevealDataOutcomeWrapper);
		assertEquals("Reveal", JXPathUtil.getString(outcome, "//NextStage/text()"));
		assertSettledPendingPayout(outcome, 0, pricePoint, 0);
		
		// Reveal 2 - without reveal data
		playerInput = new PlayerInputBuilder()
				.appendActionInput("play")
				.appendJSONInput("{'revealStatus':1,'revealData':null}").build();
		outcome = OutcomeUtil.getSignedData(outcome, "Outcome");
		outcome = manager.play(InputUtil.buildInput(gameParams, XMLUtil.getElement(outcome), playerInput));
		logger.debug(outcome);
		
		assertJsonContent(
				"{'revealData':'msg1'}",
				JXPathUtil.getString(outcome, "//JSONOutcome[@name='ODERevealData']/text()"),
				odeRevealDataOutcomeWrapper);
		assertEquals("Reveal", JXPathUtil.getString(outcome, "//NextStage/text()"));
		assertSettledPendingPayout(outcome, 0, pricePoint, 0);
		
		// Reveal 3 (complete) - with reveal data
		playerInput = new PlayerInputBuilder()
				.appendActionInput("play")
				.appendJSONInput("{'revealStatus':0,'revealData':'msg2'}").build();
		outcome = OutcomeUtil.getSignedData(outcome, "Outcome");
		outcome = manager.play(InputUtil.buildInput(gameParams, XMLUtil.getElement(outcome), playerInput));
		logger.debug(outcome);
		
		assertJsonContent(
				"{'revealData':'msg2'}",
				JXPathUtil.getString(outcome, "//JSONOutcome[@name='ODERevealData']/text()"),
				odeRevealDataOutcomeWrapper);
		assertEquals("Wager", JXPathUtil.getString(outcome, "//NextStage/text()"));
		assertSettledPendingPayout(outcome, pricePoint, 0, 0);
	}
	
	@Test
	public void test_try_NoWin()
			throws Exception
	{
		Element playerInput;
		
		// Wager
		playerInput = new PlayerInputBuilder()
				.appendActionInput(tryAction)
				.appendWagerInput(new Wager("Wager1", 1))
				.appendJSONInput("{'testData':0}").build();
		TestODEProxy.setJsonResponse("{'prizeValue':0,'moreStuff':'toTest','prettyPrint':'forGhst','prizeDivision':4}");
		Node outcome = manager.play(InputUtil.buildInput(gameParams, previousOutcome, playerInput));
		logger.debug(outcome);
		
		assertEquals("Wager", JXPathUtil.getString(outcome, "//NextStage/text()"));
		assertSettledPendingPayout(outcome, 1, 0, 0);
	}
	
	@Test
	public void test_try_2500x_100CreditsWin_NoMerchPrize()
			throws Exception
	{
		int pricePoint = 2500;
		Element playerInput;
		
		// Wager
		playerInput = new PlayerInputBuilder()
				.appendActionInput(tryAction)
				.appendWagerInput(new Wager("Wager1", pricePoint))
				.appendJSONInput("{'testData':0}").build();
		TestODEProxy.setJsonResponse("{'prizeValue':100,'moreStuff':'toTest','prettyPrint':'forGhst','prizeDivision':4}");
		Node outcome = manager.play(InputUtil.buildInput(gameParams, previousOutcome, playerInput));
		logger.debug(outcome);
		
		assertEquals("Wager", JXPathUtil.getString(outcome, "//NextStage/text()"));
		assertSettledPendingPayout(outcome, pricePoint, 0, 100);
	}
	
	@Test
	public void test_try_2500x_MerchPrize()
			throws Exception
	{
		int pricePoint = 2500;
		Element playerInput;
		
		// Wager
		playerInput = new PlayerInputBuilder()
				.appendActionInput(tryAction)
				.appendWagerInput(new Wager("Wager1", pricePoint))
				.appendJSONInput("{'testData':0}").build();
		TestODEProxy.setJsonResponse("{'prizeValue':0,'prizeDivision':1}");
		Node outcome = manager.play(InputUtil.buildInput(gameParams, previousOutcome, playerInput));
		logger.debug(outcome);
		
		assertEquals("MerchandisePrize", JXPathUtil.getString(outcome, "//NextStage/text()"));
		assertSettledPendingPayout(outcome, 0, pricePoint, 0);
		
		// MerchandisePrize
		// Fake system input by placing NonCashPrizeInput in PlayerInput node.
		playerInput = new PlayerInputBuilder().appendNonCashPrizeInput("01-01-01", "Top Tier Merchandise Prize", 1, 100).build();
		outcome = OutcomeUtil.getSignedData(outcome, "Outcome");
		outcome = manager.play(InputUtil.buildInput(gameParams, XMLUtil.getElement(outcome), playerInput));
		logger.debug(outcome);
		
		assertEquals("Wager", JXPathUtil.getString(outcome, "//NextStage/text()"));
		assertSettledPendingPayout(outcome, pricePoint, 0, 0);
	}
	
	private void assertJsonContent(String expectedJson, String actualJson, JSONOutcomeWrapper jsonWrapper)
	{
		String expected = jsonWrapper.formatJson(expectedJson);
		assertEquals(expected, actualJson);
	}
	
	private void assertSettledPendingPayout(Node outcome, int settled, int pending, int payout)
			throws XPathExpressionException
	{
		assertEquals(settled, JXPathUtil.getDouble(outcome, "//OutcomeDetail/Settled/text()"), 0d);
		assertEquals(pending, JXPathUtil.getDouble(outcome, "//OutcomeDetail/Pending/text()"), 0d);
		assertEquals(payout, JXPathUtil.getDouble(outcome, "//OutcomeDetail/Payout/text()"), 0d);
	}
}
