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

import static org.junit.Assert.*;
import org.junit.Test;

/**
 * Tests {@link ThreadPool}.
 * 
 * @version 1.0
 */
public class ThreadPoolTest
{
    // Classes - Testing *******************************************************
    /**
     * A test thread class, which loops until an exit signal is raised.
     * 
     * @version 1.0
     */
    public class TptTestThread extends ExtendedThread
    {
        @Override
        public void run()
        {
            // Loop until stopped...
            while(!extended_isStopped())   
            {
                try
                {
                    Thread.sleep(1);
                }
                catch(InterruptedException ex)
                {
                }
            }
        }
    }
    /**
     * Tests the mutators and accessors.
     * 
     * @since 1.0
     */
    @Test
    public void testMutatorsAccessors()
    {
        ThreadPool<TptTestThread> tp = new ThreadPool<>();
        
        assertEquals(0, tp.size());
        assertNull(tp.get(0));
        
        tp.add(new TptTestThread());
        assertEquals(1, tp.size());
        tp.clear();
        assertEquals(0, tp.size());
        
        TptTestThread t = new TptTestThread();
        tp.add(t);
        assertEquals(1, tp.size());
        tp.remove(t);
        assertEquals(0, tp.size());
        tp.add(t);
        assertEquals(1, tp.size());
        tp.remove(1);
        assertEquals(1, tp.size());
        tp.remove(0);
        assertEquals(0, tp.size());
    }
    /**
     * Tests starting and stopping the thread-pool.
     * 
     * @since 1.0
     */
    @Test
    public void testStartStopJoin()
    {
        ThreadPool<TptTestThread> tp = new ThreadPool<>();
        
        TptTestThread tt = new TptTestThread();
        tp.add(tt);
        
        tp.start();
        assertTrue(tt.isAlive());
        
        tp.stop();
        
        try
        {
            Thread.sleep(5);
        }
        catch(InterruptedException ex)
        {
        }
        
        assertFalse(tt.isAlive());
        
        tp.clear();
        tp.add(tt = new TptTestThread());

        tp.start();
        assertTrue(tt.isAlive());
        
        tp.stopJoin();
        assertFalse(tt.isAlive());
    }
}
