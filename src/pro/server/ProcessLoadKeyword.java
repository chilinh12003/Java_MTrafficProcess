package pro.server;

import java.util.Hashtable;
import java.util.Iterator;
import java.util.Vector;

import uti.utility.MyLogger;
import uti.utility.MyText;

/**
 * Process load keyword trong database
 * 
 * @author Administrator
 * 
 */
public class ProcessLoadKeyword extends Thread
{
	MyLogger mLog = new MyLogger(LocalConfig.LogConfigPath,this.getClass().toString());

	private Hashtable<?, ?> keywords;
	// Là 1 cắp ServiceID và keyword chó định dang là: ServiceID@Keyword.
	// Dữ liệu này được lấy trong table Keyword_Config.

	public Vector<?> vtKeyword;
	public boolean isLoaded = false;

	// Kiếm tra cặp ServiceID@Keyword đã được cấu hình hay chưa.
	public Keyword getKeyword(String keyword, String serviceid)
	{
		Keyword retobj = new Keyword();

		retobj.setClassname(LocalConfig.INV_CLASS);
		retobj.setKeyword(LocalConfig.INV_KEYWORD);
		retobj.setServiceid(serviceid);

		String keytosearch = serviceid + "@" + keyword;

		keytosearch = keytosearch.toUpperCase();
		String strkey = LocalConfig.INV_KEYWORD;

		// Kiếm tra xem cắp ServiceID@Keyword đã được cấu hình trong DB (Table
		// keyword_Config) hay chưa
		for (Iterator<?> it = vtKeyword.iterator(); it.hasNext();)
		{
			String currLabel = (String) it.next();

			if (keytosearch.startsWith(currLabel))
			{
				strkey = currLabel;
				retobj = (Keyword) keywords.get(strkey);
				return retobj;
			}
		}
		return retobj;
	}

	public Keyword getKeywordInvalid(String keyword, String serviceid)
	{
		Keyword retobj = new Keyword();
		String newkeyword = MyText.replaceWhiteLetter(keyword);
		retobj.setClassname(LocalConfig.INV_CLASS);
		retobj.setKeyword(LocalConfig.INV_KEYWORD);
		retobj.setServiceid(serviceid);

		String keytosearch = serviceid + "@" + newkeyword;
		keytosearch = keytosearch.toUpperCase();
		String strkey = LocalConfig.INV_KEYWORD;
		for (Iterator<?> it = vtKeyword.iterator(); it.hasNext();)
		{
			String currLabel = (String) it.next();
			if (keytosearch.startsWith(currLabel))
			{
				strkey = currLabel;
				retobj = (Keyword) keywords.get(strkey);
				return retobj;

			}
		}
		return retobj;
	}

	public Keyword getKeywordInvalidLast(String keyword, String serviceid)
	{
		Keyword retobj = new Keyword();
		String newkeyword = MyText.replaceWhiteLetter(keyword);

		newkeyword = newkeyword.replace(".", "");

		newkeyword = newkeyword.replace(" ", "");

		retobj.setClassname(LocalConfig.INV_CLASS);
		retobj.setKeyword(LocalConfig.INV_KEYWORD);
		retobj.setServiceid(serviceid);

		String keytosearch = serviceid + "@" + newkeyword;
		keytosearch = keytosearch.toUpperCase();
		String strkey = LocalConfig.INV_KEYWORD;
		for (Iterator<?> it = vtKeyword.iterator(); it.hasNext();)
		{
			String currLabel = (String) it.next();
			if (keytosearch.startsWith(currLabel))
			{
				strkey = currLabel;
				retobj = (Keyword) keywords.get(strkey);
				return retobj;

			}
		}
		return retobj;
	}

	public void run()
	{

		mLog.log.info("LoadConfig - Start");
		while (Program.processData)
		{
			try
			{
				keywords = Keyword.retrieveKeyword();
				isLoaded = true;
				try
				{
					sleep(1000 * 60);
				}
				catch (InterruptedException ex3)
				{
					mLog.log.error(ex3);
				}
			}
			catch (Exception ex3)
			{
				mLog.log.error("Loi khi doc cau hinh:" + ex3.toString(), ex3);
			}

		}

	}

}
