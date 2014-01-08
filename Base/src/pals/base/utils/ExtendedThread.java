package pals.base.utils;

/**
 * An extended thread which allows communication of when to cease execution of
 * a thread; when the inheriting thread goes to cease execution, it should call
 * extended_reset.
 * 
 * Thread-safe.
 */
public class ExtendedThread extends Thread
{
    // Fields ******************************************************************
    private boolean stopped;
    // Methods - Constructors **************************************************
    public ExtendedThread()
    {
        stopped = false;
    }
    // Methods *****************************************************************
    /**
     * @return Indicates if the thread should stop execution.
     */
    public synchronized boolean extended_isStopped()
    {
        return stopped;
    }
    /**
     * Resets the thread's stop state; this should be called by the
     * inheriting thread.
     */
    protected synchronized void extended_reset()
    {
        stopped = false;
    }
    /**
     * Requests the thread to cease execution.
     */
    public synchronized void extended_stop()
    {
        stopped = true;
        interrupt();
    }
}
