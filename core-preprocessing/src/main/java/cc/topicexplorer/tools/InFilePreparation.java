package cc.topicexplorer.tools;

import java.io.BufferedWriter;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.charset.Charset;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import org.apache.commons.chain.Context;
import org.apache.log4j.Logger;

import cc.topicexplorer.chain.CommunicationContext;
import cc.topicexplorer.chain.commands.DependencyCommand;
import cc.topicexplorer.database.Database;

import com.csvreader.CsvReader;

/** MIT-JOOQ-START 
import static jooq.generated.Tables.DOCUMENT_TERM_TOPIC;
MIT-JOOQ-ENDE */ 

/**
 * <h1>This class represents the first step in the TopicExplorer-workflow</h1>
 * 
 * <p>
 * The class owns functions to proof the given in-file and prepare the
 * Mallet-in-file
 * </p>
 * 
 * @author Matthias Pfuhl
 * 
 */
public class InFilePreparation extends DependencyCommand {
	private String malletPreparedFile;
	private Properties properties;
	private static cc.topicexplorer.database.Database database;
	private static CsvReader inCsv;
	private static Logger logger = Logger.getRootLogger();

	/**
	 * The funtion checks if the used separator and the static defined names are
	 * correct
	 * 
	 * @param colEntries
	 *            are the names we have to check
	 * @return true if all is correct, else false (with a little information)
	 */
	public static boolean checkHeader(String inFile) throws SQLException{
		try {
			inCsv = new CsvReader(new FileInputStream(inFile), ';',
					Charset.forName("UTF-8"));
			try {
				if (inCsv.readHeaders()) {
					String[] headerEntries = inCsv.getHeaders();
					List<String> tableColumnList=new ArrayList<String>();
					
					/** MIT-JOOQ-START 
					ResultSet rs = database.executeQuery("SELECT * FROM "
							+ DOCUMENT_TERM_TOPIC.getName());
                    MIT-JOOQ-ENDE */ 
					/** OHNE_JOOQ-START */ 
					ResultSet rs = database.executeQuery("SELECT * FROM "
							+ "DOCUMENT_TERM_TOPIC");
					/** OHNE_JOOQ-ENDE */ 
					ResultSetMetaData md = rs.getMetaData();
					for (int i = 1; i <= md.getColumnCount(); i++) {
						tableColumnList.add(md.getColumnName(i));
					}

					for (int j = 0; j < headerEntries.length; j++) {
						if (!tableColumnList.contains(headerEntries[j])) {
							logger.warn("The CSV-Column "
									+ headerEntries[j]
													+ " is not in the DOCUMENT_TERM_TOPIC table");
							return false;
						}
					}
				} else {
					logger.fatal("CSV-Header not read");
					System.exit(1);
				}
			} catch (IOException e) {
				logger.fatal("CSV-Header not read" + e);
				System.exit(2);
			}
		} catch (FileNotFoundException e) {
			logger.fatal("Input CSV-File couldn't be read - maybe the path is incorrect" + e);
			System.exit(3);
		}
		return true;
	}

	private boolean writeMalletInFile() throws IOException {
		BufferedWriter malletInFileWriter = new BufferedWriter(
				new OutputStreamWriter(
						new FileOutputStream(malletPreparedFile), "UTF-8"));
		int currentDocID = -1;
		String documentString = "";

		while (inCsv.readRecord()) {
			if (currentDocID == -1) {

				// first Doc -> Set the currentDocID
				currentDocID = Integer.parseInt(inCsv.get("DOCUMENT_ID"));
				// start to set the documentString with ID and LANGUAGE
				documentString = currentDocID + "\tDE\t" + inCsv.get("TERM");

			} else if (currentDocID != Integer.parseInt(inCsv
					.get("DOCUMENT_ID"))) {

				// new document, write current down
				documentString += "\n";
				malletInFileWriter.write(documentString);
				// set new DocID
				currentDocID = Integer.parseInt(inCsv.get("DOCUMENT_ID"));
				// start to set the new documentString with ID and LANGUAGE
				documentString = currentDocID + "\tDE\t" + inCsv.get("TERM");

			} else {

				// same Document, but new Token -> append it
				documentString += " " + inCsv.get("TERM");
			}
		}
		// We have to set the last document
		malletInFileWriter.write(documentString);

		malletInFileWriter.close();

		return true;
	}

	@Override
	public void specialExecute(Context context) throws Exception {

		logger.info("[ " + getClass() + " ] - "
				+ "preparing the in-file for mallet");

		CommunicationContext communicationContext = (CommunicationContext) context;
		properties = (Properties) communicationContext.get("properties");
		database = (Database) communicationContext.get("database");
		malletPreparedFile = "temp/malletinput.txt";
		String inFile = properties.getProperty("InCSVFile");

		if (checkHeader(inFile)) {
			if (writeMalletInFile()) {
				logger.info("[ " + getClass() + " ] - "
						+ "the in-file for mallet successfully prepared");
				inCsv.close();
			} else {
				System.exit(4);
			}
		} else {
			System.exit(0);
		}
	}
}