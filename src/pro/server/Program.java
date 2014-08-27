package pro.server;

//import java.io.BufferedReader;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.Enumeration;
import java.util.Vector;
import java.util.concurrent.atomic.AtomicLong;

import pro.check.CheckCharge;
import uti.utility.MyLogger;
import dat.service.DefineMT;
import dat.service.DefineMTObject;
import dat.service.Service;
import dat.service.ServiceObject;

public class Program extends Thread
{
	static MyLogger mLog = new MyLogger(LocalConfig.LogConfigPath, Program.class.toString());
	public static boolean getData = true;
	public static boolean processData = true;

	public static boolean isAllThreadStarted = false;
	public static ProcessLoadKeyword mLoadKeyword = null;
	public static MsgQueue queue = new MsgQueue();
	public static MsgQueue queueLog = new MsgQueue();

	public static LoadMO[] loadMO = null;

	public static ExecuteQueue[] executequeue = new ExecuteQueue[20];

	public static ExecuteInsertReceiveLog[] insert_receive_log = new ExecuteInsertReceiveLog[1];

	static String VERSION = "2013";

	// bo dem
	private static AtomicLong[] moload =
	{new AtomicLong(0), new AtomicLong(0), new AtomicLong(0), new AtomicLong(0)};
	private static AtomicLong[] moprocess =
	{new AtomicLong(0), new AtomicLong(0), new AtomicLong(0), new AtomicLong(0)};

	// cac khai bao them
	/**
	 * Danh sách các MT đã được định nghĩa (được lấy trong DB khi chươn trinh
	 * bắt đầu chạy
	 */
	public static Vector<DefineMTObject> ListDefineMT = new Vector<DefineMTObject>();

	/**
	 * Chứa danh sách các dịch vụ đang được kích hoạt
	 */
	public static Vector<ServiceObject> mListServiceObject = new Vector<ServiceObject>();

	public Program()
	{
		try
		{
			loadMO = new LoadMO[2];

			LocalConfig.loadProperties("config.properties");
			executequeue = new ExecuteQueue[LocalConfig.NUM_THREAD];
			loadMO = new LoadMO[LocalConfig.NUM_THREAD_LOAD_MO];
			insert_receive_log = new ExecuteInsertReceiveLog[LocalConfig.NUM_THREAD_INSERTLOG];

			moload = new AtomicLong[LocalConfig.TELCOS.length];
			moprocess = new AtomicLong[LocalConfig.TELCOS.length];
			for (int i = 0; i < LocalConfig.TELCOS.length; i++)
			{
				moload[i] = new AtomicLong();
				moprocess[i] = new AtomicLong();
			}

			System.out.println("Start :" + VERSION);

			// Lấy DefineMT trong DB
			DefineMT mDefineMT = new DefineMT(LocalConfig.mDBConfig_MSSQL);
			ListDefineMT = mDefineMT.GetAllMT();

			// Load các đối tượng dạng Từ điển
			Service mService = new Service(LocalConfig.mDBConfig_MSSQL);
			mListServiceObject = mService.GetAllService();

			Init();
		}
		catch (Exception e)
		{
			mLog.log.error(e);
		}

	}

	private void Init() throws Exception
	{
		// Tạo đối tượng lấy dữ liệu trong table Keyword_Config
		// Mỗi 1 phút lại lấy 1 lần
		mLoadKeyword = new ProcessLoadKeyword();
		mLoadKeyword.setPriority(Thread.MAX_PRIORITY);
		mLoadKeyword.start();

		System.out.println("Loading...");
		while (!mLoadKeyword.isLoaded)
		{
			try
			{
				sleep(50);
				System.out.print(".");
			}
			catch (InterruptedException e)
			{
				mLog.log.error(e);
			}
		}
		mLog.log.debug("Loaded.");

		// Lấy các queue đang xử lý trước đây (trường hợp ch trình bị tắt thì
		// các queue chưa xử lý sẽ được lưu vào 2 file .dat
		loadSMSDataTable("data.dat", queue);
		loadSMSDataTable("receivelog.dat", queueLog);

		mLog.log.debug("Start: LoadMO");

		for (int j = 0; j < loadMO.length; j++)
		{
			// Tọa đối tượng LoadMO để lấy các MO trong Table
			// sms_receive_queue
			// sau đó bỏ váo queue và xóa row đã lấy trong table
			// sms_receive_queue
			loadMO[j] = new LoadMO(queue, loadMO.length, j);
			loadMO[j].setPriority(Thread.MAX_PRIORITY);
			loadMO[j].start();
		}

		mLog.log.debug("Start: ExecuteQueue");
		for (int i = 0; i < executequeue.length; i++)
		{
			executequeue[i] = new ExecuteQueue(queue, queueLog, i);
			executequeue[i].setPriority(Thread.MAX_PRIORITY);
			executequeue[i].start();
		}

		mLog.log.debug("Start: ExecuteInsertReceiveLog");
		for (int i = 0; i < insert_receive_log.length; i++)
		{
			insert_receive_log[i] = new ExecuteInsertReceiveLog(queueLog);
			insert_receive_log[i].setPriority(Thread.NORM_PRIORITY);
			insert_receive_log[i].start();
		}

		isAllThreadStarted = true;

		CheckCharge mCheckCharge = new CheckCharge();
		mCheckCharge.setPriority(MAX_PRIORITY);
		mCheckCharge.start();

		// trafficthread = new TrafficWatcherThread();
		// trafficthread.start();

	}

	public void windowClosing()
	{
		int nCount = 0;
		getData = false;
		processData = false;

		System.out.print("\nWaiting .....");
		mLog.log.info("\nWaiting .....");

		try
		{
			Thread.sleep(500);

		}
		catch (InterruptedException ex)
		{
			System.out.println(ex.toString());
		}

		while ((queue.getSize() > 0) && nCount < 5)
		{
			nCount++;
			try
			{
				System.out.println("...Queue(" + queue.getSize() + ")");

				Thread.sleep(100);
			}
			catch (InterruptedException ex)
			{
				System.out.println(ex.toString());
			}
		}

		mLog.log.info("saveSMSDataTable(data.dat");
		System.out.println("saveSMSDataTable(data.dat");
		saveSMSDataTable("data.dat", queue);

		mLog.log.info("saveSMSDataTable(receivelog.dat");
		System.out.println("saveSMSDataTable(receivelog.dat");
		saveSMSDataTable("receivelog.dat", queueLog);

		mLog.log.info("Shutdown");

		System.out.print("\nExit");

	}

	/**
	 * Load các đối tượng nằm trong file .dat lên queue
	 * 
	 * @param fileName
	 * @param queue
	 */
	public static void loadSMSDataTable(String fileName, MsgQueue queue)
	{

		boolean flag = true;
		FileInputStream fin = null;
		ObjectInputStream objIn = null;
		FileOutputStream fout = null;
		mLog.log.info("loadSMSDataTable:" + fileName);
		long nummo = 0;
		try
		{

			fin = new java.io.FileInputStream(fileName);

			if (fin.available() <= 0)
			{
				mLog.log.info(fileName + " is empty");
				return;
			}

			objIn = new ObjectInputStream(fin);

			while (flag)
			{
				try
				{
					MsgObject object = (MsgObject) objIn.readObject();
					queue.add(object);
					nummo++;

				}
				catch (Exception ex)
				{
					flag = false;
				}
			}
			if (nummo == 0)
			{
				mLog.log.info(fileName + " is empty");
			}
			else
			{
				mLog.log.info("Load data successful: " + nummo + " MO");
			}

		}
		catch (IOException ex)
		{
			mLog.log.error("Load data error: " + ex.getMessage(), ex);
		}
		finally
		{
			try
			{
				fin.close();
				fout = new java.io.FileOutputStream(fileName, false); // append
				// =
				// false
				fout.close();
				mLog.log.info("Deleting.....: " + fileName);
			}
			catch (Exception ex)
			{

			}
		}

	}

	/**
	 * Lưu các đối tượng đang nằm trong queue xuống file .dat
	 * 
	 * @param fileName
	 * @param queue
	 */
	public static void saveSMSDataTable(String fileName, MsgQueue queue)
	{
		mLog.log.info("Saving " + fileName + " . . .");
		FileOutputStream fout = null;
		ObjectOutputStream objOut = null;
		long numqueue = 0;

		try
		{
			fout = new java.io.FileOutputStream(fileName, false); // append =
			// false
			objOut = new ObjectOutputStream(fout);
			for (Enumeration<?> e = queue.getVector().elements(); e.hasMoreElements();)
			{
				MsgObject object = (MsgObject) e.nextElement();
				objOut.writeObject(object);
				objOut.flush();
				numqueue++;
			}
			mLog.log.info("complete:" + numqueue);
		}
		catch (IOException ex)
		{
			mLog.log.error("Save data error: " + ex.getMessage(), ex);
		}
		finally
		{
			try
			{
				objOut.close();
				fout.close();
			}
			catch (IOException ex)
			{

			}
		}
	}

	public static void main(String[] args)
	{
		System.out.println("Starting ProcessServer - version " + VERSION);
		System.out.println("Copyright 2006-2008 NCL. - All Rights Reserved.");
		Program smsConsole = new Program();
		ShutdownInterceptor shutdownInterceptor = new ShutdownInterceptor(smsConsole);
		Runtime.getRuntime().addShutdownHook(shutdownInterceptor);
		smsConsole.start();

	}

	public void run()
	{

		try
		{
			if (LocalConfig.RUNCLASS != null)
			{
				for (int i = 0; i < LocalConfig.RUNCLASS.length; i++)
				{
					runthread(LocalConfig.RUNCLASS[i]);
				}

			}
			mLog.log.debug("Listen MO...............................................................");
		}

		catch (Exception e)
		{
			mLog.log.error(e);
			e.printStackTrace();
		}

	}

	private void runthread(String classname)
	{
		Class<?> delegateClass;
		try
		{
			delegateClass = Class.forName(classname);
			mLog.log.info("{runthread}{Start:" + classname + "}");
			Object delegateObject = null;
			try
			{
				delegateObject = delegateClass.newInstance();
			}
			catch (InstantiationException e)
			{
				mLog.log.error(e.toString(), e);

			}
			catch (IllegalAccessException e)
			{
				mLog.log.error(e.toString(), e);

			}
			Thread delegate = (Thread) delegateObject;

			delegate.start();
		}
		catch (ClassNotFoundException e)
		{
			mLog.log.error(e.toString(), e);
		}

	}

	public static void checkstatus_thread()
	{

		try
		{

			for (int i = 0; i < loadMO.length; i++)
			{

				if (loadMO[i].isInterrupted() || !loadMO[i].isAlive())
				{
					restartthread_loadmo(i);
				}
			}

			for (int i = 0; i < executequeue.length; i++)
			{
				if (executequeue[i].isInterrupted() || !executequeue[i].isAlive())
				{
					restartthread_executequeue(i);
				}
			}
			for (int i = 0; i < insert_receive_log.length; i++)
			{

				if (insert_receive_log[i].isInterrupted() || !insert_receive_log[i].isAlive())
				{
					restartthread_insert_receive_log(i);
				}
			}

		}
		catch (Exception e)
		{
			mLog.log.error(e.toString(), e);
		}

	}

	@SuppressWarnings("deprecation")
	public static void restartthread_insert_receive_log(int i)
	{
		mLog.log.info("insert_receive_log[" + i + "] is alive:" + insert_receive_log[i].isAlive()
				+ "@insert_receive_log[" + i + "] is Interrupted:" + insert_receive_log[i].isInterrupted());
		mLog.log.info("restart insert_receive_log[" + i + "]");
		insert_receive_log[i].stop();
		insert_receive_log[i] = new ExecuteInsertReceiveLog(queueLog);
		insert_receive_log[i].setPriority(Thread.NORM_PRIORITY);
		insert_receive_log[i].start();
	}

	public static void restartthread_executequeue(int i)
	{
		mLog.log.info("executequeue[" + i + "] is alive:" + executequeue[i].isAlive() + "@executequeue[" + i
				+ "] is Interrupted:" + executequeue[i].isInterrupted());
		mLog.log.info("restart executequeue[" + i + "]");
		executequeue[i].stop();
		executequeue[i] = new ExecuteQueue(queue, queueLog, i);
		executequeue[i].setPriority(Thread.MAX_PRIORITY);
		executequeue[i].start();
	}

	public static void restartthread_loadmo(int i)
	{
		mLog.log.info("loadMO[" + i + "] is alive:" + loadMO[i].isAlive() + "@loadMO[" + i + "] is Interrupted:"
				+ loadMO[i].isInterrupted());
		mLog.log.info("restart loadMO[" + i + "]");

		loadMO[i].stop();
		loadMO[i] = new LoadMO(queue, loadMO.length, loadMO[i].processindex);
		loadMO[i].setPriority(Thread.MAX_PRIORITY);
		loadMO[i].start();
	}

	public static void incrementAndGet_load(String operator)
	{

		String[] telcos = LocalConfig.TELCOS;
		for (int i = 0; i < telcos.length; i++)
		{
			if (operator.equalsIgnoreCase(telcos[i]))
			{
				moload[i].incrementAndGet();
			}
		}

	}

	public static void incrementAndGet_process(String operator)
	{
		String[] telcos = LocalConfig.TELCOS;

		for (int i = 0; i < telcos.length; i++)
		{
			if (operator.equalsIgnoreCase(telcos[i]))
			{
				moprocess[i].incrementAndGet();
			}
		}

	}

	public static long getAndSet_process(String operator)
	{
		String[] telcos = LocalConfig.TELCOS;
		for (int i = 0; i < telcos.length; i++)
		{
			if (operator.equalsIgnoreCase(telcos[i])) { return moprocess[i].getAndSet(0); }
		}
		return 0;

	}

	public static long getAndSet_load(String operator)
	{
		String[] telcos = LocalConfig.TELCOS;
		for (int i = 0; i < telcos.length; i++)
		{
			if (operator.equalsIgnoreCase(telcos[i])) { return moload[i].getAndSet(0); }
		}
		return 0;
	}

}
