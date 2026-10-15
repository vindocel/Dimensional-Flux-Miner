package dimensionalfluxminer.miner.profile;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.LinkedHashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;

class ProfileWeightsTest {
    @Test
    void exponentGivesRareFamiliesAModerateBoost() {
        // Vanilla Overworld raw scores (iron, coal, diamond, emerald): rare families get a larger share than linear,
        // but less than a square root would give.
        Map<String, Double> raw = new LinkedHashMap<>();
        raw.put("iron", 940.0);
        raw.put("coal", 850.0);
        raw.put("diamond", 77.3333);
        raw.put("emerald", 56.6038);
        Map<String, Integer> weights = ProfileWeights.normalize(raw);
        int total = weights.values().stream().mapToInt(Integer::intValue).sum();
        assertTrue(Math.abs(total - ProfileWeights.TARGET_TOTAL) <= raw.size());
        double linearDiamond = 77.3333 / (940 + 850 + 77.3333 + 56.6038);
        double diamond = weights.get("diamond") / (double) total;
        assertTrue(diamond > linearDiamond && diamond < 2 * linearDiamond, "diamond " + diamond + " vs linear " + linearDiamond);
        assertEquals(Math.pow(940.0 / 850.0, 0.75), weights.get("iron") / (double) weights.get("coal"), 0.01);
    }

    @Test
    void everyFamilyKeepsAtLeastOneWeight() {
        Map<String, Double> raw = new LinkedHashMap<>();
        raw.put("common", 1.0e9);
        raw.put("tiny", 0.0);
        Map<String, Integer> weights = ProfileWeights.normalize(raw);
        assertEquals(1, weights.get("tiny"));
    }

    @Test
    void theVoidLeavesTheFamiliesTheirProportionsInTheRest() {
        Map<String, Integer> weights = new LinkedHashMap<>();
        weights.put("platinum", 7_500);
        weights.put("other", 2_500);
        Map<String, Integer> out = ProfileWeights.scaleForEmpty(weights, 0.97);
        assertEquals(9_700, ProfileWeights.emptyWeight(0.97));
        assertEquals(225, out.get("platinum"));
        assertEquals(75, out.get("other"));
        assertEquals(10_000, out.get("platinum") + out.get("other") + ProfileWeights.emptyWeight(0.97));
    }

    @Test
    void datapackSharesReserveTheirPartOfTheProfile() {
        Map<String, Integer> weights = new LinkedHashMap<>();
        weights.put("iron", 6_000);
        weights.put("coal", 3_000);
        weights.put("diamond", 1_000);
        Map<String, Integer> out = ProfileWeights.applyShares(weights, Map.of("diamond", 0.25));
        assertEquals(2_500, out.get("diamond"));
        assertEquals(5_000, out.get("iron"));
        assertEquals(2_500, out.get("coal"));
        assertEquals(weights, ProfileWeights.applyShares(weights, Map.of()), "no shares, no change");
    }
}
