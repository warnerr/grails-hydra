package com.seven10.hydra.resource.discovery;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.dcache.nfs.v3.xdr.exportnode;
import org.dcache.nfs.v3.xdr.exports;
import org.dcache.nfs.v3.xdr.mount_prot;
import org.dcache.utils.net.InetSocketAddresses;
import org.dcache.xdr.IpProtocolType;
import org.dcache.xdr.OncRpcClient;
import org.dcache.xdr.RpcAuth;
import org.dcache.xdr.RpcAuthTypeNone;
import org.dcache.xdr.RpcCall;
import org.dcache.xdr.XdrTransport;
import org.dcache.xdr.XdrVoid;
import org.dcache.xdr.portmap.GenericPortmapClient;
import org.dcache.xdr.portmap.OncPortmapClient;

import com.seven10.restobjects.devices.resource.DiscoveredResource;

import jcifs.smb.NtlmPasswordAuthentication;
import jcifs.smb.SmbException;
import jcifs.smb.SmbFile;

/**
 * The Class ResourceDiscovery.
 */
public class ResourceDiscovery 
{
	private static final Logger m_logger = LogManager.getFormatterLogger(ResourceDiscovery.class.getSimpleName());
	
	/** The Constant SMB_STRING. */
	private final static String SMB_STRING = "smb://";
	private static final int PORTMAP_PORT = 111;
	
	private String m_deviceAddress;
	private final String m_username;
	private final String m_password;	
	private final String m_domain;	
	
	/**
	 * Instantiates a new resource discovery.
	 *
	 * @param deviceAddress the device address
	 * @param userName the user name
	 * @param password the password
	 * @param domain the domain
	 * @throws IllegalArgumentException the illegal argument exception
	 */
	public ResourceDiscovery(String deviceAddress, 
							 String userName, 
							 String password, 
							 String domain) throws IllegalArgumentException
	{
		validateParams(deviceAddress, userName, password, domain);
		
		this.m_deviceAddress = deviceAddress;
		this.m_username = userName;
		this.m_password = password;
		this.m_domain = domain;
	}
	
	/**
	 * Validate parameters to ctor.
	 *
	 * @param deviceAddress the device address
	 * @param userName the user name
	 * @param password the password
	 * @param domain the domain
	 * @throws IllegalArgumentException the illegal argument exception
	 */
	private static final void validateParams(String deviceAddress, 
											 String userName, 
											 String password, 
											 String domain) throws IllegalArgumentException
	{
		if (deviceAddress == null || deviceAddress.isEmpty())
		{
			m_logger.error(".validateParams(): Device address can not be null or empty");
			throw new IllegalArgumentException("Device Address can not be null or empty");
		}
		
		if (userName == null || userName.isEmpty())
		{
			m_logger.error(".validateParams(): Username can not be null or empty");
			throw new IllegalArgumentException("Username can not be null or empty");
		}
		
		if (password == null || password.isEmpty())
		{
			m_logger.error(".validateParams(): Password can not be null or empty");
			throw new IllegalArgumentException("Password can not be null or empty");
		}
		
		if (domain == null || domain.isEmpty())
		{
			m_logger.error(".validateParams(): Domain can not be null or empty");
			throw new IllegalArgumentException("Domain can not be null or empty");
		}	
	}
	
	/**
	 * Get all of the shares associated with this device
	 * 
	 * @return
	 * @throws IllegalArgumentException
	 */
	public DiscoveredResource[] getAllShares() throws IllegalArgumentException
	{
		// domain, username, and password for SMB auth
		NtlmPasswordAuthentication auth = new NtlmPasswordAuthentication(m_domain, 
																		 m_username, 
																		 m_password);
		
		try
		{
			// Connect to the root of the SMB resource and list the shares present
			SmbFile server = new SmbFile(SMB_STRING + m_deviceAddress + "/", auth);			
			String[] shares = server.list();
			
			// List to be returned
			DiscoveredResource[] discoveryList = new DiscoveredResource[shares.length];
			
			// Add each discovered resource to the list
			for (int i = 0; i < shares.length; i++)
			{
				/* 12/16/2015 - ADM - Removed the UNC path because we don't really need it.
				SmbFile share = new SmbFile(SMB_STRING + m_deviceAddress + "/" + shares[i], 
											auth);
				
				DiscoveredResource discoveredShare = new DiscoveredResource(shares[i], 
																			share.getUncPath());
				 */
				DiscoveredResource discoveredShare = new DiscoveredResource(shares[i], shares[i]); 
				discoveryList[i] = discoveredShare;
			}
			
			return discoveryList;
		}
		catch (MalformedURLException ex)
		{
			throw new IllegalArgumentException("Incorrect URL discovering shares", ex);
		}
		catch (SmbException ex)
		{
			throw new IllegalArgumentException("Failed (" + ex.getNtStatus() + ") to discover shares: " + ex.getMessage() );
		}
	}
	
	/**
	 * Get a list of all NFS exports 
	 * @return
	 * @throws IOException 
	 */
	public DiscoveredResource[] getAllExports() throws IOException
	{
	    // Get address of mounted service
	    OncRpcClient rpcClient = new OncRpcClient(InetAddress.getByName(m_deviceAddress), 
	    										  IpProtocolType.TCP, 
	    										  PORTMAP_PORT);
	    XdrTransport transport = rpcClient.connect(); // IOException

	    OncPortmapClient portmapClient = new GenericPortmapClient(transport);
	    String strMountedAddress = portmapClient.getPort(mount_prot.MOUNT_PROGRAM, 
	    												 mount_prot.MOUNT_V3, 
	    												 "tcp");
	    rpcClient.close();

	    // Convert the mountedAddress into a InetSocketAddress
	    // The first 4 octects are the IPv4 IP and the last two octects are the big endian notation of the service port
	    InetSocketAddress address = InetSocketAddresses.forUaddrString(strMountedAddress);

	    // Returned listen address contains wildcard. replace it with the host name.
	    address = new InetSocketAddress(m_deviceAddress, address.getPort());

	    // Connect to mountd and cass dump
	    rpcClient = new OncRpcClient(address, IpProtocolType.TCP);
	    transport = rpcClient.connect();
	    
	    RpcAuth auth = new RpcAuthTypeNone();
	    RpcCall call = new RpcCall(mount_prot.MOUNT_PROGRAM, 
	    						   mount_prot.MOUNT_V3, 
	    						   auth, 
	    						   transport);

	    // Run the EXPORT command for the list of exports accessible by the host device machine
	    exports exports = new exports();
	    call.call(mount_prot.MOUNTPROC3_EXPORT_3, 
	    		  XdrVoid.XDR_VOID, 
	    		  exports);

	    // Iterate through the return exports and compile our list of nfs exports to return
	    List<DiscoveredResource> exportList = new ArrayList<>();
	    
	    exportnode exportEntry = exports.value;
	    while (exportEntry != null) 
	    {
	        String strExportDir = exportEntry.ex_dir.value;
	        
	        exportList.add(new DiscoveredResource(strExportDir, strExportDir));
	        
	        // C style so move the pointer to point to the next export
	        exportEntry = exportEntry.ex_next.value;
	    }
	    rpcClient.close();
	    
	    // Convert our list to an array
		return exportList.toArray(new DiscoveredResource[exportList.size()]);
	}
	
	public void setDeviceAddress(String strDeviceAddress)
	{
		m_deviceAddress = strDeviceAddress;
	}
}
