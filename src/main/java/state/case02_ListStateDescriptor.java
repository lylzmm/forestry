package state;

import bin.WaterSensor;
import org.apache.flink.api.common.state.ListState;
import org.apache.flink.api.common.state.ListStateDescriptor;
import org.apache.flink.configuration.Configuration;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.streaming.api.functions.KeyedProcessFunction;
import org.apache.flink.util.Collector;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.TreeSet;

/**
 * 针对每个传感器输出最高的3个水位值
 */
public class case02_ListStateDescriptor {
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
                    private ListState<Integer> vcState;

                    @Override
                    public void open(Configuration parameters) throws Exception {
                        vcState = getRuntimeContext().getListState(new ListStateDescriptor<>("vcState", Integer.class));
                    }

                    @Override
                    public void processElement(WaterSensor waterSensor, Context context, Collector<String> collector) throws Exception {
                        vcState.add(waterSensor.getVc());

                        TreeSet<Integer> set = new TreeSet<>(Comparator.reverseOrder());
                        for (Integer v : vcState.get()) set.add(v);
                        ArrayList<Integer> arrayList = new ArrayList<>(set);

                        if (arrayList.size() > 3)
                            arrayList.remove(3);
                        vcState.update(arrayList);

                        collector.collect(arrayList.toString());
                    }
                }).print();

        env.execute();


    }
}
