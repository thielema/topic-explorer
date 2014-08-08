package cc.topicexplorer.commoncrawl;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.regex.Pattern;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.hadoop.fs.GlobPattern;
import org.apache.hadoop.mapreduce.Mapper.Context;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

// TODO move language detection to optional class
public class JapaneseBlogIdentifier extends BlogIdentifier {
    public String domainFilePath = null;
    private GlobPattern _globPattern = null;

    private final String _patternString = "[\\p{InHiragana}\\p{InKatakana}\u3000-\u303F\uFF5F-\uFF9F]+";
    private final Pattern _pattern = Pattern.compile(_patternString);

    /**
     * Constructs a new BlogIdentifier.
     * @param domainFilePath Path of a file that contains valid
     */
    public JapaneseBlogIdentifier(String domainFilePath) {
        this.domainFilePath = domainFilePath;
    }

    /**
     * Tests if the web document is a valid blog, i.e.
     * its URL matches and its metadata contains a feed.
     * If the URL does not match and the metadata contains a feed tests if the
     * language is
     * @param url The URL of the web document.
     * @param metadataString A JSON string containing the document's metadata.
     * @return true, if the document is a valid blog, false
     *
     * @see JapaneseBlogIdentifier#isValidURL(String)
     * @see JapaneseBlogIdentifier#isFeed(JsonObject)
     * @see JapaneseBlogIdentifier#isJapanese(JsonObject)
     */
    @SuppressWarnings("rawtypes")
    @Override
    public boolean isValidBlog(String url, String metadataString, Context context) {
        boolean isValidURL = isValidURL(url);

        JsonParser parser = new JsonParser();
        JsonObject metadata = parser.parse(metadataString).getAsJsonObject();

        boolean isFeed = isFeed(metadata);

        if (!isValidURL && isFeed) {
            boolean isJapanese = this.isJapanese(metadata);
            return isJapanese;
        }

        return isValidURL && isFeed;
    }

    /**
     * Tests if a URL is valid by matching it against urls in the file
     * domainFilePath.
     * @param url The url that should be tested.
     * @return true, if url is valid, false otherwise.
     */
    @Override
    public boolean isValidURL(String url) {
        if (this._globPattern == null) {
            this.initializeGlobPattern();
        }

        return this._globPattern.matches(url);
    }

    private void initializeGlobPattern() {
        try {
            List<?> lines = FileUtils.readLines(new File(this.domainFilePath));
            String globPatternString = StringUtils.join(lines.toArray(), ",");

            this._globPattern = new GlobPattern("http*://{" + globPatternString + "}");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Tests if a blog contains Japanese Posts.
     * @param metadata The metadata of the blog.
     * @return true, if japanese posts were found, false otherwise.
     */
    public boolean isJapanese(JsonObject metadata) {
        try {
            JsonObject content = metadata.get("content").getAsJsonObject();
            JsonArray items = content.get("items").getAsJsonArray();
            for (JsonElement e: items) {
                JsonObject o = e.getAsJsonObject();
                String title = o.get("title").getAsString();

                boolean foundJapaneseChars = this._pattern.matcher(title).matches();
                if (foundJapaneseChars) {
                    return true;
                }
            }
        } catch(Exception e) {
            return false;
        }

        return false;
    }

    /**
     * Tests if a JSON object contains a feed.
     * @param metadataString The JSON object.
     * @return true, if a feed is found, false otherwise.
     */
    @Override
    public boolean isFeed(JsonObject metadata) {

        // catch malformed
        try {
            JsonObject content = metadata.get("content").getAsJsonObject();
            String contentType = content.get("type").getAsString();
            return contentType.contains("feed");
        } catch (Exception e) {
            return false;
        }
    }
}
