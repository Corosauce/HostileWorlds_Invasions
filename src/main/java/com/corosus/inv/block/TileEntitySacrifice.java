package com.corosus.inv.block;

import CoroUtil.difficulty.DifficultyInfoPlayer;
import CoroUtil.difficulty.DynamicDifficulty;
import CoroUtil.forge.CULog;
import com.corosus.inv.InvasionManager;
import com.corosus.inv.InventoryBasicCopy;
import com.corosus.inv.config.ConfigInvasion;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.IInventory;
import net.minecraft.inventory.ISidedInventory;
import net.minecraft.inventory.ItemStackHelper;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.DamageSource;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.ITickable;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.world.World;

public class TileEntitySacrifice extends TileEntity implements ITickable, ISidedInventory
{
    private static final int[] SLOTS_ALL = new int[] {0, 1, 2, 3, 4, 5, 6, 7, 8};

    private InventoryBasicCopy inventory;

    /**
     * mainly just for caching, but shouldnt be required to be bound to tile entity
     * since this block is bound to a single player, and since some calculations are player specific:
     * lets only show the info for that player
     * therefore we need to use tile entity position specifically to get the relevant player
     *
     * used on both sides, synced when client opens gui
     */
    private DifficultyInfoPlayer difficultyInfoPlayer;

    private String ownerName = "";
    private EntityPlayer player;

    private String inventoryName;

    public TileEntitySacrifice() {
        inventoryName = "Sacrifice Inventory";
        inventory = new InventoryBasicCopy(inventoryName, true, 9);
    }

    @Override
    public void onLoad() {
        super.onLoad();

        difficultyInfoPlayer = new DifficultyInfoPlayer(getPos());
    }

    @Override
    public void update()
    {
    	if (!world.isRemote) {
            if (world.getTotalWorldTime() % 20 == 0) {

                //backwards compat fix + set to closest on new placement
                if (ownerName.equals("")) {
                    EntityPlayer player = world.getClosestPlayer(this.getPos().getX(), this.getPos().getY(), this.getPos().getZ(), 128, false);
                    if (player != null) {
                        ownerName = player.getName();
                    }
                }

                boolean skipped = tryToSacrifice();
                if (skipped) {
                    InvasionManager.skipNextInvasionForPlayer(player);

                    CULog.dbg("skipped: " + skipped);
                }
            }
    	}
    }

    @Override
    public NBTTagCompound writeToNBT(NBTTagCompound var1)
    {
        ItemStackHelper.saveAllItems(var1, this.inventory.inventoryContents);

        var1.setString("ownerName", ownerName);

        //System.out.println("write");

        return super.writeToNBT(var1);
    }

    @Override
    public void readFromNBT(NBTTagCompound var1)
    {
        super.readFromNBT(var1);

        ownerName = var1.getString(ownerName);

        //System.out.println("read");

        ItemStackHelper.loadAllItems(var1, this.inventory.inventoryContents);

    }

    /**
     * Tries to spend the items to do a proper sacrifice
     *
     * @return
     */
    public boolean tryToSacrifice() {
        EntityPlayer player = getPlayer();
        if (player != null) {
            if (InvasionManager.canPlayerSkipInvasion(player)) {
                if (spendItem()) {
                    return true;
                }
            }
        }
        return false;
    }

    public boolean spendItem() {
        if (spendItemImpl(true)) {
            return spendItemImpl(false);
        }
        return false;
    }

    public boolean spendItemImpl(boolean dryRun) {
        Item item = Item.getByNameOrId(ConfigInvasion.Sacrifice_Item_Name);

        EntityPlayer player = getPlayer();

        if (player != null) {

            int amountNeeded;

            int skipCount = player.getEntityData().getInteger(DynamicDifficulty.dataPlayerInvasionSkipCountForMultiplier);

            if (skipCount == 0) {
                amountNeeded = ConfigInvasion.Sacrifice_CountNeeded;
            } else {
                amountNeeded = (int) ((double) ConfigInvasion.Sacrifice_CountNeeded * ConfigInvasion.Sacrifice_CountNeeded_Multiplier * (double)skipCount);
            }

            if (item != null) {
                for (int i = 0; i < inventory.getInventoryStackLimit() && amountNeeded > 0; i++) {
                    ItemStack stack = inventory.getStackInSlot(i);
                    if (stack.getItem() == item) {
                        if (ConfigInvasion.Sacrifice_Item_Meta == -1 || stack.getMetadata() == ConfigInvasion.Sacrifice_Item_Meta) {
                            if (stack.getCount() > amountNeeded) {
                                if (!dryRun) {
                                    stack.shrink(amountNeeded);
                                }
                                amountNeeded = 0;
                            } else {
                                amountNeeded -= stack.getCount();
                                if (!dryRun) {
                                    inventory.setInventorySlotContents(i, ItemStack.EMPTY);
                                }
                            }
                        }
                    }
                }
            }
            return amountNeeded == 0;
        }
        return false;
    }

    public void onBlockActivated(World worldIn, BlockPos pos, IBlockState state, EntityPlayer playerIn, EnumHand hand, EnumFacing facing, float hitX, float hitY, float hitZ) {
        if (InvasionManager.skipNextInvasionForPlayer(playerIn)) {
            playerIn.sendMessage(new TextComponentString("Sacrifice received!"));
            if (!playerIn.isCreative()) {
                playerIn.attackEntityFrom(DamageSource.MAGIC, 12);
            }
        }

	}

    public InventoryBasicCopy getInventory() {
        return inventory;
    }

    public void setInventory(InventoryBasicCopy inventory) {
        this.inventory = inventory;
    }

    public DifficultyInfoPlayer getDifficultyInfoPlayer() {
        return difficultyInfoPlayer;
    }

    public void setDifficultyInfoPlayer(DifficultyInfoPlayer difficultyInfoPlayer) {
        this.difficultyInfoPlayer = difficultyInfoPlayer;
    }

    public void setOwnerName(String name) {
        this.ownerName = name;
    }

    public EntityPlayer getPlayer() {
        if (player == null) {
            EntityPlayer findPlayer = world.getPlayerEntityByName(ownerName);
            player = findPlayer;
        }
        return player;
    }

    @Override
    public int getSizeInventory() {
        return inventory.getSizeInventory();
    }

    @Override
    public boolean isEmpty() {
        for (ItemStack itemstack : this.inventory.inventoryContents)
        {
            if (!itemstack.isEmpty())
            {
                return false;
            }
        }

        return true;
    }

    @Override
    public ItemStack getStackInSlot(int index) {
        return inventory.getStackInSlot(index);
    }

    @Override
    public ItemStack decrStackSize(int index, int count) {
        ItemStack itemstack = ItemStackHelper.getAndSplit(inventory.inventoryContents, index, count);

        if (!itemstack.isEmpty())
        {
            this.markDirty();
        }

        return itemstack;
    }

    @Override
    public ItemStack removeStackFromSlot(int index) {
        return ItemStackHelper.getAndRemove(inventory.inventoryContents, index);
    }

    @Override
    public void setInventorySlotContents(int index, ItemStack stack) {
        inventory.inventoryContents.set(index, stack);

        if (stack.getCount() > this.getInventoryStackLimit())
        {
            stack.setCount(this.getInventoryStackLimit());
        }

        this.markDirty();
    }

    @Override
    public int getInventoryStackLimit() {
        return 64;
    }

    @Override
    public boolean isUsableByPlayer(EntityPlayer player) {
        return true;
    }

    @Override
    public void openInventory(EntityPlayer player) {

    }

    @Override
    public void closeInventory(EntityPlayer player) {

    }

    @Override
    public boolean isItemValidForSlot(int index, ItemStack stack) {
        return true;
    }

    @Override
    public int getField(int id) {
        return 0;
    }

    @Override
    public void setField(int id, int value) {

    }

    @Override
    public int getFieldCount() {
        return 0;
    }

    @Override
    public void clear() {
        inventory.inventoryContents.clear();
    }

    @Override
    public String getName() {
        return inventoryName;
    }

    @Override
    public boolean hasCustomName() {
        return true;
    }

    net.minecraftforge.items.IItemHandler handlerAll = new net.minecraftforge.items.wrapper.SidedInvWrapper(this, net.minecraft.util.EnumFacing.WEST);

    @SuppressWarnings("unchecked")
    @Override
    public <T> T getCapability(net.minecraftforge.common.capabilities.Capability<T> capability, @javax.annotation.Nullable net.minecraft.util.EnumFacing facing)
    {
        if (capability == net.minecraftforge.items.CapabilityItemHandler.ITEM_HANDLER_CAPABILITY) {
            return (T) handlerAll;
        }
        return super.getCapability(capability, facing);
    }

    @Override
    public int[] getSlotsForFace(EnumFacing side) {
        return SLOTS_ALL;
    }

    @Override
    public boolean canInsertItem(int index, ItemStack itemStackIn, EnumFacing direction) {
        return true;
    }

    @Override
    public boolean canExtractItem(int index, ItemStack stack, EnumFacing direction) {
        return true;
    }
}
