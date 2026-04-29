package com.jadaptive.api.app;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.FileSystem;
import java.nio.file.FileSystemNotFoundException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import com.jadaptive.api.tenant.Tenant;

public class ConfigHelper {

	private static List<ResourcePackage> sharedPackages;
	private static List<ResourcePackage> systemPrivatePackages;
	private static Map<String,List<ResourcePackage>> tenantPackages = new HashMap<>();

	public static Collection<ResourcePackage> getSharedPackages() throws IOException {
		
		if(sharedPackages==null) {
			sharedPackages = new ArrayList<>();
			
			Path sharedDir = ConfigLocations.Defaults.get().getShared();
			if(Files.exists(sharedDir)) {
				try(var stream = Files.newDirectoryStream(sharedDir, f->f.getFileName().toString().endsWith(".zip"))) {
					for(var pkg : stream) {
						try {
							URI uri = new URI(String.format("jar:%s", pkg.toUri().toString()));
							sharedPackages.add(getZipPackage(uri, pkg.getFileName().toString()));
						} catch (URISyntaxException e) {
							throw new IOException(e.getMessage(), e);
						}
					}
				} catch(IOException e) {
					throw new IOException(e.getMessage(), e);
				}
			}
		}
		return Collections.unmodifiableCollection(sharedPackages);
	}
	
	public static Collection<ResourcePackage> getTenantPackages(Tenant tenant) throws IOException {
		
		if(!tenantPackages.containsKey(tenant.getDomain())) {
			tenantPackages.put(tenant.getDomain(), new ArrayList<>());
			Path tenantDir = ConfigLocations.Defaults.get().getTenant(tenant);
			if(Files.exists(tenantDir)) {
				try(var stream = Files.newDirectoryStream(tenantDir, f->f.getFileName().toString().endsWith(".zip"))) {
					for(var pkg : stream) {
						try {
							URI uri = new URI(String.format("jar:%s", pkg.toUri().toString()));
							tenantPackages.get(tenant.getDomain()).add(getZipPackage(uri, pkg.getFileName().toString()));
						} catch (URISyntaxException | IOException e) {
							throw new RuntimeException(e.getMessage(), e);
						}
					}
				} catch(IOException e) {
					throw new IOException(e.getMessage(), e);
				}
			}
		}
		return Collections.unmodifiableCollection(tenantPackages.get(tenant.getDomain()));
	}
	
	public static Collection<ResourcePackage> getSystemPrivatePackages() throws IOException {
		if(systemPrivatePackages==null) {
			systemPrivatePackages = new ArrayList<>();
			Path privateDir = ConfigLocations.Defaults.get().getPrivateResources();
			if(Files.exists(privateDir)) {
				try(var stream = Files.newDirectoryStream(privateDir, f->f.getFileName().toString().endsWith(".zip"))) {
					for(var pkg : stream) {
						try {
							URI uri = new URI(String.format("jar:%s", pkg.toUri().toString()));
							systemPrivatePackages.add(getZipPackage(uri, pkg.getFileName().toString()));
						} catch (URISyntaxException e) {
							throw new IOException(e.getMessage(), e);
						}
					}
				}
			}
		}
		return Collections.unmodifiableCollection(systemPrivatePackages);
	}
	
	private static ResourcePackage getZipPackage(URI uri, String filename) throws IOException {
		
		FileSystem fs;
		try {
			fs = FileSystems.getFileSystem(uri);
		} catch(FileSystemNotFoundException e) {
			fs = FileSystems.newFileSystem(uri, new HashMap<>());
		}
		
		Path propertiesPath = fs.getPath("/package.properties");
		Properties properties = new Properties();
		if(Files.exists(propertiesPath)) {
			try(InputStream in = Files.newInputStream(propertiesPath)) {
				properties.load(in);
			}
		}
		return new ResourcePackage(uri, fs, filename, properties);
	}
	
}
