package assignment;

import java.util.*;
import java.util.Map.Entry;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;
import java.net.*;
import org.attoparser.simple.*;

/**
 * A markup handler which is called by the Attoparser markup parser as it parses
 * the input; responsible for building the actual web index.
 *
 * TODO: Implement this!
 */
public class CrawlingMarkupHandler extends AbstractSimpleMarkupHandler {

	public URL currentURL;
	public int wordCounter = 0; // will not be destroyed
	public ArrayList<URL> leftoverURLs = new ArrayList<URL>();
	public WebIndex index = new WebIndex();
	String title = "";
	String currentOpenElement;
	boolean titleFlag;

	public CrawlingMarkupHandler() {
	}

	/**
	 * This method returns the complete index that has been crawled thus far when
	 * called.
	 */
	public Index getIndex() {
		return index;
	}

	/**
	 * This method returns any new URLs found to the Crawler; upon being called, the
	 * set of new URLs should be cleared.
	 */

	/**
	 * This method will first check if this URL has already been added if not add to
	 * process next
	 * 
	 * @param url
	 * @return
	 */
	boolean addURL(URL url) {
		URL currentURL = url;
		if (url.toString().contains("#")) {
			try {
				currentURL = new URL(currentURL.toString().substring(0, url.toString().indexOf('#')));
			} catch (Exception e) {
			}
		}

		if (!WebCrawler.set.contains(currentURL)) {
			WebCrawler.set.add(currentURL);
			leftoverURLs.add(currentURL);
			return true;
		}
		return false;
	}

	public List<URL> newURLs() {
		return leftoverURLs;
	}

	/**
	 * These are some of the methods from AbstractSimpleMarkupHandler. All of its
	 * method implementations are NoOps, so we've added some things to do; please
	 * remove all the extra printing before you turn in your code.
	 *
	 * Note: each of these methods defines a line and col param, but you probably
	 * don't need those values. You can look at the documentation for the superclass
	 * to see all of the handler methods.
	 */

	/**
	 * Called when the parser first starts reading a document.
	 * 
	 * @param startTimeNanos
	 *            the current time (in nanoseconds) when parsing starts
	 * @param line
	 *            the line of the document where parsing starts
	 * @param col
	 *            the column of the document where parsing starts
	 */
	public void handleDocumentStart(long startTimeNanos, int line, int col) {
		// reset variables
		wordCounter = 0;
		leftoverURLs = new ArrayList<URL>();
	}

	/**
	 * Called when the parser finishes reading a document.
	 * 
	 * @param endTimeNanos
	 *            the current time (in nanoseconds) when parsing ends
	 * @param totalTimeNanos
	 *            the difference between current times at the start and end of
	 *            parsing
	 * @param line
	 *            the line of the document where parsing ends
	 * @param col
	 *            the column of the document where the parsing ends
	 */
	public void handleDocumentEnd(long endTimeNanos, long totalTimeNanos, int line, int col) {
		// add this page to index;
		Pattern p = Pattern.compile("[a-zA-Z0-9]+");
		Matcher matcher = p.matcher(title.toLowerCase());
		String temp = "";
		while (matcher.find())
			temp = temp + " " + matcher.group();
		index.titleMap.put(currentURL, temp.trim());
	}

	/**
	 * Called at the start of any tag.
	 * 
	 * @param elementName
	 *            the element name (such as "div")
	 * @param attributes
	 *            the element attributes map, or null if it has no attributes
	 * @param line
	 *            the line in the document where this elements appears
	 * @param col
	 *            the column in the document where this element appears
	 */

	// stick to direct hyperlinks via the "a" tag
	public void handleOpenElement(String elementName, Map<String, String> attributes, int line, int col) {
		currentOpenElement = elementName;
		URL currentRelativeURL = null;
		String currentRelativeURLString = null;
		if (elementName.toLowerCase().equals("a")) {
			// attribute testing
			Set keyset = attributes.keySet();
			Iterator iterator = keyset.iterator();
			while (iterator.hasNext()) {
				String key = (String) iterator.next();
				if (key.toLowerCase().equals("href")) {
					currentRelativeURLString = attributes.get(key);
					if (!currentRelativeURLString.contains(".htm"))
						return; // edge case of#
				}
			}

			// try to paste out absolute path
			try {
				currentRelativeURL = new URL(currentURL, currentRelativeURLString);
			} catch (MalformedURLException e) {
			}

			if (currentRelativeURL != null) {
				addURL(currentRelativeURL);
			}

		}

		if (elementName.equals("title")) {
			title = "";
			titleFlag = true;

		}
	}

	/**
	 * Called at the end of any tag.
	 * 
	 * @param elementName
	 *            the element name (such as "div").
	 * @param line
	 *            the line in the document where this elements appears.
	 * @param col
	 *            the column in the document where this element appears.
	 */
	public void handleCloseElement(String elementName, int line, int col) {
		// TODO: Implement this.
		if (elementName.equals("title")) {
			titleFlag = false;
		}

	}

	/**
	 * Called whenever characters are found inside a tag. Note that the parser is
	 * not required to return all characters in the tag in a single chunk.
	 * Whitespace is also returned as characters.
	 * 
	 * @param ch
	 *            buffer containint characters; do not modify this buffer
	 * @param start
	 *            location of 1st character in ch
	 * @param length
	 *            number of characters in ch
	 */
	public void handleText(char ch[], int start, int length, int line, int col) {
		if (currentOpenElement.toLowerCase().trim().equals("style")
				| currentOpenElement.toLowerCase().trim().equals("script")) {
			return;
		}
		String temp = "";
		for (int i = start; i < start + length; i++) {
			// Instead of printing raw whitespace, we're escaping it
			// traverse whole thing and then add position
			// and query constant time different index // key be the word value position

			// put all words in this interval to a string
			switch (ch[i]) {
			case '\\': {
				temp = temp + " ";
				if (titleFlag)
					title = title + "";
			}
				break;
			case '"': {
				temp = temp + " ";
				if (titleFlag)
					title = title + "";
			}
				break;
			case '\n': {
				temp = temp + " ";
				if (titleFlag)
					title = title + "";
			}
			case '\r': // enter
			{
				temp = temp + " ";
				if (titleFlag)
					title = title + "";
			}
				break;
			case '\t': {
				temp = temp + " ";
				if (titleFlag)
					title = title + "";
			}
				break;
			default: {
				temp = temp + ch[i];
				if (titleFlag)
					title = title + ch[i];
			}
				break;
			}
		}
		// convert string to words with position
		Pattern p = Pattern.compile("[a-zA-Z0-9]+");
		Matcher matcher = p.matcher(temp.toLowerCase());
		while (matcher.find()) {
			index.addNewURL(currentURL, wordCounter, matcher.group());
			wordCounter++;
		}

	}

	/**
	 * Set current URL
	 * 
	 * @param url
	 */
	public void currentURL(URL url) {
		currentURL = url;
		addURL(currentURL);

	}
}
