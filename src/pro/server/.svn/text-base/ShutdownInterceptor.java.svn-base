package MyProcessServer;

public class ShutdownInterceptor extends Thread
{

	private ConsoleSRV app;

	public ShutdownInterceptor(ConsoleSRV app)
	{
		this.app = app;
	}

	public void run()
	{
		System.out.println("Call the shutdown routine");
		app.windowClosing();
	}
}
