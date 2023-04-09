package tjacobs.io;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.net.ServerSocket;
import java.net.Socket;

import javax.swing.JLabel;

import tjacobs.CollectionUtils;
import tjacobs.ui.util.WindowUtilities;

/**
 * Provides easy Application persistance via the file system 
 * App also provides a mechanism to create an app callback,
 * so that repeatedly opening the application requires just one
 * instance of the program to run.
 * @see Main
 */
public class App extends Main {

	private AppStartCallBack mCallBackApp;
	
	private static final String PORT = "connect_port";
	private static final int minPort = 1024;	
	private ServerSocket mServerSock;

	public static interface AppStartCallBack {
		public void run(String[] args);
	}
	
	public App(String appName, AppStartCallBack callback, String[] args) {
		this (appName, callback);
		start(args);
	}
	
	public App(String appName, AppStartCallBack callback) {
		super(appName);
		super.setProjectName(appName);
		mCallBackApp = callback;
		loadProperties();
		loadData();
	}
		
	public boolean start(String args[]) {
		if (mCallBackApp != null) {
			if (getPropertyFile().exists()) {
				//main config file exists
				String port = getProperty(PORT);
				boolean result = false;
				if (port != null) {
					Socket s = null;
					try {
						s = new Socket("localhost", Integer.parseInt(port));
						PrintStream out = new PrintStream(s.getOutputStream());
						out.println(getProjectName());
						out.println("" + args.length);
						for (int i = 0; i < args.length; i++) {
							out.println(args[i]);
						}
						InputStream in = s.getInputStream();
						BufferedReader br = new BufferedReader(new InputStreamReader(in));
						String str = br.readLine();
						if (str != null && str.equalsIgnoreCase("y")) {
							return true;
						}
	 				}
					catch (IOException iox) {
						//iox.printStackTrace();
					}
					finally {
						try {
							if (s != null)
								s.close();
						}
						catch (IOException iox) {}
					}
					if (result) return true;
				}
			}
			mCallBackApp.run(args);
				//otherwise create the serversocket
			int port = getProperty(PORT) != null ? Integer.parseInt(getProperty(PORT)) : minPort;
			while (port < minPort + 100) {
				try {
					mServerSock = new ServerSocket(port);
					break;
				}
				catch (IOException iox) {
					port++;
				}
			}
			//System.out.println(sServeSock.getLocalPort());
			if (mServerSock != null) {
				setProperty(PORT, "" + mServerSock.getLocalPort());
				saveProperties();
				ServerRunner runner = new ServerRunner();
				Thread t = new Thread(runner);
				t.start();
			}
		}
		return false;
	}
	
	class ServerRunner implements Runnable {
		public void run() {
			while(true) {
				Socket s = null;
				try {
					s = mServerSock.accept();
					BufferedReader reader = new BufferedReader(new InputStreamReader(s.getInputStream()));
					String name = reader.readLine();
					if (name == null || (!name.equals(getProjectName()))) {
						continue;
					}
					int params = Integer.parseInt(reader.readLine());
					String args[] = new String[params];
					for (int i = 0; i < params; i++) {
						args[i] = reader.readLine();
					}
					mCallBackApp.run(args);
					PrintStream out = new PrintStream(s.getOutputStream());
					out.println("y");
				}
				catch (IOException iox) {
					iox.printStackTrace();
					return;
				}
				finally {
					try {
						if (s != null) s.close();
					}
					catch (IOException iox) {}
				}
			}
		}
	}
	
	public static void main(String[] args) {
		AppStartCallBack cb = new AppStartCallBack() {
			JLabel label;
			public void run(String args[]) {
				App a = (App) Main.getSingleton();
				a.loadProperties();
				CollectionUtils.printCollection(a.getProperties());
				a.setProperty("foo", "fubar foobar");
				a.saveProperties();
				if (label == null) {
					label = new JLabel("Hello World");
					WindowUtilities.visualize(label);
					return;
				}
				System.out.println("hello world!");
				for (int i = 0; i < args.length; i++) {
					System.out.println(args[i]);
				}
			}
		};
		new App("test", cb, args);
	}
}