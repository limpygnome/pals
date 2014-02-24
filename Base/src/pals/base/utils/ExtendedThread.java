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
    Version:    1.0
    Authors:    Marcus Craske           <limpygnome@gmail.com>
    ----------------------------------------------------------------------------
*/
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
