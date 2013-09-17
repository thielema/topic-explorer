package cc.topicexplorer.chain;

import java.util.HashMap;
import java.util.Map;

import org.apache.commons.chain.impl.ContextBase;
import org.apache.log4j.Logger;

public class CommunicationContext extends ContextBase {

	private static final long serialVersionUID = -7187427960916517256L;
	private Map<String, Object> objects = new HashMap<String, Object>();
	private Logger logger = Logger.getRootLogger();

	public void put(String name, Object object) {

		if (objects.containsKey(name)) {
			logger.warn("An object with this name (" + name + ") is already included, it will be replaced by the new object.");
		}

		objects.put(name, object);
	}

	public Object get(String name) {
		return objects.get(name);
	}

	public void remove(String name) {
		objects.remove(name);
	}
}