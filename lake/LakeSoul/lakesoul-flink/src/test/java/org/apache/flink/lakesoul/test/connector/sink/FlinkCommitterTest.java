package org.apache.flink.lakesoul.test.connector.sink;

import org.apache.flink.api.common.RuntimeExecutionMode;
import org.apache.flink.api.common.typeinfo.IntegerTypeInfo;
import org.apache.flink.api.java.tuple.Tuple3;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.streaming.runtime.operators.sink.TestSink;
import org.apache.flink.streaming.util.FiniteTestSource;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.Serializable;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.function.BooleanSupplier;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import static java.util.stream.Collectors.joining;
import static org.apache.flink.streaming.runtime.operators.sink.TestSink.END_OF_INPUT_STR;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;


public class FlinkCommitterTest {
    static final List<Integer> SOURCE_DATA =
            Arrays.asList(
                    895, 127, 148, 161, 148, 662, 822, 491, 275, 122, 850, 630, 682, 765, 434, 970,
                    714, 795, 288, 422);

    // source send data two times
    static final int STREAMING_SOURCE_SEND_ELEMENTS_NUM = SOURCE_DATA.size() * 2;

    static final List<String> EXPECTED_COMMITTED_DATA_IN_STREAMING_MODE =
            SOURCE_DATA.stream()
                    // source send data two times
                    .flatMap(
                            x ->
                                    Collections.nCopies(
                                                    2, Tuple3.of(x, null, Long.MIN_VALUE).toString())
                                            .stream())
                    .collect(Collectors.toList());

    static final List<String> EXPECTED_COMMITTED_DATA_IN_BATCH_MODE =
            SOURCE_DATA.stream()
                    .map(x -> Tuple3.of(x, null, Long.MIN_VALUE).toString())
                    .collect(Collectors.toList());

    static final List<String> EXPECTED_GLOBAL_COMMITTED_DATA_IN_STREAMING_MODE =
            SOURCE_DATA.stream()
                    // source send data two times
                    .flatMap(
                            x ->
                                    Collections.nCopies(
                                                    2, Tuple3.of(x, null, Long.MIN_VALUE).toString())
                                            .stream())
                    .collect(Collectors.toList());

    static final List<String> EXPECTED_GLOBAL_COMMITTED_DATA_IN_BATCH_MODE =
            Arrays.asList(
                    SOURCE_DATA.stream()
                            .map(x -> Tuple3.of(x, null, Long.MIN_VALUE).toString())
                            .sorted()
                            .collect(joining("+")),
                    END_OF_INPUT_STR);

    static final Queue<String> COMMIT_QUEUE = new ConcurrentLinkedQueue<>();

    static final Queue<String> GLOBAL_COMMIT_QUEUE = new ConcurrentLinkedQueue<>();

    static final BooleanSupplier COMMIT_QUEUE_RECEIVE_ALL_DATA =
            (BooleanSupplier & Serializable)
                    () -> COMMIT_QUEUE.size() == STREAMING_SOURCE_SEND_ELEMENTS_NUM;

    static final BooleanSupplier GLOBAL_COMMIT_QUEUE_RECEIVE_ALL_DATA =
            (BooleanSupplier & Serializable)
                    () ->
                            getSplittedGlobalCommittedData().size()
                                    == STREAMING_SOURCE_SEND_ELEMENTS_NUM;

    static final BooleanSupplier BOTH_QUEUE_RECEIVE_ALL_DATA =
            (BooleanSupplier & Serializable)
                    () ->
                            COMMIT_QUEUE_RECEIVE_ALL_DATA.getAsBoolean()
                                    && GLOBAL_COMMIT_QUEUE_RECEIVE_ALL_DATA.getAsBoolean();

    @BeforeClass
    public static void setup() {


    }

    @Before
    public void init() {
        COMMIT_QUEUE.clear();
        GLOBAL_COMMIT_QUEUE.clear();
    }

    @Test
    public void writerAndCommitterAndGlobalCommitterExecuteInStreamingMode() throws Exception {
        final StreamExecutionEnvironment env = buildStreamEnv();
        final FiniteTestSource<Integer> source =
                new FiniteTestSource<>(BOTH_QUEUE_RECEIVE_ALL_DATA, SOURCE_DATA);

        env.addSource(source, IntegerTypeInfo.INT_TYPE_INFO)
                .sinkTo(
                        TestSink.newBuilder()
                                .setDefaultCommitter(
                                        (Supplier<Queue<String>> & Serializable) () -> COMMIT_QUEUE)
                                .setGlobalCommitter(
                                        (Supplier<Queue<String>> & Serializable)
                                                () -> GLOBAL_COMMIT_QUEUE)
                                .build());

        env.execute();

        // TODO: At present, for a bounded scenario, the occurrence of final checkpoint is not a
        // deterministic event, so
        // we do not need to verify this matter. After the final checkpoint becomes ready in the
        // future,
        // the verification of "end of input" would be restored.
        GLOBAL_COMMIT_QUEUE.remove(END_OF_INPUT_STR);

        System.out.println("EXPECTED_COMMITTED_DATA_IN_STREAMING_MODE: " + EXPECTED_COMMITTED_DATA_IN_STREAMING_MODE);
        assertThat(
                COMMIT_QUEUE,
                containsInAnyOrder(EXPECTED_COMMITTED_DATA_IN_STREAMING_MODE.toArray()));

        System.out.println("EXPECTED_GLOBAL_COMMITTED_DATA_IN_STREAMING_MODE: " + EXPECTED_GLOBAL_COMMITTED_DATA_IN_STREAMING_MODE);
        assertThat(
                getSplittedGlobalCommittedData(),
                containsInAnyOrder(EXPECTED_GLOBAL_COMMITTED_DATA_IN_STREAMING_MODE.toArray()));
    }

    @Test
    public void writerAndCommitterAndGlobalCommitterExecuteInBatchMode() throws Exception {
        final StreamExecutionEnvironment env = buildBatchEnv();

        env.fromCollection(SOURCE_DATA)
                .sinkTo(
                        TestSink.newBuilder()
                                .setDefaultCommitter(
                                        (Supplier<Queue<String>> & Serializable) () -> COMMIT_QUEUE)
                                .setGlobalCommitter(
                                        (Supplier<Queue<String>> & Serializable)
                                                () -> GLOBAL_COMMIT_QUEUE)
                                .build());

        env.execute();

        System.out.println("EXPECTED_COMMITTED_DATA_IN_BATCH_MODE: " + EXPECTED_COMMITTED_DATA_IN_BATCH_MODE);
        assertThat(
                COMMIT_QUEUE, containsInAnyOrder(EXPECTED_COMMITTED_DATA_IN_BATCH_MODE.toArray()));

        System.out.println("EXPECTED_GLOBAL_COMMITTED_DATA_IN_BATCH_MODE: " + EXPECTED_GLOBAL_COMMITTED_DATA_IN_BATCH_MODE);
        assertThat(
                GLOBAL_COMMIT_QUEUE,
                containsInAnyOrder(EXPECTED_GLOBAL_COMMITTED_DATA_IN_BATCH_MODE.toArray()));
    }

    @Test
    public void writerAndCommitterExecuteInStreamingMode() throws Exception {
        final StreamExecutionEnvironment env = buildStreamEnv();
        final FiniteTestSource<Integer> source =
                new FiniteTestSource<>(COMMIT_QUEUE_RECEIVE_ALL_DATA, SOURCE_DATA);

        env.addSource(source, IntegerTypeInfo.INT_TYPE_INFO)
                .sinkTo(
                        TestSink.newBuilder()
                                .setDefaultCommitter(
                                        (Supplier<Queue<String>> & Serializable) () -> COMMIT_QUEUE)
                                .build());
        env.execute();

        System.out.println("EXPECTED_COMMITTED_DATA_IN_STREAMING_MODE: " + EXPECTED_COMMITTED_DATA_IN_STREAMING_MODE);
        assertThat(
                COMMIT_QUEUE,
                containsInAnyOrder(EXPECTED_COMMITTED_DATA_IN_STREAMING_MODE.toArray()));
    }

    @Test
    public void writerAndCommitterExecuteInBatchMode() throws Exception {
        final StreamExecutionEnvironment env = buildBatchEnv();

        env.fromCollection(SOURCE_DATA)
                .sinkTo(
                        TestSink.newBuilder()
                                .setDefaultCommitter(
                                        (Supplier<Queue<String>> & Serializable) () -> COMMIT_QUEUE)
                                .build());
        env.execute();
        System.out.println("EXPECTED_COMMITTED_DATA_IN_BATCH_MODE: " + EXPECTED_COMMITTED_DATA_IN_BATCH_MODE);
        assertThat(
                COMMIT_QUEUE, containsInAnyOrder(EXPECTED_COMMITTED_DATA_IN_BATCH_MODE.toArray()));
    }

    @Test
    public void writerAndGlobalCommitterExecuteInStreamingMode() throws Exception {
        final StreamExecutionEnvironment env = buildStreamEnv();
        final FiniteTestSource<Integer> source =
                new FiniteTestSource<>(GLOBAL_COMMIT_QUEUE_RECEIVE_ALL_DATA, SOURCE_DATA);

        env.addSource(source, IntegerTypeInfo.INT_TYPE_INFO)
                .sinkTo(
                        TestSink.newBuilder()
                                .setCommittableSerializer(
                                        TestSink.StringCommittableSerializer.INSTANCE)
                                .setGlobalCommitter(
                                        (Supplier<Queue<String>> & Serializable)
                                                () -> GLOBAL_COMMIT_QUEUE)
                                .build());

        env.execute();

        // TODO: At present, for a bounded scenario, the occurrence of final checkpoint is not a
        // deterministic event, so
        // we do not need to verify this matter. After the final checkpoint becomes ready in the
        // future,
        // the verification of "end of input" would be restored.
        GLOBAL_COMMIT_QUEUE.remove(END_OF_INPUT_STR);

        System.out.println("EXPECTED_GLOBAL_COMMITTED_DATA_IN_STREAMING_MODE: " + EXPECTED_GLOBAL_COMMITTED_DATA_IN_STREAMING_MODE);
        assertThat(
                getSplittedGlobalCommittedData(),
                containsInAnyOrder(EXPECTED_GLOBAL_COMMITTED_DATA_IN_STREAMING_MODE.toArray()));
    }

    @Test
    public void writerAndGlobalCommitterExecuteInBatchMode() throws Exception {
        final StreamExecutionEnvironment env = buildBatchEnv();

        env.fromCollection(SOURCE_DATA)
                .sinkTo(
                        TestSink.newBuilder()
                                .setCommittableSerializer(
                                        TestSink.StringCommittableSerializer.INSTANCE)
                                .setGlobalCommitter(
                                        (Supplier<Queue<String>> & Serializable)
                                                () -> GLOBAL_COMMIT_QUEUE)
                                .build());
        env.execute();

        System.out.println("EXPECTED_GLOBAL_COMMITTED_DATA_IN_BATCH_MODE: " + EXPECTED_GLOBAL_COMMITTED_DATA_IN_BATCH_MODE);
        assertThat(
                GLOBAL_COMMIT_QUEUE,
                containsInAnyOrder(EXPECTED_GLOBAL_COMMITTED_DATA_IN_BATCH_MODE.toArray()));
    }

    private static List<String> getSplittedGlobalCommittedData() {
        return GLOBAL_COMMIT_QUEUE.stream()
                .flatMap(x -> Arrays.stream(x.split("\\+")))
                .collect(Collectors.toList());
    }

    private StreamExecutionEnvironment buildStreamEnv() {
        final StreamExecutionEnvironment env = StreamExecutionEnvironment.getExecutionEnvironment();
        env.setRuntimeMode(RuntimeExecutionMode.STREAMING);
        env.enableCheckpointing(100);
        return env;
    }

    private StreamExecutionEnvironment buildBatchEnv() {
        final StreamExecutionEnvironment env = StreamExecutionEnvironment.getExecutionEnvironment();
        env.setRuntimeMode(RuntimeExecutionMode.BATCH);
        return env;
    }
}
