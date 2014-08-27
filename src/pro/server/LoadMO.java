package pro.server;

import java.math.BigDecimal;
import java.sql.SQLException;
import java.sql.Timestamp;

import uti.utility.MyLogger;
import dat.gateway.sms_receive_queue;
import db.define.MyTableModel;

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
public class LoadMO extends Thread
{

	MyLogger mLog = new MyLogger(LocalConfig.LogConfigPath, LoadMO.class.toString());

	MsgQueue queue = null;
	int processnum = 1;
	int processindex = 1;
	// private Logger logger = null;
	static int TIME_DELAY_LOAD_MO = 100;
	int dbid = 0;

	sms_receive_queue mReceiveQueue;

	public static String[] mobileOperators = { "GPC", "VMS", "VIETTEL", "HTC" };

	public LoadMO()
	{
		try
		{
			mReceiveQueue = new sms_receive_queue(LocalConfig.mDBConfig_MySQL);
		}
		catch (Exception ex)
		{
			mLog.log.error("Error Contructor LoadMO()", ex);
		}
	}

	public LoadMO(MsgQueue queue, int processnum, int processindex)
	{
		try
		{
			this.queue = queue;
			this.processnum = processnum;
			this.processindex = processindex;
			mReceiveQueue = new sms_receive_queue(LocalConfig.mDBConfig_MySQL);
		}
		catch (Exception ex)
		{
			mLog.log.error("Error Contructor LoadMO(MsgQueue,int,int,int)", ex);
		}
	}
	
	public void run()
	{
		MsgObject msgObject = null;
		String serviceId = "";
		String userId = "";
		String info = "";
		Timestamp tTime;
		String operator = "";
		BigDecimal requestId = new BigDecimal(-1);
		long receiveid = 0;
		String dport = "0";
		Integer ChannelTypeID = 1;
		while (Program.getData)
		{
			try
			{
				MyTableModel mTable = mReceiveQueue.SelectMO(processnum, processindex);

				String ListID_Delete = "";
				for (int i = 0; i < mTable.GetRowCount(); i++)
				{
					serviceId = mTable.GetValueAt(i, "SERVICE_ID").toString();

					userId = mTable.GetValueAt(i, "USER_ID").toString();
					info = mTable.GetValueAt(i, "INFO").toString();
					tTime = (Timestamp) mTable.GetValueAt(i, "RECEIVE_DATE");
					operator = (String) mTable.GetValueAt(i, "MOBILE_OPERATOR");
					requestId = new BigDecimal(mTable.GetValueAt(i, "REQUEST_ID").toString());

					receiveid = (Long) mTable.GetValueAt(i, "ID");

					dport = (String) mTable.GetValueAt(i, "DPORT");
					
					ChannelTypeID = Integer.parseInt(mTable.GetValueAt(i, "CHANNEL_TYPE").toString());

					msgObject = new MsgObject(receiveid, serviceId, userId, info, info, requestId, tTime, operator, 0, 0, receiveid, info);
					msgObject.channelType =ChannelTypeID;

					msgObject.setDport(dport);

					try
					{
						if (!ListID_Delete.equals(""))
							ListID_Delete += ",";

						ListID_Delete += "" + receiveid;

						queue.add(msgObject);
						
						Program.incrementAndGet_load(operator);

						mLog.log.info(Common.GetStringLog("LoadMO AddToQueue", "Q:" + serviceId + "[Size:" + queue.getSize() + "]", msgObject));
					}
					catch (Exception ex)
					{
						mLog.log.error("Error While", ex);
					}
				}
				// xóa cac dòng dã xu lý
				if (!ListID_Delete.equals(""))
				{
					mReceiveQueue.DeleteMO(ListID_Delete);
					ListID_Delete = "";
				}
			}
			catch (SQLException ex)
			{
				mLog.log.error("Error run 1", ex);
			}
			catch (Exception ex)
			{
				mLog.log.error("Error run 2", ex);
			}

			try
			{
				sleep(TIME_DELAY_LOAD_MO);
			}
			catch (InterruptedException ex)
			{
				mLog.log.error("Error run 3", ex);
			}
		}

	}

}
