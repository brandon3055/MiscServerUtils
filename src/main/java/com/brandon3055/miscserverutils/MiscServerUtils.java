package com.brandon3055.miscserverutils;

import com.brandon3055.miscserverutils.commands.CommandTPOfflinePlayer;
import com.brandon3055.miscserverutils.modules.ModuleDeleteScheduler;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.config.Configuration;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.common.event.FMLServerStartingEvent;
import net.minecraftforge.fml.common.network.NetworkCheckHandler;
import net.minecraftforge.fml.relauncher.Side;

import java.io.File;
import java.util.Map;

@Mod(modid = MiscServerUtils.MODID, name = MiscServerUtils.MODNAME,version = MiscServerUtils.VERSION)
public class MiscServerUtils
{
    public static final String MODID = "miscserverutils";
	public static final String MODNAME = "Misc Server Utils";
    public static final String VERSION = "${mod_version}";
    public static File modConfigDir;
    public static Configuration configuration;

    private void registerModules() {
        ModuleRegistry.register(new ModuleDeleteScheduler());
    }



    @NetworkCheckHandler
    public boolean networkCheck(Map<String, String> map, Side side) {
        return true;
    }

    @Mod.EventHandler
    public void serverStart(FMLServerStartingEvent event) {
        ModuleRegistry.registerCommands(event);
        event.registerServerCommand(new CommandTPOfflinePlayer());
    }

    @Mod.EventHandler
    public void preInit(FMLPreInitializationEvent event) {
        modConfigDir = new File(event.getModConfigurationDirectory(), "brandon3055/MiscServerUtils");
        if (!modConfigDir.exists()) {
            modConfigDir.mkdirs();
        }
        configuration = new Configuration(new File(modConfigDir, "MiscServerUtils.cfg"));
        registerModules();
        ModuleRegistry.loadModules(configuration);
        MinecraftForge.EVENT_BUS.register(new ModEventHandler());
    }
}
