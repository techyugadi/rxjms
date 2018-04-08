package com.techyugadi.reactive.rxjms;

import java.util.Properties;

import javax.jms.ConnectionFactory;
import javax.jms.Connection;
import javax.jms.Destination;
import javax.jms.JMSException;
import javax.jms.Session;
import javax.jms.Message;
import javax.jms.MessageConsumer;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;

import com.techyugadi.reactive.rxjms.RxJMSListener;

import io.reactivex.BackpressureStrategy;
import io.reactivex.Flowable;
import io.reactivex.Observable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class JMSObservable {
	
	private static final Logger log = LoggerFactory.getLogger(
											JMSObservable.class);
	
	private ConnectionFactory connectionFactory;
	private Connection connection;
	private Destination destination;
	private Session session;
	private boolean sessionTransacted;
	private MessageConsumer messageConsumer;
	
	private Properties jndiConfig, appConfig;
	
	public JMSObservable(Properties jndiProps, Properties appProps) 
							throws ConfigurationException, JMSException {
		
		this.jndiConfig = jndiProps;
		this.appConfig = appProps;
 
    	if (jndiConfig.getProperty("java.naming.factory.initial") == null ||
    		jndiConfig.getProperty("java.naming.provider.url") == null)
    		throw new ConfigurationException(
    				"JNDI InitialCntext Factory and URL must be specified");
    	
    	Context ctx = null;
    	try {
    		ctx = new InitialContext(jndiConfig);
    	} catch (NamingException ex) {
    		ex.printStackTrace();
    		throw new ConfigurationException(
    				"Exception while initializing JNDI Context: " + ex);
    	}
    		
    	if (appConfig.getProperty("session_transacted") == "true")
           	sessionTransacted = true;
        else 
          	sessionTransacted = false;
            
        String acknowledgeMode = appConfig.getProperty("acknowledgeMode");
        int acknowledge = -1;
        if (acknowledgeMode != null) {
			if ("AUTO_ACKNOWLEDGE".equalsIgnoreCase(acknowledgeMode))
				acknowledge = Session.AUTO_ACKNOWLEDGE;
			else if ("CLIENT_ACKNOWLEDGE".equalsIgnoreCase(acknowledgeMode))
				acknowledge = Session.CLIENT_ACKNOWLEDGE;
			else if ("DUPS_OK_ACKNOWLEDGE".equalsIgnoreCase(acknowledgeMode))
				acknowledge = Session.DUPS_OK_ACKNOWLEDGE;
			else
				throw new ConfigurationException(
						"Unknown Session Acknowledge mode: " + acknowledgeMode);
		}
        
        if (sessionTransacted && acknowledge != -1)
        	log.warn("Acknowledgement mode set with transacted JMS session. " +
        				"This could lead to unexpected results.");
    	
        String connectionFactoryName = appConfig.getProperty(
        								"connectionFactoryName");
        if (connectionFactoryName == null || connectionFactoryName.length() == 0)
        	throw new ConfigurationException(
				"No ConnectionFactory specified.");
        
        String destinationName = appConfig.getProperty("destinationName");
        if (destinationName == null || destinationName.length() == 0)
        	throw new ConfigurationException(
				"No JMS Destination specified.");
        
    	try {
    		connectionFactory = (ConnectionFactory)
    									ctx.lookup(connectionFactoryName);
    		destination = (Destination) ctx.lookup(destinationName);
    	} catch (NamingException ex) {
    		ex.printStackTrace();
    		throw new ConfigurationException(
    				"Exception while performing JNDI lookup: " + ex);
    	}
    	
    	connection = ((ConnectionFactory) connectionFactory)
    												.createConnection();
    	
    	try {
    		ctx.close();
    	} catch (NamingException ex) {
    		log.warn("NamingException while closing JNDI InitialCintext: " + ex);
    		ex.printStackTrace();
    	}
    			
    	session = ((Connection) connection).createSession(
    									sessionTransacted, acknowledge);
    		
    	String messageSelector = appConfig.getProperty("messageSelector");
    	if (messageSelector != null && messageSelector.length() != 0)
    		messageConsumer = session.createConsumer(destination, messageSelector);
    	else 
    		messageConsumer = session.createConsumer(destination);

	}
	
	public Observable<Message> retrieveObservable() {
		
		Observable<Message> observable = 
				Observable.create(emitter -> {
						
						RxJMSListener listener = new RxJMSListener() {
							
							@Override 
							public void onMessage(Message event) { 
								emitter.onNext(event);
								String propName = appConfig.getProperty(
												"checkPropertyName");
								String propVal = appConfig.getProperty(
												"checkPropertyValue");
								
								try {
									if (propName != null) {
										String messagePropVal = 
											event.getStringProperty(propName);
								
										if (messagePropVal != null &&
											messagePropVal.
												equalsIgnoreCase(propVal)) { 
											emitter.onComplete(); 
										} 
									}
								} catch (JMSException e) {
									e.printStackTrace();
								}
							} 

							@Override 
							public void onError(Throwable e) { 
								emitter.onError(e); 
							} 
							
							@Override
							public void onException(JMSException je) {
								emitter.onError(je);
							}
							
						};
						
						messageConsumer.setMessageListener(listener);
						connection.start();
						
					}
				);
		
		return observable;
		
	}
	
	public Flowable<Message> retrieveFlowable(
			BackpressureStrategy strategy) {

		return retrieveObservable().toFlowable(strategy);

	}
	
	public void cleanup() {
		
		try {
			session.close();
		} catch (JMSException e) {
			e.printStackTrace();
		} finally {
			if (connection != null) {
				try {
					connection.close();
				} catch (JMSException e) {
					e.printStackTrace();
				}
			}
		}
		
	}

}
