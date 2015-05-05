package cc.topicexplorer.initcorpus;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Properties;
import java.util.Set;

import org.apache.log4j.Logger;

import com.google.common.collect.Sets;

import cc.commandmanager.core.Command;
import cc.commandmanager.core.Context;


public class RemoveDuplicates implements Command {
	
	private static final Logger logger = Logger.getLogger(RemoveDuplicates.class);
	private String tableName;
	
	@Override
	public void execute(Context context) {
		Properties properties = (Properties) context.get("properties");
		Connection crawlManagerConnection = (Connection) context.get("CrawlManagmentConnection");
		
		String dbName = properties.getProperty("database.DB");
		
		this.tableName = dbName + ".DUPLICATES";	
		
		
		
		try {
			Statement stmt = crawlManagerConnection.createStatement();
			
			stmt.executeUpdate("create table " + this.tableName + " as "
					+ "select URL_MD5, min(DOCUMENT_DATE) mindate, min(DOCUMENT_ID) minid from " + dbName + ".orgTable_meta "
					+ "group by URL_MD5 having count(*) > 1");
			
			stmt.executeUpdate("create index url_idx on " + this.tableName + "(URL_MD5,mindate,minid)");
			
			stmt.executeUpdate("delete " + dbName + ".orgTable_meta, " + dbName + ".orgTable_text from " 
					+ dbName + ".orgTable_meta, " + dbName + ".orgTable_text, " + this.tableName
					+ " where orgTable_meta.DOCUMENT_ID=orgTable_text.DOCUMENT_ID and orgTable_meta.URL_MD5=" + this.tableName + ".URL_MD5 and "
					+ "(orgTable_meta.DOCUMENT_DATE>" + this.tableName + ".mindate or (orgTable_meta.DOCUMENT_DATE=" + this.tableName + ".mindate "
					+ "and orgTable_meta.DOCUMENT_ID>" + this.tableName + ".minid))");
			
			stmt.executeUpdate("drop table " + this.tableName);
			
		} catch (SQLException e) {
			logger.error("Table " + this.tableName + " could not be filled properly.");
			throw new RuntimeException(e);
		}
		
	}

	@Override
	public Set<String> getAfterDependencies() {
		return Sets.newHashSet();
	}

	@Override
	public Set<String> getBeforeDependencies() {
		return Sets.newHashSet("CopyOrgTable");
	}

	@Override
	public Set<String> getOptionalAfterDependencies() {
		return Sets.newHashSet();
	}

	@Override
	public Set<String> getOptionalBeforeDependencies() {
		return Sets.newHashSet();
	}

	

}