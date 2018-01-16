package assignment;

import static org.junit.Assert.assertEquals;

import java.io.Serializable;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;

/**
 * The Page class holds anything that the QueryEngine returns to the server. The
 * field and method we provided here is the bare minimum requirement to be a
 * Page - feel free to add anything you want as long as you don't break the
 * getURL method.
 *
 * TODO: Implement this!
 */
public class Page implements Serializable {
	// The URL the page was located at.
	private URL url;
	private String title = "";
	private String preview = "";
	public HashMap<String, Integer> wordScore;
	private boolean scoreCalculated = false;
	private int score = 0;

	/**
	 * Creates a Page with a given URL.
	 * 
	 * @param url
	 *            The url of the page.
	 */
	public Page(URL url) {
		wordScore = new HashMap<>();
		this.url = url;
	}

	public Page(String url) throws MalformedURLException {
		wordScore = new HashMap<>();
		this.url = new URL(url);
	}

	/**
	 * Add title to this page
	 * 
	 * @param titleInput
	 */
	public void addTitle(String titleInput) {
		title = titleInput.toLowerCase();
	}

	public String getTitle() {
		return title;
	}

	/**
	 * Add score for this page using words and recurrence
	 * 
	 * @param input
	 * @param score
	 */
	public void addScore(String input, int score) {
		wordScore.put(input, score);
		scoreCalculated = false;
	}

	/**
	 * calculate the score of the page
	 * 
	 * @return int
	 */
	public int calculateScore() {

		for (String item : wordScore.keySet()) {
			score = score + wordScore.get(item);
			if (title != null && title.contains(item))
				score = score + 3;
		}
		scoreCalculated = true;
		return score;
	}

	public int getScore() {
		if (scoreCalculated)
			return score;
		return calculateScore();
	}

	public void setPreview(String prev) {
		preview = prev;
	}

	public String getPreview() {
		return preview;
	}

	/**
	 * @return the URL of the page.
	 */
	public URL getURL() {
		return url;
	}

	/**
	 * test if two pages are equals
	 * 
	 * @param page2
	 * @return
	 */
	public boolean equals(Page page2) {
		return url.toString().equals(page2.getURL().toString());
	}
}
