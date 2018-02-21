package org.ak.trafficController.annotations.samples.parallel;

import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.EnableAspectJAutoProxy;

@Configuration
@ComponentScan(basePackages="org.ak")
@EnableAspectJAutoProxy
public class ParallelConfig {

}
