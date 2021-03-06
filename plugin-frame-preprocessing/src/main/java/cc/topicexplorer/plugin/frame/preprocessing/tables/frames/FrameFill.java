package cc.topicexplorer.plugin.frame.preprocessing.tables.frames;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;

import cc.topicexplorer.commands.TableFillCommand;

import com.google.common.collect.Sets;

/**
 * <b>Needed database tables</b>: {@code TERM}, {@code TERM_TOPIC},
 * {@code DOCUMENT_TERM_TOPIC}.
 * <p>
 * <b>{@link #fillTable()} method</b> will arrange the content of the table
 * {@code TopTermsDocSameTopic} and search for frames (noun-verb combinations)
 * in that table. Every so found frame will be written to the table
 * {@code FRAMES}. For every topic only the 20 best fitting nouns and 20 best
 * fitting verbs are used.
 * <p>
 * <b>Frames will be identified</b> if the distance between a noun and a
 * succeeding verb is less or equal 150 characters. Further both, noun and verb,
 * must be consistent with their {@code TOPIC_ID} and {@code DOCUMENT_ID}.
 * Accepted values in the column {@code $WORDTYPE} are {@code SUBS} and
 * {@code VERB}.
 * <p>
 * Within any frame there will be <b>only one noun and one verb</b>. No second
 * verb to a specific noun and no second noun to a specific verb, unless one
 * occurs a second time in the text corpus.
 */
public final class FrameFill extends TableFillCommand {

	private static final Logger logger = Logger.getLogger(FrameFill.class);
	private List<String> frameTypes = new ArrayList<String>();

	@Override
	public void setTableName() {
		this.tableName = "FRAME$FRAMES";
	}

	@Override
	public Set<String> getAfterDependencies() {
		return Sets.newHashSet();
	}

	@Override
	public Set<String> getBeforeDependencies() {
		return Sets.newHashSet("TermFill", "TermTopicFill", "DocumentTermTopicFill", "Frame_FrameCreate",
				"WordType_TermFill", "Frame_DelimiterPositionsFill");
	}

	@Override
	public Set<String> getOptionalAfterDependencies() {
		return Sets.newHashSet();
	}

	@Override
	public Set<String> getOptionalBeforeDependencies() {
		return Sets.newHashSet("HierarchicalTopic_TermTopicFill");
	}

	@Override
	public void fillTable() {
		String[] startWordTypes = ((String) properties.get("Frame_firstWordType")).split(",");
		String[] endWordTypes = ((String) properties.get("Frame_lastWordType")).split(",");
		String[] startWordTypeLimits = ((String) properties.get("Frame_firstWordTypeLimit")).split(",");
		String[] endWordTypeLimits = ((String) properties.get("Frame_firstWordTypeLimit")).split(",");
		String[] maxFrameSizes = ((String) properties.get("Frame_maxFrameSize")).split(",");
		if (!(startWordTypes.length == endWordTypes.length && startWordTypes.length == startWordTypeLimits.length
				&& startWordTypes.length == endWordTypeLimits.length && startWordTypes.length == maxFrameSizes.length)) {
			logger.error("Sizes of frame property fields do not match");
			throw new RuntimeException();
		}

		if (startWordTypes[0].isEmpty()) {
			logger.warn("Skip quitely computing frames, no frames types specified. ");
			return;
		}

		for (int i = 0; i < startWordTypes.length; i++) {
			createAndFillTableTopTerms(startWordTypes[i], startWordTypeLimits[i], endWordTypes[i], endWordTypeLimits[i]);
			logger.info(i + 1 + ". frame: topTerms filled");
			String frameType = startWordTypes[i] + "_" + startWordTypeLimits[i] + "_" + endWordTypes[i] + "_"
					+ endWordTypeLimits[i] + "_" + maxFrameSizes[i];
			frameTypes.add(frameType);
			findFrames(Integer.parseInt(maxFrameSizes[i]), startWordTypes[i], frameType);
			logger.info(i + 1 + ". frametype filled");

			dropTopTermTable();

		}
		try {
			database.executeUpdateQuery("ALTER TABLE " + this.tableName
					+ " ADD KEY IDX0 (DOCUMENT_ID,TOPIC_ID,START_POSITION,END_POSITION) ");
		} catch (SQLException e) {
			logger.error("Exception while creating frames indezes.");
			throw new RuntimeException(e);
		}
		processFrameDelimiter();
		logger.info("frames deactivated");
		logger.info(String.format("Table %s is filled.", this.tableName));
	}

	private void processFrameDelimiter() {
		if (Boolean.parseBoolean(properties.getProperty("Frame_frameDelimiter")) == true) {
			try {
				database.executeUpdateQuery("UPDATE "
						+ this.tableName
						+ ",FRAME$DELIMITER_POSITIONS SET ACTIVE=0 WHERE "
						+ "FRAME$DELIMITER_POSITIONS.DOCUMENT_ID=FRAME$FRAMES.DOCUMENT_ID AND START_POSITION<POSITION AND END_POSITION>POSITION");
			} catch (SQLException e) {
				logger.error("Exception while deactivating frames.");
				throw new RuntimeException(e);
			}
		}
	}

	private void createAndFillTableTopTerms(String startWordType, String startWordTypeLimit, String endWordType,
			String endWordTypeLimit) {
		try {
			boolean first = true;
			boolean withPostypeTable = properties.getProperty("plugins").contains("mecab");

			ArrayList<Integer> childrenWordtypes = new ArrayList<Integer>();
			String startWordTypeChildren = "";
			String endWordTypeChildren = "";

			if (withPostypeTable) {
				ResultSet wordtypeChildrenRs = database
						.executeQuery("SELECT p1.POS FROM POS_TYPE p1, POS_TYPE p2 WHERE p2.POS=" + startWordType
								+ " AND p1.LOW>=p2.LOW AND p1.HIGH<=p2.HIGH");
				while (wordtypeChildrenRs.next()) {
					childrenWordtypes.add(wordtypeChildrenRs.getInt("POS"));
				}
				startWordTypeChildren = StringUtils.join(childrenWordtypes, ",");

				childrenWordtypes.clear();

				wordtypeChildrenRs = database.executeQuery("SELECT p1.POS FROM POS_TYPE p1, POS_TYPE p2 WHERE p2.POS="
						+ endWordType + " AND p1.LOW>=p2.LOW AND p1.HIGH<=p2.HIGH");
				while (wordtypeChildrenRs.next()) {
					childrenWordtypes.add(wordtypeChildrenRs.getInt("POS"));
				}
				endWordTypeChildren = StringUtils.join(childrenWordtypes, ",");

			} else {
				startWordType = "'" + startWordType + "'";
				endWordType = "'" + endWordType + "'";
			}

			List<Integer> topicIds = new ArrayList<Integer>();

			ResultSet topipcIdsRs = database.executeQuery("SELECT TOPIC_ID FROM TOPIC");
			while (topipcIdsRs.next()) {
				topicIds.add(topipcIdsRs.getInt("TOPIC_ID"));
			}
			for (int i : topicIds) {
				if (first) {
					database.executeUpdateQuery("create table TopTerms (TERM_NAME VARCHAR(100), WORDTYPE$WORDTYPE VARCHAR(100), TOPIC_ID INTEGER,  PR_TERM_GIVEN_TOPIC DOUBLE) ENGINE=InnoDB "
							+ "select TERM_NAME, '"
							+ startWordType
							+ "' AS WORDTYPE$WORDTYPE, TOPIC_ID, PR_TERM_GIVEN_TOPIC from TERM_TOPIC join TERM using (TERM_ID) "
							+ "where TOPIC_ID="
							+ i
							+ " AND WORDTYPE$WORDTYPE in ("
							+ startWordTypeChildren
							+ ") order by PR_TERM_GIVEN_TOPIC desc limit " + startWordTypeLimit + ";");
					database.executeUpdateQuery("alter table TopTerms add index (TOPIC_ID,TERM_NAME)");
					database.executeUpdateQuery("alter table TopTerms add index (TERM_NAME)");

					first = false;
				} else {
					database.executeUpdateQueryForUpdate("insert into TopTerms " + "select TERM_NAME, '"
							+ startWordType
							+ "' AS WORDTYPE$WORDTYPE, TOPIC_ID, PR_TERM_GIVEN_TOPIC from TERM_TOPIC join TERM "
							+ "using (TERM_ID) where TOPIC_ID=" + i + " AND WORDTYPE$WORDTYPE in ("
							+ startWordTypeChildren + ") order by PR_TERM_GIVEN_TOPIC desc limit " + startWordTypeLimit
							+ ";");
				}
			}

			for (int i : topicIds) {
				database.executeUpdateQueryForUpdate("insert into TopTerms " + "select TERM_NAME, '" + endWordType
						+ "' AS WORDTYPE$WORDTYPE, TOPIC_ID, PR_TERM_GIVEN_TOPIC from TERM_TOPIC join TERM "
						+ "using (TERM_ID) where TOPIC_ID=" + i + " AND WORDTYPE$WORDTYPE in (" + endWordTypeChildren
						+ ") order by PR_TERM_GIVEN_TOPIC desc limit " + endWordTypeLimit + ";");
			}
		} catch (SQLException e) {
			logger.error("Exception while handling temporary table TopTerms.");
			throw new RuntimeException(e);
		}
	}

	private void findFrames(int maxFrameSize, String startWordType, String frameType) {
		try {
			ResultSet topTerms;
			Statement stmt;
			File fileTemp = new File("temp/frames.sql.csv");
			if (fileTemp.exists()) {
				fileTemp.delete();
			}
			BufferedWriter frameCSVWriter = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(
					"temp/frames.sql.csv", true), "UTF-8"));

			if (Arrays.asList(properties.get("plugins").toString().split(",")).contains("hierarchicaltopic")) {
				List<Integer> docIds = new ArrayList<Integer>();

				ResultSet docIdsRS = database.executeQuery("SELECT DISTINCT DOCUMENT_ID FROM DOCUMENT");
				while (docIdsRS.next()) {
					docIds.add(docIdsRS.getInt("DOCUMENT_ID"));
				}
				stmt = database.getConnection().createStatement(java.sql.ResultSet.TYPE_FORWARD_ONLY,
						java.sql.ResultSet.CONCUR_READ_ONLY);
				stmt.setFetchSize(Integer.MIN_VALUE);
				for (int docId : docIds) {
					topTerms = stmt
							.executeQuery("select DOCUMENT_TERM_TOPIC.DOCUMENT_ID, TopTerms.TOPIC_ID, "
									+ "DOCUMENT_TERM_TOPIC.POSITION_OF_TOKEN_IN_DOCUMENT, DOCUMENT_TERM_TOPIC.TERM, DOCUMENT_TERM_TOPIC.WORDTYPE$WORDTYPE "
									+ "from DOCUMENT_TERM_TOPIC, TopTerms, TOPIC t1, TOPIC t2 WHERE DOCUMENT_TERM_TOPIC.TERM=TopTerms.TERM_NAME and "
									+ "DOCUMENT_TERM_TOPIC.TOPIC_ID=t1.TOPIC_ID AND t1.HIERARCHICAL_TOPIC$START >= t2.HIERARCHICAL_TOPIC$START AND "
									+ "t1.HIERARCHICAL_TOPIC$END <= t2.HIERARCHICAL_TOPIC$END AND t2.TOPIC_ID=TopTerms.TOPIC_ID "
									+ "and DOCUMENT_TERM_TOPIC.WORDTYPE$WORDTYPE=TopTerms.WORDTYPE$WORDTYPE AND DOCUMENT_TERM_TOPIC.DOCUMENT_ID="
									+ docId
									+ " order by TopTerms.TOPIC_ID asc,	DOCUMENT_TERM_TOPIC.POSITION_OF_TOKEN_IN_DOCUMENT asc");
					checkForFrames(maxFrameSize, startWordType, frameType, topTerms, frameCSVWriter);
				}

			} else {
				topTerms = database
						.executeQuery("select DOCUMENT_TERM_TOPIC.DOCUMENT_ID, TopTerms.TOPIC_ID, "
								+ "DOCUMENT_TERM_TOPIC.POSITION_OF_TOKEN_IN_DOCUMENT, DOCUMENT_TERM_TOPIC.TERM, DOCUMENT_TERM_TOPIC.WORDTYPE$WORDTYPE "
								+ "from DOCUMENT_TERM_TOPIC, TopTerms WHERE DOCUMENT_TERM_TOPIC.TERM=TopTerms.TERM_NAME and "
								+ "DOCUMENT_TERM_TOPIC.TOPIC_ID=TopTerms.TOPIC_ID "
								+ "and DOCUMENT_TERM_TOPIC.WORDTYPE$WORDTYPE=TopTerms.WORDTYPE$WORDTYPE "
								+ "order by DOCUMENT_TERM_TOPIC.DOCUMENT_ID asc, TopTerms.TOPIC_ID asc,	DOCUMENT_TERM_TOPIC.POSITION_OF_TOKEN_IN_DOCUMENT asc");
				checkForFrames(maxFrameSize, startWordType, frameType, topTerms, frameCSVWriter);
			}

			frameCSVWriter.flush();
			frameCSVWriter.close();

			database.executeUpdateQuery("LOAD DATA LOCAL INFILE 'temp/frames.sql.csv' IGNORE INTO TABLE "
					+ tableName
					+ " CHARACTER SET utf8 FIELDS TERMINATED BY ',' ENCLOSED BY '\"' (DOCUMENT_ID, TOPIC_ID, FRAME, START_POSITION, END_POSITION, FRAME_TYPE);");
		} catch (SQLException e) {
			logger.error("Exception while handling temporary table TOP_TERMS_DOC_SAME_TOPIC.");
			throw new RuntimeException(e);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	private void checkForFrames(int maxFrameSize, String startWordType, String frameType, ResultSet topTerms,
			BufferedWriter frameCSVWriter) {
		try {
			int documentId = 0;
			int position = 0;
			int topicId = 0;
			String term = null, wordType = null;
			int endPos;

			while (topTerms.next()) {
				if (documentId != topTerms.getInt("DOCUMENT_ID") || topicId != topTerms.getInt("TOPIC_ID")
						|| topTerms.getInt("POSITION_OF_TOKEN_IN_DOCUMENT") - position > maxFrameSize
						|| topTerms.getString("WORDTYPE$WORDTYPE").equals(startWordType)) {
					documentId = topTerms.getInt("DOCUMENT_ID");
					topicId = topTerms.getInt("TOPIC_ID");
					position = topTerms.getInt("POSITION_OF_TOKEN_IN_DOCUMENT");
					term = topTerms.getString("TERM");
					wordType = topTerms.getString("WORDTYPE$WORDTYPE");
				} else {
					if (wordType.equals(startWordType)) {

						endPos = topTerms.getInt("POSITION_OF_TOKEN_IN_DOCUMENT") + topTerms.getString("TERM").length();
						frameCSVWriter.write("\"" + documentId + "\",\"" + topicId + "\",\"" + term + ","
								+ topTerms.getString("TERM") + "\",\"" + position + "\",\"" + endPos + "\",\""
								+ frameType + "\"\n");
					}
					position = 0 - maxFrameSize;
				}
			}
		} catch (SQLException e) {
			logger.error("Error getting potential frame data.");
			throw new RuntimeException(e);
		} catch (IOException e) {
			logger.error("Error writing frame input stream.");
			throw new RuntimeException(e);
		}
	}

	private void dropTopTermTable() {
		try {
			database.dropTable("TopTerms");
		} catch (SQLException e) {
			logger.warn("At least one temporarely created table or column could not be dropped.", e);
		}
	}

}
