package com.knightonline.shared.helper;

import java.io.File;
import java.util.regex.Pattern;

import org.springframework.stereotype.Component;

/**
 * @author Mamaorha
 *
 */
@Component
public class FileHelper
{
	public File createDir(String path)
	{
		path = path.replace("/", "\\").replace("\\", File.separator);
		
		String[] folders = path.split(Pattern.quote(File.separator));
		
		File dir = null;
		
		for (String folder : folders)
		{
			if(null == dir)
			{
				dir = new File(path);
			}
			
			else
			{
				dir = new File(dir.getPath() + File.separator + folder);
			}
			
			if(dir.exists() && !dir.isDirectory())
			{
				throw new IllegalArgumentException(String.format("Couldn't create dir at the given path [%s]", dir.getPath()));
			}
			
			dir.mkdirs();
			
			if(!dir.exists())
			{
				throw new InternalError("Couldn't create dir at the given path [%s]");
			}
		}
		
		return dir;
	}
}
