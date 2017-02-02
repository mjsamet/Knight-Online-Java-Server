package com.knightonline.shared.network.packet.handlers;

import org.springframework.beans.factory.annotation.Autowired;

import com.knightonline.shared.network.KOServer;
import com.knightonline.shared.network.packet.IPacketHandler;
import com.knightonline.shared.network.packet.Packet;
import com.knightonline.shared.network.packet.PacketWriter;

/**
 * @author Mamaorha
 *
 */
public abstract class LoggedInHandler implements IPacketHandler
{
	@Autowired
	protected PacketWriter packetWriter;
	
	protected abstract KOServer getKOServer();
	
	@Override
	public void handlePacket(Packet requestPacket)
	{
		boolean connectedAccount = getKOServer().isConnectedAccount(requestPacket.getMessageInfo().getChannelId());

		if (connectedAccount)
		{
			handlePacketImpl(requestPacket);
		}
		
		else
		{
			System.out.println("Client tried to send packet without log in");
			killConnection(requestPacket);			
		}
	}
	
	protected void killConnection(Packet requestPacket)
	{
		getKOServer().killConnection(requestPacket.getMessageInfo().getChannelId());
	}
	
	protected abstract void handlePacketImpl(Packet requestPacket);
}
