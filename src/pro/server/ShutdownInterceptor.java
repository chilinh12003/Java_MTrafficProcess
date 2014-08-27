package pro.server;

public class ShutdownInterceptor extends Thread
{

	private Program app;

	public ShutdownInterceptor(Program app)
	{
		this.app = app;
	}

	public void run()
	{
		System.out.println("Call the shutdown routine");
		app.windowClosing();
	}
}
