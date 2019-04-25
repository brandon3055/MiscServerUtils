package com.brandon3055.miscserverutils.commands;

import com.brandon3055.miscserverutils.TeleportUtils;
import com.google.common.base.Charsets;
import com.mojang.authlib.GameProfile;
import net.minecraft.command.CommandBase;
import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.command.PlayerNotFoundException;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.nbt.CompressedStreamTools;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagDouble;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.text.Style;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextFormatting;

import java.io.*;
import java.util.UUID;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

/**
 * Created by brandon3055 on 16/12/2015.
 */
public class CommandTPOfflinePlayer extends CommandBase {
    @Override
    public String getName() {
        return "tpofflineplayer";
    }

    @Override
    public String getUsage(ICommandSender p_71518_1_) {
        return "/tpofflineplayer [target player] <destination player> OR /tpofflineplayer [target player] <x> <y> <z> [dimension]";
    }

    @Override
    public int getRequiredPermissionLevel() {
        return 2;
    }

    @Override
    public void execute(MinecraftServer server, ICommandSender sender, String[] args) throws CommandException {
        if (args.length == 1) {
            Player user = new Player(server, getCommandSenderAsPlayer(sender));
            user.teleport(new Player(server, args[0]));
            notifyCommandListener(sender, this, "commands.tp.success", sender.getName(), args[0]);
        }
        else if (args.length == 2) {
            Player player = new Player(server, args[0]);
            Player target = new Player(server, args[1]);
            player.teleport(target);
//            EntityPlayer player = getPlayer(server, sender, args[1]);
//            tpOfflinePlayer(server, args[0], player.posX, player.posY, player.posZ, true, player.dimension);
            notifyCommandListener(sender, this, "commands.tp.success", args[0], args[1]);
        }
        else if (args.length >= 3) {
            try {
                double x = parseDouble(args[0]);
                double y = parseDouble(args[1]);
                double z = parseDouble(args[2]);
                Player player = new Player(server, getCommandSenderAsPlayer(sender));

                if (args.length == 4) {
                    player.teleport(x, y, z, parseInt(args[3]));
                }
                else {
                    player.teleport(x, y, z, player.dimension);
                }
                notifyCommandListener(sender, this, "commands.tp.success.coordinates", args[0], x, y, z);
            }
            catch (CommandException ignored) {
                Player player = new Player(server, args[0]);
                double x = parseDouble(args[1]);
                double y = parseDouble(args[2]);
                double z = parseDouble(args[3]);

                if (args.length == 5) {
                    player.teleport(x, y, z, parseInt(args[4]));
                }
                else {
                    player.teleport(x, y, z, player.dimension);
                }
                notifyCommandListener(sender, this, "commands.tp.success.coordinates", args[0], x, y, z);
            }
//
//
//            double x = parseDouble(args[1]);
//            double y = parseDouble(args[2]);
//            double z = parseDouble(args[3]);
//            if (args.length == 5) {
//                int dim = parseInt(args[4]);
//                WorldServer tWorld = server.worldServerForDimension(dim);
//                if (tWorld == null) {
//                    throw new CommandException("Target world dose not exist!");
//                }
//                tpOfflinePlayer(server, args[0], x, y, z, true, dim);
//            }
//            tpOfflinePlayer(server, args[0], x, y, z, false, 0)
        }
        else {
            sender.sendMessage(new TextComponentString("Usage: /tpofflineplayer [target player] <destination player> OR tpofflineplayer [target player] <x> <y> <z> [dimension]"));
            sender.sendMessage(new TextComponentString("-Works the same as the vanilla tp command except has support for offline players and international teleportation.").setStyle(new Style().setColor(TextFormatting.GRAY)));
        }
    }

    private static void tpOfflinePlayer(MinecraftServer server, String username, double x, double y, double z, boolean setDim, int dim) throws CommandException {
        File playerFile = getPlayerFile(server, username);
        NBTTagCompound playerCompound = getPlayerCompound(playerFile);

        NBTTagList pos = new NBTTagList();
        pos.appendTag(new NBTTagDouble(x));
        pos.appendTag(new NBTTagDouble(y));
        pos.appendTag(new NBTTagDouble(z));
        playerCompound.setTag("Pos", pos);
        if (setDim) {
            playerCompound.setInteger("Dimension", dim);
        }

        writePlayerCompound(playerFile, playerCompound);
    }

    public static void setOfflinePlayerPos() {
    }

    public static File getPlayerFile(MinecraftServer server, String username) throws CommandException {
        UUID offlineUUID = UUID.nameUUIDFromBytes(("OfflinePlayer:" + username).getBytes(Charsets.UTF_8));
        GameProfile onlineProfile = server.getPlayerProfileCache().getGameProfileForUsername(username);
        UUID onlineUUID = onlineProfile == null ? null : onlineProfile.getId();

        File playerFolder = new File(server.getEntityWorld().getSaveHandler().getWorldDirectory(), "playerdata");
        File[] playerArray = playerFolder.listFiles();
        if (playerArray == null) {
            throw new PlayerNotFoundException("There are no players in the playerdata folder");
        }

        File playerFile = null;
        for (File file : playerArray) {
            if (file.getName().replace(".dat", "").equals(offlineUUID.toString()) || (onlineUUID != null && file.getName().replace(".dat", "").equals(onlineUUID.toString()))) {
                playerFile = file;
                break;
            }
        }

        if (playerFile == null) {
            throw new PlayerNotFoundException(username);
        }

        return playerFile;
    }

    private static void tpToOfflinePlayer(MinecraftServer server, EntityPlayerMP player, String targetPlayer) throws CommandException {
        File playerFile = getPlayerFile(server, targetPlayer);
        NBTTagCompound playerCompound = getPlayerCompound(playerFile);
        NBTTagList pos = playerCompound.getTagList("Pos", 6);
        player.connection.setPlayerLocation(pos.getDoubleAt(0), pos.getDoubleAt(1), pos.getDoubleAt(2), player.rotationYaw, player.rotationPitch);
    }

    public static NBTTagCompound getPlayerCompound(File playerData) throws CommandException {
        NBTTagCompound c;

        try {
            DataInputStream is = new DataInputStream(new GZIPInputStream(new FileInputStream(playerData)));
            c = CompressedStreamTools.read(is);
            is.close();
        }
        catch (Exception e) {
            e.printStackTrace();
            throw new CommandException(e.toString());
        }

        return c;
    }

    public static void writePlayerCompound(File playerFile, NBTTagCompound playerCompound) throws CommandException {
        try {
            DataOutputStream os = new DataOutputStream(new GZIPOutputStream(new FileOutputStream(playerFile)));
            CompressedStreamTools.write(playerCompound, os);

            os.close();
        }
        catch (Exception e) {
            e.printStackTrace();
            throw new CommandException(e.toString());
        }
    }

    public static class Player {
        private boolean isOffline = false;
        private EntityPlayerMP onlinePlayer;
        private String username;
        public double posX;
        public double posY;
        public double posZ;
        public int dimension;
        private MinecraftServer server;

        public Player(MinecraftServer server, String username) throws CommandException {
            this.server = server;
            this.onlinePlayer = server.getPlayerList().getPlayerByUsername(username);

            if (onlinePlayer == null) {
                isOffline = true;
            }

            this.username = username;
            init();
        }

        public Player(MinecraftServer server, EntityPlayerMP playerMP) throws CommandException {
            this.server = server;
            onlinePlayer = playerMP;
            this.username = playerMP.getName();
            init();
        }

        private void init() throws CommandException {
            if (isOffline) {
                File playerFile = getPlayerFile(server, username);
                NBTTagCompound playerCompound = getPlayerCompound(playerFile);
                NBTTagList pos = playerCompound.getTagList("Pos", 6);
                posX = pos.getDoubleAt(0);
                posY = pos.getDoubleAt(1);
                posZ = pos.getDoubleAt(2);
                dimension = playerCompound.getInteger("Dimension");
            }
            else {
                posX = onlinePlayer.posX;
                posY = onlinePlayer.posY;
                posZ = onlinePlayer.posZ;
                dimension = onlinePlayer.dimension;
            }
        }

        private void teleport(double x, double y, double z, int dimension) throws CommandException {
            if (isOffline) {
                tpOfflinePlayer(server, username, x, y, z, dimension != this.dimension, dimension);
            }
            else {
                TeleportUtils.teleportEntity(onlinePlayer, dimension, x, y, z);
            }
        }

        private void teleport(Player other) throws CommandException {
            teleport(other.posX, other.posY, other.posZ, other.dimension);
        }
    }
}
