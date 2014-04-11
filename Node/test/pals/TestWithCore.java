package pals;


import org.junit.AfterClass;
import static org.junit.Assert.assertTrue;
import org.junit.BeforeClass;
import pals.base.NodeCore;

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
/**
 * A simple helper class for tests requiring a NodeCore instance.
 * 
 * @version 1.0
 */
public class TestWithCore
{
    protected static NodeCore core;
    
    /**
     * Setups a core for the tests.
     * 
     * @since 1.0
     */
    @BeforeClass
    public static void setup()
    {
        core = NodeCore.getInstance();
        core.setPathPlugins("../Plugins");
        core.start();
        assertTrue(core.getState() == NodeCore.State.Started);
    }
    /**
     * Disposes the core.
     * 
     * @since 1.0
     */
    @AfterClass
    public static void dispose()
    {
        core.stop(NodeCore.StopType.Shutdown);
        assertTrue(core.getState() == NodeCore.State.Shutdown);
        core = null;
    }
}
