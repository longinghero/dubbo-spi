package com.longing.spi.util;

public class Holder<T>{
	
	private volatile T value;

	public T getValue() {
		return value;
	}

	public void setValue(T value) {
		this.value = value;
	}
	
}
