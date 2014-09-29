package cc.topicexplorer.commoncrawl;

import java.io.IOException;

import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Mapper;
import org.archive.io.ArchiveReader;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

public class BlogURLExtractor extends DataExtractor {
    private static final Text   BLOG_LINK_OUTPUT_KEY = new Text("BLOG_LINK");
    private static final String HREF_ATTRIBUTE_NAME  = "href";
    private static final String TYPE_ATTRIBUTE_NAME  = "type";
    public static final String  ATOM_FEED_MIMETYPE   = "application/atom+xml";
    public static final String  RSS_FEED_MIMETYPE    = "application/rss+xml";

    @Override
    public void extract(RecordWrapper wrapper,
                        Mapper<Text, ArchiveReader, Text, Text>.Context context)
        throws IOException, InterruptedException {
        String html = wrapper.getHTTPBody();
        Document d = Jsoup.parse(html);
        Element head = d.head();
        Elements feeds = head.getElementsByAttributeValue(TYPE_ATTRIBUTE_NAME,
                                                          RSS_FEED_MIMETYPE);
        Elements atomFeeds = head.getElementsByAttributeValue(TYPE_ATTRIBUTE_NAME,
                                                              ATOM_FEED_MIMETYPE);
        feeds.addAll(atomFeeds);

        for (Element element : atomFeeds) {
            String href = element.attr(HREF_ATTRIBUTE_NAME);
            context.write(BLOG_LINK_OUTPUT_KEY, new Text(href));
        }
    }
}
