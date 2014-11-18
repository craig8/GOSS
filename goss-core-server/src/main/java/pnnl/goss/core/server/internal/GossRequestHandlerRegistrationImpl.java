/*
	Copyright (c) 2014, Battelle Memorial Institute
    All rights reserved.
    Redistribution and use in source and binary forms, with or without
    modification, are permitted provided that the following conditions are met:
    1. Redistributions of source code must retain the above copyright notice, this
    list of conditions and the following disclaimer.
    2. Redistributions in binary form must reproduce the above copyright notice,
    this list of conditions and the following disclaimer in the documentation
    and/or other materials provided with the distribution.
    THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
    ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
    WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
     
    DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR
    ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
    (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
    LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
    ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
    (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
    SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
    The views and conclusions contained in the software and documentation are those
    of the authors and should not be interpreted as representing official policies,
    either expressed or implied, of the FreeBSD Project.
    This material was prepared as an account of work sponsored by an
    agency of the United States Government. Neither the United States
    Government nor the United States Department of Energy, nor Battelle,
    nor any of their employees, nor any jurisdiction or organization
    that has cooperated in the development of these materials, makes
    any warranty, express or implied, or assumes any legal liability
    or responsibility for the accuracy, completeness, or usefulness or
    any information, apparatus, product, software, or process disclosed,
    or represents that its use would not infringe privately owned rights.
    Reference herein to any specific commercial product, process, or
    service by trade name, trademark, manufacturer, or otherwise does
    not necessarily constitute or imply its endorsement, recommendation,
    or favoring by the United States Government or any agency thereof,
    or Battelle Memorial Institute. The views and opinions of authors
    expressed herein do not necessarily state or reflect those of the
    United States Government or any agency thereof.
    PACIFIC NORTHWEST NATIONAL LABORATORY
    operated by BATTELLE for the UNITED STATES DEPARTMENT OF ENERGY
    under Contract DE-AC05-76RL01830
*/
package pnnl.goss.core.server.internal;

import java.io.IOException;
import java.lang.annotation.Annotation;
import java.net.URL;
import java.util.Collections;
import java.util.Dictionary;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Set;

import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Instantiate;
import org.apache.felix.ipojo.annotations.Invalidate;
import org.apache.felix.ipojo.annotations.Provides;
import org.apache.felix.ipojo.annotations.Requires;
import org.apache.felix.ipojo.annotations.Validate;
import org.reflections.Reflections;
import org.reflections.util.ConfigurationBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import eu.infomas.annotation.AnnotationDetector;
import eu.infomas.annotation.AnnotationDetector.TypeReporter;
import pnnl.goss.core.DataError;
import pnnl.goss.core.DataResponse;
import pnnl.goss.core.Request;
import pnnl.goss.core.RequestAsync;
import pnnl.goss.core.Response;
import pnnl.goss.core.UploadRequest;
import pnnl.goss.security.core.GossSecurityHandler;
import pnnl.goss.core.server.annotations.RequestHandler;
import pnnl.goss.core.server.annotations.RequestItem;
import pnnl.goss.core.server.GossDataServices;
import pnnl.goss.core.server.AbstractRequestHandler;
import pnnl.goss.core.server.GossRequestHandlerRegistrationService;

@SuppressWarnings("rawtypes")
@Instantiate
@Provides
@Component
//@Component@org.apache.felix.ipojo.whiteboard.Wbp(filter="(foo=true)",
//	onArrival="onArrival",
//	onDeparture="onDeparture",
//	onModification="onModification")
public class GossRequestHandlerRegistrationImpl implements GossRequestHandlerRegistrationService {

	private static final Logger log = LoggerFactory.getLogger(GossRequestHandlerRegistrationImpl.class);
	private HashMap<String, String> handlerMap = new HashMap<String, String>();
	private HashMap<String, Class> handlerToClasss = new HashMap<String, Class>();
	private GossSecurityHandler securityHandler;
	private Dictionary coreServerConfig = null;
	 
	private GossDataServices dataServices;
	
	public GossRequestHandlerRegistrationImpl(@Requires GossDataServices dataServices){
		log.debug("Constructing");
		if (dataServices == null){
			throw new NullPointerException("DataServices cannot be null!");
		}

		this.dataServices = dataServices;
	}
	
	@Validate
	public void startHandler(){
		log.debug("Starting handler");
	}

	@Invalidate
	public void shutdown(){
		log.debug("shutdown");
		this.handlerMap.clear();
	}
	
	public void addHandlersFromClassPath(){
		TypeReporter reporter = new TypeReporter() {
			
			@SuppressWarnings("unchecked")
			@Override
			public Class<? extends Annotation>[] annotations() {
				return new Class[] {RequestHandler.class};
			}
			
			@Override
			public void reportTypeAnnotation(Class<? extends Annotation> annotation,
					String className) {
				try {
					RequestHandler ann = (RequestHandler)(Class.forName(className)).getAnnotation(RequestHandler.class);
					for(int i=0; i<ann.value().length; i++){
						RequestItem itm = ann.value()[i];
						addHandlerMapping(itm.value().getName(), className);
						
					}
				} catch (ClassNotFoundException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				log.debug("Found handler: " + className);
				
			}
		};
		
		AnnotationDetector detector = new AnnotationDetector(reporter);
		try {
			detector.detect();
		} catch (IOException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
	}
	
	public void addHandlerMapping(String request, String handler) {
		if (request == null || handler == null) {
			log.error("request and handler must not be null!");
			return;
		}
		
		log.debug("adding handler mapping: "+request+" -> "+ handler);
		try {
			Class requestCls = Class.forName(request);
			Class handlerCls = Class.forName(handler);
			
			addHandlerMapping(requestCls, handlerCls);
			
		} catch (ClassNotFoundException e) {
			log.error("Error with class not found", e);
		}
	}
	
	
	public void addHandlerMapping(Class request, Class handler) {
		if (request == null || handler == null) {
			log.error("request and handler must not be null!");
			return;
		}

		try {
			log.debug("add handler mapping.\n\tRequest: " + request.getName() + "\n\tHandler: " + handler.getName());

			// Attempt to instantiate class before adding the string
			handler.newInstance();
			
			Class superClassTester = request.getSuperclass();
			boolean foundSuperClassRequest = false;
			boolean foundSuperClassHandler = false;

			while(superClassTester != null){
				
				if(superClassTester.equals(Request.class) || superClassTester.equals(RequestAsync.class) || superClassTester.equals(UploadRequest.class)){
					foundSuperClassRequest = true;
					break;
				}
				superClassTester = superClassTester.getSuperclass();
			}
			
			superClassTester = handler.getSuperclass();
			
			while(superClassTester != null){
				
				if(superClassTester.equals(AbstractRequestHandler.class)){
					foundSuperClassHandler = true;
					break;
				}
				superClassTester = superClassTester.getSuperclass();
			}
			
			if(!foundSuperClassHandler){
				throw new Exception("Invalid handler, must be subclass of "+AbstractRequestHandler.class.toString());
			}
			
			if(!foundSuperClassRequest){
				throw new Exception("Invalid request, must be subclass of "+Request.class.toString());
			}
			
			// Keep the string of the class.
			handlerMap.put(request.getName(), handler.getName());
			handlerToClasss.put(request.getName(), handler);

		} catch (InstantiationException e) {
			log.error("Couldn't instantiate " + handler.getName(), e);
		} catch (IllegalAccessException e) {
			log.error("Access error couldn't instantiate " + handler.getName(), e);
		} catch (Exception e) {
			log.error("AddHandlerMapping Exception ", e);
		}
	}
	
	/**
	 * Creates mapping between upload data type and corresponding RequestHandler class.
	 * @param dataType 
	 * 				String representing upload data type
	 * @param handler
	 * 				RequestHandler class name
	 */
	//TODO: complete data type and handler mapping.
	public void addUploadHandlerMapping(String dataType, String handler) {
		if (handler == null || dataType==null) {
			log.error("data type and handler must not be null!");
			return;
		}
		
		try {
			Class handlerCls = Class.forName(handler);
			addUploadHandlerMapping(dataType, handlerCls);
			
		} catch (ClassNotFoundException e) {
			log.error("Error with class not found", e);
		}
	}
	
	
	/**
	 * Creates mapping between upload data type and corresponding RequestHandler class.
	 * @param dataType 
	 * 				String representing upload data type
	 * @param handler
	 * 				RequestHandler class
	 */
	//TODO: complete data type and handler mapping.
	public void addUploadHandlerMapping(String dataType, Class handler) {
		if (handler == null || dataType==null) {
			log.error("data type and handler must not be null!");
			return;
		}
		
		try {
			
			// Attempt to instantiate class before adding the string
			handler.newInstance();
			
			Class superClassTester = handler.getSuperclass();
			boolean foundSuperClassHandler = false;
			
			while(superClassTester != null){
				
				if(superClassTester.equals(AbstractRequestHandler.class)){
					foundSuperClassHandler = true;
					break;
				}
				superClassTester = superClassTester.getSuperclass();
			}
			
			if(!foundSuperClassHandler){
				throw new Exception("Invalid handler, must be subclass of "+AbstractRequestHandler.class.toString());
			}
			
			// Keep the string of the class.
			handlerMap.put(dataType, handler.getName());

			
		} catch (ClassNotFoundException e) {
			log.error("Error with class not found", e);
		} catch (IllegalAccessException e) {
			log.error("Access error couldn't instantiate " + handler.getName(), e);
		} catch (Exception e) {
			log.error("AddUploadHandlerMapping Exception ", e);
		}
	}
	
	
	public void removeHandlerMapping(Class request) {
		if (request != null) {
			log.debug("removing mapping for: " + request.getName());

			if (handlerMap.containsKey(request)) {
				handlerMap.remove(request);
			}
			
			if (handlerToClasss.containsKey(request)){
				handlerToClasss.remove(request);
			}
		}
	}

	public Response handle(Request request) {
		Response response = null;
		AbstractRequestHandler handler = null;
		if (request != null) {
			log.debug("handling request for:\n\t " + request.getClass().getName() + " => " + handlerMap.get(request.getClass().getName()));

			if (handlerMap.containsKey(request.getClass().getName())) {
				try {
					Class handlerClass = handlerToClasss.get(request.getClass().getName());
					//Class handlerClass = Class.forName(handlerMap.get(request.getClass().getName()));
					handler = (AbstractRequestHandler) handlerClass.newInstance();
					if(handler!=null){
						handler.setGossDataservices(dataServices);
						handler.setHandlerService(this);
						response = handler.handle(request);
					}
					/*
					 * String handlerStr =
					 * handlerMap.get(request.getClass().getName());
					 * GossRequestHandler handler = (GossRequestHandler)
					 * Class.forName(handlerStr).newInstance(); response =
					 * handler.handle(request);
					 */
				} catch (Exception e) {
					log.error("Handle error exception", e);
				}
				/*
				 * catch (InstantiationException e) { log.error(e, e);
				 * e.printStackTrace(); } catch (IllegalAccessException e) {
				 * log.error(e, e); e.printStackTrace(); } catch
				 * (ClassNotFoundException e) { log.error(e, e);
				 * e.printStackTrace(); }
				 */
			}
		}

		if (handler == null) {
			log.debug("Passed handler object instance was null!");
			response = new DataResponse(new DataError("Handler mapping for: " + request.getClass().getName() + " not found!"));
		}
		if (response == null) {
			log.debug("Passed response object instance was null!");
			response = new DataResponse(new DataError("Empty response for: " + request.getClass().getName() + "!"));
		}
		return response;
	}
	
	@Override
	public Response handle(Request request, String dataType) {
		Response response = null;
		AbstractRequestHandler handler = null;
		if (dataType != null) {
			log.debug("handling request for: " + dataType);
			if (handlerMap.containsKey(dataType)) {
				try {
					Class handlerClass = Class.forName(handlerMap.get(dataType));
					handler = (AbstractRequestHandler) handlerClass.newInstance();
					if(handler!=null){
						handler.setGossDataservices(dataServices);
						handler.setHandlerService(this);
						response = handler.handle(request);
					}
					/*
					 * String handlerStr =
					 * handlerMap.get(request.getClass().getName());
					 * GossRequestHandler handler = (GossRequestHandler)
					 * Class.forName(handlerStr).newInstance(); response =
					 * handler.handle(request);
					 */
				} catch (Exception e) {
					log.error("Handle error exception", e);
				}
				/*
				 * catch (InstantiationException e) { log.error(e, e);
				 * e.printStackTrace(); } catch (IllegalAccessException e) {
				 * log.error(e, e); e.printStackTrace(); } catch
				 * (ClassNotFoundException e) { log.error(e, e);
				 * e.printStackTrace(); }
				 */
			}
		}
		return response;
	}
	
	public boolean checkAccess(Request request, String userPrincipals, String tempDestination) {
		return securityHandler.checkAccess(request, userPrincipals, tempDestination);
	}

	
	public void addSecurityMapping(Class request, Class handler) {
		if (securityHandler != null){
			securityHandler.addHandlerMapping(request, handler);
		}
		else{
			log.error("Security handler is null!");
		}
	}

	
	public void removeSecurityMapping(Class request) {
		log.debug("Removing security mapping for: "+ request.getClass().getName());
		if(securityHandler != null){
			securityHandler.removeHandlerMapping(request);
		}
		else{
			log.error("Security handler is null!");
		}
	}

	
	public Dictionary getCoreServerConfig() {
		return coreServerConfig;
	}

	
	public void setCoreServerConfig(Dictionary config) {
		coreServerConfig = config;
		
	}

	
	public AbstractRequestHandler getHandler(Request request) {
		AbstractRequestHandler handler = null;
		if (request != null) {
			log.debug("handling request for: " + request.getClass().getName());

			if (handlerMap.containsKey(request.getClass().getName())) {
				try {
					Class handlerClass = Class.forName(handlerMap.get(request.getClass().getName()));
					handler = (AbstractRequestHandler) handlerClass.newInstance();
				} catch (Exception e) {
					log.error("Handle error exception", e);
				}
			}
		}
		return handler;
		
	
	}


//	@Override
//	public void registerHandlers(Enumeration<URL> urls) {
//	
//		for(URL u:Collections.list(urls)){
//			System.out.println(u.toString());
//		}
//		Reflections ref = new Reflections(new ConfigurationBuilder()
//			.setUrls(Collections.list(urls)));
//	
//		Set<Class<?>> handers = ref.getTypesAnnotatedWith(RequestHandler.class);
//		
//		// For each of the annotations we need to add all of the elements that are specified
//		// in the requests parameter.
//		for(Class<?> handler : handers){
//			log.debug("Found handler: "+handler.getName());
//			for(Annotation annotation : handler.getAnnotations()){
//				for(RequestItem item: ((RequestHandler)annotation).value()){
//					addHandlerMapping(item.value(), annotation.getClass());
//				}
//			}
//		}
//	}
//
//	@Override
//	public void unregisterHandlers(Enumeration<URL> urls) {
//		Reflections ref = new Reflections(new ConfigurationBuilder()
//		.setUrls(Collections.list(urls)));
//
//		Set<Class<?>> handers = ref.getTypesAnnotatedWith(RequestHandler.class);
//		
//		// For each of the annotations we need to remove all of the requests.
//		for(Class<?> handler : handers){
//			log.debug("Found handler: "+handler.getName());
//			for(Annotation annotation : handler.getAnnotations()){
//				for(RequestItem item: ((RequestHandler)annotation).value()){
//					removeHandlerMapping(item.value());
//				}
//			}
//		}
//		
//	}

}
