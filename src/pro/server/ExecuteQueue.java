package pro.server;

import java.math.BigDecimal;

import uti.utility.MyLogger;
import uti.utility.MyText;

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
public class ExecuteQueue extends Thread
{

	MyLogger mLog = new MyLogger(LocalConfig.LogConfigPath,this.getClass().toString());

	int threadID = 0;
	MsgQueue queue = null;
	MsgQueue queueLog = null;

	BigDecimal AM = new BigDecimal(-1);

	public ExecuteQueue(MsgQueue queue, MsgQueue queueLog, int threadID)
	{
		this.queue = queue;
		this.queueLog = queueLog;
		this.threadID = threadID;
	}

	public void run()
	{

		MsgObject msgObject = null;
		String serviceId = "";
		String info = "";
		Keyword keyword = null;
		String process_result = "";

		try
		{
			sleep(1500);
		}
		catch (InterruptedException ex1)
		{
		}

		while (Program.processData)
		{
			process_result = "";
			try
			{
				// Lấy 1 item trong MO Queue và xóa nó đi
				msgObject = (MsgObject) queue.remove();
				serviceId = msgObject.getServiceid();
				info = msgObject.getUsertext();

				keyword = Program.mLoadKeyword.getKeyword(info, serviceId);

				if (LocalConfig.INV_KEYWORD.equalsIgnoreCase(keyword.getKeyword()))
				{
					// Nếu keyword không nằm trong list keyword cho phép

					keyword = Program.mLoadKeyword.getKeywordInvalid(info, serviceId);

					if (!LocalConfig.INV_KEYWORD.equalsIgnoreCase(keyword.getKeyword()))
					{
						String newinfo = MyText.replaceWhiteLetter(info);
						msgObject.setUsertext(newinfo);
					}
					else
					{
						keyword = Program.mLoadKeyword.getKeywordInvalidLast(info, serviceId);

						if (!LocalConfig.INV_KEYWORD.equalsIgnoreCase(keyword.getKeyword()))
						{
							String newinfo = MyText.replaceWhiteLetter(info);
							newinfo = newinfo.replace(".", "");
							newinfo = newinfo.replace(" ", "");

							msgObject.setUsertext(newinfo);
						}

					}
				}

				mLog.log.info(Common.GetStringLog("Check Exist Keyword", "Keyword:" + keyword.getKeyword(), msgObject));

				msgObject.setKeyword(keyword.getKeyword());
				msgObject.setCpid(keyword.getCpid());
				process_result = processQueueMsg(msgObject, keyword);

				msgObject.setMsgNotes(process_result);

				Program.incrementAndGet_process(msgObject.getMobileoperator());

				queueLog.add(new MsgObject(msgObject.getId(), serviceId, msgObject.getUserid(), keyword.getKeyword(), info, msgObject.getRequestid(), msgObject
						.getTTimes(), msgObject.getMobileoperator(), 0, 0, msgObject.getCpid(), msgObject.getMsgnotes()));

			}
			catch (Exception ex)
			{
				mLog.log.error(Common.GetStringLog(msgObject), ex);
				queue.add(msgObject);
			}

		}

	}

	private String processQueueMsg(MsgObject msgObject, Keyword keyword)
	{
		ContentAbstract delegate = null;
		try
		{
			String classname = "";
			if (keyword.getClassname().startsWith("~"))
			{
				classname = "MyProcess.InvalidProcess";

				// Cau hinh tra loi luon
				// $msgtype$info
				String sInfo = keyword.getClassname();
				String[] arrInfo = sInfo.split("~");
				String mtreply = "";
				int msgtype = 2;
				if (arrInfo.length > 2)
				{
					mtreply = arrInfo[2];
					if (LocalConfig.MT_CHARGING.equals(arrInfo[1]))
					{
						msgtype = 1;
					}
					else if (LocalConfig.MT_PUSH.equals(arrInfo[1]))
					{
						msgtype = 3;
					}
					else if (LocalConfig.MT_REFUND_SYNTAX.equals(arrInfo[1]))
					{
						msgtype = 21;
					}
					else if (LocalConfig.MT_REFUND_CONTENT.equals(arrInfo[1]))
					{
						msgtype = 22;
					}
					else
					{
						msgtype = 2;
					}

				}
				else
				{
					mtreply = arrInfo[1];
					msgtype = 2;
				}
				msgObject.setUsertext(mtreply);
				msgObject.setMsgtype(msgtype);
			}
			else
			{
				classname = keyword.getClassname();
			}

			// Ghi log xem class nào xử lý MO này
			mLog.log.info(Common.GetStringLog("Call Class Process Keyword", "ClassName:" + classname, msgObject));

			// Khởi tạo đối tượng process xử lý keyword
			Class<?> delegateClass = Class.forName(classname);
			Object delegateObject = delegateClass.newInstance();
			delegate = (ContentAbstract) delegateObject;

			delegate.start(msgObject, keyword);
			return "OK";

		}
		catch (Exception e)
		{
			mLog.log.error(Common.GetStringLog(msgObject), e);

			return msgObject.getUserid() + ":" + e.toString();
		}

	}

}
