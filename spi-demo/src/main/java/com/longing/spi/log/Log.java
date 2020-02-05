package com.longing.spi.log;

import com.longing.spi.annotion.TobySpi;

@TobySpi("longingLog")
public interface Log {
	
	public void say();
}
