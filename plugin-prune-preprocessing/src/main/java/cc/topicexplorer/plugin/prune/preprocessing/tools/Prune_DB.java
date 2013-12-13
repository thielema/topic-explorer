package cc.topicexplorer.plugin.prune.preprocessing.tools;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.nio.charset.Charset;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.Types;
import java.util.Properties;

import org.apache.commons.chain.Context;

import cc.topicexplorer.chain.CommunicationContext;
import cc.topicexplorer.chain.commands.DependencyCommand;
import cc.topicexplorer.database.Database;

import com.csvreader.CsvReader;

/**
 * MIT-JOOQ-START import static jooq.generated.Tables.DOCUMENT_TERM_TOPIC;
 * MIT-JOOQ-ENDE
 */

public class Prune_DB extends DependencyCommand {
	private Properties properties;
	private static CsvReader inCsv;
	protected cc.topicexplorer.database.Database database;

	private void renameFile(String source, String destination) {
		File sourceFile = new File(source);
		File destinationFile = new File(destination);

		if (!sourceFile.renameTo(destinationFile)) {
			logger.fatal("[ " + getClass() + " ] - " + "Fehler beim Umbenennen der Datei: " + source);
			System.exit(0);
		}
	}

	@Override
	public void specialExecute(Context context) throws Exception {

		logger.info("[ " + getClass() + " ] - " + "pruning vocabular");

		CommunicationContext communicationContext = (CommunicationContext) context;
		properties = (Properties) communicationContext.get("properties");
		database = (Database) communicationContext.get("database");

		float upperBound, lowerBound;
		String inFilePath = properties.getProperty("InCSVFile");
		try {
			inCsv = new CsvReader(new FileInputStream(inFilePath), ';', Charset.forName("UTF-8"));

			if (inCsv.readHeaders()) {
				String[] headerEntries = inCsv.getHeaders();
				String select = "TEMP4PRUNE." + headerEntries[0];
				String header = "\"" + headerEntries[0] + "\"";
				for (int j = 1; j < headerEntries.length; j++) {
					select += ",TEMP4PRUNE." + headerEntries[j];
					header += ";\"" + headerEntries[j] + "\"";
				}

				float upperBoundPercent = Float.parseFloat(properties.getProperty("Prune_upperBound"));
				float lowerBoundPercent = Float.parseFloat(properties.getProperty("Prune_lowerBound"));

				// are the bounds valid?
				if (upperBoundPercent < 0 || lowerBoundPercent < 0 || upperBoundPercent > 100
						|| lowerBoundPercent > 100 || upperBoundPercent < lowerBoundPercent) {
					logger.fatal("Stop: Invalid Pruning Bounds!");
					System.exit(0);
				}

				// copy from doctermtopic and delete topic_id
				/**
				 * MIT-JOOQ-START database.executeUpdateQuery(
				 * "CREATE TABLE IF NOT EXISTS TEMP4PRUNE LIKE " +
				 * DOCUMENT_TERM_TOPIC.getName()); MIT-JOOQ-ENDE
				 */
				/** OHNE_JOOQ-START */
				database.executeUpdateQuery("CREATE TABLE IF NOT EXISTS TEMP4PRUNE LIKE DOCUMENT_TERM_TOPIC");
				/** OHNE_JOOQ-ENDE */
				database.executeUpdateQuery("ALTER TABLE TEMP4PRUNE DROP COLUMN TOPIC_ID");

				database.executeUpdateQuery("LOAD DATA LOCAL INFILE '" + inFilePath + "' IGNORE INTO TABLE TEMP4PRUNE "
						+ "CHARACTER SET UTF8 FIELDS TERMINATED BY ';' ENCLOSED BY '\"' IGNORE 1 LINES (" + select
						+ ");");

				ResultSet rsDocCount = database.executeQuery("SELECT COUNT(DISTINCT DOCUMENT_ID) FROM TEMP4PRUNE");
				int count = 0;
				if (rsDocCount.next()) {
					count = rsDocCount.getInt(1);
					upperBound = (float) (count / 100.0) * upperBoundPercent;
					lowerBound = (float) (count / 100.0) * lowerBoundPercent;
				} else {
					lowerBound = 0.0f;
					upperBound = Float.MAX_VALUE;
				}

				logger.info("Pruning: count: " + count + " lower: " + lowerBound + " upper: " + upperBound);
				PrintWriter writer = new PrintWriter(new OutputStreamWriter(new BufferedOutputStream(
						new FileOutputStream(properties.getProperty("InCSVFile") + ".pruned.Lower." + lowerBound
								+ ".Upper." + upperBound + ".csv")), "UTF-8"));

				database.executeUpdateQuery("CREATE TABLE TEMP4PRUNE2 (UNIQUE (TERM)) "
						+ "ENGINE=MEMORY AS SELECT TERM FROM TEMP4PRUNE GROUP BY TERM "
						+ "HAVING COUNT(DISTINCT DOCUMENT_ID) < " + upperBound + " AND COUNT(DISTINCT DOCUMENT_ID) > "
						+ lowerBound);

				ResultSet prunedRS = database.executeQuery("SELECT " + select + " FROM " + "TEMP4PRUNE,TEMP4PRUNE2 "
						+ "WHERE TEMP4PRUNE.TERM=TEMP4PRUNE2.TERM "
						+ "ORDER BY DOCUMENT_ID,POSITION_OF_TOKEN_IN_DOCUMENT");

				ResultSetMetaData md = prunedRS.getMetaData();

				writer.append(header).println();

				String line;
				while (prunedRS.next()) {
					line = "";
					for (int i = 1; i <= md.getColumnCount(); i++) {
						if (i > 1) {
							line += ";";
						}

						int type = md.getColumnType(i);
						if (type == Types.VARCHAR || type == Types.CHAR) {
							line += "\"" + prunedRS.getString(i) + "\"";
						} else {
							line += "\"" + prunedRS.getLong(i) + "\"";
						}
					}

					writer.append(line).println();
				}
				writer.close();

				database.executeUpdateQuery("DROP TABLE TEMP4PRUNE;");
				database.executeUpdateQuery("DROP TABLE TEMP4PRUNE2;");

				this.renameFile(properties.getProperty("InCSVFile"), properties.getProperty("InCSVFile") + ".org."
						+ System.currentTimeMillis());

				this.renameFile(properties.getProperty("InCSVFile") + ".pruned.Lower." + lowerBound + ".Upper."
						+ upperBound + ".csv", properties.getProperty("InCSVFile"));
			} else {
				logger.fatal("CSV-Header not read");
				System.exit(1);
			}

		} catch (FileNotFoundException e) {
			logger.fatal("Input CSV-File couldn't be read - maybe the path is incorrect");
			e.printStackTrace();
			System.exit(3);
		} catch (IOException e) {
			logger.fatal("CSV-Header not read");
			e.printStackTrace();
			System.exit(2);
		}
		inCsv.close();
	}

	@Override
	public void addDependencies() {
		beforeDependencies.add("DocumentTermTopicCreate");
		afterDependencies.add("InFilePreparation");
	}
}