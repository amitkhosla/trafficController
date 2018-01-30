package org.ak.trafficController.annotations.samples.normalFlow;

import javax.inject.Named;

import org.ak.trafficController.annotations.api.Controlled;
import org.ak.trafficController.annotations.api.TaskType;

@Named
public class DataType1DAOService {
	
	@Controlled(taskType=TaskType.SLOW)
	public DataType1 getDataTypeService(int id) { //this will be actually a db call.
		return new DataType1().setIntVal1(id).setStringVal1("someValue");
	}
}
