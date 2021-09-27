package bin;

import sun.rmi.runtime.Log;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class PV {
    private String pv;
    private Long sum;
    private String dt;
}
