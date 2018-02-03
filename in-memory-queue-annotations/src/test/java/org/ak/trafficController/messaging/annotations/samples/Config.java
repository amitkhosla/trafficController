package org.ak.trafficController.messaging.annotations.samples;

import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.EnableAspectJAutoProxy;

@Configuration(value="some name")
@ComponentScan(basePackages="org.ak")
@EnableAspectJAutoProxy
public class Config {

}
