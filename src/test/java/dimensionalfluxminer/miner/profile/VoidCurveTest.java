package dimensionalfluxminer.miner.profile;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class VoidCurveTest {
    @Test
    void geometricVoidFromOneToFiveFamilies() {
        // 97 % void with one family, none from five: the chance of finding something grows by the same factor each time.
        double[] expected = {0.97, 0.9279157, 0.8267949, 0.5838209, 0.0, 0.0};
        for (int families = 1; families <= 6; families++) {
            assertEquals(expected[families - 1], VoidCurve.GEOMETRIC.voidShare(0.97, 5, families), 1e-6, families + " families");
        }
        double ratio = (1 - VoidCurve.GEOMETRIC.voidShare(0.97, 5, 2)) / (1 - VoidCurve.GEOMETRIC.voidShare(0.97, 5, 1));
        assertEquals(ratio, (1 - VoidCurve.GEOMETRIC.voidShare(0.97, 5, 3)) / (1 - VoidCurve.GEOMETRIC.voidShare(0.97, 5, 2)), 1e-9);
    }

    @Test
    void linearAndStepCurves() {
        assertEquals(0.7275, VoidCurve.LINEAR.voidShare(0.97, 5, 2), 1e-9);
        assertEquals(0.2425, VoidCurve.LINEAR.voidShare(0.97, 5, 4), 1e-9);
        assertEquals(0.97, VoidCurve.STEP.voidShare(0.97, 5, 4), 1e-9);
        assertEquals(0.0, VoidCurve.STEP.voidShare(0.97, 5, 5), 1e-9);
    }

    @Test
    void noFamiliesOrNoShareMeansNoVoid() {
        for (VoidCurve curve : VoidCurve.values()) {
            assertEquals(0.0, curve.voidShare(0.97, 5, 0), curve + ": an empty profile has no void");
            assertEquals(0.0, curve.voidShare(0.0, 5, 1), curve + ": no share, no void");
        }
        assertEquals(VoidCurve.GEOMETRIC, VoidCurve.byName("geometric"));
    }
}
