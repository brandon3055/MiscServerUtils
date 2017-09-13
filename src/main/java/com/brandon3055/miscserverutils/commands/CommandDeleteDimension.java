package com.brandon3055.miscserverutils.commands;

import com.brandon3055.miscserverutils.modules.ModuleDeleteScheduler;
import com.brandon3055.miscserverutils.modules.ModuleDeleteScheduler.DeletionSchedule;
import net.minecraft.command.CommandBase;
import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.Style;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.WorldServer;

import javax.annotation.Nullable;
import java.util.List;

/**
 * Created by brandon3055 on 9/12/2016.
 */
public class CommandDeleteDimension extends CommandBase {

    @Override
    public String getName() {
        return "deldimension";
    }

    @Override
    public String getUsage(ICommandSender sender) {
        return "/deldimension";
    }


    @Override
    public void execute(MinecraftServer server, ICommandSender sender, String[] args) throws CommandException {

        //region DelNow
        if (arg(args, 0, "delnow") && args.length > 1) {
            int dim = parseInt(args[1]);
            WorldServer world = server.getWorld(dim);
            if (world == null) {
                throw new CommandException("Dimension " + dim + " Not Found!");
            }
            else if (dim == 0) {
                throw new CommandException("This command can not delete the Overworld!");
            }

            String name = world.provider.getDimensionType().name();

            if (arg(args, 2, "confirm")) {
                ModuleDeleteScheduler.addImmediateDeletion(dim);
                sender.sendMessage(new TextComponentString("Dimension \"" + name + "\" Will now be deleted!").setStyle(new Style().setColor(TextFormatting.RED)));
                return;
            }
            else {
                sender.sendMessage(new TextComponentString("Are you sure you want to delete dimension \"" + name + "\"? If so add \"confirm\" to the end of the command you just ran.").setStyle(new Style().setColor(TextFormatting.RED)));
                return;
            }
        }
        //endregion

        //region Schedule
        else if (arg(args, 0, "schedule") && args.length >= 6 && args.length <= 7) {
            int dim = parseInt(args[1]);
            int month = arg(args, 2, "*") ? -1 : parseInt(args[2]);
            int day = arg(args, 3, "*") ? -1 : parseInt(args[3]);
            int hour = arg(args, 4, "*") ? -1 : parseInt(args[4]);
            int minute = parseInt(args[5]);
            boolean repeat = args.length == 7 && arg(args, 6, "-r");
            if (args.length == 7 && !repeat) {
                throw new CommandException("Invalid repeat flag! Expected -r or nothing found " + args[6]);
            }

            ModuleDeleteScheduler.addSchedule(dim, minute, hour, day, month, repeat);
            sender.sendMessage(new TextComponentString("Schedule Created!").setStyle(new Style().setColor(TextFormatting.GREEN)));
        }
        //endregion

        //region Reload
        else if (arg(args, 0, "reload")) {
            boolean failSafe = ModuleDeleteScheduler.ohCrapTriggered;
            ModuleDeleteScheduler.reload();
            sender.sendMessage(new TextComponentString("Deletion Schedule reloaded from disk!").setStyle(new Style().setColor(TextFormatting.GREEN)));
            if (failSafe) {
                sender.sendMessage(new TextComponentString("Fail safe mode deactivated! Deletion scheduler is now active!").setStyle(new Style().setColor(TextFormatting.RED)));
            }
        }
        //endregion

        //region List
        else if (arg(args, 0, "list")) {
            sender.sendMessage(new TextComponentString("Active Schedules:").setStyle(new Style().setColor(TextFormatting.GREEN)));
            for (DeletionSchedule schedule : ModuleDeleteScheduler.getDeletionSchedules()) {
                sender.sendMessage(new TextComponentString(schedule.toString()).setStyle(new Style().setColor(TextFormatting.GOLD)));
            }
        }
        //endregion

        //region Unschedule
        else if (arg(args, 0, "unschedule") && args.length == 2) {
            int id = parseInt(args[1]);
            if (ModuleDeleteScheduler.removeSchedule(id)) {
                sender.sendMessage(new TextComponentString("Success").setStyle(new Style().setColor(TextFormatting.GREEN)));
            }
            else {
                sender.sendMessage(new TextComponentString("Error! Did not find a schedule with that id! Use /" + getName() + " list to get a list of all active schedules and their id's").setStyle(new Style().setColor(TextFormatting.RED)));
            }
        }
        //endregion

        //region Stop
        else if (arg(args, 0, "stop")) {
            ModuleDeleteScheduler.stopEverything();
            sender.sendMessage(new TextComponentString("Fail safe mode activated! Deletion scheduler is now inactive! use \"/" + getName() + " reload\" or restart the game to restore normal operation.").setStyle(new Style().setColor(TextFormatting.RED)));
        }
        //endregion

        else {
            help(sender);
        }
    }

    private void help(ICommandSender sender) {
        sender.sendMessage(new TextComponentString("/deldimension delnow <dimension>").setStyle(new Style().setColor(TextFormatting.GOLD)));
        sender.sendMessage(new TextComponentString("/deldimension schedule <dimension> <month or *> <day of month or *> <hour of day or *> <minute of hour> [-r]").setStyle(new Style().setColor(TextFormatting.GOLD)));
        sender.sendMessage(new TextComponentString("Adding -r will make this a repeat event."));
        sender.sendMessage(new TextComponentString("/deldimension reload").setStyle(new Style().setColor(TextFormatting.GOLD)));
        sender.sendMessage(new TextComponentString("/deldimension list").setStyle(new Style().setColor(TextFormatting.GOLD)));
        sender.sendMessage(new TextComponentString("Lists all active deletion schedules."));
        sender.sendMessage(new TextComponentString("/deldimension unschedule <schedule id>").setStyle(new Style().setColor(TextFormatting.GOLD)));
        sender.sendMessage(new TextComponentString("Removes a deletion schedule. Use /deldimension list to get the schedule id."));
        sender.sendMessage(new TextComponentString("/deldimension stop").setStyle(new Style().setColor(TextFormatting.GOLD)));
        sender.sendMessage(new TextComponentString("This is an \"Oh Crap\" command that stops all active schedules. Run the reload command to restore normal operation."));
    }

    @Override
    public List<String> getTabCompletions(MinecraftServer server, ICommandSender sender, String[] args, @Nullable BlockPos pos) {
        return getListOfStringsMatchingLastWord(args, "delnow", "schedule", "reload", "list", "unschedule", "stop");
    }

    public static boolean arg(String[] args, int argToCheck, String compare) {
        return args.length > argToCheck && args[argToCheck].equals(compare);
    }
}

