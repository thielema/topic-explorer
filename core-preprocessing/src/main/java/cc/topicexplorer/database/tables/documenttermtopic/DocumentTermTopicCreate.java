package cc.topicexplorer.database.tables.documenttermtopic;

import java.sql.SQLException;

import cc.topicexplorer.commands.TableCreateCommand;

public class DocumentTermTopicCreate extends TableCreateCommand {

	@Override
	public void createTable() {
		try {
			database.executeUpdateQuery(" CREATE TABLE `" + this.tableName + "` ( "
					+ "`DOCUMENT_ID` INT(11) NOT NULL, " + "`POSITION_OF_TOKEN_IN_DOCUMENT` INT(11) NOT NULL, "
					+ "`TERM` varchar(255) COLLATE utf8_bin NOT NULL, "
					+ "`TOKEN` varchar(255) COLLATE utf8_bin NOT NULL, " + "`TOPIC_ID` int(11) NOT NULL, "
					+ "`PROBALITY_TOPIC_GIVEN_DOCUMENT_TERM` FLOAT, "
					+ "`DOCUMENT_TERM_TOPIC_ASSIGNMENT` INT(11) NOT NULL "
					+ ") ENGINE=InnoDB CHARSET=utf8 COLLATE=utf8_bin ;");
		} catch (SQLException e) {
			logger.error("Table " + this.tableName + " could not be created");
			throw new RuntimeException(e);
		}

	}

	@Override
	public void setTableName() {
		tableName = "DOCUMENT_TERM_TOPIC";
	}
}
