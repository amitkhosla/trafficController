package org.ak.trafficController.annotations.samples.SubmitExample.normalFlow;

import javax.inject.Inject;
import javax.inject.Named;

import org.ak.trafficController.TaskExecutor;
import org.ak.trafficController.annotations.api.Controlled;

@Named
public class DataCollectionService {
	
	@Inject
	DataType1DAOService daoService1;
	
	@Inject
	DataType2DAOService daoService2;
	
	@Inject
	DataType3DAOService daoService3;
	
	@Controlled
	public void getData(int id) {
		DataType1 dt1 = daoService1.getDataTypeService(id);
		DataType2 dt2 = daoService2.getDataType2(dt1.getStringVal1());
		DataType3 dt3 = daoService3.getDataType3(dt1.getStringVal1());
		
		//some join operation
		
	}
	
	public void betterGetData(int id) {
		try {
			TaskExecutor.getInstance()
				.of(()-> daoService1.getDataTypeService(id))
				.thenConsumeMultiple(
						d->daoService2.getDataType2(d.getStringVal1()),
						d->daoService3.getDataType3(d.getStringVal1())
				).start();
		} catch (Throwable e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
}
