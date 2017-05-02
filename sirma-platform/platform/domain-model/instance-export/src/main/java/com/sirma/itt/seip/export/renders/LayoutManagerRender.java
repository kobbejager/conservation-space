package com.sirma.itt.seip.export.renders;

import javax.enterprise.context.ApplicationScoped;

import org.apache.log4j.Logger;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import com.sirma.itt.seip.export.renders.utils.JsoupUtil;
import com.sirma.itt.seip.plugin.Extension;
import com.sirmaenterprise.sep.content.idoc.ContentNode;
import com.sirmaenterprise.sep.export.renders.html.table.HtmlTableBuilder;
import com.sirmaenterprise.sep.export.renders.html.table.HtmlValueElementBuilder;

/**
 * Render for LayoutManager.
 * 
 * @author Boyan Tonchev
 */
@ApplicationScoped
@Extension(target = IdocRenderer.PLUGIN_NAME, order = 33)
public class LayoutManagerRender extends BaseRenderer {

	private static final Logger LOGGER = Logger.getLogger(LayoutManagerRender.class);

	@Override
	public boolean accept(ContentNode node) {
		return node.isLayoutManager();
	}

	@Override
	public Element render(String currentInstanceId, ContentNode node) {

		try {
			Element element = node.getElement();
			HtmlTableBuilder table = new HtmlTableBuilder("");
			setWidth(table, element, NIGHTY_NINE_PRECENT);
			Element layoutContainer = element.child(0);
			Element layoutRow = layoutContainer.child(0);
			populateTable(table, layoutRow);
			table.addAttribute(JsoupUtil.ATTRIBUTE_BORDER, "0");
			return table.build();
		} catch (IndexOutOfBoundsException e) {
			LOGGER.debug("Faild to process layoutmanager!", e);
		}
		return new HtmlTableBuilder("").build();
	}

	/**
	 * Populate <code>layoutRow</code> contents into <code>table</code> as table.
	 * 
	 * @param table
	 *            where <code>layoutRow</code> contents to be populated.
	 * @param layoutRow
	 *            elements which content have to be populated.
	 */
	private static void populateTable(HtmlTableBuilder table, Element layoutRow) {
		int columnIndex = 0;
		Elements layoutColumns = layoutRow.children();
		if (layoutColumns.isEmpty()) {
			return;
		}
		for (Element layoutColumn : layoutColumns) {
			for (Element layoutColumnValue : layoutColumn.children()) {
				for (Element columntValue : layoutColumnValue.children()) {
					table.addTdValue(0, columnIndex, new HtmlValueElementBuilder(columntValue));
				}
				int tableColumnIndex = columnIndex;
				layoutColumn.classNames().forEach(className -> table.addTdClass(0, tableColumnIndex, className));
				table.addTdStyle(0, columnIndex, "vertical-align:top;text-align: left;");
				columnIndex++;
			}
		}
	}
}