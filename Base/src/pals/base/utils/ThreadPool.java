/*
    The MIT License (MIT)

    Copyright (c) 2014 Marcus Craske <limpygnome@gmail.com>

    Permission is hereby granted, free of charge, to any person obtaining a copy
    of this software and associated documentation files (the "Software"), to deal
    in the Software without restriction, including without limitation the rights
    to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
    copies of the Software, and to permit persons to whom the Software is
    furnished to do so, subject to the following conditions:

    The above copyright notice and this permission notice shall be included in
    all copies or substantial portions of the Software.

    THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
    IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
    FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
    AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
    LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
    OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
    THE SOFTWARE.
    ----------------------------------------------------------------------------
    Authors:    Marcus Craske           <limpygnome@gmail.com>
    ----------------------------------------------------------------------------
*/
package pals.base.utils;

import java.util.ArrayList;

/**
 * A data-structure for handling a pool of threads.
 * 
 * Thread-safe.
 * 
 * @param <T> The data-type of the pool.
 * @version 1.0
 */
public class ThreadPool<T extends ExtendedThread>
{
    // Fields ******************************************************************
    private final ArrayList<T> threads;
    // Methods - Constructors **************************************************
    /**
     * Constructs a new instance.
     * 
     * @since 1.0
     */
    public ThreadPool()
    {
        threads = new ArrayList<>();
    }
    // Methods *****************************************************************
    /**
     * Starts all of the threads.
     * 
     * @since 1.0
     */
    public synchronized void start()
    {
        for(T thread : threads)
            thread.start();
    }
    /**
     * Stops all of the threads.
     * 
     * @since 1.0
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
     * 
     * @since 1.0
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
     * 
     * @since 1.0
     */
    public synchronized void interruptAll()
    {
        for(T thread : threads)
            thread.interrupt();
    }
    // Methods - Accessors *****************************************************
    /**
     * The size of the pool.
     * 
     * @return The size of the collection.
     * @since 1.0
     */
    public synchronized int size()
    {
        return threads.size();
    }
    /**
     * Fetches an item in the pool.
     * 
     * @param index The index of the item to retrieve.
     * @return The item at the index; null if the item cannot be found.
     * @since 1.0
     */
    public synchronized T get(int index)
    {
        return threads.get(index);
    }
    // Methods - Mutators ******************************************************
    /**
     * Clears all the threads in the collection; these threads are not stopped,
     * thus they may still be executing after this call.
     * 
     * @since 1.0
     */
    public synchronized void clear()
    {
        threads.clear();
    }
    /**
     * Adds a new thread to the pool.
     * 
     * @param thread The thread to add to the collection.
     * @since 1.0
     */
    public synchronized void add(T thread)
    {
        threads.add(thread);
    }
    /**
     * Removes a thread from the pool.
     * 
     * @param thread Removes the specified thread from the collection.
     * @since 1.0
     */
    public synchronized void remove(T thread)
    {
        threads.remove(thread);
    }
    /**
     * Removes a thread from the pool.
     * 
     * @param index Removes a thread at the specified index in the collection.
     * @since 1.0
     */
    public synchronized void remove(int index)
    {
        threads.remove(index);
    }
}
