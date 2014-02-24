package pals.base.utils;

import java.util.ArrayList;

/**
 * A data-structure for handling a pool of threads.
 * 
 * Thread-safe.
 * 
 * @param <T> The data-type of the pool.
 */
public class ThreadPool<T extends ExtendedThread>
{
    // Fields ******************************************************************
    private final ArrayList<T> threads;
    // Methods - Constructors **************************************************
    public ThreadPool()
    {
        threads = new ArrayList<>();
    }
    // Methods *****************************************************************
    /**
     * Starts all of the threads.
     */
    public synchronized void start()
    {
        for(T thread : threads)
            thread.start();
    }
    /**
     * Stops all of the threads.
     */
    public synchronized void stop()
    {
        for(T thread : threads)
            thread.extended_stop();
    }
    /**
     * Stops all of the threads, but the current thread joins each thread. Thus
     * invoking this method will cause the invoking thread to hang until all of
     * the threads stop execution.
     */
    public synchronized void stopJoin()
    {
        for(T thread : threads)
        {
            thread.extended_stop();
            try
            {
                thread.join();
            }
            catch(InterruptedException ex)
            {
            }
        }
    }
    /**
     * Interrupts all the threads held in the pool.
     */
    public synchronized void interruptAll()
    {
        for(T thread : threads)
            thread.interrupt();
    }
    // Methods - Accessors *****************************************************
    /**
     * @return The size of the collection.
     */
    public synchronized int size()
    {
        return threads.size();
    }
    /**
     * @param index The index of the item to retrieve.
     * @return The item at the index; null if the item cannot be found.
     */
    public synchronized T get(int index)
    {
        return threads.get(index);
    }
    // Methods - Mutators ******************************************************
    /**
     * Clears all the threads in the collection; these threads are not stopped,
     * thus they may still be executing after this call.
     */
    public synchronized void clear()
    {
        threads.clear();
    }
    /**
     * @param thread The thread to add to the collection.
     */
    public synchronized void add(T thread)
    {
        threads.add(thread);
    }
    /**
     * @param thread Removes the specified thread from the collection.
     */
    public synchronized void remove(T thread)
    {
        threads.remove(thread);
    }
    /**
     * @param index Removes a thread at the specified index in the collection.
     */
    public synchronized void remove(int index)
    {
        threads.remove(index);
    }
}
