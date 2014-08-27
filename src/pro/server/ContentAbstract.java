package pro.server;

import java.sql.SQLException;
import java.util.Calendar;
import java.util.Collection;
import java.util.Iterator;
import java.util.StringTokenizer;
import java.util.Vector;

import uti.utility.MyLogger;
import dat.gateway.ems_send_queue;

/**
 * <p>
 * Title:
 * </p>
 * 
 * <p>
 * Description:
 * </p>
 * 
 * <p>
 * Copyright: Copyright (c) 2007
 * </p>
 * 
 * <p>
 * Company:
 * </p>
 * 
 * @author not attributable
 * @version 1.0
 */
public abstract class ContentAbstract
{
	static MyLogger mLog = new MyLogger(LocalConfig.LogConfigPath, ContentAbstract.class.toString());

	public void start(MsgObject msgObject, Keyword keyword) throws Exception
	{
		try
		{

			Collection<?> messages = getMessages(msgObject, keyword);
			if (messages != null)
			{
				Iterator<?> iter = messages.iterator();
				for (; iter.hasNext();)
				{
					MsgObject msgMT = (MsgObject) iter.next();
					sendMulti(msgMT);
				}
			}
			else
			{
				mLog.log.info(Common.GetStringLog("ContentAbstract", "MT TRA VE LA NULL", msgObject));
			}
		}
		catch (Exception e)
		{
			mLog.log.error(Common.GetStringLog("ContentAbstract", msgObject));
		}

	}

	protected abstract Collection<?> getMessages(MsgObject msgObject, Keyword keyword) throws Exception;

	private static synchronized Collection<String> splitMsg(String arg)
	{
		String[] result = new String[3];
		Vector<String> v = new Vector<String>();
		int segment = 0;

		if (arg.length() <= 160)
		{
			result[0] = arg;
			v.add(result[0]);
			return v;

		}
		else
		{
			segment = 160;
		}

		StringTokenizer tk = new StringTokenizer(arg, " ");
		String temp = "";
		int j = 0;

		int tksize = tk.countTokens();
		int tkcount = 0;
		while (tk.hasMoreElements())
		{
			String token = (String) tk.nextElement();
			tkcount++;
			if (temp.equals(""))
			{
				temp = temp + token;
			}
			else
			{
				temp = temp + " " + token;
			}

			if (temp.length() > segment)
			{
				temp = token;
				j++;
				if ((tkcount == tksize) && (j <= 3))
				{
					result[j] = token;
				}
			}
			else
			{
				result[j] = temp;
			}

			if (j == 3)
			{
				break;
			}
		}

		for (int i = 0; i < result.length; i++)
		{
			if (result[i] != null)
			{
				v.add(result[i]);
			}
		}

		return v;
	}

	private static int sendMulti(MsgObject msgObject)
	{

		if ("".equalsIgnoreCase(msgObject.getUsertext().trim()) || msgObject.getUsertext() == null)
		{
			// Truong hop gui ban tin loi
			mLog.log.debug(Common.GetStringLog("ContentAbstract_SEND_MT", "MT LA NULL", msgObject));
			return 1;

		}

		// int idsendqueue =
		// LocalConfig.getIDdbsendqueue(msgObject.getUserid());
		if (msgObject.getUsertext().length() <= 160)
		{
			msgObject.setContenttype(0);
		}

		if (msgObject.getContenttype() == 0 && msgObject.getUsertext().length() > 160)
		{
			String mtcontent = msgObject.getUsertext();

			Collection<?> listmt = splitMsg(mtcontent);
			Iterator<?> itermt = listmt.iterator();
			int cnttype = msgObject.getContenttype();

			for (int j = 1; itermt.hasNext(); j++)
			{
				String temp = (String) itermt.next();

				msgObject.setUsertext(temp);
				if (j == 1)
				{
					msgObject.setMsgtype(cnttype);
					sendMT(msgObject);
				}
				else
				{
					msgObject.setMsgtype(0);
					sendMT(msgObject);
				}
			}
			return 1;

		}
		else return sendMT(msgObject);

	}

	private static int sendMT(MsgObject msgObject)
	{

		if ("".equalsIgnoreCase(msgObject.getUsertext().trim()) || msgObject.getUsertext() == null)
		{
			// Truong hop gui ban tin loi
			mLog.log.debug(Common.GetStringLog("ContentAbstract_SEND_MT", "MT LA NULL", msgObject));
			return 1;
		}

		try
		{
			ems_send_queue mSendQueue = new ems_send_queue(LocalConfig.mDBConfig_MySQL);

			boolean Result = mSendQueue.Insert(msgObject.getUserid(), msgObject.getServiceid(),
					msgObject.getMobileoperator(), msgObject.getKeyword(), msgObject.getUsertext(),
					msgObject.getReceiveDate(), Calendar.getInstance().getTime(),
					Integer.toString(msgObject.getMsgtype()), msgObject.getRequestid().toString(), "1",
					Integer.toString(msgObject.getContenttype()), Integer.toString(msgObject.getCpid()));

			if (!Result)
			{
				mLog.log.debug(Common.GetStringLog("ContentAbstract_SEND_MT", "KHONG SEND DUOC MT", msgObject));
				return -1;
			}
			mLog.log.debug(Common.GetStringLog("ContentAbstract_SEND_MT", "SEND MT THANH CONG", msgObject));
			return 1;
		}
		catch (SQLException e)
		{
			mLog.log.error(Common.GetStringLog("ContentAbstract_SEND_MT", "KHONG SEND DUOC MT", msgObject), e);
			return -1;
		}
		catch (Exception e)
		{
			mLog.log.error(Common.GetStringLog("ContentAbstract_SEND_MT", "KHONG SEND DUOC MT", msgObject), e);
			return -1;
		}

	}

}
