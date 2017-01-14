package com.example.androiddriver.drivermodel;

import org.ogema.core.model.simple.FloatResource;
import org.ogema.model.communication.CommunicationInformation;

/** 
 * Driver models should be provided in a separate app that only provides
 * additional data models such as the app generic-drivers/driver-models. In this way
 * external applications can configure driver connections without having to add a
 * dependency to the driver itself.
 */
public interface SampleAndroiddriverModel extends CommunicationInformation {

	/**
	 * Resource to read/write; change resource type to what the driver provides. If the driver is intended
	 * to write into existing resources that are referenced, rename to "target"
	 */
	FloatResource value();
}