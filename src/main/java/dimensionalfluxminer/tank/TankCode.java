package dimensionalfluxminer.tank;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;
import net.minecraft.core.UUIDUtil;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.DyeColor;

/**
 * Dimensional address of a tank: three of the 16 dye colors, read from left to right on the front panel. The order
 * matters (red, blue, white is not blue, red, white). Colors are kept by their stable names, never by an RGB value.
 * A personal code also has an owner: it is a channel of its own for that player, apart from the public one with the
 * same colors.
 */
public record TankCode(DyeColor first, DyeColor second, DyeColor third, Optional<UUID> owner, Optional<String> ownerName) {
    public static final int POSITIONS = 3;
    /** Code of a newly placed tank: public, white, white, white. */
    public static final TankCode DEFAULT = new TankCode(DyeColor.WHITE, DyeColor.WHITE, DyeColor.WHITE);

    public static final Codec<TankCode> CODEC = RecordCodecBuilder.create(i -> i.group(
            DyeColor.CODEC.fieldOf("first").forGetter(TankCode::first),
            DyeColor.CODEC.fieldOf("second").forGetter(TankCode::second),
            DyeColor.CODEC.fieldOf("third").forGetter(TankCode::third),
            UUIDUtil.CODEC.optionalFieldOf("owner").forGetter(TankCode::owner),
            Codec.STRING.optionalFieldOf("owner_name").forGetter(TankCode::ownerName)
    ).apply(i, TankCode::new));
    public static final StreamCodec<ByteBuf, TankCode> STREAM_CODEC = StreamCodec.composite(
            DyeColor.STREAM_CODEC, TankCode::first,
            DyeColor.STREAM_CODEC, TankCode::second,
            DyeColor.STREAM_CODEC, TankCode::third,
            ByteBufCodecs.optional(UUIDUtil.STREAM_CODEC), TankCode::owner,
            ByteBufCodecs.optional(ByteBufCodecs.STRING_UTF8), TankCode::ownerName,
            TankCode::new);

    public TankCode(DyeColor first, DyeColor second, DyeColor third) {
        this(first, second, third, Optional.empty(), Optional.empty());
    }

    public DyeColor get(int position) {
        return switch (position) {
            case 0 -> first;
            case 1 -> second;
            default -> third;
        };
    }

    public TankCode with(int position, DyeColor color) {
        return switch (position) {
            case 0 -> new TankCode(color, second, third, owner, ownerName);
            case 1 -> new TankCode(first, color, third, owner, ownerName);
            default -> new TankCode(first, second, color, owner, ownerName);
        };
    }

    public boolean personal() {
        return owner.isPresent();
    }

    public TankCode withOwner(Player player) {
        return new TankCode(first, second, third, Optional.of(player.getUUID()), Optional.of(player.getGameProfile().getName()));
    }

    public TankCode withoutOwner() {
        return new TankCode(first, second, third);
    }

    /**
     * Channel key: the tank kind is part of it, so a single and a dual tank never share a channel; a personal code adds
     * its owner (the name shown is not part of it).
     */
    public String key(TankKind kind) {
        String colors = kind.id() + "/" + first.getSerializedName() + "/" + second.getSerializedName() + "/" + third.getSerializedName();
        return owner.map(uuid -> colors + "/" + uuid).orElse(colors);
    }

    /** Same channel: same colors and same owner. */
    public boolean sameChannel(TankCode other) {
        return first == other.first && second == other.second && third == other.third && owner.equals(other.owner);
    }

    public CompoundTag save() {
        CompoundTag tag = new CompoundTag();
        tag.putString("first", first.getSerializedName());
        tag.putString("second", second.getSerializedName());
        tag.putString("third", third.getSerializedName());
        owner.ifPresent(uuid -> tag.putUUID("owner", uuid));
        ownerName.ifPresent(name -> tag.putString("owner_name", name));
        return tag;
    }

    /** Missing or unknown colors fall back to white; no owner = public. */
    public static TankCode load(CompoundTag tag) {
        Optional<UUID> owner = tag.hasUUID("owner") ? Optional.of(tag.getUUID("owner")) : Optional.empty();
        Optional<String> name = owner.isPresent() && tag.contains("owner_name") ? Optional.of(tag.getString("owner_name")) : Optional.empty();
        return new TankCode(color(tag.getString("first")), color(tag.getString("second")), color(tag.getString("third")), owner, name);
    }

    private static DyeColor color(String name) {
        return DyeColor.byName(name.toLowerCase(Locale.ROOT), DyeColor.WHITE);
    }
}
