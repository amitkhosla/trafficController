package com.samples.sample1.annotatedFlow;

import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.EnableAspectJAutoProxy;

@Configuration
@ComponentScan(basePackages="com.samples.sample1.annotatedFlow, org.ak.trafficController")
@EnableAspectJAutoProxy
public class ConfigClass {
	
}
