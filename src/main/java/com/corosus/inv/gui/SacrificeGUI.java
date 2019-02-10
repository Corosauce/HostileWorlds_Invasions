package com.corosus.inv.gui;

import CoroUtil.util.CoroUtilMath;
import com.corosus.inv.InvasionNetworkHandler;
import com.corosus.inv.block.TileEntitySacrifice;
import com.corosus.inv.network.MessageRequestDifficultyData;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.util.ResourceLocation;

public class SacrificeGUI extends GuiContainer {

    private static final ResourceLocation DISPENSER_GUI_TEXTURES = new ResourceLocation("textures/gui/container/dispenser.png");

    private TileEntitySacrifice tile;
    private EntityPlayer player;

    private FontRenderer fontRendererUnicode;

    public SacrificeGUI(InventoryPlayer inventoryPlayer, TileEntitySacrifice tile) {
        super(new SacrificeContainer(inventoryPlayer, tile));
        this.tile = tile;
        player = inventoryPlayer.player;
        fontRendererUnicode = new FontRenderer(Minecraft.getMinecraft().gameSettings, new ResourceLocation("textures/font/ascii.png"), Minecraft.getMinecraft().renderEngine, true);
    }

    @Override
    public void initGui() {
        super.initGui();

        MessageRequestDifficultyData message = new MessageRequestDifficultyData(player.world, tile.getPos());
        InvasionNetworkHandler.INSTANCE.sendToServer(message);
    }

    @Override
    protected void actionPerformed(GuiButton guibutton) {
        if (guibutton.id == 1) {

        }
    }

    /**
     * Draws the screen and all the components in it.
     */
    public void drawScreen(int mouseX, int mouseY, float partialTicks)
    {
        this.drawDefaultBackground();
        super.drawScreen(mouseX, mouseY, partialTicks);



        this.renderHoveredToolTip(mouseX, mouseY);
    }

    /**
     * Draw the foreground layer for the GuiContainer (everything in front of the items)
     */
    protected void drawGuiContainerForegroundLayer(int mouseX, int mouseY)
    {
        String s = tile.getInventory().getDisplayName().getUnformattedText();
        this.fontRenderer.drawString(s, this.xSize / 2 - this.fontRenderer.getStringWidth(s) / 2, 6, 4210752);
        this.fontRenderer.drawString(player.inventory.getDisplayName().getUnformattedText(), 8, this.ySize - 96 + 2, 4210752);

        float scale = 1F;

        GlStateManager.pushMatrix();
        GlStateManager.scale(scale, scale, 1F);
        //this.fontRenderer.setUnicodeFlag(false);
        //this.fontRenderer.drawString("DPS Rating: " + CoroUtilMath.roundVal(tile.getDifficultyInfoPlayer().dps), (int)(8 / scale), (int)((6+10) / scale), 4210752);
        this.fontRendererUnicode.drawString("DPS Rating: " + CoroUtilMath.roundVal(tile.getDifficultyInfoPlayer().difficultyDPS), (int)(8 / scale), (int)((6+10) / scale), 4210752);

        this.fontRendererUnicode.drawString("Skip: " + tile.getDifficultyInfoPlayer().skipCount, (int)(8 / scale), (int)((6+20) / scale), 4210752);
        this.fontRendererUnicode.drawString("Items Needed: " + tile.getDifficultyInfoPlayer().itemsNeeded, (int)(8 / scale), (int)((6+30) / scale), 4210752);
        GlStateManager.popMatrix();
    }

    /**
     * Draws the background layer of this container (behind the items).
     */
    protected void drawGuiContainerBackgroundLayer(float partialTicks, int mouseX, int mouseY)
    {
        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
        this.mc.getTextureManager().bindTexture(DISPENSER_GUI_TEXTURES);
        int i = (this.width - this.xSize) / 2;
        int j = (this.height - this.ySize) / 2;
        this.drawTexturedModalRect(i, j, 0, 0, this.xSize, this.ySize);
    }

    @Override
    public void onGuiClosed() {
        super.onGuiClosed();
    }
}
