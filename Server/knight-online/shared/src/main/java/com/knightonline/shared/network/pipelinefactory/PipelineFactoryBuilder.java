package com.knightonline.shared.network.pipelinefactory;

import com.knightonline.shared.exception.ConnectivityException;
import com.knightonline.shared.network.common.HandlerTypeEnum;

/**
 * @author Mamaorha
 *
 */
public class PipelineFactoryBuilder
{
	private static final String UNSUPPORTED_HANDLER_TYPE = "Unsupported handler type [%s]";

	public static PipelineFactory buildChannelPipelineFactory(HandlerTypeEnum handlerType) throws ConnectivityException
	{
		if (HandlerTypeEnum.ByteBuffer == handlerType)
		{
			return new BytesPipelineFactory();
		}

		throw new ConnectivityException(String.format(UNSUPPORTED_HANDLER_TYPE, handlerType.toString()));
	}
}
