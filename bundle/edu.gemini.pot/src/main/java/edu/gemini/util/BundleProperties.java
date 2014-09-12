package edu.gemini.util;

import java.util.NoSuchElementException;

import org.osgi.framework.BundleContext;

public class BundleProperties {

	private final BundleContext context;

	public BundleProperties(BundleContext context) {
		this.context = context;
	}
	
	public String getString(String key, String defaultValue) {
		String value = context.getProperty(key);
		return value != null ? value : defaultValue;
	}
	
	public String getString(String key) {
		String value = context.getProperty(key);
		if (value == null) throw new NoSuchElementException(key);
		return value;
	}
	
	public int getInt(String key, int defaultValue) {
		String value = context.getProperty(key);
		return value != null ? Integer.valueOf(value) : defaultValue;
	}

	public int getInt(String key) {
		return Integer.valueOf(getString(key));
	}
	
	public boolean getBoolean(String key) {
		return Boolean.valueOf(getString(key));
	}
	
	public boolean getBoolean(String key, boolean defaultValue) {
		String value = context.getProperty(key);
		return value != null ? Boolean.valueOf(value) : defaultValue;
	}
	
	@SuppressWarnings("unchecked")
	public <T extends Enum> T getEnum(String key, T defaultValue, Class<T> enumType) {
		String value = context.getProperty(key);
		return value != null ? (T) Enum.valueOf(enumType, value) : defaultValue;
	}
	
	@SuppressWarnings("unchecked")
	public <T extends Enum> T getEnum(String key, Class<T> enumType) {
		return (T) Enum.valueOf(enumType, getString(key));
	}
	
}
