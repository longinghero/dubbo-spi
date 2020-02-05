package com.longing.spi.loader;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.URL;
import java.rmi.server.LoaderHandler;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.regex.Pattern;

import com.longing.spi.annotion.TobySpi;
import com.longing.spi.util.Holder;

public class TobyExtrnsionLoader<T>{

	private static final String DIRECTORY = "META-INF/";

	private static final Pattern NAME_SPERATOR = Pattern.compile("\\s*[,]+\\s*");

	private static final ConcurrentMap<Class<?>, TobyExtrnsionLoader<?>> EXTENSION_LOADERS =
			new ConcurrentHashMap<Class<?>, TobyExtrnsionLoader<?>>();

	private static final ConcurrentMap<Class<?>, Object> EXTENSION_INSTANCES = new ConcurrentHashMap<Class<?>, Object>();

	private final Class<?> type;

	private String cachedDefaultName;

	private Map<String,IllegalStateException> exceptions = new ConcurrentHashMap<String,IllegalStateException>();

	private final Holder<Map<String,Class<?>>> cachedClasses = new Holder<Map<String,Class<?>>>();

	private final ConcurrentMap<String,Holder<Object>> cachedInstances = new ConcurrentHashMap<String, Holder<Object>>();




	private static <T> boolean withExtensionAnnotation(Class<T> type){
		return type.isAnnotationPresent(TobySpi.class);
	}

	private TobyExtrnsionLoader(Class<T> type){
		this.type = type;
	}
	@SuppressWarnings("unchecked")
	public static <T> TobyExtrnsionLoader<T> getExtensionLoader(Class<T> type){
		if(type == null){
			throw new IllegalArgumentException("Extension type == null");

		}
		if(!type.isInterface()){
			throw new IllegalArgumentException("Extension type["+type+"] is not interface");
		}
		if(!withExtensionAnnotation(type)){
			throw new IllegalArgumentException("Extension type["+type+"] is not annotion");
		}
		TobyExtrnsionLoader<T> loader = (TobyExtrnsionLoader<T>) EXTENSION_LOADERS.get(type);
		if(loader == null){
			EXTENSION_LOADERS.putIfAbsent(type, new TobyExtrnsionLoader<T>(type));
			loader  = (TobyExtrnsionLoader<T>) EXTENSION_LOADERS.get(type);
		}
		return loader;
	}

	@SuppressWarnings("unchecked")
	public T getExtension(String name){
		if(name == null || name.length() == 0){
			throw new IllegalArgumentException("Extension name == null");
		}
		if("true".equals(name)){
			return getDefaultExtension();
		}
		Holder<Object> holder  = cachedInstances.get(name);
		if(holder == null){
			cachedInstances.putIfAbsent(name, new Holder<Object>());
			holder  = cachedInstances.get(name);
		}
		Object instance = holder.getValue();
		if(instance == null){
			synchronized (holder) {
				instance = holder.getValue();
				if(instance == null){
					instance = createExtension(name);
					holder.setValue(instance);
				}
			}
		}
		return (T) instance;
	}

	private T createExtension(String name)  {
		// TODO Auto-generated method stub
		Class<?> clazz = getExtensionClasses().get(name);
		if(clazz == null){
			throw findException(name);
		}
		try{
			T instance = (T) EXTENSION_INSTANCES.get(clazz);
			if(instance == null){
				EXTENSION_INSTANCES.putIfAbsent(clazz, clazz.newInstance());
				instance = (T) EXTENSION_INSTANCES.get(clazz);
			}
			return instance;
		}catch(Throwable t){
			throw new IllegalStateException("Extension instance(name: " + name + ", class: " +
					type + ")  could not be instantiated: " + t.getMessage(), t);
		}

	}
	private IllegalStateException findException(String name) {
		// TODO Auto-generated method stub
		for (Map.Entry<String, IllegalStateException> entry : exceptions.entrySet()) {
			if (entry.getKey().toLowerCase().contains(name.toLowerCase())) {
				return entry.getValue();
			}
		}
		StringBuilder buf = new StringBuilder("No such extension " + type.getName() + " by name " + name);

		int i = 1;
		for (Map.Entry<String, IllegalStateException> entry : exceptions.entrySet()) {
			if (i == 1) {
				buf.append(", possible causes: ");
			}

			buf.append("\r\n(");
			buf.append(i++);
			buf.append(") ");
			buf.append(entry.getKey());
			buf.append(":\r\n");
			buf.append(entry.getValue().toString());
		}
		return new IllegalStateException(buf.toString());
	}

	private Map<String,Class<?>> getExtensionClasses() {

		// TODO Auto-generated method stub
		Map<String,Class<?>> classes = cachedClasses.getValue();
		if(classes == null){
			synchronized (cachedClasses) {
				classes = cachedClasses.getValue();
				if(classes == null){
					classes = loadExtensionClasses();
					cachedClasses.setValue(classes);
				}
			}
		}
		return classes;
	}

	private Map<String, Class<?>> loadExtensionClasses() {
		// TODO Auto-generated method stub
		final TobySpi defaultAnnotation = type.getAnnotation(TobySpi.class);
		if(defaultAnnotation != null){
			String value = defaultAnnotation.value();
			if(value != null && (value = value.trim()).length()>0){
				String[] names = NAME_SPERATOR.split(value);
				if(names.length>1){
					throw new IllegalStateException("more than 1 default extension name "+type.getName());
				}
				if(names.length == 1){
					cachedDefaultName = names[0];
				}
			}
		}
		Map<String,Class<?>> extensionClasses = new HashMap<String, Class<?>>();
		loadFile(extensionClasses,DIRECTORY);
		return extensionClasses;
	}

	private void loadFile(Map<String, Class<?>> extensionClasses,
			String directory) {
		// TODO Auto-generated method stub
		String fileName = directory + type.getName();

		try {
            Enumeration<URL> urls;
            ClassLoader classLoader = findClassLoader();
            if (classLoader != null) {
                urls = classLoader.getResources(fileName);
            } else {
                urls = ClassLoader.getSystemResources(fileName);
            }
            if (urls != null) {
                while (urls.hasMoreElements()) {
                    java.net.URL url = urls.nextElement();
                    try {
                        BufferedReader reader = new BufferedReader(new InputStreamReader(url.openStream(), "utf-8"));
                        try {
                            String line = null;
                            while ((line = reader.readLine()) != null) {
                                final int ci = line.indexOf('#');
                                if (ci >= 0) line = line.substring(0, ci);
                                line = line.trim();
                                if (line.length() > 0) {
                                    try {
                                        String name = null;
                                        int i = line.indexOf('=');
                                        if (i > 0) {
                                            name = line.substring(0, i).trim();
                                            line = line.substring(i + 1).trim();
                                        }
                                        if (line.length() > 0) {
                                            Class<?> clazz = Class.forName(line, true, classLoader);
                                            if (!type.isAssignableFrom(clazz)) {
                                                throw new IllegalStateException("Error when load extension class(interface: " +
                                                        type + ", class line: " + clazz.getName() + "), class "
                                                        + clazz.getName() + "is not subtype of interface.");
                                            }
                                            extensionClasses.put(name, clazz);//加入缓存
                                        }//源码中还有其他的判断,这个版本暂不实现
                                    } catch (Throwable t) {
                                        IllegalStateException e = new IllegalStateException("Failed to load extension class(interface: " + type + ", class line: " + line + ") in " + url + ", cause: " + t.getMessage(), t);
                                        exceptions.put(line, e);
                                    }
                                }
                            } // end of while read lines
                        } finally {
                            reader.close();
                        }
                    } catch (Throwable t) {
                        //logger.error("Exception when load extension class(interface: " +
                        //        type + ", class file: " + url + ") in " + url, t);
                    }
                } // end of while urls
            }
        } catch (Throwable e) {
            //logger.error("Exception when load extension class(interface: " + type + ", description file: " + fileName + ").", e);
        }
	}

	private ClassLoader findClassLoader() {
		// TODO Auto-generated method stub
		return TobyExtrnsionLoader.class.getClassLoader();
	}

	public T getDefaultExtension() {
		// TODO Auto-generated method stub
		getExtensionClasses();
		if(null == cachedDefaultName || cachedDefaultName.length() == 0
				|| "true".equals(cachedDefaultName)){
			return null;
		}
		return getExtension(cachedDefaultName);
	}
}
