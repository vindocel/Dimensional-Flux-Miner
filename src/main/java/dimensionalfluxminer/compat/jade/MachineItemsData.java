package dimensionalfluxminer.compat.jade;

import dimensionalfluxminer.DimensionalFluxMiner;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.item.ItemResource;
import snownee.jade.api.Accessor;
import snownee.jade.api.BlockAccessor;
import snownee.jade.api.view.IServerExtensionProvider;
import snownee.jade.api.view.ViewGroup;

/**
 * Items of the machines for Jade, read from their no-face view. Only the stacks that are there are sent: an empty
 * machine shows no item box (Jade would draw an empty frame over the mod name).
 */
enum MachineItemsData implements IServerExtensionProvider<ItemStack> {
    INSTANCE;

    private static final Identifier UID = DimensionalFluxMiner.id("machine_items");

    @Override
    public List<ViewGroup<ItemStack>> getGroups(Accessor<?> accessor) {
        if (!(accessor instanceof BlockAccessor block)) {
            return List.of();
        }
        ResourceHandler<ItemResource> items = block.getLevel().getCapability(Capabilities.Item.BLOCK, block.getPosition(), null);
        if (items == null) {
            return List.of();
        }
        List<ItemStack> stacks = new ArrayList<>();
        for (int i = 0; i < items.size(); i++) {
            ItemResource resource = items.getResource(i);
            int amount = items.getAmountAsInt(i);
            if (!resource.isEmpty() && amount > 0) {
                stacks.add(resource.toStack(amount));
            }
        }
        return stacks.isEmpty() ? List.of() : List.of(new ViewGroup<>(stacks));
    }

    @Override
    public Identifier getUid() {
        return UID;
    }
}
