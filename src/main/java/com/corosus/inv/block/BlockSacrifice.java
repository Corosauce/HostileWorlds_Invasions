package com.corosus.inv.block;

import com.corosus.inv.Invasion;
import net.minecraft.block.BlockContainer;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumBlockRenderType;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.World;

import javax.annotation.Nullable;
import java.util.List;

public class BlockSacrifice extends BlockContainer
{
    public BlockSacrifice()
    {
        super(Material.CLAY);
        setHardness(0.6F);
        setResistance(10.0F);
    }

    @Override
    public TileEntity createNewTileEntity(World var1, int meta)
    {
        return new TileEntitySacrifice();
    }
    
    @Override
    public boolean isOpaqueCube(IBlockState state)
    {
        return false;
    }
    
    /**
     * The type of render function called. 3 for standard block models, 2 for TESR's, 1 for liquids, -1 is no render
     */
    @Override
    public EnumBlockRenderType getRenderType(IBlockState state)
    {
        return EnumBlockRenderType.MODEL;
    }

    @Override
    public boolean onBlockActivated(World worldIn, BlockPos pos, IBlockState state, EntityPlayer playerIn, EnumHand hand, EnumFacing facing, float hitX, float hitY, float hitZ) {

        boolean old = false;

        if (old) {
            if (!worldIn.isRemote && hand == EnumHand.MAIN_HAND) {
                TileEntity tEnt = worldIn.getTileEntity(pos);

                if (tEnt instanceof TileEntitySacrifice) {
                    ((TileEntitySacrifice) tEnt).onBlockActivated(worldIn, pos, state, playerIn, hand, facing, hitX, hitY, hitZ);
                }
            }

            return super.onBlockActivated(worldIn, pos, state, playerIn, hand, facing, hitX, hitY, hitZ);
        } else {

            if (worldIn.isRemote) {
                return true;
            }

            if (hand == EnumHand.MAIN_HAND && !playerIn.isSneaking()) {
                playerIn.openGui(Invasion.instance, 0, worldIn, pos.getX(), pos.getY(), pos.getZ());
                return true;
            } else {
                return super.onBlockActivated(worldIn, pos, state, playerIn, hand, facing, hitX, hitY, hitZ);
            }
        }

    }

    @Override
    public void addInformation(ItemStack stack, @Nullable World player, List<String> tooltip, ITooltipFlag advanced) {
        super.addInformation(stack, player, tooltip, advanced);
        tooltip.add(TextFormatting.YELLOW + "Right click on day of invasion to trade blood to skip invasion.");
        tooltip.add(TextFormatting.YELLOW + "Will make next one harder, can skip up to 3 invasions.");
        tooltip.add(TextFormatting.YELLOW + "Then you must fight the invasion, and can use the block again.");
    }

    @Override
    public void onBlockPlacedBy(World worldIn, BlockPos pos, IBlockState state, EntityLivingBase placer, ItemStack stack) {
        super.onBlockPlacedBy(worldIn, pos, state, placer, stack);

        if (placer instanceof EntityPlayer) {
            TileEntity tEnt = worldIn.getTileEntity(pos);

            if (tEnt instanceof TileEntitySacrifice) {
                ((TileEntitySacrifice) tEnt).setOwnerName(placer.getName());
            }
        }
    }
}
