package com.brandon3055.miscserverutils;

import com.brandon3055.miscserverutils.modules.SUModuleBase;
import net.minecraftforge.common.config.Configuration;
import net.minecraftforge.fml.common.event.FMLServerStartingEvent;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by brandon3055 on 27/11/2016.
 */
public class ModuleRegistry {
    private static final Map<SUModuleBase, Boolean> REGISTRY = new HashMap<SUModuleBase, Boolean>();
    private static boolean registryInitialized = false;

    public static void register(SUModuleBase module) {
        register(module, true);
    }

    public static void register(SUModuleBase module, boolean enabledByDefault) {
        if (registryInitialized) {
            throw new RuntimeException("Modules must be registered in preInit before the registry is initialized!");
        }
        REGISTRY.put(module, enabledByDefault);
    }

    public static void loadModules(Configuration config) {
        config.setCategoryComment("Modules", "This section allows you do disable/enable any of the available ServerUtils Modules.");
        for (SUModuleBase module : REGISTRY.keySet()) {
            boolean enabled = config.get("Modules", module.moduleID, REGISTRY.get(module), module.moduleDescription).getBoolean(REGISTRY.get(module));
            REGISTRY.put(module, enabled);

            if (enabled) {
                module.initialize();
            }
        }

        loadModuleConfig(config);

        registryInitialized = true;
    }

    public static void loadModuleConfig(Configuration config) {
        for (SUModuleBase module : REGISTRY.keySet()) {
//            if (REGISTRY.get(module)) {
                module.loadConfig(config);
//            }
        }

        if (config.hasChanged()) {
            config.save();
        }
    }

    public static void registerCommands(FMLServerStartingEvent event) {
        for (SUModuleBase module : REGISTRY.keySet()) {
            if (REGISTRY.get(module)) {
                module.registerCommands(event);
            }
        }
    }
}
