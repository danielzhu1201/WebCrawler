package assignment;

import java.awt.RenderingHints;
import java.lang.Character.Subset;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.*;
import java.util.function.BinaryOperator;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.print.attribute.standard.PagesPerMinute;

import org.attoparser.ParseException;

import com.sun.java.swing.plaf.windows.resources.windows_zh_TW;
import com.sun.xml.internal.bind.v2.runtime.RuntimeUtil.ToStringAdapter;
import com.sun.xml.internal.bind.v2.runtime.unmarshaller.XsiNilLoader.Array;
import com.sun.xml.internal.ws.api.pipe.ThrowableContainerPropertySet;

import sun.reflect.generics.tree.Tree;

/**
 * A query engine which holds an underlying web index and can answer textual
 * queries with a collection of relevant pages.
 *
 * TODO: Implement this!
 */
public class WebQueryEngine {
	/**
	 * Returns a WebQueryEngine that uses the given Index to constructe answers to
	 * queries.
	 *
	 * @param index
	 *            The WebIndex this WebQueryEngine should use.
	 * @return A WebQueryEngine ready to be queried.
	 */
	public static WebQueryEngine fromIndex(WebIndex index) {
		return new WebQueryEngine(index);
	}

	/**
	 * comparator used to rank the page
	 * 
	 * @author zhaosong
	 *
	 */
	class PageRankCompare implements Comparator<Page> {

		@Override
		public int compare(Page o1, Page o2) {
			int score1 = o1.getScore();
			int score2 = o2.getScore();
			return score2 - score1;
		}

	}

	public WebQueryEngine() {
	};

	private WebIndex myIndex;

	// constructor
	public WebQueryEngine(Index index) {
		myIndex = (WebIndex) index;
	}

	/**
	 * Returns a Collection of URLs (as Strings) of web pages satisfying the query
	 * expression.
	 *
	 * @param query
	 *            A query expression.
	 * @return A collection of web pages satisfying the query.
	 * @throws MalformedURLException
	 * @throws ParseException
	 */

	public Collection<Page> query(String query) throws MalformedURLException, ParseException {
		// all queries will be trimmed before processing
		if (myIndex == null)
			throw new NullPointerException("Index is not initialized!");
		if (checkCharacters(query.trim()) == false) {
			throw new ParseException("Incorrect Query Input");
		}
		if ((query.contains("(") | query.contains(")")))
			if (checkQuery(query.trim()) == false) {
				throw new ParseException("Incorrect Query Input");
			}
		// set init string to be query
		setCurrentStream(query.trim());
		ArrayList<Token> postFix = infixToPostfix(query.trim());
		return postfixCalc(postFix);
	}

	/**
	 * with a given set of postfix tokens perform calculations
	 * 
	 * @param postfix
	 * @return
	 */
	public ArrayList<Page> postfixCalc(ArrayList<Token> postfix) {
		// edge case to deal with
		if (postfix.size() == 0)
			return new ArrayList<Page>();

		Stack<Object> postfixStack = new Stack<>();
		ArrayList<Page> result = null;
		for (int i = 0; i < postfix.size(); i++) {
			if (postfix.get(i).isWord) {
				if (postfix.get(i).isNegation) {
					if (postfix.get(i).isWord)
						postfixStack.push(myIndex.negationSearch(postfix.get(i).word));
				} else {
					if (postfix.size() == 1) {
						result = myIndex.wordSearch(postfix.get(i).word);
						for (Page pa : result) {
							pa.setPreview(myIndex.pagePreview(pa));
						}
						Collections.sort(result, new PageRankCompare());
						return result;
					}
					postfixStack.push(postfix.get(i)); // single word query
				}
			}

			if (postfix.get(i).isPhrase) {
				if (postfix.get(i).isNegation) {
					ArrayList<Page> searchResult = myIndex.phraseSearch(postfix.get(i).phrase);
					postfixStack.push(myIndex.negationSearchOnResult(myIndex.phraseSearch(postfix.get(i).phrase)));
				} else
					postfixStack.push(myIndex.phraseSearch(postfix.get(i).phrase));// assume phrase stores phrase
			}

			if (postfix.get(i).isQuery) {
				if (postfix.get(i).isNegation)
					try {
						postfixStack.push(
								myIndex.negationSearchOnResult(postfixCalc(infixToPostfix(postfix.get(i).query))));
					} catch (ParseException e) {
						System.err.println("Error Calculating Negation Query");
						return new ArrayList<Page>();
					}
			}

			if (postfix.get(i).andToken) {
				Object right, left;
				if (postfixStack.isEmpty() == false)
					right = postfixStack.pop();
				else
					return null;
				if (postfixStack.isEmpty() == false)
					left = postfixStack.pop();
				else
					return null;
				// 4 cases collection+coll, coll+word, word+coll, word+word
				if (left instanceof Collection && right instanceof Collection)
					postfixStack.push(myIndex.intersection((ArrayList<Page>) left, (ArrayList<Page>) right));
				if (left instanceof Collection && right instanceof Token)
					postfixStack.push(myIndex.intersection((ArrayList<Page>) left, ((Token) right).word));
				if (left instanceof Token && right instanceof Collection)
					postfixStack.push(myIndex.intersection((ArrayList<Page>) right, ((Token) left).word));
				if (left instanceof Token && right instanceof Token)
					postfixStack.push(myIndex.intersection(((Token) left).word, ((Token) right).word));
			}

			if (postfix.get(i).orToken) {
				Object right, left;
				if (postfixStack.isEmpty() == false)
					right = postfixStack.pop();
				else
					return null;
				if (postfixStack.isEmpty() == false)
					left = postfixStack.pop();
				else
					return null;
				// 4 cases collection+coll, coll+word, word+coll, word+word
				if (left instanceof Collection && right instanceof Collection)
					postfixStack.push(myIndex.union((ArrayList<Page>) left, (ArrayList<Page>) right));
				if (left instanceof Collection && right instanceof Token)
					postfixStack.push(myIndex.union((ArrayList<Page>) left, ((Token) right).word));
				if (left instanceof Token && right instanceof Collection)
					postfixStack.push(myIndex.union((ArrayList<Page>) right, ((Token) left).word));
				if (left instanceof Token && right instanceof Token)
					postfixStack.push(myIndex.union(((Token) left).word, ((Token) right).word));
			}
		}
		result = (ArrayList<Page>) postfixStack.pop();
		for (Page pa : result) {
			pa.setPreview(myIndex.pagePreview(pa));
		}

		Collections.sort(result, new PageRankCompare());
		return result;

	}

	/**
	 * translate a query of infix to postfix
	 * 
	 * @param query
	 * @return
	 * @throws ParseException
	 */
	public ArrayList<Token> infixToPostfix(String query) throws ParseException {
		// obtain infix
		ArrayList<Token> infix = new ArrayList<Token>();
		setCurrentStream(query); // error!!
		Token infixCurrentToken = getToken(getCurrentStream());
		while (infixCurrentToken != null) {
			infix.add(infixCurrentToken);
			infixCurrentToken = getToken(getCurrentStream());
		}

		ArrayList<Token> output = new ArrayList<>();
		Stack<Token> operatorStack = new Stack<>();
		for (int i = 0; i < infix.size(); i++) {
			Token currentToken = infix.get(i);
			if (currentToken.isPhrase | currentToken.isWord | currentToken.isNegation)
				output.add(currentToken);
			else {
				if (currentToken.LeftParnToken)
					operatorStack.push(currentToken);
				if (currentToken.orToken) {
					if (operatorStack.empty())
						operatorStack.push(currentToken);
					else {
						Token peek = operatorStack.peek();
						while (!operatorStack.isEmpty() && !peek.LeftParnToken) {
							output.add(operatorStack.pop());
							try {
								peek = operatorStack.peek();
							} catch (Exception e) {
								continue;
							}
						}
						operatorStack.push(currentToken);
					}
				}
				if (currentToken.andToken) {
					if (operatorStack.empty())
						operatorStack.push(currentToken);
					else {
						Token peek = operatorStack.peek();
						while ((!operatorStack.isEmpty() && !peek.LeftParnToken & !peek.orToken)) {
							output.add(operatorStack.pop());
							try {
								peek = operatorStack.peek();
							} catch (Exception e) {
								continue;
							}

						}
						operatorStack.push(currentToken);
					}
				}
				if (currentToken.RightParenToken) {
					Token peek = operatorStack.peek();
					while (!operatorStack.isEmpty() && !peek.LeftParnToken) {
						output.add(operatorStack.pop());
						try {
							peek = operatorStack.peek();
						} catch (Exception e) {
							continue;
						}
					}
					operatorStack.pop();
				}
			}
		}

		while (operatorStack.isEmpty() == false) {
			output.add(operatorStack.pop());
		}

		return output;

	}

	/**
	 * translate infix tokens to postfix
	 * 
	 * @param infix
	 * @return
	 * @throws ParseException
	 */
	public ArrayList<Token> infixToPostfix(ArrayList<Token> infix) throws ParseException {
		ArrayList<Token> output = new ArrayList<>();
		Stack<Token> operatorStack = new Stack<>();
		for (int i = 0; i < infix.size(); i++) {
			Token currentToken = infix.get(i);
			if (currentToken.isPhrase | currentToken.isWord | currentToken.isNegation)
				output.add(currentToken);
			else {
				if (currentToken.LeftParnToken)
					operatorStack.push(currentToken);
				if (currentToken.orToken) {
					if (operatorStack.empty())
						operatorStack.push(currentToken);
					else {
						Token peek = operatorStack.peek();
						while (!operatorStack.isEmpty() && !peek.LeftParnToken) {
							output.add(operatorStack.pop());
							try {
								peek = operatorStack.peek();
							} catch (Exception e) {
								continue;
							}
						}
						operatorStack.push(currentToken);
					}
				}
				if (currentToken.andToken) {
					if (operatorStack.empty())
						operatorStack.push(currentToken);
					else {
						Token peek = operatorStack.peek();
						while ((!operatorStack.isEmpty() && !peek.LeftParnToken & !peek.orToken)) {
							output.add(operatorStack.pop());
							try {
								peek = operatorStack.peek();
							} catch (Exception e) {
								continue;
							}

						}
						operatorStack.push(currentToken);
					}
				}
				if (currentToken.RightParenToken) {
					Token peek = operatorStack.peek();
					while (!operatorStack.isEmpty() && !peek.LeftParnToken) {
						output.add(operatorStack.pop());
						try {
							peek = operatorStack.peek();
						} catch (Exception e) {
							continue;
						}
					}
					operatorStack.pop();
				}
			}
		}

		while (operatorStack.isEmpty() == false) {
			output.add(operatorStack.pop());
		}

		return output;

	}

	/**
	 * check if weird characters exists in a query
	 * 
	 * @param stream
	 * @return
	 */
	public boolean checkCharacters(String stream) {
		if (stream.length() == 0)
			return false;
		for (int i = 0; i < stream.length(); i++) {
			if (stream.charAt(i) == '(')
				continue;
			if (stream.charAt(i) == ')')
				continue;
			if (stream.charAt(i) == '"')
				continue;
			if (stream.charAt(i) == '&' | stream.charAt(i) == '|')
				continue;
			if (stream.charAt(i) == '!')
				continue;
			if (stream.charAt(i) == ' ')
				continue;
			if (!isLetterOrDigit(stream.charAt(i)))
				return false;
		}
		return true;
	}

	/**
	 * check if a query is correct
	 * 
	 * @param stream
	 * @return
	 */
	public boolean checkQuery(String stream) {
		if (stream.length() == 0)
			return false;

		Stack parenStack = new Stack<>();
		int parenPair = 0;
		int andOr = 0;
		boolean insidePhrase = false;
		for (int i = 0; i < stream.length(); i++) {
			// put ( into the stack
			if (stream.charAt(i) == '(') {
				if ((i + 1) < stream.length() && stream.charAt(i + 1) == ')')
					return false;
				parenStack.push('(');
			}

			// pop one element out if we can find a closing paren
			if (stream.charAt(i) == ')') {
				if (parenStack.empty())
					return false;
				if ((i + 1) < stream.length() && stream.charAt(i + 1) == '(')
					return false;
				if ((char) parenStack.peek() == '(') {
					parenStack.pop();
					parenPair++;
				}
			}

			// "
			if (stream.charAt(i) == '"') {
				insidePhrase = !insidePhrase;
				if ((i + 1) < stream.length() && stream.charAt(i + 1) == '"')
					return false;
			}

			// check on & |
			if (stream.charAt(i) == '&' | stream.charAt(i) == '|') {
				if (insidePhrase)
					return false;
				else if (i + 1 == stream.length())
					return false; // can't be the last character
				andOr++;
			}

			// !
			if (stream.charAt(i) == '!') {
				if (i + 1 == stream.length())
					return false; // can't be the last character
				else if ((i + 1) < stream.length()) {
					if (isLetterOrDigit(stream.charAt(i + 1)) == false
							&& Character.isSpace(stream.charAt(i + 1)) == false && stream.charAt(i + 1) != '('
							&& stream.charAt(i + 1) != '"')
						return false;
				}

			}
		}

		if (parenPair == andOr && parenStack.empty())
			return true;
		else
			return false;
	}

	/**
	 * The Token class, used to represent word, phrase, operator
	 * 
	 * @author zhaosong
	 *
	 */
	class Token {
		public boolean andToken;
		public boolean orToken;
		public boolean LeftParnToken;
		public boolean RightParenToken;
		public boolean isWord;
		public boolean isNegation;
		public boolean isPhrase;
		public boolean isQuery;

		public String word;
		public String phrase;
		public ArrayList<Token> query = null;

		public Token() {
		}
	}

	private String currentStream = "";

	public String getCurrentStream() {
		return currentStream;
	}

	public void setCurrentStream(String cStream) {
		currentStream = cStream;
	}

	/**
	 * return the first token we can find with a stream given
	 * 
	 * @param stream
	 * @return
	 * @throws ParseException
	 */
	public Token getToken(String stream) throws ParseException {
		if (stream.length() == 0)
			return null;
		int index = 0;
		String c = stream.substring(0, 1);
		char ch = stream.charAt(0);

		if (c.equals("&")) {
			Token andToken = new Token();
			andToken.andToken = true;
			setCurrentStream(stream.substring(1).trim());
			return andToken;
		}

		if (c.equals("|")) {
			Token orToken = new Token();
			orToken.orToken = true;
			setCurrentStream(stream.substring(1).trim());
			return orToken;
		}

		if (c.equals("(")) {
			Token leftParenToken = new Token();
			leftParenToken.LeftParnToken = true;
			setCurrentStream(stream.substring(1).trim());
			return leftParenToken;
		}

		if (c.equals(")")) {
			Token rightParenToken = new Token();
			rightParenToken.RightParenToken = true;
			setCurrentStream(stream.substring(1));
			return rightParenToken;
		}

		if (c.equals("!")) {
			Token negationToken = getToken(stream.substring(1).trim());
			if (negationToken.isWord)
				negationToken.isNegation = true;
			else if (negationToken.isPhrase)
				negationToken.isNegation = true;
			else {
				if (negationToken.LeftParnToken) {
					int parenPair = 1;
					ArrayList<Token> queries = new ArrayList<>();
					queries.add(negationToken);
					Token infixCurrentToken = getToken(getCurrentStream());
					while (infixCurrentToken != null && parenPair > 0) {
						if (infixCurrentToken.LeftParnToken) {
							queries.add(infixCurrentToken);
							parenPair = parenPair + 1;
							infixCurrentToken = getToken(getCurrentStream());
							continue;
						} else if (infixCurrentToken.RightParenToken) {
							queries.add(infixCurrentToken);
							parenPair = parenPair - 1;
							if (parenPair > 0)
								infixCurrentToken = getToken(getCurrentStream());
							continue;
						}
						queries.add(infixCurrentToken);
						infixCurrentToken = getToken(getCurrentStream());
					}
					Token newToken = new Token();
					newToken.isQuery = true;
					newToken.isNegation = true;
					newToken.query = queries;
					return newToken;
				} else
					throw new ParseException("Error Negation Grammar");
			}

			return negationToken;
		}

		if (c.equals(" ")) {
			if (stream.length() == 1)
				return null;
			else {
				if (stream.charAt(1) != ' ' && stream.charAt(1) != '&' && stream.charAt(1) != '|'
						&& stream.charAt(1) != ')') {
					// right ) needed
					Token andToken = new Token();
					andToken.andToken = true;
					setCurrentStream(stream.substring(1));
					return andToken;
				} else
					return getToken(stream.substring(1));
			}
		}

		// phrase case
		if (ch == '"') {
			index++;
			while (stream.charAt(index) != '"' && index < stream.length()) {
				index++;
				if (index == stream.length())
					continue;
			}

			// formatting
			Token phrase = new Token();
			phrase.isPhrase = true;
			phrase.phrase = stream.substring(0, index);
			Pattern pattern = Pattern.compile("[a-zA-Z0-9]+");
			Matcher match = pattern.matcher(phrase.phrase);
			String temp = "";
			while (match.find())
				temp = temp + match.group() + " ";
			phrase.phrase = temp.substring(0, temp.length() - 1);

			if (index >= stream.length()) {
				setCurrentStream("");
				return phrase;
			}

			else {
				setCurrentStream(stream.substring(index + 1));
				return phrase;
			}

		}

		// word case
		else {
			while (ch != '&' && ch != '|' && ch != '(' && ch != ')' && ch != ' ' && index < stream.length()) {
				index++;
				if (index == stream.length())
					continue;
				ch = stream.charAt(index);
			}
			c = stream.substring(0, index).trim();
			Token wordToken = new Token();
			wordToken.isWord = true;
			wordToken.word = c;
			setCurrentStream(stream.substring(index));
			return wordToken;
		}
	}

	/**
	 * check if a character is a letter or digit
	 * 
	 * @param c
	 * @return
	 */
	private static boolean isLetterOrDigit(char c) {
		return (c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z') || (c >= '0' && c <= '9');
	}

}
