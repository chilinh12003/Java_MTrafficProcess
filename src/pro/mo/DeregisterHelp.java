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

public class DeregisterHelp extends ContentAbstract
{
	MyLogger mLog = new MyLogger(LocalConfig.LogConfigPath,this.getClass().toString());
	Collection<MsgObject> ListMessOject = new ArrayList<MsgObject>();

	MsgObject mMsgObject = null;
	SubscriberObject mSubObj = new SubscriberObject();

	ServiceObject mServiceObj = new ServiceObject();
	Calendar mCal_Current = Calendar.getInstance();
	Calendar mCal_SendMO = Calendar.getInstance();

	Subscriber mSub = null;
	UnSubscriber mUnSub = null;
	MOLog mMOLog = null;

	MyTableModel mTable_MOLog = null;
	MyTableModel mTable_Sub = null;

	DefineMT.MTType mMTType = MTType.RegFail;

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
			mRow_Log.SetValueCell("LogContent", "Huy DV:" + mServiceObj.ServiceName);
			mRow_Log.SetValueCell("PID", MyConvert.GetPIDByMSISDN(mMsgObject.getUserid(), LocalConfig.MAX_PID));
			mRow_Log.SetValueCell("RequestID", mMsgObject.getRequestid().toString());

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
			MyTableModel mTable_UnSub = mUnSub.Select(0);
			mTable_UnSub.Clear();

			// Tạo row để insert vào Table Sub
			MyDataRow mNewRow = mTable_UnSub.CreateNewRow();
			mNewRow.SetValueCell("MSISDN", mSubObj.MSISDN);
			mNewRow.SetValueCell("ServiceID", mSubObj.ServiceID);

			mNewRow.SetValueCell("FirstDate", MyConfig.Get_DateFormat_InsertDB().format(mSubObj.FirstDate));
			mNewRow.SetValueCell("EffectiveDate", MyConfig.Get_DateFormat_InsertDB().format(mSubObj.EffectiveDate));
			mNewRow.SetValueCell("ExpiryDate", MyConfig.Get_DateFormat_InsertDB().format(mSubObj.ExpiryDate));

			if (mSubObj.ChargeDate != null)
				mNewRow.SetValueCell("ChargeDate", MyConfig.Get_DateFormat_InsertDB().format(mSubObj.ChargeDate));

			if (mSubObj.RetryChargeCount != null)
				mNewRow.SetValueCell("RetryChargeCount", mSubObj.RetryChargeCount);

			if (mSubObj.RetryChargeDate != null)
				mNewRow.SetValueCell("RetryChargeDate", MyConfig.Get_DateFormat_InsertDB().format(mSubObj.RetryChargeDate));

			mNewRow.SetValueCell("ChannelTypeID", mSubObj.ChannelTypeID);
			mNewRow.SetValueCell("ChannelTypeName", mSubObj.ChannelTypeName);
			mNewRow.SetValueCell("StatusID", mSubObj.StatusID);
			mNewRow.SetValueCell("StatusName", mSubObj.StatusName);
			mNewRow.SetValueCell("PID", mSubObj.PID);
			mNewRow.SetValueCell("TotalMT", mSubObj.TotalMT);
			mNewRow.SetValueCell("TotalMTByDay", mSubObj.TotalMTByDay);
			mNewRow.SetValueCell("OrderID", mSubObj.OrderID);

			if (mSubObj.LastUpdate != null)
				mNewRow.SetValueCell("LastUpdate", MyConfig.Get_DateFormat_InsertDB().format(mSubObj.LastUpdate));

			if (mSubObj.DeregDate != null)
				mNewRow.SetValueCell("DeregDate", MyConfig.Get_DateFormat_InsertDB().format(mSubObj.DeregDate));

			mTable_UnSub.AddNewRow(mNewRow);
			return mTable_UnSub;
		}
		catch (Exception ex)
		{
			throw ex;
		}
	}

	private boolean MoveToSub() throws Exception
	{
		try
		{
			MyTableModel mTable_UnSub = AddInfo();

			if (!mUnSub.Move(0, mTable_UnSub.GetXML()))
			{
				mLog.log.info(" Move Tu Sub Sang UnSub KHONG THANH CONG: XML Insert-->" + mTable_UnSub.GetXML());
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
	private void CreateDeReg() throws Exception
	{
		try
		{
			mSubObj.ChannelTypeID = mMsgObject.getChannelType();
			mSubObj.ChannelTypeName = MyConfig.ChannelType.FromInt(mMsgObject.getChannelType()).toString();
			mSubObj.DeregDate = mCal_Current.getTime();
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
			mServiceObj = Common.GetService("", mMsgObject.getUsertext());

			if (mServiceObj.IsNull())
			{
				mLog.log.info("Dich vu khong ton tai.");
				mMTType = MTType.Invalid;
				return AddToList();
			}
			Integer PID = MyConvert.GetPIDByMSISDN(mMsgObject.getUserid(), LocalConfig.MAX_PID);

			MyTableModel mTable_Sub = mSub.Select(2, PID.toString(), mMsgObject.getUserid(), mServiceObj.ServiceID.toString());

			if (mTable_Sub.GetRowCount() > 0)
				mSubObj = SubscriberObject.Convert(mTable_Sub,false);

			mSubObj.PID = MyConvert.GetPIDByMSISDN(mMsgObject.getUserid(), LocalConfig.MAX_PID);

			// Nếu chưa đăng ký dịch vụ
			if (mSubObj.IsNull())
			{
				mMTType = MTType.DeRegNotRegister;
				return AddToList();
			}

			CreateDeReg();
			ErrorCode mResult = Charge.ChargeDereg(mSubObj.PartnerID, mServiceObj, mMsgObject.getUserid(), mMsgObject.getKeyword(),
					MyConfig.ChannelType.FromInt(mMsgObject.getChannelType()));
			if (mResult != ErrorCode.ChargeSuccess)
			{
				mMTType = MTType.RegFail;
				return AddToList();
			}

			if (MoveToSub())
			{
				mMTType = MTType.DeRegSuccess;
				return AddToList();
			}

			mMTType = MTType.DeRegFail;

			return AddToList();
		}
		catch (Exception ex)
		{
			mLog.log.error(Common.GetStringLog(msgObject), ex);
			mMTType = MTType.DeRegFail;
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