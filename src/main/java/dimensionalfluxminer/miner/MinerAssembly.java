package dimensionalfluxminer.miner;

import dimensionalfluxminer.miner.MinerStructure.Member;
import dimensionalfluxminer.network.MinerAssemblyPayload;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * Assisted assembly from an unformed terminal: each click places the next missing part (bottom layer first) from the
 * player's inventory, through the normal block placement (so protections, sounds and item use apply), and shows the
 * player where the remaining parts go. Placing the terminal shows the highlight; a click that places nothing switches
 * it off and on.
 */
public final class MinerAssembly {
    private MinerAssembly() {
    }

    /** Missing participants of the assembly around {@code terminal}, bottom layer first. */
    public static List<Member> missing(ServerLevel level, BlockPos terminal) {
        BlockState terminalState = level.getBlockState(terminal);
        if (!terminalState.hasProperty(BlockStateProperties.HORIZONTAL_FACING)) {
            return List.of();
        }
        MinerStructure layout = MinerStructure.layout(terminal, terminalState.getValue(BlockStateProperties.HORIZONTAL_FACING));
        List<Member> missing = new ArrayList<>();
        for (Member member : layout.members()) {
            if (level.isLoaded(member.pos()) && !level.getBlockState(member.pos()).is(MinerStructure.blockFor(member.slot().part()))) {
                missing.add(member);
            }
        }
        missing.sort(Comparator.comparingInt(m -> m.pos().getY()));
        return missing;
    }

    /**
     * Places the next missing part the player can place and returns the message for the player, or {@code null} when
     * nothing is missing.
     */
    public static Component placeNext(ServerLevel level, BlockPos terminal, Player player) {
        List<Member> missing = missing(level, terminal);
        if (missing.isEmpty()) {
            return null;
        }
        Member placed = null;
        Member lacking = null;
        Member occupied = null;
        for (Member member : missing) {
            if (!level.getBlockState(member.pos()).canBeReplaced()) {
                occupied = occupied == null ? member : occupied;
                continue;
            }
            Item item = MinerStructure.blockFor(member.slot().part()).asItem();
            ItemStack stack = find(player.getInventory(), item);
            if (stack.isEmpty()) {
                if (!player.hasInfiniteMaterials()) {
                    lacking = lacking == null ? member : lacking;
                    continue;
                }
                stack = new ItemStack(item);
            }
            BlockHitResult hit = new BlockHitResult(Vec3.atCenterOf(member.pos()), Direction.UP, member.pos(), false);
            if (stack.useOn(new BlockPlaceContext(level, player, InteractionHand.MAIN_HAND, stack, hit)).consumesAction()) {
                placed = member;
                break;
            }
        }
        List<Member> left = missing(level, terminal);
        if (placed != null) {
            return Component.translatable("message.dimensionalfluxminer.assembly.placed", partName(placed), left.size());
        }
        if (lacking != null) {
            return Component.translatable("message.dimensionalfluxminer.assembly.lacking", partName(lacking), left.size());
        }
        Member first = occupied != null ? occupied : missing.getFirst();
        return Component.translatable(occupied != null ? "message.dimensionalfluxminer.assembly.occupied"
                        : "message.dimensionalfluxminer.structure_incomplete",
                partName(first), first.pos().getX(), first.pos().getY(), first.pos().getZ());
    }

    /** Sends the positions still missing to the player's highlight (an empty list clears it). */
    public static void highlight(ServerPlayer player, BlockPos terminal, List<Member> missing) {
        List<MinerAssemblyPayload.Entry> entries = new ArrayList<>(missing.size());
        for (Member member : missing) {
            entries.add(new MinerAssemblyPayload.Entry(member.pos(), member.slot().part().ordinal()));
        }
        if (entries.isEmpty()) {
            SHOWN.remove(player.getUUID());
        } else {
            SHOWN.put(player.getUUID(), terminal.immutable());
        }
        PacketDistributor.sendToPlayer(player, new MinerAssemblyPayload(terminal, entries));
    }

    /** Shows the highlight of {@code terminal}, or hides it when it is the one the player already sees. */
    public static void toggleHighlight(ServerPlayer player, BlockPos terminal) {
        boolean shown = terminal.equals(SHOWN.get(player.getUUID()));
        highlight(player, terminal, shown ? List.of() : missing(player.level(), terminal));
    }

    /** Terminal whose highlight each player sees (server side, to switch it with a click; one entry per player). */
    private static final Map<UUID, BlockPos> SHOWN = new ConcurrentHashMap<>();

    private static Component partName(Member member) {
        return Component.translatable(MinerStructure.blockFor(member.slot().part()).getDescriptionId());
    }

    /** The first stack of {@code item} in the inventory (hands first), or EMPTY. */
    private static ItemStack find(Inventory inventory, Item item) {
        if (inventory.getSelectedItem().is(item)) {
            return inventory.getSelectedItem();
        }
        if (inventory.getItem(Inventory.SLOT_OFFHAND).is(item)) {
            return inventory.getItem(Inventory.SLOT_OFFHAND);
        }
        for (ItemStack stack : inventory.getNonEquipmentItems()) {
            if (stack.is(item)) {
                return stack;
            }
        }
        return ItemStack.EMPTY;
    }
}
