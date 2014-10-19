package chaski.api.util;

import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Enumeration;
import java.util.TimeZone;

import org.apache.http.conn.util.InetAddressUtils;

import android.annotation.SuppressLint;
import android.util.Log;

public class Util {
	
	private static final String MD5 = "MD5";

	private static final String TAG = Util.class.getName(); 

	public static final String DEFAULT_NETWORK_PASSWORD = "chaskiGo";
	public static final String KEY_NETWORK_ALREADY_CONFIGURED = "NETWORK_ALREADY_CONFIGURED";
	public static final String KEY_NETWORK_ID = "NETWORK_ID";
	public static final int MAXREPLYWAIT = 10;  // 1 = MAXREPLYSLEEP
	public static final long MAXREPLYSLEEP = 300; //ms
	public final static String CR_LF = "\r\n";
	public static final String NO_SOCKET_HELPER = "No socket helper discovered to process client request.";
	
	/* removes the md5 prefix from a msg, returns an emtpy string if md5 sum
	 * the prefix */
	public static String decodeMsg(String msg) {
		Log.d(TAG, "Trying to decode: " + msg);
		String rcvdHash = null;
	
		if (msg.length() >= 32) {
			rcvdHash = msg.substring(0, 32);
			msg = msg.substring(32);
		} else {
			Log.d(TAG, "Decoding failed");
			return "";
		}
	
		if (rcvdHash.equals(Util.generateMD5(msg))) {
			Log.d(TAG, "Decoding successful");
			return msg;
		} else {
			Log.d(TAG, "Decoding failed");
			return "";
		}
	}

	public static String generateMD5(String s) {
	    try {
	    	
	        // Create MD5 Hash
	        MessageDigest digest = java.security.MessageDigest.getInstance(MD5);
	        digest.update(s.getBytes());
	        byte messageDigest[] = digest.digest();
	       
	        // Create Hex String
	        StringBuffer hexString = new StringBuffer();
	        for (int i=0; i<messageDigest.length; i++) {
	        	String ch = Integer.toHexString(messageDigest[i] & 0xFF);
	        	ch = ch.length() == 1 ? "0"+ch : ch; //ensure MD5 has 32 bytes
	            hexString.append(ch);
	        }
	        
	        return hexString.toString();
	        
	    } catch (NoSuchAlgorithmException e) {
	        e.printStackTrace();
	    }
	    return "";
	}

	/* size of buffer for UDP packets */
	public static final int BUFFER_SIZE = 1024;

	public static final int UNICAST_PORT = 8889;

	/* generates a string prefixed by its md5 */
	public static String encodeMsg(String msg) {
		String hash = Util.generateMD5(msg);
	    	
	    if (hash.length() > 0) {
	    	Log.d(TAG, "Encoded msg: "+hash+msg);
	    	return hash+msg;
		} else {
			return "";
		}
	}
		
		@SuppressLint("SimpleDateFormat")
		public static String getCurrentTime(){
			
			Calendar cal = Calendar.getInstance();
			
			SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");
			//BUGFIX: we add 'Z' to string, so we need to provide date in GMT time zone
			dateFormat.setTimeZone(TimeZone.getTimeZone("GMT"));
			String timeValue = dateFormat.format(cal.getTime());
			
			return timeValue;
		}

		public static final String HOST_IP_NUMBER = "192.168.43.1"; 
		
		public static String getLocalIpAddress() {
			String ipv4;
			try {
				for (Enumeration<NetworkInterface> en = NetworkInterface
						.getNetworkInterfaces(); en.hasMoreElements();) {
					NetworkInterface intf = en.nextElement();
					  
					if(intf.getName().startsWith("wlan") || intf.getName().startsWith("eth0")){
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

}
