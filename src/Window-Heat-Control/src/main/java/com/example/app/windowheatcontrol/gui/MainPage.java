package com.example.app.windowheatcontrol.gui;

import java.util.LinkedHashMap;
import java.util.Map;

import org.ogema.core.model.simple.FloatResource;
import org.ogema.model.locations.Room;
import org.ogema.tools.resource.util.ResourceUtils;

import com.example.app.windowheatcontrol.api.internal.RoomController;
import com.example.app.windowheatcontrol.api.internal.RoomManagement;
import com.example.app.windowheatcontrol.pattern.ElectricityStoragePattern;
import com.example.app.windowheatcontrol.patternlistener.ElectricityStorageListener;

import de.iwes.widgets.api.widgets.WidgetPage;
import de.iwes.widgets.api.widgets.dynamics.TriggeredAction;
import de.iwes.widgets.api.widgets.dynamics.TriggeringAction;
import de.iwes.widgets.api.widgets.html.StaticTable;
import de.iwes.widgets.api.widgets.sessionmanagement.OgemaHttpRequest;
import de.iwes.widgets.html.alert.Alert;
import de.iwes.widgets.html.complextable.DynamicTable;
import de.iwes.widgets.html.complextable.RowTemplate;
import de.iwes.widgets.html.form.label.Header;
import de.iwes.widgets.html.form.label.HeaderData;
import de.iwes.widgets.html.form.label.Label;
import de.iwes.widgets.html.form.textfield.ValueInputField;
import de.iwes.widgets.object.widget.table.DefaultObjectRowTemplate;
import de.iwes.widgets.resource.widget.label.ValueResourceLabel;

/**
 * An HTML page, generated from the Java code.
 */
public class MainPage {
	
	public final long UPDATE_RATE = 5*1000;
	private final WidgetPage<?> page; 
	
	// the widgets, or building blocks, of the page; defined in the constructor
	private final Header header;
	private final Alert alert;
	private final Header roomsHeader;
	private final ValueResourceLabel<FloatResource> batterySOC;
	private final DynamicTable<Room> roomTable;
	
	public MainPage(final WidgetPage<?> page, final RoomManagement rooms, final ElectricityStorageListener batteryListener) {
		this.page = page;

		//init all widgets
		
		this.header = new Header(page, "header", "Battery-extended Window-Heat Control");
		header.addDefaultStyle(HeaderData.CENTERED);
		
		// displays messages to the user
		this.alert = new Alert(page, "alert", "");
		alert.setDefaultVisibility(false);

		// this widget simply displays the value of a resource, here the state of charge of the battery
		batterySOC = new ValueResourceLabel<FloatResource>(page, "batterySOC") {
			
			private static final long serialVersionUID = 1L;

			@Override
			public void onGET(OgemaHttpRequest req) {
				final ElectricityStoragePattern activeBattery = batteryListener.getActiveBattery();
				selectItem((activeBattery != null ? activeBattery.soc : null), req);
			}
		};
		
		this.roomsHeader = new Header(page, "roomsheader", "Controlled rooms");
		roomsHeader.addDefaultStyle(HeaderData.CENTERED);
		roomsHeader.setDefaultHeaderType(2);
		
		// the roomTable widget below shows one table row per room; here we define the row content
		RowTemplate<Room> roomTemplate = new DefaultObjectRowTemplate<Room>() {
			
			@Override
			public Map<String, Object> getHeader() {
				final Map<String,Object> header = new LinkedHashMap<>();
				// keys must be chosen in agreement with cells added in addRow method below
				header.put("roomname", "Room name");
				header.put("temperaturesetpoint", "Temperature setpoint");
				header.put("nrWindowSensors", "Window sensors");
				header.put("nrThermostats", "Thermostats");
				header.put("windowstatus", "Window open");
				return header;
			}
			
			@Override
			public Row addRow(final Room room, final OgemaHttpRequest req) {
				final Row row = new Row();
				final RoomController controller = rooms.getController(room);
				final String lineId = getLineId(room);
				final String roomLabel = ResourceUtils.getHumanReadableName(room);
				// this widget displays the name of the room; since the content cannot change, we
				// simply set a default text in the constructor, and do not overwrite the onGET method
				Label name = new Label(page, "name_"+lineId, roomLabel);
				// set first column content
				row.addCell("roomname",name, 2);
				
				// FIXME remove; problem: this only works with a single thermostat, but there may be multiple
//				ValueResourceTextField<TemperatureResource> setpoint = new ValueResourceTextField<TemperatureResource>(page, "setPoint_" + lineId) {
//					
//					private static final long serialVersionUID = 1L;
//					
//					@Override
//					public void onGET(OgemaHttpRequest req) {
//						ThermostatPattern thPattern = app.thermostats.getFirstElement(room);
//						if (thPattern != null) {
//							selectItem(thPattern.setpoint, req);
//						} else {
//							selectItem(null, req);
//						}
//					}
//				};
				// this widget displays the current temperature setpoint for the room (onGET), and allows the user to change it (onPOST)
				ValueInputField<Float> setpoint = new ValueInputField<Float>(page, "setpoint_" + lineId, Float.TYPE) {

					private static final long serialVersionUID = 1L;

					@Override
					public void onPOSTComplete(String data, OgemaHttpRequest req) {
						Float value = getNumericalValue(req);
						if (value == null) {
							alert.showAlert("Please enter a valid temperature", false, req);
							return;
						}
						controller.setTemperatureSetpoint(value);
						alert.showAlert("New temperature setpoint for room " + roomLabel + ": " + value + "°C", true, req);
					}
					
					@Override
					public void onGET(OgemaHttpRequest req) {
						float temp = controller.getTemperatureSetpoint();
						setNumericalValue(temp, req);
					}
					
				};
				setpoint.setDefaultUnit("°C");
				setpoint.setDefaultPollingInterval(UPDATE_RATE);
				row.addCell("temperaturesetpoint",setpoint, 2);
				setpoint.triggerAction(setpoint, TriggeringAction.POST_REQUEST, TriggeredAction.GET_REQUEST);
				// in the onPOSTComplete method of setpoint, we set a message to be displayed by alert, hence we need to reload the alert after the POST
				setpoint.triggerAction(alert, TriggeringAction.POST_REQUEST, TriggeredAction.GET_REQUEST);
				
				// FIXME remove
//				ValueResourceLabel<BooleanResource> openStatus = new ValueResourceLabel<BooleanResource>(page, "openStatus_" + lineId) {
//					
//					private static final long serialVersionUID = 1L;
//					
//					@Override
//					public void onGET(OgemaHttpRequest req) {
//						WindowSensorPattern winPattern = app.windowSensors.getFirstElement(listElement);
//						if (winPattern != null) {
//							selectItem(winPattern.open, req);
//						} else {
//							selectItem(null, req);
//						}
//					}
//				};
				
				Label nrWindowSensors = new Label(page, "nrWindowSensors_" + lineId) {

					private static final long serialVersionUID = 1L;
					
					@Override
					public void onGET(OgemaHttpRequest req) {
						setText(String.valueOf(controller.windowSensorCount()), req);
					}
					
				};
				row.addCell("nrWindowSensors", nrWindowSensors);
				
				Label nrThermostats = new Label(page, "nrThermostats_" + lineId) {

					private static final long serialVersionUID = 1L;
					
					@Override
					public void onGET(OgemaHttpRequest req) {
						setText(String.valueOf(controller.thermostatCount()), req);
					}
					
				};
				row.addCell("nrThermostats", nrThermostats);
				
				Label openStatus = new Label(page, "openStatus_" + lineId) {
					
					private static final long serialVersionUID = 1L;
					
					@Override
					public void onGET(OgemaHttpRequest req) {
						setText(String.valueOf(controller.isWindowOpen()), req);
					}
					
				};
				
				openStatus.setDefaultPollingInterval(UPDATE_RATE);
				row.addCell("windowstatus", openStatus, 2);
				return row;
			}
		};
		
		// the table shows the same rows, independently of the user session, hence it can be made global
		roomTable = new DynamicTable<Room>(page, "roomTable", true) {
			
			private static final long serialVersionUID = 1L;
			
			@Override
			public void onGET(OgemaHttpRequest req) {
				//find all managed rooms
				updateRows(rooms.getActiveRooms(), req);
			}
		};
		roomTable.setRowTemplate(roomTemplate);
		buildPage();
		setDependencies();
	}
	
	private final void buildPage() {
		page.append(header).linebreak().append(alert).linebreak();
    	//dropDetailRoom.registerDependentWidget(windowSensorDetails);
    	//dropDetailRoom.registerDependentWidget(thermostatDetails);
		StaticTable table1 = new StaticTable(1, 2);
		page.append(table1);
		table1.setContent(0, 0, "Battery SOC:").setContent(0, 1, batterySOC);
		page.linebreak().append(roomsHeader).append(roomTable);
		//page.append(dropDetailRoom);
		StaticTable table2 = new StaticTable(1, 2);
		page.append(table2);
		//table2.setContent(0, 0, windowSensorDetails).setContent(0, 1, thermostatDetails);
	}
	
	// the page is completely static, no dependencies to be set here
	private final void setDependencies() {
	}
	
}