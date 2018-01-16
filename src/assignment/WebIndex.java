package assignment;

import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.sun.org.apache.bcel.internal.generic.DDIV;

/**
 * A web-index which efficiently stores information about pages. Serialization
 * is done automatically via the superclass "Index" and Java's Serializable
 * interface.
 *
 * TODO: Implement this!
 */
public class WebIndex extends Index {
	/**
	 * Needed for Serialization (provided by Index) - don't remove this!
	 */
	private static final long serialVersionUID = 1L;

	// TODO: Implement all of this! You may choose your own data structures and
	// internal APIs.
	// You should not need to worry about serialization (just make any other data
	// structures you use
	// here also serializable - the Java standard library data structures already
	// are, for example).

	public HashMap<URL, HashMap<Integer, String>> URLPositionMap = new HashMap<URL, HashMap<Integer, String>>();
	public HashMap<URL, HashMap<String, ArrayList<Integer>>> URLWordMap = new HashMap<URL, HashMap<String, ArrayList<Integer>>>();
	public HashMap<URL, String> titleMap = new HashMap<>();

	/**
	 * This method a word with given position under a certain URL to the HashMap
	 * 
	 * @param url
	 * @param position
	 * @param word
	 */
	public void addNewURL(URL url, int position, String word) {
		// if the url not added
		if (!URLPositionMap.containsKey(url)) {
			HashMap<Integer, String> wordPositionMap = new HashMap<>();
			wordPositionMap.put(position, word);
			URLPositionMap.put(url, wordPositionMap);

			// add to URLWordMap
			HashMap<String, ArrayList<Integer>> wordOccurMap = new HashMap<>();
			ArrayList<Integer> wordOccur = new ArrayList<>();
			wordOccur.add(position);
			wordOccurMap.put(word, wordOccur);
			URLWordMap.put(url, wordOccurMap);
		}

		// if url exists
		else {
			URLPositionMap.get(url).put(position, word); // identical
			// urlwordmap
			HashMap<String, ArrayList<Integer>> wordOccurMap = URLWordMap.get(url);
			if (wordOccurMap.containsKey(word))
				URLWordMap.get(url).get(word).add(position);
			else { // if word not in map
				ArrayList<Integer> wordOccur = new ArrayList<>();
				wordOccur.add(position);
				URLWordMap.get(url).put(word, wordOccur);
			}
		}
	}

	/**
	 * This method adds a title from handler
	 * 
	 * @param url
	 * @param title
	 */
	public void addTitle(URL url, String title) {
		titleMap.put(url, title);
	}

	/**
	 * with a given word, this method searches all URLs we have and return all pages
	 * with this word
	 * 
	 * @param word
	 * @return
	 */
	public ArrayList<Page> wordSearch(String word) {
		ArrayList<Page> result = new ArrayList<>();
		for (URL url : URLPositionMap.keySet()) {
			if (URLPositionMap.get(url).containsValue(word)) {
				Page page = new Page(url);
				page.addScore(word, URLWordMap.get(url).get(word).size());
				page.addTitle(titleMap.get(url));
				result.add(page);
			}
		}
		return result;
	}

	/**
	 * With a given phrase, this method finds all pages with this phrase
	 * 
	 * @param phrase
	 * @return ArrayList<Page>
	 */
	public ArrayList<Page> phraseSearch(String phrase) {
		ArrayList<String> wordList = new ArrayList<>();
		Pattern p = Pattern.compile("[a-zA-Z0-9]+");
		Matcher matcher = p.matcher(phrase);
		while (matcher.find()) {
			wordList.add(matcher.group());
		}

		ArrayList<Page> possibleChoice = wordSearch(wordList.get(0));
		ArrayList<ArrayList<Integer>> index = new ArrayList<>();

		for (Page page : possibleChoice) {
			index.add(URLWordMap.get(page.getURL()).get(wordList.get(0)));
		}

		// checking each possible URL-position
		for (int i = 0; i < possibleChoice.size(); i++) {
			ArrayList<Integer> temp = new ArrayList<>();
			for (int j = 0; j < index.get(i).size(); j++) {
				ArrayList<Integer> positionList = index.get(i);
				boolean flag = true;
				for (int k = 1; k < wordList.size(); k++) {
					if (URLPositionMap.get(possibleChoice.get(i).getURL()).get(k + positionList.get(j))
							.equals(wordList.get(k)) == false) {
						flag = false;
						break;
					}
				}
				if (flag)
					temp.add(positionList.get(j));
			}
			index.set(i, temp);
		}

		// output the results
		ArrayList<Page> result = new ArrayList<>();
		for (int i = 0; i < index.size(); i++)
			if (index.get(i).size() > 0) {
				possibleChoice.get(i).addScore(phrase, wordList.size());
				result.add(possibleChoice.get(i));
			}
		return result;
	}

	/**
	 * Perform negative search on a word
	 * 
	 * @param word
	 * @return
	 */
	public ArrayList<Page> negationSearch(String word) {
		ArrayList<Page> result = new ArrayList<>();
		for (URL url : URLPositionMap.keySet()) {
			if (!URLPositionMap.get(url).containsValue(word)) {
				Page page = new Page(url);
				result.add(page);
			}
		}
		return result;
	}

	/**
	 * Perform negative search on all other cases, which will return a set of pages
	 * 
	 * @param pages
	 * @return
	 */
	public ArrayList<Page> negationSearchOnResult(ArrayList<Page> pages) {
		HashSet<URL> urlHave = new HashSet<>();
		ArrayList<Page> result = new ArrayList<>();
		for (Page pa : pages)
			urlHave.add(pa.getURL());
		for (URL url : URLPositionMap.keySet())
			if (urlHave.contains(url) == false) {
				Page page = new Page(url);
				page.addTitle(titleMap.get(url));
				result.add(page);
			}
		return result;

	}

	/**
	 * give the results of c1 & c2
	 * 
	 * @param c1
	 * @param c2
	 * @return
	 */
	public ArrayList<Page> intersection(ArrayList<Page> c1, ArrayList<Page> c2) {
		ArrayList<Page> result = new ArrayList<>();
		for (int i = 0; i < c1.size(); i++)
			for (int j = 0; j < c2.size(); j++) {
				if (c1.get(i).equals(c2.get(j)))
					result.add(c1.get(i));
			}
		return result;
	}

	/**
	 * give the results of c1 & c2
	 * 
	 * @param c1
	 * @param c2
	 * @return
	 */
	public ArrayList<Page> intersection(ArrayList<Page> c1, String c2) {
		ArrayList<Page> result = new ArrayList<>();
		for (Page page : c1) {
			if (URLWordMap.get(page.getURL()).containsKey(c2)) {
				page.addScore(c2, URLWordMap.get(page.getURL()).get(c2).size());
				page.addTitle(titleMap.get(page.getURL()));
				result.add(page);
			}
		}
		return result;
	}

	/**
	 * give the results of c1 & c2
	 * 
	 * @param c1
	 * @param c2
	 * @return
	 */
	public ArrayList<Page> intersection(String c1, String c2) {
		ArrayList<Page> result = new ArrayList<>();
		for (URL url : URLPositionMap.keySet()) {
			if (URLPositionMap.get(url).containsValue(c1) && URLPositionMap.get(url).containsValue(c2)) {
				Page page = new Page(url);
				page.addScore(c1, URLWordMap.get(url).get(c1).size());
				page.addScore(c2, URLWordMap.get(url).get(c2).size());// both showed up so add both
				page.addTitle(titleMap.get(url));
				result.add(page);
			}
		}
		return result;
	}

	/**
	 * give the results of c1 | c2
	 * 
	 * @param c1
	 * @param c2
	 * @return
	 */
	public ArrayList<Page> union(ArrayList<Page> c1, ArrayList<Page> c2) {
		ArrayList<Page> result = c1;
		int initSize = result.size();
		for (int i = 0; i < c2.size(); i++) {
			int flag = 0;
			for (int j = 0; j < initSize; j++) {
				if (c2.get(i).equals(result.get(j)))
					flag = 1;
			}
			if (flag == 0)
				result.add(c2.get(i));
		}
		return result;
	}

	/**
	 * give the results of c1 | c2
	 * 
	 * @param c1
	 * @param c2
	 * @return
	 */
	public ArrayList<Page> union(ArrayList<Page> c1, String c3) {
		ArrayList<Page> result = c1;
		ArrayList<Page> c2 = wordSearch(c3);
		int initSize = result.size();
		for (int i = 0; i < c2.size(); i++) {
			int flag = 0;
			for (int j = 0; j < initSize; j++) {
				if (c2.get(i).equals(result.get(j))) {
					flag = 1;
					result.get(j).addScore(c3, URLWordMap.get(result.get(j).getURL()).get(c3).size());
					result.get(j).addTitle(titleMap.get(result.get(j).getURL()));

				}
			}
			if (flag == 0) {
				c2.get(i).addScore(c3, URLWordMap.get(c2.get(i).getURL()).get(c3).size());
				result.add(c2.get(i));
			}
		}
		return result;
	}

	/**
	 * give the results of c1 | c2
	 * 
	 * @param c1
	 * @param c2
	 * @return
	 */
	public ArrayList<Page> union(String c1, String c2) {
		ArrayList<Page> result = new ArrayList<>();
		for (URL url : URLPositionMap.keySet()) {
			if (URLPositionMap.get(url).containsValue(c1)) {
				Page page = new Page(url);
				page.addScore(c1, URLWordMap.get(url).get(c1).size());
				page.addTitle(titleMap.get(url));
				result.add(page);
			} else if (URLPositionMap.get(url).containsValue(c2)) {
				Page page = new Page(url);
				page.addScore(c2, URLWordMap.get(url).get(c2).size());
				page.addTitle(titleMap.get(url));
				result.add(page);
			}
		}
		return result;
	}

	/**
	 * This method uses the data we stored to set preview for a given page object
	 * 
	 * @param page
	 * @return
	 */
	public String pagePreview(Page page) {
		Set<String> words = page.wordScore.keySet();
		URL currentURL = page.getURL();
		String preview = "";
		Iterator<String> iterator = words.iterator();
		for (int i = 0; i < words.size() && i < 5; i++) {
			if (iterator.hasNext()) {
				String currentWord = iterator.next();
				int position = 0;
				if (URLWordMap.get(currentURL).get(currentWord) != null)
					position = URLWordMap.get(currentURL).get(currentWord).get(0);
				else
					continue;
				if (URLPositionMap.get(currentURL).get(position - 2) != null) {
					preview = preview + " " + URLPositionMap.get(currentURL).get(position - 2);
				}
				if (URLPositionMap.get(currentURL).get(position - 1) != null)
					preview = preview + " " + URLPositionMap.get(currentURL).get(position - 1);
				preview = preview + " " + "<span style=\"background-color: #FFFF00\">" + currentWord + "</span>";
				if (URLPositionMap.get(currentURL).get(position + 1) != null)
					preview = preview + " " + URLPositionMap.get(currentURL).get(position + 1);
				if (URLPositionMap.get(currentURL).get(position + 2) != null)
					preview = preview + " " + URLPositionMap.get(currentURL).get(position + 2);
			}

			preview = preview + "...";

		}
		return preview;
	}

}
