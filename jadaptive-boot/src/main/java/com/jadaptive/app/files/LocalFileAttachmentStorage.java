package com.jadaptive.app.files;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.security.DigestOutputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import org.apache.commons.compress.utils.IOUtils;
import org.apache.tomcat.util.buf.HexUtils;
import org.pf4j.Extension;
import org.springframework.beans.factory.annotation.Autowired;

import com.jadaptive.api.app.ConfigLocations;
import com.jadaptive.api.db.SystemOnlyObjectDatabase;
import com.jadaptive.api.db.TenantAwareObjectDatabase;
import com.jadaptive.api.files.FileAttachment;
import com.jadaptive.api.files.FileAttachmentStorage;
import com.jadaptive.api.files.FileStorageProvider;

@Extension
public class LocalFileAttachmentStorage implements FileAttachmentStorage {

	public static final String UUID = "b908313d-be99-446c-966e-89107b3901ca";
	
	public static final Path LOCATION = ConfigLocations.Defaults.get().getDropInConfig().resolve("attachments");
	public static final Path FILES = ConfigLocations.Defaults.get().getDropInConfig().resolve("files");
	
	@Autowired
	private SystemOnlyObjectDatabase<FileStorageProvider> providerDatabase;
	
	@Autowired
	private TenantAwareObjectDatabase<FileAttachment> attachmentDatabase;
	
	static {
		try {
			Files.createDirectories(LOCATION);
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}
	
	@Override
	public String getUuid() {
		return UUID;
	}

	@Override
	public InputStream getAttachmentContent(String attachmentUUID) throws FileNotFoundException {
		try {
			return Files.newInputStream(LOCATION.resolve(attachmentUUID));
		} catch(NoSuchFileException nsfe) {
			throw new FileNotFoundException(nsfe.getFile());
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}

	@Override
	public long getMaximumSize() {
		return 1024000;
	}

	@Override
	public FileAttachment createAttachment(InputStream in, String filename, String contentType, String formVariable, String template) throws IOException {
		
		String uuid = java.util.UUID.randomUUID().toString();
		FileAttachment attachment = new FileAttachment();
		attachment.setUuid(uuid);
		attachment.setFilename(filename);
		attachment.setProvider(providerDatabase.get(UUID, FileStorageProvider.class));
		attachment.setContentType(contentType);
		attachment.setFormVariable(formVariable);
		attachment.setAttachedTo(template);
		
		Path file = LOCATION.resolve(uuid);
		try(OutputStream fout = Files.newOutputStream(file)) {
			try(DigestOutputStream out = new DigestOutputStream(fout, MessageDigest.getInstance("MD5"))) {
				attachment.setSize(IOUtils.copy(in, out, 65535));
				attachment.setHash(HexUtils.toHexString(out.getMessageDigest().digest()));
			} catch (NoSuchAlgorithmException e) {
				throw new IllegalStateException(e.getMessage(), e);
			}
		}
		
		attachmentDatabase.saveOrUpdate(attachment);
		IOUtils.closeQuietly(in);
		
		return attachment;
	}

	@Override
	public String getName() {
		return "Local File System";
	}

	@Override
	public InputStream getInputstream(String path) throws IOException {
		return Files.newInputStream(FILES.resolve(path));
	}

	@Override
	public OutputStream getOutputStream(String path, String contentType) throws IOException {
		return Files.newOutputStream(FILES.resolve(path));
	}

	@Override
	public void deleteAttachment(FileAttachment object) throws IOException {
		Files.deleteIfExists(LOCATION.resolve(object.getUuid()));
	}

}
