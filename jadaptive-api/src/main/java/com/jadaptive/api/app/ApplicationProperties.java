package com.jadaptive.api.app;

import static java.nio.file.Files.createDirectories;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.security.Provider;
import java.security.Security;
import java.util.Objects;
import java.util.Properties;

import org.apache.commons.io.FileUtils;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ApplicationProperties {

	
	static ApplicationProperties instance = new ApplicationProperties();
    static Properties properties;
	
	ApplicationProperties() {
		
		checkBouncyCastleProvider();
		
		var confFolder = ConfigLocations.Defaults.get().getConfig();
		var confdFolder = ConfigLocations.Defaults.get().getDropInConfig();
		
		try {
			createDirectories(confFolder);
			createDirectories(confdFolder);
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
		
		var propertiesFile = confdFolder.resolve("ssl.properties");
		var certificateProperties = confdFolder.resolve("certificate.properties");
		var serverProperties = confdFolder.resolve("server.properties");
		
		if(!Files.exists(certificateProperties)) {
			try {
				
				FileUtils.writeStringToFile(certificateProperties.toFile(),"""		
				# Certificate Properties
				server.ssl.bundle=default
				spring.ssl.bundle.jks.default.reload-on-update=true
				spring.ssl.bundle.jks.default.key.alias=server
				spring.ssl.bundle.jks.default.keystore.location=conf.d/default/cert.p12
				spring.ssl.bundle.jks.default.keystore.password=changeit
				spring.ssl.bundle.jks.default.keystore.type=PKCS12

				""", Charset.forName("UTF-8"));
				
			} catch (IOException e) {
				throw new IllegalStateException(e.getMessage(), e);
			}
		}
		
		if(Files.exists(propertiesFile)) {
			
			if(!Files.exists(serverProperties)) {
				try {
					Properties props = new Properties();
					try(var in = Files.newInputStream(propertiesFile)) {
						props.load(in);
					}
					
					Properties newProps = new Properties();
					newProps.setProperty("server.port", props.getProperty("server.port"));
					newProps.setProperty("server.ssl.enabled", "true");
					newProps.setProperty("server.ssl.protocol", "TLS");
					
					try(var out = Files.newOutputStream(serverProperties)) {
						newProps.store(out, "Server Properties");
					}
	
				} catch(IOException e ) {
					throw new IllegalStateException(e.getMessage(), e);
				}
			}
			
			try {
				Files.delete(propertiesFile);
			} catch (IOException e) {
				throw new IllegalStateException("Cannot delete conf.d/ssl.properties! Please delete this file manually");
			}
		} else if(!Files.exists(serverProperties)) {
			
			try {
				FileUtils.writeStringToFile(serverProperties.toFile(),"""		
						# Server Properties
						server.port=443
						server.ssl=true
						""", Charset.forName("UTF-8"));
			} catch (IOException e) {
				throw new IllegalStateException(e.getMessage(), e);
			}
		}
		
		propertiesFile = confdFolder.resolve("database.properties");
		if(!Files.exists(propertiesFile)) {
			try {
				
				FileUtils.writeStringToFile(propertiesFile.toFile(),"""		
				# Database Properties
				#mongodb.embedded=true
				#mongodb.connection=
				""", Charset.forName("UTF-8"));
				
			} catch (IOException e) {
				throw new IllegalStateException(e.getMessage(), e);
			}
		}
		
		var log4j = confdFolder.resolve("log4j2.xml");
		if(!Files.exists(log4j)) {
			try {
			FileUtils.writeStringToFile(log4j.toFile(),"""	
<?xml version="1.0" encoding="UTF-8"?>
<Configuration status="WARN" monitorInterval="30">
    <Properties>
        <Property name="LOG_PATTERN">
            %d{yyyy-MM-dd HH:mm:ss.SSS} %5p --- [%15.15t] %-40.40c{1.} : %m%n%ex
        </Property>
        <Property name="LOG_PATTERN_COLOURED">
            %highlight{%d{ABSOLUTE} %d{yyyy-MM-dd HH:mm:ss.SSS} %5p --- [%15.15t] %-40.40c{1.} : %m%n%ex}{FATAL=red, ERROR=red, WARN=yellow, INFO=green, DEBUG=cyan, TRACE=black}
        </Property>
    </Properties>
    <Appenders>
        <Console name="ConsoleAppender" target="SYSTEM_OUT">
            <PatternLayout pattern="${LOG_PATTERN_COLOURED}"/>
        </Console>
		<RollingFile name="FileAppender" fileName="${sys:jadaptive.logs:-logs}/application.log"
		         filePattern="${sys:jadaptive.logs:-logs}/application-%i.log.gz">
		    <PatternLayout>
		        <Pattern>${LOG_PATTERN}</Pattern>
		    </PatternLayout>
		    <Policies>
		        <SizeBasedTriggeringPolicy size="10MB" />
		    </Policies>
		    <DefaultRolloverStrategy max="10"/>
		</RollingFile>
    </Appenders>
    <Loggers>
        <Root level="info">
            <AppenderRef ref="FileAppender" />
            <AppenderRef ref="ConsoleAppender"/>
        </Root>
    </Loggers>
</Configuration>

				""", Charset.forName("UTF-8"));
			} catch(IOException e) {
				throw new IllegalStateException(e.getMessage(), e);
			}
		}
		System.setProperty("log4j.configurationFile", log4j.toAbsolutePath().toString());
		
		
		properties = new Properties();
		
		Logger log = LoggerFactory.getLogger(ApplicationProperties.class);
		try(var str = Files.newDirectoryStream(confdFolder, f -> Files.isRegularFile(f) && f.getFileName().toString().endsWith(".properties"))) {
			for(var path : str) {
				log.info("Loading properties file {}", path.getFileName());
				try {
					properties.putAll(loadPropertiesFile(path.toFile()));
				} catch (IOException e) {
					log.error("Faild to load properties file {}", path.getFileName(), e);
				}
			}
		} catch(IOException e) {
			log.error("Failed to read properties files from conf.d", e);
		}
		checkLoaded(log);
	}
	
	private void checkBouncyCastleProvider() {
		
		Provider provider = Security.getProvider("BC");
		if(Objects.isNull(provider)) {
			Security.addProvider(new BouncyCastleProvider());
		}
	}

	public static Properties loadPropertiesFile(File propertiesFile) throws IOException {
		Properties properties = new Properties();
		try(InputStream in = new FileInputStream(propertiesFile)) {
			properties.load(in);
		} 
		return properties;
	}
	
	public static Properties getProperties() {
		return properties;
	}
	
	public static String getValue(String name, String defaultValue) {
		if(Objects.isNull(properties)) {
			return defaultValue;
		}
		String val = properties.getProperty(name);
		if(Objects.isNull(val)) {
			val = System.getProperty(name);
			if(Objects.isNull(val)) {
				return defaultValue;
			}
		}
		return val;
	}
	
	public static boolean getValue(String name, boolean defaultValue) {
		if(Objects.isNull(properties)) {
			return defaultValue;
		}
		String val = properties.getProperty(name);
		if(Objects.isNull(val)) {
			val = System.getProperty(name);
			if(Objects.isNull(val)) {
				return defaultValue;
			}
		}
		return Boolean.parseBoolean(val);
	}
	
	public static int getValue(String name, int defaultValue) {
		if(Objects.isNull(properties)) {
			return defaultValue;
		}
		String val = properties.getProperty(name);
		if(Objects.isNull(val)) {
			val = System.getProperty(name);
			if(Objects.isNull(val)) {
				return defaultValue;
			}
		}
		return Integer.parseInt(val);
	}


	private static void checkLoaded(Logger log) {
		if(Objects.isNull(properties)) {
			log.warn("No jadaptive.properties has been loaded. Using application defaults");
		}
	}

	
}
