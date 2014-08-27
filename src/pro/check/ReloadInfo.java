package pro.check;
import pro.server.LocalConfig;
import uti.utility.MyLogger;

public class ReloadInfo extends Thread
{

	MyLogger mLog = new MyLogger(LocalConfig.LogConfigPath,ReloadInfo.class.toString());

	public void run()
	{
		
	}
}
