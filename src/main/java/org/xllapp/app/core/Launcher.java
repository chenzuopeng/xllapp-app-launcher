package org.xllapp.app.core;

import java.io.File;
import java.io.FilenameFilter;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.security.AccessController;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * 
 * 启动类.
 * 
 * @author dylan.chen Aug 27, 2012
 * 
 */
public class Launcher {

	private static final Logger			logger	= Logger.getLogger(Launcher.class.getName());

	private static final ClassLoader	loader;

	static {
		String homeDir = System.getProperty("app.home", "");
		if (homeDir != null && "".equals(homeDir.trim())) {
			File userDir = new File(System.getProperty("user.dir"));
			homeDir = userDir.getAbsolutePath();
		}
		logger.info("homeDir:" + homeDir);
		File libDir = new File(homeDir, "lib");
		logger.info("libDir:" + libDir.getAbsolutePath());
		File[] libJars = libDir.listFiles(new FilenameFilter() {
			@Override
			public boolean accept(File dir, String name) {
				return name.endsWith(".jar");
			}
		});
		if (libJars == null) {
			new Throwable("can not access " + libDir).printStackTrace();
		}
		Arrays.sort(libJars); // 确保加载的jar包的顺序是可以预期的

		final List<URL> jars = new LinkedList<URL>();
		try {
			jars.add(new File(homeDir).toURI().toURL()); // 添加当前目录
			logger.info("load dir[" + homeDir + "] to classpath");
			File confDir = new File(homeDir, "conf");
			jars.add(confDir.toURI().toURL()); // 添加配置文件目录
			logger.info("load dir[" + confDir.getAbsolutePath() + "] to classpath");
		} catch (MalformedURLException e) {
		}
		for (int i = 0; i < libJars.length; i++) {
			try {
				String s = libJars[i].getPath();
				jars.add(new File(s).toURI().toURL()); // See Java bug 4496398
				logger.info("load jar[" + s + "] to classpath");
			} catch (MalformedURLException e) {
				logger.log(Level.SEVERE, e.getLocalizedMessage(), e);
			}
		}

		loader = AccessController.doPrivileged(new java.security.PrivilegedAction<ClassLoader>() {
			@Override
			public ClassLoader run() {
				return new URLClassLoader(jars.toArray(new URL[0]));
			}
		});
	}

	/**
	 * startClass参数指定的类必须实现java.lang.Runnable接口.
	 */
	public static void main(String[] args) {
		try {
			String startClassName = System.getProperty("startClass");
			logger.info("startClass:" + startClassName);
			if (startClassName == null || "".equals(startClassName.trim())) {
				throw new RuntimeException("startClass parameter cannot be null.");
			}
			Thread.currentThread().setContextClassLoader(loader);
			Class<?> startClass = loader.loadClass(startClassName);
			Object instance = startClass.newInstance();
			Method startup = startClass.getMethod("run", (Class<?>[]) null);
			startup.invoke(instance, (Object[]) null);
		} catch (Exception e) {
			logger.log(Level.SEVERE, e.getLocalizedMessage(), e);
		}
	}

}
