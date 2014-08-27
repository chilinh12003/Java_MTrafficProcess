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
import java.io.Serializable;
import java.math.BigDecimal;
import java.sql.Timestamp;
import java.util.Date;

public class MsgObject implements Serializable
{
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	long id = 1;
	String serviceid = "";
	String userid = "";
	String keyword = "";
	String usertext = "";
	BigDecimal requestid = new BigDecimal(-1);
	Timestamp tTimes;
	String mobileoperator = "";
	int cpid = 0;
	int msgtype = 1;
	int contenttype = 0;
	String msgnotes = "";
	long receiveid = 0;
	int channelType = 1;

	String dport = "0";
	String MO = "";

	public String getMO()
	{
		return this.MO;
	}

	public String getDport()
	{
		return dport;
	}

	public int getChannelType()
	{
		return channelType;
	}

	public void setDport(String dport)
	{
		this.dport = dport;
	}

	public long getReceiveid()
	{
		return receiveid;
	}

	public void setReceiveid(long receiveid)
	{
		this.receiveid = receiveid;
	}

	public MsgObject()
	{

	}

	public MsgObject(MsgObject msgobject)
	{
		this.id = msgobject.getId();
		this.serviceid = msgobject.getServiceid();
		this.userid = msgobject.getUserid();
		this.keyword = msgobject.getKeyword();
		this.usertext = msgobject.getUsertext();
		this.requestid = msgobject.getRequestid();
		this.tTimes = msgobject.getTTimes();
		this.mobileoperator = msgobject.getMobileoperator();
		this.cpid = msgobject.getCpid();
		this.msgtype = msgobject.getMsgtype();
		this.contenttype = msgobject.getContenttype();
		this.msgnotes = msgobject.getMsgnotes();
		this.receiveid = msgobject.getReceiveid();
		this.dport = msgobject.getDport();
		this.MO = msgobject.getMO();
	}

	public MsgObject(long id, String serviceid, String userid, String keyword, String usertext, BigDecimal requestid, Timestamp tTimes, String mobileoperator,
			int msgtype, int contenttype, String MO)
	{
		this.id = id;
		this.serviceid = serviceid;
		this.userid = userid;
		this.keyword = keyword;
		this.usertext = usertext;
		this.requestid = requestid;
		this.tTimes = tTimes;
		this.mobileoperator = mobileoperator;
		this.msgtype = msgtype;
		this.contenttype = contenttype;
		this.MO = MO;
	}

	public MsgObject(long id, String serviceid, String userid, String keyword, String usertext, BigDecimal requestid, Timestamp tTimes, String mobileoperator,
			int msgtype, int contenttype, long receiveid, String MO)
	{
		this.id = id;
		this.serviceid = serviceid;
		this.userid = userid;
		this.keyword = keyword;
		this.usertext = usertext;
		this.requestid = requestid;
		this.tTimes = tTimes;
		this.mobileoperator = mobileoperator;
		this.msgtype = msgtype;
		this.contenttype = contenttype;
		this.receiveid = receiveid;
		this.MO = MO;
	}

	public MsgObject(long id, String serviceid, String userid, String keyword, String usertext, BigDecimal requestid, Timestamp tTimes, String mobileoperator,
			int msgtype, int contenttype, int cpid, String msgnotes, String MO)
	{
		this.id = id;
		this.serviceid = serviceid;
		this.userid = userid;
		this.keyword = keyword;
		this.usertext = usertext;
		this.requestid = requestid;
		this.tTimes = tTimes;
		this.mobileoperator = mobileoperator;
		this.msgtype = msgtype;
		this.contenttype = contenttype;
		this.cpid = cpid;
		this.msgnotes = msgnotes;
		this.MO = MO;
	}

	public MsgObject(long id, String serviceid, String userid, String keyword, String usertext, BigDecimal requestid, Timestamp tTimes, String mobileoperator,
			int msgtype, int contenttype, int cpid, String msgnotes, int ChannelType, String MO)
	{
		this.id = id;
		this.serviceid = serviceid;
		this.userid = userid;
		this.keyword = keyword;
		this.usertext = usertext;
		this.requestid = requestid;
		this.tTimes = tTimes;
		this.mobileoperator = mobileoperator;
		this.msgtype = msgtype;
		this.contenttype = contenttype;
		this.cpid = cpid;
		this.msgnotes = msgnotes;
		this.channelType = ChannelType;
		this.MO = MO;
	}

	public String getServiceid()
	{
		return serviceid;
	}

	public void setServiceid(String serviceid)
	{
		this.serviceid = serviceid;
	}

	public String getUserid()
	{
		return userid;
	}

	public void setUserid(String userid)
	{
		this.userid = userid;
	}

	public String getKeyword()
	{
		return keyword;
	}

	public String getMsgnotes()
	{
		return msgnotes;
	}

	public void setKeyword(String keyword)
	{
		this.keyword = keyword;
	}

	public String getUsertext()
	{
		return usertext;
	}

	public void setUsertext(String usertext)
	{
		this.usertext = usertext;
	}

	public BigDecimal getRequestid()
	{
		return requestid;
	}

	public void setRequestid(BigDecimal requestid)
	{
		this.requestid = requestid;
	}

	public Timestamp getTTimes()
	{
		return tTimes;
	}
	
	public Date getReceiveDate()
	{
		return new Date(getTTimes().getTime());
	}
	

	public void setTTimes(Timestamp times)
	{
		tTimes = times;
	}

	public String getMobileoperator()
	{
		return mobileoperator;
	}

	public void setMobileoperator(String mobileoperator)
	{
		this.mobileoperator = mobileoperator;
	}

	public int getMsgtype()
	{
		return msgtype;
	}

	public void setMsgtype(int msgtype)
	{
		this.msgtype = msgtype;
	}

	public int getContenttype()
	{
		return contenttype;
	}

	public void setContenttype(int contenttype)
	{
		this.contenttype = contenttype;
	}

	public void setMsgNotes(String msgnotes)
	{
		if ("".equals(this.msgnotes))
		{
			this.msgnotes = msgnotes;
		}
		else
		{
			this.msgnotes = this.msgnotes + "@" + msgnotes;
		}

	}

	public int getCpid()
	{
		return cpid;
	}

	public void setCpid(int cpid)
	{
		this.cpid = cpid;
	}

	public void setMsgnotes(String msgnotes)
	{
		this.msgnotes = msgnotes;
	}

	public BigDecimal getLongRequestid()
	{
		return this.requestid;
	}

	public long getId()
	{
		return id;

	}
}
