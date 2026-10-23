package dimensionalfluxminer.gametest;

import dimensionalfluxminer.DimensionalFluxMiner;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Consumer;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.FunctionGameTestInstance;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.gametest.framework.TestData;
import net.minecraft.gametest.framework.TestEnvironmentDefinition;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.storage.LevelData;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.RegisterGameTestsEvent;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * Registers every {@link GameTest} method of the test classes: a test function each, and a test instance that runs it in
 * its structure. Each batch gets a test environment of its own, so its tests run apart from the others.
 */
public final class DfmGameTests {
    private static final List<Class<?>> CLASSES = List.of(BenchmarkGameTests.class, ContentGameTests.class, CrusherGameTests.class,
            ElectricFurnaceGameTests.class, EnergyGameTests.class, MatterGameTests.class, MinerGameTests.class,
            OptimizationGameTests.class, ProgressionGameTests.class, ProtectionGameTests.class, TankGameTests.class,
            ThroughputGameTests.class);
    private static final DeferredRegister<Consumer<GameTestHelper>> FUNCTIONS =
            DeferredRegister.create(Registries.TEST_FUNCTION, DimensionalFluxMiner.MOD_ID);
    private static final DeferredRegister<MapCodec<? extends TestEnvironmentDefinition<?>>> ENVIRONMENT_TYPES =
            DeferredRegister.create(Registries.TEST_ENVIRONMENT_DEFINITION_TYPE, DimensionalFluxMiner.MOD_ID);

    static {
        ENVIRONMENT_TYPES.register("spawn_away", () -> SpawnAway.CODEC);
    }

    private DfmGameTests() {
    }

    public static void register(IEventBus modBus) {
        for (Class<?> type : CLASSES) {
            for (Method method : type.getDeclaredMethods()) {
                if (method.isAnnotationPresent(GameTest.class)) {
                    FUNCTIONS.register(id(method).getPath(), () -> helper -> invoke(method, helper));
                }
            }
        }
        FUNCTIONS.register(modBus);
        ENVIRONMENT_TYPES.register(modBus);
        modBus.addListener(DfmGameTests::onRegisterGameTests);
    }

    private static void onRegisterGameTests(RegisterGameTestsEvent event) {
        Map<String, Holder<TestEnvironmentDefinition<?>>> environments = new HashMap<>();
        for (Class<?> type : CLASSES) {
            for (Method method : type.getDeclaredMethods()) {
                GameTest test = method.getAnnotation(GameTest.class);
                if (test == null) {
                    continue;
                }
                String batch = test.batch().isEmpty() ? "default" : test.batch();
                Holder<TestEnvironmentDefinition<?>> environment = environments.computeIfAbsent(batch,
                        name -> event.registerEnvironment(DimensionalFluxMiner.id("batch/" + name), new SpawnAway(name)));
                Identifier id = id(method);
                TestData<Holder<TestEnvironmentDefinition<?>>> data = new TestData<>(environment, Level.OVERWORLD,
                        DimensionalFluxMiner.id(test.template()), test.timeoutTicks(), test.setupTicks(), test.required(),
                        Rotation.NONE, false, 1, 1, test.skyAccess(), 0);
                event.registerTest(id, new FunctionGameTestInstance(ResourceKey.create(Registries.TEST_FUNCTION, id), data));
            }
        }
    }

    /**
     * The test server puts the world spawn where the tests are built, so the first tests of a batch would sit inside the
     * spawn protection that the corruption respects. During a batch the spawn moves far away (and comes back after).
     */
    private record SpawnAway(String batch) implements TestEnvironmentDefinition<LevelData.RespawnData> {
        static final MapCodec<SpawnAway> CODEC = com.mojang.serialization.Codec.STRING.fieldOf("batch").xmap(SpawnAway::new, SpawnAway::batch);
        private static final int DISTANCE = 100_000;

        @Override
        public LevelData.RespawnData setup(ServerLevel level) {
            LevelData.RespawnData original = level.getRespawnData();
            BlockPos pos = original.pos();
            level.setRespawnData(LevelData.RespawnData.of(original.globalPos().dimension(),
                    pos.offset(pos.getX() > 0 ? -DISTANCE : DISTANCE, 0, 0), original.yaw(), original.pitch()));
            return original;
        }

        @Override
        public void teardown(ServerLevel level, LevelData.RespawnData original) {
            level.setRespawnData(original);
        }

        @Override
        public MapCodec<SpawnAway> codec() {
            return CODEC;
        }
    }

    private static Identifier id(Method method) {
        if (!Modifier.isStatic(method.getModifiers())) {
            throw new IllegalStateException("GameTest " + method + " must be static");
        }
        return DimensionalFluxMiner.id(snake(method.getDeclaringClass().getSimpleName()) + "/" + snake(method.getName()));
    }

    private static String snake(String name) {
        return name.replaceAll("([a-z0-9])([A-Z])", "$1_$2").toLowerCase(Locale.ROOT);
    }

    private static void invoke(Method method, GameTestHelper helper) {
        try {
            method.invoke(null, helper);
        } catch (InvocationTargetException e) {
            if (e.getCause() instanceof RuntimeException runtime) {
                throw runtime;
            }
            if (e.getCause() instanceof Error error) {
                throw error;
            }
            throw new IllegalStateException(e.getCause());
        } catch (IllegalAccessException e) {
            throw new IllegalStateException(e);
        }
    }
}
