using System;
using System.Collections.Generic;
using System.IO;
using System.Text;
using System.Diagnostics;
using System.Security;
using System.Threading;

// Arguments
// -- 0: username
// -- 1: password
// -- 2: max-time in m/s before killed / process time-out
// -- 3: filename
// -- 4: args <optional>
//
namespace WindowsUserTool
{
    class Program
    {
        private static Process proc = null;
        private static Thread threadInput;
        
        static void Main(string[] args)
        {
            try
            {
                if (args.Length != 4 && args.Length != 5)
                {
                    Console.WriteLine("WindowsUserTool: invalid arguments.");
                    return;
                }
                // Parse arguments and prepare credentials
                // -- Username
                String username = args[0];
                // -- Password
                String passwordTemp = args[1];
                SecureString password = new SecureString();
                foreach (char c in passwordTemp)
                    password.AppendChar(c);
                // -- Timeout of process
                int timeout;
                if (!int.TryParse(args[2], out timeout) || timeout <= 0)
                {
                    Console.WriteLine("WindowsUserTool: invalid time-out period.");
                    return;
                }
                // Create process
                proc = new Process();
                proc.StartInfo.UserName = username;
                proc.StartInfo.Password = password;
                proc.StartInfo.FileName = args[3];
                proc.StartInfo.WorkingDirectory = Path.GetDirectoryName(args[3]);
                proc.StartInfo.RedirectStandardInput = true;
                proc.StartInfo.RedirectStandardOutput = true;
                proc.StartInfo.RedirectStandardError = true;
                if (args.Length == 5)
                    proc.StartInfo.Arguments = args[4].Replace("\\\"", "\"");
                proc.StartInfo.UseShellExecute = false;
                // Add hooks for standard in/out/err
                proc.ErrorDataReceived += new DataReceivedEventHandler(proc_ErrorDataReceived);
                proc.OutputDataReceived += new DataReceivedEventHandler(proc_OutputDataReceived);
                // Launch the process
                proc.Start();
                proc.BeginErrorReadLine();
                proc.BeginOutputReadLine();
                // Start input thread
                threadInput = new Thread(delegate()
                {
                    while (true)
                    {
                        proc.StandardInput.WriteLine(Console.ReadLine());
                    }
                });
                threadInput.Start();
                // Wait for the process to exit
                long time = 0;
                while (!proc.HasExited && time < timeout)
                {
                    Thread.Sleep(10);
                    time += 10;
                }
                if (!proc.HasExited)
                    proc.Kill();
                threadInput.Abort();
                // Kill the application - easier than using Win32 hacks to close the I/O stream
                // -- If not the program will hang-open until a key is pressed due to the input thread
                Environment.Exit(0);
            }
            catch (Exception ex)
            {
                // This is completely as a fail-safe.
                Console.WriteLine("WindowsUserTool: exception '"+ex.GetType().Name+"' occurred ~ '"+ex.Message+"'.");
            }
        }
        static void proc_OutputDataReceived(object sender, DataReceivedEventArgs e)
        {
            Console.WriteLine(e.Data);
        }
        static void proc_ErrorDataReceived(object sender, DataReceivedEventArgs e)
        {
            Console.Error.WriteLine(e.Data);
        }
    }
}
