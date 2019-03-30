/**
 * 
 */
package org.ak.trafficController.multiRequests;

import java.lang.annotation.Annotation;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.Signature;
import org.junit.Test;
import org.mockito.Mockito;

import junit.framework.Assert;

/**
 * @author amit.khosla
 *
 */
public class ControlMultiRequestHandlerMicroTest {
	@Test
	public void testGetName() {
		ControlMultiRequestHandler handler = new ControlMultiRequestHandler();
		ControlMultiRequest controlMultiRequest = Mockito.mock(ControlMultiRequest.class);
		Mockito.when(controlMultiRequest.uniqueName()).thenReturn("myUniqueName");
		ProceedingJoinPoint joinPoint = null;
		String name = handler.getName(controlMultiRequest, joinPoint);
		Assert.assertEquals("myUniqueName", name);
	}
	
	@Test
	public void testGetNameWhenPassedEmpty() {
		ControlMultiRequestHandler handler = new ControlMultiRequestHandler();
		ControlMultiRequest controlMultiRequest = Mockito.mock(ControlMultiRequest.class);
		Mockito.when(controlMultiRequest.uniqueName()).thenReturn("");
		ProceedingJoinPoint joinPoint = Mockito.mock(ProceedingJoinPoint.class);
		Signature signature = Mockito.mock(Signature.class);
		Mockito.when(joinPoint.getSignature()).thenReturn(signature);
		Mockito.when(signature.toString()).thenReturn("signature of method");
		String name = handler.getName(controlMultiRequest, joinPoint);
		Assert.assertEquals("signature of method", name);
		//check null
		Mockito.when(controlMultiRequest.uniqueName()).thenReturn(null);
		name = handler.getName(controlMultiRequest, joinPoint);
		Assert.assertEquals("signature of method", name);
		
	}
}
