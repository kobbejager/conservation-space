package com.sirma.itt.seip.content.upload;

import static com.sirma.itt.seip.collections.CollectionUtils.isEmpty;
import static com.sirma.itt.seip.util.EqualsHelper.nullSafeEquals;

import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.inject.Singleton;
import javax.ws.rs.core.Response.Status;

import org.apache.commons.fileupload.FileItem;

import com.sirma.itt.seip.content.Content;
import com.sirma.itt.seip.content.ContentInfo;
import com.sirma.itt.seip.content.InstanceContentService;
import com.sirma.itt.seip.domain.instance.EmfInstance;
import com.sirma.itt.seip.domain.instance.Instance;
import com.sirma.itt.seip.exception.RollbackedRuntimeException;
import com.sirma.itt.seip.io.FileDescriptor;
import com.sirma.itt.seip.rest.exceptions.BadRequestException;
import com.sirma.itt.seip.rest.exceptions.ResourceException;
import com.sirma.itt.seip.rest.models.ErrorData;
import com.sirma.itt.seip.rest.utils.JSON;

/**
 * {@link ContentUploader} handles different scenarios for content upload from different rest services.
 *
 * @author BBonev
 */
@Singleton
public class ContentUploader {

	@Inject
	private InstanceContentService instanceContentService;

	/**
	 * Upload single file that is not attached to any instance.
	 *
	 * @param uploadRequest
	 *            the upload request
	 * @param purpose
	 *            the purpose
	 * @return the content info of the uploaded file
	 */
	public ContentInfo uploadWithoutInstance(UploadRequest uploadRequest, String purpose) {
		try {
			return doUpload(uploadRequest, null, purpose, false);
		} finally {
			// clear the temp folder
			uploadRequest.getFileItemFactory().resetRepository();
		}
	}

	/**
	 * Update a content identified by the given content id. The assigned instance will be resolved based on the content
	 * id.
	 *
	 * @param contentId
	 *            the content id to update
	 * @param uploadRequest
	 *            the upload request
	 * @return the content info of the uploaded file
	 */
	public ContentInfo updateWithoutInstance(String contentId, UploadRequest uploadRequest) {
		try {
			ContentInfo info = instanceContentService.getContent(contentId, "");
			if (!info.exists()) {
				throw new ResourceException(Status.NOT_FOUND, new ErrorData("Content not found"), null);
			}
			return doUpload(uploadRequest, info.getInstanceId(), info.getContentPurpose(), false);
		} finally {
			// clear the temp folder
			uploadRequest.getFileItemFactory().resetRepository();
		}
	}

	/**
	 * Update a content identified by the given content id. The assigned instance will be resolved based on the content
	 * id.
	 *
	 * @param contentId
	 *            the content id to update
	 * @param uploadRequest
	 *            the upload request
	 * @param purpose
	 *            the purpose
	 * @return the content info of the uploaded file
	 */
	public ContentInfo updateWithoutInstance(String contentId, UploadRequest uploadRequest, String purpose) {
		try {
			ContentInfo info = instanceContentService.getContent(contentId, "");
			return doUpload(uploadRequest, info.getInstanceId(), purpose, false);
		} finally {
			// clear the temp folder
			uploadRequest.getFileItemFactory().resetRepository();
		}
	}

	/**
	 * Updates the given content according to the data passed in the uploadRequest.
	 *
	 * @param instance
	 *            the instance that is related to the content for update
	 * @param content
	 *            is the content to update
	 * @param contentId
	 *            is the content ID
	 * @return the {@link ContentInfo} of the updated content
	 */
	public ContentInfo updateContent(Instance instance, Content content, String contentId) {
		ContentInfo info = instanceContentService.getContent(contentId, "");
		content.setPurpose(info.getContentPurpose());
		return instanceContentService.saveContent(instance, content);
	}

	/**
	 * Upload/update single file that is or will be attached to the given instance.
	 *
	 * @param uploadRequest
	 *            the upload request
	 * @param instanceId
	 *            the instance id
	 * @param purpose
	 *            the purpose
	 * @param version
	 *            if the upload should create new version or not
	 * @return the content info of the uploaded file
	 */
	public ContentInfo uploadForInstance(UploadRequest uploadRequest, String instanceId, String purpose,
			boolean version) {
		try {
			return doUpload(uploadRequest, instanceId, purpose, version);
		} finally {
			// clear the temp folder
			uploadRequest.getFileItemFactory().resetRepository();
		}
	}

	private ContentInfo doUpload(UploadRequest uploadRequest, Serializable instanceId, String purpose,
			boolean version) {
		Content content = buildContentFromRequest(uploadRequest);
		content.setDetectedMimeTypeFromContent(true);
		content.setPurpose(purpose);
		content.setVersionable(version);
		// enable indexing by default for uploaded files
		content.setIndexable(true);

		// dummy instance
		Instance instance = new EmfInstance();
		instance.setId(instanceId);
		instance.addAllProperties(getRelatedInstanceMetadata(uploadRequest));

		return instanceContentService.saveContent(instance, content);
	}

	/**
	 * Extracts the document content from the given {@link UploadRequest} and converts it into a {@link Content}
	 * instance.
	 *
	 * @param uploadRequest
	 *            is the upload request
	 * @return the content
	 */
	@SuppressWarnings("static-method")
	public Content buildContentFromRequest(UploadRequest uploadRequest) {
		List<FileItem> items = uploadRequest.getRequestItems();
		if (isEmpty(items)) {
			throw new BadRequestException("Expect at least one file item for upload");
		}

		List<Content> contents = items
				.stream()
					.filter(item -> !item.isFormField())
					.map(ContentUploader::toContent)
					.collect(Collectors.toList());

		if (contents.size() != 1) {
			throw new BadRequestException("Expected only one file item for upload");
		}

		return contents.get(0);
	}

	/**
	 * Reads a form field with name {@code metadata} that is of type application/json and converts it's value to a
	 * kay/value map.
	 *
	 * @param uploadRequest
	 *            the upload request
	 * @return the related instance metadata or empty {@link Map} if non are found
	 */
	@SuppressWarnings("static-method")
	public Map<String, Serializable> getRelatedInstanceMetadata(UploadRequest uploadRequest) {
		return uploadRequest
				.getRequestItems()
					.stream()
					.filter(FileItem::isFormField)
					.filter(item -> nullSafeEquals(item.getFieldName(), "metadata"))
					.findFirst()
					.map(ContentUploader::toStreamSupplier)
					.map(supplier -> JSON.readObject(supplier.get(), JSON::jsonToMap))
					.orElse(Collections.emptyMap());
	}

	@SuppressWarnings("boxing")
	static Content toContent(FileItem item) {
		return Content
				.createEmpty()
					.setContent(FileDescriptor.create(item.getName(), toStreamSupplier(item), item.getSize()))
					.setName(item.getName())
					.setContentLength(item.getSize())
					.setMimeType(item.getContentType());
	}

	private static Supplier<InputStream> toStreamSupplier(FileItem item) {
		return () -> {
			try {
				return item.getInputStream();
			} catch (IOException e) {
				throw new RollbackedRuntimeException(e);
			}
		};
	}
}
