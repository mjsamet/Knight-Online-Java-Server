package com.knightonline.shared.network.messagehandler;

import java.util.HashMap;
import java.util.Map;
import java.util.Queue;

import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.ChannelStateEvent;
import org.jboss.netty.channel.SimpleChannelHandler;

import com.knightonline.shared.network.common.IConnectionStateReport;
import com.knightonline.shared.network.common.IResponseHandler;
import com.knightonline.shared.network.common.MessageInfo;

/**
 * @author Mamaorha
 *
 */
public abstract class BaseServerMessageHandler extends SimpleChannelHandler
{
	// input
	protected IConnectionStateReport connectionStateReport;
	protected Queue<MessageInfo> queue = null;
	protected long channelId;

	// variables
	protected Map<String, Object> context;

	public BaseServerMessageHandler(IConnectionStateReport connectionStateReport, Queue<MessageInfo> queue, long channelId)
	{
		this.queue = queue;
		this.channelId = channelId;
		this.connectionStateReport = connectionStateReport;
		
		this.context = new HashMap<>();
	}

	@Override
	public void channelConnected(ChannelHandlerContext ctx, ChannelStateEvent e) throws Exception
	{
		connectionStateReport.connected(channelId, ctx);

		super.channelConnected(ctx, e);
	}

	@Override
	public void channelDisconnected(ChannelHandlerContext ctx, ChannelStateEvent e) throws Exception
	{
		connectionStateReport.disconnected(channelId, ctx);

		super.channelDisconnected(ctx, e);
	}

	public long getChannelId()
	{
		return channelId;
	}

	public IResponseHandler getResponseHandler()
	{
		return connectionStateReport;
	}
	
	public IConnectionStateReport getConnectionStateReport()
	{
		return connectionStateReport;
	}

	public Queue<MessageInfo> getQueue()
	{
		return queue;
	}

	public Map<String, Object> getContext()
	{
		return context;
	}
}
