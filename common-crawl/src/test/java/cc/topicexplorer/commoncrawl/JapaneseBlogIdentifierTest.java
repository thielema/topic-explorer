package cc.topicexplorer.commoncrawl;

import static org.fest.assertions.Assertions.assertThat;
import com.google.gson.JsonParser;
import com.google.gson.JsonObject;

import org.junit.Test;

import cc.topicexplorer.commoncrawl.JapaneseBlogIdentifier;

public class JapaneseBlogIdentifierTest {
    private final String path = this.getClass().getResource("/blogproviders.txt").getPath();
    private JapaneseBlogIdentifier id = new JapaneseBlogIdentifier(path);
    private JsonParser parser = new JsonParser();

    private JsonObject parseJson(String jsonString) {
        return this.parser.parse(jsonString).getAsJsonObject();
    }

    @Test
    public void testIsFeed_atomFeed() {
        String jsonString = "{\"content\":{\"type\":\"atom-feed\"}}";
        assertThat(id.isFeed(this.parseJson(jsonString))).isEqualTo(true);
    }

    @Test
    public void testIsFeed_rssFeed() {
        String jsonString = "{\"content\":{\"type\":\"rss-feed\"}}";
        assertThat(id.isFeed(this.parseJson(jsonString))).isEqualTo(true);
    }

    @Test
    public void testIsFeed_htmlDoc() {
        String jsonString = "{\"content\":{\"type\":\"html-doc\"}}";
        assertThat(id.isFeed(this.parseJson(jsonString))).isEqualTo(false);
    }

    @Test
    public void testIsFeed_invalidObject() {
        String jsonString = "{}";
        assertThat(id.isFeed(this.parseJson(jsonString))).isEqualTo(false);
    }

    @Test
    public void testIsValidURL_minkara() {
        String blogURL = "http://minkara.carview.co.jp/userid/601087/blog/";
        assertThat(id.isValidURL(blogURL)).isEqualTo(true);
    }

    @Test
    public void testIsValidURL_hatenablog() {
        String blogURL = "http://imikowa.hatenablog.com/";
        assertThat(id.isValidURL(blogURL)).isEqualTo(true);
    }

    @Test
    public void testIsValidURL_hatenaExtended() {
        String blogURL = "http://nazlife.hatenablog.com/entry/2014/07/05/";
        assertThat(id.isValidURL(blogURL)).isEqualTo(true);
    }

    @Test
    public void testIsValidURL_invalidURL() {
        String blogURL = "http://hatenablog.com";
        assertThat(id.isValidURL(blogURL)).isEqualTo(false);
    }
}