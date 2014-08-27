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
import java.util.Vector;

import uti.utility.MyLogger;

public class MsgQueue
{
	MyLogger mLog = new MyLogger(LocalConfig.LogConfigPath,this.getClass().toString());
	
	// Queue for ACK message
	protected Vector<Object> queue;

	public MsgQueue()
	{
		queue = new Vector<Object>();
	}

	public Object remove()
	{
		synchronized (queue)
		{
			while (queue.isEmpty())
			{ // Threads are blocked
				try
				{ // if the queue is empty.
					queue.wait(); // wait until other thread call notify().
				}
				catch (InterruptedException ex)
				{
					mLog.log.error(ex);
				}
			}
			Object item = queue.firstElement();
			queue.removeElement(item);
			return item;
		}
	}

	public void add(Object item)
	{
		synchronized (queue)
		{
			queue.addElement(item);
			queue.notify();
		}
	}

	public long getSize()
	{
		return queue.size();
	}

	public boolean isEmpty()
	{
		return queue.isEmpty();
	}

	public void setVector(Vector<Object> v)
	{
		this.queue = v;
	}

	public Vector<Object> getVector()
	{
		return this.queue;
	}
}
