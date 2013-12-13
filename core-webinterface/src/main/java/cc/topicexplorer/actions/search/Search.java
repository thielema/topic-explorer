package cc.topicexplorer.actions.search;

import java.io.PrintWriter;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;

import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import cc.topicexplorer.database.Database;
import cc.topicexplorer.database.SelectMap;

public class Search {
	SelectMap searchMap;
	Database database;
	PrintWriter outWriter;
	String searchWord;
	int limit, numberOfTopics;

	public Search(String searchWord, Database db, PrintWriter out, int limit, int numberOfTopics) {
		searchMap = new SelectMap();
		searchMap.select.add("DOCUMENT.DOCUMENT_ID");
		searchMap.from.add("DOCUMENT");
		searchMap.limit = limit;

		setDatabase(db);
		setServletWriter(out);
		setSearchWord(searchWord);
		setLimit(limit);
		setNumberOfTopics(numberOfTopics);
	}

	private void setSearchWord(String searchWord) {
		this.searchWord = searchWord;
	}

	public String getSearchWord() {
		return this.searchWord;
	}

	public void setDatabase(Database database) {
		this.database = database;
	}

	public void setServletWriter(PrintWriter servletWriter) {
		this.outWriter = servletWriter;
	}

	public void setLimit(Integer limit) {
		this.limit = limit;
	}

	public void addSearchColumn(String documentColumn, String documentColumnName) {
		searchMap.select.add(documentColumn + " as " + documentColumnName);
	}

	public void addWhereClause(String whereClause) {
		searchMap.where.add(whereClause);
	}

	public void setNumberOfTopics(Integer numberOfTopics) {
		this.numberOfTopics = numberOfTopics;
	}

	private Integer getNumberOfTopics() {
		return this.numberOfTopics;
	}

	public void executeQuery() {
		JSONArray topTopic = new JSONArray();
		JSONObject doc = new JSONObject();
		JSONObject docs = new JSONObject();
		JSONObject all = new JSONObject();

		ArrayList<String> docColumnList = searchMap.getCleanColumnNames();
		String docId;
		System.out.println(searchMap.getSQLString());
		try {

			ResultSet mainQueryRS = database.executeQuery(searchMap.getSQLString());
			while (mainQueryRS.next()) {
				docId = mainQueryRS.getString("DOCUMENT_ID");
				for (int i = 0; i < docColumnList.size(); i++) {
					doc.put(docColumnList.get(i), mainQueryRS.getString(docColumnList.get(i)));
				}
				ResultSet bestTopicsRS = database.executeQuery("SELECT TOPIC_ID FROM DOCUMENT_TOPIC WHERE TOPIC_ID < "
						+ getNumberOfTopics().toString() + " AND DOCUMENT_ID= " + docId
						+ " ORDER BY PR_TOPIC_GIVEN_DOCUMENT DESC LIMIT 4");
				while (bestTopicsRS.next()) {
					topTopic.add(bestTopicsRS.getInt("TOPIC_ID"));
				}
				doc.put("TOP_TOPIC", topTopic);
				docs.put(docId, doc);
				topTopic.clear();
			}
			all.put("DOCUMENT", docs);
		} catch (SQLException e) {
			e.printStackTrace();
		}
		outWriter.print(all.toString());

	}
}