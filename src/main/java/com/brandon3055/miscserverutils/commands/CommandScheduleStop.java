package com.brandon3055.miscserverutils.commands;

import com.brandon3055.miscserverutils.modules.ModuleAutoShutdown;
import net.minecraft.command.CommandBase;
import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.Style;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextFormatting;

import javax.annotation.Nullable;
import java.time.Instant;
import java.time.ZoneId;
import java.util.List;

/**
 * Created by brandon3055 on 23/01/2017.
 */
public class CommandScheduleStop extends CommandBase {

    @Override
    public String getName() {
        return "ms_stop";
    }

    @Override
    public String getUsage(ICommandSender sender) {
        return "/ms_stop [Minutes - Default: 5]\n/ms_stop cancel (Cancel a scheduled stop)\n/ms_stop reset (Reset the scheduled stop)";
    }

    @Override
    public void execute(MinecraftServer server, ICommandSender sender, String[] args) throws CommandException {
        if (args.length == 0) {
            ModuleAutoShutdown.enableTimedShutdown = true;
            ModuleAutoShutdown.shutdownDelayMins = 5;
            ModuleAutoShutdown.startTime = Instant.now().atZone(ZoneId.systemDefault()).toLocalDateTime();
            sender.sendMessage(new TextComponentString(String.format(ModuleAutoShutdown.preShutdownMessage, "5 Minutes")).setStyle(new Style().setColor(TextFormatting.RED)));
        }
        else if (args.length == 1 && args[0].equals("cancel")) {
            ModuleAutoShutdown.enableTimedShutdown = false;
            sender.sendMessage(new TextComponentString("Auto shutdown canceled."));
        }
        else if (args.length == 1 && args[0].equals("reset")) {
            ModuleAutoShutdown.enableTimedShutdown = ModuleAutoShutdown.timedShutdownConfig;
            ModuleAutoShutdown.shutdownDelayMins = ModuleAutoShutdown.shutdownDelayConfig;
            ModuleAutoShutdown.startTime = Instant.now().atZone(ZoneId.systemDefault()).toLocalDateTime();
            ModuleAutoShutdown.warningState = 0;
            sender.sendMessage(new TextComponentString("Auto shutdown reset."));
        }
        else if (args.length == 1) {
            ModuleAutoShutdown.enableTimedShutdown = true;
            ModuleAutoShutdown.shutdownDelayMins = parseInt(args[0]);
            ModuleAutoShutdown.startTime = Instant.now().atZone(ZoneId.systemDefault()).toLocalDateTime();
            sender.sendMessage(new TextComponentString(String.format(ModuleAutoShutdown.preShutdownMessage, ModuleAutoShutdown.shutdownDelayMins + " Minute" + (ModuleAutoShutdown.shutdownDelayMins > 1 ? "s" : ""))).setStyle(new Style().setColor(TextFormatting.RED)));
        }
    }

    @Override
    public List<String> getTabCompletions(MinecraftServer server, ICommandSender sender, String[] args, @Nullable BlockPos pos) {
        return getListOfStringsMatchingLastWord(args, "cancel", "reset");
    }
}
