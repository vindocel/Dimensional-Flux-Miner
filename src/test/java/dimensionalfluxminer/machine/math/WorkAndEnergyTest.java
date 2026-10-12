package dimensionalfluxminer.machine.math;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class WorkAndEnergyTest {
    @Test
    void fractionalRateAccumulatesWithoutLoss() {
        WorkAccumulator acc = new WorkAccumulator();
        long done = 0;
        for (int tick = 0; tick < 200; tick++) {
            WorkAccumulator.Plan plan = acc.plan(0.005, Long.MAX_VALUE);
            acc.commit(plan);
            done += plan.operations();
        }
        assertEquals(1, done);
    }

    @Test
    void bulkRateRunsSeveralOperationsPerTick() {
        WorkAccumulator acc = new WorkAccumulator();
        WorkAccumulator.Plan plan = acc.plan(64.0, Long.MAX_VALUE);
        assertEquals(64, plan.operations());
        assertEquals(64.0, plan.workDone(), 1e-9);
        assertFalse(plan.limited());
    }

    @Test
    void bottleneckOnlyChargesPossibleWork() {
        WorkAccumulator acc = new WorkAccumulator();
        WorkAccumulator.Plan plan = acc.plan(64.0, 10);
        assertEquals(10, plan.operations());
        assertTrue(plan.limited());
        assertTrue(plan.workDone() < 11.0);
        acc.commit(plan);
        WorkAccumulator.Plan idle = acc.plan(64.0, 0);
        assertEquals(0, idle.operations());
        assertEquals(0.0, idle.workDone());
    }

    @Test
    void loweringTargetKeepsStoredEnergy() {
        DynamicEnergyBuffer buffer = new DynamicEnergyBuffer(15_000_000);
        assertEquals(15_000_000, buffer.receive(20_000_000, false));
        buffer.setTarget(5_000);
        assertEquals(15_000_000, buffer.stored());
        assertEquals(0, buffer.receive(100, false));
        buffer.consume(14_999_000);
        assertEquals(1_000, buffer.stored());
        assertEquals(4_000, buffer.receive(10_000, false));
        assertEquals(5_000, buffer.capacity());
    }

    @Test
    void headroomFeedsAcceleratedCyclesWithoutChangingTheTarget() {
        // Rate 10 of the Miner: 1.5M FE per cycle, target 15M; 20 extra cycles per game time.
        DynamicEnergyBuffer buffer = new DynamicEnergyBuffer(15_000_000);
        buffer.setHeadroom(20 * 1_500_000L);
        assertEquals(45_000_000, buffer.receive(Integer.MAX_VALUE, false), "one refill covers buffer + accelerated cycles");
        assertEquals(15_000_000, buffer.target(), "the buffer itself stays 10 x FE/t");
        for (int cycle = 0; cycle < 21; cycle++) {
            assertEquals(1_500_000, buffer.consume(1_500_000), "cycle " + cycle + " fully powered");
        }
        buffer.setHeadroom(0);
        assertEquals(1_500_000, buffer.receive(Integer.MAX_VALUE, false), "no acceleration: refilled up to the target");
        assertEquals(15_000_000, buffer.stored());
        assertEquals(0, buffer.receive(Integer.MAX_VALUE, false), "and never above it");
    }

    @Test
    void accelerationDemandFollowsTheExtraCyclesOfEachGameTime() {
        dimensionalfluxminer.machine.AccelerationDemand demand = new dimensionalfluxminer.machine.AccelerationDemand();
        assertEquals(0, demand.extraPerTick(100));
        for (int i = 0; i < 6; i++) {
            demand.recordExtra(100);
        }
        assertEquals(6, demand.extraPerTick(100));
        assertEquals(6, demand.extraPerTick(101), "next game time keeps the last rate until it is exceeded");
        demand.recordExtra(101);
        assertEquals(6, demand.extraPerTick(101));
        assertEquals(1, demand.extraPerTick(102));
        assertEquals(0, demand.extraPerTick(103), "acceleration removed: back to zero");
        demand.recordExtra(110);
        assertEquals(1, demand.extraPerTick(110));
        assertEquals(0, demand.extraPerTick(112), "a gap of game times resets it");
    }
}
