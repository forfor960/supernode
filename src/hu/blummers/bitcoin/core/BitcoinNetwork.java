package hu.blummers.bitcoin.core;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import hu.blummers.bitcoin.core.WireFormat.Address;
import hu.blummers.p2p.P2P;

public class BitcoinNetwork extends P2P {
	private static final Logger log = LoggerFactory.getLogger(BitcoinNetwork.class);
	private Chain chain;
	private UnconfirmedTransactions unconfirmedTransactions = new UnconfirmedTransactions();
	
	public BitcoinNetwork (Chain chain) throws IOException
	{
		super (chain.getPort());
		this.chain = chain;
	}

	@Override
	public Peer createPeer(InetSocketAddress address) {
		BitcoinPeer peer = new BitcoinPeer (this, address);
		
		peer.addListener(new BitcoinMessageListener (){
			@Override
			public void process(BitcoinMessage m, BitcoinPeer peer) {
				if ( m instanceof AddrMessage )
				{
					AddrMessage am = (AddrMessage)m;
					for ( Address a : am.getAddresses() )
					{
						log.trace("received new address " + a.address);
						addPeer (a.address, (int)a.port);
					}
				}
			}});
		
		peer.addListener(unconfirmedTransactions);
		
		return peer;
	}

	public Chain getChain() {
		return chain;
	}
	
	public void discover() {
		log.info("Discovering network");
		int n = 0;
		for (String hostName : chain.getSeedHosts()) {
			try {
				InetAddress[] hostAddresses = InetAddress.getAllByName(hostName);

				for (InetAddress inetAddress : hostAddresses) {
					addPeer(inetAddress, chain.getPort());
					++n;
				}
			} catch (Exception e) {
				log.trace("DNS lookup for " + hostName + " failed.");
			}
		}
		log.info("Found " + n  + " addresses of seed hosts");
	}
}