package window;

import bin.AdsClickLog;
import bin.WaterSensor;
import org.apache.flink.api.common.eventtime.SerializableTimestampAssigner;
import org.apache.flink.api.common.eventtime.WatermarkStrategy;
import org.apache.flink.api.common.functions.MapFunction;
import org.apache.flink.api.java.functions.KeySelector;
import org.apache.flink.api.java.tuple.Tuple2;
import org.apache.flink.streaming.api.datastream.SingleOutputStreamOperator;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.streaming.api.functions.windowing.ProcessWindowFunction;
import org.apache.flink.streaming.api.windowing.assigners.TumblingEventTimeWindows;
import org.apache.flink.streaming.api.windowing.time.Time;
import org.apache.flink.streaming.api.windowing.windows.TimeWindow;
import org.apache.flink.util.Collector;
import org.apache.flink.util.OutputTag;

import java.time.Duration;

public class WatermarkTest {

    public static void main(String[] args) throws Exception {

        StreamExecutionEnvironment env = StreamExecutionEnvironment.getExecutionEnvironment().setParallelism(1);
        System.out.println(env.getConfig());

        SingleOutputStreamOperator<WaterSensor> stream = env
                .socketTextStream("node01", 9999)  // 在socket终端只输入毫秒级别的时间戳
                .map(new MapFunction<String, WaterSensor>() {
                    @Override
                    public WaterSensor map(String value) throws Exception {
                        String[] datas = value.split(",");
                        return new WaterSensor(datas[0], Long.valueOf(datas[1]), Integer.valueOf(datas[2]));

                    }
                });

        // 创建水印生产策略
        WatermarkStrategy<WaterSensor> wms = WatermarkStrategy
                .<WaterSensor>forBoundedOutOfOrderness(Duration.ofSeconds(2)) // 最大容忍的延迟时间
                .withTimestampAssigner(new SerializableTimestampAssigner<WaterSensor>() { // 指定时间戳
                    @Override
                    public long extractTimestamp(WaterSensor element, long recordTimestamp) {
                        return element.getTs();
                    }
                });


        stream
                .assignTimestampsAndWatermarks(wms)
                .keyBy(WaterSensor::getId)
                .window(TumblingEventTimeWindows.of(Time.seconds(5)))
                .allowedLateness(Time.seconds(2))
                .process(new ProcessWindowFunction<WaterSensor, String, String, TimeWindow>() {
                    @Override
                    public void process(String s, Context context, Iterable<WaterSensor> iterable, Collector<String> collector) throws Exception {
                        System.out.println(iterable);
                    }
                })
                .print();
//                .sideOutputLateData(new OutputTag<WaterSensor>("side_1") {
//                }) // 设置侧输出流
//                .process(new ProcessWindowFunction<WaterSensor, String, String, TimeWindow>() {
//                    @Override
//                    public void process(String key, Context context, Iterable<WaterSensor> elements, Collector<String> out) throws Exception {
//                        String msg = "当前key: " + key
//                                + " 窗口: [" + context.window().getStart() / 1000 + "," + context.window().getEnd() / 1000 + ") 一共有 "
//                                + elements.spliterator().estimateSize() + "条数据" +
//                                "watermark: " + context.currentWatermark();
//                        out.collect(context.window().toString());
//                        out.collect(msg);
//                    }
//                });
        env.execute();
    }
}
