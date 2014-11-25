package pro.server;

import java.io.FileInputStream;
import java.util.Iterator;
import java.util.Properties;
import java.util.Vector;

import uti.utility.MyConfig;
import uti.utility.MyConfig.Telco;
import db.define.DBConfig;

public class LocalConfig
{
	public static String ProcessConfigFile = "config.properties";

	public static String LogConfigPath = "log4j.properties";
	public static String LogDataFolder = ".\\LogFile\\";

	private static String DBConfigPath = "ProxoolConfig.xml";
	private static String MySQLPoolName = "MySQL";
	private static String MSSQLPoolName = "MSSQL";

	public static DBConfig mDBConfig_MSSQL = new DBConfig("MySQL");
	public static DBConfig mDBConfig_MySQL = new DBConfig("MSSQL");

	public static String Default_PositionName = "Ha Noi";
	public static Integer Defualt_PositionID = 1;

	public static Integer MAX_PID = 20;

	// Số lần retry cho phép nếu charge vinpahone bị lỗi
	public static Integer CHARGE_MAX_ERROR_RETRY = 1;
	/**
	 * Ten cua dich vu
	 */
	public static String ServiceName = "VOV Giao Thong";
	public static int NUM_THREAD = 10;
	public static int NUM_THREAD_LOAD_MO = 2;
	public static int NUM_THREAD_INSERTLOG = 1;

	public static int MAX_RETRIES = 10;

	public static String LOAD_MO_MODE = "DB";
	public static String MO_DIR = "Z:/";

	public static String[] RUNCLASS = null;

	public static MyConfig.Telco CURRENT_TELCO = Telco.NOTHING;

	public static String[] TELCOS =
	{"GPC"};
	public static String SHORT_CODE = "1546";

	public static int TIME_DELAY_LOAD_MO = 100;

	/**
	 * Cấu hình cho phép bắn MT dài theo content Type là gì
	 */
	public static Integer LONG_MESSAGE_CONTENT_TYPE = 21;

	/**
	 * Nếu bản tin thì cần delay để đảm bảo tin push đủ cho khách hàng
	 */
	public static int TIME_DELAY_SEND_MT = 300;

	public static Properties _prop;

	public static String MT_CHARGING = "1";
	public static String MT_NOCHARGE = "0";
	public static String MT_PUSH = "3";
	public static String MT_REFUND = "2";
	public static String MT_REFUND_SYNTAX = "21";
	public static String MT_REFUND_CONTENT = "22";

	public static String MT_SYSTEM_ERROR = "Xin loi ban, hien tai the thong dang qua tai, xin vui long thu lai sau it phut.";

	public static String INV_CLASS = "MyProcess.InvalidProcess";
	public static String INV_KEYWORD = "INV";
	public static String INV_INFO = "Tin nhan sai cu phap";

	/**
	 * Neu IS_PUSH_MT = 1; se lay INV_INFO de lam MT tra ve cho khach hang Neu
	 * IS_PUSH_MT = 0; se Luu MO vao table sms_receive_queue_inv
	 */
	public static String IS_PUSH_MT = "1";

	// ----------------cau hinh Charging-----------------------------
	public static String VNPURLCharging = "http://115.146.122.173:8092/SetRingBack.asmx";
	public static String VNPCPName = "MTRAFFIC";
	public static String VNPUserName = "mtraffic";
	public static String VNPPassword = "mtraffic#1235";

	/**
	 * Khoảng thời gian cho mỗi lần delay
	 */
	public static Integer CHARGE_TIME_DELAY = 60;

	/**
	 * các khung giờ tiến hành charging trong ngày
	 */
	public static String[] CHARGE_LIST_TIME =
	{"10"};

	/**
	 * Các khùng giờ charge ko cho phép Hủy dịch vụ
	 */
	public static String[] CHARGE_LIST_TIME_NOT_DEREG = {};

	/**
	 * Số lần retry lớn nhất cho phép
	 */
	public static Integer CHARGE_MAX_RETRY = 10;

	public static Integer CHARGE_PROCESS_NUMBER = 2;

	public static Integer CHARGE_ROWCOUNT = 10;

	public static boolean loadProperties(String propFile)
	{
		Properties properties = new Properties();
		System.out.println("Reading configuration file " + propFile);
		try
		{
			FileInputStream fin = new FileInputStream(propFile);
			properties.load(fin);
			_prop = properties;
			fin.close();

			LogDataFolder = properties.getProperty("LogDataFolder", LogDataFolder);
			LogConfigPath = properties.getProperty("LogConfigPath", LogConfigPath);
			DBConfigPath = properties.getProperty("DBConfigPath", DBConfigPath);
			MySQLPoolName = properties.getProperty("MySQLPoolName", MySQLPoolName);
			MSSQLPoolName = properties.getProperty("MSSQLPoolName", MSSQLPoolName);

			mDBConfig_MSSQL = new DBConfig(DBConfigPath, MSSQLPoolName);
			mDBConfig_MySQL = new DBConfig(DBConfigPath, MySQLPoolName);
			

			NUM_THREAD = Integer.parseInt(properties.getProperty("NUM_THREAD", "10"));
			NUM_THREAD_LOAD_MO = Integer.parseInt(properties.getProperty("NUM_THREAD_LOAD_MO", "2"));
			LOAD_MO_MODE = properties.getProperty("LOAD_MO_MODE", "DB");

			TIME_DELAY_LOAD_MO = Integer
					.parseInt(properties.getProperty("TIME_DELAY_LOAD_MO", "" + TIME_DELAY_LOAD_MO));
			TIME_DELAY_SEND_MT = Integer
					.parseInt(properties.getProperty("TIME_DELAY_SEND_MT", "" + TIME_DELAY_SEND_MT));

			String runclass = properties.getProperty("RUNCLASS", "");
			RUNCLASS = parseString(runclass, ",");

			INV_CLASS = properties.getProperty("INV_CLASS", INV_CLASS);
			INV_KEYWORD = properties.getProperty("INV_KEYWORD", INV_KEYWORD);
			INV_INFO = properties.getProperty("INV_INFO", INV_INFO);
			IS_PUSH_MT = properties.getProperty("IS_PUSH_MT", IS_PUSH_MT);

			MT_SYSTEM_ERROR = properties.getProperty("MT_SYSTEM_ERROR",
					"Xin loi ban, hien tai the thong dang qua tai, xin vui long thu lai sau it phut.");

			MAX_RETRIES = Integer.parseInt(properties.getProperty("MAX_RETRIES", "10"));

			String Temp_CURRENT_TELCO = properties.getProperty("CURRENT_TELCO", "NOTHING");

			if (Temp_CURRENT_TELCO.equalsIgnoreCase(Telco.VIETTEL.toString()))
			{
				CURRENT_TELCO = Telco.VIETTEL;
			}
			else if (Temp_CURRENT_TELCO.equalsIgnoreCase(Telco.VMS.toString()))
			{
				CURRENT_TELCO = Telco.VMS;
			}
			else if (Temp_CURRENT_TELCO.equalsIgnoreCase(Telco.GPC.toString()))
			{
				CURRENT_TELCO = Telco.GPC;
			}
			else if (Temp_CURRENT_TELCO.equalsIgnoreCase(Telco.HTC.toString()))
			{
				CURRENT_TELCO = Telco.HTC;
			}
			else if (Temp_CURRENT_TELCO.equalsIgnoreCase(Telco.BEELINE.toString()))
			{
				CURRENT_TELCO = Telco.BEELINE;
			}
			else if (Temp_CURRENT_TELCO.equalsIgnoreCase(Telco.SFONE.toString()))
			{
				CURRENT_TELCO = Telco.SFONE;
			}
			else CURRENT_TELCO = Telco.NOTHING;

			VNPURLCharging = properties.getProperty("VNPURLCharging", VNPURLCharging);
			VNPUserName = properties.getProperty("VNPUserName", VNPUserName);
			VNPPassword = properties.getProperty("VNPPassword", VNPPassword);
			VNPCPName = properties.getProperty("VNPCPName", VNPCPName);

			CHARGE_TIME_DELAY = Integer.parseInt(properties.getProperty("CHARGE_TIME_DELAY",
					CHARGE_TIME_DELAY.toString()));
			CHARGE_MAX_RETRY = Integer
					.parseInt(properties.getProperty("CHARGE_MAX_RETRY", CHARGE_MAX_RETRY.toString()));
			CHARGE_ROWCOUNT = Integer.parseInt(properties.getProperty("CHARGE_ROWCOUNT", CHARGE_ROWCOUNT.toString()));
			CHARGE_PROCESS_NUMBER = Integer.parseInt(properties.getProperty("CHARGE_PROCESS_NUMBER",
					CHARGE_PROCESS_NUMBER.toString()));

			CHARGE_LIST_TIME = properties.getProperty("CHARGE_LIST_TIME", "10").split("\\|");

			CHARGE_LIST_TIME_NOT_DEREG = properties.getProperty("CHARGE_LIST_TIME_NOT_DEREG", "10").split("\\|");

			return true;

		}
		catch (Exception e)
		{
			System.out.println(e);
			return false;
		}

	}

	public static int getintproperties(String text, int defaultval)
	{
		try
		{
			return Integer.parseInt(_prop.getProperty(text, defaultval + ""));

		}
		catch (Exception e)
		{
			System.out.println(e);
		}
		return defaultval;
	}

	public static String getstringproperties(String text, String defaultval)
	{
		try
		{
			return (_prop.getProperty(text, defaultval + ""));
		}
		catch (Exception e)
		{
			System.out.println(e);
		}
		return defaultval;
	}

	public static String[] parseString(String text, String seperator)
	{
		Vector<String> vResult = new Vector<String>();
		if (text == null || "".equals(text)) { return null; }
		String tempStr = text.trim();
		String currentLabel = null;
		int index = tempStr.indexOf(seperator);
		while (index != -1)
		{
			currentLabel = tempStr.substring(0, index).trim();

			if (!"".equals(currentLabel))
			{
				vResult.addElement(currentLabel);
			}
			tempStr = tempStr.substring(index + 1);
			index = tempStr.indexOf(seperator);
		} // Last label
		currentLabel = tempStr.trim();
		if (!"".equals(currentLabel))
		{
			vResult.addElement(currentLabel);
		}
		String[] re = new String[vResult.size()];
		Iterator<String> it = vResult.iterator();
		index = 0;
		while (it.hasNext())
		{
			re[index] = (String) it.next();
			index++;
		}
		return re;
	}

}
