package timer;

import bin.WaterSensor;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.streaming.api.functions.KeyedProcessFunction;
import org.apache.flink.util.Collector;

public class test01 {
    public static void main(String[] args) throws Exception {
        StreamExecutionEnvironment env = StreamExecutionEnvironment.getExecutionEnvironment();
        env.setParallelism(1);
        env.socketTextStream("node01", 9999)
                .map(value -> {
                    String[] datas = value.split(",");
                    return new WaterSensor(datas[0], Long.valueOf(datas[1]), Integer.valueOf(datas[2]));

                })
                .keyBy(WaterSensor::getId)
                .process(new KeyedProcessFunction<String, WaterSensor, String>() {

                    private Long tm = Long.MIN_VALUE;
                    private Integer lastVc = null;

                    @Override
                    public void processElement(WaterSensor waterSensor, Context context, Collector<String> collector) throws Exception {

                        Integer vc = waterSensor.getVc();

                        // 当前值大于上次值
                        // 2
                        // 2
                        // 3
                        if (lastVc != null && vc > lastVc) {
                            tm = context.timerService().currentProcessingTime();
                            context.timerService().registerProcessingTimeTimer(tm + 5000);
                        } else if (lastVc != null) {
                            // 删除定时器
                            context.timerService().deleteEventTimeTimer(tm);

                        }

                        lastVc = vc;
                        collector.collect(vc.toString());
                    }

                    @Override
                    public void onTimer(long timestamp, OnTimerContext ctx, Collector<String> out) throws Exception {

                        out.collect("出现预警" + ctx.getCurrentKey());
                    }
                })
                .print();

        env.execute();
    }
}
