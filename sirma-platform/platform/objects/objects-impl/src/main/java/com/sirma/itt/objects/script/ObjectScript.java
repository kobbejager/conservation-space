package com.sirma.itt.objects.script;

import java.nio.charset.StandardCharsets;

import javax.inject.Inject;

import com.sirma.itt.commons.utils.string.StringUtils;
import com.sirma.itt.seip.content.Content;
import com.sirma.itt.seip.content.ContentInfo;
import com.sirma.itt.seip.content.InstanceContentService;
import com.sirma.itt.seip.domain.ObjectTypes;
import com.sirma.itt.seip.domain.security.ActionTypeConstants;
import com.sirma.itt.seip.instance.dao.InstanceType;
import com.sirma.itt.seip.instance.script.ScriptNode;

/**
 * Script node extension for object instance nodes.
 *
 * @author BBonev
 */
@InstanceType(type = ObjectTypes.OBJECT)
public class ObjectScript extends ScriptNode {

	@Inject
	private InstanceContentService instanceContentService;

	/**
	 * Attach to the current object to the given destination node
	 *
	 * @param destination
	 *            the destination
	 * @return true, if successful
	 */
	public boolean attachTo(ScriptNode destination) {
		return attachTo(destination, ActionTypeConstants.ATTACH_OBJECT);
	}

	/**
	 * Copy the content from the given template id if found to the given instance.
	 *
	 * @param newContent
	 *            the content
	 * @return true, if successful
	 */
	@Override
	public boolean setTextContent(String newContent) {
		String localContent = newContent;
		if (StringUtils.isNullOrEmpty(newContent)) {
			localContent = "";
		}
		ContentInfo view = instanceContentService.getContent(getTarget(), Content.PRIMARY_VIEW);
		Content content = Content
				.createEmpty()
					.setName(view.getName())
					.setContent(localContent, StandardCharsets.UTF_8)
					.setMimeType(view.getMimeType())
					.setView(true)
					.setPurpose(view.getContentPurpose());
		return instanceContentService.saveContent(getTarget(), content).exists();
	}
}
