package com.brandon3055.miscserverutils.modules;

import com.brandon3055.miscserverutils.LogHelper;
import com.brandon3055.miscserverutils.ModEventHandler;
import com.brandon3055.miscserverutils.commands.CommandScheduleStop;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.text.Style;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextFormatting;
import net.minecraftforge.common.config.Configuration;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.event.FMLServerStartingEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Arrays;

import static java.time.temporal.ChronoUnit.SECONDS;

/**
 * Created by brandon3055 on 10/12/2016.
 */
public class ModuleAutoShutdown extends SUModuleBase {

    public static LocalDateTime startTime;
    private LocalDateTime tpsDropTime = null;
    private int tick = 0;

    public static boolean enableTimedShutdown = true;
    public static boolean timedShutdownConfig = true;
    public static int shutdownDelayMins = 60;
    public static int shutdownDelayConfig = 60;
    public static String preShutdownMessage = "The server will be rebooting in %s";
    public boolean enableTPSShutdown = true;
    public int tpsOverSeconds = 60;
    public int tpsThreshold = 10;
    public String preTPSShutdownMessage = "The server is rebooting due to low TPS";

    public ModuleAutoShutdown() {
        super("autoServerShutdown", "Will automatically shut down the server after a set uptime. On its own this may not be very useful but if combined with an auto restart script this can be used to automatically restart a server. This can also be configured to stop the server if the average tps over x amount of time drops bellow y.");
        addListener(ModEventHandler.EventType.SERVER_TICK);
    }

    @Override
    public void initialize() {
        super.initialize();
    }

    @Override
    public void loadConfig(Configuration config) {
        timedShutdownConfig = config.get("ModuleAutoShutdown", "enableTimedShutdown", timedShutdownConfig, "Enable the Auto shutdown feature.").getBoolean(timedShutdownConfig);
        enableTimedShutdown = timedShutdownConfig;
        shutdownDelayConfig = config.get("ModuleAutoShutdown", "shutdownDelayMins", shutdownDelayConfig, "Sets how many minutes after startup the server will shutdown.").getInt(shutdownDelayConfig);
        shutdownDelayMins = shutdownDelayConfig;
        preShutdownMessage = config.get("ModuleAutoShutdown", "preShutdownMessage", preShutdownMessage, "This is the message that will be displayed before a scheduled shutdown. %s will be replaced with the time till shutdown.").getString();
        enableTPSShutdown = config.get("ModuleAutoShutdown", "enableTPSShutdown", enableTPSShutdown, "Enables the low tps shutdown feature.").getBoolean(enableTPSShutdown);
        tpsOverSeconds = config.get("ModuleAutoShutdown", "tpsOverSeconds", tpsOverSeconds, "How many seconds to average the tps over. (Note this time is dependent on tick time)").getInt(tpsOverSeconds);
        tpsThreshold = config.get("ModuleAutoShutdown", "tpsThreshold", tpsThreshold, "The minimum average tps before the server shuts down.").getInt(tpsThreshold);
        preTPSShutdownMessage = config.get("ModuleAutoShutdown", "preTPSShutdownMessage", preTPSShutdownMessage, "This message will be displayed immediately before the server shuts down due to low tps.").getString();
    }

    @Override
    public void registerCommands(FMLServerStartingEvent event) {
        event.registerServerCommand(new CommandScheduleStop());
        startTime = Instant.now().atZone(ZoneId.systemDefault()).toLocalDateTime();
        warningState = 0;

        tpsCounter = new double[tpsOverSeconds];
        Arrays.fill(tpsCounter, 20D);
    }

    public static int warningState = 0;
    private double[] tpsCounter;

    @Override
    public void onEvent(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }

        MinecraftServer server = FMLCommonHandler.instance().getMinecraftServerInstance();

        //region Shutdown
        if (enableTimedShutdown) {
            LocalDateTime now = Instant.now().atZone(ZoneId.systemDefault()).toLocalDateTime();
            long sts = (shutdownDelayMins * 60) - startTime.until(now, SECONDS);

            if (sts < 10 * 60 && warningState == 0) {
                server.getPlayerList().sendChatMsg(new TextComponentString(String.format(preShutdownMessage, "10 Minutes")).setStyle(new Style().setColor(TextFormatting.RED)));
                warningState++;
            }
            else if (sts < 5 * 60 && warningState == 1) {
                server.getPlayerList().sendChatMsg(new TextComponentString(String.format(preShutdownMessage, "5 Minutes")).setStyle(new Style().setColor(TextFormatting.RED)));
                warningState++;
            }
            else if (sts < 4 * 60 && warningState == 2) {
                server.getPlayerList().sendChatMsg(new TextComponentString(String.format(preShutdownMessage, "4 Minutes")).setStyle(new Style().setColor(TextFormatting.RED)));
                warningState++;
            }
            else if (sts < 3 * 60 && warningState == 3) {
                server.getPlayerList().sendChatMsg(new TextComponentString(String.format(preShutdownMessage, "3 Minutes")).setStyle(new Style().setColor(TextFormatting.RED)));
                warningState++;
            }
            else if (sts < 2 * 60 && warningState == 4) {
                server.getPlayerList().sendChatMsg(new TextComponentString(String.format(preShutdownMessage, "2 Minutes")).setStyle(new Style().setColor(TextFormatting.RED)));
                warningState++;
            }
            else if (sts < 60 && warningState == 5) {
                server.getPlayerList().sendChatMsg(new TextComponentString(String.format(preShutdownMessage, "1 Minute")).setStyle(new Style().setColor(TextFormatting.RED)));
                warningState++;
            }
            else if (sts < 20 && warningState == 6) {
                server.getPlayerList().sendChatMsg(new TextComponentString(String.format(preShutdownMessage, "20 Seconds")).setStyle(new Style().setColor(TextFormatting.RED)));
                warningState++;
            }
            else if (sts < 4 && warningState == 7) {
                server.getPlayerList().sendChatMsg(new TextComponentString(String.format(preShutdownMessage, "3 Seconds")).setStyle(new Style().setColor(TextFormatting.RED)));
                warningState++;
            }
            else if (sts < 3 && warningState == 8) {
                server.getPlayerList().sendChatMsg(new TextComponentString(String.format(preShutdownMessage, "2 Seconds")).setStyle(new Style().setColor(TextFormatting.RED)));
                warningState++;
            }
            else if (sts < 2 && warningState == 9) {
                server.getPlayerList().sendChatMsg(new TextComponentString(String.format(preShutdownMessage, "1 Second")).setStyle(new Style().setColor(TextFormatting.RED)));
                warningState++;
            }

            if (sts <= 0) {
                LogHelper.info("Executing Scheduled Shutdown....");
                server.initiateShutdown();
            }
        }
        //endregion

        //region TPS Shutdown

        if (enableTPSShutdown && tick % 20 == 0 && server.worldTickTimes.containsKey(0) && server.worldTickTimes.get(0).length == 100 && server.getTickCounter() > 20) {
            int secondIndex = (tick / 20) % tpsCounter.length;
            long tickTime = 0;

            for (int i = 0; i < 20; i++) {
                long ot = server.tickTimeArray[(server.getTickCounter() - i) % 100];
                long wt = server.worldTickTimes.get(0)[(server.getTickCounter() - i) % 100];;
                tickTime += ot > wt ? ot : wt;
            }

            tickTime /= 20L;
            double tt = tickTime / 1000000D;

            if (tt <= 0) {
                tt = 0.1;
            }

            double tps = Math.min(1000D / tt, 20D);
            tpsCounter[secondIndex] = tps;

            double meanTPS = getTPSCount();

            if (meanTPS < tpsThreshold && tpsDropTime == null) {
                tpsDropTime = Instant.now().atZone(ZoneId.systemDefault()).toLocalDateTime();
            }
            else if (meanTPS > tpsThreshold && tpsDropTime != null) {
                tpsDropTime = null;
            }

            if (meanTPS < tpsThreshold && tpsDropTime != null) {
                LocalDateTime now = Instant.now().atZone(ZoneId.systemDefault()).toLocalDateTime();
                long sts = (tpsOverSeconds) - tpsDropTime.until(now, SECONDS);
                if (sts <= 0) {
                    server.getPlayerList().sendChatMsg(new TextComponentString(preTPSShutdownMessage).setStyle(new Style().setColor(TextFormatting.RED)));
                    server.initiateShutdown();
                }
            }
        }

        //endregion

        tick++;
    }

    public double getTPSCount() {
        double tps = 0;
        for (double d : tpsCounter) {
            tps += d;
        }
        return tps / (double) tpsCounter.length;
    }
}
