package chaski.api.util.networking;

import java.util.List;

import android.accounts.NetworkErrorException;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import chaski.api.ChaskiConstants;
import chaski.api.ChaskiService;
import chaski.api.util.Util;

public class ConnectToSSIDRunnable implements Runnable {

	private static final String TAG = ConnectToSSIDRunnable.class.getName();
	
	private WifiManager mWifiManager;

	private String mNetworkSSID;

	private Handler mHandler;
	
	private int mNetworkId;

	private boolean mNetworkAlreadyConfigured;

	private String mPassword;
	
	public ConnectToSSIDRunnable(Handler handler, String networkSSID, String password, WifiManager wifiManager)
	{
		mHandler = handler;
		mWifiManager = wifiManager;
		mNetworkSSID = networkSSID;
		mNetworkId = -1;
		mNetworkAlreadyConfigured = false;
		initPassword(password);
	}
	
	private void initPassword(String password) {
			if(password!=null){
	    		mPassword = password;
	    		Log.d(TAG, "Specific password will be used for connecting to hotspot");
	    	}
	    	else{
	    		mPassword = Util.DEFAULT_NETWORK_PASSWORD;
	    		Log.d(TAG, "Default password will be used for connecting to hotspot");
	    	}
	}

	@Override
	public void run() {
		
		Log.i(TAG, "\\nConnecting to " +mNetworkSSID+ " network...\\n");	
		
		try {
			enableNetwork(mNetworkSSID, mPassword);
			
			if (mHandler != null)
			{
				processSendMessageToHandler();
			}
			else
			{
				Log.e(TAG, "No handler registered to inform about network SSID: " + mNetworkSSID);
			}
		} catch (NetworkErrorException e) {
			// TODO Amro send messsage to handler that enabling network failed and broadcast related Chaski event
			String errorMsg = e.getMessage();
			processSendErrorMessageToHandler(errorMsg);
		}
		 
		
	}

	private void processSendMessageToHandler() {
		
		//send broadcast
		Message msg = mHandler.obtainMessage(ChaskiService.HANDLE_CONNECTION_TO_NETWORK_STATE);
		Bundle bundle = new Bundle();		
		bundle.putString(ChaskiConstants.KEY_SSID, mNetworkSSID);
		bundle.putInt(Util.KEY_NETWORK_ID, mNetworkId);
		bundle.putBoolean(Util.KEY_NETWORK_ALREADY_CONFIGURED, mNetworkAlreadyConfigured);
		msg.setData(bundle);
		mHandler.sendMessage(msg);
	}
	
	private void processSendErrorMessageToHandler(String message) {
		
		//send broadcast
		Message msg = mHandler.obtainMessage(ChaskiService.HANDLE_CONNECTION_TO_NETWORK_STATE);
		Bundle bundle = new Bundle();		
		bundle.putString(ChaskiConstants.KEY_CONNECTION_ERROR_DIAGNOSE, message);
		bundle.putString(ChaskiConstants.KEY_SSID, mNetworkSSID);
		bundle.putBoolean(Util.KEY_NETWORK_ALREADY_CONFIGURED, mNetworkAlreadyConfigured);
		msg.setData(bundle);
		mHandler.sendMessage(msg);
	}
	
	private void enableNetwork(String SSID, String password) throws NetworkErrorException{
    	
    	String s = "\""+SSID+"\"";
    	
    	int netId=-1; 
    	
    	String detailMessage;
    	
    	// Check if you are already connected to the given network
    	WifiInfo lConnectionInfo = mWifiManager.getConnectionInfo();
    	
    	String lSSID = lConnectionInfo.getSSID();
    	
    	if(lSSID!=null && lSSID.equals(SSID)) 
    	{
    		Log.e(TAG, "Already connected to SSID: " + SSID);
    		
    		return;
    	}
    	
    	List<WifiConfiguration> configuredNetworks = this.mWifiManager.getConfiguredNetworks();    	
    	
    	int i=0;
    	while(!mNetworkAlreadyConfigured && i<configuredNetworks.size()){
    		WifiConfiguration wifiCongfiguration = configuredNetworks.get(i);
    		
    		if(wifiCongfiguration.SSID != null && wifiCongfiguration.SSID.equals(s))
        	{
    			netId= wifiCongfiguration.networkId;
    			mNetworkAlreadyConfigured =true;
        	}
    		
    		i++;
    	}
    	
    	if(!mNetworkAlreadyConfigured)
    	{
    		//TODO Amro look into deactivated lines due to Android 4.3
    		
	    	WifiConfiguration config = new WifiConfiguration();
	    	config.SSID = "\""+SSID+"\"";
	    	config.priority = 1;
	    	config.preSharedKey = "\""+ mPassword +"\"";
//	    	config.status = WifiConfiguration.Status.DISABLED;
//	    	config.status = WifiConfiguration.Status.CURRENT; 
//	    	config.status = WifiConfiguration.Status.ENABLED;
	    	
//	    	config.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.IEEE8021X); 
	    	/*
	    	config.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.NONE);
	    	config.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.WPA_EAP);
	    	config.allowedPairwiseCiphers.set(WifiConfiguration.PairwiseCipher.CCMP);
	    	config.allowedPairwiseCiphers.set(WifiConfiguration.PairwiseCipher.NONE);
	    	
	    	 
	    	
	    	
	    	config.allowedAuthAlgorithms.set(WifiConfiguration.AuthAlgorithm.OPEN);
	    	config.allowedAuthAlgorithms.set(WifiConfiguration.AuthAlgorithm.SHARED);
	    	config.allowedAuthAlgorithms.set(WifiConfiguration.AuthAlgorithm.LEAP);
	    		    	 
	    	config.allowedProtocols.set(WifiConfiguration.Protocol.RSN);
	        config.allowedProtocols.set(WifiConfiguration.Protocol.WPA);
	        config.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.WPA_PSK);
	        config.allowedPairwiseCiphers.set(WifiConfiguration.PairwiseCipher.CCMP);
	        config.allowedPairwiseCiphers.set(WifiConfiguration.PairwiseCipher.TKIP);
	        config.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.WEP40);
	        config.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.WEP104);
	        config.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.CCMP);
	        config.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.TKIP);
	        */
	    	
	    	config.allowedProtocols.set(WifiConfiguration.Protocol.WPA);
	    	config.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.WPA_PSK);
	    	config.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.TKIP);
	    	config.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.CCMP);
	    	config.allowedProtocols.set(WifiConfiguration.Protocol.RSN);
	    	
	    	netId = this.mWifiManager.addNetwork(config);
	    	
	    	if(netId==-1){
	    		
	    		detailMessage = "Adding network failed";
	    		
	    		Log.e(TAG, detailMessage);
	    		
	    		throw new NetworkErrorException(detailMessage);
	    		
	    		
	    	}
	    	else
	    	{
	    		mNetworkId = netId;
	    	}
    	}
    	
    	boolean enabledDesiredNetwork = mWifiManager.enableNetwork(netId, true);
    	
    	if(enabledDesiredNetwork){
			Log.i(TAG, "Enabling desired network:" + mNetworkSSID);
		
			//TODO: Amro check if still required, rather not as having SSIDs in Wi-Fi settings list is ugly
//	    	mWifiManager.saveConfiguration();
			
			return;
    	}
    	else {
    		detailMessage = "Could not enable desired network:";
    		Log.i(TAG, detailMessage + mNetworkSSID);
    		throw new NetworkErrorException(detailMessage);
    		
    	}
    	    	    	
    }

}