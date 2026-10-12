package dimensionalfluxminer.registry;

import dimensionalfluxminer.DimensionalFluxMiner;
import dimensionalfluxminer.content.CorruptedWaterFluid;
import dimensionalfluxminer.content.TransportOnlyFluid;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.pathfinder.PathType;
import net.neoforged.neoforge.fluids.FluidStack;
import net.minecraft.world.level.material.FlowingFluid;
import net.minecraft.world.level.material.Fluid;
import net.neoforged.neoforge.common.SoundActions;
import net.neoforged.neoforge.fluids.BaseFlowingFluid;
import net.neoforged.neoforge.fluids.FluidType;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

public final class DfmFluids {
    public static final DeferredRegister<FluidType> FLUID_TYPES = DeferredRegister.create(NeoForgeRegistries.Keys.FLUID_TYPES, DimensionalFluxMiner.MOD_ID);
    public static final DeferredRegister<Fluid> FLUIDS = DeferredRegister.create(Registries.FLUID, DimensionalFluxMiner.MOD_ID);

    /** Output-only transport representation of the Alloy Smelter's stored experience. */
    public static final DeferredHolder<FluidType, FluidType> LIQUID_EXPERIENCE_TYPE = FLUID_TYPES.register("liquid_experience",
            () -> new FluidType(FluidType.Properties.create()
                    .descriptionId("fluid_type.dimensionalfluxminer.liquid_experience")
                    .lightLevel(6)
                    .canConvertToSource(false)
                    .canSwim(false)
                    .canDrown(false)
                    .canPushEntity(false)
                    .supportsBoating(false)
                    .canHydrate(false)
                    .canExtinguish(false)));

    public static final DeferredHolder<Fluid, TransportOnlyFluid> LIQUID_EXPERIENCE = FLUIDS.register("liquid_experience",
            () -> new TransportOnlyFluid(LIQUID_EXPERIENCE_TYPE));

    /**
     * Corrupted Water behaves exactly like water (same properties, infinite sources following the gamerule, in the
     * {@code minecraft:water} fluid tag); only its effects on living entities differ.
     */
    public static final DeferredHolder<FluidType, FluidType> CORRUPTED_WATER_TYPE = FLUID_TYPES.register("corrupted_water",
            () -> new FluidType(FluidType.Properties.create()
                    .descriptionId("fluid_type.dimensionalfluxminer.corrupted_water")
                    .canConvertToSource(true)
                    .fallDistanceModifier(0.0F)
                    .canExtinguish(true)
                    .canDrown(true)
                    .canSwim(true)
                    .supportsBoating(true)
                    .canHydrate(true)
                    .motionScale(0.014D)
                    .pathType(PathType.WATER)
                    .adjacentPathType(PathType.WATER_BORDER)
                    .sound(SoundActions.BUCKET_FILL, SoundEvents.BUCKET_FILL)
                    .sound(SoundActions.BUCKET_EMPTY, SoundEvents.BUCKET_EMPTY)
                    .sound(SoundActions.FLUID_VAPORIZE, SoundEvents.FIRE_EXTINGUISH)) {
                @Override
                public boolean canConvertToSource(FluidState state, LevelReader reader, BlockPos pos) {
                    if (reader instanceof Level level) {
                        return level.getGameRules().getBoolean(GameRules.RULE_WATER_SOURCE_CONVERSION);
                    }
                    return super.canConvertToSource(state, reader, pos);
                }

                @Override
                public boolean isVaporizedOnPlacement(Level level, BlockPos pos, FluidStack stack) {
                    return level.dimensionType().ultraWarm();
                }

                /** As in water: drowned, guardians, fish and the like breathe in it; the others drown. */
                @Override
                public boolean canDrownIn(LivingEntity entity) {
                    return !entity.canBreatheUnderwater();
                }
            });

    public static final DeferredHolder<Fluid, FlowingFluid> CORRUPTED_WATER = FLUIDS.register("corrupted_water",
            () -> new CorruptedWaterFluid.Source(corruptedWaterProperties()));
    public static final DeferredHolder<Fluid, FlowingFluid> FLOWING_CORRUPTED_WATER = FLUIDS.register("flowing_corrupted_water",
            () -> new CorruptedWaterFluid.Flowing(corruptedWaterProperties()));

    private static BaseFlowingFluid.Properties corruptedWaterProperties() {
        return new BaseFlowingFluid.Properties(CORRUPTED_WATER_TYPE, CORRUPTED_WATER, FLOWING_CORRUPTED_WATER)
                .bucket(DfmItems.CORRUPTED_WATER_BUCKET)
                .block(DfmBlocks.CORRUPTED_WATER)
                .slopeFindDistance(4)
                .levelDecreasePerBlock(1)
                .tickRate(5)
                .explosionResistance(100.0F);
    }

    private DfmFluids() {
    }
}
