package com.longing.spi.test;

import com.longing.spi.loader.TobyExtrnsionLoader;
import com.longing.spi.log.Log;

public class SpiTest {
	
	public static void main(String[] args){
		  Log defaultExtension = TobyExtrnsionLoader.
	                getExtensionLoader(Log.class).
	                getDefaultExtension();
	        defaultExtension.say();

	        //ָ���ض���ʵ����,�������õ�tobyLog
	        Log tobyLog = TobyExtrnsionLoader.
	                getExtensionLoader(Log.class).
	                getExtension("tobyLog");
	        tobyLog.say();
	}
}
