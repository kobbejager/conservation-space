package com.sirma.itt.cmf.beans;

import java.io.File;

/**
 * File descriptor for local files that are proxied against desired file name
 * 
 * @author bbanchev
 */
public class LocalProxyFileDescriptor extends LocalFileDescriptor {

	/**
	 * Comment for serialVersionUID.
	 */
	private static final long serialVersionUID = -2730182502454744150L;
	private String mimicName;

	/**
	 * Instantiates a new local file descriptor.
	 *
	 * @param mimicName
	 *            is the name to represent descriptor with
	 * @param path
	 *            the actual path
	 */
	public LocalProxyFileDescriptor(String mimicName, String path) {
		super(path);
		setFilename(mimicName, new File(path));
	}

	/**
	 * Instantiates a new local file descriptor.
	 *
	 * @param mimicName
	 *            is the name to represent descriptor with
	 * @param file
	 *            the actual file
	 */
	public LocalProxyFileDescriptor(String mimicName, File file) {
		super(file);
		setFilename(mimicName, file);
	}

	/**
	 * Sets the filename.
	 * 
	 * @param mimicName
	 *            the mimic name
	 * @param file
	 *            the file
	 */
	private void setFilename(String mimicName, File file) {
		if (mimicName == null) {
			throw new IllegalArgumentException(
					"Cannot create a LocalProxyFileDescriptor with NULL name");
		}
		this.mimicName = new File(file.getParentFile(), mimicName).getAbsolutePath();
	}

	/**
	 * Gets the proxied id.
	 * 
	 * @return the proxied id
	 */
	public String getProxiedId() {
		return mimicName;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = (prime * result) + ((mimicName == null) ? 0 : mimicName.hashCode());
		result = (prime * result) + ((path == null) ? 0 : path.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null) {
			return false;
		}
		if (getClass() != obj.getClass()) {
			return false;
		}
		LocalProxyFileDescriptor other = (LocalProxyFileDescriptor) obj;
		if (mimicName == null) {
			if (other.mimicName != null) {
				return false;
			}
		} else if (!mimicName.equals(other.mimicName)) {
			return false;
		}
		if (path == null) {
			if (other.path != null) {
				return false;
			}
		} else if (!path.equals(other.path)) {
			return false;
		}
		return true;
	}

	@Override
	public String toString() {
		return "LocalProxyFileDescriptor [path=" + path + ", mimicName=" + mimicName + "]";
	}

	@Override
	public String getContainerId() {
		return null;
	}

}
