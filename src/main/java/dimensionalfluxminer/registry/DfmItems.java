package dimensionalfluxminer.registry;

import dimensionalfluxminer.DimensionalFluxMiner;
import dimensionalfluxminer.content.GuideBookItem;
import dimensionalfluxminer.content.ReinforcedEnderEyeItem;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.world.item.BucketItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.Rarity;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class DfmItems {
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(DimensionalFluxMiner.MOD_ID);

    /** Materials with a registered DFM dust, in canonical order. */
    public static final List<String> DUST_MATERIALS = List.of(
            "iron", "gold", "copper", "coal", "diamond", "emerald", "lapis", "quartz",
            "tin", "lead", "silver", "uranium", "aluminum", "nickel", "osmium", "zinc");
    /** Dusts that have a built-in DFM acquisition recipe and are therefore advertised in the creative tab. */
    public static final List<String> DUSTS_WITH_BUILTIN_RECIPE = List.of("iron", "coal");

    // Assembly parts
    public static final DeferredItem<?> STEEL_CHASSIS = ITEMS.registerSimpleBlockItem(DfmBlocks.STEEL_CHASSIS);
    public static final DeferredItem<?> VENTILATION_UNIT = ITEMS.registerSimpleBlockItem(DfmBlocks.VENTILATION_UNIT);
    public static final DeferredItem<?> REINFORCED_STRUCTURAL_GLASS = ITEMS.registerSimpleBlockItem(DfmBlocks.REINFORCED_STRUCTURAL_GLASS);
    public static final DeferredItem<?> DIMENSIONAL_PORT = ITEMS.registerSimpleBlockItem(DfmBlocks.DIMENSIONAL_PORT);
    public static final DeferredItem<?> QUANTUM_CORE = ITEMS.registerSimpleBlockItem(DfmBlocks.QUANTUM_CORE);
    public static final DeferredItem<?> MINER_TERMINAL = ITEMS.registerSimpleBlockItem(DfmBlocks.MINER_TERMINAL);

    // Machines
    public static final DeferredItem<?> DIMENSIONAL_CRUSHER = ITEMS.registerSimpleBlockItem(DfmBlocks.DIMENSIONAL_CRUSHER);
    public static final DeferredItem<?> ALLOY_SMELTER = ITEMS.registerSimpleBlockItem(DfmBlocks.ALLOY_SMELTER);

    // Cores and upgrade
    public static final DeferredItem<Item> BASIC_DIMENSIONAL_CORE = ITEMS.registerSimpleItem("basic_dimensional_core", new Item.Properties().rarity(Rarity.RARE));
    public static final DeferredItem<Item> STABILIZED_DIMENSIONAL_CORE = ITEMS.registerSimpleItem("stabilized_dimensional_core", new Item.Properties().rarity(Rarity.EPIC));
    public static final DeferredItem<Item> EFFICIENCY_UPGRADE = ITEMS.registerSimpleItem("efficiency_upgrade", new Item.Properties().rarity(Rarity.EPIC));

    // Guide
    public static final DeferredItem<GuideBookItem> GUIDE_BOOK = ITEMS.registerItem("guide_book", GuideBookItem::new,
            new Item.Properties().stacksTo(1).rarity(Rarity.UNCOMMON));

    // Materials
    public static final DeferredItem<ReinforcedEnderEyeItem> ENDER_EYE_REINFORCED = ITEMS.registerItem("ender_eye_reinforced", ReinforcedEnderEyeItem::new);
    public static final DeferredItem<Item> INGOT_STEEL = ITEMS.registerSimpleItem("ingot_steel");
    public static final DeferredItem<Item> GRAIN_CALCITE = ITEMS.registerSimpleItem("grain_calcite");
    public static final DeferredItem<Item> SLAG = ITEMS.registerSimpleItem("slag");
    public static final DeferredItem<?> SLAG_AGGREGATE = ITEMS.registerSimpleBlockItem(DfmBlocks.SLAG_AGGREGATE);
    public static final Map<String, DeferredItem<Item>> DUSTS = registerDusts();

    // Dimensional Blight
    public static final DeferredItem<?> CORRUPTED_DIRT = ITEMS.registerSimpleBlockItem(DfmBlocks.CORRUPTED_DIRT);
    public static final DeferredItem<?> CORRUPTED_GRASS = ITEMS.registerSimpleBlockItem(DfmBlocks.CORRUPTED_GRASS);
    public static final DeferredItem<?> CORRUPTED_STONE = ITEMS.registerSimpleBlockItem(DfmBlocks.CORRUPTED_STONE);
    public static final DeferredItem<?> CORRUPTED_TERRACOTTA = ITEMS.registerSimpleBlockItem(DfmBlocks.CORRUPTED_TERRACOTTA);
    public static final DeferredItem<?> CORRUPTED_SEDIMENT = ITEMS.registerSimpleBlockItem(DfmBlocks.CORRUPTED_SEDIMENT);
    public static final DeferredItem<?> CORRUPTED_GRASS_PLANT = ITEMS.registerSimpleBlockItem(DfmBlocks.CORRUPTED_GRASS_PLANT);
    public static final DeferredItem<BucketItem> CORRUPTED_WATER_BUCKET = ITEMS.registerItem("corrupted_water_bucket",
            props -> new BucketItem(DfmFluids.CORRUPTED_WATER.get(), props), new Item.Properties().craftRemainder(Items.BUCKET).stacksTo(1));

    private static Map<String, DeferredItem<Item>> registerDusts() {
        Map<String, DeferredItem<Item>> dusts = new LinkedHashMap<>();
        for (String material : DUST_MATERIALS) {
            dusts.put(material, ITEMS.registerSimpleItem("dust_" + material));
        }
        return Map.copyOf(dusts);
    }

    public static boolean isCore(Item item) {
        return item == BASIC_DIMENSIONAL_CORE.get() || item == STABILIZED_DIMENSIONAL_CORE.get();
    }

    private DfmItems() {
    }
}
