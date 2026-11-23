package dimensionalfluxminer.client.render;

import dimensionalfluxminer.DimensionalFluxMiner;
import dimensionalfluxminer.client.screen.GuiThemes;
import dimensionalfluxminer.content.DfmStateProperties;
import dimensionalfluxminer.content.MachineState;
import dimensionalfluxminer.machine.AbstractMachineBlockEntity;
import dimensionalfluxminer.registry.DfmBlocks;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
//? if >=26.1 {
import com.mojang.serialization.MapCodec;
import java.util.List;
import net.minecraft.client.color.block.BlockTintSource;
import net.minecraft.client.color.item.ItemTintSource;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.block.BlockAndTintGetter;
//?}
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
//? if >=26.1 {
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.properties.Property;
//?}
import net.neoforged.neoforge.client.event.RegisterColorHandlersEvent;
//? if >=26.1 {
import org.jspecify.annotations.Nullable;
//?}

/**
 * The animated terminal of the machines follows the theme color the player picked (tint layer 0 of the terminal
 * faces, whose textures are neutral gray). The color is the player's own, read on this computer by position, so
 * every player sees the machines in the color of their own choice. Off machines (gray terminal) and the Miner's
 * no-core warning (yellow) are drawn with white, which keeps their artwork.
 */
public final class TerminalTint {
    /** Keeps the texture as drawn. */
    public static final int WHITE = 0xFFFFFFFF;

    private TerminalTint() {
    }

    /** Theme color (ARGB) of the terminal of the machine at {@code pos}. */
    public static int at(BlockPos pos) {
        return 0xFF000000 | GuiThemes.terminalRgb(pos);
    }

    /** Theme color (ARGB) shared by every machine: items, guide pictures. */
    public static int global() {
        return 0xFF000000 | GuiThemes.globalTerminalRgb();
    }

    /** Crusher and Electric Furnace in the world: white while off, otherwise the machine's theme color. */
    static int machine(BlockState state, BlockPos pos) {
        if (!state.hasProperty(DfmStateProperties.MACHINE_STATE)
                || state.getValue(DfmStateProperties.MACHINE_STATE) == MachineState.OFF) {
            return WHITE;
        }
        return at(pos);
    }

    /** Redraws the chunk of the machine at {@code pos} after its theme color changed. */
    public static void refresh(BlockPos pos) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) {
            return;
        }
        //? if >=26.3 {
        /*mc.levelExtractor.setBlocksDirty(pos.getX(), pos.getY(), pos.getZ(), pos.getX(), pos.getY(), pos.getZ());
        *///?} else {
        mc.levelRenderer.setBlocksDirty(pos.getX(), pos.getY(), pos.getZ(), pos.getX(), pos.getY(), pos.getZ());
        //?}
    }

    /** Machines with a tinted terminal seen on this client (positions are checked again before each use). */
    private static final Set<BlockPos> SEEN = ConcurrentHashMap.newKeySet();

    /** Client tick of a Crusher or Electric Furnace. */
    public static void seen(BlockEntity be) {
        SEEN.add(be.getBlockPos().immutable());
    }

    /** Redraws the terminal of every machine seen after the shared theme color changed. */
    public static void refreshAll() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) {
            SEEN.clear();
            return;
        }
        SEEN.removeIf(pos -> !(mc.level.getBlockEntity(pos) instanceof AbstractMachineBlockEntity));
        SEEN.forEach(TerminalTint::refresh);
    }

    //? if >=26.1 {
    public static void registerBlocks(RegisterColorHandlersEvent.BlockTintSources event) {
        event.register(List.of(new MachineSource()), DfmBlocks.MECHANICAL_CRUSHER.get(), DfmBlocks.ELECTRIC_FURNACE.get());
    }

    public static void registerItems(RegisterColorHandlersEvent.ItemTintSources event) {
        event.register(DimensionalFluxMiner.id("theme"), ItemTheme.MAP_CODEC);
    }

    private static final class MachineSource implements BlockTintSource {
        @Override
        public int color(BlockState state) {
            return state.hasProperty(DfmStateProperties.MACHINE_STATE)
                    && state.getValue(DfmStateProperties.MACHINE_STATE) != MachineState.OFF ? global() : WHITE;
        }

        @Override
        public int colorInWorld(BlockState state, BlockAndTintGetter level, BlockPos pos) {
            return machine(state, pos);
        }

        @Override
        public Set<Property<?>> relevantProperties() {
            return Set.of(DfmStateProperties.MACHINE_STATE);
        }
    }

    /** {@code dimensionalfluxminer:theme} item tint: the shared theme color. */
    public record ItemTheme() implements ItemTintSource {
        public static final MapCodec<ItemTheme> MAP_CODEC = MapCodec.unit(new ItemTheme());

        @Override
        public int calculate(ItemStack stack, @Nullable ClientLevel level, @Nullable LivingEntity owner) {
            return global();
        }

        @Override
        public MapCodec<ItemTheme> type() {
            return MAP_CODEC;
        }
    }
    //?} else {
    /*public static void registerBlocks(RegisterColorHandlersEvent.Block event) {
        event.register((state, level, pos, tintIndex) -> {
            if (tintIndex != 0) {
                return WHITE;
            }
            return pos == null ? global() : machine(state, pos);
        }, DfmBlocks.MECHANICAL_CRUSHER.get(), DfmBlocks.ELECTRIC_FURNACE.get());
    }

    public static void registerItems(RegisterColorHandlersEvent.Item event) {
        event.register((stack, tintIndex) -> tintIndex == 0 ? global() : WHITE,
                DfmBlocks.MECHANICAL_CRUSHER.get(), DfmBlocks.ELECTRIC_FURNACE.get());
    }
    *///?}
}
