package pro.mo;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;

import pro.charge.Charge;
import pro.charge.Charge.ErrorCode;
import pro.server.Common;
import pro.server.ContentAbstract;
import pro.server.Keyword;
import pro.server.LocalConfig;
import pro.server.MsgObject;
import uti.utility.MyConfig;
import uti.utility.MyConfig.ChannelType;
import uti.utility.MyConvert;
import uti.utility.MyLogger;
import dat.service.DefineMT;
import dat.service.DefineMT.MTType;
import dat.service.MOLog;
import dat.service.ServiceObject;
import dat.sub.Subscriber;
import dat.sub.SubscriberObject;
import dat.sub.UnSubscriber;
import db.define.MyDataRow;
import db.define.MyTableModel;

public class RegisterHelp extends ContentAbstract
{
	MyLogger mLog = new MyLogger(LocalConfig.LogConfigPath,this.getClass().toString());
	Collection<MsgObject> ListMessOject = new ArrayList<MsgObject>();

	MsgObject mMsgObject = null;
	SubscriberObject mSubObj = new SubscriberObject();

	ServiceObject mServiceObj = new ServiceObject();
	Calendar mCal_Current = Calendar.getInstance();
	Calendar mCal_SendMO = Calendar.getInstance();
	Calendar mCal_Expire = Calendar.getInstance();

	Subscriber mSub = null;
	UnSubscriber mUnSub = null;
	MOLog mMOLog = null;
	dat.service.Keyword mKeyword=null;
	
	MyTableModel mTable_MOLog = null;
	MyTableModel mTable_Sub = null;

	DefineMT.MTType mMTType = MTType.RegFail;

	private int FreeCount = 365;
	String MTContent = "";

	private void Init(MsgObject msgObject, Keyword keyword) throws Exception
	{
		try
		{
			mSub = new Subscriber(LocalConfig.mDBConfig_MSSQL);
			mUnSub = new UnSubscriber(LocalConfig.mDBConfig_MSSQL);
			mMOLog = new MOLog(LocalConfig.mDBConfig_MSSQL);

			mTable_MOLog = mMOLog.Select(0);
			mTable_Sub = mSub.Select(0);

			mMsgObject = msgObject;

			mCal_SendMO.setTime(mMsgObject.getTTimes());

			mCal_Expire.set(Calendar.MILLISECOND, 0);
			mCal_Expire.add(Calendar.DATE, FreeCount);

		}
		catch (Exception ex)
		{
			throw ex;
		}
	}

	private Collection<MsgObject> AddToList() throws Exception
	{
		try
		{
			ListMessOject.clear();
			MTContent = Common.GetDefineMT_Message(mMTType, mServiceObj);

			mMsgObject.setUsertext(MTContent);
			mMsgObject.setContenttype(21);
			mMsgObject.setMsgtype(1);

			ListMessOject.add(new MsgObject(mMsgObject));
			return ListMessOject;
		}
		catch (Exception ex)
		{
			throw ex;
		}
	}

	private void Insert_MOLog() throws Exception
	{
		try
		{
			MOLog mMOLog = new MOLog(LocalConfig.mDBConfig_MSSQL);
			mTable_MOLog.Clear();
			MyDataRow mRow_Log = mTable_MOLog.CreateNewRow();

			mRow_Log.SetValueCell("ServiceID", mServiceObj.ServiceID);
			mRow_Log.SetValueCell("MSISDN", mMsgObject.getUserid());
			mRow_Log.SetValueCell("ReceiveDate", MyConfig.Get_DateFormat_InsertDB().format(mCal_SendMO.getTime()));
			mRow_Log.SetValueCell("LogDate", MyConfig.Get_DateFormat_InsertDB().format(mCal_Current.getTime()));
			mRow_Log.SetValueCell("ChannelTypeID", mMsgObject.getChannelType());
			mRow_Log.SetValueCell("ChannelTypeName", MyConfig.ChannelType.FromInt(mMsgObject.getChannelType()).toString());
			mRow_Log.SetValueCell("MTTypeID", mMTType.GetValue());
			mRow_Log.SetValueCell("MTTypeName", mMTType.toString());
			mRow_Log.SetValueCell("MO", mMsgObject.getMO());
			mRow_Log.SetValueCell("MT", MTContent);
			mRow_Log.SetValueCell("LogContent", "DKDV:" + mServiceObj.ServiceName);
			mRow_Log.SetValueCell("PID", MyConvert.GetPIDByMSISDN(mMsgObject.getUserid(), LocalConfig.MAX_PID));
			mRow_Log.SetValueCell("RequestID", mMsgObject.getRequestid().toString());
			mRow_Log.SetValueCell("PartnerID", mSubObj.PartnerID);
			mTable_MOLog.AddNewRow(mRow_Log);

			mMOLog.Insert(0, mTable_MOLog.GetXML());
		}
		catch (Exception ex)
		{
			mLog.log.error(ex);
		}
	}

	private MyTableModel AddInfo() throws Exception
	{
		try
		{
			MyTableModel mTable_Sub = mSub.Select(0);
			mTable_Sub.Clear();

			// Tạo row để insert vào Table Sub
			MyDataRow mRow_Sub = mTable_Sub.CreateNewRow();
			mRow_Sub.SetValueCell("MSISDN", mSubObj.MSISDN);
			mRow_Sub.SetValueCell("ServiceID", mSubObj.ServiceID);

			mRow_Sub.SetValueCell("FirstDate", MyConfig.Get_DateFormat_InsertDB().format(mSubObj.FirstDate));
			mRow_Sub.SetValueCell("EffectiveDate", MyConfig.Get_DateFormat_InsertDB().format(mSubObj.EffectiveDate));
			mRow_Sub.SetValueCell("ExpiryDate", MyConfig.Get_DateFormat_InsertDB().format(mSubObj.ExpiryDate));

			mRow_Sub.SetValueCell("RetryChargeCount", mSubObj.RetryChargeCount);

			if (mSubObj.ChargeDate != null)
				mRow_Sub.SetValueCell("ChargeDate", MyConfig.Get_DateFormat_InsertDB().format(mSubObj.ChargeDate));

			mRow_Sub.SetValueCell("ChannelTypeID", mSubObj.ChannelTypeID);
			mRow_Sub.SetValueCell("ChannelTypeName", mSubObj.ChannelTypeName);
			mRow_Sub.SetValueCell("StatusID", mSubObj.StatusID);
			mRow_Sub.SetValueCell("StatusName", mSubObj.StatusName);
			mRow_Sub.SetValueCell("PID", mSubObj.PID);
			mRow_Sub.SetValueCell("TotalMT", mSubObj.TotalMT);
			mRow_Sub.SetValueCell("TotalMTByDay", mSubObj.TotalMTByDay);
			mRow_Sub.SetValueCell("PartnerID", mSubObj.PartnerID);
			if (mSubObj.LastUpdate != null)
				mRow_Sub.SetValueCell("LastUpdate", MyConfig.Get_DateFormat_InsertDB().format(mSubObj.LastUpdate));

			mTable_Sub.AddNewRow(mRow_Sub);
			return mTable_Sub;
		}
		catch (Exception ex)
		{
			throw ex;
		}
	}

	private boolean Insert_Sub() throws Exception
	{
		try
		{
			MyTableModel mTable_Sub = AddInfo();

			if (!mSub.Insert(0, mTable_Sub.GetXML()))
			{
				mLog.log.info("Insert vao table Subscriber KHONG THANH CONG: XML Insert-->" + mTable_Sub.GetXML());
				return false;
			}

			return true;
		}
		catch (Exception ex)
		{
			throw ex;
		}
	}

	private boolean MoveToUnSub() throws Exception
	{
		try
		{
			MyTableModel mTable_Sub = AddInfo();

			if (!mSub.Move(0, mTable_Sub.GetXML()))
			{
				mLog.log.info("Move tu UnSub Sang Sub KHONG THANH CONG: XML Insert-->" + mTable_Sub.GetXML());
				return false;
			}

			return true;
		}
		catch (Exception ex)
		{
			throw ex;
		}
	}

	/**
	 * tạo dữ liệu cho những đăng ký lại (trước đó đã hủy dịch vụ)
	 * 
	 * @throws Exception
	 */
	private void CreateRegAgain() throws Exception
	{
		try
		{
			mSubObj.ChannelTypeID = mMsgObject.getChannelType();
			mSubObj.ChannelTypeName = MyConfig.ChannelType.FromInt(mMsgObject.getChannelType()).toString();

			mSubObj.EffectiveDate = mCal_Current.getTime();
			mSubObj.ExpiryDate = mCal_Expire.getTime();

			mSubObj.LastUpdate = mCal_Current.getTime();
			mSubObj.MSISDN = mMsgObject.getUserid();
			mSubObj.PID = MyConvert.GetPIDByMSISDN(mSubObj.MSISDN, LocalConfig.MAX_PID);
			mSubObj.ServiceID = mServiceObj.ServiceID;
			mSubObj.StatusID = dat.sub.Subscriber.Status.Active.GetValue();
			mSubObj.StatusName = dat.sub.Subscriber.Status.Active.toString();
			mSubObj.TotalMTByDay = 0;
		}
		catch (Exception ex)
		{
			throw ex;
		}

	}

	/**
	 * Tạo dữ liệu cho một đăng ký mới
	 * 
	 * @throws Exception
	 */
	private void CreateNewReg() throws Exception
	{
		try
		{
			mSubObj.ChannelTypeID = mMsgObject.getChannelType();
			mSubObj.ChannelTypeName = MyConfig.ChannelType.FromInt(mMsgObject.getChannelType()).toString();
			mSubObj.ChargeDate = null;
			mSubObj.DeregDate = null;
			mSubObj.EffectiveDate = mCal_Current.getTime();
			mSubObj.ExpiryDate = mCal_Expire.getTime();
			mSubObj.FirstDate = mCal_Current.getTime();
			mSubObj.IsDereg = false;
			mSubObj.LastUpdate = mCal_Current.getTime();
			mSubObj.MSISDN = mMsgObject.getUserid();
			mSubObj.PID = MyConvert.GetPIDByMSISDN(mSubObj.MSISDN, LocalConfig.MAX_PID);
			mSubObj.ServiceID = mServiceObj.ServiceID;
			mSubObj.StatusID = dat.sub.Subscriber.Status.Active.GetValue();
			mSubObj.StatusName = dat.sub.Subscriber.Status.Active.toString();
			mSubObj.TotalMT = 0;
			mSubObj.TotalMTByDay = 0;

		}
		catch (Exception ex)
		{
			throw ex;
		}
	}

	protected Collection<MsgObject> getMessages(MsgObject msgObject, Keyword keyword) throws Exception
	{
		try
		{
			// Khoi tao
			Init(msgObject, keyword);

			// Lấy service
			mServiceObj = Common.GetService(mMsgObject.getUsertext(), "");

			if(MyConfig.ChannelType.FromInt(mMsgObject.getChannelType()) == ChannelType.WAP)
			{
				//nếu đăng ký từ wap thì sẽ dựa vào table Keyword (MS SQL) để lấy dịch vụ
				MyTableModel mTable_Keyword = mKeyword.Select(4, mMsgObject.getKeyword());
				if(mTable_Keyword.GetRowCount() > 0 && mTable_Keyword.GetValueAt(0, "ServiceID") != null)
				{
					Integer mServiceID =Integer.parseInt(mTable_Keyword.GetValueAt(0, "ServiceID").toString());
					mServiceObj = Common.GetService(mServiceID);
				}
				else
				{
					mServiceObj = Common.GetService(mMsgObject.getUsertext(), "");
				}
			}
			else
			{
				// Lấy service
				mServiceObj = Common.GetService(mMsgObject.getUsertext(), "");
			}		
			

			Integer PID = MyConvert.GetPIDByMSISDN(mMsgObject.getUserid(), LocalConfig.MAX_PID);
			// Lấy thông tin khách hàng đã đăng ký
			MyTableModel mTable_Sub = mSub.Select(2, PID.toString(), mMsgObject.getUserid(), mServiceObj.ServiceID.toString());

			mSubObj = SubscriberObject.Convert(mTable_Sub,false);

			if (mSubObj.IsNull())
			{
				mTable_Sub = mUnSub.Select(2, PID.toString(), mMsgObject.getUserid(), mServiceObj.ServiceID.toString());

				if (mTable_Sub.GetRowCount() > 0)
					mSubObj = SubscriberObject.Convert(mTable_Sub,true);
			}

			mSubObj.PID = MyConvert.GetPIDByMSISDN(mMsgObject.getUserid(), LocalConfig.MAX_PID);

			mSubObj.PartnerID = mKeyword.GetPartnerID(msgObject.getKeyword());
			
			// Đăng ký mới (chưa từng đăng ký trước đây)
			if (mSubObj.IsNull())
			{
				// Tạo dữ liệu cho đăng ký mới
				CreateNewReg();

				ErrorCode mResult = Charge.ChargeRegFree(mSubObj.PartnerID,mServiceObj, mMsgObject.getUserid(), mMsgObject.getKeyword(),
						MyConfig.ChannelType.FromInt(mMsgObject.getChannelType()));
				if (mResult != ErrorCode.ChargeSuccess)
				{
					mMTType = MTType.RegFail; // Đăng ký lại nhưng miễn phí
					return AddToList();
				}

				if (Insert_Sub())
				{
					mMTType = MTType.RegHelp;
				}
				else
				{
					mMTType = MTType.RegFail;
				}

				return AddToList();
			}

			// Nếu đã đăng ký rồi và tiếp tục đăng ký
			if (!mSubObj.IsNull() && mSubObj.IsDereg == false)
			{

				mMTType = MTType.RegRepeatFree;
				return AddToList();
			}

			// Đã đăng ký trước đó nhưng đang hủy
			if (mSubObj.IsDereg)
			{
				CreateRegAgain();

				ErrorCode mResult = Charge.ChargeRegFree(mSubObj.PartnerID,mServiceObj, mMsgObject.getUserid(), mMsgObject.getKeyword(),
						MyConfig.ChannelType.FromInt(mMsgObject.getChannelType()));
				if (mResult != ErrorCode.ChargeSuccess)
				{
					mMTType = MTType.RegFail; // Đăng ký lại nhưng miễn phí
					return AddToList();
				}

				// Nếu xóa unsub hoặc Insert sub không thành công thì thông
				// báo lỗi
				if (MoveToUnSub())
				{
					mMTType = MTType.RegHelp;
					return AddToList();
				}

				mMTType = MTType.RegFail; // Đăng ký lại nhưng miễn phí
				return AddToList();
			}

			mMTType = MTType.RegFail;
			return AddToList();
		}
		catch (Exception ex)
		{
			mLog.log.error(Common.GetStringLog(msgObject), ex);
			mMTType = MTType.RegFail;
			return AddToList();
		}
		finally
		{
			// Insert vao log
			Insert_MOLog();

			mLog.log.debug(Common.GetStringLog(mMsgObject));
		}
	}

}