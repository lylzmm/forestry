package state;


import bin.WaterSensor;
import org.apache.flink.api.common.functions.ReduceFunction;
import org.apache.flink.api.common.state.ReducingState;
import org.apache.flink.api.common.state.ReducingStateDescriptor;
import org.apache.flink.configuration.Configuration;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.streaming.api.functions.KeyedProcessFunction;
import org.apache.flink.util.Collector;

/**
 * 计算每个传感器的水位和
 */
public class case04_ReducingState {
    public static void main(String[] args) throws Exception {
        StreamExecutionEnvironment env = StreamExecutionEnvironment
                .getExecutionEnvironment()
                .setParallelism(3);

        env
                .socketTextStream("node01", 9999)
                .map(value -> {

                    if (value.split(",").length == 3) {
                        String[] datas = value.split(",");
                        return new WaterSensor(datas[0], Long.valueOf(datas[1]), Integer.valueOf(datas[2]));
                    }
                    return new WaterSensor("0", 2L, 2);
                })
                .keyBy(WaterSensor::getId)
                .process(new KeyedProcessFunction<String, WaterSensor, String>() {
                    private ReducingState<Integer> reduce;

                    @Override
                    public void open(Configuration parameters) throws Exception {
                        reduce = getRuntimeContext().getReducingState(new ReducingStateDescriptor<Integer>("reduce", Integer::sum, Integer.class));
                    }

                    @Override
                    public void processElement(WaterSensor waterSensor, Context context, Collector<String> collector) throws Exception {
                        Integer vc = waterSensor.getVc();
                        reduce.add(vc);

                        collector.collect(reduce.get().toString());
                    }
                })
                .print();

        env.execute();
    }
}
