package dimensionalfluxminer.compat;

import dimensionalfluxminer.DimensionalFluxMiner;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.AABB;
import net.neoforged.fml.ModList;

/**
 * Optional support for Logistics Networks, read by reflection (no compile dependency). Its nodes are entities attached
 * to a block; each node has channels that send (pull from the block) or receive (push into the block) items, fluids or
 * energy. The Miner reads the channels of the nodes on its port blocks to set the port roles itself.
 */
public final class LogisticsNetworksCompat {
    public static final String MOD_ID = "logisticsnetworks";
    private static final String NODE_CLASS = "me.almana.logisticsnetworks.entity.LogisticsNodeEntity";

    /**
     * What one node does with the block it is attached to.
     *
     * @param takesItems   an enabled item channel that sends (the block is a source: Item Output)
     * @param givesItems   an enabled item channel that receives (the block is a destination: Matter Input)
     * @param givesEnergy  an enabled energy channel that receives (energy goes into the block)
     * @param color        the node's network color (0xRRGGBB)
     */
    public record Node(Entity entity, boolean takesItems, boolean givesItems, boolean givesEnergy, int color, List<Channel> channels) {
    }

    /**
     * One enabled channel: what it moves ({@code ITEM}, {@code FLUID}, {@code ENERGY}...), whether it sends (pulls
     * from the block, {@code EXPORT}) or receives (pushes into it, {@code IMPORT}), and the side of the block it uses.
     */
    public record Channel(String type, boolean sends, net.minecraft.core.Direction side) {
    }

    private static Boolean loaded;
    private static boolean broken;
    private static Method attachedPos;
    private static Method channels;
    private static Method enabled;
    private static Method mode;
    private static Method type;
    private static Method ioDirection;
    private static Method networkColor;
    private static Method renderVisible;
    private static Method setRenderVisible;

    private LogisticsNetworksCompat() {
    }

    public static boolean loaded() {
        if (loaded == null) {
            loaded = ModList.get().isLoaded(MOD_ID);
        }
        return loaded && !broken;
    }

    /** The nodes attached to {@code pos} (empty without the mod). */
    public static List<Node> nodesAt(ServerLevel level, BlockPos pos) {
        List<Node> nodes = new ArrayList<>();
        if (!loaded()) {
            return nodes;
        }
        try {
            for (Entity entity : level.getEntities((Entity) null, new AABB(pos).inflate(0.25),
                    e -> e.getClass().getName().equals(NODE_CLASS))) {
                bind(entity.getClass());
                if (!pos.equals(attachedPos.invoke(entity))) {
                    continue;
                }
                boolean takes = false;
                boolean gives = false;
                boolean energy = false;
                List<Channel> list = new ArrayList<>();
                for (Object channel : (Object[]) channels.invoke(entity)) {
                    if (channel == null || !(boolean) enabled.invoke(channel)) {
                        continue;
                    }
                    String m = ((Enum<?>) mode.invoke(channel)).name();
                    String t = ((Enum<?>) type.invoke(channel)).name();
                    list.add(new Channel(t, "EXPORT".equals(m), (net.minecraft.core.Direction) ioDirection.invoke(channel)));
                    if ("ITEM".equals(t)) {
                        takes |= "EXPORT".equals(m);
                        gives |= "IMPORT".equals(m);
                    } else if ("ENERGY".equals(t)) {
                        energy |= "IMPORT".equals(m);
                    }
                }
                nodes.add(new Node(entity, takes, gives, energy, (int) networkColor.invoke(entity) & 0xFFFFFF, List.copyOf(list)));
            }
        } catch (ReflectiveOperationException | RuntimeException e) {
            broken = true;
            DimensionalFluxMiner.LOGGER.warn("Logistics Networks support turned off: its nodes could not be read", e);
            nodes.clear();
        }
        return nodes;
    }

    /** Hides the node's own box (the Miner draws a frame around the port instead); the wrench still shows it. */
    public static void hide(Node node) {
        try {
            if ((boolean) renderVisible.invoke(node.entity())) {
                setRenderVisible.invoke(node.entity(), false);
            }
        } catch (ReflectiveOperationException | RuntimeException e) {
            DimensionalFluxMiner.LOGGER.debug("Could not hide a Logistics Networks node", e);
        }
    }

    private static void bind(Class<?> node) throws ReflectiveOperationException {
        if (attachedPos != null) {
            return;
        }
        attachedPos = node.getMethod("getAttachedPos");
        channels = node.getMethod("getChannels");
        networkColor = node.getMethod("getNetworkColor");
        renderVisible = node.getMethod("isRenderVisible");
        setRenderVisible = node.getMethod("setRenderVisible", boolean.class);
        Class<?> channel = channels.getReturnType().getComponentType();
        enabled = channel.getMethod("isEnabled");
        mode = channel.getMethod("getMode");
        type = channel.getMethod("getType");
        ioDirection = channel.getMethod("getIoDirection");
    }
}
