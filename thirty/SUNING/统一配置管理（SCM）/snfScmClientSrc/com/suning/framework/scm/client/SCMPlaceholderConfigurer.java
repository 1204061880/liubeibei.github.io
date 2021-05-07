package com.suning.framework.scm.client;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.config.PlaceholderConfigurerSupport;
import org.springframework.core.Constants;
import org.springframework.util.ObjectUtils;
import org.springframework.util.PropertyPlaceholderHelper;
import org.springframework.util.StringValueResolver;

import java.io.IOException;
import java.io.StringReader;
import java.util.Enumeration;
import java.util.Properties;
import java.util.Set;

public class SCMPlaceholderConfigurer extends PlaceholderConfigurerSupport {
    public static final int SYSTEM_PROPERTIES_MODE_NEVER = 0;
    public static final int SYSTEM_PROPERTIES_MODE_FALLBACK = 1;
    public static final int SYSTEM_PROPERTIES_MODE_OVERRIDE = 2;
    private static final Constants constants = new Constants(SCMPlaceholderConfigurer.class);
    private int systemPropertiesMode;
    private boolean searchSystemEnvironment;
    private String scmPath;

    public SCMPlaceholderConfigurer() {
        this.systemPropertiesMode = 1;

        this.searchSystemEnvironment = true;
    }

    public void setSystemPropertiesModeName(String constantName) throws IllegalArgumentException {
        this.systemPropertiesMode = constants.asNumber(constantName).intValue();
    }

    public void setSystemPropertiesMode(int systemPropertiesMode) {
        this.systemPropertiesMode = systemPropertiesMode;
    }

    public void setSearchSystemEnvironment(boolean searchSystemEnvironment) {
        this.searchSystemEnvironment = searchSystemEnvironment;
    }

    protected String resolvePlaceholder(String placeholder, Properties props, int systemPropertiesMode) {
        String propVal = null;
        if (systemPropertiesMode == 2) {
            propVal = resolveSystemProperty(placeholder);
        }
        if (propVal == null) {
            propVal = resolvePlaceholder(placeholder, props);
        }
        if ((propVal == null) && (systemPropertiesMode == 1)) {
            propVal = resolveSystemProperty(placeholder);
        }
        return propVal;
    }

    protected String resolvePlaceholder(String placeholder, Properties props) {
        return props.getProperty(placeholder);
    }

    protected String resolveSystemProperty(String key) {
        try {
            String value = System.getProperty(key);
            if ((value == null) && (this.searchSystemEnvironment)) {
            }
            return System.getenv(key);
        } catch (Throwable ex) {
            if (logger.isDebugEnabled()) {
                logger.debug("Could not access system property '" + key + "': " + ex);
            }
        }
        return null;
    }

    @Override
    protected void convertProperties(Properties props) {
        Enumeration<?> propertyNames = props.propertyNames();
        while (propertyNames.hasMoreElements()) {
            String propertyName = (String) propertyNames.nextElement();
            String propertyValue = props.getProperty(propertyName);
            String convertedValue = convertProperty(propertyName, propertyValue);
            if (!ObjectUtils.nullSafeEquals(propertyValue, convertedValue)) {
                props.setProperty(propertyName, convertedValue);
            }
        }
    }

    @Override
    protected void processProperties(ConfigurableListableBeanFactory beanFactoryToProcess, Properties props) throws BeansException {
        Properties properties = new Properties();
        SCMClient scmClient = SCMClientImpl.getInstance();
        SCMNode node = scmClient.getConfig(this.scmPath);
        node.sync();
        String configInfo = node.getValue();
        if ((configInfo == null) || ("".equals(configInfo))) {
            throw new SCMException("get scm config path:" + this.scmPath + " is null!");
        }
        StringReader stringReader = new StringReader(configInfo);
        try {
            properties.load(stringReader);
        } catch (IOException e) {
            logger.error(e);
        }
        StringValueResolver valueResolver = new PlaceholderResolvingStringValueResolver(properties);
        doProcessProperties(beanFactoryToProcess, valueResolver);
    }

    @Deprecated
    protected String parseStringValue(String strVal, Properties props, Set<?> visitedPlaceholders) {
        PropertyPlaceholderHelper helper = new PropertyPlaceholderHelper(this.placeholderPrefix, this.placeholderSuffix, this.valueSeparator, this.ignoreUnresolvablePlaceholders);
        PropertyPlaceholderHelper.PlaceholderResolver resolver = new PropertyPlaceholderConfigurerResolver(props);
        return helper.replacePlaceholders(strVal, resolver);
    }

    private class PlaceholderResolvingStringValueResolver implements StringValueResolver {
        private final PropertyPlaceholderHelper helper;
        private final PropertyPlaceholderHelper.PlaceholderResolver resolver;

        public PlaceholderResolvingStringValueResolver(Properties props) {
            this.helper = new PropertyPlaceholderHelper(SCMPlaceholderConfigurer.this.placeholderPrefix, SCMPlaceholderConfigurer.this.placeholderSuffix, SCMPlaceholderConfigurer.this.valueSeparator, SCMPlaceholderConfigurer.this.ignoreUnresolvablePlaceholders);
            this.resolver = new SCMPlaceholderConfigurer.PropertyPlaceholderConfigurerResolver(props);
        }

        @Override
        public String resolveStringValue(String strVal)
                throws BeansException {
            String value = this.helper.replacePlaceholders(strVal, this.resolver);
            return value.equals(SCMPlaceholderConfigurer.this.nullValue) ? null : value;
        }
    }

    private class PropertyPlaceholderConfigurerResolver implements PropertyPlaceholderHelper.PlaceholderResolver {
        private final Properties props;

        private PropertyPlaceholderConfigurerResolver(Properties props) {
            this.props = props;
        }

        @Override
        public String resolvePlaceholder(String placeholderName) {
            return SCMPlaceholderConfigurer.this.resolvePlaceholder(placeholderName, this.props, SCMPlaceholderConfigurer.this.systemPropertiesMode);
        }
    }

    public String getScmPath() {
        return this.scmPath;
    }

    public void setScmPath(String scmPath) {
        this.scmPath = scmPath;
    }
}
