package mserver.plugin;

import java.net.URLClassLoader;

public interface ApplicationClassLoaderProvider {
    URLClassLoader get();
}
