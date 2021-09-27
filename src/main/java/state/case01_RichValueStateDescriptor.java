package state;

import bin.WaterSensor;
import org.apache.flink.api.common.functions.RichFilterFunction;
import org.apache.flink.api.common.functions.RichFlatMapFunction;
import org.apache.flink.api.common.functions.RichMapFunction;
import org.apache.flink.api.common.state.ValueState;
import org.apache.flink.api.common.state.ValueStateDescriptor;
import org.apache.flink.configuration.Configuration;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.streaming.api.functions.KeyedProcessFunction;
import org.apache.flink.util.Collector;

/**
 * 检测传感器的水位线值，如果连续的两个水位线差值超过10，就输出报警
 */
public class case01_RichValueStateDescriptor {
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
                .flatMap(new RichFlatMapFunction<WaterSensor, String>() {


                    @Override
                    public void flatMap(WaterSensor waterSensor, Collector<String> collector) throws Exception {

                        collector.collect("");
                    }
                })

                // Todo  map
//                .map(new RichMapFunction<WaterSensor, String>() {
//                    private ValueState<Integer> state;
//
//                    @Override
//                    public void open(Configuration parameters) throws Exception {
//                        state = getRuntimeContext().getState(new ValueStateDescriptor<Integer>("state", Integer.class));
//                    }
//
//                    @Override
//                    public String map(WaterSensor value) throws Exception {
//                        Integer lastVc = state.value() == null ? 0 : state.value();
//                        String str = "";
//                        if (Math.abs(value.getVc() - lastVc) >= 10) {
//                            str += "红色警报";
//                            return str;
//                        }
//                        state.update(value.getVc());
//
//                        return "null";
//                    }
//                })
                // Todo filter
//                .filter(new RichFilterFunction<WaterSensor>() {
//                    private ValueState<Integer> state;
//
//                    @Override
//                    public void open(Configuration parameters) throws Exception {
//                        state = getRuntimeContext().getState(new ValueStateDescriptor<Integer>("state", Integer.class));
//                    }
//
//                    @Override
//                    public boolean filter(WaterSensor value) throws Exception {
//                        Integer lastVc = state.value() == null ? 0 : state.value();
//                        String str = "";
//                        if (Math.abs(value.getVc() - lastVc) >= 10) {
//                            str += "红色警报";
//                            return true;
//                        }
//                        state.update(value.getVc());
//
//                        return false;
//                    }
//                })
                .print();
//                .process(new KeyedProcessFunction<String, WaterSensor, String>() {
//
//                    private ValueState<Integer> state;
//
//                    @Override
//                    public void open(Configuration parameters) throws Exception {
//                        state = getRuntimeContext().getState(new ValueStateDescriptor<Integer>("state", Integer.class));
//                    }
//
//                    @Override
//                    public void processElement(WaterSensor value, Context context, Collector<String> out) throws Exception {
//                        Integer lastVc = state.value() == null ? 0 : state.value();
//                        String str = "";
//                        if (Math.abs(value.getVc() - lastVc) >= 10) {
//                            str += "红色警报";
//                        }
//                        state.update(value.getVc());
//
//                        str += value.getId() + "                     value.getVc() " + value.getVc() + " lastVc  " + lastVc;
//                        out.collect(str);
//                    }
//                })
//                .print();

        env.execute();
    }
}
