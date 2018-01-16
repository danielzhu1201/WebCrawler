package assignment;

import static org.junit.Assert.*;

import java.io.IOException;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.Queue;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.attoparser.ParseException;
import org.attoparser.config.ParseConfiguration;
import org.attoparser.simple.ISimpleMarkupParser;
import org.attoparser.simple.SimpleMarkupParser;
import org.junit.Test;

import com.sun.org.apache.bcel.internal.generic.NEW;
import com.sun.swing.internal.plaf.metal.resources.metal;
import com.sun.xml.internal.fastinfoset.algorithm.BuiltInEncodingAlgorithm.WordListener;

import assignment.WebQueryEngine.Token;

public class UnitIntegratedTest {

	@Test
	public void CrawlingMarkupHandler() throws ParseException, IOException {
		// Create a parser from the attoparser library, and our handler for markup.
		ISimpleMarkupParser parser = new SimpleMarkupParser(ParseConfiguration.htmlConfiguration());
		// handler created
		CrawlingMarkupHandler handler = new CrawlingMarkupHandler();

		URL currentURL = new URL(
				"file:///home/zhaosong/eclipse-workspace/prog7%20files/rhf/www.netfunny.com/rhf/jokes/87/10496.1.html");
		handler.currentURL(currentURL);
		parser.parse(new InputStreamReader(currentURL.openStream()), handler);
		ArrayList<URL> list1 = (ArrayList<URL>) handler.newURLs();
		currentURL = new URL(
				"file:///home/zhaosong/eclipse-workspace/prog7%20files/rhf/www.netfunny.com/rhf/jokes/87/7957.html");
		handler.currentURL(currentURL);
		parser.parse(new InputStreamReader(currentURL.openStream()), handler);
		ArrayList<URL> list2 = (ArrayList<URL>) handler.newURLs();

		// two already added URLs shouldn't be found
		assertFalse(handler.newURLs().contains(new URL(
				"file:///home/zhaosong/eclipse-workspace/prog7%20files/rhf/www.netfunny.com/rhf/current.html")));
		assertFalse(handler.newURLs().contains(
				new URL("file:///home/zhaosong/eclipse-workspace/prog7%20files/rhf/www.netfunny.com/rhf/past.html")));
		// a newly exist URL should be found
		assertTrue(handler.newURLs().contains(new URL(
				"file:///home/zhaosong/eclipse-workspace/prog7%20files/rhf/www.netfunny.com/rhf/jokes/87/7433.12.html")));
		// to be precise all URLs in the new list shouldn't show up in the old list
		for (URL url2 : list2)
			assertFalse(list1.contains(url2));
		// edge cases adding same url twice
		for (URL url : list1)
			assertFalse(handler.addURL(url));
		for (URL url : list2)
			assertFalse(handler.addURL(url));
		// edge cases adding same url with #
		assertFalse(handler.addURL(new URL(
				"file:///home/zhaosong/eclipse-workspace/prog7%20files/rhf/www.netfunny.com/rhf/jokes/87/10496.1.html#2313")));

		handler.handleDocumentStart(0, 0, 0);
		// leftoverURL should be cleared when restart with another html address
		assertEquals(0, handler.newURLs().size());
		assertEquals(0, handler.wordCounter);
	}

	// Token Parser Test
	@Test
	public void negationQueryPhraseParserTest() throws ParseException {
		WebQueryEngine engine = new WebQueryEngine();
		engine.setCurrentStream("!  \"phrase1  phrase 2\"");
		Token myToken = engine.getToken(engine.getCurrentStream());
		assertTrue(myToken.isNegation);
		assertTrue(myToken.isPhrase);
		assertEquals("phrase1 phrase 2", myToken.phrase);

		engine.setCurrentStream("(a & !\"phrase1 phrase 2\")");
		myToken = engine.getToken(engine.getCurrentStream());
		assertTrue(myToken.LeftParnToken);
		myToken = engine.getToken(engine.getCurrentStream());
		assertTrue(myToken.word.equals("a"));
		myToken = engine.getToken(engine.getCurrentStream());
		assertTrue(myToken.andToken);
		myToken = engine.getToken(engine.getCurrentStream());
		assertTrue(myToken.isNegation);
		assertTrue(myToken.isPhrase);
		assertEquals("phrase1 phrase 2", myToken.phrase);
		myToken = engine.getToken(engine.getCurrentStream());
		assertTrue(myToken.RightParenToken);

		engine.setCurrentStream("a !\"phrase1 phrase 2\"");
		myToken = engine.getToken(engine.getCurrentStream());
		assertTrue(myToken.word.equals("a"));
		myToken = engine.getToken(engine.getCurrentStream());
		assertTrue(myToken.andToken);
		myToken = engine.getToken(engine.getCurrentStream());
		assertTrue(myToken.isNegation);
		assertTrue(myToken.isPhrase);
		assertEquals("phrase1 phrase 2", myToken.phrase);
	}

	@Test
	public void negationQueryQueryParserTest() throws ParseException {
		WebQueryEngine engine = new WebQueryEngine();
		engine.setCurrentStream("!(a & b)");
		Token myToken = engine.getToken(engine.getCurrentStream());
		assertTrue(myToken.isNegation);
		assertTrue(myToken.isQuery);
		assertEquals(myToken.query.size(), 5);

		engine.setCurrentStream("(a & !  (a & b))");
		myToken = engine.getToken(engine.getCurrentStream());
		assertTrue(myToken.LeftParnToken);
		myToken = engine.getToken(engine.getCurrentStream());
		assertTrue(myToken.word.equals("a"));
		myToken = engine.getToken(engine.getCurrentStream());
		assertTrue(myToken.andToken);
		myToken = engine.getToken(engine.getCurrentStream());
		assertTrue(myToken.isNegation);
		assertTrue(myToken.isQuery);
		assertEquals(myToken.query.size(), 5);
		myToken = engine.getToken(engine.getCurrentStream());
		assertTrue(myToken.RightParenToken);

		engine.setCurrentStream("(a  !  (a & b))");
		myToken = engine.getToken(engine.getCurrentStream());
		assertTrue(myToken.LeftParnToken);
		myToken = engine.getToken(engine.getCurrentStream());
		assertTrue(myToken.word.equals("a"));
		myToken = engine.getToken(engine.getCurrentStream());
		assertTrue(myToken.andToken);
		myToken = engine.getToken(engine.getCurrentStream());
		assertTrue(myToken.isNegation);
		assertTrue(myToken.isQuery);
		assertEquals(myToken.query.size(), 5);
		myToken = engine.getToken(engine.getCurrentStream());
		assertTrue(myToken.RightParenToken);
	}

	@Test
	public void singleWordTokenParserTest() throws ParseException {
		WebQueryEngine engine = new WebQueryEngine();
		engine.setCurrentStream("sanitytest");
		// single token cases
		assertEquals("sanitytest", engine.getCurrentStream());
		Token myToken = engine.getToken(engine.getCurrentStream());
		assertTrue(myToken.isWord);
		assertEquals("sanitytest", myToken.word);
	}

	@Test
	public void negationWordTokenParserTest() throws ParseException {
		WebQueryEngine engine = new WebQueryEngine();
		Token myToken;
		// negation single word
		engine.setCurrentStream("!sanitytest");
		assertEquals("!sanitytest", engine.getCurrentStream());
		myToken = engine.getToken(engine.getCurrentStream());
		assertTrue(myToken.isWord);
		assertTrue(myToken.isNegation);
		assertEquals("sanitytest", myToken.word);

		// irregular spacing
		engine.setCurrentStream("!    sanity  test  ");
		myToken = engine.getToken(engine.getCurrentStream());
		assertTrue(myToken.isWord);
		assertTrue(myToken.isNegation);
		assertEquals("sanity", myToken.word);
		myToken = engine.getToken(engine.getCurrentStream());
		assertTrue(myToken.andToken);
		myToken = engine.getToken(engine.getCurrentStream());
		assertTrue(myToken.isWord);
		assertEquals("test", myToken.word);
	}

	@Test
	public void phraseTokenParserTest() throws ParseException {
		WebQueryEngine engine = new WebQueryEngine();
		Token myToken;
		// phrase parse
		engine.setCurrentStream("\"word1 word2 word3\"");
		myToken = engine.getToken(engine.getCurrentStream());
		assertTrue(myToken.isPhrase);
		assertEquals("word1 word2 word3", myToken.phrase);

		// phrase - irregular spacing
		engine.setCurrentStream("\"word1     word2  word3\"");
		myToken = engine.getToken(engine.getCurrentStream());
		assertTrue(myToken.isPhrase);
		assertEquals("word1 word2 word3", myToken.phrase); // output should be well sorted with only 1 space betw
	}

	@Test
	public void andTokenParserTest() throws ParseException {
		WebQueryEngine engine = new WebQueryEngine();
		Token myToken;
		// and & with spacing
		String[] items = { "(apple & banana)", "(   apple & banana   )", "(apple   & banana)", "(apple &   banana)",
				"(apple   &   banana)" };
		for (String test : items) {
			engine.setCurrentStream(test);
			myToken = engine.getToken(engine.getCurrentStream());
			assertTrue(myToken.LeftParnToken);
			myToken = engine.getToken(engine.getCurrentStream());
			assertTrue(myToken.isWord);
			assertEquals("apple", myToken.word);
			myToken = engine.getToken(engine.getCurrentStream());
			assertTrue(myToken.andToken);
			myToken = engine.getToken(engine.getCurrentStream());
			assertTrue(myToken.isWord);
			assertEquals("banana", myToken.word);
			myToken = engine.getToken(engine.getCurrentStream());
			assertTrue(myToken.RightParenToken);
		}
	}

	@Test
	public void andNegationTokenParserTest() throws ParseException {
		WebQueryEngine engine = new WebQueryEngine();
		Token myToken;
		// and with negation and spacing
		String[] negationItems = { "(!apple & !banana)", "(   ! apple & !  banana   )", "(!apple   & !  banana)",
				"(!apple &   !   banana)", "(!apple   &   !banana)" };
		for (String test : negationItems) {
			engine.setCurrentStream(test);
			myToken = engine.getToken(engine.getCurrentStream());
			assertTrue(myToken.LeftParnToken);
			myToken = engine.getToken(engine.getCurrentStream());
			assertTrue(myToken.isNegation);
			assertTrue(myToken.isWord);
			assertEquals("apple", myToken.word);
			myToken = engine.getToken(engine.getCurrentStream());
			assertTrue(myToken.andToken);
			myToken = engine.getToken(engine.getCurrentStream());
			assertTrue(myToken.isNegation);
			assertTrue(myToken.isWord);
			assertEquals("banana", myToken.word);
			myToken = engine.getToken(engine.getCurrentStream());
			assertTrue(myToken.RightParenToken);
		}
	}

	@Test
	public void andPhraseTokenParserTest() throws ParseException {
		WebQueryEngine engine = new WebQueryEngine();
		Token myToken;
		// and with phrase and spacing
		String[] phraseItems = { "(\"phrase twophrase\" & \"sentence twosentence\")",
				"(   \"phrase twophrase\" & \"sentence twosentence\"   )",
				"(\"phrase twophrase\"      & \"sentence twosentence\")",
				"(\"phrase twophrase\" &      \"sentence twosentence\")" };// can't have empty query
		for (String test : phraseItems) {
			engine.setCurrentStream(test);
			myToken = engine.getToken(engine.getCurrentStream());
			assertTrue(myToken.LeftParnToken);
			myToken = engine.getToken(engine.getCurrentStream());
			assertTrue(myToken.isPhrase);
			assertEquals("phrase twophrase", myToken.phrase);
			myToken = engine.getToken(engine.getCurrentStream());
			assertTrue(myToken.andToken);
			myToken = engine.getToken(engine.getCurrentStream());
			assertTrue(myToken.isPhrase);
			assertEquals("sentence twosentence", myToken.phrase);
			myToken = engine.getToken(engine.getCurrentStream());
			assertTrue(myToken.RightParenToken);
		}
	}

	@Test
	public void TokenParserIntegratedTest() throws ParseException {
		WebQueryEngine engine = new WebQueryEngine();
		// single token cases
		// single word
		engine.setCurrentStream("sanitytest");
		assertEquals("sanitytest", engine.getCurrentStream());
		Token myToken = engine.getToken(engine.getCurrentStream());
		assertTrue(myToken.isWord);
		assertEquals("sanitytest", myToken.word);

		// mix cases
		// and with two complex Queries
		String[] complexQuery1 = { "word (item & (word3 & word4))", "word (item & (word3 & word4)  )",
				"word  (  item & (word3 & word4))", "word (item   &   (word3 & word4))" };
		for (String test : complexQuery1) {
			engine.setCurrentStream(test);
			myToken = engine.getToken(engine.getCurrentStream());
			assertTrue(myToken.isWord);
			assertEquals("word", myToken.word);
			myToken = engine.getToken(engine.getCurrentStream());
			assertTrue(myToken.andToken);
			myToken = engine.getToken(engine.getCurrentStream());
			assertTrue(myToken.LeftParnToken);
			myToken = engine.getToken(engine.getCurrentStream());
			assertTrue(myToken.isWord);
			assertEquals("item", myToken.word);
			myToken = engine.getToken(engine.getCurrentStream());
			assertTrue(myToken.andToken);
			myToken = engine.getToken(engine.getCurrentStream());
			assertTrue(myToken.LeftParnToken);
			myToken = engine.getToken(engine.getCurrentStream());
			assertTrue(myToken.isWord);
			assertEquals("word3", myToken.word);
			myToken = engine.getToken(engine.getCurrentStream());
			assertTrue(myToken.andToken);
			myToken = engine.getToken(engine.getCurrentStream());
			assertTrue(myToken.isWord);
			assertEquals("word4", myToken.word);
			myToken = engine.getToken(engine.getCurrentStream());
			assertTrue(myToken.RightParenToken);
			myToken = engine.getToken(engine.getCurrentStream());
			assertTrue(myToken.RightParenToken);
		}

		String[] complexQuery2 = { "!word (!item & (word3 & word4))", "!word (!item & (word3 & word4) )",
				"!word  (  !item & (word3 & word4))", "!word (!item   &   (word3 & word4))" };
		for (String test : complexQuery2) {
			engine.setCurrentStream(test);
			myToken = engine.getToken(engine.getCurrentStream());
			assertTrue(myToken.isNegation);
			assertTrue(myToken.isWord);
			assertEquals("word", myToken.word);
			myToken = engine.getToken(engine.getCurrentStream());
			assertTrue(myToken.andToken);
			myToken = engine.getToken(engine.getCurrentStream());
			assertTrue(myToken.LeftParnToken);
			myToken = engine.getToken(engine.getCurrentStream());
			assertTrue(myToken.isWord);
			assertTrue(myToken.isNegation);
			assertEquals("item", myToken.word);
			myToken = engine.getToken(engine.getCurrentStream());
			assertTrue(myToken.andToken);
			myToken = engine.getToken(engine.getCurrentStream());
			assertTrue(myToken.LeftParnToken);
			myToken = engine.getToken(engine.getCurrentStream());
			assertTrue(myToken.isWord);
			assertEquals("word3", myToken.word);
			myToken = engine.getToken(engine.getCurrentStream());
			assertTrue(myToken.andToken);
			myToken = engine.getToken(engine.getCurrentStream());
			assertTrue(myToken.isWord);
			assertEquals("word4", myToken.word);
			myToken = engine.getToken(engine.getCurrentStream());
			assertTrue(myToken.RightParenToken);
			myToken = engine.getToken(engine.getCurrentStream());
			assertTrue(myToken.RightParenToken);
		}

		String[] complexQuery3 = { "\"word item  \" (\"item word\" & (word3&word4))",
				"\"word item\"    (   \"item word\" & (word3 & word4) )",
				"\"word item\"    (\"item word\"    &(word3 & word4))",
				"\"word item\"  (  \"item word\"&   (word3 & word4))" };
		for (String test : complexQuery3) {
			engine.setCurrentStream(test);
			myToken = engine.getToken(engine.getCurrentStream());
			assertTrue(myToken.isPhrase);
			assertEquals("word item", myToken.phrase);
			myToken = engine.getToken(engine.getCurrentStream());
			assertTrue(myToken.andToken);
			myToken = engine.getToken(engine.getCurrentStream());
			assertTrue(myToken.LeftParnToken);
			myToken = engine.getToken(engine.getCurrentStream());
			assertTrue(myToken.isPhrase);
			assertEquals("item word", myToken.phrase);
			myToken = engine.getToken(engine.getCurrentStream());
			assertTrue(myToken.andToken);
			myToken = engine.getToken(engine.getCurrentStream());
			assertTrue(myToken.LeftParnToken);
			myToken = engine.getToken(engine.getCurrentStream());
			assertTrue(myToken.isWord);
			assertEquals("word3", myToken.word);
			myToken = engine.getToken(engine.getCurrentStream());
			assertTrue(myToken.andToken);
			myToken = engine.getToken(engine.getCurrentStream());
			assertTrue(myToken.isWord);
			assertEquals("word4", myToken.word);
			myToken = engine.getToken(engine.getCurrentStream());
			assertTrue(myToken.RightParenToken);
			myToken = engine.getToken(engine.getCurrentStream());
			assertTrue(myToken.RightParenToken);
		}

		// an case integrating everything
		engine.setCurrentStream("(ivy &  !liang) \"wealth fame\"   (sanity   | (  happiness & joy  ) )  ");
		myToken = engine.getToken(engine.getCurrentStream());
		assertTrue(myToken.LeftParnToken);
		myToken = engine.getToken(engine.getCurrentStream());
		assertTrue(myToken.isWord);
		assertEquals("ivy", myToken.word);
		myToken = engine.getToken(engine.getCurrentStream());
		assertTrue(myToken.andToken);
		myToken = engine.getToken(engine.getCurrentStream());
		assertTrue(myToken.isWord);
		assertTrue(myToken.isNegation);
		assertEquals("liang", myToken.word);
		myToken = engine.getToken(engine.getCurrentStream());
		assertTrue(myToken.RightParenToken);
		myToken = engine.getToken(engine.getCurrentStream());
		assertTrue(myToken.andToken);
		myToken = engine.getToken(engine.getCurrentStream());
		assertTrue(myToken.isPhrase);
		myToken = engine.getToken(engine.getCurrentStream());
		assertTrue(myToken.andToken);
		myToken = engine.getToken(engine.getCurrentStream());
		assertTrue(myToken.LeftParnToken);
		myToken = engine.getToken(engine.getCurrentStream());
		assertTrue(myToken.isWord);
		assertEquals("sanity", myToken.word);
		myToken = engine.getToken(engine.getCurrentStream());
		assertTrue(myToken.orToken);
		myToken = engine.getToken(engine.getCurrentStream());
		assertTrue(myToken.LeftParnToken);
		myToken = engine.getToken(engine.getCurrentStream());
		assertTrue(myToken.isWord);
		assertEquals("happiness", myToken.word);
		myToken = engine.getToken(engine.getCurrentStream());
		assertTrue(myToken.andToken);
		myToken = engine.getToken(engine.getCurrentStream());
		assertTrue(myToken.isWord);
		assertEquals("joy", myToken.word);
		myToken = engine.getToken(engine.getCurrentStream());
		assertTrue(myToken.RightParenToken);
		myToken = engine.getToken(engine.getCurrentStream());
		assertTrue(myToken.RightParenToken);
		assertTrue(engine.getCurrentStream().length() == 0);
	}

	// Query Input Validity Cases:
	@Test
	public void negationQueryPhraseTest() {
		WebQueryEngine engine = new WebQueryEngine();
		assertTrue(engine.checkCharacters("!\"phrase1 phrase 2\""));
		assertTrue(engine.checkQuery("!\"phrase1 phrase 2\""));

		assertTrue(engine.checkCharacters("! \"phrase1 phrase 2\""));
		assertTrue(engine.checkQuery("!  \"phrase1 phrase 2\""));

		assertTrue(engine.checkCharacters("(a & !\"phrase1 phrase 2\")"));
		assertTrue(engine.checkQuery("(a & !\"phrase1 phrase 2\")"));

		assertTrue(engine.checkCharacters("(a | !\"phrase1 phrase 2\")"));
		assertTrue(engine.checkQuery("(a | !\"phrase1 phrase 2\")"));

		assertTrue(engine.checkCharacters("a !\"phrase1 phrase 2\""));
		assertTrue(engine.checkQuery("a !\"phrase1 phrase 2\""));
	}

	@Test
	public void negationQueryQueryTest() {
		WebQueryEngine engine = new WebQueryEngine();
		assertTrue(engine.checkCharacters("!(a & b)"));
		assertTrue(engine.checkQuery("!(a & b)"));

		assertTrue(engine.checkCharacters("!  (a & b)"));
		assertTrue(engine.checkQuery("!  (a & b)"));

		assertTrue(engine.checkCharacters("(a & !  (a & b))"));
		assertTrue(engine.checkQuery("(a & !  (a & b))"));
	}

	@Test
	public void irrguCharacterTest() {
		WebQueryEngine engine = new WebQueryEngine();
		assertFalse(engine.checkCharacters("ri?se"));// irregular characters
		assertFalse(engine.checkQuery("!&word"));// wierd character after ! except space
	}

	@Test
	public void parenPairMatchTest() {
		WebQueryEngine engine = new WebQueryEngine();
		assertFalse(engine.checkQuery("(a & b"));
		assertFalse(engine.checkQuery("a & b)"));// paren not match
		assertFalse(engine.checkQuery("(a b)"));
		assertFalse(engine.checkQuery("&"));
		assertFalse(engine.checkQuery("|"));
		assertFalse(engine.checkQuery("(a | & b)"));// parenPair != andOrPair
	}

	@Test
	public void emptyQueryTest() {
		WebQueryEngine engine = new WebQueryEngine();
		assertFalse(engine.checkQuery(""));
		assertFalse(engine.checkQuery("\"\""));
		assertFalse(engine.checkQuery("(a | \"\")"));// empty string or inside quotation mark
	}

	@Test
	public void implicitAndTest() {
		WebQueryEngine engine = new WebQueryEngine();
		assertFalse(engine.checkQuery("(a & b)(c&d)"));// lack a space
		assertFalse(engine.checkQuery("() (c&d)"));
		assertFalse(engine.checkQuery("\"word word\"\"word word\"")); // implicit and
	}

	@Test
	public void queryCheckIntegrationTest() {
		// we will allow extra spaces at the begin/end of query input
		// will trim
		// good cases we used above
		WebQueryEngine engine = new WebQueryEngine();
		String[] items = { "(apple & banana)", "(   apple&banana   )", "(apple   & banana)", "(apple &   banana)",
				"(apple   &   banana)" };
		String[] negationItems = { "(!apple & !banana)", "(   ! apple&!  banana   )", "(!apple   & !  banana)",
				"(!apple &   !   banana)", "(!apple   &   !banana)" };
		String[] phraseItems = { "(\"phrase twophrase\" & \"sentence twosentence\")",
				"(   \"phrase twophrase\"&\"sentence twosentence\"   )",
				"(\"phrase twophrase\"      & \"sentence twosentence\")",
				"(\"phrase twophrase\" &      \"sentence twosentence\")" };// can't have empty query
		String[] complexQuery1 = { "word (item & (word3 & word4))", "  word (item & (word3 & word4)  ) ",
				"word  (  item & (word3 & word4))", "word (item   &   (word3 & word4))" };
		String[] complexQuery2 = { "!word (!item & (word3 & word4))", "  !word (!item&(word3 & word4) )  ",
				"!word  (  !item & (word3 & word4))", "!word (!item   &   (word3 & word4))" };
		String[] complexQuery3 = { "\"word item  \" (\"item word\" & (word3&word4))",
				"  \"word item\"    (   \"item word\"&(word3 & word4) )  ",
				"    \"word item\"    (\"item word\"    &(word3 & word4))",
				"    \"word item\"  (  \"item word\"&   (word3 & word4))" };
		assertTrue(engine.checkQuery(("(ivy &  !liang) \"wealth fame\"  ( sanity   | (  happiness&joy  ))"))); // a
																												// ()s
		for (String test : negationItems)
			assertTrue(engine.checkQuery(test));
		for (String test : phraseItems)
			assertTrue(engine.checkQuery(test));
		for (String test : complexQuery1)
			assertTrue(engine.checkQuery(test));
		for (String test : complexQuery2)
			assertTrue(engine.checkQuery(test));
		for (String test : complexQuery3)
			assertTrue(engine.checkQuery(test));
	}

	// Prefix to Postfix Test
	@Test
	public void singleTokenInfixPostfix() throws ParseException {
		WebQueryEngine engine = new WebQueryEngine();
		// single element case
		ArrayList<Token> postfix = engine.infixToPostfix("word");
		assertEquals("word", postfix.get(0).word);
		postfix = engine.infixToPostfix("!word");
		assertTrue(postfix.get(0).isNegation);
		assertEquals("word", postfix.get(0).word);
		postfix = engine.infixToPostfix("\"word   string\"");
		assertTrue(postfix.get(0).isPhrase);
		assertEquals("word string", postfix.get(0).phrase);
	}

	@Test
	public void andOrTokenInfixPostfix() throws ParseException {
		WebQueryEngine engine = new WebQueryEngine();
		ArrayList<Token> postfix;
		// simple and/or case with negation
		postfix = engine.infixToPostfix("(apple & !banana)"); // and
		assertEquals("apple", postfix.get(0).word);
		assertTrue(postfix.get(1).isNegation);
		assertEquals("banana", postfix.get(1).word);
		assertTrue(postfix.get(2).andToken);

		postfix = engine.infixToPostfix("(apple | !banana)");
		assertEquals("apple", postfix.get(0).word);
		assertTrue(postfix.get(1).isNegation);
		assertEquals("banana", postfix.get(1).word);
		assertTrue(postfix.get(2).orToken);
	}

	@Test
	public void nestedAndOrTokenInfixPostfix() throws ParseException {
		WebQueryEngine engine = new WebQueryEngine();
		ArrayList<Token> postfix;
		// simple nested and/or case - testing hierarchy
		postfix = engine.infixToPostfix("(word & (a | b))");
		assertEquals("word", postfix.get(0).word);
		assertEquals("a", postfix.get(1).word);
		assertEquals("b", postfix.get(2).word);
		assertTrue(postfix.get(3).orToken);
		assertTrue(postfix.get(4).andToken);

		postfix = engine.infixToPostfix("((a & b) & (c | !d))");
		assertEquals("a", postfix.get(0).word);
		assertEquals("b", postfix.get(1).word);
		assertTrue(postfix.get(2).andToken);
		assertEquals("c", postfix.get(3).word);
		assertEquals("d", postfix.get(4).word);
		assertTrue(postfix.get(4).isNegation);
		assertTrue(postfix.get(5).orToken);
		assertTrue(postfix.get(6).andToken);
		assertTrue(postfix.size() == 7);
	}

	@Test
	public void implicitAndTokenInfixPostfix() throws ParseException {
		WebQueryEngine engine = new WebQueryEngine();
		ArrayList<Token> postfix;
		// simple implicit and case
		postfix = engine.infixToPostfix("a (b | c)");
		assertEquals("a", postfix.get(0).word);
		assertEquals("b", postfix.get(1).word);
		assertEquals("c", postfix.get(2).word);
		assertTrue(postfix.get(3).orToken);
		assertTrue(postfix.get(4).andToken);

		postfix = engine.infixToPostfix("((d & e) | f) word");
		assertEquals("d", postfix.get(0).word);
		assertEquals("e", postfix.get(1).word);
		assertTrue(postfix.get(2).andToken);
		assertEquals("f", postfix.get(3).word);
		assertTrue(postfix.get(4).orToken);
		assertEquals("word", postfix.get(5).word);
		assertTrue(postfix.get(6).andToken);
	}

	// Postfix and Calculation using Index
	@Test
	public void infixtoPostfixIntegrationTest() throws ParseException, ClassNotFoundException, IOException {
		WebQueryEngine engine = new WebQueryEngine();
		ArrayList<Token> postfix;
		// with implicit and
		postfix = engine.infixToPostfix("((a & (b | c)) | (((d & e) | f) word))");
		assertEquals("a", postfix.get(0).word);
		assertEquals("b", postfix.get(1).word);
		assertEquals("c", postfix.get(2).word);
		assertTrue(postfix.get(3).orToken);
		assertTrue(postfix.get(4).andToken);
		assertEquals("d", postfix.get(5).word);
		assertEquals("e", postfix.get(6).word);
		assertTrue(postfix.get(7).andToken);
		assertEquals("f", postfix.get(8).word);
		assertTrue(postfix.get(9).orToken);
		assertEquals("word", postfix.get(10).word);
		assertTrue(postfix.get(11).andToken);
		assertTrue(postfix.get(12).orToken);
		assertTrue(postfix.size() == 13);

		postfix = engine.infixToPostfix("(ivy &  !liang) (\"wealth fame\"   (sanity   | (  happiness &  joy  )))");
		assertEquals("ivy", postfix.get(0).word);
		assertEquals("liang", postfix.get(1).word);
		assertTrue(postfix.get(1).isNegation);
		assertTrue(postfix.get(2).andToken);
		assertTrue(postfix.get(3).isPhrase);
		assertEquals("sanity", postfix.get(4).word);
		assertEquals("happiness", postfix.get(5).word);
		assertEquals("joy", postfix.get(6).word);
		assertTrue(postfix.get(7).andToken);
		assertTrue(postfix.get(8).orToken);
		assertTrue(postfix.get(9).andToken);
		assertTrue(postfix.get(10).andToken);
	}

	// WebIndex test
	@Test
	public void indexEmptySearchTest() {
		WebIndex testindex = new WebIndex();
		// search with empty index
		ArrayList<Page> searchResult = testindex.wordSearch("a");
		assertEquals(0, searchResult.size());
		searchResult = testindex.negationSearch("a");
		assertEquals(0, searchResult.size());
		searchResult = testindex.phraseSearch("word sentence");
		assertEquals(0, searchResult.size());
	}

	@Test
	public void indexSingleTokenTest() throws MalformedURLException {
		WebIndex testindex = new WebIndex();
		// parse 5 random websites - using same code from WebCrawler
		Queue<URL> remaining = new LinkedList<>();
		remaining.add(
				new URL("file:///home/zhaosong/eclipse-workspace/prog7%20files/rhf/www.netfunny.com/rhf/index.html"));
		remaining.add(
				new URL("file:///home/zhaosong/eclipse-workspace/prog7%20files/rhf/www.netfunny.com/rhf/best.html"));
		remaining.add(new URL(
				"file:///home/zhaosong/eclipse-workspace/prog7%20files/rhf/www.netfunny.com/rhf/jokes/89q1/bjobs.229.html"));
		remaining.add(new URL(
				"file:///home/zhaosong/eclipse-workspace/prog7%20files/rhf/www.netfunny.com/rhf/jokes/fav.html"));
		remaining.add(new URL(
				"file:///home/zhaosong/eclipse-workspace/prog7%20files/rhf/www.netfunny.com/rhf/bestindx.html"));
		// Create a parser from the attoparser library, and our handler for markup.
		ISimpleMarkupParser parser = new SimpleMarkupParser(ParseConfiguration.htmlConfiguration());
		// handler created
		CrawlingMarkupHandler handler = new CrawlingMarkupHandler();
		// Try to start crawling, adding new URLS as we see them.
		try {
			while (!remaining.isEmpty()) {
				// set current parsed URL:
				URL currentURL = remaining.poll();
				handler.currentURL(currentURL);
				// Parse the next URL's page
				try {
					parser.parse(new InputStreamReader(currentURL.openStream()), handler);
				} catch (Exception e) {
					// TODO: handle exception
					e.printStackTrace();
					System.err.println("File not found exception or illegal");
				}
			}

			testindex = (WebIndex) handler.getIndex(); // we can cast to WebIndex

		} catch (Exception e) {
		}

		// word search particular words
		ArrayList<Page> result = testindex.wordSearch("would");
		URL resulturl = result.get(0).getURL();
		assertEquals(
				new URL("file:///home/zhaosong/eclipse-workspace/prog7%20files/rhf/www.netfunny.com/rhf/bestindx.html"),
				resulturl);

		// the four other websites don't contain would
		result = testindex.negationSearch("would");
		assertEquals(4, result.size());

		// search words with capitalizaiton
		result = testindex.wordSearch("comedy");
		assertEquals(4, result.size());
	}

	@Test
	public void indexPhraseSearchTest() throws MalformedURLException {
		WebIndex testindex = new WebIndex();
		// parse 5 random websites - using same code from WebCrawler
		Queue<URL> remaining = new LinkedList<>();
		remaining.add(
				new URL("file:///home/zhaosong/eclipse-workspace/prog7%20files/rhf/www.netfunny.com/rhf/index.html"));
		remaining.add(
				new URL("file:///home/zhaosong/eclipse-workspace/prog7%20files/rhf/www.netfunny.com/rhf/best.html"));
		remaining.add(new URL(
				"file:///home/zhaosong/eclipse-workspace/prog7%20files/rhf/www.netfunny.com/rhf/jokes/89q1/bjobs.229.html"));
		remaining.add(new URL(
				"file:///home/zhaosong/eclipse-workspace/prog7%20files/rhf/www.netfunny.com/rhf/jokes/fav.html"));
		remaining.add(new URL(
				"file:///home/zhaosong/eclipse-workspace/prog7%20files/rhf/www.netfunny.com/rhf/bestindx.html"));
		// Create a parser from the attoparser library, and our handler for markup.
		ISimpleMarkupParser parser = new SimpleMarkupParser(ParseConfiguration.htmlConfiguration());
		// handler created
		CrawlingMarkupHandler handler = new CrawlingMarkupHandler();
		// Try to start crawling, adding new URLS as we see them.
		try {
			while (!remaining.isEmpty()) {
				// set current parsed URL:
				URL currentURL = remaining.poll();
				handler.currentURL(currentURL);
				// Parse the next URL's page
				try {
					parser.parse(new InputStreamReader(currentURL.openStream()), handler);
				} catch (Exception e) {
					// TODO: handle exception
					e.printStackTrace();
					System.err.println("File not found exception or illegal");
				}
			}

			testindex = (WebIndex) handler.getIndex(); // we can cast to WebIndex

		} catch (Exception e) {
		}

		ArrayList<Page> result;
		// testing phrase search
		result = testindex.phraseSearch("you can");
		assertEquals(2, result.size());

		ArrayList<String> wordList = new ArrayList<>();
		Pattern p = Pattern.compile("[a-zA-Z0-9]+");
		Matcher matcher = p.matcher("would like to");
		while (matcher.find()) {
			wordList.add(matcher.group());
		}

		ArrayList<Page> possibleChoice = testindex.wordSearch(wordList.get(0));
		ArrayList<ArrayList<Integer>> index = new ArrayList<>();

		for (Page page : possibleChoice) {
			index.add(testindex.URLWordMap.get(page.getURL()).get(wordList.get(0)));
		}
		for (int i : index.get(0))
			assertEquals(new URL(
					"file:///home/zhaosong/eclipse-workspace/prog7%20files/rhf/www.netfunny.com/rhf/bestindx.html"),
					testindex.phraseSearch("would like to").get(0).getURL());
	}

	@Test
	public void indexQueryEngineIntegrationTest() throws MalformedURLException, ParseException {
		WebIndex testindex = new WebIndex();
		// parse 5 random websites - using same code from WebCrawler
		Queue<URL> remaining = new LinkedList<>();
		remaining.add(
				new URL("file:///home/zhaosong/eclipse-workspace/prog7%20files/rhf/www.netfunny.com/rhf/index.html"));
		remaining.add(
				new URL("file:///home/zhaosong/eclipse-workspace/prog7%20files/rhf/www.netfunny.com/rhf/best.html"));
		remaining.add(new URL(
				"file:///home/zhaosong/eclipse-workspace/prog7%20files/rhf/www.netfunny.com/rhf/jokes/89q1/bjobs.229.html"));
		remaining.add(new URL(
				"file:///home/zhaosong/eclipse-workspace/prog7%20files/rhf/www.netfunny.com/rhf/jokes/fav.html"));
		remaining.add(new URL(
				"file:///home/zhaosong/eclipse-workspace/prog7%20files/rhf/www.netfunny.com/rhf/bestindx.html"));
		// Create a parser from the attoparser library, and our handler for markup.
		ISimpleMarkupParser parser = new SimpleMarkupParser(ParseConfiguration.htmlConfiguration());
		// handler created
		CrawlingMarkupHandler handler = new CrawlingMarkupHandler();
		// Try to start crawling, adding new URLS as we see them.
		try {
			while (!remaining.isEmpty()) {
				// set current parsed URL:
				URL currentURL = remaining.poll();
				handler.currentURL(currentURL);
				// Parse the next URL's page
				try {
					parser.parse(new InputStreamReader(currentURL.openStream()), handler);
				} catch (Exception e) {
					// TODO: handle exception
					e.printStackTrace();
					System.err.println("File not found exception or illegal");
				}
			}

			testindex = (WebIndex) handler.getIndex(); // we can cast to WebIndex

		} catch (Exception e) {
		}

		// new stuff
		testindex = (WebIndex) handler.getIndex(); // we can cast to WebIndex
		WebQueryEngine engine = new WebQueryEngine(testindex);
		ArrayList<Page> result;

		// single token + simple & | situations
		result = engine.postfixCalc(engine.infixToPostfix("old"));
		assertEquals(3, result.size());

		result = engine.postfixCalc(engine.infixToPostfix("!old"));
		assertEquals(2, result.size());

		result = engine.postfixCalc(engine.infixToPostfix("\"submitter sent \""));
		assertEquals(1, result.size());

		result = engine.postfixCalc(engine.infixToPostfix("(oldest & most)"));
		assertEquals(2, result.size());

		result = engine.postfixCalc(engine.infixToPostfix("(comedy | dizzy)"));
		assertEquals(5, result.size());

		result = engine.postfixCalc(engine.infixToPostfix("(comedy | \"submitter sent \")"));
		assertEquals(5, result.size());

		// implicit and
		result = engine.postfixCalc(engine.infixToPostfix("oldest most"));
		assertEquals(2, result.size());

		result = engine.postfixCalc(engine.infixToPostfix("dizzy (comedy | dizzy)"));
		assertEquals(1, result.size());

		result = engine.postfixCalc(engine
				.infixToPostfix("(comedy | \"submitter sent \") \"a man with a problem\" (!best | (comedy | dizzy))"));
		assertEquals(1, result.size());

	}

	// Page Class Tests
	@Test
	public void pageBasicsTest() throws MalformedURLException {
		Page testPage = new Page(
				"file:///home/zhaosong/eclipse-workspace/" + "prog7%20files/rhf/www.netfunny.com/rhf/index.html");// provides
																													// a
																													// URL

		// Title method
		testPage.addTitle("Test title");
		assertEquals("test title", testPage.getTitle());

		// adding three random scores and calculate final
		testPage.addScore("test", 3);
		testPage.addScore("page", 2);
		testPage.addScore("title", 5);
		assertEquals(16, testPage.calculateScore());

		// get URL
		assertEquals(new URL(
				"file:///home/zhaosong/eclipse-workspace/" + "prog7%20files/rhf/www.netfunny.com/rhf/index.html"),
				testPage.getURL());

		// equals
		Page testPage2 = new Page(
				"file:///home/zhaosong/eclipse-workspace/" + "prog7%20files/rhf/www.netfunny.com/rhf/index.html");
		assertTrue(testPage.equals(testPage2));
	}

	@Test
	public void postFixCalcPageIntegrationTest() throws MalformedURLException, ParseException {
		// parse 5 random websites - using same code from WebCrawler
		Queue<URL> remaining = new LinkedList<>();
		remaining.add(
				new URL("file:///home/zhaosong/eclipse-workspace/prog7%20files/rhf/www.netfunny.com/rhf/index.html"));
		remaining.add(
				new URL("file:///home/zhaosong/eclipse-workspace/prog7%20files/rhf/www.netfunny.com/rhf/best.html"));
		remaining.add(new URL(
				"file:///home/zhaosong/eclipse-workspace/prog7%20files/rhf/www.netfunny.com/rhf/jokes/89q1/bjobs.229.html"));
		remaining.add(new URL(
				"file:///home/zhaosong/eclipse-workspace/prog7%20files/rhf/www.netfunny.com/rhf/jokes/fav.html"));
		remaining.add(new URL(
				"file:///home/zhaosong/eclipse-workspace/prog7%20files/rhf/www.netfunny.com/rhf/bestindx.html"));
		// Create a parser from the attoparser library, and our handler for markup.
		ISimpleMarkupParser parser = new SimpleMarkupParser(ParseConfiguration.htmlConfiguration());
		// handler created
		CrawlingMarkupHandler handler = new CrawlingMarkupHandler();
		// Try to start crawling, adding new URLS as we see them.
		try {
			while (!remaining.isEmpty()) {
				// set current parsed URL:
				URL currentURL = remaining.poll();
				handler.currentURL(currentURL);
				// Parse the next URL's page
				try {
					parser.parse(new InputStreamReader(currentURL.openStream()), handler);
				} catch (Exception e) {
					// TODO: handle exception
					e.printStackTrace();
					System.err.println("File not found exception or illegal");
				}

			}
		} catch (Exception e) {
			// TODO: handle exception
		}

		WebIndex testindex = (WebIndex) handler.getIndex(); // we can cast to WebIndex
		WebQueryEngine engine = new WebQueryEngine(testindex);

		// single token + simple & | situations
		ArrayList<Page> result = engine.postfixCalc(engine.infixToPostfix("old"));
		for (Page pa : result)
			assertEquals(1, pa.getScore());
		assertEquals(3, result.size());

		result = engine.postfixCalc(engine.infixToPostfix("!old"));
		for (Page pa : result)
			assertEquals(0, pa.getScore());
		assertEquals(2, result.size());

		result = engine.postfixCalc(engine.infixToPostfix("\"submitter sent \""));
		for (Page pa : result)
			assertEquals(3, pa.getScore());
		assertEquals(1, result.size());

		result = engine.postfixCalc(engine.infixToPostfix("(oldest & most)"));
		for (Page pa : result) {
			if (pa.getURL().equals(new URL("file:///home/zhaosong/eclipse-workspace/"
					+ "prog7%20files/rhf/www.netfunny.com/rhf/bestindx.html")))
				assertEquals(3, pa.getScore());
			else
				assertEquals(2, pa.getScore());
		}
		assertEquals(2, result.size());

		result = engine.postfixCalc(engine.infixToPostfix("(comedy | dizzy)"));
		for (Page pa : result)
			if (pa.getURL().equals(new URL("file:///home/zhaosong/"
					+ "eclipse-workspace/prog7%20files/rhf/www.netfunny.com/rhf/bestindx.html")))
				assertEquals(5, pa.getScore()); // word exists in title too
		assertEquals(5, result.size());

		result = engine.postfixCalc(engine.infixToPostfix("(comedy | \"submitter sent \")"));
		for (Page pa : result)
			if (pa.getURL().equals(new URL(
					"file:///home/zhaosong/eclipse-workspace/" + "prog7%20files/rhf/www.netfunny.com/rhf/best.html")))
				assertEquals(1, pa.getScore());
		assertEquals(5, result.size());

		// implicit and
		result = engine.postfixCalc(engine.infixToPostfix("oldest most"));
		for (Page pa : result) {
			if (pa.getURL().equals(new URL("file:///home/zhaosong/eclipse-workspace/"
					+ "prog7%20files/rhf/www.netfunny.com/rhf/bestindx.html")))
				assertEquals(3, pa.getScore());
			else
				assertEquals(2, pa.getScore());
		}
		assertEquals(2, result.size());

		result = engine.postfixCalc(engine.infixToPostfix("dizzy (comedy | dizzy)"));
		for (Page pa : result)
			if (pa.getURL().equals(new URL("file:///home/zhaosong/eclipse-workspace/"
					+ "prog7%20files/rhf/www.netfunny.com/rhf/jokes/89q1/bjobs.229.html")))
				assertEquals(1, pa.getScore());
		assertEquals(1, result.size());
	}

}
