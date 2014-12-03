package chaski.api;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.Enumeration;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.apache.http.conn.util.InetAddressUtils;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.NetworkInfo.State;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Binder;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.RemoteException;
import android.util.Log;
import chaski.api.util.Util;
import chaski.api.util.networking.ConnectToSSIDRunnable;
import chaski.api.util.networking.ConnectionsCacheHelper;

public class ChaskiService extends Service implements IChaskiService{
	
	
	private static final String GET_WIFI_AP_CONFIGURATION = "getWifiApConfiguration";
	public static final int HANDLE_CONNECTION_TO_NETWORK_STATE = 0;
	public static final int HANDLE_CONNECTION_COUNT_CHANGE = 1;
	
	//Used to catch underlying OS action string when OS broadcast AP change
	private static final String ANDROID_WIFI_AP_STATE_CHANGED_ACTION = "android.net.wifi.WIFI_AP_STATE_CHANGED";
	
	//Hidden method names in android.net.wifi.WifiManager
	private static final String GET_WIFI_AP_STATE = "getWifiApState";
	private static final String SET_WIFI_AP_CONFIGURATION = "setWifiApConfiguration";
	private static final String SET_WIFI_AP_ENABLED = "setWifiApEnabled";
	
	private static final String TAG = ChaskiService.class.getName();
	
	private int mNetworkId;
	
	private boolean mAlreadyConfigured;
	
	private int mConnectionCount;
	
	private ChaskiServiceHandler mHandler;
	
	private WifiManager mWifiManager;
	
	private ConnectivityManager mConnectivityManager;
	
	private String mSSID;
	
	private WifiConfiguration mWifiApConfig;
	
	//Flag to indicate if AP shall be updated or closed
	//This is necessary as below 4.0 updateApConfig does not work, 
	//so we need to close AP and open AP.
	protected boolean mUpdateSSIDFlag;	
	
	private final IBinder mBinder = new MyBinder();

	private ScheduledThreadPoolExecutor stpeConnectToSSID;
	private ConnectToSSIDRunnable connectToSSIDThread;
	
	private ScheduledThreadPoolExecutor stpeReadArpCache;
	
	static class ChaskiServiceHandler extends Handler{
		
		private ChaskiService lChaskiService;

		public ChaskiServiceHandler(ChaskiService chaskiService){
			lChaskiService = chaskiService; 
		}
		
		@Override
		public void handleMessage(Message msg) {
			Bundle bundle = msg.getData();
			
			switch(msg.what){
			
			case 
				HANDLE_CONNECTION_TO_NETWORK_STATE: {
				Log.d(TAG,
						"mHandler.handleMessage(): HANDLE_REGISTERED_CLIENT_CONNECTIONS");
	
				boolean networkIDAdded = bundle.containsKey(Util.KEY_NETWORK_ID); 
	
				lChaskiService.mNetworkId = bundle
						.getInt(Util.KEY_NETWORK_ID); // later needed in client
														// notification to
														// indicate if WiFi
														// configuration was
														// available before
	
				lChaskiService.mAlreadyConfigured = bundle
						.getBoolean(Util.KEY_NETWORK_ALREADY_CONFIGURED); 
				
				//###make this member variable and check in handleConnected
				//also set flag
				String lSSID = bundle.getString(ChaskiConstants.KEY_SSID);
	
				if (networkIDAdded) {
					Log.i(TAG,
							"Connected to network: " + lSSID);
	
					lChaskiService.stpeConnectToSSID.shutdownNow();
					
					///no need to send broadcast at this point, 
					//as broadcast receiver will forward connection state broadcast
					//TODO Amro but check if code jumps in here first to check if relevant network was connected
					Log.d(TAG, "Network " +lSSID+ " enabled.");
	
				} else {
					Log.i(TAG, "Enabling of network: " +lSSID+ " failed.");
					
					String errorMsg = bundle.getString(ChaskiConstants.KEY_CONNECTION_ERROR_DIAGNOSE);
	
					lChaskiService.sendConnectionBroadcast(ChaskiConstants.CONNECTION_ERROR_STATE, lSSID, null, errorMsg);
					
				}
	
				}
				break;
			
				
				case HANDLE_CONNECTION_COUNT_CHANGE: {
					
					Log.d(TAG, 
							"mHandler.handleMessage(): HANDLE_CONNECTION_COUNT_CHANGE");
					
					int numberOfConnectedClients = bundle.getInt(
									ChaskiConstants.KEY_CLIENTS);
					
					lChaskiService.mConnectionCount = numberOfConnectedClients; 
					
					Log.d(TAG, "Number of connected clients has changed to: " + numberOfConnectedClients);
					
					Intent intent = new Intent();
					intent.setAction(ChaskiConstants.CLIENTS_STATE_ACTION);
					intent.putExtras(bundle);
					
					lChaskiService.sendBroadcast(intent);
				}
				
			}
		}
		
	}

	@Override
	public IBinder onBind(Intent arg0) {
		
		initWifiManager();
		
		initConnectivtyManager();
		
		mHandler = new ChaskiServiceHandler(this);
		
		setUpBroadcastReceiver();
		
		return mBinder;
	}
	
	@Override
	public void onDestroy() {
		unregisterReceiver(broadcastReceiver);
		
		super.onDestroy();
	}
	
	

	private void initConnectivtyManager() {
		mConnectivityManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
		
		if(mConnectivityManager == null){
			Log.e(TAG, "checkin(): failed to get ConnectivityManager");
		}
		
	}

	private void initWifiManager() {
		mWifiManager = (WifiManager) getSystemService(Context.WIFI_SERVICE);

		if (mWifiManager == null) {
			Log.e(TAG, "checkin(): failed to get WifiManager");
		}
	}
	
	
	
	private void sendConnectionBroadcast(int connectionState, String ssid, String ipAddress, String errorMsg) {
		Intent intent = new Intent();
		intent.setAction(ChaskiConstants.CONNECTION_STATE_ACTION);
		
		Bundle lBundle = new Bundle();
		lBundle.putInt(ChaskiConstants.CONNECTION_STATE, connectionState);
		
		if(ssid!=null)
			lBundle.putString(ChaskiConstants.KEY_SSID, ssid);
		
		if(ipAddress!=null){
			lBundle.putString(ChaskiConstants.KEY_IP_ADDRESS, ipAddress);
		}
		
		if(errorMsg!=null)
			lBundle.putString(ChaskiConstants.KEY_CONNECTION_ERROR_DIAGNOSE, errorMsg);
		
		intent.putExtras(lBundle);
		sendBroadcast(intent);
	}

	public class MyBinder extends Binder {
	    public ChaskiService getService() {
	      return ChaskiService.this;
	    }
	  }
	
		@Override
		public boolean enableAp(String ssid, String password) throws RemoteException, IllegalArgumentException{
			
			String networkPassword = password;
			if(password==null || password.isEmpty()){
				networkPassword=Util.DEFAULT_NETWORK_PASSWORD; 
			}
			
			if(ssid==null || ssid.isEmpty() || ssid.length()>32){
				throw new IllegalArgumentException(
						"Network name must not be null, empty or exceed 32 characters.");
					
			}
			
			if(ssid.contains("\"")){
				throw new IllegalArgumentException(
						"Network name cannot contain inverted commas ('\"'').");
				
			}
			
			if(networkPassword!=null && networkPassword.length()!=8){
				throw new IllegalArgumentException(
						"Network password must be 8 characters long.");
				
			}
			
			setUpWifiConfig(ssid, networkPassword);
			
			if(isApEnabled()){
				
				mUpdateSSIDFlag = true;
				return disableAp();
				
			}
			else{ 
				
				mSSID = ssid;
						
				return processSetWifiApEnabled(true, mWifiApConfig);
			}
		}

		@Override
		public boolean disableAp() throws RemoteException {
			
			int state = getWifiApState();
			
			if(state == ChaskiConstants.AP_STATE_ENABLED){
				
				WifiConfiguration wifiConfig = processGetWifiApConfiguration();
				
				mSSID = wifiConfig.SSID;
				
				return processSetWifiApEnabled(false, null);
				
			}
			else{
				//Wifi AP is already disabled.
				
				Log.e(TAG, "Wifi AP is already disabled."); 
				return false;
			}
		}

		@Override
		public boolean isApEnabled() throws RemoteException  {
			
			
			return (getWifiApState()==ChaskiConstants.AP_STATE_ENABLED);
			
		}
		
		@Override
		public WifiConfiguration getWifiApConfiguration () throws RemoteException {
			
			WifiConfiguration wifiConfig = processGetWifiApConfiguration();
			
			Log.d(TAG, "Retrieved WiFi SSID: " + wifiConfig.SSID);
			
			return wifiConfig;
		}

		@Override
		public boolean disconnectFromAp()  {
			
			if (mNetworkId != -1) {
				boolean networkDisabled = mWifiManager.disableNetwork(mNetworkId);

				Log.i(TAG, "networkDisabled: " + networkDisabled);

				// TODO Amro urgent remove this from OS list after
				// disconnecting, or will removal disconnect?
				if (networkDisabled && ! mAlreadyConfigured) {
					mWifiManager.removeNetwork(mNetworkId);
				}

				
			}
			
			return false;
		}

		@Override
		public boolean isConnectedToApWithSSID(String charSequence)  {
			
			NetworkInfo networkInfo = mConnectivityManager.getActiveNetworkInfo();
			
			if(networkInfo!=null && networkInfo.isConnected() && networkInfo.getType()==ConnectivityManager.TYPE_WIFI){
				final WifiInfo wifiInfo = mWifiManager.getConnectionInfo();
				String ssid = wifiInfo.getSSID();
				//do massage to lSsid, i.e. remove first and last occurrence of "
				
				if(ssid.contains("\"")){
					//since Android SDK 15 this is the case
					ssid = ssid.substring(1, ssid.length()-1);
				}
				
				if( ssid!=null && !ssid.isEmpty() ){
					
					Log.d(TAG, "Is connected to AP with SSID = " + ssid);
					
					if(ssid.equals(charSequence)){
						return true;
					}
					
				}
			}
			
			return false;
		}

		@Override
		public boolean connectToAp(String ssid, String password) throws RemoteException {
			
			if(ssid==null || ssid.isEmpty()){
				return false;
			}
			
			connectToSSIDThread = new ConnectToSSIDRunnable(mHandler, ssid, password, 
					mWifiManager); 
		
			stpeConnectToSSID = new ScheduledThreadPoolExecutor(1);
		
			stpeConnectToSSID.schedule(connectToSSIDThread, 0,
					TimeUnit.MILLISECONDS);
			
			return true;
		}

		@Override
		public void triggerIpAddressesOfValidClients(final String operationString){
			
			Runnable runnable = new Runnable() {
				
				@Override
				public void run() {
					List<String> connectedClientList = new ConnectionsCacheHelper().getListOfValidClients();
					
					String[] connectedClientsArray = new String [connectedClientList.size()];
					
					connectedClientList.toArray(connectedClientsArray);
										
					Intent intent = new Intent();
					intent.setAction(ChaskiConstants.CLIENTS_STATE_ACTION);
					Bundle bundle = new Bundle();
					bundle.putStringArray(ChaskiConstants.KEY_STRING_ARRAY_OF_CLIENTS, connectedClientsArray);
					bundle.putString(ChaskiConstants.KEY_OPERATION_STRING, operationString);
					
					intent.putExtras(bundle);
					sendBroadcast(intent);
				}
			};
			
			new Thread(runnable).start();			
			
		}

		@Override
		public void triggerNumberOfClients(final String operationString){
	
			Runnable runnable = new Runnable() {
		
		@Override
		public void run() {
			boolean val  = new ConnectionsCacheHelper().canReachAtLeastOneClient();
			
			Intent intent = new Intent();
			intent.setAction(ChaskiConstants.CLIENTS_STATE_ACTION);
			Bundle bundle = new Bundle();
			bundle.putBoolean(ChaskiConstants.KEY_CLIENTS, val);
			bundle.putString(ChaskiConstants.KEY_OPERATION_STRING, operationString);
			intent.putExtras(bundle);
			sendBroadcast(intent);
		}
	};
	
	new Thread(runnable).start();			
	
}
		
	BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {
		
		@Override
		public void onReceive(Context context, Intent intent) {
			
				
				if(ANDROID_WIFI_AP_STATE_CHANGED_ACTION.equals( 
						 intent.getAction())){
					 
					 int iApState;
					 
					try {
						
						iApState = getWifiApState();
						
						handleApStateChanged(iApState);
						
					} catch (RemoteException e) {
						Log.e(TAG, "RemoteException was thrown when calling getWifiApState");
					} 
				 }
				
				if(WifiManager.NETWORK_STATE_CHANGED_ACTION.equals(intent.getAction()) ){
					NetworkInfo networkInfo = (NetworkInfo) intent.getParcelableExtra(WifiManager.EXTRA_NETWORK_INFO);
					handleNetworkStateChangedAction(networkInfo);
				}
			
		}
	
		private void handleApStateChanged(int iApState) {
			
			
			Log.d(TAG,"wifiapstate="+ iApState); 
			
			if (iApState == ChaskiConstants.AP_STATE_ENABLED) {
				Log.i(TAG, "Ap is open.");
				
				startReadingARPCache();
				
				sendApBroadcast(ChaskiConstants.AP_STATE_ENABLED);

			} 
			
			else if (iApState == ChaskiConstants.AP_STATE_DISABLED) {	
				
				if(mUpdateSSIDFlag){
					//update was meant, see declaration of this variable
					mUpdateSSIDFlag = false;
					
					processSetWifiApEnabled(true, mWifiApConfig);
				}
				else{
					stopReadingARPCache();
					
					sendApBroadcast(ChaskiConstants.AP_STATE_DISABLED);
				}						
				
			}
			
		}

		private void handleNetworkStateChangedAction(NetworkInfo networkInfo) {
			Log.i(TAG,
					networkInfo.getState() + " with" + networkInfo.toString());

			if (networkInfo.getState() == State.CONNECTED) {
				final WifiInfo wifiInfo = mWifiManager.getConnectionInfo();
				
				if(wifiInfo!=null && wifiInfo.getSSID()!=null && wifiInfo.getSSID().length()>0){
					String lSSID = wifiInfo.getSSID().replace("\"", "");
					
					mNetworkId = wifiInfo.getNetworkId();
					
					int ipAddress = wifiInfo.getIpAddress();
					
					String ipFormattedAsString = formatIpAddress(ipAddress);
					
					if (lSSID !=null) {
						
						sendConnectionBroadcast(ChaskiConstants.CONNECTED_STATE, lSSID, ipFormattedAsString, null);
						
						
					}
				}
			}
			
			if (networkInfo.getState() == State.DISCONNECTED) {
				
				Log.d(TAG, "disconnected");
				
				sendConnectionBroadcast(ChaskiConstants.DISCONNECTED_STATE, null, null, null);
				
			}
			
		}
		
		private void sendApBroadcast(int apState) {
			Intent intent = new Intent();
			intent.setAction(ChaskiConstants.AP_STATE_ACTION);
			Bundle bundle = new Bundle();
			bundle.putInt(ChaskiConstants.AP_STATE, apState);
			
			if(mSSID!=null && mSSID.length()>0){
				bundle.putString(ChaskiConstants.KEY_SSID, new String(mSSID));
			}
			
			intent.putExtras(bundle);
			sendBroadcast(intent);
		}
	};
	
	
	
	protected void stopReadingARPCache() {
		
		if(stpeReadArpCache != null){
			stpeReadArpCache.shutdown();
		}
	}

	protected String formatIpAddress(int ipAddress) {
		return String.format(Locale.getDefault(), "%d.%d.%d.%d",
				   (ipAddress & 0xff), (ipAddress >> 8 & 0xff),
				   (ipAddress >> 16 & 0xff), (ipAddress >> 24 & 0xff));
	}

	protected void startReadingARPCache() {
		
		mConnectionCount = 0;
		
		stpeReadArpCache = new ScheduledThreadPoolExecutor(1); 
		
		Runnable runnable = new Runnable() { 
			
			@Override
			public synchronized void run() {
				List<String> listOfIpAddressesOfClients = new ConnectionsCacheHelper().extractIPs();
				
				int numberOfClients = listOfIpAddressesOfClients.size();
				
				if(mConnectionCount!=numberOfClients){
					
					mConnectionCount = numberOfClients; 
					
					String[] clientsArray = new String [numberOfClients];
					
					listOfIpAddressesOfClients.toArray(clientsArray);
					
					Intent intent = new Intent();
					intent.setAction(ChaskiConstants.CLIENTS_STATE_ACTION);
					Bundle bundle = new Bundle();
					bundle.putInt(ChaskiConstants.KEY_CLIENTS,numberOfClients);
					bundle.putStringArray(ChaskiConstants.KEY_STRING_ARRAY_OF_CLIENTS, clientsArray);
					
					intent.putExtras(bundle);
					
					sendBroadcast(intent);
				}
			}
		};
		
		stpeReadArpCache.scheduleAtFixedRate(runnable, 0, 1000, TimeUnit.MILLISECONDS);
		
	}

	@Override
	public boolean isConnectedToApWithPrefix(String prefix) {
		NetworkInfo networkInfo = mConnectivityManager.getActiveNetworkInfo();
		
		if(networkInfo!=null && networkInfo.isConnected() && networkInfo.getType()==ConnectivityManager.TYPE_WIFI){
			final WifiInfo wifiInfo = mWifiManager.getConnectionInfo();
			String ssid = wifiInfo.getSSID();
			
			//do massage to lSsid, i.e. remove first and last occurrence of "
			if(ssid!=null && ssid.contains("\"")){
				//since Android SDK 15 this is the case
				ssid = ssid.substring(1, ssid.length()-1);
			}
			
			if(ssid!=null && !ssid.isEmpty())
			{
					Log.d(TAG, "Is connected to AP with SSID = " + ssid);
					
					if(ssid.startsWith(prefix)){
						return true;
					}
			}
		}
		
		return false;
	}

	protected void setUpWifiConfig(String networkName, String networkPassword) {
		
		mWifiApConfig = new WifiConfiguration();
	
		mWifiApConfig.SSID = networkName;
		
		if(networkPassword!=null){
			mWifiApConfig.preSharedKey = networkPassword;
		}
		else{
			mWifiApConfig.preSharedKey = Util.DEFAULT_NETWORK_PASSWORD;
		}
				
		mWifiApConfig.allowedAuthAlgorithms
				.set(WifiConfiguration.AuthAlgorithm.OPEN);
		mWifiApConfig.allowedGroupCiphers
				.set(WifiConfiguration.GroupCipher.CCMP);
		mWifiApConfig.allowedGroupCiphers
				.set(WifiConfiguration.GroupCipher.TKIP);
		mWifiApConfig.allowedKeyManagement
				.set(WifiConfiguration.KeyMgmt.WPA_PSK);
		mWifiApConfig.allowedPairwiseCiphers
				.set(WifiConfiguration.PairwiseCipher.TKIP);
		mWifiApConfig.allowedPairwiseCiphers
				.set(WifiConfiguration.PairwiseCipher.CCMP);
		mWifiApConfig.allowedProtocols.set(WifiConfiguration.Protocol.RSN);
		mWifiApConfig.status = WifiConfiguration.Status.ENABLED;
		
	}

	/**
	 * Gets the Wi-Fi AP Configuration.
	 * @return AP details in {@link WifiConfiguration}
	 */
	private WifiConfiguration processGetWifiApConfiguration() {
		try {
			Method method = mWifiManager.getClass().getMethod(GET_WIFI_AP_CONFIGURATION);
			return (WifiConfiguration) method.invoke(mWifiManager);
		} catch (Exception e) {
			Log.e(TAG, "", e);
			return null;
		}
	}

	/**
	 * Gets the Wi-Fi AP Configuration.
	 * @return AP details in {@link WifiConfiguration}
	 */
	@SuppressWarnings("unused")
	private boolean processSetWifiApConfiguration(WifiConfiguration wifiApConfig) {
		
		boolean val = false;
		Method method;
		try {
			method = mWifiManager.getClass().getMethod(SET_WIFI_AP_CONFIGURATION,
					WifiConfiguration.class);
	
			val = (Boolean) method.invoke(mWifiManager, wifiApConfig);
	
			if (val)
				Log.i(TAG, SET_WIFI_AP_CONFIGURATION+" invoked successful.");
	
			return true;
	
		} catch (NoSuchMethodException e) {
			Log.e(TAG, e.getMessage());
		} catch (IllegalArgumentException e) {
			Log.e(TAG, e.getMessage());
		} catch (IllegalAccessException e) {
			Log.e(TAG, e.getMessage());
		} catch (InvocationTargetException e) {
			Log.e(TAG, e.getMessage());
		}
	
		return false;
	}

	/**
	 * Creates a WiFi AP
	 * 
	 * @param wmManager
	 *            The WiFi-Manager
	 * @param config
	 *            The settings for the AP to create
	 */
	private boolean processSetWifiApEnabled(boolean bEnable, WifiConfiguration wifiApConfig) {
		boolean val = false;
		Method method;
		try {
			method = mWifiManager.getClass().getMethod(SET_WIFI_AP_ENABLED,
					WifiConfiguration.class, boolean.class);
	
			val = (Boolean) method.invoke(mWifiManager, wifiApConfig, bEnable);
	
			if (val)
				Log.i(TAG, SET_WIFI_AP_ENABLED + " invoked successful.");
	
			return true;
	
		} catch (NoSuchMethodException e) {
			Log.e(TAG, e.getMessage());
		} catch (IllegalArgumentException e) {
			Log.e(TAG, e.getMessage());
		} catch (IllegalAccessException e) {
			Log.e(TAG, e.getMessage());
		} catch (InvocationTargetException e) {
			Log.e(TAG, e.getMessage());
		}
	
		return false;
	
	}

	private void setUpBroadcastReceiver() {
		
		IntentFilter filter = new IntentFilter();
		
		filter.addAction(ANDROID_WIFI_AP_STATE_CHANGED_ACTION);
		filter.addAction(WifiManager.NETWORK_STATE_CHANGED_ACTION);
		
		registerReceiver(broadcastReceiver, filter);
		
	}

	@Override
	public String getLocalIpAdress() {
		
		String ipv4;
		try {
			for (Enumeration<NetworkInterface> en = NetworkInterface
					.getNetworkInterfaces(); en.hasMoreElements();) {
				NetworkInterface intf = en.nextElement();
				  
				if(intf.getName().startsWith("wlan") || intf.getName().startsWith("eth0") || intf.getName().startsWith("wl0.1")){
					for (Enumeration<InetAddress> enumIpAddr = intf
							.getInetAddresses(); enumIpAddr.hasMoreElements();) {
						InetAddress inetAddress = enumIpAddr.nextElement();
						if (!inetAddress.isLoopbackAddress()
								&& InetAddressUtils
										.isIPv4Address(ipv4 = inetAddress
												.getHostAddress())) {
							return ipv4;
						}
					}
				}
			}
		} catch (SocketException ex) {
			Log.e(TAG, ex.toString());
		}
		return null;
	}

	@Override
	public int getWifiApState() throws RemoteException {
		
		try {
			Method method = mWifiManager.getClass()
					.getMethod(GET_WIFI_AP_STATE);
			int val = (Integer) method.invoke(mWifiManager);
	
			// Modulo 10 to avoid problems with Android 4.x where the value of
			// the constants is increased by 10
			val = val % 10;
	
			return val;
		} catch (Exception e) {
			Log.e(TAG, "", e);
			return WifiManager.WIFI_STATE_UNKNOWN;
		}
	}

}
