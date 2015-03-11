package chaski.api;

public interface ChaskiConstants {		
	
		/**
		 * Action indicator for changes to access point state
		 */
		public static final String AP_STATE_ACTION = "chaski.api.wifi.ap.STATE";

		/**
		 * Action indicator for changes to access point connection states
		 */
		public static final String CONNECTION_STATE_ACTION = "chaski.api.wifi.connection.STATE";
		
		/**
		 * Action indicator for client related changes
		 */
		public static final String CLIENTS_STATE_ACTION = "chaski.api.wifi.conneciton.CLIENTS";

		
		
		/**
		 * Key to retrieve the state of a WiFi access point
		 */
		public static final String AP_STATE = "apstate";
		
		/**
		 * Key to retrieve the connection state of a Wi-Fi station
		 */
		public static final String CONNECTION_STATE = "connectionState";
		
		
		
		/**
		 *  State in which a WiFi station is disconnected
		 */
		public static final int DISCONNECTED_STATE = 51;
		
		/**
		 * State in which a WiFi station is connected
		 */
		public static final int CONNECTED_STATE = 52; 
		
		/**
		 * State in which the connection of a WiFi station is an erroneous state
		 */
		public static final int CONNECTION_ERROR_STATE = 53; 
		
		
		
		/**
		 * Key for retrieving connected WiFi stations to WiFi access points
		 */
		public static final String KEY_CLIENTS = "KEY_CLIENTS";
		
		/**
		 * Key for retrieving array of IP addresses of clients
		 */
		public static final String KEY_STRING_ARRAY_OF_CLIENTS = "KEY_STRING_ARRAY_OF_CLIENTS_CONNECTED";
		
		/**
		 * Key to retrieve a value, which has originally been passed as an input parameter for a method that sends the broadcast containing this value.
		 */
		public static final String KEY_OPERATION_STRING = "KEY_OPERATION_STRING";
		
		/**
		 * Key for messages related to unsuccessful connection trials
		 */
		public static final String KEY_CONNECTION_ERROR_DIAGNOSE = "KEY_CONNECTION_ERROR_DIAGNOSE";
		
		/**
		 * Network name of connected access point
		 */
		public static final String KEY_SSID = "CONNECTED_SSID";

		/**
		 * Key to retrieve IP address
		 */
		public static final String KEY_IP_ADDRESS = "KEY_IP_ADDRESS";;
	
}
