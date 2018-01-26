package org.ak.trafficController.annotations.samples.SubmitExample.normalFlow;

import javax.inject.Named;

import org.ak.trafficController.annotations.api.Controlled;
import org.ak.trafficController.annotations.api.TaskType;

@Named
public class DataType2DAOService {

	@Controlled(taskType=TaskType.SLOW)
	public DataType2 getDataType2(String id) {
		return new DataType2().setId(id).setValue(2.3).setSomeOtherValue(4.5);
	}
}
