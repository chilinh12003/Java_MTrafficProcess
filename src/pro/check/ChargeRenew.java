package pro.check;

import java.util.Calendar;

import pro.charge.Charge;
import pro.charge.Charge.ErrorCode;
import pro.define.ChargeThreadObject;
import pro.define.ChargeThreadObject.ThreadStatus;
import pro.server.Common;
import pro.server.Program;
import pro.server.LocalConfig;
import uti.utility.MyCheck;
import uti.utility.MyConfig;
import uti.utility.MyConfig.ChannelType;
import uti.utility.MyLogger;
import dat.service.ChargeLog;
import dat.service.DefineMT.MTType;
import dat.service.MOLog;
import dat.service.ServiceObject;
import dat.sub.Subscriber;
import dat.sub.Subscriber.Status;
import dat.sub.UnSubscriber;
import db.define.MyDataRow;
import db.define.MyTableModel;

/**
 * Thread sẽ bắn tin cho từng dịch vụ
 * 
 * @author Administrator
 * 
 */
public class ChargeRenew extends Thread
{
	MyLogger mLog = new MyLogger(LocalConfig.LogConfigPath,this.getClass().toString());

	public ChargeThreadObject mCTObject = new ChargeThreadObject();

	public ChargeRenew()
	{

	}

	public ChargeRenew(ChargeThreadObject mCTObject)
	{
		this.mCTObject = mCTObject;
	}

	Subscriber mSub = null;
	UnSubscriber mUnSub = null;
	MOLog mMOLog = null;
	ChargeLog mChargeLog = null;

	MyTableModel mTable_SubUpdate = null;
	MyTableModel mTable_ChargeLog = null;
	MyTableModel mTable_MOLog = null;

	public void run()
	{
		if (Program.processData)
		{
			try
			{
				mSub = new Subscriber(LocalConfig.mDBConfig_MSSQL);
				mUnSub = new UnSubscriber(LocalConfig.mDBConfig_MSSQL);
				mTable_SubUpdate = mSub.Select(0);
				mTable_SubUpdate.Clear();

				mChargeLog = new ChargeLog(LocalConfig.mDBConfig_MSSQL);
				mTable_ChargeLog = mChargeLog.Select(0);

				mMOLog = new MOLog(LocalConfig.mDBConfig_MSSQL);
				mTable_MOLog = mMOLog.Select(0);

				PushForEach();
			}
			catch (Exception ex)
			{
				mCTObject.mThreadStatus = ThreadStatus.Error;

				mLog.log.error("Loi xay ra trong qua trinh Charging, Thead Index:" + mCTObject.ProcessIndex, ex);
			}
		}
	}

	private boolean PushForEach() throws Exception
	{
		MyTableModel mTable = new MyTableModel(null, null);
		try
		{
			Integer MinPID = 0;

			if (mCTObject.CurrentPID > 0) MinPID = mCTObject.CurrentPID;

			for (Integer PID = MinPID; PID <= LocalConfig.MAX_PID; PID++)
			{
				mCTObject.CurrentPID = PID;
				mCTObject.MaxOrderID = 0;

				mTable = GetSubscriber(PID);

				while (!mTable.IsEmpty())
				{
					for (Integer i = 0; i < mTable.GetRowCount(); i++)
					{
						// nếu bị dừng đột ngột
						if (!Program.processData)
						{
							mLog.log.debug("Bi dung Charge: Charge Info:" + mCTObject.GetLogString(""));

							mCTObject.mThreadStatus = ThreadStatus.Stop;
							mCTObject.QueueDate = Calendar.getInstance().getTime();

							UpdateCharge();
							return false;
						}

						mCTObject.MaxOrderID = Integer.parseInt(mTable.GetValueAt(i, "OrderID").toString());

						Integer PartnerID = 0;
						if (mTable.GetValueAt(i, "PartnerID") != null)
						{
							PartnerID = Integer.parseInt(mTable.GetValueAt(i, "PartnerID").toString());
						}

						String MSISDN = mTable.GetValueAt(i, "MSISDN").toString();

						if (!MSISDN.startsWith("84")) MSISDN = MyCheck.ValidPhoneNumber(MSISDN, "84");

						mCTObject.MSISDN = MSISDN;

						Calendar mCal_Current = Calendar.getInstance();
						Calendar mCal_ExpireDate = Calendar.getInstance();
						mCal_ExpireDate.setTime(MyConfig.Get_DateFormat_InsertDB().parse(
								mTable.GetValueAt(i, "ExpiryDate").toString()));

						mLog.log.info("Xu ly charging MSISDN=" + MSISDN + "||ServiceName:"
								+ mCTObject.mServiceObject.ServiceName);

						if (mCal_ExpireDate.after(mCal_Current))
						{
							// nếu chưa hết hạn thì không tiến hành xử
							// lý charge
							continue;
						}

						Subscriber.Status mStatus = Status.FromInt(Integer.parseInt(mTable.GetValueAt(i, "StatusID")
								.toString()));
						Integer RetryChargeCount = 0;
						if (mTable.GetValueAt(i, "StatusID") != null)
							RetryChargeCount = Integer.parseInt(mTable.GetValueAt(i, "RetryChargeCount").toString());

						MyConfig.ChannelType mChannel = MyConfig.ChannelType.SYSTEM;

						ErrorCode mResultCharge = Charge.ChargeRenew(PartnerID, mCTObject.mServiceObject,
								mCTObject.MSISDN, mCTObject.mServiceObject.RegKeyword, mChannel);

						// Theo yêu cầu của VNP thì các trường hợp này phải hủy,
						// chi tiết hãy đọc tài liêu
						if (mResultCharge == ErrorCode.UserNotExist
								|| mResultCharge == ErrorCode.InvalidSubscriptionState)
						{
							mTable.SetValueAt(MyConfig.Get_DateFormat_InsertDB().format(mCal_Current.getTime()), i,
									"RetryChargeDate");
							// Hủy nhưng ko gửi MT
							DeregSub(mTable.GetRow(i), false);
							continue;
						}

						// Nếu là trường hợp này thì tiến hành đăng ký
						// lại
						if (mResultCharge == ErrorCode.InvalidSubscriptionState)
						{
							ErrorCode mResult_Reg = Charge.ChargeRegFree(PartnerID, mCTObject.mServiceObject,
									mCTObject.MSISDN, mCTObject.mServiceObject.RegKeyword, mChannel);
						}

						if (mResultCharge != ErrorCode.ChargeSuccess && mStatus == Status.ChargeFail
								&& RetryChargeCount >= LocalConfig.CHARGE_MAX_RETRY && mCTObject.AllowDereg)
						{
							mTable.SetValueAt(MyConfig.Get_DateFormat_InsertDB().format(mCal_Current.getTime()), i,
									"RetryChargeDate");

							DeregSub(mTable.GetRow(i), true);
							continue;
						}

						if (mResultCharge == ErrorCode.ChargeSuccess)
						{
							Calendar mCal_NewExpireDate = Calendar.getInstance();
							mCal_NewExpireDate.set(Calendar.MILLISECOND, 0);
							mCal_NewExpireDate.set(mCal_Current.get(Calendar.YEAR), mCal_Current.get(Calendar.MONTH),
									mCal_Current.get(Calendar.DATE), 23, 59, 59);

							mTable.SetValueAt(MyConfig.Get_DateFormat_InsertDB().format(mCal_NewExpireDate.getTime()),
									i, "ExpiryDate");

							mTable.SetValueAt(MyConfig.Get_DateFormat_InsertDB().format(mCal_Current.getTime()), i,
									"RenewChargeDate");

							mTable.SetValueAt(MyConfig.Get_DateFormat_InsertDB().format(mCal_Current.getTime()), i,
									"ChargeDate");

							mTable.SetValueAt(0, i, "RetryChargeCount");
							mTable.SetValueAt(Status.Active.GetValue(), i, "StatusID");
							mTable.SetValueAt(Status.Active.toString(), i, "StatusName");

							/*
							String MTContent = Common.GetDefineMT_Message(MTType.ExtendSuccess,
									mCTObject.mServiceObject);
							MTContent = MTContent.replace("[Date]",
									MyConfig.Get_DateFormat_VNShort().format(mCal_NewExpireDate.getTime()));
							
							//yêu cầu của Chi, Tùng bỏ đi
							if (Common.SendMT(mCTObject, MTContent))
							{
								mLog.log.debug("GUI MT RENEW THANH CONG:" + MTContent);
								Insert_MOLog(mTable.GetRow(i), MTType.ExtendSuccess,
										MyConfig.ChannelType.SYSTEM, MTContent, mCTObject.mServiceObject);
							}
							else
							{
								mLog.log.debug("GUI MT RENEW FAIL:" + MTContent);
							}*/

							// Tăng số MT bắn thành công
							mCTObject.SuccessNumber++;
						}
						else
						{
							mTable.SetValueAt(MyConfig.Get_DateFormat_InsertDB().format(mCal_Current.getTime()), i,
									"RetryChargeDate");

							mTable.SetValueAt(MyConfig.Get_DateFormat_InsertDB().format(mCal_Current.getTime()), i,
									"RetryChargeDate");
							mTable.SetValueAt(RetryChargeCount + 1, i, "RetryChargeCount");
							mTable.SetValueAt(Status.ChargeFail.GetValue(), i, "StatusID");
							mTable.SetValueAt(Status.ChargeFail.toString(), i, "StatusName");

							// Tăng số MT bắn không thành công
							mCTObject.FailNumber++;

							// Ghi lại các trường hợp chưa bắn được MT
							// để sau này push lại
							mCTObject.QueueDate = Calendar.getInstance().getTime();
						}

						MyDataRow mUpdateRow = mTable.GetRow(i).clone();
						mTable_SubUpdate.AddNewRow(mUpdateRow);
					}

					UpdateCharge();
					mTable.Clear();
					mTable = GetSubscriber(PID);
				}
			}
			mCTObject.mThreadStatus = ThreadStatus.Complete;
			return true;
		}
		catch (Exception ex)
		{
			mLog.log.debug("Loi trong charge renew cho dich vu:" + mCTObject.mServiceObject.ServiceName);
			throw ex;
		}
		finally
		{
			UpdateCharge();
			// Cập nhật thời gian kết thúc bắn tin
			mCTObject.FinishDate = Calendar.getInstance().getTime();

			mLog.log.debug("KET THUC CHARGING:" + mCTObject.mServiceObject.ServiceName);
		}
	}

	private MyTableModel AddInfo(MyDataRow mRow) throws Exception
	{
		try
		{
			MyTableModel mTable_UnSub = mUnSub.Select(0);
			mTable_UnSub.Clear();

			// Tạo row để insert vào Table Sub
			MyDataRow mNewRow = mTable_UnSub.CreateNewRow();
			mNewRow.SetValueCell("MSISDN", mRow.GetValueCell("MSISDN"));
			mNewRow.SetValueCell("ServiceID", mRow.GetValueCell("ServiceID"));

			mNewRow.SetValueCell("FirstDate", mRow.GetValueCell("FirstDate"));
			mNewRow.SetValueCell("EffectiveDate", mRow.GetValueCell("EffectiveDate"));
			mNewRow.SetValueCell("ExpiryDate", mRow.GetValueCell("ExpiryDate"));

			if (mRow.GetValueCell("ChargeDate") != null)
				mNewRow.SetValueCell("ChargeDate", mRow.GetValueCell("ChargeDate"));

			if (mRow.GetValueCell("RetryChargeCount") != null)
				mNewRow.SetValueCell("RetryChargeCount", mRow.GetValueCell("RetryChargeCount"));

			if (mRow.GetValueCell("RenewChargeDate") != null)
				mNewRow.SetValueCell("RenewChargeDate", mRow.GetValueCell("RenewChargeDate"));

			if (mRow.GetValueCell("RetryChargeDate") != null)
				mNewRow.SetValueCell("RetryChargeDate", mRow.GetValueCell("RetryChargeDate"));

			mNewRow.SetValueCell("ChannelTypeID", mRow.GetValueCell("ChannelTypeID"));
			mNewRow.SetValueCell("ChannelTypeName", mRow.GetValueCell("ChannelTypeName"));
			mNewRow.SetValueCell("StatusID", mRow.GetValueCell("StatusID"));
			mNewRow.SetValueCell("StatusName", mRow.GetValueCell("StatusName"));
			mNewRow.SetValueCell("PID", mRow.GetValueCell("PID"));
			mNewRow.SetValueCell("TotalMT", mRow.GetValueCell("TotalMT"));
			mNewRow.SetValueCell("TotalMTByDay", mRow.GetValueCell("TotalMTByDay"));
			mNewRow.SetValueCell("OrderID", mRow.GetValueCell("OrderID"));
			
			if (mRow.GetValueCell("PartnerID") != null)
				mNewRow.SetValueCell("PartnerID", mRow.GetValueCell("PartnerID"));

			if (mRow.GetValueCell("LastUpdate") != null)
				mNewRow.SetValueCell("LastUpdate", mRow.GetValueCell("LastUpdate"));

			mNewRow.SetValueCell("DeregDate",
					MyConfig.Get_DateFormat_InsertDB().format(Calendar.getInstance().getTime()));

			mTable_UnSub.AddNewRow(mNewRow);
			return mTable_UnSub;
		}
		catch (Exception ex)
		{
			throw ex;
		}
	}

	private void Insert_MOLog(MyDataRow mRow, MTType mMTType, ChannelType mChannel, String MTContent,
			ServiceObject mServiceObj) throws Exception
	{
		try
		{

			mTable_MOLog.Clear();
			MyDataRow mRow_Log = mTable_MOLog.CreateNewRow();

			mRow_Log.SetValueCell("MSISDN", mRow.GetValueCell("MSISDN"));
			mRow_Log.SetValueCell("ServiceID", mRow.GetValueCell("ServiceID"));
			mRow_Log.SetValueCell("ReceiveDate",
					MyConfig.Get_DateFormat_InsertDB().format(Calendar.getInstance().getTime()));
			mRow_Log.SetValueCell("LogDate", MyConfig.Get_DateFormat_InsertDB()
					.format(Calendar.getInstance().getTime()));
			mRow_Log.SetValueCell("ChannelTypeID", mChannel.GetValue());
			mRow_Log.SetValueCell("ChannelTypeName", mChannel.toString());
			mRow_Log.SetValueCell("MTTypeID", mMTType.GetValue());
			mRow_Log.SetValueCell("MTTypeName", mMTType.toString());
			mRow_Log.SetValueCell("MO", "");
			mRow_Log.SetValueCell("MT", MTContent);
			mRow_Log.SetValueCell("LogContent", "Renew Service:" + mServiceObj.ServiceName);
			mRow_Log.SetValueCell("PID", mRow.GetValueCell("PID"));
			mRow_Log.SetValueCell("RequestID", "0");

			mTable_MOLog.AddNewRow(mRow_Log);

			mMOLog.Insert(0, mTable_MOLog.GetXML());
		}
		catch (Exception ex)
		{
			mLog.log.error(ex);
		}
	}

	/**
	 * Hủy dịch vụ một số thuê bao khi charge không thành công
	 */
	private void DeregSub(MyDataRow mRow, boolean AllowSendMT)
	{
		String XML = "";
		try
		{
			MyTableModel mTable = AddInfo(mRow.clone());

			Integer PartnerID = 0;
			if (mRow.GetValueCell("PartnerID") != null)
			{
				PartnerID = Integer.parseInt(mRow.GetValueCell("PartnerID").toString());
			}

			XML = mTable.GetXML();

			// Tiến hành hủy đăng ký khi mà retry không
			// thành công
			if (ErrorCode.ChargeSuccess == Charge.ChargeDereg(PartnerID, mCTObject.mServiceObject, mCTObject.MSISDN,
					mCTObject.mServiceObject.DeregKeyword, MyConfig.ChannelType.MAXRETRY))
			{
				MyLogger.WriteDataLog(LocalConfig.LogDataFolder, "_Charge_Sync_Dereg_VNP_FAIL",
						"DEREG RECORD FAIL --> " + XML);
			}

			if (mUnSub.Move(0, XML))
			{
				// Có những trường hợp Hủy nhưng ko cần gửi MT
				if (AllowSendMT)
				{
					String MTContent = Common.GetDefineMT_Message(MTType.ExtendFail, mCTObject.mServiceObject);

					if (Common.SendMT(mCTObject, MTContent))
						Insert_MOLog(mRow, MTType.ExtendFail, MyConfig.ChannelType.MAXRETRY, MTContent,
								mCTObject.mServiceObject);
				}
				else
				{
					MyLogger.WriteDataLog(LocalConfig.LogDataFolder, "_Charge_Sync_Dereg_NOT_SEND_MT", "INFO --> "
							+ XML);
				}

			}
			else
			{
				MyLogger.WriteDataLog(LocalConfig.LogDataFolder, "_Charge_NotMoveToUnSub", "DEREG RECORD FAIL --> "
						+ XML);
			}
		}
		catch (Exception ex)
		{
			MyLogger.WriteDataLog(LocalConfig.LogDataFolder, "_Charge_NotMoveToUnSub", "DEREG RECORD FAIL --> " + XML);
			mLog.log.error("MSISDN:" + mCTObject.MSISDN + "||ServiceID:" + mCTObject.mServiceObject.ServiceID, ex);
		}
	}

	private void UpdateCharge() throws Exception
	{
		String XML = "";
		try
		{
			if (mTable_SubUpdate.IsEmpty()) return;

			XML = mTable_SubUpdate.GetXML();

			boolean isError = true;
			Integer retryCount = 0;

			while (isError && retryCount < 3)
			{
				retryCount++;

				try
				{
					if (!mSub.UpdateCharge(0, XML))
					{
						MyLogger.WriteDataLog(LocalConfig.LogDataFolder, "_Charge_NotUpdateDB", "LIST RECORD --> "
								+ XML);
					}
				}
				catch (Exception ex)
				{
					mLog.log.error("Loi Update charge, se duoc retry lai:retryCount:" + retryCount.toString()
							+ "|XML-->" + XML, ex);
					Thread.sleep(200);
					continue;
				}
				isError = false;
			}

		}
		catch (Exception ex)
		{
			MyLogger.WriteDataLog(LocalConfig.LogDataFolder, "_Charge_NotUpdateDB", "LIST RECORD --> " + XML);
			mLog.log.error(ex);
		}
		finally
		{
			mTable_SubUpdate.Clear();
		}
	}

	/**
	 * Lấy dữ liệu từ database
	 * 
	 * @return
	 * @throws Exception
	 */
	public MyTableModel GetSubscriber(Integer PID) throws Exception
	{
		try
		{
			// Lấy danh sách Type = 8:Lấy danh sách(Para_1 = RowCount, Para_2 =
			// PID, Para_3 = ServiceID, Para_4 = OrderID, Para_5 =
			// ProcessNumber, Para_6 = ProcessIndex
			return mSub.Select(8, mCTObject.RowCount.toString(), PID.toString(),
					mCTObject.mServiceObject.ServiceID.toString(), mCTObject.MaxOrderID.toString(),
					mCTObject.ProcessNumber.toString(), mCTObject.ProcessIndex.toString());
		}
		catch (Exception ex)
		{
			throw ex;
		}
	}

}
