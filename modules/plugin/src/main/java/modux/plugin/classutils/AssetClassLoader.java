package modux.plugin.classutils;

import org.apache.xbean.classloader.NamedClassLoader;

import java.io.IOException;
import java.net.URL;
import java.util.Enumeration;

public class AssetClassLoader extends ClassLoader {

    private NamedClassLoader delegated;
    private URL[] paths;

    public AssetClassLoader(ClassLoader parent, URL[] paths) {
        super(parent);
        this.paths = paths;
    }

    public void killDelegated() {
        try {
            if (this.delegated != null) {
                this.delegated.destroy();
                this.delegated.close();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void reload() {
        setDelegated(new NamedClassLoader("resources-classloader", paths, getParent()));
    }

    public void setDelegated(NamedClassLoader delegated) {
        killDelegated();
        this.delegated = delegated;
    }


    @Override
    public URL getResource(String name) {
        URL resource = delegated.getResource(name);
        return resource == null ? super.getResource(name) : resource;
    }

    @Override
    protected Enumeration<URL> findResources(String name) throws IOException {
        Enumeration<URL> resource = delegated.getResources(name);
        return resource == null ? getParent().getResources(name) : resource;
    }

    @Override
    protected URL findResource(String name) {
        URL resources = delegated.getResource(name);
        return resources == null ? getParent().getResource(name) : resources;
    }
}
