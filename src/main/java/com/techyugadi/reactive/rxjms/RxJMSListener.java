package com.techyugadi.reactive.rxjms;

import javax.jms.MessageListener;
import javax.jms.ExceptionListener;
import javax.jms.JMSException;

public interface RxJMSListener extends MessageListener, ExceptionListener {
	
	public void onError(Throwable e);
	public void onException(JMSException je);

}
