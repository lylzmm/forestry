package timer;

import bin.WaterSensor;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.streaming.api.functions.KeyedProcessFunction;
import org.apache.flink.util.Collector;

public class case01_ProcessingTimeTimer {
    public static void main(String[] args) throws Exception {

        StreamExecutionEnvironment env = StreamExecutionEnvironment.getExecutionEnvironment();
        env.socketTextStream("node01", 9999)
                .map(value -> {
                    String[] datas = value.split(",");
                    return new WaterSensor(datas[0], Long.valueOf(datas[1]), Integer.valueOf(datas[2]));

                })
                .keyBy(WaterSensor::getId)
                .process(new KeyedProcessFunction<String, WaterSensor, String>() {
                    @Override
                    public void processElement(WaterSensor value, Context ctx, Collector<String> collector) throws Exception {
                        // 处理时间过后5s后触发定时器
                        long currentTime = ctx.timerService().currentProcessingTime();
                        ctx.timerService().registerProcessingTimeTimer(currentTime + 5000 );


                        collector.collect(String.valueOf(currentTime));
                    }

                    @Override
                    public void onTimer(long timestamp, OnTimerContext ctx, Collector<String> out) throws Exception {

                        System.out.println(timestamp);
                        out.collect("定时器");
                    }
                })
                .print();

        env.execute();
    }
}
