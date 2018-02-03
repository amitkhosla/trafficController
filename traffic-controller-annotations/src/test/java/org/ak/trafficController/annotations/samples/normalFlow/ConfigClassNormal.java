package org.ak.trafficController.annotations.samples.normalFlow;

import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.EnableAspectJAutoProxy;

@Configuration
@ComponentScan(basePackages="org.ak")
@EnableAspectJAutoProxy
public class ConfigClassNormal {
	
}
