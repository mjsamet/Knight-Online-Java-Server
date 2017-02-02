package com.knightonline.shared.network;

import java.io.Serializable;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

import org.jboss.netty.bootstrap.ServerBootstrap;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelFuture;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.ChannelPipeline;
import org.jboss.netty.channel.socket.nio.NioServerSocketChannelFactory;

import com.knightonline.shared.data.common.TwoSidesMap;
import com.knightonline.shared.data.constants.BootstrapConstants;
import com.knightonline.shared.exception.ConnectivityException;
import com.knightonline.shared.network.common.IConnectionStateReport;
import com.knightonline.shared.network.common.IResponseHandler;
import com.knightonline.shared.network.common.MessageInfo;
import com.knightonline.shared.network.common.ServerConfiguration;
import com.knightonline.shared.network.packet.Packet;
import com.knightonline.shared.network.pipelinefactory.PipelineFactory;
import com.knightonline.shared.network.pipelinefactory.PipelineFactoryBuilder;
import com.knightonline.shared.persistence.dao.IOnlineUserDAO;
import com.knightonline.shared.utils.KOApplicationContext;
import com.knightonline.shared.utils.PacketUtils;

/**
 * @author Mamaorha
 *
 */
public class KOServer implements IConnectionStateReport, Runnable, IResponseHandler
{
	private static final String SERVER_CONFIGURATION_HANDLER_TYPE = "server configuration handler type [%s]";
	private static final String CREATED_PIPELINE_FACTORY = "Created pipeline factory [%s] successfully";
	private static final String SERVER_LISTEN = "Server listen on [%s:%s]";
	private static final String CLIENT_CONNECTED = "client connected [%s]";
	private static final String CLIENT_DISCONNECTED = "client disconnected [%s]";
	private static final String RECIEVER_THREAD = "Reciever Thread";
	private static final String RESPONSE_OBJECT_IS_EMPTY = "Response Object is empty";
	private static final String TIMEOUT_MS = "Got timeout for response to be completed within the specified time limit [%s] milliseconds";
	private static final String CHANNEL_ID_DISCONNECTED = "Channel Id [%s] is disconnected and removed from channels map";
	private static final String CHANNEL_ID_IS_NOT_DEFINED = "Channel Id [%s] is not defined in channel map";

	// input
	protected ServerConfiguration configuration;

	// variables
	protected BlockingQueue<MessageInfo> requestQueue;
	protected AtomicLong channelIdSeq;
	protected Map<Long, ChannelPipeline> channelPipelineMap;
	protected TwoSidesMap<String, Long> connectedAccountsMap;
	protected Channel serverChannel;
	protected ServerBootstrap bootstrap;
	protected ExecutorService threadExecutors;
	protected AtomicBoolean isAlive;

	public KOServer(ServerConfiguration configuration)
	{
		init(configuration);
	}

	private void init(ServerConfiguration configuration) throws ConnectivityException
	{
		this.configuration = configuration;
		this.requestQueue = new LinkedBlockingQueue<MessageInfo>();
		this.bootstrap = new ServerBootstrap(new NioServerSocketChannelFactory(Executors.newCachedThreadPool(), Executors.newCachedThreadPool()));
		this.channelIdSeq = new AtomicLong(0);
		this.channelPipelineMap = new ConcurrentHashMap<Long, ChannelPipeline>();
		this.connectedAccountsMap = new TwoSidesMap<String, Long>();

		// Set up the pipeline factory.
		this.bootstrap.setOption(BootstrapConstants.TCP_NO_DELAY, true);
		this.bootstrap.setOption(BootstrapConstants.RECEIVE_BUFFER_SIZE, configuration.getReceiveBufferSize());

		System.out.println(String.format(SERVER_CONFIGURATION_HANDLER_TYPE, configuration.getHandlerType().toString()));

		PipelineFactory channelPipelineFactory = PipelineFactoryBuilder.buildChannelPipelineFactory(configuration.getHandlerType());

		System.out.println(String.format(CREATED_PIPELINE_FACTORY, channelPipelineFactory.getClass().getName()));

		channelPipelineFactory.init(this, requestQueue, channelIdSeq);

		bootstrap.setPipelineFactory(channelPipelineFactory);

		// Bind and start to accept incoming connections.
		this.serverChannel = bootstrap.bind(new InetSocketAddress(configuration.getIp(), configuration.getPort()));

		System.out.println(String.format(SERVER_LISTEN, configuration.getIp(), configuration.getPort()));

		this.isAlive = new AtomicBoolean(true);
		this.threadExecutors = Executors.newFixedThreadPool(1);
		this.threadExecutors.submit(this);
	}

	public void shutdown()
	{
		this.serverChannel.disconnect();
		this.serverChannel.close();
		this.bootstrap.releaseExternalResources();
	}

	@Override
	public void connected(long channelId, ChannelHandlerContext ctx)
	{
		if (!channelPipelineMap.containsKey(channelId))
		{
			channelPipelineMap.put(channelId, ctx.getPipeline());
			System.out.println(String.format(CLIENT_CONNECTED, channelId));
		}
	}

	@Override
	public void disconnected(long channelId, ChannelHandlerContext ctx)
	{
		ChannelPipeline channelPipeline = channelPipelineMap.get(channelId);

		if (channelPipeline != null)
		{
			String username = connectedAccountsMap.getByValue(channelId);
			
			if(null != username)
			{
				IOnlineUserDAO onlineUserDAO = (IOnlineUserDAO)KOApplicationContext.getInstance().getApplicationContext().getBean("onlineUserHibernateDAO");
				onlineUserDAO.deleteOnlineUser(username);
			}
			
			channelPipelineMap.remove(channelId);
			connectedAccountsMap.removeByValue(channelId);
			System.out.println(String.format(CLIENT_DISCONNECTED, channelId));
		}
	}

	@Override
	public void run()
	{
		Thread.currentThread().setName(RECIEVER_THREAD);

		try
		{
			while (isAlive.get())
			{
				MessageInfo messageInfo = requestQueue.take();

				byte[] array = (byte[]) messageInfo.getRequest();

				ByteBuffer currBuff = ByteBuffer.allocate(array.length);
				currBuff.order(ByteOrder.LITTLE_ENDIAN);
				currBuff.put(array);
				currBuff.rewind();

				byte[] opcodeByte = new byte[1];
				currBuff.get(opcodeByte);

				short opcode = PacketUtils.byteArrayToShort(new byte[] { opcodeByte[0] }, 0);

				byte[] data = new byte[currBuff.remaining()];
				currBuff.get(data);

				Packet packet = new Packet(opcode, messageInfo);
				packet.setData(data);

				configuration.getPacketHandler().addWork(packet);
			}
		}
		catch (Exception e)
		{
			if (isAlive.get())
			{
				e.printStackTrace();
			}
		}
	}

	@Override
	public void sendAsyncResponseMessage(MessageInfo messageInfo) throws ConnectivityException
	{
		sendResponseMessage(messageInfo, false);
	}

	private void sendResponseMessage(MessageInfo messageInfo, boolean sync) throws ConnectivityException
	{
		Serializable response = messageInfo.getResponse();

		if (response == null)
		{
			throw new ConnectivityException(RESPONSE_OBJECT_IS_EMPTY);
		}

		ChannelPipeline channelPipeline = channelPipelineMap.get(messageInfo.getChannelId());

		Serializable message = messageInfo.getResponse();

		if (channelPipeline != null)
		{
			if (channelPipeline.getChannel().isConnected())
			{
				ChannelFuture channelFuture = channelPipeline.getChannel().write(message);

				if (sync)
				{
					boolean isCompleted = channelFuture.awaitUninterruptibly(this.configuration.getMsgTimeout(), TimeUnit.MILLISECONDS);

					if (!isCompleted)
					{
						throw new ConnectivityException(String.format(TIMEOUT_MS, this.configuration.getMsgTimeout()));
					}
				}
			}

			else
			{
				channelPipelineMap.remove(messageInfo.getChannelId());
				throw new ConnectivityException(String.format(CHANNEL_ID_DISCONNECTED, messageInfo.getChannelId()));
			}
		}

		else
		{
			throw new ConnectivityException(String.format(CHANNEL_ID_IS_NOT_DEFINED, messageInfo.getChannelId()));
		}
	}

	public int getConnected()
	{
		return channelPipelineMap.size();
	}

	public int getConnectedAccounts()
	{
		return connectedAccountsMap.size();
	}

	public void connectAccount(String account, Long id)
	{
		connectedAccountsMap.put(account, id);
	}

	public boolean isConnectedAccount(String account)
	{
		return null != connectedAccountsMap.getByKey(account);
	}

	public boolean isConnectedAccount(Long channelId)
	{
		return null != connectedAccountsMap.getByValue(channelId);
	}

	public void killAccount(String username)
	{
		Long channelId = connectedAccountsMap.getByKey(username);

		if (null != channelId)
		{
			killConnection(channelId);
		}
	}

	public void killConnection(Long channelId)
	{
		if (null != channelId)
		{
			ChannelPipeline channelPipeline = channelPipelineMap.get(channelId);

			if (null != channelPipeline)
			{
				channelPipeline.getChannel().close();
			}
		}
	}
}
