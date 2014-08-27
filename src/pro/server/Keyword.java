package pro.server;

/**
 * <p>Title: </p>
 *
 * <p>Description: </p>
 *
 * <p>Copyright: Copyright (c) 2007</p>
 *
 * <p>Company: </p>
 *
 * @author not attributable
 * @version 1.0
 */

import java.sql.SQLException;
import java.util.Hashtable;
import java.util.Vector;

import uti.utility.MyLogger;
import db.define.MyTableModel;

public class Keyword
{
	static MyLogger mLog = new MyLogger(LocalConfig.LogConfigPath, Keyword.class.toString());

	private String serviceid;
	private String keyword;
	private String classname;
	private int cpid = 0;
	private String options;

	public String getKeyword()
	{
		return keyword;
	}

	public String getServiceid()
	{
		return serviceid;
	}

	public String getClassname()
	{
		return classname;
	}

	public String getOptions()
	{
		return options;
	}

	public void setKeyword(String keyword)
	{
		this.keyword = keyword;
	}

	public void setServiceid(String serviceid)
	{
		this.serviceid = serviceid;
	}

	public void setClassname(String classname)
	{
		this.classname = classname;
	}

	public void setOptions(String options)
	{
		this.options = options;
	}

	public static Hashtable<?, Keyword> retrieveKeyword() throws Exception
	{

		dat.gateway.Keyword mKeyword = new dat.gateway.Keyword(LocalConfig.mDBConfig_MySQL);
		Hashtable<String, Keyword> keywords = new Hashtable<String, Keyword>();
		Vector<String> vtkeywords = new Vector<String>();

		try
		{
			MyTableModel mTable = mKeyword.SelectActive();

			for (int i = 0; i < mTable.GetRowCount(); i++)
			{
				Keyword keywordtemp = new Keyword();
				keywordtemp.serviceid = mTable.GetValueAt(i, "service_id").toString();
				keywordtemp.keyword = mTable.GetValueAt(i, "keyword").toString().toUpperCase();
				keywordtemp.classname = mTable.GetValueAt(i, "class_name").toString();

				keywordtemp.options = mTable.GetValueAt(i, "options").toString();

				if (mTable.GetValueAt(i, "cpid") != null)
					keywordtemp.cpid = Integer.parseInt(mTable.GetValueAt(i, "cpid").toString());

				keywords.put(keywordtemp.serviceid + "@" + keywordtemp.keyword, keywordtemp);

				vtkeywords.addElement(keywordtemp.serviceid + "@" + keywordtemp.keyword);
			}

		}
		catch (SQLException ex)
		{
			mLog.log.error("Error retrieveKeyword sql" + ex);
		}
		catch (Exception ex)
		{
			mLog.log.error("Error retrieveKeyword" + ex);
		}

		Program.mLoadKeyword.vtKeyword = vtkeywords;
		return keywords;
	}

	public int getCpid()
	{
		return cpid;
	}

	public void setCpid(int cpid)
	{
		this.cpid = cpid;
	}

}
