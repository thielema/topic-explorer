package cc.topicexplorer.actions.autocomplete;

import java.io.PrintWriter;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;

import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import cc.topicexplorer.database.Database;
import cc.topicexplorer.database.SelectMap;
import cc.topicexplorer.utils.MySQLEncoder;

public class Autocomplete {
	SelectMap autocompleteMap;
	Database database;
	PrintWriter outWriter;
	int numberOfTopics;

	public Autocomplete(String searchWord, Database db, PrintWriter out, int numberOfTopics) {
		MySQLEncoder me = new MySQLEncoder();
		
		autocompleteMap = new SelectMap();
		autocompleteMap.select.add("TERM.TERM_ID");
		autocompleteMap.select.add("TERM.TERM_NAME");
		autocompleteMap.select.add("TERM_TOPIC.TOPIC_ID");
		autocompleteMap.select.add("TERM_TOPIC.PR_TOPIC_GIVEN_TERM");
		autocompleteMap.from.add("TERM");
		autocompleteMap.from.add("TERM_TOPIC");
		autocompleteMap.where.add("TERM_TOPIC.TERM_ID = TERM.TERM_ID");
		autocompleteMap.where.add("UPPER(TERM.TERM_NAME) like '" + me.encode(searchWord).toUpperCase() + "%'");
//		autocompleteMap.where.add("TERM_TOPIC.TOPIC_ID < " + numberOfTopics);
		autocompleteMap.orderBy.add("TERM.TERM_ID");
		autocompleteMap.orderBy.add("TERM_TOPIC.PR_TOPIC_GIVEN_TERM DESC");

		setDatabase(db);
		setServletWriter(out);
	}

	public void setDatabase(Database database) {
		this.database = database;
	}

	public void setServletWriter(PrintWriter servletWriter) {
		this.outWriter = servletWriter;
	}

	public void addAutocompleteColumn(String documentColumn, String documentColumnName) {
		autocompleteMap.select.add(documentColumn + " as " + documentColumnName);
	}

	public String getQueryForExecute() {
		return this.autocompleteMap.getSQLString();
	}

	public void executeQuery() throws SQLException {
		JSONArray topicList = new JSONArray();
		JSONArray all = new JSONArray();
		JSONObject term = new JSONObject();

		ArrayList<String> fieldList = autocompleteMap.getCleanColumnNames();
		fieldList.remove("TOPIC_ID");
		fieldList.remove("PR_TOPIC_GIVEN_TERM");

		ResultSet autocompleteQueryRS = database.executeQuery(autocompleteMap.getSQLString());
		int termId = -1;
		while (autocompleteQueryRS.next()) {
			if (termId != autocompleteQueryRS.getInt("TERM_ID")) {
				if (term.size() > 0) {
					term.put("TOP_TOPIC", topicList);
					all.add(term);
					term.clear();
					topicList.clear();
				}
				for (int i = 0; i < fieldList.size(); i++) {
					term.put(fieldList.get(i), autocompleteQueryRS.getString(fieldList.get(i)));
				}
				termId = autocompleteQueryRS.getInt("TERM_ID");
			}
			topicList.add(autocompleteQueryRS.getInt("TOPIC_ID"));
		}
		autocompleteQueryRS.close();
		if (term.size() > 0) {
			term.put("TOP_TOPIC", topicList);
			all.add(term);
		}
		this.outWriter.print(all.toString());
	}
}