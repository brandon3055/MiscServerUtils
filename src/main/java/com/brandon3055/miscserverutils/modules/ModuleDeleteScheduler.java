package com.brandon3055.miscserverutils.modules;

import com.brandon3055.miscserverutils.LogHelper;
import com.brandon3055.miscserverutils.ModEventHandler;
import com.brandon3055.miscserverutils.TeleportUtils;
import com.brandon3055.miscserverutils.commands.CommandDeleteDimension;
import com.brandon3055.miscserverutils.commands.CommandTPOfflinePlayer;
import com.google.common.collect.ImmutableSetMultimap;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import net.minecraft.command.CommandException;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagDouble;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.text.Style;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.WorldServer;
import net.minecraftforge.common.DimensionManager;
import net.minecraftforge.common.ForgeChunkManager;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.config.Configuration;
import net.minecraftforge.event.entity.EntityTravelToDimensionEvent;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.event.FMLServerStartingEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.*;

/**
 * Created by brandon3055 on 10/12/2016.
 */
public class ModuleDeleteScheduler extends SUModuleBase {

    private static final CommandDeleteDimension COMMAND_INSTANCE = new CommandDeleteDimension();
    private static File config;
    private static List<DeletionSchedule> deletionSchedules = new LinkedList<>();
    private static Map<DeletionSchedule, DeleteHandler> deleteHandlerMap = new HashMap<>();
    private static Map<Integer, DeleteHandler> dimHandlerMap = new HashMap<>();
    public static int tick = 0;
    public static boolean ohCrapTriggered = false;

    public ModuleDeleteScheduler() {
        super("dimensionDeletionScheduler", "Allows you to schedule the automatic deletion of dimensions.");
        addListener(ModEventHandler.EventType.SERVER_TICK);
    }

    @Override
    public void initialize() {
        super.initialize();
        MinecraftForge.EVENT_BUS.register(this);
    }

    @Override
    public void registerCommands(FMLServerStartingEvent event) {
        event.registerServerCommand(COMMAND_INSTANCE);
    }

    @Override
    public void onEvent(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }
        tick++;

        if (tick % 20 == 0) {
            for (DeletionSchedule schedule : deletionSchedules) {
                long time = schedule.secondsTillNext();
                if (schedule.secondsTillNext() > 0 && time < 60 * 5 && !deleteHandlerMap.containsKey(schedule)) {
                    deleteHandlerMap.put(schedule, new DeleteHandler(schedule));
                    LogHelper.info("Dimension " + schedule.dimension + " Is scheduled to be deleted in " + time + " Seconds!");
                    messageAdmins("Dimension " + schedule.dimension + " Is scheduled to be deleted in " + time + " Seconds!");
                }
            }

            if (!deleteHandlerMap.isEmpty()) {
                List<DeletionSchedule> finished = new ArrayList<>();
                for (DeleteHandler handler : deleteHandlerMap.values()) {
                    if (handler.update()) {
                        finished.add(handler.scheduledBy);
                    }
                }
                if (!finished.isEmpty()) {
                    for (DeletionSchedule schedule : finished) {
                        deleteHandlerMap.remove(schedule);
                    }
                    finished.clear();
                }
            }
        }
    }

    @SubscribeEvent
    public void entitySwitchWorlds(EntityTravelToDimensionEvent event) {
        if (dimHandlerMap.containsKey(event.getDimension())) {
            long time = dimHandlerMap.get(event.getDimension()).scheduledBy.secondsTillNext();

            if (time < 5) {
                event.setCanceled(true);
                if (event.getEntity() instanceof EntityPlayer) {
                    ((EntityPlayer) event.getEntity()).addChatComponentMessage(new TextComponentString("The dimension you are attempting to travel to is about to be deleted/reset. Please wait a few seconds then try again.").setStyle(new Style().setColor(TextFormatting.RED)));
                }
            }
            else if (event.getEntity() instanceof EntityPlayer) {
                ((EntityPlayer) event.getEntity()).addChatComponentMessage(new TextComponentString("Warning! This dimension will be deleted/reset in " + (Math.round((time / 60D) * 10D) / 10D) + " minutes! (" + time + " seconds)").setStyle(new Style().setColor(TextFormatting.RED)));
            }
        }
    }

    //region Save/Load

    @Override
    public void loadConfig(Configuration config) {
        super.loadConfig(config);
        File folder = config.getConfigFile().getParentFile();
        if (!folder.exists()) {
            folder.mkdirs();
        }
        ModuleDeleteScheduler.config = new File(folder, "SUDeletionSchedule.json");
        load();
    }

    public static void save() {
        if (ohCrapTriggered) {
            LogHelper.error("Fail safe mode has been activated. Can not save while in this state. Run the reload command to reset fail save mode.");
            return;
        }

        try {
            FileWriter writer = new FileWriter(config);
            Gson gson = new GsonBuilder().setPrettyPrinting().create();
            gson.toJson(deletionSchedules, writer);
            writer.close();
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void load() {
        if (ohCrapTriggered) {
            LogHelper.error("Fail safe mode has been activated. Can not load while in this state. Run the reload command to reset fail save mode.");
            return;
        }

        try {
            deletionSchedules = new Gson().fromJson(new FileReader(config), new TypeToken<List<DeletionSchedule>>() {
            }.getType());
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void reload() {
        ohCrapTriggered = false;
        load();
    }

    //endregion

    //region Interact

    public static void addSchedule(int dimension, int minute, int hour, int day, int month, boolean repeat) throws CommandException {
        if (ohCrapTriggered) {
            throw new CommandException("Fail safe mode has been activated. Can not save while in this state. Run the reload command to reset fail save mode.");
        }
        DeletionSchedule schedule = new DeletionSchedule(dimension, minute, hour, day, month, repeat, getNextID());
        deletionSchedules.add(schedule);
        save();
    }

    public static void addImmediateDeletion(int dimension) throws CommandException {
        if (ohCrapTriggered) {
            throw new CommandException("Fail safe mode has been activated. Can not save while in this state. Run the reload command to reset fail save mode.");
        }
        DeletionSchedule schedule = new DeletionSchedule(dimension, -1, -1, -1, -1, false, getNextID()) {
            @Override
            public long secondsTillNext() {
                return 0;
            }
        };
        deleteHandlerMap.put(schedule, new DeleteHandler(schedule));
    }

    public static List<DeletionSchedule> getDeletionSchedules() {
        return deletionSchedules;
    }

    public static boolean removeSchedule(int id) throws CommandException {
        if (ohCrapTriggered) {
            throw new CommandException("Fail safe mode has been activated. Can not save while in this state. Run the reload command to reset fail save mode.");
        }
        Iterator<DeletionSchedule> i = deletionSchedules.iterator();

        boolean removed = false;
        while (i.hasNext()) {
            DeletionSchedule next = i.next();
            if (next.scheduleID == id) {
                i.remove();
                if (deleteHandlerMap.containsKey(next)) {
                    deleteHandlerMap.remove(next);
                }
                removed = true;
            }
        }

        if (removed) {
            save();
        }

        return removed;
    }

    public static void stopEverything() {
        ohCrapTriggered = true;
        deletionSchedules.clear();
        deleteHandlerMap.clear();
    }

    //endregion

    public static void clearDimension(/*MinecraftServer server, */WorldServer world/*, BlockPos spawn*/) {
        MinecraftServer server = world.getMinecraftServer();
        BlockPos spawn = server.worldServerForDimension(0).getSpawnPoint();
        List<EntityPlayer> players = new ArrayList<EntityPlayer>(world.playerEntities);
        int dim = world.provider.getDimension();
        int playersMoved = 0;

        for (EntityPlayer player : players) {
            TeleportUtils.teleportEntity(player, 0, spawn.getX() + 0.5, spawn.getY() + 0.5, spawn.getZ() + 0.5);
            player.addChatComponentMessage(new TextComponentString("You were moved because the dimension you were in is being deleted/reset.").setStyle(new Style().setColor(TextFormatting.RED)));
            playersMoved++;
        }
        server.getPlayerList().saveAllPlayerData();

        LogHelper.info("Moved " + playersMoved + " Active users from dim " + dim + " To spawn.");
        playersMoved = 0;

        File playerFolder = new File(server.getEntityWorld().getSaveHandler().getWorldDirectory(), "playerdata");
        File[] playerArray = playerFolder.listFiles();
        if (playerArray == null) {
            return;
        }

        for (File file : playerArray) {
            if (file.isFile() && file.getName().endsWith(".dat")) {
                try {
                    NBTTagCompound compound = CommandTPOfflinePlayer.getPlayerCompound(file);
                    if (compound.getInteger("Dimension") == dim) {
                        LogHelper.dev("Teleport " + file);
                        compound.setInteger("Dimension", 0);
                        NBTTagList posTag = new NBTTagList();
                        posTag.appendTag(new NBTTagDouble(spawn.getX() + 0.5));
                        posTag.appendTag(new NBTTagDouble(spawn.getY() + 0.5));
                        posTag.appendTag(new NBTTagDouble(spawn.getZ() + 0.5));
                        compound.setTag("Pos", posTag);
                        CommandTPOfflinePlayer.writePlayerCompound(file, compound);
                        playersMoved++;
                    }
                }
                catch (Exception ignored) {
                }
            }
        }
        LogHelper.info("Moved " + playersMoved + " Offline users from dim " + dim + " To spawn.");
    }

    private static int getNextID() {
        int next = 0;

        for (DeletionSchedule schedule : deletionSchedules) {
            if (schedule.scheduleID >= next) {
                next = schedule.scheduleID + 1;
            }
        }

        return next;
    }

    public static void messageAdmins(String message) {
        MinecraftServer server = FMLCommonHandler.instance().getMinecraftServerInstance();
        if (server == null) {
            return;
        }
        TextComponentString text = new TextComponentString(TextFormatting.RED + "[" + TextFormatting.BLUE + "Misc Server Utils" + TextFormatting.RED + "]" + TextFormatting.GREEN + ": ");
        text.appendSibling(new TextComponentString(message).setStyle(new Style().setColor(TextFormatting.GREEN)));

        for (EntityPlayerMP player : server.getPlayerList().getPlayerList()) {
            LogHelper.dev(player);
            if (server.getPlayerList().canSendCommands(player.getGameProfile()) && player.canCommandSenderUseCommand(COMMAND_INSTANCE.getRequiredPermissionLevel(), COMMAND_INSTANCE.getCommandName())) {
                player.addChatMessage(text);
            }
        }
    }

    //region Sub Classes

    public static class DeletionSchedule {
        public final int minute;
        public final int hour;
        public final int day;
        public final int month;
        public final int dimension;
        public final boolean repeat;
        public final int scheduleID;

        public DeletionSchedule(int dimension, int minute, int hour, int day, int month, boolean repeat, int scheduleID) {
            this.dimension = dimension;
            this.minute = minute;
            this.hour = hour;
            this.day = day;
            this.month = month;
            this.repeat = repeat;
            this.scheduleID = scheduleID;
        }

        public long secondsTillNext() {
            LocalDateTime currentDate = Instant.now().atZone(ZoneId.systemDefault()).toLocalDateTime();
            LocalDate targetDate = LocalDate.of(currentDate.getYear(), month == -1 ? currentDate.getMonthValue() : month, day == -1 ? currentDate.getDayOfMonth() : day);
            LocalDateTime targetTime = targetDate.atTime(hour == -1 ? currentDate.getHour() : hour, minute);
            return currentDate.until(targetTime, ChronoUnit.SECONDS);
        }

        @Override
        public String toString() {
            return String.format("{ScheduleID: %s, Dim: %s, %s/%s-%s:%s, Repeat: %s}", scheduleID, dimension, day == -1 ? "*" : day, month == -1 ? "*" : month, hour == -1 ? "*" : hour, minute, repeat);
        }
    }

    public static class DeleteHandler {
        private final DeletionSchedule scheduledBy;
        private int timer = -1;
        private int fails = 0;
        private File dimFolder;

        public DeleteHandler(DeletionSchedule scheduledBy) {
            this.scheduledBy = scheduledBy;
        }

        /**
         * Updates the handler.
         *
         * @return true if the handler is finished.
         */
        public boolean update() {
            MinecraftServer server = FMLCommonHandler.instance().getMinecraftServerInstance();
            if (!dimHandlerMap.containsKey(scheduledBy.dimension)) {
                dimHandlerMap.put(scheduledBy.dimension, this);
            }
            else if (dimHandlerMap.get(scheduledBy.dimension) != this && scheduledBy.secondsTillNext() < dimHandlerMap.get(scheduledBy.dimension).scheduledBy.secondsTillNext()) {
                dimHandlerMap.put(scheduledBy.dimension, this);
            }

            long timeTillDeletion = scheduledBy.secondsTillNext();

            if (timeTillDeletion > 0 && timeTillDeletion % 60 == 0) {
                messageAdmins("Dimension " + scheduledBy.dimension + " Is scheduled to be deleted in " + timeTillDeletion + " Seconds!");
            }

            if (timeTillDeletion <= 0) {
                if (timer == -1) {
                    WorldServer world = server.worldServerForDimension(scheduledBy.dimension);
                    if (world == null) {
                        LogHelper.error("Can not delete dimension " + scheduledBy.dimension + " because the dimension dose not exist!");
                        messageAdmins("Can not delete dimension " + scheduledBy.dimension + " because the dimension dose not exist!");
                        return true;
                    }
                    timer = 5;
                    LogHelper.info("Preparing to delete dimension " + scheduledBy.dimension);
                    messageAdmins("Preparing to delete dimension " + scheduledBy.dimension);
                    clearDimension(world);
                }
                else if (timer == -2) {
                    if (!scheduledBy.repeat) {
                        deletionSchedules.remove(scheduledBy);
                        save();
                    }
                    dimHandlerMap.remove(scheduledBy.dimension);
                    return true;
                }
                else {
                    if (timer == 4) {
                        WorldServer world = server.worldServerForDimension(scheduledBy.dimension);
                        if (world == null) {
                            LogHelper.error("Can not delete dimension " + scheduledBy.dimension + " because the dimension dose not exist!");
                            dimHandlerMap.remove(scheduledBy.dimension);
                            return true;
                        }
                        prepDimension(world);
                    }
                    if (timer == 0) {
                        LogHelper.info("Deleting Dimension " + scheduledBy.dimension);

                        try {
                            FileUtils.deleteDirectory(dimFolder);
                            LogHelper.info("Success! Dimension Deleted!");
                            messageAdmins("Dimension " + scheduledBy.dimension + " Has been Successfully Deleted!");
                            timer = -2;
                            return false;
                        }
                        catch (Exception e) {
                            fails++;
                            if (fails > 5) {
                                LogHelper.error("Failed To Delete dimension after 5 attempts... Canceling.");
                                messageAdmins("Failed To Delete dimension after 5 attempts... Canceling.");
                                timer = -2;
                                return false;
                            }

                            messageAdmins("Failed To Delete dimension " + scheduledBy.scheduleID + " Will try again in 5 seconds...");
                            LogHelper.error("Failed To Delete Dimension!");
                            LogHelper.error(e.getMessage());
                            LogHelper.error("Will try again in 5 seconds...");
                        }
                    }

                    timer--;
                }
            }
            return false;
        }

        public void prepDimension(WorldServer world) {
            int dim = world.provider.getDimension();
            LogHelper.dev("Preparing to delete dimension " + dim);
            dimFolder = new File(world.getSaveHandler().getWorldDirectory(), world.provider.getSaveFolder());

            ImmutableSetMultimap<ChunkPos, ForgeChunkManager.Ticket> chunks = ForgeChunkManager.getPersistentChunksFor(world);
            for (ForgeChunkManager.Ticket ticket : chunks.values()) {
                ForgeChunkManager.releaseTicket(ticket);
            }
            DimensionManager.unloadWorld(dim);
        }
    }

    //endregion
}
