package dimensionalfluxminer.content;

import dimensionalfluxminer.registry.DfmItems;
import java.util.Set;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.projectile.EyeOfEnder;
import net.minecraft.world.level.storage.loot.LootPool;
import net.minecraft.world.level.storage.loot.entries.LootItem;
import net.minecraft.world.level.storage.loot.predicates.LootItemRandomChanceCondition;
import net.minecraft.world.level.storage.loot.providers.number.ConstantValue;
import net.neoforged.neoforge.event.LootTableLoadEvent;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;

/** Game-bus hooks for simple content: reinforced eye survival and the Efficiency Upgrade structure loot. */
public final class ContentEvents {
    /** Each generated chest of these tables rolls once with a 1% chance of one Efficiency Upgrade. */
    private static final Set<ResourceLocation> UPGRADE_LOOT_TABLES = Set.of(
            ResourceLocation.withDefaultNamespace("chests/end_city_treasure"),
            ResourceLocation.withDefaultNamespace("chests/bastion_bridge"),
            ResourceLocation.withDefaultNamespace("chests/bastion_hoglin_stable"),
            ResourceLocation.withDefaultNamespace("chests/bastion_other"),
            ResourceLocation.withDefaultNamespace("chests/bastion_treasure"),
            ResourceLocation.withDefaultNamespace("chests/ancient_city"),
            ResourceLocation.withDefaultNamespace("chests/ancient_city_ice_box"));
    private static final float UPGRADE_CHANCE = 0.01F;

    private ContentEvents() {
    }

    public static void onEntityJoin(EntityJoinLevelEvent event) {
        if (!event.getLevel().isClientSide() && event.getEntity() instanceof EyeOfEnder eye) {
            ReinforcedEnderEyeItem.keepAlive(eye);
        }
    }

    public static void onLootTableLoad(LootTableLoadEvent event) {
        if (UPGRADE_LOOT_TABLES.contains(event.getName())) {
            event.getTable().addPool(LootPool.lootPool()
                    .name("dimensionalfluxminer_efficiency_upgrade")
                    .setRolls(ConstantValue.exactly(1))
                    .when(LootItemRandomChanceCondition.randomChance(UPGRADE_CHANCE))
                    .add(LootItem.lootTableItem(DfmItems.EFFICIENCY_UPGRADE.get()))
                    .build());
        }
    }
}
