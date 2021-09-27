package timer;

import bin.WaterSensor;
import org.apache.flink.api.common.state.ValueState;
import org.apache.flink.api.common.state.ValueStateDescriptor;
import org.apache.flink.configuration.Configuration;
import org.apache.flink.streaming.api.datastream.SingleOutputStreamOperator;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.streaming.api.functions.windowing.ProcessWindowFunction;
import org.apache.flink.streaming.api.windowing.assigners.TumblingProcessingTimeWindows;
import org.apache.flink.streaming.api.windowing.time.Time;
import org.apache.flink.streaming.api.windowing.windows.TimeWindow;
import org.apache.flink.util.Collector;
import org.apache.flink.util.OutputTag;

public class TumblingProcessingTimeWindowsTest {
    public static void main(String[] args) throws Exception {
        StreamExecutionEnvironment env = StreamExecutionEnvironment.getExecutionEnvironment();
        SingleOutputStreamOperator<String> process = env
                .socketTextStream("node01", 9999)
                .map(value -> {
                    String[] datas = value.split(",");
                    return new WaterSensor(datas[0], Long.valueOf(datas[1]), Integer.valueOf(datas[2]));

                })
                .keyBy(WaterSensor::getId)
                .window(TumblingProcessingTimeWindows.of(Time.seconds(6))) // 添加滚动窗口
                .process(new ProcessWindowFunction<WaterSensor, String, String, TimeWindow>() {
                    private ValueState<Integer> value;

                    @Override
                    public void open(Configuration parameters) throws Exception {
                        value = getRuntimeContext().getState(new ValueStateDescriptor<Integer>("value", Integer.class));
                    }

                    @Override
                    public void process(String s, Context context, Iterable<WaterSensor> iterable, Collector<String> collector) throws Exception {
                        collector.collect("sss");
                        context.output(new OutputTag<String>("警告") {
                        }, iterable.toString());
                    }
                });

        process.print("主流");
        process.getSideOutput(new OutputTag<String>("警告") {
        }).print();

        env.execute();
    }
}
