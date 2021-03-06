package wikiParser;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.Vector;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import tools.Stopwatch;
import tools.WikiIDTitlePair;

public class PreMalletAction_EntryPointForParallelisation {

	private Database db;
	private final Properties prop;
	private Stopwatch stopWatch;

	private final String fileSeparator = System.getProperty("file.separator");

	private final String fileNameInputSQLParsed = "inputsql.csv";
	private final String fileNameInputMallet = "inputmallet.csv";
	private final String fileNameLogging = "logging.txt";

	public PreMalletAction_EntryPointForParallelisation(Properties prop) {
		this.prop = prop;

		try {
			this.init();
		} catch (SQLException e) {
			e.printStackTrace();
		} catch (InstantiationException e) {
			e.printStackTrace();
		} catch (IllegalAccessException e) {
			e.printStackTrace();
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		}
	}

	private void init() throws SQLException, InstantiationException, IllegalAccessException, ClassNotFoundException {
		db = new Database(prop);

		String outputFolder = prop.getProperty("Wiki_outputFolder");
		File dir = new File(outputFolder);
		dir.mkdir();

		dir = new File(outputFolder + fileSeparator + "temp");
		dir.mkdir();

		if (prop.getProperty("Wiki_fileOutput").equalsIgnoreCase("true")) {
			String outputFolderFiles = prop.getProperty("Wiki_fileOutputFolder");
			dir = new File(outputFolderFiles);
			dir.mkdir();
		}

		stopWatch = new Stopwatch();
	}

	/**
	 * reads the output-directory and joins the outputfiles into one
	 */
	private void joinTheOutputsAndDeleteTempFilesInTempFolder() {

		System.out.println("joining the outputfiles and deleting the temp files");

		try {
			String outputFolder = prop.getProperty("Wiki_outputFolder");

			File tempFolder = new File(outputFolder + fileSeparator + "temp");
			File[] fileList = tempFolder.listFiles();

			File fileInputMallet = new File(outputFolder + fileSeparator + fileNameInputMallet);
			File fileInputSQLParsed = new File(outputFolder + fileSeparator + fileNameInputSQLParsed);
			File fileLogging = new File(outputFolder + fileSeparator + fileNameLogging);

			this.mergeDataIntoOneFile(fileInputMallet, fileList, "malletWikiText");
			this.mergeDataIntoOneFile(fileInputSQLParsed, fileList, "inputParsedText");
			this.mergeDataIntoOneFile(fileLogging, fileList, "logging");

			if (prop.getProperty("Wiki_deleteTempFolder").equalsIgnoreCase("true")) {
				deleteTempOutputFiles(tempFolder);
			}

		} catch (Exception e) {
			System.err.println("Failure in joinTheOutputsAndDeleteTempFilesInTempFolder.");
			throw new RuntimeException(e);
		}

	}

	private void mergeDataIntoOneFile(File fileName, File[] fileArray, String filterName) throws Exception {
		BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(fileName), "UTF-8"));
		BufferedReader br;

		if (filterName.contains("mallet")) {
			bw.write("\"DOCUMENT_ID\";\"POSITION_OF_TOKEN_IN_DOCUMENT\";\"TERM\";\"TOKEN\";\"WIKI$POSITION\"\n");
		}

		for (int i = 0; i < fileArray.length; i++) {
			if (fileArray[i].getName().contains(filterName)) {
				br = new BufferedReader(new InputStreamReader(new FileInputStream(fileArray[i]), "UTF-8"));

				String line = br.readLine();
				while (line != null) {

					bw.write(line);
					bw.write("\n");
					line = br.readLine();
				}
				br.close();
				bw.flush();
			}
		}
		bw.flush();
		bw.close();
	}

	private void deleteTempOutputFiles(File path) {
		try {
			for (File file : path.listFiles()) {
				if (file.isDirectory()) {
					deleteTempOutputFiles(file);
				}
				file.delete();
			}
			path.delete();
		} catch (Exception e) {// TODO Specify exception type!
			e.printStackTrace();
		}
	}

	/**
	 * Testfunktion für Ausgabe
	 * 
	 * @throws ClassNotFoundException
	 * @throws IllegalAccessException
	 * @throws InstantiationException
	 * @throws SQLException
	 */
	private void startOnlyOneParsing(Integer old_id) throws InterruptedException, SQLException, InstantiationException,
			IllegalAccessException, ClassNotFoundException {
		System.out.println("Start parsing one article.");

		SupporterForBothTypes t = new SupporterForBothTypes(prop);

		Vector<WikiIDTitlePair> vec = new Vector<WikiIDTitlePair>(1);

		vec.add(t.getOldIdAndWikiTitleFromWikiPageIdFromDatabase(old_id));

		t.closeDBConnection();

		PreMalletParallelization h = new PreMalletParallelization(vec, prop, null, "Thread-0");
		h.setOnlyOneOutputParameter(true);

		h.start();
		h.join();
	}

	private void startWithOffsetNew(Integer limitOrAll, Integer numberOfAvailableProcessors, Integer offset,
			Integer multiplicator) throws InterruptedException {
		ArrayList<WikiIDTitlePair> newList;

		// get all articles from database
		SupporterForBothTypes s = null;
		try {
			s = new SupporterForBothTypes(prop);
		} catch (SQLException e) {
			System.err.println("Database supporter could not be constructed.");
			e.printStackTrace();
		} catch (InstantiationException e) {
			System.err.println("Database supporter could not be constructed.");
			e.printStackTrace();
		} catch (IllegalAccessException e) {
			System.err.println("Database supporter could not be constructed.");
			e.printStackTrace();
		} catch (ClassNotFoundException e) {
			System.err.println("Database supporter could not be constructed.");
			e.printStackTrace();
		}
		List<WikiIDTitlePair> inputList = s.getArticlesLimitOffset(limitOrAll, offset);
		try {
			s.closeDBConnection();
		} catch (SQLException e) {
			System.err.println("Connection could not be closed.");
			e.printStackTrace();
		}

		ThreadGroup group = new ThreadGroup("wikiParsing");

		// split the whole array into parts
		ArrayList[] splittedWikiIDTitleArray = s.splitIntoArray(numberOfAvailableProcessors * multiplicator, inputList);
		inputList = null;

		// declare threadpool
		Integer listLenght = splittedWikiIDTitleArray.length;
		Integer processorMin = numberOfAvailableProcessors;
		Integer processorMax = numberOfAvailableProcessors;
		long keepAliveTime = 1;
		final LinkedBlockingQueue workQueue = new LinkedBlockingQueue();
		ThreadPoolExecutor poolExecutor = new ThreadPoolExecutor(processorMin, processorMax, keepAliveTime,
				TimeUnit.SECONDS, workQueue);
		poolExecutor.setCorePoolSize(numberOfAvailableProcessors);

		// start the threads
		for (Integer i = 0; i < splittedWikiIDTitleArray.length; i++) {
			newList = (ArrayList<WikiIDTitlePair>) splittedWikiIDTitleArray[i].clone();
			poolExecutor.execute(new PreMalletParallelization(newList, prop, group, "threadpart-" + new Integer(i + 1)
					+ "of" + listLenght));
			newList = null;
		}
		splittedWikiIDTitleArray = null;

		// wait till the shutdown is finished, otherwise the program tries to
		// join the unfinished outputs
		poolExecutor.shutdown();
		while (!poolExecutor.awaitTermination(30, TimeUnit.SECONDS)) {
			// do nothing
		}
		poolExecutor = null;
	}

	public void start() {

		try {
			Integer limit, offset, old_id, numberOfAvailableProcesssors, multiplicator;

			PreMalletAction_EntryPointForParallelisation p = new PreMalletAction_EntryPointForParallelisation(prop);

			String functionName = "parsing of wiki-articles incl joining the outputs";
			p.stopWatch.startStopping(functionName);

			// read properties

			try {
				numberOfAvailableProcesssors = Integer.valueOf((String) prop.get("Wiki_numberOfParallelThreads"));
			} catch (NumberFormatException e2) {
				numberOfAvailableProcesssors = 1;
			}

			try {
				limit = Integer.valueOf(prop.getProperty("Wiki_limit"));
				// System.out.println("Limit is " + limit);
			} catch (NumberFormatException e1) {
				limit = 0; // bedeutet alles, kein Fehler
			}
			try {
				offset = Integer.valueOf(prop.getProperty("Wiki_offset"));
			} catch (NumberFormatException e1) {
				offset = 0;
			}

			try {
				multiplicator = Integer.valueOf(prop.getProperty("Wiki_multiplicatorForNumberOfThreads"));

			} catch (NumberFormatException e) {
				multiplicator = 2;
			}

			try {
				old_id = Integer.valueOf(prop.getProperty("Wiki_old_id"));
			} catch (NumberFormatException e) {
				old_id = 0;
			}

			// //mainpart
			// für Testzwecke, kann zusammengeschrupft oder mit extra Option
			// gemacht werden
			if (old_id > 0) {
				p.startOnlyOneParsing(old_id);
			} else {
				p.startWithOffsetNew(limit, numberOfAvailableProcesssors, offset, multiplicator);
			}

			p.joinTheOutputsAndDeleteTempFilesInTempFolder();
			p.stopWatch.stopStoppingAndDoOutputToConsole(functionName);

		} catch (InterruptedException e) {
			System.err.println("Prehelper:");
			e.printStackTrace();
		} catch (SQLException e) {
			System.err.println("Prehelper:");
			e.printStackTrace();
		} catch (InstantiationException e) {
			System.err.println("Prehelper:");
			e.printStackTrace();
		} catch (IllegalAccessException e) {
			System.err.println("Prehelper:");
			e.printStackTrace();
		} catch (ClassNotFoundException e) {
			System.err.println("Prehelper:");
			e.printStackTrace();
		}
	}

	private static Properties forLocalExcetution() throws IOException {

		Properties prop;
		String fileName = "src/test/resources/localwikiconfig.ini";

		File f = new File(fileName);
		if (f.exists()) {
			prop = new Properties();
			// prop.load(this.getClass().getResourceAsStream("/config.ini"));

			FileInputStream fis = new FileInputStream(fileName);

			prop.load(fis);

			return prop;

		} else {
			System.err.print(f.getAbsolutePath() + "\n");
			throw new FileNotFoundException(f + "not found.");
		}

	}

	public static void main(String[] args) {

		try {
			PreMalletAction_EntryPointForParallelisation p = new PreMalletAction_EntryPointForParallelisation(
					forLocalExcetution());
			p.start();
		} catch (IOException e) {
			e.printStackTrace();
		}

	}

}
