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
package pals.base.web;

import org.junit.Test;
import static org.junit.Assert.*;

/**
 * Tests {@link RemoteRequest}.
 * 
 * @version 1.0
 */
public class RemoteRequestTest
{
    @Test
    public void testMutatorsAccessors()
    {
        RemoteRequest req = new RemoteRequest("session id", "relativeurl", "127.0.0.1");
        
        // Test constructor
        assertEquals("session id", req.getSessionID());
        assertEquals("relativeurl", req.getRelativeUrl());;
        assertEquals("127.0.0.1", req.getIpAddress());
        
        // Test main mutators
        req.setSessionID("test");
        assertEquals("test", req.getSessionID());
        
        req.setRelativeUrl("url");
        assertEquals("url", req.getRelativeUrl());
        
        // Test file mutators
        UploadedFile f = new UploadedFile(null, null, 0, null);
        assertEquals(0, req.getFilesCount());
        assertArrayEquals(new UploadedFile[0], req.getFiles());
        assertNotNull(req.getFilesMap());
        req.setFile("test file", f);
        assertEquals(f, req.getFile("test file"));
        assertArrayEquals(new String[]{"test file"}, req.getFileNames());
        assertArrayEquals(new UploadedFile[]{f}, req.getFiles());
        assertEquals(1, req.getFilesCount());
        assertNotNull(req.getFilesMap());
        req.removeFile("test file");
        assertEquals(0, req.getFilesCount());
        assertArrayEquals(new UploadedFile[0], req.getFiles());
        
        // Test field mutators
        assertNotNull(req.getFieldsMap());
        assertEquals(0, req.getFieldsCount());
        assertNull(req.getField("test"));
        assertNull(req.getFields("test"));
        
        assertFalse(req.containsField("test"));
        req.setField("test", "a");
        req.setAddFields("test", "b");
        assertNotNull(req.getFieldsMap());
        assertEquals("a", req.getField("test"));
        assertArrayEquals(new String[]{"a","b"}, req.getFields("test"));
        assertTrue(req.containsField("test"));
        assertArrayEquals(new String[]{"test"}, req.getFieldNames());
        assertEquals(1, req.getFieldsCount());
        req.removeField("test");
        assertEquals(0, req.getFieldsCount());
        assertNotNull(req.getFieldsMap());
    }
}
