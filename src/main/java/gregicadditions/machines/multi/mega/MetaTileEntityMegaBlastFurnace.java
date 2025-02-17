package gregicadditions.machines.multi.mega;

import gregicadditions.GAConfig;
import gregicadditions.GAUtility;
import gregicadditions.GAValues;
import gregicadditions.capabilities.GregicAdditionsCapabilities;
import gregicadditions.item.GAHeatingCoil;
import gregicadditions.item.GAMetaBlocks;
import gregicadditions.item.GATransparentCasing;
import gregicadditions.machines.multi.override.MetaTileEntityElectricBlastFurnace;
import gregicadditions.utils.GALog;
import gregtech.api.capability.IMultipleTankHandler;
import gregtech.api.metatileentity.MetaTileEntity;
import gregtech.api.metatileentity.MetaTileEntityHolder;
import gregtech.api.metatileentity.multiblock.IMultiblockPart;
import gregtech.api.metatileentity.multiblock.MultiblockAbility;
import gregtech.api.metatileentity.multiblock.RecipeMapMultiblockController;
import gregtech.api.multiblock.BlockPattern;
import gregtech.api.multiblock.BlockWorldState;
import gregtech.api.multiblock.FactoryBlockPattern;
import gregtech.api.multiblock.PatternMatchContext;
import gregtech.api.recipes.CountableIngredient;
import gregtech.api.recipes.Recipe;
import gregtech.api.recipes.RecipeBuilder;
import gregtech.api.recipes.recipeproperties.BlastTemperatureProperty;
import gregtech.api.render.ICubeRenderer;
import gregtech.api.render.Textures;
import gregtech.api.util.GTUtility;
import gregtech.api.util.InventoryUtils;
import gregtech.common.blocks.*;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.resources.I18n;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumParticleTypes;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.world.World;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import net.minecraftforge.items.IItemHandlerModifiable;

import javax.annotation.Nullable;
import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import static gregtech.api.multiblock.BlockPattern.RelativeDirection.*;
import static gregtech.api.recipes.RecipeMaps.BLAST_RECIPES;
import static gregtech.api.unification.material.Materials.BlackSteel;
import static gregtech.api.unification.material.Materials.BlueSteel;

public class MetaTileEntityMegaBlastFurnace extends MegaMultiblockRecipeMapController {

    protected int blastFurnaceTemperature;
    private int bonusTemperature;

    private static final MultiblockAbility<?>[] ALLOWED_ABILITIES = {
            MultiblockAbility.IMPORT_ITEMS, MultiblockAbility.EXPORT_ITEMS,
            MultiblockAbility.IMPORT_FLUIDS, MultiblockAbility.INPUT_ENERGY,
            GregicAdditionsCapabilities.MAINTENANCE_HATCH, MultiblockAbility.EXPORT_FLUIDS
    };

    public MetaTileEntityMegaBlastFurnace(ResourceLocation metaTileEntityId) {
        super(metaTileEntityId, BLAST_RECIPES, 100, 100, 100, 0, true, true, true);
        this.recipeMapWorkable = new MegaBlastFurnaceRecipeLogic(this, 100, 100, 100);
    }

    @Override
    public MetaTileEntity createMetaTileEntity(MetaTileEntityHolder holder) {
        return new MetaTileEntityMegaBlastFurnace(metaTileEntityId);
    }

    @Override
    protected BlockPattern createStructurePattern() {
        return FactoryBlockPattern.start(FRONT, RIGHT, DOWN)
                /*
                .aisle("XXXXXXXSXXXXXXX", "XXXXXXXXXXXXXXX", "XXXXXXXXXXXXXXX", "XXXXXXXXXXXXXXX", "XXXXXXXXXXXXXXX", "XXXXXXXXXXXXXXX", "XXXXXXXXXXXXXXX", "XXXXXXXXXXXXXXX", "XXXXXXXXXXXXXXX", "XXXXXXXXXXXXXXX", "XXXXXXXXXXXXXXX", "XXXXXXXXXXXXXXX", "XXXXXXXXXXXXXXX", "XXXXXXXXXXXXXXX", "XXXXXXXXXXXXXXX")
                .aisle("GGGGGGGGGGGGGGG", "GCCCCCCCCCCCCCG", "GCP###ppp###PCG", "GC###########CG", "GC###########CG", "GC###########CG", "GCp#########pCG", "GCp#########pCG", "GCp#########pCG", "GC###########CG", "GC###########CG", "GC###########CG", "GCP###ppp###PCG", "GCCCCCCCCCCCCCG", "GGGGGGGGGGGGGGG").setRepeatable(18)
                .aisle("XXXXXXXXXXXXXXX", "XXXXXXXXXXXXXXX", "XXgggggggggggXX", "XXgggggggggggXX", "XXgggggggggggXX", "XXgggggggggggXX", "XXggggXXXggggXX", "XXggggXmXggggXX", "XXggggXXXggggXX", "XXgggggggggggXX", "XXgggggggggggXX", "XXgggggggggggXX", "XXgggggggggggXX", "XXXXXXXXXXXXXXX", "XXXXXXXXXXXXXXX")
                 */
                .aisle("###############", "###############", "###############", "######TTT######", "####TTTTTTT####", "####TTTTTTT####", "###TTTTTTTTT###", "###TTTTmTTTT###", "###TTTTpTTTT###", "####TTTpTTT####", "####TTTpTTT####", "######TTT######", "###############", "###############", "###############")
                .aisle("###############", "###############", "###############", "###############", "#######f#######", "#####CCCCC#####", "#####C###C#####", "####fC###Cf####", "#####C###C#####", "#####CCCCC#####", "#######p#######", "###############", "###############", "###############", "###############").setRepeatable(6)
                .aisle("###############", "###############", "###############", "#######T#######", "#######f#######", "#####CCCCC#####", "#####C###C#####", "###TfC###CfT###", "#####C###C#####", "#####CCCCC#####", "#######p#######", "#######p#######", "#######p#######", "#######p#######", "###############")
                .aisle("###############", "###############", "###############", "######TTT######", "####TTTTTTT####", "####TTTTTTT####", "###TTTTTTTTT###", "###TTTTTTTTT###", "###TTTTTTTTT###", "####TTTTTTT####", "####TTTTTTT####", "######TTT######", "###############", "#######p#######", "###############")
                .aisle("###############", "#FFFFFFFFFFFFF#", "#FFFFFFFFFFFFF#", "#FF#########FF#", "#FF####T####FF#", "#FF##T###T##FF#", "#FF#########FF#", "#FF#T##P##T#FF#", "#FF#########FF#", "#FF##T###T##FF#", "#FF####T####FF#", "#FF#########FF#", "#FFFFFXXXFFFFF#", "#FFFFFXpXFFFFF#", "###############")
                .aisle("#######p#######", "#F##ppppppp####", "##ppp#####ppp##", "##p#########p##", "#pp####T####pp#", "#pp##T###T###p#", "##pp#########p#", "##ppT##P##T##pp", "#############p#", "#pp##T###T###p#", "#pp####T####pp#", "##p#########p##", "##ppp#####ppp##", "####ppppppp####", "#######p#######")
                .aisle("#####XXpXX#####", "#F#XpXXpXXpX###", "##pXpXXXXXpXp##", "#XXXXXXXXXXXXX#", "#ppXXXXXXXXXpp#", "XXXXXXXXXXXXXXX", "XXXXXXXXXXXXXXX", "GGGpXXXPXXXXXpp", "XXXXXXXXXXXXXXX", "XXXXXXXXXXXXXXX", "#ppXXXXXXXXXpp#", "#XXXXXXXXXXXXX#", "##pXpXXXXXpXp##", "###XpXXpXXpX###", "#####XXpXX#####")
                .aisle("#####XXXXX#####", "#F#XXX#p#XXX###", "##XXp#X#X#pXX##", "#XXX##X#X##XXX#", "#Xp#X##X##X#pX#", "XX###X#X#X###XX", "XXXX##XXX##XXXX", "GGGpXXXPXXX##pX", "XXXX##XXX##XXXX", "XX###X#X#X###XX", "#Xp#X##X##X#pX#", "#XXX##X#X##XXX#", "##XXp#X#X#pXX##", "###XXX#p#XXX###", "#####XXXXX#####")
                .aisle("#####XXGXX#####", "#F#XXX#R#XXX###", "##GXR#X#X#RXG##", "#XXX##X#X##XXX#", "#GR#X##X##X#RG#", "XX###X#X#X###XX", "XXXX##XXX##XXXX", "GGGpXXXPXXX##RG", "XXXX##XXX##XXXX", "XX###X#X#X###XX", "#GR#X##X##X#RG#", "#XXX##X#X##XXX#", "##GXR#X#X#RXG##", "###XXX#R#XXX###", "#####XXGXX#####")
                .aisle("#####XXXXX#####", "#F#XXXBBBXXX###", "##XXBBXBXBBXX##", "#XXXBBXBXBBXXX#", "#XBBXBBXBBXBBX#", "XXBBBXBXBXBBBXX", "XXXXBBXXXBBXXBX", "SGGpXXXPXXXBBBX", "XXXXBBXXXBBXXBX", "XXBBBXBXBXBBBXX", "#XBBXBBXBBXBBX#", "#XXXBBXBXBBXXX#", "##XXBBXBXBBXX##", "###XXXBBBXXX###", "#####XXXXX#####")
                .setAmountAtLeast('L', 100)
                .where('S', selfPredicate())
                .where('L', statePredicate(getCasingState()))
                .where('f', statePredicate(getFrameState()))
                .where('F', statePredicate(getSecondaryFrameState()))
                .where('X', statePredicate(getCasingState()).or(abilityPartPredicate(ALLOWED_ABILITIES)))
                .where('T', statePredicate(getSecondaryCasingState()).or(abilityPartPredicate(ALLOWED_ABILITIES)))
                .where('C', (heatingCoilPredicate().or(heatingCoilPredicate2())))
                .where('P', frameworkPredicate().or(frameworkPredicate2()))
                .where('p', statePredicate(MetaBlocks.BOILER_CASING.getState(BlockBoilerCasing.BoilerCasingType.TUNGSTENSTEEL_PIPE)))
                .where('G', statePredicate(GAMetaBlocks.TRANSPARENT_CASING.getState(GATransparentCasing.CasingType.OSMIRIDIUM_GLASS)))
                .where('g', statePredicate(MetaBlocks.MUTLIBLOCK_CASING.getState(BlockMultiblockCasing.MultiblockCasingType.GRATE_CASING)))
                .where('m', abilityPartPredicate(GregicAdditionsCapabilities.MUFFLER_HATCH))
                .where('R', statePredicate(MetaBlocks.BOILER_FIREBOX_CASING.getState(BlockFireboxCasing.FireboxCasingType.TUNGSTENSTEEL_FIREBOX)))
                .where('B', statePredicate(MetaBlocks.METAL_CASING.getState(BlockMetalCasing.MetalCasingType.PRIMITIVE_BRICKS)))
                .where('#', (tile) -> true)
                .build();
    }

    public static IBlockState getFrameState() {
        return MetaBlocks.FRAMES.get(BlueSteel).getDefaultState();
    }

    public static IBlockState getSecondaryFrameState() {
        return MetaBlocks.FRAMES.get(BlackSteel).getDefaultState();
    }

    @Override
    protected void formStructure(PatternMatchContext context) {
        super.formStructure(context);
        blastFurnaceTemperature = context.getOrDefault("blastFurnaceTemperature", 0);

        int energyTier = GAUtility.getTierByVoltage(getEnergyContainer().getInputVoltage());
        this.bonusTemperature = Math.max(0, 100 * Math.min(GAUtility.getTierByVoltage(this.maxVoltage), energyTier - 2));
        this.blastFurnaceTemperature += this.bonusTemperature;
    }

    @Override
    public void invalidateStructure() {
        super.invalidateStructure();
        this.blastFurnaceTemperature = 0;
        this.bonusTemperature = 0;
    }

    public static Predicate<BlockWorldState> heatingCoilPredicate() {
        return blockWorldState -> {
            IBlockState blockState = blockWorldState.getBlockState();
            if (!(blockState.getBlock() instanceof BlockWireCoil))
                return false;
            BlockWireCoil blockWireCoil = (BlockWireCoil) blockState.getBlock();
            BlockWireCoil.CoilType coilType = blockWireCoil.getState(blockState);
            if (Arrays.asList(GAConfig.multis.heatingCoils.gtceHeatingCoilsBlacklist).contains(coilType.getName()))
                return false;

            int blastFurnaceTemperature = coilType.getCoilTemperature();
            int currentTemperature = blockWorldState.getMatchContext().getOrPut("blastFurnaceTemperature", blastFurnaceTemperature);

            BlockWireCoil.CoilType currentCoilType = blockWorldState.getMatchContext().getOrPut("coilType", coilType);

            return currentTemperature == blastFurnaceTemperature && coilType.equals(currentCoilType);
        };
    }
    public static Predicate<BlockWorldState> heatingCoilPredicate2() {
        return blockWorldState -> {
            IBlockState blockState = blockWorldState.getBlockState();
            if (!(blockState.getBlock() instanceof GAHeatingCoil))
                return false;
            GAHeatingCoil blockWireCoil = (GAHeatingCoil) blockState.getBlock();
            GAHeatingCoil.CoilType coilType = blockWireCoil.getState(blockState);
            if (Arrays.asList(GAConfig.multis.heatingCoils.gregicalityheatingCoilsBlacklist).contains(coilType.getName()))
                return false;

            int blastFurnaceTemperature = coilType.getCoilTemperature();
            int currentTemperature = blockWorldState.getMatchContext().getOrPut("blastFurnaceTemperature", blastFurnaceTemperature);

            GAHeatingCoil.CoilType currentCoilType = blockWorldState.getMatchContext().getOrPut("gaCoilType", coilType);

            return currentTemperature == blastFurnaceTemperature && coilType.equals(currentCoilType);
        };
    }
    @Override
    public boolean checkRecipe(Recipe recipe, boolean consumeIfSuccess) {
        int recipeRequiredTemp = recipe.getRecipePropertyStorage().getRecipePropertyValue(BlastTemperatureProperty.getInstance(), 0); //todo why is this 0
        return this.blastFurnaceTemperature >= recipeRequiredTemp && super.checkRecipe(recipe, consumeIfSuccess);
    }

    @Override
    protected void addDisplayText(List<ITextComponent> textList) {
        super.addDisplayText(textList);
        if (isStructureFormed() && !hasProblems()) {
            textList.add(new TextComponentTranslation("gregtech.multiblock.blast_furnace.max_temperature", blastFurnaceTemperature));
            textList.add(new TextComponentTranslation("gtadditions.multiblock.blast_furnace.additional_temperature", bonusTemperature));
        }
    }

    @Override
    public void addInformation(ItemStack stack, @Nullable World player, List<String> tooltip, boolean advanced) {
        tooltip.add(I18n.format("gtadditions.multiblock.universal.tooltip.1", this.recipeMap.getLocalizedName()));
        tooltip.add(I18n.format("gtadditions.multiblock.mega_logic.tooltip.1"));
        tooltip.add(I18n.format("gtadditions.multiblock.mega_blast_logic.tooltip.1"));
        tooltip.add(I18n.format("gtadditions.multiblock.mega_blast_logic.tooltip.2"));
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void runMufflerEffect(float xPos, float yPos, float zPos, float xSpd, float ySpd, float zSpd) {
        for (int x = -1; x <= 1; x++) {
            for (int z = -1; z <= 1; z++) {
                getWorld().spawnParticle(EnumParticleTypes.SMOKE_LARGE, xPos + x, yPos, zPos + z, xSpd, ySpd, zSpd);
            }
        }
    }

    public static final IBlockState casingState = MetaBlocks.METAL_CASING.getState(BlockMetalCasing.MetalCasingType.INVAR_HEATPROOF);

    public IBlockState getCasingState() {
        return casingState;
    }

    public static final IBlockState secondaryCasingState = MetaBlocks.METAL_CASING.getState(BlockMetalCasing.MetalCasingType.TUNGSTENSTEEL_ROBUST);

    public IBlockState getSecondaryCasingState() {
        return secondaryCasingState;
    }

    @Override
    public ICubeRenderer getBaseTexture(IMultiblockPart sourcePart) {
        return Textures.HEAT_PROOF_CASING;
    }

    protected int getBlastFurnaceTemperature() {
        return this.blastFurnaceTemperature;
    }


    protected static class MegaBlastFurnaceRecipeLogic extends MegaMultiblockRecipeLogic {

        private static final double LOG_4 = Math.log(4);

        public MegaBlastFurnaceRecipeLogic(RecipeMapMultiblockController tileEntity, int EUtPercentage, int durationPercentage, int chancePercentage) {
            super(tileEntity, EUtPercentage, durationPercentage, chancePercentage);
        }

        @Override
        protected void setupRecipe(Recipe recipe) {
            long maxVoltage = ((MegaMultiblockRecipeMapController) metaTileEntity).maxVoltage;
            int[] resultOverclock = calculateOverclock(recipe.getEUt(), maxVoltage, recipe.getDuration());
            this.progressTime = 1;
            this.setMaxProgress(resultOverclock[1]);
            this.recipeEUt = resultOverclock[0];
            this.fluidOutputs = GTUtility.copyFluidList(recipe.getFluidOutputs());
            int tier = this.getMachineTierForRecipe(recipe);
            this.itemOutputs = GTUtility.copyStackList(recipe.getResultItemOutputs(this.getOutputInventory().getSlots(), this.random, tier));
            if (this.wasActiveAndNeedsUpdate) {
                this.wasActiveAndNeedsUpdate = false;
            } else {
                this.setActive(true);
            }
        }

        @Override
        protected Recipe findRecipe(long maxVoltage, IItemHandlerModifiable inputs, IMultipleTankHandler fluidInputs) {
            Recipe recipe = super.findRecipe(maxVoltage, inputs, fluidInputs);
            int currentTemp = ((MetaTileEntityMegaBlastFurnace) metaTileEntity).getBlastFurnaceTemperature();
            if (recipe != null && recipe.getRecipePropertyStorage().getRecipePropertyValue(BlastTemperatureProperty.getInstance(), 0) <= currentTemp)
                return createRecipe(maxVoltage, inputs, fluidInputs, recipe);
            return null;
        }

        @Override
        protected Recipe createRecipe(long maxVoltage, IItemHandlerModifiable inputs, IMultipleTankHandler fluidInputs, Recipe matchingRecipe) {
            long EUt;
            int duration;
            int minMultiplier = Integer.MAX_VALUE;
            int recipeTemp = matchingRecipe.getRecipePropertyStorage().getRecipePropertyValue(BlastTemperatureProperty.getInstance(), 0);
            int tier = getOverclockingTier(maxVoltage);

            Set<ItemStack> countIngredients = new HashSet<>();
            if (matchingRecipe.getInputs().size() != 0) {
                this.findIngredients(countIngredients, inputs);
                minMultiplier = this.getMinRatioItem(countIngredients, matchingRecipe, MAX_ITEMS_LIMIT);
            }

            Map<String, Integer> countFluid = new HashMap<>();
            if (matchingRecipe.getFluidInputs().size() != 0) {

                this.findFluid(countFluid, fluidInputs);
                minMultiplier = Math.min(minMultiplier, this.getMinRatioFluid(countFluid, matchingRecipe, MAX_ITEMS_LIMIT));
            }

            if (minMultiplier == Integer.MAX_VALUE) {
                GALog.logger.error("Cannot calculate ratio of items for the mega blast furnace");
                return null;
            }

            EUt = matchingRecipe.getEUt();
            duration = matchingRecipe.getDuration();
            int currentTemp = ((MetaTileEntityMegaBlastFurnace) this.metaTileEntity).getBlastFurnaceTemperature();

            // Get amount of 900Ks over the recipe temperature
            int bonusAmount = Math.max(0, (currentTemp - recipeTemp) / 900);

            // Apply EUt discount for every 900K above the base recipe temperature
            EUt *= Math.pow(0.95, bonusAmount);

            // Get parallel recipes to run: [0, 256]
            int multiplier = Math.min(minMultiplier, (int) (getMaxVoltage() / EUt));

            // Change EUt to be the parallel amount
            EUt *= multiplier;

            // Modify bonus amount to prefer parallel logic
            bonusAmount = (int) Math.max(0, bonusAmount - Math.log(multiplier) / LOG_4 * 2);

            // Apply MEBF duration discount
            duration *= 0.5;

            // Apply Super Overclocks for every 1800k above the base recipe temperature
            for (int i = bonusAmount; EUt <= GAValues.V[tier - 1] && duration >= 3 && i > 0; i--) {
                if (i % 2 == 0) {
                    EUt *= 4;
                    duration *= 0.25;
                }
            }

            // Apply Regular Overclocking
            while (duration >= 3 && EUt <= GAValues.V[tier - 1]) {
                EUt *= 4;
                duration /= 2.8;
            }

            if (duration < 3)
                duration = 3;

            List<CountableIngredient> newRecipeInputs = new ArrayList<>();
            List<FluidStack> newFluidInputs = new ArrayList<>();
            List<ItemStack> outputI = new ArrayList<>();
            List<FluidStack> outputF = new ArrayList<>();
            this.multiplyInputsAndOutputs(newRecipeInputs, newFluidInputs, outputI, outputF, matchingRecipe, multiplier);

            RecipeBuilder<?> newRecipe = recipeMap.recipeBuilder();
            copyChancedItemOutputs(newRecipe, matchingRecipe, minMultiplier);

            // determine if there is enough room in the output to fit all of this
            // if there isn't, we can't process this recipe.
            List<ItemStack> totalOutputs = newRecipe.getChancedOutputs().stream().map(Recipe.ChanceEntry::getItemStack).collect(Collectors.toList());
            totalOutputs.addAll(outputI);
            boolean canFitOutputs = InventoryUtils.simulateItemStackMerge(totalOutputs, this.getOutputInventory());
            if (!canFitOutputs)
                return matchingRecipe;

            newRecipe.inputsIngredients(newRecipeInputs)
                    .fluidInputs(newFluidInputs)
                    .outputs(outputI)
                    .fluidOutputs(outputF)
                    .EUt((int) Math.max(1, EUt))
                    .duration(duration);

            return newRecipe.build().getResult();
        }
    }
}
