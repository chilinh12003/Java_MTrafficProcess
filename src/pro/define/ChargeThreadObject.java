package pro.define;

import java.util.Date;

import uti.utility.MyConfig;
import dat.service.ServiceObject;

/**
 * Class khai báo các thread đang Charging cho từng dịch vụ.
 * Thường sẽ được lưu xuống file khi stop chương trình với thông tin
 * (Push tới đâu (PID, OrderID), cho dịch nào, với tin tức nào Khi start lại
 * trường trình sẽ charge lại
 * 
 * @author Administrator
 * 
 */
public class ChargeThreadObject implements java.io.Serializable
{
	/**
	 * Cho biết tình trạng của Thread này
	 * 
	 * @author Administrator
	 * 
	 */
	public enum ThreadStatus
	{
		Default(1),

		/**
		 * bị dừng đột ngột do người quản trị
		 */
		Stop(2),
		/**
		 * Bị lỗi trong quá trình bắn tin
		 */
		Error(3),
		/**
		 * Hoàn thành bắn tin
		 */
		Complete(4),
		/**
		 * đang Charge
		 */
		Charging(5),
		/**
		 * Cho biết thread đang charge retry
		 */
		RetryCharging(6),
		/**
		 * Cho biết thread đang charge record da charge không thành công
		 */
		RetryChargingFail(7), ;

		private int value;

		private ThreadStatus(int value)
		{
			this.value = value;
		}

		public int GetValue()
		{
			return this.value;
		}

		public static ThreadStatus FromInt(int iValue)
		{
			for (ThreadStatus type : ThreadStatus.values())
			{
				if (type.GetValue() == iValue)
					return type;
			}
			return Default;
		}

	}

	/**
	 * Cho biết tình trạng của Thread
	 */
	public ThreadStatus mThreadStatus = ThreadStatus.Default;

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	
	/**
	 * Cho biết đang push tới PID nào
	 */
	public Integer CurrentPID = 1;

	public String MSISDN = "";

	/**
	 * Số lượng process Push MT được tạo ra
	 */
	public Integer ProcessNumber = 1;

	/**
	 * Thứ tự của 1 process
	 */
	public Integer ProcessIndex = 1;

	/**
	 * Số thứ tự (OrderID) trong table Subscriber, process sẽ lấy những record
	 * có OrderID >= MaxOrderID
	 */
	public Integer MaxOrderID = 0;

	/**
	 * Tổng số record mỗi lần lấy lên để xử lý
	 */
	public Integer RowCount = 10;

	/**
	 * Đối tượng dịch vụ cần push tin
	 */
	public ServiceObject mServiceObject = new ServiceObject();

	
	/**
	 * Thời gian bắt đầu chạy thead
	 */
	public Date StartDate = null;

	/**
	 * Thời gian kết thúc chạy thead
	 */
	public Date FinishDate = null;

	/**
	 * Số MT bắn thành công đối với thead này
	 */
	public Integer SuccessNumber = 0;

	/**
	 * Số MT bắn không thành công
	 */
	public Integer FailNumber = 0;

	/**
	 * Số lần push lại không thành công
	 */
	public Integer RetryCount = 0;

	/**
	 * Thơi gian đưa MT này vào queue
	 */
	public Date QueueDate = null;

	public String ResultFromVNP = "";

	/**
	 * Thời gian retry lần cuối
	 */
	public Date RetryDate = null;

	public boolean IsNull()
	{
		if (MSISDN == "" || mServiceObject.IsNull())
			return true;
		else
			return false;
	}

	/**
	 * Cho phép hủy khi charge không thành công.
	 * vì charge nhiều lần trong ngày nên sẽ cấu hình khùng giờ nào cho phép Hủy dịch vụ khi charge gia hạn không thành công
	 */
	public boolean AllowDereg = true;
	public String GetLogString(String Suffix) throws Exception
	{
		try
		{
			if (IsNull())
				return "";
			String Fomart = "MSISDN:%s || ProcessIndex:%s || CurrentPID:%s || MaxOrderID:%s || ServiceID:%s || PushTime:%s || QueueDate:%s || RetryCount:%s || RetryDate:%s || Suffix:%s";
			return String.format(Fomart, new Object[] { MSISDN, ProcessIndex, CurrentPID, MaxOrderID, mServiceObject.ServiceID,
					MyConfig.Get_DateFormat_InsertDB().format(mServiceObject.CurrentPushTime), MyConfig.Get_DateFormat_InsertDB().format(QueueDate), RetryCount,
					(RetryDate == null ? "null" : MyConfig.Get_DateFormat_InsertDB().format(RetryDate)), Suffix });
		}
		catch (Exception ex)
		{
			throw ex;
		}
	}

	
}
