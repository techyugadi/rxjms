package com.techyugadi.reactive.rxjms.sample;

import java.util.Hashtable;
import java.util.Properties;

import javax.jms.DeliveryMode;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageProducer;
import javax.jms.Queue;
import javax.jms.QueueConnection;
import javax.jms.QueueConnectionFactory;
import javax.jms.QueueSession;
import javax.jms.Session;
import javax.jms.TextMessage;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;

import com.techyugadi.reactive.rxjms.JMSObservable;
import com.techyugadi.reactive.rxjms.sample.SimpleApp.TestMessageProducer;

import io.reactivex.Observable;

public class SampleApp {
	
	private static String fileUrl = "file:///tmp";

    private static String factoryName = "QCF";
    private static String destName = "myQ";

    public static class TestMessageProducer implements Runnable {
		
        public void run() {
        	
        	System.out.println("Running TestMessageProducer +++++ ");
        	
        	Hashtable env;
            Context	ctx = null;
            QueueConnectionFactory qcf = null;
            QueueConnection qcon;
            QueueSession qsess;
            MessageProducer msgProducer;
            Queue queue = null;
            TextMessage msg;

            env = new Hashtable();

            env.put(Context.INITIAL_CONTEXT_FACTORY, 
            		"com.sun.jndi.fscontext.RefFSContextFactory");
            env.put(Context.PROVIDER_URL, fileUrl);

            try {
    	    // Create the initial context.
            	ctx = new InitialContext(env);
            } catch (NamingException ne)  {
            	System.err.println("Failed to create InitialContext.");
    	    	ne.printStackTrace();
            }
            
            try {
                qcf = (javax.jms.QueueConnectionFactory) ctx.lookup(factoryName);
                queue = (javax.jms.Queue) ctx.lookup(destName);
            } catch (NamingException ne)  {
            	System.err.println(
            			"Failed to lookup Connection Factory / Queue object.");
            }
            
            try {
                qcon = qcf.createQueueConnection();
                
                qsess = qcon.createQueueSession(false, Session.AUTO_ACKNOWLEDGE);

                msgProducer = qsess.createProducer(queue);  
                qcon.start();
                
                msgProducer.setDeliveryMode(DeliveryMode.NON_PERSISTENT);
                
                // Create messages
                for (int i=0; i<=10; i++) {
                	
                	String text = "MSG #" + i;  
                	TextMessage message = qsess.createTextMessage(text);
                	
                	if (i == 10) {
                		message.setStringProperty("STOP", "yes");
                	}
 
                	// Tell the producer to send the message
                	msgProducer.send(message);
                	System.out.println("SENT: " + text);
                	
                }
 
                // Clean up
                qsess.close();
                qcon.close();
                
            } catch (JMSException e)  {
        	    System.err.println("Error in connecting / sending messages");
        	    e.printStackTrace();
        	    System.exit(-1);
            }
            
        }
    }
    
    public static void main(String[] args) throws Exception {
    	
    	Integer maxMessages = 24;
		
		Properties jndiProps = new Properties();
		jndiProps.setProperty("java.naming.factory.initial", 
					"com.sun.jndi.fscontext.RefFSContextFactory");
		jndiProps.setProperty("java.naming.provider.url", fileUrl);
		
		Properties appProps = new Properties();
		appProps.setProperty("connectionFactoryName", "QCF");
		appProps.setProperty("destinationName", "myQ");
		appProps.setProperty("acknowledgeMode", "AUTO_ACKNOWLEDGE");
		
		appProps.setProperty("checkPropertyName", "STOP");
		appProps.setProperty("checkPropertyValue", "yes");
		
		JMSObservable jmsObservable = new JMSObservable(jndiProps, appProps);
		
		Observable<Message> observable = jmsObservable.retrieveObservable();
		
		Observable<String> newObservable = observable
				   							.map(m->m.toString())
				   							.zipWith(
				   									Observable.range(
				   										1, maxMessages),
				   										(str,seq) -> 
				   										"Seq #" + seq + ":" + str
				   							);
  
		newObservable.subscribe(System.out::println,
									err -> {System.out.println(
											"RECEIVED ERROR:" + 
													err.toString()); 
									err.printStackTrace();}
								);
		
		Runtime.getRuntime().addShutdownHook(new Thread(){
	      public void run() {
	        System.out.println("Cleaning up JMS Resources");
	        jmsObservable.cleanup();
	      }
	    });
		
		Thread producerThread = new Thread(new TestMessageProducer());
		producerThread.start();
		
		// Simplistic approach for this sample app
		// Ideally the producer thread and the observable will be
		// on two separate programs
		Thread.sleep(3000);
		System.exit(0);
		
	}

}
