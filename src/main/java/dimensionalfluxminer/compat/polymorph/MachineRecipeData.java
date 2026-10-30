package dimensionalfluxminer.compat.polymorph;

import com.illusivesoulworks.polymorph.api.PolymorphApi;
import com.illusivesoulworks.polymorph.api.common.base.IRecipePair;
import com.illusivesoulworks.polymorph.api.common.capability.IBlockEntityRecipeData;
import dimensionalfluxminer.recipe.RecipeChoices;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.UUID;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeInput;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;

/**
 * Recipe list of one machine for the players with its screen open: the machine's current {@link RecipeChoices.Offer},
 * resent whenever it changes. A pick is stored in the machine's own {@link RecipeChoices} (saved by the machine), so
 * this object keeps no data of its own.
 */
final class MachineRecipeData implements IBlockEntityRecipeData {
    private final BlockEntity owner;
    private final RecipeChoices.Host host;
    private final Map<UUID, ServerPlayer> listeners = new HashMap<>();

    private SortedSet<IRecipePair> recipes = new TreeSet<>();
    private List<RecipeChoices.Option> options = List.of();
    private RecipeHolder<?> selected;

    private List<RecipeChoices.Option> sentOptions;
    private ResourceLocation sentSelected;

    private MachineRecipeData(BlockEntity owner, RecipeChoices.Host host) {
        this.owner = owner;
        this.host = host;
    }

    static IBlockEntityRecipeData create(BlockEntity blockEntity) {
        return blockEntity instanceof RecipeChoices.Host host ? new MachineRecipeData(blockEntity, host) : null;
    }

    /** Rebuilds the list from what the machine holds now. */
    private void refresh() {
        Level level = owner.getLevel();
        SortedSet<IRecipePair> list = new TreeSet<>();
        List<RecipeChoices.Option> current = List.of();
        RecipeHolder<?> chosen = null;
        Optional<RecipeChoices.Offer> offer = level == null ? Optional.empty() : host.recipeOffer();
        if (offer.isPresent()) {
            current = offer.get().options();
            for (int i = 0; i < current.size(); i++) {
                list.add(new Pair(i, current.get(i).id(), current.get(i).result()));
            }
            chosen = level.getRecipeManager().byKey(offer.get().selected(host.recipeChoices())).orElse(null);
        }
        recipes = list;
        options = current;
        selected = chosen;
    }

    private ResourceLocation selectedId() {
        return selected == null ? null : selected.id();
    }

    @Override
    public void tick() {
        if (listeners.isEmpty()) {
            return;
        }
        refresh();
        if (sentOptions == null || !sameOptions(sentOptions, options) || !Objects.equals(sentSelected, selectedId())) {
            sendRecipesListToListeners();
        }
    }

    private static boolean sameOptions(List<RecipeChoices.Option> a, List<RecipeChoices.Option> b) {
        if (a.size() != b.size()) {
            return false;
        }
        for (int i = 0; i < a.size(); i++) {
            if (!a.get(i).id().equals(b.get(i).id()) || !ItemStack.matches(a.get(i).result(), b.get(i).result())) {
                return false;
            }
        }
        return true;
    }

    @Override
    public boolean isEmpty() {
        refresh();
        return recipes.isEmpty();
    }

    /**
     * Polymorph sends here every recipe lookup made while the machine ticks. The machines apply their own
     * {@link RecipeChoices}, so this answers what the lookup would answer without Polymorph: the first recipe of the type
     * that matches. It must not call {@code RecipeManager.getRecipeFor}, which would come back here.
     */
    @Override
    public <I extends RecipeInput, T extends Recipe<I>> RecipeHolder<T> getRecipe(RecipeType<T> type, I input, Level level,
            List<RecipeHolder<T>> recipesList) {
        if (!recipesList.isEmpty()) {
            return recipesList.getFirst();
        }
        if (input.isEmpty()) {
            return null;
        }
        for (RecipeHolder<T> holder : level.getRecipeManager().getAllRecipesFor(type)) {
            if (holder.value().matches(input, level)) {
                return holder;
            }
        }
        return null;
    }

    @Override
    public void selectRecipe(RecipeHolder<?> recipe) {
        if (recipe != null && host.chooseRecipe(recipe.id())) {
            refresh();
            sendRecipesListToListeners();
        }
    }

    @Override
    public RecipeHolder<?> getSelectedRecipe() {
        refresh();
        return selected;
    }

    @Override
    public void setSelectedRecipe(RecipeHolder<?> recipe) {
        selectRecipe(recipe);
    }

    @Override
    public SortedSet<IRecipePair> getRecipesList() {
        refresh();
        return recipes;
    }

    /** The list always comes from the machine's contents. */
    @Override
    public void setRecipesList(SortedSet<IRecipePair> list) {
    }

    @Override
    public Collection<ServerPlayer> getListeners() {
        return Collections.unmodifiableCollection(listeners.values());
    }

    @Override
    public void addListener(ServerPlayer player) {
        listeners.put(player.getUUID(), player);
    }

    @Override
    public void removeListener(ServerPlayer player) {
        listeners.remove(player.getUUID());
    }

    @Override
    public void clearListeners() {
        listeners.clear();
    }

    @Override
    public void sendRecipesListToListeners() {
        sentOptions = options;
        sentSelected = selectedId();
        for (ServerPlayer player : listeners.values()) {
            PolymorphApi.getInstance().getNetwork().sendRecipesListS2C(player, recipes, sentSelected);
        }
    }

    @Override
    public BlockEntity getOwner() {
        return owner;
    }

    /** The choices are saved by the machine itself. */
    @Override
    public CompoundTag writeNBT(HolderLookup.Provider registries) {
        return new CompoundTag();
    }

    @Override
    public void readNBT(HolderLookup.Provider registries, CompoundTag tag) {
    }

    /** One entry of the list, kept in the machine's order (the default recipe first). */
    private record Pair(int order, ResourceLocation id, ItemStack output) implements IRecipePair {
        @Override
        public ItemStack getOutput() {
            return output;
        }

        @Override
        public ResourceLocation getResourceLocation() {
            return id;
        }

        @Override
        public int compareTo(IRecipePair other) {
            return other instanceof Pair pair ? Integer.compare(order, pair.order) : id.compareTo(other.getResourceLocation());
        }
    }
}
