package cc.topicexplorer.commands;

import java.io.IOException;
import java.util.List;
import java.util.MissingResourceException;
import java.util.Properties;

import org.apache.commons.chain.Context;

import cc.commandmanager.core.CommunicationContext;
import cc.commandmanager.core.DependencyCommand;
import cc.topicexplorer.utils.PropertiesUtil;
import cc.topicexplorer.utils.PropertiesUtil.PropertyKind;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Strings;
import com.google.common.collect.Lists;

public class PropertiesCommand extends DependencyCommand {
	@VisibleForTesting
	static final String PROPERTIES_CONTEXT_KEY = "properties";
	private static final String CONFIG_GLOBAL_PROPERTIES = "config.global.properties";
	private static final String CONFIG_LOCAL_PROPERTIES = "config.local.properties";
	private static final String DATABASE_GLOBAL_PROPERTIES = "database.global.properties";
	private static final String DATABASE_LOCAL_PROPERTIES = "database.local.properties";
	private static final String NO_PREFIX = "";
	private static final String DATABASE_PREFIX = "database.";
	private static final String PLUGINS_CONTEXT_KEY = "plugins";

	private Properties _properties;

	/**
	 * @throws MissingResourceException
	 *             if local.config.properties could not be resolved as a stream
	 * @throws IOException
	 *             if an error occured when reading from the input stream
	 * @throws IllegalStateException
	 *             if one of the essential properties could not be loaded as
	 *             they are config.local.properties, database.global.properties,
	 *             database.local.properties
	 */
	@Override
	public void specialExecute(Context context) {
		CommunicationContext communicationContext = (CommunicationContext) context;

		loadGlobalAndLocalConfigProperties();
		loadGlobalAndLocalDbProperties();
		loadPluginProperties();

		communicationContext.put(PROPERTIES_CONTEXT_KEY, _properties);
	}

	private void loadGlobalAndLocalConfigProperties() {
		_properties = PropertiesUtil.loadMandatoryProperties(CONFIG_GLOBAL_PROPERTIES, NO_PREFIX, PropertyKind.GLOBAL);
		_properties = PropertiesUtil.updateMandatoryProperties(_properties, CONFIG_GLOBAL_PROPERTIES, NO_PREFIX,
				PropertyKind.GLOBAL);
		_properties = PropertiesUtil.updateMandatoryProperties(_properties, CONFIG_LOCAL_PROPERTIES, NO_PREFIX,
				PropertyKind.LOCAL);
	}

	private void loadGlobalAndLocalDbProperties() {
		_properties = PropertiesUtil.updateMandatoryProperties(_properties, DATABASE_GLOBAL_PROPERTIES,
				DATABASE_PREFIX, PropertyKind.GLOBAL);
		_properties = PropertiesUtil.updateMandatoryProperties(_properties, DATABASE_LOCAL_PROPERTIES, DATABASE_PREFIX,
				PropertyKind.LOCAL);
	}

	private void loadPluginProperties() {
		List<String> enabledPlugins = getEnabledPlugins();
		if (enabledPlugins.size() > 1 || !Strings.isNullOrEmpty(enabledPlugins.get(0))) {
			for (String plugin : enabledPlugins) {
				plugin = removeWhiteSpacesAndlowerCase(plugin);
				_properties.setProperty("plugin_" + plugin, "true");

				String globalPluginName = plugin + ".global.properties";
				String localPluginName = plugin + ".local.properties";
				String prefix = plugin.substring(0, 1).toUpperCase() + plugin.substring(1) + "_";

				_properties = PropertiesUtil.updateOptionalProperties(_properties, globalPluginName, prefix,
						PropertyKind.GLOBAL);
				_properties = PropertiesUtil.updateOptionalProperties(_properties, localPluginName, prefix,
						PropertyKind.LOCAL);
			}
		}
	}

	private List<String> getEnabledPlugins() {
		return Lists.newArrayList(_properties.getProperty(PLUGINS_CONTEXT_KEY).split(","));
	}

	private static String removeWhiteSpacesAndlowerCase(String plugin) {
		return plugin.replaceAll("\\s", "").toLowerCase();
	}

}
