package window;

import bin.AdsClickLog;
import bin.WC;
import org.apache.flink.api.common.eventtime.SerializableTimestampAssigner;
import org.apache.flink.api.common.eventtime.WatermarkStrategy;
import org.apache.flink.api.common.functions.AggregateFunction;
import org.apache.flink.api.common.functions.MapFunction;
import org.apache.flink.api.java.functions.KeySelector;
import org.apache.flink.api.java.tuple.Tuple2;
import org.apache.flink.api.java.tuple.Tuple3;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.streaming.api.functions.windowing.ProcessWindowFunction;
import org.apache.flink.streaming.api.functions.windowing.WindowFunction;
import org.apache.flink.streaming.api.windowing.assigners.TumblingEventTimeWindows;
import org.apache.flink.streaming.api.windowing.time.Time;
import org.apache.flink.streaming.api.windowing.windows.TimeWindow;
import org.apache.flink.util.Collector;


import java.text.SimpleDateFormat;
import java.time.*;
import java.util.Date;

public class T {
    public static void main(String[] args) throws Exception {
        StreamExecutionEnvironment env = StreamExecutionEnvironment.getExecutionEnvironment();
        // 创建WatermarkStrategy
        WatermarkStrategy<String> wms = WatermarkStrategy
                .<String>forBoundedOutOfOrderness(Duration.ofSeconds(1))
                .withTimestampAssigner(new SerializableTimestampAssigner<String>() {
                    @Override
                    public long extractTimestamp(String s, long l) {
                        return Long.parseLong(s.split(",")[2]);
                    }
                });

        env
//                .readTextFile("input/wordCount.csv")
                .socketTextStream("node01", 9999)
                .assignTimestampsAndWatermarks(wms)
                .map(new MapFunction<String, Tuple2<String, Long>>() {
                    @Override
                    public Tuple2<String, Long> map(String s) throws Exception {
                        String[] split = s.split(",");
                        return new Tuple2<>(split[0] + "-" + split[1], 1L);
                    }
                })
                .keyBy(tp2 -> tp2.f0)
                .window(TumblingEventTimeWindows.of(Time.seconds(20)))
                .aggregate(new AggregateFunction<Tuple2<String, Long>, WC, Long>() {
                    @Override
                    public WC createAccumulator() {
                        return new WC("city", 0L);
                    }

                    @Override
                    public WC add(Tuple2<String, Long> tuple2, WC wc) {
                        return new WC("city", tuple2.f1 + wc.getSum());
                    }

                    @Override
                    public Long getResult(WC wc) {
                        return wc.getSum();
                    }

                    @Override
                    public WC merge(WC wc, WC acc1) {
                        return new WC("city", wc.getSum() + acc1.getSum());
                    }
                }, new WindowFunction<Long, Tuple2<String, Long>, String, TimeWindow>() {
                    @Override
                    public void apply(String s, TimeWindow timeWindow, Iterable<Long> iterable, Collector<Tuple2<String, Long>> collector) throws Exception {
                        Long next = iterable.iterator().next();
                        long end = timeWindow.getEnd();


                        collector.collect(new Tuple2<>(String.valueOf(end), next));
                    }
                })
                .keyBy(tuple2 -> tuple2.f0)
                .window(TumblingEventTimeWindows.of(Time.seconds(20)))
                .sum(1)


                // === 降低维度分组
//                .keyBy(new KeySelector<Tuple2<String, Long>, String>() {
//                    @Override
//                    public String getKey(Tuple2<String, Long> tuple2) throws Exception {
//                        return tuple2.f0.split("-")[0];
//                    }
//                })
//                .window(TumblingEventTimeWindows.of(Time.seconds(5)))
//                .process(new ProcessWindowFunction<Tuple2<String, Long>, Tuple2<String, Long>, String, TimeWindow>() {
//                    @Override
//                    public void process(String s, Context context, Iterable<Tuple2<String, Long>> iterable, Collector<Tuple2<String, Long>> collector) throws Exception {
//                        long start = context.window().getStart();
//                        System.out.println(s + "," + LongToString(start, "yyyy-MM-dd HH:mm:ss") + "," + LongToString(context.window().getEnd(), "yyyy-MM-dd HH:mm:ss"));
//                        Long sum = 0L;
//                        for (Tuple2<String, Long> t : iterable) {
//                            sum += t.f1;
//                        }
//                        collector.collect(new Tuple2<>(s, sum));
//                    }
//                })
                .print();


        env.execute();
    }

    public static String LongToString(Long time, String format) {
        if (time != null) {
            return new SimpleDateFormat(format).format(new Date(time));
        }
        return null;
    }
}
