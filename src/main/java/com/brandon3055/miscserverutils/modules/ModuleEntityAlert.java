package com.brandon3055.miscserverutils.modules;

import com.brandon3055.miscserverutils.LogHelper;
import com.brandon3055.miscserverutils.ModEventHandler;
import net.minecraft.command.CommandBase;
import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.entity.Entity;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.world.World;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.config.Configuration;
import net.minecraftforge.event.entity.EntityJoinWorldEvent;
import net.minecraftforge.fml.common.event.FMLServerStartingEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by brandon3055 on 10/12/2016.
 */
public class ModuleEntityAlert extends SUModuleBase {

    public int alertPeriod = 20; //Ticks
    public int alertCount = 1000; //Entities
    public boolean enabled = false;
    private Configuration config;
//    private List<SpawnData> newEntities = new ArrayList<>();
    private List<SpawnData> spawnDataList = new ArrayList<>();

    public ModuleEntityAlert() {
        super("entityAlert", "Will generate a report if more than a defined number of entities spawns within a defined period of time.");
        addListener(ModEventHandler.EventType.SERVER_TICK);
    }

    @Override
    public void initialize() {
        super.initialize();
        MinecraftForge.EVENT_BUS.register(this);
    }

    @Override
    public void loadConfig(Configuration config) {
        alertPeriod = config.get("ModuleEntityAlert", "alertPeriod", alertPeriod, "'X' number of entities need to be added within this many ticks to trigger an alert.").getInt(alertPeriod);
        alertCount = config.get("ModuleEntityAlert", "alertCount", alertCount, "This is the number of entities that must spawn within the alert period to trigger an alert.").getInt(alertCount);
        this.config = config;
    }

    @Override
    public void registerCommands(FMLServerStartingEvent event) {
        event.registerServerCommand(new CommandBase() {
            @Override
            public String getName() {
                return "msu_entity_alert";
            }

            @Override
            public String getUsage(ICommandSender sender) {
                return "/msu_entity_alert";
            }

            @Override
            public void execute(MinecraftServer server, ICommandSender sender, String[] args) throws CommandException {
                if (args.length == 2) {
                    if (args[0].equalsIgnoreCase("setPeriod")) {
                        alertPeriod = parseInt(args[1], 1, 10000);
                        config.get("ModuleEntityAlert", "alertPeriod", alertPeriod, "'X' number of entities need to be added within this many ticks to trigger an alert.").set(alertPeriod);
                        config.save();
                        sender.sendMessage(new TextComponentString("Period set to " + alertPeriod + " Ticks!"));
                        return;
                    }
                    else if (args[0].equalsIgnoreCase("setCount")) {
                        alertCount = parseInt(args[1], 1, 10000);
                        config.get("ModuleEntityAlert", "alertCount", alertCount, "This is the number of entities that must spawn within the alert period to trigger an alert.").set(alertCount);
                        config.save();
                        sender.sendMessage(new TextComponentString("Count set to " + alertCount + " Entities!"));
                        return;
                    }
                }
                else if (args.length == 1 && args[0].equalsIgnoreCase("enable")) {
                    enabled = true;
                    sender.sendMessage(new TextComponentString("Entity Alert is now enabled"));
                    return;
                }
                else if (args.length == 1 && args[0].equalsIgnoreCase("disable")) {
                    enabled = false;
                    sender.sendMessage(new TextComponentString("Entity Alert is now disabled"));
                    return;
                }

                sender.sendMessage(new TextComponentString("Usage:"));
                sender.sendMessage(new TextComponentString("/msu_entity_alert enable"));
                sender.sendMessage(new TextComponentString("/msu_entity_alert disable"));
                sender.sendMessage(new TextComponentString("/msu_entity_alert setPeriod <number>"));
                sender.sendMessage(new TextComponentString(" -X' number of entities need to be added within this many ticks to trigger an alert."));
                sender.sendMessage(new TextComponentString("/msu_entity_alert setCount <number>"));
                sender.sendMessage(new TextComponentString(" -This is the number of entities that must spawn within the alert period to trigger an alert.\n"));
                sender.sendMessage(new TextComponentString("Entity Alert is currently " + (enabled ? "Enabled" : "Disabled")));
                sender.sendMessage(new TextComponentString("Current Period: " + alertPeriod + " Ticks"));
                sender.sendMessage(new TextComponentString("Current Count: " + alertCount + " Entities"));
            }

            @Override
            public List<String> getTabCompletions(MinecraftServer server, ICommandSender sender, String[] args, @Nullable BlockPos targetPos) {
                return getListOfStringsMatchingLastWord(args, "setPeriod", "setCount", "enable", "disable");
            }
        });
    }

//    @SideOnly(Side.SERVER)
    @SubscribeEvent
    public void onEvent(EntityJoinWorldEvent event) {
        if (event.getWorld().isRemote || !enabled) return;
        spawnDataList.add(new SpawnData(tick, event.getEntity(), event.getWorld()));
    }

    private long tick = 0;

//    @SideOnly(Side.SERVER)
    @Override
    public void onEvent(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }

        spawnDataList.removeIf(spawnData -> tick - spawnData.spawnTick > alertPeriod);
//        spawnDataList.addAll(newEntities);
//        newEntities.clear();

        if (spawnDataList.size() > alertCount) {
            LogHelper.warn("################################################################################");
            LogHelper.warn("# MSU Entity Alert has been triggered!");
            LogHelper.warn("# What follows is a summary of the entities that triggered this alert.");
            LogHelper.warn("################################################################################");
            LogHelper.warn("Detected " + spawnDataList.size() + " Spawns within a period of " + alertPeriod + " Ticks.");
            spawnDataList.forEach(spawnData -> LogHelper.warn(spawnData.toString()));
            LogHelper.warn("################################################################################");
            spawnDataList.clear();
        }

        tick++;
    }

    private static class SpawnData {
        private long spawnTick;
        private String entityClass;
        private BlockPos pos;
        private int dim;

        public SpawnData(long spawnTick, Entity entity, World world) {
            this.spawnTick = spawnTick;
            entityClass = entity.getClass().getName();
            pos = new BlockPos(entity);
            dim = world.provider.getDimension();
        }

        @Override
        public String toString() {
            return entityClass + "@" + pos.toString() + " in dimension " + dim;
        }
    }
}
