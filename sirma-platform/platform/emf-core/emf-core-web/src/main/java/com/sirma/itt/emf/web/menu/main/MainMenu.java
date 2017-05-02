package com.sirma.itt.emf.web.menu.main;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.inject.Named;

import com.sirma.itt.emf.web.application.ApplicationConfigurationProvider;
import com.sirma.itt.emf.web.plugin.AbstractPageModel;
import com.sirma.itt.emf.web.plugin.PageFragment;
import com.sirma.itt.emf.web.plugin.PageModel;
import com.sirma.itt.emf.web.plugin.Plugable;
import com.sirma.itt.seip.plugin.Extension;
import com.sirma.itt.seip.plugin.ExtensionPoint;

/**
 * Registered main menu item plugins.
 *
 * @author svelikov
 */
@Named
public class MainMenu implements Plugable {

	public static final String EXTENSION_POINT = "mainMenu";

	/** Template name of of the separator fragment. */
	public static final String MENU_SEPARATOR_TEMPLATE = "header/separator";

	@Override
	public String getExtensionPoint() {
		return EXTENSION_POINT;
	}

	/**
	 * The Class LogoExtension.
	 */
	@ApplicationScoped
	@Extension(target = EXTENSION_POINT, enabled = true, order = 100, priority = 1)
	public static class LogoExtension extends AbstractPageModel implements PageFragment {

		private static final String FRAGMENT_NAME = "logo";
		private static final String LOGO_TEMPLATE_PATH = "header/mainmenu/" + FRAGMENT_NAME + "/" + FRAGMENT_NAME;

		@Inject
		private ApplicationConfigurationProvider configurationProvider;

		@Override
		public String getPath() {
			return "/menu/main/logo.xhtml";
		}

		@Override
		public String getPageFragment() {
			Map<String, Object> model = new HashMap<>(9);
			model.put("id", FRAGMENT_NAME);
			model.put("name", FRAGMENT_NAME);
			model.put("href", configurationProvider.getUi2Url());
			model.put("icon", configurationProvider.getLogoImageName());

			return buildTemplate(model, LOGO_TEMPLATE_PATH);
		}
	}

	/**
	 * The Class SecondLogoExtension.
	 */
	@ApplicationScoped
	@Extension(target = EXTENSION_POINT, enabled = true, order = 200, priority = 1)
	public static class SecondLogoExtension extends AbstractPageModel implements PageFragment {

		private static final String FRAGMENT_NAME = "second-logo";

		private static final String SECOND_LOGO_TEMPLATE_PATH = "header/mainmenu/" + FRAGMENT_NAME + "/"
				+ FRAGMENT_NAME;

		@Override
		public String getPath() {
			return "/menu/main/second-logo.xhtml";
		}

		@Override
		public String getPageFragment() {
			return buildTemplate(createModel(FRAGMENT_NAME, null, null, null, null, null), SECOND_LOGO_TEMPLATE_PATH);
		}
	}

	/**
	 * The Class BasicSearchMenu.
	 */
	@ApplicationScoped
	@Extension(target = EXTENSION_POINT, enabled = true, order = 400, priority = 1)
	public static class BasicSearchMenu extends AbstractPageModel implements PageFragment {

		private static final String FRAGMENT_NAME = "basic-search";

		private static final String BASIC_SEARCH_TEMPLATE_PATH = "header/mainmenu/" + FRAGMENT_NAME + "/"
				+ FRAGMENT_NAME;
		@Inject
		private ApplicationConfigurationProvider configurationProvider;

		@Override
		public String getPath() {
			return "/menu/main/basic-search.xhtml";
		}

		@Override
		public String getPageFragment() {
			Map<String, Object> model = new HashMap<>(9);
			model.put("id", FRAGMENT_NAME);
			model.put("name", FRAGMENT_NAME);
			model.put("href", configurationProvider.getUi2Url() + "#/search");

			String lbl = getLabelProvider().getValue("cmf.btn.search");
			if (lbl != null) {
				model.put("label", lbl);
			}

			return buildTemplate(model, BASIC_SEARCH_TEMPLATE_PATH);
		}
	}

	/**
	 * The Class AdminMenuExtension.
	 */
	@Named
	@ApplicationScoped
	@Extension(target = MainMenu.EXTENSION_POINT, enabled = true, order = 1000, priority = 10)
	public static class AdminMenuExtension extends AbstractPageModel implements PageFragment, Plugable {

		public static final String EXTENSION_POINT = "adminMenu";

		public static final String FRAGMENT_NAME = "admin-menu";

		private static final String ADMIN_MENU_TEMPLATE_PATH = "header/mainmenu/" + FRAGMENT_NAME + "/" + FRAGMENT_NAME;

		@Inject
		@ExtensionPoint(value = EXTENSION_POINT)
		private Iterable<PageModel> extension;

		@Override
		public String getPath() {
			return "/menu/main/admin.xhtml";
		}

		@Override
		public String getExtensionPoint() {
			return EXTENSION_POINT;
		}

		@Override
		public String getPageFragment() {
			if (getAuthorityService().isAdminOrSystemUser()) {
				Iterator<PageModel> iterator = extension.iterator();
				List<String> loadHtmlPageFragments = loadHtmlPageFragments(iterator);
				return buildTemplate(
						createModel(FRAGMENT_NAME, null, "emf.adminmenu", null, loadHtmlPageFragments, Boolean.TRUE),
						ADMIN_MENU_TEMPLATE_PATH);
			}
			return "";
		}
	}

}
