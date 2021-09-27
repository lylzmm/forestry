package window;

import bin.UserBehavior;
import bin.WaterSensor;
import org.apache.flink.api.common.eventtime.SerializableTimestampAssigner;
import org.apache.flink.api.common.eventtime.WatermarkStrategy;
import org.apache.flink.api.common.functions.AggregateFunction;
import org.apache.flink.api.common.functions.FlatMapFunction;
import org.apache.flink.api.java.tuple.Tuple2;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.streaming.api.functions.windowing.WindowFunction;
import org.apache.flink.streaming.api.windowing.assigners.TumblingProcessingTimeWindows;
import org.apache.flink.streaming.api.windowing.time.Time;
import org.apache.flink.streaming.api.windowing.windows.TimeWindow;
import org.apache.flink.util.Collector;

import java.time.Duration;

public class agg {
    public static void main(String[] args) throws Exception {
        StreamExecutionEnvironment env = StreamExecutionEnvironment.getExecutionEnvironment();

        env.socketTextStream("node01", 9999)
                .flatMap(new FlatMapFunction<String, Tuple2<String, Integer>>() {
                    @Override
                    public void flatMap(String s, Collector<Tuple2<String, Integer>> collector) throws Exception {
                        String[] datas = s.split(",");
                        for (String data : datas) {
                            collector.collect(new Tuple2<>(data, 1));
                        }
                    }
                })
                .keyBy(t2 -> t2.f0)
                .window(TumblingProcessingTimeWindows.of(Time.seconds(10)))
                .aggregate(new AggregateFunction<Tuple2<String, Integer>, Integer, Integer>() {
                    @Override
                    public Integer createAccumulator() {
                        return 0;
                    }

                    @Override
                    public Integer add(Tuple2<String, Integer> tuple2, Integer integer) {
                        return tuple2.f1 + integer;
                    }

                    @Override
                    public Integer getResult(Integer integer) {
                        return integer;
                    }

                    @Override
                    public Integer merge(Integer integer, Integer acc1) {
                        return integer + acc1;
                    }
                }, new WindowFunction<Integer, String, String, TimeWindow>() {
                    @Override
                    public void apply(String s, TimeWindow timeWindow, Iterable<Integer> iterable, Collector<String> collector) throws Exception {
                        Integer next = iterable.iterator().next();
                        collector.collect(s + "," + next);
                    }
                }).print();


        env.execute();
    }
}
