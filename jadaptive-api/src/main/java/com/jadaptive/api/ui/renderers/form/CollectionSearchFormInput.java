package com.jadaptive.api.ui.renderers.form;

import java.util.Collection;

import org.apache.commons.lang3.StringUtils;
import org.jsoup.nodes.Element;

import com.jadaptive.api.template.TemplateViewField;
import com.jadaptive.api.ui.NamePairValue;

/**
 * Renderer for a form input that allows searching and selecting items from a collection.
 * The input is rendered as a dropdown with a search field, and selected items are displayed in
 * a table below the input. The renderer supports both read-only and editable modes, and can be configured
 * to use resource keys for display names if needed.
 */
public class CollectionSearchFormInput extends DropdownFormInput {

	private final String url;
	private final String searchField;
	private final String idField;
	private final boolean nameIsResourceKey;
	private final Collection<NamePairValue> selectedValues;

	public CollectionSearchFormInput(
			TemplateViewField field, 
			String url, 
			String searchField, 
			String idField,
			Collection<NamePairValue> selectedValues,
			boolean nameIsResourceKey
		) {
		super(field);
		this.nameIsResourceKey = nameIsResourceKey;
		this.selectedValues = selectedValues;
		this.url = url;
		this.searchField = searchField;
		this.idField = idField;
		this.resourceKey = field.getResourceKey();
		this.formVariable = field.getFormVariable();
		this.bundle = field.getBundle();
	}

	@Override
	public Class<?> getResourceClass() {
		return DropdownFormInput.class;
	}

	@Override
	public String getHtmlResource() {
		return String.format("%s.html", CollectionSearchFormInput.class);
	}

	@Override
	protected void configureInputElement() {
		super.configureInputElement();
		nameElement.dataset().put("id", idField);
		nameElement.dataset().put("url", url);
		nameElement.dataset().put("field", searchField);
		nameElement.dataset().put("form-variable", formVariable);
	}

	@Override
	protected void onRender(Element rootElement, String defaultValue, boolean readOnly, String... classes) {
		super.onRender(rootElement, defaultValue, readOnly, classes);
		var items = rootElement.getElementsByAttributeValue("jad:role", "items").first();
		if(selectedValues.size() > 0) {
			rootElement.getElementsByAttributeValue("jad:role", "empty-table").addClass("d-none");
			var template = rootElement.getElementsByAttributeValue("jad:role", "item").get(0);
			for(NamePairValue value : selectedValues) {
				Element row = template.firstElementChild().clone().removeAttr("jad:role").appendTo(items);
				
				row.getElementsByAttributeValue("jad:role", "form-variable").first().
					attr("name", formVariable).
					attr("value", value.getValue()).
					attr("id", formVariable);
				
				row.getElementsByAttributeValue("jad:role", "form-variable-text").first().
					attr("name", String.format("%sText", formVariable)).
					attr("value", value.getName()).
					attr("id", String.format("%sText", formVariable));
				
				Element displayName = row.getElementsByAttributeValue("jad:role", "display-name").first();
				displayName.text(StringUtils.defaultIfBlank(value.getName(), "-"));
				if(nameIsResourceKey) {
					displayName.attr("jad:bundle", bundle)
								.attr("jad:i18n", value.getName());
				}
			}
		}
		else {
			rootElement.getElementsByAttributeValue("jad:role", "table").select("table").first().addClass("d-none");
		}
	}

}
