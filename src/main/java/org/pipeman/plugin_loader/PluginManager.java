package org.pipeman.plugin_loader;

import org.json.JSONObject;
import org.pipeman.lib.Plugin;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Timer;
import java.util.TimerTask;
import java.util.zip.ZipFile;

public class PluginManager {
    private final ArrayList<Class<?>> pluginMains = new ArrayList<>();
    private final ArrayList<Plugin> enabledPlugins = new ArrayList<>();

    private void loadPlugin(File jar) throws IOException, ClassNotFoundException {
        String mainClass;
        try (ZipFile zip = new ZipFile(jar); InputStream stream = zip.getInputStream(zip.getEntry("plugin.json"))) {
            String content = new String(stream.readAllBytes());
            JSONObject obj = new JSONObject(content);
            mainClass = obj.getString("main");
        }

        ClassLoader l = URLClassLoader.newInstance(new URL[]{jar.toURI().toURL()}, getClass().getClassLoader());

        Class<?> clazz = l.loadClass(mainClass);
        pluginMains.add(clazz);
    }

    public void enablePlugins() {
        for (Class<?> clazz : pluginMains) {
            try {
                Plugin plugin = (Plugin) clazz.getDeclaredConstructor().newInstance();
                plugin.onEnable();
                enabledPlugins.add(plugin);
            } catch (Exception ignored) {}
        }
    }

    public void disablePlugins() {
        for (Plugin plugin : enabledPlugins) {
            try {
                plugin.onDisable();
            } catch (Exception ignored) {}
        }
    }

    public void loadPlugins() throws IOException, ClassNotFoundException {
        File pluginDir = new File("plugins");
        File[] files = pluginDir.listFiles((dir, name) -> name.endsWith(".jar"));
        for (File f : files) {
            loadPlugin(f);
        }
    }

    public void unloadPlugins() {
        enabledPlugins.clear();
        pluginMains.clear();
    }

    public static void main(String[] args) throws IOException, ClassNotFoundException {
        PluginManager pm = new PluginManager();
        pm.loadPlugins();
        pm.enablePlugins();
        pm.disablePlugins();
        new Timer().schedule(new TimerTask() {
            @Override
            public void run() {
                pm.unloadPlugins();
            }
        }, 5000);
    }
}
