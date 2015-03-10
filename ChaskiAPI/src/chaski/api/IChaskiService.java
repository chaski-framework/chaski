package chaski.api;

import android.net.wifi.WifiConfiguration;
import android.os.RemoteException;

//service interface
public interface IChaskiService {
    
    /**
	 * Enables an access point (Wi-Fi network). If the operation succeeds it will broadcast a AP_STATE_ACTION with the 
     * result AP_STATE_Enabled (see {@link ChaskiConstants}). 
	 * @param ssid SSID access point should have
	 * @param password Password access point should have
	 * @return TRUE if invoking of the underlying Android method returns true. 
	 * Otherwise the method returns FALSE. 
	 * @throws RemoteException
	 */
    boolean enableAp(String ssid, String password) throws RemoteException, IllegalArgumentException;
    
    /**
     * Disables an access point (Wi-Fi network). 
     * If the operation succeeds it will broadcast a AP_STATE_ACTION with the 
     * result AP_STATE_DISABLED (see {@link ChaskiConstants}).
     * 
     * @return TRUE if invoking of the underlying Android method returns true
     * @throws RemoteException
     */
    boolean disableAp() throws RemoteException;
    
    /**
     * Checks if the device has deployed an access point (Wi-Fi network).
     * @return TRUE if access point is enabled and otherwise FALSE.
     * @throws RemoteException
     */
    boolean isApEnabled() throws RemoteException;
    
    /**
     * Triggers the process of connecting to an access point (Wi-Fi network). 
     * If the operation succeeds it will broadcast a CONNECTION_STATE_ACTION with the 
     * result CONNECTED_STATE (see {@link ChaskiConstants}).
     * 
     * @param ssid SSID of access point
     * @param password Password of access point
     * @return TRUE if invoking of the underlying Android method returns true. Otherwise the method returns FALSE.
     * @throws RemoteException
     */
    boolean connectToAp(String ssid, String password) throws RemoteException; 
    
    /**
     * Triggers the process of disconnecting from an access point (Wi-Fi network). 
     * If the operation succeeds it will broadcast a CONNECTION_STATE_ACTION with the 
     * result DISCONNECTED_STATE (see {@link ChaskiConstants}).
     * @return
     * @throws RemoteException
     */
    boolean disconnectFromAp() throws RemoteException;
    
    /**
     * Checks if connection to the specified access point is established.
     * @param charSequence SSID of access point
     * @return TRUI if connection to access point is established and FALSE otherwise.
     * @throws RemoteException
     */
    boolean isConnectedToApWithSSID(String charSequence) throws RemoteException;
    
    /**
     * Checks if a connection any access point is established
     * @return
     */
    boolean isConnectedToApWithPrefix(String prefix);
    
    /**
     * Method that triggers a ping to each all clients' IP addresses listed in the ARP cache. 
     * After having sent a ping to all registered clients Chaski will broadcast a CLIENTS_STATE_ACTION. 
     * The BOOLEAN result TRUE or FALSE can be obtained via KEY_CLIENTS_CONNECTED (see {@link ChaskiConstants}).
     * @param operationString String for app developers to assign a call identifier. 
     * For instance, the value of this parameter could be current time stamp.
     */
    void triggerNumberOfClients(String operationString);
    

    /**
     * Method that triggers a ping to each all clients' IP addresses listed in the ARP cache. 
     * After having sent a ping to all registered clients Chaski will broadcast a CLIENTS_STATE_ACTION. 
     * The String array containing IPs of connected clients can be obtained via KEY_STRING_ARRAY_OF_CLIENTS_CONNECTED (in {@link ChaskiConstants}).
     * @param operationString String for app developers to assign a call identifier. 
     * For instance, the value of this parameter could be current time stamp.
     */
	void triggerIpAddressesOfValidClients(String operationString);

	/**
	 * Retrieves the current WifiConfiguration for the access point
	 * @return WifiConfiguration, e.g. config.SSID
	 * @throws RemoteException
	 */
	WifiConfiguration getWifiApConfiguration() throws RemoteException;
	
	/**
	 * Method that retrieves the local IP address of a Wi-Fi client or AP. 
	 * The IP address complies to IPv4.
	 * @return String representation of an IP address, e.g. "192.168.43.22"
	 */
	String getLocalIpAdress();

	/**
	 * Gets the current state of the access point, i.e. enabled, disabled or unknown.
	 * @return the state of the Wi-Fi access point.
	 * @throws RemoteException
	 */
	public int getWifiApState() throws RemoteException;
	
	/**
	 * Method that retrieves the state value of Wi-Fi AP when it is disabled.
	 * @return State value in which a WiFi access point is disabled
	 * @throws RemoteException
	 */
	public int getWifiApStateDisabled() throws RemoteException;
	
	/**
	 * Method that retrieves the state value of Wi-Fi AP when it is enabled.
	 * @return Method that retrieves the state value of Wi-Fi AP when it is enabled.
	 * @throws RemoteException
	 */
	public int getWifiApStateEnabled() throws RemoteException;

    
    
}