package window;

import bin.AdsClickLog;
import org.apache.flink.api.common.eventtime.SerializableTimestampAssigner;
import org.apache.flink.api.common.eventtime.WatermarkStrategy;
import org.apache.flink.api.common.functions.MapFunction;
import org.apache.flink.api.java.functions.KeySelector;
import org.apache.flink.api.java.tuple.Tuple2;
import org.apache.flink.streaming.api.datastream.SingleOutputStreamOperator;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.streaming.api.windowing.assigners.TumblingEventTimeWindows;
import org.apache.flink.streaming.api.windowing.time.Time;

import java.time.Duration;

public class AdsClick {

    public static void main(String[] args) throws Exception {
        StreamExecutionEnvironment env = StreamExecutionEnvironment.getExecutionEnvironment();
        env.setParallelism(2);
        // 创建WatermarkStrategy
        WatermarkStrategy<AdsClickLog> wms = WatermarkStrategy
                .<AdsClickLog>forBoundedOutOfOrderness(Duration.ofSeconds(20))
                .withTimestampAssigner(new SerializableTimestampAssigner<AdsClickLog>() {
                    @Override
                    public long extractTimestamp(AdsClickLog element, long recordTimestamp) {
                        return element.getTimestamp() * 1000L;
                    }
                });


        env
                .readTextFile("input/AdClickLog.csv")
                .map(line -> {
                    String[] datas = line.split(",");
                    return new AdsClickLog(Long.valueOf(datas[0]),
                            Long.valueOf(datas[1]),
                            datas[2],
                            datas[3],
                            Long.valueOf(datas[4]));
                })
                .assignTimestampsAndWatermarks(wms)
                .map(new MapFunction<AdsClickLog, Tuple2<AdsClickLog, Long>>() {
                    @Override
                    public Tuple2<AdsClickLog, Long> map(AdsClickLog adsClickLog) throws Exception {
                        return new Tuple2<>(adsClickLog, 1L);
                    }
                })
                // 按照装 (用户, 广告) 分组
                .keyBy(new KeySelector<Tuple2<AdsClickLog, Long>, String>() {
                    @Override
                    public String getKey(Tuple2<AdsClickLog, Long> adsClickLogLongTuple2) throws Exception {
                        return adsClickLogLongTuple2.f0.getUserId() + "-" + adsClickLogLongTuple2.f0.getAdId();
                    }
                })
                .window(TumblingEventTimeWindows.of(Time.seconds(12000)))
                .sum(1)
                .print();
        env.execute();
    }
}
