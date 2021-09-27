package window;

import bin.UserBehavior;
import org.apache.flink.api.common.eventtime.SerializableTimestampAssigner;
import org.apache.flink.api.common.eventtime.WatermarkStrategy;
import org.apache.flink.api.common.functions.MapFunction;
import org.apache.flink.api.common.state.ValueState;
import org.apache.flink.api.common.state.ValueStateDescriptor;
import org.apache.flink.api.common.typeinfo.Types;
import org.apache.flink.api.java.functions.KeySelector;
import org.apache.flink.api.java.tuple.Tuple2;
import org.apache.flink.configuration.Configuration;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.streaming.api.functions.windowing.ProcessWindowFunction;
import org.apache.flink.streaming.api.windowing.assigners.TumblingEventTimeWindows;
import org.apache.flink.streaming.api.windowing.time.Time;
import org.apache.flink.streaming.api.windowing.windows.TimeWindow;
import org.apache.flink.util.Collector;

import java.time.Duration;

public class processWindowFunctionPv {
    public static void main(String[] args) throws Exception {
        StreamExecutionEnvironment env = StreamExecutionEnvironment.getExecutionEnvironment();

        // 创建WatermarkStrategy
        WatermarkStrategy<UserBehavior> wms = WatermarkStrategy
                .<UserBehavior>forBoundedOutOfOrderness(Duration.ofSeconds(5))
                .withTimestampAssigner(new SerializableTimestampAssigner<UserBehavior>() {
                    @Override
                    public long extractTimestamp(UserBehavior element, long recordTimestamp) {
                        return element.getTimestamp() * 1000L;
                    }
                });

        env
                .readTextFile("input/UserBehavior.csv")
                .map(line -> { // 对数据切割, 然后封装到POJO中
                    String[] split = line.split(",");
                    return new UserBehavior(Long.valueOf(split[0]), Long.valueOf(split[1]), Integer.valueOf(split[2]), split[3], Long.valueOf(split[4]));
                })
                .filter(behavior -> "pv".equals(behavior.getBehavior())) //过滤出pv行为
                .assignTimestampsAndWatermarks(wms)  // 添加 Watermark
                .map(behavior -> Tuple2.of("pv-" + (int) (1 + Math.random() * 10), 1L))
                .returns(Types.TUPLE(Types.STRING, Types.LONG)) // 使用Tuple类型, 方便后面求和
                .keyBy(value -> value.f0)  // keyBy: 按照key分组
                .window(TumblingEventTimeWindows.of(Time.minutes(60)))  // 分配窗口
                .sum(1) // 求和

                // 降低维度
                .map(new MapFunction<Tuple2<String, Long>, Tuple2<String, Long>>() {
                    @Override
                    public Tuple2<String, Long> map(Tuple2<String, Long> tuple2) throws Exception {
                        return new Tuple2<>(tuple2.f0.split("-")[0], tuple2.f1);
                    }
                })
                .keyBy(t2 -> t2.f0)
                .window(TumblingEventTimeWindows.of(Time.minutes(60)))  // 分配窗口
                .sum(1)

//                // 降低维度
//                .keyBy(new KeySelector<Tuple2<String, Long>, String>() {
//                    @Override
//                    public String getKey(Tuple2<String, Long> tuple2) throws Exception {
//                        return tuple2.f0.split("-")[0];
//                    }
//                })
//                .window(TumblingEventTimeWindows.of(Time.minutes(60)))  // 分配窗
//                .process(new ProcessWindowFunction<Tuple2<String, Long>, Tuple2<String, Long>, String, TimeWindow>() {
//                    private ValueState<Long> pv_value;
//
//                    @Override
//                    public void open(Configuration parameters) throws Exception {
//                        pv_value = getRuntimeContext().getState(new ValueStateDescriptor<Long>("pv_value", Long.class));
//                    }
//
//                    @Override
//                    public void process(String s, Context context, Iterable<Tuple2<String, Long>> iterable, Collector<Tuple2<String, Long>> collector) throws Exception {
//                        Long value = pv_value.value();
//                        if (value == null) {
//                            value = 0L;
//                        }
//                        for (Tuple2<String, Long> t : iterable) {
//                            value += t.f1;
//                        }
//
//                        collector.collect(new Tuple2<>(s, value));
//                    }
//                })
                .print();
        env.execute();
    }

}
