package wikiParser;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
//import junit.framework.Assert;


import wikiParser.Database;
import wikiParser.WikiIDTitlePair;

public class Supporter
{

	private Properties prop;
	private Database db;

	private boolean debug = false;
	
	public Supporter(Properties prop)
	{
		this.prop = prop;
		try {
			init();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public Supporter(Database db)
	{
		this.db = db;
		this.prop = db.getProperties();
	}

	public Supporter(Properties properties, Boolean otherDBOrAlsoTargetDB)
	{
		this.prop = properties;
		try
		{
			this.init(otherDBOrAlsoTargetDB);
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
	}

	public Database getDatabase()
	{
		return db;
	}

	public void printIntoFile(String txt, String filename)
	{
		try
		{
			File file = new File(filename);
			BufferedWriter bwCSV = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(file), "UTF-8"));

			String textnew = new String(txt);

			bwCSV.append(textnew);
			bwCSV.flush();
			bwCSV.close();
		}
		catch (Exception e)
		{
			System.err.println("Fehler printIntoFile");
		}

	}

	private void init() throws Exception
	{
		db = new Database(prop);
	}

	private void init(Boolean otherDB) throws Exception
	{
		db = new Database(prop, otherDB);
	}
	
	public void closeDBConnection()
	{
			try
			{
				db.shutdownDB();
			}
			catch (SQLException e)
			{
				e.printStackTrace();
			}
	}

//	public WikiIDTitlePair getOldIdAndWikiTitleFromWikiTitleFromDatabase(String wikiTitle) throws SQLException, Exception
//	{
//		// whitespaces in the title must be replaced
//		if (wikiTitle.indexOf(" ") != -1)
//		{
//			wikiTitle = wikiTitle.trim();
//			wikiTitle = wikiTitle.replaceAll(" ", "_");
//		}
//
//		int rev_text_id = -1;
//		int page_id = -1;
//
//		String sql = " SELECT p.page_id " + " FROM page p  " + " WHERE lower(page_title) = '" + wikiTitle.toLowerCase() + "' ; ";
//
//		ResultSet rs = db.executeQuery(sql);
//
//		// only 1 result is needed, if not exception
//		if (rs.next())
//		{
//			page_id = Integer.valueOf(rs.getString(1));
//		}
//		else
//		{
//			throw new NullPointerException("The articlename cannot be resolved.");
//		}
//
//		rs.close();
//
//		// id for wikitext , max(rev_text_id) is the last revision
//		sql = " SELECT max(rev_text_id) " + " FROM revision " + " WHERE  rev_page = " + page_id + " ;";
//
//		rs = db.executeQuery(sql);
//
//		// only 1 result is needed, if not exception
//		if (rs.next())
//		{
//			rev_text_id = Integer.valueOf(rs.getString(1));
//		}
//		else
//		{
//			throw new NullPointerException("The rev_text_id of the articlename cannot be resolved.");
//		}
//		rs.close();
//
//		return new WikiIDTitlePair(rev_text_id, wikiTitle);
//	}
	
	public WikiIDTitlePair getOldIdAndWikiTitleFromWikiPageIdFromDatabase(Integer old_id) throws SQLException, Exception
	{
		
		String wikiTitle;
		Integer rev_page = -1 ;
		
		
		String sql = " SELECT rev_page FROM revision " + " WHERE  rev_text_id = " + old_id + " ;";
//		System.out.println(sql);
		ResultSet rs = db.executeQuery(sql);
		
		if (rs.next())
		{
			rev_page = Integer.valueOf(rs.getString(1));
		}
		else
		{
			throw new NullPointerException("The articlename cannot be resolved.");
		}
		
		rs.close();
		
		sql = " SELECT page_title FROM page p WHERE page_id = " + rev_page + ";";
		System.out.println(sql);
		rs = db.executeQuery(sql);

		// only 1 result is needed, if not exception
		if (rs.next())
		{
			wikiTitle = rs.getString(1);
		}
		else
		{
			throw new NullPointerException("The articlename cannot be resolved.");
		}

		rs.close();

		return new WikiIDTitlePair(old_id, wikiTitle);
	}
	

	public String getWikitextFromFromWikiTitleFromDatabase(String wikiTitle) throws SQLException, Exception
	{
		// whitespaces in the title must be replaced
		if (wikiTitle.indexOf(" ") != -1)
		{
			wikiTitle = wikiTitle.trim();
			wikiTitle = wikiTitle.replaceAll(" ", "_");
		}

		Integer rev_text_id_old_id = -1;
		String wikitxt = "";

		String sql = " SELECT max_rev_text_id " + " FROM page " + " INNER JOIN (" + " 				SELECT rev_page, max( rev_text_id ) AS max_rev_text_id"
				+ "				FROM revision " + "				GROUP BY rev_page " + "			   )rev ON " + " page.page_id = rev.rev_page " + " WHERE page_title LIKE '" + wikiTitle
				+ "';";

		// String sql = " SELECT p.page_id " + " FROM page p  " +
		// " WHERE lower(page_title) = '" + wikiTitle.toLowerCase() + "' ; ";

		if (debug)
		{
			System.out.println(sql);
		}

		ResultSet rs = db.executeQuery(sql);

		// only 1 result is needed, if not exception
		if (rs.next())
		{
			rev_text_id_old_id = Integer.valueOf(rs.getString(1));
		}
		else
		{
			throw new NullPointerException("The articlename cannot be resolved.");
		}

		rs.close();

		// query for the wikitext
		sql = " SELECT CONVERT(CAST(old_text as binary) USING utf8) " + " FROM text " + " WHERE old_id = " + rev_text_id_old_id + "; ";

		if (debug)
		{
			System.out.println(sql);
		}

		rs = db.executeQuery(sql);

		if (rs.next())
		{
			wikitxt = rs.getString(1);
			System.out.println(wikiTitle + " , " + rev_text_id_old_id);
		}
		else
		{
			throw new NullPointerException("The wikitext cannot be found. Wikititle:" + wikiTitle);
		}

		// rs.close();

		// test, weil die Abfrage einen Wert liefert und dieser trotzdem nicht
		// gespeichert wird und null bringt
		if (wikitxt == null)
		{
			try
			{
				ResultSet rs2 = db.executeQuery(sql);
				if (rs2.next())
				{
					wikitxt = rs2.getString(1);

					if (wikitxt == null)
					{
						throw new Exception(
								wikiTitle
										+ " : "
										+ rev_text_id_old_id
										+ " , wikitext == null , entweder die Rekursion mit der Weiterleitung von Wikiartikeln will nicht funktionieren, oder es liegt ein anderer Fehler vor."
										+ this.getClass());
					}
				}
				rs2.close();
			}
			catch (Exception e)
			{
				System.err.println(e.getMessage());
				System.err.println(sql);
				// e.printStackTrace();
				return null;
			}

		}

		return wikitxt;
	}

	/**
	 * NUR KOPIERT ...
	 */
	public String getWikiTextOnlyWithID(Integer id) throws SQLException, Exception
	{
		String wikitxt = "";

		// query for the wikitext
		String sql = " SELECT CONVERT(CAST(old_text as binary) USING utf8) " + " FROM text " + " WHERE old_id = " + id + ";";

		ResultSet rs = db.executeQuery(sql);

		if (rs.next())
		{
			wikitxt = rs.getString(1);
//			System.out.println(id);
		}
		else
		{
			throw new NullPointerException("The wikitext of text.old_id cannot be found. Wikititle:" + id);
		}

		// rs.close();

		// test, weil die Abfrage einen Wert liefert und dieser trotzdem nicht
		// gespeichert wird und null bringt
		if (wikitxt == null)
		{

			rs = db.executeQuery("SELECT old_text  FROM text  WHERE old_id = " + id);

			if (rs.next())
			{
				byte[] bytes = (byte[]) rs.getObject(1);

				try
				{

					wikitxt = new String(bytes, "UTF-8");

				}
				catch (UnsupportedEncodingException e)
				{
					wikitxt = wikitxt + " " + e.getMessage(); // as errorcode
				}
			}
		}

		// immernoch null
		if (wikitxt == null)
			throw new Exception(id + " , wikitext == null , " + sql + " " + this.getClass() + "\n");

		rs.close();
		
		rs = null;
		sql = null;
		
		return wikitxt;
	}

	
//	@SuppressWarnings( "unchecked" )
	public ArrayList <WikiIDTitlePair>[] splitIntoArray(Integer numberofSplittedThreads, List<WikiIDTitlePair> inputList)
	{
		
		ArrayList [] outputList =	new ArrayList[numberofSplittedThreads];
		
		Integer partBeginn = 0 ;
		Integer partEnd = 0;
		Integer numberOfArticles = inputList.size();
		
		for (int i = 0; i < numberofSplittedThreads; i++)
			{
				if (i == 0)
				{
					partBeginn =0;
					partEnd = numberOfArticles/ numberofSplittedThreads;
				}
				else if ((i > 0) && (i < numberofSplittedThreads - 1))
				{
					partBeginn = partEnd;
					partEnd = numberOfArticles / numberofSplittedThreads * (i + 1);
				}
				else if (i == numberofSplittedThreads - 1)
				{
					partBeginn = partEnd;
					partEnd = numberOfArticles ;
				}
//				System.out.println(partBeginn + " - "  + partEnd);
				outputList[i] = new ArrayList<WikiIDTitlePair>(inputList.subList(partBeginn,partEnd));
			}
		return outputList;
	}

	public List<WikiIDTitlePair> getArticlesLimitOffset(Integer intLimit, Integer offset)
	{

		List<WikiIDTitlePair> list = new ArrayList<WikiIDTitlePair>();
		try
		{
			String sql = " SELECT page_latest , page_title " 
					   + " FROM page "
					   + " WHERE page_is_redirect = 0 AND page_namespace = 0 ";
			
			try
			{
				if (prop.getProperty("Wiki_sqlAddBoolean").equals(1)){
					
					String sqlAdd = prop.getProperty("Wiki_sqlAddToWhere");
			
					if (sqlAdd.length() >0)
						sql = sql + sqlAdd;
				}
			}
			catch (Exception e)
			{
				System.err.println("getArticles... cannot add 'sqlAddToWhere' from wiki_properties to sql-string. Proceed without it.");
			}
			
			if (intLimit > 0 && offset >= 0)
			{
				sql = sql + " ORDER BY page_latest " + " LIMIT " + offset + " , " + intLimit + " ; ";
			}
//			 System.out.println(sql);
			
			ResultSet rs = db.executeQuery(sql);

			while (rs.next())
			{
				list.add(new WikiIDTitlePair(Integer.valueOf(rs.getString(1)), rs.getString(2)));
			}

		}
		catch (Exception e)
		{
			System.err.println("Supporter.getArticlesLimitOffset failure." );
			e.printStackTrace();
		}
		return list;
	}
}