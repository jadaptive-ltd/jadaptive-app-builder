package com.jadaptive.api.app;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;

import com.jadaptive.api.tenant.Tenant;

/**
 * Central interface for resolving application configuration and resource
 * locations. This interface provides methods to retrieve paths for various
 * application components such as plugins, configuration files, logs, and
 * tenant-specific resources. The default implementation initializes these paths
 * based on environment variables or system properties, allowing for flexible
 * configuration of the application environment.
 */
public interface ConfigLocations {
	
	public final static class Defaults {
		
		private static String envOrDefault(String envKey, String defaultValue) {
			String value = System.getenv(envKey);
			return value != null ? value : defaultValue;
		}
		
		private static String resolvePath(String relPath) {
			var app = Paths.get(System.getProperty("jadaptive.app", System.getProperty("user.dir")));
			var ppath = Paths.get(relPath);
			return ppath.isAbsolute() ? ppath.toString() : app.resolve(ppath).toString();
		}
		
		private static ConfigLocations CONFIG_LOCATIONS = new ConfigLocations() {
			
			{
				/* Initialize system properties with environment variables or defaults */
				
				System.setProperty("jadaptive.app", envOrDefault("JADAPTIVE_APP", System.getProperty("user.dir")));
				
				System.setProperty("jadaptive.plugins",
						resolvePath(envOrDefault("JADAPTIVE_PLUGINS", System.getProperty("jadaptive.plugins", "plugins"))));
				
				System.setProperty("jadaptive.user.plugins", 
						resolvePath(envOrDefault("JADAPTIVE_USER_PLUGINS",
								System.getProperty("jadaptive.user.plugins", "user-plugins"))));
				
				System.setProperty("jadaptive.conf.d",
						resolvePath(envOrDefault("JADAPTIVE_DROP_IN_CONFIG", System.getProperty("jadaptive.conf.d", "conf.d"))));
				
				System.setProperty("jadaptive.conf",
						resolvePath(envOrDefault("JADAPTIVE_SYSTEM_CONFIG", System.getProperty("jadaptive.conf", "conf"))));
				
				System.setProperty("jadaptive.tmp",
						resolvePath(envOrDefault("JADAPTIVE_TMP", System.getProperty("jadaptive.tmp", "tmp"))));
				
				System.setProperty("jadaptive.logs",
						resolvePath(envOrDefault("JADAPTIVE_LOGS", System.getProperty("jadaptive.logs", "logs"))));
				
				System.setProperty("jadaptive.db",
						resolvePath(envOrDefault("JADAPTIVE_DB", System.getProperty("jadaptive.db", "db"))));
				
				System.setProperty("jadaptive.templatePath", envOrDefault("JADAPTIVE_TEMPLATES",
						resolvePath(System.getProperty("jadaptive.templatePath", System.getProperty("jadaptive.conf.d")))));
				
				System.setProperty("jadaptive.system",
						resolvePath(envOrDefault("JADAPTIVE_SYSTEM", System.getProperty("jadaptive.system",
								System.getProperty("jadaptive.conf.d") + File.separator + "system"))));
				
				System.setProperty("jadaptive.private",
						resolvePath(envOrDefault("JADAPTIVE_PRIVATE",
								System.getProperty("jadaptive.private", System.getProperty("jadaptive.system") + File.separator + "private"))));
				
				System.setProperty("jadaptive.privateKeys",
						resolvePath(envOrDefault("JADAPTIVE_PRIVATE_KEYS",
								System.getProperty("jadaptive.privateKeys", System.getProperty("private.dir",
										System.getProperty("jadaptive.conf") + File.separator + "private")))));
				
				System.setProperty("jadaptive.shared",
						resolvePath(envOrDefault("JADAPTIVE_SHARED", System.getProperty("jadaptive.shared",
								System.getProperty("jadaptive.system") + File.separator + "shared"))));
				
				System.setProperty("jadaptive.tenants",
						resolvePath(envOrDefault("JADAPTIVE_TENANTS", System.getProperty("jadaptive.tenants",
								System.getProperty("jadaptive.conf.d") + File.separator + "tenants"))));
			}
			
			@Override
			public Path getBasePlugins() {
				return Path.of(System.getProperty("jadaptive.plugins"));
			}
			
			@Override
			public Path getUserPlugins() {
				return Path.of(System.getProperty("jadaptive.user.plugins"));
			}
			
			@Override
			public Path getDropInConfig() {
				return Path.of(System.getProperty("jadaptive.conf.d"));
			}
			
			@Override
			public Path getConfig() {
				return Path.of(System.getProperty("jadaptive.conf"));
			}
			
			@Override
			public Path getApp() {
				return Path.of(System.getProperty("user.dir"));
			}
			
			@Override
			public Path getTemp() {
				return Path.of(System.getProperty("jadaptive.tmp"));
			}
			
			@Override
			public Path getLogs() {
				return Path.of(System.getProperty("jadaptive.logs"));
			}
			
			@Override
			public Path getDb() {
				return Path.of(System.getProperty("jadaptive.db"));
			}

			@Override
			public Path getTemplates() {
				return Path.of(System.getProperty("jadaptive.templatePath"));
			}

			@Override
			public Path getShared() {
				return Path.of(System.getProperty("jadaptive.shared"));
			}

			@Override
			public Path getSystem() {
				return Path.of(System.getProperty("jadaptive.system"));
			}

			@Override
			public Path getPrivateResources() {
				return Path.of(System.getProperty("jadaptive.private"));
			}

			@Override
			public Path getPrivateKeys() {
				return Path.of(System.getProperty("jadaptive.privateKeys"));
			}

			@Override
			public Path getTenants() {
				return Path.of(System.getProperty("jadaptive.tenants"));
			}
		};
		
		public static void set(ConfigLocations configLocations) {
			CONFIG_LOCATIONS = configLocations;
		}
		
		public static ConfigLocations get() {
			return CONFIG_LOCATIONS;
		}
	}
	
	/**
	 * Get the location for tenant-specific resources.
	 * 
	 * @return the path to the tenant-specific resources. This is the base directory for tenant-specific resources, and individual tenant directories will be located under this path based on their domain names.
	 */
	Path getTenants();
	
	/**
	 * Get the location for tenant-specific resources.
	 * 
	 * @param tenant the tenant for which to retrieve the resource location. If the tenant is a system tenant, it will return the location for private resources. For regular tenants, it will return a path under the tenants directory specific to that tenant's domain.
	 * @return the path to the tenant-specific resources. For system tenants, this will be the private resources path. For regular tenants, this will be a path under the tenants directory specific to that tenant's domain.
	 */
	default Path getTenant(Tenant tenant) {
		if(tenant.isSystem()) {
			return getPrivateResources();
		}
		return getTenants().resolve(tenant.getDomain());
	}
	
	/**
	 * Get the location for shared resources.
	 * @return
	 */
	Path getShared();
		
	/**
	 * Get the location for system resources.
	 * 
	 * @return system resources path
	 */
	Path getSystem();	
	
	/**
	 * Get the location for private resources. 
	 * @return
	 */
	Path getPrivateResources();
	
	/**
	 * Get the location for private keys. This is used for storing private keys that are
	 * used for encryption, signing, or any other cryptographic operations that require
	 * private keys. This location should be secure and protected, as it contains sensitive
	 * information that should not be exposed to unauthorized users. The application can use
	 * this location to read and write private keys as needed for its cryptographic operations.
	 * 
	 * @return private keys path
	 */
	Path getPrivateKeys();
	
	/**
	 * Get the location for template files. This is used for storing template files that
	 * can be used by the application for various purposes such as email templates, report
	 * templates, or any other type of template that the application may require.
	 * 
	 * @return template path
	 */
	Path getTemplates();

	/**
	 * Get the location for base plugins. This is used for plugins that are part of
	 * the main application distribution and should not be modified by the user. It
	 * allows for a clear separation between core application plugins and user-added
	 * plugins, ensuring that the core functionality of the application remains
	 * intact while still allowing for extensibility through user plugins.
	 * 
	 * @return base plugins path
	 */
	Path getBasePlugins();
	
	/**
	 * Get the location for user plugins. This is used for plugins that can be added
	 * or updated by the user without modifying the main application plugins. It
	 * allows for dynamic extension of the application functionality by enabling
	 * users to add their own plugins or update existing ones without changing the
	 * core application files.
	 * 
	 * @return
	 */
	Path getUserPlugins();
	
	/**
	 * Get the location for drop-in configuration files. This is used for
	 * configuration files that can be added or overridden by the user without
	 * modifying the main application configuration. It allows for flexible
	 * configuration management, enabling users to provide custom settings or
	 * override default configurations without changing the core application files.
	 * 
	 * @return
	 */
	Path getDropInConfig();
	
	/**
	 * Get the location of the main application configuration files and folders.
	 *  
	 * @return configuration path
	 */
	Path getConfig();
	
	/**
	 * Get the location of the application. This is typically the current working directory
	 * or a specified application directory. This location is used as the base for resolving
	 * relative paths for other configuration and resource locations.
	 * 
	 * @return application base path
	 */
	Path getApp();
	
	/**
	 * Get location for temporary files. This is used by the application for storing
	 * temporary data such as file uploads, temporary processing files, and other
	 * data that does not need to be persisted across application restarts or shared
	 * across the cluster.
	 * 
	 * @return temp
	 */
	Path getTemp();
	
	/**
	 * Get location for log files.
	 * 
	 * @return logs
	 */
	Path getLogs();
	
	/**
	 * Get the location of database files. This is used by the embedded MongoDB
	 * instance to store data. It is not used for external databases.
	 * 
	 * @return
	 */
	Path getDb();
}
