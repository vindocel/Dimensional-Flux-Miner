package dimensionalfluxminer.miner;

import dimensionalfluxminer.content.DfmStateProperties;
import dimensionalfluxminer.miner.MinerLayout.Front;
import dimensionalfluxminer.miner.MinerLayout.Part;
import dimensionalfluxminer.miner.MinerLayout.PortRole;
import dimensionalfluxminer.miner.MinerLayout.Slot;
import dimensionalfluxminer.registry.DfmBlocks;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;

/** A concrete, world-positioned Dimensional Flux Miner assembly. */
public record MinerStructure(BlockPos terminal, Direction front, BlockPos core, List<Member> members) {
    /** One of the 27 participants. {@code outward} is set for ports only. */
    public record Member(BlockPos pos, Slot slot, Direction outward) {
    }

    /** Result of a validation attempt, with the first mismatch for player feedback. */
    public record Check(Optional<MinerStructure> structure, BlockPos mismatch, Part expected) {
    }

    public static Front toFront(Direction facing) {
        return switch (facing) {
            case WEST -> Front.WEST;
            case NORTH -> Front.NORTH;
            case EAST -> Front.EAST;
            default -> Front.SOUTH;
        };
    }

    public static Block blockFor(Part part) {
        return switch (part) {
            case STEEL_CHASSIS -> DfmBlocks.STEEL_CHASSIS.get();
            case VENTILATION_UNIT -> DfmBlocks.VENTILATION_UNIT.get();
            case REINFORCED_STRUCTURAL_GLASS -> DfmBlocks.REINFORCED_STRUCTURAL_GLASS.get();
            case DIMENSIONAL_PORT -> DfmBlocks.DIMENSIONAL_PORT.get();
            case QUANTUM_CORE -> DfmBlocks.QUANTUM_CORE.get();
            case MINER_TERMINAL -> DfmBlocks.MINER_TERMINAL.get();
        };
    }

    /** Builds the expected assembly around a terminal, without looking at the world. */
    public static MinerStructure layout(BlockPos terminal, Direction facing) {
        Front front = toFront(facing);
        int[] c = MinerLayout.coreFromTerminal(front);
        BlockPos core = terminal.offset(c[0], c[1], c[2]);
        List<Member> members = new ArrayList<>(27);
        for (Slot slot : MinerLayout.BASE_SLOTS) {
            int[] o = MinerLayout.offsetFromCore(slot, front);
            Direction outward = null;
            if (slot.role() != null) {
                int[] out = MinerLayout.rotate(slot.outX(), slot.outZ(), front);
                outward = Direction.fromDelta(out[0], 0, out[1]);
            }
            members.add(new Member(core.offset(o[0], o[1], o[2]), slot, outward));
        }
        return new MinerStructure(terminal, facing, core, List.copyOf(members));
    }

    /**
     * Validates the 27 positions: block ids by rotated slot, the terminal facing outward, and no participant
     * already belonging to another formed machine.
     */
    public static Check check(Level level, BlockPos terminal) {
        BlockState terminalState = level.getBlockState(terminal);
        if (!terminalState.is(DfmBlocks.MINER_TERMINAL.get())) {
            return new Check(Optional.empty(), terminal, Part.MINER_TERMINAL);
        }
        MinerStructure structure = layout(terminal, terminalState.getValue(BlockStateProperties.HORIZONTAL_FACING));
        for (Member member : structure.members) {
            if (!level.isLoaded(member.pos)) {
                return new Check(Optional.empty(), member.pos, member.slot.part());
            }
            BlockState state = level.getBlockState(member.pos);
            if (!state.is(blockFor(member.slot.part()))) {
                return new Check(Optional.empty(), member.pos, member.slot.part());
            }
        }
        return new Check(Optional.of(structure), null, null);
    }

    public boolean anyMemberFormed(Level level) {
        for (Member member : members) {
            BlockState state = level.getBlockState(member.pos);
            if (state.hasProperty(DfmStateProperties.FORMED) && state.getValue(DfmStateProperties.FORMED)) {
                return true;
            }
        }
        return false;
    }

    public boolean contains(BlockPos pos) {
        for (Member member : members) {
            if (member.pos.equals(pos)) {
                return true;
            }
        }
        return false;
    }

    public Optional<Member> member(BlockPos pos) {
        for (Member member : members) {
            if (member.pos.equals(pos)) {
                return Optional.of(member);
            }
        }
        return Optional.empty();
    }

    public Optional<Member> port(PortRole role) {
        for (Member member : members) {
            if (member.slot.role() == role) {
                return Optional.of(member);
            }
        }
        return Optional.empty();
    }

    /** Sets the FORMED flag on every participant that is still the expected block. */
    public void setFormed(Level level, boolean formed) {
        for (Member member : members) {
            BlockState state = level.getBlockState(member.pos);
            if (state.is(blockFor(member.slot.part())) && state.hasProperty(DfmStateProperties.FORMED)
                    && state.getValue(DfmStateProperties.FORMED) != formed) {
                level.setBlock(member.pos, state.setValue(DfmStateProperties.FORMED, formed), Block.UPDATE_ALL);
            }
        }
    }
}
