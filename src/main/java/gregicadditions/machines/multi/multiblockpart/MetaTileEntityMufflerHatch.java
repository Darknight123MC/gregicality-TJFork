package gregicadditions.machines.multi.multiblockpart;

import codechicken.lib.render.CCRenderState;
import codechicken.lib.render.pipeline.IVertexOperation;
import codechicken.lib.vec.Matrix4;
import gregicadditions.capabilities.GregicAdditionsCapabilities;
import gregicadditions.capabilities.impl.GARecipeMapMultiblockController;
import gregicadditions.client.ClientHandler;
import gregtech.api.gui.GuiTextures;
import gregtech.api.gui.ModularUI;
import gregtech.api.gui.widgets.SlotWidget;
import gregtech.api.metatileentity.ITieredMetaTileEntity;
import gregtech.api.metatileentity.MetaTileEntity;
import gregtech.api.metatileentity.MetaTileEntityHolder;
import gregtech.api.metatileentity.multiblock.IMultiblockAbilityPart;
import gregtech.api.metatileentity.multiblock.MultiblockAbility;
import gregtech.api.metatileentity.multiblock.MultiblockControllerBase;
import gregtech.common.metatileentities.electric.multiblockpart.MetaTileEntityMultiblockPart;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.resources.I18n;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.PacketBuffer;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumParticleTypes;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import net.minecraftforge.items.ItemStackHandler;

import javax.annotation.Nullable;
import java.util.List;
import java.util.stream.IntStream;

import static gregicadditions.capabilities.impl.GARecipeMapMultiblockController.XSTR_RAND;

public class MetaTileEntityMufflerHatch extends MetaTileEntityMultiblockPart implements IMultiblockAbilityPart<MetaTileEntityMufflerHatch>, ITieredMetaTileEntity {

    private final int recoveryChance;
    private final ItemStackHandler inventory;

    private boolean frontFaceFree;

    public MetaTileEntityMufflerHatch(ResourceLocation metaTileEntityId, int tier) {
        super(metaTileEntityId, tier);
        switch (tier) { //todo replace this with a proper function
            case 2:
                recoveryChance = 20;
                break;
            case 3:
                recoveryChance = 33;
                break;
            case 4:
                recoveryChance = 44;
                break;
            case 5:
                recoveryChance = 53;
                break;
            case 6:
                recoveryChance = 61;
                break;
            case 7:
                recoveryChance = 68;
                break;
            case 8:
                recoveryChance = 73;
                break;
            default:
                recoveryChance = 5;
                break;
        }
        this.inventory = new ItemStackHandler((int) Math.pow(tier + 1, 2));
        this.frontFaceFree = false;
    }

    @Override
    public MetaTileEntity createMetaTileEntity(MetaTileEntityHolder holder) {
        return new MetaTileEntityMufflerHatch(metaTileEntityId, getTier());
    }

    @Override
    public void update() {
        super.update();

        if (!getWorld().isRemote) {
            if (getOffsetTimer() % 10 == 0)
                this.frontFaceFree = checkFrontFaceFree();
        }
        GARecipeMapMultiblockController controller = (GARecipeMapMultiblockController) getController();
        if (getWorld().isRemote && controller != null && controller.isActive())
            pollutionParticles();
    }

    // Leaving this here for now to show old behavior
    @Deprecated
    public void recoverItems(List<ItemStack> recoveryItems) {
        if (calculateChance())
            MetaTileEntity.addItemsToItemHandler(inventory, false, recoveryItems);
    }

    public void recoverItemsTable(List<ItemStack> recoveryItems) {
        int numRolls = Math.min(recoveryItems.size(), inventory.getSlots());
        IntStream.range(0, numRolls).forEach(slot -> {
            if (calculateChance())
                inventory.insertItem(slot, recoveryItems.get(slot), false);
        });
    }

    private boolean calculateChance() {
        return recoveryChance >= 100 || recoveryChance >= XSTR_RAND.nextInt(100);
    }

    @Override
    public void receiveCustomData(int dataId, PacketBuffer buf) {
        super.receiveCustomData(dataId, buf);
        // TODO if needed
    }

    @Override
    public NBTTagCompound writeToNBT(NBTTagCompound data) {
        super.writeToNBT(data);
        data.setTag("RecoveryInventory", inventory.serializeNBT());
        return data;
    }

    @Override
    public void readFromNBT(NBTTagCompound data) {
        super.readFromNBT(data);
        this.inventory.deserializeNBT(data.getCompoundTag("RecoveryInventory"));
    }

    @Override
    public void renderMetaTileEntity(CCRenderState renderState, Matrix4 translation, IVertexOperation[] pipeline) {
        super.renderMetaTileEntity(renderState, translation, pipeline);
        if (shouldRenderOverlay())
            ClientHandler.MUFFLER_OVERLAY.renderSided(getFrontFacing(), renderState, translation, pipeline);
    }

    @Override
    protected ModularUI createUI(EntityPlayer entityPlayer) {
        int rowSize = this.getTier() + 1;
        ModularUI.Builder builder = ModularUI.builder(GuiTextures.BACKGROUND, 176,
                18 + 18 * rowSize + 94)
                .label(10, 5, this.getMetaFullName());

        for(int y = 0; y < rowSize; ++y) {
            for(int x = 0; x < rowSize; ++x) {
                int index = y * rowSize + x;
                builder.widget((new SlotWidget(inventory, index, 89 - rowSize * 9 + x * 18, 18 + y * 18, true, false))
                        .setBackgroundTexture(GuiTextures.SLOT));
            }
        }

        builder.bindPlayerInventory(entityPlayer.inventory, GuiTextures.SLOT, 8, 18 + 18 * rowSize + 12);
        return builder.build(this.getHolder(), entityPlayer);
    }

    @Override
    public void addInformation(ItemStack stack, @Nullable World player, List<String> tooltip, boolean advanced) {
        tooltip.add(I18n.format("gtadditions.muffler.recovery_tooltip", recoveryChance));
        tooltip.add(I18n.format("gregtech.universal.enabled"));
    }

    @Override
    public MultiblockAbility<MetaTileEntityMufflerHatch> getAbility() {
        return GregicAdditionsCapabilities.MUFFLER_HATCH;
    }

    @Override
    public void registerAbilities(List<MetaTileEntityMufflerHatch> abilityList) {
        abilityList.add(this);
    }

    /**
     * @return true if front face is free and contains only air blocks in 1x1 area
     */
    public boolean isFrontFaceFree() {
        return frontFaceFree;
    }

    private boolean checkFrontFaceFree() {
        BlockPos frontPos = getPos().offset(getFrontFacing());
        IBlockState blockState = getWorld().getBlockState(frontPos);
        return blockState.getBlock().isAir(blockState, getWorld(), frontPos);
    }

    @SideOnly(Side.CLIENT)
    public void pollutionParticles() {

        BlockPos pos = this.getPos();
        EnumFacing facing = this.getFrontFacing();
        float xPos = facing.getXOffset() * 0.76F + pos.getX() + 0.25F;
        float yPos = facing.getYOffset() * 0.76F + pos.getY() + 0.25F;
        float zPos = facing.getZOffset() * 0.76F + pos.getZ() + 0.25F;

        float ySpd = facing.getYOffset() * 0.1F + 0.2F + 0.1F * XSTR_RAND.nextFloat();
        float xSpd;
        float zSpd;

        if (facing.getYOffset() == -1) {
            float temp = XSTR_RAND.nextFloat() * 2 * (float) Math.PI;
            xSpd = (float) Math.sin(temp) * 0.1F;
            zSpd = (float) Math.cos(temp) * 0.1F;
        } else {
            xSpd = facing.getXOffset() * (0.1F + 0.2F * XSTR_RAND.nextFloat());
            zSpd = facing.getZOffset() * (0.1F + 0.2F * XSTR_RAND.nextFloat());
        }
        MultiblockControllerBase controllerBase = getController();
        if (controllerBase instanceof GARecipeMapMultiblockController) {
            GARecipeMapMultiblockController controller = (GARecipeMapMultiblockController) controllerBase;
            controller.runMufflerEffect(xPos, yPos, zPos, xSpd, ySpd, zSpd);
        }
    }
}
