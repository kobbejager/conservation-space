package com.sirma.itt.emf.semantic.label;

import com.sirma.itt.seip.definition.label.ClasspathLabelBundleProvider;
import com.sirma.itt.seip.domain.definition.label.LabelBundleProvider;
import com.sirma.itt.seip.plugin.Extension;

/**
 * Provides labels for resources located in emf-semantic-impl module.
 *
 * @author nvelkov
 */
@Extension(target = LabelBundleProvider.TARGET_NAME, order = 70)
public class EmfSemanticLabelBundleProvider extends ClasspathLabelBundleProvider {

	@Override
	protected String getBaseName() {
		return "com.sirma.itt.emf.semantic.i18n.i18n";
	}

}
