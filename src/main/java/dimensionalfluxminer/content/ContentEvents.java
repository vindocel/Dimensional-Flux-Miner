package dimensionalfluxminer.content;

import dimensionalfluxminer.registry.DfmItems;
import java.util.Set;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.projectile.EyeOfEnder;
import net.minecraft.world.level.storage.loot.LootPool;
import net.minecraft.world.level.storage.loot.entries.LootItem;
import net.minecraft.world.level.storage.loot.predicates.LootItemRandomChanceCondition;
import net.minecraft.world.level.storage.loot.providers.number.ints.ContextIntProviders;
import net.neoforged.neoforge.event.LootTableLoadEvent;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;

/** Game-bus hooks for simple content: reinforced eye survival and the Efficiency Upgrade structure loot. */
public final class ContentEvents {
    /** Each generated chest of these tables rolls once with a 1% chance of one Efficiency Upgrade. */
    private static final Set<Identifier> UPGRADE_LOOT_TABLES = Set.of(
            Identifier.withDefaultNamespace("chests/end_city_treasure"),
            Identifier.withDefaultNamespace("chests/bastion_bridge"),
            Identifier.withDefaultNamespace("chests/bastion_hoglin_stable"),
            Identifier.withDefaultNamespace("chests/bastion_other"),
            Identifier.withDefaultNamespace("chests/bastion_treasure"),
            Identifier.withDefaultNamespace("chests/ancient_city"),
            Identifier.withDefaultNamespace("chests/ancient_city_ice_box"));
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
                    .setRolls(ContextIntProviders.exactly(1))
                    .when(LootItemRandomChanceCondition.randomChance(UPGRADE_CHANCE))
                    .add(LootItem.lootTableItem(DfmItems.EFFICIENCY_UPGRADE.get()))
                    .build());
        }
    }
}
