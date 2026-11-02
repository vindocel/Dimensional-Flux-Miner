package dimensionalfluxminer.compat.rei;

import dimensionalfluxminer.registry.DfmItems;
import me.shedaniel.rei.api.common.entry.comparison.ItemComparatorRegistry;
import me.shedaniel.rei.api.common.plugins.REICommonPlugin;
import me.shedaniel.rei.forge.REIPluginCommon;

/** The side of the REI integration that REI loads on both client and server. */
@REIPluginCommon
public final class DfmReiCommonPlugin implements REICommonPlugin {
    /** Creative tanks holding different liquids are different entries. */
    @Override
    public void registerItemComparators(ItemComparatorRegistry registry) {
        registry.registerComponents(DfmItems.CREATIVE_DIMENSIONAL_TANK.get());
    }
}
