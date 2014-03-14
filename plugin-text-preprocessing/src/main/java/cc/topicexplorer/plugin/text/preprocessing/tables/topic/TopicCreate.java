package cc.topicexplorer.plugin.text.preprocessing.tables.topic;

import java.sql.SQLException;

import cc.topicexplorer.commands.TableCreateCommand;

/*
 * angefangen von Mattes weiterverarbeitet von Gert Kommaersetzung, Pfadangabe
 * eingefügt, Tabellenname mit Jooq verknüpft
 * 
 */
public class TopicCreate extends TableCreateCommand {

	@Override
	public void createTable() {
		try {
			this.database.executeUpdateQuery("ALTER TABLE " + this.tableName
					+ " ADD COLUMN TOPIC.TEXT$TOPIC_LABEL VARCHAR(255) COLLATE utf8_bin DEFAULT ''");
		} catch (SQLException e) {
			logger.error("Column TOPIC.TEXT$TOPIC_LABEL could not be added to table " + this.tableName);
			throw new RuntimeException(e);
		}
	}

	private void dropColumns() {
		try {
			this.database.executeUpdateQuery("ALTER TABLE " + this.tableName + " DROP COLUMN TOPIC.TEXT$TOPIC_LABEL");
		} catch (SQLException e) {
			if (e.getErrorCode() != 1091) { // MySQL Error code for 'Can't DROP
				// ..; check that column/key exists
				logger.error("TopicMetaData.dropColumns: Cannot drop column.");
				throw new RuntimeException(e);
			}
		}
	}

	@Override
	public void dropTable() {
		this.dropColumns();
	}

	@Override
	public void setTableName() {
		this.tableName = "TOPIC";

	}

	@Override
	public void addDependencies() {
		beforeDependencies.add("DocumentTermTopicCreate");
		beforeDependencies.add("TopicCreate");
	}
}
