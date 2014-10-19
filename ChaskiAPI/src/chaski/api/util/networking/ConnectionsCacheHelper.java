package chaski.api.util.networking;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import android.util.Log;

public class ConnectionsCacheHelper {
	
	
	public final static String TAG =  ConnectionsCacheHelper.class.getName();
	
	private final static int TIMEOUT = 2000;

	/**
		  * Try to extract IP address using the ARP cache (/proc/net/arp).<br>
		  * <br>
		  * We assume that the file has this structure:<br>
		  * <br>
		  * IP address       HW type     Flags       HW address            Mask     Device
		  * 192.168.18.11    0x1         0x2         00:04:20:06:55:1a     *        eth0
		  * 192.168.18.36    0x1         0x2         00:22:43:ab:2a:5b     *        eth0
		  *
		  * @param ip
		  * @return a list containing all registered IP addresses in the ARP cache
		  */
		 private List <String> extractIPs() {
		      
		     BufferedReader br = null;
		     
		     
		     List <String> extractedMacKList = new 
//		    		 ArrayList
		    		 CopyOnWriteArrayList
		    		 <String>();
		     try {
		         br = new BufferedReader(new FileReader("/proc/net/arp"));
		         String line;
		         while ((line = br.readLine()) != null) {
		        	  
		        	 String[] splitted = line.split(" +");
		             if (splitted != null && splitted.length >= 4 && !splitted[0].startsWith("IP")) {
		                 // Basic sanity check
		                 String ip = splitted[0];
		                 if (ip!=null && !ip.isEmpty()) {
		                     extractedMacKList.add(ip);
		                 } else {
		                     return null;
		                 }
		             }
		         }
		         
		         
		     } 
		     catch (FileNotFoundException e) {
					Log.e(TAG, "Problem reading file to get connected devices.");	
				}
		     catch (IOException e) {
		    	 Log.e(TAG, "Problem reading a line to get connected devices.");
		     } finally {
		         try {
		             br.close();
		         } catch (IOException e) {
		        	 Log.e(TAG, "Problem closing buffered reader.");
		         }
		     }
		     
		     return extractedMacKList;
		 }

	
		 public boolean canReachAtLeastOneClient(){
			 
			 List<String> ipList = extractIPs();
			 
			 boolean val = false;
			 
			 
			 for(String ip : ipList){
				 
				 val = isReachable(ip);
				 
				 if(val){
					 return true;
				 }
			 }
			 
			 return val;
		 }
		 
		 public List <String> getListOfIpAddressesOfClients(){
			 
			 	List<String> ipList = extractIPs();
				 
				 boolean val = false;
				 
				 
				 for(String ip : ipList){
					 
					 val = isReachable(ip);
					 
					 if(!val){ 
						 //client not reachable => remove it from list 
						 ipList.remove(ip);
					 }
				 }
				 
				 return ipList;
			
		 }


		private boolean isReachable(String host) {
				
		    	 
		    	 
		    	 InetAddress[] addresses;
				try {
					addresses = InetAddress.getAllByName(host);
					
					for (InetAddress address : addresses)
			    	 { 
			    	     if ( address.isReachable(TIMEOUT))
			    	     {
			    	        return true; // just set a break point here
			    	     }
			    	 }
			    	 
				} catch (UnknownHostException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} 
		    	 
		    	 return false;
			}
	
		 
	
}
