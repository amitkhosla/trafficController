package org.ak.trafficController.annotations.samples.parallel;

import javax.inject.Named;

import org.ak.trafficController.annotations.api.Join;

@Named
public class Joiner2 {

	@Join
	public Integer getTwice(Integer a) {
		return a * 2;
	}
}
