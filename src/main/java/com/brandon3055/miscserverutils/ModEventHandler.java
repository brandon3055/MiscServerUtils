package com.brandon3055.miscserverutils;

import com.brandon3055.miscserverutils.modules.SUModuleBase;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.event.world.BlockEvent;
import net.minecraftforge.event.world.WorldEvent;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by brandon3055 on 11/11/2016.
 */
public class ModEventHandler {

    public static final Map<EventType, List<SUModuleBase>> eventListeners = new HashMap<EventType, List<SUModuleBase>>();

    static {
        for (EventType type : EventType.values()) {
            eventListeners.put(type, new ArrayList<SUModuleBase>());
        }
    }

    @SubscribeEvent
    public void worldUnload(WorldEvent.Unload event) {
//        LogHelper.dev("Unload");
    }

    @SubscribeEvent
    public void onEvent(TickEvent.ServerTickEvent event) {
        for (SUModuleBase module : eventListeners.get(EventType.SERVER_TICK)) {
            module.onEvent(event);
        }

//        Instant target = Instant.now();
//        try {
//
//        }
//        catch (Throwable e) {
//            e.printStackTrace();
//        }
        //deldimension schedule 07:**:**-01:00

//        target.get

//        Date target = new Date();
//        target.

//        LogHelper.dev(new Date());



//        if (timer > -1) {
//            timer--;
//
//            if (timer == 0) {
//
//
//
//
//
////            int dim = world.provider.getDimension();
//
////            ImmutableSetMultimap<ChunkPos, ForgeChunkManager.Ticket> chunks = ForgeChunkManager.getPersistentChunksFor(world);
////            for (ForgeChunkManager.Ticket ticket : chunks.values()) {
////                ForgeChunkManager.releaseTicket(ticket);
////            }
//
//
//
////            world.getChunkProvider().unloadAllChunks();
////            DimensionManager.setWorld(dim, null, world.getMinecraftServer());
//
//
//            }
//        }
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void onEventLast(TickEvent.ServerTickEvent event) {
        for (SUModuleBase module : eventListeners.get(EventType.SERVER_TICK_LAST)) {
            module.onEvent(event);
        }
    }

    @SubscribeEvent
    public void onEvent(TickEvent.PlayerTickEvent event) {
        if (event.player.world.isRemote) {
            return;
        }
        for (SUModuleBase module : eventListeners.get(EventType.PLAYER_TICK)) {
            module.onEvent(event);
        }
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void onEventLast(TickEvent.PlayerTickEvent event) {
        if (event.player.world.isRemote) {
            return;
        }
        for (SUModuleBase module : eventListeners.get(EventType.PLAYER_TICK_LAST)) {
            module.onEvent(event);
        }
    }

    @SubscribeEvent
    public void onEvent(PlayerInteractEvent event) {
        if (event.getWorld().isRemote) {
            return;
        }
        for (SUModuleBase module : eventListeners.get(EventType.PLAYER_INTERACT)) {
            module.onEvent(event);
        }
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void onEventLast(PlayerInteractEvent event) {
        if (event.getWorld().isRemote) {
            return;
        }
        for (SUModuleBase module : eventListeners.get(EventType.PLAYER_INTERACT_LAST)) {
            module.onEvent(event);
        }
    }

    @SubscribeEvent
    public void onEvent(BlockEvent event) {
        if (event.getWorld().isRemote) {
            return;
        }
        for (SUModuleBase module : eventListeners.get(EventType.BLOCK_EVENT)) {
            module.onEvent(event);
        }
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void onEventLast(BlockEvent event) {
        if (event.getWorld().isRemote) {
            return;
        }
        for (SUModuleBase module : eventListeners.get(EventType.BLOCK_EVENT_LAST)) {
            module.onEvent(event);
        }
    }

    public static enum EventType {
        SERVER_TICK, SERVER_TICK_LAST,

        PLAYER_TICK, PLAYER_TICK_LAST,

        PLAYER_INTERACT, PLAYER_INTERACT_LAST,

        BLOCK_EVENT, BLOCK_EVENT_LAST,
    }
}
