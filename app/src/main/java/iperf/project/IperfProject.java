package iperf.project;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import iperf.project.R;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.net.nsd.NsdManager;
import android.net.nsd.NsdServiceInfo;
import android.net.wifi.WifiManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.text.Editable;
import android.text.TextWatcher;
import android.text.format.Formatter;
import android.util.Log;
import android.view.View;
import android.view.animation.AnimationUtils;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;
import android.widget.ViewFlipper;

//Main class of the activity
public class IperfProject extends Activity {
	
	String txtFileName = "iperf.txt";

	private String mServiceName="Iperf";
	private String mServiceType = "_http._tcp.";

	private NsdManager mNsdManager;

    public ArrayList<String> peers=new ArrayList<String>();
	public ArrayList<String> donePeers=new ArrayList<String>();

	public int threads=0;

	public boolean repeat=false;

	public	long startTime=System.currentTimeMillis();

	public TextView mainScreen;

    public IperfProject() {
	}

	//A global pointer for instances of of iperf (only one at a time is allowed). 
	IperfTask iperfTask = null;
	
	//This is a required method that implements the activity startup
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		//Shows the logo screen, and waits for a tap to continue to the main screen
		setContentView(R.layout.iperf_activity);
		//Get Wifi connection information and displays it in the main screen
		getCurrentIP();

        mNsdManager = (NsdManager)getSystemService(Context.NSD_SERVICE);

        mainScreen=(TextView)findViewById(R.id.NsdText);

        registerService(9000);

        Log.d("SurajRana", "Service discovery started : ");

        //An instance of WifiManger is used to retrieve connection info.
        WifiManager wim = (WifiManager) getSystemService(WIFI_SERVICE);

//        if (wim.getConnectionInfo() != null) {
//            if ((wim.getConnectionInfo().getIpAddress()) != 0) {
//                mainScreen.append("Your IP address is: "
//                        + Formatter.formatIpAddress(wim.getConnectionInfo()
//                        .getIpAddress()));
//            } else {
//                mainScreen.append("Error: a wifi connection cannot be detected.");
//            }
//        } else {
//            mainScreen.append("Error: a wifi connection cannot be detected.");
//        }


        mNsdManager.discoverServices(
				mServiceType, NsdManager.PROTOCOL_DNS_SD, mDiscoveryListener);

        initIperfMain();

		TextView clientText=(TextView) findViewById(R.id.OutputText);
		TextView tv1=(TextView) findViewById(R.id.ServerText);


		IperfTask server=new IperfTask("-s -u",tv1);
		server.execute();

		TimerTask timer=new TimerTask();
		timer.execute();


    }




	class TimerTask extends AsyncTask<Void, String, String> {



		protected String doInBackground(Void... voids) {


			while(true)
			{
				if(!repeat)
				{
					try {
						Thread.sleep(120000);
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
					repeat=true;


				}
				else
				{
					Log.d("Suraj", "You're it");
					Log.d("suraj",threads+"");
					publishProgress("suraj");
					repeat=false;
				}
			}

		}

		@Override
		public void onProgressUpdate(String... strings) {
			//The next command is used to roll the text to the bottom
			Log.d("Suraj", "Getting it");

			Toast.makeText(getApplicationContext(),"Resetting", Toast.LENGTH_LONG).show();
			TextView output=(TextView)findViewById(R.id.OutputText);
			TextView nsd=(TextView)findViewById(R.id.NsdText);

			nsd.setText("");
			output.setText("");

			for(String a:peers) {

				IperfTask client = new IperfTask("-c " + a+ " -u", output);
				nsd.append("-"+a+"\n");
				client.execute();
			}
		}

	}

	public void initIperfMain() {
        final TextView tv = (TextView) findViewById(R.id.OutputText);
        InputStream in;
        try {
            //The asset "iperf" (from assets folder) inside the activity is opened for reading.
            in = getResources().getAssets().open("iperf");
        } catch (IOException e2) {
            tv.append("\nError occurred while accessing system resources, please reboot and try again.");
            return;
        }
        try {
            //Checks if the file already exists, if not copies it.
            new FileInputStream("/data/data/iperf.project/iperf");
        } catch (FileNotFoundException e1) {
            try {
                //The file named "iperf" is created in a system designated folder for this application.
                OutputStream out = new FileOutputStream("/data/data/iperf.project/iperf", false);
                // Transfer bytes from "in" to "out"
                byte[] buf = new byte[1024];
                int len;
                while ((len = in.read(buf)) > 0) {
                    out.write(buf, 0, len);
                }
                in.close();
                out.close();
                //After the copy operation is finished, we give execute permissions to the "iperf" executable using shell commands.
                Process processChmod = Runtime.getRuntime().exec("/system/bin/chmod 744 /data/data/iperf.project/iperf");
                // Executes the command and waits untill it finishes.
                processChmod.waitFor();
            } catch (IOException e) {
                tv.append("\nError occurred while accessing system resources, please reboot and try again.");
                return;
            } catch (InterruptedException e) {
                tv.append("\nError occurred while accessing system resources, please reboot and try again.");
                return;
            }

        }
    }

    public void registerService(int port) {
        // Create the NsdServiceInfo object, and populate it
        NsdServiceInfo serviceInfo  = new NsdServiceInfo();


        // The name is subject to change based on conflicts
        // with other services advertised on the same network.
        serviceInfo.setServiceName("IPChat");
        serviceInfo.setServiceType("_http._tcp.");
        serviceInfo.setPort(port);



        mNsdManager.registerService(
                serviceInfo, NsdManager.PROTOCOL_DNS_SD, mRegistrationListener);
    }


    NsdManager.DiscoveryListener mDiscoveryListener = new NsdManager.DiscoveryListener() {

        // Called as soon as service discovery begins.
        @Override
        public void onDiscoveryStarted(String regType) {
            Log.d("SurajRana", "Service discovery started");
        }

        @Override
        public void onServiceFound(NsdServiceInfo service) {
            // A service was found! Do something with it.
            Log.d("SurajRana", "Service discovery success : " + service);
            Log.d("SurajRana", "Host = "+ service.getServiceName());
            Log.d("SurajRana", "port = " + String.valueOf(service.getPort()));

			if(!service.getServiceName().contains("IPChat"))
			{
				Log.d("SurajRana", "Unknown Service: " + service.getServiceName());
				return;
			}

            if (!service.getServiceType().equals(mServiceType)) {
                // Service type is the string containing the protocol and
                // transport layer for this service.
                Log.d("SurajRana", "Unknown Service Type: " + service.getServiceType());
            } else if (service.getServiceName().equals(mServiceName)) {
                // The name of the service tells the user what they'd be
                // connecting to. It could be "Bob's Chat App".
                Log.d("SurajRana", "Same machine: " + mServiceName);
            } else {
                Log.d("SurajRana", "Diff Machine : " + service.getServiceName());
                // connect to the service and obtain serviceInfo
                mNsdManager.resolveService(service, new myResolveListener());
            }
        }

        @Override
        public void onServiceLost(NsdServiceInfo service) {
            // When the network service is no longer available.
            // Internal bookkeeping code goes here.
            Log.e("SurajRana", "service lost" + service);
        }

        @Override
        public void onDiscoveryStopped(String serviceType) {
            Log.i("SurajRana", "Discovery stopped: " + serviceType);
        }

        @Override
        public void onStartDiscoveryFailed(String serviceType, int errorCode) {
            Log.e("SurajRana", "Discovery failed: Error code:" + errorCode);
            mNsdManager.stopServiceDiscovery(this);
        }

        @Override
        public void onStopDiscoveryFailed(String serviceType, int errorCode) {
            Log.e("SurajRana", "Discovery failed: Error code:" + errorCode);
            mNsdManager.stopServiceDiscovery(this);
        }
    };


    private class myResolveListener implements NsdManager.ResolveListener {

        @Override
        public void onResolveFailed(NsdServiceInfo serviceInfo, int errorCode) {
            // Called when the resolve fails. Use the error code to debug.
            Log.e("SurajRana", "Resolve failed " + errorCode);
            Log.e("SurajRana", "serivce = " + serviceInfo);
        }

        @Override
        public void onServiceResolved(NsdServiceInfo serviceInfo) {
            Log.d("SurajRana", "Resolve Succeeded. " + serviceInfo);

            if (serviceInfo.getServiceName().equals(mServiceName)) {
                Log.d("SurajRana", "Same IP.");
                return;
            }

            // Obtain port and IP
            String hostPort = serviceInfo.getPort()+"";
            final String hostAddress = serviceInfo.getHost()+"";

            if(peers.contains(hostAddress.substring(1)))
            {
                Log.d("SurajRana","Seen this Machine");
                return;
            }

            runOnUiThread(new Runnable() {
                public void run() {
                    String temp=mainScreen.getText().toString();
                    mainScreen.setText(temp+"\n"+hostAddress);
                }
            });

			TextView clientText=(TextView)findViewById(R.id.OutputText);

			repeat=false;
			startTime=System.currentTimeMillis();

			IperfTask client=new IperfTask("-c "+hostAddress.substring(1)+" -u",clientText);
			client.execute();


            peers.add(hostAddress.substring(1));

        }
    }



    NsdManager.RegistrationListener mRegistrationListener = new NsdManager.RegistrationListener() {

        @Override
        public void onServiceRegistered(NsdServiceInfo NsdServiceInfo) {
            // Save the service name.  Android may have changed it in order to
            // resolve a conflict, so update the name you initially requested
            // with the name Android actually used.
            mServiceName = NsdServiceInfo.getServiceName();
        }

        @Override
        public void onRegistrationFailed(NsdServiceInfo serviceInfo, int errorCode) {
            // Registration failed!  Put debugging code here to determine why.
        }

        @Override
        public void onServiceUnregistered(NsdServiceInfo arg0) {
            // Service has been unregistered.  This only happens when you call
            // NsdManager.unregisterService() and pass in this listener.
        }

        @Override
        public void onUnregistrationFailed(NsdServiceInfo serviceInfo, int errorCode) {
            // Unregistration failed.  Put debugging code here to determine why.
        }
    };


    //Used to retrieve WiFi connection information and check if a connection exists. If not, display an error on the main screen, otherwise displays the local IP.
	public void getCurrentIP() {
		final TextView ip = (TextView) findViewById(R.id.ip);
		//An instance of WifiManger is used to retrieve connection info.
		WifiManager wim = (WifiManager) getSystemService(WIFI_SERVICE);
		if (wim.getConnectionInfo() != null) {
			if ((wim.getConnectionInfo().getIpAddress()) != 0) {
				//IP is parsed into readable format
				ip.append("Your IP address is: "
						+ Formatter.formatIpAddress(wim.getConnectionInfo()
								.getIpAddress()));
			} else {
				ip.append("Error: a wifi connection cannot be detected.");
			}
		} else {
			ip.append("Error: a wifi connection cannot be detected.");
		}
	}


	//This function is used to copy the iperf executable to a directory which execute permissions for this application, and then gives it execute permissions.
	//It runs on every initiation of an iperf test, but copies the file only if it's needed.
	public void initIperf() {
		final TextView tv = (TextView) findViewById(R.id.OutputText);
		InputStream in;
		try {
			//The asset "iperf" (from assets folder) inside the activity is opened for reading.
			in = getResources().getAssets().open("iperf");
		} catch (IOException e2) {
			tv.append("\nError occurred while accessing system resources, please reboot and try again.");
			return;			
		}
		try {
			//Checks if the file already exists, if not copies it.
			new FileInputStream("/data/data/iperf.project/iperf");
		} catch (FileNotFoundException e1) {
			try {
				//The file named "iperf" is created in a system designated folder for this application.
				OutputStream out = new FileOutputStream("/data/data/iperf.project/iperf", false); 
				// Transfer bytes from "in" to "out"
				byte[] buf = new byte[1024];
				int len;
				while ((len = in.read(buf)) > 0) {
					out.write(buf, 0, len);
				}
				in.close();
				out.close();
				//After the copy operation is finished, we give execute permissions to the "iperf" executable using shell commands.
				Process processChmod = Runtime.getRuntime().exec("/system/bin/chmod 744 /data/data/iperf.project/iperf"); 
				// Executes the command and waits untill it finishes.
				processChmod.waitFor();
			} catch (IOException e) {
				tv.append("\nError occurred while accessing system resources, please reboot and try again.");
				return;
			} catch (InterruptedException e) {
				tv.append("\nError occurred while accessing system resources, please reboot and try again.");
				return;
			}		
			//Creates an instance of the class IperfTask for running an iperf test, then executes.
			final EditText inputCommands = (EditText) findViewById(R.id.InputCommands);

			iperfTask = new IperfTask(inputCommands.getText().toString());
			iperfTask.execute();				
			return;					
		} 
		//Creates an instance of the class IperfTask for running an iperf test, then executes.
		final EditText inputCommands = (EditText) findViewById(R.id.InputCommands);
		iperfTask = new IperfTask(inputCommands.getText().toString());
		iperfTask.execute();
		return;
	}

	//This method is used to handle toggle button clicks
	public void ToggleButtonClick(View v) {
		final ToggleButton toggleButton = (ToggleButton) findViewById(R.id.toggleButton);
		final EditText inputCommands = (EditText) findViewById(R.id.InputCommands);
		//If the button is not pushed (waiting for starting a test), then a iperf task is started.
		if (toggleButton.isChecked()) {
			InputMethodManager mgr = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
			mgr.hideSoftInputFromWindow(inputCommands.getWindowToken(), 0);
			initIperf();
		//If a test is already running then a cancel command is issued through the iperfTask interface.
		} else {
			if (iperfTask == null){
				toggleButton.setChecked(false);
				return;
			}
			iperfTask.cancel(true);
			iperfTask = null;
		}
	}

	//This method is used to handle the save button click
	public void SaveButtonClick(View v) {
		final TextView tv = (TextView) findViewById(R.id.OutputText);
		
		//Create a dialog for filename input
		final AlertDialog.Builder alert = new AlertDialog.Builder(this);
		final EditText input = new EditText(this);
		input.setHint("Please enter a filename");
		alert.setView(input);
		alert.setPositiveButton("Save", new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int whichButton) {
				String value = input.getText().toString().trim();
				try {
					//Save file on SD card
				    File sdroot = Environment.getExternalStorageDirectory();
				    if (sdroot.canWrite()){
				        File txtfile = new File(sdroot, (value + ".txt"));
				        FileWriter txtwriter = new FileWriter(txtfile);
				        BufferedWriter out = new BufferedWriter(txtwriter);
				        out.write(tv.getText().toString());
				        out.close();
						tv.append("\nLog file saved to SD card.");		 
				    }
				} catch (IOException e) {
					tv.append("\nError occurred while saving log file, please check the SD card.");		    
				}
			}
		});

		alert.setNegativeButton("Cancel",
				new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int whichButton) {
						dialog.cancel();
					}
				});
		alert.show();
		

	}
	
	
	//This is used to switch from the logo screen to the main screen with animation.
	public void SkipWelcome(View v) {
		ViewFlipper switcher = (ViewFlipper) findViewById(R.id.viewSwitcher);
		switcher.setAnimation(AnimationUtils.loadAnimation(v.getContext(), R.anim.fade));
		switcher.showNext();
	}
	//This is used to switch from the main screen to the help screen with animation.
	public void GoToHelp(View v) {
		ViewFlipper switcher = (ViewFlipper) findViewById(R.id.viewSwitcher);
		final EditText inputCommands = (EditText) findViewById(R.id.InputCommands);
		InputMethodManager mgr = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
		mgr.hideSoftInputFromWindow(inputCommands.getWindowToken(), 0);
		switcher.setAnimation(AnimationUtils.loadAnimation(v.getContext(), R.anim.fade));
		switcher.showNext();

	}
	
	//This is used to switch from the help screen to the main screen with animation.
	public void ReturnFromHelp(View v) {
		ViewFlipper switcher = (ViewFlipper) findViewById(R.id.viewSwitcher);
		switcher.setAnimation(AnimationUtils.loadAnimation(v.getContext(), R.anim.fade));
		switcher.showPrevious();
	}


	//The main class for executing iperf instances.
	//With every test started, an instance of this class is created, and is destroyed when the test is done.
	//This class extends the class AsyncTask which is used to perform long background tasks and allow updates to the GUI while running.
	//This is done by overriding certain functions that offer this functionality.
	class IperfTask extends AsyncTask<Void, String, String> {


		TextView tv ;
		final ScrollView scroller = (ScrollView) findViewById(R.id.Scroller);
		final EditText inputCommands = (EditText) findViewById(R.id.InputCommands);
		final ToggleButton toggleButton = (ToggleButton) findViewById(R.id.toggleButton);
		public String command;

		IperfTask(String command)
		{
			this.command=command;
			threads++;
		}

		IperfTask(String command,TextView tv)
		{
			this.command=command;
			this.tv=tv;
			threads++;
		}

		Process process = null;

		//This function is used to implement the main task that runs on the background.
		@Override
		protected String doInBackground(Void... voids) {
			//Iperf command syntax check using a Regular expression to protect the system from user exploitation.

			String str = command;
            if(str.equals(""))
            {
                str="-s -u";
            }

			if (!str.matches("(iperf )?((-[s,-server])|(-[c,-client] ([01]?\\d\\d?|2[0-4]\\d|25[0-5])\\.([01]?\\d\\d?|2[0-4]\\d|25[0-5])\\.([01]?\\d\\d?|2[0-4]\\d|25[0-5])\\.([01]?\\d\\d?|2[0-4]\\d|25[0-5]))|(-[c,-client] \\w{1,63})|(-[h,-help]))(( -[f,-format] [bBkKmMgG])|(\\s)|( -[l,-len] \\d{1,5}[KM])|( -[B,-bind] \\w{1,63})|( -[r,-tradeoff])|( -[v,-version])|( -[N,-nodelay])|( -[T,-ttl] \\d{1,8})|( -[U,-single_udp])|( -[d,-dualtest])|( -[w,-window] \\d{1,5}[KM])|( -[n,-num] \\d{1,10}[KM])|( -[p,-port] \\d{1,5})|( -[L,-listenport] \\d{1,5})|( -[t,-time] \\d{1,8})|( -[i,-interval] \\d{1,4})|( -[u,-udp])|( -[b,-bandwidth] \\d{1,20}[bBkKmMgG])|( -[m,-print_mss])|( -[P,-parallel] d{1,2})|( -[M,-mss] d{1,20}))*"))
			{
				publishProgress("Error: invalid syntax. Please try again.\n\n");
				return null;
			}
			try {
				//The user input for the parameters is parsed into a string list as required from the ProcessBuilder Class.
				String[] commands = str.split(" ");
				List<String> commandList = new ArrayList<String>(Arrays.asList(commands));
				//If the first parameter is "iperf", it is removed
				if (commandList.get(0).equals((String)"iperf")) {
					commandList.remove(0);
				}
				//The execution command is added first in the list for the shell interface.
				commandList.add(0,"/data/data/iperf.project/iperf");
				//The process is now being run with the verified parameters.
				process = new ProcessBuilder().command(commandList).redirectErrorStream(true).start();
				//A buffered output of the stdout is being initialized so the iperf output could be displayed on the screen.
				BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
				int read;
				//The output text is accumulated into a string buffer and published to the GUI
				char[] buffer = new char[4096];
				StringBuffer output = new StringBuffer();
				while ((read = reader.read(buffer)) > 0) {
					output.append(buffer, 0, read);
					//This is used to pass the output to the thread running the GUI, since this is separate thread.
					publishProgress(output.toString());
					output.delete(0, output.length());
				}
				reader.close();
				process.destroy();
			}
			catch (IOException e) {
				publishProgress("\nError occurred while accessing system resources, please reboot and try again.");
				e.printStackTrace();
			}
			return null;
		}

		//This function is called by AsyncTask when publishProgress is called.
		//This function runs on the main GUI thread so it can publish changes to it, while not getting in the way of the main task.
		@Override
		public void onProgressUpdate(String... strings) {
			if(tv==null)
			{
				tv= (TextView) findViewById(R.id.OutputText);
			}
			tv.append(strings[0]);
			//The next command is used to roll the text to the bottom
			scroller.post(new Runnable() {
				public void run() {
					scroller.smoothScrollTo(0, tv.getBottom());
				}
			});

			if(tv.getId()==(R.id.ServerText))
			{
				int x=-1;
				if((x=strings[0].indexOf("connected with"))>-1)
				{
					String ip=strings[0].substring(x+15).split(" ")[0];
					String port=strings[0].substring(x+15).split(" ")[2];
					Log.d("SurajIP",ip);
					Log.d("SurajPort",port);
					if(!peers.contains(ip))
					{
						peers.add(ip);

						TextView clientText=(TextView)findViewById(R.id.OutputText);
						TextView peersText=(TextView)findViewById(R.id.NsdText);
						peersText.append("\n"+ip);

						repeat=false;
						startTime=System.currentTimeMillis();

						IperfTask client=new IperfTask("-c "+ip+" -u",clientText);
						client.execute();

					}

				}
			}
		}

		//This function is called by the AsyncTask class when IperfTask.cancel is called.
		//It is used to terminate an already running task.
		@Override
		public void onCancelled() {
			//The running process is destroyed and system resources are freed.
			if (process != null) {
				process.destroy();
				try {
					process.waitFor();
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}

			threads--;
			//The toggle button is switched to "off"
			toggleButton.setChecked(false);
			tv.append("\nOperation aborted.\n\n");
			//The next command is used to roll the text to the bottom
			scroller.post(new Runnable() {
				public void run() {
					scroller.smoothScrollTo(0, tv.getBottom());
				}
			});
		}

		@Override
		public void onPostExecute(String result) {
			//The running process is destroyed and system resources are freed.
			Log.d("Suraj Worried","Process Destroyed");

			threads--;
			if (process != null) {
				process.destroy();
			
				try {
					process.waitFor();
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
				tv.append("\nTest is done.\n\n");
			}
			//The toggle button is switched to "off"
			toggleButton.setChecked(false);
			//The next command is used to roll the text to the bottom
			scroller.post(new Runnable() {
				public void run() {
					scroller.smoothScrollTo(0, tv.getBottom());
				}
			});
		}
	}



    @Override
    protected void onPause() {
        try {
            if (mNsdManager != null) {
                mNsdManager.unregisterService(mRegistrationListener);
            }
        }
        catch(IllegalArgumentException e)
        {

        }
        super.onPause();
    }

    @Override
    protected void onResume() {
        super.onResume();
        try{
            if (mNsdManager != null)
            {
                registerService(9000);
            }
        }
        catch(IllegalArgumentException e)
        {

        }

    }

    @Override
    protected void onDestroy() {
        if (mNsdManager != null) {
            mNsdManager.unregisterService(mRegistrationListener);
        }
        super.onDestroy();
    }

}
