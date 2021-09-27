package state;

import bin.WaterSensor;
import org.apache.flink.api.common.functions.AggregateFunction;
import org.apache.flink.api.common.state.AggregatingState;
import org.apache.flink.api.common.state.AggregatingStateDescriptor;
import org.apache.flink.configuration.Configuration;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.streaming.api.functions.KeyedProcessFunction;
import org.apache.flink.util.Collector;

public class case05_AggregatingState {
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
                    private AggregatingState<Integer, Integer> vc;

                    @Override
                    public void open(Configuration parameters) throws Exception {
                        vc = getRuntimeContext().getAggregatingState(new AggregatingStateDescriptor<Integer, Integer, Integer>("vc", new AggregateFunction<Integer, Integer, Integer>() {
                            @Override
                            public Integer createAccumulator() {
                                return 0;
                            }

                            @Override
                            public Integer add(Integer integer, Integer integer2) {
                                return integer + integer2;
                            }

                            @Override
                            public Integer getResult(Integer integer) {
                                return integer;
                            }

                            @Override
                            public Integer merge(Integer integer, Integer acc1) {
                                return integer + acc1;
                            }
                        }, Integer.class));
                    }


                    @Override
                    public void processElement(WaterSensor waterSensor, Context context, Collector<String> collector) throws Exception {
                        vc.add(waterSensor.getVc());

                        collector.collect(vc.get().toString());
                    }
                }).print();


        env.execute();

    }
}
