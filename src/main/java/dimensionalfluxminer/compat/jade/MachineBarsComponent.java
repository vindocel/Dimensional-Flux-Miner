package dimensionalfluxminer.compat.jade;

import dimensionalfluxminer.client.screen.DfmScreen;
import dimensionalfluxminer.registry.DfmFluids;
import java.util.Locale;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import snownee.jade.api.BlockAccessor;
import snownee.jade.api.IBlockComponentProvider;
import snownee.jade.api.ITooltip;
import snownee.jade.api.config.IPluginConfig;

/**
 * Client side of the machine bars: the machine energy bar, then the Electric Furnace's stored experience as a Liquid
 * Experience bar in millibuckets, the unit pipes extract it in.
 */
enum MachineBarsComponent implements IBlockComponentProvider {
    INSTANCE;

    /** Experience has no capacity: the bar uses a logarithmic scale, full at 10 000 XP. */
    private static final double XP_FULL_LOG = 4.0;
    private static final int MB_PER_XP = 20;

    @Override
    public void appendTooltip(ITooltip tooltip, BlockAccessor accessor, IPluginConfig config) {
        CompoundTag data = accessor.getServerData();
        if (!data.contains(DfmJadePlugin.ENERGY)) {
            return;
        }
        long energy = data.getLong(DfmJadePlugin.ENERGY);
        long capacity = Math.max(1, data.getLong(DfmJadePlugin.CAPACITY));
        // The energy fill follows the icon color the player picked for this machine (blue, or purple with purple icons).
        net.minecraft.core.BlockPos machine = data.contains(DfmJadePlugin.MACHINE)
                ? net.minecraft.core.BlockPos.of(data.getLong(DfmJadePlugin.MACHINE)) : accessor.getPosition();
        tooltip.add(MachineBarElement.energy(energy / (double) capacity,
                Component.literal(DfmScreen.compact(energy) + " / " + DfmScreen.fe(capacity)),
                dimensionalfluxminer.client.screen.GuiThemes.icons(machine)));
        if (data.contains(DfmJadePlugin.XP)) {
            double xp = data.getDouble(DfmJadePlugin.XP);
            long millibuckets = Math.round(xp * MB_PER_XP);
            Component text = Component.empty().append(DfmFluids.LIQUID_EXPERIENCE_TYPE.get().getDescription())
                    .append(String.format(Locale.ROOT, ": %,d mB", millibuckets));
            tooltip.add(MachineBarElement.fluid(DfmFluids.LIQUID_EXPERIENCE.get(), Math.log10(1 + xp) / XP_FULL_LOG, text));
        }
    }

    @Override
    public ResourceLocation getUid() {
        return DfmJadePlugin.UID;
    }
}
