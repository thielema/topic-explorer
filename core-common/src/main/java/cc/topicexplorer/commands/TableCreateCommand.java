package cc.topicexplorer.commands;

import java.sql.SQLException;

import org.apache.log4j.Logger;

import cc.commandmanager.core.Context;

public abstract class TableCreateCommand extends TableCommand {

	private static final Logger logger = Logger.getLogger(TableCreateCommand.class);

	@Override
	public void tableExecute(Context context) {
		dropTable();
		logger.info("Dropped table " + this.tableName);
		createTable();
		logger.info("Created table " + this.tableName);
	}

	public abstract void createTable();

	public void dropTable() {
		try {
			database.dropTable(this.tableName);
		} catch (SQLException e) {
			if (!exceptionIsTolerated(e.getErrorCode())) {
				logger.error("dropTable: Cannot drop table " + this.tableName);
				throw new RuntimeException(e);
			}
		}
	}

	private boolean exceptionIsTolerated(int errorCode) {
		return errorCode == 1091;
	}

}
