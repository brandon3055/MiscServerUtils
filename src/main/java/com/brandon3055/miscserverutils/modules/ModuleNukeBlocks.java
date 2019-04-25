package com.brandon3055.miscserverutils.modules;

import com.brandon3055.miscserverutils.LogHelper;
import com.brandon3055.miscserverutils.ModEventHandler;
import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Blocks;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.chunk.Chunk;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.config.Configuration;
import net.minecraftforge.event.world.ChunkDataEvent;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

import java.util.HashSet;
import java.util.Set;

/**
 * Created by brandon3055 on 3/02/2017.
 */
@SuppressWarnings("Duplicates")
public class ModuleNukeBlocks extends SUModuleBase {

    private static String DATA_TAG = "MSUWorldGen";
    private IBlockState replacement = Blocks.STONE.getDefaultState();
    private String marker = "msu-nuked";
    private boolean log = true;
    private Set<IBlockState> targets = new HashSet<>();

    public ModuleNukeBlocks() {
        super("nukeBlocks", "Allows you to remove specific blocks from the world on load (chunk load)");
        addListener(ModEventHandler.EventType.BLOCK_EVENT_LAST);
    }

    @Override
    public void initialize() {
        super.initialize();
        MinecraftForge.EVENT_BUS.register(this);
    }

    String[] targetArray;
    String replacementName;

    @Override
    public void loadConfig(Configuration config) {
        try {
            config.setCategoryComment("ModuleNukeBlocks", "This is where you specify blocks that should be removed from the world the next time the world loads.");
            targetArray = config.getStringList("BlockNames", "ModuleNukeBlocks", new String[0], "A list of blocks that should be removed. Use block registry name pipe meta. e.g. minecraft:stone|0, minecraft:stone|5");
            replacementName = config.getString("Replacement", "ModuleNukeBlocks", "minecraft:stone", "the block that should be used as a replacement for the removed blocks.");
            marker = config.getString("ChunkMarker", "ModuleNukeBlocks", marker, "This marker is used to flag chunks that have already been nuked. If you need to remove new blocks from previously processed chunks then change this to some new unique string.");
            log = config.getBoolean("LogReplaceEvents", "ModuleNukeBlocks", log, "");
        }
        catch (Exception e) {
            e.printStackTrace();
        }

        LogHelper.info("Loaded the following Blocks to be nuked");
        for (IBlockState block : targets) {
            LogHelper.info(block);
        }
    }

    @Override
    public void fmlLoadEvent() {
        for (String target : targetArray) {
            String name = target.substring(0, target.indexOf("|"));
            int meta = target.contains("|") ? Integer.parseInt(target.substring(target.indexOf("|") + 1)) : 0;
            Block block = Block.REGISTRY.getObject(new ResourceLocation(name));
            if (block != Blocks.AIR) {
                targets.add(block.getStateFromMeta(meta));
            }
            else {
                LogHelper.warn("Could not locate block with name: " + name + " meta: " + meta);
            }
        }

        replacement = Block.REGISTRY.getObject(new ResourceLocation(replacementName)).getDefaultState();
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public void chunkLoad(ChunkDataEvent.Load event) {
        NBTTagCompound tag = event.getData().getCompoundTag(DATA_TAG);

        if (tag.getBoolean(marker)) {
            return;
        }

        tag.setBoolean(marker, true);
        event.getData().setTag(DATA_TAG, tag);

        Chunk chunk = event.getChunk();

        BlockPos origin = chunk.getPos().getBlock(0, 0, 0);
        Iterable<BlockPos> blocks = BlockPos.getAllInBox(origin, origin.add(15, 255, 15));

        for (BlockPos pos : blocks) {
            IBlockState state = chunk.getBlockState(pos);
            if (targets.contains(state)) {
                chunk.setBlockState(pos, replacement);
                if (log) {
                    LogHelper.info("Chunk: " + chunk.getPos() + " Replaced: " + state + " At: " + pos);
                }
            }
        }
    }
}
