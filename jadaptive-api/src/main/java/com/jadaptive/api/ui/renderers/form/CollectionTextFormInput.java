package com.jadaptive.api.ui.renderers.form;

import static java.util.Optional.ofNullable;

import java.util.Collection;

import org.apache.commons.lang3.StringUtils;
import org.jsoup.nodes.Element;

import com.jadaptive.api.template.ObjectTemplate;
import com.jadaptive.api.template.TemplateViewField;

public class CollectionTextFormInput extends TextFormInput {

	protected ObjectTemplate template;
	private Collection<String> values;

	public CollectionTextFormInput(ObjectTemplate template, TemplateViewField field, Collection<String> values) {
		super(template, field);
		this.values = values;
	}

	@Override
	protected void onRender(Element rootElement, String defaultValue, boolean readOnly, String... classes) {

		var container = elementForRole(rootElement, "container");
		container.dataset().put("resource-key", resourceKey);

		/* Label */
		ofNullable(elementsForRole(container, "label").first()).ifPresent(lbl -> {
			if (decorate) {
				lbl.removeAttr("jad:role");
				lbl.attr("for", getFormVariable());
				lbl.attr("jad:bundle", getBundle());
				lbl.attr("jad:i18n", String.format("%s.name", getResourceKey()));
			} else {
				lbl.remove();
			}
		});

		/* Help */
		elementForRoleOr(rootElement, "help").ifPresent(dsc -> {
			if (decorate) {
				dsc.removeAttr("jad:role");
				dsc.attr("jad:bundle", getBundle());
				dsc.attr("jad:i18n", String.format("%s.desc", getResourceKey()));
			} else {
				dsc.remove();
			}
		});

		/* Item Table */

		var tableContainer = elementForRole(rootElement, "table");
		tableContainer.attr("id", formVariable);
		
		var items = rootElement.getElementsByAttributeValue("jad:role", "items").first();
		if (values.size() > 0) {
			rootElement.getElementsByAttributeValue("jad:role", "empty-table").addClass("d-none");
			var template = rootElement.getElementsByAttributeValue("jad:role", "item").get(0);
			for (var value : values) {
				Element row = template.firstElementChild().clone().removeAttr("jad:role").appendTo(items);

				row.getElementsByAttributeValue("jad:role", "form-variable").first().attr("name", formVariable)
						.attr("value", value).attr("id", formVariable);

				Element displayName = row.getElementsByAttributeValue("jad:role", "display-name").first();
				displayName.text(StringUtils.defaultIfBlank(value, "-"));
			}
		} else {
			rootElement.getElementsByAttributeValue("jad:role", "table").select("table").first().addClass("d-none");
		}

		/* Clean up */
		if(readOnly) {
			rootElement.getElementsByClass("remove-if-read-only").remove();
		}
		else {

			var component = elementForRole(rootElement, "component");
			component.attr("id", String.format("%sTextFormInput", resourceKey));

			input = elementForRole(component, "input");
			input.dataset().put("form-variable", formVariable);
			if (!disableIDAttribute) {
				input.attr("id", String.format("%sText", resourceKey));
			}
		}

	}
}
