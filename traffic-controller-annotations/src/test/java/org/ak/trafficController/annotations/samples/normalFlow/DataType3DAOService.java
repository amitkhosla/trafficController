package org.ak.trafficController.annotations.samples.normalFlow;

import javax.inject.Named;

import org.ak.trafficController.annotations.api.Controlled;
import org.ak.trafficController.annotations.api.TaskType;

@Named
public class DataType3DAOService {
	
	@Controlled(taskType=TaskType.SLOW)
	public DataType3 getDataType3(String id) {
		return new DataType3().setId(id).setValue1(345).setValue2(456);
	}
}
