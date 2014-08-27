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

import java.math.BigDecimal;
import java.sql.SQLException;

import uti.utility.MyLogger;
import dat.gateway.sms_receive_log;

public class ExecuteInsertReceiveLog extends Thread
{

	public static MyLogger mLog = new MyLogger(LocalConfig.LogConfigPath, ExecuteInsertReceiveLog.class.toString());
	public static sms_receive_log mReceiveLog = null;

	static MsgQueue queueLog = null;

	BigDecimal AM = new BigDecimal(-1);

	public ExecuteInsertReceiveLog(MsgQueue queueLog)
	{
		try
		{
			mReceiveLog = new sms_receive_log(LocalConfig.mDBConfig_MySQL);
			ExecuteInsertReceiveLog.queueLog = queueLog;
		}
		catch (Exception ex)
		{

		}
	}

	public static void add2queueReceiveLog(MsgObject msgObject)
	{
		queueLog.add(msgObject);
	}

	public void run()
	{

		MsgObject msgObject = null;

		BigDecimal returnId = AM;

		try
		{
			sleep(1000);
		}
		catch (InterruptedException ex1)
		{
		}

		try
		{
			while (Program.processData)
			{
				returnId = AM;
				try
				{
					msgObject = (MsgObject) queueLog.remove();

					returnId = processQueueMsg(msgObject);
					if (returnId.equals(AM))
					{
						queueLog.add(msgObject);
					}

				}
				catch (Exception ex)
				{
					mLog.log.error(ex.toString(), ex);
					queueLog.add(msgObject);
				}
				sleep(50);
			}
		}
		catch (Exception ex)
		{
			mLog.log.error("Error: ExecuteAddReceivelog.run :" + ex.toString(), ex);
		}
	}

	private BigDecimal processQueueMsg(MsgObject msgObject)
	{
		BigDecimal returnid = add2SMSReceiveLog(msgObject);
		return returnid;
	}

	private static BigDecimal add2SMSReceiveLog(MsgObject msgObject)
	{

		mLog.log.info("add2SMSReceiveLog:" + msgObject.getUserid() + "@" + msgObject.getUsertext());

		try
		{

			mReceiveLog = new sms_receive_log(LocalConfig.mDBConfig_MySQL);

			if (!mReceiveLog.Insert(msgObject.getRequestid().toString(), msgObject.getUserid(), msgObject.getServiceid(), msgObject.getMobileoperator(),
					msgObject.getKeyword(), msgObject.getUsertext(), msgObject.getTTimes(), "0", msgObject.getCpid(), msgObject.getChannelType()))
			{
				mLog.log.info("add2ReceiveLog:" + msgObject.getUserid() + ":" + msgObject.getUsertext() + ":statement.executeUpdate failed");
				return new BigDecimal(-1);
			}

			return msgObject.getRequestid();
		}
		catch (SQLException e)
		{
			mLog.log.error("add2ReceiveLog:" + msgObject.getUserid() + ":" + msgObject.getUsertext() + ":Error add row from sms receive log:" + e.toString(), e);

			return new BigDecimal(-1);
		}
		catch (Exception e)
		{
			mLog.log.error("add2ReceiveLog:" + msgObject.getUserid() + ":" + msgObject.getUsertext() + ":Error add row from sms receive log:" + e.toString(), e);
			return new BigDecimal(-1);
		}

	}

}
