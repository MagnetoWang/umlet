package com.baselet.gwt.client.copy.umlet.element.experimental.uml;

import com.baselet.gwt.client.copy.umlet.element.experimental.ElementId;
import com.baselet.gwt.client.copy.umlet.element.experimental.NewGridElement;
import com.baselet.gwt.client.copy.umlet.element.experimental.settings.Settings;
import com.baselet.gwt.client.copy.umlet.element.experimental.settings.SettingsClass;


public class Class extends NewGridElement {

	public static final ElementId ID = ElementId.UMLClass;
	@Override
	public ElementId getId() {
		return ID;
	}
	
	@Override
	public void updateConcreteModel() {
		drawer.drawRectangle(0, 0, getRealSize().width-1, getRealSize().height-1);
		properties.drawPropertiesText();
	}

	@Override
	public Settings getSettings() {
		return new SettingsClass();
	}
}

