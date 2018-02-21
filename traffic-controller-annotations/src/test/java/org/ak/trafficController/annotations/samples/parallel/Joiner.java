package org.ak.trafficController.annotations.samples.parallel;

import javax.inject.Named;

import org.ak.trafficController.annotations.api.Join;

@Named
public class Joiner {

	@Join
	public Integer join(Integer a, Integer b, Integer c, Integer d) {
		return a + b + c + d;
	}
}
