package modux.plugin.classutils;

import java.net.URLClassLoader;

public interface ApplicationClassLoaderProvider {
    URLClassLoader get();
}
