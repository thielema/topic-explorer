package cc.topicexplorer.web;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;

public class OnStartup implements ServletContextListener {

	@Override
	public void contextDestroyed(ServletContextEvent arg0) {

	}

	@Override
	public void contextInitialized(ServletContextEvent arg0) {
		System.out.println("Do on startup.");
		Context.init();
	}

}
