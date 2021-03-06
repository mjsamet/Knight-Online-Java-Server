package com.knightonline.shared.network.pipelinefactory;

import java.util.Queue;
import java.util.concurrent.atomic.AtomicLong;

import org.jboss.netty.channel.ChannelPipeline;
import org.jboss.netty.channel.ChannelPipelineFactory;

import com.knightonline.shared.network.common.IConnectionStateReport;
import com.knightonline.shared.network.common.MessageInfo;

/**
 * @author Mamaorha
 *
 */
public abstract class PipelineFactory implements ChannelPipelineFactory
{
	// input
	protected AtomicLong channelIdSeq = null;
	protected IConnectionStateReport connectionStateReport = null;
	protected Queue<MessageInfo> requestQueue = null;

	public void init(IConnectionStateReport connectionStateReport, Queue<MessageInfo> requestQueue, AtomicLong channelIdSeq)
	{
		this.connectionStateReport = connectionStateReport;
		this.requestQueue = requestQueue;
		this.channelIdSeq = channelIdSeq;
	}

	public abstract ChannelPipeline createPipeline(long channelId) throws Exception;
	
	@Override
	public ChannelPipeline getPipeline() throws Exception
	{
		return createPipeline(this.channelIdSeq.incrementAndGet());
	}

	public IConnectionStateReport getConnectionStateReport()
	{
		return connectionStateReport;
	}

	public Queue<MessageInfo> getRequestQueue()
	{
		return requestQueue;
	}
}
