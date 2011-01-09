package com.podio.item.map;

import java.beans.PropertyDescriptor;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import com.podio.app.ApplicationField;
import com.podio.item.FieldValuesUpdate;
import com.podio.item.FieldValuesView;
import com.podio.item.map.converter.FieldConverter;
import com.podio.item.map.converter.FieldConverterRegistry;

public class FieldMap {

	private final String externalId;

	private final PropertyDescriptor property;

	private final boolean single;

	private final FieldConverter converter;

	private FieldMap(String externalId, PropertyDescriptor property,
			boolean single, FieldConverter converter) {
		super();

		this.externalId = externalId;
		this.property = property;
		this.single = single;
		this.converter = converter;
	}

	public FieldValuesUpdate fromModel(Object model) {
		Object value;
		try {
			value = property.getReadMethod().invoke(model);
		} catch (Exception e) {
			throw new RuntimeException("Unable to get model value", e);
		}

		List<Map<String, Object>> apiValues = new ArrayList<Map<String, Object>>();

		if (value != null) {
			if (!single) {
				Collection subValues = (Collection) value;
				for (Object subValue : subValues) {
					apiValues.add(converter.fromModel(subValue));
				}
			} else {
				apiValues.add(converter.fromModel(value));
			}
		}

		return new FieldValuesUpdate(externalId, apiValues);
	}

	public void toModel(Object model, List<FieldValuesView> views) {
		for (FieldValuesView view : views) {
			if (view.getExternalId().equals(externalId)) {
				if (view.getValues().size() > 0) {
					try {
						if (single) {
							if (view.getValues().size() > 1) {
								throw new RuntimeException(
										"Expected at most one value");
							}

							Object value = converter.toModel(view.getValues()
									.get(0), property.getPropertyType());
							property.getWriteMethod().invoke(model, value);
						} else {
							// FIXME: Not yet done
						}
					} catch (Exception e) {
						throw new RuntimeException("Unable to set model value",
								e);
					}
				}
			}
		}
	}

	public static FieldMap get(PropertyDescriptor property,
			Map<String, ApplicationField> fieldMap) {
		Field field = property.getReadMethod().getAnnotation(Field.class);
		String externalId;
		if (field == null || field.value() == "") {
			externalId = NameUtil.toAPI(property.getName());
		} else {
			externalId = field.value();
		}

		ApplicationField applicationField = fieldMap.get(externalId);
		if (applicationField == null) {
			throw new RuntimeException("No field found with external id "
					+ externalId);
		}

		boolean single = !Collection.class.isAssignableFrom(property
				.getPropertyType());

		FieldConverter converter = FieldConverterRegistry.getConverter(
				applicationField, property.getReadMethod());

		return new FieldMap(externalId, property, single, converter);
	}
}
