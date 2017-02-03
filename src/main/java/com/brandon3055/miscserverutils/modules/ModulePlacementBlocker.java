package com.brandon3055.miscserverutils.modules;

import com.brandon3055.miscserverutils.LogHelper;
import com.brandon3055.miscserverutils.ModEventHandler;
import com.brandon3055.miscserverutils.StackReference;
import net.minecraft.block.state.IBlockState;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.world.World;
import net.minecraftforge.common.config.Configuration;
import net.minecraftforge.event.world.BlockEvent;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by brandon3055 on 3/02/2017.
 */
public class ModulePlacementBlocker extends SUModuleBase {

    private List<RestrictionSet> xyPlacementRestrictions = new ArrayList<>();
    private String placementBlockedMessage = "You can not place that block there!";

    public ModulePlacementBlocker() {
        super("placementBlocker", "Allows you to add certain restrictions for block placement such as preventing users from placing block x next to block y");
        addListener(ModEventHandler.EventType.BLOCK_EVENT_LAST);
    }

    @Override
    public void loadConfig(Configuration config) {
        try {
            config.setCategoryComment("ModulePlacementBlocker", "This is where you specify block placement restrictions. The format for specifying blocks is as follows.\n\"minecraft:stone\" Or\n\"minecraft:stone,64\" Or\n\"minecraft:stone,64,3\" Or\n\"minecraft:stone,64,3,{NBT}\"\nNote: this checks the block the player places while it is still an item in the players hand before it is actually placed in the world.");

            String[] restrictionList = config.getStringList("BlockByBlock", "ModulePlacementBlocker", new String[0], "Prevents block X from being placed next to block Y.\nFormat:\nminecraft:stone-minecraft:glass");
            placementBlockedMessage = config.getString("PlacementBlockedMessage", "ModulePlacementBlocker", placementBlockedMessage, "This is the message a player will see when they are prevented from placing a block.");

            for (String entry : restrictionList) {
                try {
                    if (!entry.contains("-")) {
                        LogHelper.error("Detected invalid BlockByBlock config string: " + entry + "\nExpected something like: stackString-stackString");
                        continue;
                    }
                    StackReference stack1 = StackReference.fromString(entry.substring(0, entry.indexOf("-")));
                    StackReference stack2 = StackReference.fromString(entry.substring(entry.indexOf("-") + 1));

                    if (stack1 == null || stack1.getCachedStack() == null) {
                        LogHelper.error("BlockByBlock: Invalid first block: " + entry.substring(0, entry.indexOf("-")));
                        continue;
                    }
                    else if (stack2 == null || stack2.getCachedStack() == null) {
                        LogHelper.error("BlockByBlock: Invalid second block: " + entry.substring(entry.indexOf("-") + 1));
                        continue;
                    }

                    xyPlacementRestrictions.add(new RestrictionSet(stack1, stack2));
                }
                catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
        catch (Exception e) {
            e.printStackTrace();
        }

        LogHelper.info("Loaded the following BlockByBlock Restrictions");
        for (RestrictionSet set : xyPlacementRestrictions) {
            LogHelper.info(set.toString());
        }
    }

    @Override
    public void onEvent(BlockEvent event) {
        if (event instanceof BlockEvent.PlaceEvent) {
            ItemStack stack = ((BlockEvent.PlaceEvent) event).getItemInHand();
            BlockPos pos = event.getPos();
            for (RestrictionSet set : xyPlacementRestrictions) {
                if (!set.doCheck(event.getWorld(), pos, stack)) {
                    event.setCanceled(true);
                    ((BlockEvent.PlaceEvent) event).getPlayer().addChatComponentMessage(new TextComponentString(placementBlockedMessage));
                    return;
                }
            }
        }
    }

    private class RestrictionSet {
        private final StackReference stack1;
        private final StackReference stack2;

        private RestrictionSet(StackReference stack1, StackReference stack2) {
            this.stack1 = stack1;
            this.stack2 = stack2;
        }

        public boolean doCheck(World world, BlockPos pos, ItemStack stack) {
            for (EnumFacing facing : EnumFacing.VALUES) {
                if (!check(world.getBlockState(pos.offset(facing)), stack)) {
                    return false;
                }
            }

            return true;
        }

        boolean check(IBlockState state, ItemStack stack) {
            ItemStack item1 = stack1.getCachedStack();
            ItemStack item2 = stack2.getCachedStack();

            if (item1.getItem() instanceof ItemBlock) {
                //If item 1 == the block
                if (((ItemBlock) item1.getItem()).block == state.getBlock() && (stack1.metadata == -1 || stack1.metadata == state.getBlock().getMetaFromState(state))) {
                    //And item 2 == the Stack
                    if (item2.getItem() == stack.getItem() && (stack2.metadata == -1 || stack2.metadata == stack.getItemDamage())) {
                        return false;
                    }
                }
            }
            
            if (item2.getItem() instanceof ItemBlock) {
                //If item 2 == the block
                if (((ItemBlock) item2.getItem()).block == state.getBlock() && (stack2.metadata == -1 || stack2.metadata == state.getBlock().getMetaFromState(state))) {
                    //And item 1 == the Stack
                    if (item1.getItem() == stack.getItem() && (stack1.metadata == -1 || stack1.metadata == stack.getItemDamage())) {
                        return false;
                    }
                }
            }

            return true;
        }

        @Override
        public String toString() {
            return stack1.toString() + "-" + stack2.toString();
        }
    }
}
