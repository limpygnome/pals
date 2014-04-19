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
package pals.plugins.marking;

import pals.base.Logging;
import pals.base.NodeCore;
import pals.base.Plugin;
import pals.base.UUID;
import pals.base.assessment.InstanceAssignmentCriteria;
import pals.base.database.Connector;
import pals.base.utils.ExtendedThread;

/**
 * A worker thread for finding and delegating instances of criteria to be
 * marked.
 * 
 * @version 1.0
 */
public class ThreadMarker extends ExtendedThread
{
    // Fields ******************************************************************
    private final AssignmentMarker  am;         // Reference to the plugin.
    private final int               number;     // The # / number of this thread (for diagnostics/debugging).
    // Methods - Constructors **************************************************
    /**
     * Creates a new instance.
     * 
     * @param marker Plugin instance.
     * @param number The current number of the thread.
     * @since 1.0
     */
    public ThreadMarker(AssignmentMarker marker, int number)
    {
        this.am = marker;
        this.number = number;
    }
    
    // Methods *****************************************************************
    /**
     * @see Thread#run()
     * 
     * @since 1.0
     */
    @Override
    public void run()
    {
        // Wait for the core to start before starting...
        // -- We need all the plugins to load in...
        while(am.getCore().getState() == NodeCore.State.Starting && !extended_isStopped())
        {
            try
            {
                //marker.getCore().waitStateChange();
                Thread.sleep(100);
            }
            catch(InterruptedException ex)
            {
            }
        }
        if(extended_isStopped())
        {
            extended_reset();
            return;
        }
        
        // Create connection
        Connector conn = am.getCore().createConnector();
        if(conn == null)
            return;
        
        am.getCore().getLogging().log("Ass. Marker", "Thread #"+number+" started.", Logging.EntryType.Info);
        
        // Iterate until the thread is stopped, checking for work to be processed
        InstanceAssignmentCriteria iac;
        while(!extended_isStopped())
        {
            try
            {
                // Fetch work - will block until work is available
                iac = am.getWorkQueue().take();
                
                // Process work
                if(iac != null)
                {
                    // Delegate to the plugin responsible for marking
                    UUID plugin = iac.getQC().getCriteria().getUuidPlugin();
                    Plugin p = am.getCore().getPlugins().getPlugin(plugin);
                    if(p == null || !p.eventHandler_handleHook("criteria_type.mark", new Object[]{conn, am.getCore(), iac}))
                    {
                        // Set to manual marking, log the error
                        iac.setStatus(InstanceAssignmentCriteria.Status.AwaitingManualMarking);
                        iac.persist(conn);
                        if(p == null)
                            am.getCore().getLogging().log("Ass. Marker", "#"+number+": Criteria-type plugin, "+plugin.getHexHyphens()+", is not loaded in the run-time.", Logging.EntryType.Warning);
                        else
                            am.getCore().getLogging().log("Ass. Marker", "#"+number+": Plugin, "+plugin.getHexHyphens()+", did not handle criteria-type, "+iac.getQC().getCriteria().getUuidCType().getHexHyphens()+".", Logging.EntryType.Warning);
                    }
                    else
                    {
                        am.getCore().getLogging().log("Ass. Marker", "#"+number+": Marked criteria '"+iac.getIAQ().getAIQID()+"','"+iac.getQC().getQCID()+"' ~ "+iac.getMark()+"%.", Logging.EntryType.Info);
                        // Set IA to be checked for mark computation
                        am.addInstanceAssignmentMarking(iac.getIAQ().getInstanceAssignment());
                    }
                }
            }
            catch(InterruptedException ex)
            {
            }
            catch(Exception ex)
            {
                am.getCore().getLogging().logEx("Ass. Marker", "Thread #"+number+" encountered exception during marking.", ex, Logging.EntryType.Warning);
            }
        }
        
        // Dispose connector
        conn.disconnect();
        am.getCore().getLogging().log("Ass. Marker", "Thread #"+number+" ending execution.", Logging.EntryType.Info);
    }
}
