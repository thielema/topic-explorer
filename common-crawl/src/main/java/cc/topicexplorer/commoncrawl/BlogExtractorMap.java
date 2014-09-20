package cc.topicexplorer.commoncrawl;

import java.io.IOException;

import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Mapper;
import org.archive.io.ArchiveReader;
import org.archive.io.ArchiveRecord;

public class BlogExtractorMap {
    public static final String WARC_TYPE_HEADER_NAME = "WARC-Type";

    // Implements the map function for MapReduce.
    public static class BlogExtractorMapper extends
        Mapper<Text, ArchiveReader, Text, Text> {
        // implement the main "map" function
        @Override
        public void map(Text key, ArchiveReader value, Context context)
            throws IOException, InterruptedException {
            BlogExtractor extractor = new BlogExtractor(context);

            for (ArchiveRecord record : value) {
                // wrap the record so it's easier to use
                RecordWrapper wrapper = new RecordWrapper(record);

                if ("response".equals(wrapper.getHeader().getHeaderValue(WARC_TYPE_HEADER_NAME))) {
                    // filter out requests and crawler metadata
                    // (crawler metadata != page metadata)
                    extractor.extract(wrapper, context);
                }
            }
        }
    }
}
