package com.brandon3055.miscserverutils.commands;

import net.minecraft.command.CommandBase;
import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.text.Style;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.WorldServer;
import net.minecraftforge.common.DimensionManager;

import static net.minecraft.util.text.TextFormatting.*;

/**
 * Created by brandon3055 on 5/01/2017.
 */
public class CommandListDims extends CommandBase {


    @Override
    public int getRequiredPermissionLevel() {
        return 2;
    }

    @Override
    public String getName() {
        return "list_dimensions";
    }

    @Override
    public String getUsage(ICommandSender sender) {
        return "/list_dimensions";
    }

    @Override
    public void execute(MinecraftServer server, ICommandSender sender, String[] args) throws CommandException {
        sender.sendMessage(new TextComponentString("================ Dimension List ================").setStyle(new Style().setColor(TextFormatting.AQUA)));
        sender.sendMessage(new TextComponentString("dim-ID | name").setStyle(new Style().setColor(DARK_PURPLE)));

        for (WorldServer worldServer : DimensionManager.getWorlds()) {
            String id = worldServer.provider.getDimension() + "";
            while (id.length() < 5) {
                id += " ";
            }

            sender.sendMessage(new TextComponentString(id + WHITE + " | " + GOLD + worldServer.provider.getDimensionType()).setStyle(new Style().setColor(GOLD)));
        }
    }
}
