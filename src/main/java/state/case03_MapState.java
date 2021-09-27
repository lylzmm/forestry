package state;

import bin.WaterSensor;
import org.apache.flink.api.common.state.MapState;
import org.apache.flink.api.common.state.MapStateDescriptor;
import org.apache.flink.configuration.Configuration;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.streaming.api.functions.KeyedProcessFunction;
import org.apache.flink.util.Collector;

/**
 * 去重: 去掉重复的水位值. 思路: 把水位值作为MapState的key来实现去重, value随意
 */
public class case03_MapState {
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
                    private MapState<Integer, String> mapState;
                    @Override
                    public void open(Configuration parameters) throws Exception {
                        mapState = getRuntimeContext().getMapState(new MapStateDescriptor<Integer, String>("mapState", Integer.class, String.class));
                    }

                    @Override
                    public void processElement(WaterSensor waterSensor, Context context, Collector<String> collector) throws Exception {
                        Integer vc = waterSensor.getVc();
                        mapState.put(vc, "value");
                        collector.collect(mapState.keys().toString());
                    }
                })
                .print();

        env.execute();
    }
}
